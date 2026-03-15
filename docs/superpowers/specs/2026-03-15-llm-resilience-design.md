# LLM Resilience Design — Onboarding and Search

**Date:** 2026-03-15
**Status:** Direction decided — ready for implementation planning

---

## Product Decisions

The following product decisions have been made and are reflected throughout this document:

1. **Neighborhoods removed from the product.** The product targets smaller cities where neighborhood segmentation is unnecessary (YAGNI). Neighborhoods are removed from onboarding, search, interpretation, ranking, UI, and data model.
2. **City remains mandatory.** City is a core concept in both search and onboarding.
3. **Search uses the city selected on the main screen.** The user does not type or select a city during search — the search operates within the city already selected in the main interface. City filtering is mandatory; results are always constrained to the selected city.
4. **Onboarding requires one or more cities.** Professionals specify the cities where they operate, chosen from the platform's set of supported cities. At least one city is required. The city currently selected on the main screen is automatically pre-selected; the professional may add or remove cities.
5. **LLM failure must be distinguished from clarification.** The flow must never send users into clarification loops when the LLM is unavailable. Fallback mechanisms must engage immediately.
6. **LLM timeouts must be short and configurable.** Onboarding and search must feel responsive. The system should fail fast and fall back. Timeout values must be configurable at the application level, not hardcoded.
7. **Operational failures must not be exposed to users.** LLM failures (outages, rate limits, misconfig) are server-side concerns. They must be logged and monitored but never surfaced as technical errors. User-facing behavior must remain simple and non-technical.
8. **Chosen strategy direction:** Local text matching fallback + manual service selection fallback (three-tier: LLM → local matching → manual selection).

---

## 1. Analysis of Current Flows

### 1.1 Professional Onboarding Flow

The onboarding flow uses a three-step pipeline:

1. **Draft creation** — User writes a free-text description in Portuguese (e.g., "Faço limpeza residencial em Batatais"). The server sends this to GPT-4o Mini via `LlmProfessionalInputInterpreter`, which returns an `OnboardingInterpretation`: a list of canonical service IDs and optionally clarification questions.

2. **Clarification (conditional)** — If the LLM sets `needsClarification: true`, the user sees up to 2 follow-up questions. Their answers are sent back to the LLM with the original description for a second interpretation pass.

3. **Confirmation** — The user reviews the interpreted services and cities, then confirms. This step does NOT call the LLM — it writes directly to the database.

**City in onboarding:** The professional must specify one or more cities where they operate, chosen from the platform's supported cities. The city currently selected on the main screen is pre-selected by default. The professional may add or remove cities, but at least one must remain selected. City selection is independent of the LLM — it is handled by a structured UI picker and is unaffected by LLM availability.

**Where the LLM sits:** Steps 1 and 2. Step 3 is LLM-independent. City selection is always LLM-independent.

**Current error handling:** `LlmProfessionalInputInterpreter` wraps the LLM call in a try-catch. On ANY failure, it returns a `fallbackResponse`:
- Empty `interpretedServices`
- `missingFields = ["services"]`
- Hardcoded Portuguese follow-up questions asking the user to describe services

This means: **LLM failure does not crash the onboarding flow.** Instead, the user enters a clarification loop. However, the clarification itself also calls the LLM — so if the LLM is fully down, the user enters an infinite fallback→clarification→fallback loop. There is no escape path that bypasses the LLM entirely.

**Required fix:** The system must distinguish between normal clarification (LLM responded but needs more info) and LLM failure (LLM is unavailable). On LLM failure, the flow must bypass clarification entirely and engage fallback mechanisms. See section 4.

### 1.2 Search Flow

The search flow uses a three-stage pipeline:

1. **Query interpretation** — User's free-text query is sent to `LlmSearchQueryInterpreter`, which calls GPT-4o Mini. The LLM returns a `SearchInterpretation`: a single canonical service ID.

2. **Profile retrieval** — Database query filters published professionals by the interpreted service ID within the city currently selected on the main screen. City filtering is mandatory — results are always constrained to the selected city.

3. **Ranking** — `ProfessionalSearchRankingService` scores each candidate across factors: service match (up to 100pts), engagement clicks (up to 40pts), engagement views (up to 25pts), profile completeness (15pts), recency (10pts), and city mismatch penalty (-50pts).

