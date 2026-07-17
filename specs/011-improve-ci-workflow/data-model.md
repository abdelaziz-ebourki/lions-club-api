# Data Model: Improve CI Workflow

## Overview

This feature modifies the GitHub Actions CI workflow configuration — no application data entities are introduced. The "data model" below documents the workflow configuration structure that defines the CI pipeline behavior.

## Workflow Configuration Model

### Workflow (`.github/workflows/ci.yml`)

| Property | Type | Values | Description |
|----------|------|--------|-------------|
| `name` | string | `"CI"` | Workflow display name |
| `on` | trigger set | `push`, `pull_request`, `workflow_dispatch` | Events that trigger the workflow |
| `concurrency` | concurrency config | group + cancel-in-progress | Controls run cancellation |
| `jobs` | job map | `build`, `dependency-review` | Collection of jobs to run |

### Trigger Events

| Event | Scope | Concurrency | Purpose |
|-------|-------|-------------|---------|
| `push` | `branches: [main]` | Yes — cancels duplicate | Builds main branch on merge |
| `pull_request` | `branches: [main]` | Yes — cancels duplicate | Validates PRs before merge |
| `workflow_dispatch` | any branch | No — never auto-cancelled | Manual re-runs and ad-hoc testing |

### Jobs

#### `build` Job

| Property | Value | Description |
|----------|-------|-------------|
| `runs-on` | `ubuntu-latest` | Runner image |
| `services.postgres` | `postgres:15-alpine` | Test database |
| Steps (ordered) | See below | Execution sequence within job |

**Build Job Steps**:

| Step | Action | When | Purpose |
|------|--------|------|---------|
| 1. Checkout | `actions/checkout@v5` | Always | Fetch source code |
| 2. Set up JDK 21 | `actions/setup-java@v5` | Always | Configure Java + Maven cache |
| 3. Make mvnw executable | `chmod +x ./mvnw` | Always | Ensure Maven wrapper works |
| 4. Run PMD check | `./mvnw pmd:check -q` | Always | Fail-fast on code quality violations |
| 5. Build and test | `./mvnw verify -q` | Always | Compile, run tests, package |
| 6. Upload test reports | `actions/upload-artifact@v4` | On failure only | Provide failure diagnostics |

#### `dependency-review` Job

| Property | Value | Description |
|----------|-------|-------------|
| `runs-on` | `ubuntu-latest` | Runner image |
| `if` | `github.event_name == 'pull_request'` | Only runs on PR events |
| Step | `actions/dependency-review-action@v4` | Scan dependency changes for CVEs |

## Validation Rules (from spec)

| Rule | Applies To | Description |
|------|-----------|-------------|
| FR-001 | Concurrency | Non-default branch pushes cancel in-progress runs |
| FR-002 | Artifacts | Reports uploaded only on failure |
| FR-003 | Triggers | Workflow dispatch available on UI |
| FR-004 | Dependency check | Vulnerabilities reported, build not failed |
| FR-005 | PMD check | Fail build if violations found, before tests |
| FR-006 | All | Existing checks remain green |

## State Transitions

```
push to PR branch
  → if run in progress → cancel in-progress → start new run
  → if no run in progress → start new run

pull_request (opened/synchronize)
  → same as push above
  → additionally: trigger dependency-review job

workflow_dispatch (manual)
  → start new run (never auto-cancelled)
  → skip dependency-review job (not a PR event)

test failure
  → upload surefire-reports/ and failsafe-reports/ as artifacts

PMD violation
  → fail immediately (step 4), skip build and test steps
```

## File Impact

| File | Change Type |
|------|-------------|
| `.github/workflows/ci.yml` | Modified — additions only |
