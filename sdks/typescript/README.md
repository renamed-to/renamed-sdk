# @renamed/sdk

Official TypeScript/JavaScript SDK for the [renamed.to](https://www.renamed.to) API.

## Installation

```bash
npm install @renamed/sdk
# or
pnpm add @renamed/sdk
# or
yarn add @renamed/sdk
```

## Quick Start

```typescript
import { RenamedClient } from "@renamed/sdk";

const client = new RenamedClient({
  apiKey: "rt_your_api_key_here",
});

// Rename a file using AI
const result = await client.rename("invoice.pdf");
console.log(result.suggestedFilename);
// => "2025-01-15_AcmeCorp_INV-12345.pdf"
```

## Usage

### Rename Files

Rename files using AI-powered content analysis:

```typescript
import { RenamedClient } from "@renamed/sdk";

const client = new RenamedClient({ apiKey: "rt_..." });

// From file path
const result = await client.rename("/path/to/document.pdf");

// From Buffer
const buffer = fs.readFileSync("document.pdf");
const result = await client.rename(buffer);

// With custom template
const result = await client.rename("invoice.pdf", {
  template: "{date}_{vendor}_{type}",
});

console.log(result.suggestedFilename); // "2025-01-15_AcmeCorp_Invoice.pdf"
console.log(result.folderPath); // "2025/AcmeCorp/Invoices"
console.log(result.confidence); // 0.95
```

### Split PDFs

Split multi-page PDFs into individual documents:

```typescript
// Start the split job
const job = await client.pdfSplit("multi-page.pdf", { mode: "auto" });

// Wait for completion with progress updates
const result = await job.wait((status) => {
  console.log(`Progress: ${status.progress}%`);
});

// Download the split documents
for (const doc of result.documents) {
  const buffer = await client.downloadFile(doc.downloadUrl);
  fs.writeFileSync(doc.filename, buffer);
}
```

Split modes:
- `auto` - AI detects document boundaries
- `pages` - Split every N pages
- `blank` - Split at blank pages

### Extract Data

Extract structured data from documents:

```typescript
const result = await client.extract("invoice.pdf", {
  prompt: "Extract invoice number, date, vendor name, and total amount",
});

console.log(result.data);
// {
//   invoiceNumber: "INV-12345",
//   date: "2025-01-15",
//   vendor: "Acme Corp",
//   total: 1234.56
// }
```

### Check Credits

```typescript
const user = await client.getUser();
console.log(`Credits remaining: ${user.credits}`);
```

## Configuration

```typescript
const client = new RenamedClient({
  // Required: Your API key (get one at https://www.renamed.to/settings)
  apiKey: "rt_...",

  // Optional: Custom base URL (default: https://www.renamed.to/api/v1)
  baseUrl: "https://www.renamed.to/api/v1",

  // Optional: Request timeout in ms (default: 30000)
  timeout: 30000,

  // Optional: Max retries for failed requests (default: 2)
  maxRetries: 2,

  // Optional: Enable debug logging (default: false)
  debug: true,

  // Optional: Custom logger (default: console when debug=true)
  logger: myCustomLogger,
});
```

## Debug Logging

Enable debug logging to see HTTP request details for troubleshooting:

```typescript
const client = new RenamedClient({
  apiKey: "rt_...",
  debug: true,
});

// Output:
// [Renamed] POST /rename -> 200 (234ms)
// [Renamed] Upload: document.pdf (1.2 MB)
```

Use a custom logger to integrate with your logging framework:

```typescript
const client = new RenamedClient({
  apiKey: "rt_...",
  logger: {
    debug: (msg, ...args) => myLogger.debug(msg, ...args),
    info: (msg, ...args) => myLogger.info(msg, ...args),
    warn: (msg, ...args) => myLogger.warn(msg, ...args),
    error: (msg, ...args) => myLogger.error(msg, ...args),
  },
});
```

## Error Handling

```typescript
import {
  RenamedClient,
  AuthenticationError,
  RateLimitError,
  InsufficientCreditsError,
  ValidationError,
} from "@renamed/sdk";

try {
  const result = await client.rename("document.pdf");
} catch (error) {
  if (error instanceof AuthenticationError) {
    console.error("Invalid API key");
  } else if (error instanceof RateLimitError) {
    console.error(`Rate limited. Retry after ${error.retryAfter}s`);
  } else if (error instanceof InsufficientCreditsError) {
    console.error("Not enough credits");
  } else if (error instanceof ValidationError) {
    console.error("Invalid request:", error.message);
  } else {
    throw error;
  }
}
```

## File Input Types

The SDK accepts multiple file input types:

```typescript
// File path (string)
await client.rename("/path/to/file.pdf");

// Buffer
const buffer = fs.readFileSync("file.pdf");
await client.rename(buffer);

// Blob (browser)
const blob = new Blob([arrayBuffer], { type: "application/pdf" });
await client.rename(blob);

// File (browser)
const file = document.querySelector("input[type=file]").files[0];
await client.rename(file);
```

## Supported File Types

- PDF (`.pdf`)
- Images: JPEG (`.jpg`, `.jpeg`), PNG (`.png`), TIFF (`.tiff`, `.tif`)

## Requirements

- Node.js 18+ (uses native `fetch` and `FormData`)
- Or modern browser with Fetch API

## License

MIT
