# QuemFaz UX Improvements Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement voice-first interaction, home screen redesign, onboarding simplification, and data model consolidation as defined in `docs/superpowers/specs/2026-03-16-ux-improvements-design.md`.

**Architecture:** Backend-first approach — DB migrations and DTOs first, then server logic, then client UI. Each task produces a self-contained, committable change. Tasks are ordered by dependency: data model changes first (they affect everything downstream), then server features, then client UI. Note: between Task 2 (DTO changes) and completion of client tasks (Tasks 9-13), the `composeApp` module will not compile — this is expected and resolved incrementally.

**Tech Stack:** Kotlin Multiplatform (Compose UI, Ktor server, PostgreSQL/Flyway, Koin DI, kotlinx.serialization). Native platform APIs for speech-to-text (Android `SpeechRecognizer`, iOS `SFSpeechRecognizer`).

**Key codebase notes:**
- Theme spacing object is `Spacing` (not `AppSpacing`) — see `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/theme/Spacing.kt`
- Navigation `Screen` sealed class uses `object` (not `data object`)
- `Screen.SearchResults` is a singleton — search query is passed via `currentQuery` state and `homeViewModel.search()`, not via constructor
- Phone numbers live in `UserPhoneAuthIdentity` (separate from `User` model) — accessed via `UserPhoneAuthIdentityRepository`
- Server Koin DI is in `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt`
- Auth routes are mounted at `/auth`, search routes at `/search`
- `mapToResponse()` in `ProfileServices.kt` is shared by `ConfirmProfessionalProfileService`, `GetMyProfessionalProfileService`, `GetPublicProfessionalProfileService`, and `UpdateProfessionalProfileService`

---

## Chunk 1: Data Model Changes (fullName, birthDate, phone consolidation)

These are foundational — everything else depends on the DTOs and schema being updated.

### Task 1: Flyway migration — fullName, dateOfBirth, drop phone columns, search_events table

All schema changes in a single migration to keep the DB consistent.

**Files:**
- Create: `server/src/main/resources/db/migration/V14__ux_improvements.sql`

- [ ] **Step 1: Write migration SQL**

```sql
-- V14__ux_improvements.sql

-- 1. Merge first_name + last_name into full_name
ALTER TABLE users ADD COLUMN full_name TEXT NOT NULL DEFAULT '';
UPDATE users SET full_name = TRIM(CONCAT(first_name, ' ', last_name));
ALTER TABLE users DROP COLUMN first_name;
ALTER TABLE users DROP COLUMN last_name;

-- 2. Add date_of_birth to users
ALTER TABLE users ADD COLUMN date_of_birth DATE;

-- 3. Drop phone columns from professional_profiles
ALTER TABLE professional_profiles DROP COLUMN contact_phone;
ALTER TABLE professional_profiles DROP COLUMN whatsapp_phone;

-- 4. Create search_events table for popular searches
CREATE TABLE search_events (
    id TEXT PRIMARY KEY,
    resolved_service_id TEXT NOT NULL,
    city_name TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_events_city_created ON search_events (city_name, created_at);
CREATE INDEX idx_search_events_created ON search_events (created_at);
```

- [ ] **Step 2: Run server tests to verify migration applies**

Run: `cd server && ./gradlew test --tests "*BaseIntegrationTest*" -x :composeApp:test 2>&1 | tail -20`
Expected: Tests pass (migration applies cleanly on test containers).

- [ ] **Step 3: Commit**

```bash
git add server/src/main/resources/db/migration/V14__ux_improvements.sql
git commit -m "feat: add V14 migration for fullName, dateOfBirth, phone removal, search_events"
```

---

### Task 2: Update shared DTOs — fullName, dateOfBirth, phone consolidation

Update all contract types across `shared/contract/`.

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/auth/AuthDtos.kt`
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt`
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/search/SearchDtos.kt` (existing file — append new DTOs)

- [ ] **Step 1: Update AuthDtos.kt**

In `AuthDtos.kt`:
- `CompleteUserProfileRequest`: replace `firstName: String` and `lastName: String` with `fullName: String`
- `UserProfileResponse`: replace `firstName: String` and `lastName: String` with `fullName: String`. Add `dateOfBirth: String? = null`
- Add `UpdateDateOfBirthRequest` to this file (it's auth-related, not search-related):

```kotlin
@Serializable
data class UpdateDateOfBirthRequest(
    val dateOfBirth: String  // ISO-8601 format, e.g. "1990-05-15"
)
```

- [ ] **Step 2: Update ProfileDtos.kt**

In `ProfileDtos.kt`:
- `ProfessionalProfileResponse`: replace `firstName: String` and `lastName: String` with `fullName: String`. Remove `whatsAppPhone: String?` and `contactPhone: String`. Add `phone: String` (the account phone).
- `ConfirmProfessionalProfileRequest`: remove `contactPhone: String` and `whatsAppPhone: String?`

- [ ] **Step 3: Add popular search DTOs to SearchDtos.kt**

Append to the existing `SearchDtos.kt`:

```kotlin
@Serializable
data class PopularServiceDto(
    val serviceId: String,
    val displayName: String
)

