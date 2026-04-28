---
generated-by: x-internal-story-build-plan@1bb391629efbed76158e524466d207669d540e94
story-id: story-0062-0007
epic-id: EPIC-0062
---

# Test Plan — story-0062-0007

## Acceptance Criterion (AC1)

```bash
grep -rE "(^|[^a-z])(adr|specs|audits)/" \
  java/src/main/resources/targets/claude/rules/
```
Expected: zero operational hits (only `docs/adr/` new canonical form remains).

## Acceptance Criterion (AC2)

Golden fixture test suite passes:
```bash
mvn test -Dtest=GoldenFileRegeneratorTest
```
Expected: BUILD SUCCESS.

## Acceptance Criterion (AC3)

Full Maven test suite passes:
```bash
mvn test
```
Expected: BUILD SUCCESS.

## Test Scenarios

### Scenario 1: Rule 05 — adr/ replaced with docs/adr/
- Given: `05-quality-gates.md` contained `adr/ADR-NNN-*.md`
- When: edit applied
- Then: file contains `docs/adr/ADR-NNN-*.md`

### Scenario 2: Rule 24 — audits/ replaced with governance/baselines/
- Given: 3 occurrences of `audits/execution-integrity-baseline.txt`
- When: edits applied
- Then: all 3 reference `governance/baselines/execution-integrity-baseline.txt`

### Scenario 3: Rule 25 — audits/ replaced with governance/baselines/
- Given: 3 occurrences of `audits/task-hierarchy-baseline.txt`
- When: edits applied
- Then: all 3 reference `governance/baselines/task-hierarchy-baseline.txt`

### Scenario 4: Rule 26 — audits/ replaced with governance/baselines/
- Given: 2 occurrences of `audits/`
- When: edits applied
- Then: both reference `governance/baselines/`

### Scenario 5: Rule 27 — audits/ replaced with governance/baselines/
- Given: 4 occurrences of `audits/execution-integrity-baseline.txt`
- When: edits applied
- Then: all 4 reference `governance/baselines/execution-integrity-baseline.txt`

### Scenario 6: Golden fixtures regenerated
- Given: 11 fixture profiles under `src/test/resources/golden/`
- When: `GoldenFileRegenerator` runs with `-DupdateGolden=true`
- Then: all 11 profiles updated to reflect new rule content
