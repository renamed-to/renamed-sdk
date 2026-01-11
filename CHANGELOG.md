# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2025-01-10

### Added

- Initial release of the renamed.to SDK
- Support for 9 programming languages:
  - TypeScript/JavaScript (`@renamed/sdk` on npm)
  - Python (`renamed` on PyPI)
  - Go (`github.com/renamed-to/renamed-sdk/sdks/go`)
  - Java (`to.renamed:renamed-sdk` on Maven Central)
  - C# (`Renamed.Sdk` on NuGet)
  - Ruby (`renamed` on RubyGems)
  - Rust (`renamed` on crates.io)
  - Swift (via Swift Package Manager)
  - PHP (`renamed/sdk` on Packagist)

### Features

- **File Renaming**: AI-powered intelligent filename suggestions
  - Support for PDF, JPEG, PNG, and TIFF files
  - Custom filename templates
  - Confidence scores for suggestions
  - Folder path recommendations

- **PDF Splitting**: Automatic document boundary detection
  - Multiple split modes: auto, pages, blank page detection
  - Async job handling with progress callbacks
  - Download URLs for split documents

- **Data Extraction**: Extract structured data from documents
  - Custom extraction prompts
  - JSON schema validation
  - Support for complex nested data structures

- **User Management**: Account and credit information
  - Credit balance checking
  - Team information access

### Documentation

- Comprehensive README with quick start guides
- Language-specific documentation in each SDK
- Example code for common use cases
- OpenAPI specification included
