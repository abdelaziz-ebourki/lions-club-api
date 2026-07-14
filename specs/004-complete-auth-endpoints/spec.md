# Feature Specification: Complete Auth Endpoints

**Feature Branch**: `004-complete-auth-endpoints`

**Created**: 2026-07-14

**Status**: Draft

**Input**: User description: "Complete auth endpoints — GET /api/auth/me and POST /api/auth/refresh for issue #4"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Member views their profile (Priority: P1)

A logged-in member wants to see their account information (name, email, role) in the UI. They navigate to a profile page or settings panel, and the system returns their current profile data.

**Why this priority**: The `/me` endpoint is the primary way for the frontend to identify the currently authenticated user and their role, which drives UI decisions (showing admin panels, member features, etc.).

**Independent Test**: Can be fully tested by logging in, then requesting `/api/auth/me` with the auth cookie and verifying the response contains the correct user data.

**Acceptance Scenarios**:

1. **Given** a valid `auth_token` cookie exists, **When** a GET request is made to `/api/auth/me`, **Then** the server returns 200 with `{"id", "email", "firstName", "lastName", "role"}` in JSON format
2. **Given** no auth cookie exists, **When** a GET request is made to `/api/auth/me`, **Then** the server returns 401 with `{"error": "Unauthorized"}`
3. **Given** an expired or invalid `auth_token` cookie, **When** a GET request is made to `/api/auth/me`, **Then** the server returns 401 with `{"error": "Unauthorized"}`
4. **Given** a valid `auth_token` cookie for a disabled or deleted user, **When** a GET request is made to `/api/auth/me`, **Then** the server returns 401 with `{"error": "Unauthorized"}`

---

### User Story 2 - Member extends their session (Priority: P1)

A member has been using the site for a while and their session is about to expire. The frontend proactively calls refresh to get a new token with a fresh expiry, keeping the user logged in without interrupting their activity.

**Why this priority**: Without a refresh mechanism, users would be logged out after the token expiry (15 minutes by default), causing a poor UX. The refresh endpoint enables seamless long-lived sessions.

**Independent Test**: Can be fully tested by logging in, waiting (or simulating), calling `/api/auth/refresh`, and confirming a new cookie is issued.

**Acceptance Scenarios**:

1. **Given** a valid `auth_token` cookie exists, **When** a POST request is made to `/api/auth/refresh`, **Then** the server returns 200 with `{"message": "Token refreshed"}` and a new `auth_token` cookie with a `Max-Age` matching the configured expiry (default 15 minutes)
2. **Given** no auth cookie exists, **When** a POST request is made to `/api/auth/refresh`, **Then** the server returns 401 with `{"error": "Unauthorized"}`
3. **Given** an expired or invalid `auth_token` cookie, **When** a POST request is made to `/api/auth/refresh`, **Then** the server returns 401 with `{"error": "Unauthorized"}`
4. **Given** a valid `auth_token` cookie for a disabled or deleted user, **When** a POST request is made to `/api/auth/refresh`, **Then** the server returns 401 with `{"error": "Unauthorized"}`

---

### Edge Cases

- What happens when an authenticated user's account has been disabled since they logged in? (Returns 401 to prevent access)
- What happens when the refresh endpoint is called with a token that was issued for a deleted user? (Returns 401)
- What happens when a rapid succession of refresh calls is made? (Each call issues a new valid token — no rate limiting in v1. The last-issued token is the effective one; there is no rotation or invalidation of prior tokens.)
- What happens when concurrent `/me` and `/refresh` calls are made with the same token? (Both succeed — the `/refresh` response may include a new cookie that replaces the one used by `/me`, but `/me` still returns the correct data.)
- What happens when a user's role changes between JWT issuance and a `/me` call? (The `/me` response reflects the current role from the database, not the role in the JWT.)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose `GET /api/auth/me` as an authenticated endpoint that returns the current user's identity details (id, email, firstName, lastName, role) in JSON format
- **FR-002**: System MUST respond with 401 and `{"error": "Unauthorized"}` when `GET /api/auth/me` is called without a valid `auth_token` cookie (same error format as feature 003 login endpoint)
- **FR-003**: System MUST expose `POST /api/auth/refresh` as an authenticated endpoint that issues a new `auth_token` cookie with a fresh expiry matching the configured `app.jwt.expiration` duration (default 15 minutes) for the current user, and returns `{"message": "Token refreshed"}`
- **FR-004**: System MUST respond with 401 and `{"error": "Unauthorized"}` when `POST /api/auth/refresh` is called without a valid `auth_token` cookie (same error format as feature 003 login endpoint)
- **FR-005**: System MUST validate that the authenticated user's account is still enabled before returning data on `/me` or issuing a new token on `/refresh`
- **FR-006**: System MUST return the user's role (MEMBER or ADMIN) in the `/me` response to enable role-based UI decisions on the frontend

### Key Entities

- **User Profile Response**: A data shape containing `id`, `email`, `firstName`, `lastName`, and `role` — represents the currently authenticated user
- **Auth Cookie**: The existing `auth_token` httpOnly cookie; `/refresh` updates its value and expiry while keeping all other cookie attributes unchanged

### Non-Functional Notes

- Observability (logging, metrics) for auth events such as refresh calls and failed `/me` attempts is not required for v1. The existing application-level logging from the JWT filter and controllers is sufficient for debugging.
- All 401 responses use the identical body `{"error": "Unauthorized"}` regardless of the specific failure reason (no cookie, expired, invalid, disabled, deleted) to prevent user enumeration.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Authenticated users can retrieve their own profile via `GET /api/auth/me` or refresh their session via `POST /api/auth/refresh` in under 200ms (excluding network latency)
- **SC-002**: Unauthenticated requests to `/api/auth/me` receive a 401 with `{"error": "Unauthorized"}` — the same body is returned regardless of whether the cause is a missing, expired, invalid, or disabled-user scenario (no data leakage)
- **SC-003**: Authenticated users can successfully refresh their session via `POST /api/auth/refresh` and receive a new cookie whose Max-Age is set relative to the current time, matching the configured `app.jwt.expiration` duration (default 15 minutes)
- **SC-004**: Disabled or deleted user accounts receive a 401 response on both `/me` and `/refresh`, preventing access after account deactivation
- **SC-005**: The `/me` response contains the correct role so the frontend can distinguish MEMBER from ADMIN users

## Assumptions

- The existing JWT infrastructure (issue #3 / feature 003) is fully operational — token generation, validation, cookie setting, and authentication filter are in place
- The frontend sends `credentials: "include"` on requests to `/api/auth/me` and `/api/auth/refresh` so the auth cookie is included
- No token blacklist or refresh token rotation is needed for v1 — the refresh endpoint simply issues a new standard JWT
- The refresh endpoint requires a currently valid (non-expired) token rather than implementing a grace-period refresh for recently expired tokens
- The same cookie attributes (httpOnly, SameSite=Lax, Path=/, secure flag) apply to the new token issued by `/refresh`
