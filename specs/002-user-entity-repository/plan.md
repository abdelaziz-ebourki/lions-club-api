# Implementation Plan: User Entity & Repository

**Branch**: `002-user-entity-repository` | **Date**: 2026-07-09 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-user-entity-repository/spec.md`

## Summary

Create the Java-side user domain model — a `Role` enum (`ADMIN`, `MEMBER`), a `User` JPA entity mapping to the existing `V1__create_users_table.sql` Flyway migration, and a `UserRepository` with `findByEmail`. Also configure JPA auditing (`@EnableJpaAuditing`) for automatic timestamp population. The database schema is already in place; this feature only adds the Java representation layer.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.4.4, spring-boot-starter-data-jpa, spring-boot-starter-validation, Lombok, Flyway (already on classpath)

**Storage**: PostgreSQL 15 (via existing Flyway migration V1)

**Testing**: Spring Boot Test (JUnit 5 + Testcontainers) — existing pattern in `src/test/`

**Target Platform**: Linux server (Docker)

**Project Type**: Web service (REST API backend)

**Performance Goals**: N/A for this feature (pure data model layer — no endpoints involved)

**Constraints**: JPA entity fields must exactly match the existing `users` table schema from `V1__create_users_table.sql`

**Scale/Scope**: Single module — 4 Java files (Role enum + User entity + JpaConfig + UserRepository) + corresponding tests

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **G1 - API-First**: N/A — this feature creates no API endpoints. The entity/repository layer is a prerequisite for future API contracts.
- [x] **G2 - Security by Design**: N/A — this feature adds no endpoints. AuthZ is designed when controllers are added in later features.
- [x] **G3 - Test-First (NON-NEGOTIABLE)**: Tests will be written before implementation code.
- [x] **G4 - DB Migration Rigor**: No new migration — reusing existing V1. Entity fields validated against existing schema.
- [x] **G5 - Clean Architecture**: Entity and Repository belong in the `domain.user` package. Config lives in `config`. No Controller or Service created here.

## Project Structure

### Documentation (this feature)

```text
specs/002-user-entity-repository/
├── plan.md               # This file
├── research.md           # Phase 0 output
├── data-model.md         # Phase 1 output
```

### Source Code (repository root)

```text
src/main/java/com/lionsclub/api/
├── config/
│   └── JpaConfig.java                       # NEW — @EnableJpaAuditing
├── domain/
│   └── user/
│       ├── Role.java                        # NEW — ADMIN, MEMBER enum
│       └── User.java                        # NEW — JPA @Entity mapping to users table
├── infrastructure/
│   └── persistence/
│       └── UserRepository.java             # NEW — Spring Data JPA interface
└── web/                                     # (empty, for future features)

src/test/java/com/lionsclub/api/
├── config/
│   ├── FlywayMigrationTest.java             # EXISTING
│   ├── SchemaValidationTest.java            # EXISTING
│   ├── OpenApiConfigTest.java              # EXISTING
│   └── HealthEndpointTest.java             # EXISTING
└── domain/
    └── user/
        ├── RoleTest.java                   # NEW
        ├── UserTest.java                   # NEW
        └── UserRepositoryTest.java          # NEW
```

**Structure Decision**: Single Spring Boot project. Domain entities in `domain.user/`, repository in `infrastructure/persistence/` (following clean architecture separation), config in `config/`. This aligns with existing project layout and keeps domain clean of infrastructure concerns.

## Complexity Tracking

No complexity violations — this is a standard JPA entity + repository pattern with no architectural workarounds needed.