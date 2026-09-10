# Lions Club FSBM REST API

REST API for Lions Club FSBM, built with Spring Boot 3 + Java 21.

## Prerequisites

- Java 21
- Docker & Docker Compose
- Maven (via `./mvnw`)

## Quickstart (one command)

```bash
# 1. Enable git hooks
git config core.hooksPath .githooks

# 2. Start the whole backend (database + API)
cp .env.example .env
docker compose up --build -d
```

The API will be available at `http://localhost:8081` (health: `/actuator/health`).
The production database starts empty (dev seeds only run on the `dev` profile);
create your first account via `POST /api/auth/register`.

## Host dev variant (API on host, DB in Docker)

```bash
docker compose up -d db
SERVER_PORT=8081 ./mvnw spring-boot:run
```

`SERVER_PORT=8081` keeps the host run on the same URL as Docker
(`server.port` stays `8080` for containers). Swagger UI is dev-profile only:
`http://localhost:8081/swagger-ui.html`.

## UI wiring

The frontend lives in `lions-club-ui` and stays outside this compose file.
Point host UI dev at the API with:

```bash
VITE_API_URL=http://localhost:8081/api npm run dev
```

(or copy `lions-club-ui/.env.example` to `.env`). Browser origins `:5173`
and `:5174` are allowed by default; override with `CORS_ALLOWED_ORIGINS`.

## Ports & env

| Variable   | Default | Purpose                              |
| ---------- | ------- | ------------------------------------ |
| `API_PORT` | `8081`  | Host port for the API (`:8080` inside)|
| `PG_PORT`  | `5433`  | Host port for postgres (`:5432` inside)|
| `DB_URL`   | `…localhost:5433…` | JDBC URL for host runs |
| `CORS_ALLOWED_ORIGINS` | `:5173,:5174` | Browser origins |
| `JWT_SECRET` | dev default | Override in production |
| `APP_JWT_SECURE` | `true` | Set `false` for plain-http deploys |

Defaults avoid clashes with common local services on `8080`/`5432`/`5173`.
Run either this stack **or** the `lions-club-e2e` full-stack harness, not both
(they bind the same host ports).

## Git hooks

The pre-commit hook runs `./mvnw test -q` before each commit. The pre-push hook runs
`./mvnw verify` before each push. Both need a reachable database
(`docker compose up -d db` first). Bypass with `--no-verify` if needed.

## Profiles

- **dev** (default): Swagger UI enabled, debug logging, SQL logging
- **prod**: Swagger UI disabled, minimal logging, env-based secrets (used by Docker)

Set with `SPRING_PROFILES_ACTIVE=prod`.

## Project Structure

```
src/
├── main/java/com/lionsclub/api/
│   ├── config/          # OpenAPI, security, app config
│   ├── domain/          # Domain entities by sub-package
│   └── LionsClubApiApplication.java
└── main/resources/
    ├── application.yml
    ├── application-{profile}.yml
    └── db/migration/    # Flyway migrations
```

## Tech Stack

- Spring Boot 3.4.4
- Java 21
- PostgreSQL 15
- Flyway
- SpringDoc OpenAPI
- JWT (Auth0)
