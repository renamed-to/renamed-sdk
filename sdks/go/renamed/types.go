package renamed

// RenameResult is the result of a rename operation.
type RenameResult struct {
	// OriginalFilename is the original filename that was uploaded.
	OriginalFilename string `json:"originalFilename"`

	// SuggestedFilename is the AI-suggested new filename.
	SuggestedFilename string `json:"suggestedFilename"`

	// FolderPath is the suggested folder path for organization.
	FolderPath string `json:"folderPath,omitempty"`

	// Confidence is the confidence score (0-1) of the suggestion.
	Confidence float64 `json:"confidence"`
}

// RenameOptions are options for the rename operation.
type RenameOptions struct {
	// Template is a custom template for filename generation.
	Template string
}

// SplitMode is the mode for PDF splitting.
type SplitMode string

const (
	// SplitModeAuto uses AI to detect document boundaries.
	SplitModeAuto SplitMode = "auto"

	// SplitModePages splits every N pages.
	SplitModePages SplitMode = "pages"

	// SplitModeBlank splits at blank pages.
	SplitModeBlank SplitMode = "blank"
)

// PdfSplitOptions are options for PDF split operation.
type PdfSplitOptions struct {
	// Mode is the split mode: auto, pages, or blank.
	Mode SplitMode

	// PagesPerSplit is the number of pages per split (for pages mode).
	PagesPerSplit int
}

// JobStatus is the status of an async job.
type JobStatus string

const (
	JobStatusPending    JobStatus = "pending"
	JobStatusProcessing JobStatus = "processing"
	JobStatusCompleted  JobStatus = "completed"
	JobStatusFailed     JobStatus = "failed"
)

// SplitDocument is a single document from PDF split.
type SplitDocument struct {
	// Index is the document index (0-based).
	Index int `json:"index"`

	// Filename is the suggested filename for this document.
	Filename string `json:"filename"`

	// Pages is the page range included in this document.
	Pages string `json:"pages"`

	// DownloadURL is the URL to download this document.
	DownloadURL string `json:"downloadUrl"`

	// Size is the size in bytes.
	Size int64 `json:"size"`
}

// PdfSplitResult is the result of PDF split operation.
type PdfSplitResult struct {
	// OriginalFilename is the original filename.
	OriginalFilename string `json:"originalFilename"`

	// Documents are the split documents.
	Documents []SplitDocument `json:"documents"`

	// TotalPages is the total number of pages in the original document.
	TotalPages int `json:"totalPages"`
}

// JobStatusResponse is the response from job status endpoint.
type JobStatusResponse struct {
	// JobID is the unique job identifier.
	JobID string `json:"jobId"`

	// Status is the current job status.
	Status JobStatus `json:"status"`

	// Progress is the progress percentage (0-100).
	Progress int `json:"progress,omitempty"`

	// Error is the error message if job failed.
	Error string `json:"error,omitempty"`

	// Result is the result data when job is completed.
	Result *PdfSplitResult `json:"result,omitempty"`
}

// ExtractOptions are options for extract operation.
type ExtractOptions struct {
	// Schema is a JSON schema defining what to extract.
	Schema map[string]any

	// Prompt is a natural language description of what to extract.
	Prompt string
}

// ExtractResult is the result of extract operation.
type ExtractResult struct {
	// Data is the extracted data matching the schema.
	Data map[string]any `json:"data"`

	// Confidence is the confidence score (0-1).
	Confidence float64 `json:"confidence"`
}

// Team is team information.
type Team struct {
	// ID is the team ID.
	ID string `json:"id"`

	// Name is the team name.
	Name string `json:"name"`
}

// User is user profile information.
type User struct {
	// ID is the user ID.
	ID string `json:"id"`

	// Email is the email address.
	Email string `json:"email"`

	// Name is the display name.
	Name string `json:"name,omitempty"`

	// Credits is the available credits.
	Credits int `json:"credits,omitempty"`

	// Team is the team information (if applicable).
	Team *Team `json:"team,omitempty"`
}

// pdfSplitResponse is the initial response from pdf-split endpoint.
type pdfSplitResponse struct {
	StatusURL string `json:"statusUrl"`
}

// MIME types for supported file formats.
var mimeTypes = map[string]string{
	".pdf":  "application/pdf",
	".jpg":  "image/jpeg",
	".jpeg": "image/jpeg",
	".png":  "image/png",
	".tiff": "image/tiff",
	".tif":  "image/tiff",
}
