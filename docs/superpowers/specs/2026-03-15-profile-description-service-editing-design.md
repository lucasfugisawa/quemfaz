# Professional Profile Description & Service Editing Design

## Problem Statement

During professional onboarding, the user enters a free-text (or voice) description of the services they provide. This input currently serves two purposes:

1. **Service inference** — the system maps the text to canonical services from the catalog
2. **Public profile description** — the same text becomes the professional's public-facing description

This creates a design problem. The professional writes with the intent of *describing what they do* for the system to understand — not with the intent of *presenting themselves professionally* to customers. The result is profile descriptions that are often raw, unpunctuated, and not suitable as public-facing text.

Additionally, after onboarding, the professional **cannot add or remove services** — the service list is locked. And the description and services are implicitly coupled through the original input text.

## Current Behavior

### Onboarding Flow

1. Professional enters free-text description (e.g., "faço pintura de casa e comércio")
2. LLM infers canonical services from the input
3. `normalizedDescription` is set to the input text with first letter capitalized
4. Professional reviews inferred services + description on a single screen (`DraftReady`)
5. Professional uploads photo, optionally sets known name, confirms profile
6. Profile is published with the raw input as the public description

### Post-Onboarding Editing

- Professional can edit the description text
- Professional **cannot** add or remove services
- Editing the description does not trigger service re-inference

### Problems

1. **Profile descriptions look unprofessional** — raw input wasn't written as marketing text
2. **Voice input makes it worse** — transcription produces no punctuation, filler words, false starts
3. **Services are locked** — no way to adjust after onboarding
4. **Single review screen overloads decisions** — services and description presented together

## Design Direction

**Approach: Staged Review with Independent Artifacts**

- Onboarding input is used for two purposes but produces two **independent outputs**: inferred services and a lightly edited description
- The review step is split into two focused screens: one for services, one for the description
- After onboarding, services and description are fully decoupled — editing one never affects the other
- Service management post-onboarding uses catalog-based selection (not natural language re-inference)

## Revised Onboarding Flow

```
Idle (free-text or voice input)
  ↓
Loading (LLM processes input)
  ↓
NeedsClarification (optional, can skip — unchanged)
  ↓
ReviewServices ← NEW: focused on inferred services only
  ↓
ReviewDescription ← NEW: focused on the edited description only
  ↓
PhotoRequired (unchanged)
  ↓
KnownName (unchanged)
  ↓
Published
```

### What changes from today

1. **The LLM now returns two outputs** from the same input: inferred service IDs (as today) + a lightly edited description draft (new).

2. **`DraftReady` splits into two screens:**
   - **ReviewServices** — shows the inferred canonical services. Professional can confirm, remove mismatches, or add missing services from the catalog. City selection also happens here (same as today). This screen is about *structured data*.
   - **ReviewDescription** — shows the LLM-edited description in an editable text field. Professional can accept as-is, tweak it, or rewrite entirely. This screen is about *their public-facing text*.

3. **The description shown on ReviewDescription is the LLM-edited version**, not the raw input.

4. **Voice input** follows the same flow. The LLM light-editing step naturally handles messier transcription output.

### What doesn't change

Clarification, photo upload, known name, and final confirmation all stay as they are.

## LLM Light-Editing Behavior

The LLM acts as a **light editor**, not a content generator. It processes the professional's original input to produce a cleaner version suitable as a profile description.

### What the LLM should do

- Fix punctuation and capitalization
- Split run-on sentences for readability
- Slightly reorganize phrasing for clarity
- Light condensation when the user was verbose about something simple
- Light transformation from "describing what I do" tone toward "profile description" tone
- Remove filler words and false starts from voice transcriptions ("tipo", "né", "aí")

### What the LLM must NOT do

- Add information the professional didn't mention
- Invent experience, credentials, or marketing claims
- Introduce services not present in the original input
- Translate the professional's language into canonical service terminology from the catalog
- Remove meaningful information
- Change the meaning of what was said

### Example transformations

| Input (raw) | Output (edited) |
|---|---|
| `faço pintura de casa e comércio tenho experiência de 10 anos` | `Faço pintura de casa e comércio. Tenho experiência de 10 anos.` |
| `tipo eu trabalho com faxina né limpeza de terreno também faço` | `Trabalho com faxina e limpeza de terreno.` |
| `ELETRICISTA RESIDENCIAL E COMERCIAL FAÇO INSTALAÇÃO DE CHUVEIRO TOMADA TUDO RELACIONADO A PARTE ELÉTRICA` | `Eletricista residencial e comercial. Faço instalação de chuveiro, tomada e tudo relacionado à parte elétrica.` |

### Voice input