@Serializable
data class PopularServicesResponse(
    val services: List<PopularServiceDto>,
    val isLocalResults: Boolean
)
```

- [ ] **Step 4: Verify shared module compiles**

Run: `./gradlew :shared:compileKotlinMetadata 2>&1 | tail -10`
Expected: Compilation succeeds (note: server and client will break until updated — that's expected).

- [ ] **Step 5: Commit**

```bash
git add shared/
git commit -m "feat: update shared DTOs for fullName, dateOfBirth, phone consolidation, popular searches"
```

---

### Task 3: Update server — User domain model and repository for fullName + dateOfBirth

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/domain/Models.kt` — update `User` data class
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/domain/Repositories.kt` — update `UserRepository` interface
- Modify: server Exposed user repository implementation — update table columns, mapping, queries
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/application/CompleteUserProfileService.kt` — use `fullName`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/application/GetAuthenticatedUserService.kt` — use `fullName` and add `dateOfBirth` to response
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/routing/AuthRoutes.kt` — add date-of-birth endpoint
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt` — if new dependencies needed

- [ ] **Step 1: Update User domain model**

In `auth/domain/Models.kt`: Replace `firstName: String` and `lastName: String` with `fullName: String`. Add `dateOfBirth: java.time.LocalDate? = null` (use `LocalDate` in the domain model — conversion to/from ISO string happens at the DTO boundary).

- [ ] **Step 2: Update UserRepository interface**

In `Repositories.kt`:
- Replace `updateName(id: UserId, firstName: String, lastName: String): User?` with `updateName(id: UserId, fullName: String): User?`
- Add `updateDateOfBirth(id: UserId, dateOfBirth: java.time.LocalDate): User?`

- [ ] **Step 3: Update Exposed repository implementation**

Find the Exposed `UsersTable` object. Replace `first_name` and `last_name` columns with `val fullName = text("full_name")`. Add `val dateOfBirth = date("date_of_birth").nullable()`. Update all `ResultRow` → `User` mapping. Update `updateName()` to set `full_name`. Add `updateDateOfBirth()`.

- [ ] **Step 4: Update CompleteUserProfileService**

Update `completeProfile()`:
- Accept `fullName` from `CompleteUserProfileRequest`
- Validate fullName has at least 2 whitespace-separated tokens after trimming
- Call `userRepository.updateName(userId, request.fullName.trim())`
- Map to `UserProfileResponse` with `fullName` and `dateOfBirth?.toString()`

- [ ] **Step 5: Update GetAuthenticatedUserService**

Update the mapping to `UserProfileResponse`:
- Use `user.fullName` instead of `user.firstName` / `user.lastName`
- Add `dateOfBirth = user.dateOfBirth?.toString()` to the response

- [ ] **Step 6: Update AuthRoutes**

Update `POST /auth/profile` to use `fullName` from `CompleteUserProfileRequest`.

Add new route inside the authenticated block:
```kotlin
put("/me/date-of-birth") {
    val userId = call.authenticatedUserId()
    val request = call.receive<UpdateDateOfBirthRequest>()
    val birthDate = LocalDate.parse(request.dateOfBirth)
    val age = Period.between(birthDate, LocalDate.now()).years
    if (age < 18) {
        call.respond(HttpStatusCode.UnprocessableEntity, mapOf("error" to "UNDERAGE"))
        return@put
    }
    userRepository.updateDateOfBirth(userId, birthDate)
    call.respond(HttpStatusCode.OK)
}
```

Note: This mounts at `/auth/me/date-of-birth` (consistent with existing `/auth/me` pattern). The spec says `/api/users/me/date-of-birth` — the plan uses `/auth/me/date-of-birth` for codebase consistency.

- [ ] **Step 7: Run server tests**

Run: `cd server && ./gradlew test -x :composeApp:test 2>&1 | tail -20`
Expected: Existing tests may need updating for new DTO shapes. Fix any failures.

- [ ] **Step 8: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/auth/
git commit -m "feat: update user domain for fullName and dateOfBirth"
```

---

