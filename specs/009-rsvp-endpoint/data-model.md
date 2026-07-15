# Data Model: RSVP Endpoint

## Entity: Rsvp

Represents a member's attendance response to an event.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID (PK) | Auto-generated | Primary key |
| event | Event (ManyToOne) | `@NotNull`, FK to events.id | The event being responded to |
| member | User (ManyToOne) | `@NotNull`, FK to users.id | The member who RSVP'd |
| status | RsvpStatus (enum) | `@NotNull`, VARCHAR | YES, NO, or MAYBE |
| plusOne | Integer | default 0 | Number of additional guests |
| notes | String (TEXT) | nullable | Free-text note from member |
| createdAt | LocalDateTime | `@CreatedDate`, auto | Timestamp of creation |
| updatedAt | LocalDateTime | `@LastModifiedDate`, auto | Timestamp of last update |

### Constraints

- **Unique**: (event_id, member_id) — one RSVP per member per event
- **FK_event**: References `events.id` with CASCADE delete
- **FK_member**: References `users.id` with CASCADE delete
- **Check**: `plusOne >= 0`

### Indexes

- Composite index on (event_id, status) for fast RSVP counting
- Composite unique index on (event_id, member_id) for upsert

### Enum: RsvpStatus

| Value | Meaning |
|-------|---------|
| YES | Member plans to attend |
| NO | Member does not plan to attend |
| MAYBE | Member is uncertain |

Stored as VARCHAR in the database (consistent with existing enums).

## Entity: Event (modified)

### New Computed Fields

| Field | Type | Source | Description |
|-------|------|--------|-------------|
| rsvpCount | int (transient) | Computed from Rsvp | Total count of RSVPs (all statuses) |
| rsvpBreakdown | Map<String, Integer> (transient) | Computed from Rsvp | Breakdown: {yes: N, no: N, maybe: N} |

These replace the currently hardcoded `rsvpCount: 0` in EventResponse DTO. They are NOT persisted columns — computed dynamically from the Rsvp table.

## Flyway Migration: V4__create_rsvps_table.sql

```sql
CREATE TABLE rsvps (
    id          UUID PRIMARY KEY,
    event_id    UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    member_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status      VARCHAR(8) NOT NULL CHECK (status IN ('YES', 'NO', 'MAYBE')),
    plus_one    INTEGER NOT NULL DEFAULT 0 CHECK (plus_one >= 0),
    notes       TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, member_id)
);

CREATE INDEX idx_rsvps_event_status ON rsvps (event_id, status);
```

## Entity Relationships

```
User (1) ────< Rsvp >──── (1) Event
  |                            |
  | (member_id FK)             | (event_id FK)
  |                            |
  └── one user can have        └── one event can have
      many RSVPs                   many RSVPs
```

- **User → Rsvp**: One-to-many. A member can RSVP to many events.
- **Event → Rsvp**: One-to-many. An event can receive many member RSVPs.
- **Rsvp → User**: Many-to-one (LAZY). Each RSVP belongs to one member.
- **Rsvp → Event**: Many-to-one (LAZY). Each RSVP belongs to one event.

## State Transitions (RSVP)

```
         ┌──────────┐
    ┌───>│   YES    │<───┐
    │    └──────────┘    │
    │         │          │
    │         │          │
    │    ┌──────────┐    │
    │    │   MAYBE  │────┘
    │    └──────────┘
    │         │
    │         │
    │    ┌──────────┐
    └────│    NO    │
         └──────────┘
```

Members can freely transition between any status (upsert replaces the previous RSVP). No progression rules — a member can change from YES to NO to MAYBE and back at any time.
