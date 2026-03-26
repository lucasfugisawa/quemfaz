# City Entity Review Fixes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Address 6 code review findings from the city entity implementation — client-side CityRepository, server-side caching, drop stale column, settings cleanup, extract mapper.

**Architecture:** Six independent changes. Task 1 (server cache) and Task 2 (migration) have no dependencies. Task 3 (ProfileResponseMapper) touches server services. Task 4 (client CityRepository) is the largest — consolidates city data into a shared singleton. Task 5 (settings cleanup) is a one-liner. Task 6 (Koin wiring) ties everything together.

**Tech Stack:** Kotlin, Ktor, Exposed, Koin, Compose Multiplatform, StateFlow, Flyway, JUnit 5

---

## File Structure

| Action | File | Responsibility |
|--------|------|---------------|
| Create | `server/src/main/resources/db/migration/V4__drop_city_name_column.sql` | Drop stale `city_name` column |
| Create | `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileResponseMapper.kt` | Shared profile→response mapping |
| Create | `server/src/test/kotlin/com/fugisawa/quemfaz/city/application/CityServiceTest.kt` | CityService cache unit tests |
| Create | `server/src/test/kotlin/com/fugisawa/quemfaz/profile/application/ProfileResponseMapperTest.kt` | Mapper unit tests |
| Create | `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/data/CityRepository.kt` | Client-side city singleton |
| Modify | `server/src/main/kotlin/com/fugisawa/quemfaz/city/application/CityService.kt` | Add in-memory cache |
| Modify | `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt` | Use mapper, remove private mapToResponse |
| Modify | `server/src/main/kotlin/com/fugisawa/quemfaz/search/application/SearchProfessionalsService.kt` | Use mapper, remove private mapToResponse |
| Modify | `server/src/main/kotlin/com/fugisawa/quemfaz/favorites/application/FavoriteServices.kt` | Use mapper, remove private mapToResponse |
| Modify | `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt` | Wire mapper, update service constructor params |
| Modify | `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeViewModel.kt` | Delegate to CityRepository |
| Modify | `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt` | Delegate to CityRepository |
| Modify | `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt` | Inject CityRepository |
| Modify | `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/di/Koin.kt` | Wire client CityRepository |
| Modify | `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt` | Use CityRepository for city display |
| Modify | `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/session/SessionManager.kt` | Clean up old settings key |

---

### Task 1: Add Server-Side CityService Caching

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/city/application/CityService.kt`
- Create: `server/src/test/kotlin/com/fugisawa/quemfaz/city/application/CityServiceTest.kt`

- [ ] **Step 1: Write the CityService cache unit tests**

Create `server/src/test/kotlin/com/fugisawa/quemfaz/city/application/CityServiceTest.kt`:

```kotlin
package com.fugisawa.quemfaz.city.application

