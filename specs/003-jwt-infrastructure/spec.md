# Feature Specification: JWT Infrastructure

**Feature Branch**: `003-jwt-infrastructure`

**Created**: 2026-07-10

**Status**: Draft

**Input**: User description: "JWT infrastructure — JwtTokenProvider + SecurityConfig + cookie-based auth"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Visitor browses public content (Priority: P1)

A first-time visitor accesses the Lions Club website. They should be able to view public content (events, swagger docs) without any authentication.

**Why this priority**: Public access is the baseline — every other flow depends on the distinction between public and protected resources being correct.

**Independent Test**: Can be fully tested by requesting public endpoints without sending any auth cookie, confirming they return 200.

**Acceptance Scenarios**:

1. **Given** no auth cookie exists, **When** a GET request is made to `/api/events/`, **Then** the server returns 200 with event data
2. **Given** no auth cookie exists, **When** a request is made to `/swagger-ui/`, **Then** the server returns the Swagger UI page
3. **Given** no auth cookie exists, **When** a request is made to `/v3/api-docs`, **Then** the server returns the OpenAPI spec

---

### User Story 2 - Authenticated member accesses protected resources (Priority: P1)

A member who has logged in should have their requests authenticated automatically via the httpOnly cookie, and be able to access member-level resources.

**Why this priority**: The core auth flow — cookie-based authentication is what enables all subsequent member functionality.

**Independent Test**: Can be fully tested by obtaining a valid JWT via login, then using it to access member-level endpoints.

**Acceptance Scenarios**:

1. **Given** a valid `auth_token` cookie exists for a MEMBER role user, **When** a POST request is made to `/api/events/`, **Then** the server returns 403 Forbidden (members cannot create events)
2. **Given** a valid `auth_token` cookie exists, **When** the cookie expires, **Then** subsequent requests return 401 Unauthorized
3. **Given** a tampered `auth_token` cookie exists with invalid signature, **When** any request is made, **Then** the server returns 401 Unauthorized

---

### User Story 3 - Admin manages resources (Priority: P1)

An administrator needs to create, update, and delete resources (events, members, forum content). Their ADMIN role must be recognized from the JWT.

**Why this priority**: Admin operations are the most sensitive — role-based authorization must work correctly to protect data integrity.

**Independent Test**: Can be fully tested by logging in as ADMIN, then performing CRUD operations and confirming authorization.

**Acceptance Scenarios**:

1. **Given** a valid `auth_token` cookie exists for an ADMIN role user, **When** a POST request is made to `/api/events`, **Then** the server returns 201 Created with the event data
2. **Given** a valid `auth_token` cookie exists for a MEMBER role user, **When** a DELETE request is made to `/api/events/1`, **Then** the server returns 403 Forbidden
3. **Given** a valid `auth_token` cookie exists for an ADMIN role user, **When** a DELETE request is made to `/api/events/1`, **Then** the server returns 204 No Content

---

### User Story 4 - Frontend developer integrates with cookie-based auth (Priority: P2)

A frontend developer building the UI needs to understand the authentication contract: no manual token handling, just `credentials: "include"` on every fetch request.

**Why this priority**: While backend functionality works without this being explicit, documenting the contract prevents integration issues.

**Independent Test**: Can be tested by confirming the `Set-Cookie` header is present on login responses and the `auth_token` cookie is sent on subsequent requests.

**Acceptance Scenarios**:

1. **Given** a successful login response, **When** the server responds, **Then** the `Set-Cookie` header includes `auth_token=<jwt>` with `httpOnly`, `SameSite=Lax`, `Path=/`, `Max-Age` matching the configured token expiry
2. **Given** a logout request with a valid cookie, **When** the server responds, **Then** the `Set-Cookie` header sets `Max-Age=0` to clear the cookie

---

### Edge Cases

- What happens when a request includes both a valid `auth_token` cookie AND an `Authorization` header? (Cookie takes precedence per filter logic)
- How does the system handle a JWT with valid signature but expired `exp` claim? (Returns 401)
- What happens when CORS preflight (OPTIONS) is sent from an unlisted origin? (Returns appropriate CORS error)
- How does the system behave during clock skew between server and JWT issuer? (Standard leeway of a few seconds is acceptable)

