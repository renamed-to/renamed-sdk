# Contributing

We welcome contributions to the renamed.to SDKs!

## Development Setup

### TypeScript

```bash
cd sdks/typescript
npm install
npm run build
npm test
```

### Python

```bash
cd sdks/python
pip install -e ".[dev]"
pytest
```

### Go

```bash
cd sdks/go
go build ./...
go test ./...
```

## Pull Request Process

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests for all affected SDKs
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## Coding Standards

### All Languages

- Keep the API consistent across all SDKs
- Add tests for new functionality
- Update documentation when changing public APIs
- Use meaningful variable and function names

### TypeScript

- Use strict TypeScript
- Export types alongside functions
- Use JSDoc for public APIs

### Python

- Follow PEP 8
- Use type hints
- Use Pydantic for models

### Go

- Follow Go conventions
- Use standard library where possible
- Add godoc comments to public APIs

## Versioning

All SDKs share the same version number. When releasing:

1. Update version in all SDK package files
2. Create a git tag: `git tag v1.0.0`
3. Push the tag: `git push origin v1.0.0`

The CI/CD pipeline will automatically publish to npm, PyPI, and Go modules.

## Questions?

Open an issue or reach out at [renamed.to](https://www.renamed.to).
