# Test Plan — story-0062-0001

**Story:** story-0062-0001  
**Scope:** SIMPLE (shell scripts)

## Acceptance Tests

| AC | Test | Result |
| :--- | :--- | :--- |
| AC1 | `grep -nE "['\"]audits/['\"]" scripts/audit-*.sh` → 0 hits | PASS |
| AC2 | `bash scripts/audit-baseline-immutability.sh --self-check` | PASS |
| AC3 | `BASELINE_DIR=/tmp bash audit-baseline-immutability.sh --self-check` | PASS |
| AC4 | `bash scripts/tests/test-audit-baseline-parametrization.sh` | 4/4 PASS |
| AC5 | CI green (no behavioral change) | PASS |

## Test Script

`scripts/tests/test-audit-baseline-parametrization.sh` covers 3 scenarios:
1. Default BASELINE_DIR reads from `audits/`
2. Override via `BASELINE_DIR` env var reads from custom path
3. Zero literal `"audits/"` in audit scripts (AC1)
