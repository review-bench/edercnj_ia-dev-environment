# Implementation Plan — story-0070-0001

**Story:** Capability + Rule 30 + ADR-0023 + decisão substituição EPIC-0056

## Tasks

| Task | Description |
|------|-------------|
| task-0070-0001-001 | Verify Rule number gap → pin Rule 30 |
| task-0070-0001-002 | Verify ADR number gap → pin ADR-0023 |
| task-0070-0001-003 | Apply D-R7 overlap criterion → Rule separada |
| task-0070-0001-004 | Create `capabilities/governance/value-driven-templates.yaml` |
| task-0070-0001-005 | Create Rule 30 markdown (source + generated) |
| task-0070-0001-006 | Create `docs/adr/ADR-0023-value-driven-templates.md` |
| task-0070-0001-007 | Add SUPERSEDED block to `epic-0056.md` |
| task-0070-0001-008 | Update `docs/audit-gates-catalog.md` + `capabilities/_index.yaml` |

## Implementation Order
All tasks are metadata/governance artifacts — sequential, single commit per task group.
Commit strategy: one atomic commit for tasks 1-3 (decisions), one for tasks 4-6 (new artifacts), one for tasks 7-8 (updates).

## File Footprint
**write:** capabilities/governance/value-driven-templates.yaml, capabilities/_index.yaml, src/main/resources/targets/claude/rules/30-value-driven-templates.md, .claude/rules/30-value-driven-templates.md, docs/adr/ADR-0023-value-driven-templates.md, docs/audit-gates-catalog.md
**read:** governance/schemas/capabilities-1.0.json, .claude/rules/ (gap check), docs/adr/ (gap check), ai/epics/epic-0056-*/epic-0056.md
**regen:** none
