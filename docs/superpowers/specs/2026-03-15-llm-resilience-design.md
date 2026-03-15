# LLM Resilience Design — Onboarding and Search

**Date:** 2026-03-15
**Status:** Draft — awaiting design direction decision

---

## 1. Analysis of Current Flows

### 1.1 Professional Onboarding Flow

The onboarding flow uses a three-step pipeline:

1. **Draft creation** — User writes a free-text description in Portuguese (e.g., "Faço limpeza residencial em Batatais, bairro Centro"). The server sends this to GPT-4o Mini via `LlmProfessionalInputInterpreter`, which returns an `OnboardingInterpretation`: a list of canonical service IDs, city, neighborhoods, and optionally clarification questions.

2. **Clarification (conditional)** — If the LLM sets `needsClarification: true`, the user sees up to 2 follow-up questions. Their answers are sent back to the LLM with the original description for a second interpretation pass.

3. **Confirmation** — The user reviews the interpreted services, city, and neighborhoods, then confirms. This step does NOT call the LLM — it writes directly to the database.

**Where the LLM sits:** Steps 1 and 2. Step 3 is LLM-independent.

**Current error handling:** `LlmProfessionalInputInterpreter` wraps the LLM call in a try-catch. On ANY failure, it returns a `fallbackResponse`:
- Empty `interpretedServices`
- `missingFields = ["services", "city"]`
- Two hardcoded Portuguese follow-up questions asking the user to describe services and city

This means: **LLM failure does not crash the onboarding flow.** Instead, the user enters a clarification loop. However, the clarification itself also calls the LLM — so if the LLM is fully down, the user enters an infinite fallback→clarification→fallback loop. There is no escape path that bypasses the LLM entirely.

### 1.2 Search Flow

The search flow uses a four-stage pipeline:

1. **Query interpretation** — User's free-text query is sent to `LlmSearchQueryInterpreter`, which calls GPT-4o Mini. The LLM returns a `SearchInterpretation`: a single canonical service ID, optional city, and neighborhoods.

2. **Profile retrieval** — Database query filters published professionals by the interpreted service IDs and city.

3. **Ranking** — `ProfessionalSearchRankingService` scores each candidate across 7 factors: service match (up to 100pts), neighborhood match (30pts), engagement clicks (up to 40pts), engagement views (up to 25pts), profile completeness (15pts), recency (10pts), and city mismatch penalty (-50pts).

4. **Pagination and response** — Results are sorted by score, paginated, and returned with the interpreted service metadata.

**Where the LLM sits:** Step 1 only. Steps 2-4 are LLM-independent.

**Current error handling:** `LlmSearchQueryInterpreter` wraps the LLM call in a try-catch. On failure, it returns a `fallbackResult` with empty `serviceIds`, preserving only the `cityContext` passed from the client. The search still executes: the database returns all published professionals in the given city (unfiltered by service), and ranking uses only engagement and completeness factors. The UI shows results — no error is displayed — but the "Showing results for: ..." banner is hidden since `interpretedServices` is empty.

**Result:** Search degrades silently. Users get broad, less relevant results instead of an error.

### 1.3 Service Catalog

Both flows depend on a predefined canonical services catalog (`CanonicalServices.kt`): **23 services across 8 categories** (Cleaning, Repairs, Painting, Garden, Events, Beauty, Moving & Assembly, Other). Each service has an ID, Portuguese display name, description, and aliases. The LLM system prompts include the full catalog, constraining the LLM to only return valid IDs.

### 1.4 LLM Client

Both flows use `LlmAgentService`, which wraps the OpenAI API via the `ai.koog.prompt` library, targeting GPT-4o Mini with structured JSON output. There is **no explicit server-side timeout** on the LLM call — it relies on the underlying HTTP client's default behavior. The client-side HTTP timeout is 60 seconds total.

---

## 2. Failure Scenarios

### 2.1 LLM API Outage (Complete Unavailability)

**Onboarding:** User writes description → LLM call fails → fallback response with empty services → user enters clarification → LLM call fails again → second fallback → user is stuck in clarification loop with no way to proceed. **Flow is effectively blocked.**

**Search:** User enters query → LLM call fails → fallback with empty services → database returns all professionals in city → ranking by engagement only → user sees broad, unfiltered results. **Flow works but with significantly degraded relevance.**

### 2.2 LLM Timeout (Slow Response >60s)

**Onboarding:** Client-side timeout fires at 60s → `OnboardingUiState.Error` displayed → user sees "Something went wrong" + Retry button. User can retry indefinitely, but if the LLM is consistently slow, they'll keep hitting the same timeout.

**Search:** Client-side timeout fires at 60s → `SearchUiState.Error` displayed → user sees generic error message. User must manually re-search.

### 2.3 LLM Partial Failure (Invalid Response)

