# Implementation Plan: Complete Auth Endpoints

**Branch**: `004-complete-auth-endpoints` | **Date**: 2026-07-14 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/004-complete-auth-endpoints/spec.md`

## Summary

Add two missing auth endpoints — `GET /api/auth/me` (return current user's profile) and `POST /api/auth/refresh` (issue a new JWT with fresh expiry) — to complete the auth API surface defined in issue #4. Both endpoints reuse the existing JWT cookie infrastructure from feature 003; no new database tables, migrations, or external dependencies are required.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.4.4, Auth0 java-jwt (for JWT parsing), Lombok

**Storage**: PostgreSQL (existing `users` table via JPA/UserRepository — no schema change)

**Testing**: JUnit 5, Spring Boot Test, Testcontainers (via existing `TestcontainersConfiguration`), MockMvc, `@Sql` for DB cleanup

**Target Platform**: Linux server, Docker container

**Project Type**: REST API (web service)

**Performance Goals**: `/me` returns in under 200ms (SC-001); `/refresh` similarly sub-200ms

**Constraints**: Must reuse existing `auth_token` cookie pattern; no new DB tables or Flyway migrations; same error format (`{"error": "..."}`) as existing endpoints

**Scale/Scope**: Adds 2 endpoints to the existing auth API; affects only `AuthController`, `AuthService`, and introduces `UserResponse` DTO

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **G1 - API-First**: Contracts for `/me` and `/refresh` are documented in this plan (see `contracts/auth-api.md`)
- [x] **G2 - Security by Design**: Both endpoints require valid `auth_token` cookie; 401 for missing, expired, or invalid tokens; disabled/deleted users also receive 401
- [x] **G3 - Test-First (NON-NEGOTIABLE)**: Tests must be written before implementation code (see constitution §III)
- [x] **G4 - DB Migration Rigor**: No schema changes — feature reads from the existing `users` table only
- [x] **G5 - Clean Architecture**: `AuthController` (HTTP) → `AuthService` (business logic) → `UserRepository` (data access); `UserResponse` DTO at API boundary; no entity leak

## Project Structure

### Documentation (this feature)

```text
specs/004-complete-auth-endpoints/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── auth-api.md      # Extended with /me and /refresh
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
src/main/java/com/lionsclub/api/
├── security/
│   └── AuthService.java              # + getCurrentUser(), + refreshToken()
├── web/
│   ├── AuthController.java           # + GET /me, + POST /refresh
│   └── dto/
│       └── UserResponse.java         # NEW: id, email, firstName, lastName, role

src/test/java/com/lionsclub/api/
├── security/
│   └── AuthServiceTest.java          # + tests for new methods
└── web/
    └── AuthControllerTest.java       # + tests for /me and /refresh
```

**Structure Decision**: Single-module Maven project (existing structure) — no new modules or packages needed.

## Complexity Tracking

> No Constitution violations — all gates pass without justification needed.
