# Implementation Plan — story-0069-0003

**Story:** Skill `/x-epic-refine` (multi-persona strategic dispatcher)
**Epic:** EPIC-0069
**Scope:** STANDARD
**Planning Mode:** INLINE

## Summary

Delivers the `x-epic-refine` skill — structurally identical to `x-story-refine` but
scoped to **epics** (strategic level). Same 4-phase dispatcher pattern:
- Phase A: parallel strategic analysis (5 fixed personas + 0–1 conditional SRE/DevOps)
- Phase B: single consolidated batch of strategic questions to operator
- Phase C: parallel specialist refinement with operator answers
- Phase D: Architect consolidation (opus tier) — merges into epic markdown sections
  (§1.3 Escopo, §1.4 Fora do escopo, §6 Decisões Arquiteturais); dual-write verdict
  with `scope="epic"`.

## Key Design Decisions (from story §8)

- D1: Multi-persona 4-phase strategic dispatcher (not linear 7-dimension questionnaire)
- D2: Dual-write via x-internal-status-update (INLINE-SKILL, Rule 13 D-R9)
- D3: Dispatcher=sonnet, persona-agents A/C=sonnet, Architect Phase D=opus (Rule 23)
- D4: Single question batch — no per-persona loop (D-R14)
- D5: NO-GOs applied silently by personas; only genuine ambiguities become questions (D-R15)
- D6: `scope="epic"` in verdict vs `scope="story"` in x-story-refine

## File Footprint

**write:**
- `src/main/resources/targets/claude/skills/core/plan/x-epic-refine/SKILL.md`
- `.claude/skills/x-epic-refine/SKILL.md`

**read:**
- `src/main/resources/targets/claude/skills/core/plan/x-story-refine/SKILL.md` (reference)
- `.claude/rules/29-refinement-gate.md`
- `.claude/rules/23-model-selection.md`
- `.claude/rules/13-skill-invocation-protocol.md`
- `.claude/rules/25-task-hierarchy.md`
- `src/main/resources/targets/claude/knowledge/refinement/dimensions.md` (KP — delivered by story-0069-0001)
- `src/main/resources/targets/claude/agents/core/product-owner.md`
- `src/main/resources/targets/claude/agents/core/tech-lead.md`
- `src/main/resources/targets/claude/agents/core/architect.md`
- `src/main/resources/targets/claude/agents/core/security-engineer.md`
- `src/main/resources/targets/claude/agents/core/qa-engineer.md`
- `src/main/resources/targets/claude/agents/core/sre-engineer.md` (conditional persona)

## Story File Footprint

| File | Operation | Collision risk |
| :--- | :--- | :--- |
| `src/.../core/plan/x-epic-refine/SKILL.md` | write (new file) | None — distinct path from x-story-refine |
| `.claude/skills/x-epic-refine/SKILL.md` | write (copy) | None |
| `knowledge/refinement/dimensions.md` | read only | Soft conflict with story-0069-0002 (both read; 0001 writes) |

## Tasks

### TASK-0069-0003-001: Create source-of-truth SKILL.md

Create `src/main/resources/targets/claude/skills/core/plan/x-epic-refine/SKILL.md`
with:

1. **Frontmatter v3.0** (Rule 28, story §5.1):
   ```yaml
   ---
   name: x-epic-refine
   description: Multi-persona strategic epic refinement dispatcher — parallel persona-agents, batched questions, Architect consolidation
   visibility: public
   user-invocable: true
   model: sonnet
   allowed-tools: [Read, Write, Edit, Skill, Agent, TaskCreate, TaskUpdate, Bash]
   requires-capabilities: [governance.refinement-gate]
   ---
   ```

