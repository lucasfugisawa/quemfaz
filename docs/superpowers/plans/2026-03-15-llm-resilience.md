# LLM Resilience Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make onboarding and search flows resilient to LLM unavailability via a three-tier fallback chain (LLM → local text matching → manual service selection), while removing neighborhoods from the product and adding configurable LLM timeouts.

**Architecture:** Remove neighborhoods from all layers (DB, domain, DTOs, LLM prompts, ranking, UI). Add an `LlmConfig` with configurable timeout to `AppConfig`. Wire `CanonicalServices.search()` as the server-side fallback in both interpreters. Add `llmUnavailable` flag to shared DTOs so clients can distinguish LLM failure from normal clarification. Build manual service selection UI as the final fallback tier.

**Tech Stack:** Kotlin, Ktor, Exposed ORM, PostgreSQL + Flyway, Compose Multiplatform, Koin DI, kotlinx.serialization

**Spec:** `docs/superpowers/specs/2026-03-15-llm-resilience-design.md`

---

## Chunk 1: Remove Neighborhoods

This chunk removes neighborhoods from the entire stack: database, domain model, server-side code, shared DTOs, LLM prompts, ranking, and UI. This must be done first because subsequent tasks modify the same files.

### Task 1: Flyway Migration — Drop Neighborhood Tables and Columns

**Files:**
- Create: `server/src/main/resources/db/migration/V10__remove_neighborhoods.sql`

The database currently has:
- `professional_profile_neighborhoods` table (created in V3)
- `search_queries.neighborhoods_json` column (created in V4)

- [ ] **Step 1: Write the migration**

```sql
-- V10: Remove neighborhoods from the product (YAGNI for small cities)

-- Drop the neighborhoods join table
DROP TABLE IF EXISTS professional_profile_neighborhoods;

-- Drop the neighborhoods column from search_queries
ALTER TABLE search_queries DROP COLUMN IF EXISTS neighborhoods_json;
```

- [ ] **Step 2: Verify migration compiles**

Run: `./gradlew :server:compileKotlin`
Expected: BUILD SUCCESSFUL (migration is SQL-only, no Kotlin dependency yet)

- [ ] **Step 3: Commit**

```bash
git add server/src/main/resources/db/migration/V10__remove_neighborhoods.sql
git commit -m "feat: V10 migration — drop neighborhoods table and column"
```

---

### Task 2: Remove Neighborhoods from Server Domain and Persistence

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/domain/Models.kt:17` — remove `neighborhoods` field from `ProfessionalProfile`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/infrastructure/persistence/ExposedProfessionalProfileRepository.kt:68-73` — delete `ProfessionalProfileNeighborhoodsTable` object and all references
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/domain/Models.kt:13` — remove `neighborhoods` field from `SearchQuery`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/domain/Models.kt:24` — remove `neighborhoods` field from `InterpretedSearchQuery`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/infrastructure/persistence/ExposedSearchQueryRepository.kt:20` — remove `neighborhoodsJson` column from `SearchQueriesTable` and from the insert block

- [ ] **Step 1: Remove `neighborhoods` from `ProfessionalProfile`**

In `server/.../profile/domain/Models.kt`, remove line 17 (`val neighborhoods: List<String>,`).

- [ ] **Step 2: Remove `ProfessionalProfileNeighborhoodsTable` and all references**

In `server/.../profile/infrastructure/persistence/ExposedProfessionalProfileRepository.kt`:
- Delete the `ProfessionalProfileNeighborhoodsTable` object (lines 68-73)
- Remove all code that reads from or writes to this table:
  - In `save()`: remove the block that deletes and inserts neighborhoods (look for `ProfessionalProfileNeighborhoodsTable.deleteWhere` and the `forEach` that inserts neighborhoods)
  - In `mapProfile()`: remove the query that loads neighborhoods from this table and remove `neighborhoods =` from the `ProfessionalProfile` constructor call

- [ ] **Step 3: Remove `neighborhoods` from search domain models**

In `server/.../search/domain/Models.kt`:
- Remove `val neighborhoods: List<String>,` from `SearchQuery` (line 13)
- Remove `val neighborhoods: List<String>,` from `InterpretedSearchQuery` (line 24)

- [ ] **Step 4: Remove `neighborhoodsJson` from `SearchQueriesTable`**

In `server/.../search/infrastructure/persistence/ExposedSearchQueryRepository.kt`:
- Remove `val neighborhoodsJson = jsonb<List<String>>("neighborhoods_json", Json)` (line 20)
- Remove `it[neighborhoodsJson] = searchQuery.neighborhoods` from the insert block (line 37)

- [ ] **Step 5: Fix all compilation errors**

After removing neighborhoods from domain models, fix all call sites that pass `neighborhoods` to constructors. These will fail to compile — follow the compiler errors. Key locations:
- `SearchProfessionalsService.kt` — remove `neighborhoods = interpreted.neighborhoods` from `SearchQuery` constructor (line 48)
- `LlmSearchQueryInterpreter.kt` — remove `neighborhoods = interpretation.neighborhoods` from `InterpretedSearchQuery` constructors in `mapToResult()` (line 47) and `fallbackResult()` (line 61)
- `MockSearchQueryInterpreter.kt` — remove neighborhood detection logic (lines 25-30) and `neighborhoods = detectedNeighborhoods` from constructor (line 45)

Run: `./gradlew :server:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/domain/Models.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/profile/infrastructure/persistence/ExposedProfessionalProfileRepository.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/search/domain/Models.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/search/infrastructure/persistence/ExposedSearchQueryRepository.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreter.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/MockSearchQueryInterpreter.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/search/application/SearchProfessionalsService.kt
git commit -m "refactor: remove neighborhoods from server domain, persistence, and interpreters"
```

---

### Task 3: Remove Neighborhoods from Shared DTOs and Interpretation Models

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt:29,53,68` — remove `neighborhoods` fields from `CreateProfessionalProfileDraftResponse`, `ConfirmProfessionalProfileRequest`, `ProfessionalProfileResponse`
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/search/SearchDtos.kt` — no neighborhoods here (already clean)
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/search/interpretation/InterpretationModels.kt:26,43,65` — remove `neighborhoods` from `InterpretedInput`, `InterpretedSearchQuery`, `InterpretedProfessionalInput`; delete `hasNeighborhoods()` extension function

