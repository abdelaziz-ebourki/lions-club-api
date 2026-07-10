# Research: JWT Infrastructure

**Date**: 2026-07-10 | **Feature**: 003-jwt-infrastructure

## Token Strategy

- **Decision**: Single JWT stored in `auth_token` httpOnly cookie
- **Rationale**: Cookie-based auth eliminates client-side token management; the browser automatically sends the cookie on every request. A single JWT with 15-minute expiry is sufficient — there's no access/refresh token pair since the client cannot programmatically read httpOnly cookies to refresh.
- **Alternatives considered**: Bearer token (constitution's OpenApiConfig already defines bearer-jwt scheme, but spec FR-003 requires cookie); access+refresh pair (unnecessary for cookie-based flow since the server controls the cookie lifecycle)

## JWT Claims Structure

- **Decision**: `sub` = userId (UUID), `role` = Role enum name (STRING), `iat`, `exp`
- **Rationale**: Auth0 java-jwt 4.5.0 (already in pom.xml) supports HS256 signing; userId identifies the principal, role enables `@PreAuthorize` checks.
- **Alternatives considered**: Including email or full name (unnecessary — adds token size without benefit; the SecurityContext can hold user details if needed)

## Signing Algorithm

- **Decision**: HMAC-SHA256 (HS256) with configurable secret (min 256 bits)
- **Rationale**: Symmetric algorithm — single service, no need for RS256 key pairs; Auth0 java-jwt supports HS256 natively; `app.jwt.secret` property is simple to configure.
- **Alternatives considered**: RS256 (asymmetric, overkill for single-service deployment)

## Cookie Configuration

- **Decision**: `Set-Cookie: auth_token=<jwt>; HttpOnly; SameSite=Lax; Path=/; Max-Age=<tokenExpirySeconds>`
- **Rationale**: HttpOnly prevents XSS token theft; SameSite=Lax prevents CSRF for same-site navigation; no `Secure` flag in dev (added for production assumption).
- **Alternatives considered**: SameSite=Strict (prevents cross-site navigation but breaks OAuth-like flows); Secure always (requires HTTPS which isn't available in local dev)

## Spring Security Filter Chain

- **Decision**: Register `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter` in the filter chain; `SecurityConfig` uses `SecurityFilterChain` bean (lambda DSL) not the deprecated `WebSecurityConfigurerAdapter`
- **Rationale**: Standard Spring Boot 3.4 pattern; cookie-based JWT must run before the default auth filter to set the SecurityContext before controllers execute.
- **Alternatives considered**: Custom `AuthenticationProvider` (unnecessarily complex — the filter reads the cookie and sets a `UsernamePasswordAuthenticationToken` directly)

## Password Encoding

- **Decision**: BCrypt via Spring Security's `PasswordEncoder` bean; the existing User entity's `passwordHash` field already stores BCrypt hashes
- **Rationale**: Spring Security's built-in `BCryptPasswordEncoder` is the industry standard; no new dependencies needed.

## Error Responses

- **Decision**: Spring Security's `AuthenticationEntryPoint` sends 401 with `{ "error": "Unauthorized" }`; access-denied sends 403 with `{ "error": "Forbidden" }`
- **Rationale**: Consistent JSON error responses; the default Spring Security HTML error pages are not suitable for a REST API.
- **Alternatives considered**: Custom `@ExceptionHandler` in a controller advice (complementary approach, but the entry point handles filter-level auth failures before controllers are reached)

## Testing Approach

- **Decision**: Unit tests for JwtTokenProvider (generate, validate, extract); `@WebMvcTest` with mocked JwtTokenProvider for JwtAuthenticationFilter; `@SpringBootTest` with Testcontainers for AuthController integration
- **Rationale**: Unit tests isolate token logic; web slice tests validate filter behavior; full integration tests validate end-to-end auth flow (login → use cookie → protected resource → logout)
- **Alternatives considered**: Pure unit tests for filters (MockHttpServletRequest/Response is sufficient); full integration for everything (too slow for dev loop)
