# Onboarding Refactor Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single `name` field with `firstName`/`lastName`, add temporary image storage behind a replaceable abstraction, extend professional profiles with an optional `knownName`, and restructure both onboarding flows into step-based experiences.

**Architecture:** DB migration first (V8), then shared contracts (source of truth for all three layers), then server domain/services/routes, then client image picker (`expect`/`actual` for Android/iOS/Web), then client onboarding screens/ViewModels wired into the existing state-machine pattern. No new nav library — hand-rolled stack in `App.kt` throughout.

**Tech Stack:** Kotlin, Ktor (server), Exposed (ORM), PostgreSQL + Flyway, Compose Multiplatform, Ktor client, Koin (DI), Kotlin/JS (web), UIKit interop (iOS), `ActivityResultContracts` (Android)

**Spec:** `docs/superpowers/specs/2026-03-14-onboarding-refactor-design.md`

---

## File Map

### Create
| File | Responsibility |
|---|---|
| `server/src/main/resources/db/migration/V8__onboarding_refactor.sql` | Schema: split name, add known_name, add stored_images |
| `shared/src/commonMain/kotlin/.../contract/image/ImageDtos.kt` | `UploadImageResponse` DTO |
| `server/src/main/kotlin/.../infrastructure/images/ImageStorageService.kt` | `ImageStorageService` interface + `StoredImage` data class |
| `server/src/main/kotlin/.../infrastructure/images/DatabaseImageStorageService.kt` | Temporary DB-backed implementation |
| `server/src/main/kotlin/.../images/routing/ImageRoutes.kt` | `POST /api/images/upload`, `GET /api/images/{id}` |
| `server/src/main/kotlin/.../auth/application/SetProfilePhotoService.kt` | Sets `users.photo_url` after validating server-issued URL |
| `server/src/main/kotlin/.../profile/application/SetKnownNameService.kt` | Sets `professional_profiles.known_name` |
| `composeApp/src/commonMain/kotlin/.../platform/ImagePicker.kt` | `expect` declaration for image picker |
| `composeApp/src/androidMain/kotlin/.../platform/ImagePicker.android.kt` | Android `actual` using `PickVisualMedia` |
| `composeApp/src/iosMain/kotlin/.../platform/ImagePicker.ios.kt` | iOS `actual` using `PHPickerViewController` |
| `composeApp/src/jsMain/kotlin/.../platform/ImagePicker.js.kt` | Web `actual` using `<input type="file">` |
| `server/src/test/kotlin/.../integration/images/ImageStorageIntegrationTest.kt` | Integration tests for image upload/retrieve routes |
| `server/src/test/kotlin/.../integration/auth/SetProfilePhotoIntegrationTest.kt` | Integration test for `/auth/photo` |
| `server/src/test/kotlin/.../integration/profile/SetKnownNameIntegrationTest.kt` | Integration test for `/professional-profile/known-name` |
| `server/src/test/kotlin/.../integration/profile/ConfirmProfilePhotoGuardTest.kt` | Integration test for safety-net guard |

### Modify
| File | Change summary |
|---|---|
| `shared/src/commonMain/kotlin/.../contract/auth/AuthDtos.kt` | Replace `CompleteUserProfileRequest(name, photoUrl)` → `(firstName, lastName)`; add `SetProfilePhotoRequest`; update `UserProfileResponse` |
| `shared/src/commonMain/kotlin/.../contract/profile/ProfileDtos.kt` | Replace `ProfessionalProfileResponse.name` → `firstName/lastName/knownName`; remove `photoUrl` from `ConfirmProfessionalProfileRequest`; add `SetKnownNameRequest` |
| `server/src/main/kotlin/.../auth/domain/Models.kt` | `User`: `name: String?` → `firstName: String, lastName: String` |
| `server/src/main/kotlin/.../auth/domain/Repositories.kt` | Replace `updateProfile(id, name, photoUrl)` → `updateName(id, firstName, lastName)` + `updatePhotoUrl(id, photoUrl)` |
| `server/src/main/kotlin/.../auth/infrastructure/ExposedRepositories.kt` | Update `UsersTable`: drop `name`, add `firstName`/`lastName`; update `ExposedUserRepository` |
| `server/src/main/kotlin/.../auth/application/CompleteUserProfileService.kt` | Use `firstName`/`lastName`; call `updateName()`; remove `photoUrl` path |
| `server/src/main/kotlin/.../auth/application/GetAuthenticatedUserService.kt` | Map `user.firstName`/`user.lastName` into response |
| `server/src/main/kotlin/.../auth/routing/AuthRoutes.kt` | Add `POST /auth/photo` route |
| `server/src/main/kotlin/.../profile/domain/Models.kt` | Add `knownName: String?` to `ProfessionalProfile`; add `updateKnownName()` to `ProfessionalProfileRepository` |
| `server/src/main/kotlin/.../profile/infrastructure/persistence/ExposedProfessionalProfileRepository.kt` | Add `knownName` to `ProfessionalProfilesTable`; implement `updateKnownName()` |
| `server/src/main/kotlin/.../profile/application/ProfileServices.kt` | Update `mapToResponse()`; remove `photoUrl` from `ConfirmProfessionalProfileService` + `UpdateProfessionalProfileService`; add photo guard |
| `server/src/main/kotlin/.../profile/routing/ProfileRoutes.kt` | Add `POST /professional-profile/known-name` route |
| `server/src/main/kotlin/.../config/KoinModules.kt` | Register `ImageStorageService`, `SetProfilePhotoService`, `SetKnownNameService` |
| `server/src/main/kotlin/.../Application.kt` | Mount `imageRoutes()` |
| `server/src/test/kotlin/.../integration/BaseIntegrationTest.kt` | Add `StoredImagesTable` to docs note (tests add it in their `tablesToClean`) |
| `server/src/test/kotlin/.../integration/auth/AuthIntegrationTest.kt` | Update `CompleteUserProfileRequest` usages to `firstName`/`lastName` |
| `server/src/test/kotlin/.../profile/application/ProfileServicesTest.kt` | Update `User` construction to `firstName`/`lastName` |
| `composeApp/src/commonMain/kotlin/.../network/FeatureApiClients.kt` | Replace `completeProfile(name, photo)` → `submitName(firstName, lastName)`; add `setProfilePhoto(url)`, `uploadImage(data, mimeType)`, `setKnownName(knownName)` |
| `composeApp/src/commonMain/kotlin/.../screens/AuthViewModel.kt` | Add `PhotoUploadRequired` state; replace `completeProfile` → `submitName`/`submitPhoto`/`skipPhoto` |
| `composeApp/src/commonMain/kotlin/.../screens/AuthScreens.kt` | Rename/rewrite `CompleteUserProfileScreen` → `NameInputScreen`; add `ProfilePhotoScreen` |
| `composeApp/src/commonMain/kotlin/.../screens/OnboardingViewModel.kt` | Add `SessionManager` dep; add `PhotoRequired`/`KnownName` states; replace `confirmProfile` flow |
| `composeApp/src/commonMain/kotlin/.../screens/OnboardingScreens.kt` | Add `PhotoRequired` + `KnownName` branches |
| `composeApp/src/commonMain/kotlin/.../screens/EditProfessionalProfileViewModel.kt` | Remove `photoUrl` parameter from `saveProfile` |
| `composeApp/src/commonMain/kotlin/.../screens/EditProfessionalProfileScreen.kt` | Remove photo URL field |
| `composeApp/src/commonMain/kotlin/.../screens/MyProfileScreen.kt` | Split into `onSaveName` + `onSavePhoto` callbacks; two name fields |
| `composeApp/src/commonMain/kotlin/.../screens/HomeScreen.kt` | `name` → `displayName` (derived from `firstName`/`lastName`) |
| `composeApp/src/commonMain/kotlin/.../screens/SearchScreens.kt` | `profile.name` → display name rule |
| `composeApp/src/commonMain/kotlin/.../screens/ProfileScreens.kt` | `profile.name` → display name rule |
| `composeApp/src/commonMain/kotlin/.../App.kt` | Update `AuthFlow` (new steps); update `MyProfileScreen`, `OnboardingStart`, `EditProfessionalProfile` wiring |
| `composeApp/src/commonMain/kotlin/.../di/Koin.kt` | `OnboardingViewModel(get(), get())` |

---

## Chunk 1: Foundation — Database Migration + Shared Contracts

### Task 1: Write database migration V8

**Files:**
- Create: `server/src/main/resources/db/migration/V8__onboarding_refactor.sql`

- [ ] **Step 1: Create the migration file**

```sql
-- V8__onboarding_refactor.sql

-- users: replace name column with first_name + last_name
ALTER TABLE users DROP COLUMN name;
ALTER TABLE users ADD COLUMN first_name TEXT NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN last_name  TEXT NOT NULL DEFAULT '';

-- professional_profiles: add optional known_name
ALTER TABLE professional_profiles ADD COLUMN known_name TEXT;

-- stored_images: temporary blob storage for profile photos
CREATE TABLE stored_images (
    id           TEXT        PRIMARY KEY,
    data         BYTEA       NOT NULL,
    content_type TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 2: Verify the file is at the right path**

Run: `ls server/src/main/resources/db/migration/`
Expected: V1 through V7 plus the new V8 file listed.

- [ ] **Step 3: Commit**

```bash
git add server/src/main/resources/db/migration/V8__onboarding_refactor.sql
git commit -m "feat: migration V8 — split name, add known_name, stored_images table"
```

---

### Task 2: Update shared auth contracts

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/auth/AuthDtos.kt`

