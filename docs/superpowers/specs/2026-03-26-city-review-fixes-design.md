# City Entity — Code Review Fixes

## Goal

Address 7 findings from the city entity code review: fix reactive city display on the client, add server-side caching, drop stale DB column, extract duplicated mapper, and clean up orphaned settings key.

## Architecture

Six targeted changes across client (`composeApp`), server, and DB migration layers. Each fix is independent and can be implemented/tested in isolation. The client-side `CityRepository` is the largest change — it consolidates duplicate city-loading logic from two ViewModels into a shared singleton.

## Changes

### 1. Client-Side CityRepository (fixes review items 1, 2, 4)

**Problem:** `HomeViewModel` and `OnboardingViewModel` independently load cities in `init`, each swallowing errors silently. `getCityDisplayName()` is a plain function reading `_cities.value` — not reactive, returns `null` before cities finish loading.

**Solution:** Extract a `CityRepository` singleton that owns city data on the client.

**File:** `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/data/CityRepository.kt`

```kotlin
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

**ViewModel changes:**

- `HomeViewModel`: Remove `_cities`, `cities`, `loadCities()`, `getCityDisplayName()`. Add constructor param `val cityRepository: CityRepository`. Expose `cityRepository.cities` and `cityRepository.loadState` to the UI. Callers use `cityRepository.getCityDisplayName(...)`.
- `OnboardingViewModel`: Same — remove duplicate fields, inject `CityRepository`.
- `EditProfessionalProfileViewModel`: Add `CityRepository` as a constructor param. Expose `cityRepository.cities` so the screen can observe it directly. Remove the `cities: List<CityResponse>` parameter from `EditProfessionalProfileScreen` — it reads from the ViewModel instead.

**Koin:** Register as `single { CityRepository(get()) }`. Update ViewModel factories to inject it.

**UI reactivity fix:** In composables that display city names, collect `cityRepository.cities` as state. When cities load, the composable recomposes and the display name updates. This replaces the broken pattern of calling `getCityDisplayName()` once in a `LaunchedEffect`.

**Error/retry:** The UI can observe `loadState` and show a retry button when `Error`. Calling `cityRepository.loadCities()` retries.

### 2. Server-Side CityService Caching (review item 3)

**Problem:** Every `resolveNameFromId`/`resolveIdFromName` hits the DB. For a search page with 20 results, that's 20 extra queries just for city name resolution.

**Solution:** In-memory cache inside `CityService`.

**File:** `server/src/main/kotlin/com/fugisawa/quemfaz/city/application/CityService.kt`

```kotlin
class CityService(
    private val cityRepository: CityRepository,
) {
    private val cacheLock = Any()

    @Volatile
    private var cachedCities: List<City> = emptyList()

    @Volatile
    private var cacheTimestamp: Instant = Instant.EPOCH

    private val cacheTtl = Duration.ofHours(1)

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

    fun listActive(): CitiesResponse {
        return CitiesResponse(cities = ensureCache().map { it.toResponse() })
    }

    fun findById(id: String): City? = ensureCache().find { it.id.value == id }

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

**Key points:**
- Double-checked locking with `@Volatile` + `synchronized` for thread safety.
- 1-hour TTL — cities rarely change.
- `findByName` now uses in-memory case-insensitive search instead of a DB query.
- `findById` also reads from cache — the `CityRepository` (server-side, DB layer) is only hit on cache refresh.

### 3. Drop `city_name` Column (review item 5)

**Problem:** V3 added `city_id` and backfilled from `city_name`, but never dropped the old column. It's stale and unused.

**File:** `server/src/main/resources/db/migration/V4__drop_city_name_column.sql`

```sql
-- Drop the old city_name column and its index.
-- Data was backfilled to city_id in V3; city_name is no longer referenced.
DROP INDEX IF EXISTS idx_professional_profiles_city_name;
ALTER TABLE professional_profiles DROP COLUMN IF EXISTS city_name;
```

### 4. Clean Up Old Settings Key (review item 8)

**Problem:** A previous version stored a city name under `"current_city"`. After the city entity migration, the key is `"current_city_id"`. The old key is orphaned on devices that ran the old version.

**File:** `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/session/SessionManager.kt`

Add to `init` block:

```kotlin
init {
    // One-time cleanup: remove stale key from pre-city-entity versions.
    settings.remove("current_city")

    val token = sessionStorage.getToken()
    // ... existing logic
}
```

No-op if the key doesn't exist. Safe, idempotent.

### 5. Extract ProfileResponseMapper (review item 6)

**Problem:** `mapToResponse(ProfessionalProfile -> ProfessionalProfileResponse)` is duplicated in `ProfileServices.kt`, `SearchProfessionalsService.kt`, and `FavoriteServices.kt`. The only difference: `ProfileServices` includes `status` on `InterpretedServiceDto` (e.g., `"pending_review"`); the others always use `"active"`.

**File:** `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileResponseMapper.kt`

```kotlin
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
            description = profile.normalizedDescription ?: "",
            cityId = profile.cityId ?: "",
            cityName = cityService.resolveNameFromId(profile.cityId) ?: "",
            services = profile.services.map { svc ->
                val canonical = catalogService.findById(svc.serviceId)
                val status = if (includeServiceStatus) {
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

**Callers:**
- `ProfileServices.kt`: Replace private `mapToResponse` with `mapper.toResponse(..., includeServiceStatus = true)`. Inject `ProfileResponseMapper` into each service that uses it (`ConfirmProfessionalProfileService`, `GetMyProfessionalProfileService`, `GetPublicProfessionalProfileService`, `UpdateProfessionalProfileService`), replacing separate `catalogService` and `cityService` params.
- `SearchProfessionalsService.kt`: Replace private `mapToResponse` with `mapper.toResponse(...)`. Inject `ProfileResponseMapper`, replacing `catalogService` and `cityService` params used only for mapping.
- `FavoriteServices.kt` (`ListFavoritesService`): Same — inject mapper, replace private function and the `catalogService`/`cityService` params.

**Koin:** Register `single { ProfileResponseMapper(get(), get()) }`. Update service registrations to inject it.

### 6. CityId in DTOs (review item 7)

**Decision:** No action. `CityId` as a value class in shared DTOs would add coupling between the domain layer and the serialization contract for minimal type-safety benefit. DTOs use `String` — standard for API contracts. Revisit if a cross-DTO type-safe ID effort is undertaken.

## Testing

- **CityRepository (client):** Verify `loadState` transitions, `getCityDisplayName` returns name after load completes, returns `null` before load.
- **CityService cache:** Unit test with a `FakeCityRepository` — verify cache hit (repository called once for multiple `findById` calls), verify TTL expiry triggers reload.
- **V4 migration:** Verified by Flyway on server startup + existing integration tests (they don't reference `city_name`).
- **SessionManager cleanup:** No test needed — `settings.remove` on a nonexistent key is a no-op.
- **ProfileResponseMapper:** Unit test with fakes — verify `includeServiceStatus = true` produces `"pending_review"` for pending services, and `false` always produces `"active"`.
- **Existing tests:** Must still pass (72 tests, 13 pre-existing failures unrelated to these changes).

## Out of Scope

- `CityId` value class in DTOs (deferred — item 7)
- Fixing the 13 pre-existing test failures (`dateOfBirth` missing in test fixtures)
- Adding new integration tests for city resolution in engagement/favorites services (tracked separately)
