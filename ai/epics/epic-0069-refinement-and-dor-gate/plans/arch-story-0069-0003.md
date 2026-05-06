# Architecture Plan — story-0069-0003

**Story:** Skill `/x-epic-refine` (multi-persona strategic epic refinement dispatcher)
**Predecessor pattern:** story-0069-0002 (`/x-story-refine`) — same 4-phase mechanic; differs only in personas, strategic angles, and target markdown sections.
**Capability:** `governance.refinement-gate`

## Architecture

Content-layer skill (SKILL.md only). No Java code. The skill is a **dispatcher**, not a linear inquiry. It applies Rule 13 Pattern 2 (SUBAGENT-GENERAL) for parallel persona dispatch (Phases A, C, D) and Rule 13 Pattern 1 (INLINE-SKILL) for the dual-write of `refinementVerdict` via `x-internal-status-update`. Visibility: public (`x-{subject}-{action}`).

## Phase Flow

```
Phase A — strategic analysis (parallel):
  5 fixed sibling Agents in ONE assistant message:
    PO · Tech Lead · Architect · Security · QA
  +0–1 conditional sibling Agent (SRE/DevOps when capability infra.observability.* OR infra.deploy.*)
  Each Agent: model=sonnet, prompt scoped to its strategic angles (§5.2 of story).
  Output: per-persona JSON gap-report { contributions, blockers, questions, dimension-keys-owned }.

Phase B — single consolidated question batch:
  Dedup textual + group by strategic category
    (Problema · Hipótese/OKRs · Alternativas · Segurança · Qualidade · Operações).
  Non-interactive: skip with WARN; verdict.questions persisted unanswered.
  Interactive: AskUserQuestion (single block, NO loop Q→A→Q→A — D5/D-R14).
  Skip Phase B entirely if Phase A produced zero questions.

Phase C — parallel rewrite with answers:
  Re-dispatch the same 5-6 personas as siblings in ONE assistant message.
  Each receives: original prompt + operator answers + its own Phase A draft.
  Output: per-persona proposedSections (mapped to epic markdown sections).

Phase D — Architect consolidation (opus):
  ONE Agent, subagent_type=general-purpose, model=opus (Rule 23 Deep Planner tier — D3/D-R13).
  Inputs: 5-6 proposedSections from Phase C + epic markdown current state.
  Outputs:
    (a) Merged sections in epic markdown (§1.3 Escopo, §1.4 Fora do escopo, §6 Decisões Arquiteturais).
    (b) Single canonical "## Refinement Verdict" block.
    (c) verdict JSON envelope { status, scope:"epic", dimensions:{po,techLead,architect,security,qa,sre?}, blockers, checkedAt }.
  Dual-write (atomic):
    Edit tool       → epic markdown (sections + Refinement Verdict block).
    INLINE-SKILL    → Skill(skill: "x-internal-status-update", model: "haiku", args: "...") writes refinementVerdict to execution-state.json.
```

## Persona-to-Strategic-Angle Mapping

| Persona            | Status      | Strategic angles owned                                           | NO-GO (silent — D6/D-R15)                                                            |
| :----------------- | :---------- | :--------------------------------------------------------------- | :----------------------------------------------------------------------------------- |
| Product Owner      | always      | problem · persona-broad · hypothesis · okrs                      | hypothesis without measurable KPI; OKR without baseline+target+horizon                |
| Tech Lead          | always      | feasibility · epic-dependencies                                  | circular epic deps; infeasibility without resolvable deps                            |
| Architect          | always      | strategic-alternatives · architectural-impact · out-of-scope     | < 2 alternatives; out-of-scope empty or < 3 items (preserves D4 as silent NO-GO)     |
| Security Engineer  | always      | security-posture · compliance-triggers                           | sensitive domain (PCI/LGPD/HIPAA) without compliance; missing threat-modeling scope  |
| QA Engineer        | always      | quality-strategy · smoke-scope                                   | no smoke strategy; no "how do we know it shipped" criterion                          |
| SRE/DevOps         | conditional | operational-impact · rollback-strategy                           | epic touches prod without rollback declared                                          |

> Use the term **angles** (or **strategic areas**) — never **dimensions** — to keep the KP `knowledge/refinement/dimensions.md` (story-0001 deliverable) syntactically distinct from this skill's persona-output keys.

## Data Flow

```
epic-XXXX.md  ──▶ Phase A persona prompts (read-only)
                     │
                     ▼
              5–6 gap-reports (JSON) ──▶ Phase B dedup+group ──▶ operator answers (or skip)
                                                                      │
                                                                      ▼
                                                          5–6 proposedSections (JSON)
                                                                      │
                                                                      ▼
                                            Phase D Architect consolidator (opus)
                                                                      │
                                            ┌─────────────────────────┴──────────────────────────┐
                                            ▼                                                    ▼
                                   Edit tool: epic markdown                  Skill: x-internal-status-update
                                   (§1.3 / §1.4 / §6 + Verdict block)        (refinementVerdict in execution-state.json)
```

## Dependency Direction

