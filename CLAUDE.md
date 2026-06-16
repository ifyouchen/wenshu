# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**文枢 wenshu** — AI-powered long-form writing workbench (Spring Boot 3 / Java 21 backend). Core planned features: controllable long-form continuation, tiered polishing, context anchors, consistency checking, novel-to-script conversion, full-text search, and writing statistics.

## Commands

### Run locally
```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Run all tests (no external services required)
```bash
mvn test
```

### Run a single test class
```bash
mvn test -Dtest=AuthControllerTests
```

### Build without tests
```bash
mvn package -DskipTests
```

### Endpoints after startup
- API health: `http://localhost:8080/api/v1/system/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Actuator: `http://localhost:8080/actuator/health`

## Architecture

The codebase strictly follows **DDD four-layer packaging** under `com.czx.wenshu`:

| Package | Role |
|---|---|
| `domain` | Pure Java — entities, value objects, repository interfaces. No Spring annotations. |
| `application` | Use-case services, Commands/Results, transaction boundaries (`@Transactional`). |
| `infrastructure` | Spring beans: MyBatis mappers/repositories, mail senders, config, storage, LLM, and mail adapters. |
| `interfaces.rest` | Controllers, request/response DTOs, `AuthInterceptor`, `GlobalExceptionHandler`. |

**Dependency direction**: `interfaces` → `application` → `domain` ← `infrastructure`

### Key architectural patterns

**Domain entities** use a private constructor + static factory methods:
- `register(...)` / `create(...)` for new instances
- `rehydrate(...)` for reconstructing from persistence
- All state mutations go through named domain methods (e.g. `user.markDeleted(clock)`, `chapter.saveContent(...)`)

**Repository pattern**: Domain defines the interface (e.g. `UserRepository`); infrastructure provides the implementation (`MyBatisUserRepository`). Repositories do upsert via mapper `findById` check → `insert` or `update`.

**API response envelope**: All endpoints return `Result<T>` with `{ code, message, data, timestamp }`. Business errors throw `ApiException(ErrorCode, message)` and are mapped to HTTP status codes by `GlobalExceptionHandler`.

**Authentication**: Opaque token scheme — access tokens prefixed `wat_`, refresh tokens prefixed `wrt_`, both stored as SHA-256 hashes. `AuthInterceptor` validates the `Authorization: Bearer <token>` header and injects `currentUser` into the request attribute. `CurrentUserProvider` extracts it in controllers.

**Mail sending**: Three application-layer interfaces (`VerificationEmailSender`, `PasswordResetEmailSender`, `SecurityAlertEmailSender`) are implemented by delegating wrappers in infrastructure that switch between SMTP and a no-op logging implementation based on `wenshu.mail.enabled`.

**Writing stats**: `WritingStatsService.recordManualDelta(userId, projectId, delta)` is called by `ProjectApplicationService` after any content change (save or snapshot restore) to accumulate daily character counts.

### Test profile

Tests use `@ActiveProfiles("test")` + `application-test.yaml`: H2 in-memory DB (PostgreSQL compatibility mode), Flyway disabled, Redis disabled. Integration tests are `@SpringBootTest(webEnvironment = RANDOM_PORT)` and reset schema via `@Sql`. Email senders are replaced with capturing test doubles via `@TestConfiguration` + `@Primary`.

### Configuration

All tunables live under the `wenshu.*` namespace in `application.yaml`, bound by `WenshuProperties`. Key env vars:

| Variable | Purpose |
|---|---|
| `WENSHU_DATASOURCE_URL/USERNAME/PASSWORD` | PostgreSQL connection |
| `WENSHU_REDIS_HOST/PORT/PASSWORD` | Redis connection |
| `WENSHU_MAIL_ENABLED` | Toggle SMTP (`false` → log-only) |
| `ANTHROPIC_API_KEY` / `DEEPSEEK_API_KEY` | LLM keys (creative model: claude-sonnet-4-6; utility: deepseek-chat) |
| `TENCENT_COS_REGION/BUCKET/SECRET_ID/SECRET_KEY` | COS object storage |

### Database

Flyway is explicitly disabled and excluded from application auto-configuration. Local schema initialization uses `src/main/resources/db/schema.sql`; the default local PostgreSQL connection is `jdbc:postgresql://localhost:5432/wenshu` with `wenshu/wenshu`, override it through `WENSHU_DATASOURCE_URL`, `WENSHU_DATASOURCE_USERNAME`, and `WENSHU_DATASOURCE_PASSWORD`. Uses `pgvector/pgvector:pg16` image for vector support. MyBatis mappers have `map-underscore-to-camel-case: true`; no XML mapper files are used — all queries are annotation-based.

### Project domain model hierarchy

`Project` → `Volume` → `Chapter` → `ChapterSnapshot`  
`Project` also has `Character` and `WorldElement` as sibling aggregates (managed by `CharacterApplicationService` and `WorldElementApplicationService`).

Ownership is verified by checking `projectRepository.existsByIdAndUserId(projectId, userId)` before mutating volumes, chapters, characters, or world elements.
