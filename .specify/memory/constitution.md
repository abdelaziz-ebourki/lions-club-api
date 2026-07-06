<!--
  Sync Impact Report — Constitution v1.0.0
  Version change: 0.0.0 (template) → 1.0.0
  Modified principles: (none — initial adoption, all 5 principles new)
  Added sections:
    - I. API-First & Contract-Driven
    - II. Security by Design
    - III. Test-First Discipline (NON-NEGOTIABLE)
    - IV. Database Migration Rigor
    - V. Clean Architecture & Layering
    - Section 2: Technology & Quality Standards
    - Section 3: Development Workflow
    - Governance
  Removed sections: (none)
  Templates requiring updates:
    - .specify/templates/plan-template.md      ⚠ pending (Constitution Check gates)
  Follow-up TODOs: none
-->

# Lions Club FSBM REST API Constitution

## Core Principles

### I. API-First & Contract-Driven

API contracts (OpenAPI specs) MUST be designed and reviewed before
implementation begins. The spec is the source of truth for all
client-server interaction. Changes to the API MUST first be reflected
in the OpenAPI spec, then implemented.

### II. Security by Design

All protected endpoints MUST require JWT authentication. Role-based
authorization MUST be enforced at the controller/endpoint level. Every
request payload MUST be validated (Jakarta Validation annotations)
before reaching service logic. Security is not an afterthought — it is
designed into every layer.

### III. Test-First Discipline (NON-NEGOTIABLE)

Tests MUST be written before implementation code (Red-Green-Refactor).
No production code is written without a failing test first. Tests must
be approved by the user before implementation begins. This applies to
all layers: unit, integration, and contract tests.

### IV. Database Migration Rigor

All schema changes MUST go through Flyway migrations. Direct database
modifications (manual SQL, DDL, or DML) are forbidden. Every migration
MUST be reversible (provide rollback strategy). Migration files are
immutable once committed.

### V. Clean Architecture & Layering

The codebase MUST follow a strict layered architecture:
Controller (HTTP) → Service (business logic) → Repository (data access).
DTOs MUST be used at API boundaries; entities MUST NOT leak into
controllers. Dependencies flow inward — inner layers know nothing
about outer layers.

## Technology & Quality Standards

**Stack**: Spring Boot 3.4.4 / Java 21 / PostgreSQL / Flyway / Lombok /
SpringDoc OpenAPI / Auth0 java-jwt.

**Quality Gates**:
- All code MUST compile without warnings.
- Tests MUST pass before merge.
- API documentation (SpringDoc) MUST be kept in sync with implementation.
- Lombok MAY be used for boilerplate reduction; do not overuse.

**API Documentation**: SpringDoc OpenAPI at `/swagger-ui.html` is the
canonical documentation endpoint. All endpoints, schemas, and security
schemes MUST be documented.

## Development Workflow

**Branching**: One branch per issue (`###-feature-name`).
**Code Review**: Every PR MUST be reviewed before merge.
**Commits**: Conventional commits format (`type: description`).
**Merge**: Squash merge into main; feature branches deleted after merge.

## Governance

This constitution supersedes all other development practices.
Amendments require:
1. Documented rationale (PR description or issue).
2. Version bump per semantic versioning rules.
3. Approval before taking effect.

All PRs and code reviews MUST verify compliance with this constitution.
Violations MUST be flagged and justified with a documented exception.

**Version**: 1.0.0 | **Ratified**: 2026-07-06 | **Last Amended**: 2026-07-06
