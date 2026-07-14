# Tasks: Complete Auth Endpoints

**Input**: Design documents from `specs/004-complete-auth-endpoints/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/auth-api.md

**Tests**: Test tasks are included per the project constitution (§III — Test-First Discipline, NON-NEGOTIABLE). Tests must be written before implementation code.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1 = /me, US2 = /refresh)
- Include exact file paths in descriptions

## Phase 1: Setup

**Purpose**: Verify infrastructure prerequisites and review existing patterns

- [x] T001 Verify feature 003 (JWT infrastructure) is fully merged — `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`, `AuthService`, `AuthController`, `AuthResult` record, cookie helpers all present in `src/main/java/com/lionsclub/api/`
- [x] T002 Review existing `AuthController` and `AuthService` method signatures, error handling patterns, and `AuthResponse` DTO in `src/main/java/com/lionsclub/api/web/dto/AuthResponse.java` to ensure new tasks align with conventions
- [x] T003 [P] Review existing `AuthControllerTest` in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` — understand `MockMvc` setup, `@Sql` cleanup, `@ImportTestcontainers`, cookie extraction pattern, and JSON path assertions

**Checkpoint**: Environment verified — existing patterns documented

---

## Phase 2: User Story 1 — Member views their profile (Priority: P1) 🎯 MVP

**Goal**: Authenticated users can retrieve their profile data via `GET /api/auth/me`.

**Independent Test**: Register a user via `/api/auth/register`, capture the `auth_token` cookie, then `GET /api/auth/me` with that cookie — expect 200 with `id`, `email`, `firstName`, `lastName`, `role`. Without cookie or with invalid cookie — expect 401.

### Tests (per Constitution §III — write first, ensure failure before implementation)

- [x] T004 [P] [US1] Write `shouldReturnUserInfoWhenAuthenticated` in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` — login, GET /me with cookie, expect 200 + all user fields
- [x] T005 [P] [US1] Write `shouldReturn401ForMeWhenNotAuthenticated` in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` — GET /me without cookie, expect 401
- [x] T006 [P] [US1] Write `shouldReturn401ForMeWithExpiredOrInvalidToken` in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` — GET /me with invalid/expired cookie, expect 401
- [x] T007 [P] [US1] Write `getCurrentUser_shouldReturnUserResponse` in `src/test/java/com/lionsclub/api/security/AuthServiceTest.java` — service returns correct DTO for valid user
- [x] T008 [P] [US1] Write `getCurrentUser_shouldThrowForDisabledUser` in `src/test/java/com/lionsclub/api/security/AuthServiceTest.java` — service throws/returns null for disabled user
- [x] T009 [P] [US1] Write `getCurrentUser_shouldThrowForDeletedUser` in `src/test/java/com/lionsclub/api/security/AuthServiceTest.java` — service throws/returns null for non-existent user

### Implementation

- [x] T010 [P] [US1] Create `UserResponse` record in `src/main/java/com/lionsclub/api/web/dto/UserResponse.java` with fields: `id` (UUID), `email` (String), `firstName` (String), `lastName` (String), `role` (String)
- [x] T011 [US1] Add `getCurrentUser(UUID userId)` method in `src/main/java/com/lionsclub/api/security/AuthService.java` — find user by ID, check enabled, map to `UserResponse`, return 401 if disabled/missing
- [x] T012 [US1] Add `@GetMapping("/me")` in `src/main/java/com/lionsclub/api/web/AuthController.java` — extract userId from SecurityContext principal, call `authService.getCurrentUser()`, return 200 with `UserResponse`
- [x] T012a [P] [US1] Add `@Operation` and `@ApiResponse` annotations to `GET /me` in `src/main/java/com/lionsclub/api/web/AuthController.java` — document 200 success with UserResponse schema and 401 error response

**Checkpoint**: `/api/auth/me` fully functional — authenticated users can view their profile, unauthenticated users receive 401

---

## Phase 3: User Story 2 — Member extends their session (Priority: P1)

**Goal**: Authenticated users can refresh their auth token via `POST /api/auth/refresh` to receive a new cookie with fresh expiry.

**Independent Test**: Login via `/api/auth/login`, capture the `auth_token` cookie, then `POST /api/auth/refresh` with that cookie — expect 200 with new `Set-Cookie` containing a fresh `Max-Age`. Without cookie — expect 401.

### Tests (per Constitution §III — write first, ensure failure before implementation)

- [x] T013 [P] [US2] Write `shouldRefreshTokenWhenAuthenticated` in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` — login, POST /refresh with cookie, expect 200 + new cookie with `Max-Age` > 0
- [x] T014 [P] [US2] Write `shouldReturn401ForRefreshWhenNotAuthenticated` in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` — POST /refresh without cookie, expect 401
- [x] T015 [P] [US2] Write `shouldReturn401ForRefreshWithExpiredOrInvalidToken` in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` — POST /refresh with invalid/expired cookie, expect 401
- [x] T016 [P] [US2] Write `refreshToken_shouldIssueNewToken` in `src/test/java/com/lionsclub/api/security/AuthServiceTest.java` — service generates valid new token for enabled user
- [x] T017 [P] [US2] Write `refreshToken_shouldThrowForDisabledUser` in `src/test/java/com/lionsclub/api/security/AuthServiceTest.java` — service rejects disabled user
- [x] T018 [P] [US2] Write `chain_shouldWorkAfterRefresh` in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` — login, refresh, then use NEW cookie for /me, expect 200

### Implementation

- [x] T019 [US2] Add `refreshToken(UUID userId)` method in `src/main/java/com/lionsclub/api/security/AuthService.java` — find user by ID, check enabled, generate new JWT via `jwtTokenProvider.generateToken()`, return token string
- [x] T020 [US2] Add `@PostMapping("/refresh")` in `src/main/java/com/lionsclub/api/web/AuthController.java` — extract userId from SecurityContext principal, call `authService.refreshToken()`, set new `auth_token` cookie via `createAuthCookie()`, return 200 with `AuthResponse("Token refreshed")`
- [x] T020a [P] [US2] Add `@Operation` and `@ApiResponse` annotations to `POST /refresh` in `src/main/java/com/lionsclub/api/web/AuthController.java` — document 200 success with AuthResponse schema and 401 error response

**Checkpoint**: `/api/auth/refresh` fully functional — authenticated users can extend their session, unauthenticated users receive 401

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, documentation alignment, and cleanup

- [x] T021 [P] Run `./mvnw test` and confirm all tests pass (existing + new) — 52/52, all green
- [x] T022 [P] Run quickstart validation scenarios from `specs/004-complete-auth-endpoints/quickstart.md` manually against running dev server — all 6 scenarios pass (register, /me 200, /me 401×2, refresh 200, refresh 401, chain)
- [x] T023 [P] Close GitHub issue #4 — PR #14 created with `Closes #4`, auto-closes on merge

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — verification only
- **US1 — /me (Phase 2)**: Depends on Phase 1 — infrastructure must be confirmed present
- **US2 — /refresh (Phase 3)**: Depends on Phase 1 — No dependency on US1 (independent endpoints)
- **Polish (Phase 4)**: Depends on all user story phases being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 1 — No dependencies on US2
- **User Story 2 (P1)**: Can start after Phase 1 — No dependencies on US1

