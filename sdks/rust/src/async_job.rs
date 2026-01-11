//! Async job handling for long-running operations.
//!
//! This module provides the [`AsyncJob`] struct for polling and waiting on
//! asynchronous operations like PDF splitting.

use std::sync::Arc;
use std::time::{Duration, Instant};

use log::debug;

use crate::error::{RenamedError, Result};
use crate::models::{JobStatus, JobStatusResponse, PdfSplitResult};

/// Default polling interval for async jobs.
const DEFAULT_POLL_INTERVAL: Duration = Duration::from_secs(2);

/// Maximum number of poll attempts (5 minutes at 2s intervals).
const MAX_POLL_ATTEMPTS: u32 = 150;

/// A callback function that receives progress updates during job polling.
pub type ProgressCallback = Box<dyn Fn(&JobStatusResponse) + Send + Sync>;

/// Represents an asynchronous job that can be polled for completion.
///
/// Async jobs are returned by operations that may take a long time to complete,
/// such as PDF splitting. Use [`wait()`](AsyncJob::wait) to block until the job
/// finishes, or [`status()`](AsyncJob::status) to check progress manually.
///
/// # Example
///
/// ```rust,no_run
/// # async fn example() -> Result<(), renamed::RenamedError> {
/// # let client = renamed::RenamedClient::new("api_key");
/// let job = client.pdf_split("document.pdf", None).await?;
///
/// // Option 1: Wait for completion
/// let result = job.wait(None).await?;
///
/// // Option 2: Poll manually
/// loop {
///     let status = job.status().await?;
///     println!("Progress: {}%", status.progress.unwrap_or(0));
///     if status.status.is_finished() {
///         break;
///     }
///     tokio::time::sleep(std::time::Duration::from_secs(2)).await;
/// }
/// # Ok(())
/// # }
/// ```
pub struct AsyncJob {
    /// HTTP client for making requests.
    client: Arc<reqwest::Client>,

    /// API key for authentication.
    api_key: String,

    /// URL to poll for job status.
    status_url: String,

    /// Interval between poll attempts.
    poll_interval: Duration,

    /// Maximum number of poll attempts before timing out.
    max_attempts: u32,

    /// Whether debug logging is enabled.
    debug: bool,
}

impl AsyncJob {
    /// Creates a new async job.
    pub(crate) fn new(
        client: Arc<reqwest::Client>,
        api_key: String,
        status_url: String,
        debug: bool,
    ) -> Self {
        Self {
            client,
            api_key,
            status_url,
            poll_interval: DEFAULT_POLL_INTERVAL,
            max_attempts: MAX_POLL_ATTEMPTS,
            debug,
        }
    }

    /// Extracts the job ID from the status URL.
    fn extract_job_id(&self) -> &str {
        // Extract job ID from URL like "https://example.com/status/abc123"
        self.status_url.rsplit('/').next().unwrap_or("unknown")
    }

    /// Sets a custom polling interval.
    ///
    /// The default is 2 seconds.
    pub fn with_poll_interval(mut self, interval: Duration) -> Self {
        self.poll_interval = interval;
        self
    }

    /// Sets the maximum number of polling attempts.
    ///
    /// The default is 150 attempts (5 minutes at 2 second intervals).
    pub fn with_max_attempts(mut self, attempts: u32) -> Self {
        self.max_attempts = attempts;
        self
    }

    /// Returns the status URL for this job.
    pub fn status_url(&self) -> &str {
        &self.status_url
    }

