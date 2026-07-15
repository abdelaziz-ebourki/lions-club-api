# Implementation Plan: Event Entity, Repository & Flyway V2

**Branch**: `005-event-entity-repository` | **Date**: 2026-07-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-event-entity-repository/spec.md`

## Summary

Create the Event domain model — an `EventStatus` enum (`DRAFT`, `PUBLISHED`, `CANCELLED`, `COMPLETED`), an `Event` JPA entity, a `V2__create_events_table.sql` Flyway migration to create the `events` table, and an `EventRepository` with `findByStatus`. JPA auditing (`@EnableJpaAuditing`) is already configured from a prior feature so `@CreatedDate`/`@LastModifiedDate` are available. The `User` entity and `users` table already exist from feature 002.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.4.4, spring-boot-starter-data-jpa, spring-boot-starter-validation, Lombok, Flyway (already on classpath)

**Storage**: PostgreSQL 15 (via Flyway migration V2)

**Testing**: Spring Boot Test (JUnit 5 + Testcontainers) — existing pattern in `src/test/`

**Target Platform**: Linux server (Docker)

**Project Type**: Web service (REST API backend)

**Performance Goals**: N/A for this feature (pure data model layer — no endpoints involved)

**Constraints**: JPA entity fields must exactly match the `events` table schema from `V2__create_events_table.sql`. The `createdBy` field must reference `users.id` via a foreign key with LAZY loading.

**Scale/Scope**: Single module — 4 Java files (EventStatus enum + Event entity + V2 migration + EventRepository) + corresponding tests

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **G1 - API-First**: N/A — this feature creates no API endpoints. The entity/repository/migration layer is a prerequisite for future API contracts.
- [x] **G2 - Security by Design**: N/A — this feature adds no endpoints. Authorization is designed when controllers are added in later features.
- [x] **G3 - Test-First (NON-NEGOTIABLE)**: Tests will be written before implementation code.
- [x] **G4 - DB Migration Rigor**: New V2 migration creates the `events` table. Entity fields validated against migration schema.
- [x] **G5 - Clean Architecture**: Entity and Repository belong in `domain.event` and `infrastructure.persistence` respectively. No Controller or Service created here.

## Project Structure

### Documentation (this feature)

```text
specs/005-event-entity-repository/
├── plan.md               # This file
├── spec.md               # Feature specification
├── research.md           # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md         # Phase 1 output
└── tasks.md              # Task breakdown
```

### Source Code (repository root)

```text
src/main/java/com/lionsclub/api/
├── domain/
│   └── event/
│       ├── EventStatus.java                 # NEW — DRAFT, PUBLISHED, CANCELLED, COMPLETED enum
│       └── Event.java                       # NEW — JPA @Entity mapping to events table
├── infrastructure/
│   └── persistence/
│       └── EventRepository.java             # NEW — Spring Data JPA interface
└── resources/
    └── db/
        └── migration/
            ├── V1__create_users_table.sql   # EXISTING
            └── V2__create_events_table.sql  # NEW — creates events table with FK to users

src/test/java/com/lionsclub/api/
├── config/
│   ├── FlywayMigrationTest.java             # EXISTING
│   ├── SchemaValidationTest.java            # EXISTING
│   ├── OpenApiConfigTest.java              # EXISTING
│   └── HealthEndpointTest.java             # EXISTING
└── domain/
    └── event/
        ├── EventStatusTest.java            # NEW
        ├── EventTest.java                  # NEW
        └── EventRepositoryTest.java         # NEW
```

**Structure Decision**: Single Spring Boot project. Domain entities in `domain.event/`, repository in `infrastructure/persistence/` (following clean architecture separation), migration in `resources/db/migration/`. This aligns with existing project layout and keeps domain clean of infrastructure concerns.

## Complexity Tracking

No complexity violations — this is a standard JPA entity + repository + Flyway migration pattern with no architectural workarounds needed.
