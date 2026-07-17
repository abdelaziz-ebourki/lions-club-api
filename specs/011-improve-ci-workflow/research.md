# Research: Improve CI Workflow

## Overview

Research decisions for adding concurrency cancellation, test report artifacts, manual trigger, dependency vulnerability scanning, and fail-fast PMD checks to the GitHub Actions CI workflow.

## Decisions

### 1. Concurrency Cancellation

- **Decision**: Use `concurrency.group: ${{ github.workflow }}-${{ github.ref }}` with `cancel-in-progress: true` at the workflow level.
- **Rationale**: Groups CI runs by workflow + branch reference. When a new push occurs on the same branch, the in-progress run is cancelled automatically. This is the standard GitHub Actions pattern and requires minimal YAML.
- **Alternatives Considered**:
  - Job-level concurrency: More granular but unnecessary since this workflow has a single job.
  - No concurrency (current state): Wastes runner minutes on redundant runs.

### 2. Test Report Artifacts

- **Decision**: Add `actions/upload-artifact@v4` step with `if: failure()` condition, uploading `target/surefire-reports/` and `target/failsafe-reports/`.
- **Rationale**: Uploading only on failure minimizes storage costs. The `if: failure()` expression ensures artifacts are available exactly when needed for debugging. Test report directories are standard Maven output locations.
- **Alternatives Considered**:
  - Upload on always: Simple but wastes storage on successful runs.
  - Upload individual XML files: More targeted but requires complex path filtering.

### 3. Manual CI Trigger

- **Decision**: Add `workflow_dispatch` to the `on:` triggers with no required inputs.
- **Rationale**: Minimal configuration — no inputs needed since the workflow already supports branch selection via GitHub UI. Adding inputs would be over-engineering for the current use case.
- **Alternatives Considered**:
  - `workflow_dispatch` with branch input: Redundant, GitHub UI already provides branch selection.
  - `repository_dispatch`: Useful for external triggers but unnecessary for manual UI triggering.

### 4. Dependency Vulnerability Scanning

- **Decision**: Add `actions/dependency-review-action@v4` as a separate job running only on `pull_request` events.
- **Rationale**: Zero configuration required — no Maven plugin installation, no pom.xml changes. Runs as a separate, quick job (not blocking the build job). Uses GitHub's native vulnerability database. Configured with `fail-on-severity: never` so findings are reported as annotations on the PR without failing the build (matching the spec's informational-only requirement).
- **Alternatives Considered**:
  - `org.owasp:dependency-check-maven`: Requires pom.xml changes, runs during Maven build, increases total CI time. Better for push-triggered scanning but adds build complexity.
  - `actions/dependency-review-action@v4` (fail mode): Using default `fail-on-severity: critical` would fail the build, violating the informational-only assumption.

### 5. Fail-Fast PMD

- **Decision**: Add a separate `Run PMD check` step before the existing `Build and test` step, executing `./mvnw pmd:check -q`. The existing PMD Maven plugin configuration in `pom.xml` is reused.
- **Rationale**: Running PMD as a separate early step causes the build to fail fast on code style violations without running the full test suite. This is cleaner than binding PMD to the `compile` phase in pom.xml, which would change local build behavior.
- **Alternatives Considered**:
  - Bind PMD to `compile` phase in pom.xml: Changes local developer experience, which is undesirable.
  - Combine with the existing `verify` phase: Current behavior, but PMD violations are only discovered after tests complete.

## Key Findings

- Current CI workflow has no concurrency control, no artifact upload, and no manual trigger capability.
- PMD plugin is already configured in `pom.xml` with a custom ruleset at `config/pmd-ruleset.xml` — only a Maven goal invocation is needed.
- All changes are additions to `.github/workflows/ci.yml` — no existing lines are modified or removed.
- No application source code, tests, or configuration files outside CI are affected.
- Estimated net YAML additions: ~35 lines (concurrency: 3, artifact upload: 8, workflow_dispatch: 1, dependency review job: 15, PMD step: 8).