**Onboarding:** LLM returns malformed JSON or service IDs that don't exist in the catalog → deserialization fails → caught by try-catch → fallback response. Same clarification loop problem as complete outage.

**Search:** LLM returns invalid service ID → `CanonicalServices.findById()` returns null → `serviceIds` becomes empty → same broad-results degradation as complete outage. Transparent to user.

### 2.4 LLM Rate Limiting (429)

**Both flows:** Treated as a generic exception. Same behavior as API outage. High-traffic periods could trigger this for many users simultaneously.

### 2.5 LLM API Key Misconfiguration

**Both flows:** `LlmAgentService` logs a warning at startup but doesn't fail. All LLM calls fail at runtime. Every onboarding attempt enters fallback loop; every search returns broad results.

### 2.6 Summary

| Scenario | Onboarding Impact | Search Impact |
|----------|------------------|---------------|
| Complete outage | **Blocked** (clarification loop) | Degraded (broad results) |
| Timeout (>60s) | Error state (retry button) | Error state (manual re-search) |
| Invalid response | **Blocked** (clarification loop) | Degraded (broad results) |
| Rate limiting | **Blocked** (clarification loop) | Degraded (broad results) |
| API key missing | **Blocked** (clarification loop) | Degraded (broad results) |

**Core problem:** Onboarding has no escape path when the LLM is unavailable. Search degrades gracefully but loses its core differentiator (intelligent service matching).

---

## 3. Product Strategies

### Strategy A: Manual Service Selection Fallback

**Design:** When the LLM fails (or after N failed attempts), present the user with the canonical services catalog as a browsable, grouped list. User manually selects their services (onboarding) or the service they're looking for (search).

**Onboarding flow change:**
- After LLM failure, show a "Select your services manually" screen with all 29 services grouped by category (Cleaning, Repairs, Painting, etc.)
- User taps to select one or more services
- Separate fields for city and neighborhoods (text input or picker from supported cities)
- Skips clarification entirely — goes directly to review/confirm

**Search flow change:**
- After LLM failure, show "Browse by category" below the search bar
- User taps a category → sees services → taps one → search executes with that service ID
- OR: show the category browser proactively alongside the search bar

**UX implications:**
- Adds friction vs. free-text AI interpretation, but only as a fallback
- 23 services across 8 categories is manageable for manual browsing
- Portuguese display names and clear grouping make this intuitive
- For onboarding: multi-select needed (a professional may offer multiple services)
- For search: single-select sufficient (matches current single-service-ID behavior)

**Product tradeoffs:**
- (+) Completely unblocks both flows when LLM is unavailable
- (+) Zero dependency on external services for the fallback path
- (+) Users familiar with app categories (common in marketplace apps)
- (-) Less magical than AI interpretation — loses the "tell us in your own words" experience
- (-) Users must know the correct category names for their work

**Implementation complexity:** Low-medium. The service catalog already exists with display names and categories. UI is a standard grouped list with checkboxes (onboarding) or single-select (search). No server changes needed — the confirm/search endpoints already accept service IDs directly.

**Effect on core vision:** Preserves AI-first experience as the primary path. Fallback is only triggered on failure. The product still leads with natural language — manual selection is a safety net, not the default.

---

### Strategy B: Local Text Matching Fallback (No LLM)

**Design:** When the LLM fails, use the existing `CanonicalServices` alias-matching system to locally parse the user's text. Each canonical service has aliases (e.g., "diarista", "faxina" → `clean-house`). Match the user's input against these aliases using substring/fuzzy matching.

**Onboarding flow change:**
- After LLM failure, run `CanonicalServices.search(inputText)` (already exists — returns scored matches)
- If matches found: present as interpreted services (same UI as LLM success)
- If no matches: fall back to Strategy A (manual selection)

**Search flow change:**
- After LLM failure, run `CanonicalServices.search(query)` locally
- Use top match as the service ID for database query
- Same ranking and results display as normal

**UX implications:**
- Nearly invisible to user — same flow, just less intelligent matching
- Works well for exact or near-exact terms ("eletricista" → electrician)
- Fails for colloquial or complex descriptions ("meu chuveiro está pingando" → plumber? shower repair?)
- No city/neighborhood extraction — would need to fall back to client-provided city context

**Product tradeoffs:**
- (+) Transparent fallback — user flow unchanged
- (+) Works for the majority of simple, direct queries
- (-) Limited to exact alias matches — misses the nuance that makes LLM valuable
- (-) Cannot extract city/neighborhoods from text
- (-) Complex or ambiguous descriptions produce poor or no matches

**Implementation complexity:** Very low. `CanonicalServices.search()` already exists in `shared/commonMain` with a scoring mechanism (exact display name: 100pts, partial display name: 50pts, exact alias: 80pts, partial alias: 40pts, description: 10pts). Only need to wire it as the server-side fallback in `LlmProfessionalInputInterpreter.fallbackResponse()` and `LlmSearchQueryInterpreter.fallbackResult()` — both of which already import and use `CanonicalServices`.

