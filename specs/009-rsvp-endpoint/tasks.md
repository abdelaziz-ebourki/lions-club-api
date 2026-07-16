# Tasks: RSVP Endpoint

**Input**: Design documents from `/specs/009-rsvp-endpoint/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Test-First discipline is NON-NEGOTIABLE per project constitution. Tests must be written before implementation code (Red-Green-Refactor). All test tasks include "(TDD: write RED before implementation)" marker.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Single Spring Boot project at repository root
- `src/main/java/com/lionsclub/api/` for main sources
- `src/test/java/com/lionsclub/api/` for test sources
- `src/main/resources/db/migration/` for Flyway migrations

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Feature branch and package scaffolding

- [ ] T001 Create feature branch `009-rsvp-endpoint` from `main`
- [ ] T002 [P] Create package directory `src/main/java/com/lionsclub/api/domain/rsvp/`
- [ ] T003 [P] Create package directory `src/test/java/com/lionsclub/api/domain/rsvp/`

**Checkpoint**: Branch created, package structure ready

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database schema and core entity — blocks ALL user stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Create Flyway migration `V4__create_rsvps_table.sql` in `src/main/resources/db/migration/V4__create_rsvps_table.sql`
- [ ] T005 [P] Create `RsvpStatus` enum in `src/main/java/com/lionsclub/api/domain/rsvp/RsvpStatus.java`
- [ ] T006 Create `Rsvp` entity in `src/main/java/com/lionsclub/api/domain/rsvp/Rsvp.java` with fields: id (UUID), event (ManyToOne), member (ManyToOne), status (RsvpStatus), plusOne, notes, createdAt, updatedAt; unique constraint on (event, member)
- [ ] T007 Create `RsvpRepository` in `src/main/java/com/lionsclub/api/infrastructure/persistence/RsvpRepository.java` with methods: findByEventIdAndMemberId, findByEventId, countByEventIdAndStatus

**Checkpoint**: Database migrated, entity and repository ready

---

## Phase 3: User Story 1 — Member RSVPs to an Event (Priority: P1) 🎯 MVP

**Goal**: Authenticated members can RSVP (YES/NO/MAYBE) with optional plusOne and notes to any event. RSVP is upsert — changing mind updates existing record. Capacity enforcement (409 if event full). Validation rejects RSVPs to cancelled/completed events.

**Independent Test**: Login as a member, POST RSVP to an event, verify 201 with RSVP details. POST again with different status, verify 200 with updated status. POST as unauthenticated user, verify 401.

### Tests for User Story 1 (TDD: RED before implementation)

- [ ] T008 [P] [US1] Write `RsvpTest` entity unit test in `src/test/java/com/lionsclub/api/domain/rsvp/RsvpTest.java` — verify field constraints, enum mapping, relationship annotations
- [ ] T009 [P] [US1] Write `RsvpRepositoryTest` in `src/test/java/com/lionsclub/api/infrastructure/persistence/RsvpRepositoryTest.java` — verify save, find, count operations with Testcontainers
- [ ] T010 [P] [US1] Write `RsvpServiceTest` in `src/test/java/com/lionsclub/api/service/RsvpServiceTest.java` — verify upsert logic, capacity enforcement, cancelled/completed event rejection, unauthenticated rejection
- [ ] T011 [P] [US1] Write `RsvpControllerTest` (POST endpoint) in `src/test/java/com/lionsclub/api/web/RsvpControllerTest.java` — verify 201 create, 200 update, 400 invalid event status, 401 unauthenticated, 404 event not found, 409 capacity exceeded, 400 invalid input

### Implementation for User Story 1

- [ ] T012 [P] [US1] Create `RsvpRequest` DTO in `src/main/java/com/lionsclub/api/web/dto/RsvpRequest.java` with fields: status (@NotNull), plusOne (default 0), notes (optional, max 500)
- [ ] T013 [P] [US1] Create `RsvpResponse` DTO in `src/main/java/com/lionsclub/api/web/dto/RsvpResponse.java` with fields: id, eventId, memberId, status, plusOne, notes, createdAt, updatedAt
- [ ] T014 [US1] Implement `RsvpService` in `src/main/java/com/lionsclub/api/service/RsvpService.java` — upsert logic (find existing or create new, update fields, save), capacity check (countByEventIdAndStatus YES < maxAttendees), event status validation (reject CANCELLED/COMPLETED), @Transactional
- [ ] T015 [US1] Implement `RsvpController` (POST endpoint) in `src/main/java/com/lionsclub/api/web/RsvpController.java` — `POST /api/events/{id}/rsvp`, extract authenticated user from SecurityContext, delegate to RsvpService, return 201 (create) or 200 (update)
- [ ] T016 [US1] Update `SecurityConfig` in `src/main/java/com/lionsclub/api/security/SecurityConfig.java` — add `.requestMatchers(HttpMethod.POST, "/api/events/{id}/rsvp").hasAnyRole("MEMBER", "ADMIN")` and `.requestMatchers(HttpMethod.GET, "/api/events/{id}/rsvps").hasRole("ADMIN")`
- [ ] T017 [US1] Add SpringDoc annotations — `@Operation`, `@ApiResponse` on RsvpController POST method for OpenAPI documentation

**Checkpoint**: Members can RSVP to events. Unauthenticated requests rejected. Capacity enforced. API documented.

---

## Phase 4: User Story 2 — Admin Views Event RSVPs (Priority: P2)

**Goal**: Admin users can list all RSVPs for an event with member details. Non-admin members receive 403.

**Independent Test**: Have 2+ members RSVP to an event. Login as admin, GET `/api/events/{id}/rsvps` — verify array with member details. Login as regular member, GET same endpoint — verify 403.

### Tests for User Story 2

- [ ] T018 [P] [US2] Write `RsvpControllerTest` (GET list endpoint) in `src/test/java/com/lionsclub/api/web/RsvpControllerTest.java` — verify admin sees RSVP list with member names, non-admin gets 403, valid JSON response shape

### Implementation for User Story 2

- [ ] T019 [US2] Add GET endpoint to `RsvpController` in `src/main/java/com/lionsclub/api/web/RsvpController.java` — `GET /api/events/{id}/rsvps`, admin-only, returns list of RsvpResponse with nested member info
- [ ] T020 [US2] Add `findAllByEventIdWithMember` query or `@EntityGraph` to `RsvpRepository` in `src/main/java/com/lionsclub/api/infrastructure/persistence/RsvpRepository.java` to eagerly fetch member details
- [ ] T021 [US2] Add SpringDoc annotations on RsvpController GET method

**Checkpoint**: Admin can view all RSVPs per event. Non-admin gets 403.

---

## Phase 5: User Story 3 — Event RSVP Count Displayed (Priority: P2)

**Goal**: Event detail endpoint (`GET /api/events/:id`) shows computed `rsvpCount` and `rsvpBreakdown` replacing the hardcoded `0`. Works for public (unauthenticated) access too.

**Independent Test**: RSVP 3 YES and 1 MAYBE to an event. GET event details — verify `rsvpCount: 4` and `rsvpBreakdown: { yes: 3, no: 0, maybe: 1 }`.

### Tests for User Story 3

- [ ] T022 [P] [US3] Write `EventControllerTest` (GET event with RSVP counts) — extend existing test to verify `rsvpCount` and `rsvpBreakdown` fields are present and accurate after creating RSVPs
- [ ] T023 [P] [US3] Write `EventServiceTest` (RSVP count computation) — extend existing test to verify service layer attaches correct counts to EventResponse

### Implementation for User Story 3

- [ ] T024 [US3] Update `EventResponse` in `src/main/java/com/lionsclub/api/web/dto/EventResponse.java` — add `rsvpCount` int and `rsvpBreakdown` Map<String, Integer> fields
- [ ] T025 [US3] Update `EventService` in `src/main/java/com/lionsclub/api/service/EventService.java` — in `getEvent()` and `listEvents()`, query RSVP counts via RsvpRepository and populate `rsvpCount`/`rsvpBreakdown` on EventResponse
- [ ] T026 [US3] Update `Event` entity in `src/main/java/com/lionsclub/api/domain/event/Event.java` — add transient `@Transient` fields `rsvpCount` and `rsvpBreakdown` (or keep computation purely at DTO/service level per clean architecture)

**Checkpoint**: Event detail endpoint shows accurate, real-time RSVP counts.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, migration integrity, and full verification

- [ ] T027 Update `FlywayMigrationTest` in `src/test/java/com/lionsclub/api/config/FlywayMigrationTest.java` — expect 4 migrations instead of 3
- [ ] T028 [P] Run full test suite: `./mvnw verify`
- [ ] T029 [P] Run quickstart scenarios from `specs/009-rsvp-endpoint/quickstart.md` against running dev instance
- [ ] T030 [P] Manual verification of SpringDoc OpenAPI spec at `/swagger-ui.html` — verify RSVP endpoints appear with correct schemas

**Checkpoint**: All tests pass, quickstart scenarios verified, Swagger UI reflects new endpoints.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational — MVP scope
- **US2 (Phase 4)**: Depends on Foundational — can run parallel with US1 (different controller methods, same entity)
- **US3 (Phase 5)**: Depends on Foundational + US1 — needs RSVPs to exist to test counts
- **Polish (Phase 6)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (P1)**: Starts after Foundational — No dependencies on other stories (MVP)
- **US2 (P2)**: Starts after Foundational — independent of US1 (same Rsvp entity/repo, different controller methods)
- **US3 (P2)**: Best after US1 — needs actual RSVP data flow working to verify counts meaningfully

### Within Each User Story

- Tests MUST be written and FAIL before implementation (RED phase)
- DTOs before services
- Services before controllers
- Controllers before security config

### Parallel Opportunities

- T002 and T003 (package dirs) can run in parallel
- T004 and T005 (migration + enum) can run in parallel
- T008-T011 (all US1 tests) can run in parallel
- T012 and T013 (DTOs) can run in parallel
- T018 with US1 implementation — US2 tests don't depend on US1 code
- T022 and T023 (US3 tests) can run in parallel
- T028-T030 (Polish) can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all US1 tests together (TDD RED phase):
Task: "Write RsvpTest in src/test/java/.../domain/rsvp/RsvpTest.java"
Task: "Write RsvpRepositoryTest in src/test/java/.../persistence/RsvpRepositoryTest.java"
Task: "Write RsvpServiceTest in src/test/java/.../service/RsvpServiceTest.java"
Task: "Write RsvpControllerTest (POST) in src/test/java/.../web/RsvpControllerTest.java"

# Launch all US1 DTOs together (after tests fail):
Task: "Create RsvpRequest DTO in src/main/java/.../web/dto/RsvpRequest.java"
Task: "Create RsvpResponse DTO in src/main/java/.../web/dto/RsvpResponse.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Write all US1 tests (T008-T011) — verify they fail (RED)
4. Implement US1 (T012-T017) — verify tests pass (GREEN)
5. **STOP and VALIDATE**: Test US1 independently via quickstart scenarios 1, 2, 6
6. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 (Member RSVP) → Test independently → Deploy/Demo (MVP!)
3. Add US2 (Admin list) → Test independently → Deploy/Demo
4. Add US3 (Event counts) → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Phase 1 + Phase 2 together
2. Once Foundational is done:
   - Developer A: US1 (Phase 3) — core RSVP upsert
   - Developer B: US2 (Phase 4) — admin list endpoint (same entity, different controller methods)
   - Developer C: US3 (Phase 5) — event count display (modifies EventService/EventResponse)
3. Stories integrate independently — US2 and US3 use the same Rsvp entity/repo as US1

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- TDD: Verify tests fail (RED) before implementing (GREEN)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Follow existing code conventions (Lombok, SLF4J logging, SpringDoc annotations)
- All new endpoints must have SpringDoc `@Operation` and `@ApiResponse` annotations
