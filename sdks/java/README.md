# Renamed.to Java SDK

Official Java SDK for the [renamed.to](https://renamed.to) API - AI-powered file renaming, PDF splitting, and data extraction.

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>to.renamed</groupId>
    <artifactId>renamed-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'to.renamed:renamed-sdk:0.1.0'
```

## Quick Start

```java
import to.renamed.sdk.*;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) {
        // Create a client with your API key
        RenamedClient client = new RenamedClient("rt_your_api_key");

        // Rename a file
        RenameResult result = client.rename(Path.of("invoice.pdf"), null);
        System.out.println("Suggested filename: " + result.getSuggestedFilename());

        // Check your credits
        User user = client.getUser();
        System.out.println("Credits remaining: " + user.getCredits());
    }
}
```

## Usage

### Creating a Client

```java
// Simple initialization
RenamedClient client = new RenamedClient("rt_your_api_key");

// With custom configuration
RenamedClient client = RenamedClient.builder("rt_your_api_key")
    .baseUrl("https://custom-api.example.com")
    .timeout(Duration.ofSeconds(60))
    .maxRetries(3)
    .build();
```

### Renaming Files

The `rename` method uses AI to suggest a better filename based on the document content.

```java
// Basic rename
RenameResult result = client.rename(Path.of("document.pdf"), null);
System.out.println("Original: " + result.getOriginalFilename());
System.out.println("Suggested: " + result.getSuggestedFilename());
System.out.println("Confidence: " + result.getConfidence());

// With custom template
RenameOptions options = new RenameOptions()
    .withTemplate("{date}_{type}_{description}");
RenameResult result = client.rename(Path.of("document.pdf"), options);

// From byte array
byte[] content = Files.readAllBytes(Path.of("document.pdf"));
RenameResult result = client.rename(content, "document.pdf", null);

// From InputStream
try (InputStream is = new FileInputStream("document.pdf")) {
    RenameResult result = client.rename(is, "document.pdf", null);
}
```

### Splitting PDFs

The `pdfSplit` method splits a multi-page PDF into separate documents. This is an async operation.

```java
// Split with automatic document boundary detection
PdfSplitOptions options = new PdfSplitOptions()
    .withMode(SplitMode.AUTO);

AsyncJob job = client.pdfSplit(Path.of("multi-page.pdf"), options);

// Wait for completion with progress updates
PdfSplitResult result = job.await(status -> {
    System.out.println("Progress: " + status.getProgress() + "%");
});

// Download the split documents
for (SplitDocument doc : result.getDocuments()) {
    System.out.println("Document " + doc.getIndex() + ": " + doc.getFilename());
    byte[] content = client.downloadFile(doc.getDownloadUrl());
    Files.write(Path.of(doc.getFilename()), content);
}
```

Split modes:
- `SplitMode.AUTO` - AI detects document boundaries automatically
- `SplitMode.PAGES` - Split every N pages (use `withPagesPerSplit()`)
- `SplitMode.BLANK` - Split at blank pages

```java
// Split every 5 pages
PdfSplitOptions options = new PdfSplitOptions()
    .withMode(SplitMode.PAGES)
    .withPagesPerSplit(5);
```

### Extracting Data

The `extract` method extracts structured data from documents using AI.

```java
// Extract with a natural language prompt
ExtractOptions options = new ExtractOptions()
    .withPrompt("Extract the invoice number, date, and total amount");

ExtractResult result = client.extract(Path.of("invoice.pdf"), options);

Map<String, Object> data = result.getData();
System.out.println("Invoice Number: " + data.get("invoiceNumber"));
System.out.println("Date: " + data.get("date"));
System.out.println("Total: $" + data.get("total"));
System.out.println("Confidence: " + result.getConfidence());

// Using helper methods
String invoiceNumber = result.getString("invoiceNumber");
Double total = result.getDouble("total");
```

You can also use a JSON schema for more precise extraction:

```java
Map<String, Object> schema = Map.of(
    "invoiceNumber", Map.of("type", "string"),
    "date", Map.of("type", "string", "format", "date"),
    "lineItems", Map.of(
        "type", "array",
        "items", Map.of(
            "type", "object",
            "properties", Map.of(
                "description", Map.of("type", "string"),
                "quantity", Map.of("type", "integer"),
                "price", Map.of("type", "number")
            )
        )
    ),
    "total", Map.of("type", "number")
);

ExtractOptions options = new ExtractOptions()
    .withSchema(schema);

ExtractResult result = client.extract(Path.of("invoice.pdf"), options);
```

### Getting User Info

```java
User user = client.getUser();
System.out.println("ID: " + user.getId());
System.out.println("Email: " + user.getEmail());
System.out.println("Credits: " + user.getCredits());

if (user.getTeam() != null) {
    System.out.println("Team: " + user.getTeam().getName());
}
```

## Error Handling

The SDK throws specific exceptions for different error conditions:

```java
try {
    RenameResult result = client.rename(Path.of("document.pdf"), null);
} catch (AuthenticationError e) {
    // Invalid or missing API key (401)
    System.err.println("Authentication failed: " + e.getMessage());
} catch (InsufficientCreditsError e) {
    // Account has no credits (402)
    System.err.println("No credits remaining: " + e.getMessage());
} catch (ValidationError e) {
    // Invalid request parameters (400/422)
    System.err.println("Validation error: " + e.getMessage());
    System.err.println("Details: " + e.getDetails());
} catch (RateLimitError e) {
    // Rate limit exceeded (429)
    System.err.println("Rate limited. Retry after " + e.getRetryAfter() + " seconds");
} catch (NetworkError e) {
    // Network connection failure
    System.err.println("Network error: " + e.getMessage());
} catch (JobError e) {
    // Async job failure
    System.err.println("Job " + e.getJobId() + " failed: " + e.getMessage());
} catch (RenamedError e) {
    // Base exception for all other errors
    System.err.println("Error [" + e.getCode() + "]: " + e.getMessage());
}
```

## Exception Hierarchy

```
RenamedError (base)
├── AuthenticationError (401)
├── InsufficientCreditsError (402)
├── ValidationError (400, 422)
├── RateLimitError (429)
├── NetworkError
└── JobError
```

## Supported File Formats

- PDF (`.pdf`)
- JPEG (`.jpg`, `.jpeg`)
- PNG (`.png`)
- TIFF (`.tiff`, `.tif`)

## Building from Source

```bash
# Clone the repository
git clone https://github.com/renamed-to/renamed-sdk.git
cd renamed-sdk/sdks/java

# Build the SDK
mvn clean install

# Run tests
mvn test
```

## License

MIT License - see [LICENSE](LICENSE) for details.

## Support

- Documentation: https://docs.renamed.to
- Issues: https://github.com/renamed-to/renamed-sdk/issues
- Email: support@renamed.to
