# Research: User Entity & Repository

**Date**: 2026-07-09

**Plan**: [plan.md](./plan.md)

## Decisions

### Decision 1 — Role enum storage strategy

- **Decision**: Use `@Enumerated(EnumType.STRING)` on the `role` field in `User` entity
- **Rationale**: The existing `V1__create_users_table.sql` migration defines `role VARCHAR(20) NOT NULL DEFAULT 'MEMBER'`. `EnumType.STRING` stores the exact enum name (`ADMIN`, `MEMBER`) as a readable string. This is human-readable in the database, survives column type changes better than ordinal, and matches the migration's VARCHAR column type.
- **Alternatives considered**: `EnumType.ORDINAL` (fragile — breaks if enum order changes), custom converter (unnecessary for this simple case)

### Decision 2 — JPA Auditing configuration

- **Decision**: Create a `JpaConfig` class + add `@EnableJpaAuditing` to the application class
- **Rationale**: The `V1` migration defines `created_at TIMESTAMP NOT NULL DEFAULT now()` and `updated_at TIMESTAMP NOT NULL DEFAULT now()`. Using `@CreatedDate` and `@LastModifiedDate` on the entity ensures the Java layer populates these consistently, independent of DB defaults. A separate `@Configuration` class with `@EnableJpaAuditing` is the standard Spring Boot approach.
- **Alternatives considered**: Annotating `LionsClubApiApplication` directly with `@EnableJpaAuditing` (works but mixes concerns); using database-only defaults (loses programmatic consistency).

### Decision 3 — Repository package location

- **Decision**: Place `UserRepository` in `com.lionsclub.api.infrastructure.persistence`
- **Rationale**: Follows clean architecture — repository is an infrastructure concern (interface to the database), not a domain concern. Domain entities (`User`, `Role`) stay in `domain.user/`. The repository is a Spring Data JPA interface that provides persistence operations.
- **Alternatives considered**: Placing in `domain.user/` (simpler but mixes concerns); placing directly under `api/` (too flat).

### Decision 4 — UUID generation strategy

- **Decision**: Use `@GeneratedValue(strategy = GenerationType.UUID)` on the `id` field
- **Rationale**: The `V1` migration defines `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`. Using `GenerationType.UUID` delegates ID generation to Hibernate's UUID generator, which aligns with the database-level `gen_random_uuid()` default. This works with Flyway-managed schema where JPA ddl-auto is disabled.
- **Alternatives considered**: `@GeneratedValue(strategy = GenerationType.IDENTITY)` (not supported for UUID); manual assignment (unnecessary boilerplate).

### Decision 5 — Entity validation approach

- **Decision**: Use Jakarta Bean Validation annotations (`@NotBlank`, `@Email`, `@NotNull`) on entity fields + rely on DB constraints
- **Rationale**: Provides early validation before DB round-trips, while database-level `NOT NULL` and unique constraints serve as the final safety net. The `@Email` annotation on the email field adds lightweight format validation beyond the DB constraint.
- **Alternatives considered**: Validation only at the DB level (delayed error reporting); custom validators (overkill for this simple case).