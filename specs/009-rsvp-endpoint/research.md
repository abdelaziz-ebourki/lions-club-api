# Research: RSVP Endpoint

## Overview

Research and design decisions for adding RSVP functionality to the Lions Club FSBM REST API. All decisions are constrained by the existing project's technology stack and architecture (Spring Boot 3.4.4 / Java 21 / PostgreSQL / Flyway / SpringDoc / Auth0 java-jwt).

## Decisions

### 1. RSVP Data Model Approach

**Decision**: Dedicated `rsvps` table with unique constraint on (event_id, user_id).

**Rationale**:
- Clean separation of concerns — RSVP is a distinct domain concept, not a field on Event or User
- Upsert (INSERT ON CONFLICT UPDATE) maps naturally to PostgreSQL's `ON CONFLICT DO UPDATE`
- Easy to query (count by event, list by member, filter by status)
- Scales independently if RSVP behavior grows (e.g., dietary restrictions, attendance tracking)

**Alternatives considered**:
- JSONB array on Event — querying/filtering individual RSVPs would be complex and not type-safe
- Embedding RSVP count on Event with separate RSVP table — needlessly duplicates data

### 2. Upsert Strategy

**Decision**: Use `@DynamicUpdate` + `merge()` in JPA for create/update, or a native `INSERT ... ON CONFLICT (event_id, user_id) DO UPDATE` query for atomicity.

**Rationale**:
- JPA's `merge()` detects existing entity by ID or unique constraint — simpler code
- Native upsert is more performant (single round-trip) but couples to PostgreSQL
- For v1 scale (1k-10k rows), JPA `merge()` is sufficient; can optimize later

**Chosen approach**: JPA `merge()` with find by event_id + user_id as the lookup.

### 3. RSVP Count Computation

**Decision**: Compute RSVP counts dynamically via JPQL query (`SELECT COUNT(*) FROM Rsvp WHERE event.id = ?1 AND status = ?2`) rather than caching a counter on the Event entity.

**Rationale**:
- No stale counter risk — always reflects current state
- Avoids additional migration to add counter columns to events table
- JPQL query is fast with an index on (event_id, status)
- For <100 RSVPs per event (club scale), overhead is negligible

**Alternatives considered**:
- Cached counter column on Event — would require transactional synchronization and a migration
- Application-level cache — adds complexity without need at current scale

### 4. Capacity Enforcement

**Decision**: Check `currentYesCount < maxAttendees` before accepting a YES RSVP. The check and RSVP save must happen in the same transaction to prevent race conditions (two simultaneous YES RSVPs exceeding capacity).

**Rationale**:
- `@Transactional` on the service method ensures atomic check-and-save
- PostgreSQL's serializable isolation would be overkill; default READ_COMMITTED + ordered execution is sufficient for club-scale concurrency
- Using `SELECT COUNT(*) FOR UPDATE` on the rsvps table (pessimistic lock) is optional for v1

**Chosen approach**: `@Transactional` service method with `SELECT COUNT(*) WHERE event_id = ? AND status = 'YES'` inside the same transaction.

### 5. API Contract Format

**Decision**: SpringDoc annotations on controller and DTOs generate OpenAPI 3.0 spec automatically.

**Rationale**:
- Existing project pattern (EventController already uses SpringDoc annotations)
- No manual OpenAPI YAML maintenance — single source of truth
- Consistent with constitution principle I (API-First & Contract-Driven)

### 6. Status Mapping (Enum Design)

**Decision**: RsvpStatus enum with values `YES`, `NO`, `MAYBE`. Stored as VARCHAR in PostgreSQL.

**Rationale**:
- Matches existing enum patterns (Role, EventStatus, EventCategory all use VARCHAR)
- No numeric mapping complexity
- Readable in database queries and dumps

## Summary of Technology Choices

| Concern | Decision |
|---------|----------|
| Language | Java 21 (existing) |
| Framework | Spring Boot 3.4.4 (existing) |
| Database | PostgreSQL 15 (existing) |
| ORM | Spring Data JPA / Hibernate (existing) |
| Migrations | Flyway V4 (existing pattern) |
| Auth | Cookie-based JWT (existing) |
| API Docs | SpringDoc annotations (existing) |
| Testing | JUnit 5 + MockMvc + Testcontainers (existing) |
| Build | Maven (existing) |
