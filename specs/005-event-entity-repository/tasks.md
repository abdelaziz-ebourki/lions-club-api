# Tasks: Event Entity, Repository & Flyway V2

**Input**: Design documents from `/specs/005-event-entity-repository/`

**Prerequisites**: plan.md, spec.md, data-model.md, research.md

**Tests**: Tests ARE included (TDD approach per project constitution - G3 Test-First is NON-NEGOTIABLE).

**Organization**: Tasks are grouped by user story.

## Format

`- [ ] [TaskID] [P?] [Story?] Description with exact file path`

## Path Conventions

- **Single project**: `src/main/java/...`, `src/test/java/...` at repository root
- Package root: `com.lionsclub.api`

---

## Phase 1: Setup

**Purpose**: Project initialization and prerequisite infrastructure

No setup tasks required — JPA auditing (`@EnableJpaAuditing`) is already configured from a prior feature.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create the Flyway migration that all other phases depend on

- [X] T001 [P] Create `V2__create_events_table.sql` in `src/main/resources/db/migration/V2__create_events_table.sql` with `events` table schema (id UUID PK DEFAULT gen_random_uuid(), title VARCHAR NOT NULL, description TEXT, start_date_time TIMESTAMP NOT NULL, end_date_time TIMESTAMP NOT NULL, location VARCHAR, address TEXT, max_attendees INTEGER, status VARCHAR NOT NULL DEFAULT 'DRAFT', created_by UUID NOT NULL REFERENCES users(id), created_at TIMESTAMP NOT NULL DEFAULT now(), updated_at TIMESTAMP NOT NULL DEFAULT now()) and indices (idx_events_status, idx_events_start_date, idx_events_created_by)

**Checkpoint**: V2 migration file exists with correct schema. Can be verified by running `./mvnw flyway:migrate` against a local PostgreSQL instance.

---

## Phase 3: User Story 1 - Application Startup (Priority: P1) 🎯 MVP

**Goal**: Application starts with `Event` entity mapped to `events` table and `EventRepository` bean available for injection

**Independent Test**: Spring context loads, Flyway records V2 migration, and `EventRepository` bean is registered

### Tests for User Story 1 (TDD - write FIRST) ⚠️

- [X] T002 [P] [US1] Create `EventStatusTest` verifying enum values `DRAFT`, `PUBLISHED`, `CANCELLED`, `COMPLETED` exist, parse correctly, and have exactly 4 values in `src/test/java/com/lionsclub/api/domain/event/EventStatusTest.java`
- [X] T003 [P] [US1] Write context-load test verifying `EventRepository` bean is present in `src/test/java/com/lionsclub/api/infrastructure/persistence/EventRepositoryTest.java` — should FAIL initially
- [X] T004 [US1] Write entity mapping test verifying `Event` fields match `events` table columns in `src/test/java/com/lionsclub/api/domain/event/EventTest.java` — should FAIL initially

### Implementation for User Story 1

- [X] T005 [P] [US1] Create `EventStatus` enum with `DRAFT`, `PUBLISHED`, `CANCELLED`, `COMPLETED` values in `src/main/java/com/lionsclub/api/domain/event/EventStatus.java`
- [X] T006 [P] [US1] Create `Event` JPA entity with all fields matching `V2__create_events_table.sql` in `src/main/java/com/lionsclub/api/domain/event/Event.java` (UUID id, title, description, startDateTime, endDateTime, location, address, maxAttendees, EventStatus status, User createdBy with LAZY ManyToOne, LocalDateTime createdAt with @CreatedDate, LocalDateTime updatedAt with @LastModifiedDate)
- [X] T007 [P] [US1] Create `EventRepository` Spring Data JPA interface with `findByStatus(EventStatus status)` returning `List<Event>` in `src/main/java/com/lionsclub/api/infrastructure/persistence/EventRepository.java`

**Checkpoint**: Application starts, EventRepository is injectable, V2 migration recorded in flyway_schema_history, all US1 tests pass

---

## Phase 4: User Story 2 - Event Domain Model Complete (Priority: P1)

**Goal**: Status persistence round-trips, timestamps auto-populate, entity validations work end-to-end

**Independent Test**: Persist an Event with status PUBLISHED, retrieve it by ID, verify status matches and timestamps are populated

### Tests for User Story 2 (TDD - write FIRST) ⚠️

