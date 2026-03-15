# Service Catalog Architecture & Evolution

## Status

**Proposed** — Design document for future implementation after LLM resilience work is complete.

## Problem Statement

The QuemFaz platform has a hardcoded catalog of 23 services across 8 categories, defined in code (`CanonicalServices.kt` in the `shared` module). This creates two critical limitations:

1. **Catalog expansion requires code changes.** Adding or modifying services requires changing code, rebuilding the application, and redeploying the platform. This slows marketplace evolution.

2. **The platform cannot detect missing services.** When users describe services that don't exist in the catalog (e.g., "instalo câmeras de segurança", "faço dedetização"), those descriptions are silently lost. The product team has no visibility into unmet demand.

## Product Principles

The design is guided by the following product constraints:

- **Precise taxonomy.** The catalog must remain explicit and well-defined. No generic "other" bucket — the `other-general` catch-all service is removed.
- **No silent loss.** The system must preserve visibility into services that are missing from the catalog.
- **No user blocking.** Professionals should not be blocked from completing onboarding when they describe services not yet in the catalog.
- **Controlled evolution.** The catalog should evolve intentionally through governed processes, not through uncontrolled automatic growth.
- **MVP-appropriate complexity.** The platform is early-stage. Solutions must prioritize simplicity and low operational overhead.

## Chosen Approach: Hybrid Signal Capture + Provisional Services

Three approaches were evaluated:

1. **Signal-only capture** — log unmatched descriptions, catalog stays fixed. Simple, but professionals with unlisted services get a degraded experience.
2. **Full provisional services** — automatically create catalog entries for every unmatched service, inject into LLM prompts. Best UX, but high risk of taxonomy noise and prompt bloat.
3. **Hybrid** — capture signals always; create provisional services when enabled, but exclude them from LLM prompts. Balances UX, taxonomy quality, and marketplace intelligence.

**Decision: Approach 3 (Hybrid).** It preserves onboarding continuity, maintains LLM interpretation quality by keeping prompts clean, and accumulates marketplace intelligence regardless of provisioning state.

---

## Section 1 — Database-Backed Catalog

### Current State

- 23 services hardcoded in `CanonicalServices.kt` (shared module), loaded as an in-memory object on both server and client. One of these is the `other-general` catch-all, which this design removes (leaving 22 curated services).
- 8 categories defined as a Kotlin enum (`ServiceCategory`) in `ServiceModels.kt`. One of these is `OTHER`, which exists only to house `other-general`.
- No database representation of either.

### Proposed State

#### `service_categories` table

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | e.g., `"CLEANING"` |
| `display_name` | TEXT NOT NULL | e.g., `"Limpeza"` |
| `sort_order` | INT NOT NULL | Display ordering |
| `created_at` | TIMESTAMPTZ | |

Categories are **database entities** — the table is the source of truth. The `ServiceCategory` enum in `shared/` is retired. Initial migration seeds 7 categories (the current 8 minus `OTHER`, which is removed along with `other-general`). For the MVP, new categories are added via direct database insert (admin action or migration). The architecture does not prevent exposing category creation through an admin endpoint later.

#### `canonical_services` table

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | e.g., `"clean-house"` |
| `display_name` | TEXT NOT NULL | e.g., `"Limpeza Residencial"` |
| `description` | TEXT NOT NULL | What the service includes |
| `category_id` | TEXT NOT NULL FK | References `service_categories.id` |
| `aliases` | JSONB NOT NULL | Array of alias strings |
| `status` | TEXT NOT NULL | `active` \| `pending_review` \| `rejected` \| `merged` |
| `created_by` | TEXT NOT NULL | `"migration"` for initial seed, `"system"` for provisional |
| `created_at` | TIMESTAMPTZ | |
| `updated_at` | TIMESTAMPTZ | |
| `merged_into_service_id` | TEXT | If status = `merged`, points to target service |
| `review_status_reason` | TEXT | Free-text note explaining admin decision |
| `reviewed_at` | TIMESTAMPTZ | When the decision was made |
| `reviewed_by` | TEXT | Who made the decision |

#### Status lifecycle

```
pending_review ──→ active      (approved)
pending_review ──→ rejected    (declined, kept for history)
pending_review ──→ merged      (absorbed into existing service)
```

