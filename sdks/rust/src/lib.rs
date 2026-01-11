//! # renamed
//!
//! Official Rust SDK for the [renamed.to](https://www.renamed.to) API.
//!
//! This crate provides a type-safe, async client for AI-powered document
//! renaming, PDF splitting, and data extraction.
//!
//! ## Quick Start
//!
//! ```rust,no_run
//! use renamed::{RenamedClient, RenameOptions};
//!
//! #[tokio::main]
//! async fn main() -> Result<(), renamed::RenamedError> {
//!     // Create a client with your API key
//!     let client = RenamedClient::new("rt_your_api_key");
//!
//!     // Check your credits
//!     let user = client.get_user().await?;
//!     println!("Credits remaining: {}", user.credits.unwrap_or(0));
//!
//!     // Rename a file using AI
//!     let result = client.rename("invoice.pdf", None).await?;
//!     println!("Suggested name: {}", result.suggested_filename);
//!
//!     Ok(())
//! }
//! ```
//!
//! ## Features
//!
//! - **Rename**: AI-powered file renaming based on content analysis
//! - **PDF Split**: Split multi-page PDFs into separate documents
//! - **Extract**: Extract structured data from documents
//!
//! ## Error Handling
//!
//! All methods return `Result<T, RenamedError>`. The error type provides
//! specific variants for different failure modes:
//!
//! ```rust,no_run
//! use renamed::{RenamedClient, RenamedError};
//!
//! # async fn example() -> Result<(), RenamedError> {
//! let client = RenamedClient::new("rt_your_api_key");
//!
//! match client.get_user().await {
//!     Ok(user) => println!("Hello, {}!", user.email),
//!     Err(RenamedError::Authentication { .. }) => {
//!         eprintln!("Invalid API key");
//!     }
//!     Err(RenamedError::InsufficientCredits { .. }) => {
//!         eprintln!("Please add more credits");
//!     }
//!     Err(RenamedError::RateLimit { retry_after, .. }) => {
//!         if let Some(seconds) = retry_after {
//!             eprintln!("Rate limited. Retry after {} seconds", seconds);
//!         }
//!     }
//!     Err(e) => eprintln!("Error: {}", e),
//! }
//! # Ok(())
//! # }
//! ```
//!
//! ## Async Jobs
//!
//! Some operations like PDF splitting are asynchronous. These return an
//! [`AsyncJob`] that can be polled for completion:
//!
//! ```rust,no_run
//! use renamed::{RenamedClient, PdfSplitOptions, SplitMode};
//!
//! # async fn example() -> Result<(), Box<dyn std::error::Error>> {
//! let client = RenamedClient::new("rt_your_api_key");
//!
//! // Start the job
//! let job = client.pdf_split("multi-page.pdf", None).await?;
//!
//! // Wait for completion with progress updates
//! let result = job.wait(Some(Box::new(|status| {
//!     println!("Progress: {}%", status.progress.unwrap_or(0));
//! }))).await?;
//!
//! // Process the results
//! for doc in result.documents {
//!     let content = client.download_file(&doc.download_url).await?;
//!     tokio::fs::write(&doc.filename, content).await?;
//! }
//! # Ok(())
//! # }
//! ```
//!
//! ## Configuration
//!
//! Use the builder pattern for custom configuration:
//!
//! ```rust,no_run
//! use renamed::RenamedClient;
//! use std::time::Duration;
//!
//! let client = RenamedClient::builder("rt_your_api_key")
//!     .timeout(Duration::from_secs(60))
//!     .max_retries(3)
//!     .build();
//! ```

#![deny(missing_docs)]
#![deny(unsafe_code)]

mod async_job;
mod client;
mod error;
mod models;

// Re-export main types at crate root for convenience
pub use async_job::{AsyncJob, ProgressCallback};
pub use client::{RenamedClient, RenamedClientBuilder};
pub use error::{RenamedError, Result};
pub use models::{
    ExtractOptions, ExtractResult, JobStatus, JobStatusResponse, PdfSplitOptions, PdfSplitResult,
    RenameOptions, RenameResult, SplitDocument, SplitMode, Team, User,
};

/// Prelude module for convenient imports.
///
/// ```rust
/// use renamed::prelude::*;
/// ```
pub mod prelude {
    pub use crate::async_job::{AsyncJob, ProgressCallback};
    pub use crate::client::RenamedClient;
    pub use crate::error::{RenamedError, Result};
    pub use crate::models::{
        ExtractOptions, ExtractResult, JobStatus, PdfSplitOptions, PdfSplitResult, RenameOptions,
        RenameResult, SplitDocument, SplitMode, User,
    };
}
