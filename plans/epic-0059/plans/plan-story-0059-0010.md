# Implementation Plan — story-0059-0010

## Story
Rule 27 + ZERO-BYPASS block in CLAUDE.md (EPIC-0059 story-0059-0010)

Note: Story refers to "Rule 26" but `.claude/rules/26-audit-gate-lifecycle.md` already exists.
Using Rule 27 (`27-zero-bypass-lifecycle.md`) as the next available rule number.

## File Footprint

### write:
- `.claude/rules/27-zero-bypass-lifecycle.md`
- `java/src/main/resources/targets/claude/rules/27-zero-bypass-lifecycle.md`
- `CLAUDE.md` (append ZERO-BYPASS block + update rules index)
- `java/src/main/resources/targets/claude/CLAUDE.md` (same changes)

### read:
- `.claude/rules/24-execution-integrity.md`
- `.claude/rules/26-audit-gate-lifecycle.md`
- `CLAUDE.md`

### regen:
- None

## Tasks

### TASK-0059-0010-001
- Create `.claude/rules/27-zero-bypass-lifecycle.md` with 6 mandatory sections
- Create `java/src/main/resources/targets/claude/rules/27-zero-bypass-lifecycle.md`
- Branch: `feat/task-0059-0010-001-rule-27`

### TASK-0059-0010-002
- Update `CLAUDE.md` with ZERO-BYPASS LIFECYCLE block and rules index entry for Rule 27
- Update `java/src/main/resources/targets/claude/CLAUDE.md`
- Branch: `feat/task-0059-0010-002-claude-md-zero-bypass`
