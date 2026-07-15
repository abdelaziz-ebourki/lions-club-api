# Quickstart: RSVP Endpoint

**Purpose**: Validation scenarios to prove the RSVP feature works end-to-end.

## Prerequisites

- Docker Compose running (PostgreSQL 15): `docker compose up -d`
- Application running on `http://localhost:8080` (use `dev` profile)
- Seed data loaded (auto on dev profile) — provides users and events
- Verify: `curl -s http://localhost:8080/actuator/health | jq .status` returns `"UP"`

## Scenario 1: Member RSVPs to an Event

```bash
# 1. Login as a member (seed user: alice@lionsclub.org / password123)
curl -s -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@lionsclub.org","password":"password123"}'

# 2. Get an upcoming event ID
EVENT_ID=$(curl -s http://localhost:8080/api/events?status=upcoming \
  | jq -r '.[0].id')

# 3. RSVP YES with plus one
curl -s -b cookies.txt -X POST "http://localhost:8080/api/events/$EVENT_ID/rsvp" \
  -H "Content-Type: application/json" \
  -d '{"status":"YES","plusOne":1,"notes":"Bringing my spouse"}'

# Expected: 201 Created with RSVP details including id, eventId, memberId, status
```

**Expected outcome**: RSVP is created. Response contains the RSVP object with status `YES`.

## Scenario 2: RSVP Upsert (Change Mind)

```bash
# Change RSVP from YES to NO
curl -s -b cookies.txt -X POST "http://localhost:8080/api/events/$EVENT_ID/rsvp" \
  -H "Content-Type: application/json" \
  -d '{"status":"NO"}'

# Expected: 200 OK — same RSVP record, status now "NO"
```

**Expected outcome**: RSVP updated in place. No duplicate record.

## Scenario 3: Verify Event RSVP Count Updated

```bash
# Get event details — should show updated counts
curl -s http://localhost:8080/api/events/$EVENT_ID | jq '.rsvpCount, .rsvpBreakdown'
```

**Expected outcome**: `rsvpCount` > 0 and `rsvpBreakdown` reflects the current RSVP states.

## Scenario 4: Admin Views All RSVPs

```bash
# 1. Login as admin (seed user: admin@lionsclub.org / password123)
curl -s -c admin-cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@lionsclub.org","password":"password123"}'

# 2. List all RSVPs for the event
curl -s -b admin-cookies.txt "http://localhost:8080/api/events/$EVENT_ID/rsvps" | jq
```

**Expected outcome**: Array of RSVPs with member details (firstName, lastName, email).

## Scenario 5: Capacity Enforcement (if maxAttendees set)

```bash
# Find an event with maxAttendees set (or create one)
# RSVP YES until capacity is reached
# Next YES RSVP should return 409

curl -s -b cookies.txt -X POST "http://localhost:8080/api/events/$EVENT_ID/rsvp" \
  -H "Content-Type: application/json" \
  -d '{"status":"YES"}' | jq .

# If at capacity, expected: 409 with {"message": "Event is at full capacity"}
```

## Scenario 6: Unauthenticated Request

```bash
curl -s -X POST "http://localhost:8080/api/events/$EVENT_ID/rsvp" \
  -H "Content-Type: application/json" \
  -d '{"status":"YES"}'

# Expected: 401 Unauthorized
```

## Run Tests

```bash
# Run all tests
./mvnw test

# Run RSVP-specific tests
./mvnw test -pl . -Dtest="RsvpControllerTest,RsvpServiceTest,RsvpRepositoryTest,RsvpTest"

# Run with Testcontainers (full integration)
./mvnw verify
```

See [contracts/api.md](./contracts/api.md) for full request/response shapes and [data-model.md](./data-model.md) for entity details.
