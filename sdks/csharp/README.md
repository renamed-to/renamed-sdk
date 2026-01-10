# Renamed.Sdk

Official .NET SDK for the [renamed.to](https://renamed.to) API - AI-powered file renaming, PDF splitting, and data extraction.

## Installation

### NuGet Package Manager

```shell
Install-Package Renamed.Sdk
```

### .NET CLI

```shell
dotnet add package Renamed.Sdk
```

## Requirements

- .NET 6.0 or .NET 8.0
- A renamed.to API key (get one at [renamed.to](https://renamed.to))

## Quick Start

```csharp
using Renamed.Sdk;
using Renamed.Sdk.Models;

// Create a client with your API key
using var client = new RenamedClient("rt_your_api_key");

// Rename a file using AI
var result = await client.RenameAsync("path/to/invoice.pdf");
Console.WriteLine($"Suggested filename: {result.SuggestedFilename}");
```

## Usage

### Initialize the Client

```csharp
// Simple initialization
using var client = new RenamedClient("rt_your_api_key");

// With custom options
using var client = new RenamedClient(new RenamedClientOptions
{
    ApiKey = "rt_your_api_key",
    BaseUrl = "https://www.renamed.to/api/v1",
    Timeout = TimeSpan.FromSeconds(60),
    MaxRetries = 3
});

// With a custom HttpClient (for dependency injection)
using var client = new RenamedClient(new RenamedClientOptions
{
    ApiKey = "rt_your_api_key",
    HttpClient = myHttpClient
});
```

### Get User Information

```csharp
var user = await client.GetUserAsync();
Console.WriteLine($"Email: {user.Email}");
Console.WriteLine($"Credits: {user.Credits}");

if (user.Team is not null)
{
    Console.WriteLine($"Team: {user.Team.Name}");
}
```

### Rename Files

Rename files using AI to get intelligently suggested filenames.

```csharp
// From a file path
var result = await client.RenameAsync("documents/scan001.pdf");
Console.WriteLine($"Original: {result.OriginalFilename}");
Console.WriteLine($"Suggested: {result.SuggestedFilename}");
Console.WriteLine($"Folder: {result.FolderPath}");
Console.WriteLine($"Confidence: {result.Confidence:P0}");

// From a stream
await using var stream = File.OpenRead("invoice.pdf");
var result = await client.RenameAsync(stream, "invoice.pdf");

// From bytes
var bytes = await File.ReadAllBytesAsync("receipt.jpg");
var result = await client.RenameAsync(bytes, "receipt.jpg");

// With a custom template
var result = await client.RenameAsync("invoice.pdf", new RenameOptions
{
    Template = "{date}_{vendor}_{type}"
});
```

### Split PDFs

Split multi-page PDFs into separate documents. This is an async operation that returns a job handle.

```csharp
// Start the split job
await using var stream = File.OpenRead("multi-page.pdf");
var job = await client.PdfSplitAsync(stream, "multi-page.pdf", new PdfSplitOptions
{
    Mode = PdfSplitMode.Auto // AI-detected document boundaries
});

// Wait for completion with progress updates
var result = await job.WaitAsync(status =>
{
    Console.WriteLine($"Status: {status.Status}, Progress: {status.Progress}%");
});

// Process the results
Console.WriteLine($"Original: {result.OriginalFilename}");
Console.WriteLine($"Total pages: {result.TotalPages}");
Console.WriteLine($"Split into {result.Documents.Count} documents");

foreach (var doc in result.Documents)
{
    Console.WriteLine($"  - {doc.Filename} (pages {doc.Pages}, {doc.Size} bytes)");

    // Download the split document
    var bytes = await client.DownloadFileAsync(doc.DownloadUrl);
    await File.WriteAllBytesAsync(doc.Filename, bytes);
}
```

#### Split Modes

- `PdfSplitMode.Auto` - AI automatically detects document boundaries
- `PdfSplitMode.Pages` - Split every N pages (use `PagesPerSplit` option)
- `PdfSplitMode.Blank` - Split at blank pages

```csharp
// Split every 5 pages
var job = await client.PdfSplitAsync("document.pdf", new PdfSplitOptions
{
    Mode = PdfSplitMode.Pages,
    PagesPerSplit = 5
});
```

### Extract Data

Extract structured data from documents using AI.

```csharp
// Extract with a prompt
var result = await client.ExtractAsync("invoice.pdf", new ExtractOptions
{
    Prompt = "Extract the invoice number, date, vendor name, and total amount"
});

Console.WriteLine($"Confidence: {result.Confidence:P0}");
foreach (var (key, value) in result.Data)
{
    Console.WriteLine($"  {key}: {value}");
}

// Extract with a schema
var result = await client.ExtractAsync("receipt.jpg", new ExtractOptions
{
    Schema = new Dictionary<string, object?>
    {
        ["merchant"] = new { type = "string" },
        ["date"] = new { type = "string", format = "date" },
        ["total"] = new { type = "number" },
        ["items"] = new { type = "array" }
    }
});
```

## Error Handling

The SDK throws specific exceptions for different error conditions:

```csharp
using Renamed.Sdk.Exceptions;

try
{
    var result = await client.RenameAsync("file.pdf");
}
catch (AuthenticationException ex)
{
    // Invalid or missing API key (HTTP 401)
    Console.WriteLine($"Authentication failed: {ex.Message}");
}
catch (InsufficientCreditsException ex)
{
    // Not enough credits (HTTP 402)
    Console.WriteLine($"Out of credits: {ex.Message}");
}
catch (ValidationException ex)
{
    // Invalid request parameters (HTTP 400/422)
    Console.WriteLine($"Validation error: {ex.Message}");
    Console.WriteLine($"Details: {ex.Details}");
}
catch (RateLimitException ex)
{
    // Rate limit exceeded (HTTP 429)
    Console.WriteLine($"Rate limited: {ex.Message}");
    if (ex.RetryAfterSeconds.HasValue)
    {
        Console.WriteLine($"Retry after: {ex.RetryAfterSeconds} seconds");
    }
}
catch (NetworkException ex)
{
    // Network connectivity issues
    Console.WriteLine($"Network error: {ex.Message}");
}
catch (RenamedTimeoutException ex)
{
    // Request timed out
    Console.WriteLine($"Timeout: {ex.Message}");
}
catch (JobException ex)
{
    // Async job failed
    Console.WriteLine($"Job failed: {ex.Message}");
    Console.WriteLine($"Job ID: {ex.JobId}");
}
catch (RenamedExceptionBase ex)
{
    // Other API errors
    Console.WriteLine($"Error [{ex.Code}]: {ex.Message}");
}
```

## Cancellation Support

All async methods support cancellation tokens:

```csharp
using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(5));

try
{
    var job = await client.PdfSplitAsync("large-document.pdf", cancellationToken: cts.Token);
    var result = await job.WaitAsync(cancellationToken: cts.Token);
}
catch (OperationCanceledException)
{
    Console.WriteLine("Operation was cancelled");
}
```

## Dependency Injection

For ASP.NET Core applications, you can register the client as a service:

```csharp
// In Program.cs or Startup.cs
builder.Services.AddSingleton(sp => new RenamedClient(new RenamedClientOptions
{
    ApiKey = builder.Configuration["Renamed:ApiKey"]!,
    HttpClient = sp.GetRequiredService<IHttpClientFactory>().CreateClient("Renamed")
}));

// In your controller or service
public class DocumentService
{
    private readonly RenamedClient _client;

    public DocumentService(RenamedClient client)
    {
        _client = client;
    }

    public async Task<string> GetSuggestedNameAsync(Stream file, string fileName)
    {
        var result = await _client.RenameAsync(file, fileName);
        return result.SuggestedFilename;
    }
}
```

## API Reference

### RenamedClient

| Method | Description |
|--------|-------------|
| `GetUserAsync()` | Get current user profile and credits |
| `RenameAsync(file, options?)` | Rename a file using AI |
| `PdfSplitAsync(file, options?)` | Split a PDF into multiple documents |
| `ExtractAsync(file, options?)` | Extract structured data from a document |
| `DownloadFileAsync(url)` | Download a file from a URL |

### Models

- `User` - User profile information
- `Team` - Team information
- `RenameResult` - Result of rename operation
- `PdfSplitResult` - Result of PDF split operation
- `SplitDocument` - Individual split document
- `ExtractResult` - Result of extract operation
- `JobStatus` - Status of async job (Pending, Processing, Completed, Failed)
- `JobStatusResponse` - Response from job status endpoint

### Options

- `RenamedClientOptions` - Client configuration
- `RenameOptions` - Options for rename operation
- `PdfSplitOptions` - Options for PDF split operation
- `ExtractOptions` - Options for extract operation

### Exceptions

- `RenamedExceptionBase` - Base exception class
- `AuthenticationException` - Invalid or missing API key
- `InsufficientCreditsException` - Not enough credits
- `ValidationException` - Invalid request parameters
- `RateLimitException` - Rate limit exceeded
- `NetworkException` - Network connectivity issues
- `RenamedTimeoutException` - Request timed out
- `JobException` - Async job failed

## License

MIT
