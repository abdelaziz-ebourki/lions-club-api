# Tasks: Events CRUD endpoints with admin authorization

**Input**: Design documents from `specs/006-events-crud/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- `src/main/java/com/lionsclub/api/` for production code
- `src/main/resources/db/migration/` for Flyway migrations
- `src/test/java/com/lionsclub/api/` for test code

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project is already initialized. No setup tasks needed.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema change, new enum, DTOs, repository queries, and service skeleton — required before any endpoint can be implemented.

- [X] T001 [P] Create V3 Flyway migration `V3__add_event_category.sql` in `src/main/resources/db/migration/` adding `category` column to `events` table
- [X] T002 [P] Create `EventCategory` enum in `src/main/java/com/lionsclub/api/domain/event/EventCategory.java` with values `HEALTH`, `ENVIRONMENT`, `YOUTH`, `COMMUNITY`, `FUNDRAISER`
- [X] T003 [P] Add `category` field to `Event` entity in `src/main/java/com/lionsclub/api/domain/event/Event.java` with `@Enumerated(EnumType.STRING)` and `@Column(nullable = false)`
- [X] T004 [P] Create `EventRequest` record in `src/main/java/com/lionsclub/api/web/dto/EventRequest.java` with fields: `title`, `description`, `date`, `time`, `location`, `category`, `status` — include Jakarta Validation annotations
- [X] T005 [P] Create `EventResponse` record in `src/main/java/com/lionsclub/api/web/dto/EventResponse.java` with fields matching the frontend contract (id, title, description, date, time, location, category, status, rsvpCount, createdAt, updatedAt)
- [X] T006 [P] Add custom JPQL query method to `EventRepository` in `src/main/java/com/lionsclub/api/infrastructure/persistence/EventRepository.java` for filtering events by date-range status (upcoming/ongoing/past)
- [X] T007 Create `EventService` in `src/main/java/com/lionsclub/api/service/EventService.java` with method stubs for: `listEvents`, `getEvent`, `createEvent`, `updateEvent`, `deleteEvent`

**Checkpoint**: Foundation ready — V3 migration applied, enums created, entity extended, DTOs defined, repository query ready, service skeleton in place.

---

## Phase 3: User Story 1 — Visitor browses events (Priority: P1) 🎯 MVP

**Goal**: Unauthenticated users can list and view event details.

**Independent Test**: Call `GET /api/events` and `GET /api/events/{id}` without any authentication token — receive 200 with event data.

### Implementation for User Story 1

- [X] T008 [US1] Implement `EventService.listEvents()` logic for listing all events with optional status filter in `src/main/java/com/lionsclub/api/service/EventService.java`
- [X] T009 [US1] Implement `EventService.getEvent()` logic for single event retrieval in `src/main/java/com/lionsclub/api/service/EventService.java`
- [X] T010 [US1] Create `EventController` in `src/main/java/com/lionsclub/api/web/EventController.java` with `GET /api/events` and `GET /api/events/{id}` endpoints (public, no auth required)

**Checkpoint**: At this point, visitors can browse events. MVP ready.

---

## Phase 4: User Story 2 — Admin creates an event (Priority: P2)

**Goal**: Admin users can create new events via `POST /api/events`.

**Independent Test**: Login as admin, call `POST /api/events` with valid data — receive 201 with created event.

### Implementation for User Story 2

- [X] T011 [US2] Implement `EventService.createEvent()` with date/time parsing, category mapping, status derivation, and DTO→entity conversion in `src/main/java/com/lionsclub/api/service/EventService.java`
- [X] T012 [US2] Add `POST /api/events` endpoint to `EventController` in `src/main/java/com/lionsclub/api/web/EventController.java` with `@Valid` on request body and admin auth

**Checkpoint**: Admin can create events. Created events visible via US1 endpoints.

---

## Phase 5: User Story 3 — Admin updates an event (Priority: P3)

**Goal**: Admin users can update existing events via `PUT /api/events/{id}`.

**Independent Test**: Login as admin, call `PUT /api/events/{id}` with updated data — receive 200 with updated event.

### Implementation for User Story 3

- [X] T013 [P] [US3] Implement `EventService.updateEvent()` with entity lookup, field update, and persistence in `src/main/java/com/lionsclub/api/service/EventService.java`
- [X] T014 [US3] Add `PUT /api/events/{id}` endpoint to `EventController` in `src/main/java/com/lionsclub/api/web/EventController.java` with `@Valid` and admin auth

**Checkpoint**: Admin can update events. Events reflect updates via US1.

---

## Phase 6: User Story 4 — Admin deletes an event (Priority: P3)

**Goal**: Admin users can delete events via `DELETE /api/events/{id}`.

**Independent Test**: Login as admin, call `DELETE /api/events/{id}` — receive 200 with `{ "success": true }`.

### Implementation for User Story 4

- [X] T015 [P] [US4] Implement `EventService.deleteEvent()` with existence check and deletion in `src/main/java/com/lionsclub/api/service/EventService.java`
- [X] T016 [US4] Add `DELETE /api/events/{id}` endpoint to `EventController` in `src/main/java/com/lionsclub/api/web/EventController.java` with admin auth

**Checkpoint**: Full event CRUD lifecycle functional.

---

## Phase 7: Tests (Constitution G3 — Test-First)

**Purpose**: Tests MUST be written before implementation code per constitution. These tests cover all user stories.

- [X] T017 [US1] Write `EventControllerTest` in `src/test/java/com/lionsclub/api/web/EventControllerTest.java` — test `GET /api/events` returns 200 with event list, `GET /api/events?status=upcoming` filters correctly, `GET /api/events/{id}` returns 200 for existing and 404 for non-existent
- [X] T018 [US2] Add test cases to `EventControllerTest` — test `POST /api/events` returns 201 for admin, 401 for unauthenticated, 403 for non-admin, 400 for invalid input
- [X] T019 [US3] Add test cases to `EventControllerTest` — test `PUT /api/events/{id}` returns 200 for admin, 404 for non-existent, 401/403 for unauthorized
- [X] T020 [US4] Add test cases to `EventControllerTest` — test `DELETE /api/events/{id}` returns 200 with `{ "success": true }` for admin, 404 for non-existent, 403 for non-admin
- [X] T021 [P] Write `EventServiceTest` in `src/test/java/com/lionsclub/api/service/EventServiceTest.java` — test status derivation mapping (upcoming/ongoing/past), date/time parsing, DTO↔entity conversion

**Checkpoint**: All endpoints and business logic covered by automated tests.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final touches that affect multiple user stories.

- [X] T022 [P] Add OpenAPI `@Operation` and `@ApiResponse` annotations to all `EventController` endpoints in `src/main/java/com/lionsclub/api/web/EventController.java`
- [X] T023 Verify error response consistency — all 4xx responses return `{ "message": "..." }` shape
- [X] T024 Run full test suite and verify all tests pass (`./mvnw test`)
- [X] T025 Run `quickstart.md` validation scenarios manually

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — project already exists
- **Foundational (Phase 2)**: No dependencies — blocks all user stories
- **US1 (Phase 3)**: Depends on Phase 2
- **US2 (Phase 4)**: Depends on Phase 3 (shares controller/service)
- **US3 (Phase 5)**: Depends on Phase 4 (shares controller/service)
- **US4 (Phase 6)**: Depends on Phase 5 (shares controller/service)
- **Tests (Phase 7)**: Can begin after Phase 2 (test stubs written before implementation)
- **Polish (Phase 8)**: Depends on all phases

### User Story Dependencies

- **US1 (P1)**: Independent — read-only, no auth needed
- **US2 (P2)**: Depends on US1 infrastructure (same controller)
- **US3 (P3)**: Depends on US2 (same controller/service)
- **US4 (P3)**: Depends on US3 (same controller/service)

### Within Each Phase

- Production code tasks before endpoint wiring
- Tests can be written alongside or before implementation (per constitution G3)
- Story complete before moving to next

### Parallel Opportunities

- **Phase 2**: T001, T002, T003, T004, T005, T006 can all run in parallel (different files, no overlapping changes)
- **Phase 7**: T017, T018, T019, T020, T021 can run in parallel (test files are independent)
- **Phase 8**: T022 is independent of other tasks

---

## Parallel Example: Foundational (Phase 2)

```bash
# Launch all foundational model/entity/DTO/migration tasks together:
Task: "T001 Create V3 migration"
Task: "T002 Create EventCategory enum"
Task: "T003 Add category field to Event entity"
Task: "T004 Create EventRequest DTO"
Task: "T005 Create EventResponse DTO"
Task: "T006 Add JPQL query to EventRepository"
```

## Implementation Strategy

### MVP First (Phase 1–3)

1. Complete Phase 2: Foundational
2. Complete Phase 3: US1 — Visitor browse events
3. **STOP and VALIDATE**: Test US1 independently (no auth needed)
4. Deploy/demo — public event listing is functional

### Full Feature (Phases 4–8)

1. Complete Phase 4: US2 — Admin create
2. Complete Phase 5: US3 — Admin update
3. Complete Phase 6: US4 — Admin delete
4. Complete Phase 7: Tests
5. Complete Phase 8: Polish
6. Run full test suite

### Test-First Integration (Constitution G3)

Per constitution: Write failing tests first, then implement. The suggested order:
1. Phase 2 (foundational) — no tests needed
2. Phase 7 (write tests) — they will fail
3. Phases 3–6 — make tests pass incrementally
4. Phase 8 — polish
