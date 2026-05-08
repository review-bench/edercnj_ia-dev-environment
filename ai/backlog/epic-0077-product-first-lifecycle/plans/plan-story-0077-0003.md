# Implementation Plan — story-0077-0003

**Story:** story-0077-0003 — Coordenação EPIC-0065: rename x-feature-create  
**Date:** 2026-05-04

---

## Sequence

### Phase A — TASK-0077-0003-001 (Coordination Record, no branch)

Create `coordination-record-0077-0003.md` committed directly to `epic/0077`.

- Document coordination outcome: EPIC-0076 already renamed `x-feature-create → x-create-feature`
- Confirm: EPIC-0077 will use distinct naming for its "Feature from Capability" concept
- Committed via `x-commit-planning` to `ai/epics/epic-0077-product-first-lifecycle/plans/`

### Phase B — TASK-0077-0003-002 (Audit Scripts, branch: feat/task-0077-0003-002-skill-rename)

1. Create `src/test/bash/audit-skill-references.sh` — scans codebase for legacy `x-feature-create` references in active skill paths, exits 1 if found
2. Create `src/test/bash/skill-rename-smoke.sh` — verifies `x-create-feature/SKILL.md` exists and smoke-tests name
3. Run audit script to verify 0 violations

### Phase C — TASK-0077-0003-003 (Deprecation Docs, branch: feat/task-0077-0003-003-deprecation-notice)

1. Create `DEPRECATIONS.md` — records `x-feature-create` as removed (renamed to `x-create-feature` in EPIC-0076)
2. Create `docs/migration/x-feature-create-to-create-feature.md` — migration guide with side-by-side comparison

## Exit Criteria

- `audit-skill-references.sh` exits 0 (0 legacy references found)
- `DEPRECATIONS.md` created with correct removal date
- `docs/migration/` guide created
- All 3 task PRs merged to epic/0077
