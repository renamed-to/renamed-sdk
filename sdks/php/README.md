# Renamed PHP SDK

Official PHP SDK for the [renamed.to](https://www.renamed.to) API. Rename files, split PDFs, and extract structured data from documents using AI.

## Requirements

- PHP 8.1 or higher
- Composer

## Installation

```bash
composer require renamed/sdk
```

## Quick Start

```php
<?php

require 'vendor/autoload.php';

use Renamed\Client;

// Initialize the client with your API key
$client = new Client('rt_your_api_key');

// Rename a file using AI
$result = $client->rename('/path/to/invoice.pdf');
echo $result->suggestedFilename; // "2025-01-15_AcmeCorp_INV-12345.pdf"
```

## Usage

### Get User Information

```php
$user = $client->getUser();
echo "Email: {$user->email}\n";
echo "Credits remaining: {$user->credits}\n";

if ($user->team) {
    echo "Team: {$user->team->name}\n";
}
```

### Rename Files

Rename files using AI to generate meaningful, consistent filenames.

```php
// Basic rename
$result = $client->rename('/path/to/document.pdf');
echo $result->suggestedFilename;  // AI-generated filename
echo $result->folderPath;         // Suggested folder path
echo $result->confidence;         // Confidence score (0-1)

// With custom template
$result = $client->rename('/path/to/invoice.pdf', [
    'template' => '{date}_{vendor}_{type}'
]);
```

### Split PDFs

Split multi-page PDFs into separate documents. This is an async operation that returns a job you can poll for completion.

```php
// Start the split job
$job = $client->pdfSplit('/path/to/multi-page.pdf', [
    'mode' => 'auto'  // 'auto', 'pages', or 'blank'
]);

// Wait for completion with progress callback
$result = $job->wait(function ($status) {
    echo "Progress: {$status->progress}%\n";
});

// Process the split documents
echo "Original: {$result->originalFilename}\n";
echo "Total pages: {$result->totalPages}\n";

foreach ($result->documents as $doc) {
    echo "Document {$doc->index}: {$doc->filename}\n";
    echo "  Pages: {$doc->pages}\n";
    echo "  Size: {$doc->size} bytes\n";

    // Download the split document
    $content = $client->downloadFile($doc->downloadUrl);
    file_put_contents($doc->filename, $content);
}
```

#### Split Modes

- `auto` - AI-detected document boundaries
- `pages` - Split every N pages (use with `pagesPerSplit` option)
- `blank` - Split at blank pages

```php
// Split every 2 pages
$job = $client->pdfSplit('/path/to/document.pdf', [
    'mode' => 'pages',
    'pagesPerSplit' => 2
]);
```

### Extract Data

Extract structured data from documents using natural language prompts or JSON schemas.

```php
// Extract with a prompt
$result = $client->extract('/path/to/invoice.pdf', [
    'prompt' => 'Extract the vendor name, invoice number, date, and total amount'
]);
print_r($result->data);
echo "Confidence: {$result->confidence}\n";

// Extract with a JSON schema
$result = $client->extract('/path/to/invoice.pdf', [
    'schema' => [
        'type' => 'object',
        'properties' => [
            'vendor' => ['type' => 'string'],
            'invoiceNumber' => ['type' => 'string'],
            'date' => ['type' => 'string', 'format' => 'date'],
            'total' => ['type' => 'number']
        ],
        'required' => ['vendor', 'total']
    ]
]);
```

## Error Handling

The SDK provides specific exception types for different error conditions.

```php
use Renamed\Client;
use Renamed\Exceptions\AuthenticationException;
use Renamed\Exceptions\InsufficientCreditsException;
use Renamed\Exceptions\RateLimitException;
use Renamed\Exceptions\ValidationException;
use Renamed\Exceptions\RenamedExceptionBase;

try {
    $result = $client->rename('/path/to/file.pdf');
} catch (AuthenticationException $e) {
    // Invalid or missing API key
    echo "Authentication failed: {$e->getMessage()}\n";
} catch (InsufficientCreditsException $e) {
    // Not enough credits
    echo "Insufficient credits: {$e->getMessage()}\n";
} catch (RateLimitException $e) {
    // Rate limit exceeded
    echo "Rate limited. Retry after: {$e->getRetryAfter()} seconds\n";
} catch (ValidationException $e) {
    // Invalid request parameters
    echo "Validation error: {$e->getMessage()}\n";
    print_r($e->getDetails());
} catch (RenamedExceptionBase $e) {
    // Other API errors
    echo "Error [{$e->getErrorCode()}]: {$e->getMessage()}\n";
}
```

### Exception Types

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| `AuthenticationException` | 401 | Invalid or missing API key |
| `InsufficientCreditsException` | 402 | Not enough credits |
| `ValidationException` | 400, 422 | Invalid request parameters |
| `RateLimitException` | 429 | Rate limit exceeded |
| `NetworkException` | - | Connection failed |
| `TimeoutException` | - | Request timed out |
| `JobException` | - | Async job failed |

## Configuration

```php
$client = new Client(
    apiKey: 'rt_your_api_key',
    baseUrl: 'https://www.renamed.to/api/v1',  // Default API URL
    timeout: 30.0,                               // Request timeout in seconds
    maxRetries: 2                                // Max retries for failed requests
);
```

## Models

### User

```php
$user->id;       // string - User ID
$user->email;    // string - Email address
$user->name;     // ?string - Display name
$user->credits;  // ?int - Available credits
$user->team;     // ?Team - Team information
```

### Team

```php
$team->id;    // string - Team ID
$team->name;  // string - Team name
```

### RenameResult

```php
$result->originalFilename;   // string - Original filename
$result->suggestedFilename;  // string - AI-suggested filename
$result->folderPath;         // ?string - Suggested folder path
$result->confidence;         // ?float - Confidence score (0-1)
```

### PdfSplitResult

```php
$result->originalFilename;  // string - Original filename
$result->documents;         // SplitDocument[] - Split documents
$result->totalPages;        // int - Total pages in original
```

### SplitDocument

```php
$doc->index;        // int - Document index (0-based)
$doc->filename;     // string - Suggested filename
$doc->pages;        // string - Page range (e.g., "1-3")
$doc->downloadUrl;  // string - Download URL
$doc->size;         // int - Size in bytes
```

### ExtractResult

```php
$result->data;        // array - Extracted data
$result->confidence;  // float - Confidence score (0-1)
```

## Supported File Types

- PDF (`.pdf`)
- Images: JPEG (`.jpg`, `.jpeg`), PNG (`.png`), TIFF (`.tif`, `.tiff`)

## License

MIT License - see [LICENSE](LICENSE) for details.
