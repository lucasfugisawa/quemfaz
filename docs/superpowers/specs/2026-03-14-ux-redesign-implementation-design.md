# QuemFaz UX Redesign — Implementation Design

**Date:** 2026-03-14
**Status:** Approved
**Reference:** `quemfaz-ux-ui-audit.md` (approved design direction)

---

## Context

The UX/UI audit in `quemfaz-ux-ui-audit.md` is complete and approved. It defines ~40 improvements across 5 implementation phases. This document specifies how those improvements will be planned and executed.

---

## Execution Model

- **One implementation plan per phase**, written immediately before execution (not all 5 upfront)
- **Claude Code implements**; the user performs manual end-to-end testing
- **Logical batches** within each phase — user tests after each batch, not after each individual item
- **Branch strategy:** Work directly on `main`; one commit per batch
- **If something breaks mid-batch:** Stop, revert to last clean commit, diagnose, re-execute with a corrected approach

Phases are executed in order (1 → 2 → 3 → 4 → 5) as defined in the audit's Phase 12. Each phase is independently shippable before the next begins.

---

## Phase 1 — Critical Usability Fixes

Phase 1 contains 10 items from the audit (P0/P1 priority). Grouped into 5 batches:

### Batch 1 — Infrastructure + Quick Wins (~2 hrs)

**Items:**
- Build toast/Snackbar system — new shared composable, wired into the main app scaffold so all screens can trigger toasts
- Replace emoji bottom nav icons (`🏠 ⭐ 👤`) with Material Icons (Outlined when inactive, Filled when active)
- Improve the Home search field placeholder from `"Search services (e.g. Plumber)"` to a multi-example string (e.g., `"Plumber, tutor, cleaner..."`) — the field currently has a placeholder but it's a single-service example that undersells the breadth of the product
- Rename "Confirm and Publish" → "Looks good, continue" in the `DraftReady` state of `OnboardingScreens.kt`

**Files:** `App.kt` (`NavigationBar` block, lines ~205–223), `OnboardingScreens.kt`, `HomeScreen.kt` (line ~80), new `ui/components/AppToast.kt` (or equivalent)

**Note on nav icons:** The emoji icons live inline in `App.kt`'s `MainFlow` composable — there is no separate bottom nav composable. Replace the three `Text("🏠")` / `Text("⭐")` / `Text("👤")` expressions with `Icon(Icons.Outlined.Home, ...)`, `Icon(Icons.Outlined.StarBorder, ...)` / `Icon(Icons.Filled.Star, ...)`, and `Icon(Icons.Outlined.Person, ...)`. Use the filled variant for the selected tab.

**Note on toast:** The toast system itself is wired in Batch 1, but the first user-visible toast fires in Batch 2 (when favorite toggle completes). The Batch 1 checkpoint does not attempt to verify a toast.

**Test checkpoint:**
1. Open the app — verify all three bottom nav tabs show vector icons (no emoji characters visible)
2. Tap the selected tab icon — verify it shows the filled variant; unselected tabs show outlined variant
3. Tap the Home tab search field — verify placeholder reads something like "Plumber, tutor, cleaner..."
4. Navigate to professional onboarding and reach the draft review screen — verify the primary button reads "Looks good, continue"

---

### Batch 2 — Search Flow (~3 hrs)

**Items:**
- Display `interpretedServices` from `SearchProfessionalsResponse` as a subtitle on `SearchResultsScreen` (field is returned by the API but currently unused in the UI)
- Add inline ❤️ favorite button to `ProfessionalCard` in search results, with optimistic toggle and toast feedback on completion

**Architectural decision — favorite state in search results:**
`SearchResultsScreen` currently renders from `HomeViewModel.searchUiState`. Neither `FavoritesViewModel` nor `ProfileViewModel` is scoped to the search results list. The correct owner is `HomeViewModel`:
- Add `favoritedProfileIds: StateFlow<Set<String>>` to `HomeViewModel`, populated by a favorites fetch that runs alongside the search call
- Add `toggleFavoriteFromSearch(profileId: String)` to `HomeViewModel`: optimistically flips the ID in/out of the set, calls the favorites API, and reverts on failure
- `ProfessionalCard` receives `isFavorited: Boolean` and `onFavoriteToggle: () -> Unit` as parameters
- The toast fires from `HomeViewModel` after the API call completes (success or failure)

**Files:** `HomeViewModel.kt`, `SearchScreens.kt`

**Test checkpoint:**
1. Search for any service (e.g., "encanador") — verify a subtitle appears below the result count showing the interpreted services (e.g., "Showing results for: Plumber · Pipe Repair")
2. If search returns no results, verify the subtitle reads something neutral (e.g., "No results found in [City]") with no crash
3. Tap ❤️ on a card directly from results — verify the icon toggles immediately (optimistic) and a toast confirms "Added to favorites"
4. Tap ❤️ again on the same card — verify it untoggled and a toast confirms removal
5. Tap ❤️ and immediately navigate away — verify the action completed silently in the background without error