Current content to replace (starting at `CompleteUserProfileRequest` through `UserProfileResponse`):

```kotlin
@Serializable
data class CompleteUserProfileRequest(
    val name: String,
    val photoUrl: String?
)

@Serializable
data class UserProfileResponse(
    val id: String,
    val phoneNumber: String,
    val name: String?,
    val photoUrl: String?,
    val cityName: String?,
    val status: String,
    val hasProfessionalProfile: Boolean = false,
)
```

- [ ] **Step 1: Update `CompleteUserProfileRequest`, add `SetProfilePhotoRequest`, update `UserProfileResponse`**

Replace the two classes above with:

```kotlin
@Serializable
data class CompleteUserProfileRequest(
    val firstName: String,
    val lastName: String,
)

@Serializable
data class SetProfilePhotoRequest(
    val photoUrl: String,
)

@Serializable
data class UserProfileResponse(
    val id: String,
    val phoneNumber: String,
    val firstName: String,
    val lastName: String,
    val photoUrl: String?,
    val cityName: String?,
    val status: String,
    val hasProfessionalProfile: Boolean = false,
)
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/auth/AuthDtos.kt
git commit -m "feat: update AuthDtos — firstName/lastName split, SetProfilePhotoRequest"
```

---

### Task 3: Update shared profile contracts and create ImageDtos

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt`
- Create: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/image/ImageDtos.kt`

- [ ] **Step 1: Update `ConfirmProfessionalProfileRequest` — remove `photoUrl`**

In `ProfileDtos.kt`, replace:
```kotlin
@Serializable
data class ConfirmProfessionalProfileRequest(
    val normalizedDescription: String,
    val selectedServiceIds: List<String>,
    val cityName: String?,
    val neighborhoods: List<String>,
    val contactPhone: String,
    val whatsAppPhone: String?,
    val photoUrl: String?,
    val portfolioPhotoUrls: List<String>
)
```
with:
```kotlin
@Serializable
data class ConfirmProfessionalProfileRequest(
    val normalizedDescription: String,
    val selectedServiceIds: List<String>,
    val cityName: String?,
    val neighborhoods: List<String>,
    val contactPhone: String,
    val whatsAppPhone: String?,
    val portfolioPhotoUrls: List<String>,
)
```

- [ ] **Step 2: Update `ProfessionalProfileResponse` — replace `name` with `firstName`/`lastName`/`knownName`**

Replace:
```kotlin
@Serializable
data class ProfessionalProfileResponse(
    val id: String,
    val name: String?,
    val photoUrl: String?,
    ...
)
```
with:
```kotlin
@Serializable
data class ProfessionalProfileResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val knownName: String?,
    val photoUrl: String?,
    val description: String,
    val cityName: String,
    val neighborhoods: List<String>,
    val services: List<InterpretedServiceDto>,
    val profileComplete: Boolean,
    val activeRecently: Boolean,
    val whatsAppPhone: String?,
    val contactPhone: String,
    val portfolioPhotoUrls: List<String> = emptyList(),
)
```

- [ ] **Step 3: Add `SetKnownNameRequest` to `ProfileDtos.kt`**

Append at the end of the file:
```kotlin
@Serializable
data class SetKnownNameRequest(
    val knownName: String?,
)
```

- [ ] **Step 4: Create `ImageDtos.kt`**

```kotlin
package com.fugisawa.quemfaz.contract.image

import kotlinx.serialization.Serializable

@Serializable
data class UploadImageResponse(val url: String)
```

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/image/ImageDtos.kt
git commit -m "feat: update ProfileDtos — name split, remove photoUrl, add SetKnownNameRequest; add ImageDtos"
```

> **Note on inter-chunk compile breakage:** After this commit, the server module will fail to compile because `ProfessionalProfileResponse` now uses `firstName`/`lastName`/`knownName` but `mapToResponse` in `ProfileServices.kt` still passes the old `name` field. This is expected — the server layer is fixed in Chunk 4 (Task 10). Do not run `:server:test` until after Task 10 is complete.

---

## Chunk 2: Server Image Storage Infrastructure

### Task 4: Image storage abstraction + DB implementation

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/infrastructure/images/ImageStorageService.kt`
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/infrastructure/images/DatabaseImageStorageService.kt`

- [ ] **Step 1: Write the failing integration test** (test-first — forces the interface to be real before wiring)

Create `server/src/test/kotlin/com/fugisawa/quemfaz/integration/images/ImageStorageIntegrationTest.kt`:

```kotlin
package com.fugisawa.quemfaz.integration.images

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.infrastructure.images.StoredImagesTable
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// NOTE: StoredImagesTable is defined in DatabaseImageStorageService.kt (Step 2 of this task).
// Write Step 2 before running this file — it will not compile until StoredImagesTable exists.

class ImageStorageIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = listOf(
        UserPhoneAuthIdentitiesTable,
        UsersTable,
        OtpChallengesTable,
        StoredImagesTable,
    )

    // obtainAuthToken is inherited from BaseIntegrationTest (added in Task 7, Step 7)

    @Test
    fun `should upload image and retrieve it by URL`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000001")
        val client = createTestClient(token)

        val imageBytes = ByteArray(100) { it.toByte() }

        val uploadResponse = client.submitFormWithBinaryData(
            url = "/api/images/upload",
            formData = formData {
                append("image", imageBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"test.png\"")
                })
            }
        )
        assertEquals(HttpStatusCode.OK, uploadResponse.status)

        val body = uploadResponse.bodyAsText()
        assertTrue(body.contains("/api/images/"))

        val url = kotlinx.serialization.json.Json.decodeFromString<com.fugisawa.quemfaz.contract.image.UploadImageResponse>(body).url
        val imageId = url.removePrefix("/api/images/")

        val getResponse = client.get("/api/images/$imageId")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertEquals("image/png", getResponse.headers[HttpHeaders.ContentType])
        val returnedBytes = getResponse.bodyAsBytes()
        assertEquals(100, returnedBytes.size)
    }

    @Test
    fun `should reject upload with disallowed MIME type`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000002")
        val client = createTestClient(token)

        val response = client.submitFormWithBinaryData(
            url = "/api/images/upload",
            formData = formData {
                append("image", ByteArray(10), Headers.build {
                    append(HttpHeaders.ContentType, "image/gif")
                    append(HttpHeaders.ContentDisposition, "filename=\"bad.gif\"")
                })
            }
        )
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `should reject upload exceeding 5MB`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000003")
        val client = createTestClient(token)

        val tooBig = ByteArray(5 * 1024 * 1024 + 1)

        val response = client.submitFormWithBinaryData(
            url = "/api/images/upload",
            formData = formData {
                append("image", tooBig, Headers.build {
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"big.png\"")
                })
            }
        )
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `should return 404 for unknown image ID`() = integrationTestApplication {
        val client = createTestClient()
        val response = client.get("/api/images/nonexistent-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
```

> **Note on `VerifyOtpRequest` field name:** Check the exact field name for the OTP code field in `VerifyOtpRequest` in `shared/contract/auth/AuthDtos.kt` before using — it may be `code` or `otpCode`. Update the helper accordingly.

- [ ] **Step 2: Create `ImageStorageService.kt` (interface + data class)**

```kotlin
package com.fugisawa.quemfaz.infrastructure.images

interface ImageStorageService {
    suspend fun store(data: ByteArray, contentType: String): String  // returns URL like "/api/images/{id}"
    suspend fun retrieve(id: String): StoredImage?
}

data class StoredImage(
    val data: ByteArray,
    val contentType: String,
)
```

- [ ] **Step 3: Create `DatabaseImageStorageService.kt` with Exposed table**

```kotlin
package com.fugisawa.quemfaz.infrastructure.images

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

object StoredImagesTable : Table("stored_images") {
    val id          = varchar("id", 128)
    val data        = blob("data")
    val contentType = varchar("content_type", 128)
    val createdAt   = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

class DatabaseImageStorageService : ImageStorageService {
    override suspend fun store(data: ByteArray, contentType: String): String =
        newSuspendedTransaction {
            val id = UUID.randomUUID().toString()
            StoredImagesTable.insert {
                it[StoredImagesTable.id]          = id
                it[StoredImagesTable.data]        = org.jetbrains.exposed.sql.statements.api.ExposedBlob(data)
                it[StoredImagesTable.contentType] = contentType
                it[StoredImagesTable.createdAt]   = Instant.now()
            }
            "/api/images/$id"
        }

    override suspend fun retrieve(id: String): StoredImage? =
        newSuspendedTransaction {
            StoredImagesTable
                .selectAll()
                .where { StoredImagesTable.id eq id }
                .map { StoredImage(it[StoredImagesTable.data].bytes, it[StoredImagesTable.contentType]) }
                .singleOrNull()
        }
}
```

**Notes:**
- `blob("data")` maps to PostgreSQL `BYTEA` in Exposed 0.60.0 and returns `ExposedBlob`. Use `ExposedBlob(data)` when inserting and `.bytes` when reading.
- `UUID.randomUUID().toString()` matches the existing ID generation pattern throughout the codebase — no ULID dependency exists.

- [ ] **Step 4: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/infrastructure/images/
git add server/src/test/kotlin/com/fugisawa/quemfaz/integration/images/ImageStorageIntegrationTest.kt
git commit -m "feat: add ImageStorageService interface and DatabaseImageStorageService"
```

---

### Task 5: Image routes

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/images/routing/ImageRoutes.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/Application.kt`

- [ ] **Step 1: Create `ImageRoutes.kt`**

```kotlin
package com.fugisawa.quemfaz.images.routing

import com.fugisawa.quemfaz.contract.image.UploadImageResponse
import com.fugisawa.quemfaz.infrastructure.images.ImageStorageService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.koin.ktor.ext.inject

private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
private const val MAX_BYTES = 5 * 1024 * 1024  // 5 MB

fun Route.imageRoutes() {
    val imageStorageService by inject<ImageStorageService>()

    route("/api/images") {
        authenticate("auth-jwt") {
            post("/upload") {
                val multipart = call.receiveMultipart()
                val parts = multipart.readAllParts()
                val filePart = parts.filterIsInstance<PartData.FileItem>().firstOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing image part"))

                val contentType = filePart.contentType?.toString()
                if (contentType !in ALLOWED_CONTENT_TYPES) {
                    filePart.dispose()
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Unsupported image type. Allowed: image/jpeg, image/png, image/webp"),
                    )
                }

                val bytes = filePart.provider().readRemaining().readByteArray()
                filePart.dispose()

                if (bytes.size > MAX_BYTES) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Image exceeds maximum size of 5 MB"),
                    )
                }

                val url = imageStorageService.store(bytes, contentType!!)
                call.respond(UploadImageResponse(url = url))
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val image = imageStorageService.retrieve(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respondBytes(
                bytes = image.data,
                contentType = ContentType.parse(image.contentType),
            )
        }
    }
}
```

- [ ] **Step 2: Mount in `Application.kt`**

In `Application.kt`, add the import and call `imageRoutes()` inside the `routing { ... }` block:

```kotlin
import com.fugisawa.quemfaz.images.routing.imageRoutes
// ...
routing {
    // ...existing routes...
    imageRoutes()
}
```

- [ ] **Step 3: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/images/routing/ImageRoutes.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/Application.kt
git commit -m "feat: add image upload/retrieve routes at /api/images"
```

---

### Task 6: Register image storage in Koin

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt`

- [ ] **Step 1: Add import and registration**

Add import:
```kotlin
import com.fugisawa.quemfaz.infrastructure.images.DatabaseImageStorageService
import com.fugisawa.quemfaz.infrastructure.images.ImageStorageService
```

In `infrastructureModule`, add after the `DataSource` single:
```kotlin
// Image Storage (temporary DB-backed implementation — replace binding to migrate to S3)
single<ImageStorageService> { DatabaseImageStorageService() }
```

- [ ] **Step 2: Run tests to verify image routes compile and start**

```bash
./gradlew :server:test --tests "com.fugisawa.quemfaz.integration.images.ImageStorageIntegrationTest"
```
Expected: tests run (some may fail until wiring is complete — that is expected at this stage).

- [ ] **Step 3: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt
git commit -m "feat: register ImageStorageService in Koin"
```

---

## Chunk 3: Server Auth Layer

### Task 7: Update User domain model and `UserRepository`

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/domain/Models.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/domain/Repositories.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/infrastructure/ExposedRepositories.kt`

- [ ] **Step 1: Update `User` data class in `Models.kt`**

Replace:
```kotlin
data class User(
    val id: UserId,
    val name: String?,
    val photoUrl: String?,
    ...
)
```
with:
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

- [ ] **Step 2: Update `UserRepository` interface in `Repositories.kt`**

Replace:
```kotlin
fun updateProfile(id: UserId, name: String, photoUrl: String?): User?
```
with:
```kotlin
fun updateName(id: UserId, firstName: String, lastName: String): User?
fun updatePhotoUrl(id: UserId, photoUrl: String): User?
```

- [ ] **Step 3: Update `UsersTable` in `ExposedRepositories.kt`**

Replace:
```kotlin
val name = varchar("name", 255).nullable()
```
with:
```kotlin
val firstName = varchar("first_name", 255)
val lastName  = varchar("last_name", 255)
```

- [ ] **Step 4: Update `ExposedUserRepository.create()` in `ExposedRepositories.kt`**

Replace:
```kotlin
it[name] = user.name
```
with:
```kotlin
it[firstName] = user.firstName
it[lastName]  = user.lastName
```

- [ ] **Step 5: Update `ExposedUserRepository.mapUser()` in `ExposedRepositories.kt`**

Replace:
```kotlin
private fun mapUser(it: ResultRow) =
    User(
        id = UserId(it[UsersTable.id]),
        name = it[UsersTable.name],
        ...
    )
```
with:
```kotlin
private fun mapUser(it: ResultRow) =
    User(
        id        = UserId(it[UsersTable.id]),
        firstName = it[UsersTable.firstName],
        lastName  = it[UsersTable.lastName],
        photoUrl  = it[UsersTable.photoUrl],
        status    = it[UsersTable.status],
        createdAt = it[UsersTable.createdAt],
        updatedAt = it[UsersTable.updatedAt],
    )
```

- [ ] **Step 6: Replace `updateProfile()` with `updateName()` and `updatePhotoUrl()` in `ExposedUserRepository`**

Remove the existing `updateProfile` method and add:
```kotlin
override fun updateName(id: UserId, firstName: String, lastName: String): User? =
    transaction {
        UsersTable.update({ UsersTable.id eq id.value }) {
            it[UsersTable.firstName] = firstName
            it[UsersTable.lastName]  = lastName
            it[updatedAt]            = Instant.now()
        }
        findById(id)
    }

override fun updatePhotoUrl(id: UserId, photoUrl: String): User? =
    transaction {
        UsersTable.update({ UsersTable.id eq id.value }) {
            it[UsersTable.photoUrl] = photoUrl
            it[updatedAt]          = Instant.now()
        }
        findById(id)
    }
```

- [ ] **Step 7: Add shared test helpers to `BaseIntegrationTest`**

Multiple integration tests need common setup helpers. Add these `protected` methods to `BaseIntegrationTest.kt`:

```kotlin
// in BaseIntegrationTest — add these protected helpers

protected suspend fun ApplicationTestBuilder.obtainAuthToken(phone: String): String {
    val client = createTestClient()
    client.post("/auth/start-otp") {
        contentType(ContentType.Application.Json)
        setBody(StartOtpRequest(phoneNumber = phone))
    }
    val verifyResponse = client.post("/auth/verify-otp") {
        contentType(ContentType.Application.Json)
        setBody(VerifyOtpRequest(phoneNumber = phone, otpCode = "123456"))
    }
    return verifyResponse.headers[HttpHeaders.Authorization]!!.removePrefix("Bearer ")
}

protected suspend fun ApplicationTestBuilder.completeNameStep(
    token: String,
    firstName: String,
    lastName: String,
) {
    createTestClient(token).post("/auth/profile") {
        contentType(ContentType.Application.Json)
        setBody(CompleteUserProfileRequest(firstName = firstName, lastName = lastName))
    }
}

protected suspend fun ApplicationTestBuilder.setUserPhoto(
    token: String,
    photoUrl: String,
) {
    createTestClient(token).post("/auth/photo") {
        contentType(ContentType.Application.Json)
        setBody(SetProfilePhotoRequest(photoUrl = photoUrl))
    }
}

protected suspend fun ApplicationTestBuilder.createAndConfirmProfile(token: String) {
    val client = createTestClient(token)
    val draftResponse = client.post("/professional-profile/draft") {
        contentType(ContentType.Application.Json)
        setBody(CreateProfessionalProfileDraftRequest(
            inputText = "Pintor residencial em São Paulo",
            inputMode = InputMode.TEXT,
        ))
    }
    val draft = draftResponse.body<CreateProfessionalProfileDraftResponse>()
    client.post("/professional-profile/confirm") {
        contentType(ContentType.Application.Json)
        setBody(ConfirmProfessionalProfileRequest(
            normalizedDescription = draft.normalizedDescription,
            selectedServiceIds = draft.interpretedServices.map { it.serviceId },
            cityName = "São Paulo",
            neighborhoods = emptyList(),
            contactPhone = "+5511999999999",
            whatsAppPhone = "+5511999999999",
            portfolioPhotoUrls = emptyList(),
        ))
    }
}
```

Add the required imports to `BaseIntegrationTest.kt`:
```kotlin
import com.fugisawa.quemfaz.contract.auth.CompleteUserProfileRequest
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.auth.StartOtpRequest
import com.fugisawa.quemfaz.contract.auth.VerifyOtpRequest
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InputMode
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
```

- [ ] **Step 7b: Remove the `private obtainAuthToken` from `ImageStorageIntegrationTest`** — it is now inherited from `BaseIntegrationTest`. Delete the private helper from that class.

- [ ] **Step 8: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/auth/domain/Models.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/auth/domain/Repositories.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/auth/infrastructure/ExposedRepositories.kt
git add server/src/test/kotlin/com/fugisawa/quemfaz/integration/BaseIntegrationTest.kt
git commit -m "refactor: User domain model firstName/lastName; split UserRepository updateName/updatePhotoUrl; add shared test helpers"
```

---

### Task 8: Update auth services and add `SetProfilePhotoService`

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/application/CompleteUserProfileService.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/application/GetAuthenticatedUserService.kt`
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/application/SetProfilePhotoService.kt`

- [ ] **Step 1: Update `CompleteUserProfileService`**

Replace the call to `userRepository.updateProfile(...)` and response construction with:

```kotlin
fun completeProfile(userId: UserId, request: CompleteUserProfileRequest): CompleteProfileResult =
    transaction {
        val user = userRepository.findById(userId)
            ?: return@transaction CompleteProfileResult.Failure("User not found")

        userRepository.updateName(userId, request.firstName.trim(), request.lastName.trim())

        val identity = userPhoneAuthIdentityRepository.findByUserId(userId)
        val profileExists = profileRepository.findByUserId(userId) != null

        CompleteProfileResult.Success(
            UserProfileResponse(
                id = user.id.value,
                phoneNumber = identity?.phoneNumber ?: "unknown",
                firstName = request.firstName.trim(),
                lastName = request.lastName.trim(),
                photoUrl = user.photoUrl,
                cityName = null,
                status = user.status.name,
                hasProfessionalProfile = profileExists,
            ),
        )
    }
```

Also add validation: if `request.firstName.isBlank() || request.lastName.isBlank()`, return `CompleteProfileResult.Failure("First name and last name are required")`.

- [ ] **Step 2: Update `GetAuthenticatedUserService`**

Replace:
```kotlin
return UserProfileResponse(
    ...
    name = user.name,
    ...
)
```
with:
```kotlin
return UserProfileResponse(
    id = user.id.value,
    phoneNumber = phoneIdentity?.phoneNumber ?: "",
    firstName = user.firstName,
    lastName = user.lastName,
    photoUrl = user.photoUrl,
    cityName = null,
    status = user.status.name,
    hasProfessionalProfile = profile != null,
)
```

- [ ] **Step 3: Create `SetProfilePhotoService.kt`**

```kotlin
package com.fugisawa.quemfaz.auth.application

import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository

private val INTERNAL_IMAGE_PATH_REGEX = Regex("^/api/images/[A-Za-z0-9_-]+$")

class SetProfilePhotoService(
    private val userRepository: UserRepository,
    private val phoneAuthRepository: com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository,
    private val profileRepository: ProfessionalProfileRepository,
) {
    fun execute(userId: UserId, request: SetProfilePhotoRequest): SetPhotoResult {
        if (!INTERNAL_IMAGE_PATH_REGEX.matches(request.photoUrl)) {
            return SetPhotoResult.InvalidUrl
        }

        val user = userRepository.findById(userId) ?: return SetPhotoResult.NotFound

        userRepository.updatePhotoUrl(userId, request.photoUrl)

        val identity = phoneAuthRepository.findByUserId(userId)
        val profileExists = profileRepository.findByUserId(userId) != null

        return SetPhotoResult.Success(
            UserProfileResponse(
                id = user.id.value,
                phoneNumber = identity?.phoneNumber ?: "",
                firstName = user.firstName,
                lastName = user.lastName,
                photoUrl = request.photoUrl,
                cityName = null,
                status = user.status.name,
                hasProfessionalProfile = profileExists,
            ),
        )
    }
}

sealed class SetPhotoResult {
    data class Success(val response: UserProfileResponse) : SetPhotoResult()
    object NotFound : SetPhotoResult()
    object InvalidUrl : SetPhotoResult()
}
```

- [ ] **Step 4: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/auth/application/CompleteUserProfileService.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/auth/application/GetAuthenticatedUserService.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/auth/application/SetProfilePhotoService.kt
git commit -m "feat: update auth services for firstName/lastName; add SetProfilePhotoService"
```

---

### Task 9: Add `/auth/photo` route and update Koin

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/auth/routing/AuthRoutes.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt`

- [ ] **Step 1: Add `POST /auth/photo` route in `AuthRoutes.kt`**

Add `SetProfilePhotoService` to the injections:
```kotlin
val setProfilePhotoService by inject<SetProfilePhotoService>()
```

Inside `authenticate("auth-jwt") { ... }`, add after the existing `/profile` handler:
```kotlin
post("/photo") {
    val principal = call.principal<JWTPrincipal>()
    val userId = principal?.payload?.getClaim("userId")?.asString()
        ?: return@post call.respond(HttpStatusCode.Unauthorized)

    val request = call.receive<SetProfilePhotoRequest>()
    when (val result = setProfilePhotoService.execute(UserId(userId), request)) {
        is SetPhotoResult.Success  -> call.respond(result.response)
        is SetPhotoResult.NotFound -> call.respond(HttpStatusCode.NotFound)
        is SetPhotoResult.InvalidUrl -> call.respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "photoUrl must be a server-issued image URL"),
        )
    }
}
```

Also update the import for `CompleteUserProfileRequest` to `SetProfilePhotoRequest` in the route file, and import `SetPhotoResult`.

- [ ] **Step 2: Register in `KoinModules.kt`**

Add import:
```kotlin
import com.fugisawa.quemfaz.auth.application.SetProfilePhotoService
```

In `infrastructureModule` Auth Services section:
```kotlin
single { SetProfilePhotoService(get(), get(), get()) }
```

- [ ] **Step 3: Write integration test for `/auth/photo`**

Create `server/src/test/kotlin/com/fugisawa/quemfaz/integration/auth/SetProfilePhotoIntegrationTest.kt`:

```kotlin
package com.fugisawa.quemfaz.integration.auth

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.infrastructure.images.StoredImagesTable
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SetProfilePhotoIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = listOf(
        UserPhoneAuthIdentitiesTable,
        UsersTable,
        OtpChallengesTable,
        StoredImagesTable,
    )

    @Test
    fun `should set photo URL when given a valid internal URL`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000010")
        // First complete name step
        completeNameStep(token, "João", "Silva")
        val client = createTestClient(token)

        val response = client.post("/auth/photo") {
            contentType(ContentType.Application.Json)
            setBody(SetProfilePhotoRequest(photoUrl = "/api/images/01ABCDEF1234567890ABCDEF"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<UserProfileResponse>()
        assertEquals("/api/images/01ABCDEF1234567890ABCDEF", body.photoUrl)
    }

    @Test
    fun `should reject arbitrary external URL`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000011")
        completeNameStep(token, "Ana", "Souza")
        val client = createTestClient(token)

        val response = client.post("/auth/photo") {
            contentType(ContentType.Application.Json)
            setBody(SetProfilePhotoRequest(photoUrl = "https://evil.com/hack.png"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
```

`obtainAuthToken` and `completeNameStep` are `protected` helpers on `BaseIntegrationTest` — added in Task 7, Step 7. Ensure Task 7 is complete before running these tests.

- [ ] **Step 4: Run auth integration tests**

```bash
./gradlew :server:test --tests "com.fugisawa.quemfaz.integration.auth.*"
```
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/auth/routing/AuthRoutes.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt
git add server/src/test/kotlin/com/fugisawa/quemfaz/integration/auth/SetProfilePhotoIntegrationTest.kt
git commit -m "feat: add POST /auth/photo route and integration test"
```

---

## Chunk 4: Server Profile Layer

### Task 10: Update ProfessionalProfile domain model and repository

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/domain/Models.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/infrastructure/persistence/ExposedProfessionalProfileRepository.kt`

- [ ] **Step 1: Add `knownName` to `ProfessionalProfile` in `Models.kt`**

```kotlin
data class ProfessionalProfile(
    val id: ProfessionalProfileId,
    val userId: UserId,
    val knownName: String?,       // ← add this field (nullable)
    val description: String?,
    val normalizedDescription: String?,
    val contactPhone: String?,
    val whatsappPhone: String?,
    val cityName: String?,
    val neighborhoods: List<String>,
    val services: List<ProfessionalProfileService>,
    val portfolioPhotos: List<PortfolioPhoto>,
    val completeness: ProfileCompleteness,
    val status: ProfessionalProfileStatus,
    val lastActiveAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

- [ ] **Step 2: Add `updateKnownName` to `ProfessionalProfileRepository` interface in `Models.kt`**

```kotlin
interface ProfessionalProfileRepository {
    fun findByUserId(userId: UserId): ProfessionalProfile?
    fun findById(id: ProfessionalProfileId): ProfessionalProfile?
    fun save(profile: ProfessionalProfile): ProfessionalProfile
    fun listPublishedByCity(cityName: String): List<ProfessionalProfile>
    fun search(serviceIds: List<String>, cityName: String?): List<ProfessionalProfile>
    fun updateStatus(id: ProfessionalProfileId, status: ProfessionalProfileStatus): Boolean
    fun updateKnownName(id: ProfessionalProfileId, knownName: String?): Boolean  // ← add
}
```

- [ ] **Step 3: Add `knownName` column to `ProfessionalProfilesTable` in `ExposedProfessionalProfileRepository.kt`**

In `ProfessionalProfilesTable`:
```kotlin
val knownName = varchar("known_name", 255).nullable()
```

- [ ] **Step 4: Update `mapProfile()` in `ExposedProfessionalProfileRepository.kt`** to include `knownName`:

```kotlin
knownName = it[ProfessionalProfilesTable.knownName],
```

- [ ] **Step 5: Implement `updateKnownName()` in `ExposedProfessionalProfileRepository`**

```kotlin
override fun updateKnownName(id: ProfessionalProfileId, knownName: String?): Boolean =
    transaction {
        ProfessionalProfilesTable.update({ ProfessionalProfilesTable.id eq id.value }) {
            it[ProfessionalProfilesTable.knownName] = knownName
            it[updatedAt] = Instant.now()
        } > 0
    }
```

- [ ] **Step 6: Update all `ProfessionalProfile(...)` constructor calls** throughout the codebase to include `knownName = null` (or the actual value). Affected locations: `ConfirmProfessionalProfileService`, `UpdateProfessionalProfileService`, `ExposedProfessionalProfileRepository.save()`.

- [ ] **Step 7: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/domain/Models.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/infrastructure/persistence/ExposedProfessionalProfileRepository.kt
git commit -m "feat: add knownName to ProfessionalProfile domain + repository"
```

---

### Task 11: Update `ProfileServices.kt` — mapToResponse, Confirm, Update

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt`

- [ ] **Step 1: Update `mapToResponse()` signature and body**

Replace:
```kotlin
private fun mapToResponse(
    profile: ProfessionalProfile,
    userName: String?,
    userPhotoUrl: String?,
): ProfessionalProfileResponse
```
with:
```kotlin
private fun mapToResponse(
    profile: ProfessionalProfile,
    firstName: String,
    lastName: String,
    userPhotoUrl: String?,
): ProfessionalProfileResponse =
    ProfessionalProfileResponse(
        id = profile.id.value,
        firstName = firstName,
        lastName = lastName,
        knownName = profile.knownName,
        photoUrl = userPhotoUrl ?: profile.portfolioPhotos.firstOrNull()?.photoUrl,
        description = profile.normalizedDescription ?: "",
        cityName = profile.cityName ?: "",
        neighborhoods = profile.neighborhoods,
        services = profile.services.map { svc ->
            val canonical = CanonicalServices.findById(CanonicalServiceId(svc.serviceId))
            InterpretedServiceDto(svc.serviceId, canonical?.displayName ?: svc.serviceId, svc.matchLevel.name)
        },
        profileComplete = profile.completeness == ProfileCompleteness.COMPLETE,
        activeRecently = profile.lastActiveAt.isAfter(Instant.now().minusSeconds(86400 * 7)),
        whatsAppPhone = profile.whatsappPhone,
        contactPhone = profile.contactPhone ?: "",
    )
```

- [ ] **Step 2: Update all `mapToResponse()` call sites** to pass `user.firstName`, `user.lastName` instead of `user.name`. Update:
  - `ConfirmProfessionalProfileService.execute()` — line `return mapToResponse(savedProfile, user.name, ...)`
  - `UpdateProfessionalProfileService.execute()` — line `return UpdateProfileResult.Success(mapToResponse(saved, user.name, ...))`
  - `GetMyProfessionalProfileService.execute()` — line `return mapToResponse(profile, user?.name, user?.photoUrl)`
  - `GetPublicProfessionalProfileService.execute()` — same

- [ ] **Step 3: Remove `photoUrl` handling from `ConfirmProfessionalProfileService`**

Remove these lines:
```kotlin
// Update user photoUrl if provided and different
if (request.photoUrl != null && request.photoUrl != user.photoUrl) {
    userRepository.updateProfile(userId, user.name ?: "", request.photoUrl)
}
return mapToResponse(savedProfile, user.name, request.photoUrl ?: user.photoUrl)
```
Replace with:
```kotlin
// Safety net: professional profile cannot be confirmed without a photo
require(user.photoUrl != null) { "Profile photo is required to confirm a professional profile" }
return mapToResponse(savedProfile, user.firstName, user.lastName, user.photoUrl)
```

The `require()` call should be placed right after loading the `user` (before creating the profile object), so it fails fast.

- [ ] **Step 4: Remove `photoUrl` handling from `UpdateProfessionalProfileService`**

Remove:
```kotlin
if (request.photoUrl != null && request.photoUrl != user.photoUrl) {
    userRepository.updateProfile(userId, user.name ?: "", request.photoUrl)
}
return UpdateProfileResult.Success(mapToResponse(saved, user.name, request.photoUrl ?: user.photoUrl))
```
Replace with:
```kotlin
return UpdateProfileResult.Success(mapToResponse(saved, user.firstName, user.lastName, user.photoUrl))
```

- [ ] **Step 5: Update remaining services' `mapToResponse` calls**

`GetMyProfessionalProfileService`:
```kotlin
return mapToResponse(profile, user?.firstName ?: "", user?.lastName ?: "", user?.photoUrl)
```

`GetPublicProfessionalProfileService`:
```kotlin
return mapToResponse(profile, user?.firstName ?: "", user?.lastName ?: "", user?.photoUrl)
```

- [ ] **Step 6: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt
git commit -m "refactor: update ProfileServices — firstName/lastName in responses, remove photoUrl handling, add confirm guard"
```

---

### Task 12: Add `SetKnownNameService`, route, and integration tests

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/SetKnownNameService.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/routing/ProfileRoutes.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt`
- Create: `server/src/test/kotlin/com/fugisawa/quemfaz/integration/profile/SetKnownNameIntegrationTest.kt`
- Create: `server/src/test/kotlin/com/fugisawa/quemfaz/integration/profile/ConfirmProfilePhotoGuardTest.kt`

- [ ] **Step 1: Create `SetKnownNameService.kt`**

```kotlin
package com.fugisawa.quemfaz.profile.application

import com.fugisawa.quemfaz.contract.profile.SetKnownNameRequest
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository

class SetKnownNameService(
    private val profileRepository: ProfessionalProfileRepository,
) {
    fun execute(userId: UserId, request: SetKnownNameRequest): SetKnownNameResult {
        val profile = profileRepository.findByUserId(userId)
            ?: return SetKnownNameResult.NotFound

        // Normalize empty string to null
        val knownName = request.knownName?.trim()?.ifBlank { null }

        profileRepository.updateKnownName(profile.id, knownName)
        return SetKnownNameResult.Success
    }
}

sealed class SetKnownNameResult {
    object Success  : SetKnownNameResult()
    object NotFound : SetKnownNameResult()
}
```

- [ ] **Step 2: Add route in `ProfileRoutes.kt`**

Add injection:
```kotlin
val setKnownNameService by inject<SetKnownNameService>()
```

Inside `authenticate("auth-jwt") { ... }` within the `route("/professional-profile")` block, add:
```kotlin
post("/known-name") {
    val principal = call.principal<JWTPrincipal>()
    val userId = principal?.payload?.getClaim("userId")?.asString()
        ?: return@post call.respond(HttpStatusCode.Unauthorized)

    val request = call.receive<SetKnownNameRequest>()
    when (setKnownNameService.execute(UserId(userId), request)) {
        is SetKnownNameResult.Success  -> call.respond(HttpStatusCode.OK, mapOf("success" to true))
        is SetKnownNameResult.NotFound -> call.respond(HttpStatusCode.NotFound)
    }
}
```

- [ ] **Step 3: Register `SetKnownNameService` in `KoinModules.kt`**

```kotlin
import com.fugisawa.quemfaz.profile.application.SetKnownNameService
// ...
single { SetKnownNameService(get()) }
```

- [ ] **Step 4: Write integration test for `/professional-profile/known-name`**

```kotlin
// SetKnownNameIntegrationTest.kt
@Test
fun `should set known name on existing professional profile`() = integrationTestApplication {
    val token = obtainAuthToken("+5511900000020")
    completeNameStep(token, "Maria", "Santos")
    setUserPhoto(token, "/api/images/01VALIDID123456789012345")
    createAndConfirmProfile(token)  // helper that posts a draft and confirms it
    val client = createTestClient(token)

    val response = client.post("/professional-profile/known-name") {
        contentType(ContentType.Application.Json)
        setBody(SetKnownNameRequest(knownName = "Mariazinha"))
    }
    assertEquals(HttpStatusCode.OK, response.status)
}

@Test
fun `should normalize empty known name to null`() = integrationTestApplication {
    val token = obtainAuthToken("+5511900000021")
    completeNameStep(token, "Pedro", "Lima")
    setUserPhoto(token, "/api/images/01VALIDID123456789012346")
    createAndConfirmProfile(token)
    val client = createTestClient(token)

    // Empty string should be accepted and stored as null
    val response = client.post("/professional-profile/known-name") {
        contentType(ContentType.Application.Json)
        setBody(SetKnownNameRequest(knownName = "  "))
    }
    assertEquals(HttpStatusCode.OK, response.status)
}
```

- [ ] **Step 5: Write integration test for the confirm photo guard**

```kotlin
// ConfirmProfilePhotoGuardTest.kt
@Test
fun `should reject confirm when user has no photo`() = integrationTestApplication {
    val token = obtainAuthToken("+5511900000030")
    completeNameStep(token, "Carlos", "Ferreira")
    // Deliberately skip setUserPhoto
    val client = createTestClient(token)

    val draftResponse = client.post("/professional-profile/draft") {
        contentType(ContentType.Application.Json)
        setBody(CreateProfessionalProfileDraftRequest(
            inputText = "Pintor residencial em São Paulo",
            inputMode = InputMode.TEXT,
        ))
    }
    assertEquals(HttpStatusCode.OK, draftResponse.status)
    val draft = draftResponse.body<CreateProfessionalProfileDraftResponse>()

    val confirmResponse = client.post("/professional-profile/confirm") {
        contentType(ContentType.Application.Json)
        setBody(ConfirmProfessionalProfileRequest(
            normalizedDescription = draft.normalizedDescription,
            selectedServiceIds = draft.interpretedServices.map { it.serviceId },
            cityName = "São Paulo",
            neighborhoods = emptyList(),
            contactPhone = "+5511999999999",
            whatsAppPhone = "+5511999999999",
            portfolioPhotoUrls = emptyList(),
        ))
    }
    // Should fail: user has no photo
    assertEquals(HttpStatusCode.InternalServerError, confirmResponse.status)
    // Note: the service throws IllegalArgumentException via require() — map to 400 if desired
    // For now document the expected behavior; refine status code in ProfileRoutes if needed
}
```

- [ ] **Step 6: Run all server tests**

```bash
./gradlew :server:test
```
Expected: all pass.

- [ ] **Step 7: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/SetKnownNameService.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/routing/ProfileRoutes.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt
git add server/src/test/kotlin/com/fugisawa/quemfaz/integration/profile/
git commit -m "feat: SetKnownNameService + /professional-profile/known-name route + integration tests"
```

---

## Chunk 5: Client Image Picker + API Client

### Task 13: `expect` declaration for image picker

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/platform/ImagePicker.kt`

The existing `platform/UrlLauncher.kt` uses a simple top-level `expect fun` pattern. Image picking is more complex because on Android it needs to integrate with Compose's activity result launcher. The cleanest CMP approach is an `expect class` with a factory composable.

- [ ] **Step 1: Create `ImagePicker.kt` in `commonMain`**

```kotlin
package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable

/**
 * Platform-specific image picker.
 * Obtain via [rememberImagePickerLauncher].
 * Call [launch] to trigger native image selection.
 */
expect class ImagePickerLauncher

@Composable
expect fun rememberImagePickerLauncher(
    onImageSelected: (data: ByteArray, mimeType: String) -> Unit,
): ImagePickerLauncher

expect fun ImagePickerLauncher.launch()
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/platform/ImagePicker.kt
git commit -m "feat: add ImagePickerLauncher expect declarations in commonMain"
```

---

### Task 14: Android image picker `actual`

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/fugisawa/quemfaz/platform/ImagePicker.android.kt`

- [ ] **Step 1: Implement Android `actual`**

```kotlin
package com.fugisawa.quemfaz.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual class ImagePickerLauncher(
    private val launchFn: () -> Unit,
)

@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (data: ByteArray, mimeType: String) -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@rememberLauncherForActivityResult
        onImageSelected(bytes, mimeType)
    }
    return remember {
        ImagePickerLauncher {
            launcher.launch(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        }
    }
}

actual fun ImagePickerLauncher.launch() = launchFn()
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/fugisawa/quemfaz/platform/ImagePicker.android.kt
git commit -m "feat: Android actual ImagePickerLauncher using PickVisualMedia"
```

---

### Task 15: iOS image picker `actual`

**Files:**
- Create: `composeApp/src/iosMain/kotlin/com/fugisawa/quemfaz/platform/ImagePicker.ios.kt`

- [ ] **Step 1: Implement iOS `actual` via UIKit interop**

```kotlin
package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.Foundation.NSData
import platform.UniformTypeIdentifiers.UTTypeImage

actual class ImagePickerLauncher(
    private val launchFn: () -> Unit,
)

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (data: ByteArray, mimeType: String) -> Unit,
): ImagePickerLauncher {
    return remember {
        ImagePickerLauncher {
            val config = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
            val picker = PHPickerViewController(configuration = config)
            val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                override fun picker(
                    picker: PHPickerViewController,
                    didFinishPicking: List<*>,
                ) {
                    picker.dismissViewControllerAnimated(true, null)
                    val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
                    result.itemProvider.loadDataRepresentationForTypeIdentifier(
                        UTTypeImage.identifier,
                    ) { data, _ ->
                        if (data != null) {
                            val bytes = data.toByteArray()
                            onImageSelected(bytes, "image/jpeg")
                        }
                    }
                }
            }
            picker.delegate = delegate
            UIApplication.sharedApplication.keyWindow?.rootViewController
                ?.presentViewController(picker, true, null)
        }
    }
}

actual fun ImagePickerLauncher.launch() = launchFn()

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    return ByteArray(size) { i -> bytes?.get(i)?.toByte() ?: 0 }
}
```

**Note:** The iOS implementation may require adjusting the NSData → ByteArray conversion based on the exact cinterop API available. Refer to the existing iOS interop patterns in the codebase and adjust as needed.

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/fugisawa/quemfaz/platform/ImagePicker.ios.kt
git commit -m "feat: iOS actual ImagePickerLauncher using PHPickerViewController"
```

---

### Task 16: Web image picker `actual`

**Files:**
- Create: `composeApp/src/jsMain/kotlin/com/fugisawa/quemfaz/platform/ImagePicker.js.kt`

- [ ] **Step 1: Implement JS `actual` using DOM file input**

```kotlin
package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

actual class ImagePickerLauncher(
    private val launchFn: () -> Unit,
)

@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (data: ByteArray, mimeType: String) -> Unit,
): ImagePickerLauncher {
    return remember {
        ImagePickerLauncher {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = "image/jpeg,image/png,image/webp"
            input.onchange = {
                val file = input.files?.get(0) ?: return@onchange Unit
                val reader = FileReader()
                reader.onload = { event ->
                    val arrayBuffer = reader.result
                    // Convert ArrayBuffer to ByteArray
                    val jsArray = js("new Uint8Array(arrayBuffer)") as ByteArray
                    onImageSelected(jsArray, file.type)
                    Unit
                }
                reader.readAsArrayBuffer(file)
                Unit
            }
            input.click()
        }
    }
}

actual fun ImagePickerLauncher.launch() = launchFn()
```

**Note:** The `js("...")` call for the `Uint8Array` → `ByteArray` conversion is a Kotlin/JS interop idiom. The exact conversion may vary; test on the web target and adjust if needed (alternative: use `Int8Array` + `.asDynamic()`).

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/jsMain/kotlin/com/fugisawa/quemfaz/platform/ImagePicker.js.kt
git commit -m "feat: Web actual ImagePickerLauncher using DOM file input"
```

---

### Task 17: Update `FeatureApiClients`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/network/FeatureApiClients.kt`

- [ ] **Step 1: Add new imports**

```kotlin
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.image.UploadImageResponse
import com.fugisawa.quemfaz.contract.profile.SetKnownNameRequest
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
```

- [ ] **Step 2: Replace `completeProfile()` with `submitName()`**

Replace:
```kotlin
suspend fun completeProfile(request: CompleteUserProfileRequest): UserProfileResponse =
    apiClient.client.post("/auth/profile") { ... }.body()
```
with:
```kotlin
suspend fun submitName(request: CompleteUserProfileRequest): UserProfileResponse =
    apiClient.client.post("/auth/profile") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body()
```

- [ ] **Step 3: Add `setProfilePhoto()`, `uploadImage()`, `setKnownName()`**

```kotlin
suspend fun setProfilePhoto(request: SetProfilePhotoRequest): UserProfileResponse =
    apiClient.client.post("/auth/photo") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body()

suspend fun uploadImage(data: ByteArray, mimeType: String): UploadImageResponse =
    apiClient.client.post("/api/images/upload") {
        setBody(
            MultiPartFormDataContent(
                formData {
                    append(
                        key = "image",
                        value = data,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=\"image\"")
                        },
                    )
                },
            ),
        )
    }.body()

suspend fun setKnownName(request: SetKnownNameRequest): Unit =
    apiClient.client.post("/professional-profile/known-name") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body()
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/network/FeatureApiClients.kt
git commit -m "feat: update FeatureApiClients — submitName, setProfilePhoto, uploadImage, setKnownName"
```

---

## Chunk 6: Client Auth Onboarding

### Task 18: Update `AuthViewModel`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/AuthViewModel.kt`

- [ ] **Step 1: Add `PhotoUploadRequired` to `AuthUiState`**

```kotlin
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class OtpSent(val phone: String) : AuthUiState()
    object ProfileCompletionRequired : AuthUiState()
    object PhotoUploadRequired : AuthUiState()   // ← new
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
```

- [ ] **Step 2: Replace `completeProfile()` with `submitName()`**

Remove `completeProfile(name: String, photoUrl: String?)` and replace with:

```kotlin
fun submitName(firstName: String, lastName: String) {
    if (firstName.isBlank() || lastName.isBlank()) {
        _uiState.value = AuthUiState.Error("First name and last name are required")
        return
    }
    viewModelScope.launch {
        _uiState.value = AuthUiState.Loading
        try {
            val response = apiClients.submitName(
                CompleteUserProfileRequest(firstName.trim(), lastName.trim())
            )
            sessionManager.setCurrentUser(response)
            _uiState.value = AuthUiState.PhotoUploadRequired
        } catch (e: Exception) {
            _uiState.value = AuthUiState.Error(e.message ?: "Failed to save name")
        }
    }
}
```

- [ ] **Step 3: Add `submitPhoto()` and `skipPhoto()`**

```kotlin
fun submitPhoto(data: ByteArray, mimeType: String) {
    viewModelScope.launch {
        _uiState.value = AuthUiState.Loading
        try {
            val uploadResponse = apiClients.uploadImage(data, mimeType)
            val userResponse = apiClients.setProfilePhoto(
                SetProfilePhotoRequest(photoUrl = uploadResponse.url)
            )
            sessionManager.setCurrentUser(userResponse)
            _uiState.value = AuthUiState.Success
        } catch (e: Exception) {
            _uiState.value = AuthUiState.Error(e.message ?: "Failed to upload photo")
        }
    }
}

fun skipPhoto() {
    _uiState.value = AuthUiState.Success
}
```

Remember to add the missing import for `SetProfilePhotoRequest` and `CompleteUserProfileRequest`.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/AuthViewModel.kt
git commit -m "feat: AuthViewModel — submitName/submitPhoto/skipPhoto, PhotoUploadRequired state"
```

---

### Task 19: Add `NameInputScreen` and `ProfilePhotoScreen` to `AuthScreens.kt`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/AuthScreens.kt`

- [ ] **Step 1: Replace `CompleteUserProfileScreen` with `NameInputScreen`**

Find `CompleteUserProfileScreen` and replace with:

```kotlin
@Composable
fun NameInputScreen(
    onSubmitName: (firstName: String, lastName: String) -> Unit,
    uiState: AuthUiState,
) {
    var firstName by remember { mutableStateOf("") }
    var lastName  by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        Text("What's your name?", style = AppTypography.headlineMedium)

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (uiState is AuthUiState.Error) {
            Text(uiState.message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { onSubmitName(firstName, lastName) },
            enabled = firstName.isNotBlank() && lastName.isNotBlank() && uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState is AuthUiState.Loading) CircularProgressIndicator(Modifier.size(20.dp))
            else Text("Continue")
        }
    }
}
```

- [ ] **Step 2: Add `ProfilePhotoScreen`** (used by both auth and professional onboarding — `showSkip` controls whether skip is shown)

The screen takes `isLoading` and `error` directly so it can be driven by either `AuthUiState` or `OnboardingUiState` at the call site.

```kotlin
@Composable
fun ProfilePhotoScreen(
    currentPhotoUrl: String?,
    displayName: String,
    headline: String,
    showSkip: Boolean,
    isLoading: Boolean,
    error: String?,
    onPickImage: () -> Unit,
    onSkip: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        Text(headline, style = AppTypography.headlineMedium)

        ProfileAvatar(
            name = displayName,
            photoUrl = currentPhotoUrl,
            size = 96.dp,
        )

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = onPickImage,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp))
            else Text("Choose photo")
        }

        if (showSkip && onSkip != null) {
            TextButton(onClick = onSkip) {
                Text("Skip for now")
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/AuthScreens.kt
git commit -m "feat: add NameInputScreen and ProfilePhotoScreen to AuthScreens"
```

---

### Task 20: Update `AuthFlow` and `MyProfileScreen` wiring in `App.kt`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/MyProfileScreen.kt`

- [ ] **Step 1: Update `AuthFlow` in `App.kt`**

Replace the entire `AuthFlow` composable:

```kotlin
@Composable
fun AuthFlow(navigateTo: (Screen) -> Unit) {
    val viewModel: AuthViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val sessionManager: SessionManager = koinInject()
    val currentUser by sessionManager.currentUser.collectAsState()

    var currentAuthStep by remember { mutableStateOf("phone") }
    var phoneForOtp by remember { mutableStateOf("") }

    // Image picker wired to AuthViewModel.submitPhoto
    val imagePicker = rememberImagePickerLauncher { data, mimeType ->
        viewModel.submitPhoto(data, mimeType)
    }

    when (currentAuthStep) {
        "phone" -> {
            PhoneLoginScreen(
                onSendOtp = { phoneForOtp = it; viewModel.startOtp(it) },
                uiState = uiState,
            )
            if (uiState is AuthUiState.OtpSent) currentAuthStep = "otp"
        }
        "otp" -> {
            OtpVerificationScreen(
                phone = phoneForOtp,
                onVerifyOtp = { viewModel.verifyOtp(phoneForOtp, it) },
                uiState = uiState,
            )
            if (uiState is AuthUiState.ProfileCompletionRequired) currentAuthStep = "name"
        }
        "name" -> {
            NameInputScreen(
                onSubmitName = { firstName, lastName -> viewModel.submitName(firstName, lastName) },
                uiState = uiState,
            )
            if (uiState is AuthUiState.PhotoUploadRequired) currentAuthStep = "photo"
        }
        "photo" -> {
            val displayName = currentUser?.let { "${it.firstName} ${it.lastName}" } ?: ""
            ProfilePhotoScreen(
                currentPhotoUrl = currentUser?.photoUrl,
                displayName = displayName,
                headline = "Add a profile photo",
                showSkip = true,
                isLoading = uiState is AuthUiState.Loading,
                error = (uiState as? AuthUiState.Error)?.message,
                onPickImage = { imagePicker.launch() },
                onSkip = { viewModel.skipPhoto() },
            )
        }
    }
}
```

Add the import for `rememberImagePickerLauncher` from `com.fugisawa.quemfaz.platform`.

- [ ] **Step 2: Update `MyProfileScreen` wiring in `App.kt`**

Replace:
```kotlin
onSaveProfile = { name, photo -> authViewModel.completeProfile(name, photo) },
```
with:
```kotlin
onSaveName = { firstName, lastName -> authViewModel.submitName(firstName, lastName) },
onSavePhoto = { data, mimeType -> authViewModel.submitPhoto(data, mimeType) },
```

- [ ] **Step 3: Update `MyProfileScreen.kt`** to use `firstName`/`lastName` and split callbacks

In `MyProfileScreen.kt`, update the composable signature and body:

```kotlin
@Composable
fun MyProfileScreen(
    currentUser: UserProfileResponse?,
    uiState: AuthUiState,
    hydrationFailed: Boolean,
    onSaveName: (firstName: String, lastName: String) -> Unit,
    onSavePhoto: (data: ByteArray, mimeType: String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onChangeCity: () -> Unit,
    onManageProfessionalProfile: () -> Unit,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    // Replace single `name` var with two vars:
    var firstName by remember(currentUser?.id) { mutableStateOf(currentUser?.firstName ?: "") }
    var lastName  by remember(currentUser?.id) { mutableStateOf(currentUser?.lastName ?: "") }

    val imagePicker = rememberImagePickerLauncher { data, mimeType ->
        onSavePhoto(data, mimeType)
    }

    // ... rest of the screen using firstName/lastName fields
    // Replace any reference to name/photo with the new fields and callbacks
    // Save name button calls: onSaveName(firstName, lastName)
    // Change photo button calls: imagePicker.launch()
}
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/MyProfileScreen.kt
git commit -m "feat: update AuthFlow with name+photo steps; update MyProfileScreen callbacks"
```

---

## Chunk 7: Client Professional Onboarding + Display Name + Cleanup

### Task 21: Update `OnboardingViewModel`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt`

- [ ] **Step 1: Add new `OnboardingUiState` entries**

```kotlin
sealed class OnboardingUiState {
    object Idle : OnboardingUiState()
    object Loading : OnboardingUiState()
    data class NeedsClarification(
        val originalDescription: String,
        val draft: CreateProfessionalProfileDraftResponse,
    ) : OnboardingUiState()
    data class DraftReady(val draft: CreateProfessionalProfileDraftResponse) : OnboardingUiState()
    data class PhotoRequired(val draft: CreateProfessionalProfileDraftResponse) : OnboardingUiState()   // ← new
    data class KnownName(val draft: CreateProfessionalProfileDraftResponse) : OnboardingUiState()      // ← new
    data class Published(val profile: ProfessionalProfileResponse) : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}
```

- [ ] **Step 2: Add `SessionManager` to constructor and add new functions**

```kotlin
class OnboardingViewModel(
    private val apiClients: FeatureApiClients,
    private val sessionManager: SessionManager,      // ← new parameter
) : ViewModel() {

    // ...existing fields and functions unchanged...

    fun proceedFromDraft(draft: CreateProfessionalProfileDraftResponse) {
        val hasPhoto = sessionManager.currentUser.value?.photoUrl != null
        _uiState.value = if (hasPhoto) OnboardingUiState.KnownName(draft) else OnboardingUiState.PhotoRequired(draft)
    }

    fun submitPhoto(data: ByteArray, mimeType: String, draft: CreateProfessionalProfileDraftResponse) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                val uploadResponse = apiClients.uploadImage(data, mimeType)
                val userResponse = apiClients.setProfilePhoto(
                    SetProfilePhotoRequest(photoUrl = uploadResponse.url)
                )
                sessionManager.setCurrentUser(userResponse)
                _uiState.value = OnboardingUiState.KnownName(draft)
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to upload photo")
            }
        }
    }

    fun submitKnownName(knownName: String?, draft: CreateProfessionalProfileDraftResponse) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                if (!knownName.isNullOrBlank()) {
                    apiClients.setKnownName(SetKnownNameRequest(knownName = knownName.trim()))
                }
                val response = apiClients.confirmProfile(
                    ConfirmProfessionalProfileRequest(
                        normalizedDescription = draft.normalizedDescription,
                        selectedServiceIds = draft.interpretedServices.map { it.serviceId },
                        cityName = draft.cityName,
                        neighborhoods = draft.neighborhoods,
                        contactPhone = "",       // contactPhone is not collected in this flow yet
                        whatsAppPhone = null,
                        portfolioPhotoUrls = emptyList(),
                    )
                )
                _uiState.value = OnboardingUiState.Published(response)
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to publish profile")
            }
        }
    }
}
```

**Note on `contactPhone`:** The existing `confirmProfile` in `OnboardingViewModel` passes `contactPhone` from the `DraftReady` screen. The `DraftReady` state still has the existing UI asking for phone. Keep the `contactPhone` field from the draft flow; update `submitKnownName` to accept it as a parameter or store it from `DraftReady`.

- [ ] **Step 3: Remove the old `confirmProfile()` function** (replaced by `proceedFromDraft` → photo → knownName → confirm chain).

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt
git commit -m "feat: OnboardingViewModel — PhotoRequired/KnownName states, proceedFromDraft, submitPhoto, submitKnownName"
```

---

### Task 22: Update `OnboardingScreens.kt`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt`

- [ ] **Step 1: Add `PhotoRequired` branch**

In the `when (uiState)` block, replace the `DraftReady` confirm button callback and add new states:

In `DraftReady`:
- Replace `onConfirm(...)` button action with `onProceedFromDraft(uiState.draft)`

Add new branches:
```kotlin
is OnboardingUiState.PhotoRequired -> {
    val sessionManager: SessionManager = koinInject()
    val currentUser by sessionManager.currentUser.collectAsState()
    val displayName = currentUser?.let { "${it.firstName} ${it.lastName}" } ?: ""

    // Reuse ProfilePhotoScreen composable from AuthScreens — no skip option here
    ProfilePhotoScreen(
        currentPhotoUrl = currentUser?.photoUrl,
        displayName = displayName,
        headline = "Add a profile photo so clients can recognize you.",
        showSkip = false,
        isLoading = uiState is OnboardingUiState.Loading,
        error = (uiState as? OnboardingUiState.Error)?.message,
        onPickImage = { onPickPhoto(uiState.draft) },
        onSkip = null,
    )
}

is OnboardingUiState.KnownName -> {
    var knownNameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(AppSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        Text("Do you have a known name?", style = AppTypography.headlineMedium)
        Text(
            "If clients know you by a nickname or trade name, enter it here.",
            style = AppTypography.bodyMedium,
        )

        OutlinedTextField(
            value = knownNameInput,
            onValueChange = { knownNameInput = it },
            label = { Text("Known name (optional)") },
            placeholder = { Text("e.g. Joãozinho da Tinta") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Button(
            onClick = { onSubmitKnownName(knownNameInput.trim().ifBlank { null }, uiState.draft) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }

        TextButton(
            onClick = { onSubmitKnownName(null, uiState.draft) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Skip")
        }
    }
}
```

- [ ] **Step 2: Update `OnboardingScreens` composable signature**

Replace `onConfirm` with the new callbacks:
```kotlin
@Composable
fun OnboardingScreens(
    uiState: OnboardingUiState,
    onCreateDraft: (String) -> Unit,
    onProceedFromDraft: (CreateProfessionalProfileDraftResponse) -> Unit,   // replaces onConfirm
    onPickPhoto: (draft: CreateProfessionalProfileDraftResponse) -> Unit,
    onSubmitKnownName: (knownName: String?, draft: CreateProfessionalProfileDraftResponse) -> Unit,
    onSubmitClarifications: (String, List<ClarificationAnswer>) -> Unit,
    onSkipClarification: (CreateProfessionalProfileDraftResponse) -> Unit,
    onFinish: () -> Unit,
)
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt
git commit -m "feat: OnboardingScreens — PhotoRequired and KnownName branches"
```

---

### Task 23: Update `App.kt` professional onboarding wiring

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt`

- [ ] **Step 1: Update `Screen.OnboardingStart` branch in `MainFlow`**

Replace:
```kotlin
is Screen.OnboardingStart -> {
    val viewModel: OnboardingViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    OnboardingScreens(
        uiState = uiState,
        onCreateDraft = { viewModel.createDraft(it) },
        onConfirm = { desc, services, city, neighborhoods, phone, photo ->
            viewModel.confirmProfile(desc, services, city, neighborhoods, phone, photo)
        },
        onSubmitClarifications = { desc, answers ->
            viewModel.submitClarifications(desc, answers)
        },
        onSkipClarification = { draft -> viewModel.skipClarification(draft) },
        onFinish = { navigateToTab(Screen.MyProfile) }
    )
}
```
with:
```kotlin
is Screen.OnboardingStart -> {
    val viewModel: OnboardingViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()

    val imagePicker = rememberImagePickerLauncher { data, mimeType ->
        val draft = (uiState as? OnboardingUiState.PhotoRequired)?.draft ?: return@rememberImagePickerLauncher
        viewModel.submitPhoto(data, mimeType, draft)
    }

    OnboardingScreens(
        uiState = uiState,
        onCreateDraft = { viewModel.createDraft(it) },
        onProceedFromDraft = { draft -> viewModel.proceedFromDraft(draft) },
        onPickPhoto = { _ -> imagePicker.launch() },
        onSubmitKnownName = { knownName, draft -> viewModel.submitKnownName(knownName, draft) },
        onSubmitClarifications = { desc, answers -> viewModel.submitClarifications(desc, answers) },
        onSkipClarification = { draft -> viewModel.skipClarification(draft) },
        onFinish = { navigateToTab(Screen.MyProfile) }
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt
git commit -m "feat: wire OnboardingScreens new callbacks in App.kt"
```

---

### Task 24: Clean up `EditProfessionalProfileViewModel` + screen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt`

- [ ] **Step 1: Remove `photoUrl` parameter from `EditProfessionalProfileViewModel.saveProfile()`**

Remove `photoUrl: String?` from the signature and from the `ConfirmProfessionalProfileRequest(...)` constructor call inside the function.

- [ ] **Step 2: Remove photo URL field from `EditProfessionalProfileScreen`**

Remove any `OutlinedTextField` or UI element that was collecting/displaying `photoUrl` for editing.

- [ ] **Step 3: Update `App.kt` `EditProfessionalProfile` wiring**

Replace:
```kotlin
onSave = { desc, city, neighborhoods, contact, whatsapp, photo ->
    viewModel.saveProfile(desc, city, neighborhoods, contact, whatsapp, photo)
},
```
with:
```kotlin
onSave = { desc, city, neighborhoods, contact, whatsapp ->
    viewModel.saveProfile(desc, city, neighborhoods, contact, whatsapp)
},
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileScreen.kt
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt
git commit -m "refactor: remove dead photoUrl param from EditProfessionalProfileViewModel and screen"
```

---

### Task 25: Update display name at all call sites

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/SearchScreens.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/ProfileScreens.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileScreen.kt`

Display name rule: `knownName ?: "$firstName $lastName"` for `ProfessionalProfileResponse`
User display name: `"${firstName} ${lastName}"` for `UserProfileResponse`

- [ ] **Step 1: Fix `HomeScreen.kt`**

Replace:
```kotlin
name = currentUser?.name,
```
with:
```kotlin
name = currentUser?.let { "${it.firstName} ${it.lastName}" },
```

- [ ] **Step 2: Fix `SearchScreens.kt`**

Replace all:
```kotlin
name = profile.name,
Text(profile.name ?: "Anonymous", ...)
```
with:
```kotlin
name = profile.knownName ?: "${profile.firstName} ${profile.lastName}",
Text(profile.knownName ?: "${profile.firstName} ${profile.lastName}", ...)
```

- [ ] **Step 3: Fix `ProfileScreens.kt`**

Replace all:
```kotlin
profile.name
```
with:
```kotlin
profile.knownName ?: "${profile.firstName} ${profile.lastName}"
```

- [ ] **Step 4: Fix `EditProfessionalProfileScreen.kt`**

Replace:
```kotlin
name = profile.name,
```
with:
```kotlin
name = profile.knownName ?: "${profile.firstName} ${profile.lastName}",
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeScreen.kt
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/SearchScreens.kt
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/ProfileScreens.kt
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileScreen.kt
git commit -m "fix: apply displayName rule (knownName ?: firstName + lastName) at all call sites"
```

---

### Task 26: Update Koin DI and run full server test suite

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/di/Koin.kt`

- [ ] **Step 1: Update `OnboardingViewModel` registration**

Replace:
```kotlin
factory { OnboardingViewModel(get()) }
```
with:
```kotlin
factory { OnboardingViewModel(get(), get()) }
```

- [ ] **Step 2: Update existing tests that construct `User` directly**

In `server/src/test/.../profile/application/ProfileServicesTest.kt`, any test that does:
```kotlin
User(userId, "Jane", null, UserStatus.ACTIVE, ...)
```
must be updated to:
```kotlin
User(userId, firstName = "Jane", lastName = "Doe", photoUrl = null, UserStatus.ACTIVE, ...)
```

Similarly update `AuthIntegrationTest.kt` if it uses `CompleteUserProfileRequest(name = "...")`.

- [ ] **Step 3: Run the full server test suite**

```bash
./gradlew :server:test
```
Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/di/Koin.kt
git commit -m "feat: update Koin — OnboardingViewModel receives SessionManager"
```

- [ ] **Step 5: Final commit — update existing tests for firstName/lastName**

```bash
git add server/src/test/
git commit -m "test: update existing tests for firstName/lastName User model changes"
```

---

## Post-implementation checklist

- [ ] `./gradlew :server:test` — all server integration tests pass
- [ ] Verify app compiles for Android target: `./gradlew :composeApp:assembleDebug`
- [ ] Verify shared module compiles: `./gradlew :shared:build`
- [ ] Confirm `StoredImagesTable` is present in `tablesToClean` for each integration test that touches image data
- [ ] Confirm the `require(user.photoUrl != null)` guard in `ConfirmProfessionalProfileService` surfaces a clear error message (consider mapping `IllegalArgumentException` to `HttpStatusCode.BadRequest` in `ProfileRoutes.kt`)
- [ ] Review the `contactPhone` handling in `OnboardingViewModel.submitKnownName` — the field still needs to reach `ConfirmProfessionalProfileRequest`; thread it through from `DraftReady` state or the existing `confirmProfile` UI