- [ ] **Step 1: Remove `neighborhoods` from profile DTOs**

In `shared/.../contract/profile/ProfileDtos.kt`:
- Remove `val neighborhoods: List<String>,` from `CreateProfessionalProfileDraftResponse` (line 29)
- Remove `val neighborhoods: List<String>,` from `ConfirmProfessionalProfileRequest` (line 53)
- Remove `val neighborhoods: List<String>,` from `ProfessionalProfileResponse` (line 68)

- [ ] **Step 2: Remove `neighborhoods` from shared interpretation models**

In `shared/.../search/interpretation/InterpretationModels.kt`:
- Remove `val neighborhoods: List<String>` from `InterpretedInput` sealed interface (line 26)
- Remove `override val neighborhoods: List<String>,` from `InterpretedSearchQuery` (line 43)
- Remove `override val neighborhoods: List<String>,` from `InterpretedProfessionalInput` (line 65)
- Delete the `hasNeighborhoods()` extension function (lines 90-91)

- [ ] **Step 3: Fix all compilation errors across all modules**

After removing neighborhoods from shared DTOs, all callers across `server/`, `composeApp/`, and `shared/` will fail to compile. Follow compiler errors to fix each call site. Key locations:

**Server:**
- `ProfileServices.kt` — remove `neighborhoods = request.neighborhoods` and `neighborhoods = profile.neighborhoods` from all `ProfessionalProfile` constructors and `mapToResponse` calls
- `LlmProfessionalInputInterpreter.kt` — remove `neighborhoods = interpretation.neighborhoods` from `mapToResponse()` (line 68), remove `neighborhoods = emptyList()` from `fallbackResponse()` (line 80)
- `MockProfessionalInputInterpreter.kt` — remove all neighborhood detection logic (lines 35-39, 52-54) and `neighborhoods = neighborhoods` from constructor call
- `SearchProfessionalsService.kt` — remove `neighborhoods = profile.neighborhoods` from `mapToResponse()`
- `FavoriteServices.kt` — remove `neighborhoods = profile.neighborhoods` from `mapToResponse()`

**ComposeApp:**
- `OnboardingViewModel.kt` — remove `neighborhoods = draft.neighborhoods` from `ConfirmProfessionalProfileRequest` constructor (line 120)
- Any screen that displays neighborhoods

Run: `./gradlew compileKotlin` (cross-module compilation)
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/ server/ composeApp/
git commit -m "refactor: remove neighborhoods from shared DTOs, interpretation models, and all call sites"
```

---

### Task 4: Remove Neighborhoods from LLM Prompts and Ranking

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/llm/LlmModels.kt:11,21` — remove `neighborhoods` fields from `OnboardingInterpretation` and `SearchInterpretation`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt:130` — remove "extract neighborhoods" from system prompt
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreter.kt:86` — remove "extract neighborhoods" from system prompt
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/ranking/ProfessionalSearchRankingService.kt:16,58-69` — remove `NEIGHBORHOOD_BONUS` constant and neighborhood matching logic

- [ ] **Step 1: Remove `neighborhoods` from LLM models**

In `server/.../llm/LlmModels.kt`:
- Remove `val neighborhoods: List<String> = emptyList(),` from `OnboardingInterpretation` (line 11)
- Remove `val neighborhoods: List<String> = emptyList(),` from `SearchInterpretation` (line 21)

- [ ] **Step 2: Remove "extract neighborhoods" from onboarding system prompt**

In `server/.../profile/interpretation/LlmProfessionalInputInterpreter.kt`, update the `SYSTEM_PROMPT` (lines 113-135). Remove:
- `- extract neighborhoods if present` (line 130)

Also remove `- extract city if present` (line 129) — city is now handled by the structured picker, not extracted from text. Update the prompt rules to only focus on service extraction and clarification.

The updated rules section should be:
```
Rules:
- infer services from description and map them to the canonical service ID values above
- the "serviceIds" field must contain only ID values from the catalog
- if important information about services is missing:
  set needsClarification = true
  generate up to 2 clarificationQuestions
- clarificationQuestions must be short and objective
```

- [ ] **Step 3: Remove "extract neighborhoods" from search system prompt**

In `server/.../search/interpretation/LlmSearchQueryInterpreter.kt`, update the `SYSTEM_PROMPT` (lines 71-88). Remove:
- `- extract city if present. If no city is mentioned, set it to null.` (line 85)
- `- extract neighborhoods if present. If no neighborhood is mentioned, return an empty list.` (line 86)

City is now provided as context from the main screen, not extracted from the query. The LLM should only focus on service identification.

- [ ] **Step 4: Remove city field from `SearchInterpretation`**

In `server/.../llm/LlmModels.kt`, also remove `val city: String? = null,` from `SearchInterpretation`. The search flow gets city from `cityContext`, not from LLM extraction.

Similarly, remove `val city: String? = null,` from `OnboardingInterpretation`. City is now handled by the structured picker.

- [ ] **Step 5: Fix interpreter code that reads city from LLM results**

In `LlmProfessionalInputInterpreter.kt`:
- In `mapToResponse()`, remove `cityName = interpretation.city` (line 67). Replace with `cityName = null` (city comes from the picker, not the LLM).
- Remove the `if (interpretation.city == null) { missingFields.add("city") }` block (lines 56-58). City is no longer a field the LLM interprets.
- In `fallbackResponse()`, remove `"city"` from `missingFields` list (line 81) and remove the "Em qual cidade você atende?" question (line 82).

In `LlmSearchQueryInterpreter.kt`:
- In `mapToResult()`, change `cityName = interpretation.city ?: cityContext` (line 46) to `cityName = cityContext`.

- [ ] **Step 6: Remove neighborhood bonus from ranking**

In `server/.../search/ranking/ProfessionalSearchRankingService.kt`:
- Remove `const val NEIGHBORHOOD_BONUS = 30` (line 16)
- Remove the entire "2. Neighborhood Bonus" block (lines 58-69)

- [ ] **Step 7: Verify compilation and tests**

Run: `./gradlew :server:compileKotlin`
Expected: BUILD SUCCESSFUL

Run: `./gradlew :server:test`
Expected: All tests pass (some tests may need updating if they reference neighborhoods)

- [ ] **Step 8: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/llm/LlmModels.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreter.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/search/ranking/ProfessionalSearchRankingService.kt
git commit -m "refactor: remove neighborhoods and city extraction from LLM prompts and ranking"
```

