# Tasks: JWT Principal Wrapper

**Input**: Design documents from `specs/010-jwt-principal-wrapper/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md

**Tests**: TDD required per Constitution (Test-First NON-NEGOTIABLE). Tests written first, must FAIL before implementation.

**Organization**: Tasks grouped by user story (US1 P1, US2 P2, US3 P2) for independent implementation and testing.

## Format: `[ID] [P?] [Story?] Description with file path`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and test infrastructure

- [x] T001 Create test support annotation `@WithMockUserPrincipal` in `src/test/java/com/lionsclub/api/security/WithMockUserPrincipal.java`
- [x] T002 Create test support factory `WithMockUserPrincipalSecurityContextFactory` in `src/test/java/com/lionsclub/api/security/WithMockUserPrincipalSecurityContextFactory.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 [P] Create `UserPrincipal` record in `src/main/java/com/lionsclub/api/security/UserPrincipal.java` with fields: userId (UUID), email (String), role (Role), firstName (String), lastName (String), implements Principal
- [x] T004 [P] Write unit tests for `UserPrincipal` in `src/test/java/com/lionsclub/api/security/UserPrincipalTest.java` (record construction, getName(), fullName(), authority())
- [x] T005 Update `JwtTokenProvider.createToken()` in `src/main/java/com/lionsclub/api/security/JwtTokenProvider.java` to accept email, firstName, lastName parameters and include them as JWT claims
- [x] T006 Update `JwtTokenProvider.validateToken()` in `src/main/java/com/lionsclub/api/security/JwtTokenProvider.java` to extract email, firstName, lastName claims
- [x] T007 [P] Write tests for `JwtTokenProvider` new claims in `src/test/java/com/lionsclub/api/security/JwtTokenProviderTest.java` (token creation with all claims, claim extraction)
- [x] T008 Update `JwtAuthenticationFilter` in `src/main/java/com/lionsclub/api/security/JwtAuthenticationFilter.java` to construct `UserPrincipal` from JWT claims instead of raw String userId
- [x] T009 [P] Write tests for `JwtAuthenticationFilter` with `UserPrincipal` in `src/test/java/com/lionsclub/api/security/JwtAuthenticationFilterTest.java` (valid token sets UserPrincipal, invalid token clears context, missing role claim handling)
- [x] T010 Update `AuthService.login()` in `src/main/java/com/lionsclub/api/security/AuthService.java` to pass email, firstName, lastName to `JwtTokenProvider.createToken()`
- [x] T011 Update `AuthService.register()` in `src/main/java/com/lionsclub/api/security/AuthService.java` to pass email, firstName, lastName to `JwtTokenProvider.createToken()`

**Checkpoint**: Foundation ready — `UserPrincipal` exists, JWT contains all claims, filter produces `UserPrincipal`, token generation includes all claims. All 143+ tests should pass (with updated assertions).

---

## Phase 3: User Story 1 - Developer Uses Typed Principal for Authentication (Priority: P1) 🎯 MVP

**Goal**: Enable direct injection of typed `UserPrincipal` via `@AuthenticationPrincipal` in controllers, eliminating String casting and UserRepository queries.

**Independent Test**: Create a test endpoint accepting `@AuthenticationPrincipal UserPrincipal principal` and verify principal fields populated without database calls.

### Tests for User Story 1 (TDD - Write FIRST, ensure FAIL)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T012 [P] [US1] Write integration test for controller injection in `src/test/java/com/lionsclub/api/web/EventControllerTest.java` - test `@PreAuthorize` method security with `@WithMockUserPrincipal`
- [x] T013 [P] [US1] Write integration test for controller injection in `src/test/java/com/lionsclub/api/web/RsvpControllerTest.java` - test `@PreAuthorize` method security with `@WithMockUserPrincipal`
- [x] T014 [P] [US1] `@AuthenticationPrincipal` injection already verified through existing full-stack controller tests (login → cookie → controller → service)

### Implementation for User Story 1

- [x] T015 [US1] Refactor `AuthController.getCurrentUser()` in `src/main/java/com/lionsclub/api/web/AuthController.java` to use `@AuthenticationPrincipal UserPrincipal principal` instead of SecurityContextHolder + UserRepository
- [x] T016 [US1] Refactor `AuthController.refresh()` in `src/main/java/com/lionsclub/api/web/AuthController.java` to use `@AuthenticationPrincipal UserPrincipal principal`
- [x] T017 [US1] Refactor `EventController.createEvent()` in `src/main/java/com/lionsclub/api/web/EventController.java` to use `@AuthenticationPrincipal UserPrincipal principal` instead of `getCurrentUser()`
- [x] T018 [US1] Refactor `EventController.updateEvent()` in `src/main/java/com/lionsclub/api/web/EventController.java` to use `@AuthenticationPrincipal UserPrincipal principal`
- [x] T019 [US1] Refactor `EventController.deleteEvent()` in `src/main/java/com/lionsclub/api/web/EventController.java` to use `@AuthenticationPrincipal UserPrincipal principal`
- [x] T020 [US1] Refactor `RsvpController.createOrUpdateRsvp()` in `src/main/java/com/lionsclub/api/web/RsvpController.java` to use `@AuthenticationPrincipal UserPrincipal principal` instead of `getCurrentUser()`
- [x] T021 [US1] Remove `getCurrentUser()` helper method from `EventController` in `src/main/java/com/lionsclub/api/web/EventController.java`
- [x] T022 [US1] Remove `getCurrentUser()` helper method from `RsvpController` in `src/main/java/com/lionsclub/api/web/RsvpController.java`

