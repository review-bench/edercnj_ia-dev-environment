---
name: spec-driven-kp
description: "Knowledge pack for spec-driven planning concepts: Planning Depth Tier, Gray-Area Discovery, Knowledge Verification Chain, Requirement Traceability, and the Deferred-Ideas ledger. Subordinate to Rule 27 (zero-bypass)."
user-invocable: false
requires-capabilities: []
---

> 🔒 **INTERNAL KNOWLEDGE PACK** — Referenced by ideation, refinement, story-authoring,
> and planning skills via `@spec-driven-kp`. Not user-invocable.

> **Attribution.** The five concepts in this KP are adapted from the open-source
> **`tlc-spec-driven`** agent skill by **Felipe Rodrigues** (Tech Leads Club —
> `tech-leads-club/agent-skills`), licensed **CC-BY-4.0**. They were *adapted*, not
> copied: the adaptive auto-sizing and Quick Mode of the original are intentionally
> **rejected** here because they conflict with Rule 27. See
> [`ADR-0050-spec-driven-adoption.md`](../../../../../docs/adr/ADR-0050-spec-driven-adoption.md).

# Knowledge Pack: Spec-Driven Planning

## Purpose

Single source of truth for five spec-driven concepts that strengthen our planning,
ideation, refinement, and story-authoring stages **without weakening the zero-bypass
lifecycle**. Skills reference this KP via `@spec-driven-kp` instead of duplicating the
contract inline.

> **Prime invariant (read first).** Nothing in this KP relaxes
> [`knowledge/lifecycle/zero-bypass.md`](../../knowledge/lifecycle/zero-bypass.md)
> (Rule 27) or [`knowledge/lifecycle/refinement-gate.md`](../../knowledge/lifecycle/refinement-gate.md)
> (Rule 29). Every mandatory artifact, surface, and evidence path still exists for
> **every** work item regardless of tier. What scales is **content depth**, never
> artifact existence. The `tlc-spec-driven` "Quick Mode" / phase auto-skip is **not
> adopted**.

---

## 1. Planning Depth Tier

A mandatory classification assigned at the start of planning (epic and story scope).
It tunes how *deep* each RA9 / value-driven section must go — it never removes a
section, artifact, gate, or telemetry marker.

### 1.1 Tiers

| Tier | Name | When it applies |
| :--- | :--- | :--- |
| **TIER-1** | Trivial | ≤ 3 files touched; no new domain concept; no external integration; no security/data-sensitive surface; reversible. |
| **TIER-2** | Standard | Clear single feature; bounded blast radius; ≤ ~10 tasks; familiar domain. **Default when unsure.** |
| **TIER-3** | Complex | Ambiguous or new domain; multi-component; new external integration; security/compliance/data-migration surface; hard-to-reverse. |

**Classification rule:** pick the **highest** tier whose trigger matches (any single
TIER-3 trigger ⇒ TIER-3). When two tiers are defensible, choose the **higher** one.
Record the chosen tier and the trigger that drove it in the artifact header field
`**Planning Depth Tier:**` and in `execution-state.json` (`planningDepthTier`).

### 1.2 Depth matrix (artifact ALWAYS present; only depth scales)

| RA9 / value section | TIER-1 minimum | TIER-2 | TIER-3 full |
| :--- | :--- | :--- | :--- |
| Contexto/Visão | 1–2 sentences | 1–2 paragraphs | Full context + value-delivery analysis |
| Packages (Hexagonal) | Files + layer | Per-layer classes | Full catalog + dependency-direction proof |
| Contratos & Endpoints | Single signature | Full request/response | + error catalogue + event schema |
| Materialização SOLID | 1 constraint line | RULE-NNN refs | Cross-cutting rule table + trade-offs |
| Quality Gates | DoD checklist | + local DoR + Gherkin (4 cat.) | + SLA/security Gherkin + risk-weighted ACs |
| Segurança | Code-level checklist | OWASP applicable | Threat-model reference + control matrix |
| Observabilidade | Log line(s) | Logging + tracing fields | SLO/SLI + alert thresholds |
| Decision Rationale | ≥1 micro-template | ≥1 micro-template | ≥1 per non-obvious decision + Gray-Area |
| Dependências & Footprint | File list | + DAG | + parallelism + rollback footprint |

> **Auditors note.** `RA9_SECTIONS_MISSING`, `RA9_RATIONALE_EMPTY`,
> `RA9_PACKAGES_MISSING`, and `EIE_EVIDENCE_MISSING` apply identically at every tier.
> TIER-1 still produces Epic, Story, plans, reports, telemetry, and PR evidence.

---

## 2. Gray-Area Discovery

Detects **user-facing ambiguity** in a spec *before* design starts, so it is resolved
once instead of mid-implementation.

### 2.1 Ambiguity classes (scan every spec/story)

| Class | Examples |
| :--- | :--- |
| Layout / structure | ordering, grouping, what is shown vs hidden by default |
| Interaction | confirm vs auto-apply, sync vs async, retry vs fail-fast |
| Error handling | message tone, partial-failure behaviour, user-recoverable vs fatal |
| Tone / voice | wording of user-visible text, locale, formality |
| Data shape | optional vs required field, default value, null semantics |
| Integration contract | which side owns the schema, versioning, idempotency key |

