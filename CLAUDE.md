# Claude Code Instructions — QuemFaz

This file is auto-loaded by Claude Code. Read it before touching any code.

---

## Read first, in this order

1. **This file** — orientation and operating rules
2. **[DEVELOPMENT_GUIDELINES.md](./DEVELOPMENT_GUIDELINES.md)** — architecture, conventions, guardrails
3. **The relevant module README** — before editing any module:
   - [composeApp/README.md](./composeApp/README.md)
   - [shared/README.md](./shared/README.md)
   - [server/README.md](./server/README.md)
4. **The code in the area you're about to edit** — read before writing

---

## Repository shape

```
composeApp/   All UI screens, ViewModels, navigation, session, Ktor client
shared/       API DTOs, domain models, error types — the cross-platform contract layer
server/       Ktor REST API, application services, PostgreSQL, Flyway
androidApp/   MainActivity only — entry point, no logic
iosApp/       Xcode project — entry point, no logic
```

Dependency direction: `androidApp/iosApp` → `composeApp` → `shared` ← `server`

---

## Before editing

- [ ] Read the module README for the area you're working in
- [ ] Read the existing code around your change (nearby files, patterns, naming)
- [ ] Identify whether your change touches a shared contract (DTO in `shared/`) — if so, assess impact across all three of `shared`, `server`, and `composeApp`
- [ ] Confirm you're reusing existing patterns: navigation, DI, state, theme, components

---

## Hard guardrails

**Do not cross these without explicit instruction:**

1. **`shared` must stay platform-agnostic.** No JVM-only, Android-only, or iOS-only code or dependencies in `shared/src/commonMain/`.
2. **DTOs live in `shared/contract/`, not in `server/` or `composeApp/`.** Never duplicate a contract type.
3. **Navigation uses the custom `Screen` sealed class in `App.kt`.** Do not introduce Compose Navigation library.
4. **Auth state lives in `SessionManager` only.** Do not manage auth or tokens elsewhere.
5. **Database schema changes require a Flyway migration.** Never alter tables without a versioned migration file in `server/src/main/resources/db/migration/`.
6. **`ApiClient` handles JWT and 401 globally.** Feature API clients must not re-implement auth.
7. **Theme tokens over hardcoded values.** Use `AppSpacing`, `AppTypography`, `AppShapes`, `AppTheme` — not inline `dp`, `sp`, or color values.

---

## Scope discipline

- Do the task asked. No more.
- Do not refactor code you weren't asked to touch.
- Do not add features, logging, error handling, or abstractions beyond what the task requires.
- If you notice a problem outside your scope, mention it in your response — do not fix it silently.

---

## Reuse before creating

Before adding a new component, utility, or abstraction:
- Check `composeApp/src/commonMain/.../ui/components/` for existing UI components
- Check `shared/src/commonMain/.../core/` for existing error types, result wrappers, value objects
- Check `server/src/main/.../infrastructure/` for existing repository patterns

---

## Making changes safely

- For server changes: route handler → service → repository → DB migration (in that order, all in one change)
- For client changes: ViewModel state → Composable → wired via Koin DI
- For shared contract changes: update DTO in `shared/` → update server handler → update client API call — all three in one change
- Keep changes small and self-contained. Prefer multiple focused commits over one large one.

---

## Testing expectations

- Server business logic changes must be covered by integration tests (`BaseIntegrationTest`)
- New Ktor routes must have at least one happy-path integration test
- Run `./gradlew :server:test` before completing any server-side task (requires Docker)

---

## Reporting assumptions and deviations

If you make an assumption about intended behavior, or if completing the task requires going beyond the stated scope, say so explicitly before or after the change — not buried in code comments.

---

## Documentation maintenance

If your change affects architecture, module structure, conventions, workflows, or build/run commands — update the relevant documentation file in the same commit. See the Documentation Maintenance table in [DEVELOPMENT_GUIDELINES.md](./DEVELOPMENT_GUIDELINES.md#documentation-maintenance).
