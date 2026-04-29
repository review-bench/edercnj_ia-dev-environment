# Implementation Plan — story-0059-0001

**Story:** Estender audit-execution-integrity.sh para os 6 Artefatos de Fase 1
**Epic:** EPIC-0059
**Date:** 2026-04-27

## Task Breakdown

### TASK-0059-0001-001: Add REQUIRED_PHASE_1_ARTIFACTS to audit-execution-integrity.sh

**Files:**
- `scripts/audit-execution-integrity.sh`

**Changes:**
1. Add `REQUIRED_PHASE_1_ARTIFACTS` array after existing constants
2. Add `check_phase1_evidence(story_id)` function
3. Call `check_phase1_evidence` inside main loop

**Artifact paths (template):**
- `plans/epic-XXXX/plans/arch-story-YYYY.md`
- `plans/epic-XXXX/plans/plan-story-YYYY.md`
- `plans/epic-XXXX/plans/tests-story-YYYY.md`
- `plans/epic-XXXX/plans/tasks-story-YYYY.md`
- `plans/epic-XXXX/plans/security-story-YYYY.md`
- `plans/epic-XXXX/plans/compliance-story-YYYY.md`

### TASK-0059-0001-002: Add --scope=fase1 and --self-check extensions

**Files:**
- `scripts/audit-execution-integrity.sh`

**Changes:**
1. Add `--scope` argument parsing (full|fase1|fase3)
2. Extend `--self-check` to count REQUIRED_PHASE_1_ARTIFACTS (must=6) + REQUIRED_PHASE_3_ARTIFACTS (must=4) = 10
3. Print `OK: 10 required artifacts configured` on success
4. Exit 4 on count mismatch

### TASK-0059-0001-003: Populate amnesty baseline (EPIC-0054–0057)

**Files:**
- `audits/execution-integrity-baseline.txt`

**Changes:**
- Append story-0054-0001 through story-0054-0004
- Append story-0055-0001 through story-0055-0012
- Append story-0056-0001 through story-0056-0008
- Append story-0057-0001 through story-0057-0008

## Implementation Notes

- No Java changes required (shell script only)
- Smoke tests: run `./scripts/audit-execution-integrity.sh --self-check`
- Manual verification: create dummy story dirs, verify exit 1 on missing Phase 1 artifacts