### Task 4: Update server — Professional profile domain for phone removal and fullName

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/domain/Models.kt` — remove phone fields from `ProfessionalProfile`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/infrastructure/persistence/ExposedProfessionalProfileRepository.kt` — remove phone columns from table and mapping
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt` — update ALL services that use `mapToResponse()`: `ConfirmProfessionalProfileService`, `GetMyProfessionalProfileService`, `GetPublicProfessionalProfileService`, `UpdateProfessionalProfileService`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/application/SearchProfessionalsService.kt` — update response mapping
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt` — inject `UserPhoneAuthIdentityRepository` into profile services

- [ ] **Step 1: Update ProfessionalProfile domain model**

In `profile/domain/Models.kt`: remove `contactPhone: String?` and `whatsappPhone: String?` from `ProfessionalProfile` data class.

- [ ] **Step 2: Update Exposed table and repository**

In `ExposedProfessionalProfileRepository.kt`:
- Remove `contactPhone` and `whatsappPhone` columns from `ProfessionalProfilesTable`
- Update `ResultRow` → `ProfessionalProfile` mapping
- Update `save()` / `update()` methods to not write phone columns

- [ ] **Step 3: Update `mapToResponse()` in ProfileServices.kt**

The private `mapToResponse()` function (around line 232) builds `ProfessionalProfileResponse`. It must be updated to:
- Use `fullName` instead of `firstName` / `lastName` from the `User` object
- Remove `whatsAppPhone` and `contactPhone`
- Add `phone` field — this requires looking up the user's phone from `UserPhoneAuthIdentity`

To get the phone number, `mapToResponse()` needs access to `UserPhoneAuthIdentityRepository`. This repository must be injected as a new dependency into the profile services that call `mapToResponse()`. Update the constructor dependencies and Koin registrations in `KoinModules.kt`.

The phone lookup: `userPhoneAuthIdentityRepository.findByUserId(profile.userId)?.phoneNumber ?: ""`

- [ ] **Step 4: Update completeness check logic**

The completeness check in `ConfirmProfessionalProfileService` (around line 82) and `UpdateProfessionalProfileService` (around line 198) currently checks `request.contactPhone.isNotBlank()`. Remove these phone-based completeness checks — phone is always available from the account.

- [ ] **Step 5: Update SearchProfessionalsService response mapping**

In `SearchProfessionalsService.kt`: update the mapping that builds `ProfessionalProfileResponse`. Replace `firstName`/`lastName` with `user.fullName`. Add `phone` field — look up from `UserPhoneAuthIdentity` (same pattern as Step 3). Inject `UserPhoneAuthIdentityRepository` into this service.

- [ ] **Step 6: Add 18+ validation to draft creation**

In the service/route that handles `POST /professional-profile/draft`: add a check that the authenticated user has `date_of_birth` set and is 18+. If `date_of_birth` is null, respond with HTTP 422 and `{ "error": "DATE_OF_BIRTH_REQUIRED" }`. If under 18, respond with `{ "error": "UNDERAGE" }`.

- [ ] **Step 7: Run server tests**

Run: `cd server && ./gradlew test -x :composeApp:test 2>&1 | tail -20`
Expected: Fix any test failures from removed phone fields, updated name fields, and new dependencies.

- [ ] **Step 8: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/ server/src/main/kotlin/com/fugisawa/quemfaz/search/ server/src/main/kotlin/com/fugisawa/quemfaz/config/
git commit -m "feat: remove phone fields from professional profile, add phone from account, add 18+ validation"
```

---

## Chunk 2: Popular Searches Backend

### Task 5: Search event logging in SearchProfessionalsService

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/search/domain/SearchEvent.kt` — domain model
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/search/infrastructure/persistence/ExposedSearchEventRepository.kt` — repository
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/application/SearchProfessionalsService.kt` — log events after interpretation
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt` — register `SearchEventRepository`

- [ ] **Step 1: Create SearchEvent domain model and repository interface**

```kotlin
// SearchEvent.kt
data class SearchEvent(
    val id: String,
    val resolvedServiceId: String,
    val cityName: String,
    val createdAt: Instant
)

interface SearchEventRepository {
    fun logEvents(events: List<SearchEvent>)
    fun getPopularServices(cityName: String?, limit: Int, windowDays: Int): List<PopularServiceResult>
    fun countSearchesInWindow(cityName: String, windowDays: Int): Long
}

data class PopularServiceResult(
    val serviceId: String,
    val displayName: String,
    val count: Long
)
```

- [ ] **Step 2: Implement Exposed repository**

Create `ExposedSearchEventRepository.kt`:
- `SearchEventsTable` object mapping to `search_events` table
- `logEvents()`: batch insert with UUID generation for `id`
- `getPopularServices()`: `SELECT resolved_service_id, COUNT(*) ... GROUP BY ... ORDER BY count DESC LIMIT N` with 30-day window filter. Join with the services table (in the catalog module) to get `displayName`. Check how `CatalogService` accesses the service catalog to find the correct table name.
- `countSearchesInWindow()`: count for threshold check

- [ ] **Step 3: Wire into SearchProfessionalsService**

After LLM interpretation resolves services in `SearchProfessionalsService.kt`, if resolved services are non-empty, create `SearchEvent` entries for each resolved service and call `searchEventRepository.logEvents()`. Use the **resolved** city variable (e.g., `interpreted.cityName ?: request.cityName`), not just `request.cityName`, to match the actual search city.

- [ ] **Step 4: Register in Koin DI**

In `KoinModules.kt`, add `SearchEventRepository` as a singleton:
```kotlin
single<SearchEventRepository> { ExposedSearchEventRepository() }
```
Update `SearchProfessionalsService` constructor to accept `SearchEventRepository`.

