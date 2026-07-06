# Requirements Quality Checklist: Project Foundation

**Purpose**: Validate the quality, clarity, and completeness of requirements for the project foundation (Docker Compose, Flyway migrations, SpringDoc, dev environment)
**Created**: 2026-07-06
**Feature**: specs/001-project-foundation/spec.md

## Requirement Completeness

- [x] CHK001 - Are Docker Compose service specifications (image tag, port mappings, volume mount path) defined for PostgreSQL? [Completeness, Spec §FR-001]
- [x] CHK002 - Are Flyway configuration details (baseline-on-migrate, fail-on-missing-locations) explicitly required? [Completeness, Spec §FR-002]
- [x] CHK003 - Is the initial V1 migration content explicitly specified rather than just "initial schema structure"? [Completeness, Spec §FR-004]
- [x] CHK004 - Are the actual SpringDoc title, version string, and description values required in the spec? [Completeness, Spec §FR-005]
- [x] CHK005 - Are environment-specific differences between dev and prod profiles documented? [Completeness, Spec §FR-010]
- [x] CHK006 - Is the Spring Boot Actuator health endpoint required as a dependency (referenced in US1 but missing from FRs)? [Gap, Spec §US1-Acceptance-3]
- [x] CHK007 - Are the required `.env` variables listed with their default values and descriptions? [Completeness, Spec §FR-011]
- [x] CHK008 - Is the database container name specified for Docker command references in quickstart? [Completeness, Spec §FR-001]

## Requirement Clarity

- [x] CHK009 - Is "persistent volume storage" specified with a named volume key and container mount path? [Clarity, Spec §FR-001]
- [x] CHK010 - Is the Flyway `locations` path precisely specified (classpath:db/migration vs filesystem)? [Clarity, Spec §FR-003]
- [x] CHK011 - Is "profile gating for dev/test" defined as Spring profile activation behavior or SpringDoc @Profile annotation? [Clarity, Spec §FR-006]
- [x] CHK012 - Are "required environment variables" enumerated with their exact names and purposes? [Clarity, Spec §FR-011]
- [x] CHK013 - Is "database connection properties" specified with the expected URL format, credentials, and driver? [Clarity, Spec §FR-009]
- [x] CHK014 - Is the V1 migration naming convention enforced (V1__description.sql vs V1_0__description.sql)? [Clarity, Spec §FR-004]

## Requirement Consistency

- [x] CHK015 - Does the SpringDoc profile gating requirement (FR-006/FR-007 for dev/test only) align with the quickstart prod profile scenario? [Consistency, Spec §FR-006 vs Quickstart §Profile]
- [x] CHK016 - Does "initial schema structure" (FR-004) align with the assumption that V1 creates the `users` table? [Consistency, Spec §FR-004 vs Assumptions]
- [x] CHK017 - Does the Actuator health endpoint assumption (US1 acceptance) conflict with the absence of an actuator FR? [Consistency, Spec §US1-Acceptance-3 vs FRs]
- [x] CHK018 - Are PostgreSQL version requirements consistent between FR-001 (PostgreSQL 15) and the Assumptions section? [Consistency, Spec §FR-001 vs Assumptions]

## Acceptance Criteria Quality

- [x] CHK019 - Can SC-001 ("under 5 minutes, at most 3 commands") be objectively measured and verified? [Measurability, Spec §SC-001]
- [x] CHK020 - Is "healthy application" (SC-002) defined with specific health indicators beyond actuator status? [Measurability, Spec §SC-002]
- [x] CHK021 - Can "immdediately after application startup" (SC-004) be verified with a specific timeout threshold? [Measurability, Spec §SC-004]
- [x] CHK022 - Are the acceptance scenario outcomes ("starts and is healthy", "receives 200 OK") verifiable without manual inspection? [Acceptance Criteria, Spec §US1]
- [x] CHK023 - Do the success criteria cover all three user stories (bootstrap, migrations, docs)? [Coverage, Spec §SC-001 through SC-004]

## Scenario Coverage

