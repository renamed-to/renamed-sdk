# renamed.to SDKs

Official SDKs for the [renamed.to](https://www.renamed.to) API — AI-powered file renaming, PDF splitting, and data extraction.

[![TypeScript](https://img.shields.io/npm/v/@renamed/sdk?label=TypeScript)](https://www.npmjs.com/package/@renamed/sdk)
[![Python](https://img.shields.io/pypi/v/renamed?label=Python)](https://pypi.org/project/renamed/)
[![Go](https://img.shields.io/github/v/tag/renamed-to/renamed-sdk?filter=go/*&label=Go)](https://pkg.go.dev/github.com/renamed-to/renamed-sdk/sdks/go)

## Installation

<table>
<tr>
<td><strong>TypeScript</strong></td>
<td><strong>Python</strong></td>
<td><strong>Go</strong></td>
</tr>
<tr>
<td>

```bash
npm install @renamed/sdk
```

</td>
<td>

```bash
pip install renamed
```

</td>
<td>

```bash
go get github.com/renamed-to/renamed-sdk/sdks/go
```

</td>
</tr>
</table>

## Quick Start

Get your API key at [renamed.to/settings](https://www.renamed.to/settings).

<table>
<tr>
<td><strong>TypeScript</strong></td>
<td><strong>Python</strong></td>
<td><strong>Go</strong></td>
</tr>
<tr>
<td>

```typescript
import { RenamedClient } from '@renamed/sdk';

const client = new RenamedClient({
  apiKey: 'rt_...'
});

const result = await client.rename('invoice.pdf');
console.log(result.suggestedFilename);
// "2025-01-15_AcmeCorp_INV-12345.pdf"
```

</td>
<td>

```python
from renamed import RenamedClient

client = RenamedClient(api_key='rt_...')

result = client.rename('invoice.pdf')
print(result.suggested_filename)
# "2025-01-15_AcmeCorp_INV-12345.pdf"
```

</td>
<td>

```go
client := renamed.NewClient("rt_...")

result, _ := client.Rename(
  ctx, "invoice.pdf", nil,
)
fmt.Println(result.SuggestedFilename)
// "2025-01-15_AcmeCorp_INV-12345.pdf"
```

</td>
</tr>
</table>

## Features

### Rename Files

AI-powered file renaming with intelligent naming suggestions:

```typescript
const result = await client.rename('scan001.pdf');
// {
//   suggestedFilename: "2025-01-15_AcmeCorp_INV-12345.pdf",
//   folderPath: "2025/AcmeCorp/Invoices",
//   confidence: 0.95
// }
```

### Split PDFs

Split multi-page PDFs into individual documents:

```typescript
const job = await client.pdfSplit('multi-page.pdf', { mode: 'auto' });
const result = await job.wait();

for (const doc of result.documents) {
  const buffer = await client.downloadFile(doc.downloadUrl);
  // Save doc.filename with buffer
}
```

### Extract Data

Extract structured data from documents:

```typescript
const result = await client.extract('invoice.pdf', {
  prompt: 'Extract invoice number, date, and total amount'
});
console.log(result.data);
// { invoiceNumber: "INV-12345", date: "2025-01-15", total: 1234.56 }
```

## API Reference

| Method | Description |
|--------|-------------|
| `rename(file)` | Rename a file using AI |
| `pdfSplit(file, options)` | Split PDF into documents |
| `extract(file, options)` | Extract structured data |
| `getUser()` | Get user profile & credits |
| `downloadFile(url)` | Download a split document |

## Supported File Types

- PDF (`.pdf`)
- Images: JPEG, PNG, TIFF

## Documentation

- [API Documentation](https://www.renamed.to/docs/api-docs)
- [TypeScript SDK](./sdks/typescript/README.md)
- [Python SDK](./sdks/python/README.md)
- [Go SDK](./sdks/go/README.md)

## License

MIT
