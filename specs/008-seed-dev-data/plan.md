# Implementation Plan: Seed Data / Dev Data Setup

**Branch**: `008-seed-dev-data` | **Date**: 2026-07-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-seed-dev-data/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Create a `@Component` + `CommandLineRunner` (`DataSeeder`) annotated with `@Profile("dev")` that checks if the database is empty, and if so, inserts an admin user, two member users, three upcoming events, and two past events matching the UI mock data. Passwords are bcrypt-hashed using the existing `PasswordEncoder` bean. User IDs use deterministic UUIDs so frontend tests remain consistent.

## Technical Context

**Language/Version**: Java 21 (LTS)

**Primary Dependencies**: Spring Boot 3.4.4 (spring-boot-starter-data-jpa, spring-boot-starter-security), Flyway, Lombok, BCrypt (via SecurityConfig bean)

**Storage**: PostgreSQL (local dev) / Testcontainers PostgreSQL (tests)

**Testing**: JUnit 5 + AssertJ + Mockito + Spring Boot Test + Testcontainers

**Target Platform**: Local developer machines (Linux/macOS/Windows), CI runners

**Project Type**: Web service (Spring Boot REST API)

**Performance Goals**: N/A — seed runs once at startup, <10s as per SC-001

**Constraints**: Dev-only profile; idempotent (no duplicates on restart); passwords must use same BCrypt encoder as production; no schema changes

**Scale/Scope**: Small dataset — 1 admin user, 2 member users, 5 events

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **G1 - API-First**: No new API endpoints introduced — seed data is internal bootstrap logic. Existing OpenAPI contracts unchanged.
- [x] **G2 - Security by Design**: No new endpoints. Passwords bcrypt-hashed via the same `PasswordEncoder` bean used by AuthService. `@Profile("dev")` prevents seed exposure in production.
- [x] **G3 - Test-First (NON-NEGOTIABLE)**: A `DataSeederTest` will be written before the `DataSeeder` component (see tasks.md). Tests verify: empty DB seeds, non-empty DB skips, non-dev profile skips.
- [x] **G4 - DB Migration Rigor**: No schema changes. Flyway migrations V1–V3 already create all needed tables.
- [x] **G5 - Clean Architecture**: `DataSeeder` is a bootstrap component (@Component + CommandLineRunner) that injects repositories directly — accepted pattern for seed/dev data setup outside the Controller→Service→Repository flow.

## Complexity Tracking

> No violations — all gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/008-seed-dev-data/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (no new API contracts)
└── tasks.md             # Phase 2 output (speckit.tasks)
```

### Source Code (repository root)

```text
src/main/java/com/lionsclub/api/
└── config/
    └── DataSeeder.java           # New: @Profile("dev") CommandLineRunner

src/test/java/com/lionsclub/api/
└── config/
    └── DataSeederTest.java       # New: tests for seed logic
```

**Structure Decision**: Single-component addition to the existing `config/` package (same package as `JpaConfig`, `OpenApiConfig`). No new packages or layers needed.
