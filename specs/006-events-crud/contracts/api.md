# API Contracts: Events CRUD

**Date**: 2026-07-15

**Base URL**: `http://localhost:8080`

## Endpoints

### `GET /api/events`

Public. List all events, optionally filtered by status.

**Query parameters**:

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| status | string | No | `"upcoming"`, `"ongoing"`, or `"past"`. Omit for all events. |

**Response 200**:
```json
[
  {
    "id": "uuid-string",
    "title": "string",
    "description": "string",
    "date": "2026-09-15",
    "time": "19:00",
    "location": "string",
    "category": "Health | Environment | Youth | Community | Fundraiser",
    "status": "upcoming | ongoing | past",
    "rsvpCount": 0,
    "createdAt": "ISO timestamp",
    "updatedAt": "ISO timestamp"
  }
]
```

**Error**: 400 if invalid status value provided.

---

### `GET /api/events/:id`

Public. Get single event by ID.

**Response 200**: Single `EventResponse` object (same shape as above).

**Response 404**: Empty body.

---

### `POST /api/events`

Admin only (requires ADMIN role in JWT). Create a new event.

**Request**:
```json
{
  "title": "string (min 3)",
  "description": "string (min 10)",
  "date": "2026-09-15",
  "time": "19:00",
  "location": "string (min 3)",
  "category": "Health | Environment | Youth | Community | Fundraiser",
  "status": "upcoming (default, optional)"
}
```

**Response 201**: Single `EventResponse` object with server-generated `id`.

**Error 400**: Validation error — `{ "message": "description" }`

**Error 401**: Unauthenticated — `{ "error": "Unauthorized" }`

**Error 403**: Non-admin — `{ "error": "Forbidden" }`

---

### `PUT /api/events/:id`

Admin only. Full-object replacement.

**Request**: Same shape as POST (all fields required).

**Response 200**: Updated `EventResponse` object.

**Response 404**: Empty body.

**Response 400**: Validation error.

**Response 403**: Forbidden.

---

### `DELETE /api/events/:id`

Admin only.

**Response 200**:
```json
{ "success": true }
```

**Response 404**: Empty body.

**Response 403**: Forbidden.

---

## Error Response Shape

All errors (4xx):
```json
{ "message": "string describing the error" }
```

Auth errors:
```json
{ "error": "Unauthorized" }
```

---

## Security

- **Auth**: JWT in `auth_token` cookie (HttpOnly, SameSite=Lax)
- **Public**: `GET /api/events` and `GET /api/events/:id`
- **Admin**: `POST`, `PUT`, `DELETE` — checked via `SecurityConfig` `.hasRole(ADMIN)`
