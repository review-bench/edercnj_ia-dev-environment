# Architecture Plan — story-0077-0003

**Story:** story-0077-0003 — Coordenação EPIC-0065: rename x-feature-create → x-aggregate-create-from-feature  
**Date:** 2026-05-04  
**Planner:** Architect (x-plan-architecture)

---

## 1. Context

story-0077-0003 is a coordination story. It resolves a naming collision between EPIC-0065's feature-creation skill and EPIC-0077's future `x-create-feature` concept (creating a Product Feature from a Product Capability). No domain Java code is introduced; the deliverables are:

- Coordination record (markdown, committed to epic/0077)
- Audit bash script verifying 0 `x-feature-create` references in active code
- Smoke bash script for the renamed skill
- DEPRECATIONS.md
- Migration guide

## 2. Current State Analysis

| Skill | Old name | Current name | Renamed by |
| :--- | :--- | :--- | :--- |
| Feature creation from spec | `x-feature-create` | `x-create-feature` | EPIC-0076 (verb-first refactor) |
| Feature ideation | `x-feature-ideate` | `x-ideate-feature` | EPIC-0076 (verb-first refactor) |

`x-feature-create` no longer exists in `.claude/skills/`. EPIC-0076 resolved the naming conflict before EPIC-0077 reached this story. The coordination outcome to record: EPIC-0077 acknowledges the rename happened and will use `x-create-product-feature` (or similar) for its distinct "Feature-from-Capability" concept.

## 3. Architecture Decision

**Decision:** Document the coordination outcome only. No files to rename (already done by EPIC-0076). Create audit script and deprecation docs that record the historical decision and verify the codebase is clean.

**Rationale:** EPIC-0076 already performed the verb-first naming refactor. story-0077-0003's value is the documented record of coordination between EPIC-0065/0077 and the audit gate proving no legacy references remain.

## 4. Deliverable Mapping

| Task | Deliverable | Layer |
| :--- | :--- | :--- |
| TASK-001 | `plans/coordination-record-0077-0003.md` | Documentation |
| TASK-002 | `src/test/bash/audit-skill-references.sh`, `src/test/bash/skill-rename-smoke.sh` | Test/Audit |
| TASK-003 | `DEPRECATIONS.md`, `docs/migration/x-feature-create-to-create-feature.md` | Documentation |

## 5. File Footprint

```
write:
  - ai/epics/epic-0077-product-first-lifecycle/plans/coordination-record-0077-0003.md
  - src/test/bash/audit-skill-references.sh
  - src/test/bash/skill-rename-smoke.sh
  - DEPRECATIONS.md
  - docs/migration/x-feature-create-to-create-feature.md
read: (none — no existing Java code to modify)
regen: (none)
```

## 6. Dependency Direction

No Java code. All deliverables are in `scripts/`, `docs/`, `src/test/bash/` — zero impact on domain purity (Rule 04).