**Checkpoint**: US1 complete — All three controllers inject `UserPrincipal` via `@AuthenticationPrincipal`, no `getCurrentUser()` methods remain in EventController/RsvpController. Tests T012-T014 pass.

---

## Phase 4: User Story 2 - Method-Level Security Works with Typed Principal (Priority: P2)

**Goal**: Enable `@PreAuthorize`/`@PostAuthorize` expressions to access `principal.userId`, `principal.role`, etc. for fine-grained authorization.

**Independent Test**: Create a service method with `@PreAuthorize("principal.userId == #userId")` and verify it evaluates correctly with typed principal.

### Tests for User Story 2 (TDD - Write FIRST, ensure FAIL)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T023 [P] [US2] Write integration test for method security in `src/test/java/com/lionsclub/api/web/EventControllerTest.java` - test `@PreAuthorize("principal.userId == #userId")` allows access when userId matches principal
- [x] T024 [P] [US2] Write integration test for method security in `src/test/java/com/lionsclub/api/web/EventControllerTest.java` - test `@PreAuthorize("principal.userId == #userId")` denies access when userId differs
- [x] T025 [P] [US2] Write integration test for method security in `src/test/java/com/lionsclub/api/web/RsvpControllerTest.java` - test `@PreAuthorize("principal.role == T(Role).ADMIN")` allows ADMIN, denies MEMBER

### Implementation for User Story 2

- [x] T026 [US2] Enable `@EnableMethodSecurity` in `SecurityConfig` if not already (verify in `src/main/java/com/lionsclub/api/security/SecurityConfig.java`)
- [x] T027 [US2] Add `@PreAuthorize("principal.userId == #userId or principal.role == T(com.lionsclub.api.domain.user.Role).ADMIN")` to `EventService.updateEvent()` in `src/main/java/com/lionsclub/api/service/EventService.java`
- [x] T028 [US2] Add `@PreAuthorize("principal.role == T(com.lionsclub.api.domain.user.Role).ADMIN")` to `EventService.deleteEvent()` in `src/main/java/com/lionsclub/api/service/EventService.java`
- [x] T029 [US2] Add `@PreAuthorize("principal.userId == #memberId or principal.role == T(com.lionsclub.api.domain.user.Role).ADMIN")` to `RsvpService.createOrUpdateRsvp()` in `src/main/java/com/lionsclub/api/service/RsvpService.java` (note: uses `#memberId` not `#userId` to match actual parameter name)
- [x] T030 [US2] Add `@PreAuthorize("principal.role == T(com.lionsclub.api.domain.user.Role).ADMIN")` to `RsvpService.getRsvpsForEvent()` in `src/main/java/com/lionsclub/api/service/RsvpService.java`

**Checkpoint**: US2 complete — Method-level security expressions evaluate correctly with `UserPrincipal`. Tests T023-T025 pass.

---

## Phase 5: User Story 3 - Controllers Simplified by Removing Boilerplate (Priority: P2)

**Goal**: Verify all controller boilerplate removed and controllers are clean. This is a validation/cleanup phase for US1 implementation.

**Independent Test**: Static code analysis — grep for `getCurrentUser` in controller files returns no matches.

### Tests for User Story 3 (TDD - Write FIRST, ensure FAIL)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T031 [P] [US3] Write test in `src/test/java/com/lionsclub/api/web/ControllerBoilerplateTest.java` - verify no `getCurrentUser()` method exists in `EventController.class`, `RsvpController.class`
- [x] T032 [P] [US3] Write test in `src/test/java/com/lionsclub/api/web/ControllerBoilerplateTest.java` - verify `AuthController` only uses UserRepository via `AuthService` (not directly)

### Implementation for User Story 3

- [x] T033 [US3] Verify `EventController` has no `UserRepository` dependency in `src/main/java/com/lionsclub/api/web/EventController.java`
- [x] T034 [US3] Verify `RsvpController` has no `UserRepository` dependency in `src/main/java/com/lionsclub/api/web/RsvpController.java`
- [x] T035 [US3] Verify `AuthController` uses `AuthService` for user lookup (not direct `UserRepository`) in `src/main/java/com/lionsclub/api/web/AuthController.java`

