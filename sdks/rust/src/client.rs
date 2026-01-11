//! The main API client for renamed.to.
//!
//! This module provides [`RenamedClient`], the primary interface for interacting
//! with the renamed.to API.
//!
//! # Debug Logging
//!
//! Enable debug logging to see HTTP requests, retries, and job polling:
//!
//! ```rust,no_run
//! use renamed::RenamedClient;
//!
//! let client = RenamedClient::builder("rt_your_api_key")
//!     .with_debug(true)
//!     .build();
//! ```
//!
//! To see log output, add a logger like `env_logger` to your project:
//!
//! ```toml
//! [dependencies]
//! env_logger = "0.11"
//! ```
//!
//! Then initialize it in your main function and set `RUST_LOG=renamed=debug`.

use std::path::Path;
use std::sync::Arc;
use std::time::{Duration, Instant};

use log::{debug, info, warn};
use reqwest::multipart::{Form, Part};

use crate::async_job::AsyncJob;
use crate::error::{RenamedError, Result};
use crate::models::{
    ExtractOptions, ExtractResult, PdfSplitOptions, PdfSplitResponse, RenameOptions, RenameResult,
    User,
};

/// Default base URL for the renamed.to API.
const DEFAULT_BASE_URL: &str = "https://www.renamed.to/api/v1";

/// Default request timeout.
const DEFAULT_TIMEOUT: Duration = Duration::from_secs(30);

/// Default maximum number of retries for failed requests.
const DEFAULT_MAX_RETRIES: u32 = 2;

/// Builder for configuring a [`RenamedClient`].
#[derive(Debug, Clone)]
pub struct RenamedClientBuilder {
    api_key: String,
    base_url: String,
    timeout: Duration,
    max_retries: u32,
    debug: bool,
}

impl RenamedClientBuilder {
    /// Creates a new builder with the given API key.
    pub fn new(api_key: impl Into<String>) -> Self {
        Self {
            api_key: api_key.into(),
            base_url: DEFAULT_BASE_URL.to_string(),
            timeout: DEFAULT_TIMEOUT,
            max_retries: DEFAULT_MAX_RETRIES,
            debug: false,
        }
    }

    /// Sets a custom base URL.
    ///
    /// Useful for testing or using a proxy.
    pub fn base_url(mut self, url: impl Into<String>) -> Self {
        self.base_url = url.into().trim_end_matches('/').to_string();
        self
    }

    /// Sets the request timeout.
    ///
    /// Default is 30 seconds.
    pub fn timeout(mut self, timeout: Duration) -> Self {
        self.timeout = timeout;
        self
    }

    /// Sets the maximum number of retries for failed requests.
    ///
    /// Default is 2 retries.
    pub fn max_retries(mut self, retries: u32) -> Self {
        self.max_retries = retries;
        self
    }

    /// Enables or disables debug logging.
    ///
    /// When enabled, the client logs HTTP requests, responses, retries, and job polling
    /// using the `log` crate. To see output, configure a logger like `env_logger`.
    ///
    /// # Example
    ///
    /// ```rust,no_run
    /// use renamed::RenamedClient;
    ///
    /// let client = RenamedClient::builder("rt_your_api_key")
    ///     .with_debug(true)
    ///     .build();
    /// ```
    pub fn with_debug(mut self, enabled: bool) -> Self {
        self.debug = enabled;
        self
    }

    /// Builds the [`RenamedClient`].
    pub fn build(self) -> RenamedClient {
        let client = reqwest::Client::builder()
            .timeout(self.timeout)
            .build()
            .expect("Failed to build HTTP client");

        let renamed_client = RenamedClient {
            api_key: self.api_key,
            base_url: self.base_url,
            max_retries: self.max_retries,
            debug: self.debug,
            client: Arc::new(client),
        };

        if self.debug {
            info!(
                "[Renamed] Client initialized (api_key: {}, base_url: {})",
                renamed_client.mask_api_key(),
                renamed_client.base_url
            );
        }

        renamed_client
    }
}

