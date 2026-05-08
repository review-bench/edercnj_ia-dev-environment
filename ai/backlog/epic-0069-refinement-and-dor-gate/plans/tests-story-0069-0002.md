# Test Plan — story-0069-0002

**Story:** Skill `/x-story-refine` (multi-persona dispatcher)
**Epic:** EPIC-0069
**Scope:** STANDARD

## Test Strategy

Content-layer skill (SKILL.md only). No Java production code. Tests validate:
1. SKILL.md structure and phase markers (TelemetryMarkerLint)
2. Rule 13 compliance (audit-tool-call-grammar.sh)
3. Capability frontmatter v3.0 (audit-frontmatter-schema.sh)
4. Rule 22 visibility markers (audit-skill-visibility.sh)
5. x-story-refine invocability — skill resolves when called by x-story-implement

## Test Cases

### TC-001: SKILL.md frontmatter completeness
- Verify `name: x-story-refine` present
- Verify `model: sonnet` present
- Verify `requires-capabilities: [governance.refinement-gate]` present
- Verify `visibility: public` (not internal)
- Verify `allowed-tools` includes: Read, Write, Edit, Skill, Agent, TaskCreate, TaskUpdate, Bash

### TC-002: Telemetry markers (TelemetryMarkerLint)
- Phase A: `phase.start` + `phase.end` pair present
- Phase B: `phase.start` + `phase.end` pair present
- Phase C: `phase.start` + `phase.end` pair present
- Phase D: `phase.start` + `phase.end` pair present
- No DANGLING_END, UNCLOSED_START, DUPLICATE_START, DUPLICATE_END violations

### TC-003: Rule 13 — SUBAGENT-GENERAL pattern (Phase A)
- Each persona Agent() invocation uses `subagent_type: "general-purpose"`
- All Agent() calls have `model: "sonnet"`
- Phase A emits ≥5 sibling Agent() calls in ONE assistant message (PO, Tech Lead, Architect, Security, QA)

### TC-004: Rule 13 — SUBAGENT-GENERAL pattern (Phase C)
- Parallel Agent() calls with operator answers distributed to each persona
- All Phase C Agent() calls have `model: "sonnet"`
- Same parallelism constraint as Phase A

### TC-005: Phase D — Architect consolidation
- Single Agent() with `model: "opus"` (Architect role)
- Agent prompt includes: merge proposedSections, produce Refinement Verdict
- Dual-write: markdown section `## Refinement Verdict` + x-internal-status-update

### TC-006: x-internal-status-update delegation (INLINE-SKILL)
- Uses `Skill(skill: "x-internal-status-update", args: "...")` (Pattern 1)
- Args include `--field refinementVerdict` + `--value {...}`
- Field `verdictHash` computed from verdict content

### TC-007: NO-GO escalation (Rule 29)
- If any persona returns NO-GO, it must NOT appear as a question in Phase B
- NO-GO personas go silent (D5 decision: NO-GOs silent, not questions)
- Phase D Architect receives NO-GO list and may include as `blockers[]`

### TC-008: Phase B — single batch constraint (D4)
- Exactly ONE batch of questions emitted (not per-persona loop)
- Questions deduplicated across personas
- Grouped by dimension (value, ac, contracts, metrics, alternatives, risks)

### TC-009: Capability frontmatter v3.0
- `requires-capabilities` declared in frontmatter
- No v2-style frontmatter (missing `requires-capabilities`)

### TC-010: Source-of-truth path
- SKILL.md at `src/main/resources/targets/claude/skills/core/plan/x-story-refine/SKILL.md`
- Copy exists at `.claude/skills/x-story-refine/SKILL.md`
- Both files are identical

## Acceptance Criteria
- All TC-001 to TC-010 pass
- `audit-frontmatter-schema.sh` exits 0 on SKILL.md
- `audit-tool-call-grammar.sh --mode=static` exits 0
- `audit-skill-visibility.sh` exits 0 (public visibility, no internal marker)
- TelemetryMarkerLint detects 0 violations
