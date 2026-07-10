# Quickstart Validation: JWT Infrastructure

**Date**: 2026-07-10 | **Feature**: 003-jwt-infrastructure

## Prerequisites

- Java 21 (JDK)
- Docker (for PostgreSQL via Testcontainers)
- No existing PostgreSQL instance required — Testcontainers spins one up

## Setup

```bash
# Build and run tests (includes all new auth tests)
./mvnw test
```

## Validation Scenarios

### 1. Public endpoints are accessible without auth

```bash
# Start the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
# Test public endpoints (these should return 200)
curl -v http://localhost:8080/api/events        # GET events — 200
curl -v http://localhost:8080/swagger-ui/       # Swagger UI — 200
curl -v http://localhost:8080/v3/api-docs       # OpenAPI spec — 200
```

### 2. Register a new user

```bash
curl -v -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123", "firstName": "Test", "lastName": "User"}'
```

Expected: `201 Created` with `Set-Cookie: auth_token=<jwt>` header.

### 3. Login with credentials

```bash
curl -v -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}'
```

Expected: `200 OK` with `Set-Cookie: auth_token=<jwt>` header.

### 4. Access protected resource with cookie

```bash
# Capture the cookie from step 2 or 3, then:
curl -v -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -H "Cookie: auth_token=<jwt>" \
  -d '{"title": "New Event", "date": "2026-08-01"}'
```

Expected: `403 Forbidden` for MEMBER role (members cannot create events).

### 5. Admin creates an event

Seed an ADMIN user, login, then:

```bash
curl -v -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -H "Cookie: auth_token=<admin-jwt>" \
  -d '{"title": "New Event", "date": "2026-08-01"}'
```

Expected: `201 Created`.

### 6. Logout clears cookie

```bash
curl -v -X POST http://localhost:8080/api/auth/logout \
  -H "Cookie: auth_token=<jwt>"
```

Expected: `200 OK` with `Set-Cookie: auth_token=; Max-Age=0`.

### 7. Protected endpoint returns 401 without cookie

```bash
curl -v -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"title": "New Event"}'
```

Expected: `401 Unauthorized`.

### 8. CORS preflight from frontend origin

```bash
curl -v -X OPTIONS http://localhost:8080/api/auth/login \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST"
```

Expected: `200 OK` with `Access-Control-Allow-Origin: http://localhost:5173`.

### 9. Invalid token returns 401

```bash
curl -v http://localhost:8080/api/events/1 \
  -H "Cookie: auth_token=invalid-token"
```

Expected: `401 Unauthorized`.

## Running Automated Tests

```bash
# Run all tests (unit + integration)
./mvnw test

# Run only auth-related tests
./mvnw test -Dtest="*Jwt*,*Auth*,*Security*"
```

Expected: All new tests pass (full suite green including existing tests).

## Test Structure

| Test Class | Type | What It Validates |
|------------|------|-------------------|
| `JwtTokenProviderTest` | Unit | Token generation, signature validation, expiry, claims extraction |
| `JwtAuthenticationFilterTest` | Unit | Cookie reading, SecurityContext setting, invalid token handling |
| `SecurityConfigTest` | Integration | Endpoint permissions, role-based access, CORS, CSRF, stateless session |
| `AuthControllerTest` | Integration | Login/register/logout flows, error responses, cookie headers |

## Contracts

See [contracts/auth-api.md](contracts/auth-api.md) for full request/response shapes.
See [data-model.md](data-model.md) for entity and claim structures.
