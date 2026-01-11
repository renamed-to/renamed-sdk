"""Type definitions for renamed.to SDK."""

from __future__ import annotations

from typing import Any, Dict, List, Literal, Optional

from pydantic import BaseModel, ConfigDict, Field


class RenameResult(BaseModel):
    """Result of a rename operation."""

    model_config = ConfigDict(populate_by_name=True)

    original_filename: str = Field(alias="originalFilename")
    """Original filename that was uploaded."""

    suggested_filename: str = Field(alias="suggestedFilename")
    """AI-suggested new filename."""

    folder_path: Optional[str] = Field(default=None, alias="folderPath")
    """Suggested folder path for organization."""

    confidence: Optional[float] = None
    """Confidence score (0-1) of the suggestion."""


class RenameOptions(BaseModel):
    """Options for rename operation."""

    template: Optional[str] = None
    """Custom template for filename generation."""


class PdfSplitOptions(BaseModel):
    """Options for PDF split operation."""

    model_config = ConfigDict(populate_by_name=True)

    mode: Optional[Literal["auto", "pages", "blank"]] = None
    """Split mode: 'auto' (AI-detected), 'pages' (every N pages), 'blank' (at blank pages)."""

    pages_per_split: Optional[int] = Field(default=None, alias="pagesPerSplit")
    """Number of pages per split (for 'pages' mode)."""


JobStatus = Literal["pending", "processing", "completed", "failed"]
"""Status of an async job."""


class SplitDocument(BaseModel):
    """A single document from PDF split."""

    model_config = ConfigDict(populate_by_name=True)

    index: int
    """Document index (0-based)."""

    filename: str
    """Suggested filename for this document."""

    pages: str
    """Page range included in this document."""

    download_url: str = Field(alias="downloadUrl")
    """URL to download this document."""

    size: int
    """Size in bytes."""


class PdfSplitResult(BaseModel):
    """Result of PDF split operation."""

    model_config = ConfigDict(populate_by_name=True)

    original_filename: str = Field(alias="originalFilename")
    """Original filename."""

    documents: List[SplitDocument]
    """Split documents."""

    total_pages: int = Field(alias="totalPages")
    """Total number of pages in original document."""


class JobStatusResponse(BaseModel):
    """Response from job status endpoint."""

    model_config = ConfigDict(populate_by_name=True)

    job_id: str = Field(alias="jobId")
    """Unique job identifier."""

    status: JobStatus
    """Current job status."""

    progress: Optional[int] = None
    """Progress percentage (0-100)."""

    error: Optional[str] = None
    """Error message if job failed."""

    result: Optional[PdfSplitResult] = None
    """Result data when job is completed."""


class ExtractOptions(BaseModel):
    """Extract operation options."""

    model_config = ConfigDict(populate_by_name=True)

    schema_: Optional[Dict[str, Any]] = Field(default=None, alias="schema")
    """Schema defining what to extract."""

    prompt: Optional[str] = None
    """Prompt describing what to extract."""


class ExtractResult(BaseModel):
    """Result of extract operation."""

    data: Dict[str, Any]
    """Extracted data matching the schema."""

    confidence: float
    """Confidence score (0-1)."""


class Team(BaseModel):
    """Team information."""

    id: str
    """Team ID."""

    name: str
    """Team name."""


class User(BaseModel):
    """User profile information."""

    id: str
    """User ID."""

    email: str
    """Email address."""

    name: Optional[str] = None
    """Display name."""

    credits: Optional[int] = None
    """Available credits."""

    team: Optional[Team] = None
    """Team information (if applicable)."""


# MIME types for supported file formats
MIME_TYPES: Dict[str, str] = {
    ".pdf": "application/pdf",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".tiff": "image/tiff",
    ".tif": "image/tiff",
}
