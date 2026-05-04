# Test Plan — story-0077-0006

## Unit Tests

### T1: `RNFNoRelaxValidatorTest`
- `allNoRelax_noOverrideValues_passes` — all RNFs with noRelaxed=true, no overrideValue → PASS
- `oneRelaxed_withJustification_passes` — noRelaxed=false with justification → PASS
- `oneRelaxed_noJustification_rejects` — noRelaxed=false without justification → FAIL with error
- `mandatoryCategory_relaxed_rejectsEvenWithJustification` — SECURITY noRelaxed=false → FAIL (hard-block for mandatory)
- `multipleViolations_reportsAll` — 2 violations → 2 errors

## Integration Tests

### IT1: `RNFOverrideApprovalIT`
- `requestApproval_setsStatusToPending` — submitting override enters pending
- `approveOverride_changesStatusToApproved` — approval action → approved
- `rejectOverride_changesStatusToRejected` — rejection → rejected

## Smoke Tests

### S1: `capability-template-smoke.sh`
- All 7 sections present in example-capability-auth.md → exit 0
- Missing section → exit 1 CAPABILITY_TEMPLATE_VIOLATION

### S2: `rnf-override-audit.sh`
- --self-check → exit 0
