# Implementation Plan: RSVP Endpoint

**Branch**: `009-rsvp-endpoint` | **Date**: 2026-07-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/009-rsvp-endpoint/spec.md`

## Summary

Add RSVP functionality to the events module — authenticated members can RSVP (YES/NO/MAYBE) with optional plus-one and notes. Admin can view all RSVPs per event. Event detail endpoint exposes dynamic attendee counts. Built on existing JWT auth, Flyway V4 migration for new `rsvps` table, and SpringDoc-annotated OpenAPI contracts.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.4.4, Spring Data JPA, Spring Security, SpringDoc OpenAPI, Auth0 java-jwt, Flyway, Lombok, PostgreSQL

**Storage**: PostgreSQL 15 via Flyway migration V4 (new `rsvps` table with unique constraint on event_id + user_id)

**Testing**: JUnit 5, MockMvc, Testcontainers (PostgreSQL), `@WebMvcTest` for controller, `@DataJpaTest` for repository, mockito for service

**Target Platform**: Linux server (Docker Compose), port 8080

**Project Type**: REST web service (Spring Boot), single Maven module

**Performance Goals**: RSVP submission acknowledged in <500ms p95; event detail response includes computed counts without measurable overhead

**Constraints**: Stateless (cookie-based JWT auth), no HttpSession; existing layered architecture (Controller → Service → Repository); idempotent upsert design

**Scale/Scope**: Single RSVP table, ~1k-10k records; one RSVP per member per event; capacity enforcement via `maxAttendees`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **G1 - API-First**: OpenAPI contracts will be designed and annotated (SpringDoc) before implementation.
- [x] **G2 - Security by Design**: RSVP create/update requires MEMBER role; RSVP list requires ADMIN role; input validated via Jakarta Validation.
- [x] **G3 - Test-First (NON-NEGOTIABLE)**: Tests will be written before implementation code (Red-Green-Refactor) across all layers.
- [x] **G4 - DB Migration Rigor**: Schema changes via Flyway V4 migration only; no manual DDL.
- [x] **G5 - Clean Architecture**: Controller → Service → Repository layering with DTOs at API boundaries.

## Project Structure

### Documentation (this feature)

```text
specs/009-rsvp-endpoint/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/lionsclub/api/
├── domain/rsvp/
│   └── Rsvp.java                          # New entity
│
├── infrastructure/persistence/
│   └── RsvpRepository.java                # New repository
│
├── service/
│   └── RsvpService.java                   # New service
│
├── web/
│   ├── RsvpController.java                # New controller
│   └── dto/
│       ├── RsvpRequest.java               # New request DTO
│       └── RsvpResponse.java              # New response DTO
│
├── domain/event/
│   └── Event.java                         # Modified: add rsvpCount, rsvpBreakdown (transient/computed)
│
├── web/dto/
│   └── EventResponse.java                 # Modified: replace hardcoded 0 with dynamic counts
│
├── service/
│   └── EventService.java                  # Modified: compute and attach RSVP counts
│
└── security/
    └── SecurityConfig.java                # Modified: add RSVP route rules

src/main/resources/db/migration/
└── V4__create_rsvps_table.sql             # New migration

src/test/java/com/lionsclub/api/
├── domain/rsvp/
│   └── RsvpTest.java                      # New: entity unit tests
├── infrastructure/persistence/
│   └── RsvpRepositoryTest.java            # New: repository integration tests
├── service/
│   └── RsvpServiceTest.java               # New: service unit tests
├── web/
│   └── RsvpControllerTest.java            # New: controller slice tests
└── config/
    └── FlywayMigrationTest.java           # Modified: expect V4
```

**Structure Decision**: All new files follow existing project conventions — entities in `domain/`, repositories in `infrastructure/persistence/`, services in `service/`, controllers and DTOs in `web/` and `web/dto/`. RSVP lives in its own `domain/rsvp/` sub-package (mirrors `domain/event/` and `domain/user/`). Existing event-related files are modified minimally (EventResponse DTO and EventService only).

## Complexity Tracking

No constitution violations expected. Feature fits within existing project patterns.
