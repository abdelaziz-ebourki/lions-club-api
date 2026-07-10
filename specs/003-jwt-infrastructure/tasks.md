---

description: "Task list for JWT Infrastructure feature"
---

# Tasks: JWT Infrastructure

**Input**: Design documents from `/specs/003-jwt-infrastructure/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/auth-api.md, quickstart.md

**Tests**: Tests are required per constitution (Test-First NON-NEGOTIABLE). Tests MUST be written first and fail before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/`, `tests/` at repository root
- Source: `src/main/java/com/lionsclub/api/`
- Tests: `src/test/java/com/lionsclub/api/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configure shared application properties for JWT

**⚠️ No project initialization needed** — project already exists with Spring Boot 3.4.4, Java 21, all dependencies (Auth0 java-jwt, Spring Security, etc.)

- [x] T001 Add JWT configuration properties (`app.jwt.secret`, `app.jwt.expiration`) to `src/main/resources/application-dev.yml`

**Checkpoint**: JWT secret and expiry configured for development profile

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: JWT token infrastructure that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Tests for Foundational Phase ⚠️

> **NOTE**: Write these tests FIRST, ensure they FAIL before implementation

- [x] T002 [P] Write unit test for JwtTokenProvider token generation and validation in `src/test/java/com/lionsclub/api/security/JwtTokenProviderTest.java`
- [x] T003 [P] Write unit test for JwtTokenProvider expiry and signature tampering in `src/test/java/com/lionsclub/api/security/JwtTokenProviderTest.java`

### Implementation for Foundational Phase

- [x] T004 Create `JwtConfig` configuration properties class in `src/main/java/com/lionsclub/api/security/JwtConfig.java` (binds `app.jwt.secret`, `app.jwt.expiration`)
- [x] T005 Create `JwtTokenProvider` for token generation and validation in `src/main/java/com/lionsclub/api/security/JwtTokenProvider.java` (HMAC-SHA256, userId+role claims, configurable expiry)

**Checkpoint**: JwtTokenProvider tested, JwtConfig ready — token generation and validation works

---

## Phase 3: User Story 1 - Visitor browses public content (Priority: P1) 🎯 MVP

**Goal**: Public endpoints (login, register, GET events, Swagger docs) are accessible without any authentication

**Independent Test**: MockMvc requests to `/api/events/`, `/swagger-ui/`, `/v3/api-docs` without auth cookie → all return 200

### Tests for User Story 1 ⚠️

> **NOTE**: Write these tests FIRST, ensure they FAIL before implementation

- [x] T006 [P] [US1] Write SecurityConfig test for public endpoint access in `src/test/java/com/lionsclub/api/security/SecurityConfigTest.java` (MockMvc, no auth cookie → expect 200)
- [x] T007 [P] [US1] Write SecurityConfig test for CORS preflight from `http://localhost:5173` in `src/test/java/com/lionsclub/api/security/SecurityConfigTest.java`
- [x] T008 [US1] Write JwtAuthenticationFilter test for no-cookie pass-through in `src/test/java/com/lionsclub/api/security/JwtAuthenticationFilterTest.java` (request without cookie → filter chain proceeds)

### Implementation for User Story 1

- [x] T009 [US1] Create `JwtAuthenticationFilter` (OncePerRequestFilter) in `src/main/java/com/lionsclub/api/security/JwtAuthenticationFilter.java` — reads `auth_token` cookie, validates via JwtTokenProvider, sets SecurityContext, passes through if no cookie
- [x] T010 [US1] Create `SecurityConfig` in `src/main/java/com/lionsclub/api/security/SecurityConfig.java` — configure public endpoints (`POST /api/auth/login`, `POST /api/auth/register`, `GET /api/events/**`, `/swagger-ui/**`, `/v3/api-docs/**`), CORS with `http://localhost:5173`, disable CSRF, stateless session, disable form login and HTTP Basic, register JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter, add PasswordEncoder (BCrypt) bean

**Checkpoint**: Public endpoints accessible without auth, CORS preflight works, unauthenticated requests to protected routes return 401

---

## Phase 4: User Story 2 - Authenticated member accesses protected resources (Priority: P1)

**Goal**: Members can log in, receive an httpOnly cookie, and use it to access member-level resources

**Independent Test**: POST `/api/auth/login` with valid credentials → receive `Set-Cookie: auth_token=<jwt>`; use that cookie on protected endpoints → 200; expired/tampered cookie → 401

### Tests for User Story 2 ⚠️