/// The main client for interacting with the renamed.to API.
///
/// # Example
///
/// ```rust,no_run
/// use renamed::RenamedClient;
///
/// # async fn example() -> Result<(), renamed::RenamedError> {
/// let client = RenamedClient::new("rt_your_api_key");
///
/// // Get user info
/// let user = client.get_user().await?;
/// println!("Credits: {}", user.credits.unwrap_or(0));
///
/// // Rename a file
/// let result = client.rename("invoice.pdf", None).await?;
/// println!("Suggested: {}", result.suggested_filename);
/// # Ok(())
/// # }
/// ```
#[derive(Debug, Clone)]
pub struct RenamedClient {
    api_key: String,
    base_url: String,
    max_retries: u32,
    debug: bool,
    client: Arc<reqwest::Client>,
}

impl RenamedClient {
    /// Creates a new client with the given API key using default settings.
    ///
    /// For custom configuration, use [`RenamedClient::builder()`] instead.
    pub fn new(api_key: impl Into<String>) -> Self {
        RenamedClientBuilder::new(api_key).build()
    }

    /// Creates a builder for configuring the client.
    ///
    /// # Example
    ///
    /// ```rust,no_run
    /// use renamed::RenamedClient;
    /// use std::time::Duration;
    ///
    /// let client = RenamedClient::builder("rt_your_api_key")
    ///     .timeout(Duration::from_secs(60))
    ///     .max_retries(3)
    ///     .build();
    /// ```
    pub fn builder(api_key: impl Into<String>) -> RenamedClientBuilder {
        RenamedClientBuilder::new(api_key)
    }

    /// Builds the full URL for an API endpoint.
    fn build_url(&self, path: &str) -> String {
        if path.starts_with("http://") || path.starts_with("https://") {
            return path.to_string();
        }
        let path = path.trim_start_matches('/');
        format!("{}/{}", self.base_url, path)
    }

    /// Masks the API key for safe logging.
    ///
    /// Returns format like `rt_...xxxx` (first 3 chars + last 4).
    fn mask_api_key(&self) -> String {
        let key = &self.api_key;
        if key.len() <= 7 {
            return "***".to_string();
        }
        let prefix = &key[..3];
        let suffix = &key[key.len() - 4..];
        format!("{}...{}", prefix, suffix)
    }

    /// Formats a file size in human-readable format.
    fn format_size(bytes: usize) -> String {
        const KB: usize = 1024;
        const MB: usize = KB * 1024;
        const GB: usize = MB * 1024;

        if bytes >= GB {
            format!("{:.1} GB", bytes as f64 / GB as f64)
        } else if bytes >= MB {
            format!("{:.1} MB", bytes as f64 / MB as f64)
        } else if bytes >= KB {
            format!("{:.1} KB", bytes as f64 / KB as f64)
        } else {
            format!("{} B", bytes)
        }
    }

    /// Extracts the path from a URL for logging.
    fn extract_path(url: &str) -> &str {
        // For full URLs, extract the path portion
        if let Some(idx) = url.find("://") {
            let after_scheme = &url[idx + 3..];
            if let Some(path_idx) = after_scheme.find('/') {
                return &after_scheme[path_idx..];
            }
        }
        // For relative paths, return as-is
        url
    }

    /// Returns whether debug logging is enabled.
    pub fn is_debug_enabled(&self) -> bool {
        self.debug
    }

    /// Makes an HTTP request with retry logic.
    async fn request(
        &self,
        method: reqwest::Method,
        path: &str,
    ) -> Result<reqwest::RequestBuilder> {
        let url = self.build_url(path);
        Ok(self
            .client
            .request(method, url)
            .header("Authorization", format!("Bearer {}", self.api_key)))
    }

