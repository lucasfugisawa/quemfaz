# QuemFaz UX Improvements — Design Spec

**Date:** 2026-03-16
**Status:** Approved
**Scope:** Voice-first interaction, home screen redesign, onboarding simplification, data model changes

---

## Context

QuemFaz is a hyperlocal service marketplace where people search for local professionals and professionals offer services. A key differentiator is natural language interaction (including voice input) powered by LLM interpretation.

Several aspects of the current UX do not fully support this vision. This spec defines UX improvements across seven areas to better align the product experience with its core value proposition.

---

## 1. Voice-First Interaction

### Problem

The microphone icon currently appears as a disabled emoji placeholder inside input fields — almost invisible. Voice input is a core differentiator but is presented as a secondary accessory.

### Design

Both voice input contexts (home screen search and professional onboarding description) use the same visual pattern:

1. **Input field** — search `OutlinedTextField` or description `TextField`
2. **Natural language example hint** — displayed below the input field in secondary text. Examples:
   - Home search: *Ex: "Preciso de alguém para pintar minha casa"*
   - Onboarding: *Ex: "Sou pintor residencial com 10 anos de experiência"*
3. **Large circular mic button** — prominent, centered, with gradient fill (primary color). Sized at ~72dp diameter.
4. **Label below mic** — "Toque para falar"

The example hint serves double duty: it communicates that natural language works AND shows users what kind of input is expected.

### Behavior

- Tapping the mic button activates native OS speech-to-text
- Transcribed text populates the input field in real-time
- The user can review and edit the transcribed text before submitting
- The existing `InputMode` enum (`TEXT` / `VOICE`) is sent with the request so the server knows the input source
- If the user types instead of speaking, `InputMode.TEXT` is sent (current behavior)

### Platform Implementation

- **Android:** `SpeechRecognizer` API
- **iOS:** `SFSpeechRecognizer` API
- Both accessed via Kotlin Multiplatform `expect`/`actual` declarations

### Permissions

- Microphone/speech recognition permissions are requested on first mic button tap
- If permission is denied, show a brief message explaining why it's needed and fall back to text input
- The mic button remains visible even after denial (user can grant permission later via settings)

### Error Handling

- If speech recognition is unavailable on the device, hide the mic button entirely
- If recognition starts but fails or times out, show a brief error and leave the text field as-is
- No blocking error dialogs — errors are non-intrusive since text input is always available

---

## 2. Voice Transcription Strategy

### Decision: Native OS STT for MVP

**Rationale:**
- Zero additional cost
- Low latency
- Works offline or with minimal infrastructure
- The LLM interpretation stage on the server already handles messy, colloquial, or poorly punctuated input
- The pipeline is: voice → native STT → text → server LLM interpretation → canonical services

**LLM-based transcription (e.g., Whisper)** is not justified for MVP because:
- It adds cost per request
- It increases latency (network round-trip before the user even sees their text)
- It adds infrastructure complexity
- The marginal quality improvement is absorbed by the downstream LLM interpretation

**Revisit trigger:** Real user feedback indicating native STT quality is causing search/interpretation failures.

---

## 3. Home Screen Redesign

### Problem

The home screen currently shows only a search field and a conditional "earn money" card. It feels empty, offers no guidance, and does not help users discover services.

### Design

Top-to-bottom layout:

1. **Top bar** — city selector (existing) + profile avatar (existing)
2. **Title** — "O que você precisa hoje?" (existing)
3. **Search field** — `OutlinedTextField` with search icon and placeholder
4. **Natural language example hint** — *Ex: "Preciso de alguém para pintar minha casa"*
5. **Mic button** — large circular CTA with "Toque para falar" label
6. **Popular searches section:**
   - Section header: "Mais buscados na sua cidade"
   - Horizontal scrollable row of service chips (canonical service names)
   - Tapping a chip triggers a search for that service
7. **Category link** — "Ver todas as categorias" → navigates to a dedicated category browsing screen
8. **Earn money card** — conditional, shown when user has no professional profile (existing, moved to bottom)

