# renamed-go

Official Go SDK for the [renamed.to](https://www.renamed.to) API.

## Installation

```bash
go get github.com/renamed-to/renamed-sdk/sdks/go
```

## Quick Start

```go
package main

import (
    "context"
    "fmt"
    "log"

    "github.com/renamed-to/renamed-sdk/sdks/go/renamed"
)

func main() {
    client := renamed.NewClient("rt_your_api_key_here")

    result, err := client.Rename(context.Background(), "invoice.pdf", nil)
    if err != nil {
        log.Fatal(err)
    }

    fmt.Println(result.SuggestedFilename)
    // => "2025-01-15_AcmeCorp_INV-12345.pdf"
}
```

## Usage

### Rename Files

Rename files using AI-powered content analysis:

```go
ctx := context.Background()
client := renamed.NewClient("rt_...")

// Simple rename
result, err := client.Rename(ctx, "document.pdf", nil)
if err != nil {
    log.Fatal(err)
}

fmt.Println(result.SuggestedFilename) // "2025-01-15_AcmeCorp_Invoice.pdf"
fmt.Println(result.FolderPath)        // "2025/AcmeCorp/Invoices"
fmt.Println(result.Confidence)        // 0.95

// With custom template
result, err = client.Rename(ctx, "invoice.pdf", &renamed.RenameOptions{
    Template: "{date}_{vendor}_{type}",
})

// From io.Reader
file, _ := os.Open("document.pdf")
defer file.Close()
result, err = client.RenameReader(ctx, file, "document.pdf", nil)
```

### Split PDFs

Split multi-page PDFs into individual documents:

```go
// Start the split job
job, err := client.PDFSplit(ctx, "multi-page.pdf", &renamed.PdfSplitOptions{
    Mode: renamed.SplitModeAuto,
})
if err != nil {
    log.Fatal(err)
}

// Wait for completion with progress updates
result, err := job.Wait(ctx, func(status *renamed.JobStatusResponse) {
    fmt.Printf("Progress: %d%%\n", status.Progress)
})
if err != nil {
    log.Fatal(err)
}

// Download the split documents
for _, doc := range result.Documents {
    content, err := client.DownloadFile(ctx, doc.DownloadURL)
    if err != nil {
        log.Fatal(err)
    }
    if err := os.WriteFile(doc.Filename, content, 0644); err != nil {
        log.Fatal(err)
    }
    fmt.Printf("Saved: %s\n", doc.Filename)
}
```

Split modes:
- `SplitModeAuto` - AI detects document boundaries
- `SplitModePages` - Split every N pages
- `SplitModeBlank` - Split at blank pages

### Extract Data

Extract structured data from documents:

```go
result, err := client.Extract(ctx, "invoice.pdf", &renamed.ExtractOptions{
    Prompt: "Extract invoice number, date, vendor name, and total amount",
})
if err != nil {
    log.Fatal(err)
}

fmt.Println(result.Data)
// map[invoiceNumber:INV-12345 date:2025-01-15 vendor:Acme Corp total:1234.56]
```

### Check Credits

```go
user, err := client.GetUser(ctx)
if err != nil {
    log.Fatal(err)
}
fmt.Printf("Credits remaining: %d\n", user.Credits)
```

## Configuration

```go
client := renamed.NewClient(
    "rt_...",
    // Custom base URL
    renamed.WithBaseURL("https://www.renamed.to/api/v1"),
    // Request timeout
    renamed.WithTimeout(30 * time.Second),
    // Max retries for failed requests
    renamed.WithMaxRetries(2),
    // Custom HTTP client
    renamed.WithHTTPClient(&http.Client{
        Transport: &http.Transport{
            MaxIdleConns: 10,
        },
    }),
    // Enable debug logging
    renamed.WithDebug(true),
    // Custom logger (optional)
    renamed.WithLogger(log.Default()),
)
```

## Debug Logging

Enable debug logging to see HTTP request details for troubleshooting:

```go
client := renamed.NewClient("rt_...", renamed.WithDebug(true))

// Output:
// [Renamed] POST /rename -> 200 (234ms)
// [Renamed] Upload: document.pdf (1.2 MB)
```

Use a custom logger by implementing the `Logger` interface:

```go
type Logger interface {
    Printf(format string, v ...any)
}

// Use any logger with Printf method
client := renamed.NewClient("rt_...", renamed.WithLogger(log.Default()))
```

## Error Handling

```go
import "errors"

result, err := client.Rename(ctx, "document.pdf", nil)
if err != nil {
    var authErr *renamed.AuthenticationError
    var rateLimitErr *renamed.RateLimitError
    var creditsErr *renamed.InsufficientCreditsError
    var validationErr *renamed.ValidationError

    switch {
    case errors.As(err, &authErr):
        log.Fatal("Invalid API key")
    case errors.As(err, &rateLimitErr):
        log.Printf("Rate limited. Retry after %ds", rateLimitErr.RetryAfter)
    case errors.As(err, &creditsErr):
        log.Fatal("Not enough credits")
    case errors.As(err, &validationErr):
        log.Printf("Invalid request: %s", validationErr.Message)
    default:
        log.Fatal(err)
    }
}
```

## Context Support

All methods accept a context for cancellation and timeouts:

```go
// With timeout
ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
defer cancel()

result, err := client.Rename(ctx, "large-file.pdf", nil)

// With cancellation
ctx, cancel := context.WithCancel(context.Background())
go func() {
    time.Sleep(10 * time.Second)
    cancel() // Cancel after 10 seconds
}()

job, err := client.PDFSplit(ctx, "multi-page.pdf", nil)
result, err := job.Wait(ctx, nil) // Will be cancelled if context is done
```

## Supported File Types

- PDF (`.pdf`)
- Images: JPEG (`.jpg`, `.jpeg`), PNG (`.png`), TIFF (`.tiff`, `.tif`)

## Requirements

- Go 1.21+
- No external dependencies (standard library only)

## License

MIT
