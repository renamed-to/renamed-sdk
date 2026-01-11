"""Exception classes for renamed.to SDK."""

from __future__ import annotations

from typing import Any


class RenamedError(Exception):
    """Base exception for all renamed.to SDK errors."""

    def __init__(
        self,
        message: str,
        code: str = "UNKNOWN_ERROR",
        status_code: int | None = None,
        details: Any = None,
    ) -> None:
        super().__init__(message)
        self.message = message
        self.code = code
        self.status_code = status_code
        self.details = details


class AuthenticationError(RenamedError):
    """Authentication error - invalid or missing API key."""

    def __init__(self, message: str = "Invalid or missing API key") -> None:
        super().__init__(message, "AUTHENTICATION_ERROR", 401)


class RateLimitError(RenamedError):
    """Rate limit exceeded."""

    def __init__(
        self,
        message: str = "Rate limit exceeded",
        retry_after: int | None = None,
    ) -> None:
        super().__init__(message, "RATE_LIMIT_ERROR", 429)
        self.retry_after = retry_after


class ValidationError(RenamedError):
    """Validation error - invalid request parameters."""

    def __init__(self, message: str, details: Any = None) -> None:
        super().__init__(message, "VALIDATION_ERROR", 400, details)


class NetworkError(RenamedError):
    """Network error - connection failed."""

    def __init__(self, message: str = "Network request failed") -> None:
        super().__init__(message, "NETWORK_ERROR")


class TimeoutError(RenamedError):
    """Timeout error - request took too long."""

    def __init__(self, message: str = "Request timed out") -> None:
        super().__init__(message, "TIMEOUT_ERROR")


class InsufficientCreditsError(RenamedError):
    """Insufficient credits error."""

    def __init__(self, message: str = "Insufficient credits") -> None:
        super().__init__(message, "INSUFFICIENT_CREDITS", 402)


class JobError(RenamedError):
    """Job error - async job failed."""

    def __init__(self, message: str, job_id: str | None = None) -> None:
        super().__init__(message, "JOB_ERROR")
        self.job_id = job_id


def from_http_status(status: int, status_text: str, payload: Any = None) -> RenamedError:
    """Create appropriate error from HTTP status code."""
    message = status_text
    if isinstance(payload, dict) and "error" in payload:
        message = str(payload["error"])

    if status == 401:
        return AuthenticationError(message)
    if status == 402:
        return InsufficientCreditsError(message)
    if status in (400, 422):
        return ValidationError(message, payload)
    if status == 429:
        retry_after = payload.get("retryAfter") if isinstance(payload, dict) else None
        return RateLimitError(message, retry_after)

    return RenamedError(message, "API_ERROR", status, payload)
