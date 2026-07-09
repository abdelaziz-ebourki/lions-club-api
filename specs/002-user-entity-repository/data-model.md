# Data Model: User

**Date**: 2026-07-09

**Plan**: [plan.md](./plan.md)

## Entities

### User

Maps to the existing `users` table created by `V1__create_users_table.sql`.

| Field | Type | Column | Constraints | JPA Mapping |
|-------|------|--------|------------|-------------|
| id | `UUID` | `id` | PK, generated via `gen_random_uuid()` | `@Id`, `@GeneratedValue(strategy = GenerationType.UUID)` |
| email | `String` | `email` | `NOT NULL`, unique (idx_users_email) | `@Column(nullable = false, unique = true)`, `@Email`, `@NotBlank` |
| passwordHash | `String` | `password_hash` | `NOT NULL` | `@Column(name = "password_hash", nullable = false)`, `@NotBlank` |
| firstName | `String` | `first_name` | `NOT NULL` | `@Column(name = "first_name", nullable = false)`, `@NotBlank` |
| lastName | `String` | `last_name` | `NOT NULL` | `@Column(name = "last_name", nullable = false)`, `@NotBlank` |
| role | `Role` (enum) | `role` | `NOT NULL`, default `MEMBER` | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)` |
| enabled | `boolean` | `enabled` | `NOT NULL`, default `true` | `@Column(nullable = false)` |
| createdAt | `LocalDateTime` | `created_at` | `NOT NULL`, default `now()` | `@CreatedDate`, `@Column(name = "created_at", nullable = false, updatable = false)` |
| updatedAt | `LocalDateTime` | `updated_at` | `NOT NULL`, default `now()` | `@LastModifiedDate`, `@Column(name = "updated_at", nullable = false)` |

### Role (Enum)

| Value | DB Representation |
|-------|-------------------|
| `ADMIN` | `"ADMIN"` (VARCHAR) |
| `MEMBER` | `"MEMBER"` (VARCHAR, default) |

## Relationships

- **User** has no relationships to other entities yet (future features will add Event owner, RSVP user).

## Validation Rules

| Field | Rule | Source |
|-------|------|--------|
| email | Must be a valid email format, not blank | `@Email`, `@NotBlank` |
| email | Must be unique across all users | `@Column(unique = true)` + DB index |
| passwordHash | Must not be blank | `@NotBlank` |
| firstName | Must not be blank | `@NotBlank` |
| lastName | Must not be blank | `@NotBlank` |
| role | Must be `ADMIN` or `MEMBER` | Enum type constraint |
| id | Auto-generated UUID | `@GeneratedValue` |
| createdAt | Auto-populated on persist, never updated | `@CreatedDate`, `updatable = false` |
| updatedAt | Auto-populated on persist and update | `@LastModifiedDate` |

## State Transitions

- **User** is a passive data record — it is created, read, and updated. No workflow states.
- `enabled` flag controls whether the user can authenticate (future auth feature).
- `role` may be updated to promote/demote users (future admin feature).

## Indices (from V1 migration)

| Index | Columns | Type |
|-------|---------|------|
| `idx_users_email` | `email` | Unique |
| `idx_users_role` | `role` | Non-unique |