//! Data models for the renamed.to API.
//!
//! This module contains all request/response types used by the SDK.

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

// ============================================================================
// Rename Types
// ============================================================================

/// Result of a rename operation.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RenameResult {
    /// The original filename that was uploaded.
    pub original_filename: String,

    /// The AI-suggested new filename.
    pub suggested_filename: String,

    /// Suggested folder path for organization (if available).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub folder_path: Option<String>,

    /// Confidence score (0.0 - 1.0) of the suggestion.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub confidence: Option<f64>,
}

/// Options for the rename operation.
#[derive(Debug, Clone, Default)]
pub struct RenameOptions {
    /// Custom template for filename generation.
    pub template: Option<String>,
}

impl RenameOptions {
    /// Creates new rename options with default values.
    pub fn new() -> Self {
        Self::default()
    }

    /// Sets a custom template for filename generation.
    pub fn with_template(mut self, template: impl Into<String>) -> Self {
        self.template = Some(template.into());
        self
    }
}

// ============================================================================
// PDF Split Types
// ============================================================================

/// Mode for PDF splitting.
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum SplitMode {
    /// Use AI to automatically detect document boundaries.
    #[default]
    Auto,
    /// Split every N pages.
    Pages,
    /// Split at blank pages.
    Blank,
}

impl std::fmt::Display for SplitMode {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            SplitMode::Auto => write!(f, "auto"),
            SplitMode::Pages => write!(f, "pages"),
            SplitMode::Blank => write!(f, "blank"),
        }
    }
}

/// Options for PDF split operation.
#[derive(Debug, Clone, Default)]
pub struct PdfSplitOptions {
    /// The split mode to use.
    pub mode: Option<SplitMode>,

    /// Number of pages per split (for `Pages` mode).
    pub pages_per_split: Option<u32>,
}

impl PdfSplitOptions {
    /// Creates new PDF split options with default values.
    pub fn new() -> Self {
        Self::default()
    }

    /// Sets the split mode.
    pub fn with_mode(mut self, mode: SplitMode) -> Self {
        self.mode = Some(mode);
        self
    }

    /// Sets the number of pages per split (for `Pages` mode).
    pub fn with_pages_per_split(mut self, pages: u32) -> Self {
        self.pages_per_split = Some(pages);
        self
    }
}

/// A single document from a PDF split operation.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SplitDocument {
    /// Document index (0-based).
    pub index: u32,

    /// Suggested filename for this document.
    pub filename: String,

    /// Page range included in this document (e.g., "1-3").
    pub pages: String,

    /// URL to download this document.
    pub download_url: String,

    /// Size in bytes.
    pub size: i64,
}

/// Result of a PDF split operation.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PdfSplitResult {
    /// The original filename.
    pub original_filename: String,

    /// The split documents.
    pub documents: Vec<SplitDocument>,

    /// Total number of pages in the original document.
    pub total_pages: u32,
}

// ============================================================================
// Job Status Types
// ============================================================================

/// Status of an async job.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum JobStatus {
    /// Job is queued and waiting to be processed.
    Pending,
    /// Job is currently being processed.
    Processing,
    /// Job completed successfully.
    Completed,
    /// Job failed.
    Failed,
}

impl JobStatus {
    /// Returns true if the job is still in progress.
    pub fn is_in_progress(&self) -> bool {
        matches!(self, JobStatus::Pending | JobStatus::Processing)
    }

    /// Returns true if the job has finished (completed or failed).
    pub fn is_finished(&self) -> bool {
        matches!(self, JobStatus::Completed | JobStatus::Failed)
    }
}

impl std::fmt::Display for JobStatus {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            JobStatus::Pending => write!(f, "pending"),
            JobStatus::Processing => write!(f, "processing"),
            JobStatus::Completed => write!(f, "completed"),
            JobStatus::Failed => write!(f, "failed"),
        }
    }
}

/// Response from the job status endpoint.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JobStatusResponse {
    /// Unique job identifier.
    pub job_id: String,

    /// Current job status.
    pub status: JobStatus,

    /// Progress percentage (0-100).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub progress: Option<u8>,

    /// Error message if job failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,

    /// Result data when job is completed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<PdfSplitResult>,
}

/// Initial response from PDF split endpoint containing the status URL.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PdfSplitResponse {
    pub status_url: String,
}

// ============================================================================
// Extract Types
// ============================================================================

/// Options for the extract operation.
#[derive(Debug, Clone, Default)]
pub struct ExtractOptions {
    /// JSON schema defining what to extract.
    pub schema: Option<HashMap<String, serde_json::Value>>,

    /// Natural language description of what to extract.
    pub prompt: Option<String>,
}

impl ExtractOptions {
    /// Creates new extract options with default values.
    pub fn new() -> Self {
        Self::default()
    }

    /// Sets a JSON schema defining what to extract.
    pub fn with_schema(mut self, schema: HashMap<String, serde_json::Value>) -> Self {
        self.schema = Some(schema);
        self
    }

    /// Sets a natural language prompt describing what to extract.
    pub fn with_prompt(mut self, prompt: impl Into<String>) -> Self {
        self.prompt = Some(prompt.into());
        self
    }
}

/// Result of an extract operation.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtractResult {
    /// The extracted data matching the schema.
    pub data: HashMap<String, serde_json::Value>,

    /// Confidence score (0.0 - 1.0).
    pub confidence: f64,
}

// ============================================================================
// User Types
// ============================================================================

/// Team information.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Team {
    /// Team ID.
    pub id: String,

    /// Team name.
    pub name: String,
}

/// User profile information.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    /// User ID.
    pub id: String,

    /// Email address.
    pub email: String,

    /// Display name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,

    /// Available credits.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub credits: Option<i32>,

    /// Team information (if applicable).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub team: Option<Team>,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_rename_result_deserialization() {
        let json = r#"{
            "originalFilename": "document.pdf",
            "suggestedFilename": "Invoice_2024_001.pdf",
            "folderPath": "Invoices/2024",
            "confidence": 0.95
        }"#;

        let result: RenameResult = serde_json::from_str(json).unwrap();
        assert_eq!(result.original_filename, "document.pdf");
        assert_eq!(result.suggested_filename, "Invoice_2024_001.pdf");
        assert_eq!(result.folder_path, Some("Invoices/2024".to_string()));
        assert_eq!(result.confidence, Some(0.95));
    }

    #[test]
    fn test_job_status_is_in_progress() {
        assert!(JobStatus::Pending.is_in_progress());
        assert!(JobStatus::Processing.is_in_progress());
        assert!(!JobStatus::Completed.is_in_progress());
        assert!(!JobStatus::Failed.is_in_progress());
    }

    #[test]
    fn test_split_mode_display() {
        assert_eq!(SplitMode::Auto.to_string(), "auto");
        assert_eq!(SplitMode::Pages.to_string(), "pages");
        assert_eq!(SplitMode::Blank.to_string(), "blank");
    }
}
