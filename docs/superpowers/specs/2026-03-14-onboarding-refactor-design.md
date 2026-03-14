# Design Spec — User & Professional Onboarding Refactor

**Date:** 2026-03-14
**Status:** Approved
**Scope:** database schema · shared contracts · server domain/services · client onboarding flows · image storage abstraction · image picker (Android / iOS / Web)

---

## 1. Context and Goals

The current onboarding model collects a single `name` field after phone/OTP login. This is insufficient for the next stage of the product. The goals of this refactor are:

- Replace the single `name` field with `firstName` + `lastName`
- Add optional profile photo support to user onboarding
- Add a temporary-but-replaceable image storage mechanism
- Extend professional profiles with an optional `knownName`
- Make professional onboarding step-based: service fit first, then photo (required), then known name (optional)
- Keep search focused on service + locality discovery — name fields are display data only at this stage

---

## 2. Database Schema (Migration V5)

### `users` table

Remove the existing `name` column. Add `first_name` and `last_name`.

```sql
ALTER TABLE users DROP COLUMN name;
ALTER TABLE users ADD COLUMN first_name TEXT NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN last_name  TEXT NOT NULL DEFAULT '';
```

The `DEFAULT ''` is a migration mechanic only. The application layer enforces non-empty, trimmed values before any row is written. Existing data does not need to be preserved (development environment).

`photo_url TEXT` (already present and nullable) — no change.

### `professional_profiles` table

```sql
ALTER TABLE professional_profiles ADD COLUMN known_name TEXT;
```

Nullable. No `photo_url` column is added here — the photo lives exclusively on `users.photo_url`.

### New `stored_images` table

Temporary blob storage for profile photos, designed to be retired when permanent file storage (e.g. S3) is introduced.

```sql
CREATE TABLE stored_images (
    id           TEXT        PRIMARY KEY,
    data         BYTEA       NOT NULL,
    content_type TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

The `users.photo_url` column stores `/api/images/{id}` when backed by this table, and will store S3 URLs after migration — no schema change required at migration time.

---

## 3. Shared Contracts (`shared/contract/`)

### `AuthDtos.kt`

```kotlin
// Name submission (step 2 of auth onboarding)
data class CompleteUserProfileRequest(
    val firstName: String,
    val lastName: String,
)

// Photo URL assignment (step 3 of auth onboarding — optional)
data class SetProfilePhotoRequest(
    val photoUrl: String,   // must be a URL issued by our own /api/images/upload endpoint
)

// Current user response
data class UserProfileResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val photoUrl: String?,
    // ...existing fields unchanged
)
```

`SetProfilePhotoRequest.photoUrl` always contains a URL returned by `POST /api/images/upload`. The endpoint should reject URLs that do not match the expected internal path pattern.

### `ProfileDtos.kt`

```kotlin
// Professional profile response — replaces name with structured fields
data class ProfessionalProfileResponse(
    // ...existing fields
    val firstName: String,
    val lastName: String,
    val knownName: String?,
    val photoUrl: String?,
)

