# Configuration Requirements Checklist: Project Foundation

**Purpose**: Validate the quality, clarity, and completeness of configuration and environment requirements (application.yml, profiles, .env, secrets management)
**Created**: 2026-07-06
**Feature**: specs/001-project-foundation/spec.md

## Requirement Completeness

- [x] CHK001 - Are all required environment variables listed in .env.example with default values and descriptions? [Completeness, Spec §FR-011]
- [x] CHK002 - Are profile-specific property overrides fully specified (what changes between dev and prod)? [Completeness, Spec §FR-010]
- [x] CHK003 - Are database connection properties (URL, driver, credentials) fully specified in requirements? [Completeness, Spec §FR-009]
- [x] CHK004 - Are requirements defined for JPA/Hibernate configuration beyond ddl-auto (show-sql, open-in-view, naming strategy)? [Completeness, Gap]
- [x] CHK005 - Are SpringDoc configuration properties specified per profile (enabled/disabled)? [Completeness, Spec §FR-006, §FR-007]
- [x] CHK006 - Are Actuator configuration properties specified (which endpoints are exposed, security)? [Completeness, Spec §FR-008a]
- [x] CHK007 - Are logging configuration requirements specified for each profile (level, format, destinations)? [Completeness, Gap]

## Requirement Clarity

- [x] CHK008 - Is "database connection properties" specified with the exact URL format, credential source, and driver class? [Clarity, Spec §FR-009]
- [x] CHK009 - Are the specific differences between dev and prod profiles enumerated rather than implied? [Clarity, Spec §FR-010]
- [x] CHK010 - Is the Flyway locations path format clarified (classpath:db/migration vs file:./db/migration)? [Clarity, Spec §FR-003]
- [x] CHK011 - Are JWT configuration defaults in application.yml distinguished from production overrides? [Clarity, Gap]
- [x] CHK012 - Is the default profile behavior clear (dev activated by default, but configurable via environment variable)? [Clarity, Spec §FR-010]

## Requirement Consistency

- [x] CHK013 - Do the database connection properties in application.yml match the Docker Compose PostgreSQL service configuration? [Consistency, Spec §FR-001 vs §FR-009]
- [x] CHK014 - Do the .env.example variables match the placeholders used in docker-compose.yml and application configurations? [Consistency, Spec §FR-001 vs §FR-011]
- [x] CHK015 - Are profile gating configurations consistent between application-dev.yml and application-prod.yml? [Consistency, Spec §FR-010]
- [x] CHK016 - Does the default profile behavior align with quickstart instructions (no SPRING_PROFILES_ACTIVE needed for dev)? [Consistency, Spec §FR-010 vs quickstart.md]

## Acceptance Criteria Quality

- [x] CHK017 - Can "under 5 minutes with at most 4 commands" (SC-001) be objectively measured? [Measurability, Spec §SC-001]
- [x] CHK018 - Is "healthy application on first attempt" (SC-002) defined with specific health indicators? [Measurability, Spec §SC-002]
- [x] CHK019 - Can the correct profile activation be verified objectively (different log levels, endpoints available)? [Measurability, Spec §FR-010]

## Scenario Coverage

- [x] CHK020 - Are requirements defined for what happens when .env file is missing? [Coverage, Spec §Edge Cases]
- [x] CHK021 - Are requirements defined for profile activation via environment variable vs via application.yml? [Coverage, Gap]
- [x] CHK022 - Are requirements defined for Spring Cloud Config or external configuration sources? [Coverage, Gap]
- [x] CHK023 - Are requirements defined for configuration validation at startup (missing required properties)? [Coverage, Gap]

## Edge Case & Failure Coverage

- [x] CHK024 - Is the expected behavior defined when the .env file contains invalid values or format? [Edge Case, Gap]
- [x] CHK025 - Is the expected behavior defined when a required environment variable is not set in production? [Edge Case, Gap]
- [x] CHK026 - Is the expected behavior defined when the configured database port is already in use? [Edge Case, Spec §Edge Cases]
- [x] CHK027 - Is the expected behavior defined for database connection retry on startup when PostgreSQL is not yet ready? [Edge Case, Gap]

## Non-Functional Requirements

- [x] CHK028 - Are requirements defined for configuration change propagation without application restart? [Operations, Gap]
- [x] CHK029 - Are requirements defined for sensitive configuration masking in logs? [Security, Gap]
- [x] CHK030 - Are requirements defined for configuration file backup and recovery? [Operations, Gap]

## Accepted As-Is

Items accepted without changes (low severity, adequate for foundation phase):

- CHK011 — JWT defaults vs prod overrides: defaults are clearly dev-only; prod overrides via env vars
- CHK021 — Profile activation via env var: `SPRING_PROFILES_ACTIVE` is standard Spring Boot behavior
- CHK024 — Invalid .env format: Docker Compose handles parsing errors; acceptable fallthrough
- CHK025 — Missing env var in prod: application will fail with clear error; acceptable fail-fast behavior
- CHK027 — DB connection retry: now documented in Clarifications (fail fast, health check mechanism)
- CHK035 — Hardcoded defaults safety: dev-only; .env.example marks `CHANGEME_IN_PRODUCTION` for secrets
- CHK036 — FR-010 vs test profile: now clarified in Clarifications section

## Deferred to Later Phases

Items intentionally out of scope for Project Foundation:

- CHK004 — JPA/Hibernate configuration beyond ddl-auto: detailed JPA tuning deferred to Phase 2+ when entities exist
- CHK007 — Logging configuration per profile (format, destinations): basic levels defined in dev/prod yml; advanced logging deferred
- CHK011 — JWT defaults vs production overrides: Phase 2 concern (JWT not active in foundation phase)
- CHK022 — Spring Cloud Config or external configuration sources: not needed for single-service local dev
- CHK023 — Configuration validation at startup: deferred to Phase 2+ when more properties exist
- CHK028-030 — Configuration change propagation, secrets masking, backup: operational concerns for production deployment

## Dependencies & Assumptions

- [x] CHK031 - Is the assumption that Java 21 is available on the developer machine documented? [Assumption, Spec §Assumptions]
- [x] CHK032 - Is the assumption that Maven (via ./mvnw) is available documented? [Assumption, Spec §Assumptions]
- [x] CHK033 - Is the assumption that port 8080 is available documented as a potential conflict? [Assumption, Spec §Assumptions]
- [x] CHK034 - Is the dependency on Docker and Docker Compose for PostgreSQL documented? [Assumption, Spec §Assumptions]

## Ambiguities & Conflicts

- [x] CHK035 - Is there ambiguity about whether hardcoded defaults (database password, JWT secret) are safe in development or should all be env-based? [Ambiguity, Spec §FR-009 vs §FR-011]
- [x] CHK036 - Does "profiles for dev and prod environments" (FR-010) conflict with the test profile mentioned in FR-006/FR-007? [Conflict, Spec §FR-010 vs §FR-006]
