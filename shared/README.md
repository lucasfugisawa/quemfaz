# shared

Cross-platform Kotlin library. The single source of truth for API contracts, domain models, and core types shared between `composeApp` and `server`.

---

## Role in the architecture

`shared` is the **contract layer**. It defines what flows between client and server. Both `composeApp` and `server` depend on it — neither defines its own equivalent types.

If a type needs to exist in both client and server, it belongs here.

---

## Source sets

| Source set | Purpose |
|---|---|
| `commonMain` | All shared code — DTOs, domain models, core abstractions |
| `androidMain` | Android-specific implementations (if any — currently minimal) |
| `iosMain` | iOS-specific implementations (if any — currently minimal) |
| `jvmMain` | JVM-specific implementations (used by server) |
| `jsMain` | JS-specific implementations |
| `wasmJsMain` | WASM-specific implementations |

All meaningful code lives in `commonMain`. Platform source sets are for `expect/actual` implementations only.

---

## Package structure

```
commonMain/kotlin/com/fugisawa/quemfaz/
├── contract/         API request/response DTOs (the client-server contract)
│   ├── auth/         AuthDtos.kt — OTP, login, user profile
│   ├── profile/      ProfileDtos.kt — professional profile CRUD
│   ├── search/       SearchDtos.kt — search request/response
│   ├── engagement/   EngagementDtos.kt — contact click, profile view tracking
│   ├── favorites/    FavoriteDtos.kt — add/remove/list favorites
│   └── moderation/   ModerationDtos.kt — profile reports
├── domain/           Domain models (richer than DTOs — business concepts)
│   ├── UserModels.kt
│   ├── ProfileModels.kt
│   ├── ServiceModels.kt
│   ├── CityModels.kt
│   └── ModerationModels.kt
└── core/             Foundational types used everywhere
    ├── AppError.kt        Sealed error hierarchy
    ├── ErrorCodes.kt      Standardized string error codes
    ├── AppResult.kt       Success/Error result wrapper
    ├── ValidationIssue.kt Validation error details
    ├── Pagination.kt      Cursor/offset pagination types
    ├── ValueObjects.kt    PhoneNumber, etc.
    ├── IDs.kt             ID generation utilities
    └── ActiveStatus.kt    Recently-active indicator logic
```

---

## Key contracts

### Auth

```kotlin
// Start OTP
StartOtpRequest(phoneNumber: String)
StartOtpResponse(otpSentAt: Instant)

// Verify OTP → get JWT
VerifyOtpRequest(phoneNumber: String, code: String)
VerifyOtpResponse(token: String, userId: String, isNewUser: Boolean)

// Complete profile
CompleteUserProfileRequest(name: String, photoUrl: String?)
UserProfileResponse(id, phone, name, photoUrl, cityName, status)
```

### Professional Profile

```kotlin
CreateProfessionalProfileDraftRequest(rawInput: String, inputMode: InputMode)
CreateProfessionalProfileDraftResponse(draft: InterpretedServiceDto, ...)
ConfirmProfessionalProfileRequest(...)
ProfessionalProfileResponse(id, userId, services, city, neighborhoods, ...)
```

### Search

```kotlin
SearchProfessionalsRequest(query: String, cityName: String, inputMode: InputMode)
SearchProfessionalsResponse(results: List<ProfessionalSearchResult>)
```

---

## Rules for modifying shared

1. **DTOs are contracts** — a change to a DTO affects both the server response and the client parsing. Always update both sides.
2. **Additive changes are safer than removals** — removing or renaming a field breaks the other side immediately. Coordinate the full change.
3. **No platform-specific dependencies in `commonMain`** — `shared` must compile for all targets. If you add a dependency, check it has a KMP-compatible artifact.
4. **`AppError` and `ErrorCodes` are the error contract** — server errors must map to these; client error handling must read from these. Do not invent new error surfaces.
5. **`AppResult<T>` is the standard result wrapper** — use it for any operation that can fail.
