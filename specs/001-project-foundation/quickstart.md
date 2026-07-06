# Quickstart: Project Foundation

## Prerequisites

- Java 21
- Docker
- Docker Compose

## Setup

```bash
# 1. Start the database
cp .env.example .env  # if not present
docker compose up -d

# 2. Wait for PostgreSQL to be healthy
docker compose ps

# 3. Start the application
./mvnw spring-boot:run
```

## Validation Scenarios

### Scenario 1: Application boots successfully

```bash
curl -s http://localhost:8080/actuator/health | jq .
# Expected: {"status":"UP"}
```

### Scenario 2: Flyway migration applied

```bash
# Check Flyway has run the baseline migration
curl -s http://localhost:8080/actuator/health | jq .status
# Application starts without errors → migrations ran

# Or check via Docker:
docker exec -it lions-club-db psql -U lions_club -d lions_club -c \
  "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
# Expected: V1, "create users table", true
```

### Scenario 3: SpringDoc UI loads

```bash
# Open in browser or:
curl -s http://localhost:8080/swagger-ui.html | head -5
# Expected: HTML page with Swagger UI title

# Check OpenAPI JSON:
curl -s http://localhost:8080/v3/api-docs | jq '.info.title'
# Expected: "Lions Club FSBM API"
```

### Scenario 4: Profiles work

```bash
# Dev profile (default):
./mvnw spring-boot:run
# Swagger UI accessible

# Prod profile:
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
# Swagger UI should return 404
```

## Expected Outcomes

| Check | Command | Expected |
|-------|---------|----------|
| App health | `curl localhost:8080/actuator/health` | `{"status":"UP"}` |
| DB reachable | `docker compose ps` | PostgreSQL container healthy |
| Migration ran | Flyway history query | V1 migration recorded success=true |
| Swagger UI | Browser to `/swagger-ui.html` | Page loads (dev profile) |
| API docs | `curl /v3/api-docs` | JSON with info.title set |
