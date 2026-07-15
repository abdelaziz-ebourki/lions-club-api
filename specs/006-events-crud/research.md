# Research: Events CRUD endpoints with admin authorization

**Date**: 2026-07-15

**Plan**: [plan.md](./plan.md)

## Decisions

### Decision 1 — Frontend status mapping (upcoming/ongoing/past) to backend EventStatus

- **Decision**: Map frontend status filter values to derived status based on `EventStatus.PUBLISHED` + date comparison:
  - `?status=upcoming` → `EventStatus.PUBLISHED` AND `startDateTime > now`
  - `?status=ongoing` → `EventStatus.PUBLISHED` AND `startDateTime <= now` AND `endDateTime >= now`
  - `?status=past` → `EventStatus.COMPLETED` OR (`EventStatus.PUBLISHED` AND `endDateTime < now`)
  - No status param → all events regardless of status
- **Rationale**: The frontend drives status display semantically (upcoming/past/ongoing) while the backend stores lifecycle status (DRAFT/PUBLISHED/COMPLETED/CANCELLED). Derived status from date boundaries provides accurate semantics without adding redundant columns.
- **Alternatives considered**: Adding a computed status column (denormalization, requires trigger); storing frontend status directly (loses distinction from lifecycle status).

### Decision 2 — Category field implementation

- **Decision**: Add `EventCategory` enum and a `category` column to the `events` table via V3 Flyway migration. Include it in the Event entity.
- **Rationale**: The frontend API contract requires `category` as a required field. Storing it at the database level ensures data integrity and enables future filtering by category. Adding it via V3 migration allows the existing V2 migration to remain immutable.
- **Alternatives considered**: DTO-only field with no DB storage (no persistence, inconsistent data); reusing existing fields (no natural fit).
- **EventCategory values**: `HEALTH`, `ENVIRONMENT`, `YOUTH`, `COMMUNITY`, `FUNDRAISER`

### Decision 3 — DTO request/response design

- **Decision**: Create two Java records — `EventRequest` for create/update input and `EventResponse` for list/detail output.
  - `EventRequest(input)`: `title`, `description`, `date`, `time`, `location`, `category`, `status` (frontend-shaped)
  - `EventResponse(output)`: `id`, `title`, `description`, `date`, `time`, `location`, `category`, `status`, `rsvpCount`, `createdAt`, `updatedAt`
- **Rationale**: The frontend sends `date` and `time` as separate strings (not `startDateTime`/`endDateTime`). The response includes computed fields (`rsvpCount` default 0) and derived status. DTOs decouple API shape from entity persistence.
- **Alternatives considered**: Exposing entity directly (breaks Clean Architecture, exposes internal shape); single combined DTO (different validation rules for create vs update).

### Decision 4 — Query method for status filtering

- **Decision**: Add a custom `@Query` method in `EventRepository` for complex status filtering by time ranges, plus keep `findByStatus` for exact matches.
- **Rationale**: The `upcoming`/`ongoing`/`past` filter requires date range comparison that Spring Data derived query methods cannot express concisely without multiple parameters and post-filtering. A JPQL query with parameters handles this at the database level.
- **Alternatives considered**: In‑memory filtering after fetching all events (inefficient for large datasets); multiple derived query methods (more methods, same complexity).

### Decision 5 — Service layer necessity

- **Decision**: Create `EventService` to encapsulate business logic (status derivation, query construction, authorization checks that go beyond role annotations).
- **Rationale**: The controller should handle only HTTP concerns (request parsing, response formatting, status codes). The service handles date‑based status mapping, DTO‑entity conversion, and any future business rules.
- **Alternatives considered**: Putting logic in the controller (bloated controller, violates Single Responsibility); putting logic in the repository (repository should not contain business rules).

### Decision 6 — No new database migration for updates (end_date_time derivation)

- **Decision**: Derive `endDateTime` from `date` + `time` + a default duration of 2 hours (typical event length). The request DTO accepts only `date` and `time`; `endDateTime` is calculated in the service layer.
- **Rationale**: The frontend contract does not provide an explicit end date/time. A sensible default avoids requiring the frontend to send redundant data.
- **Alternatives considered**: Making endDateTime required in the request (frontend change needed); making endDateTime nullable in the entity (existing schema has `NOT NULL`).
