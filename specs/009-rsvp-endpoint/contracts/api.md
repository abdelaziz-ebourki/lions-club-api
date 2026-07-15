# API Contracts: RSVP Endpoint

**Date**: 2026-07-15

**Base URL**: `http://localhost:8080`

## Endpoints

### `POST /api/events/:id/rsvp`

Authenticated (MEMBER or ADMIN). Create or update the authenticated member's RSVP for an event (upsert).

**Request**:
```json
{
  "status": "YES | NO | MAYBE",
  "plusOne": 0,
  "notes": "string (optional)"
}
```

**Validation rules**:
| Field | Rule |
|-------|------|
| status | Required. Must be one of: `YES`, `NO`, `MAYBE` |
| plusOne | Optional. Integer >= 0. Default 0. |
| notes | Optional. Free text. Max 500 characters. |

**Response 201** (created — first RSVP):
```json
{
  "id": "uuid-string",
  "eventId": "uuid-string",
  "memberId": "uuid-string",
  "status": "YES",
  "plusOne": 0,
  "notes": "Looking forward to it!",
  "createdAt": "2026-07-15T19:00:00Z",
  "updatedAt": "2026-07-15T19:00:00Z"
}
```

**Response 200** (updated — existing RSVP changed):
Same body shape as 201.

**Error 400**: Event is CANCELLED or COMPLETED.
```json
{ "message": "Cannot RSVP to a cancelled or completed event" }
```

**Error 400**: Invalid status value.
```json
{ "message": "Invalid RSVP status. Must be YES, NO, or MAYBE" }
```

**Error 401**: Not authenticated.
```json
{ "error": "Unauthorized" }
```

**Error 404**: Event not found.
```json
{ "message": "Event not found" }
```

**Error 409**: Event is full (maxAttendees reached, YES RSVP attempted).
```json
{ "message": "Event is at full capacity" }
```

---

### `GET /api/events/:id/rsvps`

Admin only. List all RSVPs for an event.

**Response 200**:
```json
[
  {
    "id": "uuid-string",
    "eventId": "uuid-string",
    "member": {
      "id": "uuid-string",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@example.com"
    },
    "status": "YES",
    "plusOne": 2,
    "notes": null,
    "createdAt": "2026-07-15T19:00:00Z",
    "updatedAt": "2026-07-15T19:00:00Z"
  }
]
```

**Error 403**: Non-admin user.
```json
{ "error": "Forbidden" }
```

**Error 404**: Event not found.

---

### `GET /api/events/:id` (modified — existing endpoint)

Public. RSVP counts now populated dynamically (replaces hardcoded `rsvpCount: 0`).

**Relevant changes to response**:
```json
{
  "rsvpCount": 4,
  "rsvpBreakdown": {
    "yes": 3,
    "no": 0,
    "maybe": 1
  }
}
```

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

| Endpoint | Auth Required | Role Required |
|----------|---------------|---------------|
| `POST /api/events/:id/rsvp` | Yes | MEMBER or ADMIN |
| `GET /api/events/:id/rsvps` | Yes | ADMIN only |
| `GET /api/events/:id` (modified) | No | Public (unchanged) |
