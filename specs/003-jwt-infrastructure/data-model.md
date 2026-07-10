# Data Model: JWT Infrastructure

**Date**: 2026-07-10 | **Feature**: 003-jwt-infrastructure

## Existing Entities (from issue #2)

### User
| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID (PK) | Auto-generated | `@GeneratedValue(strategy = GenerationType.UUID)` |
| email | String | `@NotBlank`, `@Email`, unique | Login identifier |
| passwordHash | String | `@NotBlank` | BCrypt hash |
| firstName | String | `@NotBlank` | — |
| lastName | String | `@NotBlank` | — |
| role | Role (enum) | `@NotNull`, `@Enumerated(EnumType.STRING)` | Values: `MEMBER`, `ADMIN` |
| enabled | boolean | `@NotNull`, default false | Must be true to authenticate |
| createdAt | LocalDateTime | `@CreatedDate`, not updatable | Auditing |
| updatedAt | LocalDateTime | `@LastModifiedDate` | Auditing |

### Role (enum)
| Value | Description |
|-------|-------------|
| `ADMIN` | Full CRUD access on events, members, forum |
| `MEMBER` | Read access on events, limited write |

## New Types

### JwtConfig (@ConfigurationProperties)
| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `app.jwt.secret` | String | — | HMAC-SHA256 key (≥256 bits) |
| `app.jwt.expiration` | Duration | `15m` | Token expiry (was `access-token-expiration`) |

### JWT Claims
| Claim | Type | Source | Description |
|-------|------|--------|-------------|
| `sub` | String (UUID) | `User.id` | Principal identifier |
| `role` | String | `User.role.name()` | Role for `@PreAuthorize` |
| `iat` | long | Current time | Issued-at timestamp |
| `exp` | long | `iat + expiration` | Expiry timestamp |

## Relationships

```
User (1) ──── role ──── Role (enum)
  │
  └─── has many JWTs over time (stateless — no persistence)
```

## Lifecycle / State Transitions

```
Registration:
  [User not exists] → POST /api/auth/register → [User created, JWT issued, cookie set]

Login:
  [User exists, enabled] → POST /api/auth/login (email+password) → [JWT issued, cookie set]

Authenticated Request:
  [auth_token cookie present] → JwtAuthenticationFilter → [JWT validated] → [SecurityContext set] → [Controller]

Logout:
  [auth_token cookie present] → POST /api/auth/logout → [cookie cleared, Max-Age=0]

Token Expiry:
  [auth_token cookie present, JWT expired] → JwtAuthenticationFilter → [401 response, cookie NOT cleared]

Invalid Token:
  [auth_token cookie present, invalid signature] → JwtAuthenticationFilter → [401 response]
```
