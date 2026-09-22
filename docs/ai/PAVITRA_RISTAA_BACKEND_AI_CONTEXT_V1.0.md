# Pavitra Ristaa — Backend AI Implementation Context

**Document:** Backend AI Context & Coding Rules  
**Version:** 1.0  
**Project:** Pavitra Ristaa  
**Current Runtime:** Java 21  
**Current Framework:** Spring Boot 4.1.1  
**Build:** Maven  
**Database:** PostgreSQL 18.x  
**Architecture:** Modular Monolith  
**API Style:** REST + WebSocket for chat delivery  
**API Base Path:** `/api/v1`

---

## 1. Purpose of this document

This document is the implementation guide for AI coding agents working on the Pavitra Ristaa backend.

When this document is provided together with:

1. `API_CONTRACT_V1.1.md`
2. `openapi-v1.1.yaml`
3. `final_database_table_schema_v1_1.docx` / Flyway migrations in `db-migration/src/main/resources/db/migration`

those documents should be treated as the project's current source of truth.

`docs/decisions/API_DECISIONS_AND_GAPS_NEXT_VERSION.md` is **not** current implementation scope. It lists deferred / next-version work. Do not use it to block or redesign v1.1.

The AI agent's job is to **implement the approved product and API**, not redesign the product, invent new architecture, or add speculative features.

---

# 2. Product context

Pavitra Ristaa is a relationship platform supporting three relationship intentions:

- Friendship
- Dating / Love
- Marriage

The platform is **not a spiritual-record platform**. Spirituality is one optional dimension of a person's profile.

The product should therefore support spiritually oriented users without forcing spiritual information on users who do not want to provide it.

The platform is intended to support discovery, connection, matching, messaging, safety, subscriptions and related account/profile capabilities.

---

# 3. Core architectural decisions

## 3.1 Architecture

Use a **modular monolith**.

Do NOT introduce microservices for the MVP.

Do NOT introduce Kafka, RabbitMQ, CQRS, event sourcing, a domain-event bus, or similar distributed architecture unless a future, explicit decision is made.

The expected high-level structure is:

```text
Frontend
   |
   +---- REST /api/v1 ------------------+
   |                                     |
   +---- WebSocket (chat only) --------> Spring Boot
                                         |
                                    Application modules
                                         |
                                  Service / domain logic
                                         |
                                    Repository layer
                                         |
                                      PostgreSQL
                                         |
                              Redis / Object Storage
```

## 3.2 API-first

The public API contract is already defined.

Implement the contract instead of designing a new API during implementation.

API paths, HTTP methods, request/response shapes, pagination conventions and error codes should remain aligned with the API contract.

If implementation requires a contract change:

1. Stop before changing the contract silently.
2. Explain why the change is required.
3. Update the API contract and implementation together.
4. Do not create a private/non-documented endpoint just to make implementation easier.

## 3.3 Database-first for persistence

The approved v1.1 schema defines the current persistence model.

Do not create arbitrary tables because an ORM mapping feels convenient.

Do not turn every JSON object into a table.

Do not create one table per API endpoint.

Do not expose database tables directly as API resources.

The API is capability-oriented, not CRUD-over-tables.

---

# 4. Current database principles

- PostgreSQL
- Snake_case column/table names
- BIGINT internal primary keys where defined by the schema
- UUIDs for externally exposed identifiers where the schema defines them
- UTC timestamps / `TIMESTAMPTZ`
- Soft deletion/audit/versioning where specified by the schema
- JSONB only where the approved schema uses it for flexible snapshots/metadata
- Passwords, OTPs and refresh tokens must be stored securely/hashed where appropriate
- Object storage is used for file/media content; the database stores metadata
- `ddl-auto` must not create or update production schema automatically
- Flyway owns the schema; migrations live in the `db-migration` module and are applied by its job, never by the API

Current approved schema contains **52 tables**.

Do not modify that table count or add schema objects unless explicitly approved.

---

# 5. Profile model — important

## 5.1 Main profile

`user_profile` is the main customer profile.