- [ ] **Step 5: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/search/ server/src/main/kotlin/com/fugisawa/quemfaz/config/
git commit -m "feat: log search events for popular searches aggregation"
```

---

### Task 6: Popular searches endpoint with caching

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/search/application/PopularSearchesService.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/routing/SearchRoutes.kt` — add GET endpoint
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt` — register `PopularSearchesService`

- [ ] **Step 1: Create PopularSearchesService with in-memory cache**

```kotlin
class PopularSearchesService(
    private val searchEventRepository: SearchEventRepository,
    private val minSearchesThreshold: Int = 10,
    private val windowDays: Int = 30,
    private val limit: Int = 8,
    private val cacheDurationMinutes: Long = 15
) {
    private data class CacheEntry(val response: PopularServicesResponse, val cachedAt: Instant)
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    fun getPopularServices(cityName: String?): PopularServicesResponse {
        val cacheKey = cityName ?: "__global__"
        val cached = cache[cacheKey]
        if (cached != null && Duration.between(cached.cachedAt, Instant.now()).toMinutes() < cacheDurationMinutes) {
            return cached.response
        }
        val response = computePopularServices(cityName)
        cache[cacheKey] = CacheEntry(response, Instant.now())
        return response
    }

    private fun computePopularServices(cityName: String?): PopularServicesResponse {
        if (cityName != null) {
            val cityCount = searchEventRepository.countSearchesInWindow(cityName, windowDays)
            if (cityCount >= minSearchesThreshold) {
                val results = searchEventRepository.getPopularServices(cityName, limit, windowDays)
                return PopularServicesResponse(
                    services = results.map { PopularServiceDto(it.serviceId, it.displayName) },
                    isLocalResults = true
                )
            }
        }
        val results = searchEventRepository.getPopularServices(null, limit, windowDays)
        return PopularServicesResponse(
            services = results.map { PopularServiceDto(it.serviceId, it.displayName) },
            isLocalResults = false
        )
    }
}
```

- [ ] **Step 2: Add route**

In `SearchRoutes.kt`, add inside the existing route block. Since search routes are mounted at `/search`, the endpoint will be at `/search/services/popular`:

```kotlin
get("/services/popular") {
    val cityName = call.request.queryParameters["cityName"]
    val response = popularSearchesService.getPopularServices(cityName)
    call.respond(response)
}
```

Note: The spec says `GET /api/services/popular` but search routes mount at `/search`. Use `/search/services/popular` for codebase consistency. Update the client API call accordingly.

- [ ] **Step 3: Register service in Koin DI**

In `KoinModules.kt`:
```kotlin
single { PopularSearchesService(get()) }
```

- [ ] **Step 4: Write integration test**

Create test class extending `BaseIntegrationTest`:
- Test empty search_events returns empty popular list with `isLocalResults = false`
- Test after inserting 10+ events for a city, returns city-level results with `isLocalResults = true`
- Test below threshold falls back to global results

- [ ] **Step 5: Run tests**

Run: `cd server && ./gradlew test -x :composeApp:test 2>&1 | tail -20`

- [ ] **Step 6: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/search/ server/src/main/kotlin/com/fugisawa/quemfaz/config/ server/src/test/
git commit -m "feat: add popular searches endpoint with caching and city fallback"
```

---

## Chunk 3: Voice Input Platform Layer (KMP expect/actual)

### Task 7: Speech-to-text expect/actual declarations

Follow the existing KMP pattern used by `ImagePicker.kt` and `BackHandler.kt` — use `@Composable expect fun` that returns a handle, not `expect class` with constructors.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/platform/SpeechRecognizer.kt` — expect declarations
- Create: `composeApp/src/androidMain/kotlin/com/fugisawa/quemfaz/platform/SpeechRecognizer.android.kt` — Android actual
- Create: `composeApp/src/iosMain/kotlin/com/fugisawa/quemfaz/platform/SpeechRecognizer.ios.kt` — iOS actual

- [ ] **Step 1: Define expect declarations in commonMain**

```kotlin
// SpeechRecognizer.kt (commonMain)
package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable

enum class SpeechRecognizerState {
    IDLE, LISTENING, ERROR, UNAVAILABLE
}

data class SpeechRecognizerResult(
    val text: String,
    val isFinal: Boolean
)

/**
 * Returns true if speech recognition is available on this device.
 * Must be called from a Composable context to access platform context (e.g., Android Context).
 */
@Composable
expect fun isSpeechRecognizerAvailable(): Boolean

/**
 * Remembers a speech recognizer handle. State changes are communicated via callbacks.
 * The returned handle's startListening/stopListening control the recognition lifecycle.
 */
@Composable
expect fun rememberSpeechRecognizer(
    onResult: (SpeechRecognizerResult) -> Unit,
    onError: (String) -> Unit,
    onStateChange: (SpeechRecognizerState) -> Unit
): SpeechRecognizerHandle

