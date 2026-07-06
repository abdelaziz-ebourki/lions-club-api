# API Documentation Requirements Checklist: Project Foundation

**Purpose**: Validate the quality, clarity, and completeness of API documentation requirements (SpringDoc OpenAPI, Swagger UI, profile gating, security scheme)
**Created**: 2026-07-06
**Feature**: specs/001-project-foundation/spec.md

## Requirement Completeness

- [x] CHK001 - Are the exact OpenAPI title, version string, and description values specified in requirements? [Completeness, Spec §FR-005]
- [x] CHK002 - Is the JWT bearer security scheme fully specified (type, scheme, bearerFormat, description) in requirements? [Completeness, Spec §FR-008]
- [x] CHK003 - Are requirements defined for the OpenAPI servers/base URL configuration? [Completeness, Gap]
- [x] CHK004 - Are requirements defined for API versioning strategy in documentation? [Completeness, Gap]
- [x] CHK005 - Are requirements specified for SpringDoc configuration in each profile (dev vs test vs prod)? [Completeness, Spec §FR-006, §FR-007]
- [x] CHK006 - Are requirements defined for customizing the Swagger UI beyond default settings (theme, logo, contact)? [Completeness, Gap]

## Requirement Clarity

- [x] CHK007 - Is "profile gating" defined as Spring @Profile annotation, SpringDoc property, or both? [Clarity, Spec §FR-006]
- [x] CHK008 - Is the relationship between /swagger-ui.html and /v3/api-docs endpoints clearly specified? [Clarity, Spec §FR-006 vs §FR-007]
- [x] CHK009 - Is "global security scheme" clarified as applying to all endpoints vs configurable per-endpoint? [Clarity, Spec §FR-008]
- [x] CHK010 - Is the title "Lions Club FSBM API" confirmed as the canonical documentation title? [Clarity, Spec §FR-005]

## Requirement Consistency

- [x] CHK011 - Does the OpenAPI contract in contracts/ match the SpringDoc configuration requirements in FR-005 through FR-008? [Consistency, Spec §FR-005 vs contracts/openapi-base.yaml]
- [x] CHK012 - Do SpringDoc requirements align with the constitution's Security by Design principle (Principle II)? [Consistency, Spec §FR-006 vs Constitution II]
- [x] CHK013 - Are SpringDoc profile exposures consistent between FR-006 (UI endpoint) and FR-007 (API docs endpoint)? [Consistency, Spec §FR-006 vs §FR-007]

## Acceptance Criteria Quality

- [x] CHK014 - Can "Swagger UI page loads" (US3/AC1) be objectively verified with a specific HTTP status code or page content check? [Measurability, Spec §US3/AC1]
- [x] CHK015 - Is "immediately after application startup" (SC-004) quantified with a specific timeout threshold? [Measurability, Spec §SC-004]
- [x] CHK016 - Is "API controllers are documented" (US3/AC2) testable before controllers exist? [Measurability, Spec §US3/AC2]
- [x] CHK017 - Can the prod profile SpringDoc disablement be verified objectively (404 vs redirect vs empty page)? [Measurability, Spec §FR-006]

## Scenario Coverage

- [x] CHK018 - Are requirements defined for SpringDoc behavior in profiles other than dev/test/prod (e.g., staging, custom profiles)? [Coverage, Spec §FR-006]
- [x] CHK019 - Are requirements defined for OpenAPI documentation when no controllers exist? [Coverage, Spec §Edge Cases]
- [x] CHK020 - Are requirements defined for documentation of error responses and exception schemas? [Coverage, Gap]

## Edge Case & Failure Coverage

- [x] CHK021 - Is the expected SpringDoc behavior defined when the application runs without a database connection? [Edge Case, Gap]
- [x] CHK022 - Is the expected behavior defined when SpringDoc conflicts with security filters (authentication required for docs)? [Edge Case, Gap]
- [x] CHK023 - Is the expected behavior defined when OpenAPI schema generation fails due to invalid annotations? [Edge Case, Gap]
- [x] CHK024 - Are requirements defined for SpringDoc behavior when deployed behind a reverse proxy with path prefix? [Edge Case, Gap]

## Non-Functional Requirements

- [x] CHK025 - Are performance requirements defined for OpenAPI schema generation at startup? [Performance, Gap]
- [x] CHK026 - Are security requirements defined for API docs in non-production environments (CORS, authentication on docs)? [Security, Gap]

## Deferred to Later Phases

Items intentionally out of scope for Project Foundation:

- CHK004 — API versioning strategy: will be defined when the first versioned API endpoint is added (Phase 2+)
- CHK006 — Custom Swagger UI beyond defaults (logo, theme, contact): cosmetic customization deferred
- CHK020 — Error response schema documentation: will be added when controllers and exceptions are defined (Phase 2+)
- CHK024 — Reverse proxy path prefix: deployment concern, deferred until production deployment
- CHK025 — SpringDoc schema generation performance: negligible for current scope
- CHK028 — All endpoints annotated with OpenAPI annotations: expectation, enforced via code review (Phase 2+)
- CHK031 — JWT global vs public endpoint conflict: will be resolved when auth endpoints exist (Phase 2)

## Accepted As-Is

Items accepted without changes (low severity, adequate for foundation phase):

- CHK009 — Global vs per-endpoint security scheme: "global" is accurate for foundation phase; per-endpoint overrides are Phase 2+
- CHK015 — "Immediately after startup": acceptable for foundation phase; actionable in seconds
- CHK016 — US3/AC2 testable before controllers: design intent; documented in spec "when API controllers exist"
- CHK018 — Profiles beyond dev/test/prod: only these three profiles exist; no others planned
- CHK019 — SpringDoc when no controllers: profile gating handles this; docs endpoint works with empty paths
- CHK021 — SpringDoc without DB: not affected; docs are in-memory, no DB dependency
- CHK022 — Security filters vs docs: handled by profile gating + Spring Security configuration in Phase 2
- CHK023 — Schema generation failure: unlikely with standard annotations; accept as operational risk
- CHK026 — CORS/auth on docs: profile gating (dev/test only) provides sufficient protection for foundation phase
- CHK027 — SpringDoc version: documented in plan.md dependencies

## Dependencies & Assumptions

- [x] CHK027 - Is the dependency on SpringDoc library version documented as a requirement assumption? [Assumption, Gap]
- [x] CHK028 - Is the assumption that all endpoints are annotated with OpenAPI annotations documented? [Assumption, Gap]
- [x] CHK029 - Is the assumption that the contract (openapi-base.yaml) will be kept in sync with implementation documented? [Assumption, Spec §Constitution I]

## Ambiguities & Conflicts

- [x] CHK030 - Is there ambiguity about whether "dev and test profiles only" means both UI and API docs are disabled in prod, or only one of them? [Ambiguity, Spec §FR-006 vs §FR-007]
- [x] CHK031 - Does FR-008 ("JWT bearer token as a global security scheme") conflict with unauthenticated endpoints (e.g., health, login)? [Conflict, Spec §FR-008 vs §FR-008a]

(End of file - total 63 lines)