```
x-epic-refine SKILL.md
    │
    ├──▶ agents/core/product-owner.md          (read-only, Phase A & C)
    ├──▶ agents/core/tech-lead.md              (read-only, Phase A & C)
    ├──▶ agents/core/architect.md              (read-only, Phase A & C; consolidator in Phase D)
    ├──▶ agents/core/security-engineer.md      (read-only, Phase A & C)
    ├──▶ agents/core/qa-engineer.md            (read-only, Phase A & C)
    ├──▶ agents/core/sre-engineer.md           (read-only, conditional Phase A & C)
    ├──▶ knowledge/refinement/dimensions.md    (read-only, KP from story-0001)
    └──▶ x-internal-status-update              (Pattern-1 INLINE-SKILL — Phase D dual-write)
```

No outbound dependencies on other planning skills. `x-epic-decompose` and `x-epic-implement` consume the verdict from `execution-state.json` (Camada 0 PreToolUse hook `enforce-refinement-gate.sh` — Rule 29).

## Markdown Merge Targets (epic-XXXX.md)

Architect (Phase D) MUST merge per-persona contributions into the canonical sections of the epic markdown. The skill body documents this explicit mapping so Architect prompt is deterministic:

| Source contribution                                            | Target section in epic-XXXX.md                  |
| :------------------------------------------------------------- | :---------------------------------------------- |
| PO problem/hypothesis/OKRs                                     | §1.1 Problema · §1.2 Hipótese · §3 OKRs/Métricas|
| Architect strategic-alternatives                               | §6 Decisões Arquiteturais (alternatives table)  |
| Architect out-of-scope (≥3 items, NO-GO if absent)             | §1.4 Fora do escopo                             |
| Architect architectural-impact                                 | §6 Decisões Arquiteturais (impact)              |
| Security compliance-triggers                                   | §1.5 Compliance & Conformidade                  |
| Tech Lead epic-dependencies                                    | §7 Dependências entre Épicos                    |
| QA quality-strategy / smoke-scope                              | §4 Definition of Done (test strategy)           |
| SRE/DevOps operational-impact / rollback                       | §6 Decisões Arquiteturais (operational ADR)     |
| Final consolidated **Refinement Verdict** block                | new top-level `## Refinement Verdict` (idempotent — replace if exists) |

## Telemetry Markers (Rule 13 §Telemetry + §Subagent)

- `phase.start` / `phase.end` pairs around Phase A, B, C, D (4 pairs). Identifiers: `Phase-A-Analyse`, `Phase-B-Batch`, `Phase-C-Refine`, `Phase-D-Consolidate`.
- `subagent.start` / `subagent.end` per persona-agent in Phase A (5–6 pairs) and Phase C (5–6 pairs).
- `subagent.start` / `subagent.end` for the single Architect consolidator in Phase D (1 pair).
- All emitted via `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh` per Rule 13 contract — no hand-rolled JSON.

## Key Deviations from story-0069-0002

1. **Personas fixed = 5 (story=5–7).** Epic refinement has a smaller deterministic persona set; conditional set is exactly 0–1 (SRE/DevOps).
2. **Strategic angles are non-overlapping per persona.** No dimension is shared between two personas — eliminates the dedup edge-case that story-0002 must handle.
3. **Markdown merge spans more sections.** Story version mostly rewrites a single Refinement Verdict block; epic version touches §1.1, §1.2, §1.3, §1.4, §1.5, §3, §4, §6, §7 in addition to the Verdict block.
4. **Architect carries D4 as silent NO-GO.** Heuristic "out-of-scope ≥ 3 items" was a top-level rule in the original linear design; here it is the Architect's own NO-GO and never surfaces as a question to the operator.
5. **Verdict scope discriminator.** State envelope MUST set `refinementVerdict.scope = "epic"` (story version uses `"story"`). Hook `enforce-refinement-gate.sh` reads this discriminator to route the gate decision per Rule 29.
6. **No dimension-keyed dedup of operator questions** — Phase B groups by strategic category (6 buckets) instead of dimension key (7 keys in story version). Reduces operator cognitive load: one bucket per category, even if two personas asked questions in the same bucket.

## Mini-ADR Stubs (consumed by `x-adr-generate`)

- **ADR (D1):** Multi-persona strategic dispatcher over linear 7-dimension inquiry. Cross-references the story-0002 ADR.
- **ADR (D3):** Sonnet for dispatcher + Phase A/C personas; Opus for Phase D Architect consolidator (Rule 23 Deep Planner tier).
- **ADR (D4-as-NO-GO):** "Out-of-scope ≥ 3 items" preserved as Architect silent NO-GO (D6/D-R15).

## Open Questions for Implementation Plan (deferred to plan-story-0069-0003.md)

- Exact JSON envelope shape returned by each persona-agent (must match shape consumed by Phase D Architect prompt — define a shared schema fragment in the KP or inline).
- Whether SRE/DevOps capability detection reads the YAML directly or delegates to a capability resolver helper.
- Whether the `## Refinement Verdict` block carries the per-persona `contributedSection` paths inline (debug aid) or only in the JSON state envelope.
