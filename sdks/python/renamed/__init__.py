"""Official Python SDK for renamed.to API."""

from renamed.client import RenamedClient, AsyncJob
from renamed.exceptions import (
    RenamedError,
    AuthenticationError,
    RateLimitError,
    ValidationError,
    NetworkError,
    TimeoutError,
    InsufficientCreditsError,
    JobError,
)
from renamed.types import (
    RenameResult,
    RenameOptions,
    PdfSplitOptions,
    PdfSplitResult,
    SplitDocument,
    JobStatusResponse,
    JobStatus,
    ExtractOptions,
    ExtractResult,
    User,
)

__version__ = "0.1.0"

__all__ = [
    # Client
    "RenamedClient",
    "AsyncJob",
    # Exceptions
    "RenamedError",
    "AuthenticationError",
    "RateLimitError",
    "ValidationError",
    "NetworkError",
    "TimeoutError",
    "InsufficientCreditsError",
    "JobError",
    # Types
    "RenameResult",
    "RenameOptions",
    "PdfSplitOptions",
    "PdfSplitResult",
    "SplitDocument",
    "JobStatusResponse",
    "JobStatus",
    "ExtractOptions",
    "ExtractResult",
    "User",
]