It contains the ordinary profile information such as identity, location, lifestyle, career, education, family, interests, hobbies, relationship intent/context and related profile data defined by the schema.

## 5.2 Spiritual profile

`spiritual_profile` is a **lightweight optional 1:1 extension** of `user_profile`.

Do NOT create a large spiritual domain model for the MVP.

Current spiritual fields are:

- `spiritual_community`
- `spiritual_interests`
- `practices`
- `any_spiritual_profession`
- `dream_spiritual_pilgrimage_destination`

All user-facing spiritual fields are optional/nullable.

A user may have no `spiritual_profile` record at all.

Examples:

```text
User A
no spiritual profile
```

```text
User B
spiritual_community = Isha
spiritual_interests = Meditation, Yoga
practices = Hatha Yoga
any_spiritual_profession = null
dream_spiritual_pilgrimage_destination = Kailash
```

Do NOT introduce these as required MVP fields:

- spiritual path
- spiritual role
- years of practice
- teacher flag
- volunteering flag
- detailed retreat history
- pilgrimage history
- guru history
- certification history

Do not create normalized spiritual tables just because a field could theoretically be multi-valued. Text is acceptable for the current MVP where the contract specifies text.

---

# 6. Relationship modes and compatibility — critical

The MVP relationship modes are:

- `DATING`
- `FRIENDSHIP`
- `MARRIAGE`

A user may select multiple relationship modes according to the approved schema/product rules.

## 6.1 One common compatibility model

There is **one common set of profile and preference attributes** used for compatibility across all three relationship modes.

Do NOT create separate tables or separate parameter models for:

- friendship compatibility
- dating compatibility
- marriage compatibility

The relationship mode describes **intent/context**, not a different compatibility engine.

Partner preferences are represented by the single `/me/preferences` resource.

The previous mode-specific preference model is removed.

## 6.2 Missing optional values

Missing optional information must not automatically be interpreted as a negative compatibility signal.

In particular, absence of spiritual information must not reduce compatibility by itself.

---

# 7. API conventions

## 7.1 Base path

All versioned REST APIs use:

```text
/api/v1
```

Examples:

```text
POST /api/v1/auth/register
GET  /api/v1/me/profile
GET  /api/v1/discovery/profiles
GET  /api/v1/matches
```

Keep API versioning at the URL/path level for MVP.

Do not invent a header-based or query-parameter versioning scheme.

## 7.2 JSON

Use camelCase at the API boundary.

Example:

```json
{
  "spiritualCommunity": "Isha",
  "spiritualInterests": "Meditation, Yoga",
  "practices": "Hatha Yoga",
  "anySpiritualProfession": null,
  "dreamSpiritualPilgrimageDestination": "Kailash"
}
```

Database names remain snake_case.

## 7.3 Authentication

Authenticated APIs use:

```http
Authorization: Bearer <JWT>
```

Authentication endpoints remain publicly accessible as defined in the API contract.

## 7.4 Pagination

Use:

- `page`: 0-based
- `size`: default 20
- `size`: maximum 100

Do not create a different pagination convention for individual modules.

## 7.5 API response envelope

Successful APIs use the approved `ApiResponse` shape.

```json
{
  "success": true,
  "data": {},
  "message": "...",
  "timestamp": "..."
}
```