class SpeechRecognizerHandle(
    val startListening: () -> Unit,
    val stopListening: () -> Unit
)
```

Note: `SpeechRecognizerHandle` is a regular class (not `expect`) — it's just a holder for lambdas. State is managed via the `onStateChange` callback and composed locally in the UI. This avoids the reactivity problem of putting `val state` on an expect class.

- [ ] **Step 2: Implement Android actual**

Use `android.speech.SpeechRecognizer` API:
- Access `LocalContext.current` inside the composable to get Android Context
- `isSpeechRecognizerAvailable()`: return `SpeechRecognizer.isRecognitionAvailable(context)`
- `rememberSpeechRecognizer()`: create `SpeechRecognizer` with `RecognitionListener`
- Request `RECORD_AUDIO` permission on `startListening()` — use `rememberLauncherForActivityResult` with `ActivityResultContracts.RequestPermission`
- Map `onResults` → `SpeechRecognizerResult(text, isFinal=true)`
- Map `onPartialResults` → `SpeechRecognizerResult(text, isFinal=false)`
- Map `onError` → error callback with user-friendly pt-BR message
- Handle permission denial: call `onError("Permissão de microfone necessária")`
- Clean up recognizer in `DisposableEffect`

- [ ] **Step 3: Implement iOS actual**

Use `Speech.SFSpeechRecognizer` API:
- `isSpeechRecognizerAvailable()`: check `SFSpeechRecognizer.isAvailable()`
- `rememberSpeechRecognizer()`: create `SFSpeechAudioBufferRecognitionRequest` + `AVAudioEngine`
- Request speech recognition + microphone permissions on `startListening()`
- Map recognition results to `SpeechRecognizerResult`
- Handle permission denial gracefully
- Clean up in `DisposableEffect`

- [ ] **Step 4: Verify compilation on both platforms**

Run: `./gradlew :composeApp:compileKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64 2>&1 | tail -20`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/platform/
git add composeApp/src/androidMain/kotlin/com/fugisawa/quemfaz/platform/
git add composeApp/src/iosMain/kotlin/com/fugisawa/quemfaz/platform/
git commit -m "feat: add KMP speech-to-text expect/actual for Android and iOS"
```

---

### Task 8: Reusable VoiceInputButton composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/VoiceInputButton.kt`

- [ ] **Step 1: Check if `Icons.Default.Mic` is available**

Check if the project already depends on `material-icons-extended`. If not, either add the dependency or use a text-based mic icon (e.g., `Text("🎤", fontSize = 28.sp)`) consistent with how other screens use emojis for icons. Follow whichever pattern the codebase already uses.

- [ ] **Step 2: Create VoiceInputButton composable**

```kotlin
@Composable
fun VoiceInputButton(
    onTranscription: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAvailable = isSpeechRecognizerAvailable()
    if (!isAvailable) return  // Hide entirely if STT unavailable

    var state by remember { mutableStateOf(SpeechRecognizerState.IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val recognizer = rememberSpeechRecognizer(
        onResult = { result -> onTranscription(result.text) },
        onError = { error -> errorMessage = error },
        onStateChange = { newState -> state = newState }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Large circular mic button with gradient
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .clickable {
                    errorMessage = null
                    if (state == SpeechRecognizerState.LISTENING) {
                        recognizer.stopListening()
                    } else {
                        recognizer.startListening()
                    }
                }
        ) {
            // Use emoji or material icon depending on project convention
            Text(
                text = if (state == SpeechRecognizerState.LISTENING) "⏹" else "🎤",
                fontSize = 28.sp
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = when (state) {
                SpeechRecognizerState.LISTENING -> "Ouvindo..."
                else -> "Toque para falar"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Non-intrusive error message
        errorMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinMetadata 2>&1 | tail -10`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/VoiceInputButton.kt
git commit -m "feat: add reusable VoiceInputButton composable"
```

---

## Chunk 4: Client-Side UI Updates

### Task 9: Update auth flow, My Profile, and all fullName references

This task updates ALL client code that references `firstName`/`lastName` to use `fullName`.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/AuthScreens.kt` — NameInputScreen
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/AuthViewModel.kt` — submitName
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/MyProfileScreen.kt` — merge name fields
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeScreen.kt` — profile display
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/ProfileScreens.kt` — professional profile display
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt` — name display
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileScreen.kt` — name display
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/SearchScreens.kt` — result card name display
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt` — call site updates for NameInputScreen, MyProfileScreen
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/preview/PreviewSamples.kt` — update sample data

- [ ] **Step 1: Update NameInputScreen**

In `AuthScreens.kt`:
- Change `onSubmitName: (firstName: String, lastName: String) -> Unit` to `onSubmitName: (fullName: String) -> Unit`
- Remove the `split(" ", limit = 2)` logic
- Add validation: enable button only when input has 2+ words after trim (`displayName.trim().split("\\s+".toRegex()).size >= 2`)
- Pass `displayName.trim()` directly to `onSubmitName`

- [ ] **Step 2: Update AuthViewModel**

In `AuthViewModel.kt`: update `submitName()` (around line 92) to accept a single `fullName: String` parameter and send `CompleteUserProfileRequest(fullName = fullName)`.

- [ ] **Step 3: Update App.kt call sites**

In `App.kt`:
- Update `NameInputScreen` call site (around line 166): change `onSubmitName = { firstName, lastName -> viewModel.submitName(firstName, lastName) }` to `onSubmitName = { fullName -> viewModel.submitName(fullName) }`
- Update `MyProfileScreen` call site (around line 393): change `onSaveName = { firstName, lastName -> ... }` to `onSaveName = { fullName -> ... }`

- [ ] **Step 4: Update MyProfileScreen**

Replace separate `firstName` and `lastName` fields with a single `fullName` field:
- `var fullName by remember { mutableStateOf(currentUser.fullName) }`
- Single `OutlinedTextField` with label "Nome completo"
- Save button validation: at least 2 words
- Update `onSaveName` callback to accept `(fullName: String)`

- [ ] **Step 5: Search and replace ALL remaining firstName/lastName references**

Search the entire `composeApp/` for `firstName` and `lastName` references. Key places to update:
- `HomeScreen.kt` (around line 58): `"${it.firstName} ${it.lastName}"` → `it.fullName`
- `ProfileScreens.kt` (around lines 52, 237, 244): use `profile.fullName` or `user.fullName`
- `OnboardingScreens.kt` (around line 422): use `fullName`
- `EditProfessionalProfileScreen.kt` (around line 134): use `profile.fullName`
- `SearchScreens.kt`: update any `firstName`/`lastName` references in result card rendering
- `PreviewSamples.kt`: update all sample `UserProfileResponse` and `ProfessionalProfileResponse` to use `fullName` instead of `firstName`/`lastName`

- [ ] **Step 6: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinMetadata 2>&1 | tail -10`

