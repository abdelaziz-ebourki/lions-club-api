# Data Model: Project Foundation

## Entities

### User (planned — V1 migration)

Created in this phase as the baseline migration, but fully utilized in Phase 2.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | UUID | PK, default `gen_random_uuid()` | Primary identifier |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | Login identifier |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt hash |
| `first_name` | VARCHAR(100) | NOT NULL | |
| `last_name` | VARCHAR(100) | NOT NULL | |
| `role` | VARCHAR(20) | NOT NULL, default 'MEMBER' | Enum: MEMBER, ADMIN, OFFICER |
| `enabled` | BOOLEAN | NOT NULL, default true | Account active flag |
| `created_at` | TIMESTAMP | NOT NULL, default now() | |
| `updated_at` | TIMESTAMP | NOT NULL, default now() | |

**Indexes**: Unique index on `email`. Index on `role` for role-based queries.

**State transitions**:
- `CREATED` (after registration) → `ACTIVE` (default, enabled=true)
- `ACTIVE` → `DISABLED` (admin sets enabled=false)
- Account deletion is soft (deferred — implemented later if needed)

### flyway_schema_history (managed by Flyway)

Auto-managed table. Not created manually.

| Field | Purpose |
|-------|---------|
| `installed_rank` | Execution order |
| `version` | Migration version string |
| `description` | Migration description |
| `type` | SQL/JAVA |
| `script` | Migration file name |
| `checksum` | Integrity check |
| `installed_by` | DB user who ran it |
| `installed_on` | Timestamp |
| `execution_time` | Milliseconds |
| `success` | Boolean flag |

## Relationships

- No entity relationships defined yet. `User` is standalone in V1.
- Future phases will add: `Event` (V2), RSVP join table (V3), etc.

## Validation Rules

- `email`: Must be valid email format (app-level + DB UNIQUE constraint)
- `password_hash`: BCrypt hash, minimum 60 chars (BCrypt output)
- `role`: Must be one of MEMBER, ADMIN, OFFICER
- `first_name`, `last_name`: 1–100 chars, trimmed
