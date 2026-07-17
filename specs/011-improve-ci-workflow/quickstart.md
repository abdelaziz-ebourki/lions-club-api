# Quickstart Validation Guide: Improve CI Workflow

**Feature**: 011-improve-ci-workflow
**Purpose**: Verify the CI workflow improvements work end-to-end after implementation

## Prerequisites

- GitHub repository with Actions enabled
- Write access to push branches and create PRs
- `.github/workflows/ci.yml` modified with all planned changes

## Setup

```bash
cd /home/abdelaziz/Desktop/portfolio/lions-club-remastered/lions-club-api

# Create a test branch for validation
git checkout -b test/ci-workflow-validation

# Make a trivial change (e.g., add a comment to ci.yml)
# Push to trigger CI
git add .github/workflows/ci.yml
git commit -m "test: validate CI workflow improvements"
git push origin test/ci-workflow-validation
```

## Validation Scenarios

### Scenario 1: CI Triggered on Push

1. Push to `test/ci-workflow-validation`
2. Go to GitHub → Actions tab
3. **Expected**: A new CI run starts for `test/ci-workflow-validation`

---

### Scenario 2: Concurrency Cancellation

1. Push to `test/ci-workflow-validation` (first push)
2. While run is in progress, push again immediately
3. **Expected**: First run is cancelled (status: "Cancelled"), second run starts and completes

---

### Scenario 3: Manual Trigger via workflow_dispatch

1. Go to GitHub → Actions → "CI" workflow
2. Click "Run workflow" → select `test/ci-workflow-validation` branch
3. **Expected**: A new run starts labeled "workflow_dispatch" in the event column

---

### Scenario 4: Artifact Upload on Failure

1. Introduce a deliberate test failure (e.g., modify a test to assert false)
2. Commit and push
3. Go to the failed CI run
4. **Expected**: An artifact named `test-reports` is available for download
5. Download and extract: contains XML test reports with failure details

---

### Scenario 5: No Artifacts on Success

1. Ensure all tests pass
2. Push
3. **Expected**: CI run is green; no artifacts section appears

---

### Scenario 6: PMD Fail-Fast

1. Introduce a deliberate PMD violation (e.g., an unused import)
2. Commit and push
3. **Expected**: CI fails at the "Run PMD check" step; the "Build and test" step is skipped entirely

---

### Scenario 7: Dependency Vulnerability Scan on PR

1. Create a branch, push, open a PR
2. **Expected**: A "Dependency Review" job appears in the CI run
3. If no vulnerable dependencies are introduced, the job passes with no annotations

---

### Scenario 8: All Existing Checks Pass (Regression Test)

1. Ensure no test failures or PMD violations
2. Push to a PR targeting `main`
3. **Expected**: All steps pass: PMD check → build → test → (no artifacts)

## Expected Test Output

```
After implementation:
- All CI runs triggered by push/PR/manual: SUCCESS
- Concurrency cancellation: verified (run cancelled on second push)
- Artifact upload on failure: verified (test-reports downloadable)
- No artifacts on success: verified (no artifacts section)
- PMD fail-fast: verified (build stops at PMD step)
- Dependency review on PR: verified (job runs, passes)
```

## Debugging

- **Concurrency not cancelling**: Verify `concurrency.group` and `cancel-in-progress` are set at the workflow level (not job level).
- **Artifacts not uploading**: Check `if: failure()` expression — may need to use `if: ${{ failure() }}`.
- **PMD not failing**: Verify `./mvnw pmd:check` returns non-zero exit code on violations.
- **Dependency review not running**: Verify `if: github.event_name == 'pull_request'` condition.
- **Workflow dispatch missing**: Verify `workflow_dispatch` is in the `on:` list.

## Rollback

Revert `.github/workflows/ci.yml` to its previous state:

```bash
git checkout HEAD~1 -- .github/workflows/ci.yml
git commit -m "revert: restore CI workflow to previous state"
```
