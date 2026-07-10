# Implementation Plan: JWT Infrastructure

**Branch**: `003-jwt-infrastructure` | **Date**: 2026-07-10 | **Spec**: spec.md

**Input**: Feature specification from `/specs/003-jwt-infrastructure/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Implement cookie-based JWT authentication for the Lions Club FSBM REST API: JwtTokenProvider generates/validates JWTs, JwtAuthenticationFilter reads the `auth_token` cookie and sets the SecurityContext, SecurityConfig enforces endpoint permissions with role-based access, and AuthController exposes login/register/logout endpoints. The existing User entity (issue #2) already has Role enum, password hash, and `findByEmail` — JWT infrastructure builds directly on it.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.4.4 (spring-boot-starter-web, spring-boot-starter-security, spring-boot-starter-data-jpa), Auth0 java-jwt 4.5.0, Lombok, SpringDoc OpenAPI 2.8.6

**Storage**: PostgreSQL 15 — user records already exist via Flyway from issue #2; no new schema changes needed for this feature

**Testing**: JUnit 5, Spring Security Test (mock MVC with auth cookie), Testcontainers (PostgreSQL 15), REST Assured or MockMvc for controller tests

**Target Platform**: Linux server (Docker), ephemeral PostgreSQL 15 service in CI

**Project Type**: Web service (Spring Boot REST API)

**Performance Goals**: N/A — auth is per-request overhead; JWT validation is sub-millisecond with local HMAC

**Constraints**: Stateless (no HttpSession), httpOnly cookie transport, single JWT (no access/refresh pair), configurable expiry via `app.jwt.*` properties

**Scale/Scope**: Single REST API backend; auth applies to every protected endpoint; roles: PUBLIC (unauthenticated), MEMBER, ADMIN

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **G1 - API-First**: Endpoint contracts defined in spec (FR-005/006/007) and formalized in `contracts/auth-api.md`
- [x] **G2 - Security by Design**: Cookie-based JWT with httpOnly+SameSite, role-based authorization per endpoint, stateless sessions, Jakarta Validation on request payloads
- [ ] **G3 - Test-First (NON-NEGOTIABLE)**: Test classes designed in `quickstart.md` and `plan.md`; will be enforced at task execution (failing tests written before implementation code per TDD)
- [x] **G4 - DB Migration Rigor**: No schema changes needed — User entity and Role enum already deployed from issue #2
- [x] **G5 - Clean Architecture**: JwtTokenProvider → JwtAuthenticationFilter → AuthController, respecting Controller → Service → Repository layering

**Post-Phase 1 Re-check**: Gates G1, G2, G4, G5 pass. G3 is gated at task execution level per constitution.

## Project Structure

### Documentation (this feature)

```text
specs/003-jwt-infrastructure/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/main/java/com/lionsclub/api/
├── config/
│   └── OpenApiConfig.java          # EXISTING — update bearer-jwt description
├── domain/user/
│   ├── Role.java                   # EXISTING — enum: ADMIN, MEMBER
│   └── User.java                   # EXISTING — entity with email, passwordHash, firstName, lastName, role, enabled
├── infrastructure/persistence/
│   └── UserRepository.java         # EXISTING — findByEmail
├── security/
│   ├── JwtConfig.java              # NEW — @ConfigurationProperties for app.jwt.*
│   ├── JwtTokenProvider.java       # NEW — generate, validate, extract token
│   ├── JwtAuthenticationFilter.java # NEW — OncePerRequestFilter, reads cookie, sets SecurityContext
│   └── SecurityConfig.java         # NEW — endpoint permissions, CORS, CSRF, stateless session
└── web/
    └── AuthController.java         # NEW — login, register, logout endpoints

src/test/java/com/lionsclub/api/
├── config/
│   └── (existing tests)
├── domain/user/
│   └── (existing tests)
├── security/
│   ├── JwtTokenProviderTest.java   # NEW — unit test
│   ├── JwtAuthenticationFilterTest.java # NEW — unit test
│   └── SecurityConfigTest.java     # NEW — integration test with MockMvc
└── web/
    └── AuthControllerTest.java     # NEW — integration test with Testcontainers
```

**Structure Decision**: Standard Spring Boot layered architecture with existing conventions — `config/` for configuration beans, `domain/` for entities, `infrastructure/` for persistence, `security/` for auth components, `web/` for controllers. Tests mirror the source tree under `src/test/java`.

## Complexity Tracking

> No Constitution Check violations to justify.
