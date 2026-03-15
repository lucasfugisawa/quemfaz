# UX Phase 5 — Marketplace Signals & Performance Design

**Date:** 2026-03-15
**Status:** Approved
**Reference:** `quemfaz-ux-ui-audit.md` (Phases 14–16), `2026-03-14-ux-redesign-implementation-design.md`

---

## Context

Phases 1–4 addressed usability, flow simplification, visual consistency, and interaction polish. Phase 5 shifts to marketplace quality and performance: making ranking smarter by feeding engagement data back into search scoring, improving perceived speed with caching and timeouts, and surfacing more useful recency information to users.

The engagement tracking infrastructure (profile views, contact clicks) was built in earlier phases and stores events in dedicated tables. However, these signals are never aggregated or used in ranking. `lastActiveAt` on professional profiles is frozen at creation time, making the "Active recently" chip misleading. There is no client-side caching and no HTTP timeout configuration.

---

## Scope

### In Scope

| # | Item | Priority | Modules |
|---|------|----------|---------|
| 1 | Feed engagement signals into ranking score | P1 | server, shared |
| 2 | Specific recency display ("Active X days ago") | P2 | server, shared, composeApp |
| 3 | Client-side search result caching (10-min TTL) | P1 | composeApp |
| 4 | Explicit Ktor client timeouts | P2 | composeApp |
| 5 | HTTP Cache-Control headers on profile endpoints | P2 | server |

### Out of Scope (Deferred)

- **Prefetch profile detail on scroll** (P3) — requires scroll-position-aware lifecycle; deferred
- **Haptic feedback** — requires `expect/actual` KMP platform code; deferred
- **Server-side Redis caching** — not needed at v1 scale
- **Pagination activation** — already functional end-to-end (server paginates with `page`/`pageSize`, client accumulates via `loadMoreResults()`, "Load more" button exists in UI)

---

## 1. Engagement Signals in Ranking

### Problem

`ProfessionalSearchRankingService` scores professionals on 5 static factors (service match, neighborhood, completeness, recency, city). Engagement events (profile views, contact clicks) are tracked and stored in `profile_view_events` and `contact_click_events` tables but never read back. A new professional with zero engagement ranks identically to an established one with dozens of contact clicks.

### Design

**Database migration (V9):** Add denormalized counters to `professional_profiles`:

```sql
ALTER TABLE professional_profiles
    ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN contact_click_count INTEGER NOT NULL DEFAULT 0;
```

**Increment on write:**

- `TrackProfileViewService.execute()`: after inserting the event, increment `view_count` on the profile row and update `last_active_at = NOW()`.
- `TrackContactClickService.execute()`: after inserting the event, increment `contact_click_count` on the profile row and update `last_active_at = NOW()`.
- Both updates happen in the same transaction as the event insert.
- Updating `last_active_at` on engagement events fixes the stale-recency bug where the field was frozen at profile creation time.

**New ranking factors in `ProfessionalSearchRankingService.calculateScore()`:**

- Contact clicks: `min(contactClickCount, 20) * 2` → up to **40 points** (strongest engagement signal — represents real user intent)
- Profile views: `min(viewCount, 50) * 0.5` → up to **25 points** (weaker signal — passive browsing)
- Both are capped to prevent runaway dominance by a single popular professional.

**Updated scoring summary:**

| Factor | Points | Source |
|--------|--------|--------|
| PRIMARY service match | 100 | existing |
| SECONDARY service match | 50 | existing |
| RELATED service match | 20 | existing |
| Neighborhood match | 30 | existing |
| Contact clicks (capped at 20) | up to 40 | **new** |
| Profile views (capped at 50) | up to 25 | **new** |
| Profile completeness | 15 | existing |
| Recently active (≤7 days) | 10 | existing |
| City mismatch penalty | -50 | existing |

**DTO change:** Add `contactCount: Int` to `ProfessionalProfileResponse` in `shared/contract/`. Populated server-side from `contact_click_count`. No UI display in this phase — the field is available for future "X people contacted this professional" display.

### Files

