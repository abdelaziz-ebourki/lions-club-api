---
description: "Task list for Seed Data / Dev Data Setup"
---

# Tasks: Seed Data / Dev Data Setup

**Input**: Design documents from `specs/008-seed-dev-data/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Included — constitution mandates test-first (G3 NON-NEGOTIABLE).

**Organization**: Tasks grouped by user story. All stories share a single `DataSeeder` component, so implementation is one phase; each story adds its own test/validation tasks.

## Format: `[ID] [P] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/`, `tests/` at repository root
- All paths below use the existing Spring Boot project structure.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify the project builds and all existing tests pass before adding seed data logic.

- [x] T001 Verify project builds with `./mvnw clean compile -q` — no compilation errors
- [x] T002 Verify existing tests pass with `./mvnw test` — no regressions before changes

---

## Phase 2: Foundational (Test-First) 🧪

**Purpose**: Write the `DataSeederTest` before any implementation code, per constitution G3.

**⚠️ CRITICAL**: No implementation begins until tests are written and FAILING.

### Tests for DataSeeder

- [x] T003 [P] Write test `seedsDataWhenDatabaseIsEmpty` in `src/test/java/com/lionsclub/api/config/DataSeederTest.java` — verifies that when `userRepository.count() == 0` and `@Profile("dev")` is active, seed users and events are inserted; assert users exist via `userRepository.findByEmail()`
- [x] T004 [P] Write test `skipsSeedingWhenDatabaseHasData` in `src/test/java/com/lionsclub/api/config/DataSeederTest.java` — pre-inserts a user, runs seeder, verifies no duplicates (count unchanged)
- [x] T005 [P] Write test `doesNotSeedInNonDevProfile` in `src/test/java/com/lionsclub/api/config/DataSeederTest.java` — runs seeder with inactive `"dev"` profile, verifies no data inserted
- [x] T006 [P] Write test `seededUserPasswordsAreBcryptHashed` in `src/test/java/com/lionsclub/api/config/DataSeederTest.java` — retrieves seeded admin user, asserts `passwordEncoder.matches("admin123", user.getPasswordHash())`
- [x] T007 [P] Write test `seededEventsHaveCorrectFields` in `src/test/java/com/lionsclub/api/config/DataSeederTest.java` — fetches events by title, asserts all fields match expected values from data-model.md

**Checkpoint**: All 5 tests written and FAILING (no DataSeeder implementation yet). ✅ RED confirmed.

---

## Phase 3: User Story 1 - Developer sets up local dev environment (Priority: P1) 🎯 MVP

**Goal**: A developer can start the app with the dev profile and have the database automatically populated with seed users and events matching UI mock data.

**Independent Test**: Run `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`, then call `GET /api/events` — response contains 5 seeded events with correct fields.

### Implementation for User Story 1

- [x] T008 [P] [US1] Create `DataSeeder` component in `src/main/java/com/lionsclub/api/config/DataSeeder.java` — annotate with `@Component`, implement `CommandLineRunner`, annotate with `@Profile("dev")`
- [x] T009 [P] [US1] Inject `UserRepository`, `EventRepository`, and `PasswordEncoder` via constructor in `DataSeeder.java`
- [x] T010 [P] [US1] Implement idempotency guard in `DataSeeder.java` — `if (userRepository.count() > 0) return;`
- [x] T011 [P] [US1] Create admin user in `DataSeeder.java` — deterministic UUID (`UUID.nameUUIDFromBytes("admin-1".getBytes())`), firstName="Ahmed", lastName="Benali", email="admin@lionsclub.com", password=encode("admin123"), role=Role.ADMIN, enabled=true
- [x] T012 [P] [US1] Create member users in `DataSeeder.java` — deterministic UUIDs for "user-1" (Fatima / El Amrani) and "user-2" (Youssef / Idrissi), role=Role.MEMBER
- [x] T013 [US1] Create upcoming events in `DataSeeder.java` — 3 events mapped per data-model.md
- [x] T014 [US1] Create past events in `DataSeeder.java` — 2 events with status→COMPLETED
- [x] T015 [US1] Add `@Transactional` to `DataSeeder.run()` — wrap all inserts in a single transaction

**Checkpoint**: `DataSeederTest` now passes. Run `./mvnw test -Dtest=DataSeederTest` — all green. ✅

---

## Phase 4: User Story 2 - Developer tests authentication flows (Priority: P1)

**Goal**: Seeded users can authenticate via the login endpoint with their known credentials.

**Independent Test**: `curl -X POST /api/auth/login` with `admin@lionsclub.com` / `admin123` returns HTTP 200 with JWT token.

### Integration Tests for User Story 2

- [x] T016 [P] [US2] Add test `loginWithSeededAdminSucceeds` in `src/test/java/com/lionsclub/api/web/SeedDataAuthIntegrationTest.java` — performs POST /api/auth/login with seeded admin credentials, asserts HTTP 200 and JWT in Set-Cookie
- [x] T017 [P] [US2] Add test `loginWithSeededMemberSucceeds` in `src/test/java/com/lionsclub/api/web/SeedDataAuthIntegrationTest.java` — same for fatima@lionsclub.com / member123

**Checkpoint**: Auth integration tests pass. Run `./mvnw test -Dtest=SeedDataAuthIntegrationTest` — all green. ✅

---

## Phase 5: User Story 3 - Developer tests event listings (Priority: P2)

**Goal**: Seeded events appear in the events API, matching UI mock data fields exactly.

**Independent Test**: `curl GET /api/events` returns 5 events (3 upcoming, 2 past) with correct titles, descriptions, locations, categories, and statuses.

### Integration Tests for User Story 3

- [x] T018 [P] [US3] Add test `listEventsReturnsSeededUpcomingEvents` in `src/test/java/com/lionsclub/api/web/SeedDataEventIntegrationTest.java` — verifies GET /api/events returns the 3 upcoming seeded events
- [x] T019 [P] [US3] Add test `listEventsReturnsSeededPastEvents` in `src/test/java/com/lionsclub/api/web/SeedDataEventIntegrationTest.java` — verifies GET /api/events returns the 2 past seeded events
- [x] T020 [P] [US3] Add test `seededEventFieldsMatchMockData` in `src/test/java/com/lionsclub/api/web/SeedDataEventIntegrationTest.java` — for each seeded event, assert fields match expected values

**Checkpoint**: Events integration tests pass. Run `./mvnw test -Dtest=SeedDataEventIntegrationTest` — all green. ✅

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, documentation updates, profile verification.

- [x] T021 Run full test suite: `./mvnw test` — 106/106 tests pass
- [ ] T022 [P] Run quickstart.md validation scenarios 1–6 manually or via script
- [ ] T023 [P] Verify seed data is NOT loaded when running with `spring.profiles.active=prod` — application starts without seed logs

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational — Tests (Phase 2)**: Depends on Phase 1 — MUST complete before implementation
- **User Stories — Implementation (Phase 3, 4, 5)**: All depend on Phase 2 completion
- **Polish (Phase 6)**: Depends on all user story phases being complete

### User Story Dependencies

- **User Story 1 (P1)**: No story dependencies — seeded users and events are created together
- **User Story 2 (P1)**: Depends on US1 (seeded users must exist before login can be tested)
- **User Story 3 (P2)**: Depends on US1 (seeded events must exist before events API can return them)

### Within Each User Story

- Tests MUST be written and FAIL before implementation (Phase 2 applies this globally)
- Implementation tasks (T008–T015) can be done in any order — all are independent
- Integration tests (Phase 4, 5) require the DataSeeder to be complete

### Parallel Opportunities

- All Phase 2 test tasks (T003–T007) can run in parallel — different test methods
- All Phase 3 implementation tasks marked [P] (T008–T012) can run in parallel — different bean constructions
- Phase 4 and 5 integration tests can run independently of each other

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (tests)
3. Complete Phase 3: User Story 1 (DataSeeder component)
4. **STOP and VALIDATE**: Run `./mvnw test -Dtest=DataSeederTest` — all pass
5. MVP delivered: seed data loads on dev startup

### Incremental Delivery

1. Phase 1 + Phase 2 → Test infrastructure ready
2. Complete Phase 3 (US1) → Seed data loads, MVP ready
3. Complete Phase 4 (US2) → Auth integration verified
4. Complete Phase 5 (US3) → Events integration verified
5. Complete Phase 6 → Full validation pass

### Parallel Team Strategy

Single developer — sequential execution is expected (this is a small feature with ~20 tasks).

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- All user stories share the same `DataSeeder` component — implementation is US1
- US2 and US3 add integration tests that verify existing auth/events APIs work with seeded data
- Verify tests fail before implementing (Phase 2)
- Commit after each logical group: tests → DataSeeder → auth tests → events tests → polish
