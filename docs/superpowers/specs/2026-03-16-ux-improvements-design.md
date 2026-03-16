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
- Fields: `resolved_service_id`, `city_id`, `timestamp`
- Aggregation: count by service per city, ordered by frequency, within a rolling time window (e.g., 30 days)
- **Per-city with global fallback:** show city-level results when sufficient data exists (threshold: 10+ searches in the window), otherwise show platform-wide results
- Endpoint: `GET /api/services/popular?cityId={id}` returns top N service names

### Category Browsing Screen

- New screen: `Screen.CategoryBrowsing`
- Displays all service categories from the existing category model
- Each category expands or navigates to show its services
- Tapping a service triggers a search for that service

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
- Add `dateOfBirth` to relevant DTOs
- Server validates 18+ before creating professional profile draft

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
- The profile view screen fetches the phone from the user's account data instead
- Alternatively, the server populates a single `phone` field in the response from the user's account phone

---

## Summary of Data Model Changes

| Change | Table | Action |
|--------|-------|--------|
| `full_name` | `users` | Add column, migrate from `first_name`+`last_name`, drop old columns |
| `date_of_birth` | `users` | Add nullable `DATE` column |
| `contact_phone` | `professional_profiles` | Drop column |
| `whatsapp_phone` | `professional_profiles` | Drop column |
| search events | new table `search_events` | `id`, `resolved_service_id`, `city_id`, `created_at` |

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