Error APIs use the approved `ApiErrorResponse` shape.

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": {}
  },
  "timestamp": "..."
}
```

Do not invent per-module response envelopes.

---

# 8. Error handling

Implement one global error handling mechanism.

Use a single `@RestControllerAdvice` / equivalent global exception handler for REST APIs.

Errors must map to the contract's error codes.

Important baseline error codes include:

- `VALIDATION_ERROR`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `RESOURCE_NOT_FOUND`
- `DUPLICATE_RESOURCE`
- `ACCOUNT_NOT_VERIFIED`
- `ACCOUNT_SUSPENDED`
- `ACCOUNT_DELETED`
- `OTP_INVALID`
- `OTP_EXPIRED`
- `TOO_MANY_ATTEMPTS`
- `INVALID_CREDENTIALS`
- `SESSION_EXPIRED`
- `SESSION_REVOKED`
- `PROFILE_NOT_ACTIVE`
- `CANNOT_INTERACT_WITH_SELF`
- `USER_BLOCKED`
- `ALREADY_INTERESTED`
- `INTEREST_NOT_ACTIONABLE`
- `MATCH_NOT_FOUND`
- `CONVERSATION_NOT_FOUND`
- `MESSAGE_NOT_FOUND`
- `MEDIA_NOT_FOUND`
- `PAYMENT_FAILED`
- `SUBSCRIPTION_NOT_ACTIVE`
- `PLAN_NOT_FOUND`
- `REPORT_NOT_FOUND`
- `VERIFICATION_NOT_FOUND`
- `SUPPORT_TICKET_NOT_FOUND`
- `RATE_LIMITED`
- `INTERNAL_ERROR`

Validation errors should return field-level information in `details` when appropriate.

Do not leak stack traces, SQL details, implementation class names or secrets in API responses.

---

# 9. Security foundation

The application currently includes Spring Security, but the default generated Spring Security user/password must not become the real product authentication model.

Build application security around:

- Spring Security
- JWT access tokens
- Refresh tokens
- Password hashing
- Account state checks
- Role/authority checks
- Public vs authenticated endpoints

Swagger/OpenAPI endpoints must remain accessible in local development.

At minimum, local development should permit the Swagger UI, API docs, health endpoint and public authentication endpoints while requiring authentication for protected application APIs.

Do not hard-code production credentials.

Secrets must come from environment variables or a proper secret-management mechanism.

---

# 10. Profiles / configuration

Use Spring profiles.

Recommended configuration layout:

```text
src/main/resources/
  application.yml
  application-local.yml
  application-dev.yml
  application-prod.yml
```

`application.yml` contains shared configuration.

Environment-specific values belong in the appropriate profile/environment.

Database username/password must not be committed to Git.

Current local database:

```text
host: localhost
port: 5432
database: pavitra_ristaa
username: pavitra_ristaa
```

Local credentials should be supplied through environment variables.

Recommended names:

```text
DB_USERNAME
DB_PASSWORD
SPRING_PROFILES_ACTIVE
```

Use:

```text
local
```

for the local development profile.

---

# 11. JPA rules

- Use JPA/Hibernate for persistence.
- Keep entities focused on persistence mapping.
- Do not expose JPA entities directly from REST controllers.
- Use DTOs for API requests/responses.
- Use explicit mapping between entity and DTO.
- Avoid lazy-loading problems by designing service/repository boundaries deliberately.
- Disable Open Session in View for API services:

```yaml
spring:
  jpa:
    open-in-view: false
```

- Do not use `ddl-auto=update`.
- Keep schema ownership with Flyway once migrations are introduced.

---

# 12. Project/package structure

Prefer **feature-first modular organization** rather than one global controller/service/repository package for the entire application.

Recommended structure:

```text
com.pavitraristaa
├── PavitraRistaaApplication
├── config
├── common
│   ├── api
│   ├── exception
│   ├── security
│   ├── validation
│   └── util
│
├── auth
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   ├── service
│   └── mapper
│
├── profile
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   ├── service
│   └── mapper
│
├── discovery
├── matching
├── messaging
├── notification
├── media
├── trust
├── subscription
├── support
└── admin
```

A module can have fewer subpackages if it is small. Do not create empty layers just for visual symmetry.

Keep module boundaries clear, but do not turn the modular monolith into artificial microservices.

---

## 12.1 Maven modules and deployables

The repository is a Maven multi-module build. It is still a modular monolith: features are packages inside one
module, not separate services.

```text
pavitra-ristaa (parent pom)
├── shared          library      ApiResponse/ApiErrorResponse, ApiException/ErrorCode, GlobalExceptionHandler,
│                                AuthenticatedUser/CurrentUserAccessor, utilities. Must not depend on any feature.
├── db-migration    DEPLOYABLE   Flyway SQL + a small runner. The only thing that changes the schema.
└── api             DEPLOYABLE   The REST API. Features (auth, master, media, profile, relationship, preference,
                                 and later discovery, matching, ...) are packages under com.pavitraristaa.
