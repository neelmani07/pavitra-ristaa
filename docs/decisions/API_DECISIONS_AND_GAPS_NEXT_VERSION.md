# API DECISIONS AND GAPS — NEXT VERSION

**Status:** Out of scope for the current v1.1 MVP implementation.

This file is **not** a source of truth for coding the current product. It records deferred capabilities and later-version work. Implement the current backend from:

1. `PAVITRA_RISTAA_BACKEND_AI_CONTEXT_V1.0.md`
2. `API_CONTRACT_V1.1.md`
3. `openapi-v1.1.yaml`
4. the approved v1.1 database schema / Flyway migrations

Do not block v1.1 implementation on items listed below. Do not invent extra tables for this list until a next-version schema is approved.

## Confirmed for current MVP (v1.1)

These remain true for the current product and are already reflected in the v1.1 contract and schema:

- Modular monolith; Spring Boot; PostgreSQL.
- REST primary API; WebSocket for chat delivery.
- Product-facing APIs, not table CRUD.
- MVP relationship modes: Dating, Friendship, Marriage.
- One shared partner-preference and compatibility parameter set is used across all relationship modes; relationship mode provides intent/context only.
- Spiritual profile is a lightweight optional 1:1 extension of `user_profile`.
- Public UUIDs where the schema defines them; internal BIGINT identifiers remain internal.

## Deferred to next version

1. Dedicated user settings / privacy / appearance / communication tables.
2. Data export / consent / retention persistence and job tracking.
3. Community features.
4. Analytics & Growth event/fact tables and APIs.
5. Dedicated Google/Apple linked-account persistence.
6. Government ID / selfie verification evidence storage and access rules (beyond `profile_verification` metadata).
7. Payment provider checkout / callback / webhook contracts (finalize when a provider is selected).
8. WebSocket / STOMP destination names (finalize before chat implementation).
9. Community and analytics APIs from the broader Product Knowledge Base.

## Already resolved in v1.1 (do not reopen)

- `partner_preference_mode` is removed from the schema and API contract.
- `spiritual_profile` is one optional 1:1 table with only lightweight fields: `spiritual_community`, `spiritual_interests`, `practices`, `any_spiritual_profession`, and `dream_spiritual_pilgrimage_destination`.
- All spiritual user-entered fields are nullable. Missing spiritual information is valid and should not be treated as a negative compatibility signal.
- No separate compatibility models are introduced for Dating, Friendship or Marriage.
- `/me/preferences` is the single partner-preference resource.