- [X] T008 [US2] Write Status persistence test persisting Event with PUBLISHED status and asserting correct retrieval in `src/test/java/com/lionsclub/api/infrastructure/persistence/EventRepositoryTest.java` — should FAIL initially
- [X] T009 [US2] Write timestamp auto-population test persisting Event and verifying createdAt/updatedAt are non-null in `src/test/java/com/lionsclub/api/infrastructure/persistence/EventRepositoryTest.java` — should FAIL initially
- [X] T010 [US2] Write findByStatus test persisting multiple Events with different statuses and verifying correct subset returned in `src/test/java/com/lionsclub/api/infrastructure/persistence/EventRepositoryTest.java` — should FAIL initially

### Implementation for User Story 2

- [X] T011 [US2] Add `@Enumerated(EnumType.STRING)` and `@Column(nullable = false)` to `status` field in `src/main/java/com/lionsclub/api/domain/event/Event.java`
- [X] T012 [US2] Add `@Column(nullable = false)` and `@NotBlank` validation to `title` field in `src/main/java/com/lionsclub/api/domain/event/Event.java`
- [X] T013 [US2] Add `@ManyToOne(fetch = FetchType.LAZY)` and `@JoinColumn(name = "created_by", nullable = false)` to `createdBy` field in `src/main/java/com/lionsclub/api/domain/event/Event.java`
- [X] T014 [US2] Add `@Column(name = "start_date_time", nullable = false)` to `startDateTime` and `@Column(name = "end_date_time", nullable = false)` to `endDateTime` in `src/main/java/com/lionsclub/api/domain/event/Event.java`
- [X] T015 [US2] Verify `@CreatedDate` on `createdAt` and `@LastModifiedDate` on `updatedAt` with correct column name mappings (`created_at`, `updated_at`, `updatable = false` on createdAt) in `src/main/java/com/lionsclub/api/domain/event/Event.java`

**Checkpoint**: Events can be persisted and retrieved with correct statuses, timestamps auto-populate, findByStatus returns correct subsets

---

## Phase 5: Polish & Cross-Cutting

**Purpose**: Final verification that everything works end-to-end

- [X] T016 Run full test suite: `./mvnw test` — all tests pass (context load, entity mapping, repository operations, status persistence, timestamps)
- [X] T017 Run `./mvnw spring-boot:run` (with Docker PostgreSQL) — application starts without entity mapping errors, V2 migration recorded in flyway_schema_history
- [X] T018 Verify `flyway_schema_history` table records V2 migration and no migration errors on startup logs

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — nothing needed (JPA auditing already exists)
- **Phase 2 (Foundational)**: No dependencies — can start immediately
- **Phase 3 (US1 - Startup)**: Depends on Phase 2 (V2 migration must exist before entity mapping)
- **Phase 4 (US2 - Domain Complete)**: Depends on Phase 3 (entity + repo must exist before persistence tests)
- **Phase 5 (Polish)**: Depends on both US1 and US2 being complete

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD)
- Migration before entity before repository
- All tests passing before story is complete

### Parallel Opportunities

- T001 (Phase 2) — standalone
- T002, T003, T004 (US1 tests) — all [P], independent
- T005, T006, T007 (US1 implementation) — all [P], independent
- All other tasks are sequential within their phase

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Flyway V2 Migration (T001)
2. Complete Phase 3: User Story 1 (T002–T007 — EventStatus enum + Event entity + EventRepository)
3. **STOP and VALIDATE**: Run `./mvnw test` — context loads, repository bean registered
4. Tests T002–T007 all pass

### Incremental Delivery

1. Phase 2 + Phase 3 → Foundation ready (migration + entity + repo exist, tests pass)
2. Phase 4 → Validation + persistence tested and working (T008–T015)
3. Phase 5 → Full test suite green (T016–T018)

---

## Notes

- [P] tasks = different files, no dependencies — can be executed in parallel
- [US1]/[US2] labels map tasks to specific user stories
- Each user story is independently completable and testable
- TDD: write tests first, verify they fail, then implement
- Run `./mvnw test` after each phase
- Database must be running (Docker PostgreSQL) for repository integration tests
- JpaConfig with `@EnableJpaAuditing` is assumed to already exist from prior feature — if missing, add Setup phase
- The `User` entity and `UserRepository` are assumed to already exist from feature 002
