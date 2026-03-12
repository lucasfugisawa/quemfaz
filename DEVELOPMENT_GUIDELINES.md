# Development Guidelines

Engineering standards, conventions, and architectural guardrails for the Quemfaz codebase.

---

## Module Boundaries

The module structure enforces a strict dependency direction:

```
androidApp / iosApp
      ↓
  composeApp  ←──────┐
      ↓              │
   shared  ──────────┘
      ↓
   server (also depends on shared)
```

**Rules:**

- `shared` must remain platform-agnostic. Never add JVM-only, Android-only, or iOS-only dependencies to `shared`. It targets Android, iOS, JVM, JS, and WASM simultaneously.
- `server` depends on `shared` for contracts (DTOs, error codes). It must not depend on `composeApp`.
- `composeApp` depends on `shared` for contracts. It must not contain business logic that belongs in services.
- `androidApp` and `iosApp` are entry points only — no logic goes there.

---

## Shared Module Is the Contract Layer

The `shared` module is the single source of truth for API contracts between client and server.

- All API request/response DTOs live in `shared/src/commonMain/.../contract/`.
- Domain models live in `shared/src/commonMain/.../domain/`.
- Core types (errors, results, validation, pagination, value objects) live in `shared/src/commonMain/.../core/`.

**If you change a DTO in `shared`, you must update both the server endpoint and the client API call in the same change.**

Never duplicate a DTO — if a concept is shared, it belongs in `shared`, not redefined in `server` or `composeApp`.

---

## Architecture Patterns

### Server (Ktor)

Follows a layered DDD-style structure:

```
Route handler  →  Application service  →  Repository (Exposed)  →  PostgreSQL
```

- Route handlers delegate immediately to services. No business logic in route files.
- Services are stateless — they receive dependencies via Koin DI.
- Repositories use Exposed ORM and return domain models or DTOs.
- Database schema changes **require a Flyway migration** (`server/src/main/resources/db/migration/`). Never alter tables without a versioned migration file.

### Client (composeApp)

Follows MVVM with Compose:

```
Composable screen  →  ViewModel (StateFlow)  →  FeatureApiClient  →  ApiClient (Ktor)
```

- ViewModels expose `StateFlow<ScreenState>`. Screens observe and react.
- ViewModels are created via Koin (`factory {}`). Do not instantiate them directly.
- `ApiClient` handles JWT injection and 401 responses globally. Feature clients must not re-implement auth.
- `SessionManager` is the single source of truth for auth state (`AuthState`) and current user/city. Do not manage auth state elsewhere.

---

## Navigation

The project uses a **custom screen-stack navigation** implemented in `composeApp/src/commonMain/.../App.kt`, not the Compose Navigation library.

- Screens are defined as a sealed class `Screen`.
- `navigateTo(screen)` pushes to the back stack.
- `navigateToTab(screen)` replaces the entire stack (used for bottom-nav tab switches).
- `navigateBack()` pops the stack.

**Do not introduce the Compose Navigation library** without a deliberate architectural discussion. The current approach is intentional for its simplicity.

---

## State Management

- All observable UI state lives in `MutableStateFlow` inside ViewModels.
- Screens must not hold mutable state beyond local transient UI state (e.g., text field values before submission).
- `SessionManager` uses `StateFlow` for `authState`, `currentUser`, and `currentCity`. These are the root state for the application shell.

---

## Dependency Injection (Koin)

- DI configuration for the client is in `composeApp/src/commonMain/.../di/`.
- DI configuration for the server is in `server/src/main/.../config/`.
- Use `single {}` for stateful/shared objects (SessionManager, ApiClient, repositories, services).
- Use `factory {}` for ViewModels and per-request objects.
- Do not use `get()` inside Composables directly — retrieve via ViewModel or a DI-aware composable entry point.

---

## UI: Theme and Component Reuse

The design system lives in `composeApp/src/commonMain/.../ui/theme/` and `composeApp/src/commonMain/.../ui/components/`.

**Always reuse before creating:**

- Use `AppTheme` as the root theme. Do not set Material3 colors or typography inline.
- Use `AppSpacing` values for all padding and spacing. Do not use hardcoded `dp` values.
- Use `AppTypography` for all text styles.
- Use `AppShapes` for corner radii.
- Reuse existing components: `AppScreen`, `ErrorMessage`, `FullScreenLoading`, `ProfileAvatar`, `ServiceChipList`, `StatusChipRow`.

Before adding a new component, check whether an existing one can be composed or parameterized to serve the same purpose.

---

## Testing

### Server

- All significant business logic must have integration test coverage using `BaseIntegrationTest` (Testcontainers + Ktor `testApplication`).
- Unit tests for services use Mockito Kotlin.
- Tables are cleaned before each test via `tablesToClean`. Add your table to this list when writing new integration tests.
- Use `createTestClient(token)` for testing authenticated endpoints.
- Tests must pass with Docker running locally and in CI (all config is overridable via env vars).

### Client (composeApp)

- Client-side testing is minimal at the MVP stage. Focus server-side integration tests for end-to-end coverage.
- Where client unit tests exist, they live in `composeApp/src/commonTest/`.

---

## Incremental, Safe Changes

- Read the code in the area you are modifying before writing anything.
- Reuse existing patterns — navigation, state management, DI, error handling — rather than inventing alternatives.
- Keep changes focused. A bug fix should not include unrelated refactors.
- If a change touches a shared contract (DTO, error code, domain model), assess the full blast radius across `shared`, `server`, and `composeApp` before proceeding.
- For database changes: write the migration first, then update the Exposed table definition and repository, then update any affected services.

---

## Documentation Maintenance

Documentation is part of the codebase.

When a change affects any of the following, update the relevant documentation in the same commit:

| Change | Update |
|---|---|
| New or removed module | `README.md` module map, `DEVELOPMENT_GUIDELINES.md` module boundaries |
| New screen, ViewModel, or navigation flow | `composeApp/README.md` |
| New or changed DTO / domain model | `shared/README.md` |
| New server endpoint or service | `server/README.md` |
| New shared component or theme token | `composeApp/README.md` components section |
| New Flyway migration | `server/README.md` schema section |
| Changed build/run/test commands | `README.md`, relevant module README |
| New architectural pattern or convention | `DEVELOPMENT_GUIDELINES.md` |
| New dev workflow or tooling | `docs/local-development.md` |

Documentation must never knowingly drift from the code. This rule applies to both human contributors and coding agents.
