# OpenAPI Specification

This directory contains the OpenAPI 3.0 specification for the renamed.to API.

## Files

- `openapi.json` - The complete OpenAPI specification

## Usage

You can use this spec to:
- Generate additional SDK clients
- Validate API responses
- Generate API documentation
- Import into tools like Postman or Swagger UI

## Endpoints

The API provides the following main endpoints:

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/user` | Get current user profile |
| `GET /api/v1/credits` | Get credit balance |
| `POST /api/v1/rename` | Rename a file using AI |
| `POST /api/v1/pdf-split` | Split a PDF into documents |
| `POST /api/v1/extract` | Extract structured data |
| `GET /api/v1/templates` | List filename templates |

## Updating

To update the OpenAPI spec:

```bash
curl -o openapi/openapi.json https://www.renamed.to/api/openapi.json
```

## Documentation

For full API documentation, visit:
- [API Documentation](https://www.renamed.to/docs/api-docs)