4. **Pagination and response** — Results are sorted by score, paginated, and returned with the interpreted service metadata.

**City in search:** The search always operates within the city selected on the main screen. The user does not type or select a city during search. City context is passed from the client to the server as part of the search request.

**Where the LLM sits:** Step 1 only. Steps 2-4 are LLM-independent.

**Current error handling:** `LlmSearchQueryInterpreter` wraps the LLM call in a try-catch. On failure, it returns a `fallbackResult` with empty `serviceIds`, preserving the city context passed from the client. The search still executes: the database returns all published professionals in the selected city (unfiltered by service), and ranking uses only engagement and completeness factors. The UI shows results — no error is displayed — but the "Showing results for: ..." banner is hidden since `interpretedServices` is empty.

**Result:** Search degrades silently. Users get broad, less relevant results instead of an error.

### 1.3 Service Catalog

Both flows depend on a predefined canonical services catalog (`CanonicalServices.kt`): **23 services across 8 categories** (Cleaning, Repairs, Painting, Garden, Events, Beauty, Moving & Assembly, Other). Each service has an ID, Portuguese display name, description, and aliases. The LLM system prompts include the full catalog, constraining the LLM to only return valid IDs.

### 1.4 LLM Client

Both flows use `LlmAgentService`, which wraps the OpenAI API via the `ai.koog.prompt` library, targeting GPT-4o Mini with structured JSON output.

**Current timeout behavior:** There is no explicit server-side timeout on the LLM call — it relies on the underlying HTTP client's default behavior. The client-side HTTP timeout is 60 seconds total.

**Required change:** LLM calls must have short, configurable timeouts at the application level. The current 60-second client-side timeout is too slow for onboarding and search flows, which must feel responsive. The system should fail fast and engage fallback mechanisms rather than making the user wait. Timeout values must not be hardcoded — they should be configurable (e.g., via application configuration or environment variables) so they can be tuned without code changes.

---

## 2. Failure Scenarios

### 2.1 LLM API Outage (Complete Unavailability)

**Onboarding (current):** User writes description → LLM call fails → fallback response with empty services → user enters clarification → LLM call fails again → second fallback → user is stuck in clarification loop with no way to proceed. **Flow is effectively blocked.**

**Onboarding (required):** LLM call fails → system immediately engages local text matching fallback → if no matches, routes to manual service selection. User is never sent into a clarification loop. The failure is logged and monitored server-side but not surfaced to the user as a technical error.

**Search (current):** User enters query → LLM call fails → fallback with empty services → database returns all professionals in city → ranking by engagement only → user sees broad, unfiltered results. **Flow works but with significantly degraded relevance.**

**Search (required):** LLM call fails → local text matching attempted first → if matches found, results filtered by matched service → if no matches, broad results returned. The failure is logged server-side. The user sees results without any technical error messaging.

### 2.2 LLM Timeout (Slow Response)

**Current:** Client-side timeout fires at 60s → error state displayed → user sees "Something went wrong."

**Required:** Server-side LLM timeout fires quickly (short, configurable value) → fallback mechanisms engage → user experiences a brief delay, then sees results. No error state is shown to the user. The timeout is logged and monitored server-side.

### 2.3 LLM Partial Failure (Invalid Response)

**Onboarding:** LLM returns malformed JSON or service IDs that don't exist in the catalog → deserialization fails → caught by try-catch → local text matching fallback engages. User is never sent into a clarification loop for an LLM failure.

**Search:** LLM returns invalid service ID → `CanonicalServices.findById()` returns null → local text matching fallback engages → if no matches, broad results returned. Transparent to user.

### 2.4 LLM Rate Limiting (429)

**Both flows:** Treated as an LLM failure. Fallback mechanisms engage immediately. The rate-limiting event is logged and generates monitoring signals. The user is not informed of the technical cause.

### 2.5 LLM API Key Misconfiguration

**Both flows:** `LlmAgentService` logs a warning at startup. All LLM calls fail at runtime, engaging fallback mechanisms. This should generate alerts for operations. Users experience the fallback path (local matching → manual selection) but are never shown technical error information.

### 2.6 Summary

| Scenario | Onboarding (required) | Search (required) |
|----------|----------------------|-------------------|
| Complete outage | Local matching → manual selection | Local matching → broad results |
| Timeout | Fail fast → local matching → manual selection | Fail fast → local matching → broad results |
| Invalid response | Local matching → manual selection | Local matching → broad results |
| Rate limiting | Local matching → manual selection | Local matching → broad results |
| API key missing | Local matching → manual selection | Local matching → broad results |