Only `active` services appear in the catalog API endpoint (`GET /services/catalog`). `pending_review` services reach clients exclusively through profile and interpretation response data (see Section 6). Only `active` services are injected into LLM prompts. `rejected` and `merged` records are retained for audit but excluded from all catalog and matching queries.

#### Automatic provisioning scope

The system only creates provisional services within existing categories. It does not create new categories automatically. This is a product constraint, not an architectural limitation — the database design supports category creation if this decision changes later.

#### Migration strategy

- Flyway migration creates both tables, seeds 7 categories and 22 services with `status = active` and `created_by = "migration"`. The `OTHER` category and `other-general` service are not migrated — they are removed entirely.
- `CanonicalServices.kt` in `shared/` is retired as a data source.
- `ServiceCategory` enum in `shared/` is retired.
- `shared/` retains lightweight DTOs (`CanonicalServiceDto`, `ServiceCategoryDto`) for API responses.
- Server becomes the single source of truth via new `CatalogRepository` and `CatalogService`.
- Server caches the catalog in memory with a refresh mechanism.
- Client fetches the catalog via a new endpoint: `GET /services/catalog`.
- Client caches locally with a version/ETag mechanism for freshness.

#### What changes across modules

| Module | Change |
|---|---|
| `shared/` | `CanonicalServices` object and `ServiceCategory` enum removed. Catalog DTOs added. |
| `server/` | New `CatalogRepository`, `CatalogService`. Catalog loaded from DB with in-memory cache. New `/services/catalog` endpoint. LLM prompt injection reads from `CatalogService`. |
| `composeApp/` | All `CanonicalServices` and `ServiceCategory` imports replaced. See Section 6 for detailed migration of each usage. |

---

## Section 2 — Signal Capture & Traceability

### Purpose

Capture every instance where a user describes a service that doesn't map confidently to the existing catalog. This data serves two functions:

1. **Marketplace intelligence** — understand what services are in demand but missing.
2. **Governance support** — provide evidence when reviewing provisional services.

### `unmatched_service_signals` table

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | UUID |
| `raw_description` | TEXT NOT NULL | Original text from user |
| `source` | TEXT NOT NULL | `"onboarding"` \| `"search"` |
| `user_id` | TEXT | References `users.id`, nullable for anonymous search |
| `best_match_service_id` | TEXT | Closest existing service the system could find, if any |
| `best_match_confidence` | TEXT | `"high"` \| `"low"` \| `"none"` |
| `provisional_service_id` | TEXT | If a provisional service was created, links to it |
| `city_name` | TEXT | Where the demand is coming from |
| `safety_classification` | TEXT | `"safe"` \| `"unsafe"` \| `"uncertain"` \| null (fallback) |
| `safety_reason` | TEXT | LLM-provided reason for unsafe/uncertain |
| `created_at` | TIMESTAMPTZ | |

### When signals are captured

A signal is recorded **every time** the interpreter detects an unmatched service, regardless of whether automatic provisioning is enabled. The signal table always accumulates data.

Capture points:

1. **LLM interpretation** — the LLM response includes `unmatchedDescriptions` where the LLM explicitly reports service descriptions it could not map confidently. Each unmatched description generates a signal row.
2. **Local matching fallback** — if the LLM is unavailable and local scoring produces no results above a confidence threshold, the raw query is captured as a signal with `best_match_confidence = "none"` and `safety_classification = null`.

### Traceability model

The relationship between signals and provisional services is **many-to-one**. Multiple signals can point to the same `provisional_service_id`. This happens when different users describe similar services that the system groups under one provisional entry.

When an admin reviews a provisional service, they can query all signals linked to it:

> **Provisional service:** "Instalação de Câmeras de Segurança" (pending_review)
> **Linked signals:** 12 occurrences over 3 weeks
> - 8 from onboarding ("instalo câmeras", "CFTV", "câmeras de segurança", ...)
> - 4 from search ("preciso instalar câmeras", "câmera de monitoramento", ...)
> - Cities: São Paulo (7), Campinas (3), Curitiba (2)

### Signal independence

Even if a provisional service is later rejected or merged, its linked signals remain unchanged. The `provisional_service_id` still points to the (now rejected/merged) record, preserving the full history. If the same type of service is requested again later, new signals accumulate — potentially justifying future reconsideration.

---

## Section 3 — Provisional Service Creation & Governance

### Configuration Flag

A runtime configuration flag controls automatic provisioning:

| Flag | Effect |
|---|---|
| `catalog.auto-provisioning.enabled = true` | Unmatched services trigger provisional service creation |
| `catalog.auto-provisioning.enabled = false` | Unmatched services are captured as signals only |

Stored in a `system_configuration` table:

| Column | Type |
|---|---|
| `key` | TEXT PK |
| `value` | TEXT NOT NULL |
| `updated_at` | TIMESTAMPTZ |

Read at interpretation time. No redeploy needed to toggle. Default is `false` (provisioning disabled) — safe by default.

### Provisional service creation flow

```
User input
  │
  ▼
LLM interpretation
  │
  ├─ Matched confidently → normal flow (existing service IDs returned)
  │
  └─ Unmatched description detected
       │
       ▼
     Safety guardrail check
       │
       ├─ unsafe → signal captured (flagged), user notified, NO provisioning
       │
       └─ safe or uncertain
            │
            ▼
          Signal captured (always)
            │
            ▼
          Auto-provisioning enabled?
            │
            ├─ No → best-effort match returned (or no service)
            │
            └─ Yes
                 │
                 ▼
               Deduplication check: does a pending_review service
               already exist that matches this description?
                 │
                 ├─ Yes → link signal to existing provisional service,
                 │        return that service to the user
                 │
                 └─ No → LLM generates candidate service definition
                         │
                         ▼
                       Create new service with status = pending_review
                       Link signal to new provisional service
                       Return provisional service to the user
```

### Candidate service generation

When a provisional service needs to be created, the system makes a second LLM call (or extends the first) to generate a structured service definition.

**Input:** The raw user description + the existing catalog (for context and naming consistency).

**Output:**
```json
{
  "serviceId": "camera-installation",
  "displayName": "Instalação de Câmeras de Segurança",
  "description": "Instalação e configuração de câmeras de segurança e sistemas de monitoramento (CFTV).",
  "categoryId": "REPAIRS",
  "aliases": ["câmeras de segurança", "CFTV", "monitoramento"]
}
```

**Prompt instructions:**
- Follow naming conventions of the existing catalog (Portuguese, title case, concise)
- Assign to the most appropriate existing category
- Generate a URL-friendly slug as the ID
- Include common aliases in Portuguese
- Do NOT create a service that overlaps significantly with an existing active service

### Deduplication

Before creating a new provisional service, the system checks for existing `pending_review` services:

1. **Exact ID match** — if the LLM generates the same slug as an existing provisional service
2. **Display name similarity** — case-insensitive normalized comparison (exact or near-exact match). This is a best-effort heuristic with implementation discretion on threshold — not a fuzzy search.
3. **LLM-assisted check** — the candidate generation prompt includes current `pending_review` services, instructing the LLM to reuse an existing one if appropriate rather than generating a new definition

All three steps are best-effort. Some duplicates will slip through and be resolved during admin review via the merge action. Perfect deduplication is not a goal — it is acceptable to have a few duplicates as long as the admin has the tools to merge them.

### Admin review workflow (MVP)

Admin review is performed through API endpoints (no UI in MVP).

**`GET /admin/catalog/pending`** — List all `pending_review` services with linked signal counts, sources, and cities.

**`POST /admin/catalog/{serviceId}/approve`** — Status → `active`. Service joins the curated catalog and LLM prompts.

**`POST /admin/catalog/{serviceId}/reject`** — Status → `rejected`. Records `review_status_reason` and `reviewed_by`. Cascades:
- Remove service from all professional profiles that reference it
- Profiles left with no services become incomplete (`missingFields: ["services"]`)
- No automatic notification in the MVP

**`POST /admin/catalog/{serviceId}/merge`** — Status → `merged`, sets `merged_into_service_id`. Cascades:
- All professionals using the provisional service migrated to the target service
- Signal records retain original `provisional_service_id` for history

**Request body (reject/merge):**
```json
{
  "reason": "Duplicate of 'eletricista' — câmeras de segurança fall under electrical services",
  "mergeIntoServiceId": "electrician"
}
```

### Safety classification behavior matrix

| Safety | Signal captured | Provisioned (if flag enabled) | Review priority | User experience |
|---|---|---|---|---|
| `safe` | Yes | Yes | Normal | Service added to profile, onboarding proceeds |
| `unsafe` | Yes (flagged) | No, never | N/A | Neutral rejection message, WhatsApp escalation link |
| `uncertain` | Yes (flagged) | Yes | Priority | Service added to profile, onboarding proceeds |

