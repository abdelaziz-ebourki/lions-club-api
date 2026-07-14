# Auth API Contract — Addendum

**Date**: 2026-07-14 | **Feature**: 004-complete-auth-endpoints

This document extends the base auth contract [`specs/003-jwt-infrastructure/contracts/auth-api.md`](../../003-jwt-infrastructure/contracts/auth-api.md) with two new endpoints. All existing endpoint contracts remain unchanged.

---

## GET /api/auth/me

Return the currently authenticated user's profile. Requires valid `auth_token` cookie.

### Request
```
GET /api/auth/me
Cookie: auth_token=<jwt>
```

### Response 200 — Success
```
Content-Type: application/json

{
  "id": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "MEMBER"
}
```

### Response 401 — Not authenticated
```
Content-Type: application/json

{
  "error": "Unauthorized"
}
```

### Response 401 — User disabled or deleted
```
Content-Type: application/json

{
  "error": "Unauthorized"
}
```

---

## POST /api/auth/refresh

Issue a new `auth_token` cookie with a fresh expiry for the currently authenticated user. Requires valid `auth_token` cookie.

### Request
```
POST /api/auth/refresh
Cookie: auth_token=<jwt>
```

### Response 200 — Success
```
Set-Cookie: auth_token=<new-jwt>; HttpOnly; SameSite=Lax; Path=/; Max-Age=<configured-expiry>
Content-Type: application/json

{
  "message": "Token refreshed"
}
```

### Response 401 — Not authenticated
```
Content-Type: application/json

{
  "error": "Unauthorized"
}
```

---

## Validation Rules

| Endpoint | Field | Rule |
|----------|-------|------|
| `GET /api/auth/me` | (none) | Valid `auth_token` cookie required |
| `POST /api/auth/refresh` | (none) | Valid `auth_token` cookie required |

Both endpoints reuse the existing JWT validation rules (signature, expiry, user enabled).
