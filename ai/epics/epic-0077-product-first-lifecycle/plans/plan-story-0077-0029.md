# Implementation Plan — story-0077-0029

**Story:** Rule 19 Amendment — flowVersion "5" Fallback Matrix Registration  

## Task Sequence

### TASK-0077-0029-001: Amend Rule 19 fallback matrix

**Branch:** `feat/task-0077-0029-001-rule19-flowversion5`

1. Edit `src/main/resources/targets/claude/rules/19-backward-compatibility.md`
   - Add row `| Field = "5" (explicit) | "5" | EPIC-0077 Product-First ... | No |` to Fallback Matrix table
   - Add `### productFirstLifecycle Field (EPIC-0077)` section with fallback matrix
2. Edit `governance/schemas/execution-state-1.0.json`
   - Add `"5"` to `flowVersion.enum`
   - Add `productFirstLifecycle: { type: boolean }` property
3. Update `.claude/rules/19-backward-compatibility.md` (generated output — build unavailable)
   - Mirror same changes as source-of-truth

### TASK-0077-0029-002: Update audit-flow-version.sh

**Branch:** `feat/task-0077-0029-002-audit-flow-version-v5`  
**Dependencies:** TASK-0077-0029-001

1. Edit all stack templates (`go`, `python`, `node`, `java-maven`, `spring-boot`, `java-gradle`): add `"5"` to valid values
2. Edit `src/main/resources/targets/claude/scripts/audit-flow-version.sh`: add `"5"` to `VALID_VALUES`
3. Edit `.claude/scripts/audit-flow-version.sh` (generated output): mirror changes
4. Create `src/test/bash/audit-flow-version-v5.sh`: 4 Gherkin scenarios

## Regression Risk: NONE

No existing values `"1"`–`"4"` are altered. Additive-only changes.
