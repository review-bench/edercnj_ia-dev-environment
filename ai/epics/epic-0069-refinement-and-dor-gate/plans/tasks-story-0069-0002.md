# Task Breakdown — story-0069-0002

**Story:** Skill `/x-story-refine` (multi-persona dispatcher)
**Epic:** EPIC-0069

## File Footprint

```
write:
  - src/main/resources/targets/claude/skills/core/plan/x-story-refine/SKILL.md
  - .claude/skills/x-story-refine/SKILL.md
read:
  - .claude/rules/29-refinement-gate.md
  - .claude/agents/*.md (PO, Tech Lead, Architect, Security, QA personas)
  - src/main/resources/targets/claude/knowledge/refinement/dimensions.md
regen: []
```

## Tasks

### TASK-0069-0002-001: Create SKILL.md at source-of-truth path

**Path:** `src/main/resources/targets/claude/skills/core/plan/x-story-refine/SKILL.md`

**Content:**
- Frontmatter: name, description, model=sonnet, requires-capabilities=[governance.refinement-gate], allowed-tools
- Phase A: parallel specialist analysis (5-7 sibling Agent() in ONE message)
  - Personas: PO, Tech Lead, Architect, Security, QA + conditionals (Perf if SLA declared, DevOps if infra changes)
  - Each agent: reads story markdown + rule 29 dimensions KP + persona agent def
  - Returns: gap-report {persona, dimension, gap, question?, noGo?}
- Phase B: dedup + group → single batch → operator responds via AskUserQuestion → distribute answers
- Phase C: 5-7 sibling Agent() with answers → collect proposedSections
- Phase D: 1 Agent() Architect (opus) → merge + Refinement Verdict → dual-write
  - x-internal-status-update for state file (INLINE-SKILL)
  - Write `## Refinement Verdict` block to story markdown (direct Write tool)
- Telemetry markers on all 4 phases
- Task hierarchy (TaskCreate/TaskUpdate) per phase

**Dependencies:** dimensions.md KP (story-0069-0001 ✅), Rule 29 (story-0069-0001 ✅)

### TASK-0069-0002-002: Copy SKILL.md to .claude/skills/

**Path:** `.claude/skills/x-story-refine/SKILL.md`

Copy from source-of-truth. Both files must be byte-identical.

Note: `.claude/` is gitignored, so copy must be applied explicitly.

### TASK-0069-0002-003: Commit and push

Commit message: `feat(epic-0069): x-story-refine skill — multi-persona 4-phase dispatcher`

Scope: `src/main/resources/targets/claude/skills/core/plan/x-story-refine/SKILL.md`

Push to `epic/0069`.
