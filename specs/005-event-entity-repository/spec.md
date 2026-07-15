# Feature Specification: Event Entity, Repository & Flyway V2

**Feature Branch**: `005-event-entity-repository`

**Created**: 2026-07-15

**Status**: Draft

**Input**: User description: "Event entity + repository + Flyway V2 for issue #5"

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Application Startup (Priority: P1)

When the application starts, the Event entity is recognized by JPA, the `events` table is created by Flyway V2 migration, and the EventRepository is available for dependency injection.

**Why this priority**: Events CRUD (#6), seed data (#8), and RSVPs (#9) all depend on the Event domain. Without the entity, migration, and repository, nothing else can be built.

**Independent Test**: The application starts, Flyway V2 migration runs successfully, and the EventRepository bean is present.

**Acceptance Scenarios**:

1. **Given** the application is configured with Flyway and JPA, **When** the application starts, **Then** the `events` table is created by the V2 migration and mapped to the JPA entity without errors
2. **Given** the application context is loaded, **When** a service requests `EventRepository` via dependency injection, **Then** the bean is available and operational
3. **Given** an `events` record exists in the database, **When** the repository queries by id, **Then** the matching event is returned

---

### User Story 2 — Event Domain Model Complete (Priority: P1)

Developers can represent and work with events in code through a well-defined type system.

**Why this priority**: The event status enum and entity class define the contract for how events are represented across the entire system — CRUD, RSVPs, calendar views.

**Independent Test**: An `EventStatus` enum value can be assigned to an `Event` entity and persisted/retrieved correctly.

**Acceptance Scenarios**:

1. **Given** an `Event` entity, **When** its `status` field is set to `DRAFT`, `PUBLISHED`, `CANCELLED`, or `COMPLETED`, **Then** the value is stored as the corresponding string in the database
2. **Given** an `Event` entity with timestamps, **When** the entity is persisted, **Then** `createdAt` and `updatedAt` are automatically populated
3. **Given** the `events` table schema, **When** all columns are examined, **Then** they match the JPA entity field mappings (id, title, description, startDateTime, endDateTime, location, address, maxAttendees, status, createdBy, createdAt, updatedAt)

---

### Edge Cases

- What happens when an event is created with a null title? The entity validation should reject it at the JPA level (column `NOT NULL` constraint)
- What happens when `endDateTime` is before `startDateTime`? The database and entity should allow it at schema level (application-level validation deferred to CRUD feature #6)
- What happens when the `EventStatus` enum receives an unexpected database value? JPA should fail with a mapping exception rather than silently defaulting
- What happens when the V2 migration hasn't run? The application should fail to start (table doesn't exist JPA validation error)
- What happens when `createdBy` references a non-existent user? The foreign key constraint should reject the insert with a referential integrity error

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST define an event status type with exactly four values: `DRAFT`, `PUBLISHED`, `CANCELLED`, and `COMPLETED`, and persist them as human-readable strings in the `status` column
- **FR-002**: System MUST provide a `V2__create_events_table.sql` Flyway migration that creates the `events` table with all required columns and constraints (UUID primary key, foreign key to users table, NOT NULL constraints on essential fields, timestamps)
- **FR-003**: System MUST provide an `Event` domain object that maps to the `events` database table, with columns: unique `id` (UUID), `title` (required), `description` (optional), `startDateTime` (required), `endDateTime` (required), `location` (optional), `address` (optional), `maxAttendees` (optional), `status` (DRAFT/PUBLISHED/CANCELLED/COMPLETED), `createdBy` (required, FK to users), `createdAt`, and `updatedAt` timestamps
- **FR-004**: System MUST automatically populate `createdAt` when a record is first created and `updatedAt` whenever a record is modified
- **FR-005**: System MUST provide a data access interface for `Event` that supports standard create, read, update, and delete operations
- **FR-006**: System MUST expose a lookup method to find events by their status, returning a collection of matching events
- **FR-007**: System MUST locate event domain types (the event object and status type) under a `domain.event` package and the data access interface under a separate `infrastructure.persistence` package, following clean architecture layering

### Key Entities *(include if feature involves data)*

- **[Event]**: Core event entity with UUID primary key, title, description, date/time range, location info, capacity, status (DRAFT/PUBLISHED/CANCELLED/COMPLETED), createdBy (User reference), and automatic timestamps
- **[EventStatus]**: Enumeration of event lifecycle states (DRAFT, PUBLISHED, CANCELLED, COMPLETED) mapped to VARCHAR in the database

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Application starts successfully with the `events` table created by Flyway V2, `Event` entity mapped, and `EventRepository` bean registered — verified by context load test
- **SC-002**: `EventRepository.findByStatus()` returns the correct event collection for valid status values and an empty collection for statuses with no matching events — verified by repository integration test
- **SC-003**: An `Event` with status `PUBLISHED` can be persisted and retrieved with the same status value — verified by entity mapping test
- **SC-004**: Automatic timestamps (`createdAt`, `updatedAt`) are populated on persist — verified by entity lifecycle test
- **SC-005**: The `V2__create_events_table.sql` migration is recorded in `flyway_schema_history` and the `events` table schema matches the `Event` entity fields — verified by startup log inspection

## Assumptions

- The `V1__create_users_table.sql` Flyway migration has already run and the `users` table exists with a UUID `id` column
- JPA auditing (`@EnableJpaAuditing`) is already configured from a prior feature, so `@CreatedDate` and `@LastModifiedDate` are available
- The `events` table uses a string column for the `status` field — the status type must be stored and retrieved as a readable string
- JPA auto-ddl is set to `validate` or `none` (managed exclusively by Flyway), so entity-column mismatches will fail fast at startup
- The project uses Lombok for boilerplate reduction (getters, setters, constructors)
- The `createdBy` relationship stores only the `users.id` UUID (no eager loading of the User entity in this feature — relationship mapping is deferred to the CRUD feature #6 where needed)
