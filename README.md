# Lions Club FSBM REST API

REST API for Lions Club FSBM, built with Spring Boot 3 + Java 21.

## Prerequisites

- Java 21
- Docker & Docker Compose
- Maven (via `./mvnw`)

## Setup

```bash
# 1. Enable git hooks
git config core.hooksPath .githooks

# 2. Start the database
cp .env.example .env
docker compose up -d

# 3. Start the application
./mvnw spring-boot:run
```

The pre-commit hook runs `./mvnw test -q` before each commit. The pre-push hook runs
`./mvnw verify` before each push. Bypass with `--no-verify` if needed.

The API will be available at `http://localhost:8080`.

## API Documentation

Once running, visit `http://localhost:8080/swagger-ui.html` (dev profile only).

## Profiles

- **dev** (default): Swagger UI enabled, debug logging, SQL logging
- **prod**: Swagger UI disabled, minimal logging, env-based secrets

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