- **Create:** `server/src/main/resources/db/migration/V9__engagement_counters.sql`
- **Modify:** `server/.../profile/domain/Models.kt` — add `viewCount`, `contactClickCount` fields to `ProfessionalProfile` data class; add `incrementViewCount(id)`, `incrementContactClickCount(id)`, and `updateLastActiveAt(id)` to `ProfessionalProfileRepository` interface
- **Modify:** `server/.../profile/infrastructure/ExposedProfessionalProfileRepository.kt` — implement `incrementViewCount`, `incrementContactClickCount`, and `updateLastActiveAt`; add `viewCount`/`contactClickCount` columns to Exposed table and `mapProfile()`
- **Modify:** `server/.../engagement/application/TrackProfileViewService.kt` — after event insert, call `profileRepository.incrementViewCount()` + `updateLastActiveAt()`
- **Modify:** `server/.../engagement/application/TrackContactClickService.kt` — after event insert, call `profileRepository.incrementContactClickCount()` + `updateLastActiveAt()`
- **Modify:** `server/.../search/ranking/ProfessionalSearchRankingService.kt` — add 2 new scoring factors
- **Modify:** `shared/.../contract/profile/ProfileDtos.kt` — add `contactCount: Int` to `ProfessionalProfileResponse`
- **Modify:** `server/.../profile/application/ProfileServices.kt` — map `contactClickCount` to `contactCount` in response

---

## 2. Recency Specificity

### Problem

`ProfessionalProfileResponse.activeRecently` is a boolean — true if `lastActiveAt` is within 7 days. The UI shows a generic "Active recently" chip with no granularity. Users can't distinguish between a professional who was active today vs. 6 days ago.

### Design

**DTO change:** Add `daysSinceActive: Int?` to `ProfessionalProfileResponse` in `shared/contract/`. Computed server-side as `ChronoUnit.DAYS.between(lastActiveAt, now)`.

**Nullability note:** The DB column `last_active_at` is nullable. The repository currently falls back to `createdAt` when it is null, so the domain field `lastActiveAt: Instant` is never null in practice. `daysSinceActive` will therefore always be non-null for existing profiles. However, the DTO field remains `Int?` for forward-compatibility if the fallback is ever removed.

**Keep `activeRecently: Boolean`** — no breaking change. The boolean is still useful for chip visibility logic.

**Server mapping:** Three files contain `mapToResponse()` functions that build `ProfessionalProfileResponse`:
- `ProfileServices.kt` has a single file-level `private fun mapToResponse()` shared by all services in the file — one change covers all four services.
- `SearchProfessionalsService.kt` has its own private `mapToResponse()`.
- `FavoriteServices.kt` has its own private `mapToResponse()`.

All three must be updated to compute `daysSinceActive`. With Section 1's fix to `lastActiveAt`, this now reflects real engagement activity.

**UI change in `StatusChipRow`** (used by `ProfessionalCard` and `ProfileHeader`):

| `daysSinceActive` | Display text |
|----|---|
| 0 | "Active today" |
| 1 | "Active yesterday" |
| 2–7 | "Active X days ago" |
| 8–30 | "Active this month" |
| 31+ or null | chip hidden |

### Files

- **Modify:** `shared/.../contract/profile/ProfileDtos.kt` — add `daysSinceActive: Int?`
- **Modify:** `server/.../search/application/SearchProfessionalsService.kt` — compute `daysSinceActive` in `mapToResponse()`
- **Modify:** `server/.../profile/application/ProfileServices.kt` — compute `daysSinceActive` in `mapToResponse()`
- **Modify:** `server/.../favorites/application/FavoriteServices.kt` — compute `daysSinceActive` in `mapToResponse()`
- **Modify:** `composeApp/.../ui/components/StatusChipRow.kt` — accept `daysSinceActive`, display granular text

---

## 3. Client-side Search Result Caching

### Problem

Every search hits the server. Navigating back from a profile to search results re-fetches the entire result set. This adds latency and wastes bandwidth.

### Design

**In-memory cache in `HomeViewModel`:**

```kotlin
private data class CachedSearch(
    val response: SearchProfessionalsResponse,
    val accumulatedResults: List<ProfessionalProfileResponse>,
    val currentPage: Int,
    val hasMore: Boolean,
    val timestamp: Long,  // kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)

private val searchCache = mutableMapOf<String, CachedSearch>()
```

**Note:** Uses `mutableMapOf` (KMP-safe) instead of `LinkedHashMap(capacity, loadFactor, accessOrder)` which is JVM-only. Eviction is by insertion order: when cache exceeds 5 entries, remove the oldest key (`searchCache.keys.first()`) before inserting the new entry. TTL is the primary eviction strategy; the 5-entry cap is a memory safety bound.

**Cache key:** `"$query:$cityName"` (normalized lowercase).