Voice transcriptions may arrive without punctuation, with filler words, and with false starts. The same editing rules apply. Filler words and false starts can be removed (they're not meaningful information). The professional's actual claims and descriptions must be preserved.

### Implementation note

The description editing can be a second instruction block within the same LLM call that already does service inference, or a separate call. That's an implementation decision.

## Post-Onboarding Service Management

After onboarding, the professional can add or remove services from their profile. This is a **catalog-based selection** experience, fully independent from the profile description.

### Where it lives

The existing `EditProfessionalProfile` screen gains a new **Services section** (currently missing — services are locked after onboarding today).

### Interaction

1. Professional opens profile editing
2. They see their current services listed (with display names from the catalog)
3. Each service has a **remove** action
4. An **"Add service"** action opens a catalog browser
5. The catalog browser shows services grouped by category, with search/filter
6. Professional selects services to add — they appear in their profile service list
7. Save persists the updated service list independently from the description

### Key behaviors

- **Adding** a service: browse catalog, select, added to profile with match level `PRIMARY`
- **Removing** a service: tap remove on an existing service, removed from profile
- **No re-inference:** editing services never triggers LLM interpretation. The catalog is the interface.
- **No coupling to description:** changing services does not affect the profile description, and vice versa.
- **Catalog only shows active services:** pending/deprecated services are not selectable.

### Reuse

The existing `ServiceCategoryPicker` component (used in onboarding as fallback when LLM finds no services) can be reused for the **"Add service"** action in the profile edit screen. Note: `ServiceCategoryPicker` currently supports only *selecting* services. The *remove* action on existing services is a separate UI element (e.g., a remove button on each service chip) that does not require the picker — it operates on the profile's current service list directly.

## Post-Onboarding Description Editing

The professional can edit their public profile description at any time. This is a **free-text editing** experience, fully independent from services.

### Where it lives

The existing `EditProfessionalProfile` screen already has a description field. No structural change — it just becomes more clearly the "marketing text" that the professional owns.

### Key behaviors

- **No LLM re-editing on save.** When the professional edits their description manually, the system saves exactly what they typed. The LLM light-editing was a one-time onboarding convenience, not a persistent filter.
- **No service re-inference.** Editing the description never triggers changes to the service list.
- **No content quality validation.** The description is the professional's own text. Content moderation for abuse/safety is a separate concern outside this design.

### Why no LLM re-editing on manual edits?

The professional is now consciously writing their profile text — they're the author. Silently re-editing what they deliberately typed would undermine ownership. The LLM step during onboarding is justified because the original input wasn't written *as* a profile description. That asymmetry doesn't exist when the professional is editing their profile directly.

## Data Model & API Implications

### Draft response changes

`CreateProfessionalProfileDraftResponse` and `ClarifyDraftRequest` response both currently return `normalizedDescription`. The design adds a new field to both:

- `normalizedDescription` — stays as-is (raw input, capitalized). Becomes **server-internal only** — the client no longer needs it. It may be retained in the response for debugging or logging purposes during development, but the client UI does not display it. The client uses only `editedDescription`.
- `editedDescription` — **new field**. The LLM light-edited version. Shown on ReviewDescription screen.

Both come from the same LLM call.

### Confirm request changes

`ConfirmProfessionalProfileRequest` currently has a field named `normalizedDescription`. This field should be **renamed to `description`** to reflect its new semantics: it now carries the professional-approved description (which starts as the LLM-edited draft but may have been modified by the professional). The old name would be semantically misleading.

`selectedServiceIds` remains a separate list confirmed on the ReviewServices screen. These two are decoupled — they come from different review steps.

### Profile update endpoint

`PUT /professional-profile/me` reuses `ConfirmProfessionalProfileRequest`. This still works — the client sends the full payload (description + services + city + phone) on every save, even if only one field changed. Description and services are independent at the **UX level** (separate sections in the edit screen), but they share a single API call. This is acceptable — no need for separate endpoints.

The change is that services are no longer always echoed back unchanged — the client can now send a modified service list.

### No new endpoints needed

The existing draft, confirm, and update endpoints cover all flows. Changes are:

- Draft and clarification responses gain `editedDescription` field
- `ConfirmProfessionalProfileRequest.normalizedDescription` renamed to `description`
- LLM prompt gains description-editing instructions
- Client UI splits the review into two screens
- Client edit screen gains service management (using existing catalog endpoint)

## Summary of Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Onboarding friction | Minimal — no new input steps, one extra review step | Publish fast, improve later. Staged review adds a screen but no new user input. |
| Review experience | Staged — services then description | Focused decisions, less cognitive load |
| LLM role in description | Light editor only | Preserve professional's authentic voice |
| Description ↔ services relationship | Fully independent | Avoids unintended side effects from coupling |
| Post-onboarding service editing | Catalog-based selection | Simple, predictable, no inference ambiguity |
| Post-onboarding description editing | Free text, no LLM re-editing | Professional is the author |
| Voice input | Same flow as text | LLM editing naturally handles messier transcription |
| Progressive profile enrichment | Out of scope | Separate initiative |