### 2.2 Protocol

1. During ideation/refinement, scan the input against §2.1.
2. If ≥1 genuine ambiguity exists, raise **one batched** `AskUserQuestion`
   (Rule 20 / ADR-0010 interactive-gate convention — never one question per ambiguity).
3. Record each resolved decision in the story's `### 8.1 Gray Area Decisions`
   sub-section (under §8 Decision Rationale) using the 4-line micro-template.
4. If `--non-interactive`, do **not** invent answers: mark each ambiguity
   `UNRESOLVED — assumption: <stated assumption>` and surface it as a refinement
   blocker candidate (see §6).

> Gray-Area Discovery feeds the **existing** `ac` and `value` refinement heuristics
> (see [`knowledge/refinement/dimensions.md`](../../knowledge/refinement/dimensions.md)).
> It does **not** add a new key to the `refinementVerdict.dimensions` JSON — the
> verdict schema and `verdictHash` are unchanged (Rule 29 contract preserved).

---

## 3. Knowledge Verification Chain

An ordered anti-fabrication gate. Before any planning/architecture artifact asserts a
technical fact (API shape, library behaviour, config key, schema), the fact MUST be
sourced by walking this chain **in order** and stopping at the first authoritative hit:

1. **Codebase** — existing source, tests, fixtures in this repo.
2. **Project docs** — `knowledge/**`, `docs/**`, ADRs, this KP set.
3. **MCP / Context7** — library/API lookup tools when available.
4. **Web** — official documentation only.
5. **Flag uncertainty** — if 1–4 do not confirm it, write
   `UNVERIFIED — <claim> (source: none)` and treat it as a risk. **Never fabricate.**

### 3.1 Provenance block (required in plan/arch/task-plan artifacts)

```markdown
## Knowledge Verification Provenance
| Claim | Chain step | Source ref | Status |
| :--- | :--- | :--- | :--- |
| <technical assertion> | codebase\|docs\|mcp\|web\|none | <path/url> | verified\|UNVERIFIED |
```

A plan that asserts technical facts without this block (TIER-2/TIER-3) is incomplete.
TIER-1 may collapse it to a single `verified against: <ref>` line.

---

## 4. Requirement Traceability

Gives every requirement a stable ID that flows end-to-end, so spec→test→commit is
auditable.

### 4.1 Convention

- ID format: `REQ-<DOMAIN>-<NN>` (e.g., `REQ-AUTH-01`), `<DOMAIN>` uppercase, `<NN>`
  zero-padded, unique within the epic.
- Declared in the Story header / Epic story index; referenced from:
  - §4/§5 AC Gherkin scenarios — annotate `# REQ-AUTH-01` on the `Cenario:` line.
  - Test Plan acceptance tests (AT) and unit tests (UT) — `Requirement` column.
  - Implementation commit — trailer `Requirement: REQ-AUTH-01` (in addition to the
    existing `Task:` trailer; does not replace Conventional Commits).

### 4.2 Traceability table (Story §5 appendix or plan)

```markdown
| REQ-ID | Acceptance Criterion | AT / UT | Task | Status |
| :--- | :--- | :--- | :--- | :--- |
| REQ-AUTH-01 | Cenario: Happy — login válido | AT-03 / UT-07 | TASK-...-002 | planned |
```

This is additive: `RULE-NNN` (business rules) and `TASK-...` IDs keep their meaning.

---

## 5. Deferred-Ideas / Scope-Creep Ledger

A lightweight place for ideas that surface mid-work but are out of scope, so they are
captured without scope-creeping the current PR.

- Primary store: `execution-state.json` → `deferredIdeas: [{ id, note, raisedAt,
  raisedBy, suggestedTarget }]` (atomic write via `x-internal-update-status`).
- Story-local fallback when no execution-state exists: `### 8.2 Deferred Ideas`
  bullet list under §8.
- A deferred idea is **never** silently implemented. Promotion path: surface it in the
  epic retro / `x-ideate-feature` for a future epic.

---

## Integration Points

| Stage | Skill(s) | Consumes |
| :--- | :--- | :--- |
| Ideation/Concept | `x-ideate-feature`, `x-create-feature` | §1 tier, §2 gray-area, §3 chain |
| Refinement | `x-refine-story`, `x-refine-epic` | §2 gray-area → existing `ac`/`value` heuristics |
| Story authoring | `x-internal-create-story`, `x-internal-create-epic`, `@planning-standards-kp` | §1 depth matrix, §4 REQ-ID |
| Technical planning | `x-plan-story`, `x-plan-task`, `x-plan-architecture` | §1 depth, §3 provenance block, §4 traceability |

## Relationship to the Zero-Bypass Lifecycle

This KP is **subordinate** to Rule 27 and Rule 29. In any conflict, zero-bypass and the
refinement gate win. Tier classification, gray-area capture, the verification chain,
traceability IDs, and the deferred-ideas ledger are **additive disciplines layered on
top of** the mandatory lifecycle — not an escape hatch from it. There is no tier, flag,
or ledger entry that removes a required artifact, surface, gate, or telemetry marker.