    /// Executes a request with retry logic and returns the response body.
    async fn execute_request(
        &self,
        request: reqwest::RequestBuilder,
        method: &str,
        path: &str,
    ) -> Result<String> {
        let mut last_error = None;
        let start = Instant::now();

        for attempt in 0..=self.max_retries {
            let req = request.try_clone().ok_or_else(|| RenamedError::Network {
                message: "Failed to clone request for retry".to_string(),
                source: None,
            })?;

            // Log retry attempts (not the first attempt)
            if attempt > 0 && self.debug {
                let delay_ms = 100 * (1 << (attempt - 1));
                warn!(
                    "[Renamed] Retry attempt {}/{}, waiting {}ms",
                    attempt, self.max_retries, delay_ms
                );
            }

            match req.send().await {
                Ok(response) => {
                    let status_code = response.status().as_u16();
                    let elapsed_ms = start.elapsed().as_millis();
                    let body = response.text().await.map_err(RenamedError::from_reqwest)?;

                    if self.debug {
                        debug!(
                            "[Renamed] {} {} -> {} ({}ms)",
                            method,
                            Self::extract_path(path),
                            status_code,
                            elapsed_ms
                        );
                    }

                    if status_code >= 400 {
                        return Err(RenamedError::from_http_status(status_code, Some(&body)));
                    }

                    return Ok(body);
                }
                Err(err) => {
                    last_error = Some(RenamedError::from_reqwest(err));
                    if attempt < self.max_retries {
                        // Exponential backoff: 100ms, 200ms, 400ms, ...
                        let delay = Duration::from_millis(100 * (1 << attempt));
                        tokio::time::sleep(delay).await;
                    }
                }
            }
        }

        Err(last_error.unwrap_or_else(|| RenamedError::Network {
            message: "Request failed after retries".to_string(),
            source: None,
        }))
    }

    /// Creates a multipart form with a file.
    ///
    /// Returns the form and file metadata (filename, size) for logging.
    async fn create_file_form(
        &self,
        file_path: impl AsRef<Path>,
        fields: Vec<(&str, String)>,
    ) -> Result<(Form, String, usize)> {
        let path = file_path.as_ref();
        let filename = path
            .file_name()
            .and_then(|n| n.to_str())
            .unwrap_or("file")
            .to_string();

        let content = tokio::fs::read(path).await.map_err(|e| {
            RenamedError::from_io(e, format!("Failed to read file: {}", path.display()))
        })?;
        let file_size = content.len();

        let mime_type = mime_guess::from_path(path)
            .first_or_octet_stream()
            .to_string();

        let file_part = Part::bytes(content)
            .file_name(filename.clone())
            .mime_str(&mime_type)
            .map_err(|e| RenamedError::Network {
                message: format!("Invalid MIME type: {}", e),
                source: None,
            })?;

        let mut form = Form::new().part("file", file_part);

        for (key, value) in fields {
            form = form.text(key.to_string(), value);
        }

        Ok((form, filename, file_size))
    }

    /// Creates a multipart form from bytes.
    ///
    /// Returns the form and file size for logging.
    fn create_bytes_form(
        &self,
        content: Vec<u8>,
        filename: &str,
        fields: Vec<(&str, String)>,
    ) -> Result<(Form, usize)> {
        let file_size = content.len();
        let mime_type = mime_guess::from_path(filename)
            .first_or_octet_stream()
            .to_string();

        let file_part = Part::bytes(content)
            .file_name(filename.to_string())
            .mime_str(&mime_type)
            .map_err(|e| RenamedError::Network {
                message: format!("Invalid MIME type: {}", e),
                source: None,
            })?;

        let mut form = Form::new().part("file", file_part);

        for (key, value) in fields {
            form = form.text(key.to_string(), value);
        }

        Ok((form, file_size))
    }

    /// Uploads a file and returns the response body.
    async fn upload_file(
        &self,
        path: &str,
        file_path: impl AsRef<Path>,
        fields: Vec<(&str, String)>,
    ) -> Result<String> {
        let (form, filename, file_size) = self.create_file_form(file_path, fields).await?;

        if self.debug {
            debug!(
                "[Renamed] Upload: {} ({})",
                filename,
                Self::format_size(file_size)
            );
        }

        let url = self.build_url(path);
        let request = self
            .request(reqwest::Method::POST, path)
            .await?
            .multipart(form);
        self.execute_request(request, "POST", &url).await
    }

    /// Uploads bytes and returns the response body.
    async fn upload_bytes(
        &self,
        path: &str,
        content: Vec<u8>,
        filename: &str,
        fields: Vec<(&str, String)>,
    ) -> Result<String> {
        let (form, file_size) = self.create_bytes_form(content, filename, fields)?;

        if self.debug {
            debug!(
                "[Renamed] Upload: {} ({})",
                filename,
                Self::format_size(file_size)
            );
        }

        let url = self.build_url(path);
        let request = self
            .request(reqwest::Method::POST, path)
            .await?
            .multipart(form);
        self.execute_request(request, "POST", &url).await
    }