> **NOTE**: Write these tests FIRST, ensure they FAIL before implementation

- [x] T012 [P] [US2] Write AuthControllerTest for login success in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` (valid credentials → 200 + Set-Cookie)
- [x] T013 [P] [US2] Write AuthControllerTest for login failure in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` (invalid credentials → 401)
- [x] T014 [P] [US2] Write AuthControllerTest for registration in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` (valid registration → 201 + Set-Cookie)
- [x] T015 [P] [US2] Write AuthControllerTest for duplicate email in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` (existing email → 409)
- [x] T016 [P] [US2] Write AuthControllerTest for logout in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` (valid cookie → 200 + Max-Age=0)
- [x] T017 [P] [US2] Write AuthControllerTest for validation errors in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` (invalid input → 400)
- [x] T018 [US2] Write JwtAuthenticationFilterTest for valid cookie auth in `src/test/java/com/lionsclub/api/security/JwtAuthenticationFilterTest.java` (valid cookie → SecurityContext populated)
- [x] T019 [US2] Write JwtAuthenticationFilterTest for expired token in `src/test/java/com/lionsclub/api/security/JwtAuthenticationFilterTest.java` (expired JWT → 401)
- [x] T020 [US2] Write JwtAuthenticationFilterTest for tampered token in `src/test/java/com/lionsclub/api/security/JwtAuthenticationFilterTest.java` (invalid signature → 401)

### Implementation for User Story 2

- [x] T021 [US2] Create `AuthService` in `src/main/java/com/lionsclub/api/security/AuthService.java` — login (validate email+password → generate JWT), register (validate uniqueness → create user → generate JWT), logout (no-op on server side)
- [x] T022 [US2] Create `LoginRequest` DTO in `src/main/java/com/lionsclub/api/web/dto/LoginRequest.java` — email, password with Jakarta Validation annotations
- [x] T023 [US2] Create `RegisterRequest` DTO in `src/main/java/com/lionsclub/api/web/dto/RegisterRequest.java` — email, password, firstName, lastName with Jakarta Validation annotations
- [x] T024 [US2] Create `AuthResponse` DTO in `src/main/java/com/lionsclub/api/web/dto/AuthResponse.java` — message field
- [x] T025 [US2] Create `AuthController` in `src/main/java/com/lionsclub/api/web/AuthController.java` — POST `/api/auth/login` (authenticate → set cookie → return 200), POST `/api/auth/register` (create user → set cookie → return 201), POST `/api/auth/logout` (clear cookie → return 200)
- [x] T026 [US2] Wire AuthController cookie-setting: extract JWT from AuthService → create `ResponseCookie` with `auth_token`, httpOnly, SameSite=Lax, Path=/, Max-Age matching token expiry
- [x] T027 [US2] Add `AuthenticationEntryPoint` to SecurityConfig for 401 JSON responses (`{ "error": "Unauthorized" }`) in `src/main/java/com/lionsclub/api/security/SecurityConfig.java`

**Checkpoint**: Login/register/logout flows work end-to-end, cookie auth works, expired/tampered tokens return 401

---

## Phase 5: User Story 3 - Admin manages resources (Priority: P1)

**Goal**: ADMIN role is enforced for privileged endpoints; MEMBER role is denied

**Independent Test**: POST `/api/events` with ADMIN cookie → 201 Created; POST `/api/events` with MEMBER cookie → 403 Forbidden

### Tests for User Story 3 ⚠️

> **NOTE**: Write these tests FIRST, ensure they FAIL before implementation

- [x] T028 [P] [US3] Write SecurityConfigTest for ADMIN role access in `src/test/java/com/lionsclub/api/security/SecurityConfigTest.java` (ADMIN cookie on POST `/api/events` → 201)
- [x] T029 [P] [US3] Write SecurityConfigTest for MEMBER forbidden on admin endpoints in `src/test/java/com/lionsclub/api/security/SecurityConfigTest.java` (MEMBER cookie on POST `/api/events` → 403)
- [x] T030 [P] [US3] Write SecurityConfigTest for unauthenticated on protected endpoints in `src/test/java/com/lionsclub/api/security/SecurityConfigTest.java` (no cookie on POST `/api/events` → 401)

### Implementation for User Story 3

