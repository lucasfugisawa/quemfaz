# server

Ktor REST API backend. JVM only. Depends on `shared` for API contracts.

---

## Stack

| Layer | Technology |
|---|---|
| HTTP framework | Ktor Server 3.4.1 (Netty engine) |
| Database | PostgreSQL 16 |
| ORM | Exposed 0.60.0 |
| Migrations | Flyway 12.1.0 |
| Connection pooling | HikariCP |
| Dependency injection | Koin 4.1.1 |
| Authentication | JWT (Auth0 Java) |
| Logging | Logback |
| Testing | JUnit 5 + Testcontainers + Mockito Kotlin |

---

## Source layout

```
src/main/kotlin/com/fugisawa/quemfaz/
├── Application.kt              Entry point — Ktor app setup, plugin installation
├── auth/                       Auth routes, services, repositories
├── profile/                    Professional profile routes, services, repositories
├── search/                     Search routes, services, ranking, interpreter
├── engagement/                 Contact click + profile view tracking
├── favorites/                  Favorites routes, services, repositories
├── moderation/                 Reporting routes, services, repositories
├── config/                     Koin module definitions, config loading
├── infrastructure/             DatabaseFactory, SMS sender, OTP, caching
└── environment/                AppEnvironment enum

src/main/resources/
├── application.conf            HOCON config with environment variable overrides
├── db/migration/               Flyway SQL migration files (V1–V6)
└── logback.xml                 Logging config
```

---

## API Routes

All routes are versioned under `/api/v1/` (set via `ApiVersion`).

### Auth — `/auth/`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/start-otp` | No | Send OTP to phone number |
| POST | `/auth/verify-otp` | No | Verify OTP, receive JWT |
| POST | `/auth/profile` | No | Create/update user profile |
| GET | `/auth/me` | Yes | Get current user profile |

### Professional Profile — `/professional-profile/`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/professional-profile/draft` | Yes | Interpret raw input, return draft |
| POST | `/professional-profile` | Yes | Confirm and create profile |
| GET | `/professional-profile/{id}` | No | Get public profile by ID |
| GET | `/professional-profile/me` | Yes | Get own professional profile |
| PUT | `/professional-profile/me` | Yes | Update own professional profile |

### Search — `/search/`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/search` | No | Search professionals by query + city |

### Engagement — `/engagement/`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/engagement/contact-click` | No | Track contact button click |
| POST | `/engagement/profile-view` | No | Track profile view |

### Favorites — `/favorites/`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/favorites/{profileId}` | Yes | Add favorite |
| DELETE | `/favorites/{profileId}` | Yes | Remove favorite |
| GET | `/favorites` | Yes | List user's favorites |

### Moderation — `/moderation/`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/moderation/report` | Yes | Report a professional profile |

---

## Layered architecture

```
Route handler  (auth/routes, profile/routes, ...)
      ↓
Application service  (StartOtpService, SearchProfessionalsService, ...)
      ↓
Repository interface  (UserRepository, ProfessionalProfileRepository, ...)
      ↓
Exposed ORM implementation  (ExposedUserRepository, ...)
      ↓
PostgreSQL
```

- **Route handlers** parse HTTP, call one service, return HTTP response. No business logic.
- **Services** are stateless. They receive all dependencies via Koin DI constructor injection.
- **Repositories** are defined as interfaces; `Exposed*` implementations live in `infrastructure/`.
- **Interpreters** (`ProfessionalInputInterpreter`, `SearchQueryInterpreter`) are interfaces. The current implementations are rule-based mocks (MVP).

---

## Database schema

Schema is managed exclusively via Flyway migrations. Never alter the schema without a migration file.

| Migration | Contents |
|---|---|
| V1 | Initial setup |
| V2 | Users, OTP challenges, auth identities |
| V3 | Professional profiles, services, neighborhoods |
| V4 | Search query event tracking |
| V5 | Favorites, contact clicks, profile reports |
| V6 | Profile view events |

Migration files: `src/main/resources/db/migration/`

---

## Configuration

Config is loaded from `application.conf` (HOCON) with environment variable overrides.

| Variable | Default | Description |
|---|---|---|
| `APP_ENV` | `local` | Environment name (local/dev/sandbox/production) |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `quemfaz` | Database name |
| `DB_USER` | `quemfaz` | DB username |
| `DB_PASS` | `quemfaz` | DB password |
| `SMS_PROVIDER` | `FAKE` | `FAKE` (logs to console) or `AWS` |
| `JWT_SECRET` | — | JWT signing secret |
| `JWT_ISSUER` | — | JWT issuer |
| `JWT_AUDIENCE` | — | JWT audience |
| `JWT_EXPIRES_IN_MS` | — | Token TTL in milliseconds |

For local development, see [docs/local-development.md](../docs/local-development.md).

---

## Testing

### Infrastructure

- `BaseIntegrationTest` — abstract base class providing:
  - Testcontainers PostgreSQL (started once per test class)
  - Ktor `testApplication {}` setup with full app wiring
  - `createTestClient(token)` helper for authenticated requests
  - Table truncation before each test (via `tablesToClean`)

### Running tests

```shell
# All server tests (requires Docker)
./gradlew :server:test

# Integration tests only
./gradlew :server:test --tests "com.fugisawa.quemfaz.integration.*"
```

### CI overrides

All test config can be set via environment variables:
- `TEST_JWT_SECRET`, `TEST_JWT_ISSUER`, `TEST_JWT_AUDIENCE`, `TEST_JWT_EXPIRES_IN`
- `TEST_SMS_PROVIDER` (defaults to `FAKE`)

### Conventions

- New routes must have at least one integration test covering the happy path.
- Use `createTestClient(token)` for protected routes — don't manually construct auth headers in tests.
- Add new tables to `tablesToClean` in your test class to guarantee isolation.
- Unit tests for services use Mockito Kotlin — mock repository interfaces, not implementations.