### Popular Searches — Data Source

- Every search that resolves to canonical services via LLM interpretation logs a search event
- Search events are recorded during the existing search flow (no new logging endpoint)
- Fields: `id` (TEXT, UUID), `resolved_service_id` (TEXT, FK to services), `city_name` (TEXT), `created_at` (TIMESTAMP)
- Server caches aggregation results (refresh every 15 minutes) to avoid computing on every home screen load
- Search events are logged after LLM interpretation resolves services. If no services are resolved, no event is logged.
- Aggregation: count by service per city, ordered by frequency, within a rolling 30-day window
- **Per-city with global fallback:** show city-level results when sufficient data exists (server-configured threshold, initially 10 searches in the window), otherwise show platform-wide results. The endpoint signals which mode was used so the UI can adjust the header (e.g., "Mais buscados na sua cidade" vs "Mais buscados no QuemFaz").
- Endpoint: `GET /api/services/popular?cityName={name}` returns top 8 services
- Response DTO (`PopularServicesResponse` in `shared/contract/`):
  ```
  data class PopularServiceDto(
      val serviceId: String,
      val displayName: String
  )
  data class PopularServicesResponse(
      val services: List<PopularServiceDto>,
      val isLocalResults: Boolean  // true = city-level, false = global fallback
  )
  ```

### Category Browsing Screen

- New screen: `Screen.CategoryBrowsing`
- Displays all service categories from the existing category model
- Flat list grouped by category, each category as a section header with its services listed below
- Tapping a service triggers a search for that service
- Reuses existing `CatalogApiClient.getCatalog()` endpoint — no new API needed
- Standard loading/error states

---

## 4. Natural Language Communication

### Problem

The UI does not communicate that users can search using full natural language sentences. Users may assume keyword-only search.

### Solution

Addressed by the natural language example hints in the voice-first pattern (Section 1):
- Below the home search field: *Ex: "Preciso de alguém para pintar minha casa"*
- Below the onboarding description field: *Ex: "Sou pintor residencial com 10 anos de experiência"*

The placeholder text inside the search field can also reinforce this (e.g., "Diga ou digite o que você precisa...").

No additional UI components are needed. The example hints are sufficient to teach the interaction model.

---

## 5. User Onboarding Simplification

### Problem

- The backend stores `firstName` and `lastName` as separate fields
- The My Profile screen shows separate "Nome" and "Sobrenome" fields
- Brazilian names don't split cleanly (e.g., "Maria da Silva Santos")

### Design

**Single `fullName` field everywhere:**

- **Auth flow:** Phone → OTP → "Nome completo" screen (already exists, just needs validation update)
- **Validation:** at least 2 words (space-separated), trimmed
- **My Profile screen:** single "Nome completo" field replacing separate "Nome" / "Sobrenome"
- **Backend:** single `full_name` column replacing `first_name` / `last_name`

### Data Model Changes

**Database migration:**
- Add `full_name` column to `users` table
- Populate from `CONCAT(first_name, ' ', last_name)` for existing users
- Drop `first_name` and `last_name` columns

**DTO changes:**
- `CompleteUserProfileRequest`: replace `firstName`/`lastName` with `fullName`
- `UserProfileResponse`: replace `firstName`/`lastName` with `fullName`
- `ProfessionalProfileResponse`: replace `firstName`/`lastName` with `fullName`

**All three layers** (shared DTOs, server, client) must be updated in one coordinated change.

---

## 6. Birth Date for Professionals

### Problem

Professionals must be 18+ but there is no age verification in the current flow.

### Design

- Birth date is asked **only during professional onboarding**, as the first step (before the description textarea)
- Not asked during account creation (keeps sign-up flow minimal)
- Screen shows: "Para oferecer serviços, você precisa ter pelo menos 18 anos."
- Input: date picker for birth date
- Validation: user must be 18+ based on current date
- If under 18: blocked with a clear message, cannot proceed
- Birth date stored in user profile (`date_of_birth` column)

