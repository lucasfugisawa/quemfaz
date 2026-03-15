# Profile Description & Service Editing Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separate onboarding input into two independent artifacts (LLM-edited description + inferred services), split the review into staged screens, and add post-onboarding catalog-based service management.

**Architecture:** The LLM prompt gains a description-editing instruction block. The draft response DTO gains an `editedDescription` field. The confirm request DTO renames `normalizedDescription` to `description`. The onboarding state machine splits `DraftReady` into `ReviewServices` → `ReviewDescription`. The profile edit screen gains a service management section using the existing `ServiceCategoryPicker`.

**Tech Stack:** Kotlin Multiplatform, Ktor (server), Compose Multiplatform (client), kotlinx.serialization, Koin DI, OpenAI GPT-4o Mini (via LlmAgentService)

**Spec:** `docs/superpowers/specs/2026-03-15-profile-description-service-editing-design.md`

---

## Chunk 1: Shared DTOs & Server-Side Changes

### Task 1: Add `editedDescription` to LLM response model

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/llm/LlmModels.kt:13-20`

- [ ] **Step 1: Add the field to `OnboardingInterpretation`**

Add `editedDescription` field to the LLM structured response model. This is what the LLM will return alongside service inference.

```kotlin
@Serializable
@SerialName("OnboardingInterpretation")
data class OnboardingInterpretation(
    val serviceIds: List<String>,
    val needsClarification: Boolean,
    val clarificationQuestions: List<String> = emptyList(),
    val unmatchedDescriptions: List<UnmatchedDescription> = emptyList(),
    val editedDescription: String = "",
)
```

- [ ] **Step 2: Verify the project compiles**

Run: `./gradlew :server:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/llm/LlmModels.kt
git commit -m "feat: add editedDescription to OnboardingInterpretation LLM model"
```

---

### Task 2: Add `editedDescription` to shared draft response DTO

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt:25-35`

- [ ] **Step 1: Add the field to `CreateProfessionalProfileDraftResponse`**

```kotlin
@Serializable
data class CreateProfessionalProfileDraftResponse(
    val normalizedDescription: String,
    val editedDescription: String = "",
    val interpretedServices: List<InterpretedServiceDto>,
    val cityName: String?,
    val missingFields: List<String>,
    val followUpQuestions: List<String>,
    val freeTextAliases: List<String>,
    val llmUnavailable: Boolean = false,
    val blockedDescriptions: List<String> = emptyList(),
)
```

- [ ] **Step 2: Verify the project compiles**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt
git commit -m "feat: add editedDescription to CreateProfessionalProfileDraftResponse DTO"
```

---

### Task 3: Rename `normalizedDescription` to `description` in confirm request DTO

This is a cross-cutting change: the field is used in `shared/`, `server/`, and `composeApp/`.

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt:50-58`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt` (lines 79, 94, 95, 176, 188, 189)
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt:162`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt:57`

- [ ] **Step 1: Rename field in the DTO**

In `ProfileDtos.kt`, rename `normalizedDescription` to `description` in `ConfirmProfessionalProfileRequest`:

```kotlin
@Serializable
data class ConfirmProfessionalProfileRequest(
    val description: String,
    val selectedServiceIds: List<String>,
    val cityName: String?,
    val contactPhone: String,
    val whatsAppPhone: String?,
    val portfolioPhotoUrls: List<String>
)
```

- [ ] **Step 2: Update `ConfirmProfessionalProfileService` in `ProfileServices.kt`**

At lines 79, 94, 95 — change `request.normalizedDescription` to `request.description`:

```kotlin
// Line 79 (completeness check)
request.description.isNotBlank() &&

// Lines 94-95 (profile creation)
description = request.description,
normalizedDescription = request.description,
```

- [ ] **Step 3: Update `UpdateProfessionalProfileService` in `ProfileServices.kt`**

At lines 176, 188, 189 — change `request.normalizedDescription` to `request.description`:

```kotlin
// Line 176 (completeness check)
request.description.isNotBlank() &&

// Lines 188-189 (profile update)
description = request.description,
normalizedDescription = request.description,
```

- [ ] **Step 4: Update `OnboardingViewModel.kt`**

At line 162 — change field name in the confirm call:

```kotlin
val response = apiClients.confirmProfile(
    ConfirmProfessionalProfileRequest(
        description = draft.normalizedDescription,
        selectedServiceIds = draft.interpretedServices.map { it.serviceId },
        cityName = _selectedCity.value,
        contactPhone = "",
        whatsAppPhone = null,
        portfolioPhotoUrls = emptyList(),
    )
)
```

Note: This still uses `draft.normalizedDescription` as the value for now. Task 8 will change it to use `editedDescription`.

- [ ] **Step 5: Update `EditProfessionalProfileViewModel.kt`**

At line 57 — change field name:

```kotlin
val updated = apiClients.updateMyProfessionalProfile(
    ConfirmProfessionalProfileRequest(
        description = description,
        selectedServiceIds = current.services.map { it.serviceId },
        cityName = cityName.ifBlank { null },
        contactPhone = contactPhone,
        whatsAppPhone = whatsAppPhone.ifBlank { null },
        portfolioPhotoUrls = current.portfolioPhotoUrls
    )
)
```

- [ ] **Step 6: Update any existing tests that reference `normalizedDescription` on `ConfirmProfessionalProfileRequest`**

Search for usages in test files and update the field name. Run:

```bash
grep -rn "normalizedDescription" server/src/test/ composeApp/src/
```

Update all occurrences that reference `ConfirmProfessionalProfileRequest.normalizedDescription` to `.description`. Known location: `BaseIntegrationTest.kt` line 191 (`normalizedDescription = draft.normalizedDescription` inside `createAndConfirmProfile()`). If the grep returns additional results, update those too.

- [ ] **Step 7: Verify full project compiles**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt \
  server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt \
  composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt \
  composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt
git commit -m "refactor: rename normalizedDescription to description in ConfirmProfessionalProfileRequest"
```