### Within Each Phase

- Tests MUST be written and fail before implementation (Constitution §III)
- Tests marked [P] can be written in parallel
- DTO before Service before Controller
- Story complete before moving to next

### Parallel Opportunities

- T003, T004, T005, T006, T007, T008, T009 can all run in parallel (different test methods)
- T010 (DTO) is standalone — no dependencies
- US1 and US2 can be developed in parallel by different developers
- T021 and T022 can run in parallel after implementation
- T023 (issue close) is the final step

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (write first, expect failure):
Task: "Write shouldReturnUserInfoWhenAuthenticated in AuthControllerTest.java"
Task: "Write shouldReturn401ForMeWhenNotAuthenticated in AuthControllerTest.java"
Task: "Write shouldReturn401ForMeWithExpiredOrInvalidToken in AuthControllerTest.java"
Task: "Write getCurrentUser_shouldReturnUserResponse in AuthServiceTest.java"
Task: "Write getCurrentUser_shouldThrowForDisabledUser in AuthServiceTest.java"
Task: "Write getCurrentUser_shouldThrowForDeletedUser in AuthServiceTest.java"

# Then implement sequentially:
Task: "Create UserResponse DTO"
Task: "Add getCurrentUser to AuthService" (depends on UserResponse)
Task: "Add GET /me to AuthController" (depends on AuthService)
```

## Parallel Example: User Story 2

```bash
# Launch all tests for User Story 2 together (write first, expect failure):
Task: "Write shouldRefreshTokenWhenAuthenticated in AuthControllerTest.java"
Task: "Write shouldReturn401ForRefreshWhenNotAuthenticated in AuthControllerTest.java"
Task: "Write shouldReturn401ForRefreshWithExpiredOrInvalidToken in AuthControllerTest.java"
Task: "Write refreshToken_shouldIssueNewToken in AuthServiceTest.java"
Task: "Write refreshToken_shouldThrowForDisabledUser in AuthServiceTest.java"

# Then implement sequentially:
Task: "Add refreshToken to AuthService"
Task: "Add POST /refresh to AuthController" (depends on AuthService)
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup
2. Write US1 tests → expect failure
3. Implement US1 (DTO → Service → Controller)
4. **STOP and VALIDATE**: Run tests — all US1 tests should now pass
5. All 401 scenarios work: no cookie, expired/invalid token, disabled user

### Incremental Delivery

1. Phase 1 + Phase 2 → `/me` endpoint complete (MVP!)
2. Phase 3 → `/refresh` endpoint complete
3. Phase 4 → Validation + issue close
4. Each phase adds value without breaking previous work

### Parallel Team Strategy

With two developers:
1. Complete Phase 1 together
2. Developer A: Phase 2 (US1 — /me)
3. Developer B: Phase 3 (US2 — /refresh)
4. Both stories complete independently and integrate

---

## Notes

- [P] tasks = different files, no dependencies
- No DB schema changes or Flyway migrations needed
- UserResponse DTO is the only new file; all other changes are additions to existing files
- `SecurityConfig` requires no changes — `/me` and `/refresh` are covered by `.anyRequest().authenticated()`
- Auth cookie helper (`createAuthCookie()`) already exists in `AuthController` — reuse for `/refresh`
- Existing `JwtTokenProvider.generateToken()` accepts `(UUID userId, Role role)` — reuse as-is
- Error responses must use `{"error": "Unauthorized"}` to match the 003 contract
