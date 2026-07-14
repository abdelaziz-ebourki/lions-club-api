# Quickstart Validation: Complete Auth Endpoints

**Date**: 2026-07-14 | **Feature**: 004-complete-auth-endpoints

## Prerequisites

- Java 21 (JDK)
- Docker (for PostgreSQL via Testcontainers)
- Feature 003 (JWT infrastructure) fully implemented and tested

## Setup

```bash
# Build and run all tests
./mvnw test
```

## Validation Scenarios

### 1. Get current user profile (/me) with valid cookie

```bash
# Start the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
# Register a user first, capture the auth_token cookie
curl -v -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123", "firstName": "Test", "lastName": "User"}'
```

Expected: `201 Created` with `Set-Cookie: auth_token=<jwt>`.

```bash
# Use the captured cookie to get profile
curl -v http://localhost:8080/api/auth/me \
  -H "Cookie: auth_token=<jwt>"
```

Expected: `200 OK` with JSON body containing `id`, `email`, `firstName`, `lastName`, `role`.

### 2. /me returns 401 without cookie

```bash
curl -v http://localhost:8080/api/auth/me
```

Expected: `401 Unauthorized` with `{"error": "Unauthorized"}`.

### 3. /me returns 401 with invalid cookie

```bash
curl -v http://localhost:8080/api/auth/me \
  -H "Cookie: auth_token=invalid-token"
```

Expected: `401 Unauthorized`.

### 4. Refresh token with valid cookie

```bash
curl -v -X POST http://localhost:8080/api/auth/refresh \
  -H "Cookie: auth_token=<jwt>"
```

Expected: `200 OK` with new `Set-Cookie: auth_token=<new-jwt>` with a fresh `Max-Age`.

### 5. Refresh returns 401 without cookie

```bash
curl -v -X POST http://localhost:8080/api/auth/refresh
```

Expected: `401 Unauthorized`.

### 6. Chain: /me works after refresh

After calling refresh (step 4), use the **new** cookie to call /me:

```bash
curl -v http://localhost:8080/api/auth/me \
  -H "Cookie: auth_token=<new-jwt>"
```

Expected: `200 OK` — proves the refreshed token is valid.

## Running Automated Tests

```bash
# Run all tests (unit + integration)
./mvnw test

# Run only auth-related tests
./mvnw test -Dtest="*Auth*"
```

Expected: All tests pass (including existing auth tests from feature 003).

## Test Structure

| Test Class | Type | What It Validates |
|------------|------|-------------------|
| `AuthControllerTest` | Integration | /me happy path, /me 401 without/invalid cookie, /refresh happy path, /refresh 401 without cookie |
| `AuthServiceTest` | Unit | `getCurrentUser()` returns correct DTO, throws on disabled/deleted user; `refreshToken()` issues valid new token |

## Contracts

See [contracts/auth-api.md](contracts/auth-api.md) for the new endpoint contracts.
See [data-model.md](data-model.md) for the UserResponse DTO structure.
