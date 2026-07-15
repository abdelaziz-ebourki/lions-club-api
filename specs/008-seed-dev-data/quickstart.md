# Quickstart: Seed Data / Dev Data Setup

## Validation Scenarios

### Prerequisites

- PostgreSQL running locally (or Docker)
- Project cloned and built: `./mvnw clean compile -q`

### Scenario 1: Seed data loads automatically in dev profile

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Expected outcome**:
- Application starts without errors
- Console shows Flyway migrations applied (V1, V2, V3)
- Seed data loaded (look for log line from DataSeeder)

### Scenario 2: Login with seeded admin credentials

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@lionsclub.com","password":"admin123"}' | jq .
```

**Expected outcome**: HTTP 200 with JWT token in `Set-Cookie` header and JSON body containing success message.

### Scenario 3: Login with seeded member credentials

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"fatima@lionsclub.com","password":"member123"}' | jq .
```

**Expected outcome**: HTTP 200 with JWT token. User has MEMBER role.

### Scenario 4: Fetch seeded events

```bash
curl -s http://localhost:8080/api/events | jq '.'
```

**Expected outcome**: HTTP 200. Response contains 5 events — 3 upcoming (Annual Charity Gala 2026, Community Clean-Up Day, Health & Wellness Workshop) and 2 past (Sight Screening Camp, Youth Leadership Summit).

### Scenario 5: Idempotency — restart does not duplicate data

1. Run the application (Scenario 1)
2. Stop the application (Ctrl+C)
3. Run the application again
4. Fetch events (Scenario 4)

**Expected outcome**: Events list still shows exactly 5 events — no duplicates.

### Scenario 6: Seed data excluded in production profile

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

**Expected outcome**: Application starts without seed data logs.

## Test Verification

Run the seeder-specific test:

```bash
./mvnw test -pl . -Dtest=DataSeederTest
```

Expected: All tests pass (empty DB seeds, non-empty DB skips, non-dev profile skips).

Run the full test suite to verify no regressions:

```bash
./mvnw verify -DskipITs=false
```

Expected: All tests pass (unit + integration).
