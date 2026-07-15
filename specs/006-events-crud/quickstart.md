# Quickstart Validation: Events CRUD

**Date**: 2026-07-15 | **Feature**: 006-events-crud

## Prerequisites

- Java 21 (JDK)
- Docker (for PostgreSQL via Testcontainers)
- Features 002–005 fully implemented and tested (User, JWT, Auth endpoints, Event entity + repository)

## Setup

```bash
# Build and run all tests
./mvnw test
```

## Validation Scenarios

### 1. V3 migration applied — category column exists

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected: Application starts. V3 migration adds `category` column to `events` table.

### 2. Unauthenticated user can list events

```bash
curl -s http://localhost:8080/api/events | jq
```

Expected: `200` with JSON array of events (or empty `[]`).

### 3. Unauthenticated user can filter events by status

```bash
curl -s "http://localhost:8080/api/events?status=upcoming" | jq
```

Expected: `200` with only upcoming events.

### 4. Unauthenticated user can get single event

```bash
curl -s http://localhost:8080/api/events/{id} | jq
```

Expected: `200` with full event object.

### 5. Authenticated admin can create an event

```bash
# Login first
curl -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"adminpass"}'

# Create event
curl -b cookies.txt -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Beach Cleanup",
    "description": "Join us for a community beach cleanup event",
    "date": "2026-09-15",
    "time": "09:00",
    "location": "Sunny Beach",
    "category": "Environment"
  }' | jq
```

Expected: `201` with created event including `id`.

### 6. Unauthenticated user cannot create events

```bash
curl -s -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"title":"Test"}' | jq
```

Expected: `401` Unauthorized.

### 7. Non-admin user gets 403 on write operations

```bash
curl -b cookies.txt -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"title":"Test"}' | jq
```

Expected: `403` Forbidden (when cookie belongs to MEMBER role user).

### 8. 404 for non-existent event

```bash
curl -s http://localhost:8080/api/events/00000000-0000-0000-0000-000000000000 | jq
```

Expected: `404` with empty body.

## Running Automated Tests

```bash
# Run all tests
./mvnw test

# Run only event CRUD tests
./mvnw test -Dtest="EventControllerTest,EventServiceTest"
```

## Test Structure

| Test Class | Type | What It Validates |
|------------|------|-------------------|
| `EventControllerTest` | Integration (MockMvc) | All 5 endpoints, auth rules, validation, 404 handling |
| `EventServiceTest` | Unit | Status derivation mapping, DTO↔entity conversion, date/time parsing |
| `EventTest` | Unit | Category field presence |

## API Contracts

See [contracts/api.md](contracts/api.md) for full endpoint specifications.

## Data Model

See [data-model.md](data-model.md) for entity changes and new enums.
