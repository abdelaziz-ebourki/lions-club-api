# Tasks: Improve CI Workflow

**Input**: Design documents from `/specs/011-improve-ci-workflow/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Not applicable — this feature modifies CI workflow configuration, not application code. Validation is performed by observing CI run behavior per quickstart.md scenarios.

**Organization**: Tasks are grouped by user story. All changes target a single file (`.github/workflows/ci.yml`), so tasks are sequential within the file but each user story is independently testable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Single file: `.github/workflows/ci.yml`

---

## Phase 1: Setup

**Purpose**: Understand the current CI workflow and create the implementation branch

- [x] T001 Read and understand the current workflow structure in `.github/workflows/ci.yml`
- [x] T002 Create a feature branch for CI improvements

---

## Phase 2: Foundational (Workflow-Level Changes)

**Purpose**: Workflow-level configuration that enables multiple user stories. These are the first edits to `.github/workflows/ci.yml` — adding new triggers and concurrency control.

- [x] T003 Add `workflow_dispatch` to the `on:` trigger list in `.github/workflows/ci.yml` to enable manual CI triggering from GitHub UI (supports US3)
- [x] T004 Add `concurrency` block at the workflow level in `.github/workflows/ci.yml` with `group: ${{ github.workflow }}-${{ github.ref }}` and `cancel-in-progress: true` (supports US1)

**Checkpoint**: Workflow now supports manual dispatch and concurrency cancellation. Push to a branch and verify concurrency by pushing twice in quick succession.

---

## Phase 3: User Story 1 — Automatic Cancellation of Redundant CI Runs (P1, MVP)

**Goal**: CI automatically cancels in-progress runs when a new push is made to the same branch, saving runner minutes and speeding up feedback.

**Independent Test**: Push to a PR branch while a CI run is in progress — the first run is cancelled within seconds and a new run starts for the latest commit.

- [ ] T005 [US1] Verify concurrency cancellation by pushing twice to the same branch and confirming the first run is cancelled in `.github/workflows/ci.yml`

**Note**: The concurrency block was added in T004. This task validates that behavior works correctly via a live CI test.

**Checkpoint**: US1 complete — concurrency cancellation works. A developer can push to a PR without waiting for the previous run.

---

## Phase 4: User Story 2 — Debugging Test Failures from CI Artifacts (P1, MVP)

**Goal**: When CI tests fail, test reports with full failure details are available for download from the CI run page.

**Independent Test**: Introduce a deliberate test failure, push to a branch, and verify that test report artifacts are downloadable from the failed CI run.

- [x] T006 [US2] Add `actions/upload-artifact@v4` step after the `Build and test` step in `.github/workflows/ci.yml` with `if: failure()` condition, uploading `target/surefire-reports/` and `target/failsafe-reports/` as artifact name `test-reports`

**Checkpoint**: US2 complete — test reports are uploaded on failure. Introduce a test failure, push, and confirm artifacts appear.

---

## Phase 5: User Story 3 — Manual CI Trigger Without Code Push (P2)

**Goal**: Maintainers can trigger CI on any branch from the GitHub UI without pushing a new commit.

**Independent Test**: Go to Actions tab → select workflow → "Run workflow" → pick a branch → confirm run starts.

- [ ] T007 [US3] Verify `workflow_dispatch` trigger works by manually triggering the CI workflow from the GitHub Actions UI on a non-main branch in `.github/workflows/ci.yml`

**Note**: Added in T003. This task validates the behavior end-to-end.

**Checkpoint**: US3 complete — manual CI triggers work from GitHub UI.

---

## Phase 6: User Story 4 — Dependency Vulnerability Scanning (P3)

**Goal**: CI checks project dependencies for known security vulnerabilities on PR events and reports findings without failing the build.

**Independent Test**: Open a PR that modifies dependencies; the "Dependency Review" job should appear in the CI run and report any vulnerable dependencies as PR annotations.

- [x] T008 [US4] Add a `dependency-review` job to `.github/workflows/ci.yml` running `actions/dependency-review-action@v4` with `fail-on-severity: never`, gated by `if: github.event_name == 'pull_request'`

**Checkpoint**: US4 complete — dependency review runs on PRs and reports vulnerabilities as annotations without blocking.

---

## Phase 7: User Story 5 — Early Code Quality Failure (P3)

**Goal**: Code quality violations (PMD) are caught before tests run, providing faster feedback to developers.

**Independent Test**: Introduce a deliberate PMD violation (e.g., unused import), push, and verify CI fails at the PMD step before reaching the build/test step.

- [x] T009 [US5] Add a `Run PMD check` step before the `Build and test` step in `.github/workflows/ci.yml` executing `./mvnw pmd:check -q` to fail fast on code quality violations

**Checkpoint**: US5 complete — PMD violations are caught early, before test execution begins.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [x] T010 Run all quickstart.md validation scenarios to confirm end-to-end functionality
- [x] T011 Commit the changes and open a PR with the feature description referencing GitHub issue #20

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — edits to `.github/workflows/ci.yml`
- **User Stories (Phase 3–7)**: Each depends on Foundational phase completion
  - All phases modify the same single file (`.github/workflows/ci.yml`) so they MUST be applied sequentially
  - However, each user story can be INDEPENDENTLY TESTED by pushing the cumulative changes to a branch and verifying behavior
- **Polish (Phase 8)**: Depends on all user story phases

### User Story Dependencies

- **US1 (P1)**: Depends on T004 (concurrency block). Testable immediately after.
- **US2 (P1)**: Depends on `Build and test` step existing (already present). No dependency on other US phases.
- **US3 (P2)**: Depends on T003 (workflow_dispatch). No dependency on other US phases.
- **US4 (P3)**: No dependency on other US phases — runs as a separate job.
- **US5 (P3)**: Depends on `Build and test` step existing (already present). No dependency on other US phases.

### Parallel Opportunities

- **Within a phase**: All tasks in Phase 1 can run in parallel (separate concerns).
- **Across phases**: All phases must be sequential (single file). However, each US can be tested independently after it's added.
- **No [P] markers**: Since all YAML edits target the same file, parallel execution is not possible.

---

## Parallel Example: Per-User-Story Validation

```bash
# Test US1 (Concurrency):
# Push twice in succession to the same branch:
git push origin feature-branch
# Immediately: make a trivial change and push again
# Expected: first run cancelled, second run completes

