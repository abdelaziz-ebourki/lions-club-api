# Research: Complete Auth Endpoints

**Date**: 2026-07-14 | **Feature**: 004-complete-auth-endpoints

## Research Tasks

Since this feature extends the existing JWT infrastructure (003), no technology choices need resolution. Two design decisions were evaluated:

### 1. Token Refresh Strategy

| Aspect | Decision |
|--------|----------|
| Decision | Issue a new JWT with same claims (userId, role) and fresh expiry |
| Rationale | Simplest approach that extends session lifetime; no refresh token rotation or blacklisting needed for v1 |
| Alternatives | Grace-period refresh (allow refresh of recently expired tokens) — adds complexity without proven need; opaque refresh tokens stored in DB — overkill for v1 |

### 2. Error Response for Deleted/Disabled Users on /me and /refresh

| Aspect | Decision |
|--------|----------|
| Decision | Return 401 Unauthorized (same as invalid/expired token) |
| Rationale | Prevents user enumeration via different error messages for "no cookie" vs "user deleted" |
| Alternatives | Return 404 Not Found — leaks information about user existence; return 403 Forbidden — semantically incorrect for authentication failure |

## Key Findings

- All technology choices are already established in the project constitution and feature 003
- No new dependencies, libraries, or tools are needed
- The existing test infrastructure (Testcontainers, MockMvc, `@Sql` cleanup) is sufficient
- Controller and service patterns are well-established from the existing `AuthController` and `AuthService`
