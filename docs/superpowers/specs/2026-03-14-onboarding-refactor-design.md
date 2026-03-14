# Design Spec — User & Professional Onboarding Refactor

**Date:** 2026-03-14
**Status:** Approved
**Scope:** database schema · shared contracts · server domain/services · client onboarding flows · image storage abstraction · image picker (Android / iOS / Web)

> **Convention note:** All DTOs in `shared/contract/` carry `@Serializable` by convention — snippets throughout this spec omit the annotation for brevity but it is always required.

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

## 2. Database Schema (Migration V8)

File: `V8__onboarding_refactor.sql`

### `users` table

Remove the existing `name` column. Add `first_name` and `last_name`.

```sql
ALTER TABLE users DROP COLUMN name;
ALTER TABLE users ADD COLUMN first_name TEXT NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN last_name  TEXT NOT NULL DEFAULT '';
```

The `DEFAULT ''` is a migration mechanic only (allows the `NOT NULL` constraint during schema change on a development database). The application layer enforces non-empty, trimmed values before any row is written. Existing data does not need to be preserved — development environment.

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
// Name submission (step 2 of auth onboarding) — replaces CompleteUserProfileRequest(name, photoUrl)
data class CompleteUserProfileRequest(
    val firstName: String,
    val lastName: String,
)

// Photo URL assignment (step 3 of auth onboarding — optional for users)
// photoUrl must be a URL issued by our own POST /api/images/upload endpoint
data class SetProfilePhotoRequest(
    val photoUrl: String,
)

// Current user response
data class UserProfileResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val photoUrl: String?,
    // ...existing fields (status, cityName, etc.) unchanged
)
```

`SetProfilePhotoRequest.photoUrl` always contains a URL returned by our own `POST /api/images/upload`. The `/auth/photo` endpoint rejects URLs that do not match the internal path pattern `/api/images/{id}`.

### `ProfileDtos.kt`

```kotlin
// Professional profile response — replaces name with structured fields + knownName
data class ProfessionalProfileResponse(
    // ...existing fields unchanged
    val firstName: String,
    val lastName: String,
    val knownName: String?,   // null if not set
    val photoUrl: String?,
)

// New: set known name during professional onboarding
// knownName: null or absent = no known name; empty string is normalized to null server-side
data class SetKnownNameRequest(
    val knownName: String?,
)

// ConfirmProfessionalProfileRequest: remove the photoUrl field entirely.
// Photo is now set beforehand via POST /auth/photo.
// The field was previously: photoUrl: String?
// It is removed in this change — no replacement within this DTO.
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
    val knownName: String?,   // added; null if not set
    // no photoUrl — stays on User
)
```

### 4.2 Repository Interface Changes

```kotlin
interface UserRepository {
    // Replaces updateProfile(userId, name, photoUrl).
    // Split into two focused methods to match the two separate onboarding steps.
    suspend fun updateName(userId: UserId, firstName: String, lastName: String)
    suspend fun updatePhotoUrl(userId: UserId, photoUrl: String)
    // ...existing methods (findById, etc.) unchanged
}