    /// Fetches the current job status.
    ///
    /// # Errors
    ///
    /// Returns an error if the network request fails or the response cannot be parsed.
    pub async fn status(&self) -> Result<JobStatusResponse> {
        let start = Instant::now();

        let response = self
            .client
            .get(&self.status_url)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(RenamedError::from_reqwest)?;

        let status_code = response.status().as_u16();
        let elapsed_ms = start.elapsed().as_millis();
        let body = response.text().await.map_err(RenamedError::from_reqwest)?;

        if status_code >= 400 {
            return Err(RenamedError::from_http_status(status_code, Some(&body)));
        }

        let status_response: JobStatusResponse =
            serde_json::from_str(&body).map_err(RenamedError::from_serde)?;

        if self.debug {
            let progress_str = status_response
                .progress
                .map(|p| format!(" ({}%)", p))
                .unwrap_or_default();
            debug!(
                "[Renamed] Job {}: {}{} ({}ms)",
                self.extract_job_id(),
                status_response.status,
                progress_str,
                elapsed_ms
            );
        }

        Ok(status_response)
    }

    /// Waits for the job to complete, polling at regular intervals.
    ///
    /// Optionally accepts a progress callback that will be invoked after each
    /// status poll.
    ///
    /// # Arguments
    ///
    /// * `on_progress` - Optional callback invoked with status updates.
    ///
    /// # Returns
    ///
    /// Returns the [`PdfSplitResult`] when the job completes successfully.
    ///
    /// # Errors
    ///
    /// - Returns [`RenamedError::Job`] if the job fails or times out.
    /// - Returns network errors if polling fails.
    ///
    /// # Example
    ///
    /// ```rust,no_run
    /// # async fn example() -> Result<(), renamed::RenamedError> {
    /// # let client = renamed::RenamedClient::new("api_key");
    /// let job = client.pdf_split("document.pdf", None).await?;
    ///
    /// // With progress callback
    /// let result = job.wait(Some(Box::new(|status| {
    ///     println!("Progress: {}%", status.progress.unwrap_or(0));
    /// }))).await?;
    ///
    /// println!("Split into {} documents", result.documents.len());
    /// # Ok(())
    /// # }
    /// ```
    pub async fn wait(&self, on_progress: Option<ProgressCallback>) -> Result<PdfSplitResult> {
        for _attempt in 0..self.max_attempts {
            let status = self.status().await?;

            // Invoke progress callback if provided
            if let Some(ref callback) = on_progress {
                callback(&status);
            }

            // Check if job completed successfully
            if status.status == JobStatus::Completed {
                return status.result.ok_or_else(|| {
                    RenamedError::job_error(
                        "Job completed but no result returned",
                        Some(status.job_id),
                    )
                });
            }

            // Check if job failed
            if status.status == JobStatus::Failed {
                return Err(RenamedError::job_error(
                    status.error.unwrap_or_else(|| "Job failed".to_string()),
                    Some(status.job_id),
                ));
            }

            // Wait before next poll
            tokio::time::sleep(self.poll_interval).await;
        }

        Err(RenamedError::job_error(
            "Job polling timeout exceeded",
            None,
        ))
    }

    /// Waits for the job to complete without a progress callback.
    ///
    /// This is a convenience method equivalent to `wait(None)`.
    pub async fn wait_without_progress(&self) -> Result<PdfSplitResult> {
        self.wait(None).await
    }
}

impl std::fmt::Debug for AsyncJob {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("AsyncJob")
            .field("status_url", &self.status_url)
            .field("poll_interval", &self.poll_interval)
            .field("max_attempts", &self.max_attempts)
            .finish()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_async_job_builder() {
        let client = Arc::new(reqwest::Client::new());
        let job = AsyncJob::new(
            client,
            "test_key".to_string(),
            "https://example.com/status".to_string(),
            false,
        )
        .with_poll_interval(Duration::from_secs(5))
        .with_max_attempts(10);

        assert_eq!(job.poll_interval, Duration::from_secs(5));
        assert_eq!(job.max_attempts, 10);
        assert_eq!(job.status_url(), "https://example.com/status");
    }

    #[test]
    fn test_extract_job_id() {
        let client = Arc::new(reqwest::Client::new());
        let job = AsyncJob::new(
            client,
            "test_key".to_string(),
            "https://example.com/status/abc123".to_string(),
            false,
        );

        assert_eq!(job.extract_job_id(), "abc123");
    }
}
