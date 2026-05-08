# Task Breakdown — story-0070-0002

**Story:** Reescrever `_TEMPLATE-EPIC.md` v2
**Epic:** EPIC-0070

## Tasks

### TASK-0070-0002-001 — Rewrite _TEMPLATE-EPIC.md to v2

**Scope:** SIMPLE
**File:** `src/main/resources/shared/templates/_TEMPLATE-EPIC.md`

- Add frontmatter v3.0 (requires-capabilities + template-version)
- Replace sections 1-9 with value-driven v2 sections
- Retain `## 0.5 Cross-Epic Dependencies` and `## Refinement Verdict`
- Write complete, well-guided placeholder text for each section
- Run TC-001 and TC-002 grep validation

### TASK-0070-0002-002 — Sync .claude/templates output

**Scope:** SIMPLE
**File:** `.claude/templates/_TEMPLATE-EPIC.md`

Copy the updated template to `.claude/templates/` (the generated output location that Claude Code reads at runtime).

### TASK-0070-0002-003 — Validate with dogfood epics

**Scope:** SIMPLE

Write brief synthetic examples showing EPIC-0070 and EPIC-0064 metadata in the v2 template format as an inline verification in the commit message / PR description.

## File Footprint

```
write:
  - src/main/resources/shared/templates/_TEMPLATE-EPIC.md
  - .claude/templates/_TEMPLATE-EPIC.md

read:
  - ai/epics/epic-0070-value-driven-templates-v2/story-0070-0002.md
  - .claude/rules/30-value-driven-templates.md
  - docs/adr/ADR-0023-value-driven-templates.md

regen: []
```