    // ========================================================================
    // Public API Methods
    // ========================================================================

    /// Gets the current user's profile and credits.
    ///
    /// # Example
    ///
    /// ```rust,no_run
    /// # async fn example() -> Result<(), renamed::RenamedError> {
    /// # let client = renamed::RenamedClient::new("api_key");
    /// let user = client.get_user().await?;
    /// println!("Email: {}", user.email);
    /// println!("Credits: {}", user.credits.unwrap_or(0));
    /// # Ok(())
    /// # }
    /// ```
    pub async fn get_user(&self) -> Result<User> {
        let path = "/user";
        let url = self.build_url(path);
        let request = self.request(reqwest::Method::GET, path).await?;
        let body = self.execute_request(request, "GET", &url).await?;
        serde_json::from_str(&body).map_err(RenamedError::from_serde)
    }

    /// Renames a file using AI.
    ///
    /// Analyzes the file content and suggests an appropriate filename.
    ///
    /// # Arguments
    ///
    /// * `file` - Path to the file to rename.
    /// * `options` - Optional configuration for the rename operation.
    ///
    /// # Example
    ///
    /// ```rust,no_run
    /// use renamed::{RenamedClient, RenameOptions};
    ///
    /// # async fn example() -> Result<(), renamed::RenamedError> {
    /// let client = RenamedClient::new("rt_your_api_key");
    ///
    /// // Basic usage
    /// let result = client.rename("document.pdf", None).await?;
    /// println!("Suggested: {}", result.suggested_filename);
    ///
    /// // With custom template
    /// let options = RenameOptions::new().with_template("{date}_{type}_{vendor}");
    /// let result = client.rename("invoice.pdf", Some(options)).await?;
    /// # Ok(())
    /// # }
    /// ```
    pub async fn rename(
        &self,
        file: impl AsRef<Path>,
        options: Option<RenameOptions>,
    ) -> Result<RenameResult> {
        let mut fields = Vec::new();

        if let Some(opts) = options {
            if let Some(template) = opts.template {
                fields.push(("template", template));
            }
        }

        let body = self.upload_file("/rename", file, fields).await?;
        serde_json::from_str(&body).map_err(RenamedError::from_serde)
    }

    /// Renames a file from bytes.
    ///
    /// Same as [`rename()`](Self::rename) but accepts raw bytes instead of a file path.
    ///
    /// # Arguments
    ///
    /// * `content` - The file content as bytes.
    /// * `filename` - The filename to use (for MIME type detection).
    /// * `options` - Optional configuration for the rename operation.
    pub async fn rename_bytes(
        &self,
        content: Vec<u8>,
        filename: &str,
        options: Option<RenameOptions>,
    ) -> Result<RenameResult> {
        let mut fields = Vec::new();

        if let Some(opts) = options {
            if let Some(template) = opts.template {
                fields.push(("template", template));
            }
        }

        let body = self
            .upload_bytes("/rename", content, filename, fields)
            .await?;
        serde_json::from_str(&body).map_err(RenamedError::from_serde)
    }

    /// Splits a PDF into multiple documents.
    ///
    /// Returns an [`AsyncJob`] that can be polled for completion. PDF splitting
    /// is an asynchronous operation that may take some time for large documents.
    ///
    /// # Arguments
    ///
    /// * `file` - Path to the PDF file to split.
    /// * `options` - Optional configuration for the split operation.
    ///
    /// # Example
    ///
    /// ```rust,no_run
    /// use renamed::{RenamedClient, PdfSplitOptions, SplitMode};
    ///
    /// # async fn example() -> Result<(), renamed::RenamedError> {
    /// let client = RenamedClient::new("rt_your_api_key");
    ///
    /// // Auto-detect document boundaries
    /// let job = client.pdf_split("multi-page.pdf", None).await?;
    /// let result = job.wait(None).await?;
    ///
    /// for doc in result.documents {
    ///     println!("{}: pages {}", doc.filename, doc.pages);
    /// }
    ///
    /// // Split every 5 pages
    /// let options = PdfSplitOptions::new()
    ///     .with_mode(SplitMode::Pages)
    ///     .with_pages_per_split(5);
    /// let job = client.pdf_split("large.pdf", Some(options)).await?;
    /// # Ok(())
    /// # }
    /// ```
    pub async fn pdf_split(
        &self,
        file: impl AsRef<Path>,
        options: Option<PdfSplitOptions>,
    ) -> Result<AsyncJob> {
        let mut fields = Vec::new();

        if let Some(opts) = options {
            if let Some(mode) = opts.mode {
                fields.push(("mode", mode.to_string()));
            }
            if let Some(pages) = opts.pages_per_split {
                fields.push(("pagesPerSplit", pages.to_string()));
            }
        }

        let body = self.upload_file("/pdf-split", file, fields).await?;
        let response: PdfSplitResponse =
            serde_json::from_str(&body).map_err(RenamedError::from_serde)?;

        Ok(AsyncJob::new(
            Arc::clone(&self.client),
            self.api_key.clone(),
            response.status_url,
            self.debug,
        ))
    }