**Timestamps use `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()`** — KMP-safe alternative to `System.currentTimeMillis()` which is JVM-only and unavailable in `commonMain`.

**TTL:** 10 minutes. On `search(query)`:
1. Compute cache key.
2. If entry exists and `Clock.System.now().toEpochMilliseconds() - entry.timestamp < 600_000`: restore UI state from cache (set Success, restore accumulated results, page number, hasMore). Return without server call.
3. Otherwise: fetch from server, store result in cache. If cache exceeds 5 entries, remove `searchCache.keys.first()` before inserting.

**`loadMoreResults()` updates the cache entry** — the cached entry is replaced with the new accumulated results and incremented page number.

**Cache invalidation:**
- Clear entire cache on `toggleFavoriteFromSearch()` — ensures that returning to search results triggers a fresh server fetch with up-to-date ranking (since the server may factor engagement into ordering)
- Clear entire cache on logout: in `HomeViewModel.init {}`, launch a coroutine collecting `sessionManager.authState.drop(1)` (skip initial emission to avoid clearing cache on ViewModel creation). When state transitions to `Unauthenticated` or `Blocked`, call `searchCache.clear()`

**No server or shared contract changes.**

### Files

- **Modify:** `composeApp/.../screens/HomeViewModel.kt` — add `CachedSearch` class, `searchCache` map, cache-check logic in `search()`, cache-update in `executeSearch()` and `loadMoreResults()`, invalidation in `toggleFavoriteFromSearch()`

---

## 4. Ktor Client Timeouts

### Problem

`ApiClient`'s `HttpClient` has no explicit timeout configuration. A slow or unresponsive server hangs the UI indefinitely with no user feedback and no automatic recovery.

### Design

**Install `HttpTimeout` plugin** in `ApiClient.kt`'s `HttpClient` block:

```kotlin
install(HttpTimeout) {
    connectTimeoutMillis = 5_000    // 5s — fail fast if server unreachable
    socketTimeoutMillis = 30_000    // 30s — generous for AI endpoints
    requestTimeoutMillis = 60_000   // 60s — overall ceiling for slow AI round-trips
}
```

**Dependency:** `HttpTimeout` is in `io.ktor:ktor-client-core` which is already on the classpath.

**No retry logic.** Timeout exceptions are caught by existing `catch` blocks in ViewModels, which display the Error UI state. This is sufficient for v1.

### Files

- **Modify:** `composeApp/.../network/ApiClient.kt` — add `HttpTimeout` plugin installation

---

## 5. HTTP Cache-Control Headers

### Problem

No server responses include `Cache-Control` headers. All responses are treated as uncacheable by intermediaries and clients.

### Design

**Add `Cache-Control` headers to profile GET endpoints:**

| Endpoint | Header | Rationale |
|----------|--------|-----------|
| `GET /professional-profile/{profileId}` | `Cache-Control: public, max-age=120` | Profile detail changes infrequently; 2-min cache reduces round-trips on back-navigation |
| `GET /professional-profile/me` | `Cache-Control: private, no-cache` | Own profile must always reflect latest edits |

**No caching on POST endpoints** — search, engagement tracking, auth, favorites are all POST. HTTP semantics already prevent caching.

**Implementation:** Add `call.response.header("Cache-Control", "...")` before `call.respond()` in the relevant route handlers. No middleware or plugin needed.

### Files

- **Modify:** `server/.../profile/routing/ProfileRoutes.kt` — add Cache-Control header to `GET /professional-profile/{profileId}` and `GET /professional-profile/me`

---

## Cross-Cutting Concerns

### Shared Contract Change

Items 1 and 2 both modify `ProfessionalProfileResponse` in `shared/contract/profile/ProfileDtos.kt`. Both new fields have defaults (`contactCount: Int = 0`, `daysSinceActive: Int? = null`) ensuring backward compatibility with existing serialized responses.

### Testing

- **Server:** Integration tests for ranking with engagement scores, counter increment on event tracking, recency computation, Cache-Control headers
- **Client:** No automated UI tests (consistent with existing test coverage)

### Documentation Maintenance

Per project guidelines, the following docs must be updated alongside this implementation:
- `server/README.md` — update schema section for V9 migration (new columns on `professional_profiles`)
- `shared/README.md` — update DTO section for new `ProfessionalProfileResponse` fields (`contactCount`, `daysSinceActive`)

### Migration Safety

V9 migration adds two `INTEGER DEFAULT 0` columns — non-destructive, no data loss, no table lock on small tables.
