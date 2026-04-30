# Test Plan — story-0069-0003

**Story:** Skill `/x-epic-refine` (multi-persona strategic dispatcher)
**Epic:** EPIC-0069
**Scope:** STANDARD

## Test Strategy

Content-layer skill (SKILL.md only). No Java production code. Tests validate:
1. SKILL.md structure and phase markers (TelemetryMarkerLint)
2. Rule 13 compliance (audit-tool-call-grammar.sh)
3. Capability frontmatter v3.0 (audit-frontmatter-schema.sh)
4. Rule 22 visibility markers (audit-skill-visibility.sh)
5. Rule 28 tool-call grammar markers (audit-tool-call-grammar.sh --mode=static)
6. Structural parity with x-story-refine (same 4-phase mechanics, different persona set and dimensions)

## Test Cases

### TC-001: SKILL.md frontmatter completeness
- Verify `name: x-epic-refine` present
- Verify `model: sonnet` present (dispatcher tier — Rule 23 §Orchestrator)
- Verify `requires-capabilities: [governance.refinement-gate]` present
- Verify `visibility: public` and `user-invocable: true` (not internal)
- Verify `allowed-tools` includes: Read, Write, Edit, Skill, Agent, TaskCreate, TaskUpdate, Bash

### TC-002: Telemetry markers (TelemetryMarkerLint)
- Phase A: `phase.start` + `phase.end` pair present
- Phase B: `phase.start` + `phase.end` pair present (or `<!-- phase-no-gate -->` if B is interactive)
- Phase C: `phase.start` + `phase.end` pair present
- Phase D: `phase.start` + `phase.end` pair present
- Phase A: `subagent.start` + `subagent.end` per persona-agent (≥5 pairs: PO, Tech Lead, Architect, Security, QA)
- Phase C: `subagent.start` + `subagent.end` per persona-agent (same count as Phase A)
- Phase D: exactly 1 `subagent.start` + `subagent.end` pair (Architect consolidator)
- Phase B: 0 subagent markers (interactive batch — no sub-agents)
- No DANGLING_END, UNCLOSED_START, DUPLICATE_START, DUPLICATE_END violations

### TC-003: Rule 13 — SUBAGENT-GENERAL pattern (Phase A)
- Each persona Agent() invocation uses `subagent_type: "general-purpose"`
- All Phase A Agent() calls carry `model: "sonnet"` (analyst tier — Rule 23)
- Phase A emits ≥5 sibling Agent() calls in ONE assistant message (PO, Tech Lead, Architect, Security, QA)
- Subject pattern per TaskCreate: `epic-XXXX › Refinement › <PersonaName>` (e.g., `epic-0099 › Refinement › Product Owner`)

### TC-004: Rule 13 — SUBAGENT-GENERAL pattern (Phase C)
- Parallel Agent() calls re-dispatch with operator answers distributed to each persona
- All Phase C Agent() calls carry `model: "sonnet"`
- Same parallelism constraint as Phase A (N siblings in ONE assistant message)
- No per-persona sequential loop in Phase C

### TC-005: Phase D — Architect consolidation (model=opus)
- Single Agent() with `model: "opus"` (Deep Planner tier — Rule 23 §Deep Planner)
- Agent prompt targets merge of persona contributions into epic markdown sections (§1.3 Scope, §1.4 Out-of-scope, §6 Architectural Decisions)
- Produces `## Refinement Verdict` block in the epic markdown
- Dual-write: Edit tool updates epic markdown AND `x-internal-status-update` INLINE-SKILL updates state

### TC-006: x-internal-status-update INLINE-SKILL delegation (Rule 13 Pattern 1)
- Uses `Skill(skill: "x-internal-status-update", model: "haiku", args: "...")` form (Pattern 1)
- Args include `--field refinementVerdict` and `--value {...}`
- Verdict payload includes: `scope: "epic"`, `status`, `checkedAt`, `dimensions` keyed by persona (po, techLead, architect, security, qa), `blockers`
- Field `verdictHash` computed and included in the payload

### TC-007: Persona-to-dimension mapping contract
- **PO** dimensions-owned: `[problem, persona-broad, hypothesis, okrs]`
- **Tech Lead** dimensions-owned: `[feasibility, epic-dependencies]`
- **Architect** dimensions-owned: `[strategic-alternatives, architectural-impact, out-of-scope]`
- **Security** dimensions-owned: `[security-posture, compliance-triggers]`
- **QA** dimensions-owned: `[quality-strategy, smoke-scope]`
- **SRE/DevOps** dimensions-owned (conditional): `[operational-impact, rollback-strategy]`
- Each persona's Agent prompt lists only its owned dimensions — no overlap

### TC-008: NO-GO silent rule (D6 / D-R15)
- If a persona returns blockers in Phase A, those items do NOT appear as questions in Phase B
- NO-GO personas contribute directly to `blockers[]` in the verdict (not surfaced to operator as questions)
- Phase D Architect receives all NO-GO blockers and includes them in `refinementVerdict.blockers`
- When any blocker exists, `refinementVerdict.status = "rejected"`

### TC-009: Phase B — single batch constraint (D-R14)
- Exactly ONE batch of questions emitted to operator (no per-persona Q→A loop)
- Questions deduplicated across personas before presentation
- Questions grouped by strategic category: Problema, Hipótese/OKRs, Alternativas, Segurança, Qualidade, Operações
- Phase B is skipped (zero questions rendered) when all personas return no clarifying questions

### TC-010: Capability frontmatter v3.0
- `requires-capabilities: [governance.refinement-gate]` declared in frontmatter (Rule 28 Invariant 1)
- No v2-style frontmatter (missing `requires-capabilities` field)
- `audit-frontmatter-schema.sh` exits 0 on SKILL.md

### TC-011: Phase gate invocations (Rule 25)
- Phase A: PRE gate (`x-internal-phase-gate --mode pre`) before dispatch + POST gate after collection
- Phase B: `<!-- phase-no-gate: interactive batch, no artifact produced -->` marker on the Phase B header
- Phase C: PRE gate before re-dispatch + POST gate after refined contributions collected
- Phase D: PRE gate before Architect agent + FINAL gate (`--mode final`) after dual-write completes

### TC-012: Structural parity with x-story-refine (TC-012)
- Same 4-phase mechanical structure: A (parallel analysis), B (single batch), C (parallel refinement), D (Architect consolidation)
- Difference: 5–6 strategic personas vs 5–6 story personas (distinct dimension sets)
- Both skills delegate Phase D to model=opus Architect agent (Rule 23 §Deep Planner)
- Both skills use `Skill(skill: "x-internal-status-update", model: "haiku", ...)` for dual-write
- Source-of-truth path: `java/src/main/resources/targets/claude/skills/plan/x-epic-refine/SKILL.md`
- Generated copy: `.claude/skills/x-epic-refine/SKILL.md` — identical to source

## Acceptance Criteria

- All TC-001 to TC-012 pass
- `audit-frontmatter-schema.sh` exits 0 on SKILL.md
- `audit-tool-call-grammar.sh --mode=static` exits 0 (all Skill/Agent calls carry grammar markers)
- `audit-skill-visibility.sh` exits 0 (public visibility, no `x-internal` marker)
- TelemetryMarkerLint reports 0 violations (4 phase pairs + N subagent pairs Phase A/C + 1 Phase D)
- Rule 28: every `Skill(...)` and `Agent(subagent_type: "general-purpose", ...)` in the SKILL.md carries `[required]`, `[optional]`, or `[conditional: ...]` grammar marker
