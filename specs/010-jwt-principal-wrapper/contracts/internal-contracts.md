# Internal Contracts: JWT Principal Wrapper

**Feature**: 010-jwt-principal-wrapper  
**Type**: Internal refactoring — no external API contract changes  
**OpenAPI Spec**: Unchanged (clients unaffected)

---

## Contract 1: JwtTokenProvider Interface

**File**: `src/main/java/com/lionsclub/api/security/JwtTokenProvider.java`

### Method: createToken (Extended Signature)

```java
String createToken(
    UUID userId,
    String email,
    Role role,
    String firstName,
    String lastName
);
```

**Preconditions**:
- `userId` non-null valid UUID
- `email` non-null valid email format
- `role` non-null (MEMBER or ADMIN)
- `firstName` non-null non-empty
- `lastName` non-null non-empty

**Postconditions**:
- Returns signed JWT string (HS256)
- Token contains claims: `sub`, `role`, `email`, `firstName`, `lastName`, `iat`, `exp`
- Token expires per `JwtConfig.expiration` (default 24h)

**Error Cases**:
- `IllegalArgumentException` if any parameter null/empty

---

### Method: validateToken (Unchanged)

```java
DecodedJWT validateToken(String token);
```

**Preconditions**:
- `token` non-null non-empty string

**Postconditions**:
- Returns `DecodedJWT` with all claims accessible via `.getClaim(name).asString()`
- Throws `RuntimeException` (wrapped `JWTVerificationException`) if:
  - Signature invalid
  - Token expired
  - Token malformed

---

## Contract 2: JwtAuthenticationFilter Behavior

**File**: `src/main/java/com/lionsclub/api/security/JwtAuthenticationFilter.java`

### Input: HTTP Request with Cookie

| Cookie Name | Required | Description |
|-------------|----------|-------------|
| `auth_token` | Yes | JWT string from login/register response |

### Output: SecurityContext Authentication

| Property | Value |
|----------|-------|
| `Authentication.getPrincipal()` | `UserPrincipal` instance (never String) |
| `Authentication.getAuthorities()` | Single `SimpleGrantedAuthority("ROLE_ADMIN")` or `ROLE_MEMBER` |
| `Authentication.getCredentials()` | `null` |
| `Authentication.isAuthenticated()` | `true` if valid token, else no authentication set |

### Error Handling

| Condition | Behavior |
|-----------|----------|
| No `auth_token` cookie | `filterChain.doFilter()` — no authentication set |
| Invalid/expired token | `SecurityContextHolder.clearContext()` — no authentication set |
| Valid token | `SecurityContextHolder.getContext().setAuthentication(auth)` with `UserPrincipal` |

---

## Contract 3: Controller Injection Contract

### @AuthenticationPrincipal UserPrincipal

All secured controllers can inject:

```java
@GetMapping("/example")
public ResponseEntity<?> example(@AuthenticationPrincipal UserPrincipal principal) {
    // principal.userId() — UUID
    // principal.email() — String
    // principal.role() — Role enum
    // principal.firstName() — String
    // principal.lastName() — String
    // principal.fullName() — String (convenience)
    // principal.authority() — "ROLE_ADMIN" or "ROLE_MEMBER"
}
```

**Guarantees**:
- `principal` never null in secured endpoints (Spring Security ensures authentication)
- All fields non-null (validated at token creation)
- No database query required — data from JWT claims

---

## Contract 4: Method Security SpEL Expressions

### Available Variables in `@PreAuthorize` / `@PostAuthorize`

| Expression | Type | Example |
|------------|------|---------|
| `principal.userId` | UUID | `principal.userId == #userId` |
| `principal.email` | String | `principal.email == 'admin@example.com'` |
| `principal.role` | Role enum | `principal.role == T(com.lionsclub.api.domain.user.Role).ADMIN` |
| `principal.firstName` | String | `principal.firstName == 'John'` |
| `principal.lastName` | String | `principal.lastName == 'Doe'` |
| `principal.fullName()` | String | `principal.fullName().contains('John')` |
| `principal.authority()` | String | `principal.authority() == 'ROLE_ADMIN'` |

### Usage Example

```java
@PreAuthorize("principal.role == T(com.lionsclub.api.domain.user.Role).ADMIN")
@DeleteMapping("/api/admin/users/{id}")
public void deleteUser(@PathVariable UUID id) { ... }

@PreAuthorize("principal.userId == #userId or principal.role == T(com.lionsclub.api.domain.user.Role).ADMIN")
@GetMapping("/api/users/{userId}/profile")
public UserProfile getProfile(@PathVariable UUID userId) { ... }
```

---

## Contract 5: Test Support — WithMockUserPrincipal

**File**: `src/test/java/com/lionsclub/api/security/WithMockUserPrincipal.java`

### Annotation

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockUserPrincipalSecurityContextFactory.class)
public @interface WithMockUserPrincipal {
    String userId();
    String email();
    String role(); // "MEMBER" or "ADMIN"
    String firstName();
    String lastName();
}
```

### Usage in Tests

```java
@Test
@WithMockUserPrincipal(
    userId = "550e8400-e29b-41d4-a716-446655440000",
    email = "john@example.com",
    role = "MEMBER",
    firstName = "John",
    lastName = "Doe"
)
void testSecuredEndpoint(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/api/events"))
       .andExpect(status().isOk());
}
```

---

## Contract Compatibility

| Contract | Change Type | Breaking? |
|----------|-------------|-----------|
| REST API (OpenAPI) | None | No |
| `JwtTokenProvider.createToken()` | Signature extended (5 params vs 2) | **Yes — internal only** |
| `JwtAuthenticationFilter` principal type | String → UserPrincipal | **Yes — internal only** |
| Controller injection | String cast → @AuthenticationPrincipal | **Yes — internal only** |
| Test annotations | @WithMockUser → @WithMockUserPrincipal | **Yes — test code only** |

**Migration**: All internal callers updated in single PR. No external consumers affected.