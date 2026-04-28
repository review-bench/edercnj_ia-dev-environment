---
generated-by: x-internal-story-build-plan@1bb391629efbed76158e524466d207669d540e94
story-id: story-0062-0007
epic-id: EPIC-0062
---

# Implementation Plan — story-0062-0007

## Goal

Update 6-7 rule source files and regenerate 11 golden fixtures so that
`grep -rE "(^|[^a-z])(adr|specs|audits)/" java/src/main/resources/targets/claude/rules/`
returns zero operational hits.

## Tasks

### task-0062-0007-001: Inventory refs in rules
- Run targeted grep across rules 05, 13, 24, 25, 26, 27, 45
- Document all hits with file and line number

### task-0062-0007-002 through 008: Apply path replacements per rule file
- `audits/foo-baseline.txt` → `governance/baselines/foo-baseline.txt`
- `adr/ADR-XXXX-` → `docs/adr/ADR-XXXX-`
- `specs/SPEC-` → `docs/specs/SPEC-`
- Edit only source-of-truth under `java/src/main/resources/targets/claude/rules/`

### task-0062-0007-009: Regenerate golden fixtures
- `mvn -q process-resources`
- `mvn -q test -Dtest=GoldenFileRegenerator -DupdateGolden=true`

### task-0062-0007-010: Run full test suite
- `mvn test` — verify GREEN

## File Footprint

### write:
- `java/src/main/resources/targets/claude/rules/05-quality-gates.md`
- `java/src/main/resources/targets/claude/rules/24-execution-integrity.md`
- `java/src/main/resources/targets/claude/rules/25-task-hierarchy.md`
- `java/src/main/resources/targets/claude/rules/26-audit-gate-lifecycle.md`
- `java/src/main/resources/targets/claude/rules/27-zero-bypass-lifecycle.md`
- `src/test/resources/golden/**` (11 profiles, regen)

### read:
- `java/src/main/resources/targets/claude/rules/*.md`
- `src/test/resources/golden/**`