```

Rules:

- Run `db-migration` before starting a new `api` version. The API has no Flyway and no permission to change the schema.
- New migrations go in `db-migration/src/main/resources/db/migration`. Integration tests apply them through a
  test-scope dependency on `db-migration`.
- Code that has no feature dependency and is needed by several features belongs in `shared`; keep it small.
- Features may depend on each other only as declared in `ALLOWED_DEPENDENCIES` in
  `api/src/test/java/com/pavitraristaa/architecture/ModuleBoundariesTest.java`. The build fails on any other
  dependency, on cycles between features, on features using `config` beyond `PavitraProperties`, and on controllers
  using JPA entities. Adding a dependency is allowed, but change that map deliberately in the same commit.
- When a feature must know about another feature's data without depending on it (for example media needing to know
  whether a profile photo still uses a file), define a small interface in the feature that needs the answer and
  implement it in the other feature (see `MediaUsageChecker`).
- Split a feature into its own Maven module only when its boundary is stable and there is a reason to.
- A third deployable (for example chat/WebSocket or background workers) is not created until it has something to run.

# 13. Swagger / OpenAPI

Use SpringDoc OpenAPI compatible with the project's Spring Boot version.

Current project uses Spring Boot 4.1.1, so keep the configured SpringDoc version compatible with Spring Boot 4.

Swagger should expose:

```text
/swagger-ui.html
/v3/api-docs
```

The public API contract is versioned separately in the project docs.

Swagger documentation should include:

- API title
- API version
- tags by capability/module
- Bearer JWT security scheme
- endpoint summaries
- request/response schemas
- common error responses

Do not document internal repositories/entities as public endpoints.

---

# 14. Authentication implementation order

Authentication is the first real business module.

Implement in small vertical slices, approximately:

```text
User/account persistence
        ↓
Registration
        ↓
OTP verification
        ↓
Password login
        ↓
JWT access token
        ↓
Refresh token
        ↓
Logout / token revocation
        ↓
Account state checks
        ↓
Session/login history
```

Google/Apple login and other authentication capabilities should follow the approved API contract and v1.1 schema.

Do not invent extra tables for next-version capabilities listed in `API_DECISIONS_AND_GAPS_NEXT_VERSION.md`.

---

# 15. Profile implementation order

After authentication:

```text
user_profile
    ↓
education
career
family
lifestyle
languages
interests
hobbies
photos
    ↓
spiritual_profile
    ↓
relationship modes
    ↓
partner preferences
```

Profile endpoints must respect privacy and account-state rules from the product/API contract.

---

# 16. Discovery and matching

Discovery should operate on profile attributes and approved filters.

Matching/compatibility uses the same underlying parameters for:

- Friendship
- Dating/Love
- Marriage

Do not build three independent engines.

Recommendation logic is application logic.

Recommendation snapshots may be persisted according to the approved schema.

Do not introduce a machine-learning service unless explicitly requested.

---

# 17. Messaging

Messaging is the only MVP capability where WebSocket is required for live delivery.

REST is still used for:

- conversation history
- listing conversations
- message history
- management operations
- media/history-related operations

Current proposed transport endpoint:

```text
/ws/chat
```

JWT is used when establishing the WebSocket connection.

Exact STOMP destinations/frames are not frozen by the current contract and must be finalized before chat implementation.

Do not invent a production messaging protocol prematurely.

---

# 18. Media

Use object storage for actual media content.

The DB stores media metadata and relationships.

Current local/dev direction:

- S3-compatible storage in deployed environments
- MinIO can be used locally when needed

Preferred flow:

```text
Client
  ↓
request upload URL
  ↓
Backend returns presigned URL
  ↓
Client uploads directly to object storage
  ↓
Client calls upload completion API
  ↓