### Impact on professional profiles

| Action | Effect on profiles |
|---|---|
| **Approved** | No change — service transitions from provisional to permanent |
| **Rejected** | Service removed from all linked profiles. Profiles with no remaining services become incomplete. |
| **Merged** | Service ID replaced with target service ID on all linked profiles. Transparent to the professional. |

### Impact on search

- `active` services: fully searchable, professionals matched normally.
- `pending_review` services: searchable via DB-level service ID matching. NOT injected into LLM prompts — matches happen only through direct service ID overlap.
- `rejected` / `merged`: excluded from all matching.

---

## Section 4 — Safety Guardrails

### Purpose

Prevent the catalog from being polluted with services that are illegal, legally risky, reputation-damaging, or clearly outside the platform's scope. Applies regardless of whether auto-provisioning is enabled.

### Guardrail placement

The safety check sits between LLM interpretation and any catalog action:

```
Unmatched description detected
  │
  ▼
Safety guardrail check
  │
  ├─ safe → proceed to signal capture + provisioning flow
  ├─ unsafe → block provisioning, capture flagged signal, notify user
  └─ uncertain → treat as safe for provisioning, flag for priority review
```

### Implementation

The safety check is an additional field in the LLM interpretation response, not a separate LLM call. The existing interpretation prompt is extended to include safety classification for each unmatched description:

```json
{
  "unmatchedDescriptions": [
    {
      "rawDescription": "instalo câmeras de segurança",
      "safetyClassification": "safe",
      "safetyReason": null
    }
  ]
}
```

**LLM prompt instructions:**
- `unsafe`: illegal activities, legally regulated services the platform cannot verify, or services that would expose the platform to legal or reputational risk
- `uncertain`: ambiguous descriptions that could be interpreted multiple ways
- `safe`: clear, legitimate home/personal services
- Provide a brief `safetyReason` for `unsafe` and `uncertain`

### User experience on block

When a service is classified as `unsafe`:
1. Service is not provisioned and not matched
2. Onboarding continues — the professional can still complete with other described services
3. Neutral, product-focused message displayed:
   > "O serviço '[description]' não é suportado pela plataforma no momento. Se você acredita que isso é um erro, entre em contato conosco pelo WhatsApp [number]."
4. No reference to AI, moderation, or internal mechanisms
5. If the blocked service was the only one described, the professional is prompted to use the category picker

### Local matching fallback

When the LLM is unavailable:
- No safety classification is possible
- No provisional service is created (provisioning requires LLM classification)
- Signal captured with `safety_classification = null`
- Professional proceeds with best-effort local matches or category picker

This is a conservative default — provisional services are only created when the LLM is available to both generate the definition and classify safety.

---

## Section 5 — LLM Interpreter Changes

### Current State

Both interpreters inject the full catalog into the system prompt, instruct mapping to canonical IDs, and silently map unknowns to closest match or `other-general`.

### Proposed Changes

#### Remove `other-general`

The `other-general` service is removed. The LLM prompt no longer instructs mapping to a generic fallback. Instead, the LLM explicitly reports services it cannot map.

#### Extended LLM response structures

These are the structures returned by the LLM itself (defined in `LlmModels.kt` on the server). They are distinct from the domain/DTO types that flow to the client.

**Search — `SearchInterpretation` (server LLM model):**
```json
{
  "serviceIds": ["electrician"],
  "unmatchedDescriptions": [
    {
      "rawDescription": "instalação de câmeras",
      "safetyClassification": "safe",
      "safetyReason": null
    }
  ]
}
```

**Onboarding — `OnboardingInterpretation` (server LLM model):**
```json
{
  "serviceIds": ["electrician", "plumber"],
  "needsClarification": true,
  "clarificationQuestions": ["Você também faz manutenção elétrica?"],
  "unmatchedDescriptions": [
    {
      "rawDescription": "instalo câmeras de segurança",
      "safetyClassification": "safe",
      "safetyReason": null
    },
    {
      "rawDescription": "faço dedetização",
      "safetyClassification": "safe",
      "safetyReason": null
    }
  ]
}
```

The LLM may return both matched service IDs and unmatched descriptions from the same input.

#### Updated system prompt

**Removed:**
- Reference to `other-general`
- "Map to closest match" instruction for unknown services