---

### Task 5: Remove Neighborhoods from UI

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt` — remove any neighborhood display
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/SearchScreens.kt` — remove any neighborhood display
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/ProfileScreens.kt` — remove any neighborhood display

- [ ] **Step 1: Search for neighborhood references in composeApp**

Search for `neighborhood` (case-insensitive) across all files in `composeApp/`. Remove any display of neighborhoods in cards, profile views, or onboarding review screens.

- [ ] **Step 2: Verify cross-module compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL (no neighborhood references remain)

- [ ] **Step 3: Commit**

```bash
git add composeApp/
git commit -m "refactor: remove neighborhood display from UI"
```

---

### Task 6: Run Full Test Suite and Fix Remaining Issues

- [ ] **Step 1: Run full test suite**

Run: `./gradlew :server:test --rerun`
Expected: All tests pass

- [ ] **Step 2: Fix any broken tests**

Tests that reference neighborhoods in assertions, request bodies, or mock data need updating. Search for `neighborhood` in `server/src/test/`.

- [ ] **Step 3: Commit fixes if any**

```bash
git add server/src/test/
git commit -m "test: fix tests after neighborhood removal"
```

---

## Chunk 2: Configurable LLM Timeout

### Task 7: Add LLM Configuration to AppConfig

**Files:**
- Modify: `server/src/main/resources/application.conf` — add `llm {}` config block
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/AppConfig.kt` — add `LlmConfig` data class and field
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/ConfigLoader.kt` — load LLM config

- [ ] **Step 1: Write the failing test**

Create test file `server/src/test/kotlin/com/fugisawa/quemfaz/config/LlmConfigTest.kt`:

```kotlin
package com.fugisawa.quemfaz.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LlmConfigTest {
    @Test
    fun `LlmConfig has sensible defaults`() {
        val config = LlmConfig()
        assertEquals(8000L, config.timeoutMs)
    }

    @Test
    fun `LlmConfig accepts custom timeout`() {
        val config = LlmConfig(timeoutMs = 5000L)
        assertEquals(5000L, config.timeoutMs)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:test --tests "com.fugisawa.quemfaz.config.LlmConfigTest" --rerun`
Expected: FAIL — `LlmConfig` does not exist

- [ ] **Step 3: Add `LlmConfig` data class**

In `server/.../config/AppConfig.kt`, add:

```kotlin
data class LlmConfig(
    val timeoutMs: Long = 8000L,
)
```

Add `val llm: LlmConfig,` to `AppConfig`.

- [ ] **Step 4: Add `llm {}` block to `application.conf`**

```hocon
llm {
    timeoutMs = 8000
    timeoutMs = ${?LLM_TIMEOUT_MS}
}
```

- [ ] **Step 5: Load LLM config in `ConfigLoader`**

In `ConfigLoader.loadConfig()`, add to the `AppConfig` constructor:

```kotlin
llm = LlmConfig(
    timeoutMs = ktorConfig.propertyOrNull("llm.timeoutMs")?.getString()?.toLong() ?: 8000L,
),
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :server:test --tests "com.fugisawa.quemfaz.config.LlmConfigTest" --rerun`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/config/AppConfig.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/config/ConfigLoader.kt \
  server/src/main/resources/application.conf \
  server/src/test/kotlin/com/fugisawa/quemfaz/config/LlmConfigTest.kt
git commit -m "feat: add configurable LLM timeout to AppConfig (default 8s)"
```

---

### Task 8: Apply Timeout to LlmAgentService

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/llm/LlmAgentService.kt` — add `withTimeout` wrapper
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt:141` — pass `LlmConfig` to `LlmAgentService`

- [ ] **Step 1: Add timeout parameter to `LlmAgentService`**

Modify `LlmAgentService` to accept a timeout and wrap `executeStructured` calls with `kotlinx.coroutines.withTimeout`:

```kotlin
import kotlinx.coroutines.withTimeout

open class LlmAgentService(
    @PublishedApi internal val promptExecutor: PromptExecutor,
    private val timeoutMs: Long = 8000L,
) {
    private val logger = LoggerFactory.getLogger(LlmAgentService::class.java)

    constructor(timeoutMs: Long = 8000L) : this(createExecutor(), timeoutMs)

    open suspend fun <T> executeStructured(
        systemPrompt: String,
        userMessage: String,
        serializer: KSerializer<T>,
    ): T {
        val result = withTimeout(timeoutMs) {
            promptExecutor.executeStructured(
                prompt = prompt("quemfaz-structured") {
                    system(systemPrompt)
                    user(userMessage)
                },
                model = OpenAIModels.Chat.GPT4oMini,
                serializer = serializer,
            )
        }
        return result.getOrThrow().data
    }

    // ... rest unchanged
}
```

- [ ] **Step 2: Update Koin wiring to pass config**

In `KoinModules.kt`, change:
```kotlin
single { LlmAgentService() }
```
to:
```kotlin
single { LlmAgentService(timeoutMs = get<AppConfig>().llm.timeoutMs) }
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/llm/LlmAgentService.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt
git commit -m "feat: apply configurable timeout to LLM calls via withTimeout"
```

---

## Chunk 3: Server-Side Fallback Chain

### Task 9: Add `llmUnavailable` Flag to Shared DTOs

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt:25-33` — add `llmUnavailable` to `CreateProfessionalProfileDraftResponse`
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/search/SearchDtos.kt:18-25` — add `llmUnavailable` to `SearchProfessionalsResponse`

This is a shared contract change — server and client both depend on these DTOs.

- [ ] **Step 1: Add `llmUnavailable` to `CreateProfessionalProfileDraftResponse`**

In `shared/.../contract/profile/ProfileDtos.kt`, add a field to `CreateProfessionalProfileDraftResponse`:

```kotlin
@Serializable
data class CreateProfessionalProfileDraftResponse(
    val normalizedDescription: String,
    val interpretedServices: List<InterpretedServiceDto>,
    val cityName: String?,
    val missingFields: List<String>,
    val followUpQuestions: List<String>,
    val freeTextAliases: List<String>,
    val llmUnavailable: Boolean = false,
)
```

Note: `neighborhoods` was already removed in Chunk 1. The `llmUnavailable` field defaults to `false` for backward compatibility.

- [ ] **Step 2: Add `llmUnavailable` to `SearchProfessionalsResponse`**

In `shared/.../contract/search/SearchDtos.kt`, add:

```kotlin
@Serializable
data class SearchProfessionalsResponse(
    val normalizedQuery: String,
    val interpretedServices: List<InterpretedServiceDto>,
    val results: List<ProfessionalProfileResponse>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val llmUnavailable: Boolean = false,
)
```

- [ ] **Step 3: Verify cross-module compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL (defaults ensure backward compatibility)

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt \
  shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/search/SearchDtos.kt
git commit -m "feat: add llmUnavailable flag to onboarding and search response DTOs"
```

---

### Task 10: Wire Local Text Matching Fallback in Onboarding Interpreter

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt` — update `fallbackResponse()` to use `CanonicalServices.search()` and set `llmUnavailable = true`
- Test: `server/src/test/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreterFallbackTest.kt`

- [ ] **Step 1: Write the failing test**

Create `server/src/test/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreterFallbackTest.kt`:

```kotlin
package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.OnboardingInterpretation
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class LlmProfessionalInputInterpreterFallbackTest {

    /** LlmAgentService that always throws to simulate LLM failure */
    private class FailingLlmAgentService : LlmAgentService(timeoutMs = 1000L) {
        override suspend fun <T> executeStructured(
            systemPrompt: String,
            userMessage: String,
            serializer: KSerializer<T>,
        ): T = throw RuntimeException("LLM unavailable")
    }

    private val interpreter = LlmProfessionalInputInterpreter(FailingLlmAgentService())

    @Test
    fun `fallback with matching alias sets llmUnavailable and returns matched services`() {
        val response = interpreter.interpret("Sou eletricista", InputMode.TEXT)

        assertTrue(response.llmUnavailable)
        assertTrue(response.interpretedServices.isNotEmpty())
        assertTrue(response.interpretedServices.any { it.serviceId == "repair-electrician" })
        assertTrue(response.followUpQuestions.isEmpty())
    }

    @Test
    fun `fallback with no matching alias sets llmUnavailable and returns empty services`() {
        val response = interpreter.interpret("xyz abc 123", InputMode.TEXT)

        assertTrue(response.llmUnavailable)
        assertTrue(response.interpretedServices.isEmpty())
        assertTrue(response.followUpQuestions.isEmpty())
    }

    @Test
    fun `fallback with empty services from LLM triggers local matching`() {
        // This tests the edge case: LLM succeeds but returns empty services
        // We test this through the fallback path since our mock always fails
        val response = interpreter.interpret("pintor", InputMode.TEXT)

        assertTrue(response.llmUnavailable)
        assertTrue(response.interpretedServices.isNotEmpty())
        assertTrue(response.interpretedServices.any { it.serviceId == "paint-residential" || it.serviceId == "paint-commercial" })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:test --tests "com.fugisawa.quemfaz.profile.interpretation.LlmProfessionalInputInterpreterFallbackTest" --rerun`
Expected: FAIL — `llmUnavailable` field doesn't exist in response, or fallback doesn't use local matching

- [ ] **Step 3: Update `fallbackResponse()` to use local matching**

In `LlmProfessionalInputInterpreter.kt`, replace the `fallbackResponse()` method:

```kotlin
private fun fallbackResponse(inputText: String): CreateProfessionalProfileDraftResponse {
    val localMatches = CanonicalServices.search(inputText)
    val interpretedServices = localMatches.map { canonical ->
        InterpretedServiceDto(canonical.id.value, canonical.displayName, ServiceMatchLevel.PRIMARY.name)
    }

    return CreateProfessionalProfileDraftResponse(
        normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
        interpretedServices = interpretedServices,
        cityName = null,
        missingFields = if (interpretedServices.isEmpty()) listOf("services") else emptyList(),
        followUpQuestions = emptyList(),
        freeTextAliases = interpretedServices.map { it.displayName },
        llmUnavailable = true,
    )
}
```

Key changes:
- Uses `CanonicalServices.search(inputText)` for local matching
- Sets `llmUnavailable = true`
- Never returns `followUpQuestions` — fallback must not trigger clarification
- Only adds `"services"` to `missingFields` if no local matches found

Also update `mapToResponse()` to include the new field:

```kotlin
return CreateProfessionalProfileDraftResponse(
    // ... existing fields ...
    llmUnavailable = false,
)
```

And handle the empty-services edge case in `mapToResponse()`: if LLM returned empty services but didn't request clarification, try local matching:

```kotlin
private fun mapToResponse(
    inputText: String,
    interpretation: OnboardingInterpretation,
): CreateProfessionalProfileDraftResponse {
    val interpretedServices = interpretation.serviceIds
        .mapNotNull { serviceId ->
            CanonicalServices.findById(CanonicalServiceId(serviceId))
        }.map { canonical ->
            InterpretedServiceDto(canonical.id.value, canonical.displayName, ServiceMatchLevel.PRIMARY.name)
        }.distinctBy { it.serviceId }

    // Edge case: LLM succeeded but returned empty services without requesting clarification
    // Try local matching as a second attempt
    val finalServices = if (interpretedServices.isEmpty() && !interpretation.needsClarification) {
        CanonicalServices.search(inputText).map { canonical ->
            InterpretedServiceDto(canonical.id.value, canonical.displayName, ServiceMatchLevel.PRIMARY.name)
        }
    } else {
        interpretedServices
    }

    val missingFields = mutableListOf<String>()
    val followUpQuestions = mutableListOf<String>()

    if (finalServices.isEmpty() && !interpretation.needsClarification) {
        missingFields.add("services")
    }

    if (interpretation.needsClarification) {
        followUpQuestions.addAll(interpretation.clarificationQuestions.take(2))
    }

    return CreateProfessionalProfileDraftResponse(
        normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
        interpretedServices = finalServices,
        cityName = null,
        missingFields = missingFields,
        followUpQuestions = followUpQuestions,
        freeTextAliases = finalServices.map { it.displayName },
        llmUnavailable = false,
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:test --tests "com.fugisawa.quemfaz.profile.interpretation.LlmProfessionalInputInterpreterFallbackTest" --rerun`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt \
  server/src/test/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreterFallbackTest.kt
git commit -m "feat: wire CanonicalServices.search() as onboarding fallback with llmUnavailable flag"
```

---

### Task 11: Wire Local Text Matching Fallback in Search Interpreter

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreter.kt` — update `fallbackResult()` to use `CanonicalServices.search()`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/application/SearchProfessionalsService.kt` — propagate `llmUnavailable` to response
- Test: `server/src/test/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreterFallbackTest.kt`

- [ ] **Step 1: Write the failing test**

Create `server/src/test/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreterFallbackTest.kt`:

```kotlin
package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.llm.LlmAgentService
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class LlmSearchQueryInterpreterFallbackTest {

    private class FailingLlmAgentService : LlmAgentService(timeoutMs = 1000L) {
        override suspend fun <T> executeStructured(
            systemPrompt: String,
            userMessage: String,
            serializer: KSerializer<T>,
        ): T = throw RuntimeException("LLM unavailable")
    }

    private val interpreter = LlmSearchQueryInterpreter(FailingLlmAgentService())

    @Test
    fun `fallback with matching alias returns matched service IDs`() {
        val result = interpreter.interpret("eletricista", "Batatais")

        assertTrue(result.serviceIds.isNotEmpty())
        assertTrue(result.serviceIds.contains("repair-electrician"))
        assertEquals("Batatais", result.cityName)
    }

    @Test
    fun `fallback with no match returns empty service IDs`() {
        val result = interpreter.interpret("xyz abc 123", "Batatais")

        assertTrue(result.serviceIds.isEmpty())
        assertEquals("Batatais", result.cityName)
    }

    @Test
    fun `fallback preserves city context`() {
        val result = interpreter.interpret("pintor", "Franca")

        assertEquals("Franca", result.cityName)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:test --tests "com.fugisawa.quemfaz.search.interpretation.LlmSearchQueryInterpreterFallbackTest" --rerun`
Expected: FAIL — fallback doesn't use local matching

- [ ] **Step 3: Update `fallbackResult()` to use local matching**

In `LlmSearchQueryInterpreter.kt`, update `fallbackResult()`:

```kotlin
private fun fallbackResult(
    query: String,
    cityContext: String?,
): InterpretedSearchQuery {
    val localMatches = CanonicalServices.search(query)
    val serviceIds = localMatches.take(1).map { it.id.value }

    return InterpretedSearchQuery(
        originalQuery = query,
        normalizedQuery = query.lowercase().trim(),
        serviceIds = serviceIds,
        cityName = cityContext,
        freeTextAliases = localMatches.take(1).map { it.displayName },
    )
}
```

Also handle the empty-services edge case in `mapToResult()`: if LLM returned an invalid service ID, try local matching:

```kotlin
private fun mapToResult(
    query: String,
    cityContext: String?,
    interpretation: SearchInterpretation,
): InterpretedSearchQuery {
    val canonical = CanonicalServices.findById(CanonicalServiceId(interpretation.serviceId))

    // If LLM returned an invalid service ID, try local matching
    if (canonical == null) {
        val localMatches = CanonicalServices.search(query)
        val serviceIds = localMatches.take(1).map { it.id.value }
        return InterpretedSearchQuery(
            originalQuery = query,
            normalizedQuery = query.lowercase().trim(),
            serviceIds = serviceIds,
            cityName = cityContext,
            freeTextAliases = localMatches.take(1).map { it.displayName },
        )
    }

    return InterpretedSearchQuery(
        originalQuery = query,
        normalizedQuery = query.lowercase().trim(),
        serviceIds = listOf(canonical.id.value),
        cityName = cityContext,
        freeTextAliases = listOf(canonical.displayName),
    )
}
```

- [ ] **Step 4: Add `llmUnavailable` tracking to search flow**

The `InterpretedSearchQuery` (server domain model) needs a way to signal that the LLM was unavailable. Add a field:

In `server/.../search/domain/Models.kt`, add to `InterpretedSearchQuery`:

```kotlin
data class InterpretedSearchQuery(
    val originalQuery: String,
    val normalizedQuery: String,
    val serviceIds: List<String>,
    val cityName: String?,
    val freeTextAliases: List<String>,
    val llmUnavailable: Boolean = false,
)
```

Update `LlmSearchQueryInterpreter.fallbackResult()` to set `llmUnavailable = true`.
Update `LlmSearchQueryInterpreter.mapToResult()` to set `llmUnavailable = false`.

In `SearchProfessionalsService.execute()`, propagate the flag to the response:

```kotlin
return SearchProfessionalsResponse(
    // ... existing fields ...
    llmUnavailable = interpreted.llmUnavailable,
)
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :server:test --tests "com.fugisawa.quemfaz.search.interpretation.LlmSearchQueryInterpreterFallbackTest" --rerun`
Expected: PASS

- [ ] **Step 6: Run full test suite**

Run: `./gradlew :server:test --rerun`
Expected: All tests pass

- [ ] **Step 7: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreter.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/search/domain/Models.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/search/application/SearchProfessionalsService.kt \
  server/src/test/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreterFallbackTest.kt
git commit -m "feat: wire CanonicalServices.search() as search fallback with llmUnavailable flag"
```

---

### Task 12: Integration Test — Onboarding Fallback Chain

**Files:**
- Create: `server/src/test/kotlin/com/fugisawa/quemfaz/integration/profile/OnboardingFallbackIntegrationTest.kt`

This test verifies the full HTTP flow: POST to `/professional-profile/draft` when the LLM is down returns `llmUnavailable = true` with locally matched services.

- [ ] **Step 1: Write the integration test**

```kotlin
package com.fugisawa.quemfaz.integration.profile

import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.Table
import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class OnboardingFallbackIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = listOf(
        ProfessionalProfilesTable,
        UserPhoneAuthIdentitiesTable,
        UsersTable,
        OtpChallengesTable,
    )

    @Test
    fun `draft creation returns services even when LLM fails (local matching fallback)`() = integrationTestApplication {
        // Note: In integration tests, OPENAI_API_KEY is not set, so LLM calls fail.
        // This tests the real fallback behavior.
        val token = obtainAuthToken("+5511900000060")

        val authedClient = createTestClient(token)
        val response = authedClient.post("/professional-profile/draft") {
            contentType(ContentType.Application.Json)
            setBody(CreateProfessionalProfileDraftRequest("Sou eletricista", InputMode.TEXT))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val draft = response.body<CreateProfessionalProfileDraftResponse>()

        assertTrue(draft.llmUnavailable, "Should flag LLM as unavailable")
        assertTrue(draft.interpretedServices.isNotEmpty(), "Should have locally matched services")
        assertTrue(draft.followUpQuestions.isEmpty(), "Should not trigger clarification on LLM failure")
    }
}
```

- [ ] **Step 2: Run test**

Run: `./gradlew :server:test --tests "com.fugisawa.quemfaz.integration.profile.OnboardingFallbackIntegrationTest" --rerun`
Expected: PASS (LLM is unavailable in test env, fallback engages)

- [ ] **Step 3: Commit**

```bash
git add server/src/test/kotlin/com/fugisawa/quemfaz/integration/profile/OnboardingFallbackIntegrationTest.kt
git commit -m "test: integration test for onboarding fallback chain"
```

---

## Chunk 4: Client-Side Fallback Routing

### Task 13: Update OnboardingViewModel to Handle `llmUnavailable`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt` — bypass clarification when `llmUnavailable` is true

- [ ] **Step 1: Update `createDraft()` to check `llmUnavailable`**

In `OnboardingViewModel.kt`, modify the `createDraft()` method. Currently (lines 40-47):

```kotlin
val response = apiClients.createDraft(...)
if (response.followUpQuestions.isNotEmpty()) {
    _uiState.value = OnboardingUiState.NeedsClarification(inputText, response)
} else {
    _uiState.value = OnboardingUiState.DraftReady(response)
}
```

Change to:

```kotlin
val response = apiClients.createDraft(...)
if (response.llmUnavailable || response.followUpQuestions.isEmpty()) {
    // LLM was unavailable OR no clarification needed — go straight to draft review.
    // If services are empty, the UI will show manual selection (Task 14).
    _uiState.value = OnboardingUiState.DraftReady(response)
} else {
    _uiState.value = OnboardingUiState.NeedsClarification(inputText, response)
}
```

- [ ] **Step 2: Update `submitClarifications()` similarly**

Apply the same logic in `submitClarifications()` (lines 58-65):

```kotlin
val response = apiClients.clarifyDraft(...)
if (response.llmUnavailable || response.followUpQuestions.isEmpty()) {
    _uiState.value = OnboardingUiState.DraftReady(response)
} else {
    _uiState.value = OnboardingUiState.NeedsClarification(originalDescription, response)
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinDesktop` (or the appropriate compile target)
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt
git commit -m "feat: bypass clarification loop when llmUnavailable is true"
```

---

### Task 14: Build Manual Service Selection UI for Onboarding

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/ServiceCategoryPicker.kt` — reusable grouped category list
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt` — show manual selection when draft has empty services

The `ServiceCategoryPicker` composable displays the 23 canonical services grouped by 8 categories. It supports multi-select (for onboarding) and single-select (for search).

- [ ] **Step 1: Add `displayName` to `ServiceCategory` enum**

In `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/domain/service/ServiceModels.kt`, the `ServiceCategory` enum currently has no `displayName`. Add a Portuguese display name:

```kotlin
enum class ServiceCategory(val displayName: String) {
    CLEANING("Limpeza"),
    REPAIRS("Reparos"),
    PAINTING("Pintura"),
    GARDEN("Jardim"),
    EVENTS("Eventos"),
    BEAUTY("Beleza"),
    MOVING_AND_ASSEMBLY("Mudanças e Montagem"),
    OTHER("Outros"),
}
```

- [ ] **Step 2: Create `ServiceCategoryPicker` composable**

Create `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/ServiceCategoryPicker.kt`:

```kotlin
package com.fugisawa.quemfaz.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fugisawa.quemfaz.domain.service.CanonicalServices
import com.fugisawa.quemfaz.domain.service.ServiceCategory
import com.fugisawa.quemfaz.ui.theme.AppSpacing

@Composable
fun ServiceCategoryPicker(
    selectedServiceIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    multiSelect: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val servicesByCategory = remember {
        ServiceCategory.entries.associateWith { category ->
            CanonicalServices.findByCategory(category)
        }.filter { it.value.isNotEmpty() }
    }

    LazyColumn(modifier = modifier) {
        servicesByCategory.forEach { (category, services) ->
            item {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(
                        start = AppSpacing.md,
                        top = AppSpacing.md,
                        bottom = AppSpacing.xs,
                    ),
                )
            }
            items(services) { service ->
                val isSelected = service.id.value in selectedServiceIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newSelection = if (multiSelect) {
                                if (isSelected) selectedServiceIds - service.id.value
                                else selectedServiceIds + service.id.value
                            } else {
                                setOf(service.id.value)
                            }
                            onSelectionChanged(newSelection)
                        }
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (multiSelect) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                        )
                    } else {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                        )
                    }
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text(
                        text = service.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Update `DraftReady` screen to show manual selection when services are empty**

In `OnboardingScreens.kt`, find the composable that renders `OnboardingUiState.DraftReady`. When `draft.interpretedServices.isEmpty()`, show the `ServiceCategoryPicker` instead of the normal service review. The user selects services manually and proceeds.

This requires adding state for manual service selection and a "Continue" button. The exact integration depends on the current layout — read the DraftReady section of `OnboardingScreens.kt` and add the picker in the appropriate location.

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinDesktop`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/domain/service/ServiceModels.kt \
  composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/ServiceCategoryPicker.kt \
  composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt
git commit -m "feat: add ServiceCategoryPicker and manual service selection for onboarding fallback"
```

---

### Task 15: Update OnboardingViewModel to Support Manual Service Selection

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt` — add method to proceed with manually selected services

- [ ] **Step 1: Add `proceedWithManualServices` method**

When the user manually selects services via the picker, the ViewModel needs to create a modified draft with those services and proceed:

```kotlin
fun proceedWithManualServices(
    draft: CreateProfessionalProfileDraftResponse,
    selectedServiceIds: Set<String>,
) {
    val manualServices = selectedServiceIds.map { serviceId ->
        val canonical = CanonicalServices.findById(
            com.fugisawa.quemfaz.core.id.CanonicalServiceId(serviceId)
        )
        InterpretedServiceDto(
            serviceId = serviceId,
            displayName = canonical?.displayName ?: serviceId,
            matchLevel = "PRIMARY",
        )
    }
    val updatedDraft = draft.copy(interpretedServices = manualServices)
    proceedFromDraft(updatedDraft)
}
```

Note: Need to import `CanonicalServices` in the ViewModel. Since it's in `shared/commonMain`, this is valid for KMP.

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinDesktop`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt
git commit -m "feat: add proceedWithManualServices for onboarding fallback"
```

---

### Task 16: Add Category Browser to Search Screen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/SearchScreens.kt` — show `ServiceCategoryPicker` when LLM is unavailable and no results
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeViewModel.kt` — add method to search by service ID directly

- [ ] **Step 1: Add `searchByServiceId` method to `HomeViewModel`**

When the user taps a service in the category browser, the search should execute with that service ID directly (bypassing LLM interpretation). Add a method:

```kotlin
fun searchByServiceId(serviceId: String) {
    // Construct a query using the service display name so the search
    // endpoint receives a meaningful query string for analytics.
    val canonical = CanonicalServices.findById(
        com.fugisawa.quemfaz.core.id.CanonicalServiceId(serviceId)
    )
    val queryText = canonical?.displayName ?: serviceId
    search(queryText)
}
```

Alternatively, if the search API accepts service IDs directly (it doesn't currently — it takes free text), the simplest approach is to search using the display name, which the local matching fallback will resolve to the correct service ID.

- [ ] **Step 2: Show category browser in search when appropriate**

In `SearchScreens.kt`, when the search state is `Success` but `results` is empty and `llmUnavailable` is true (or always when results are empty), show the `ServiceCategoryPicker` in single-select mode below an "Or browse by category" heading.

To detect `llmUnavailable` in the UI, the `SearchUiState.Success` state needs to carry the `llmUnavailable` flag. Update `HomeViewModel`'s `SearchUiState.Success` to include this field:

```kotlin
data class Success(
    val results: List<ProfessionalProfileResponse>,
    // ... existing fields ...
    val llmUnavailable: Boolean = false,
) : SearchUiState()
```

Propagate from the `SearchProfessionalsResponse.llmUnavailable` when constructing the state.

When `results.isEmpty()`, show:

```kotlin
ServiceCategoryPicker(
    selectedServiceIds = emptySet(),
    onSelectionChanged = { selected ->
        selected.firstOrNull()?.let { serviceId ->
            viewModel.searchByServiceId(serviceId)
        }
    },
    multiSelect = false,
)
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinDesktop`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/SearchScreens.kt \
  composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeViewModel.kt
git commit -m "feat: add category browser to search screen for manual service selection"
```

---

### Task 17: City Picker for Onboarding (Multi-City from Supported Set)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt` — add city picker to the DraftReady screen
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt` — add selected cities state, pre-select main-screen city
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt` — change `cityName: String?` to `cityNames: List<String>` in `ConfirmProfessionalProfileRequest` (or keep single city for now — see note)

**Important note:** The spec says "Professionals may operate in multiple cities" and "at least one city is required." However, the current `ConfirmProfessionalProfileRequest` has `cityName: String?` (single city), and `ProfessionalProfile` has `cityName: String?` (single city). Changing to multi-city requires modifying the shared DTO, the server confirmation service, the database schema (a new join table or array column), and the profile response.

**Decision point for implementer:** If multi-city is a larger change than scoped here, this task can implement a single-city picker from supported cities with the main-screen city pre-selected, and defer multi-city to a follow-up. The key requirement is that the city comes from a structured picker (not LLM extraction) and that the main-screen city is pre-selected.

- [ ] **Step 1: Define supported cities list**

The platform needs a list of supported cities. For now, define this as a simple list in a shared location. Create or add to `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/domain/city/SupportedCities.kt`:

```kotlin
package com.fugisawa.quemfaz.domain.city

object SupportedCities {
    val all: List<String> = listOf(
        "Batatais",
        "Franca",
        "Ribeirão Preto",
    )
}
```

This list will grow as the product expands. It should be the single source of truth for what cities the platform supports.

- [ ] **Step 2: Add city picker to onboarding DraftReady screen**

In `OnboardingScreens.kt`, in the `DraftReady` section, add a city selection dropdown or picker. The city currently selected on the main screen should be pre-selected (passed from the ViewModel or the HomeViewModel's selected city state).

Use a simple dropdown or exposed dropdown menu with the supported cities list. Ensure the selected city is stored in the ViewModel state and used when confirming the profile.

- [ ] **Step 3: Update `OnboardingViewModel.submitKnownName` to use selected city**

Currently (line 119): `cityName = draft.cityName`. After this change, the city should come from the ViewModel's own selected-city state, which was initialized from the main screen and may have been changed by the user in the picker.

Add a `selectedCity` state to `OnboardingViewModel`:

```kotlin
private val _selectedCity = MutableStateFlow<String?>(null)

fun initializeCity(mainScreenCity: String?) {
    if (_selectedCity.value == null) {
        _selectedCity.value = mainScreenCity
    }
}

fun selectCity(city: String) {
    _selectedCity.value = city
}
```

In `submitKnownName`, use `_selectedCity.value` instead of `draft.cityName`.

- [ ] **Step 4: Verify compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/domain/city/SupportedCities.kt \
  composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt \
  composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt
git commit -m "feat: add city picker to onboarding with supported cities and main-screen pre-selection"
```

---

## Chunk 5: Server-Side Logging and Full Verification

### Task 18: Add Structured Logging for LLM Failures

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt` — enhance failure logging
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreter.kt` — enhance failure logging

- [ ] **Step 1: Enhance logging in onboarding interpreter**

In `LlmProfessionalInputInterpreter.kt`, update the catch blocks in `interpret()` and `interpretWithClarifications()` to log with structured context:

```kotlin
catch (e: Exception) {
    logger.error(
        "LLM interpretation failed [flow=onboarding, inputLength={}, errorType={}, message={}]. Engaging local matching fallback.",
        inputText.length,
        e::class.simpleName,
        e.message,
    )
    fallbackResponse(inputText)
}
```

- [ ] **Step 2: Enhance logging in search interpreter**

In `LlmSearchQueryInterpreter.kt`, update the catch block:

```kotlin
catch (e: Exception) {
    logger.error(
        "LLM interpretation failed [flow=search, query={}, cityContext={}, errorType={}, message={}]. Engaging local matching fallback.",
        query,
        cityContext,
        e::class.simpleName,
        e.message,
    )
    fallbackResult(query, cityContext)
}
```

- [ ] **Step 3: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreter.kt
git commit -m "feat: add structured logging for LLM failures with flow context"
```

---

### Task 19: Update Mock Interpreters for Consistency

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/MockProfessionalInputInterpreter.kt` — remove neighborhoods, add `llmUnavailable`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/MockSearchQueryInterpreter.kt` — remove neighborhoods

- [ ] **Step 1: Update MockProfessionalInputInterpreter**

In `MockProfessionalInputInterpreter.kt`:
- Remove all neighborhood detection logic (lines 35-39, 52-54)
- Remove `neighborhoods = neighborhoods` from the constructor call
- Add `llmUnavailable = false` to the return value
- Remove city detection logic (lines 32-33, 49-54) — city is now handled by the picker

- [ ] **Step 2: Update MockSearchQueryInterpreter**

In `MockSearchQueryInterpreter.kt`:
- Remove all neighborhood detection logic (lines 25-30)
- Remove `neighborhoods = detectedNeighborhoods` from the constructor call
- Remove city detection logic (lines 33-38) — city comes from `cityContext` parameter

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/MockProfessionalInputInterpreter.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/MockSearchQueryInterpreter.kt
git commit -m "refactor: update mock interpreters — remove neighborhoods, add llmUnavailable"
```

---

### Task 20: Run Full Test Suite and Final Verification

- [ ] **Step 1: Run full server test suite**

Run: `./gradlew :server:test --rerun`
Expected: All tests pass

- [ ] **Step 2: Run cross-module compilation**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL across all modules

- [ ] **Step 3: Fix any remaining issues**

Address any compilation or test failures discovered in this step.

- [ ] **Step 4: Final commit if any fixes needed**

Stage only the specific files that were fixed, then commit:

```bash
git commit -m "fix: address remaining issues from LLM resilience implementation"
```

---

### Task 21: Update Documentation

**Files:**
- Modify: `server/README.md` — add V10 migration note
- Modify: `shared/README.md` — document `llmUnavailable` field, remove neighborhoods from DTO docs

- [ ] **Step 1: Update server README**

Add V10 to the migration table in `server/README.md`:
- V10: Remove neighborhoods (drop `professional_profile_neighborhoods` table, drop `search_queries.neighborhoods_json`)

- [ ] **Step 2: Update shared README**

In `shared/README.md`:
- Remove `neighborhoods` from DTO documentation
- Add `llmUnavailable: Boolean` field documentation for `CreateProfessionalProfileDraftResponse` and `SearchProfessionalsResponse`

- [ ] **Step 3: Commit**

```bash
git add server/README.md shared/README.md
git commit -m "docs: update READMEs for V10 migration and llmUnavailable field"
```
