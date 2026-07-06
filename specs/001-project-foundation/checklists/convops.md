# ConvOps & CI Requirements Checklist: Project Foundation

**Purpose**: Validate the quality, clarity, and completeness of development operations and CI requirements (Docker Compose, build pipeline, portability, environment consistency)
**Created**: 2026-07-06
**Feature**: specs/001-project-foundation/spec.md

## Requirement Completeness

- [x] CHK001 - Are Docker Compose service specifications (image tag, port mappings, environment variables, volume mount path) fully defined? [Completeness, Spec §FR-001]
- [x] CHK002 - Are Docker Compose health check configuration details specified (command, interval, timeout, retries)? [Completeness, Spec §FR-001]
- [x] CHK003 - Are requirements defined for the .dockerignore file content? [Completeness, Gap]
- [x] CHK004 - Are requirements defined for Maven Wrapper (./mvnw) configuration and committed files? [Completeness, Gap]
- [x] CHK005 - Are requirements defined for application startup order (database readiness before app starts)? [Completeness, Gap]
- [x] CHK006 - Are requirements defined for container restart policies? [Completeness, Spec §FR-001]
- [x] CHK007 - Are requirements defined for Docker volume lifecycle (backup, restore, cleanup)? [Completeness, Gap]
- [x] CHK008 - Are requirements defined for test execution pipeline (unit, integration, contract tests)? [Completeness, Gap]

## Requirement Clarity

- [x] CHK009 - Is "named volume (pgdata)" specified with its mount path at /var/lib/postgresql/data? [Clarity, Spec §FR-001]
- [x] CHK010 - Is "PostgreSQL 15" clarified as the exact Docker image tag (15-alpine vs 15-bullseye vs 15)? [Clarity, Spec §FR-001]
- [x] CHK011 - Is "health endpoint at /actuator/health" clarified as the Actuator path or a custom endpoint? [Clarity, Spec §FR-008a]
- [x] CHK012 - Are Docker networking requirements documented (default bridge vs custom network)? [Clarity, Gap]
- [x] CHK013 - Is "runs on port 8080" clarified as host port mapping vs container port? [Clarity, Spec §Assumptions]

## Requirement Consistency

- [x] CHK014 - Do the Docker Compose environment variables align with the .env.example file? [Consistency, Spec §FR-001 vs §FR-011]
- [x] CHK015 - Does the Actuator health endpoint assumption align across US1 acceptance criteria, FR-008a, and SC-002? [Consistency, Spec §US1/AC3 vs §FR-008a vs §SC-002]
- [x] CHK016 - Is the application port consistent between Docker Compose (no app container), application.yml (server.port), and quickstart.md? [Consistency, Spec §FR-001 vs §FR-009 vs quickstart.md]
- [x] CHK017 - Do the quickstart validation commands (cp, docker compose, mvnw) match the SC-001 success criteria? [Consistency, Spec §SC-001 vs quickstart.md]

## Acceptance Criteria Quality

- [x] CHK018 - Can "PostgreSQL container starts and is healthy" (US1/AC1) be objectively verified via docker compose ps or health check? [Measurability, Spec §US1/AC1]
- [x] CHK019 - Is "all containers are stopped and removed" (US1/AC4) verifiable with docker compose ps --all? [Measurability, Spec §US1/AC4]
- [x] CHK020 - Is "under 5 minutes" (SC-001) measurable with specific start/stop timing? [Measurability, Spec §SC-001]
- [x] CHK021 - Can "healthy application on first attempt" (SC-002) be verified independently of network latency? [Measurability, Spec §SC-002]

## Scenario Coverage

- [x] CHK022 - Are requirements defined for multi-platform Docker support (linux/amd64, linux/arm64)? [Coverage, Gap]
- [x] CHK023 - Are requirements defined for CI pipeline integration (GitHub Actions, GitLab CI)? [Coverage, Gap]
- [x] CHK024 - Are requirements defined for local development without Docker (standalone PostgreSQL)? [Coverage, Gap]