**Added:**
- Report unmatched services via `unmatchedDescriptions`
- Safety classification instructions
- Only `active` services included in prompt (not `pending_review`)

**Example prompt fragment (onboarding):**
```
You MUST map described services to canonical service IDs from the catalog below.
Use ONLY the service ID values from the catalog. Do not invent new service IDs.

If the description mentions a service that does NOT exist in the catalog:
- DO NOT force-map it to an unrelated service
- Instead, add it to the "unmatchedDescriptions" array with the raw description
- Classify its safety: "safe", "unsafe", or "uncertain"
- For "unsafe" or "uncertain", provide a brief "safetyReason"

A service is "unsafe" if it involves illegal activities, legally regulated services
the platform cannot verify, or anything that would expose the platform to legal
or reputational risk.

Supported services catalog:
[dynamically injected active services only]
```

#### Post-LLM processing flow

```
For each unmatchedDescription:
  │
  ├─ if unsafe
  │    → capture flagged signal
  │    → add rejection message to response
  │    → skip provisioning
  │
  ├─ if safe or uncertain
  │    → capture signal (always)
  │    → if auto-provisioning enabled:
  │         → deduplication check
  │         → create or link provisional service
  │         → add provisional service ID to response
  │    → if auto-provisioning disabled:
  │         → no service added
  │
  └─ (continue with next)

Combine matched service IDs + any provisional service IDs
Return unified response
```

#### Response DTO extensions

- `InterpretedSearchQuery` gains `blockedDescriptions: List<String>`
- `CreateProfessionalProfileDraftResponse` gains `blockedDescriptions: List<String>`

The client uses these to display neutral rejection messages.

#### Local matching fallback changes

`CatalogService.search()` (replacing `CanonicalServices.search()`):
- Searches only `active` services (minus `other-general`)
- If results found above confidence threshold: return them
- If no results: capture signal with `safety_classification = null`, no provisioning
- Professional proceeds with category picker

#### Catalog injection source

Currently: `CanonicalServices.all` (in-memory hardcoded list).
After: `CatalogService.getActiveServices()` (in-memory cache of DB-backed catalog, filtered to `status = active`).

Same format: `- ${service.id}: ${service.displayName} (aliases: ${service.aliases})`. Generated from cached database query instead of hardcoded object.

---

## Section 6 — Client-Side Changes

### Catalog API endpoint

**`GET /services/catalog`**

Returns the full active catalog. Does not include `pending_review`, `rejected`, or `merged` services.

**Response:**
```json
{
  "version": "abc123",
  "categories": [
    {
      "id": "CLEANING",
      "displayName": "Limpeza",
      "sortOrder": 1
    }
  ],
  "services": [
    {
      "id": "clean-house",
      "displayName": "Limpeza Residencial",
      "description": "...",
      "categoryId": "CLEANING",
      "aliases": ["diarista", "faxina"]
    }
  ]
}
```

The `version` field is a content hash. Client sends it via `If-None-Match` header. Server returns `304 Not Modified` if unchanged.

### Client caching

- First launch: fetch full catalog, store locally (in-memory + persistent local storage)
- Subsequent launches: send cached version, receive 304 or updated catalog
- Network unavailable: use cached catalog
- Cache invalidation: catalog changes infrequently, aggressive caching appropriate

### Provisional services in the client

Provisional services appear through **profile data, not the catalog endpoint**. The catalog endpoint stays user-agnostic and cacheable.

`InterpretedServiceDto` gains a `status` field:
```json
{
  "serviceId": "camera-installation",
  "displayName": "Instalação de Câmeras de Segurança",
  "matchLevel": "PRIMARY",
  "status": "pending_review"
}
```

### UI changes

**`ServiceCategoryPicker`:** Currently imports `CanonicalServices` and `ServiceCategory` directly. After: fetches from `GET /services/catalog` via a cached catalog provider. Groups by `categoryId` from the fetched data. No structural change to the component.

**`OnboardingViewModel`:** Currently calls `CanonicalServices.findById()` to resolve service display names. After: resolves display names from the locally cached catalog data.

**`HomeViewModel`:** Currently calls `CanonicalServices.findById()` to resolve service display names for search pre-selection. After: resolves from locally cached catalog data.

**`OnboardingScreens` and `SearchScreens`:** Currently reference `ServiceCategory` enum for category display. After: use category data from the cached catalog response.

**`ServiceChipList`:** When `status == "pending_review"`, renders chip with subtle visual distinction.