---

### Batch 3 — Professional Profile Screen (~3 hrs)

**Items:**
- Sticky bottom contact bar — WhatsApp and Call buttons pinned at the bottom of the screen regardless of scroll position, replacing their current position as inline content
- Portfolio photos horizontal strip — surface `portfolioPhotoUrls` from `ProfessionalProfileResponse` as a horizontally scrollable row of photos below the avatar section (field confirmed in DTO, not currently rendered)

**Files:** `ProfileScreens.kt`

**Note on portfolio data:** `portfolioPhotoUrls` defaults to `emptyList()` on most test profiles. The strip should only render if the list is non-empty (conditional rendering). To verify this batch fully, either: (a) confirm a professional in the test environment has portfolio photos in their profile, or (b) temporarily hardcode a photo URL in the ViewModel during development to verify the strip layout, then remove before committing.

**Test checkpoint:**
1. Open any professional profile — scroll down to the bottom — verify WhatsApp and Call buttons remain pinned at the bottom of the screen at all scroll positions
2. Verify no content is obscured by the sticky bar (the scrollable content area ends above the bar, not behind it)
3. If a professional with portfolio photos is available: verify a horizontal strip of photos appears below the avatar section and is scrollable
4. If no professional with portfolio photos is available: verify the strip is simply absent (no empty placeholder, no crash)

---

### Batch 4 — Auth: OTP Input (~2 hrs)

**Items:**
- Replace the single OTP `TextField` in `OtpVerificationScreen` with 6 individual auto-advancing input boxes (one per digit)
- Each box: single-digit input, auto-advances focus to next on entry, auto-retreats on backspace
- Paste from clipboard fills all 6 boxes at once
- The Verify button becomes enabled (or verification triggers automatically) when all 6 digits are filled

**Why isolated:** Auth is the highest-risk flow to break. Isolating it ensures a failure here doesn't affect other batches.

**Files:** `AuthScreens.kt`

**Test checkpoint:**
1. Log out of the app
2. Enter your phone number and request an OTP — verify the OTP screen shows 6 individual boxes instead of a single text field
3. Type digits one at a time — verify focus auto-advances to the next box with each digit
4. Delete a digit — verify focus retreats to the previous box
5. Copy a 6-digit code to clipboard and paste — verify all boxes fill automatically
6. Fill in the 6th digit — verify the Verify button enables immediately (or the form auto-submits)

---

### Batch 5 — Professional Onboarding Progress Indicator (~1.5 hrs)

**Items:**
- Add a step progress indicator visible across all active onboarding states: `Idle` (step 1), `NeedsClarification` (step 2, conditional), `DraftReady` (step 2 or 3), `PhotoRequired` (next step), `KnownName` (final step before publish)
- During `Loading` states: indicator remains visible and frozen at the current step (it does not disappear or advance during async waits)
- During `Published` and `Error` states: indicator is hidden (these are terminal states, not steps)

**Step numbering:** The total step count depends on whether `NeedsClarification` appears (it's conditional). Handle this by showing a fixed total (e.g., "Step X of 4") using the maximum possible step count, which is the clearest and simplest approach.

**Files:** `OnboardingScreens.kt`

**Test checkpoint:**
1. Start professional onboarding from the home screen
2. On the description entry screen (Idle state) — verify "Step 1 of 4" (or equivalent indicator) is visible
3. Submit the description and wait for AI interpretation — verify the indicator remains visible and frozen during the loading state
4. On the draft review screen (DraftReady) — verify the step has advanced
5. Proceed through PhotoRequired and KnownName — verify the step advances at each screen
6. On the Published screen — verify the indicator is no longer shown

---

## Commit Convention

One commit per batch on `main`:

```
feat(ui): [batch description] — Phase 1, Batch N

- [item 1 summary]
- [item 2 summary]
```

No intermediate commits within a batch. Changes are staged and correct before committing.

---

## Handoff Format

After each batch commit:

```
## Batch N complete — [name]

### What changed
- [file]: [description]

### How to test
1. [specific action + expected result]
2. [specific action + expected result]

### Known limitations / deferred
- [anything intentionally left out]

### Ready for Batch N+1?
```

---

## Phases 2–5

Plans for Phases 2–5 will be written immediately before each phase begins, against the actual codebase state at that time. Each plan will follow the same structure: batch groupings, file-level detail, architectural decisions, test checkpoints, commit convention.

Reference the audit document for the full item list per phase:
- **Phase 2:** Flow simplification (city selection, name field merge, photo gate, pagination, neighborhoods chip input)
- **Phase 3:** Visual system consolidation (brand colors, avatar sizes, skeleton loading, empty states)
- **Phase 4:** Interaction polish (transitions, AI loading animation, search button animation)
- **Phase 5:** Marketplace signals and performance (engagement ranking, caching, explicit timeouts)
