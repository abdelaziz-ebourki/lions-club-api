# Research: JWT Principal Wrapper

**Date**: 2026-07-16
**Feature**: specs/010-jwt-principal-wrapper
**Status**: Complete — all NEEDS CLARIFICATION resolved

---

## R1: UserPrincipal Type Requirements

**Question**: Does `UserPrincipal` need to implement `Principal` interface or is a plain record sufficient for `@AuthenticationPrincipal`?

**Decision**: Plain record is sufficient.

**Rationale**: Spring Security's `AuthenticationPrincipalArgumentResolver` (registered by default with `@EnableWebMvc` or Spring Boot) resolves any principal type returned by `Authentication.getPrincipal()`. It does not require `java.security.Principal` implementation. The interface is only needed if code explicitly calls `principal.getName()` via the `Principal` type.

**Implementation**: Implement `java.security.Principal` anyway (single method `getName()` returning email) for compatibility with any legacy code expecting the interface.

**Alternatives Considered**:
- Implement `UserDetails` — rejected: overkill, couples to Spring Security internals
- Extend `AbstractAuthenticationToken` — rejected: unnecessary complexity
- Plain record — chosen: minimal, immutable, serializable, works with SpEL

---

## R2: Adding Custom Claims with Auth0 java-jwt

**Question**: How to add email, firstName, lastName claims to JWT token using Auth0 java-jwt library?

**Decision**: Use `JWT.create().withClaim(key, value)` for each custom claim.

**Rationale**: Auth0 java-jwt 4.x supports arbitrary string claims via `withClaim(String, String)`. The token creation happens in `AuthService.login()` and `AuthService.register()`.

**Code Pattern**:
```java
// In AuthService when creating token
String token = JWT.create()
    .withSubject(user.getId().toString())
    .withClaim("role", user.getRole().name())
    .withClaim("email", user.getEmail())
    .withClaim("firstName", user.getFirstName())
    .withClaim("lastName", user.getLastName())
    .withExpiresAt(Instant.now().plus(jwtConfig.getExpiration()))
    .sign(algorithm);
```

**Extraction in Filter**:
```java
var decoded = jwtTokenProvider.validateToken(token);
String userId = decoded.getSubject();
String email = decoded.getClaim("email").asString();
String firstName = decoded.getClaim("firstName").asString();
String lastName = decoded.getClaim("lastName").asString();
String role = decoded.getClaim("role").asString();
```

**Alternatives Considered**:
- Use `withArrayClaim` for roles — rejected: single role per user
- Store full user JSON in single claim — rejected: larger token, parsing complexity

---

## R3: Spring Security Method Security with Typed Principal

**Question**: How does `@EnableMethodSecurity` work with `@AuthenticationPrincipal` and SpEL expressions referencing principal fields?

**Decision**: Works out of the box with `@EnableMethodSecurity` (already present in `SecurityConfig`).

**Rationale**: 
- `@EnableMethodSecurity` registers `MethodSecurityExpressionHandler` which supports `principal` variable in SpEL
- `AuthenticationPrincipalArgumentResolver` resolves `@AuthenticationPrincipal UserPrincipal principal` by calling `authentication.getPrincipal()` and casting
- SpEL `principal.userId` invokes `userId()` on the record (or `getUserId()` if using JavaBean convention)

**Verification**: 
```java
@PreAuthorize("principal.userId == #userId")
public void someMethod(@AuthenticationPrincipal UserPrincipal principal, UUID userId) { ... }
```

**Alternatives Considered**:
- Custom `SecurityExpressionRoot` — rejected: unnecessary for simple field access
- `@AuthenticationPrincipal(expression = "principal.userId")` — rejected: not a valid attribute

---

## R4: Testing Patterns for UserPrincipal

**Question**: How to test controllers that now expect `UserPrincipal` via `@AuthenticationPrincipal`?

**Decision**: Use Spring Security's test support with custom `WithSecurityContextTestExecutionListener` or `@WithMockUser` equivalent for custom principal.

**Patterns**:

**Unit Tests (Service/Filter)**:
```java
// Direct instantiation
UserPrincipal principal = new UserPrincipal(userId, email, Role.MEMBER, "John", "Doe");
```

**Integration Tests (Controller)**:
```java
@Test
@WithMockUserPrincipal(userId = "uuid", email = "test@example.com", role = "MEMBER", firstName = "John", lastName = "Doe")
void testEndpoint(@Autowired MockMvc mvc) { ... }
```

**Custom Annotation**:
```java
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockUserPrincipalSecurityContextFactory.class)
public @interface WithMockUserPrincipal {
    String userId();
    String email();
    String role();
    String firstName();
    String lastName();
}

public class WithMockUserPrincipalSecurityContextFactory implements WithSecurityContextFactory<WithMockUserPrincipal> {
    @Override
    public SecurityContext createSecurityContext(WithMockUserPrincipal annotation) {
        UserPrincipal principal = new UserPrincipal(
            UUID.fromString(annotation.userId()),
            annotation.email(),
            Role.valueOf(annotation.role()),
            annotation.firstName(),
            annotation.lastName()
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
```

**Alternatives Considered**:
- Manual `SecurityContextHolder.setContext()` in `@BeforeEach` — rejected: verbose, not reusable
- `@WithMockUser` + principal casting — rejected: doesn't work with custom principal type

---

## Summary of Decisions

| Research Item | Decision | Key Rationale |
|---------------|----------|---------------|
| R1: Principal type | Record implementing `Principal` | Minimal, works with `@AuthenticationPrincipal`, SpEL compatible |
| R2: JWT claims | `withClaim()` for each field | Standard Auth0 java-jwt API, simple extraction |
| R3: Method security | `@EnableMethodSecurity` (existing) | Built-in SpEL support for `principal.field` |
| R4: Testing | Custom `@WithMockUserPrincipal` annotation | Reusable, clean integration tests, matches production principal type |

All clarifications resolved. Ready for Phase 1 design.