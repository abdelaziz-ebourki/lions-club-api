# Feature Specification: Seed Data / Dev Data Setup

**Feature Branch**: `008-seed-dev-data`

**Created**: 2026-07-15

**Status**: Draft

**Input**: User description: "gh issue 8 shall be handled"

## User Scenarios & Testing

### User Story 1 - Developer sets up local dev environment (Priority: P1)

A developer clones the repository and runs the project with the dev profile. Without any manual data entry, the database is pre-populated with realistic users and events that match the UI mock data, so they can immediately interact with the application.

**Why this priority**: Without seed data, developers must manually create users and events before testing any feature, which is a barrier to onboarding and slows development iteration.

**Independent Test**: A developer can clone the repo, run `./mvnw spring-boot:run -Dspring.profiles.active=dev`, log in with `admin@lionsclub.com / admin123`, and see seeded events in the UI.

**Acceptance Scenarios**:

1. **Given** a fresh database, **When** the application starts with the dev profile active, **Then** seed data is loaded automatically without manual intervention
2. **Given** a database that already contains data, **When** the application starts with the dev profile active, **Then** existing data is not duplicated or overwritten
3. **Given** the production profile, **When** the application starts, **Then** no seed data is loaded

---

### User Story 2 - Developer tests authentication flows (Priority: P1)

A developer needs to verify login, registration, and profile access. Seeded user accounts with known credentials allow immediate testing without account creation.

**Why this priority**: Authentication is the foundation of every protected endpoint; having test users on day one enables parallel work on auth-dependent features.

**Independent Test**: A developer can log in as an admin using the seeded admin credentials and as a member using seeded member credentials, verifying role-based access works.

**Acceptance Scenarios**:

1. **Given** the dev database is seeded, **When** a developer logs in with `admin@lionsclub.com` and the correct password, **Then** login succeeds and a JWT token is returned
2. **Given** the dev database is seeded, **When** a developer logs in with `fatima@lionsclub.com` and the correct password, **Then** login succeeds with member role
3. **Given** the dev database is seeded, **When** a developer logs in with invalid credentials, **Then** login is rejected

---

### User Story 3 - Developer tests event listings (Priority: P2)

A developer can browse upcoming and past events without creating them first, enabling frontend-backend integration testing.

**Why this priority**: Events are the core domain entity; pre-seeded events (matching UI mock data) let frontend and backend teams validate rendering and API responses simultaneously.

**Independent Test**: A developer can call the events API endpoint and receive a response containing both upcoming and past events with the correct fields and statuses.

**Acceptance Scenarios**:

1. **Given** the dev database is seeded, **When** a developer fetches events, **Then** the response includes upcoming events (Annual Charity Gala 2026, Community Clean-Up Day, Health & Wellness Workshop)
2. **Given** the dev database is seeded, **When** a developer fetches events, **Then** the response includes past events (Sight Screening Camp, Youth Leadership Summit)
3. **Given** the dev database is seeded, **When** a developer fetches a specific event, **Then** event fields (title, description, date, time, location, category, status) match the UI mock data

---

### Edge Cases

- What happens when the database already contains partial data (some tables populated, others empty)?
- How does the system handle seed data when the dev profile is combined with other profiles?

## Requirements

### Functional Requirements

- **FR-001**: System MUST seed an admin user with known credentials when running with the dev profile and an empty database
- **FR-002**: System MUST seed at least two member users with known credentials when running with the dev profile and an empty database
- **FR-003**: System MUST seed upcoming events with titles, descriptions, dates, times, locations, categories, and statuses matching UI mock data
- **FR-004**: System MUST seed past events with the same data completeness as upcoming events
- **FR-005**: System MUST NOT seed data when the dev profile is not active
- **FR-006**: System MUST be idempotent — seeding must not duplicate data if the application restarts
- **FR-007**: System MUST hash all user passwords using the same mechanism as the production authentication flow
- **FR-008**: Seeded user IDs MUST match the UI mock data IDs for consistent frontend-backend integration

### Key Entities

- **User**: Represents a system user with admin or member role, seeded with known credentials and IDs matching UI mock data
- **Event**: Represents a Lions Club event with title, description, date, time, location, category, and status (upcoming/past), seeded to match UI mock data

## Success Criteria

### Measurable Outcomes

- **SC-001**: Developer can start the application with the dev profile and have a fully populated database in under 10 seconds of startup time
- **SC-002**: All seeded users can authenticate successfully on first login attempt
- **SC-003**: Events endpoint returns exactly 5 seeded events (3 upcoming, 2 past) with all fields matching the UI mock data
- **SC-004**: Repeated application restarts with the dev profile do not change the data state (no duplicates, no corruption)

## Assumptions

- The development environment uses the same database technology as production (PostgreSQL or H2 for local dev)
- Seeded user passwords are bcrypt-hashed using the application's existing password encoder
- The seed data describes a real-world Lions Club FSBM chapter in Casablanca, Morocco
- Seed data consists exclusively of Users and Events; future entities will be seeded in subsequent features
- The UI mock data is the authoritative source for seed data values
