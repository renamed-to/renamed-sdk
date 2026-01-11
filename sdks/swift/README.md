# Renamed Swift SDK

Official Swift SDK for the [renamed.to](https://www.renamed.to) API - AI-powered file renaming, PDF splitting, and document extraction.

## Requirements

- iOS 15.0+ / macOS 12.0+
- Swift 5.9+
- Xcode 15.0+

## Installation

### Swift Package Manager

Add the following to your `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/renamed-to/renamed-sdk", from: "0.1.0")
]
```

Then add `Renamed` as a dependency of your target:

```swift
targets: [
    .target(
        name: "YourTarget",
        dependencies: ["Renamed"]
    )
]
```

Or in Xcode: File > Add Package Dependencies, then enter the repository URL.

## Quick Start

```swift
import Renamed

// Initialize the client with your API key
let client = try RenamedClient(apiKey: "rt_your_api_key")

// Rename a file using AI
let file = try FileInput(url: URL(fileURLWithPath: "invoice.pdf"))
let result = try await client.rename(file: file)
print("Suggested filename: \(result.suggestedFilename)")
```

## Usage

### Renaming Files

Use AI to generate descriptive filenames based on document content:

```swift
import Renamed

let client = try RenamedClient(apiKey: "rt_your_api_key")

// From a file URL
let file = try FileInput(url: URL(fileURLWithPath: "document.pdf"))
let result = try await client.rename(file: file)

print("Original: \(result.originalFilename)")
print("Suggested: \(result.suggestedFilename)")
print("Folder: \(result.folderPath ?? "N/A")")
print("Confidence: \(result.confidence ?? 0)")

// With a custom template
let options = RenameOptions(template: "{date}_{vendor}_{type}")
let result2 = try await client.rename(file: file, options: options)
```

### Splitting PDFs

Split multi-page PDFs into separate documents:

```swift
let file = try FileInput(url: URL(fileURLWithPath: "multi-page.pdf"))

// Start the split job (async operation)
let job = try await client.pdfSplit(
    file: file,
    options: PdfSplitOptions(mode: .auto)
)

// Wait for completion with progress updates
let result = try await job.wait { status in
    print("Status: \(status.status), Progress: \(status.progress ?? 0)%")
}

// Download the split documents
for doc in result.documents {
    print("Document: \(doc.filename), Pages: \(doc.pages)")
    let data = try await client.downloadFile(url: doc.downloadUrl)
    try data.write(to: URL(fileURLWithPath: doc.filename))
}
```

Split modes:
- `.auto` - AI-detected document boundaries
- `.pages` - Split every N pages (use `pagesPerSplit`)
- `.blank` - Split at blank pages

### Extracting Data

Extract structured data from documents:

```swift
let file = try FileInput(url: URL(fileURLWithPath: "invoice.pdf"))

// Extract with a prompt
let result = try await client.extract(
    file: file,
    options: ExtractOptions(prompt: "Extract the invoice number, date, and total amount")
)

print("Extracted data: \(result.data)")
print("Confidence: \(result.confidence)")

// Extract with a schema
let schema: [String: Any] = [
    "invoiceNumber": "string",
    "date": "string",
    "totalAmount": "number",
    "lineItems": [
        ["description": "string", "amount": "number"]
    ]
]
let result2 = try await client.extract(
    file: file,
    options: ExtractOptions(schema: schema)
)
```

### Getting User Info

Check your account status and credits:

```swift
let user = try await client.getUser()

print("User: \(user.name ?? user.email)")
print("Credits: \(user.credits ?? 0)")

if let team = user.team {
    print("Team: \(team.name)")
}
```

## Configuration

### Client Options

```swift
let options = RenamedClientOptions(
    apiKey: "rt_your_api_key",
    baseUrl: "https://www.renamed.to/api/v1",  // Optional: custom base URL
    timeout: 30,                                 // Optional: request timeout in seconds
    maxRetries: 2,                               // Optional: retry count for failed requests
    debug: true                                  // Optional: enable debug logging
)

let client = try RenamedClient(options: options)
```

## Debug Logging

Enable debug logging to see HTTP request details for troubleshooting:

```swift
let client = try RenamedClient(
    options: RenamedClientOptions(apiKey: "rt_...", debug: true)
)

// Output:
// [Renamed] POST /rename -> 200 (234ms)
// [Renamed] Upload: document.pdf (1.2 MB)
```

Use a custom logger by implementing `RenamedLogger`:

```swift
class MyLogger: RenamedLogger {
    func log(level: RenamedLogLevel, message: String) {
        print("[\(level)] \(message)")
    }
}

let client = try RenamedClient(
    options: RenamedClientOptions(apiKey: "rt_...", logger: MyLogger())
)
```

## File Input

The SDK supports multiple ways to provide file data:

```swift
// From a file URL
let file1 = try FileInput(url: URL(fileURLWithPath: "/path/to/document.pdf"))

// From raw data with filename
let data = try Data(contentsOf: someURL)
let file2 = FileInput(data: data, filename: "document.pdf")

// With explicit MIME type
let file3 = FileInput(data: data, filename: "document.pdf", mimeType: "application/pdf")
```

Supported file types:
- PDF (`.pdf`)
- JPEG (`.jpg`, `.jpeg`)
- PNG (`.png`)
- TIFF (`.tiff`, `.tif`)

## Error Handling

The SDK uses typed errors for different failure scenarios:

```swift
do {
    let result = try await client.rename(file: file)
} catch RenamedError.authentication(let message) {
    print("Authentication failed: \(message)")
} catch RenamedError.insufficientCredits(let message) {
    print("Not enough credits: \(message)")
} catch RenamedError.rateLimit(let message, let retryAfter) {
    print("Rate limited: \(message)")
    if let seconds = retryAfter {
        print("Retry after \(seconds) seconds")
    }
} catch RenamedError.validation(let message, let details) {
    print("Invalid request: \(message)")
} catch RenamedError.network(let message) {
    print("Network error: \(message)")
} catch RenamedError.timeout(let message) {
    print("Request timed out: \(message)")
} catch RenamedError.job(let message, let jobId) {
    print("Job failed: \(message), ID: \(jobId ?? "unknown")")
} catch RenamedError.api(let message, let statusCode) {
    print("API error (\(statusCode)): \(message)")
}
```

## Async Job Handling

Long-running operations like PDF splitting return an `AsyncJob`:

```swift
let job = try await client.pdfSplit(file: file)

// Check status manually
let status = try await job.status()
print("Job \(status.jobId): \(status.status)")

// Or wait for completion
let result = try await job.wait { status in
    // Optional progress callback
    if let progress = status.progress {
        print("Progress: \(progress)%")
    }
}
```

## Thread Safety

The `RenamedClient` is thread-safe and can be shared across multiple concurrent tasks. All public methods are async and use Swift's structured concurrency.

## License

MIT License - see LICENSE file for details.

## Support

- Documentation: [https://www.renamed.to/docs](https://www.renamed.to/docs)
- API Reference: [https://www.renamed.to/docs/api-docs](https://www.renamed.to/docs/api-docs)
- Issues: [https://github.com/renamed-to/renamed-sdk/issues](https://github.com/renamed-to/renamed-sdk/issues)
