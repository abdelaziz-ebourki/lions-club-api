# Data Model: Seed Data / Dev Data Setup

## Overview

Seed data instances for the existing `User` and `Event` entities. No new entities or schema changes.

## Users

| Field | Admin | Member 1 | Member 2 |
|-------|-------|----------|----------|
| id (deterministic UUID) | `uuid("admin-1")` | `uuid("user-1")` | `uuid("user-2")` |
| firstName | Ahmed | Fatima Zahra | Youssef |
| lastName | Benali | El Amrani | Idrissi |
| email | admin@lionsclub.com | fatima@lionsclub.com | youssef@lionsclub.com |
| password (plain) | admin123 | member123 | member123 |
| role | ADMIN | MEMBER | MEMBER |
| enabled | true | true | true |

UUID derivation: `UUID.nameUUIDFromBytes(STRING_ID.getBytes())` produces a deterministic, reproducible UUID for each string ID.

## Events

### Upcoming Events (status → `PUBLISHED`)

| Field | Annual Charity Gala 2026 | Community Clean-Up Day | Health & Wellness Workshop |
|-------|-------------------------|----------------------|---------------------------|
| title | Annual Charity Gala 2026 | Community Clean-Up Day | Health & Wellness Workshop |
| description | Join us for an elegant evening of dinner, auctions, and entertainment to raise funds for local education initiatives. | A day dedicated to cleaning and beautifying our local parks and public spaces. | Free workshop covering basic health screenings, nutrition advice, and mental wellness resources. |
| startDateTime | 2026-09-15T19:00 | 2026-08-22T08:00 | 2026-07-10T10:00 |
| endDateTime | 2026-09-15T21:00 | 2026-08-22T10:00 | 2026-07-10T12:00 |
| location | Hyatt Regency Casablanca | Parc de la Ligue Arabe, Casablanca | Centre Culturel d'Anfa |
| address | null | null | null |
| maxAttendees | null | null | null |
| category | FUNDRAISER | COMMUNITY | HEALTH |
| status | PUBLISHED | PUBLISHED | PUBLISHED |
| createdBy | Admin user | Admin user | Admin user |

### Past Events (status → `COMPLETED`)

| Field | Sight Screening Camp | Youth Leadership Summit |
|-------|---------------------|----------------------|
| title | Sight Screening Camp | Youth Leadership Summit |
| description | A club-organized vision screening camp providing free eye checkups and glasses to those in need. | A two-day summit empowering young leaders with skills in public speaking and project management. |
| startDateTime | 2026-05-20T09:00 | 2026-04-05T09:00 |
| endDateTime | 2026-05-20T11:00 | 2026-04-05T11:00 |
| location | Sidi Moumen Community Center | Université Hassan II |
| address | null | null |
| maxAttendees | null | null |
| category | HEALTH | YOUTH |
| status | COMPLETED | COMPLETED |
| createdBy | Admin user | Admin user |