interface ProfessionalProfileRepository {
    suspend fun updateKnownName(profileId: ProfessionalProfileId, knownName: String?)
    // ...existing methods unchanged
}
```

### 4.3 Image Storage Abstraction

Location: `server/src/main/kotlin/.../infrastructure/images/`

**Accepted MIME types:** `image/jpeg`, `image/png`, `image/webp`
**Maximum upload size:** 5 MB
Requests exceeding these constraints are rejected with `400 Bad Request` before the storage layer is invoked.

```kotlin
interface ImageStorageService {
    suspend fun store(data: ByteArray, contentType: String): String   // returns URL string
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
| `CompleteUserProfileService` | Saves `firstName` + `lastName` via `userRepository.updateName()`. **Remove** any call to update `photoUrl` — that is now handled exclusively by `SetProfilePhotoService`. |
| New `SetProfilePhotoService` | Validates `photoUrl` matches `/api/images/{id}` pattern; saves via `userRepository.updatePhotoUrl()` |
| New `SetKnownNameService` | Normalizes empty string to `null`; calls `professionalProfileRepository.updateKnownName()` |
| `ConfirmProfessionalProfileService` | **Remove** `photoUrl` handling (was `userRepository.updateProfile(..., request.photoUrl)`). Add safety-net guard: `require(user.photoUrl != null)` — this is not the primary enforcement path; see Section 7 for flow enforcement |
| `UpdateProfessionalProfileService` | **Remove** `photoUrl` handling (same pattern as `ConfirmProfessionalProfileService` — lines `if (request.photoUrl != null ...) userRepository.updateProfile(...)` and `request.photoUrl ?: user.photoUrl` in the mapper call). After removal, `mapToResponse` receives `user.photoUrl` directly. |
| All response mappers (`mapToResponse`) | Replace `userName: String?` parameter with `firstName: String, lastName: String`. Add `knownName: String?` sourced from the `ProfessionalProfile` domain model (where it lives). `userPhotoUrl` parameter stays — sourced from `user.photoUrl` at the call site. |

### 4.5 Routes

```
POST /auth/profile                    → CompleteUserProfileService
POST /auth/photo                      → SetProfilePhotoService
POST /api/images/upload               → validate size+type → ImageStorageService.store  (multipart/form-data); /api/images/upload must generate unguessable IDs (like ULID or UUID)
GET  /api/images/{id}                 → ImageStorageService.retrieve
POST /professional-profile/known-name → SetKnownNameService
```

All existing routes and their behaviour are unchanged.

**Note on `GET /api/images/{id}` → `ImageStorageService.retrieve`:** The route handler extracts the bare `id` path parameter and passes it to `retrieve(id)`. The `retrieve` method takes just the ID string, not the full `/api/images/{id}` URL path.

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

Upload lives in shared code using Ktor's multipart support (`MultiPartFormDataContent`), available in `commonMain`. No new dependency required.

The returned URL is then passed to `POST /auth/photo` (auth flow) or held in ViewModel state pending the professional confirm step (professional flow).

---

## 6. Client: Auth Onboarding Flow

### Step sequence

```
phone → otp → name → photo → done → MainFlow
```

`"profile"` step string is renamed to `"name"`. `"photo"` step is new.

### `AuthUiState` additions

The existing auth step mechanism: `App.kt` observes `AuthUiState` and advances the local `currentAuthStep` string. A new state drives the photo step:

```kotlin
sealed class AuthUiState {
    // ...existing entries unchanged
    object ProfileCompletionRequired : AuthUiState()   // triggers "name" step (was "profile")
    object PhotoUploadRequired : AuthUiState()          // new — triggers "photo" step
    object Success : AuthUiState()
    // ...
}
```

`App.kt` wiring — the detection for each step lives inside the preceding step's `when` branch, parallel to the existing pattern where `ProfileCompletionRequired` detection sits inside the `"otp"` branch:

```kotlin
// inside "otp" branch (existing → updated):
if (uiState is AuthUiState.ProfileCompletionRequired) currentAuthStep = "name"

// inside "name" branch (new):
if (uiState is AuthUiState.PhotoUploadRequired) currentAuthStep = "photo"
```

The `"photo"` step completes on `AuthUiState.Success` (same as the existing final step), advancing `App.kt` to `MainFlow`.

### Key rules

- **Both `firstName` and `lastName` are required** in the name step — validated non-empty, trimmed, before the request is sent
- **Photo is optional** — `skipPhoto()` is a first-class, zero-friction path; the skip option is equally prominent to the upload options
- Completing auth onboarding does **not** depend on having a photo
- The photo step is additive enrichment only — no backend gate on `/auth/photo`

### `AuthViewModel` changes

```kotlin
// Replaces: completeProfile(name: String, photoUrl: String?)
// After success: calls fetchCurrentUser() to refresh SessionManager with new firstName/lastName,
// then emits PhotoUploadRequired.
fun submitName(firstName: String, lastName: String)

// upload → POST /auth/photo → updates SessionManager → emits Success
fun submitPhoto(data: ByteArray, mimeType: String)

// emits Success directly, no network call
fun skipPhoto()
```

### Screens (`AuthScreens.kt`)

**`NameInputScreen`** — two `OutlinedTextField`s (First name, Last name), a single "Continue" button. Validates non-empty and trimmed.

**`ProfilePhotoScreen`** (auth variant) — shows `ProfileAvatar` component. Options: pick from library, take photo, skip. On selection: uploads, shows loading state, advances. On skip: advances immediately with no photo.

Both screens use `AppTypography`, `AppSpacing`, `AppTheme` — no new theme values.

### `MyProfileScreen` wiring

The current `onSaveProfile: (String, String?) -> Unit` callback is replaced by two separate callbacks:

```kotlin
onSaveName: (firstName: String, lastName: String) -> Unit
onSavePhoto: (data: ByteArray, mimeType: String) -> Unit
```

The `App.kt` wiring for `MyProfileScreen` updates accordingly. The screen displays the two name fields and the existing photo display — they save independently.

---

## 7. Client: Professional Onboarding Flow

### State machine extension

`OnboardingUiState` gains two new states appended after `DraftReady`:

```
Idle → Loading → NeedsClarification → DraftReady
    → PhotoRequired → KnownName → Published
```

These states are a natural continuation of the existing flow — same screen container, same ViewModel.

### `ProfileDraft` type

Throughout this section, `ProfileDraft` is a shorthand alias for `CreateProfessionalProfileDraftResponse` — the existing type already used by `DraftReady` and `NeedsClarification` states. No new type is introduced.

### State transitions

| Transition | Condition |
|---|---|
| `DraftReady → PhotoRequired` | User taps "Continue"; `sessionManager.currentUser.value?.photoUrl == null` |
| `DraftReady → KnownName` | User taps "Continue"; `sessionManager.currentUser.value?.photoUrl != null` (photo already present) |
| `PhotoRequired → KnownName` | User uploads photo successfully; `POST /auth/photo` confirmed; `SessionManager` updated |
| `KnownName → Published` | User submits known name or skips; `POST /professional-profile/known-name` (if value present); then `POST /professional-profile/confirm` |

### Key rules

- **`PhotoRequired` has no skip option** — this is the primary enforcement of the photo requirement; the backend `require(user.photoUrl != null)` guard in `ConfirmProfessionalProfileService` is a safety net only
- **`KnownName` is optional and lightweight** — skip is clearly available and low-friction
- `CreateProfessionalProfileDraftResponse` is threaded through `PhotoRequired` and `KnownName` states so the final confirm call has all necessary data without a separate store
- Photo upload in this step goes through the same `POST /api/images/upload` endpoint and `ImageStorageService` abstraction as the auth flow

### `OnboardingViewModel` constructor

`OnboardingViewModel` must receive `SessionManager` as an injected dependency (alongside `FeatureApiClients`). It needs `SessionManager` to:
- read `currentUser.value?.photoUrl` in `proceedFromDraft`
- update the session after `submitPhoto` succeeds (via `sessionManager.setCurrentUser(updatedUser)`)

Update the Koin factory registration accordingly: `factory { OnboardingViewModel(get(), get()) }`.

### `OnboardingViewModel` additions

```kotlin
// Called when user taps "Continue" on DraftReady screen
fun proceedFromDraft(draft: CreateProfessionalProfileDraftResponse) {
    val hasPhoto = sessionManager.currentUser.value?.photoUrl != null
    _state.value = if (hasPhoto) KnownName(draft) else PhotoRequired(draft)
}

// submitPhoto is a separate implementation of the same POST /auth/photo call used in AuthViewModel.
// After success it updates SessionManager directly (same pattern as AuthViewModel.fetchCurrentUser),
// then transitions to KnownName(draft).
fun submitPhoto(data: ByteArray, mimeType: String, draft: CreateProfessionalProfileDraftResponse)

fun submitKnownName(knownName: String?, draft: CreateProfessionalProfileDraftResponse)
// On any network failure in submitKnownName or the subsequent confirmProfile call, emit Error(message)
// using the existing OnboardingUiState.Error pattern.
```

### New `OnboardingUiState` entries

```kotlin
data class PhotoRequired(val draft: CreateProfessionalProfileDraftResponse) : OnboardingUiState()
data class KnownName(val draft: CreateProfessionalProfileDraftResponse) : OnboardingUiState()
```

### `App.kt` wiring note

The existing `OnboardingStart` branch in `App.kt` passes an `onConfirm` lambda to `OnboardingScreens`. Under the new flow, the confirm step is no longer a direct callback from the `DraftReady` screen — it happens internally in the ViewModel after the `KnownName` step. The `onConfirm` lambda is replaced by `onProceedFromDraft: (CreateProfessionalProfileDraftResponse) -> Unit`. The `onConfirm` lambda that previously included `photoUrl: String?` is removed.

### Screens (`OnboardingScreens.kt`)

**`PhotoRequired` branch** — same `ProfilePhotoScreen` composable as the auth flow, **no skip option**. Copy: *"Add a profile photo so clients can recognize you."*

**`KnownName` branch** — single `OutlinedTextField` with hint *"e.g. Joãozinho da Tinta"*, a "Continue" button, and a clearly visible "Skip" link.

---

## 8. Search

No changes to the search pipeline in this task. `firstName`, `lastName`, and `knownName` are profile and display data only. The search flow remains focused on service type + locality.

`ProfessionalProfileResponse` exposes all three fields. Callers render `knownName ?: "$firstName $lastName"`. Name-based search can be added later as a deliberate product decision when nominal discovery becomes a core product value.

---

## 9. Testing Expectations

- New Ktor routes (`/auth/photo`, `/api/images/upload`, `/api/images/{id}`, `/professional-profile/known-name`) each require at least one happy-path integration test extending `BaseIntegrationTest`
- `POST /api/images/upload` requires rejection tests for: oversized payload (> 5 MB), disallowed MIME type (e.g. `image/gif`)
- `ConfirmProfessionalProfileService` guard (`user.photoUrl != null`) requires an integration test for the rejection case (attempting to confirm without a photo)
- `CompleteUserProfileService` requires updated tests covering `firstName` / `lastName` fields
- `DatabaseImageStorageService.store` and `retrieve` require integration tests
- `stored_images` must be added to `tablesToClean` in `BaseIntegrationTest` — without this, image-related tests will pollute each other

---

---

## 10. `EditProfessionalProfileScreen` Photo Handling

`EditProfessionalProfileViewModel.saveProfile` currently accepts a `photoUrl: String?` parameter and passes it into `ConfirmProfessionalProfileRequest`. Once `photoUrl` is removed from that DTO, this parameter becomes dead.

**Action required:**
- Remove `photoUrl: String?` from `EditProfessionalProfileViewModel.saveProfile`
- Remove the corresponding argument from the `App.kt` wiring for `Screen.EditProfessionalProfile`
- The photo URL field in `EditProfessionalProfileScreen` UI is removed in this change

Photo editing from the professional profile edit screen is **out of scope for this task**. The user's photo is set during onboarding and via `MyProfileScreen`. A dedicated photo-update path in the edit flow can be added later.

---

## 11. Out of Scope

- Name-based / nominal search
- Desktop/JVM image picker
- Photo editing via `EditProfessionalProfileScreen` (covered in Section 10)
- Portfolio photo upload UI (existing `emptyList()` passthrough preserved)
- WhatsApp phone deduplication (existing hardcode preserved — known issue, separate task)
- S3 or any external file storage implementation
