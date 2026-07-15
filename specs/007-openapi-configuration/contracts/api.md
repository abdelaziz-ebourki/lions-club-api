# API Contract: OpenAPI Spec

## Contract Source

The OpenAPI specification is **auto-generated** at runtime by SpringDoc from controller annotations. There is no static spec file to maintain — the source of truth is the code annotations on all `@RestController` classes.

## Endpoints

| Path | Content | Purpose |
|------|---------|---------|
| `GET /api-docs` | OpenAPI 3.0 JSON | Raw spec consumed by CI, client generators, -e2e tests |
| `GET /swagger-ui.html` | HTML page | Interactive documentation for developers |

## Security Scheme

| Field | Value |
|-------|-------|
| Name | `cookie-jwt` |
| Type | API Key |
| In | Cookie |
| Cookie Name | `auth_token` |

## Consumers

- **-e2e repo**: `GET /api-docs` for contract testing (checking that endpoints match expected behavior)
- **-ui repo**: `GET /api-docs` for API client code generation and developer reference
- **Developers**: `GET /swagger-ui.html` for browsing and testing endpoints manually