# Test US2 (Artifacts):
# Introduce a test failure and push:
# Edit a test to assert false, commit, push
# Expected: CI fails, test-reports artifact available

# Test US3 (Manual trigger):
# GitHub UI → Actions → CI → "Run workflow" → select branch
# Expected: new run starts labeled "workflow_dispatch"

# Test US4 (Dependency review):
# Open a PR → check "Dependency Review" job in CI run
# Expected: job passes with no annotations

# Test US5 (PMD fail-fast):
# Introduce an unused import, commit, push
# Expected: CI fails at PMD step, build/test step is skipped
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (workflow_dispatch + concurrency)
3. Complete Phase 3: User Story 1 (concurrency — P1)
4. Complete Phase 4: User Story 2 (artifacts — P1)
5. **STOP and VALIDATE**: Concurrency + artifacts both working
6. Deploy/demo the MVP

### Incremental Delivery

1. Phase 1–2: Foundation ready (workflow_dispatch + concurrency added)
2. Phase 3–4: Add US1 + US2 → Test independently → Deploy/Demo (MVP!)
3. Phase 5: Add US3 → Test independently → Deploy/Demo
4. Phase 6: Add US4 → Test independently → Deploy/Demo
5. Phase 7: Add US5 → Test independently → Deploy/Demo
6. Each story adds value independently; all previous stories remain functional

### Ordering Recommendation

Apply the YAML changes in this order to minimize merge conflicts and maintain readability:

1. `workflow_dispatch` trigger (top of file, `on:` block)
2. `concurrency` block (right after `on:` block)
3. `PMD` step (before `Build and test` in build job)
4. Artifact upload step (after `Build and test` in build job)
5. `dependency-review` job (after `build` job at bottom of file)

---

## Notes

- All changes target a single file: `.github/workflows/ci.yml`
- No [P] markers — all tasks modify the same file and must be sequential
- Tests are NOT applicable — this is CI configuration, not application code
- Validation is performed via quickstart.md scenarios on live CI runs
- Commit after each logical group (Phase 2, Phase 3+4, Phase 5, Phase 6, Phase 7)
- Stop at any checkpoint to validate the cumulative changes independently
