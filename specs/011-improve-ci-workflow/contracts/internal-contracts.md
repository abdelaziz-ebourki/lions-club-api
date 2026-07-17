# Internal Contracts: Improve CI Workflow

## Purpose

This CI workflow is an internal contract between the repository and its developers. It defines how the CI pipeline is triggered, what it produces, and how developers interact with its outputs.

## Trigger Interface

The workflow accepts three trigger events:

| Trigger | Source | Branch Scope | Concurrency |
|---------|--------|-------------|-------------|
| `push` | Git push to `main` | `main` only | Auto-cancel on new push |
| `pull_request` | PR open/sync targeting `main` | Any PR branch | Auto-cancel on new push |
| `workflow_dispatch` | Manual via GitHub UI | Any branch | No auto-cancel |

### Manual Dispatch Inputs

None required. Branch selection is handled by the GitHub UI workflow dispatch form.

## Output Contract

### On Success

- GitHub check run marked green (✅)
- No artifacts uploaded

### On Build/Test Failure

- GitHub check run marked red (❌)
- Artifact `test-reports` available containing:
  - `target/surefire-reports/*.xml` — Unit test results
  - `target/failsafe-reports/*.xml` — Integration test results

### On PMD Violation

- GitHub check run marked red (❌)
- Failure occurs at the PMD step, before any test steps execute
- Error output contains PMD violation details (file, line, rule violated)

### On Dependency Vulnerability Found (PR only)

- PR receives annotations with vulnerability details
- Check run passes (informational only, does not block merge)

## Developer Workflow Contract

1. **Push to a PR**: Triggers CI; any previous run on the same PR is auto-cancelled.
2. **View CI failure**: Navigate to Actions tab → failed run → download `test-reports` artifact.
3. **Manual re-run**: Go to Actions → select workflow → "Run workflow" → choose branch.
4. **Check dependency vulnerabilities**: Open PR → scroll to "Dependency Review" check → view reported vulnerabilities.
5. **Fix PMD violations**: Run `./mvnw pmd:check` locally to reproduce; fix violations; push again.

## Guarantees

- Manual triggers (`workflow_dispatch`) are never auto-cancelled by concurrency.
- Artifacts are uploaded only on failure, minimizing storage consumption.
- Dependency review runs only on `pull_request` events (not on `push` or `workflow_dispatch`).
- All existing CI checks (build, test) remain unchanged in behavior and ordering.
