# Quickstart: OpenAPI Configuration Validation

## Prerequisites

- Application running locally (dev profile): `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

## Validation Scenarios

### 1. OpenAPI JSON Spec

```bash
curl -s http://localhost:8080/api-docs | jq .
```

**Expected**: Valid JSON with:
- `info.title` = "Lions Club FSBM API"
- `info.version` = "0.1.0"
- `paths` contains all endpoints: `/api/auth/login`, `/api/auth/register`, `/api/auth/logout`, `/api/auth/me`, `/api/auth/refresh`, `/api/events`, `/api/events/{id}`
- `components.securitySchemes` includes `cookie-jwt` scheme

### 2. Swagger UI

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui.html
```

**Expected**: HTTP 200 (HTML page content)

### 3. Unauthenticated Access (No Auth Required)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api-docs
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui.html
```

**Expected**: Both return 200 without any auth headers or cookies.

### 4. Auth Endpoint Documentation Completeness

```bash
curl -s http://localhost:8080/api-docs | jq '.paths["/api/auth/login"] | has("post")'
curl -s http://localhost:8080/api-docs | jq '.paths["/api/auth/register"] | has("post")'
curl -s http://localhost:8080/api-docs | jq '.paths["/api/auth/logout"] | has("post")'
```

**Expected**: All return `true`, and each has `summary` and `responses` fields.

### 5. Automated Tests

```bash
./mvnw test -pl . -Dtest=OpenApiConfigTest,SecurityConfigTest
```

**Expected**: All tests pass (these verify metadata and security configuration).

## Contract References

- API contract details: [contracts/api.md](contracts/api.md)
- No data model changes for this feature