2. **Phase A** — strategic parallel analysis (story §5.2 + §6.2):
   - Resolve active personas: 5 fixed (PO, Tech Lead, Architect, Security, QA) +
     conditional SRE/DevOps (capability `infra.observability.*` OR `infra.deploy.*`).
   - Emit N TaskCreate per active persona — subject: `epic-XXXX › Refinement › <PersonaName>`.
   - Dispatch N sibling `Agent(subagent_type: "general-purpose", model: "sonnet", ...)` in ONE
     assistant message. Each agent prompt: persona definition + strategic dimensions-owned
     (§5.2) + heuristics (§6.6) + epic markdown. Output schema:
     `{questions, blockers, silent-rejections, proposedSection}`.
   - Emit `TaskUpdate(status: "in_progress")` after collecting outputs.
   - Telemetry: `phase.start/end` pair + `subagent.start/end` per persona.

3. **Phase B** — single consolidated strategic question batch (story §5.2 + §6.3):
   - Dedup by textual similarity (threshold 0.8); group by strategic category
     (Problema, Hipótese/OKRs, Alternativas, Segurança, Qualidade, Operações).
   - Render numbered Markdown batch with persona attribution.
   - Receive operator answer as single text block; distribute relevant answers to personas.
   - Skip Phase B if zero questions from Phase A.
   - Telemetry: `phase.start/end` pair.

4. **Phase C** — parallel strategic refinement with answers (story §5.2 + §6.4):
   - Re-dispatch N sibling Agents in ONE assistant message — same personas with answers
     + refreshed epic markdown. Output: `{proposedSection, dimensionVerdict, notes}`.
   - Collect verdicts; aggregate blockers.
   - Telemetry: `phase.start/end` pair + `subagent.start/end` per persona.

5. **Phase D** — Architect consolidation (story §5.2 + §6.5, D-R13):
   - Dispatch ONE `Agent(subagent_type: "general-purpose", model: "opus", ...)` — Architect
     in consolidator mode. Prompt: all proposedSection outputs + blockers + epic markdown.
     Target sections to merge: §1.3 Escopo, §1.4 Fora do escopo, §6 Decisões Arquiteturais.
     Regenerate canonical `## Refinement Verdict` block with `scope="epic"`.
   - Apply merge to epic markdown via `Edit`.
   - Dual-write verdict atomically:
     ```
     Skill(skill: "x-internal-status-update", model: "haiku",
           args: "--key refinementVerdict --value <json: status, scope=epic, checkedAt, dimensions, blockers>")
     ```
     [required]
   - Emit `TaskUpdate(status: "completed")` for all N tasks.
   - Telemetry: `phase.start/end` pair + `subagent.start/end` for Architect.

6. **Modes** (story §6.7):
   - `--dry-run`: execute Phases A–D without writing markdown or state.
   - `--legacy-refinement`: skip all phases, emit warning, return `verdict.status="tbd"`.

### TASK-0069-0003-002: Copy to .claude/skills/ and smoke-test locally

1. Copy `src/main/resources/targets/claude/skills/core/plan/x-epic-refine/SKILL.md`
   to `.claude/skills/x-epic-refine/SKILL.md`.
2. Verify frontmatter integrity (name, model, requires-capabilities).
3. Run 2 synthetic smoke scenarios (story §6.8):
   - (a) Multi-persona approval: all 5 personas approve, Architect produces
     `refinementVerdict.status="approved"`, `scope="epic"`.
   - (b) PO blocker (silent NO-GO): hypothesis missing measurable KPI →
     `verdict.dimension.po = blocker`, `status="rejected"`.

### TASK-0069-0003-003: Commit

Commit via pre-commit chain (`x-code-format` → `x-code-lint` → `x-git-commit`):
- Commit message: `feat(epic-0069): x-epic-refine multi-persona strategic dispatcher skill`
- Staged files: source-of-truth SKILL.md + `.claude/` copy.

## Dependencies

- **Blocked by:** story-0069-0001 (capability `governance.refinement-gate` + KP `dimensions.md`)
- **Parallel-safe with:** story-0069-0002 (distinct write paths; shared KP is read-only)
- **Blocks:** story-0069-0005 (hook reads verdict), story-0069-0007 (E2E smoke)
