# Feature Specification: RSVP Endpoint

**Feature Branch**: `009-rsvp-endpoint`

**Created**: 2026-07-15

**Status**: Draft

**Input**: User description: "we shall handle the remaining work"

## User Scenarios & Testing

### User Story 1 - Member RSVPs to an Event (Priority: P1)

A logged-in member browses upcoming events and decides to attend one. They click "RSVP" and choose their attendance status (Yes / No / Maybe), optionally indicating a plus-one or adding a note. The system confirms their RSVP and updates the event's attendee count.

**Why this priority**: Core loop of the feature — members need to indicate attendance for the club to plan capacity.

**Independent Test**: Can be tested by creating a member account, fetching an event, posting an RSVP, and verifying the response confirms the choice.

**Acceptance Scenarios**:

1. **Given** a member is authenticated, **When** they POST an RSVP with status "YES" to an event, **Then** the system returns a 201 with the RSVP details including event ID, member ID, and status.
2. **Given** a member has already RSVP'd "YES" to an event, **When** they POST again with "NO", **Then** the RSVP is updated to "NO" (upsert) and a 200 is returned.
3. **Given** an unauthenticated user, **When** they attempt to RSVP, **Then** the system returns 401.

---

### User Story 2 - Admin Views Event RSVPs (Priority: P2)

An admin opens an event's details and wants to see who has RSVP'd — which members are coming, how many, and any notes.

**Why this priority**: Admin needs visibility into attendance for logistics planning.

**Independent Test**: Can be tested by having multiple members RSVP to an event, then an admin fetches the RSVP list and sees all responses.

**Acceptance Scenarios**:

1. **Given** an admin is authenticated, **When** they GET `/api/events/{id}/rsvps`, **Then** they receive a list of all RSVPs with member names, status, plus-one count, and notes.
2. **Given** a member is authenticated (not admin), **When** they GET `/api/events/{id}/rsvps`, **Then** the system returns 403.

---

### User Story 3 - Event RSVP Count Displayed (Priority: P2)

When any user (authenticated or not) views an event's details, they can see how many people have RSVP'd (total, YES, NO, MAYBE counts).

**Why this priority**: Attendee count is useful for all event viewers and was previously hardcoded to 0.

**Independent Test**: Can be tested by creating RSVPs and verifying the event detail response includes updated counts.

**Acceptance Scenarios**:

1. **Given** an event with 3 YES, 1 MAYBE RSVPs, **When** a user GETs the event details, **Then** the response includes `rsvpCount: 4` and `rsvpBreakdown: { yes: 3, no: 0, maybe: 1 }`.

---

### Edge Cases

- What happens when a member RSVPs "YES" and the event has reached `maxAttendees`? System should return 409 (conflict) with a "event full" message.
- What happens when a member RSVPs to a cancelled/completed event? System should return 400 (bad request).
- What happens when a member RSVPs to a non-existent event? System should return 404.
- What happens when a member tries to RSVP for another member? System should enforce that RSVP is tied to the authenticated user only.

## Requirements

### Functional Requirements

- **FR-001**: Authenticated members MUST be able to RSVP to an event with status YES, NO, or MAYBE.
- **FR-002**: RSVP MUST be an upsert — if the member already RSVP'd, update the existing record instead of creating a duplicate.
- **FR-003**: RSVP SHALL support an optional `plusOne` integer field (default 0) and an optional `notes` text field.
- **FR-004**: System MUST reject RSVPs to events that are CANCELLED or COMPLETED.
- **FR-005**: System MUST reject RSVPs with status YES when `maxAttendees` would be exceeded (current YES count >= maxAttendees).
- **FR-006**: Admin users MUST be able to retrieve all RSVPs for a given event.
- **FR-007**: System MUST expose RSVP counts (`rsvpCount`, `rsvpBreakdown`) on the public event detail endpoint.
- **FR-008**: Only the authenticated member's own RSVP SHALL be created/updated — no impersonation.
- **FR-009**: Unauthenticated requests to RSVP endpoints MUST return 401.
- **FR-010**: Non-admin requests to the RSVP list endpoint MUST return 403.

### Key Entities

- **RSVP**: Represents a member's response to an event. Key attributes: event, member (user), status (YES/NO/MAYBE), plusOne count, notes, created timestamp, last updated timestamp. One RSVP per member per event (unique constraint on event_id + user_id).
- **Event** (existing): Updated to include computed RSVP counts. The `rsvpCount` (currently hardcoded to 0) must reflect actual RSVP data.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Members can submit an RSVP and receive confirmation within 1 second.
- **SC-002**: Admin can view the complete list of attendees for any event with all RSVP details.
- **SC-003**: Event detail pages display accurate attendee counts, updated immediately after each RSVP change.
- **SC-004**: RSVP capacity enforcement works correctly — events at capacity return a clear error message when a new YES RSVP is attempted.

## Assumptions

- The existing JWT authentication and role-based security infrastructure will be reused.
- The existing `EventResponse` DTO will be extended with dynamic RSVP counts rather than keeping the hardcoded `0`.
- RSVP is per-user — no "guest RSVP" or unauthenticated RSVP is needed for v1.
- The `maxAttendees` field on Event is the authoritative capacity limit.
- No email notifications are needed for v1 (e.g., confirmation email, admin notification of new RSVPs).
- Flyway migration V4 will create the RSVP table.
