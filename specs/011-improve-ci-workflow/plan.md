# Implementation Plan: Improve CI Workflow

**Branch**: `` | **Date**: 2026-07-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/011-improve-ci-workflow/spec.md`

## Summary

Improve the GitHub Actions CI workflow (`.github/workflows/ci.yml`) by adding concurrency-based cancellation of redundant runs, uploading test report artifacts on failure, enabling manual workflow dispatch, integrating dependency vulnerability scanning, and running PMD code quality checks before tests for fast failure. No application source code changes are required — all changes are confined to CI configuration.

## Technical Context

**Language/Version**: YAML (GitHub Actions workflow syntax)

**Primary Dependencies**: GitHub Actions (`actions/checkout@v5`, `actions/setup-java@v5`, `actions/upload-artifact@v4`), Maven 3.9+ (existing), `maven-pmd-plugin` (existing)

**Storage**: GitHub Actions artifact store (test reports on failure)

**Testing**: Existing Maven test suite (JUnit 5, Surefire, Failsafe); CI workflow validated by observing run behavior on push/PR/manual trigger

**Target Platform**: GitHub Actions CI (ubuntu-latest)

**Project Type**: CI/CD workflow configuration for a Spring Boot 3.4 / Java 21 REST API

**Performance Goals**: Reduce wasted CI runner minutes by cancelling redundant in-progress runs; no increase in total CI runtime for single runs

**Constraints**: All existing CI checks must remain green; changes affect only `.github/workflows/ci.yml`; concurrency must not cancel deployment or manually triggered runs

**Scale/Scope**: Single workflow file, approximately 10 lines of new YAML configuration

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **G1 - API-First**: N/A — no API contracts change; workflow affects internal CI only.
- [x] **G2 - Security by Design**: N/A — no authentication/authorization changes; dependency scanning improves security posture.
- [x] **G3 - Test-First (NON-NEGOTIABLE)**: N/A — CI configuration, not application code. Validation is via CI run behavior.
- [x] **G4 - DB Migration Rigor**: N/A — no database schema changes.
- [x] **G5 - Clean Architecture**: N/A — no application source code changes; layering is unaffected.

**Result**: All gates pass. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/011-improve-ci-workflow/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
.github/workflows/
└── ci.yml               # Only file modified by this feature
```

**Structure Decision**: Single workflow file at `.github/workflows/ci.yml`. No new files created in source tree. No application source code changes.

## Complexity Tracking

No Constitution violations exist. Complexity tracking not required.
