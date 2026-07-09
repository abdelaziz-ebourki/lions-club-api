# Tasks: User Entity & Repository

**Input**: Design documents from `/specs/002-user-entity-repository/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md

**Tests**: Tests ARE included (TDD approach per project constitution).

**Organization**: Tasks are grouped by user story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/main/java/...`, `src/test/java/...` at repository root
- Package root: `com.lionsclub.api`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Enable JPA auditing so entity timestamps work

- [X] T001 Create `JpaConfig` with `@EnableJpaAuditing` and `@Configuration` in `src/main/java/com/lionsclub/api/config/JpaConfig.java`

**Checkpoint**: JPA auditing ready — user story implementation can begin

---

## Phase 2: User Story 1 - Application Startup (Priority: P1) 🎯 MVP

**Goal**: Application starts with User entity mapped to `users` table and UserRepository bean available for injection

**Independent Test**: Spring context loads and `UserRepository` bean is registered

### Tests for User Story 1 (TDD - write FIRST) ⚠️

- [X] T002 [P] [US1] Create `RoleTest` verifying enum values `ADMIN` and `MEMBER` exist, parse correctly, and have exactly 2 values in `src/test/java/com/lionsclub/api/domain/user/RoleTest.java`
- [X] T003 [P] [US1] Write context-load test verifying `UserRepository` bean is present in `src/test/java/com/lionsclub/api/domain/user/UserRepositoryTest.java` — should FAIL initially
- [X] T004 [US1] Write entity mapping test verifying `User` fields match `users` table columns in `src/test/java/com/lionsclub/api/domain/user/UserTest.java` — should FAIL initially

### Implementation for User Story 1

- [X] T005 [P] [US1] Create `Role` enum with `ADMIN` and `MEMBER` values in `src/main/java/com/lionsclub/api/domain/user/Role.java`
- [X] T006 [P] [US1] Create `User` JPA entity with all fields matching `V1__create_users_table.sql` in `src/main/java/com/lionsclub/api/domain/user/User.java` (UUID id, email, passwordHash, firstName, lastName, Role role, boolean enabled, LocalDateTime createdAt with `@CreatedDate`, LocalDateTime updatedAt with `@LastModifiedDate`)
- [X] T007 [P] [US1] Create `UserRepository` Spring Data JPA interface with `findByEmail(String email)` returning `Optional<User>` in `src/main/java/com/lionsclub/api/infrastructure/persistence/UserRepository.java` (under infrastructure.persistence, not domain.user, per clean architecture)

**Checkpoint**: Application starts, UserRepository is injectable, all tests pass

---

## Phase 3: User Story 2 - User Domain Model Complete (Priority: P1)

**Goal**: Role persistence round-trips, timestamps auto-populate, entity validations work

**Independent Test**: Persist a User with Role.ADMIN, retrieve it, verify role matches and timestamps are populated

### Tests for User Story 2 (TDD - write FIRST) ⚠️

- [X] T008 [US2] Write Role persistence test in `src/test/java/com/lionsclub/api/domain/user/UserRepositoryTest.java`: persist User with ADMIN role, retrieve by email, assert role is ADMIN — should FAIL initially
- [X] T009 [US2] Write timestamp auto-population test in `src/test/java/com/lionsclub/api/domain/user/UserRepositoryTest.java`: persist User, verify createdAt and updatedAt are non-null — should FAIL initially
- [X] T010 [US2] Write findByEmail-not-found test in `src/test/java/com/lionsclub/api/domain/user/UserRepositoryTest.java`: query non-existent email, verify Optional.empty() — should FAIL initially

### Implementation for User Story 2

- [X] T011 [US2] Add `@Enumerated(EnumType.STRING)` to `role` field in `User.java` to ensure string-based enum persistence
- [X] T012 [US2] Add `@Column(nullable = false, unique = true)` and `@Email`, `@NotBlank` validations to email field in `User.java`
- [X] T013 [US2] Add `@Column(name = "password_hash", nullable = false)` and `@NotBlank` to passwordHash field in `User.java`
- [X] T014 [P] [US2] Add `@Column(name = "first_name", nullable = false)` and `@NotBlank` to firstName field in `User.java`
- [X] T015 [P] [US2] Add `@Column(name = "last_name", nullable = false)` and `@NotBlank` to lastName field in `User.java`
- [X] T016 [US2] Verify `@CreatedDate` and `@LastModifiedDate` are correctly configured on createdAt/updatedAt fields with proper column name mappings (`created_at`, `updated_at`)

**Checkpoint**: Users can be persisted and retrieved with correct roles, timestamps auto-populate, bean validation works

---

## Phase 4: Polish & Cross-Cutting

**Purpose**: Final verification that everything works end-to-end

- [X] T017 Run full test suite: `./mvnw test` — all tests pass (context load, entity mapping, repository operations, role persistence, timestamps)
- [X] T018 Run `./mvnw spring-boot:run` (with Docker PostgreSQL) — application starts without entity mapping errors
- [X] T019 Verify `flyway_schema_history` table records V1 migration and no migration errors on startup

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **User Story 1 (Phase 2)**: Depends on Setup completion
- **User Story 2 (Phase 3)**: Depends on User Story 1 completion (entity + repo must exist before adding validation/persistence tests)
- **Polish (Phase 4)**: Depends on both user stories being complete

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD)
- Entity before repository
- All tests passing before story is complete

### Parallel Opportunities

- T002, T003, T004 (US1 tests) — all [P], independent
- T005, T006, T007 (US1 implementation) — all [P], independent
- T014, T015 (US2 field mapping) — [P], independent
- All other tasks are sequential within their phase

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Write RoleTest.java in src/test/java/com/lionsclub/api/domain/user/RoleTest.java"
Task: "Write UserRepository context load test"
Task: "Write User entity mapping test"

# Launch all implementation for User Story 1 together:
Task: "Create Role.java in src/main/java/com/lionsclub/api/domain/user/Role.java"
Task: "Create User.java in src/main/java/com/lionsclub/api/domain/user/User.java"
Task: "Create UserRepository.java in src/main/java/com/lionsclub/api/infrastructure/persistence/UserRepository.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (JpaConfig)
2. Complete Phase 2: User Story 1 (Role enum + User entity + UserRepository)
3. **STOP and VALIDATE**: Run `./mvnw test` — context loads, repository bean registered
4. Tests T002–T007 all pass

### Incremental Delivery

1. Setup + US1 → Foundation ready (entity+repo exist, tests pass)
2. US2 → Validation+persistence tested and working
3. Polish → Full test suite green

---

## Notes

- [P] tasks = different files, no dependencies
- [US1]/[US2] labels map tasks to specific user stories
- Each user story is independently completable and testable
- TDD: write tests first, verify they fail, then implement
- Run `./mvnw test` after each phase
- Database must be running (Docker PostgreSQL) for repository tests