- [x] CHK024 - Are requirements defined for the application startup order (DB must be ready before app starts)? [Coverage, Gap]
- [x] CHK025 - Are requirements for Docker Compose health checks on the PostgreSQL service specified? [Coverage, Spec §US1]
- [x] CHK026 - Are requirements for running without Docker (e.g., local PostgreSQL install) defined as out of scope or explicitly excluded? [Coverage, Assumptions]
- [x] CHK027 - Are requirements for migration rollback or repair strategy defined? [Coverage, Gap]
- [x] CHK028 - Are requirements for SpringDoc customisation (logo, contact info, license) beyond title/version/description specified? [Coverage, Spec §FR-005]

## Edge Case & Failure Coverage

- [x] CHK029 - Is the expected behaviour defined when Docker port 5432 is already in use by another PostgreSQL instance? [Edge Case, Spec §Edge Cases]
- [x] CHK030 - Are requirements for Flyway out-of-order migration detection specified? [Edge Case, Gap]
- [x] CHK031 - Is the application startup behaviour specified when the .env file is missing? [Edge Case, Spec §FR-011]
- [x] CHK032 - Are requirements defined for database connection retry on startup when PostgreSQL is not yet ready? [Edge Case, Gap]
- [x] CHK033 - Is SpringDoc behavior defined when no controllers exist (empty paths)? [Edge Case, Spec §FR-006]
- [x] CHK034 - Are requirements for handling Flyway migration failure during startup specified? [Edge Case, Gap]

## Non-Functional Requirements

- [x] CHK035 - Are security requirements defined for the dev profile (e.g., relaxed CORS, verbose errors) vs prod (strict)? [Security, Spec §FR-010]
- [x] CHK036 - Are observability requirements (logging level, format, destinations) specified for dev vs prod? [Observability, Gap]
- [x] CHK037 - Are performance constraints specified for Docker container resource limits (memory/CPU)? [Performance, Gap]

## Accepted As-Is

Items accepted without changes (low severity, adequate for foundation phase):

- CHK002 — Flyway config details (baseline-on-migrate): documented in plan.md/research.md; spec uses FR-002 as a high-level requirement; detail level is appropriate for foundation phase
- CHK019 — SC-001 "under 5 minutes": measurable via stopwatch; acceptable for foundation phase without stricter timing
- CHK020 — "Healthy application": defined by actuator UP status; sufficient for foundation phase
- CHK021 — "Immediately": no strict timeout needed for local dev; actionable in seconds
- CHK024 — DB startup order: Docker Compose health check + application fail-fast ensures correct behavior; explicit spec not needed
- CHK039 — ./mvnw committed: now documented in Assumptions
- CHK040 — Empty database: now documented in Assumptions

## Deferred to Later Phases

Items intentionally out of scope for Project Foundation:

- CHK027 — Migration rollback/repair strategy: planned for Phase 2+ when rollback migrations are added
- CHK030 — Flyway out-of-order migration detection: out-of-order opt-in not needed for initial linear migrations
- CHK032 — Database connection retry on startup: Docker Compose health check is the expected mechanism; app-level retry deferred
- CHK034 — Flyway migration failure handling during startup: Flyway's default fail-fast behavior is acceptable for this phase
- CHK036 — Observability/logging configuration: detailed logging destinations and formats deferred to Phase 2+
- CHK037 — Docker container resource limits: not applicable for single-developer local dev

## Dependencies & Assumptions

- [x] CHK038 - Is the dependency on Java 21 being available on the dev machine validated or assumed? [Assumption, Spec §Assumptions]
- [x] CHK039 - Is the assumption that `./mvnw` is committed to the repository explicitly stated? [Assumption, Spec §Assumptions]
- [x] CHK040 - Is the assumption that the database will be empty on first run documented? [Assumption, Gap]
- [x] CHK041 - Are the port availability assumptions (8080, 5432) flagged as potential conflicts? [Assumption, Spec §Assumptions]
- [x] CHK042 - Is the assumption that the V1 migration creates the `users` table clearly documented as a cross-phase dependency? [Assumption, Spec §Assumptions]