---

### Task 4: Add description-editing instructions to LLM prompt

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt:151-184`

- [ ] **Step 1: Replace `buildSystemPrompt()` with the updated version**

Replace the entire `buildSystemPrompt()` method with the version below. The only addition is the "Description editing:" block at the end of the prompt string:

```kotlin
private fun buildSystemPrompt(): String {
    val catalog = catalogService.getActiveServices().joinToString("\n") { service ->
        "- ${service.id}: ${service.displayName} (aliases: ${service.aliases.joinToString(", ")})"
    }
    return """
        Extract structured data about a professional service provider.

        Return structured output.

        You MUST map the described services to the canonical services supported by the platform.
        Use ONLY the service IDs from the catalog below. Do not invent new service IDs.

        If the description mentions a service that does NOT exist in the catalog:
        - DO NOT force-map it to an unrelated service
        - Instead, add it to the "unmatchedDescriptions" array with the raw description
        - Classify its safety: "safe", "unsafe", or "uncertain"
        - For "unsafe" or "uncertain", provide a brief "safetyReason"

        A service is "unsafe" if it involves illegal activities, legally regulated services
        the platform cannot verify, or anything that would expose the platform to legal
        or reputational risk.

        Supported services catalog:
        $catalog

        Rules:
        - infer services from description and map them to the canonical service ID values above
        - the "serviceIds" field must contain only ID values from the catalog
        - if important information about services is missing:
          set needsClarification = true
          generate up to 2 clarificationQuestions
        - clarificationQuestions must be short and objective

        Description editing:
        You must also produce an "editedDescription" field — a lightly edited version of the user's
        original text, suitable as a public profile description.

        Rules for editedDescription:
        - Fix punctuation and capitalization
        - Split run-on sentences for readability
        - Slightly reorganize phrasing for clarity
        - Apply light condensation if the user was verbose about something simple
        - Apply a light transformation from "describing what I do" tone toward "profile description" tone
        - Remove filler words and false starts (e.g. "tipo", "né", "aí", "então")

        You MUST NOT:
        - Add information the user did not mention
        - Invent experience, credentials, or marketing claims
        - Introduce services not present in the original text
        - Translate the user's language into canonical service names from the catalog above
        - Remove meaningful information
        - Change the meaning of what was said

        The editedDescription must preserve the user's authentic voice and wording.
        It is a cleaned-up, slightly condensed, better-structured version of what they wrote — not a rewrite.
    """.trimIndent()
}
```

- [ ] **Step 2: Verify the project compiles**

Run: `./gradlew :server:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt
git commit -m "feat: add description-editing instructions to LLM system prompt"
```

---

### Task 5: Propagate `editedDescription` through server response mapping

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt:45-99,101-123`

- [ ] **Step 1: Update `mapToResponse()` return statement to include `editedDescription`**

The method signature stays the same — `editedDescription` comes from `interpretation.editedDescription`. Update the return statement at line 89:

```kotlin
private suspend fun mapToResponse(
    inputText: String,
    interpretation: OnboardingInterpretation,
): CreateProfessionalProfileDraftResponse {
    // ... existing service mapping code stays the same (lines 49-75) ...

    // ... existing unmatched/blocked handling stays the same (lines 77-87) ...

    return CreateProfessionalProfileDraftResponse(
        normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
        editedDescription = interpretation.editedDescription.ifBlank {
            inputText.replaceFirstChar { it.uppercase() }
        },
        interpretedServices = finalServices,
        cityName = null,
        missingFields = missingFields,
        followUpQuestions = followUpQuestions,
        freeTextAliases = finalServices.map { it.displayName },
        llmUnavailable = false,
        blockedDescriptions = blockedDescriptions,
    )
}
```

