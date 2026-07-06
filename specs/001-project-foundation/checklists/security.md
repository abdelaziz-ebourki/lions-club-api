# Security Requirements Checklist: Project Foundation

**Purpose**: Validate the quality, clarity, and completeness of security-related requirements (JWT auth scheme, profile isolation, data protection, environment gating)
**Created**: 2026-07-06
**Feature**: specs/001-project-foundation/spec.md

## Requirement Completeness

- [x] CHK001 - Are JWT authentication requirements fully specified (token format, signing algorithm, expiration, refresh mechanism)? [Completeness, Spec §FR-008]
- [x] CHK002 - Are role-based authorization requirements defined for the users table role column? [Completeness, Spec §FR-004]
- [x] CHK003 - Are requirements defined for profile-specific security configuration (dev relaxed vs prod strict)? [Completeness, Spec §FR-010]
- [x] CHK004 - Are CORS requirements specified for different environments? [Completeness, Gap]
- [x] CHK005 - Are password hashing requirements defined (algorithm, cost factor, storage format)? [Completeness, Gap]
- [x] CHK006 - Are requirements defined for secrets management (JWT secret, DB password) per environment? [Completeness, Spec §FR-011]
- [x] CHK007 - Are requirements specified for API endpoint access control (public vs protected endpoints)? [Completeness, Gap]

## Requirement Clarity

- [x] CHK008 - Is "global security scheme" in FR-008 clearly defined as applying to all API endpoints or configurable per-endpoint? [Clarity, Spec §FR-008]
- [x] CHK009 - Is the JWT bearerFormat explicitly specified (JWT vs opaque token)? [Clarity, Spec §FR-008]
- [x] CHK010 - Is "dev and test profiles only" precisely defined for SpringDoc security gating (which annotations/properties)? [Clarity, Spec §FR-006]
- [x] CHK011 - Are "required environment variables" in .env.example enumerated with their security sensitivity levels? [Clarity, Spec §FR-011]

## Requirement Consistency

- [x] CHK012 - Does the JWT requirement (FR-008) align with the security scheme in contracts/openapi-base.yaml? [Consistency, Spec §FR-008 vs contracts/openapi-base.yaml]
- [x] CHK013 - Do security gating requirements align with the constitution's Security by Design principle (Principle II)? [Consistency, Spec §FR-006 vs Constitution II]
- [x] CHK014 - Are SpringDoc profile gating requirements consistent with the Clarifications section (docs not accessible in production)? [Consistency, Spec §FR-006 vs §Clarifications]
- [x] CHK015 - Does the prod profile secret requirement (env-based) align with the .env.example secret defaults? [Consistency, Spec §FR-010 vs §FR-011]

## Acceptance Criteria Quality

- [x] CHK016 - Can "SpringDoc hidden in prod" be objectively verified? [Measurability, Spec §US3]
- [x] CHK017 - Is there a measurable acceptance criterion for JWT token validation behavior? [Measurability, Gap]
- [x] CHK018 - Can "secure by design" be tested with specific security verification steps? [Measurability, Constitution II]

## Scenario Coverage

- [x] CHK019 - Are requirements defined for authentication failure scenarios (invalid token, expired token, malformed token)? [Coverage, Gap]
- [x] CHK020 - Are requirements defined for authorization failure (valid token but insufficient role)? [Coverage, Gap]
- [x] CHK021 - Are requirements defined for unauthenticated access to public endpoints (health check)? [Coverage, Gap]
- [x] CHK022 - Are requirements defined for security header configuration (X-Content-Type-Options, HSTS, CSP)? [Coverage, Gap]

## Edge Case & Failure Coverage

- [x] CHK023 - Is the expected behavior defined when the JWT secret is not configured in production? [Edge Case, Gap]
- [x] CHK024 - Is the expected behavior defined when JWT signing key rotation is needed? [Edge Case, Gap]
- [x] CHK025 - Is the expected behavior defined when SpringDoc security scheme configuration conflicts with endpoint-level security annotations? [Edge Case, Gap]
- [x] CHK026 - Is the expected behavior defined for token leakage scenarios (token in URLs, logs)? [Edge Case, Gap]

## Non-Functional Requirements

- [x] CHK027 - Are requirements defined for JWT token expiration durations (access vs refresh tokens)? [Security, Gap]
- [x] CHK028 - Are requirements defined for rate limiting on authentication endpoints? [Security, Gap]
- [x] CHK029 - Are requirements defined for audit logging of security-relevant events (login, access denied)? [Observability, Gap]

## Accepted As-Is

Items accepted without changes (low severity, adequate for foundation phase):

- CHK008 — Global security scope: "global" is accurate; endpoint exceptions are Phase 2+
- CHK010 — Dev/test gating mechanism: clearly specified as `@Profile` annotation + SpringDoc properties
- CHK011 — Env variable sensitivity: `.env.example` names are self-explanatory; sensitivity levels not required at this phase
- CHK018 — "Secure by design" testability: constitutional principle, verified via code review
- CHK032 — HTTPS termination: known deployment assumption; not an application concern

## Deferred to Later Phases

Items intentionally out of scope for Project Foundation:

- CHK001 — JWT auth details (format, signing algorithm, expiration, refresh): full auth implementation is Phase 2
- CHK002 — Role-based authorization: Phase 2 when User entity and auth endpoints are implemented
- CHK004 — CORS requirements: will be configured when frontend integration begins (Phase 2+)
- CHK005 — Password hashing (algorithm, cost factor): Phase 2 auth implementation
- CHK007 — Endpoint access control: no endpoints exist yet; will be defined per endpoint in Phase 2+
- CHK017 — JWT token validation acceptance criteria: Phase 2
- CHK019-022 — Auth failure, authorization failure, public endpoint access, security headers: all Phase 2+ concerns
- CHK023-026 — JWT edge cases (missing secret, key rotation, token leakage): Phase 2+
- CHK027-029 — JWT performance, rate limiting, audit logging: Phase 2+ operational concerns
- CHK032 — HTTPS termination: platform/deployment concern, not application-level

## Dependencies & Assumptions

- [x] CHK030 - Is the dependency on Auth0 java-jwt 4.5.0 for JWT handling documented in requirements? [Dependency, Gap]
- [x] CHK031 - Is the assumption that production secrets are provided via environment variables (never in config files) documented? [Assumption, Spec §FR-011]
- [x] CHK032 - Is the assumption that HTTPS will terminate at a reverse proxy (not in application) documented? [Assumption, Gap]
- [x] CHK033 - Is the dependency on Spring Security for authentication/authorization explicitly called out? [Dependency, Gap]

## Ambiguities & Conflicts

- [x] CHK034 - Does FR-008 requirement for "JWT bearer token as a global security scheme" potentially conflict with unauthenticated public endpoints (health, login, docs)? [Conflict, Spec §FR-008 vs §FR-008a]
- [x] CHK035 - Is there ambiguity about whether role-based authorization is a Phase 2 requirement or part of the foundation phase? [Ambiguity, Spec §FR-004 vs Assumptions]
