# Quickstart Validation Guide: JWT Principal Wrapper

**Feature**: 010-jwt-principal-wrapper  
**Purpose**: Verify the typed principal works end-to-end after implementation

---

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ (Testcontainers used for integration tests)
- IDE with Spring Boot support (IntelliJ IDEA / VS Code)

---

## Setup

```bash
cd /home/abdelaziz/Desktop/portfolio/lions-club-remastered/lions-club-api

# Run all tests (should pass after implementation)
./mvnw test

# Run specific test classes
./mvnw test -Dtest=JwtAuthenticationFilterTest
./mvnw test -Dtest=*Controller*Test
```

---

## Validation Scenarios

### Scenario 1: Filter Creates UserPrincipal from Valid JWT

**Test**: `JwtAuthenticationFilterTest.validToken_setsUserPrincipalAuthentication`

```java
// Given: Valid JWT cookie with all claims
// When: Request passes through filter
// Then: SecurityContext authentication principal is UserPrincipal
//       with correct userId, email, role, firstName, lastName
```

**Run**: `./mvnw test -Dtest=JwtAuthenticationFilterTest#validToken_setsUserPrincipalAuthentication`

---

### Scenario 2: Controller Injects UserPrincipal via @AuthenticationPrincipal

**Test**: `EventControllerTest.createEvent_injectsPrincipal`

```java
// Given: Authenticated admin request with @WithMockUserPrincipal
// When: POST /api/events with valid EventRequest
// Then: Event created with creator = principal.userId()
//       No UserRepository.findById() called
```

**Run**: `./mvnw test -Dtest=EventControllerTest#createEvent_injectsPrincipal`

---

### Scenario 3: Method Security Evaluates principal.userId

**Test**: `EventServiceTest.preAuthorize_principalUserId`

```java
// Given: @PreAuthorize("principal.userId == #userId") on service method
// When: Member calls method with their own userId
// Then: Access granted (expression evaluates true)
```

**Run**: `./mvnw test -Dtest=EventServiceTest#preAuthorize_principalUserId`

---

### Scenario 4: Method Security Evaluates principal.role

**Test**: `RsvpControllerTest.preAuthorize_adminRole`

```java
// Given: @PreAuthorize("principal.role == T(Role).ADMIN") on admin endpoint
// When: Admin user calls GET /api/events/{id}/rsvps
// Then: 200 OK with RSVP list
```

**Run**: `./mvnw test -Dtest=RsvpControllerTest#preAuthorize_adminRole`

---

### Scenario 5: Controllers Have No getCurrentUser() Boilerplate

**Test**: Manual code inspection (no automated test)

**Verify**:
- `EventController` has no `getCurrentUser()` method
- `RsvpController` has no `getCurrentUser()` method
- `AuthController.getCurrentUser()` still exists (uses UserService, not UserRepository directly)
- All controllers use `@AuthenticationPrincipal UserPrincipal principal`

**Check**:
```bash
grep -n "getCurrentUser" src/main/java/com/lionsclub/api/web/*.java
# Should return NO matches for EventController, RsvpController
```

---

### Scenario 6: JWT Token Contains All Required Claims

**Test**: `AuthServiceTest.loginToken_containsAllClaims`

```java
// Given: User logs in with email/password
// When: Login succeeds
// Then: Response cookie contains JWT with claims:
//       sub, role, email, firstName, lastName, iat, exp
```

**Run**: `./mvnw test -Dtest=AuthServiceTest#loginToken_containsAllClaims`

---

### Scenario 7: Invalid JWT Clears Security Context

**Test**: `JwtAuthenticationFilterTest.invalidToken_clearsContext`

```java
// Given: Request with expired/malformed JWT cookie
// When: Filter processes request
// Then: SecurityContextHolder.getContext().getAuthentication() == null
```

**Run**: `./mvnw test -Dtest=JwtAuthenticationFilterTest#invalidToken_clearsContext`

---

### Scenario 8: Full Integration — RSVP Flow with Typed Principal

**Test**: `RsvpControllerIntegrationTest.rsvpFlow_withTypedPrincipal`

```java
// Given: Member authenticated via @WithMockUserPrincipal
// When: POST /api/events/{id}/rsvp with status YES
// Then: 201 Created, RSVP created with memberId = principal.userId()
//       No UserRepository query in controller
```

**Run**: `./mvnw test -Dtest=RsvpControllerIntegrationTest#rsvpFlow_withTypedPrincipal`

---

## Success Criteria Checklist

| Criterion | Verification |
|-----------|--------------|
| SC-001: Zero `getCurrentUser()` in controllers | `grep` check passes |
| SC-002: All endpoints access user via @AuthenticationPrincipal | All controller tests pass |
| SC-003: @PreAuthorize with principal.* works | Method security tests pass |
| SC-004: All 143 existing tests pass | `./mvnw test` → BUILD SUCCESS |
| SC-005: JWT token size increase minimal | Token length < 500 chars (was ~400) |

---

## Debugging Tips

### Inspect Actual JWT Claims

```java
// In test or debug breakpoint:
DecodedJWT jwt = JWT.decode(tokenFromCookie);
System.out.println("Claims: " + jwt.getClaims().keySet());
jwt.getClaims().forEach((k, v) -> System.out.println(k + " = " + v.asString()));
```

### Verify Principal in Controller

```java
@GetMapping("/debug/principal")
public ResponseEntity<?> debugPrincipal(@AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok(Map.of(
        "userId", principal.userId(),
        "email", principal.email(),
        "role", principal.role(),
        "fullName", principal.fullName()
    ));
}
```

### Check Security Context in Filter Test

```java
SecurityContext context = SecurityContextHolder.getContext();
Authentication auth = context.getAuthentication();
assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);
UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
assertThat(principal.userId()).isEqualTo(expectedUserId);
```

---

## Expected Test Output (After Implementation)

```
Tests run: 143, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### New Tests Added (Estimated)

| Test Class | New Tests |
|------------|-----------|
| `JwtAuthenticationFilterTest` | +4 (UserPrincipal creation, claims, error handling) |
| `AuthServiceTest` | +2 (token claims verification) |
| `EventControllerTest` | +2 (principal injection, no getCurrentUser) |
| `RsvpControllerTest` | +2 (principal injection, method security) |
| `WithMockUserPrincipalTest` | +3 (annotation factory) |
| **Total** | **+13 new tests** |

---

## Rollback Plan

If issues arise:

1. Revert `JwtAuthenticationFilter` to set String principal
2. Revert `JwtTokenProvider.createToken()` to 2-parameter signature
3. Restore `getCurrentUser()` methods in controllers
4. Revert test annotations to `@WithMockUser`

All changes are localized to:
- `JwtAuthenticationFilter.java`
- `JwtTokenProvider.java`
- `AuthService.java` (token creation call site)
- `EventController.java`, `RsvpController.java` (injection style)
- Test classes (annotations)