    /// Splits a PDF from bytes.
    ///
    /// Same as [`pdf_split()`](Self::pdf_split) but accepts raw bytes.
    pub async fn pdf_split_bytes(
        &self,
        content: Vec<u8>,
        filename: &str,
        options: Option<PdfSplitOptions>,
    ) -> Result<AsyncJob> {
        let mut fields = Vec::new();

        if let Some(opts) = options {
            if let Some(mode) = opts.mode {
                fields.push(("mode", mode.to_string()));
            }
            if let Some(pages) = opts.pages_per_split {
                fields.push(("pagesPerSplit", pages.to_string()));
            }
        }

        let body = self
            .upload_bytes("/pdf-split", content, filename, fields)
            .await?;
        let response: PdfSplitResponse =
            serde_json::from_str(&body).map_err(RenamedError::from_serde)?;

        Ok(AsyncJob::new(
            Arc::clone(&self.client),
            self.api_key.clone(),
            response.status_url,
            self.debug,
        ))
    }

    /// Extracts structured data from a document.
    ///
    /// Uses AI to extract data matching a schema or natural language prompt.
    ///
    /// # Arguments
    ///
    /// * `file` - Path to the document to extract data from.
    /// * `options` - Configuration specifying what to extract.
    ///
    /// # Example
    ///
    /// ```rust,no_run
    /// use renamed::{RenamedClient, ExtractOptions};
    ///
    /// # async fn example() -> Result<(), renamed::RenamedError> {
    /// let client = RenamedClient::new("rt_your_api_key");
    ///
    /// // Using natural language prompt
    /// let options = ExtractOptions::new()
    ///     .with_prompt("Extract invoice number, date, and total amount");
    /// let result = client.extract("invoice.pdf", Some(options)).await?;
    ///
    /// println!("Extracted data: {:?}", result.data);
    /// println!("Confidence: {:.0}%", result.confidence * 100.0);
    /// # Ok(())
    /// # }
    /// ```
    pub async fn extract(
        &self,
        file: impl AsRef<Path>,
        options: Option<ExtractOptions>,
    ) -> Result<ExtractResult> {
        let mut fields = Vec::new();

        if let Some(opts) = options {
            if let Some(prompt) = opts.prompt {
                fields.push(("prompt", prompt));
            }
            if let Some(schema) = opts.schema {
                let schema_json =
                    serde_json::to_string(&schema).map_err(RenamedError::from_serde)?;
                fields.push(("schema", schema_json));
            }
        }

        let body = self.upload_file("/extract", file, fields).await?;
        serde_json::from_str(&body).map_err(RenamedError::from_serde)
    }

    /// Extracts data from bytes.
    ///
    /// Same as [`extract()`](Self::extract) but accepts raw bytes.
    pub async fn extract_bytes(
        &self,
        content: Vec<u8>,
        filename: &str,
        options: Option<ExtractOptions>,
    ) -> Result<ExtractResult> {
        let mut fields = Vec::new();

        if let Some(opts) = options {
            if let Some(prompt) = opts.prompt {
                fields.push(("prompt", prompt));
            }
            if let Some(schema) = opts.schema {
                let schema_json =
                    serde_json::to_string(&schema).map_err(RenamedError::from_serde)?;
                fields.push(("schema", schema_json));
            }
        }

        let body = self
            .upload_bytes("/extract", content, filename, fields)
            .await?;
        serde_json::from_str(&body).map_err(RenamedError::from_serde)
    }

