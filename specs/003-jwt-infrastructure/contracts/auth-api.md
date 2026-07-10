# Auth API Contract

**Date**: 2026-07-10 | **Feature**: 003-jwt-infrastructure

## POST /api/auth/login

Authenticate a user by email and password. Sets `auth_token` cookie on success.

### Request
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

### Response 200 — Success
```
Set-Cookie: auth_token=<jwt>; HttpOnly; SameSite=Lax; Path=/; Max-Age=900
Content-Type: application/json

{
  "message": "Login successful"
}
```

### Response 401 — Invalid credentials
```
Content-Type: application/json

{
  "error": "Invalid credentials"
}
```

### Response 401 — Account disabled
```
Content-Type: application/json

{
  "error": "Account disabled"
}
```

### Validation Rules
| Field | Rule |
|-------|------|
| `email` | Not blank, valid email format |
| `password` | Not blank |

---

## POST /api/auth/register

Create a new user account and set auth cookie.

### Request
```
POST /api/auth/register
Content-Type: application/json

{
  "email": "newuser@example.com",
  "password": "securePassword123",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Response 201 — Created
```
Set-Cookie: auth_token=<jwt>; HttpOnly; SameSite=Lax; Path=/; Max-Age=900
Content-Type: application/json

{
  "message": "Registration successful"
}
```

### Response 400 — Validation error
```
Content-Type: application/json

{
  "error": "Validation failed",
  "details": {
    "email": "Email is already in use"
  }
}
```

### Response 409 — Email conflict
```
Content-Type: application/json

{
  "error": "Email is already registered"
}
```

### Validation Rules
| Field | Rule |
|-------|------|
| `email` | Not blank, valid email format, unique |
| `password` | Not blank, min 8 characters |
| `firstName` | Not blank |
| `lastName` | Not blank |

---

## POST /api/auth/logout

Clear the auth cookie. Requires valid auth cookie.

### Request
```
POST /api/auth/logout
Cookie: auth_token=<jwt>
```

### Response 200 — Success
```
Set-Cookie: auth_token=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0
Content-Type: application/json

{
  "message": "Logout successful"
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

## Common Response Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created (registration) |
| 400 | Validation error (bad request body) |
| 401 | Unauthenticated (no/expired/invalid token, bad credentials) |
| 403 | Forbidden (authenticated but wrong role) |
| 409 | Conflict (email already registered) |
