# Contracts: Seed Data / Dev Data Setup

No new API endpoints or interfaces are introduced by this feature. The seed data populates existing entities consumed by the existing REST API:

- **Auth API** (`POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/me`, `POST /api/auth/refresh`) — seeded users enable immediate login testing
- **Events API** (`GET /api/events`, `GET /api/events/{id}`, `POST /api/events`, `PUT /api/events/{id}`, `DELETE /api/events/{id}`) — seeded events provide realistic data for listing and detail views

See the existing OpenAPI spec at `/swagger-ui.html` (dev profile) for full contract details.
