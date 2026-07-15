# Contracts: RSVP Endpoint

**Date**: 2026-07-15

This directory contains the API contract documentation for the RSVP feature.

## Files

| File | Description |
|------|-------------|
| [api.md](./api.md) | HTTP endpoint contracts (request/response shapes, status codes) |

## Design Principles

- Contracts follow existing `lions-club-api` patterns (cookie-based JWT auth, JSON request/response, SpringDoc annotations)
- RSVP is nested under `/api/events/:id/rsvp` to leverage existing event resource path
- Admin-only list endpoint at `/api/events/:id/rsvps` (plural) for distinction