- [ ] **Step 7: Commit**

```bash
git add composeApp/
git commit -m "feat: update all client code for single fullName field"
```

---

### Task 10: Update Home Screen — voice button, example hint, popular searches

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeViewModel.kt` — add popular searches loading, inputMode tracking
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/api/FeatureApiClients.kt` (or wherever API client methods live) — add popular searches method
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt` — update HomeScreen call site with new parameters

- [ ] **Step 1: Add popular searches API client method**

Add a method to the appropriate API client to call `GET /search/services/popular?cityName={name}` (matching the server route from Task 6).

- [ ] **Step 2: Update HomeViewModel**

Add state for popular searches:
```kotlin
private val _popularServices = MutableStateFlow<PopularServicesResponse?>(null)
val popularServices: StateFlow<PopularServicesResponse?> = _popularServices.asStateFlow()
```

Load on init (and when city changes):
```kotlin
fun loadPopularServices(cityName: String?) {
    viewModelScope.launch {
        try {
            _popularServices.value = apiClients.getPopularServices(cityName)
        } catch (e: Exception) {
            // Non-critical — don't show popular searches on error
        }
    }
}
```

Add `inputMode` tracking:
```kotlin
var inputMode: InputMode = InputMode.TEXT
    private set

fun setVoiceInputMode() { inputMode = InputMode.VOICE }
fun resetInputMode() { inputMode = InputMode.TEXT }
```

Update `search()` to pass `inputMode` and reset it after search.

- [ ] **Step 3: Redesign HomeScreen composable**

Update the layout to follow the spec order:

1. Top bar (existing)
2. Title (existing)
3. Search field — update placeholder to "Diga ou digite o que você precisa...". Remove old disabled mic emoji trailing icon.
4. Example hint — `Text("Ex: \"Preciso de alguém para pintar minha casa\"", style = bodySmall, color = onSurfaceVariant)`
5. VoiceInputButton — `VoiceInputButton(onTranscription = { query = it; viewModel.setVoiceInputMode() })`
6. Popular searches section:
   - Header: "Mais buscados na sua cidade" or "Mais buscados no QuemFaz" based on `isLocalResults`
   - `LazyRow` of `SuggestionChip` items from `popularServices`
   - `onClick` for each chip: set `currentQuery` to service name and trigger search via existing pattern
7. Category link: `TextButton("Ver todas as categorias") { onNavigateToCategoryBrowsing() }`
8. Earn money card (existing, moved to bottom)

Add `onNavigateToCategoryBrowsing: () -> Unit` parameter to the composable.

- [ ] **Step 4: Update App.kt HomeScreen call site**

Update the HomeScreen instantiation in App.kt to pass:
- `onNavigateToCategoryBrowsing = { navigateTo(Screen.CategoryBrowsing) }`
- Popular services state from ViewModel
- Any other new parameters

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinMetadata 2>&1 | tail -10`

- [ ] **Step 6: Commit**

```bash
git add composeApp/
git commit -m "feat: redesign home screen with voice button, example hint, and popular searches"
```

---

### Task 11: Category Browsing Screen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/CategoryBrowsingScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/navigation/Screens.kt` — add `Screen.CategoryBrowsing`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt` — add navigation case

- [ ] **Step 1: Add Screen.CategoryBrowsing to navigation**

In `Screens.kt` (use `object`, not `data object`, to match existing convention):
```kotlin
object CategoryBrowsing : Screen("categories")
```

- [ ] **Step 2: Create CategoryBrowsingScreen composable**