Backend persists/activates media metadata
```

Do not stream every media file through the Spring Boot server unless the API contract explicitly requires it.

---

# 19. Deferred / unresolved areas

The following are present in the broader product documentation but are not fully represented by the approved v1.1 MVP schema or contract decisions.

Do not invent database models for them without an explicit decision.

Examples:

- User settings/privacy/appearance/communication persistence
- Data consent/export/retention persistence
- Google/Apple linked-account persistence
- Government ID/selfie verification evidence storage
- Payment-provider-specific checkout/callback/webhook behavior
- Exact WebSocket/STOMP destinations
- Community domain persistence: events, groups, discussions, retreats, volunteer activities, directory, resources
- Product analytics event/fact storage
- Referral/growth experiment persistence

When an API exists but persistence is unresolved, flag the issue instead of inventing schema.

---

# 20. What the AI agent must NOT do

The agent must not:

- redesign the architecture
- introduce microservices
- introduce Kafka/RabbitMQ/CQRS without approval
- create extra tables without approval
- create a separate compatibility system for each relationship mode
- make optional spiritual fields mandatory
- turn the spiritual profile into a complex spiritual-record system
- expose JPA entities directly as REST DTOs
- create CRUD endpoints for every table
- change API paths casually
- hard-code secrets
- use `ddl-auto=update`
- add speculative libraries without explaining why they are needed
- create huge classes that combine controller, service, persistence and security logic
- write production-insecure shortcuts merely to make tests pass
- delete existing working code without checking dependencies
- replace existing behavior without explaining the impact

---

# 21. How AI should implement a task

Every coding task should follow this sequence:

### Step 1 — Read the source of truth

Before changing code, inspect:

- this context document
- the relevant section of the API contract
- the relevant schema table definitions
- the relevant API decisions/gaps

### Step 2 — Identify affected files

State which files will be created/changed and why.

### Step 3 — Check existing code

Do not recreate classes that already exist.

Do not duplicate configuration or utilities.

### Step 4 — Implement the smallest complete slice

Example:

```text
Controller
↓
Request DTO
↓
Service
↓
Repository
↓
Entity mapping
↓
Response DTO
↓
Exception handling
↓
Test
```

Only add layers that the feature actually needs.

### Step 5 — Validate

Run:

```text
compile
unit tests
relevant integration tests
```

For DB-related work, use Testcontainers/integration testing where appropriate.

### Step 6 — Report

At the end of every task, report:

- files created
- files changed
- behavior implemented
- tests run
- any unresolved issue
- any contract/schema decision still required

---

# 22. Contract-change protocol

If the AI believes the current API contract is wrong or incomplete:

```text
DO NOT silently modify the API.
```

Instead provide:

```text
Proposed change
Why it is needed
Affected endpoint(s)
Affected DTO(s)
Affected DB model(s)
Frontend impact
Backward-compatibility impact
```

Then wait for approval before changing the public contract.

---

# 23. Database-change protocol

If the AI believes the schema needs a change:

```text
DO NOT silently create the new table/column.
```

Instead report:

```text
Proposed schema change
Reason
Affected table(s)
Migration impact
API impact
Why the existing schema is insufficient
```

Schema changes must be approved before being treated as the new source of truth.

---

# 24. Testing standards

At minimum, write tests for:

- service business rules
- validation failures
- authentication/security rules
- repository behavior where queries are non-trivial
- controller/API contract behavior for important endpoints
- important state transitions

Use:

- JUnit 5
- Spring Boot test support
- Mockito only where useful
- Testcontainers for integration tests involving PostgreSQL

Do not test implementation details unnecessarily.

---

# 25. Code quality standards

Prefer:

- clear names
- small methods
- immutable DTOs where practical
- constructor injection
- explicit validation
- transactional boundaries at service level
- clear repository queries
- domain/business rules in services
- centralized exception handling

Avoid:

- field injection
- massive service classes
- deeply nested conditionals
- generic `Object` everywhere
- swallowing exceptions
- logging passwords/tokens/OTP values
- unnecessary abstraction layers
- premature design patterns

Use Lombok only where it improves readability.

---

# 26. Logging and sensitive data

Never log:

- passwords
- raw OTPs
- JWTs
- refresh tokens
- payment secrets
- identity-verification documents
- other sensitive personal data unnecessarily

Use structured, useful application logs.

Production logging must be safe by default.

---

# 27. Git workflow

The project is developed in one repository.

Recommended branches:

```text
main
feature/<feature-name>
fix/<issue-name>
```

Work should normally be committed to a feature branch, reviewed, and merged through the normal repository workflow.

AI should not merge branches automatically.

AI should not push to `main` automatically.

---

# 28. Development order

Use this implementation order unless the human developer explicitly changes it:

```text
FOUNDATION
  ↓