- [x] T031 [US3] Add `.hasRole("ADMIN")` to SecurityConfig for admin-only endpoints in `src/main/java/com/lionsclub/api/security/SecurityConfig.java`: POST `/api/events`, PUT `/api/events/**`, DELETE `/api/events/**`, `/api/admin/**`, GET `/api/contact`, POST/PUT/DELETE `/api/members`, DELETE `/api/forum/replies`, PATCH/DELETE `/api/forum/threads`
- [x] T032 [US3] Add `@EnableMethodSecurity` to SecurityConfig in `src/main/java/com/lionsclub/api/security/SecurityConfig.java`
- [x] T033 [US3] Add `AccessDeniedHandler` to SecurityConfig for 403 JSON responses (`{ "error": "Forbidden" }`) in `src/main/java/com/lionsclub/api/security/SecurityConfig.java`

**Checkpoint**: Role-based authorization fully enforced; ADMIN vs MEMBER distinction works for all protected endpoints

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T034 Update `OpenApiConfig` security scheme from bearer-jwt to cookie-based in `src/main/java/com/lionsclub/api/config/OpenApiConfig.java` (update SecurityScheme to `apiKey` type with cookie name `auth_token`)
- [x] T035 Run `./mvnw test` to verify all tests pass (39 tests — 18 existing + 21 new auth tests all green)
- [x] T036 Run `./mvnw verify` to verify PMD and Error Prone pass with no violations
- [ ] T037 Run quickstart.md validation scenarios against running application (requires manual startup)
- [x] T038 [P] Verify spec.md Assumptions section uses `app.jwt.expiration` consistently (no stale access-token-expiration references)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational (needs JwtTokenProvider for filter)
- **US2 (Phase 4)**: Depends on US1 completion (needs SecurityConfig + JwtAuthenticationFilter)
- **US3 (Phase 5)**: Depends on US2 completion (needs auth flow working before role checks are meaningful)
- **Polish (Final Phase)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational — independent of other stories
- **US2 (P1)**: Depends on US1 (needs working filter + SecurityConfig)
- **US3 (P1)**: Depends on US2 (needs working auth flow)
- **US4 (P2)**: Implicitly covered by US2 and US3 — no separate implementation phase needed (cookie contract documented in contracts/auth-api.md and verified by AuthControllerTest)

### Within Each Phase

- Tests MUST be written and FAIL before implementation (per constitution Test-First NON-NEGOTIABLE)
- [P] tasks within a phase can run in parallel
- Non-[P] tasks within a phase must run sequentially

### Parallel Opportunities

- T002 and T003 (JwtTokenProvider tests) can run in parallel
- T006+T007 (SecurityConfig public tests) can run in parallel
- T012-T017 (AuthControllerTest scenarios) can run in parallel
- T018-T020 (filter tests) can run in parallel
- T028-T030 (role tests) can run in parallel

---

## Parallel Example: Foundational Phase

```bash
# Launch all JwtConfig + JwtTokenProvider tasks together (different files, no dependencies):
Task: "Write JwtTokenProviderTest — token generation and validation"
Task: "Write JwtTokenProviderTest — expiry and signature tampering"
```

---

## Parallel Example: User Story 4 (AuthController)

```bash
# Launch all AuthControllerTest scenarios together:
Task: "Write AuthControllerTest — login success"
Task: "Write AuthControllerTest — login failure"
Task: "Write AuthControllerTest — registration"
Task: "Write AuthControllerTest — duplicate email"
Task: "Write AuthControllerTest — logout"
Task: "Write AuthControllerTest — validation errors"
```

---

## Implementation Strategy

### MVP First (Phase 3 — User Story 1 Only)

1. Complete Phase 1: Setup (T001)
2. Complete Phase 2: Foundational (T002-T005) — **CRITICAL**
3. Complete Phase 3: US1: Visitor browses public content (T006-T010)
4. **STOP and VALIDATE**: Public endpoints accessible without auth, protected endpoints return 401
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Token infrastructure ready
2. Add US1 (SecurityConfig + public endpoints) → Test independently → Deploy/Demo (MVP!)
3. Add US2 (login/register/logout + cookie auth) → Test independently → Deploy/Demo
4. Add US3 (role-based authorization) → Test independently → Deploy/Demo
5. Polish → Final QA and cleanup

### Single Developer Strategy

Sequential execution in order: Phase 1 → Phase 2 → Phase 3 (US1) → Phase 4 (US2) → Phase 5 (US3) → Phase 6. Within each phase, write failing tests first, then implement, then refactor (Red-Green-Refactor).

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All new files go under `src/main/java/com/lionsclub/api/security/` or `src/main/java/com/lionsclub/api/web/`
- All new tests go under `src/test/java/com/lionsclub/api/security/` or `src/test/java/com/lionsclub/api/web/`