### Professional Onboarding Flow (Updated)

1. **Birth date verification** (new)
2. Description (textarea + example hint + mic button)
3. Clarification questions (if AI needs more info)
4. Review services
5. Review description
6. Photo
7. Known name (optional)

### Data Model Changes

- Add `date_of_birth` column to `users` table (nullable `DATE`)
- Add `dateOfBirth` to `UserProfileResponse`
- New endpoint to save birth date: `PUT /auth/me/date-of-birth` with `UpdateDateOfBirthRequest(dateOfBirth: String)` (ISO date format) — under `/auth` for consistency with existing user endpoints
- Server validates 18+ both when saving birth date and before creating professional profile draft
- Client validates 18+ locally before submitting (immediate feedback)
- If the user already has a `date_of_birth` stored (e.g., from a previous onboarding attempt), skip the birth date step in the onboarding flow
- New `OnboardingUiState.BirthDateRequired` state is the initial onboarding state (replaces `Idle` as the starting state)
- After birth date is saved, transition to `Idle` (description input)
- If user already has `date_of_birth` stored, skip directly to `Idle`
- Back press from `BirthDateRequired` exits onboarding (pops back to the previous screen in the nav stack)
- Server rejects draft creation with HTTP 422 if `date_of_birth` is missing or user is under 18. Error response: `{ "error": "UNDERAGE" }` or `{ "error": "DATE_OF_BIRTH_REQUIRED" }`
- `dateOfBirth` is `String` in ISO-8601 format (e.g., "1990-05-15") in all DTOs

---

## 7. Professional Contact Phone

### Problem

Professional profiles have separate `contactPhone` and `whatsAppPhone` fields that start empty, even though the user already has a verified phone from authentication. This creates redundant data entry and potential inconsistency.

### Design

- **Single phone number:** the account phone number (verified via OTP) is used as the professional's contact number
- **Remove** `contact_phone` and `whatsapp_phone` from the professional profile table
- **Remove** phone fields from the professional onboarding flow
- **Remove** phone fields from the profile edit screen
- **Profile view:** both "WhatsApp" and "Ligar" buttons remain, both using the account phone number
  - WhatsApp: opens `https://wa.me/{digits}`
  - Ligar: opens `tel:{phone}`

### Data Model Changes

**Database migration:**
- Drop `contact_phone` and `whatsapp_phone` columns from professional profiles table

**DTO changes:**
- Remove `whatsAppPhone` and `contactPhone` from `ProfessionalProfileResponse`
- Add a single `phone` field to `ProfessionalProfileResponse`, populated by the server from the user's account phone number. This is necessary because the profile view is used for viewing other users' profiles, not just your own. Note: this is consistent with the current behavior where `contactPhone` is already visible to all profile viewers — the difference is that now the professional cannot set a different contact number.
- Remove `contactPhone` and `whatsAppPhone` from `ConfirmProfessionalProfileRequest`
- Update `EditProfessionalProfileViewModel.saveProfile()` to remove phone parameters

---

## Summary of Data Model Changes

| Change | Table | Action |
|--------|-------|--------|
| `full_name` | `users` | Add column, migrate from `first_name`+`last_name`, drop old columns |
| `date_of_birth` | `users` | Add nullable `DATE` column |
| `contact_phone` | `professional_profiles` | Drop column |
| `whatsapp_phone` | `professional_profiles` | Drop column |
| search events | new table `search_events` | `id`, `resolved_service_id`, `city_name`, `created_at` |

## Summary of Screen Changes

| Screen | Changes |
|--------|---------|
| Home | Add example hint, prominent mic button, popular searches section, category link |
| Search results | No changes |
| Auth - Name | Update validation (2+ words), update DTO to `fullName` |
| My Profile | Merge name fields into single "Nome completo" |
| Professional onboarding | Add birth date as step 1, add mic button + example hint to description step, remove phone fields |
| Profile edit | Remove phone fields |
| Profile view | Use account phone for both WhatsApp and Ligar buttons |
| Category browsing | New screen |
