---

description: "Tasks for Project Foundation — Docker Compose, Flyway, SpringDoc, dev environment"

# Tasks: Project Foundation

**Input**: Design documents from `specs/001-project-foundation/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Test tasks are included per constitution principle III (Test-First Discipline — NON-NEGOTIABLE).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/`, `tests/` at repository root
- Base package: `src/main/java/com/lionsclub/api/`
- Resources: `src/main/resources/`
- Tests: `src/test/java/com/lionsclub/api/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 [P] Add Spring Boot Actuator dependency to `pom.xml`
- [x] T002 [P] Create base test class in `src/test/java/com/lionsclub/api/LionsClubApiApplicationTests.java`
- [x] T003 Verify project compiles with `./mvnw compile -q`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Configure `application.yml` with database connection, JPA, Flyway, and SpringDoc properties in `src/main/resources/application.yml`
- [x] T005 [P] Create `application-dev.yml` profile in `src/main/resources/application-dev.yml`
- [x] T006 [P] Create `application-prod.yml` profile in `src/main/resources/application-prod.yml`
- [x] T007 [P] Create `.env.example` file at project root with all required environment variables

**Checkpoint**: Foundation ready — user story implementation can now begin in parallel

---

## Phase 3: User Story 1 — Developer Environment Bootstrap (Priority: P1) 🎯 MVP

**Goal**: A developer can clone the repo, start PostgreSQL with Docker, and boot the API.

**Independent Test**: Run `docker compose up -d` then `./mvnw spring-boot:run` and confirm
`curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`.

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T008 [P] [US1] Write application context load test in `src/test/java/com/lionsclub/api/LionsClubApiApplicationTests.java`
- [x] T009 [P] [US1] Write health endpoint test verifying actuator returns UP in `src/test/java/com/lionsclub/api/config/HealthEndpointTest.java`

### Implementation for User Story 1

- [x] T010 [P] [US1] Create `docker-compose.yml` at project root with PostgreSQL 15 service (`db`), named volume (`pgdata`), health check, port mapping, and default network
- [x] T011 [US1] Copy `.env.example` to `.env` at project root (or create `.env` with `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 — Database Schema Management (Priority: P1)

**Goal**: Schema changes are managed through versioned Flyway migrations that run on startup.

**Independent Test**: Start the app, query `flyway_schema_history` table via Docker — V1 migration is recorded.

### Tests for User Story 2 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T012 [P] [US2] Write Flyway migration test verifying V1 migration runs in `src/test/java/com/lionsclub/api/config/FlywayMigrationTest.java`

### Implementation for User Story 2

- [x] T013 [US2] Create `V1__create_users_table.sql` in `src/main/resources/db/migration/V1__create_users_table.sql` with UUID PK, email (unique), password_hash, first_name, last_name, role, enabled, timestamps
- [x] T014 [US2] Add `@Testcontainers` setup for integration testing in `src/test/java/com/lionsclub/api/config/TestcontainersConfig.java`

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 — API Documentation Access (Priority: P2)

**Goal**: Developers and integrators can browse live API documentation via Swagger UI.

**Independent Test**: Start the app in dev profile, navigate to `/swagger-ui.html` — page loads
with "Lions Club FSBM API" title. In prod profile, `/swagger-ui.html` returns 404.

### Tests for User Story 3 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T015 [P] [US3] Write SpringDoc config test verifying custom OpenAPI metadata in `src/test/java/com/lionsclub/api/config/OpenApiConfigTest.java`
- [x] T016 [P] [US3] Write profile gating test verifying Swagger UI returns 404 in prod profile in `src/test/java/com/lionsclub/api/config/SpringDocProfileTest.java`

### Implementation for User Story 3

- [x] T017 [US3] Create `OpenApiConfig.java` in `src/main/java/com/lionsclub/api/config/OpenApiConfig.java` with custom title, version, description, JWT bearer security scheme, and `@Profile({"dev", "test"})` gating
- [x] T018 [US3] Verify SpringDoc exposes `/swagger-ui.html` in dev profile and hides in prod in `src/main/resources/application-prod.yml`

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T019 [P] Update `README.md` with prerequisites, setup commands (docker compose, mvnw), and API docs link
- [x] T020 Run quickstart validation — execute steps from `specs/001-project-foundation/quickstart.md` and confirm all scenarios pass
- [x] T021 [P] Write schema validation test verifying V1 migration columns in `src/test/java/com/lionsclub/api/config/SchemaValidationTest.java`
- [x] T022 Clean up any duplicate or unused configuration after all stories are integrated

---

## Phase 7: Convergence

- [x] T023 [P] [US3] Write test verifying `/swagger-ui.html` returns 200 OK in dev profile in `src/test/java/com/lionsclub/api/config/OpenApiConfigTest.java` per US3/AC1 (partial)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (US1 → US2 → US3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational — No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational — depends on Docker (US1) for DB, but migration SQL is standalone
- **User Story 3 (P2)**: Can start after Foundational — independent from US1/US2

### Within Each User Story

- Tests (included) MUST be written and FAIL before implementation
- Core infrastructure before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel
- All tests for a user story marked [P] can run in parallel
- T017 and T015/T016 (SpringDoc implementation + tests) can run in parallel once foundational is done

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Write application context load test in src/test/java/..."
Task: "Write health endpoint test in src/test/java/..."

# Launch all implementation for User Story 1 together:
Task: "Create docker-compose.yml at project root"
Task: "Create .env file at project root"
```

## Parallel Example: User Story 3

```bash
# Launch all tests for User Story 3 together:
Task: "Write SpringDoc config test in src/test/java/..."
Task: "Write profile gating test in src/test/java/..."

# Launch all implementation for User Story 3 together:
Task: "Create OpenApiConfig.java in src/main/java/..."
Task: "Configure prod profile in application-prod.yml"
```

---

## Implementation Strategy

### MVP First (User Story 1 + User Story 2)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1 (Docker + app boots)
4. **STOP and VALIDATE**: `docker compose up -d && ./mvnw spring-boot:run`
5. Complete Phase 4: User Story 2 (Flyway migration)
6. **STOP and VALIDATE**: Migration runs on startup
7. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 (Docker) → Test → Deploy (MVP: boots in Docker!)
3. Add User Story 2 (Flyway) → Test → Deploy (MVP: schema managed)
4. Add User Story 3 (SpringDoc) → Test → Deploy (MVP: docs available)

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 + User Story 2
   - Developer B: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