## Clarifications

### Session 2026-07-10

- Q: Should the login endpoint contract be defined now? → A: Yes, define the full login endpoint contract now (request body, response body, cookie-setting behavior, error responses).
- Q: How should the User role be modeled? → A: Role as a JPA enum field (`MEMBER`, `ADMIN`) on the existing User entity.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST generate a JWT containing userId and role claims with a configurable expiry (default 15 minutes)
- **FR-002**: System MUST validate JWT signature and expiry on every protected request
- **FR-003**: System MUST read the JWT from the `auth_token` httpOnly cookie, not from the Authorization header
- **FR-004**: System MUST set the `auth_token` cookie with `httpOnly=true`, `SameSite=Lax`, `Path=/` and `Max-Age` matching the token's configured expiry
- **FR-005**: System MUST expose `POST /api/auth/login` as a public endpoint accepting `{ "email": "...", "password": "..." }` and responding with `Set-Cookie: auth_token=<jwt>` on success (200) or `{ "error": "Invalid credentials" }` on failure (401)
- **FR-006**: System MUST expose `POST /api/auth/register` as a public endpoint accepting `{ "email": "...", "password": "...", "firstName": "...", "lastName": "..." }`, creating the user with role MEMBER and enabled=true, setting the auth cookie, and returning 201
- **FR-007**: System MUST expose `POST /api/auth/logout` as an authenticated endpoint that clears the `auth_token` cookie by setting `Max-Age=0` and returns 200
- **FR-008**: System MUST allow public access to: `POST /api/auth/login`, `POST /api/auth/register`, GET `/api/events/**`, `/swagger-ui/**`, `/v3/api-docs/**`
- **FR-009**: System MUST require authentication for all POST/PUT/PATCH/DELETE operations under `/api/events/`
- **FR-010**: System MUST require ADMIN role for: POST `/api/events`, PUT `/api/events/**`, DELETE `/api/events/**`, `/api/admin/**`, GET `/api/contact`, POST/PUT/DELETE `/api/members`, DELETE `/api/forum/replies`, PATCH/DELETE `/api/forum/threads`
- **FR-011**: System MUST use stateless session management (no HttpSession)
- **FR-012**: System MUST allow CORS requests from `http://localhost:5173` with credentials enabled
- **FR-013**: System MUST disable CSRF protection (stateless JWT auth)
- **FR-014**: System MUST disable form login and HTTP Basic authentication

### Key Entities

- **JWT Token**: A signed JSON Web Token containing `userId` (UUID) and `role` (enum) claims, stored in an httpOnly cookie
- **Auth Cookie**: The `auth_token` cookie that the browser sends automatically on every request
- **Security Context**: Spring Security's SecurityContextHolder containing the authenticated user's details after successful JWT validation
- **Role**: An enum (`MEMBER`, `ADMIN`) stored as a field on the `User` entity; mapped to JPA via `@Enumerated(EnumType.STRING)`; used in JWT `role` claim and `@PreAuthorize` checks

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Unauthenticated requests to protected endpoints return 401 with no redirect
- **SC-002**: Authenticated requests with valid JWT pass through without being challenged
- **SC-003**: Invalid or expired JWT returns 401 regardless of endpoint
- **SC-004**: CORS preflight requests from `http://localhost:5173` succeed with credentials
- **SC-005**: All public endpoints (login, register, GET events, Swagger docs) are accessible without any auth cookie
- **SC-006**: Role-based authorization correctly distinguishes MEMBER from ADMIN for all protected operations
- **SC-007**: Logout clears the auth cookie on the client side

## Assumptions

- The JWT secret is configured via `app.jwt.secret` (min 256 bits for HS256)
- Token expiry is configurable via `app.jwt.expiration` (default 15m)
- The frontend is hosted on `http://localhost:5173` during development
- Production CORS origins and secure cookie flag will be configured separately
- The existing `User` entity and `UserRepository` are already in place (issue #2)
- No token blacklisting is needed for v1 (token expiry is sufficient invalidation)
- A single JWT with configurable expiry (default 15 minutes) is stored in the `auth_token` cookie; no separate access/refresh token pair
