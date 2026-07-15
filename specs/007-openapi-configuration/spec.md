# Feature Specification: SpringDoc OpenAPI Configuration

**Feature Branch**: `007-openapi-configuration`

**Created**: 2026-07-15

**Status**: Draft

**Input**: User description: "Configure SpringDoc to generate OpenAPI 3.0 spec from annotations. The generated spec becomes the contract shared with -ui and -e2e repos. Swagger UI must be accessible without authentication."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Developer views API documentation in Swagger UI (Priority: P1)

As a frontend or e2e developer integrating with the API, I want to browse all available endpoints with their request/response schemas so I can understand what the API offers without reading source code or asking a backend developer.

**Why this priority**: This is the primary consumption path for the API documentation — the UI and e2e repos rely on this being available from day one of integration.

**Independent Test**: Can be fully tested by navigating to `/swagger-ui.html` and verifying all CRUD endpoints are listed with their schemas. Delivers immediate value to all API consumers.

**Acceptance Scenarios**:

1. **Given** the API is running, **When** I navigate to `/swagger-ui.html`, **Then** the Swagger UI page loads with all documented endpoints listed
2. **Given** the Swagger UI is open, **When** I expand an endpoint, **Then** I see its HTTP method, path, request parameters, and response schemas
3. **Given** the Swagger UI is open, **When** I inspect the security section, **Then** I see cookie-based authentication is the required scheme
4. **Given** I am not logged in, **When** I navigate to `/swagger-ui.html`, **Then** the page loads without redirecting to a login page

---

### User Story 2 - Developer retrieves raw OpenAPI spec programmatically (Priority: P1)

As an automated tool or CI pipeline, I want to fetch the raw OpenAPI JSON spec so I can generate client code, run contract tests, or validate API changes.

**Why this priority**: The e2e and UI repos need the raw spec as their source of truth. Without automated access, contract testing cannot be automated.

**Independent Test**: Can be fully tested by sending `GET /api-docs` and validating the response is valid JSON conforming to the OpenAPI 3.0 schema.

**Acceptance Scenarios**:

1. **Given** the API is running, **When** I send `GET /api-docs`, **Then** I receive a 200 response with `Content-Type: application/json`
2. **Given** I receive the OpenAPI JSON, **When** I inspect the `info` object, **Then** it contains `title`, `version`, and `description` fields
3. **Given** I inspect the spec, **When** I look at the `paths` section, **Then** all registered API endpoints are present with their request/response schemas
4. **Given** I inspect the `components` section, **When** I look at security schemes, **Then** cookie-based authentication is defined

---

### User Story 3 - Backend developer validates endpoint documentation completeness (Priority: P2)

As a backend developer, I want to verify that every controller endpoint has `@Operation` and `@ApiResponse` annotations so the generated spec accurately represents the API contract.

**Why this priority**: Incomplete docs reduce trust in the spec. This is a quality gate that becomes valuable once the basic doc setup works.

**Independent Test**: Can be tested by comparing the OpenAPI spec paths against the known set of controller endpoints and checking each path has meaningful descriptions and response schemas.

**Acceptance Scenarios**:

1. **Given** the OpenAPI spec is generated, **When** I review each path entry, **Then** it includes a `summary` or `description` explaining the endpoint's purpose
2. **Given** a path entry with a response body, **When** I inspect its `responses` section, **Then** each response code (200, 201, 400, 401, 403, 404, 500) has a schema reference or description

---

### Edge Cases

- What happens when SpringDoc dependency is missing from the classpath? The application should fail to start with a clear error.
- What happens if a controller method lacks `@Operation`? The endpoint still appears in the spec but without a meaningful description — this is acceptable but flagged for improvement.
- What happens if the security filter has not been configured to allow doc paths? The Swagger UI would fail to load for unauthenticated users. The doc paths must be explicitly permitted.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose an OpenAPI 3.0 JSON spec at a documented path
- **FR-002**: System MUST serve a Swagger UI page that renders the OpenAPI spec as interactive documentation
- **FR-003**: All API endpoints MUST include `@Operation` summaries and `@ApiResponse` annotations for all relevant HTTP status codes
- **FR-004**: The API spec MUST include the security scheme definition for cookie-based authentication
- **FR-005**: Swagger UI and the raw OpenAPI spec MUST be accessible without authentication
- **FR-006**: The OpenAPI `info` section MUST include the API title, version, and description
- **FR-007**: Each endpoint's request body and response schemas MUST be reflected in the generated spec (DTOs must generate proper schema definitions)

### Key Entities *(include if feature involves data)*

No new data entities are introduced. This feature is purely configuration and documentation metadata.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer can load `GET /api-docs` without any authentication and receive valid OpenAPI 3.0 JSON
- **SC-002**: A developer can load `/swagger-ui.html` in a browser without any authentication and see all endpoints listed
- **SC-003**: Every controller endpoint appears in the spec with its path, HTTP method, description, and response schemas
- **SC-004**: The spec's security section documents the cookie-based auth scheme used by the API
- **SC-005**: New endpoints added in future features automatically appear in the spec when annotated following the established pattern

## Assumptions

- SpringDoc OpenAPI dependency is already declared in the project's build file
- The existing `application.yml` already contains `springdoc` configuration for paths and is correct as-is
- All existing controller methods will be annotated as part of this work
- The existing `SecurityConfig` already handles endpoint-level access; doc paths will be added to the permit list
- The `/swagger-ui.html` and `/api-docs` paths will be used as the standard documentation endpoints
