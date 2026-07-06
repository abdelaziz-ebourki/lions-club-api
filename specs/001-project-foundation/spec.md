# Feature Specification: Project Foundation

**Feature Branch**: `001-project-foundation`

**Created**: 2026-07-06

**Status**: Draft

**Input**: User description: "Set up project foundation with Docker Compose, Flyway migrations, dev environment, and SpringDoc OpenAPI documentation"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Developer Environment Bootstrap (Priority: P1)

A new developer clones the repo and can start the full stack (database + API) with a single command.

**Why this priority**: Without a working dev environment, no feature can be built or tested.

**Independent Test**: A developer can run `docker compose up -d` to start PostgreSQL, then `./mvnw spring-boot:run` to start the API, and the application starts without errors.

**Acceptance Scenarios**:

1. **Given** a fresh clone of the repository, **When** the developer runs `docker compose up -d`, **Then** PostgreSQL container starts and is healthy
2. **Given** PostgreSQL is running, **When** the developer runs `./mvnw spring-boot:run`, **Then** the Spring Boot application starts on port 8080
3. **Given** the application is running, **When** the developer visits `http://localhost:8080/actuator/health`, **Then** they receive a 200 OK response with status "UP"
4. **Given** a stopped environment, **When** the developer runs `docker compose down`, **Then** all containers are stopped and removed

---

### User Story 2 - Database Schema Management (Priority: P1)

Database schema changes are managed through versioned Flyway migrations that run automatically on application startup.

**Why this priority**: The database schema is the foundation of all data operations. Without migration management, schema changes are unreliable and unrepeatable.

**Independent Test**: A Flyway migration file added to the migrations directory runs automatically when the application starts.

**Acceptance Scenarios**:

1. **Given** the application is running for the first time, **When** Flyway runs, **Then** the `flyway_schema_history` table is created with initial migration records
2. **Given** a new migration file `V2__create_users.sql` is added to `src/main/resources/db/migration/`, **When** the application restarts, **Then** Flyway executes the new migration and records it in the history
3. **Given** a migration has already been applied, **When** the application restarts, **Then** Flyway skips already-applied migrations without errors

---

### User Story 3 - API Documentation Access (Priority: P2)

Developers and integrators can browse live API documentation through a web interface.

**Why this priority**: API documentation is essential for frontend developers and third-party integrators but does not block initial development.

**Independent Test**: A user can open the Swagger UI in a browser and see the API documentation rendered.

**Acceptance Scenarios**:

1. **Given** the application is running, **When** the user navigates to `http://localhost:8080/swagger-ui.html`, **Then** the Swagger UI page loads
2. **Given** the Swagger UI is loaded, **When** API controllers exist, **Then** they are documented with their request/response schemas

---

### Edge Cases

- What happens when Docker is not installed? The developer receives a clear error message in the README prerequisites
- What happens when port 8080 or 5432 is already in use? Docker Compose should fail with a port conflict error
- What happens when a migration checksum changes after it was already applied? Flyway should fail with a checksum mismatch error to prevent silent data corruption
- What happens when there are no migrations yet? The application should start successfully with only the flyway_schema_history table created
- What happens when the database is not yet ready when the application starts? The application should fail to start (Flyway/connection error) — the Docker health check and `depends_on` in docker-compose.yml are expected to prevent this in the normal case
- What happens when a migration SQL statement fails during execution? Flyway should fail the migration and prevent the application from starting, requiring manual intervention (repair/clean)
- What happens when Flyway detects an out-of-order migration version? Flyway should fail to prevent accidental misordering; out-of-order mode requires explicit opt-in
- What happens when the `.env` file is missing or contains invalid values? Docker Compose should fail with a clear error; the application should use defaults from application.yml but log a warning
- What happens when a migration file is deleted after being applied? Flyway should detect the missing migration (checksum mismatch on next startup) and fail

## Clarifications

### Session 2026-07-06

- Q: Should SpringDoc API docs be accessible in production, or gated to dev/test only? → A: Gate SpringDoc to dev/test profiles only; docs not accessible in production.
- Q: How does the `test` profile relate to FR-010 (dev + prod profiles)? → A: `test` profile is used for SpringDoc gating only; it inherits configuration from the default `dev` profile. FR-010 covers the primary working profiles (dev, prod). The `test` profile does not require its own `application-test.yml`.
- Q: Should the application retry database connection on startup if PostgreSQL is not ready? → A: No explicit retry at the application level — Flyway/Spring Boot will fail fast. Docker Compose health check + container dependency is the expected mechanism to ensure DB readiness.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a `docker-compose.yml` that starts PostgreSQL 15 with a named volume (`pgdata`) mounted at `/var/lib/postgresql/data`
- **FR-002**: System MUST configure Flyway to run migrations automatically on Spring Boot startup
- **FR-003**: System MUST place Flyway migration files under `src/main/resources/db/migration/`
- **FR-004**: System MUST include a baseline migration (`V1__create_users_table`) that creates the `users` table with UUID PK, email (unique), password_hash, first_name, last_name, role, enabled, and timestamps
- **FR-005**: System MUST configure SpringDoc OpenAPI with a custom title, version, and description matching the project
- **FR-006**: System MUST expose SpringDoc UI at `/swagger-ui.html` in `dev` and `test` profiles only
- **FR-007**: System MUST expose SpringDoc API docs at `/v3/api-docs` in `dev` and `test` profiles only
- **FR-008**: System MUST configure SpringDoc to include JWT bearer token as a global security scheme
- **FR-008a**: System MUST include Spring Boot Actuator dependency with a health endpoint at `/actuator/health`
- **FR-009**: System MUST configure `application.yml` with database connection properties pointing to the Docker PostgreSQL instance
- **FR-010**: System MUST configure `application.yml` with profiles for `dev` and `prod` environments
- **FR-011**: System MUST provide a `.env.example` file with required environment variables

### Key Entities *(include if feature involves data)*

- **[flyway_schema_history]**: Auto-managed table tracking which migrations have been applied, their checksums, and execution timestamps

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer can go from `git clone` to running API in under 5 minutes with at most 4 commands (`cp .env.example .env`, `docker compose up -d`, `./mvnw spring-boot:run`, and the clone itself)
- **SC-002**: Running `docker compose up -d` followed by `./mvnw spring-boot:run` results in a healthy application on first attempt
- **SC-003**: Adding a new Flyway migration file and restarting applies the migration automatically with zero manual steps
- **SC-004**: API documentation is accessible at `/swagger-ui.html` immediately after application startup

## Assumptions

- Docker and Docker Compose are installed on the developer machine
- Java 21 and Maven (via `./mvnw`) are available
- PostgreSQL 15 is the target database version
- The application will run on port 8080
- The database will be available at `localhost:5432`
- Initial migration (`V1__`) will create the `users` table as a starting point for Phase 2 (Auth)
- The database is expected to be empty on first run (no existing `users` table or conflicting schema)
- Logging configuration (levels, format) is a profile-level concern: dev uses DEBUG with SQL logging, prod uses WARN with minimal output. Exact log destinations (console, file, external system) are not specified in this phase
- `./mvnw` (Maven Wrapper) is committed to the repository and is the standard build invocation method
