# Task Breakdown — story-0077-0003

**Story:** story-0077-0003 — Coordenação EPIC-0065: rename x-feature-create  
**Date:** 2026-05-04

---

## Tasks

### TASK-0077-0003-001 — Coordination Record (no branch, direct epic/0077 commit)

**Layer:** Documentation  
**Size:** S

**Deliverables:**
- `ai/epics/epic-0077-product-first-lifecycle/plans/coordination-record-0077-0003.md`

**File Footprint:**
```
write:
  - ai/epics/epic-0077-product-first-lifecycle/plans/coordination-record-0077-0003.md
```

### TASK-0077-0003-002 — Skill Audit Scripts (branch: feat/task-0077-0003-002-skill-rename)

**Layer:** Test/Audit  
**Size:** M

**Deliverables:**
- `src/test/bash/audit-skill-references.sh`
- `src/test/bash/skill-rename-smoke.sh`

**File Footprint:**
```
write:
  - src/test/bash/audit-skill-references.sh
  - src/test/bash/skill-rename-smoke.sh
```

### TASK-0077-0003-003 — Deprecation Docs (branch: feat/task-0077-0003-003-deprecation-notice)

**Layer:** Documentation  
**Size:** S

**Deliverables:**
- `DEPRECATIONS.md`
- `docs/migration/x-feature-create-to-create-feature.md`

**File Footprint:**
```
write:
  - DEPRECATIONS.md
  - docs/migration/x-feature-create-to-create-feature.md
```

## Execution Order

TASK-001 → TASK-002 → TASK-003 (sequential; each depends on prior)

## Story File Footprint (aggregate)

```
write:
  - ai/epics/epic-0077-product-first-lifecycle/plans/coordination-record-0077-0003.md
  - src/test/bash/audit-skill-references.sh
  - src/test/bash/skill-rename-smoke.sh
  - DEPRECATIONS.md
  - docs/migration/x-feature-create-to-create-feature.md
read: (none)
regen: (none)
```