**Operational handling:** All failure scenarios are logged with sufficient detail for diagnosis. Persistent failures (e.g., API key misconfiguration, sustained outages) should generate alerts. No failure scenario exposes technical error information to users.

**Core requirement:** The user is never blocked. The fallback chain always produces a usable outcome.

---

## 3. Product Strategies (Evaluated)

### Strategy A: Manual Service Selection Fallback

**Design:** When the LLM fails (or after N failed attempts), present the user with the canonical services catalog as a browsable, grouped list. User manually selects their services (onboarding) or the service they're looking for (search).

**Onboarding flow change:**
- After LLM failure, show a "Select your services" screen with all 23 services grouped by category (Cleaning, Repairs, Painting, etc.)
- User taps to select one or more services
- City selection is handled separately by a structured picker (unaffected by LLM status)
- Skips clarification entirely — goes directly to review/confirm

**Search flow change:**
- After LLM failure, show "Browse by category" below the search bar
- User taps a category → sees services → taps one → search executes with that service ID within the currently selected city
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

**Design:** When the LLM fails, use the existing `CanonicalServices` alias-matching system to parse the user's text server-side. Each canonical service has aliases (e.g., "diarista", "faxina" → `clean-house`). Match the user's input against these aliases using substring/fuzzy matching.

**Onboarding flow change:**
- After LLM failure, run `CanonicalServices.search(inputText)` server-side (already exists — returns scored matches)
- If matches found: present as interpreted services (same UI as LLM success)
- If no matches: fall back to Strategy A (manual selection)

**Search flow change:**
- After LLM failure, run `CanonicalServices.search(query)` server-side
- Use top match as the service ID for database query within the currently selected city
- Same ranking and results display as normal

**UX implications:**
- Nearly invisible to user — same flow, just less intelligent matching
- Works well for exact or near-exact terms ("eletricista" → electrician)
- Fails for colloquial or complex descriptions ("meu chuveiro está pingando" → plumber? shower repair?)
- City is not extracted from text — city context comes from the main screen selection (search) or structured picker (onboarding), so this is not an issue

**Product tradeoffs:**
- (+) Transparent fallback — user flow unchanged
- (+) Works for the majority of simple, direct queries
- (-) Limited to exact alias matches — misses the nuance that makes LLM valuable
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
- If LLM interprets successfully: results filtered by interpreted service within selected city
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

**Design:** Keep the current AI-first flow as primary. When the LLM fails, show a clear message explaining that AI interpretation is temporarily unavailable, and offer a manual selection flow as an alternative.

**Not selected.** This strategy conflicts with the product decision that operational failures must not be exposed to users as technical errors. Mentioning "AI assistant is temporarily unavailable" surfaces an infrastructure concern that should remain internal. See section 4 for the chosen approach, which handles LLM failure silently through the fallback chain.

---

## 4. Chosen Direction

### Strategy B + A: Local Text Matching + Manual Service Selection (Three-Tier Fallback)

**Approach: Three-tier fallback that is seamless and non-technical from the user's perspective.**

The system uses a progressive fallback chain. Each tier is less intelligent but still functional. The user is never blocked, and operational failures are never surfaced as technical errors.

**Tier 1 — LLM interpretation (primary):** The LLM interprets free-text input and returns canonical service IDs. This is the primary path and produces the best results. LLM calls use short, configurable timeouts to ensure responsiveness.

**Tier 2 — Local text matching (invisible fallback):** When the LLM fails (timeout, outage, invalid response, rate limit, misconfiguration), the server immediately attempts local alias matching via `CanonicalServices.search()`. This handles common, direct service terms ("eletricista", "pintor", "faxina") without any visible degradation. The user sees the same flow — just powered by alias matching instead of the LLM. The failure is logged and monitored server-side.

**Tier 3 — Manual service selection (visible fallback):** If local matching also produces no results (complex or ambiguous input), the user is presented with a manual service category browser. This is presented as a natural part of the flow — not as an error or degradation notice. The user browses 23 services across 8 categories and selects manually.

### Onboarding behavior

