"""Main client for renamed.to API."""

from __future__ import annotations

import asyncio
import logging
import mimetypes
import sys
import time
from pathlib import Path
from typing import Any, BinaryIO, Callable, Dict, Optional, Tuple, Union

import httpx

from renamed.exceptions import (
    AuthenticationError,
    JobError,
    NetworkError,
    RenamedError,
    TimeoutError,
    from_http_status,
)
from renamed.types import (
    ExtractOptions,
    ExtractResult,
    JobStatusResponse,
    MIME_TYPES,
    PdfSplitOptions,
    PdfSplitResult,
    RenameOptions,
    RenameResult,
    User,
)

DEFAULT_BASE_URL = "https://www.renamed.to/api/v1"
DEFAULT_TIMEOUT = 30.0
DEFAULT_MAX_RETRIES = 2
POLL_INTERVAL = 2.0
MAX_POLL_ATTEMPTS = 150  # 5 minutes at 2s intervals

# Type alias for file input
FileInput = Union[str, Path, bytes, BinaryIO]


def _get_mime_type(filename: str) -> str:
    """Get MIME type from filename."""
    ext = Path(filename).suffix.lower()
    if ext in MIME_TYPES:
        return MIME_TYPES[ext]
    mime_type, _ = mimetypes.guess_type(filename)
    return mime_type or "application/octet-stream"


def _mask_api_key(api_key: str) -> str:
    """Mask API key for logging. Shows first 3 chars + last 4 chars."""
    if len(api_key) <= 7:
        return "***"
    return f"{api_key[:3]}...{api_key[-4:]}"


def _format_file_size(size_bytes: int) -> str:
    """Format file size in human-readable format."""
    if size_bytes < 1024:
        return f"{size_bytes} B"
    if size_bytes < 1024 * 1024:
        return f"{size_bytes / 1024:.1f} KB"
    return f"{size_bytes / (1024 * 1024):.1f} MB"


def _extract_path(url: str, base_url: str) -> str:
    """Extract path from URL for logging (don't log full URL)."""
    if url.startswith(base_url):
        return url[len(base_url):]
    if url.startswith("http://") or url.startswith("https://"):
        # External URL - show just the path portion
        from urllib.parse import urlparse
        parsed = urlparse(url)
        return parsed.path
    return url


def _create_default_logger() -> logging.Logger:
    """Create a default logger that outputs to stderr with DEBUG level."""
    logger = logging.getLogger("renamed")

    # Only configure if no handlers exist (avoid duplicate handlers)
    if not logger.handlers:
        handler = logging.StreamHandler(sys.stderr)
        handler.setLevel(logging.DEBUG)
        formatter = logging.Formatter("[Renamed] %(message)s")
        handler.setFormatter(formatter)
        logger.addHandler(handler)

    logger.setLevel(logging.DEBUG)
    return logger


class AsyncJob:
    """Async job handle for long-running operations like PDF split."""

    def __init__(
        self,
        client: RenamedClient,
        status_url: str,
        poll_interval: float = POLL_INTERVAL,
        max_attempts: int = MAX_POLL_ATTEMPTS,
        *,
        job_id: Optional[str] = None,
    ) -> None:
        self._client = client
        self._status_url = status_url
        self._job_id = job_id
        self._poll_interval = poll_interval
        self._max_attempts = max_attempts

    def _log_job_status(self, status: JobStatusResponse) -> None:
        """Log job polling status."""
        if self._client._logger:
            job_id = status.job_id or self._job_id or "unknown"
            # Truncate job_id for readability
            display_id = job_id[:8] if len(job_id) > 8 else job_id
            progress_str = f" ({status.progress}%)" if status.progress is not None else ""
            self._client._logger.debug(f"Job {display_id}: {status.status}{progress_str}")

    def status(self) -> JobStatusResponse:
        """Get current job status."""
        response = self._client._request("GET", self._status_url)
        return JobStatusResponse.model_validate(response)

    def wait(
        self,
        on_progress: Optional[Callable[[JobStatusResponse], None]] = None,
    ) -> PdfSplitResult:
        """
        Wait for job completion, polling at regular intervals.

        Args:
            on_progress: Optional callback called with status updates

        Returns:
            The completed job result

        Raises:
            JobError: If the job fails or times out
        """
        attempts = 0

        while attempts < self._max_attempts:
            status = self.status()

            # Log job status
            self._log_job_status(status)

            if on_progress:
                on_progress(status)

            if status.status == "completed" and status.result:
                return status.result

            if status.status == "failed":
                raise JobError(status.error or "Job failed", status.job_id)

            attempts += 1
            time.sleep(self._poll_interval)

        raise JobError("Job polling timeout exceeded")

    async def status_async(self) -> JobStatusResponse:
        """Get current job status (async)."""
        response = await self._client._request_async("GET", self._status_url)
        return JobStatusResponse.model_validate(response)

    async def wait_async(
        self,
        on_progress: Optional[Callable[[JobStatusResponse], None]] = None,
    ) -> PdfSplitResult:
        """
        Wait for job completion (async version).

        Args:
            on_progress: Optional callback called with status updates

        Returns:
            The completed job result

        Raises:
            JobError: If the job fails or times out
        """
        attempts = 0

        while attempts < self._max_attempts:
            status = await self.status_async()

            # Log job status
            self._log_job_status(status)

            if on_progress:
                on_progress(status)

            if status.status == "completed" and status.result:
                return status.result

            if status.status == "failed":
                raise JobError(status.error or "Job failed", status.job_id)

            attempts += 1
            await asyncio.sleep(self._poll_interval)

        raise JobError("Job polling timeout exceeded")


