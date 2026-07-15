# Research: Seed Data / Dev Data Setup

## Approach Decision

| Decision | Chosen | Rationale | Alternatives Considered |
|----------|--------|-----------|------------------------|
| Seeding mechanism | `@Component` + `CommandLineRunner` (`DataSeeder`) | Runs automatically on startup, has full access to Spring beans (PasswordEncoder, repositories), supports `@Profile` filtering. Matches the issue's recommended approach. | Flyway V4 seed migration (SQL-only, cannot use BCrypt encoder bean, hardcoded hashes fragile across encoder upgrades) |
| Profile gating | `@Profile("dev")` | Prevents any seed data from loading in production. Dev is the default profile, so it activates automatically for local development. | Manual profile check in code (more error-prone) |
| Idempotency check | `userRepository.count() == 0` | Simple, reliable, and matches the issue's recommendation. Only seeds when the users table is empty. | Checking each table individually (over-engineered for dev data) |
| User ID strategy | Deterministic UUIDs (`UUID.nameUUIDFromBytes(STRING_ID.getBytes())`) | Produces the same UUID every time from the same string ID (e.g., "admin-1" → consistent UUID). Satisfies FR-008 (IDs match mock data for frontend testing) without changing the entity's UUID type. | Hardcoded UUIDs (fragile), string IDs (requires entity schema change), auto-generated UUIDs (breaks frontend test consistency) |
| Event status mapping | "upcoming" → `PUBLISHED`, "past" → `COMPLETED` | The entity enum has `DRAFT, PUBLISHED, CANCELLED, COMPLETED`. The frontend derives "upcoming"/"past" from the event date, but the spec requires explicit status. | Adding "upcoming"/"past" to enum (schema change) |
| Event time modeling | Single `startDateTime` + `endDateTime` (2-hour duration) | Matches existing `Event` entity schema. Date from mock data becomes `startDateTime`, time from mock data sets the time component. | Separate date/time fields (would require schema change) |

## Entity Mapping Decisions

### User seed data → Entity mapping

| Mock Data Field | Entity Field | Transformation |
|-----------------|-------------|----------------|
| `id: "admin-1"` | `id: UUID` | `UUID.nameUUIDFromBytes("admin-1".getBytes())` |
| `name: "Ahmed Benali"` | `firstName`, `lastName` | Split on first space: "Ahmed" / "Benali" |
| `email: "admin@lionsclub.com"` | `email` | Direct |
| `password: "admin123 (bcrypt-hashed)"` | `passwordHash` | `passwordEncoder.encode("admin123")` |
| `role: "admin"` | `role` | `Role.ADMIN` |

### Event seed data → Entity mapping

| Mock Data Field | Entity Field | Transformation |
|-----------------|-------------|----------------|
| `date: "2026-09-15"` + `time: "19:00"` | `startDateTime` | `LocalDateTime.parse("2026-09-15T19:00")` |
| (duration not specified) | `endDateTime` | `startDateTime.plusHours(2)` (existing convention from EventService) |
| `status: "upcoming"` | `status` | `EventStatus.PUBLISHED` |
| `status: "past"` | `status` | `EventStatus.COMPLETED` |
| `location: "Hyatt Regency Casablanca"` | `location` | Direct |
| (not in mock) | `address` | `null` |
| (not in mock) | `maxAttendees` | `null` |
| (not in mock) | `createdBy` | The admin user (seeded first, referenced by ID) |

## Technology Stack (Existing)

- Spring Boot 3.4.4
- Java 21
- PostgreSQL (local dev) / H2 not used — all tests use Testcontainers PostgreSQL
- Flyway (V1–V3 migrations already applied)
- BCryptPasswordEncoder (bean in SecurityConfig)
- Lombok (@RequiredArgsConstructor for constructor injection)
- Testcontainers (PostgreSQL 15-alpine for integration tests)
