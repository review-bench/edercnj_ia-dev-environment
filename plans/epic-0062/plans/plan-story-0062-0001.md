# Implementation Plan — story-0062-0001

**Story:** story-0062-0001  
**Scope:** SIMPLE  

## Task Breakdown

| Task | File | Change |
| :--- | :--- | :--- |
| task-0062-0001-001 | 6 audit scripts | Add `BASELINE_DIR` var; replace `audits/` with `${BASELINE_DIR}/` |
| task-0062-0001-002 | scripts/tests/test-audit-baseline-parametrization.sh | New smoke test (3 scenarios) |
| task-0062-0001-003 | .github/workflows/ci-release.yml | Update comments |

## Implementation Notes

- Sed-based replacement of hardcoded `audits/` in code paths
- Comments referencing `audits/` updated to use `${BASELINE_DIR}`
- Test script validates default, override, and AC1 grep check
