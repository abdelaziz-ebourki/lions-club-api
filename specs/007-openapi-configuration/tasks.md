# Tasks: SpringDoc OpenAPI Configuration

**Input**: Design documents from `specs/007-openapi-configuration/`

**Tests**: Tests are included per constitution G3 (Test-First Discipline).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Single Spring Boot project — `src/` and `src/test/` at repository root.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure — N/A for this feature. All infrastructure is already in place on `main` (SpringDoc dependency, OpenApiConfig bean, SecurityConfig permit rules). No setup tasks needed.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure blocking all user stories — N/A for this feature. SpringDoc is already configured and running. The remaining work consists of isolated edits to existing files.

---

## Phase 3: User Story 1 — Developer views API documentation in Swagger UI (Priority: P1) 🎯 MVP

**Goal**: Developers can browse all endpoints via Swagger UI at `/swagger-ui.html` without authentication.

**Independent Test**: Navigate to `/swagger-ui.html` without any auth headers — page loads with all endpoints listed and cookie-JWT security scheme visible.

### Implementation for User Story 1

- [X] T001 [P] [US1] Configure api-docs path in `src/main/resources/application.yml`:
      ```yaml
      springdoc:
        api-docs:
          path: /api-docs
        swagger-ui:
          path: /swagger-ui.html
      ```
- [X] T002 [P] [US1] Add `/api-docs` to SecurityConfig permit list in `src/main/java/com/lionsclub/api/security/SecurityConfig.java`

**Checkpoint**: Swagger UI should now be accessible at `/swagger-ui.html` without authentication, listing all endpoints.

---

## Phase 4: User Story 2 — Developer retrieves raw OpenAPI spec programmatically (Priority: P1)

**Goal**: CI pipelines and client generators can fetch `GET /api-docs` and receive valid OpenAPI 3.0 JSON with correct metadata.

**Independent Test**: `curl http://localhost:8080/api-docs | jq .info.version` returns `"0.1.0"` and paths include all 10 endpoints.

**⚠️ G3 - Test-First**: Update existing test to expect the new version BEFORE bumping the config.

### Tests for User Story 2

- [X] T003 [P] [US2] Update `OpenApiConfigTest` to expect version `"0.1.0"` in `src/test/java/com/lionsclub/api/config/OpenApiConfigTest.java` — write this FIRST so it fails, confirming the test works

### Implementation for User Story 2

- [X] T004 [P] [US2] Bump OpenAPI version from `0.0.1` to `0.1.0` in `src/main/java/com/lionsclub/api/config/OpenApiConfig.java`
 
 **Checkpoint**: `GET /api-docs` now returns valid OpenAPI 3.0 JSON with `info.version = "0.1.0"` and all endpoints.
 
 ---
 
 ## Phase 5: User Story 3 — Backend developer validates endpoint documentation completeness (Priority: P2)
 
 **Goal**: Every controller endpoint has `@Operation` and `@ApiResponse` annotations.
 
 **Independent Test**: `GET /api-docs` shows `summary` field on all 10 endpoints covering auth + events.
 
 **⚠️ G3 - Test-First**: Write the failing test for AuthController login/register/logout BEFORE adding annotations.
 
 ### Tests for User Story 3
 
 - [X] T005 [US3] Write a test in `src/test/java/com/lionsclub/api/web/AuthControllerTest.java` that verifies `@Operation` and `@ApiResponse` annotations exist on `login()`, `register()`, and `logout()` methods — confirm it fails initially
 
 ### Implementation for User Story 3
 
 - [X] T006 [P] [US3] Add `@Operation(summary = "...", description = "...")` on `AuthController.login()` in `src/main/java/com/lionsclub/api/web/AuthController.java`
 - [X] T007 [P] [US3] Add `@ApiResponse` annotations for 200 and 401 on `AuthController.login()` in `src/main/java/com/lionsclub/api/web/AuthController.java`
 - [X] T008 [P] [US3] Add `@Operation(summary = "...", description = "...")` on `AuthController.register()` in `src/main/java/com/lionsclub/api/web/AuthController.java`
 - [X] T009 [P] [US3] Add `@ApiResponse` annotations for 201 and 409 on `AuthController.register()` in `src/main/java/com/lionsclub/api/web/AuthController.java`
 - [X] T010 [P] [US3] Add `@Operation(summary = "...", description = "...")` on `AuthController.logout()` in `src/main/java/com/lionsclub/api/web/AuthController.java`
 - [X] T011 [P] [US3] Add `@ApiResponse` annotations for 200 on `AuthController.logout()` in `src/main/java/com/lionsclub/api/web/AuthController.java`
 
 **Checkpoint**: All 10 API endpoints now have complete OpenAPI documentation annotations. Tests pass.
 
 ---
 
 ## Phase 6: Polish & Cross-Cutting Concerns
 
 **Purpose**: Final validation and cleanup.
 
 - [X] T012 Run `./mvnw test` to confirm all tests pass
 - [X] T013 Run `quickstart.md` validation scenarios (api-docs, swagger-ui, unauthenticated access, auth endpoint docs)
 - [X] T014 Verify `GET /api-docs` returns 200 without auth headers via `curl`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1-2 (Setup/Foundational)**: N/A — already complete on `main`
- **Phase 3 (US1)**: Can start immediately — no dependencies
- **Phase 4 (US2)**: Can start immediately — no dependencies on other stories
- **Phase 5 (US3)**: Can start immediately — no dependencies on other stories
- **Phase 6 (Polish)**: Depends on Phases 3-5 completion

### User Story Dependencies

- **US1 (P1)**: No story dependencies — completely independent
- **US2 (P1)**: No story dependencies — completely independent
- **US3 (P2)**: No story dependencies — completely independent

### Parallel Opportunities

- All Phase 3-5 tasks marked **[P]** can run in parallel
- **US1, US2, and US3 are fully independent** — no shared file conflicts
  - US1 touches `application.yml` and `SecurityConfig.java`
  - US2 touches `OpenApiConfigTest.java` and `OpenApiConfig.java`
  - US3 touches `AuthController.java` and `AuthControllerTest.java`
  - No file overlaps between stories → full parallel execution possible

---

## Parallel Example: All User Stories

```bash
# US1 - Configure api-docs path:
Task: "Add springdoc.api-docs.path to application.yml"

# US2 - Fix version in config + test:
Task: "Update OpenApiConfigTest to expect 0.1.0"
Task: "Bump OpenApiConfig version to 0.1.0"

# US3 - Annotate auth endpoints:
Task: "Add @Operation + @ApiResponse to login()"
Task: "Add @Operation + @ApiResponse to register()"
Task: "Add @Operation + @ApiResponse to logout()"
```

---

## Implementation Strategy

### MVP First (US1 + US2 — Both P1)

1. Complete all Phase 3 tasks — Swagger UI is accessible
2. Complete Phase 4 tasks — Raw spec is fetchable with correct metadata
3. **STOP and VALIDATE**: Both P1 stories independently testable → deploy/demo-ready

### Incremental Delivery

1. Phase 3 + 4 → P1 features complete → MVP ready
2. Phase 5 → P2 completeness validation → quality gate
3. Phase 6 → Final validation → ready to merge

### Parallel Strategy (Single Developer)

1. Complete T001 + T002 (US1) — sequential (SecurityConfig needs application.yml)
2. Complete T003 + T004 (US2) — test-first order
3. Complete T005 through T011 (US3) — test-first, then all annotations in parallel
4. Complete T012-T014 (Polish) — final validation
