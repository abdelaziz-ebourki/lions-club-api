# Research: Project Foundation

## Docker Compose for PostgreSQL

- **Decision**: Use `postgres:15-alpine` image with named volume for persistence
- **Rationale**: Alpine variant is lightweight; named volume survives container restarts
- **Config**: Port 5432, user `lions_club`, database `lions_club`, password via `.env`
- **Alternatives**: Postgres 16 (not needed; 15 is stable), Bitnami image (heavier)
- **Health check**: Use `pg_isready` to confirm DB readiness before app starts

## Flyway Migration Pipeline

- **Decision**: Flyway with `baseline-on-migrate: true`, migrations in
  `src/main/resources/db/migration/`, `ddl-auto: validate`
- **Rationale**: `validate` ensures schema matches entities without Hibernate
  auto-DDL. Baseline allows existing databases to be adopted.
- **Alternatives**: Liquibase (more verbose XML), Hibernate auto-DDL (unsafe
  for production), manual SQL files (unrepeatable)
- **Naming**: `V{number}__{description}.sql` — descriptive snake_case names

## SpringDoc OpenAPI Configuration

- **Decision**: SpringDoc 2.8.6 with custom OpenAPI bean, JWT bearer security
  scheme, gated to `dev`/`test` profiles
- **Rationale**: Aligns with constitution's Security by Design principle.
  Profile gating prevents docs exposure in production.
- **Alternatives**: SpringFox (deprecated), manual REST Docs (more work)
- **Key config**: Title "Lions Club FSBM API", version from `pom.xml`,
  description matching project, bearer auth scheme

## Environment Profiles

- **Decision**: `dev` (default) + `prod` profiles with `application-{profile}.yml`
- **Rationale**: Separates concerns; dev gets relaxed settings, prod gets
  strict security configs. JWT secret comes from env var in prod.
- **Alternatives**: Single file with conditional properties (messy)

## V1 Migration — Users Table Schema

- **Decision**: Create `users` table with fields needed for JWT auth (Phase 2)
- **Fields**: `id` (UUID PK), `email` (unique, not null), `password_hash`
  (not null), `first_name`, `last_name`, `role` (enum: MEMBER, ADMIN, OFFICER),
  `enabled` (boolean), `created_at`, `updated_at`
- **Rationale**: UUID avoids sequential ID enumeration; email is natural login
  identifier; role enum enables authorization in Phase 2+
- **Alternatives**: Auto-increment IDs (predictable), single `name` field
  (less flexible), soft-delete via `deleted_at` (deferred for now)