import com.fugisawa.quemfaz.city.domain.CityRepository
import com.fugisawa.quemfaz.contract.city.CityResponse
import com.fugisawa.quemfaz.core.id.CityId
import com.fugisawa.quemfaz.domain.city.City
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CityServiceTest {

    private val batatais = City(
        id = CityId("batatais"),
        name = "Batatais",
        stateCode = "SP",
        country = "BR",
        latitude = -20.8914,
        longitude = -47.5864,
        isActive = true,
    )
    private val franca = City(
        id = CityId("franca"),
        name = "Franca",
        stateCode = "SP",
        country = "BR",
        latitude = -20.5389,
        longitude = -47.4008,
        isActive = true,
    )

    private fun createService(cities: List<City> = listOf(batatais, franca)): Pair<CityService, CountingCityRepository> {
        val repo = CountingCityRepository(cities)
        return CityService(repo) to repo
    }

    @Test
    fun `listActive returns all active cities`() {
        val (service, _) = createService()
        val response = service.listActive()
        assertEquals(2, response.cities.size)
        assertEquals("Batatais", response.cities.first { it.id == "batatais" }.name)
    }

    @Test
    fun `findById returns city from cache`() {
        val (service, repo) = createService()
        val city = service.findById("batatais")
        assertNotNull(city)
        assertEquals("Batatais", city.name)

        // Second call should use cache — repo only called once
        service.findById("batatais")
        assertEquals(1, repo.listActiveCallCount)
    }

    @Test
    fun `findById returns null for unknown id`() {
        val (service, _) = createService()
        assertNull(service.findById("unknown"))
    }

    @Test
    fun `resolveNameFromId returns name for valid id`() {
        val (service, _) = createService()
        assertEquals("Batatais", service.resolveNameFromId("batatais"))
    }

    @Test
    fun `resolveNameFromId returns null for null input`() {
        val (service, _) = createService()
        assertNull(service.resolveNameFromId(null))
    }

    @Test
    fun `resolveNameFromId returns null for unknown id`() {
        val (service, _) = createService()
        assertNull(service.resolveNameFromId("unknown"))
    }

    @Test
    fun `resolveIdFromName returns id for valid name`() {
        val (service, _) = createService()
        assertEquals("batatais", service.resolveIdFromName("Batatais"))
    }

    @Test
    fun `resolveIdFromName is case insensitive`() {
        val (service, _) = createService()
        assertEquals("batatais", service.resolveIdFromName("batatais"))
        assertEquals("batatais", service.resolveIdFromName("BATATAIS"))
    }

    @Test
    fun `resolveIdFromName returns null for null input`() {
        val (service, _) = createService()
        assertNull(service.resolveIdFromName(null))
    }

    @Test
    fun `resolveIdFromName returns null for unknown name`() {
        val (service, _) = createService()
        assertNull(service.resolveIdFromName("Unknown City"))
    }

    @Test
    fun `cache is hit on repeated calls — repository called only once`() {
        val (service, repo) = createService()
        service.findById("batatais")
        service.resolveNameFromId("franca")
        service.resolveIdFromName("Batatais")
        service.listActive()
        assertEquals(1, repo.listActiveCallCount)
    }

    private class CountingCityRepository(
        private val cities: List<City>,
    ) : CityRepository {
        var listActiveCallCount = 0
            private set

        override fun findById(id: String): City? = cities.find { it.id.value == id }
        override fun findByName(name: String): City? = cities.find { it.name.equals(name, ignoreCase = true) }
        override fun listActive(): List<City> {
            listActiveCallCount++
            return cities
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:test --tests "com.fugisawa.quemfaz.city.application.CityServiceTest" -x compileTestKotlin`

Expected: Compilation error or test failures because `CityService` doesn't have a cache yet (tests checking `repo.listActiveCallCount == 1` will fail since current code calls `findById`/`findByName` directly on the repo each time).

- [ ] **Step 3: Implement the cached CityService**

Replace the contents of `server/src/main/kotlin/com/fugisawa/quemfaz/city/application/CityService.kt`:

```kotlin
package com.fugisawa.quemfaz.city.application

import com.fugisawa.quemfaz.city.domain.CityRepository
import com.fugisawa.quemfaz.contract.city.CitiesResponse
import com.fugisawa.quemfaz.contract.city.CityResponse
import com.fugisawa.quemfaz.domain.city.City
import java.time.Duration
import java.time.Instant

class CityService(
    private val cityRepository: CityRepository,
) {
    private val cacheLock = Any()

    @Volatile
    private var cachedCities: List<City> = emptyList()

    @Volatile
    private var cacheTimestamp: Instant = Instant.EPOCH

    private val cacheTtl: Duration = Duration.ofHours(1)

    private fun ensureCache(): List<City> {
        if (cachedCities.isNotEmpty() && Instant.now() < cacheTimestamp.plus(cacheTtl)) {
            return cachedCities
        }
        synchronized(cacheLock) {
            if (cachedCities.isNotEmpty() && Instant.now() < cacheTimestamp.plus(cacheTtl)) {
                return cachedCities
            }
            cachedCities = cityRepository.listActive()
            cacheTimestamp = Instant.now()
            return cachedCities
        }
    }

    fun listActive(): CitiesResponse =
        CitiesResponse(cities = ensureCache().map { it.toResponse() })

    fun findById(id: String): City? =
        ensureCache().find { it.id.value == id }

    fun resolveNameFromId(cityId: String?): String? {
        if (cityId == null) return null
        return findById(cityId)?.name
    }

    fun resolveIdFromName(cityName: String?): String? {
        if (cityName == null) return null
        return ensureCache().find { it.name.equals(cityName, ignoreCase = true) }?.id?.value
    }

    private fun City.toResponse(): CityResponse =
        CityResponse(id = id.value, name = name, stateCode = stateCode)
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:test --tests "com.fugisawa.quemfaz.city.application.CityServiceTest"`

Expected: All 9 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/city/application/CityService.kt \
        server/src/test/kotlin/com/fugisawa/quemfaz/city/application/CityServiceTest.kt
git commit -m "feat: add in-memory cache to CityService

Cities are near-static reference data queried on every profile response.
Cache avoids N+1 DB hits in search/favorites/profile result mapping.
Double-checked locking with 1-hour TTL."
```

---

### Task 2: Drop `city_name` Column (V4 Migration)

**Files:**
- Create: `server/src/main/resources/db/migration/V4__drop_city_name_column.sql`

- [ ] **Step 1: Create the migration file**

Create `server/src/main/resources/db/migration/V4__drop_city_name_column.sql`:

```sql
------------------------------------------------------------
-- Drop the stale city_name column from professional_profiles.
-- Data was backfilled to city_id in V3; city_name is no longer referenced.
------------------------------------------------------------

DROP INDEX IF EXISTS idx_professional_profiles_city_name;
ALTER TABLE professional_profiles DROP COLUMN IF EXISTS city_name;
```

- [ ] **Step 2: Verify compilation still works**

Run: `./gradlew :server:compileKotlin`

Expected: BUILD SUCCESSFUL (migration is SQL-only, no Kotlin changes).

- [ ] **Step 3: Commit**

```bash
git add server/src/main/resources/db/migration/V4__drop_city_name_column.sql
git commit -m "chore: drop stale city_name column from professional_profiles

The city_id FK was added in V3 with backfill. The old city_name column
is no longer referenced by any code."
```

---

### Task 3: Extract ProfileResponseMapper

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileResponseMapper.kt`
- Create: `server/src/test/kotlin/com/fugisawa/quemfaz/profile/application/ProfileResponseMapperTest.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/application/SearchProfessionalsService.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/favorites/application/FavoriteServices.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt`

- [ ] **Step 1: Write the ProfileResponseMapper unit tests**

Create `server/src/test/kotlin/com/fugisawa/quemfaz/profile/application/ProfileResponseMapperTest.kt`:

```kotlin
package com.fugisawa.quemfaz.profile.application

import com.fugisawa.quemfaz.catalog.application.CatalogEntry
import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.city.application.CityService
import com.fugisawa.quemfaz.city.domain.CityRepository
import com.fugisawa.quemfaz.core.id.CityId
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.city.City
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileService
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals

class ProfileResponseMapperTest {

    private val batatais = City(
        id = CityId("batatais"), name = "Batatais", stateCode = "SP",
        country = "BR", latitude = -20.89, longitude = -47.58, isActive = true,
    )

    private val fakeCityRepo = object : CityRepository {
        override fun findById(id: String) = if (id == "batatais") batatais else null
        override fun findByName(name: String) = if (name.equals("Batatais", ignoreCase = true)) batatais else null
        override fun listActive() = listOf(batatais)
    }

    private val cityService = CityService(fakeCityRepo)

    private val pendingEntry = CatalogEntry(
        id = "svc-pending", displayName = "Pending Service", description = "",
        aliases = emptyList(), categoryId = "cat", status = CatalogServiceStatus.PENDING_REVIEW,
    )
    private val activeEntry = CatalogEntry(
        id = "svc-active", displayName = "Active Service", description = "",
        aliases = emptyList(), categoryId = "cat", status = CatalogServiceStatus.ACTIVE,
    )

    private val catalogService: CatalogService = mock<CatalogService>().also {
        whenever(it.findById("svc-pending")).thenReturn(pendingEntry)
        whenever(it.findById("svc-active")).thenReturn(activeEntry)
    }

    private val mapper = ProfileResponseMapper(catalogService, cityService)

    private fun buildProfile(
        services: List<ProfessionalProfileService> = listOf(
            ProfessionalProfileService("svc-pending", ServiceMatchLevel.PRIMARY),
            ProfessionalProfileService("svc-active", ServiceMatchLevel.SECONDARY),
        ),
    ) = ProfessionalProfile(
        id = ProfessionalProfileId("prof-1"),
        userId = UserId("user-1"),
        knownName = "Ana",
        description = "desc",
        normalizedDescription = "desc",
        cityId = "batatais",
        services = services,
        portfolioPhotos = emptyList(),
        completeness = ProfileCompleteness.COMPLETE,
        status = ProfessionalProfileStatus.PUBLISHED,
        lastActiveAt = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test
    fun `toResponse with includeServiceStatus true shows pending_review`() {
        val response = mapper.toResponse(buildProfile(), "Ana Silva", "photo.jpg", "11999", includeServiceStatus = true)

        assertEquals("prof-1", response.id)
        assertEquals("Ana Silva", response.fullName)
        assertEquals("batatais", response.cityId)
        assertEquals("Batatais", response.cityName)

        val pendingSvc = response.services.first { it.serviceId == "svc-pending" }
        assertEquals("pending_review", pendingSvc.status)

        val activeSvc = response.services.first { it.serviceId == "svc-active" }
        assertEquals("active", activeSvc.status)
    }

    @Test
    fun `toResponse with includeServiceStatus false always shows active`() {
        val response = mapper.toResponse(buildProfile(), "Ana Silva", "photo.jpg", "11999", includeServiceStatus = false)

        val pendingSvc = response.services.first { it.serviceId == "svc-pending" }
        assertEquals("active", pendingSvc.status)
    }

    @Test
    fun `toResponse resolves city name from cityId`() {
        val response = mapper.toResponse(buildProfile(), "Ana", null, "")
        assertEquals("Batatais", response.cityName)
    }

    @Test
    fun `toResponse returns empty cityName for unknown cityId`() {
        val profile = buildProfile().copy(cityId = "unknown")
        val response = mapper.toResponse(profile, "Ana", null, "")
        assertEquals("", response.cityName)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail (class doesn't exist)**

Run: `./gradlew :server:compileTestKotlin 2>&1 | head -5`

Expected: Compilation error — `ProfileResponseMapper` not found.

- [ ] **Step 3: Create the ProfileResponseMapper class**

Create `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileResponseMapper.kt`:

```kotlin
package com.fugisawa.quemfaz.profile.application

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.city.application.CityService
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import java.time.Instant
import java.time.temporal.ChronoUnit

class ProfileResponseMapper(
    private val catalogService: CatalogService,
    private val cityService: CityService,
) {
    fun toResponse(
        profile: ProfessionalProfile,
        fullName: String,
        userPhotoUrl: String?,
        phone: String,
        includeServiceStatus: Boolean = false,
    ): ProfessionalProfileResponse =
        ProfessionalProfileResponse(
            id = profile.id.value,
            fullName = fullName,
            knownName = profile.knownName,
            photoUrl = userPhotoUrl ?: profile.portfolioPhotos.firstOrNull()?.photoUrl,
            description = profile.description ?: "",
            cityId = profile.cityId ?: "",
            cityName = cityService.resolveNameFromId(profile.cityId) ?: "",
            services =
                profile.services.map { svc ->
                    val canonical = catalogService.findById(svc.serviceId)
                    val status =
                        if (includeServiceStatus) {
                            when (canonical?.status) {
                                CatalogServiceStatus.PENDING_REVIEW -> "pending_review"
                                else -> "active"
                            }
                        } else {
                            "active"
                        }
                    InterpretedServiceDto(
                        svc.serviceId,
                        canonical?.displayName ?: svc.serviceId,
                        svc.matchLevel.name,
                        status = status,
                    )
                },
            profileComplete = profile.completeness == ProfileCompleteness.COMPLETE,
            activeRecently = profile.lastActiveAt.isAfter(Instant.now().minusSeconds(86400 * 7)),
            phone = phone,
            portfolioPhotoUrls = profile.portfolioPhotos.map { it.photoUrl },
            contactCount = profile.contactClickCount,
            daysSinceActive = ChronoUnit.DAYS.between(profile.lastActiveAt, Instant.now()).toInt(),
        )
}
```

- [ ] **Step 4: Run mapper tests to verify they pass**

Run: `./gradlew :server:test --tests "com.fugisawa.quemfaz.profile.application.ProfileResponseMapperTest"`

Expected: All 4 tests PASS.

- [ ] **Step 5: Refactor ProfileServices.kt to use ProfileResponseMapper**

Replace the contents of `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt`. The key changes:
- Remove `catalogService` and `cityService` from `ConfirmProfessionalProfileService`, `GetMyProfessionalProfileService`, `GetPublicProfessionalProfileService`, `UpdateProfessionalProfileService`
- Add `profileResponseMapper: ProfileResponseMapper` to each
- Replace `mapToResponse(...)` calls with `profileResponseMapper.toResponse(..., includeServiceStatus = true)`
- **Keep `cityService` in `ConfirmProfessionalProfileService` and `UpdateProfessionalProfileService`** for the `INVALID_CITY_ID` validation
- Delete the file-level private `mapToResponse` function at the bottom

```kotlin
package com.fugisawa.quemfaz.profile.application

import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.city.application.CityService
import com.fugisawa.quemfaz.contract.profile.ClarifyDraftRequest
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.PortfolioPhoto
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileService
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import com.fugisawa.quemfaz.profile.interpretation.ProfessionalInputInterpreter
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.UUID

class CreateProfessionalProfileDraftService(
    private val interpreter: ProfessionalInputInterpreter,
) {
    fun execute(
        userId: UserId,
        request: CreateProfessionalProfileDraftRequest,
    ): CreateProfessionalProfileDraftResponse = interpreter.interpret(request.inputText, request.inputMode)
}

class ClarifyProfessionalProfileDraftService(
    private val interpreter: ProfessionalInputInterpreter,
) {
    fun execute(
        userId: UserId,
        request: ClarifyDraftRequest,
    ): CreateProfessionalProfileDraftResponse =
        interpreter.interpretWithClarifications(
            request.originalDescription,
            request.clarificationAnswers,
            request.inputMode,
            request.clarificationRound,
        )
}

class ConfirmProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val cityService: CityService,
    private val profileResponseMapper: ProfileResponseMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(
        userId: UserId,
        request: ConfirmProfessionalProfileRequest,
    ): ProfessionalProfileResponse {
        val user = userRepository.findById(userId) ?: throw IllegalStateException("User not found")
        require(user.photoUrl != null) { "Profile photo is required to confirm a professional profile" }
        requireNotNull(user.dateOfBirth) { "DATE_OF_BIRTH_REQUIRED" }
        require(Period.between(user.dateOfBirth, LocalDate.now()).years >= 18) { "UNDERAGE" }

        if (!request.cityId.isNullOrBlank()) {
            require(cityService.findById(request.cityId!!) != null) { "INVALID_CITY_ID" }
        }

        val existingProfile = profileRepository.findByUserId(userId)

        val profileId = existingProfile?.id ?: ProfessionalProfileId(UUID.randomUUID().toString())

        val services =
            request.selectedServiceIds.map { serviceId ->
                ProfessionalProfileService(serviceId, ServiceMatchLevel.PRIMARY)
            }

        val portfolioPhotos =
            request.portfolioPhotoUrls.map { url ->
                PortfolioPhoto(UUID.randomUUID().toString(), url, null, Instant.now())
            }

        val completeness =
            if (
                request.description.isNotBlank() &&
                request.selectedServiceIds.isNotEmpty() &&
                !request.cityId.isNullOrBlank()
            ) {
                ProfileCompleteness.COMPLETE
            } else {
                ProfileCompleteness.INCOMPLETE
            }

        val profile =
            ProfessionalProfile(
                id = profileId,
                userId = userId,
                knownName = null,
                description = request.description,
                normalizedDescription = request.description,
                cityId = request.cityId,
                services = services,
                portfolioPhotos = portfolioPhotos,
                completeness = completeness,
                status = ProfessionalProfileStatus.PUBLISHED,
                lastActiveAt = Instant.now(),
                createdAt = existingProfile?.createdAt ?: Instant.now(),
                updatedAt = Instant.now(),
            )

        val savedProfile = profileRepository.save(profile)

        val phone = phoneAuthRepository.findByUserId(userId)?.phoneNumber ?: ""
        return profileResponseMapper.toResponse(savedProfile, user.fullName, user.photoUrl, phone, includeServiceStatus = true)
    }
}

class GetMyProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val profileResponseMapper: ProfileResponseMapper,
) {
    fun execute(userId: UserId): ProfessionalProfileResponse? {
        val profile = profileRepository.findByUserId(userId) ?: return null
        val user = userRepository.findById(userId)
        val phone = phoneAuthRepository.findByUserId(userId)?.phoneNumber ?: ""
        return profileResponseMapper.toResponse(profile, user?.fullName ?: "", user?.photoUrl, phone, includeServiceStatus = true)
    }
}

class GetPublicProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val profileResponseMapper: ProfileResponseMapper,
) {
    fun execute(profileId: ProfessionalProfileId): ProfessionalProfileResponse? {
        val profile = profileRepository.findById(profileId) ?: return null
        if (profile.status != ProfessionalProfileStatus.PUBLISHED) return null

        val user = userRepository.findById(profile.userId)
        val phone = phoneAuthRepository.findByUserId(profile.userId)?.phoneNumber ?: ""
        return profileResponseMapper.toResponse(profile, user?.fullName ?: "", user?.photoUrl, phone, includeServiceStatus = true)
    }
}

sealed class DisableProfileResult {
    object Success : DisableProfileResult()

    object NotFound : DisableProfileResult()

    object AlreadyInactive : DisableProfileResult()
}

class DisableProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
) {
    fun execute(userId: UserId): DisableProfileResult {
        val existing = profileRepository.findByUserId(userId) ?: return DisableProfileResult.NotFound
        if (existing.status == ProfessionalProfileStatus.INACTIVE) return DisableProfileResult.AlreadyInactive

        profileRepository.updateStatus(existing.id, ProfessionalProfileStatus.INACTIVE)
        return DisableProfileResult.Success
    }
}

sealed class UpdateProfileResult {
    data class Success(
        val response: ProfessionalProfileResponse,
    ) : UpdateProfileResult()

    object NotFound : UpdateProfileResult()

    object Blocked : UpdateProfileResult()
}

class UpdateProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val cityService: CityService,
    private val profileResponseMapper: ProfileResponseMapper,
) {
    fun execute(
        userId: UserId,
        request: ConfirmProfessionalProfileRequest,
    ): UpdateProfileResult {
        val existing = profileRepository.findByUserId(userId) ?: return UpdateProfileResult.NotFound
        if (existing.status == ProfessionalProfileStatus.BLOCKED) return UpdateProfileResult.Blocked

        if (!request.cityId.isNullOrBlank()) {
            require(cityService.findById(request.cityId!!) != null) { "INVALID_CITY_ID" }
        }

        val user = userRepository.findById(userId) ?: return UpdateProfileResult.NotFound

        val services =
            request.selectedServiceIds.map { serviceId ->
                ProfessionalProfileService(serviceId, ServiceMatchLevel.PRIMARY)
            }

        val portfolioPhotos =
            request.portfolioPhotoUrls.map { url ->
                PortfolioPhoto(UUID.randomUUID().toString(), url, null, Instant.now())
            }

        val completeness =
            if (
                request.description.isNotBlank() &&
                request.selectedServiceIds.isNotEmpty() &&
                !request.cityId.isNullOrBlank()
            ) {
                ProfileCompleteness.COMPLETE
            } else {
                ProfileCompleteness.INCOMPLETE
            }

        // Auto-disable when all services are removed; auto-reactivate when services are added back.
        val newStatus =
            when {
                services.isEmpty() -> ProfessionalProfileStatus.INACTIVE
                existing.status == ProfessionalProfileStatus.INACTIVE && services.isNotEmpty() -> ProfessionalProfileStatus.PUBLISHED
                else -> existing.status
            }

        val updated =
            existing.copy(
                description = request.description,
                normalizedDescription = request.description,
                cityId = request.cityId,
                services = services,
                portfolioPhotos = portfolioPhotos,
                completeness = completeness,
                status = newStatus,
                lastActiveAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        val saved = profileRepository.save(updated)

        val phone = phoneAuthRepository.findByUserId(userId)?.phoneNumber ?: ""
        return UpdateProfileResult.Success(profileResponseMapper.toResponse(saved, user.fullName, user.photoUrl, phone, includeServiceStatus = true))
    }
}
```

- [ ] **Step 6: Refactor SearchProfessionalsService to use ProfileResponseMapper**

In `server/src/main/kotlin/com/fugisawa/quemfaz/search/application/SearchProfessionalsService.kt`:
- Replace `catalogService` and `cityService` constructor params (used only for mapping) with `profileResponseMapper: ProfileResponseMapper`. **Keep `cityService`** because it's also used for city resolution in the search flow (lines 45-50).
- Replace `mapToResponse(...)` call on line 122 with `profileResponseMapper.toResponse(...)`
- Delete the private `mapToResponse` function (lines 132-157)
- Remove unused imports: `InterpretedServiceDto`, `ProfileCompleteness`, `ChronoUnit`

The constructor becomes:
```kotlin
class SearchProfessionalsService(
    private val interpreter: SearchQueryInterpreter,
    private val rankingService: ProfessionalSearchRankingService,
    private val searchQueryRepository: SearchQueryRepository,
    private val searchEventRepository: SearchEventRepository,
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val cityService: CityService,
    private val profileResponseMapper: ProfileResponseMapper,
)
```

And the mapping call on line 122 becomes:
```kotlin
profileResponseMapper.toResponse(profile, user?.fullName ?: "", user?.photoUrl, phone)
```

Note: `catalogService` import and field are removed. The `InterpretedServiceDto` for `interpretedServices` (line 116) still needs `catalogService` — **wait, check this**. Let me re-read line 113-117:

```kotlin
interpretedServices =
    interpreted.serviceIds.map { serviceId ->
        val canonical = catalogService.findById(serviceId)
        InterpretedServiceDto(serviceId, canonical?.displayName ?: serviceId, "PRIMARY")
    },
```

This uses `catalogService` directly for search response metadata, not for profile mapping. So `catalogService` must remain in the constructor. The updated constructor:

```kotlin
class SearchProfessionalsService(
    private val interpreter: SearchQueryInterpreter,
    private val rankingService: ProfessionalSearchRankingService,
    private val searchQueryRepository: SearchQueryRepository,
    private val searchEventRepository: SearchEventRepository,
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val catalogService: CatalogService,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val cityService: CityService,
    private val profileResponseMapper: ProfileResponseMapper,
)
```

Delete only the private `mapToResponse` function and replace its call with the mapper. The constructor gains `profileResponseMapper` (10 params total, was 9). Remove the `ProfileCompleteness` and `ChronoUnit` imports that were only used by the deleted function.

- [ ] **Step 7: Refactor FavoriteServices to use ProfileResponseMapper**

In `server/src/main/kotlin/com/fugisawa/quemfaz/favorites/application/FavoriteServices.kt`, change `ListFavoritesService`:
- Replace `catalogService: CatalogService` and `cityService: CityService` with `profileResponseMapper: ProfileResponseMapper`
- Replace `mapToResponse(...)` call with `profileResponseMapper.toResponse(...)`
- Delete the private `mapToResponse` function
- Remove unused imports

```kotlin
class ListFavoritesService(
    private val favoriteRepository: FavoriteRepository,
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val profileResponseMapper: ProfileResponseMapper,
) {
    fun execute(userId: UserId): FavoritesListResponse {
        val favorites = favoriteRepository.listByUserId(userId)
        val profiles =
            favorites.mapNotNull { fav ->
                val profile = profileRepository.findById(fav.professionalProfileId)
                if (profile != null && profile.status == ProfessionalProfileStatus.PUBLISHED) {
                    val user = userRepository.findById(profile.userId)
                    val phone = phoneAuthRepository.findByUserId(profile.userId)?.phoneNumber ?: ""
                    profileResponseMapper.toResponse(profile, user?.fullName ?: "", user?.photoUrl, phone)
                } else {
                    null
                }
            }
        return FavoritesListResponse(profiles)
    }
}
```

Remove these imports from the file (only used by the deleted mapper):
- `com.fugisawa.quemfaz.catalog.application.CatalogService`
- `com.fugisawa.quemfaz.city.application.CityService`
- `com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto`
- `com.fugisawa.quemfaz.profile.domain.ProfileCompleteness`
- `java.time.temporal.ChronoUnit`

Keep: `ProfessionalProfileStatus` (used in the `if` check), `Instant` (used for Favorite creation in AddFavoriteService).

- [ ] **Step 8: Update KoinModules to wire ProfileResponseMapper**

In `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt`:

Add import:
```kotlin
import com.fugisawa.quemfaz.profile.application.ProfileResponseMapper
```

Add mapper registration after the CityService line (around line 159):
```kotlin
single { ProfileResponseMapper(get(), get()) }
```

Update profile service registrations (lines 179-182):
```kotlin
single { ConfirmProfessionalProfileService(get(), get(), get(), get(), get()) }
single { GetMyProfessionalProfileService(get(), get(), get(), get()) }
single { GetPublicProfessionalProfileService(get(), get(), get(), get()) }
single { UpdateProfessionalProfileService(get(), get(), get(), get(), get()) }
```

Update search service registration (line 197):
```kotlin
single { SearchProfessionalsService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
```

Update favorites service registration (line 204):
```kotlin
single { ListFavoritesService(get(), get(), get(), get(), get()) }
```

- [ ] **Step 9: Compile and run all server tests**

Run: `./gradlew :server:compileKotlin :server:compileTestKotlin`

Expected: BUILD SUCCESSFUL

Then: `./gradlew :server:test`

Expected: 72 tests, same 13 pre-existing failures. No new failures.

- [ ] **Step 10: Run formatKotlin**

Run: `./gradlew formatKotlin`

If any files changed, stage them.

- [ ] **Step 11: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileResponseMapper.kt \
        server/src/test/kotlin/com/fugisawa/quemfaz/profile/application/ProfileResponseMapperTest.kt \
        server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt \
        server/src/main/kotlin/com/fugisawa/quemfaz/search/application/SearchProfessionalsService.kt \
        server/src/main/kotlin/com/fugisawa/quemfaz/favorites/application/FavoriteServices.kt \
        server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt
git commit -m "refactor: extract ProfileResponseMapper to eliminate 3-way duplication

Consolidates profile→response mapping from ProfileServices,
SearchProfessionalsService, and FavoriteServices into a single class.
includeServiceStatus flag handles the ProfileServices variant that
shows pending_review status."
```

---

### Task 4: Create Client-Side CityRepository

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/data/CityRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/di/Koin.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt`

- [ ] **Step 1: Create the CityRepository class**

Create `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/data/CityRepository.kt`:

```kotlin
package com.fugisawa.quemfaz.data

import com.fugisawa.quemfaz.contract.city.CityResponse
import com.fugisawa.quemfaz.network.FeatureApiClients
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CitiesLoadState { Idle, Loading, Loaded, Error }

class CityRepository(
    private val apiClients: FeatureApiClients,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _cities = MutableStateFlow<List<CityResponse>>(emptyList())
    val cities: StateFlow<List<CityResponse>> = _cities.asStateFlow()

    private val _loadState = MutableStateFlow(CitiesLoadState.Idle)
    val loadState: StateFlow<CitiesLoadState> = _loadState.asStateFlow()

    init {
        loadCities()
    }

    fun loadCities() {
        if (_loadState.value == CitiesLoadState.Loading) return
        scope.launch {
            _loadState.value = CitiesLoadState.Loading
            try {
                _cities.value = apiClients.getCities().cities
                _loadState.value = CitiesLoadState.Loaded
            } catch (_: Exception) {
                _loadState.value = CitiesLoadState.Error
            }
        }
    }

    fun getCityDisplayName(cityId: String?): String? =
        if (cityId == null) null else _cities.value.find { it.id == cityId }?.name
}
```

- [ ] **Step 2: Register CityRepository in Koin**

In `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/di/Koin.kt`, add:

Import:
```kotlin
import com.fugisawa.quemfaz.data.CityRepository
```

After `single { CatalogApiClient(get<ApiClient>().client) }` (line 31), add:
```kotlin
single { CityRepository(get()) }
```

Update ViewModel factories:
```kotlin
factory { HomeViewModel(get(), get(), get(), get()) }
factory { OnboardingViewModel(get(), get(), get(), get()) }
factory { EditProfessionalProfileViewModel(get(), get(), get()) }
```

- [ ] **Step 3: Update HomeViewModel to delegate to CityRepository**

In `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeViewModel.kt`:

Add constructor param:
```kotlin
class HomeViewModel(
    private val apiClients: FeatureApiClients,
    private val sessionManager: SessionManager,
    private val catalogApiClient: CatalogApiClient,
    val cityRepository: com.fugisawa.quemfaz.data.CityRepository,
) : ViewModel() {
```

Remove these lines:
- Lines 88-89: `private val _cities` and `val cities` StateFlow declarations
- Lines 114-120: the `loadCities()` function
- Line 126: the `loadCities()` call in `init`
- Lines 146-147: the `getCityDisplayName()` function

Remove unused import: `com.fugisawa.quemfaz.contract.city.CityResponse`

- [ ] **Step 4: Update OnboardingViewModel to delegate to CityRepository**

In `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt`:

Add constructor param:
```kotlin
class OnboardingViewModel(
    private val apiClients: FeatureApiClients,
    private val sessionManager: SessionManager,
    private val catalogApiClient: CatalogApiClient,
    val cityRepository: com.fugisawa.quemfaz.data.CityRepository,
) : ViewModel() {
```

Remove these lines:
- Lines 77-78: `private val _cities` and `val cities` StateFlow declarations
- Lines 91-95: the cities loading coroutine in `init`
- Lines 108-109: the `getCityDisplayName()` function

Remove unused import: `com.fugisawa.quemfaz.contract.city.CityResponse`

- [ ] **Step 5: Update EditProfessionalProfileViewModel**

In `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt`:

Add constructor param:
```kotlin
class EditProfessionalProfileViewModel(
    private val apiClients: FeatureApiClients,
    private val catalogApiClient: CatalogApiClient,
    val cityRepository: com.fugisawa.quemfaz.data.CityRepository,
) : ViewModel() {
```

- [ ] **Step 6: Update App.kt to use CityRepository**

In `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt`:

**Line 251** — change city collection from HomeViewModel to CityRepository:
```kotlin
// Before:
val cities by homeViewModel.cities.collectAsState()
// After:
val cities by homeViewModel.cityRepository.cities.collectAsState()
```

**Line 340** — fix reactive city display for HomeScreen:
```kotlin
// Before:
currentCity = homeViewModel.getCityDisplayName(currentCityId),
// After:
currentCity = homeViewModel.cityRepository.getCityDisplayName(currentCityId),
```

**Line 461** — change onboarding cities collection:
```kotlin
// Before:
val onboardingCities by viewModel.cities.collectAsState()
// After:
val onboardingCities by viewModel.cityRepository.cities.collectAsState()
```

**Line 486** — fix reactive city display for OnboardingScreens:
```kotlin
// Before:
selectedCityDisplayName = viewModel.getCityDisplayName(selectedCityId),
// After:
selectedCityDisplayName = viewModel.cityRepository.getCityDisplayName(selectedCityId),
```

**Line 526** — EditProfessionalProfile city collection:
```kotlin
// Before:
val editCatalog by viewModel.catalog.collectAsState()
// After:
val editCatalog by viewModel.catalog.collectAsState()
val editCities by viewModel.cityRepository.cities.collectAsState()
```

**Line 534** — pass editCities instead of homeViewModel's cities:
```kotlin
// Before:
cities = cities,
// After:
cities = editCities,
```

- [ ] **Step 7: Compile**

Run: `./gradlew :composeApp:compileKotlinDesktop` (or whatever the available target is)

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/data/CityRepository.kt \
        composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeViewModel.kt \
        composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt \
        composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt \
        composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/di/Koin.kt \
        composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt
git commit -m "refactor: extract client-side CityRepository singleton

Consolidates city loading from HomeViewModel and OnboardingViewModel
into a shared CityRepository singleton. Fixes non-reactive
getCityDisplayName — city names now update when cities finish loading.
Exposes loadState for future retry UI."
```

---

### Task 5: Clean Up Old Settings Key

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/session/SessionManager.kt`

- [ ] **Step 1: Add cleanup to SessionManager init**

In `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/session/SessionManager.kt`, add at the beginning of the `init` block (before `val token = ...`):

```kotlin
init {
    // One-time cleanup: remove stale key from pre-city-entity versions.
    settings.remove("current_city")

    val token = sessionStorage.getToken()
    if (token != null) {
        _authState.value = AuthState.Authenticated
    } else {
        _authState.value = AuthState.Unauthenticated
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :composeApp:compileKotlinDesktop`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/session/SessionManager.kt
git commit -m "chore: remove orphaned current_city settings key

Previous version stored city name under 'current_city'. The city entity
migration changed to 'current_city_id'. Clean up the stale key on startup."
```

---

### Task 6: Final Verification

**Files:** None (verification only)

- [ ] **Step 1: Run full server compilation and tests**

Run: `./gradlew :server:compileKotlin :server:compileTestKotlin`

Expected: BUILD SUCCESSFUL

Run: `./gradlew :server:test`

Expected: 72+ tests, same 13 pre-existing failures. No new failures.

- [ ] **Step 2: Run lintKotlin and formatKotlin**

Run: `./gradlew lintKotlin`

Expected: BUILD SUCCESSFUL

Run: `./gradlew formatKotlin`

If any files changed, stage and commit as `style: apply formatKotlin fixes`.

- [ ] **Step 3: Compile client**

Run: `./gradlew :composeApp:compileKotlinDesktop` (or `:composeApp:compileKotlinAndroidDebug`)

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Verify git status is clean**

Run: `git status`

Expected: Only untracked files (`.claude/`, `ui-report.md`, plan docs). No unstaged changes.
