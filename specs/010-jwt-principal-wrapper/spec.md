# Feature Specification: JWT Principal Wrapper

**Feature Branch**: `010-jwt-principal-wrapper`

**Created**: 2026-07-16

**Status**: Draft

**Input**: User description: "Consider using a lightweight principal wrapper in JwtAuthenticationFilter"

## User Scenarios & Testing

### User Story 1 - Developer Uses Typed Principal for Authentication (Priority: P1)

A developer implements a new secured endpoint that needs access to the authenticated user's ID, email, and role. Instead of casting the principal to String and querying the UserRepository, they inject the principal directly via `@AuthenticationPrincipal` and access typed fields.

**Why this priority**: Current code repeats the same boilerplate in every controller — casting principal to String, parsing UUID, querying UserRepository. A typed wrapper eliminates this duplication and enables method-level security.

**Independent Test**: Can be tested by creating a test controller endpoint that accepts `@AuthenticationPrincipal UserPrincipal principal` and verifies the principal contains userId, email, and role without any repository calls.

**Acceptance Scenarios**:

1. **Given** a valid JWT cookie is present, **When** a request reaches a secured endpoint, **Then** the authentication principal is an instance of `UserPrincipal` containing userId, email, and role.
2. **Given** an endpoint uses `@AuthenticationPrincipal UserPrincipal principal`, **When** the endpoint is called with valid auth, **Then** the principal fields are populated correctly without additional database queries.
3. **Given** an invalid or expired JWT, **When** the request is processed, **Then** the security context is cleared and no principal is set.

---

### User Story 2 - Method-Level Security Works with Typed Principal (Priority: P2)

A developer adds `@PreAuthorize("@securityService.canAccessEvent(#eventId, principal)")` to a controller method. The `SecurityService.canAccessEvent` receives the `UserPrincipal` directly and can evaluate permissions without database lookups.

**Why this priority**: Spring Security's method-level security (`@EnableMethodSecurity`) can resolve `@AuthenticationPrincipal` arguments. A typed principal makes this pattern practical and performant.

**Independent Test**: Can be tested by creating a service method annotated with `@PreAuthorize` that takes `UserPrincipal` and verifying it receives the typed principal with correct data.

**Acceptance Scenarios**:

1. **Given** a method annotated with `@PreAuthorize` referencing `principal.userId`, **When** called with valid authentication, **Then** the expression evaluates using the typed principal's userId field.
2. **Given** a method annotated with `@PreAuthorize` referencing `principal.role`, **When** called by an admin user, **Then** the expression correctly evaluates the admin role.

---

### User Story 3 - Controllers Simplified by Removing Boilerplate (Priority: P2)

Existing controllers (`EventController`, `RsvpController`, future controllers) no longer need the `getCurrentUser()` helper method that casts principal to String, parses UUID, and queries UserRepository.

**Why this priority**: Reduces code duplication across all controllers, eliminates N+1 query risk, and makes controller logic cleaner.

**Independent Test**: Can be tested by verifying `EventController` and `RsvpController` no longer contain `getCurrentUser()` methods and instead use `@AuthenticationPrincipal UserPrincipal principal`.

**Acceptance Scenarios**:

1. **Given** `EventController.createEvent` uses `@AuthenticationPrincipal UserPrincipal principal`, **When** an admin creates an event, **Then** the creator is set from `principal.userId()` without a repository call.
2. **Given** `RsvpController.createOrUpdateRsvp` uses `@AuthenticationPrincipal UserPrincipal principal`, **When** a member RSVPs, **Then** the member is resolved from `principal.userId()` without a repository call.

---

### Edge Cases

- What happens when JWT contains a userId that no longer exists in the database? The filter should still create the principal (token is valid) but downstream code handles "user not found" at the service layer.
- What happens when JWT is missing the "role" claim? The filter should handle gracefully (default to MEMBER or fail closed).
- What happens when multiple JWT tokens are present? The filter uses the first "auth_token" cookie.
- Should the principal include full name (firstName + lastName)? Initially include for display purposes but keep it minimal.

## Requirements

### Functional Requirements

- **FR-001**: System MUST provide a `UserPrincipal` record/class containing at minimum: `userId` (UUID), `email` (String), `role` (Role enum), `firstName` (String), `lastName` (String).
- **FR-002**: `JwtAuthenticationFilter` MUST create and set `UserPrincipal` as the authentication principal instead of raw String userId.
- **FR-003**: `UserPrincipal` MUST implement `java.security.Principal` (or be compatible with `@AuthenticationPrincipal`).
- **FR-004**: The authentication authorities MUST still be derived from the JWT role claim (ROLE_ADMIN, ROLE_MEMBER).
- **FR-005**: Existing role-based access control in `SecurityConfig` MUST continue to work unchanged.
- **FR-006**: Controllers MUST be able to inject `UserPrincipal` via `@AuthenticationPrincipal UserPrincipal principal`.
- **FR-007**: Method-level security (`@PreAuthorize`, `@PostAuthorize`) MUST be able to access `principal.userId`, `principal.role`, etc.
- **FR-008**: The solution MUST NOT require additional database queries in the filter — all principal data comes from JWT claims.

### Key Entities

- **UserPrincipal**: Lightweight immutable record representing the authenticated user. Contains: userId (UUID), email (String), role (Role enum), firstName (String), lastName (String). Created from JWT claims in the filter. Does NOT implement UserDetails — it's a custom principal for this application.
- **JwtTokenProvider**: Existing component that validates JWT and extracts claims. Extended to include firstName, lastName in token (or filter fetches from DB once — but requirement FR-008 says no DB queries in filter, so claims must contain all needed data).

## Success Criteria

### Measurable Outcomes

- **SC-001**: Zero controllers contain `getCurrentUser()` helper method that queries UserRepository after implementation.
- **SC-002**: All secured endpoints can access authenticated user data via `@AuthenticationPrincipal UserPrincipal` without additional database queries.
- **SC-003**: Method-level security expressions (e.g., `@PreAuthorize("principal.userId == #userId")`) work correctly with the typed principal.
- **SC-004**: All existing tests pass (143 tests) without modification to authentication logic — only test updates for the new principal type.
- **SC-005**: JWT token size increase is minimal (adding firstName, lastName claims ~50-100 bytes).

## Assumptions

- The JWT token generation (login/register) will be updated to include `firstName`, `lastName`, and `role` as claims so the filter has all data without DB access.
- The `Role` enum (MEMBER, ADMIN) already exists and is used in the User entity.
- No changes to `SecurityConfig` authorization rules are needed — only the principal type changes.
- The `JwtTokenProvider` currently only extracts `subject` (userId) and `role` claim; it will be extended to include `email`, `firstName`, `lastName`.
- Existing integration tests that check authentication will need updates to expect `UserPrincipal` instead of String principal.
- This is an internal refactoring — no API contract changes for clients (OpenAPI spec unchanged).