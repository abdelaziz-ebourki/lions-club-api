# Implementation Plan: Events CRUD endpoints with admin authorization

**Branch**: `006-events-crud` | **Date**: 2026-07-15 | **Spec**: [spec.md](spec.md)

## Summary

Implement REST API endpoints for Event CRUD operations. Read endpoints (`GET /api/events`, `GET /api/events/:id`) are publicly accessible; write endpoints (`POST`/`PUT`/`DELETE`) require `ADMIN` role via JWT cookie. The API contracts follow the frontend-driven response shape (split date/time, category string, derived status labels) requiring a DTO mapping layer.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.4.4 (Web, Security, Data JPA, Validation), Lombok, Auth0 java-jwt 4.5.0, SpringDoc OpenAPI 2.8.6

**Storage**: PostgreSQL 15 via Flyway migrations (no new tables — V3 migration adds `category` column to `events`)

**Testing**: Spring Boot Test + Testcontainers (PostgreSQL 15-alpine) + MockMvc for controller integration tests

**Target Platform**: Linux server, JVM 21

**Project Type**: Web service (REST API)

**Performance Goals**: Standard web API latency targets; no specific SLA defined

**Constraints**: Existing `events` table schema must not change incompatibly; status filter mapping (frontend `upcoming`/`ongoing`/`past` → backend `EventStatus` + date comparison); DTO layer required for shape differences

**Scale/Scope**: Single API module, 5 endpoints, admin authorization reuse

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **G1 - API-First**: API contracts are fully defined in issue #6 and spec.md. OpenAPI annotations will be added during implementation.
- [x] **G2 - Security by Design**: Endpoint security already configured in `SecurityConfig` (GET permitted, POST/PUT/DELETE require ADMIN). JWT auth from existing infrastructure.
- [x] **G3 - Test-First (NON-NEGOTIABLE)**: Tests will be written before implementation per constitution. Controller integration tests will cover all acceptance scenarios.
- [x] **G4 - DB Migration Rigor**: Schema changes (adding `category` column) will go through V3 Flyway migration. Rollback migration provided.
- [x] **G5 - Clean Architecture**: Design follows Controller → Service → Repository layering. DTOs at API boundary, entities stay in domain layer.

## Project Structure

### Documentation (this feature)

```text
specs/006-events-crud/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/lionsclub/api/
├── domain/
│   └── event/
│       ├── Event.java               # Existing — add category field
│       ├── EventStatus.java          # Existing — no changes
│       └── EventCategory.java        # NEW — enum for category (Health, Environment, ...)
├── web/
│   ├── EventController.java          # NEW — REST controller
│   └── dto/
│       ├── EventRequest.java         # NEW — create/update request DTO
│       └── EventResponse.java        # NEW — list/detail response DTO
├── service/
│   └── EventService.java             # NEW — business logic layer
├── infrastructure/
│   └── persistence/
│       ├── EventRepository.java       # Existing — add query methods
│       └── UserRepository.java        # Existing — no changes
└── security/
    ├── SecurityConfig.java           # Existing — already configured for events
    └── ...

src/main/resources/db/migration/
├── V1__create_users_table.sql        # Existing
├── V2__create_events_table.sql       # Existing
└── V3__add_event_category.sql        # NEW — add category column

src/test/java/com/lionsclub/api/
├── web/
│   └── EventControllerTest.java      # NEW — controller integration tests
├── service/
│   └── EventServiceTest.java         # NEW — service unit tests
└── domain/event/
    ├── EventStatusTest.java          # Existing
    └── EventTest.java                # Existing — update for category
```

**Structure Decision**: Single-module Maven project with `src/main/java` package-by-feature layering (controller → service → repository). Tests mirror source structure with `Test` suffix.

## Complexity Tracking

No Constitution violations — all 5 gates pass cleanly.
