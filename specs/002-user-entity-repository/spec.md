# Feature Specification: User Entity & Repository

**Feature Branch**: `002-user-entity-repository`

**Created**: 2026-07-09

**Status**: Draft

**Input**: User description: "Handle the second GitHub issue — User entity + repository + Flyway V1. Flyway V1 migration already exists; need the Java side (Role enum, User JPA entity, UserRepository)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Application Startup (Priority: P1)

When the application starts, the User entity is recognized by JPA and the UserRepository is available for dependency injection.

**Why this priority**: Auth, events, and RSVPs all depend on the User domain. Without the entity and repository, nothing else can be built.

**Independent Test**: The application context loads and the UserRepository bean is present.

**Acceptance Scenarios**:

1. **Given** the application is configured with Flyway and JPA, **When** the application starts, **Then** the `users` table is mapped to the JPA entity without errors
2. **Given** the application context is loaded, **When** a service requests `UserRepository` via dependency injection, **Then** the bean is available and operational
3. **Given** a `users` record exists in the database, **When** the repository queries by email, **Then** the matching user is returned

---

### User Story 2 — User Domain Model Complete (Priority: P1)

Developers can represent and work with users in code through a well-defined type system.

**Why this priority**: The role enum and entity class define the contract for how users are represented across the entire system — auth, events, RSVPs.

**Independent Test**: A `Role` enum value can be assigned to a `User` entity and persisted/retrieved correctly.

**Acceptance Scenarios**:

1. **Given** a `User` entity, **When** its `role` field is set to `ADMIN` or `MEMBER`, **Then** the value is stored as the corresponding string in the database
2. **Given** a `User` entity with timestamps, **When** the entity is persisted, **Then** `createdAt` and `updatedAt` are automatically populated
3. **Given** the `users` table schema, **When** all columns are examined, **Then** they match the JPA entity field mappings (id, email, passwordHash, firstName, lastName, role, enabled, createdAt, updatedAt)

---

### Edge Cases

- What happens when a user tries to create a `User` with a null email? The entity validation should reject it at the JPA level (column `NOT NULL` constraint)
- What happens when a user looks up a non-existent email? The repository should return an empty `Optional`
- What happens when the `Role` enum receives an unexpected database value? JPA should fail with a mapping exception rather than silently defaulting
- What happens when the entity is loaded but the Flyway migration hasn't run? The application should fail to start (table doesn't exist JPA validation error)

## Clarifications

### Session 2026-07-09

- Q: Where should `@EnableJpaAuditing` configuration live? → A: In a new `JpaConfig` class under `com.lionsclub.api.config`, alongside the existing `OpenApiConfig`. This ensures timestamp auto-population is available to all entities from the start. (Removed from Assumptions as this is now a resolved design decision.)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST define a `Role` enum with values `ADMIN` and `MEMBER` that maps to the VARCHAR `role` column in the `users` table
- **FR-002**: System MUST provide a `User` JPA entity annotated to map to the `users` table, with all columns: `id` (UUID), `email` (unique), `passwordHash`, `firstName`, `lastName`, `role` (Role enum), `enabled` (boolean), `createdAt`, `updatedAt`
- **FR-003**: System MUST configure `createdAt` and `updatedAt` fields to be automatically populated on persist/update
- **FR-004**: System MUST provide a `UserRepository` Spring Data JPA interface
- **FR-005**: System MUST expose a `findByEmail(String email)` query method on `UserRepository` returning `Optional<User>`
- **FR-006**: System MUST locate entities and enums under the `com.lionsclub.api.domain.user` package and the repository under `com.lionsclub.api.infrastructure.persistence`

### Key Entities *(include if feature involves data)*

- **[User]**: Core user entity with UUID primary key, email (unique), password hash, name fields, role (ADMIN/MEMBER), enabled flag, and automatic timestamps
- **[Role]**: Enumeration of user roles (ADMIN, MEMBER) mapped to VARCHAR in the database

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Application starts successfully with `User` entity and `UserRepository` beans registered — verified by context load test
- **SC-002**: `UserRepository.findByEmail()` returns the correct user for valid emails and an empty `Optional` for non-existent emails — verified by repository integration test
- **SC-003**: A `User` with role `ADMIN` can be persisted and retrieved with the same role value — verified by entity mapping test
- **SC-004**: Automatic timestamps (`createdAt`, `updatedAt`) are populated on persist — verified by entity lifecycle test

## Assumptions

- The `V1__create_users_table.sql` Flyway migration is already present and defines the `users` table schema correctly
- The `users` table uses `VARCHAR` for the `role` column — the JPA enum mapping will use `EnumType.STRING`
- JPA auto-ddl is set to `validate` or `none` (managed exclusively by Flyway), so entity-column mismatches will fail fast at startup
- The project uses Lombok for boilerplate reduction (getters, setters, constructors)