1. User writes free-text description
2. Server attempts LLM interpretation (short configurable timeout)
3. **If LLM succeeds and needs clarification:** Normal clarification flow — user answers follow-up questions, LLM re-interprets
4. **If LLM fails:** Server attempts `CanonicalServices.search()` on the input text
   - If matches found: present as interpreted services (same UI as LLM success path)
   - If no matches: route to manual service selection (grouped category browser)
   - **The clarification loop is never entered on LLM failure.** The server returns an `llmUnavailable` flag in the response. The client checks this flag and bypasses clarification, routing directly to either the matched-services confirmation or the manual selection screen. This is stateless — any single LLM failure triggers the fallback path.
5. User reviews services and cities, then confirms
6. City selection is handled by a structured picker throughout, independent of the LLM. The city currently selected on the main screen is pre-selected. The professional may add or remove cities from the platform's supported set. At least one city is required.

### Search behavior

1. User types free-text search query
2. Server attempts LLM interpretation (short configurable timeout). City context is provided by the client from the main screen selection — city is not extracted from the query text.
3. **If LLM succeeds:** Search filters by interpreted service ID within the selected city
4. **If LLM fails:** Server attempts `CanonicalServices.search()` on the query text
   - If matches found: top-scoring service ID is used to filter the database query (same as LLM success)
   - If no matches: search proceeds with empty service IDs — returning all published professionals in the selected city, ranked by engagement and completeness
5. Results are always constrained to the selected city. The user sees results without any technical error messaging.

### Why this direction

1. **The user is never blocked.** The fallback chain always produces a usable outcome — interpreted services, alias-matched services, or a manual selection screen. No infinite loops. No dead ends.

2. **Operational failures stay internal.** LLM outages, timeouts, rate limits, and misconfigurations are logged and monitored but never surfaced to users as technical errors. The user experience remains simple and non-technical.

3. **Minimizes visible degradation.** Most real-world queries in a Brazilian home services marketplace use common service terms. "Eletricista", "pintor", "encanador", "faxina" — these all have direct alias matches in the catalog. Local matching handles these silently. Users only see the manual fallback for genuinely ambiguous queries.

4. **Preserves the AI-first identity.** The product leads with natural language input. The fallback chain (LLM → local matching → manual selection) progressively reduces intelligence but the user experience remains smooth. Most users will never notice the degradation.

5. **Responsive by design.** Short, configurable LLM timeouts mean the system fails fast rather than making users wait. The fallback mechanisms are local and immediate.

6. **Minimal implementation cost.** Local matching already exists (`CanonicalServices.search()`). The manual selection UI (grouped category list) is standard marketplace UX. No secondary LLM provider needed. No complex error-communication UI needed.

### Not selected

- **Strategy C (multiple LLM providers):** High implementation cost, doesn't solve the fundamental problem (what if ALL providers are down), and adds ongoing maintenance burden. Worth revisiting later as a reliability improvement, but not a substitute for a local fallback.

- **Strategy D (hybrid input):** Overcomplicates the UI for the current product stage. The dual-mode screen design is appropriate for a mature product with power users, but premature for an early marketplace. It also dilutes the AI-first messaging.

- **Strategy E (graceful degradation with user communication):** Conflicts with the product decision that operational failures must not be exposed to users. Mentioning "AI is temporarily unavailable" surfaces infrastructure concerns to users. The chosen approach handles failures silently through the fallback chain.

### Implementation sequence

1. Add short, configurable server-side timeout for LLM calls (application-level configuration, not hardcoded)
2. Wire `CanonicalServices.search()` as the server-side fallback in both `LlmProfessionalInputInterpreter.fallbackResponse()` and `LlmSearchQueryInterpreter.fallbackResult()`
3. Add `llmUnavailable` flag to onboarding and search response DTOs so the client can distinguish LLM failure from normal clarification
4. Update client onboarding flow: when `llmUnavailable` is true, bypass clarification and route to either matched-services confirmation or manual selection
5. Build the manual service selection UI (grouped category list, multi-select for onboarding, single-select for search)
6. Update client search flow: when `llmUnavailable` is true and no local matches, show the category browser
7. Remove neighborhoods from interpretation, ranking, data model, and UI
8. Ensure city selection in onboarding uses a structured picker from supported cities, with the main-screen city pre-selected and support for multiple cities
9. Add server-side logging and monitoring for all LLM failure events (outage, timeout, invalid response, rate limit, misconfiguration)

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
