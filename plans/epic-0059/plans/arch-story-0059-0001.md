# Architecture Plan — story-0059-0001

**Story:** Estender audit-execution-integrity.sh para os 6 Artefatos de Fase 1
**Epic:** EPIC-0059
**Date:** 2026-04-27

## Scope

SIMPLE scope — shell script extension with no new dependencies.

## Component Changes

### Primary: `scripts/audit-execution-integrity.sh`

- Add `REQUIRED_PHASE_1_ARTIFACTS` array (6 entries)
- Add `check_phase1_evidence()` function mirroring existing `check_evidence()`
- Extend main loop to call `check_phase1_evidence()` per story
- Add `--scope` flag: `full` (default), `fase1`, `fase3`
- Extend `--self-check` to count artifacts (must equal 10)

### Secondary: `audits/execution-integrity-baseline.txt`

- Append amnesty entries for EPIC-0054, 0055, 0056, 0057
- Format: `story-XXXX-YYYY  # amnesty EPIC-0059`

## Design Decisions

1. **Single function `check_phase1_evidence()`** — isolates Phase 1 checks from existing Phase 3 `check_evidence()` to minimize regression risk
2. **Backward compatible** — default `--scope=full` runs both; `--scope=fase3` preserves old behavior verbatim
3. **Template paths** — `plans/epic-XXXX/plans/{arch,plan,tests,tasks,security,compliance}-story-YYYY.md`
4. **Self-check** counts both arrays; exit 4 if sum != 10

## No ADR Required

Change is additive to existing shell script, no new architectural pattern.
