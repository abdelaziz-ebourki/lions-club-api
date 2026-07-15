# Quickstart Validation: Event Entity, Repository & Flyway V2

**Date**: 2026-07-15 | **Feature**: 005-event-entity-repository

## Prerequisites

- Java 21 (JDK)
- Docker (for PostgreSQL via Testcontainers)
- Feature 002 (User entity + repository + V1 migration) fully implemented and tested
- JPA auditing (`@EnableJpaAuditing`) already configured from prior feature

## Setup

```bash
# Build and run all tests
./mvnw test
```

## Validation Scenarios

### 1. Application starts with V2 migration applied

```bash
# Start the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected: Application starts without errors. The `flyway_schema_history` table records the V2 migration.

### 2. V2 migration creates events table

Connect to PostgreSQL and verify:

```sql
\d events
```

Expected: Table exists with columns: `id` (UUID PK), `title`, `description`, `start_date_time`, `end_date_time`, `location`, `address`, `max_attendees`, `status`, `created_by` (FK to users.id), `created_at`, `updated_at`.

### 3. EventRepository bean is available

```bash
./mvnw test -Dtest="EventRepositoryTest#contextLoads"
```

Expected: Test passes — EventRepository bean is registered in the application context.

### 4. Event can be persisted and retrieved

Verifiable via the repository integration tests:

```bash
./mvnw test -Dtest="EventRepositoryTest"
```

Expected: All tests pass — events can be persisted with various statuses, found by status, and timestamps are auto-populated.

## Running Automated Tests

```bash
# Run all tests (unit + integration)
./mvnw test

# Run only event-related tests
./mvnw test -Dtest="*Event*"
```

Expected: All tests pass (including existing User tests from feature 002).

## Test Structure

| Test Class | Type | What It Validates |
|------------|------|-------------------|
| `EventStatusTest` | Unit | Enum values exist (DRAFT, PUBLISHED, CANCELLED, COMPLETED), parse correctly, exactly 4 values |
| `EventTest` | Unit | Entity fields map to `events` table columns, bean validation annotations present |
| `EventRepositoryTest` | Integration | Bean registration, CRUD operations, findByStatus, timestamp auto-population |

## Data Model

See [data-model.md](data-model.md) for the Event entity structure and relationships.
