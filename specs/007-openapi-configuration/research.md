# Research: SpringDoc OpenAPI Configuration

## Summary

This feature is primarily complete on `main`. The remaining work is a small set of additions to fully satisfy issue #7 requirements.

## Current State Audit

| Check | Status | Detail |
|-------|--------|--------|
| SpringDoc dependency | ✅ Present | `springdoc-openapi-starter-webmvc-ui:2.8.6` in pom.xml |
| OpenApiConfig bean | ✅ Present | Cookie-JWT security scheme, title/version/description configured |
| Swagger UI path config | ✅ Present | `springdoc.swagger-ui.path: /swagger-ui.html` in application.yml |
| API docs path config | ❌ Missing | `springdoc.api-docs.path: /api-docs` not set (defaults to `/v3/api-docs`) |
| SecurityConfig permit | ✅ Present | `/swagger-ui/**` and `/v3/api-docs/**` are permitAll |
| EventController annotations | ✅ Complete | All 5 endpoints have @Operation and @ApiResponse |
| AuthController annotations | ❌ Partial | `login()`, `register()`, `logout()` lack Swagger annotations |
| OpenAPI version | ❌ Needs fix | Currently `0.0.1`, should be `0.1.0` per issue #7 |
| Dev profile | ✅ Correct | Both api-docs and swagger-ui enabled |
| Prod profile | ✅ Correct | Both api-docs and swagger-ui disabled |
| Existing tests | ✅ Present | OpenApiConfigTest and SecurityConfigTest exist |

## Decisions

- **Decision**: Add `springdoc.api-docs.path: /api-docs` to `application.yml`
  - **Rationale**: Issue #7 specifies `GET /api-docs` as the spec endpoint. Currently defaults to `/v3/api-docs`.
  - **Alternatives considered**: Keep `/v3/api-docs` default — rejected because issue explicitly requires `/api-docs`.

- **Decision**: Add `@Operation` + `@ApiResponse` annotations to `login()`, `register()`, `logout()` in AuthController
  - **Rationale**: Without these, the spec is incomplete for the auth endpoints.
  - **Alternatives considered**: None — issue #7 requires all endpoints to be documented.

- **Decision**: Bump version from `0.0.1` to `0.1.0` in OpenApiConfig
  - **Rationale**: Issue #7 specifies `0.1.0`. The project is past initial scaffolding.
  - **Alternatives considered**: Keep `0.0.1` — inconsistent with issue requirements.

## Verification Plan

1. `GET /api-docs` returns valid OpenAPI JSON
2. `GET /swagger-ui.html` renders Swagger UI
3. All 10 endpoints appear in spec with descriptions
4. Cookie-JWT security scheme is documented
5. Unauthenticated access to doc paths works (no redirect)
