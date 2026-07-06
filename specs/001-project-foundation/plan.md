# Implementation Plan: Project Foundation

**Branch**: `001-project-foundation` | **Date**: 2026-07-06 | **Spec**: specs/001-project-foundation/spec.md

**Input**: Feature specification from `specs/001-project-foundation/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Set up the project foundation: Docker Compose with PostgreSQL, Flyway migration
pipeline, SpringDoc OpenAPI documentation, and environment configuration. This
phase enables local development for all subsequent phases.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.4.4, Spring Web, Spring Security,
Spring Data JPA, Spring Validation, Spring Boot Actuator,
Flyway (core + postgresql), PostgreSQL driver, Auth0 java-jwt 4.5.0,
SpringDoc OpenAPI 2.8.6, Lombok, Testcontainers (test scope)

**Storage**: PostgreSQL 15 via Docker Compose with persistent volume

**Testing**: Spring Boot Test + Spring Security Test + JUnit 5
(via spring-boot-starter-test)

**Target Platform**: Linux server (Docker container)

**Project Type**: Web service (REST API)

**Performance Goals**: N/A — foundation phase; no performance targets yet

**Constraints**: Must boot locally with Docker. Minimal dependencies added.

**Scale/Scope**: Single-developer project. Club management scope (members,
events, RSVPs).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **G1 - API-First**: OpenAPI base contract created at `contracts/openapi-base.yaml`. SpringDoc will auto-generate docs from annotations.
- [x] **G2 - Security by Design**: JWT bearer security scheme defined in OpenAPI contract. SpringDoc gated to `dev`/`test` profiles only.
- [x] **G3 - Test-First (NON-NEGOTIABLE)**: Tests will cover Flyway migration execution, SpringDoc config, and application context load.
- [x] **G4 - DB Migration Rigor**: V1 migration planned via Flyway. `ddl-auto: validate` prevents accidental schema drift.
- [x] **G5 - Clean Architecture**: Project uses standard layered structure. No business logic in this phase. ✅ **Post-design: all gates still pass.**

## Project Structure

### Documentation (this feature)

```text
specs/001-project-foundation/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/com/lionsclub/api/
│   │   ├── LionsClubApiApplication.java
│   │   ├── config/
│   │   │   └── OpenApiConfig.java
│   │   ├── domain/
│   │   │   ├── user/
│   │   │   │   └── (Phase 2 — V1 migration creates the users table)
│   │   │   ├── event/
│   │   │   │   └── (Phase 3)
│   │   │   └── rsvp/
│   │   │       └── (Phase 4)
│   │   ├── security/
│   │   │   └── (Phase 2)
│   │   └── web/
│   │       └── (Phase 2+)
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── db/migration/
│           ├── V1__create_users_table.sql
│           └── (future migrations)
└── test/
    └── java/com/lionsclub/api/
        └── config/
            ├── LionsClubApiApplicationTests.java
            ├── HealthEndpointTest.java
            ├── FlywayMigrationTest.java
            ├── TestcontainersConfig.java
            ├── OpenApiConfigTest.java
            ├── SpringDocProfileTest.java
            ├── SchemaValidationTest.java
            └── (future tests)

docker-compose.yml
.env.example
```

**Structure Decision**: Single project (Spring Boot monolith) with domain
packaging. `src/main/java/com/lionsclub/api/` is the base package. Domains
are split by sub-package: `domain/user/`, `domain/event/`, etc. Config lives
in `config/`, security in `security/`, web controllers in `web/`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

N/A — all gates pass.
