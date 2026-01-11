# renamed

Official Rust SDK for the [renamed.to](https://www.renamed.to) API.

AI-powered document renaming, PDF splitting, and data extraction.

## Installation

Add to your `Cargo.toml`:

```toml
[dependencies]
renamed = "0.1"
tokio = { version = "1", features = ["full"] }
```

## Quick Start

```rust
use renamed::{RenamedClient, RenameOptions};

#[tokio::main]
async fn main() -> Result<(), renamed::RenamedError> {
    let client = RenamedClient::new("rt_your_api_key");

    // Check your credits
    let user = client.get_user().await?;
    println!("Credits remaining: {}", user.credits.unwrap_or(0));

    // Rename a file using AI
    let result = client.rename("invoice.pdf", None).await?;
    println!("Suggested name: {}", result.suggested_filename);

    Ok(())
}
```

## Examples

See runnable examples in the SDK repo: [examples/rust](https://github.com/renamed-to/renamed-sdk/tree/main/examples/rust) (`basic_usage.rs`).

## Features

### Rename Files

Analyze document content and get AI-suggested filenames:

```rust
use renamed::{RenamedClient, RenameOptions};

let client = RenamedClient::new("rt_your_api_key");

// Basic usage
let result = client.rename("document.pdf", None).await?;
println!("Original: {}", result.original_filename);
println!("Suggested: {}", result.suggested_filename);

// With custom template
let options = RenameOptions::new()
    .with_template("{date}_{type}_{vendor}");
let result = client.rename("invoice.pdf", Some(options)).await?;
```

### Split PDFs

Split multi-page PDFs into separate documents:

```rust
use renamed::{RenamedClient, PdfSplitOptions, SplitMode};

let client = RenamedClient::new("rt_your_api_key");

// Auto-detect document boundaries
let job = client.pdf_split("multi-page.pdf", None).await?;

// Wait with progress updates
let result = job.wait(Some(Box::new(|status| {
    println!("Progress: {}%", status.progress.unwrap_or(0));
}))).await?;

// Download split documents
for doc in result.documents {
    let content = client.download_file(&doc.download_url).await?;
    tokio::fs::write(&doc.filename, content).await?;
    println!("Saved: {} (pages {})", doc.filename, doc.pages);
}
```

Split modes:
- `SplitMode::Auto` - AI detects document boundaries (default)
- `SplitMode::Pages` - Split every N pages
- `SplitMode::Blank` - Split at blank pages

### Extract Data

Extract structured data from documents:

```rust
use renamed::{RenamedClient, ExtractOptions};

let client = RenamedClient::new("rt_your_api_key");

let options = ExtractOptions::new()
    .with_prompt("Extract invoice number, date, and total amount");

let result = client.extract("invoice.pdf", Some(options)).await?;

println!("Data: {:?}", result.data);
println!("Confidence: {:.0}%", result.confidence * 100.0);
```

## Error Handling

The SDK provides specific error types for different failure modes:

```rust
use renamed::{RenamedClient, RenamedError};

let client = RenamedClient::new("rt_your_api_key");

match client.get_user().await {
    Ok(user) => println!("Hello, {}!", user.email),
    Err(RenamedError::Authentication { .. }) => {
        eprintln!("Invalid API key");
    }
    Err(RenamedError::InsufficientCredits { .. }) => {
        eprintln!("Please add more credits");
    }
    Err(RenamedError::RateLimit { retry_after, .. }) => {
        if let Some(seconds) = retry_after {
            eprintln!("Rate limited. Retry after {} seconds", seconds);
        }
    }
    Err(RenamedError::Validation { message, .. }) => {
        eprintln!("Invalid request: {}", message);
    }
    Err(e) => eprintln!("Error: {}", e),
}
```

Error variants:
- `Authentication` - Invalid or missing API key (401)
- `InsufficientCredits` - Not enough credits (402)
- `RateLimit` - Too many requests (429)
- `Validation` - Invalid request parameters (400/422)
- `Network` - Connection failures
- `Timeout` - Request timeout
- `Job` - Async job failure
- `Api` - Other API errors

## Configuration

Use the builder pattern for custom configuration:

```rust
use renamed::RenamedClient;
use std::time::Duration;

let client = RenamedClient::builder("rt_your_api_key")
    .base_url("https://custom.api.com")
    .timeout(Duration::from_secs(60))
    .max_retries(3)
    .build();
```

## Working with Bytes

All file methods have `_bytes` variants for working with in-memory data:

```rust
let content = std::fs::read("document.pdf")?;
let result = client.rename_bytes(content, "document.pdf", None).await?;
```

## License

MIT
