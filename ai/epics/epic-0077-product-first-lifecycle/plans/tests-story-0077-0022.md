# Test Plan — story-0077-0022

**Story:** x-internal-rnf-validate (no-relax markers + justification gate)
**Date:** 2026-05-05

## Test Strategy

| Layer | Class | Type | Count |
| :--- | :--- | :--- | :--- |
| Application | `ValidateRNFNoRelaxUseCaseIT` | Integration | 6 |
| CLI Adapter | `XInternalRnfValidateCommandTest` | Unit | 7 |

## ValidateRNFNoRelaxUseCaseIT

| # | Method | Input | Expected |
| :--- | :--- | :--- | :--- |
| 1 | `execute_allNoRelax_passes` | List of 2 noRelax overrides | result.passed() == true, errors empty |
| 2 | `execute_relaxedWithJustification_passes` | PERFORMANCE relaxed with justification | result.passed() == true |
| 3 | `execute_securityRelaxed_fails` | SECURITY relaxed (with justification) | result.passed() == false, error contains "SECURITY" |
| 4 | `execute_complianceRelaxed_fails` | COMPLIANCE relaxed (with justification) | result.passed() == false, error contains "COMPLIANCE" |
| 5 | `execute_relaxedNoJustification_fails` | PERFORMANCE relaxed, justification null | result.passed() == false |
| 6 | `execute_nullOverrides_throwsIllegalArgument` | null argument | throws IllegalArgumentException |

## XInternalRnfValidateCommandTest

| # | Method | Scenario | Expected |
| :--- | :--- | :--- | :--- |
| 1 | `parseOne_norelaxSpec_returnsNoRelaxOverride` | `PERFORMANCE:norelax` | override.noRelaxed()==true, category==PERFORMANCE |
| 2 | `parseOne_relaxedSpec_returnsRelaxedOverride` | `PERFORMANCE:relaxed:old:new:justified` | override.isRelaxed()==true, justification=="justified" |
| 3 | `parseOne_invalidSpec_throwsIllegalArgument` | `INVALID_FORMAT` | throws IllegalArgumentException |
| 4 | `call_allValid_returnsZero` | 1 norelax override | call() == 0 |
| 5 | `call_securityRelaxed_returnsOne` | SECURITY relaxed | call() == 1 |
| 6 | `call_dryRunWithViolation_returnsZero` | SECURITY relaxed + --dry-run | call() == 0 |
| 7 | `call_invalidSpec_returnsTwo` | malformed spec | call() == 2 |

## Coverage Targets

| Metric | Target |
| :--- | :--- |
| Line | ≥ 95% |
| Branch | ≥ 90% |
