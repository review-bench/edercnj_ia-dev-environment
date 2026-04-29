# Test Plan — story-0059-0001

**Story:** Estender audit-execution-integrity.sh para os 6 Artefatos de Fase 1
**Epic:** EPIC-0059
**Date:** 2026-04-27

## Test Strategy

SIMPLE scope — shell script smoke tests + manual scenario verification.

## Acceptance Tests (Gherkin Scenarios)

### Scenario 1: Audit passes when no story present in PR
- Execute script with no story patterns in history
- Expect: exit 0, `storiesChecked: []`

### Scenario 2: Audit passes when story has all 10 artifacts
- Setup: create temp story dir with 6 Phase 1 + 4 Phase 3 artifacts
- Execute: `--story-id story-0059-0099`
- Expect: exit 0

### Scenario 3: Audit fails when story has no Phase 1 artifacts
- Setup: create temp story dir with only Phase 3 artifacts
- Execute: `--story-id story-0059-0099 --scope=fase1`
- Expect: exit 1 (EIE_EVIDENCE_MISSING)

### Scenario 4: Audit passes for story in baseline
- Verify: `story-0057-0003` in baseline
- Execute: `--story-id story-0057-0003`
- Expect: exit 0 (grandfathered)

### Scenario 5: Self-check returns OK with 10 artifacts
- Execute: `--self-check`
- Expect: exit 0, output contains "OK: 10 required artifacts configured"

### Scenario 6: --scope=fase1 skips Phase 3 validation
- Setup: story with Phase 1 present, Phase 3 absent
- Execute: `--story-id story-XXXX-YYYY --scope=fase1`
- Expect: exit 0

### Scenario 7: Corrupt baseline returns exit 2
- Setup: malformed baseline file
- Expect: exit 2 (EIE_BASELINE_CORRUPT)

## Unit Tests

- `check_phase1_evidence()` returns 0 when all 6 artifacts present
- `check_phase1_evidence()` returns 1 when any artifact missing
- Argument parsing: `--scope` accepts fase1/fase3/full only
- Self-check counts: 6 + 4 = 10

## Test Commands

```bash
# Self-check
scripts/audit-execution-integrity.sh --self-check

# Smoke test Phase 1 enforcement
mkdir -p /tmp/test-story/plans/epic-0099/plans
scripts/audit-execution-integrity.sh --story-id story-0099-0001 --scope=fase1
# Expect exit 1

# Create all 6 Phase 1 artifacts then retest
touch /tmp/test-story/plans/epic-0099/plans/{arch,plan,tests,tasks,security,compliance}-story-0099-0001.md
scripts/audit-execution-integrity.sh --story-id story-0099-0001 --scope=fase1
# Expect exit 0
```
