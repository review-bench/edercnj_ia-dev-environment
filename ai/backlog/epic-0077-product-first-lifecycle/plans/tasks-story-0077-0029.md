# Task Breakdown — story-0077-0029

**Story:** Rule 19 Amendment — flowVersion "5" Fallback Matrix Registration  

## Tasks

| ID | Title | Layer | Size | Branch |
| :--- | :--- | :--- | :--- | :--- |
| TASK-0077-0029-001 | Amend Rule 19 fallback matrix — add flowVersion "5" | Infrastructure/Doc | S | `feat/task-0077-0029-001-rule19-flowversion5` |
| TASK-0077-0029-002 | Update audit-flow-version.sh to accept "5" | Infrastructure/Script | S | `feat/task-0077-0029-002-audit-flow-version-v5` |

## Files

### TASK-0077-0029-001
- `src/main/resources/targets/claude/rules/19-backward-compatibility.md` (edit)
- `governance/schemas/execution-state-1.0.json` (edit)
- `.claude/rules/19-backward-compatibility.md` (generated output — mirror)

### TASK-0077-0029-002
- `src/main/resources/targets/claude/scripts/go/audit-flow-version.sh.tpl` (edit)
- `src/main/resources/targets/claude/scripts/python/audit-flow-version.sh.tpl` (edit)
- `src/main/resources/targets/claude/scripts/node/audit-flow-version.sh.tpl` (edit)
- `src/main/resources/targets/claude/scripts/java-maven/audit-flow-version.sh.tpl` (edit)
- `src/main/resources/targets/claude/scripts/spring-boot/audit-flow-version.sh.tpl` (edit)
- `src/main/resources/targets/claude/scripts/java-gradle/audit-flow-version.sh.tpl` (edit)
- `src/main/resources/targets/claude/scripts/audit-flow-version.sh` (edit)
- `.claude/scripts/audit-flow-version.sh` (generated output — mirror)
- `src/test/bash/audit-flow-version-v5.sh` (new)
