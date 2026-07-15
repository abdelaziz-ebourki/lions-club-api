# Data Model: Event

**Date**: 2026-07-15

**Plan**: [plan.md](./plan.md)

## Entities

### Event

Maps to the `events` table created by `V2__create_events_table.sql`.

| Field | Type | Column | Constraints | JPA Mapping |
|-------|------|--------|------------|-------------|
| id | `UUID` | `id` | PK, generated via `gen_random_uuid()` | `@Id`, `@GeneratedValue(strategy = GenerationType.UUID)` |
| title | `String` | `title` | `NOT NULL` | `@Column(nullable = false)`, `@NotBlank` |
| description | `String` | `description` | nullable | `@Column(columnDefinition = "TEXT")` |
| startDateTime | `LocalDateTime` | `start_date_time` | `NOT NULL` | `@Column(name = "start_date_time", nullable = false)` |
| endDateTime | `LocalDateTime` | `end_date_time` | `NOT NULL` | `@Column(name = "end_date_time", nullable = false)` |
| location | `String` | `location` | nullable | `@Column` |
| address | `String` | `address` | nullable | `@Column(columnDefinition = "TEXT")` |
| maxAttendees | `Integer` | `max_attendees` | nullable | `@Column(name = "max_attendees")` |
| status | `EventStatus` (enum) | `status` | `NOT NULL`, default `DRAFT` | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)` |
| createdBy | `User` (UUID FK) | `created_by` | `NOT NULL`, FK to users.id | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "created_by", nullable = false)` |
| createdAt | `LocalDateTime` | `created_at` | `NOT NULL`, default `now()` | `@CreatedDate`, `@Column(name = "created_at", nullable = false, updatable = false)` |
| updatedAt | `LocalDateTime` | `updated_at` | `NOT NULL`, default `now()` | `@LastModifiedDate`, `@Column(name = "updated_at", nullable = false)` |

### EventStatus (Enum)

| Value | DB Representation |
|-------|-------------------|
| `DRAFT` | `"DRAFT"` (VARCHAR) |
| `PUBLISHED` | `"PUBLISHED"` (VARCHAR) |
| `CANCELLED` | `"CANCELLED"` (VARCHAR) |
| `COMPLETED` | `"COMPLETED"` (VARCHAR, default) |

## Relationships

| Entity | Relationship | Target | Cardinality | Description |
|--------|-------------|--------|-------------|-------------|
| Event | ManyToOne | User | Many events → One user | Each event has a creator (createdBy). A user can create many events. |

## Validation Rules

| Field | Rule | Source |
|-------|------|--------|
| title | Must not be blank | `@NotBlank` |
| startDateTime | Must not be null | `@Column(nullable = false)` |
| endDateTime | Must not be null | `@Column(nullable = false)` |
| status | Must be DRAFT, PUBLISHED, CANCELLED, or COMPLETED | Enum type constraint |
| createdBy | Must reference an existing user | FK constraint + `@ManyToOne` |
| id | Auto-generated UUID | `@GeneratedValue` |
| createdAt | Auto-populated on persist, never updated | `@CreatedDate`, `updatable = false` |
| updatedAt | Auto-populated on persist and update | `@LastModifiedDate` |

## State Transitions

- **Event** follows a lifecycle: `DRAFT` → `PUBLISHED` → `COMPLETED` (forward flow)
- `CANCELLED` is a terminal state reachable from `DRAFT` or `PUBLISHED`
- State transitions will be enforced at the application layer in the CRUD feature (#6)

## Indices (from V2 migration)

| Index | Columns | Type |
|-------|---------|------|
| `idx_events_status` | `status` | Non-unique |
| `idx_events_start_date` | `start_date_time` | Non-unique |
| `idx_events_created_by` | `created_by` | Non-unique |