**Onboarding draft screen:** Displays neutral rejection messages for `blockedDescriptions`. Provisional services shown alongside matched services in the confirmation step.

**Search results:** If `blockedDescriptions` present, brief message that the service is not available. No other search UI changes.

### Shared module changes

`shared/` no longer contains `CanonicalServices` or `ServiceCategory` as data. It retains:
- `CanonicalServiceId` value class
- `ServiceMatchLevel` enum
- DTOs: `CatalogResponse`, `ServiceCategoryDto`, `CanonicalServiceDto`, `InterpretedServiceDto` (gains `status` field for provisional service visibility), updated request/response DTOs with `blockedDescriptions`. Note: `CanonicalServiceDto` does NOT include a `status` field — the catalog endpoint only returns active services, so status would be redundant there. The `status` field exists only on `InterpretedServiceDto`, where provisional services appear through profile/interpretation responses.

---

## Section 7 — Evolution Phases

### Phase 1 — MVP

**Catalog migration:**
- `service_categories` and `canonical_services` tables via Flyway migration
- 7 categories and 22 services seeded with `status = active` (`OTHER` category and `other-general` service removed)
- `CanonicalServices.kt` and `ServiceCategory` enum retired
- `CatalogRepository`, `CatalogService` on server with in-memory cache
- `GET /services/catalog` endpoint with ETag caching
- Client fetches and caches catalog from API

**Signal capture:**
- `unmatched_service_signals` table
- `system_configuration` table with `catalog.auto-provisioning.enabled` flag
- LLM responses extended with `unmatchedDescriptions` and safety classification
- Signals from both onboarding and search
- Local fallback captures signals with `safety_classification = null`

**Provisional service creation:**
- When enabled and safety = `safe` or `uncertain`: create `pending_review` service, link signal
- Deduplication check before creation
- Provisional services in search matching but excluded from LLM prompts

**Safety guardrails:**
- LLM-based classification in interpretation prompts
- `unsafe` blocked from provisioning, user shown neutral message
- WhatsApp escalation path

**Admin review:**
- `GET /admin/catalog/pending`
- `POST /admin/catalog/{serviceId}/approve`
- `POST /admin/catalog/{serviceId}/reject`
- `POST /admin/catalog/{serviceId}/merge`
- Reject/merge cascade to professional profiles
- No admin UI — endpoints only

**Client:**
- `ServiceCategoryPicker` fetches from API
- `InterpretedServiceDto` gains `status`
- Provisional services with subtle visual distinction
- `blockedDescriptions` shown as rejection messages

**Not in MVP:**
- No admin UI
- No professional notifications on reject/merge
- No automated deduplication suggestions
- No keyword blocklist
- No category creation

### Phase 2 — Governance & Observability

**Admin dashboard:** Web interface for reviewing pending services with linked signals (source, frequency, cities, timeline). Approve/reject/merge with inline reason. Priority queue for `uncertain` services.

**Professional notifications:** Notify on rejection (suggest alternatives) and optionally on approval. Push notification or in-app message.

**Deduplication assistance:** Suggested merges during review. System highlights potential duplicates.

**Observability:** Metrics (signals/week, provisioning rate, approval ratio, top unmatched descriptions). Alerts (spike in `unsafe`, growing pending backlog).

### Phase 3 — Catalog Intelligence

**Keyword blocklist:** Fast pre-filter before LLM for obvious unsafe inputs. Database-maintained, admin-editable.

**Category evolution:** Admin endpoint for new categories. Signal data guides category creation decisions.

**Smarter deduplication:** Automated clustering of signals and provisional services. LLM-assisted merge suggestions. Batch review.

**Internal support system:** Replace WhatsApp with ticketed support. Link tickets to flagged signals.

**Catalog analytics:** Service growth trends, geographic demand patterns, seasonal trends, supply/demand gap analysis.

### Phase summary

| Phase | Focus | Key deliverable |
|---|---|---|
| **1 — MVP** | Foundation | DB-backed catalog, signal capture, provisional services, safety guardrails, admin API |
| **2 — Governance** | Operations | Admin dashboard, notifications, dedup assistance, observability |
| **3 — Intelligence** | Scale | Blocklist, category evolution, automated clustering, analytics |

Each phase is independently valuable. Phase 1 can run indefinitely without Phase 2. Phase 2 can run without Phase 3.
