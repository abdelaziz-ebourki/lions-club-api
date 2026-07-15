# Data Model: Events CRUD

**Date**: 2026-07-15

**Plan**: [plan.md](./plan.md)

## Entities

### Event (existing — add `category` field)

Maps to the `events` table. V3 migration adds the `category` column.

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
| category | `EventCategory` (enum) | `category` | `NOT NULL` | NEW — `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)` |
| status | `EventStatus` (enum) | `status` | `NOT NULL`, default `DRAFT` | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)` |
| createdBy | `User` (UUID FK) | `created_by` | `NOT NULL`, FK to users.id | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "created_by", nullable = false)` |
| createdAt | `LocalDateTime` | `created_at` | `NOT NULL`, default `now()` | `@CreatedDate`, `@Column(name = "created_at", nullable = false, updatable = false)` |
| updatedAt | `LocalDateTime` | `updated_at` | `NOT NULL`, default `now()` | `@LastModifiedDate`, `@Column(name = "updated_at", nullable = false)` |

### EventCategory (NEW enum)

| Value | DB Representation |
|-------|-------------------|
| `HEALTH` | `"HEALTH"` (VARCHAR) |
| `ENVIRONMENT` | `"ENVIRONMENT"` (VARCHAR) |
| `YOUTH` | `"YOUTH"` (VARCHAR) |
| `COMMUNITY` | `"COMMUNITY"` (VARCHAR) |
| `FUNDRAISER` | `"FUNDRAISER"` (VARCHAR) |

### EventStatus (existing — unchanged)

| Value | DB Representation |
|-------|-------------------|
| `DRAFT` | `"DRAFT"` (VARCHAR) |
| `PUBLISHED` | `"PUBLISHED"` (VARCHAR) |
| `CANCELLED` | `"CANCELLED"` (VARCHAR) |
| `COMPLETED` | `"COMPLETED"` (VARCHAR) |

## DTO Shapes

### EventRequest (create/update input)

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| title | `String` | yes | `@NotBlank`, min 3 chars |
| description | `String` | yes | `@NotBlank`, min 10 chars |
| date | `String` | yes | `@NotBlank`, ISO date format `YYYY-MM-DD` |
| time | `String` | yes | `@NotBlank`, 24h format `HH:mm` |
| location | `String` | yes | `@NotBlank`, min 3 chars |
| category | `String` | yes | Must match one of: Health, Environment, Youth, Community, Fundraiser |
| status | `String` | no | Default `"upcoming"` if omitted |

### EventResponse (list/detail output)

| Field | Type | Source |
|-------|------|--------|
| id | `String` (UUID) | Event.id |
| title | `String` | Event.title |
| description | `String` | Event.description |
| date | `String` | Derived from Event.startDateTime (YYYY-MM-DD) |
| time | `String` | Derived from Event.startDateTime (HH:mm) |
| location | `String` | Event.location |
| category | `String` | Event.category enum name |
| status | `String` | Derived: `"upcoming"`, `"ongoing"`, `"past"` |
| rsvpCount | `int` | Hardcoded `0` (issue #9) |
| createdAt | `String` | ISO timestamp from Event.createdAt |
| updatedAt | `String` | ISO timestamp from Event.updatedAt |

## Status Derivation Map

| Frontend status filter | Backend query logic |
|------------------------|---------------------|
| `?status=upcoming` | `status = PUBLISHED AND startDateTime > now` |
| `?status=ongoing` | `status = PUBLISHED AND startDateTime <= now AND endDateTime >= now` |
| `?status=past` | `status = COMPLETED OR (status = PUBLISHED AND endDateTime < now)` |
| (no filter) | All events, any status |

## Validation Rules

| Field | Rule | Source |
|-------|------|--------|
| title | Must not be blank, min 3 chars | `@NotBlank` |
| description | Must not be blank, min 10 chars | `@NotBlank` |
| date | Must be valid ISO date | `@NotBlank` + format validation |
| time | Must be valid 24h time | `@NotBlank` + format validation |
| location | Must not be blank, min 3 chars | `@NotBlank` |
| category | Must be a valid enum value | Custom validation |

## State Transitions

- Created events default to `PUBLISHED` (not DRAFT — frontend creates published events)
- `CANCELLED` is settable via `PUT` (admin can cancel events)
- Status transitions enforced at the service layer

## V3 Migration: add_event_category

```sql
ALTER TABLE events ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'COMMUNITY';
CREATE INDEX idx_events_category ON events (category);
```

Rollback:
```sql
DROP INDEX IF EXISTS idx_events_category;
ALTER TABLE events DROP COLUMN category;
```
