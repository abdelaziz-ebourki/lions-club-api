# Implementation Plan: SpringDoc OpenAPI Configuration

**Branch**: `007-openapi-configuration` | **Date**: 2026-07-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-openapi-configuration/spec.md`

## Summary

Add remaining SpringDoc OpenAPI configuration items so the auto-generated OpenAPI 3.0 spec at `/api-docs` is complete with all 10 endpoints documented, the cookie-JWT security scheme is visible, and Swagger UI is accessible without authentication. Most infrastructure exists on `main` already.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui:2.8.6`) — already in pom.xml

**Storage**: N/A (no data layer changes)

**Testing**: JUnit 5 + SpringBootTest + MockMvc (existing `OpenApiConfigTest` and `SecurityConfigTest`)

**Target Platform**: Linux server (Docker), Spring Boot 3.4.4

**Project Type**: REST web service (Spring Boot)

**Performance Goals**: N/A (configuration only, no runtime impact)

**Constraints**: OpenAPI docs disabled in production via `application-prod.yml` (existing), only enabled in dev/test profiles

**Scale/Scope**: Small — 4 concrete changes to existing files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **G1 - API-First**: Are API contracts (OpenAPI) designed and reviewed before implementation?
- [x] **G2 - Security by Design**: Is authentication/authorization designed for every protected endpoint?
- [x] **G3 - Test-First (NON-NEGOTIABLE)**: Are failing tests written before implementation code?
- [x] **G4 - DB Migration Rigor**: Are schema changes planned via Flyway migrations (no manual DDL)?
- [x] **G5 - Clean Architecture**: Does the design respect Controller → Service → Repository layering?

**Gate Verdict**: ALL PASS — no violations. Proceeding to Phase 0.

### Post-Design Re-evaluation

- [x] **G1 - API-First**: The OpenAPI spec IS the contract, auto-generated from code. Already in place.
- [x] **G2 - Security by Design**: Security scheme (cookie-JWT) already documented in OpenApiConfig.
- [x] **G3 - Test-First**: Existing tests (OpenApiConfigTest, SecurityConfigTest) will be updated. New tests for AuthController annotations will be added.
- [x] **G4 - DB Migration Rigor**: No schema changes.
- [x] **G5 - Clean Architecture**: No layering changes. Annotations live on controllers (outer layer) only.

## Project Structure

### Documentation (this feature)

```text
specs/007-openapi-configuration/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (N/A — no data)
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── api.md           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (changes)

```text
src/main/java/com/lionsclub/api/
├── config/
│   └── OpenApiConfig.java        # [EDIT] Bump version 0.0.1 → 0.1.0
└── web/
    └── AuthController.java       # [EDIT] Add @Operation + @ApiResponse to login/register/logout

src/main/resources/
└── application.yml               # [EDIT] Add springdoc.api-docs.path: /api-docs

src/test/java/com/lionsclub/api/
├── config/
│   └── OpenApiConfigTest.java    # [EDIT] Update expected version in test
└── web/
    └── AuthControllerTest.java   # [EDIT] Add test verifying Swagger annotations on auth endpoints
```

**Structure Decision**: Single project (Spring Boot monolith). Only 4 files edited; no new files.

## Complexity Tracking

No complexity justification needed — all constitution gates pass.
