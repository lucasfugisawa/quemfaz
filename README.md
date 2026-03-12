# QuemFaz

A local-professionals-radar app connecting service seekers with professionals in Brazilian cities. Built with Kotlin Multiplatform (KMP) and Compose Multiplatform, targeting Android, iOS, Web, and a Ktor backend.

---

## Module Map

```
quemfaz/
├── composeApp/   Shared UI and client logic (Compose Multiplatform)
├── shared/       Cross-platform contracts: DTOs, domain models, core types
├── server/       Ktor REST API + PostgreSQL backend (JVM only)
├── androidApp/   Android entry point — thin wrapper around composeApp
└── iosApp/       iOS entry point (Xcode) — thin wrapper around composeApp
```

| Module | Platforms | Purpose |
|---|---|---|
| `composeApp` | Android, iOS, WASM, JS | All UI screens, ViewModels, navigation, session, network client |
| `shared` | All (including JVM for server) | API DTOs, domain models, error types, value objects |
| `server` | JVM | REST endpoints, business services, PostgreSQL, Flyway migrations |
| `androidApp` | Android | `MainActivity` — calls `App()` from composeApp |
| `iosApp` | iOS | Xcode project — embeds composeApp framework binary |

For architecture details, guardrails, and conventions, see [DEVELOPMENT_GUIDELINES.md](./DEVELOPMENT_GUIDELINES.md).

---

## Quick Start

### Prerequisites

- JDK 17+
- Android SDK (for Android targets)
- Xcode (for iOS targets)
- Docker (for local server development and running server tests)

### Local database

```shell
docker compose -f docker-compose.local.yml up -d
```

See [docs/local-development.md](./docs/local-development.md) for full server setup including configuration and SMS/OTP.

### Run — Android

```shell
./gradlew :androidApp:assembleDebug
```

Or use the IDE run configuration.

### Run — Server

```shell
./gradlew :server:run
```

### Run — Web (WASM)

```shell
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

### Run — Web (JS fallback)

```shell
./gradlew :composeApp:jsBrowserDevelopmentRun
```

### Run — iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run.

---

## Testing

### Server unit + integration tests

```shell
./gradlew :server:test
```

Integration tests use Testcontainers (PostgreSQL) — Docker must be running.

```shell
# Integration tests only
./gradlew :server:test --tests "com.fugisawa.quemfaz.integration.*"
```

See [server/README.md](./server/README.md) for test infrastructure details.

---

## Key docs

| Doc | What it covers |
|---|---|
| [DEVELOPMENT_GUIDELINES.md](./DEVELOPMENT_GUIDELINES.md) | Engineering standards, architecture, conventions |
| [CLAUDE.md](./CLAUDE.md) | Agent operating instructions (Claude Code) |
| [docs/local-development.md](./docs/local-development.md) | Local server setup, DB, SMS/OTP config |
| [composeApp/README.md](./composeApp/README.md) | UI architecture, navigation, theme, components |
| [shared/README.md](./shared/README.md) | Cross-platform contracts, DTOs, domain models |
| [server/README.md](./server/README.md) | Backend stack, routes, DB schema, test infra |

---

## Further reading

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)