**Effect on core vision:** Minimal impact. The AI-first flow remains primary. Local matching is a reasonable approximation for simple cases. Users with complex descriptions would still need manual selection as a second-tier fallback.

---

### Strategy C: Multiple LLM Providers with Automatic Failover

**Design:** Configure a secondary LLM provider (e.g., Anthropic Claude, Google Gemini, or a self-hosted model) as a failover when the primary (OpenAI) is unavailable. `LlmAgentService` tries the primary provider first, and if it fails, retries with the secondary.

**Flow changes:** None at the UI level. The failover is entirely server-side.

**UX implications:**
- Invisible to user — same experience regardless of which provider serves the request
- Slightly increased latency on failover (primary timeout + secondary call)

**Product tradeoffs:**
- (+) Dramatically reduces probability of total LLM unavailability (two independent providers)
- (+) No UI changes, no UX degradation
- (-) Requires maintaining prompts compatible with multiple LLM providers
- (-) Structured output format may differ between providers
- (-) Two API contracts to maintain and test
- (-) Does not solve the problem if BOTH providers are down simultaneously
- (-) Cost: paying for a secondary provider that's rarely used
- (-) Prompt engineering effort: each provider may interpret service catalogs differently

**Implementation complexity:** Medium-high. Requires abstracting `LlmAgentService` to support multiple backends, implementing health checks or circuit breakers, and maintaining parallel prompt configurations. Structured output support varies significantly between providers.

**Effect on core vision:** Strengthens the AI-first vision by making it more reliable. Does not address the fundamental question of what happens when AI is completely unavailable.

---

### Strategy D: Hybrid Input (AI + Structured Simultaneously)

**Design:** Always present both input modes: a free-text field for AI interpretation AND a manual service category selector. User can use either or both. AI interprets the text and pre-selects services, but user can manually adjust the selection before confirming.

**Onboarding flow change:**
- Description input field + "Or select services manually" expandable section below
- If user types text: LLM interprets → pre-selects services in the category list
- If LLM fails: text field still works for description, manual selection unaffected
- User can always add/remove services manually regardless of LLM status

**Search flow change:**
- Search bar + category chips/tabs below
- User can type a query OR tap a category to browse
- If LLM interprets successfully: results filtered by interpreted service
- If LLM fails: category browsing still works perfectly

**UX implications:**
- More complex UI — two input mechanisms on screen
- Risk of confusing users ("should I type or tap?")
- More powerful for users who want precise control
- Solves the "LLM misinterpreted my services" problem (user can correct without re-describing)

**Product tradeoffs:**
- (+) Always works regardless of LLM availability
- (+) Gives users correction ability even when LLM is available
- (+) AI enhances rather than gates the experience
- (-) More complex screen layout
- (-) May reduce the "magic" factor of the AI-first experience
- (-) Users may default to manual selection, underutilizing the AI differentiator

**Implementation complexity:** Medium. Requires redesigning onboarding and search screens to accommodate dual input. Service selection UI needed either way (for Strategy A). The main additional work is integrating both modes on the same screen and handling the interaction between AI pre-selection and manual adjustment.

**Effect on core vision:** Shifts from "AI-first" to "AI-enhanced." The product still uses AI as its primary intelligence, but exposes the structured fallback as a parallel option rather than a hidden emergency path. This is a meaningful philosophical shift.

---

### Strategy E: Graceful Degradation with User Communication

**Design:** Keep the current AI-first flow as primary. When the LLM fails, show a clear message explaining that AI interpretation is temporarily unavailable, and offer a manual selection flow as an alternative. Unlike Strategy A (silent fallback), this strategy explicitly communicates the degradation.

**Onboarding flow change:**
- LLM fails → show: "Our AI assistant is temporarily unavailable. You can select your services manually."
- Present the category browser (same as Strategy A)
- Add a "Try AI again" button alongside the manual option

**Search flow change:**
- LLM fails → show: "Smart search is temporarily unavailable. Browse by category instead."
- Show category browser with service list
- Keep the search bar active for retry

**UX implications:**
- Honest with users about degradation
- Sets correct expectations (results may be less precise)
- The "Try AI again" option lets users wait for recovery without abandoning the flow
- Manual path is clearly secondary, preserving the AI-first positioning

**Product tradeoffs:**
- (+) Transparent and trustworthy — users know what's happening
- (+) Preserves AI-first framing (manual is explicitly the backup)
- (+) "Try AI again" captures users who'd rather wait for AI
- (-) Draws attention to the failure — some users might lose confidence
- (-) Extra screen state to design and maintain

