# renamed

Official Python SDK for the [renamed.to](https://www.renamed.to) API.

## Installation

```bash
pip install renamed
# or
poetry add renamed
# or
uv add renamed
```

## Quick Start

```python
from renamed import RenamedClient

client = RenamedClient(api_key="rt_your_api_key_here")

# Rename a file using AI
result = client.rename("invoice.pdf")
print(result.suggested_filename)
# => "2025-01-15_AcmeCorp_INV-12345.pdf"
```

## Usage

### Rename Files

Rename files using AI-powered content analysis:

```python
from renamed import RenamedClient

client = RenamedClient(api_key="rt_...")

# From file path
result = client.rename("/path/to/document.pdf")

# From bytes
with open("document.pdf", "rb") as f:
    result = client.rename(f.read())

# With custom template
result = client.rename("invoice.pdf", template="{date}_{vendor}_{type}")

print(result.suggested_filename)  # "2025-01-15_AcmeCorp_Invoice.pdf"
print(result.folder_path)         # "2025/AcmeCorp/Invoices"
print(result.confidence)          # 0.95
```

### Split PDFs

Split multi-page PDFs into individual documents:

```python
from pathlib import Path

# Start the split job
job = client.pdf_split("multi-page.pdf", mode="auto")

# Wait for completion with progress updates
result = job.wait(lambda status: print(f"Progress: {status.progress}%"))

# Download the split documents
for doc in result.documents:
    content = client.download_file(doc.download_url)
    Path(doc.filename).write_bytes(content)
```

Split modes:
- `auto` - AI detects document boundaries
- `pages` - Split every N pages
- `blank` - Split at blank pages

### Extract Data

Extract structured data from documents:

```python
result = client.extract(
    "invoice.pdf",
    prompt="Extract invoice number, date, vendor name, and total amount"
)

print(result.data)
# {
#     "invoiceNumber": "INV-12345",
#     "date": "2025-01-15",
#     "vendor": "Acme Corp",
#     "total": 1234.56
# }
```

### Check Credits

```python
user = client.get_user()
print(f"Credits remaining: {user.credits}")
```

## Async Support

All methods have async versions with the `_async` suffix:

```python
import asyncio
from renamed import RenamedClient

async def main():
    client = RenamedClient(api_key="rt_...")

    # Async rename
    result = await client.rename_async("invoice.pdf")
    print(result.suggested_filename)

    # Async PDF split
    job = await client.pdf_split_async("multi-page.pdf", mode="auto")
    result = await job.wait_async()

    await client.aclose()

asyncio.run(main())
```

Or use as a context manager:

```python
async with RenamedClient(api_key="rt_...") as client:
    result = await client.rename_async("invoice.pdf")
```

## Configuration

```python
client = RenamedClient(
    # Required: Your API key (get one at https://www.renamed.to/settings)
    api_key="rt_...",

    # Optional: Custom base URL (default: https://www.renamed.to/api/v1)
    base_url="https://www.renamed.to/api/v1",

    # Optional: Request timeout in seconds (default: 30.0)
    timeout=30.0,

    # Optional: Max retries for failed requests (default: 2)
    max_retries=2,

    # Optional: Enable debug logging (default: False)
    debug=True,

    # Optional: Custom logger (default: stderr logger when debug=True)
    logger=my_logger,
)
```

## Debug Logging

Enable debug logging to see HTTP request details for troubleshooting:

```python
client = RenamedClient(api_key="rt_...", debug=True)

# Output:
# [Renamed] POST /rename -> 200 (234ms)
# [Renamed] Upload: document.pdf (1.2 MB)
```

Use Python's standard logging module for custom logging:

```python
import logging

# Configure logging level
logging.basicConfig(level=logging.DEBUG)

# Or use a custom logger
logger = logging.getLogger("my_app")
client = RenamedClient(api_key="rt_...", logger=logger)
```

## Error Handling

```python
from renamed import (
    RenamedClient,
    AuthenticationError,
    RateLimitError,
    InsufficientCreditsError,
    ValidationError,
)

try:
    result = client.rename("document.pdf")
except AuthenticationError:
    print("Invalid API key")
except RateLimitError as e:
    print(f"Rate limited. Retry after {e.retry_after}s")
except InsufficientCreditsError:
    print("Not enough credits")
except ValidationError as e:
    print(f"Invalid request: {e.message}")
```

## File Input Types

The SDK accepts multiple file input types:

```python
from pathlib import Path

# File path (str)
client.rename("/path/to/file.pdf")

# Path object
client.rename(Path("file.pdf"))

# Bytes
content = Path("file.pdf").read_bytes()
client.rename(content)

# File-like object
with open("file.pdf", "rb") as f:
    client.rename(f)
```

## Supported File Types

- PDF (`.pdf`)
- Images: JPEG (`.jpg`, `.jpeg`), PNG (`.png`), TIFF (`.tiff`, `.tif`)

## Requirements

- Python 3.9+
- Dependencies: `httpx`, `pydantic`

## License

MIT
