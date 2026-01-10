# renamed.to SDKs

Official SDKs for the [renamed.to](https://www.renamed.to) API — AI-powered file renaming, PDF splitting, and data extraction.

[![TypeScript](https://img.shields.io/npm/v/@renamed/sdk?label=TypeScript)](https://www.npmjs.com/package/@renamed/sdk)
[![Python](https://img.shields.io/pypi/v/renamed?label=Python)](https://pypi.org/project/renamed/)
[![Go](https://img.shields.io/github/v/tag/renamed-to/renamed-sdk?filter=go/*&label=Go)](https://pkg.go.dev/github.com/renamed-to/renamed-sdk/sdks/go)
[![Java](https://img.shields.io/maven-central/v/to.renamed/renamed-sdk?label=Java)](https://central.sonatype.com/artifact/to.renamed/renamed-sdk)
[![C#](https://img.shields.io/nuget/v/Renamed.Sdk?label=C%23)](https://www.nuget.org/packages/Renamed.Sdk)
[![Ruby](https://img.shields.io/gem/v/renamed?label=Ruby)](https://rubygems.org/gems/renamed)
[![Rust](https://img.shields.io/crates/v/renamed?label=Rust)](https://crates.io/crates/renamed)
[![Swift](https://img.shields.io/github/v/tag/renamed-to/renamed-sdk?filter=swift/*&label=Swift)](https://github.com/renamed-to/renamed-sdk)
[![PHP](https://img.shields.io/packagist/v/renamed/sdk?label=PHP)](https://packagist.org/packages/renamed/sdk)

## Installation

### TypeScript / JavaScript

```bash
npm install @renamed/sdk
# or
pnpm add @renamed/sdk
# or
yarn add @renamed/sdk
```

### Python

```bash
pip install renamed
# or
poetry add renamed
# or
uv add renamed
```

### Go

```bash
go get github.com/renamed-to/renamed-sdk/sdks/go
```

### Java

**Maven:**
```xml
<dependency>
    <groupId>to.renamed</groupId>
    <artifactId>renamed-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'to.renamed:renamed-sdk:0.1.0'
```

### C# / .NET

```bash
dotnet add package Renamed.Sdk
# or
Install-Package Renamed.Sdk
```

### Ruby

```bash
gem install renamed
# or add to Gemfile:
gem 'renamed'
```

### Rust

Add to your `Cargo.toml`:
```toml
[dependencies]
renamed = "0.1"
tokio = { version = "1", features = ["full"] }
```

### Swift

Add to your `Package.swift`:
```swift
dependencies: [
    .package(url: "https://github.com/renamed-to/renamed-sdk", from: "0.1.0")
]
```

### PHP

```bash
composer require renamed/sdk
```

## Quick Start

Get your API key at [renamed.to/settings](https://www.renamed.to/settings).

### TypeScript

```typescript
import { RenamedClient } from '@renamed/sdk';

const client = new RenamedClient({ apiKey: 'rt_...' });

const result = await client.rename('invoice.pdf');
console.log(result.suggestedFilename);
// "2025-01-15_AcmeCorp_INV-12345.pdf"
```

### Python

```python
from renamed import RenamedClient

client = RenamedClient(api_key='rt_...')

result = client.rename('invoice.pdf')
print(result.suggested_filename)
# "2025-01-15_AcmeCorp_INV-12345.pdf"
```

### Go

```go
import "github.com/renamed-to/renamed-sdk/sdks/go/renamed"

client := renamed.NewClient("rt_...")

result, _ := client.Rename(ctx, "invoice.pdf", nil)
fmt.Println(result.SuggestedFilename)
// "2025-01-15_AcmeCorp_INV-12345.pdf"
```

### Java

```java
import to.renamed.sdk.*;

RenamedClient client = new RenamedClient("rt_...");

RenameResult result = client.rename(Path.of("invoice.pdf"), null);
System.out.println(result.getSuggestedFilename());
// "2025-01-15_AcmeCorp_INV-12345.pdf"
```

### C#

```csharp
using Renamed.Sdk;

using var client = new RenamedClient("rt_...");

var result = await client.RenameAsync("invoice.pdf");
Console.WriteLine(result.SuggestedFilename);
// "2025-01-15_AcmeCorp_INV-12345.pdf"
```

### Ruby

```ruby
require 'renamed'

client = Renamed::Client.new(api_key: 'rt_...')

result = client.rename('invoice.pdf')
puts result.suggested_filename
# "2025-01-15_AcmeCorp_INV-12345.pdf"
```

### Rust

```rust
use renamed::RenamedClient;

let client = RenamedClient::new("rt_...");

let result = client.rename("invoice.pdf", None).await?;
println!("{}", result.suggested_filename);
// "2025-01-15_AcmeCorp_INV-12345.pdf"
```

### Swift

```swift
import Renamed

let client = try RenamedClient(apiKey: "rt_...")

let file = try FileInput(url: URL(fileURLWithPath: "invoice.pdf"))
let result = try await client.rename(file: file)
print(result.suggestedFilename)
// "2025-01-15_AcmeCorp_INV-12345.pdf"
```

### PHP

```php
use Renamed\Client;

$client = new Client('rt_...');

$result = $client->rename('invoice.pdf');
echo $result->suggestedFilename;
// "2025-01-15_AcmeCorp_INV-12345.pdf"
```

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
- [Java SDK](./sdks/java/README.md)
- [C# SDK](./sdks/csharp/README.md)
- [Ruby SDK](./sdks/ruby/README.md)
- [Rust SDK](./sdks/rust/README.md)
- [Swift SDK](./sdks/swift/README.md)
- [PHP SDK](./sdks/php/README.md)

## License

MIT