AUTHENTICATION & ACCOUNTS
  ↓
USER PROFILE
  ↓
SPIRITUAL PROFILE
  ↓
RELATIONSHIP MODES
  ↓
PARTNER PREFERENCES
  ↓
DISCOVERY
  ↓
INTERESTS / MATCHES / COMPATIBILITY
  ↓
FAVORITES / SHORTLIST
  ↓
MESSAGING
  ↓
MEDIA
  ↓
NOTIFICATIONS
  ↓
TRUST & SAFETY
  ↓
SUBSCRIPTIONS / PAYMENTS
  ↓
SUPPORT / ADMIN
```

Community and Analytics/Growth remain next-version work. Do not implement them in v1.1.

---

# 29. Immediate foundation setup checklist

Before implementing authentication, establish:

- Spring profiles: local/dev/prod
- environment-based secrets
- `spring.jpa.open-in-view=false`
- API base path `/api/v1`
- global API success/error model
- global exception handler
- validation error mapping
- Spring Security configuration
- public Swagger/OpenAPI access
- public auth endpoint access
- protected application endpoint policy
- JWT security foundation (implementation completed with auth module)
- Swagger metadata and Bearer auth scheme
- module/package structure
- basic actuator health endpoint

Do not add business entities as part of this foundation task.

---

# 30. Recommended AI operating prompt

When starting a coding task, the human developer can give the AI this instruction:

```text
You are implementing the Pavitra Ristaa backend.

Read these files before coding:
1. PAVITRA_RISTAA_BACKEND_AI_CONTEXT_V1.0.md
2. API_CONTRACT_V1.1.md
3. openapi-v1.1.yaml
4. final_database_table_schema_v1_1.docx / Flyway migrations

These files are the current source of truth.
Ignore `API_DECISIONS_AND_GAPS_NEXT_VERSION.md` except as a list of deferred next-version items.

Implement only the requested task.
Do not redesign the architecture.
Do not introduce microservices or messaging/event infrastructure.
Do not invent tables, endpoints, fields, response shapes or compatibility models.
Preserve `/api/v1` and the approved API contract.
Use the approved database schema for persistence.
Keep spiritual profile lightweight and optional.
Use one shared compatibility parameter model for Friendship, Dating and Marriage.

Before coding:
- inspect existing project structure
- identify affected files
- explain the implementation plan briefly

Then implement the smallest complete production-quality slice.
Include tests.
Run compile/tests if possible.
Do not silently change API or DB contracts.
If a contract/schema change is necessary, stop and propose it instead.

At the end report:
- created files
- changed files
- implemented behavior
- tests run
- unresolved issues
```

---

# 31. Final principle

### Simplicity first.

The MVP should be a clean, maintainable backend that satisfies the approved product and API contract.

When deciding between two technically valid solutions, prefer the solution with:

- fewer moving parts
- fewer tables
- fewer abstractions
- clearer ownership
- easier testing
- easier frontend integration
- easier future modification

Do not build future complexity before the product actually requires it.

---

## Source documents used with this context

- Product Knowledge Base
- `API_CONTRACT_V1.1.md`
- `openapi-v1.1.yaml`
- `final_database_table_schema_v1_1.docx`
- `API_DECISIONS_AND_GAPS_NEXT_VERSION.md` (next version only; not current implementation scope)

**End of document.**
