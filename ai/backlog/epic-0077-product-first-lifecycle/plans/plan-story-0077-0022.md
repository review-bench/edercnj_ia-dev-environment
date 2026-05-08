# Implementation Plan — story-0077-0022

**Story:** x-internal-rnf-validate (no-relax markers + justification gate)
**Date:** 2026-05-05

## Summary

Wire existing domain `RNFNoRelaxValidator` into an application-layer use case, expose it through a picocli CLI command, and create the SKILL.md for the `x-internal-rnf-validate` internal skill.

## TASK-0077-0022-001 — ValidateRNFNoRelaxUseCase + IT

**File:** `src/main/java/dev/iadev/application/capability/ValidateRNFNoRelaxUseCase.java`

- Constructor creates `RNFNoRelaxValidator` internally (no injection needed — pure domain)
- `execute(List<RNFOverride>)` → validates null arg, delegates to validator
- Returns `RNFRootValidationResult`

**Test file:** `src/test/java/dev/iadev/application/capability/ValidateRNFNoRelaxUseCaseIT.java`

Scenarios:
1. `execute_allNoRelax_passes` — all overrides intact
2. `execute_relaxedWithJustification_passes` — PERFORMANCE relaxed with justification
3. `execute_securityRelaxed_fails` — SECURITY cannot be relaxed
4. `execute_complianceRelaxed_fails` — COMPLIANCE cannot be relaxed
5. `execute_relaxedNoJustification_fails` — non-hard-blocked category with no justification
6. `execute_nullOverrides_throwsIllegalArgument` — guard check

## TASK-0077-0022-002 — XInternalRnfValidateCommand + tests

**File:** `src/main/java/dev/iadev/adapter/inbound/cli/XInternalRnfValidateCommand.java`

Picocli `@Command`:
- `--override` (repeatable) — spec string: `CATEGORY:norelax[:originalValue]` or `CATEGORY:relaxed:original:newValue:justification`
- `--dry-run` — always returns 0 (parse + report only)

Exit codes: 0=OK, 1=VALIDATION_FAILURE, 2=EXECUTION_ERROR

**Test file:** `src/test/java/dev/iadev/adapter/inbound/cli/XInternalRnfValidateCommandTest.java`

Scenarios (unit, no real file I/O):
1. `parseOne_norelaxSpec_returnsNoRelaxOverride`
2. `parseOne_relaxedSpec_returnsRelaxedOverride`
3. `parseOne_invalidSpec_throwsIllegalArgument`
4. `call_allValid_returnsZero`
5. `call_securityRelaxed_returnsOne`
6. `call_dryRunWithViolation_returnsZero`
7. `call_invalidSpec_returnsTwo`

## TASK-0077-0022-003 — SKILL.md

**File:** `src/main/resources/targets/claude/skills/core/internal/ops/x-internal-rnf-validate/SKILL.md`
**Generated copy:** `.claude/skills/x-internal-rnf-validate/SKILL.md`

SKILL.md content: purpose, parameters table, exit codes, examples, integration notes.
