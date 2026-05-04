# Test Plan — story-0077-0029

**Story:** Rule 19 Amendment — flowVersion "5" Fallback Matrix Registration  
**Test type:** Audit / Smoke  

## Scenarios (from §5.2 Gherkin)

1. **Happy path:** flowVersion "5" → exit 0, no fallback warning
2. **Warning path:** `productFirstLifecycle` absent → exit 0 + WARN `[productFirstLifecycle-absent]`
3. **Boundary:** flowVersion "five" (typo) → fallback v1 + WARN `[flowVersion-fallback]`
4. **Regression:** flowVersion "4" → exit 0, unchanged behavior

## Evidence Artifacts

- `src/test/bash/audit-flow-version-v5.sh` — 4-scenario smoke script (new file)
- Exit codes per Rule 26 §Standardized (0=OK, 1=violation, 2=operational error)