Note: If `editedDescription` is blank (e.g., LLM didn't return it), fall back to the capitalized input.

- [ ] **Step 2: Add `editedDescription` to `fallbackResponse()`**

At line 114, update the fallback return to include the field:

```kotlin
private fun fallbackResponse(inputText: String): CreateProfessionalProfileDraftResponse {
    // ... existing code stays the same ...

    return CreateProfessionalProfileDraftResponse(
        normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
        editedDescription = inputText.replaceFirstChar { it.uppercase() },
        interpretedServices = interpretedServices,
        cityName = null,
        missingFields = if (interpretedServices.isEmpty()) listOf("services") else emptyList(),
        followUpQuestions = emptyList(),
        freeTextAliases = interpretedServices.map { it.displayName },
        llmUnavailable = true,
    )
}
```

Note: In the fallback case (LLM unavailable), `editedDescription` equals the capitalized input — no editing possible without the LLM.

- [ ] **Step 3: Verify the project compiles**

Run: `./gradlew :server:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run existing server tests**

Run: `./gradlew :server:test`
Expected: All tests pass. The new field has a default value so existing tests won't break.

- [ ] **Step 5: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt
git commit -m "feat: propagate editedDescription through LLM response mapping and fallback"
```

---

### Task 6: Update `mapToResponse()` in `ProfileServices.kt` to use `description` field

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt:206-232`

Currently `mapToResponse()` uses `profile.normalizedDescription` for the response's `description` field (line 219). It should use `profile.description` instead, since that field now carries the professional-approved description.

- [ ] **Step 1: Change `mapToResponse()` to use `profile.description`**

At line 219:

```kotlin
// Before:
description = profile.normalizedDescription ?: "",

// After:
description = profile.description ?: "",
```

- [ ] **Step 2: Verify the project compiles**

Run: `./gradlew :server:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt
git commit -m "fix: use description field (not normalizedDescription) in profile response mapping"
```

---

### Task 7: Write server integration test for edited description in draft response

**Files:**
- Modify: `server/src/test/kotlin/com/fugisawa/quemfaz/integration/profile/OnboardingFallbackIntegrationTest.kt` (existing file that already tests the draft endpoint)

The clarify endpoint (`POST /professional-profile/draft/clarify`) reuses `CreateProfessionalProfileDraftResponse` as its return type, so the DTO change in Task 2 covers both endpoints. This test verifies the draft path; the clarify path is implicitly covered by the shared return type.

- [ ] **Step 1: Add test to `OnboardingFallbackIntegrationTest`**

Add a test that verifies `editedDescription` is present in the draft response:

```kotlin
@Test
fun `draft response includes editedDescription field`() = integrationTestApplication {
    val token = obtainAuthToken("+5511900000061")
    val authedClient = createTestClient(token)
    val response = authedClient.post("/professional-profile/draft") {
        contentType(ContentType.Application.Json)
        setBody(CreateProfessionalProfileDraftRequest("faço pintura de casa e comércio", InputMode.TEXT))
    }
    assertEquals(HttpStatusCode.OK, response.status)
    val draft = response.body<CreateProfessionalProfileDraftResponse>()
    assertTrue(draft.editedDescription.isNotBlank(), "editedDescription should not be blank")
}
```

Note: If the LLM is unavailable in test environment, `editedDescription` will equal the capitalized input (fallback behavior) — still non-blank. Follow the same `obtainAuthToken()` + `createTestClient()` pattern used in existing tests in this file.

- [ ] **Step 2: Run the test**

Run: `./gradlew :server:test --tests "*OnboardingFallbackIntegrationTest*editedDescription*"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add server/src/test/kotlin/com/fugisawa/quemfaz/integration/profile/OnboardingFallbackIntegrationTest.kt
git commit -m "test: verify editedDescription is present in draft response"
```

---

## Chunk 2: Client-Side Onboarding Changes

### Task 8: Split `DraftReady` into `ReviewServices` and `ReviewDescription` states

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt`

- [ ] **Step 1: Replace `DraftReady` with two new states in `OnboardingUiState`**

```kotlin
sealed class OnboardingUiState {
    object Idle : OnboardingUiState()
    object Loading : OnboardingUiState()
    data class NeedsClarification(
        val originalDescription: String,
        val draft: CreateProfessionalProfileDraftResponse,
    ) : OnboardingUiState()
    data class ReviewServices(val draft: CreateProfessionalProfileDraftResponse) : OnboardingUiState()
    data class ReviewDescription(
        val draft: CreateProfessionalProfileDraftResponse,
        val confirmedServiceIds: List<String>,
    ) : OnboardingUiState()
    data class PhotoRequired(
        val draft: CreateProfessionalProfileDraftResponse,
        val confirmedServiceIds: List<String>,
        val confirmedDescription: String,
    ) : OnboardingUiState()
    data class KnownName(
        val draft: CreateProfessionalProfileDraftResponse,
        val confirmedServiceIds: List<String>,
        val confirmedDescription: String,
    ) : OnboardingUiState()
    data class Published(val profile: ProfessionalProfileResponse) : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}
```

Note: `PhotoRequired` and `KnownName` now carry `confirmedServiceIds` and `confirmedDescription` separately since the two review screens produce independent outputs.

- [ ] **Step 2: Update `createDraft()` and `submitClarifications()` to route to `ReviewServices`**

Replace all occurrences of `OnboardingUiState.DraftReady(response)` with `OnboardingUiState.ReviewServices(response)` in both methods (lines 73, 91). Also replace `OnboardingUiState.DraftReady(draft)` in `skipClarification()` (line 102).

- [ ] **Step 3: Add `proceedFromServices()` method**

This transitions from ReviewServices → ReviewDescription. The service IDs are confirmed at this point.

```kotlin
fun proceedFromServices(draft: CreateProfessionalProfileDraftResponse, confirmedServiceIds: List<String>) {
    _uiState.value = OnboardingUiState.ReviewDescription(draft, confirmedServiceIds)
}
```

- [ ] **Step 4: Add `proceedFromDescription()` method**

This transitions from ReviewDescription → PhotoRequired or KnownName.

```kotlin
fun proceedFromDescription(
    draft: CreateProfessionalProfileDraftResponse,
    confirmedServiceIds: List<String>,
    confirmedDescription: String,
) {
    val hasPhoto = sessionManager.currentUser.value?.photoUrl != null
    _uiState.value = if (hasPhoto) {
        OnboardingUiState.KnownName(draft, confirmedServiceIds, confirmedDescription)
    } else {
        OnboardingUiState.PhotoRequired(draft, confirmedServiceIds, confirmedDescription)
    }
}
```

- [ ] **Step 5: Remove old `proceedFromDraft()` method**

Delete the `proceedFromDraft()` method (lines 119–122) — it's replaced by `proceedFromServices()` and `proceedFromDescription()`.

- [ ] **Step 6: Update `proceedWithManualServices()`**

This now transitions to ReviewDescription instead of directly to PhotoRequired/KnownName:

```kotlin
fun proceedWithManualServices(
    draft: CreateProfessionalProfileDraftResponse,
    selectedServiceIds: Set<String>,
) {
    val manualServices = selectedServiceIds.map { serviceId ->
        val catalogEntry = _catalog.value?.services?.find { it.id == serviceId }
        InterpretedServiceDto(
            serviceId = serviceId,
            displayName = catalogEntry?.displayName ?: serviceId,
            matchLevel = "PRIMARY",
        )
    }
    val updatedDraft = draft.copy(interpretedServices = manualServices)
    _uiState.value = OnboardingUiState.ReviewDescription(
        updatedDraft,
        selectedServiceIds.toList(),
    )
}
```

- [ ] **Step 7: Update `submitPhoto()` to carry confirmed data through**

```kotlin
fun submitPhoto(
    data: ByteArray,
    mimeType: String,
    draft: CreateProfessionalProfileDraftResponse,
    confirmedServiceIds: List<String>,
    confirmedDescription: String,
) {
    viewModelScope.launch {
        _uiState.value = OnboardingUiState.Loading
        try {
            val uploadResponse = apiClients.uploadImage(data, mimeType)
            val userResponse = apiClients.setProfilePhoto(
                SetProfilePhotoRequest(photoUrl = uploadResponse.url)
            )
            sessionManager.setCurrentUser(userResponse)
            _uiState.value = OnboardingUiState.KnownName(draft, confirmedServiceIds, confirmedDescription)
        } catch (e: Exception) {
            _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to upload photo")
        }
    }
}
```

- [ ] **Step 8: Update `submitKnownName()` to use confirmed data**

```kotlin
fun submitKnownName(
    knownName: String?,
    confirmedServiceIds: List<String>,
    confirmedDescription: String,
) {
    viewModelScope.launch {
        _uiState.value = OnboardingUiState.Loading
        try {
            val response = apiClients.confirmProfile(
                ConfirmProfessionalProfileRequest(
                    description = confirmedDescription,
                    selectedServiceIds = confirmedServiceIds,
                    cityName = _selectedCity.value,
                    contactPhone = "",
                    whatsAppPhone = null,
                    portfolioPhotoUrls = emptyList(),
                )
            )
            if (!knownName.isNullOrBlank()) {
                apiClients.setKnownName(SetKnownNameRequest(knownName = knownName.trim()))
            }
            _uiState.value = OnboardingUiState.Published(response)
        } catch (e: Exception) {
            _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to publish profile")
        }
    }
}
```

- [ ] **Step 9: Update `goBack()` navigation**

```kotlin
fun goBack() {
    _uiState.value = when (val current = _uiState.value) {
        is OnboardingUiState.NeedsClarification -> OnboardingUiState.Idle
        is OnboardingUiState.ReviewServices -> OnboardingUiState.Idle
        is OnboardingUiState.ReviewDescription -> OnboardingUiState.ReviewServices(current.draft)
        is OnboardingUiState.PhotoRequired -> OnboardingUiState.ReviewDescription(
            current.draft, current.confirmedServiceIds
        )
        is OnboardingUiState.KnownName -> {
            val hasPhoto = sessionManager.currentUser.value?.photoUrl != null
            if (hasPhoto) OnboardingUiState.ReviewDescription(current.draft, current.confirmedServiceIds)
            else OnboardingUiState.PhotoRequired(current.draft, current.confirmedServiceIds, current.confirmedDescription)
        }
        else -> current
    }
}
```

- [ ] **Step 10: Verify the project compiles**

Run: `./gradlew :composeApp:compileKotlinMetadata`
Expected: Compilation errors in `OnboardingScreens.kt` and `App.kt` (expected — these files still reference the old states). This confirms the ViewModel changes are structurally complete and the UI is the only remaining consumer to update.

- [ ] **Step 11: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt
git commit -m "feat: split DraftReady into ReviewServices and ReviewDescription states"
```

---

### Task 9: Update `OnboardingScreens.kt` — composable signature, step indicators, and infrastructure

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt`

This task updates all the infrastructure in `OnboardingScreens.kt` that supports the state machine: the composable signature, step indicator logic, back button logic, and animation step indices.

- [ ] **Step 1: Update `stepIndex()` function (lines 34–41)**

Replace `DraftReady` with the two new states and shift subsequent indices:

```kotlin
private fun OnboardingUiState.stepIndex() = when (this) {
    is OnboardingUiState.Idle -> 1
    is OnboardingUiState.NeedsClarification -> 1
    is OnboardingUiState.ReviewServices -> 2
    is OnboardingUiState.ReviewDescription -> 3
    is OnboardingUiState.PhotoRequired -> 4
    is OnboardingUiState.KnownName -> 5
    else -> -1
}
```

- [ ] **Step 2: Update the `OnboardingScreens` composable signature (lines 44–58)**

Replace the old callbacks with the new ones:

```kotlin
@Composable
fun OnboardingScreens(
    uiState: OnboardingUiState,
    selectedCity: String?,
    catalog: CatalogResponse?,
    onCreateDraft: (String) -> Unit,
    onSelectCity: (String) -> Unit,
    onProceedFromServices: (CreateProfessionalProfileDraftResponse, List<String>) -> Unit,
    onProceedWithManualServices: (CreateProfessionalProfileDraftResponse, Set<String>) -> Unit,
    onProceedFromDescription: (CreateProfessionalProfileDraftResponse, List<String>, String) -> Unit,
    onPickPhoto: () -> Unit,
    onSubmitKnownName: (knownName: String?, confirmedServiceIds: List<String>, confirmedDescription: String) -> Unit,
    onSubmitClarifications: (String, List<ClarificationAnswer>) -> Unit,
    onSkipClarification: (CreateProfessionalProfileDraftResponse) -> Unit,
    onBack: () -> Unit,
    onFinish: (ProfessionalProfileResponse) -> Unit,
) {
```

Key changes:
- `onProceedFromDraft` → `onProceedFromServices` (takes draft + service IDs)
- Added `onProceedFromDescription` (takes draft + service IDs + description text)
- `onPickPhoto` simplified to `() -> Unit` (no longer passes draft — `App.kt` reads state from ViewModel directly)
- `onSubmitKnownName` now takes `confirmedServiceIds` and `confirmedDescription` instead of `draft`

- [ ] **Step 3: Update `currentStep` LaunchedEffect (lines 64–75)**

```kotlin
LaunchedEffect(uiState) {
    currentStep = when (uiState) {
        is OnboardingUiState.Idle -> 1
        is OnboardingUiState.NeedsClarification -> 1
        is OnboardingUiState.ReviewServices -> 2
        is OnboardingUiState.ReviewDescription -> 3
        is OnboardingUiState.PhotoRequired -> 4
        is OnboardingUiState.KnownName -> 5
        is OnboardingUiState.Loading,
        is OnboardingUiState.Published,
        is OnboardingUiState.Error -> currentStep
    }
}
```

- [ ] **Step 4: Update step count text (line 84)**

```kotlin
// Before:
text = "Step $currentStep of 4",

// After:
text = "Step $currentStep of 5",
```

- [ ] **Step 5: Update `showBack` condition (lines 91–94)**

```kotlin
val showBack = uiState is OnboardingUiState.NeedsClarification ||
               uiState is OnboardingUiState.ReviewServices ||
               uiState is OnboardingUiState.ReviewDescription ||
               uiState is OnboardingUiState.PhotoRequired ||
               uiState is OnboardingUiState.KnownName
```

- [ ] **Step 6: Verify — do not commit yet**

These changes will cause compile errors since the `when` branches still reference `DraftReady`. This is expected. Continue to Task 10.

---

### Task 10: Update `OnboardingScreens.kt` — ReviewServices, ReviewDescription, and remaining branches

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt`

- [ ] **Step 1: Replace `is OnboardingUiState.DraftReady` with `is OnboardingUiState.ReviewServices` (lines 242–362)**

**Empty services branch** — keep as-is but change the state type:

```kotlin
is OnboardingUiState.ReviewServices -> {
    val draft = state.draft
    if (draft.interpretedServices.isEmpty()) {
        var manualSelectedServices by remember { mutableStateOf(emptySet<String>()) }
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Selecione seus serviços", style = MaterialTheme.typography.headlineLarge)
            Text("Não conseguimos identificar seus serviços automaticamente. Selecione abaixo os serviços que você oferece.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            if (catalog != null) {
                ServiceCategoryPicker(
                    categories = catalog.categories,
                    services = catalog.services,
                    selectedServiceIds = manualSelectedServices,
                    onSelectionChanged = { manualSelectedServices = it },
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onProceedWithManualServices(draft, manualSelectedServices) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = manualSelectedServices.isNotEmpty(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Continuar", style = MaterialTheme.typography.titleMedium)
            }
        }
    } else {
        // Services present — show services + city, NO description card.
        // Note: The spec mentions "confirm, remove mismatches, or add missing services" on this screen.
        // This implementation shows services as read-only chips for confirmation only.
        // Full add/remove capability is available post-onboarding via profile editing (Task 12).
        // This is an intentional simplification to keep onboarding minimal per the design constraint.
        var cityDropdownExpanded by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize()) {
            Text("Review your services", style = MaterialTheme.typography.headlineLarge)
            Text("These are the services we identified.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Interpreted services:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        draft.interpretedServices.forEach { service ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(service.displayName) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // City selector — same as before
            Text("Your city:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = cityDropdownExpanded,
                onExpandedChange = { cityDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = selectedCity ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("City") },
                    placeholder = { Text("Select a city") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityDropdownExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                ExposedDropdownMenu(
                    expanded = cityDropdownExpanded,
                    onDismissRequest = { cityDropdownExpanded = false },
                ) {
                    SupportedCities.all.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city) },
                            onClick = {
                                onSelectCity(city)
                                cityDropdownExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onProceedFromServices(draft, draft.interpretedServices.map { it.serviceId })
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Looks good, continue", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
```

- [ ] **Step 2: Add the `ReviewDescription` branch**

Add after the `ReviewServices` branch:

```kotlin
is OnboardingUiState.ReviewDescription -> {
    var descriptionText by remember {
        mutableStateOf(state.draft.editedDescription.ifBlank { state.draft.normalizedDescription })
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Descrição do perfil", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Esta é a descrição que os clientes verão no seu perfil. Você pode editá-la se quiser.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = descriptionText,
            onValueChange = { descriptionText = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            label = { Text("Descrição") },
            maxLines = 8,
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { onProceedFromDescription(state.draft, state.confirmedServiceIds, descriptionText) },
            enabled = descriptionText.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text("Continuar", style = MaterialTheme.typography.titleMedium)
        }
    }
}
```

- [ ] **Step 3: Update `PhotoRequired` branch (lines 363–378)**

Change `onPickImage` callback — `onPickPhoto` no longer takes a draft parameter:

```kotlin
is OnboardingUiState.PhotoRequired -> {
    val sessionManager: SessionManager = koinInject()
    val currentUser by sessionManager.currentUser.collectAsState()
    val displayName = currentUser?.let { "${it.firstName} ${it.lastName}" } ?: ""

    ProfilePhotoScreen(
        currentPhotoUrl = currentUser?.photoUrl,
        displayName = displayName,
        headline = "Add a profile photo so clients can recognize you.",
        showSkip = false,
        isLoading = false,
        error = null,
        onPickImage = { onPickPhoto() },
        onSkip = null,
    )
}
```

- [ ] **Step 4: Update `KnownName` branch (lines 379–415)**

Pass `confirmedServiceIds` and `confirmedDescription` to `onSubmitKnownName`:

```kotlin
is OnboardingUiState.KnownName -> {
    var knownNameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Do you have a known name?", style = MaterialTheme.typography.headlineMedium)
        Text("If clients know you by a nickname or trade name, enter it here.", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = knownNameInput,
            onValueChange = { knownNameInput = it },
            label = { Text("Known name (optional)") },
            placeholder = { Text("e.g. Joãozinho da Tinta") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Button(
            onClick = { onSubmitKnownName(knownNameInput.trim().ifBlank { null }, state.confirmedServiceIds, state.confirmedDescription) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }

        TextButton(
            onClick = { onSubmitKnownName(null, state.confirmedServiceIds, state.confirmedDescription) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Skip") }
    }
}
```

- [ ] **Step 5: Update preview functions (lines 461–512)**

Update all preview composable calls to use the new callback signatures. Replace `onProceedFromDraft = {}` with `onProceedFromServices = { _, _ -> }` and add `onProceedFromDescription = { _, _, _ -> }`. Update `onPickPhoto = {}` (no params), `onSubmitKnownName = { _, _, _ -> }`.

Example for `OnboardingIdlePreview`:
```kotlin
@LightDarkScreenPreview
@Composable
private fun OnboardingIdlePreview() {
    AppTheme { OnboardingScreens(uiState = OnboardingUiState.Idle, selectedCity = null, catalog = null, onCreateDraft = {}, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }) }
}
```

Update all 5 preview functions following the same pattern. For `OnboardingDraftReadyPreview`, rename to `OnboardingReviewServicesPreview` and change the state:
```kotlin
uiState = OnboardingUiState.ReviewServices(PreviewSamples.sampleDraftResponse),
```

Add a new preview for the ReviewDescription screen:
```kotlin
@LightDarkScreenPreview
@Composable
private fun OnboardingReviewDescriptionPreview() {
    AppTheme {
        OnboardingScreens(
            uiState = OnboardingUiState.ReviewDescription(
                PreviewSamples.sampleDraftResponse,
                listOf("paint-residential"),
            ),
            selectedCity = "Franca",
            catalog = null,
            onCreateDraft = {}, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
        )
    }
}
```

- [ ] **Step 6: Update `App.kt` — onboarding call site (lines 396–438)**

Update the `isOnboardingInProgress` check (line 400):
```kotlin
val isOnboardingInProgress =
    uiState is OnboardingUiState.ReviewServices ||
    uiState is OnboardingUiState.ReviewDescription ||
    uiState is OnboardingUiState.PhotoRequired ||
    uiState is OnboardingUiState.KnownName
```

Update the `imagePicker` launcher (lines 411–414) to extract confirmed data from the new `PhotoRequired` state:
```kotlin
val imagePicker = rememberImagePickerLauncher { data, mimeType ->
    val photoState = (uiState as? OnboardingUiState.PhotoRequired) ?: return@rememberImagePickerLauncher
    viewModel.submitPhoto(data, mimeType, photoState.draft, photoState.confirmedServiceIds, photoState.confirmedDescription)
}
```

Update the `OnboardingScreens` call (lines 416–438):
```kotlin
OnboardingScreens(
    uiState = uiState,
    selectedCity = selectedCity,
    catalog = onboardingCatalog,
    onCreateDraft = { viewModel.createDraft(it) },
    onSelectCity = { viewModel.selectCity(it) },
    onProceedFromServices = { draft, serviceIds -> viewModel.proceedFromServices(draft, serviceIds) },
    onProceedWithManualServices = { draft, serviceIds -> viewModel.proceedWithManualServices(draft, serviceIds) },
    onProceedFromDescription = { draft, serviceIds, description -> viewModel.proceedFromDescription(draft, serviceIds, description) },
    onPickPhoto = { imagePicker.launch() },
    onSubmitKnownName = { knownName, serviceIds, description -> viewModel.submitKnownName(knownName, serviceIds, description) },
    onSubmitClarifications = { desc, answers -> viewModel.submitClarifications(desc, answers) },
    onSkipClarification = { draft -> viewModel.skipClarification(draft) },
    onBack = { viewModel.goBack() },
    onFinish = { profile ->
        currentProfileId = profile.id
        navigateToTab(Screen.Home)
        navigateTo(Screen.ProfessionalProfile)
    }
)
```

- [ ] **Step 7: Verify the full project compiles**

Run: `./gradlew :composeApp:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt \
  composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt
git commit -m "feat: implement ReviewServices and ReviewDescription screens, update onboarding flow wiring"
```

---

## Chunk 3: Post-Onboarding Service Editing

### Task 11: Add service management to `EditProfessionalProfileViewModel`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt`

- [ ] **Step 1: Add `CatalogApiClient` dependency and service state**

The ViewModel needs access to the catalog to show the service picker. Update the constructor and add state for managing services. Add these imports at the top of the file:

```kotlin
import com.fugisawa.quemfaz.network.CatalogApiClient
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
```

Update the class:

```kotlin
class EditProfessionalProfileViewModel(
    private val apiClients: FeatureApiClients,
    private val catalogApiClient: CatalogApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _catalog = MutableStateFlow<CatalogResponse?>(null)
    val catalog: StateFlow<CatalogResponse?> = _catalog.asStateFlow()

    private val _editedServiceIds = MutableStateFlow<List<String>>(emptyList())
    val editedServiceIds: StateFlow<List<String>> = _editedServiceIds.asStateFlow()

    // ... existing loadProfile(), saveProfile() ...
}
```

- [ ] **Step 2: Load catalog on init and populate service state from profile**

```kotlin
init {
    viewModelScope.launch {
        try {
            _catalog.value = catalogApiClient.getCatalog()
        } catch (_: Exception) { }
    }
}

fun loadProfile() {
    viewModelScope.launch {
        _uiState.value = EditProfileUiState.Loading
        try {
            val profile = apiClients.getMyProfessionalProfile()
            _editedServiceIds.value = profile.services.map { it.serviceId }
            _uiState.value = EditProfileUiState.Ready(profile)
        } catch (e: Exception) {
            _uiState.value = EditProfileUiState.NoProfile
        }
    }
}
```

- [ ] **Step 3: Add service management methods**

```kotlin
fun addService(serviceId: String) {
    if (serviceId !in _editedServiceIds.value) {
        _editedServiceIds.value = _editedServiceIds.value + serviceId
    }
}

fun removeService(serviceId: String) {
    _editedServiceIds.value = _editedServiceIds.value - serviceId
}
```

- [ ] **Step 4: Update `saveProfile()` to use `editedServiceIds`**

```kotlin
fun saveProfile(
    description: String,
    cityName: String,
    contactPhone: String,
    whatsAppPhone: String,
) {
    val current = when (val s = _uiState.value) {
        is EditProfileUiState.Ready -> s.profile
        is EditProfileUiState.Saved -> s.profile
        else -> return
    }
    viewModelScope.launch {
        _uiState.value = EditProfileUiState.Saving(current)
        try {
            val updated = apiClients.updateMyProfessionalProfile(
                ConfirmProfessionalProfileRequest(
                    description = description,
                    selectedServiceIds = _editedServiceIds.value,
                    cityName = cityName.ifBlank { null },
                    contactPhone = contactPhone,
                    whatsAppPhone = whatsAppPhone.ifBlank { null },
                    portfolioPhotoUrls = current.portfolioPhotoUrls
                )
            )
            _editedServiceIds.value = updated.services.map { it.serviceId }
            _uiState.value = EditProfileUiState.Saved(updated)
        } catch (e: Exception) {
            _uiState.value = EditProfileUiState.Error(e.message ?: "Failed to save profile")
        }
    }
}
```

- [ ] **Step 5: Update Koin DI registration**

In `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/di/Koin.kt`, update the factory to include `CatalogApiClient`:

```kotlin
// Before:
factory { EditProfessionalProfileViewModel(get()) }

// After:
factory { EditProfessionalProfileViewModel(get(), get()) }
```

- [ ] **Step 6: Verify the project compiles**

Run: `./gradlew :composeApp:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL (or compile errors only in EditProfessionalProfileScreen.kt which will be updated next)

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt \
  composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/di/Koin.kt
git commit -m "feat: add service management to EditProfessionalProfileViewModel"
```

---

### Task 12: Add service management UI to `EditProfessionalProfileScreen`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt`

- [ ] **Step 1: Add new imports to `EditProfessionalProfileScreen.kt`**

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.ui.components.ServiceCategoryPicker
```

- [ ] **Step 2: Update the `EditProfessionalProfileScreen` composable signature**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfessionalProfileScreen(
    uiState: EditProfileUiState,
    editedServiceIds: List<String>,
    catalog: CatalogResponse?,
    onAddService: (String) -> Unit,
    onRemoveService: (String) -> Unit,
    onSave: (description: String, city: String, contactPhone: String, whatsAppPhone: String) -> Unit,
    onNavigateBack: () -> Unit,
    onGoToOnboarding: () -> Unit,
)
```

- [ ] **Step 3: Update `EditProfileForm` signature to receive service data**

The inner `EditProfileForm` composable (line 98) also needs the new parameters:

```kotlin
@Composable
private fun EditProfileForm(
    profile: ProfessionalProfileResponse,
    isSaving: Boolean,
    isSaved: Boolean,
    editedServiceIds: List<String>,
    catalog: CatalogResponse?,
    onAddService: (String) -> Unit,
    onRemoveService: (String) -> Unit,
    onSave: (description: String, city: String, contactPhone: String, whatsAppPhone: String) -> Unit,
)
```

Update the three call sites in `EditProfessionalProfileScreen` (lines 83–91) to pass the new parameters:

```kotlin
is EditProfileUiState.Ready -> EditProfileForm(
    uiState.profile, isSaving = false, isSaved = false, editedServiceIds, catalog, onAddService, onRemoveService, onSave
)
is EditProfileUiState.Saving -> EditProfileForm(
    uiState.profile, isSaving = true, isSaved = false, editedServiceIds, catalog, onAddService, onRemoveService, onSave
)
is EditProfileUiState.Saved -> EditProfileForm(
    uiState.profile, isSaving = false, isSaved = true, editedServiceIds, catalog, onAddService, onRemoveService, onSave
)
```

- [ ] **Step 4: Replace the read-only service display with an interactive section**

In `EditProfileForm`, replace the read-only service display (lines 122–129) with:

```kotlin
// Services section
Text("Serviços", style = MaterialTheme.typography.titleMedium)
Spacer(modifier = Modifier.height(8.dp))

@OptIn(ExperimentalLayoutApi::class)
FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    editedServiceIds.forEach { serviceId ->
        val displayName = catalog?.services?.find { it.id == serviceId }?.displayName ?: serviceId
        InputChip(
            selected = true,
            onClick = { onRemoveService(serviceId) },
            label = { Text(displayName) },
            trailingIcon = {
                Icon(Icons.Default.Close, contentDescription = "Remover", modifier = Modifier.size(16.dp))
            },
        )
    }
}

Spacer(modifier = Modifier.height(8.dp))

// Add service button + dialog
var showServicePicker by remember { mutableStateOf(false) }
OutlinedButton(onClick = { showServicePicker = true }) {
    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(8.dp))
    Text("Adicionar serviço")
}

if (showServicePicker && catalog != null) {
    val alreadySelected = editedServiceIds.toSet()
    var pickerSelection by remember { mutableStateOf(emptySet<String>()) }

    AlertDialog(
        onDismissRequest = { showServicePicker = false },
        title = { Text("Adicionar serviços") },
        text = {
            // Constrain height to avoid layout issues — ServiceCategoryPicker uses LazyColumn internally
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                ServiceCategoryPicker(
                    categories = catalog.categories,
                    services = catalog.services.filter { it.id !in alreadySelected },
                    selectedServiceIds = pickerSelection,
                    onSelectionChanged = { pickerSelection = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                pickerSelection.forEach { onAddService(it) }
                showServicePicker = false
            }) { Text("Adicionar") }
        },
        dismissButton = {
            TextButton(onClick = { showServicePicker = false }) { Text("Cancelar") }
        },
    )
}
```

Note: `ServiceCategoryPicker` uses `LazyColumn` internally, so wrap it in `Box(modifier = Modifier.heightIn(max = 400.dp))` to prevent unbounded height inside the `AlertDialog`.

- [ ] **Step 5: Update the call site in `App.kt` (lines 440–454)**

```kotlin
is Screen.EditProfessionalProfile -> {
    val viewModel: EditProfessionalProfileViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val editedServiceIds by viewModel.editedServiceIds.collectAsState()
    val editCatalog by viewModel.catalog.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }
    EditProfessionalProfileScreen(
        uiState = uiState,
        editedServiceIds = editedServiceIds,
        catalog = editCatalog,
        onAddService = viewModel::addService,
        onRemoveService = viewModel::removeService,
        onSave = { desc, city, contact, whatsapp ->
            viewModel.saveProfile(desc, city, contact, whatsapp)
        },
        onNavigateBack = navigateBack,
        onGoToOnboarding = { navigateTo(Screen.OnboardingStart) }
    )
}
```

- [ ] **Step 6: Update preview functions (lines 205–259)**

All 6 preview composables need the new parameters. Add `editedServiceIds = emptyList(), catalog = null, onAddService = {}, onRemoveService = {},` to each call.

Example for `EditProfileReadyPreview`:
```kotlin
@LightDarkScreenPreview
@Composable
private fun EditProfileReadyPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Ready(PreviewSamples.sampleProfile),
            editedServiceIds = PreviewSamples.sampleProfile.services.map { it.serviceId },
            catalog = null,
            onAddService = {}, onRemoveService = {},
            onSave = { _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}
```

Update all 6 previews following the same pattern. For `Loading`, `NoProfile`, and `Error` states, use `editedServiceIds = emptyList()`.

- [ ] **Step 7: Verify the full project compiles**

Run: `./gradlew :composeApp:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileScreen.kt \
  composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt
git commit -m "feat: add service management UI to profile edit screen"
```

---

### Task 13: Write integration test for profile update with modified services

**Files:**
- Create: `server/src/test/kotlin/com/fugisawa/quemfaz/integration/profile/ProfileUpdateIntegrationTest.kt`

The catalog is seeded via Flyway migration `V11__service_catalog.sql` with active services including `paint-residential`, `paint-commercial`, and `clean-house`. Use these IDs in the test.

- [ ] **Step 1: Create the test file**

Follow the same pattern as `OnboardingFallbackIntegrationTest`. Key patterns to follow:
- `obtainAuthToken(phone)` returns a token string
- `createTestClient(token)` creates an authenticated HTTP client
- `completeNameStep(token, first, last)` sets up the user's name (required before profile)
- `setUserPhoto(token, url)` sets a photo (required before confirming a profile)
- `tablesToClean` must be declared (abstract property from `BaseIntegrationTest`)

```kotlin
package com.fugisawa.quemfaz.integration.profile

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileUpdateIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = listOf(
        ProfessionalProfilesTable,
        UserPhoneAuthIdentitiesTable,
        UsersTable,
        OtpChallengesTable,
    )

    @Test
    fun `updating profile can change services`() = integrationTestApplication {
        // Set up user: auth + name + photo (required before confirming a profile)
        val token = obtainAuthToken("+5516900000070")
        completeNameStep(token, "Test", "User")
        setUserPhoto(token, "https://example.com/photo.jpg")
        val client = createTestClient(token)

        // Create a profile with initial service
        client.post("/professional-profile/confirm") {
            contentType(ContentType.Application.Json)
            setBody(ConfirmProfessionalProfileRequest(
                description = "Pintor residencial",
                selectedServiceIds = listOf("paint-residential"),
                cityName = "Franca",
                contactPhone = "16999999999",
                whatsAppPhone = null,
                portfolioPhotoUrls = emptyList(),
            ))
        }

        // Update with an additional service
        val updateResponse = client.put("/professional-profile/me") {
            contentType(ContentType.Application.Json)
            setBody(ConfirmProfessionalProfileRequest(
                description = "Pintor residencial",
                selectedServiceIds = listOf("paint-residential", "paint-commercial"),
                cityName = "Franca",
                contactPhone = "16999999999",
                whatsAppPhone = null,
                portfolioPhotoUrls = emptyList(),
            ))
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updated = updateResponse.body<ProfessionalProfileResponse>()
        assertEquals(2, updated.services.size)
        assertTrue(updated.services.any { it.serviceId == "paint-commercial" })
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :server:test --tests "*ProfileUpdateIntegrationTest*"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add server/src/test/kotlin/com/fugisawa/quemfaz/integration/profile/ProfileUpdateIntegrationTest.kt
git commit -m "test: verify profile update can modify services"
```

---

### Task 14: Final verification — full build and test suite

- [ ] **Step 1: Run the full server test suite**

Run: `./gradlew :server:test`
Expected: All tests pass

- [ ] **Step 2: Run the full project build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Manual smoke test checklist**

This validates the cumulative result of all three chunks. Verify the following flows work correctly:

1. Onboarding: enter text → ReviewServices screen shows services + city → ReviewDescription screen shows edited description → photo → known name → published
2. Profile edit: open edit screen → see removable service chips → remove a service → add a service from catalog → edit description → save
3. LLM fallback: if LLM is unavailable, `editedDescription` falls back to capitalized input

- [ ] **Step 4: Commit any remaining fixes**

If any fixes were needed, stage only the specific files that were changed and commit:

```bash
git add <specific-files-changed>
git commit -m "fix: address issues found during final verification"
```