    /// Downloads a file from a URL (e.g., a split document).
    ///
    /// # Arguments
    ///
    /// * `url` - The URL to download from.
    ///
    /// # Example
    ///
    /// ```rust,no_run
    /// # async fn example() -> Result<(), Box<dyn std::error::Error>> {
    /// # let client = renamed::RenamedClient::new("api_key");
    /// let job = client.pdf_split("document.pdf", None).await?;
    /// let result = job.wait(None).await?;
    ///
    /// for doc in result.documents {
    ///     let content = client.download_file(&doc.download_url).await?;
    ///     tokio::fs::write(&doc.filename, content).await?;
    /// }
    /// # Ok(())
    /// # }
    /// ```
    pub async fn download_file(&self, url: &str) -> Result<Vec<u8>> {
        let start = Instant::now();

        let response = self
            .client
            .get(url)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(RenamedError::from_reqwest)?;

        let status_code = response.status().as_u16();
        let elapsed_ms = start.elapsed().as_millis();

        if self.debug {
            debug!(
                "[Renamed] GET {} -> {} ({}ms)",
                Self::extract_path(url),
                status_code,
                elapsed_ms
            );
        }

        if status_code >= 400 {
            let body = response.text().await.map_err(RenamedError::from_reqwest)?;
            return Err(RenamedError::from_http_status(status_code, Some(&body)));
        }

        response
            .bytes()
            .await
            .map(|b| b.to_vec())
            .map_err(RenamedError::from_reqwest)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_build_url() {
        let client = RenamedClient::new("test_key");

        assert_eq!(
            client.build_url("/rename"),
            "https://www.renamed.to/api/v1/rename"
        );
        assert_eq!(
            client.build_url("rename"),
            "https://www.renamed.to/api/v1/rename"
        );
        assert_eq!(
            client.build_url("https://example.com/status"),
            "https://example.com/status"
        );
    }

    #[test]
    fn test_builder() {
        let client = RenamedClient::builder("test_key")
            .base_url("https://custom.api.com/")
            .timeout(Duration::from_secs(60))
            .max_retries(5)
            .build();

        assert_eq!(client.base_url, "https://custom.api.com");
        assert_eq!(client.max_retries, 5);
        assert!(!client.debug);
    }

    #[test]
    fn test_builder_with_debug() {
        let client = RenamedClient::builder("test_key").with_debug(true).build();

        assert!(client.debug);
        assert!(client.is_debug_enabled());
    }

    #[test]
    fn test_mask_api_key() {
        // Standard API key
        let client = RenamedClient::new("rt_1234567890abcdef");
        assert_eq!(client.mask_api_key(), "rt_...cdef");

        // Short API key (edge case)
        let client_short = RenamedClient::new("short");
        assert_eq!(client_short.mask_api_key(), "***");

        // Exactly 8 characters
        let client_8 = RenamedClient::new("12345678");
        assert_eq!(client_8.mask_api_key(), "123...5678");
    }

    #[test]
    fn test_format_size() {
        assert_eq!(RenamedClient::format_size(0), "0 B");
        assert_eq!(RenamedClient::format_size(512), "512 B");
        assert_eq!(RenamedClient::format_size(1024), "1.0 KB");
        assert_eq!(RenamedClient::format_size(1536), "1.5 KB");
        assert_eq!(RenamedClient::format_size(1048576), "1.0 MB");
        assert_eq!(RenamedClient::format_size(1572864), "1.5 MB");
        assert_eq!(RenamedClient::format_size(1073741824), "1.0 GB");
    }

    #[test]
    fn test_extract_path() {
        assert_eq!(
            RenamedClient::extract_path("https://api.example.com/v1/rename"),
            "/v1/rename"
        );
        assert_eq!(
            RenamedClient::extract_path("http://localhost:3000/user"),
            "/user"
        );
        assert_eq!(RenamedClient::extract_path("/rename"), "/rename");
        assert_eq!(RenamedClient::extract_path("rename"), "rename");
    }
}
