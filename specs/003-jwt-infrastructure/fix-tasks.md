---

description: "Task list for fixing 3 bugs found in JWT Infrastructure PR #12 review"
---

# Tasks: JWT Infrastructure — Bug Fixes (Post-PR Review)

**Input**: Fix plan from review findings

**Tests**: Tests MUST be written first and fail before implementation (per constitution).

## Format: `[ID] [P?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths

## File Conventions

- Source: `src/main/java/com/lionsclub/api/`
- Tests: `src/test/java/com/lionsclub/api/`

---

## Phase 1: Fix Info Disclosure in Login (Bug 1)

**Purpose**: `AuthService.login()` leaks account existence via distinct `"Account disabled"` error message

### Test

- [x] T001 Write unit test for disabled account returning `"Invalid credentials"` in `src/test/java/com/lionsclub/api/security/AuthServiceTest.java` — create disabled user, call login with correct password, verify error is `"Invalid credentials"` (not `"Account disabled"`)

### Implementation

- [x] T002 Reorder checks in `AuthService.login()` in `src/main/java/com/lionsclub/api/security/AuthService.java` — move password validation before `isEnabled()` check; change disabled-account response from `"Account disabled"` to `"Invalid credentials"`

### Verification

- [x] T003 Run `./mvnw test -Dtest=AuthServiceTest` — new test passes

**Checkpoint**: Disabled accounts no longer reveal their existence via distinct error messages

---

## Phase 2: Fix Race Condition in Registration (Bug 2)

**Purpose**: Concurrent `register` calls with the same email can produce 500 instead of 409

### Test

- [x] T004 Write unit test for `DataIntegrityViolationException` handling in `AuthService.register()` in `src/test/java/com/lionsclub/api/security/AuthServiceTest.java` — stub `userRepository.save()` to throw `DataIntegrityViolationException`, verify `ERROR_DUPLICATE_EMAIL` is returned

### Implementation

- [x] T005 [P] Add `@Transactional` annotation to `AuthService.register()` in `src/main/java/com/lionsclub/api/security/AuthService.java`
- [x] T006 [P] Wrap `userRepository.save()` in try-catch for `DataIntegrityViolationException` in `src/main/java/com/lionsclub/api/security/AuthService.java` — catch and return `AuthResult.failure(ERROR_DUPLICATE_EMAIL)`; add import for `org.springframework.dao.DataIntegrityViolationException` and `org.springframework.transaction.annotation.Transactional`

### Verification

- [x] T007 Run `./mvnw test -Dtest=AuthServiceTest` — new test passes

**Checkpoint**: Concurrent duplicate registrations return 409 instead of crashing with 500

---

## Phase 3: Remove Unused Imports (Bug 3)

**Purpose**: Clean up unused imports across 3 files (low priority, cleanup only)

- [x] T008 [P] Remove unused `AntPathRequestMatcher` import in `src/main/java/com/lionsclub/api/security/SecurityConfig.java` (line 20)
- [x] T009 [P] Remove unused imports (`verifyNoInteractions`, `get`, `status`) in `src/test/java/com/lionsclub/api/security/JwtAuthenticationFilterTest.java` (lines 5-7)
- [x] T010 [P] Remove unused `java.util.Map` import in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` (line 12)

**Checkpoint**: No unused imports across modified files

---

## Phase 4: Full Validation

**Purpose**: Confirm all fixes compile and all tests still pass

- [x] T011 Run `./mvnw verify` — all tests pass (including new tests), PMD 0, Error Prone 0

**Checkpoint**: Full suite green, no regressions

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1** (Info Disclosure): No dependencies — can start immediately
- **Phase 2** (Race Condition): No dependencies — independent of Phase 1
- **Phase 3** (Unused Imports): No dependencies — independent of Phase 1 and 2
- **Phase 4** (Validation): Depends on Phase 1, 2, and 3

### Parallel Opportunities

- T001 and T004 (test files for different bugs) can be written in parallel
- T002 and T005+T006 (different source files, same `AuthService.java`) — T002 then T005+T006
- T005 and T006 can be done together (both edit `AuthService.java`)
- T008, T009, T010 (different files) can run in parallel

### Parallel Example: Phase 1 + Phase 2

```bash
# Launch FaultServiceTest tests for both bugs simultaneously:
Task: "Write AuthServiceTest — disabled account returns 'Invalid credentials'"
Task: "Write AuthServiceTest — DataIntegrityViolationException handling"
```

---

## Implementation Strategy

### Order

1. Write T001 and T004 (tests) first — they should fail initially
2. Fix Bug 1 (T002) — simple reorder, no new imports
3. Fix Bug 2 (T005+T006) — mildly more involved, needs imports
4. Clean up unused imports (T008+T009+T010) — trivial
5. Run full validation (T011)

### Single Developer Strategy

Sequential: Phase 1 → Phase 2 → Phase 3 → Phase 4. Within each phase, write failing test first, then implement, then refactor (Red-Green-Refactor).

---

## Notes

- [P] tasks = different files, no dependencies
- Verify tests fail before implementing
- Commit after each logical group
- All changes are in existing files — no new files created (test file is new: `AuthServiceTest.java`)
