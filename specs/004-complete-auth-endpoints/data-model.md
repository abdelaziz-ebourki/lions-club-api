# Data Model: Complete Auth Endpoints

**Date**: 2026-07-14 | **Feature**: 004-complete-auth-endpoints

## New Types

### UserResponse (DTO)

| Field | Type | Source | Description |
|-------|------|--------|-------------|
| `id` | UUID | `User.id` | Unique user identifier |
| `email` | String | `User.email` | User's email address |
| `firstName` | String | `User.firstName` | User's first name |
| `lastName` | String | `User.lastName` | User's last name |
| `role` | String | `User.role.name()` | User's role (`MEMBER` or `ADMIN`) |

This DTO is returned by `GET /api/auth/me`. It sits at the API boundary — the controller never exposes the `User` entity directly.

### Auth Token (existing, unchanged)

Re-used by `POST /api/auth/refresh`:

| Attribute | Value |
|-----------|-------|
| Cookie name | `auth_token` |
| Claims | `sub` (userId), `role`, `iat`, `exp` |
| Expiry after refresh | Same configured duration as original (default 15 min via `app.jwt.expiration`) |
| Cookie attributes | `httpOnly=true`, `SameSite=Lax`, `Path=/`, `secure` (per environment) |

## Lifecycle / State Transitions

```
Get Profile (/me):
  [valid auth_token cookie] → JwtAuthenticationFilter → [SecurityContext set with userId]
  → AuthController.getCurrentUser() → AuthService.getCurrentUser(userId)
  → UserRepository.findById(userId) → [user enabled] → UserResponse → 200
  → [user disabled/not found] → 401

Refresh Token:
  [valid auth_token cookie] → JwtAuthenticationFilter → [SecurityContext set with userId]
  → AuthController.refresh() → AuthService.refreshToken(userId)
  → UserRepository.findById(userId) → [user enabled] → new JWT → new cookie → 200
  → [user disabled/not found] → 401
```
