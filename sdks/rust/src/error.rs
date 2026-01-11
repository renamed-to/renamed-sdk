//! Error types for the renamed.to SDK.
//!
//! This module provides a comprehensive error enum that covers all failure modes
//! when interacting with the renamed.to API.

use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use thiserror::Error;

/// The main error type for the renamed.to SDK.
///
/// All SDK methods return `Result<T, RenamedError>`, allowing callers to handle
/// specific error cases appropriately.
#[derive(Error, Debug)]
pub enum RenamedError {
    /// Invalid or missing API key.
    #[error("Authentication error: {message}")]
    Authentication {
        /// Error message describing the authentication failure.
        message: String,
        /// HTTP status code (typically 401).
        status_code: u16,
    },

    /// Not enough credits to complete the operation.
    #[error("Insufficient credits: {message}")]
    InsufficientCredits {
        /// Error message describing the credit issue.
        message: String,
        /// HTTP status code (typically 402).
        status_code: u16,
    },

    /// Rate limit exceeded. Wait before retrying.
    #[error("Rate limit exceeded: {message}")]
    RateLimit {
        /// Error message describing the rate limit.
        message: String,
        /// HTTP status code (typically 429).
        status_code: u16,
        /// Seconds to wait before retrying (if provided by the API).
        retry_after: Option<u32>,
    },

    /// Invalid request parameters or payload.
    #[error("Validation error: {message}")]
    Validation {
        /// Error message describing the validation failure.
        message: String,
        /// HTTP status code (typically 400 or 422).
        status_code: u16,
        /// Additional details about the validation failure.
        details: Option<HashMap<String, serde_json::Value>>,
    },

    /// Network or connection failure.
    #[error("Network error: {message}")]
    Network {
        /// Error message describing the network failure.
        message: String,
        /// The underlying reqwest error, if available.
        #[source]
        source: Option<reqwest::Error>,
    },

    /// Request timed out.
    #[error("Timeout error: {message}")]
    Timeout {
        /// Error message describing the timeout.
        message: String,
    },

    /// Async job failed during processing.
    #[error("Job error: {message}")]
    Job {
        /// Error message describing the job failure.
        message: String,
        /// The job ID if available.
        job_id: Option<String>,
    },

    /// Generic API error for unexpected status codes.
    #[error("API error ({status_code}): {message}")]
    Api {
        /// Error message from the API.
        message: String,
        /// HTTP status code.
        status_code: u16,
        /// Error code from the API.
        code: String,
        /// Additional error details.
        details: Option<HashMap<String, serde_json::Value>>,
    },

    /// File I/O error.
    #[error("File error: {message}")]
    File {
        /// Error message describing the file operation failure.
        message: String,
        /// The underlying I/O error, if available.
        #[source]
        source: Option<std::io::Error>,
    },

    /// JSON serialization/deserialization error.
    #[error("Serialization error: {message}")]
    Serialization {
        /// Error message describing the serialization failure.
        message: String,
        /// The underlying serde_json error, if available.
        #[source]
        source: Option<serde_json::Error>,
    },
}

/// API error response structure for deserializing error payloads.
#[derive(Debug, Deserialize, Serialize)]
pub(crate) struct ApiErrorResponse {
    pub error: Option<String>,
    #[serde(rename = "retryAfter")]
    pub retry_after: Option<u32>,
    #[serde(flatten)]
    pub extra: HashMap<String, serde_json::Value>,
}

impl RenamedError {
    /// Creates an appropriate error variant from an HTTP status code and response body.
    pub(crate) fn from_http_status(status: u16, body: Option<&str>) -> Self {
        let error_response: Option<ApiErrorResponse> =
            body.and_then(|b| serde_json::from_str(b).ok());

        let message = error_response
            .as_ref()
            .and_then(|r| r.error.clone())
            .unwrap_or_else(|| format!("HTTP {}", status));

        let details = error_response
            .as_ref()
            .map(|r| r.extra.clone())
            .filter(|d| !d.is_empty());

        match status {
            401 => RenamedError::Authentication {
                message,
                status_code: status,
            },
            402 => RenamedError::InsufficientCredits {
                message,
                status_code: status,
            },
            400 | 422 => RenamedError::Validation {
                message,
                status_code: status,
                details,
            },
            429 => RenamedError::RateLimit {
                message,
                status_code: status,
                retry_after: error_response.and_then(|r| r.retry_after),
            },
            _ => RenamedError::Api {
                message,
                status_code: status,
                code: "API_ERROR".to_string(),
                details,
            },
        }
    }

    /// Creates a network error from a reqwest error.
    pub(crate) fn from_reqwest(err: reqwest::Error) -> Self {
        if err.is_timeout() {
            RenamedError::Timeout {
                message: "Request timed out".to_string(),
            }
        } else if err.is_connect() {
            RenamedError::Network {
                message: "Connection failed".to_string(),
                source: Some(err),
            }
        } else {
            RenamedError::Network {
                message: err.to_string(),
                source: Some(err),
            }
        }
    }

    /// Creates a job error.
    pub(crate) fn job_error(message: impl Into<String>, job_id: Option<String>) -> Self {
        RenamedError::Job {
            message: message.into(),
            job_id,
        }
    }

    /// Creates a file error from an I/O error.
    pub(crate) fn from_io(err: std::io::Error, context: impl Into<String>) -> Self {
        RenamedError::File {
            message: context.into(),
            source: Some(err),
        }
    }

    /// Creates a serialization error.
    pub(crate) fn from_serde(err: serde_json::Error) -> Self {
        RenamedError::Serialization {
            message: err.to_string(),
            source: Some(err),
        }
    }
}

/// Type alias for Results using RenamedError.
pub type Result<T> = std::result::Result<T, RenamedError>;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_error_from_401() {
        let err = RenamedError::from_http_status(401, Some(r#"{"error": "Invalid API key"}"#));
        assert!(matches!(err, RenamedError::Authentication { .. }));
    }

    #[test]
    fn test_error_from_402() {
        let err = RenamedError::from_http_status(402, Some(r#"{"error": "No credits"}"#));
        assert!(matches!(err, RenamedError::InsufficientCredits { .. }));
    }

    #[test]
    fn test_error_from_429() {
        let err = RenamedError::from_http_status(
            429,
            Some(r#"{"error": "Slow down", "retryAfter": 30}"#),
        );
        if let RenamedError::RateLimit { retry_after, .. } = err {
            assert_eq!(retry_after, Some(30));
        } else {
            panic!("Expected RateLimit error");
        }
    }
}