## Edge Case & Failure Coverage

- [x] CHK025 - Is the expected behavior defined when Docker is not installed or Docker daemon is not running? [Edge Case, Spec §Edge Cases]
- [x] CHK026 - Is the expected behavior defined when port 5432 is already in use on the host? [Edge Case, Spec §Edge Cases]
- [x] CHK027 - Is the expected behavior defined when the Docker volume already exists with conflicting data? [Edge Case, Gap]
- [x] CHK028 - Is the expected behavior defined when Docker Compose fails to pull the PostgreSQL image? [Edge Case, Gap]
- [x] CHK029 - Is the expected behavior defined when the Maven build fails due to network issues (dependency download)? [Edge Case, Gap]
- [x] CHK030 - Is the expected behavior defined when ./mvnw is not executable or missing? [Edge Case, Gap]

## Non-Functional Requirements

- [x] CHK031 - Are requirements defined for Docker container resource limits (memory, CPU)? [Performance, Gap]
- [x] CHK032 - Are requirements defined for Docker image size optimization? [Performance, Gap]
- [x] CHK033 - Are requirements defined for build reproducibility (Maven dependency lock, Docker image digest pinning)? [Reliability, Gap]
- [x] CHK034 - Are requirements defined for log collection and retention from containers? [Observability, Gap]

## Accepted As-Is

Items accepted without changes (low severity, adequate for foundation phase):

- CHK003 — .dockerignore content: implementation detail; file exists with comprehensive patterns
- CHK005 — Startup order: Docker Compose health check + application fail-fast provides correct behavior
- CHK012 — Docker networking: default bridge network sufficient for single-service setup
- CHK027 — Existing Docker volume with data: edge case; Docker Compose behavior is standard
- CHK028 — Docker Compose image pull failure: standard Docker behavior; network error
- CHK029 — Maven build network failure: standard build issue; not spec-mandated
- CHK030 — ./mvnw missing or not executable: now documented in Assumptions
- CHK037 — Empty database assumption: now documented in Assumptions
- CHK041 — Multi-service networking: not applicable (single PostgreSQL service)

## Deferred to Later Phases

Items intentionally out of scope for Project Foundation:

- CHK004 — Maven Wrapper configuration: implementation detail; ./mvnw is standard and committed
- CHK007 — Docker volume lifecycle (backup, restore, cleanup): operational concern for production, not local dev
- CHK008 — Test execution pipeline (CI): Phase 2+ when tests exist across multiple phases
- CHK012 — Docker networking (custom bridge vs default): default bridge sufficient for single-service setup
- CHK022 — Multi-platform Docker support (linux/amd64, linux/arm64): deferred until needed
- CHK023 — CI pipeline integration (GitHub Actions, GitLab CI): deferred to Phase 2+
- CHK024 — Local development without Docker: not planned; Docker is required per assumptions
- CHK031-034 — Container resource limits, image size, build reproducibility, log collection: production deployment concerns

## Dependencies & Assumptions

- [x] CHK035 - Is the assumption that Docker and Docker Compose are installed on developer machines documented? [Assumption, Spec §Assumptions]
- [x] CHK036 - Is the assumption that ./mvnw is committed to the repository explicitly stated? [Assumption, Spec §Assumptions]
- [x] CHK037 - Is the assumption that the database will be empty on first run explicitly documented? [Assumption, Gap]
- [x] CHK038 - Is the assumption that port 8080 will be available documented as a potential issue? [Assumption, Spec §Assumptions]

## Ambiguities & Conflicts

- [x] CHK039 - Does FR-001 describe a complete Docker Compose setup or just the PostgreSQL service (no app container in Docker Compose)? [Ambiguity, Spec §FR-001]
- [x] CHK040 - Is there ambiguity about whether health check failures should restart the container or just mark it unhealthy? [Ambiguity, Spec §FR-001]
- [x] CHK041 - Is there a conflict between the Docker Compose default network behavior and any planned multi-service architecture? [Conflict, Gap]
