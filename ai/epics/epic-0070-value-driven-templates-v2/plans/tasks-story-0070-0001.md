# Task Breakdown — story-0070-0001

## Tasks

| ID | Title | Deps | Files |
|----|-------|------|-------|
| task-0070-0001-001 | Verify Rule gap + pin Rule 30 | — | .claude/rules/ (read) |
| task-0070-0001-002 | Verify ADR gap + pin ADR-0023 | — | docs/adr/ (read) |
| task-0070-0001-003 | Apply D-R7 + decide Rule separada | 001, 002 | Decision documented in ADR |
| task-0070-0001-004 | Create capability YAML | 003 | capabilities/governance/value-driven-templates.yaml |
| task-0070-0001-005 | Create Rule 30 MD | 003, 004 | src/.../rules/30-value-driven-templates.md + .claude/rules/ |
| task-0070-0001-006 | Create ADR-0023 | 003 | docs/adr/ADR-0023-value-driven-templates.md |
| task-0070-0001-007 | Add SUPERSEDED block to epic-0056.md | 003 | ai/epics/epic-0056-*/epic-0056.md |
| task-0070-0001-008 | Update _index.yaml + audit-gates-catalog | 004 | capabilities/_index.yaml, docs/audit-gates-catalog.md |

## Commit Strategy
- Commit A: tasks 001-003 (decisions — analysis only, no new files)
- Commit B: tasks 004-006 (new artifacts: capability + rule + ADR)
- Commit C: tasks 007-008 (updates: SUPERSEDED + catalog + index)