```kotlin
@Composable
fun CategoryBrowsingScreen(
    onServiceClick: (serviceName: String) -> Unit,
    onBack: () -> Unit
) {
    val catalogClient = koinInject<CatalogApiClient>()
    var catalog by remember { mutableStateOf<CatalogResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            catalog = catalogClient.getCatalog()
        } catch (e: Exception) {
            error = "Não foi possível carregar as categorias."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> { /* CircularProgressIndicator centered */ }
            error != null -> { /* Error message */ }
            catalog != null -> {
                LazyColumn(contentPadding = padding) {
                    catalog!!.categories.forEach { category ->
                        item {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = Spacing.screenEdge, vertical = Spacing.sm)
                            )
                        }
                        items(category.services) { service ->
                            Text(
                                text = service.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onServiceClick(service.displayName) }
                                    .padding(horizontal = Spacing.screenEdge + Spacing.md, vertical = Spacing.sm)
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Add navigation case in App.kt**

In the `when (currentScreen)` block:
```kotlin
is Screen.CategoryBrowsing -> CategoryBrowsingScreen(
    onServiceClick = { serviceName ->
        // Use existing search pattern: set query, trigger search, navigate
        currentQuery = serviceName
        homeViewModel.search(serviceName)
        navigateTo(Screen.SearchResults)
    },
    onBack = { navigateBack() }
)
```

Note: `Screen.SearchResults` is a singleton object — pass the query via `currentQuery` state and `homeViewModel.search()`, matching the existing navigation pattern.

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinMetadata 2>&1 | tail -10`

- [ ] **Step 5: Commit**

```bash
git add composeApp/
git commit -m "feat: add category browsing screen"
```

---

### Task 12: Update Professional Onboarding — birth date step, voice input, remove phone

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt` — add BirthDateRequired state, dateOfBirth API call, remove phone from submitKnownName
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt` — add birth date screen, add voice button to description step, update stepIndex and showBack
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt` — update back handler for BirthDateRequired
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/api/FeatureApiClients.kt` (or equivalent) — add `updateDateOfBirth()` method

- [ ] **Step 1: Add updateDateOfBirth API client method**

Add method to call `PUT /auth/me/date-of-birth` with `UpdateDateOfBirthRequest`.

- [ ] **Step 2: Update OnboardingUiState**

Add `BirthDateRequired` as a new state:
```kotlin
sealed class OnboardingUiState {
    object BirthDateRequired : OnboardingUiState()  // NEW — initial state
    object Idle : OnboardingUiState()
    // ... rest unchanged
}
```

- [ ] **Step 3: Update OnboardingViewModel initialization and logic**

On init, check `sessionManager.currentUser.value?.dateOfBirth`:
- If non-null → set state to `Idle` (skip birth date step)
- If null → set state to `BirthDateRequired`

Add function:
```kotlin
fun submitDateOfBirth(dateOfBirth: String) {
    val birthDate = LocalDate.parse(dateOfBirth)
    val age = Period.between(birthDate, LocalDate.now()).years
    if (age < 18) {
        _uiState.value = Error("Você precisa ter pelo menos 18 anos para oferecer serviços.")
        return
    }
    viewModelScope.launch {
        try {
            _uiState.value = Loading
            apiClients.updateDateOfBirth(dateOfBirth)
            _uiState.value = Idle
        } catch (e: Exception) {
            _uiState.value = Error("Não foi possível salvar a data de nascimento.")
        }
    }
}
```

Update `goBack()` function: add case for `BirthDateRequired` — this is handled by hardware back (exits onboarding via `navigateBack()` in App.kt). In-screen back from `BirthDateRequired` should also call the `onBack` callback.

Update `submitKnownName()` (around line 201-208): remove `contactPhone = ""` and `whatsAppPhone = null` from the `ConfirmProfessionalProfileRequest` construction (these fields no longer exist on the DTO after Task 2).

- [ ] **Step 4: Update OnboardingScreens composable**

Update `stepIndex()` function (around line 38-46): add `is OnboardingUiState.BirthDateRequired -> 0` and shift all other indices by 1.

Update `showBack` / `showStepIndicator` logic (around lines 87-94): include `BirthDateRequired` in the states that show a back button and step indicator.

Add birth date composable when state is `BirthDateRequired`:
- Title: "Para oferecer serviços, você precisa ter pelo menos 18 anos."
- Date picker input for birth date
- "Continuar" button that calls `viewModel.submitDateOfBirth(selectedDate)`

- [ ] **Step 5: Add voice input to description step (Idle state)**

In the `Idle` state composable:
- Keep the existing textarea
- Add example hint below: `Text("Ex: \"Sou pintor residencial com 10 anos de experiência\"", style = bodySmall, color = onSurfaceVariant)`
- Add `VoiceInputButton(onTranscription = { description = it })` below the hint
- Track `inputMode` — set to `VOICE` when transcription is used, pass to `createDraft()`

- [ ] **Step 6: Update back navigation in App.kt**

In `App.kt`, the `isOnboardingInProgress` check (around lines 410-414) controls hardware back handling. Since `BirthDateRequired` is NOT in the in-progress list, hardware back will use the global back handler which calls `navigateBack()` — this correctly exits onboarding. No change needed here.

