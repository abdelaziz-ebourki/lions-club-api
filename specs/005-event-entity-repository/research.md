# Research: Event Entity, Repository & Flyway V2

**Date**: 2026-07-15

**Plan**: [plan.md](./plan.md)

## Decisions

### Decision 1 — EventStatus enum storage strategy

- **Decision**: Use `@Enumerated(EnumType.STRING)` on the `status` field in `Event` entity
- **Rationale**: Consistent with the existing `Role` enum pattern from feature 002. Stores the exact enum name (`DRAFT`, `PUBLISHED`, `CANCELLED`, `COMPLETED`) as a human-readable VARCHAR in the database. Survives column type changes and enum reordering.
- **Alternatives considered**: `EnumType.ORDINAL` (fragile — breaks if enum order changes), custom converter (unnecessary overhead for this simple case).

### Decision 2 — UUID generation strategy

- **Decision**: Use `@GeneratedValue(strategy = GenerationType.UUID)` on the `id` field
- **Rationale**: Consistent with the existing `User` entity pattern from feature 002. The V2 migration will define `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`. `GenerationType.UUID` delegates ID generation to Hibernate, aligning with the database-level `gen_random_uuid()` default.
- **Alternatives considered**: `@GeneratedValue(strategy = GenerationType.IDENTITY)` (not supported for UUID), manual assignment (unnecessary boilerplate).

### Decision 3 — createdBy relationship mapping

- **Decision**: Use `@ManyToOne(fetch = FetchType.LAZY)` with `@JoinColumn(name = "created_by", nullable = false)`
- **Rationale**: The `created_by` column is a foreign key to `users.id`. LAZY fetching avoids loading the full `User` entity every time an `Event` is read, which is appropriate since this feature only needs the UUID reference. The FK constraint at the database level ensures referential integrity.
- **Alternatives considered**: `FetchType.EAGER` (loads User unnecessarily on every Event read), storing only a raw UUID field without ORM relationship (loses referential integrity enforcement and navigability).

### Decision 4 — Column naming convention

- **Decision**: Use explicit `@Column(name = "...")` with snake_case names (e.g., `start_date_time`, `end_date_time`, `max_attendees`, `created_by`)
- **Rationale**: Consistent with the existing `users` table naming convention from V1 migration and the `User` entity pattern. Snake_case is standard in PostgreSQL and Flyway migrations.
- **Alternatives considered**: Implicit camelCase-to-snake_case via Spring Boot naming strategy (works but explicit is clearer for Flyway-managed schemas).

### Decision 5 — Repository package location

- **Decision**: Place `EventRepository` in `com.lionsclub.api.infrastructure.persistence`
- **Rationale**: Consistent with the existing `UserRepository` placement from feature 002. Repository is an infrastructure concern, separate from the domain `event` package.
- **Alternatives considered**: Placing in `domain.event/` (simpler but mixes concerns).

### Decision 6 — Entity validation approach

- **Decision**: Use Jakarta Bean Validation annotations (`@NotBlank`, `@NotNull`) on entity fields + rely on DB constraints
- **Rationale**: Consistent with the `User` entity pattern from feature 002. Provides early validation before DB round-trips, while database-level `NOT NULL` and FK constraints serve as the final safety net.
- **Alternatives considered**: Validation only at the DB level (delayed error reporting).

### Decision 7 — Flyway V2 migration design

- **Decision**: Create `V2__create_events_table.sql` with columns matching the `Event` entity, FK to `users(id)`, and indices on `status`, `start_date_time`, and `created_by`
- **Rationale**: The V2 naming ensures it runs after V1 (users table). Including a FK to `users(id)` enforces referential integrity at the database level. Indices on frequently-queried columns (`status` for filtering, `start_date_time` for date-range queries, `created_by` for user-event lookups) follow standard PostgreSQL indexing best practices.
- **Alternatives considered**: No indices (acceptable for small datasets but poor at scale), composite index on `(status, start_date_time)` (could be added later based on actual query patterns).