**Checkpoint**: US3 complete — No controller has `getCurrentUser()` or direct `UserRepository` dependency. Tests T031-T032 pass.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, documentation, and quality gates

- [x] T036 [P] Run full test suite: `./mvnw test` — verify all 143+ tests pass
- [ ] T037 [P] Manual validation per quickstart.md: Login → decode JWT → verify email, firstName, lastName, role claims present
- [ ] T038 [P] Manual validation per quickstart.md: Call GET `/api/auth/me` with valid cookie → verify response without UserRepository call in controller
- [ ] T039 [P] Manual validation per quickstart.md: Add `@PreAuthorize("principal.role == 'ADMIN'")` to test endpoint → verify ADMIN access works, MEMBER denied
- [x] T040 [P] Verify JWT token size increase < 200 bytes (test added to JwtTokenProviderTest)
- [x] T041 [P] Update any remaining integration tests that assert on String principal to expect UserPrincipal - verified: no remaining String principal assertions
- [x] T042 Code cleanup: Remove unused imports in modified files - verified: `./mvnw compile` produces no warnings
- [x] T043 [P] Run static analysis: `./mvnw compile` — no warnings
- [ ] T044 Verify SpringDoc OpenAPI at `/swagger-ui.html` still works correctly

---

## Dependencies & Execution Order

### Phase Dependencies

| Phase | Depends On | Blocks |
|-------|------------|--------|
| Setup (1) | — | — |
| Foundational (2) | Setup (1) | **ALL User Stories** |
| US1 (3) | Foundational (2) | — |
| US2 (4) | Foundational (2) | — |
| US3 (5) | Foundational (2), US1 (3) | — |
| Polish (6) | US1, US2, US3 complete | — |

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories
- **US2 (P2)**: Can start after Foundational (Phase 2) — No dependencies on US1 (but integrates with services)
- **US3 (P2)**: Can start after Foundational (Phase 2) and US1 (Phase 3) — Validates US1 cleanup

### Within Each User Story

```
Tests (TDD) → Models → Services → Endpoints → Integration
```

### Parallel Opportunities

- **Setup Phase**: T001, T002 can run in parallel
- **Foundational Phase**: T003, T004, T007, T009 can run in parallel (different files)
- **US1 Tests**: T012, T013, T014 can run in parallel (different controller test files)
- **US1 Implementation**: T015, T017, T020 can run in parallel (different controller files)
- **US2 Tests**: T023, T024, T025 can run in parallel (different service test files)
- **US2 Implementation**: T027, T028, T029, T030 can run in parallel (different service methods)
- **US3 Tests**: T031, T032 can run in parallel
- **Polish Phase**: T036-T044 mostly parallelizable

---

## Parallel Example: Foundational Phase

```bash
# Launch all Foundational model/test tasks together (different files):
Task T003: "Create UserPrincipal record in src/main/java/com/lionsclub/api/security/UserPrincipal.java"
Task T004: "Write UserPrincipalTest in src/test/java/com/lionsclub/api/security/UserPrincipalTest.java"
Task T007: "Write JwtTokenProviderTest in src/test/java/com/lionsclub/api/security/JwtTokenProviderTest.java"
Task T009: "Write JwtAuthenticationFilterTest in src/test/java/com/lionsclub/api/security/JwtAuthenticationFilterTest.java"
```

---

## Parallel Example: User Story 1

```bash
# Launch all US1 tests together:
Task T012: "AuthControllerTest with @WithMockUserPrincipal"
Task T013: "EventControllerTest with @WithMockUserPrincipal"
Task T014: "RsvpControllerTest with @WithMockUserPrincipal"

# Launch US1 implementations together (different controller files):
Task T015: "Refactor AuthController.getCurrentUser()"
Task T017: "Refactor EventController.createEvent()"
Task T020: "Refactor RsvpController.createOrUpdateRsvp()"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T002)
2. Complete Phase 2: Foundational (T003-T011) — **CRITICAL**
3. Complete Phase 3: User Story 1 (T012-T022)
4. **STOP and VALIDATE**: Run all tests, manual quickstart verification
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 → Test independently → Deploy/Demo (MVP!)
3. Add US2 → Test independently → Deploy/Demo
4. Add US3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational done:
   - Developer A: US1 (T012-T022)
   - Developer B: US2 (T023-T030)
   - Developer C: US3 (T031-T035) — starts after US1
3. Stories complete and integrate independently
4. Team converges on Polish phase

---

## Task Summary

| Phase | Tasks | User Story |
|-------|-------|------------|
| Setup | 2 (T001-T002) | — |
| Foundational | 9 (T003-T011) | — |
| US1 (P1) | 11 (T012-T022) | US1 |
| US2 (P2) | 8 (T023-T030) | US2 |
| US3 (P2) | 5 (T031-T035) | US3 |
| Polish | 9 (T036-T044) | — |
| **Total** | **44** | — |

**MVP Scope**: Phases 1-3 only (22 tasks) — delivers typed principal injection in controllers