**Implementation complexity:** Low-medium. Same as Strategy A (manual selection UI) plus a transitional "AI unavailable" message screen. Minimal server changes.

**Effect on core vision:** Strongly preserves AI-first vision. The communication makes it clear that AI is the intended experience, and manual selection is a temporary accommodation. This actually reinforces the value proposition rather than undermining it.

---

## 4. Recommended Direction

### Recommendation: Strategy B + E (Local Text Matching + Graceful Degradation with User Communication)

**Approach: Two-tier fallback with honest communication.**

**Tier 1 — Local text matching (invisible):** When the LLM fails, immediately attempt local alias matching against the canonical services catalog. This handles the majority of simple, common queries ("eletricista", "pintor", "faxina") without any visible degradation. The user sees the same flow — just powered by alias matching instead of the LLM.

**Tier 2 — Manual selection with communication (visible):** If local matching also produces no results (complex or ambiguous input), show a clear message that AI interpretation is temporarily unavailable and present the manual category browser. Include a "Try AI again" option.

**Why this combination:**

1. **Minimizes visible degradation.** Most real-world queries in a Brazilian home services marketplace use common service terms. "Eletricista", "pintor", "encanador", "faxina" — these all have direct alias matches in the catalog. Local matching handles these silently. Users only see the manual fallback for genuinely ambiguous queries that the LLM would have disambiguated.

2. **Preserves the AI-first identity.** The product leads with natural language input. The fallback chain (LLM → local matching → manual selection) progressively reduces intelligence but never blocks the user. Most users will never notice the degradation because their common queries match aliases.

3. **Honest about limitations.** When manual selection IS needed, the product communicates clearly. This builds trust. The "Try AI again" button signals that AI is the intended experience and will return.

4. **Minimal implementation cost.** Local matching already exists (`CanonicalServices.search()`). The manual selection UI (grouped category list) is standard marketplace UX and modest to build. The "AI unavailable" message is a simple conditional screen state. No secondary LLM provider needed.

5. **Solves the onboarding deadlock.** The critical bug — the infinite clarification loop when the LLM is fully down — is resolved. Users always have a path to complete onboarding.

6. **Search degrades gracefully.** The current silent fallback (broad results) is acceptable for search as a third tier. The chain becomes: LLM interpretation → local alias matching → broad results. Each tier is less precise but still functional. Specifically: if `CanonicalServices.search()` returns matches, the top-scoring service ID is used to filter the database query (same as LLM success). If it returns no matches, the search proceeds with empty service IDs — producing the current broad-results behavior (all published professionals in city).

### Specifically not recommended:

- **Strategy C (multiple LLM providers):** High implementation cost, doesn't solve the fundamental problem (what if ALL providers are down), and adds ongoing maintenance burden. Worth revisiting later as a reliability improvement, but not a substitute for a local fallback.

- **Strategy D (hybrid input):** Overcomplicates the UI for the current product stage. The dual-mode screen design is appropriate for a mature product with power users, but premature for an early marketplace. It also dilutes the AI-first messaging.

### Implementation sequence:

1. Wire `CanonicalServices.search()` as the first fallback in both `LlmProfessionalInputInterpreter.fallbackResponse()` and `LlmSearchQueryInterpreter.fallbackResult()`
2. Build the manual service selection UI (grouped category list, single-select for search, multi-select for onboarding)
3. Add the "AI unavailable" transitional state to onboarding and search screens
4. Add "Try AI again" button alongside manual selection
5. Fix the clarification loop: when `LlmProfessionalInputInterpreter.fallbackResponse()` fires (LLM failure), return a new response field (e.g., `llmUnavailable: true`) in `CreateProfessionalProfileDraftResponse`. The client checks this field: if true, it skips the clarification step and routes directly to the manual service selection screen. This is stateless — no failure counter needed. Any single LLM failure during interpretation triggers the manual fallback path. The search flow's `fallbackResult()` similarly sets an `llmUnavailable` flag, prompting the client to show the category browser

---

## Appendix: Service Catalog Summary

| Category | Count | Example Services |
|----------|-------|-----------------|
| Cleaning | 4 | Limpeza Residencial, Limpeza Pós-Obra, Limpeza de Sofá, Limpeza de Terreno |
| Repairs | 5 | Eletricista, Encanador, Marido de Aluguel, Montagem de Móveis, Troca de Chuveiro |
| Painting | 2 | Pintura Residencial, Pintura Comercial |
| Garden | 3 | Jardinagem, Poda de Árvores, Roçagem |
| Events | 3 | Garçom, Bartender, Ajudante de Cozinha |
| Beauty | 3 | Manicure, Cabeleireiro(a), Maquiador(a) |
| Moving | 2 | Ajudante de Mudança, Pequenos Fretes |
| Other | 1 | Outros Serviços |
| **Total** | **23** | |

23 services with comprehensive Portuguese aliases for local text matching.
