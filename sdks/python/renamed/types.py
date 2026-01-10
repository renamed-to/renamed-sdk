"""Type definitions for renamed.to SDK."""

from typing import Literal

from pydantic import BaseModel, Field


class RenameResult(BaseModel):
    """Result of a rename operation."""

    original_filename: str = Field(alias="originalFilename")
    """Original filename that was uploaded."""

    suggested_filename: str = Field(alias="suggestedFilename")
    """AI-suggested new filename."""

    folder_path: str | None = Field(default=None, alias="folderPath")
    """Suggested folder path for organization."""

    confidence: float
    """Confidence score (0-1) of the suggestion."""

    class Config:
        populate_by_name = True


class RenameOptions(BaseModel):
    """Options for rename operation."""

    template: str | None = None
    """Custom template for filename generation."""


class PdfSplitOptions(BaseModel):
    """Options for PDF split operation."""

    mode: Literal["auto", "pages", "blank"] | None = None
    """Split mode: 'auto' (AI-detected), 'pages' (every N pages), 'blank' (at blank pages)."""

    pages_per_split: int | None = Field(default=None, alias="pagesPerSplit")
    """Number of pages per split (for 'pages' mode)."""

    class Config:
        populate_by_name = True


JobStatus = Literal["pending", "processing", "completed", "failed"]
"""Status of an async job."""


class SplitDocument(BaseModel):
    """A single document from PDF split."""

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

    class Config:
        populate_by_name = True


class PdfSplitResult(BaseModel):
    """Result of PDF split operation."""

    original_filename: str = Field(alias="originalFilename")
    """Original filename."""

    documents: list[SplitDocument]
    """Split documents."""

    total_pages: int = Field(alias="totalPages")
    """Total number of pages in original document."""

    class Config:
        populate_by_name = True


class JobStatusResponse(BaseModel):
    """Response from job status endpoint."""

    job_id: str = Field(alias="jobId")
    """Unique job identifier."""

    status: JobStatus
    """Current job status."""

    progress: int | None = None
    """Progress percentage (0-100)."""

    error: str | None = None
    """Error message if job failed."""

    result: PdfSplitResult | None = None
    """Result data when job is completed."""

    class Config:
        populate_by_name = True


class ExtractOptions(BaseModel):
    """Extract operation options."""

    schema_: dict | None = Field(default=None, alias="schema")
    """Schema defining what to extract."""

    prompt: str | None = None
    """Prompt describing what to extract."""

    class Config:
        populate_by_name = True


class ExtractResult(BaseModel):
    """Result of extract operation."""

    data: dict
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

    name: str | None = None
    """Display name."""

    credits: int
    """Available credits."""

    team: Team | None = None
    """Team information (if applicable)."""


# MIME types for supported file formats
MIME_TYPES: dict[str, str] = {
    ".pdf": "application/pdf",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".tiff": "image/tiff",
    ".tif": "image/tiff",
}