// New: set known name during professional onboarding
data class SetKnownNameRequest(
    val knownName: String?,   // null or empty string = no known name
)
```

**Display name rule (client-side):**
```
displayName = knownName ?: "$firstName $lastName"
```

This computation lives in the client. The server always returns all three fields separately.

### New `ImageDtos.kt`

```kotlin
data class UploadImageResponse(val url: String)
```

---

## 4. Server Changes

### 4.1 Domain Models

**`auth/domain/Models.kt`**

```kotlin
data class User(
    val id: UserId,
    val firstName: String,
    val lastName: String,
    val photoUrl: String?,
    val status: UserStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

**`profile/domain/Models.kt`**

```kotlin
data class ProfessionalProfile(
    // ...existing fields unchanged
    val knownName: String?,   // added
    // no photoUrl — stays on User
)
```

### 4.2 Repository Interface Changes

```kotlin
interface UserRepository {
    // replaces updateProfile(userId, name, photoUrl)
    suspend fun updateName(userId: UserId, firstName: String, lastName: String)
    suspend fun updatePhotoUrl(userId: UserId, photoUrl: String)
    // ...existing methods unchanged
}

interface ProfessionalProfileRepository {
    suspend fun updateKnownName(profileId: ProfessionalProfileId, knownName: String?)
    // ...existing methods unchanged
}
```

### 4.3 Image Storage Abstraction

Location: `server/src/main/kotlin/.../infrastructure/images/`

```kotlin
interface ImageStorageService {
    suspend fun store(data: ByteArray, contentType: String): String   // returns URL
    suspend fun retrieve(id: String): StoredImage?
}

data class StoredImage(val data: ByteArray, val contentType: String)

// Temporary implementation backed by stored_images table
class DatabaseImageStorageService(db: Database) : ImageStorageService {
    override suspend fun store(data: ByteArray, contentType: String): String {
        val id = ULID.random()
        // INSERT INTO stored_images (id, data, content_type) VALUES (...)
        return "/api/images/$id"
    }
    override suspend fun retrieve(id: String): StoredImage? { /* SELECT ... */ }
}
```

**Koin binding:**
```kotlin
single<ImageStorageService> { DatabaseImageStorageService(get()) }
```

Migrating to S3 later = implement `S3ImageStorageService`, change this one Koin binding.

### 4.4 Application Services

| Service | Change |
|---|---|
| `CompleteUserProfileService` | Saves `firstName` + `lastName` via `userRepository.updateName()` |
| New `SetProfilePhotoService` | Validates URL is internal, saves via `userRepository.updatePhotoUrl()` |
| New `SetKnownNameService` | Calls `professionalProfileRepository.updateKnownName()` |
| `ConfirmProfessionalProfileService` | Adds guard: `require(user.photoUrl != null)` — safety net only; flow enforcement is in the ViewModel |
| All response mappers | Replace `name` with `firstName`, `lastName`, `knownName` |

### 4.5 Routes

```
POST /auth/profile                    → CompleteUserProfileService
POST /auth/photo                      → SetProfilePhotoService
POST /api/images/upload               → ImageStorageService.store  (multipart/form-data)
GET  /api/images/{id}                 → ImageStorageService.retrieve
POST /professional-profile/known-name → SetKnownNameService
```

All existing routes and their behaviour are unchanged.

---

## 5. Client: Image Picker

### Direction

A shared abstraction in `commonMain` bridges platform-specific image picking via standard KMP/CMP mechanisms (`expect`/`actual` or equivalent). The exact API shape is left to the implementation phase to follow current Compose Multiplatform best practices and integrate naturally with Compose lifecycle.

### Platform coverage

| Platform | Mechanism |
|---|---|
| Android | `ActivityResultContracts.PickVisualMedia` (or camera) |
| iOS | `PHPickerViewController` via UIKit interop |
| Web (jsMain) | `<input type="file" accept="image/*">` DOM API |
| Desktop/JVM | Not a target — not implemented |

### Upload flow

After selection, the ViewModel receives a `ByteArray` + MIME type and performs:

```kotlin
val response = apiClient.uploadImage(data, mimeType)   // POST /api/images/upload, multipart/form-data
// response.url = "/api/images/{id}"
```

Upload lives in shared code using Ktor's multipart support (`MultiPartFormDataContent`), which is available in `commonMain`. No new dependency required.

The returned URL is then passed to `POST /auth/photo` or held in ViewModel state for the professional confirm step.

---

## 6. Client: Auth Onboarding Flow

### Step sequence

```
phone → otp → name → photo → done → MainFlow
```

`"profile"` step is renamed to `"name"`. `"photo"` step is new.

### Key rules

- **Both `firstName` and `lastName` are required** in the name step — validated non-empty, trimmed, before the request is sent
- **Photo is optional** — `skipPhoto()` is a first-class, zero-friction path; the skip option is equally prominent to the upload options
- Completing auth onboarding does **not** depend on having a photo
- The photo step is additive enrichment only — no backend gate on `/auth/photo`

### `AuthViewModel` additions

```kotlin
fun submitName(firstName: String, lastName: String)    // POST /auth/profile
fun submitPhoto(data: ByteArray, mimeType: String)     // upload → POST /auth/photo
fun skipPhoto()                                         // advance without photo
```

### Screens (`AuthScreens.kt`)

**`NameInputScreen`** — two `OutlinedTextField`s (First name, Last name), a single "Continue" button. Validates non-empty and trimmed.

**`ProfilePhotoScreen`** (auth variant) — shows `ProfileAvatar` component. Options: pick from library, take photo, skip. On selection: uploads, shows loading state, advances. On skip: advances immediately.

Both screens use `AppTypography`, `AppSpacing`, `AppTheme` — no new theme values.

### `MyProfileScreen`

Updated to two fields (`firstName`, `lastName`) plus existing photo display. Uses the same `submitName` / `submitPhoto` intents.

---

## 7. Client: Professional Onboarding Flow

### State machine extension

`OnboardingUiState` gains two new states appended after `DraftReady`:

```
Idle → Loading → NeedsClarification → DraftReady
    → PhotoRequired → KnownName → Published
```

These new states feel like a natural continuation of the existing flow — same screen container, same ViewModel.

### State transitions

| Transition | Condition |
|---|---|
| `DraftReady → PhotoRequired` | User taps "Continue"; `sessionManager.currentUser.photoUrl == null` |
| `DraftReady → KnownName` | User taps "Continue"; `sessionManager.currentUser.photoUrl != null` (photo already set) |
| `PhotoRequired → KnownName` | User uploads photo successfully; `POST /auth/photo` confirmed |
| `KnownName → Published` | User submits known name (optional) or skips; then `POST /professional-profile/confirm` |

### Key rules

- **`PhotoRequired` has no skip option** — this is a true required gate in the professional flow
- **`KnownName` is optional and lightweight** — skip is clearly available and low-friction
- The `draft` object is threaded through `PhotoRequired` and `KnownName` states so the final confirm call has all necessary data without a separate store
- The `ImageStorageService` abstraction applies here too — the photo upload goes through the same `POST /api/images/upload` endpoint, same mechanism

### `OnboardingViewModel` additions

```kotlin
fun proceedFromDraft(draft: ProfileDraft) {
    val hasPhoto = sessionManager.currentUser.value?.photoUrl != null
    _state.value = if (hasPhoto) KnownName(draft) else PhotoRequired(draft)
}

fun submitPhoto(data: ByteArray, mimeType: String, draft: ProfileDraft)
fun submitKnownName(knownName: String?, draft: ProfileDraft)
```

### New `OnboardingUiState` entries

```kotlin
data class PhotoRequired(val draft: ProfileDraft) : OnboardingUiState()
data class KnownName(val draft: ProfileDraft) : OnboardingUiState()
```

### Screens (`OnboardingScreens.kt`)

**`PhotoRequired` branch** — same `ProfilePhotoScreen` composable from auth onboarding, **no skip option**. Copy: *"Add a profile photo so clients can recognize you."*

**`KnownName` branch** — single `OutlinedTextField` with hint *"e.g. Joãozinho da Tinta"*, a "Continue" button, and a clearly visible "Skip" link.

---

## 8. Search

No changes to the search pipeline in this task. `firstName`, `lastName`, and `knownName` are profile and display data only. The search flow remains focused on service type + locality.

`ProfessionalProfileResponse` exposes all three fields. Callers render `knownName ?: "$firstName $lastName"`. Name-based search can be added later as a deliberate product decision when nominal discovery becomes a core product value.

---

## 9. Testing Expectations

- New Ktor routes (`/auth/photo`, `/api/images/upload`, `/api/images/{id}`, `/professional-profile/known-name`) each require at least one happy-path integration test extending `BaseIntegrationTest`
- `ConfirmProfessionalProfileService` guard (`user.photoUrl != null`) requires an integration test for the rejection case
- `CompleteUserProfileService` requires updated tests for `firstName` / `lastName` fields
- `DatabaseImageStorageService.store` and `retrieve` require integration tests

---

## 10. Out of Scope

- Name-based / nominal search
- Desktop/JVM image picker
- Portfolio photo upload UI (existing emptyList() passthrough preserved)
- WhatsApp phone deduplication (existing hardcode preserved — known issue, separate task)
- S3 or any external file storage implementation