However, verify that the in-screen back button callback for the onboarding screen passes through correctly for `BirthDateRequired`.

- [ ] **Step 7: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinMetadata 2>&1 | tail -10`

- [ ] **Step 8: Commit**

```bash
git add composeApp/
git commit -m "feat: add birth date step and voice input to professional onboarding"
```

---

### Task 13: Update Profile Edit and Profile View screens

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileScreen.kt` — remove phone fields, update onSave signature
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt` — update saveProfile
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/ProfileScreens.kt` — use `phone` field for WhatsApp/Ligar buttons
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt` — update EditProfessionalProfile and ProfessionalProfile call sites

- [ ] **Step 1: Remove phone fields from EditProfessionalProfileScreen**

Remove the `OutlinedTextField` for "Telefone de contato" and "Telefone do WhatsApp". Update `onSave` callback signature from `(description: String, city: String, contactPhone: String, whatsAppPhone: String) -> Unit` to `(description: String, city: String) -> Unit`.

- [ ] **Step 2: Update EditProfessionalProfileViewModel.saveProfile()**

Remove `contactPhone` and `whatsAppPhone` parameters. Update the `ConfirmProfessionalProfileRequest` construction to not include phone fields.

- [ ] **Step 3: Update App.kt call site for EditProfessionalProfile**

Update the call site (around line 463): change `onSave = { desc, city, contact, whatsapp -> viewModel.saveProfile(desc, city, contact, whatsapp) }` to `onSave = { desc, city -> viewModel.saveProfile(desc, city) }`.

- [ ] **Step 4: Update ProfessionalProfileScreen**

Update the WhatsApp and Ligar buttons to use `profile.phone` instead of `profile.whatsAppPhone` / `profile.contactPhone`.

Update the contact click handling in `App.kt` (around lines 353-361): replace `profile?.whatsAppPhone` and `profile?.contactPhone` references with `profile?.phone`.

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinMetadata 2>&1 | tail -10`

- [ ] **Step 6: Commit**

```bash
git add composeApp/
git commit -m "feat: remove phone fields from profile edit, use account phone in profile view"
```

---

## Chunk 5: Integration Testing and Final Verification

### Task 14: Server integration tests for new features

**Files:**
- Create: `server/src/test/kotlin/com/fugisawa/quemfaz/integration/UxImprovementsIntegrationTest.kt`

- [ ] **Step 1: Write test for fullName profile completion**

Extend `BaseIntegrationTest`. Test cases:
- `POST /auth/profile` with `{ "fullName": "Maria da Silva" }` → 200 OK
- `POST /auth/profile` with `{ "fullName": "Maria" }` → 400 or validation error (single word)
- `POST /auth/profile` with `{ "fullName": "  Maria  " }` → 400 (leading/trailing spaces, single token after trim)
- `POST /auth/profile` with `{ "fullName": "Maria da Silva Santos" }` → 200 OK (multi-part name)

- [ ] **Step 2: Write test for date of birth endpoint**

Test cases:
- `PUT /auth/me/date-of-birth` with `{ "dateOfBirth": "1990-05-15" }` → 200 OK
- `PUT /auth/me/date-of-birth` with `{ "dateOfBirth": "2015-01-01" }` → 422 with `UNDERAGE` error
- `PUT /auth/me/date-of-birth` with `{ "dateOfBirth": "invalid" }` → 400

- [ ] **Step 3: Write test for popular searches endpoint**

Test cases:
- `GET /search/services/popular?cityName=Batatais` → 200 with `PopularServicesResponse`
- When no search events exist → empty services list, `isLocalResults: false`
- After inserting 10+ events for "Batatais" → non-empty services, `isLocalResults: true`
- With fewer than threshold → falls back to global, `isLocalResults: false`

- [ ] **Step 4: Write test for professional profile without phone fields**

Test cases:
- `POST /professional-profile/confirm` without phone fields → 200 OK
- `GET /professional-profile/{id}` → response includes `phone` from user account, no `contactPhone`/`whatsAppPhone`

- [ ] **Step 5: Write test for 18+ enforcement on draft creation**

Test cases:
- Draft creation with user missing `date_of_birth` → 422 `DATE_OF_BIRTH_REQUIRED`
- Draft creation with underage user → 422 `UNDERAGE`
- Draft creation with 18+ user → proceeds normally

- [ ] **Step 6: Run all server tests**

Run: `cd server && ./gradlew test -x :composeApp:test 2>&1 | tail -30`
Expected: All tests pass.

- [ ] **Step 7: Commit**

```bash
git add server/src/test/
git commit -m "test: add integration tests for UX improvements"
```

---

### Task 15: Full compilation and smoke test

- [ ] **Step 1: Full project build**

Run: `./gradlew build -x test 2>&1 | tail -20`
Expected: All modules compile.

- [ ] **Step 2: Run all server tests**

Run: `cd server && ./gradlew test 2>&1 | tail -20`
Expected: All tests pass.

- [ ] **Step 3: Final commit if any fixes were needed**

```bash
git add -A
git commit -m "fix: resolve build issues from UX improvements integration"
```
