# Feature Specification: Events CRUD endpoints with admin authorization

**Feature Branch**: `006-events-crud`

**Created**: 2026-07-15

**Status**: Draft

**Input**: User description: "issue #6"

## User Scenarios & Testing

### User Story 1 - Visitor browses events (Priority: P1)

As a site visitor (unauthenticated user), I want to see the list of all events and view individual event details so that I can learn about upcoming club activities.

**Why this priority**: Read endpoints are the most fundamental — they serve the widest audience and don't require authentication, making them the easiest to deliver first.

**Independent Test**: Can be fully tested by calling `GET /api/events` and `GET /api/events/:id` without any authentication token and receiving valid event data.

**Acceptance Scenarios**:

1. **Given** there are multiple events in the system, **When** a visitor calls `GET /api/events`, **Then** they receive a 200 response with an array of event objects.
2. **Given** there are events with various statuses, **When** a visitor calls `GET /api/events?status=upcoming`, **Then** they receive only events matching that status filter.
3. **Given** a specific event exists, **When** a visitor calls `GET /api/events/{id}`, **Then** they receive a 200 response with the full event object.
4. **Given** an event does not exist, **When** a visitor calls `GET /api/events/{nonExistentId}`, **Then** they receive a 404 response with an empty body.

---

### User Story 2 - Admin creates an event (Priority: P2)

As an admin user, I want to create a new event so that visitors can see it listed on the site.

**Why this priority**: Creating events requires admin authentication, which builds on the read endpoints. Admins need to populate content before visitors can browse it.

**Independent Test**: Can be tested by authenticating as an admin, calling `POST /api/events` with valid data, and confirming a 201 response with the created event.

**Acceptance Scenarios**:

1. **Given** I am authenticated as an admin, **When** I send a valid event payload to `POST /api/events`, **Then** I receive a 201 response with the created event object including a server-generated id.
2. **Given** I am not authenticated, **When** I send a valid event payload to `POST /api/events`, **Then** I receive a 401 response.
3. **Given** I am authenticated but not an admin, **When** I send a valid event payload to `POST /api/events`, **Then** I receive a 403 response.
4. **Given** I am authenticated as an admin, **When** I send an invalid payload (missing required fields) to `POST /api/events`, **Then** I receive a 400 response with validation errors.

---

### User Story 3 - Admin updates an event (Priority: P3)

As an admin user, I want to update an existing event so that event details remain accurate and up-to-date.

**Why this priority**: Update operations require both admin auth and an existing event, making them less foundational than create or read.

**Independent Test**: Can be tested by authenticating as an admin, calling `PUT /api/events/{id}` with updated event data, and confirming a 200 response.

**Acceptance Scenarios**:

1. **Given** I am authenticated as an admin and an event exists, **When** I send a valid updated event to `PUT /api/events/{id}`, **Then** I receive a 200 response with the updated event object.
2. **Given** I am authenticated as an admin, **When** I send a valid update to `PUT /api/events/{nonExistentId}`, **Then** I receive a 404 response.
3. **Given** I am not authenticated, **When** I send a valid payload to `PUT /api/events/{id}`, **Then** I receive a 401 response.
4. **Given** I am authenticated but not an admin, **When** I send a valid payload to `PUT /api/events/{id}`, **Then** I receive a 403 response.

---

### User Story 4 - Admin deletes an event (Priority: P3)

As an admin user, I want to delete an event so that outdated or cancelled events are removed from the listings.

**Why this priority**: Delete is the least critical write operation and only relevant when events need removal.

**Independent Test**: Can be tested by authenticating as an admin, calling `DELETE /api/events/{id}`, and confirming a 200 response with `{ "success": true }`.

**Acceptance Scenarios**:

1. **Given** I am authenticated as an admin and an event exists, **When** I call `DELETE /api/events/{id}`, **Then** I receive a 200 response with `{ "success": true }` and the event is removed.
2. **Given** I am authenticated as an admin, **When** I call `DELETE /api/events/{nonExistentId}`, **Then** I receive a 404 response.

---

### Edge Cases

- What happens when an empty events list is returned? → `GET /api/events` returns an empty array `[]` with 200 status.
- What happens when invalid status filter value is provided? → System returns a 400 with descriptive error message.
- What happens when the event id format is invalid (not a UUID)? → System returns a 400 with descriptive error message.
- What happens when a deleted event is requested? → System returns 404.

## Requirements

### Functional Requirements

- **FR-001**: Unauthenticated users MUST be able to list all events via `GET /api/events`
- **FR-002**: Unauthenticated users MUST be able to filter events by status via `GET /api/events?status=upcoming|past|ongoing`
- **FR-003**: Unauthenticated users MUST be able to retrieve a single event by id via `GET /api/events/:id`
- **FR-004**: Only authenticated users with ADMIN role MUST be able to create events via `POST /api/events`
- **FR-005**: Only authenticated users with ADMIN role MUST be able to update events via `PUT /api/events/:id`
- **FR-006**: Only authenticated users with ADMIN role MUST be able to delete events via `DELETE /api/events/:id`
- **FR-007**: Non-admin authenticated users MUST receive a 403 response on write operations
- **FR-008**: Request bodies for create and update MUST be validated using `@Valid`
- **FR-009**: Invalid input MUST return a 400 response with a descriptive error message
- **FR-010**: Requests for non-existent events MUST return a 404 response
- **FR-011**: All error responses MUST return a JSON body with a `message` field describing the error
- **FR-012**: The event list response MUST include an `rsvpCount` field (initially 0)
- **FR-013**: The event detail and list response MUST include `createdAt` and `updatedAt` ISO timestamps

### Key Entities

- **Event**: A club event with fields including id, title, description, date, time, location, category, status, rsvpCount, createdAt, updatedAt. Maps to the existing `events` table in the database.
- **User**: The authenticated user who performs CRUD operations. Admin role is checked for write operations.

## Success Criteria

### Measurable Outcomes

- **SC-001**: An unauthenticated visitor can browse the full event catalog without any login or registration steps
- **SC-002**: An admin user can complete the full event lifecycle (create → read → update → delete) using only HTTP API calls
- **SC-003**: Unauthorized write attempts (no auth, expired auth, or non-admin role) are consistently rejected with appropriate HTTP status codes (401/403) within the same response time as authorized requests
- **SC-004**: All validation failures return a clear, human-readable error message describing which field failed and why

## Assumptions

- The existing `events` table created by V2 migration is sufficient and no new database changes are needed for this feature
- The existing `Event` entity and `EventRepository` from issue #5 will be reused
- The existing authentication infrastructure (JWT cookie filter, User entity with role) from issues #3 and #4 will be reused
- Admin role is determined by the `role` field on the `User` entity (existing `ADMIN` value in the `Role` enum)
- The `rsvpCount` field will be hardcoded to 0 initially since RSVP functionality (issue #9) is separate
- Frontend sends the `date` and `time` as separate string fields which the API will combine into the existing `startDateTime` on the Event entity
- The response shape differs from the internal entity shape (e.g., `date`/`time` split, `category` as a string) — a DTO mapping layer will be required