class RenamedClient:
    """
    renamed.to API client.

    Args:
        api_key: API key for authentication (starts with rt_)
        base_url: Base URL for the API (default: https://www.renamed.to/api/v1)
        timeout: Request timeout in seconds (default: 30.0)
        max_retries: Maximum number of retries for failed requests (default: 2)
        debug: Enable debug logging to stderr (default: False)
        logger: Custom logger instance (overrides debug parameter)

    Example:
        ```python
        from renamed import RenamedClient

        client = RenamedClient(api_key="rt_your_api_key")
        result = client.rename("invoice.pdf")
        print(result.suggested_filename)

        # With debug logging
        client = RenamedClient(api_key="rt_your_api_key", debug=True)
        ```
    """

    def __init__(
        self,
        api_key: str,
        *,
        base_url: str = DEFAULT_BASE_URL,
        timeout: float = DEFAULT_TIMEOUT,
        max_retries: int = DEFAULT_MAX_RETRIES,
        debug: bool = False,
        logger: Optional[logging.Logger] = None,
    ) -> None:
        if not api_key:
            raise AuthenticationError("API key is required")

        self._api_key = api_key
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout
        self._max_retries = max_retries

        # Configure logging
        if logger is not None:
            self._logger: Optional[logging.Logger] = logger
        elif debug:
            self._logger = _create_default_logger()
        else:
            self._logger = None

        self._sync_client = httpx.Client(
            timeout=timeout,
            headers={"Authorization": f"Bearer {api_key}"},
        )
        self._async_client: Optional[httpx.AsyncClient] = None

        # Log initialization
        if self._logger:
            masked_key = _mask_api_key(api_key)
            self._logger.debug(f"Client initialized (api_key={masked_key})")

    def _get_async_client(self) -> httpx.AsyncClient:
        """Get or create async client."""
        if self._async_client is None:
            self._async_client = httpx.AsyncClient(
                timeout=self._timeout,
                headers={"Authorization": f"Bearer {self._api_key}"},
            )
        return self._async_client

    def _build_url(self, path: str) -> str:
        """Build full URL from path."""
        if path.startswith("http://") or path.startswith("https://"):
            return path
        if path.startswith("/"):
            return f"{self._base_url}{path}"
        return f"{self._base_url}/{path}"

    def _handle_response(self, response: httpx.Response) -> Any:
        """Handle response and raise appropriate errors."""
        if response.status_code >= 400:
            try:
                payload = response.json()
            except Exception:
                payload = response.text
            raise from_http_status(response.status_code, response.reason_phrase, payload)

        if response.text:
            return response.json()
        return {}

    def _request(self, method: str, path: str, **kwargs: Any) -> Any:
        """Make a request with retries."""
        url = self._build_url(path)
        display_path = _extract_path(url, self._base_url)
        last_error: Optional[Exception] = None
        attempts = 0
        start_time = time.perf_counter()

        while attempts <= self._max_retries:
            try:
                response = self._sync_client.request(method, url, **kwargs)
                elapsed_ms = (time.perf_counter() - start_time) * 1000

                # Log successful request
                if self._logger:
                    self._logger.debug(
                        f"{method} {display_path} -> {response.status_code} ({elapsed_ms:.0f}ms)"
                    )

                return self._handle_response(response)
            except httpx.ConnectError as e:
                last_error = NetworkError(str(e))
            except httpx.TimeoutException as e:
                last_error = TimeoutError(str(e))
            except Exception as e:
                last_error = e
                # Don't retry client errors
                if isinstance(e, RenamedError) and e.status_code and 400 <= e.status_code < 500:
                    elapsed_ms = (time.perf_counter() - start_time) * 1000
                    if self._logger:
                        self._logger.debug(
                            f"{method} {display_path} -> {e.status_code} ({elapsed_ms:.0f}ms)"
                        )
                    raise

            attempts += 1
            if attempts <= self._max_retries:
                backoff_ms = int(2**attempts * 100)
                if self._logger:
                    self._logger.debug(
                        f"Retry attempt {attempts}/{self._max_retries}, waiting {backoff_ms}ms"
                    )
                time.sleep(backoff_ms / 1000)

        if last_error:
            raise last_error
        raise NetworkError()

    async def _request_async(self, method: str, path: str, **kwargs: Any) -> Any:
        """Make an async request with retries."""
        url = self._build_url(path)
        display_path = _extract_path(url, self._base_url)
        client = self._get_async_client()
        last_error: Optional[Exception] = None
        attempts = 0
        start_time = time.perf_counter()

        while attempts <= self._max_retries:
            try:
                response = await client.request(method, url, **kwargs)
                elapsed_ms = (time.perf_counter() - start_time) * 1000

                # Log successful request
                if self._logger:
                    self._logger.debug(
                        f"{method} {display_path} -> {response.status_code} ({elapsed_ms:.0f}ms)"
                    )

                return self._handle_response(response)
            except httpx.ConnectError as e:
                last_error = NetworkError(str(e))
            except httpx.TimeoutException as e:
                last_error = TimeoutError(str(e))
            except Exception as e:
                last_error = e
                if isinstance(e, RenamedError) and e.status_code and 400 <= e.status_code < 500:
                    elapsed_ms = (time.perf_counter() - start_time) * 1000
                    if self._logger:
                        self._logger.debug(
                            f"{method} {display_path} -> {e.status_code} ({elapsed_ms:.0f}ms)"
                        )
                    raise

            attempts += 1
            if attempts <= self._max_retries:
                backoff_ms = int(2**attempts * 100)
                if self._logger:
                    self._logger.debug(
                        f"Retry attempt {attempts}/{self._max_retries}, waiting {backoff_ms}ms"
                    )
                await asyncio.sleep(backoff_ms / 1000)

        if last_error:
            raise last_error
        raise NetworkError()

    def _prepare_file(
        self,
        file: FileInput,
        filename: Optional[str] = None,
    ) -> Tuple[str, bytes, str]:
        """Prepare file for upload. Returns (filename, content, mime_type)."""
        if isinstance(file, (str, Path)):
            path = Path(file)
            content = path.read_bytes()
            name = filename or path.name
            return name, content, _get_mime_type(name)

        if isinstance(file, bytes):
            name = filename or "file"
            return name, file, _get_mime_type(name)

        # BinaryIO
        content = file.read()
        file_name: Optional[Union[str, bytes]] = getattr(file, "name", None)
        if file_name is None:
            name = filename or "file"
        elif isinstance(file_name, bytes):
            name = filename or file_name.decode()
        else:
            name = filename or file_name
        name = Path(name).name
        return name, content, _get_mime_type(name)

    def _log_upload(self, filename: str, size_bytes: int) -> None:
        """Log file upload details."""
        if self._logger:
            size_str = _format_file_size(size_bytes)
            self._logger.debug(f"Upload: {filename} ({size_str})")

    def _upload_file(
        self,
        path: str,
        file: FileInput,
        filename: Optional[str] = None,
        field_name: str = "file",
        additional_fields: Optional[Dict[str, str]] = None,
    ) -> Any:
        """Upload a file to the API."""
        name, content, mime_type = self._prepare_file(file, filename)

        # Log upload
        self._log_upload(name, len(content))

        files = {field_name: (name, content, mime_type)}
        data = additional_fields or {}

        return self._request("POST", path, files=files, data=data)

    async def _upload_file_async(
        self,
        path: str,
        file: FileInput,
        filename: Optional[str] = None,
        field_name: str = "file",
        additional_fields: Optional[Dict[str, str]] = None,
    ) -> Any:
        """Upload a file to the API (async)."""
        name, content, mime_type = self._prepare_file(file, filename)

        # Log upload
        self._log_upload(name, len(content))

        files = {field_name: (name, content, mime_type)}
        data = additional_fields or {}

        return await self._request_async("POST", path, files=files, data=data)

    def rename(
        self,
        file: FileInput,
        *,
        options: Optional[RenameOptions] = None,
        template: Optional[str] = None,
    ) -> RenameResult:
        """
        Rename a file using AI.

        Args:
            file: File to rename (path, bytes, or file-like object)
            options: Rename options (deprecated, use template instead)
            template: Custom template for filename generation

        Returns:
            RenameResult with suggested filename and folder path

        Example:
            ```python
            result = client.rename("invoice.pdf")
            print(result.suggested_filename)  # "2025-01-15_AcmeCorp_INV-12345.pdf"
            ```
        """
        additional_fields: dict[str, str] = {}
        if template:
            additional_fields["template"] = template
        elif options and options.template:
            additional_fields["template"] = options.template

        response = self._upload_file(
            "/rename",
            file,
            additional_fields=additional_fields if additional_fields else None,
        )
        return RenameResult.model_validate(response)

    async def rename_async(
        self,
        file: FileInput,
        *,
        options: Optional[RenameOptions] = None,
        template: Optional[str] = None,
    ) -> RenameResult:
        """Rename a file using AI (async version)."""
        additional_fields: dict[str, str] = {}
        if template:
            additional_fields["template"] = template
        elif options and options.template:
            additional_fields["template"] = options.template

        response = await self._upload_file_async(
            "/rename",
            file,
            additional_fields=additional_fields if additional_fields else None,
        )
        return RenameResult.model_validate(response)

    def pdf_split(
        self,
        file: FileInput,
        *,
        options: Optional[PdfSplitOptions] = None,
        mode: Optional[str] = None,
        pages_per_split: Optional[int] = None,
    ) -> AsyncJob:
        """
        Split a PDF into multiple documents.

        Returns an AsyncJob that can be polled for completion.

        Args:
            file: PDF file to split
            options: Split options (deprecated, use mode/pages_per_split instead)
            mode: Split mode ('auto', 'pages', or 'blank')
            pages_per_split: Number of pages per split (for 'pages' mode)

        Returns:
            AsyncJob that can be waited on for the result

        Example:
            ```python
            job = client.pdf_split("multi-page.pdf", mode="auto")
            result = job.wait(lambda s: print(f"Progress: {s.progress}%"))
            for doc in result.documents:
                print(doc.filename, doc.download_url)
            ```
        """
        additional_fields: dict[str, str] = {}

        effective_mode = mode or (options.mode if options else None)
        effective_pages = pages_per_split or (options.pages_per_split if options else None)

        if effective_mode:
            additional_fields["mode"] = effective_mode
        if effective_pages is not None:
            additional_fields["pagesPerSplit"] = str(effective_pages)

        response = self._upload_file(
            "/pdf-split",
            file,
            additional_fields=additional_fields if additional_fields else None,
        )

        # Extract job_id from response if available for logging
        extracted_job_id = response.get("jobId") or response.get("job_id")

        return AsyncJob(self, response["statusUrl"], job_id=extracted_job_id)

    async def pdf_split_async(
        self,
        file: FileInput,
        *,
        options: Optional[PdfSplitOptions] = None,
        mode: Optional[str] = None,
        pages_per_split: Optional[int] = None,
    ) -> AsyncJob:
        """Split a PDF into multiple documents (async version)."""
        additional_fields: dict[str, str] = {}

        effective_mode = mode or (options.mode if options else None)
        effective_pages = pages_per_split or (options.pages_per_split if options else None)

        if effective_mode:
            additional_fields["mode"] = effective_mode
        if effective_pages is not None:
            additional_fields["pagesPerSplit"] = str(effective_pages)

        response = await self._upload_file_async(
            "/pdf-split",
            file,
            additional_fields=additional_fields if additional_fields else None,
        )

        # Extract job_id from response if available for logging
        extracted_job_id = response.get("jobId") or response.get("job_id")

        return AsyncJob(self, response["statusUrl"], job_id=extracted_job_id)

    def extract(
        self,
        file: FileInput,
        *,
        options: Optional[ExtractOptions] = None,
        prompt: Optional[str] = None,
        schema: Optional[Dict[str, Any]] = None,
    ) -> ExtractResult:
        """
        Extract structured data from a document.

        Args:
            file: Document to extract data from
            options: Extract options (deprecated, use prompt/schema instead)
            prompt: Natural language description of what to extract
            schema: JSON schema defining the structure of data to extract

        Returns:
            ExtractResult with extracted data

        Example:
            ```python
            result = client.extract("invoice.pdf", prompt="Extract invoice details")
            print(result.data)
            ```
        """
        additional_fields: dict[str, str] = {}

        effective_prompt = prompt or (options.prompt if options else None)
        effective_schema = schema or (options.schema_ if options else None)

        if effective_prompt:
            additional_fields["prompt"] = effective_prompt
        if effective_schema:
            import json

            additional_fields["schema"] = json.dumps(effective_schema)

        response = self._upload_file(
            "/extract",
            file,
            additional_fields=additional_fields if additional_fields else None,
        )
        return ExtractResult.model_validate(response)

    async def extract_async(
        self,
        file: FileInput,
        *,
        options: Optional[ExtractOptions] = None,
        prompt: Optional[str] = None,
        schema: Optional[Dict[str, Any]] = None,
    ) -> ExtractResult:
        """Extract structured data from a document (async version)."""
        additional_fields: dict[str, str] = {}

        effective_prompt = prompt or (options.prompt if options else None)
        effective_schema = schema or (options.schema_ if options else None)

        if effective_prompt:
            additional_fields["prompt"] = effective_prompt
        if effective_schema:
            import json

            additional_fields["schema"] = json.dumps(effective_schema)

        response = await self._upload_file_async(
            "/extract",
            file,
            additional_fields=additional_fields if additional_fields else None,
        )
        return ExtractResult.model_validate(response)

    def get_user(self) -> User:
        """
        Get current user profile and credits.

        Returns:
            User profile with credits balance

        Example:
            ```python
            user = client.get_user()
            print(f"Credits remaining: {user.credits}")
            ```
        """
        response = self._request("GET", "/user")
        return User.model_validate(response)

    async def get_user_async(self) -> User:
        """Get current user profile and credits (async version)."""
        response = await self._request_async("GET", "/user")
        return User.model_validate(response)

    def download_file(self, url: str) -> bytes:
        """
        Download a file from a URL (e.g., split document).

        Args:
            url: URL to download from

        Returns:
            File content as bytes

        Example:
            ```python
            result = job.wait()
            for doc in result.documents:
                content = client.download_file(doc.download_url)
                Path(doc.filename).write_bytes(content)
            ```
        """
        start_time = time.perf_counter()
        response = self._sync_client.get(url)
        elapsed_ms = (time.perf_counter() - start_time) * 1000

        if self._logger:
            display_path = _extract_path(url, self._base_url)
            self._logger.debug(
                f"GET {display_path} -> {response.status_code} ({elapsed_ms:.0f}ms)"
            )

        if response.status_code >= 400:
            raise from_http_status(response.status_code, response.reason_phrase)
        return response.content

    async def download_file_async(self, url: str) -> bytes:
        """Download a file from a URL (async version)."""
        client = self._get_async_client()
        start_time = time.perf_counter()
        response = await client.get(url)
        elapsed_ms = (time.perf_counter() - start_time) * 1000

        if self._logger:
            display_path = _extract_path(url, self._base_url)
            self._logger.debug(
                f"GET {display_path} -> {response.status_code} ({elapsed_ms:.0f}ms)"
            )

        if response.status_code >= 400:
            raise from_http_status(response.status_code, response.reason_phrase)
        return response.content

    def close(self) -> None:
        """Close the client and release resources."""
        self._sync_client.close()
        if self._async_client:
            # Can't close async client synchronously, but at least clear the reference
            self._async_client = None

    async def aclose(self) -> None:
        """Close the client and release resources (async)."""
        self._sync_client.close()
        if self._async_client:
            await self._async_client.aclose()
            self._async_client = None

    def __enter__(self) -> RenamedClient:
        return self

    def __exit__(self, *args: Any) -> None:
        self.close()

    async def __aenter__(self) -> RenamedClient:
        return self

    async def __aexit__(self, *args: Any) -> None:
        await self.aclose()
