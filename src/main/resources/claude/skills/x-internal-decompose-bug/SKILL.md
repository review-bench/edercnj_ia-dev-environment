---
visibility: internal
user-invocable: false
requires-capabilities: [governance.bug-lifecycle]
description: "Decompose a bug into 2-4 implementation stories by severity+scope rules"
---

> 🔒 **INTERNAL SKILL** — Invoked only by `x-create-bug` and `x-refine-bug`. Not for direct user invocation.

# Skill: x-internal-decompose-bug (slim — ADR-0012)

## Purpose

Decompose a `bug.md` file into 2-4 implementation story files deterministically based on `(severity, scope)`. Implements the decomposition rules table defined in story-0080-0002.

## Triggers (internal only)

```markdown
Skill(skill: "x-internal-decompose-bug", model: "haiku", args: "<bug-id>")
```

Example: `Skill(skill: "x-internal-decompose-bug", args: "bug-000001")`

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `bug-id` | Yes | Bug ID matching pattern `^bug-[0-9]{6}$` |

## Decomposition Rules

| `severity` | `scope` | Stories produced |
| :--------- | :------ | :--------------- |
| any | single-file | story-01-regression-test, story-02-fix |
| any | single-module | story-01-regression-test, story-02-fix |
| HIGH+ | cross-module | story-01-regression-test, story-02-fix, story-03-doc-update |
| CRITICAL | any | story-01-regression-test, story-02-fix, story-03-doc-update, story-04-rollback-plan |

## Output Contract

On success, emits JSON to stdout:

```json
{
  "bugId": "bug-000001",
  "storiesCreated": [
    "ai/bugs/bug-000001/story-01-regression-test.md",
    "ai/bugs/bug-000001/story-02-fix.md"
  ],
  "idempotent": false,
  "wallClockMs": 123
}
```

When idempotent (files already exist):
```json
{
  "bugId": "bug-000001",
  "storiesCreated": [],
  "idempotent": true,
  "wallClockMs": 5
}
```

## Exit Codes

| Code | Name | Condition |
|------|------|-----------|
| 0 | SUCCESS | Stories created (or idempotent skip) |
| 2 | ARGS_INVALID | Bug ID fails pattern `^bug-[0-9]{6}$` |
| 3 | BUG_NOT_FOUND | `ai/bugs/<bug-id>/` directory absent |
| 4 | BUG_FILE_NOT_FOUND | `bug.md` absent inside bug directory |
| 5 | PARSE_ERROR | Cannot extract severity or scope from bug.md |
| 127 | NO_JQ | `jq` absent on PATH |

## Workflow Overview

```text
1. PARSE        -> Validate bug-id regex; resolve ai/bugs/<bug-id>/; verify bug.md presence
2. IDEMPOTENCY  -> If story-01-regression-test.md exists → emit idempotent envelope + exit 0
3. METADATA     -> grep -m1 ^**Severity:**, ^**Scope:** from bug.md (exit 5 on parse failure)
4. RULES        -> Build STORIES_TO_CREATE per decomposition table (severity × scope)
5. INSTANTIATE  -> sed-substitute .claude/templates/_TEMPLATE-BUG-STORY.md per story
                   (per-kind KIND/TITLE/OBJECTIVE/VALUE/BLOCKED_BY mapping)
6. EMIT         -> jq -n envelope with bugId / storiesCreated / idempotent / wallClockMs
```

Detailed bash for each step, regex validation, per-story field mapping (regression-test / fix / doc-update / rollback-plan), and template substitution in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): `^bug-[0-9]{6}$` regex validation (path-traversal guard); directory + file existence checks with exit codes 3/4.
- **Step 2** (§Step 2): single-file existence shortcut; full `jq -n` idempotent envelope.
- **Step 3** (§Step 3): `grep -m1` extraction of Severity/Scope; exit 5 on missing field.
- **Step 4** (§Step 4): array-build logic (cross-module + HIGH/CRITICAL adds doc-update; CRITICAL forces doc-update + rollback-plan).
- **Step 5** (§Step 5): full per-story `case` block with 4 story kinds; 9-token template substitution; wall-clock measurement.
- **Step 6** (§Step 6): array → JSON marshalling via `jq -R | jq -s`.

## Error Handling

| Scenario | Action |
|----------|--------|
| Invalid bug-id (path traversal attempt) | ABORT exit 2 before any file I/O |
| Bug directory missing | ABORT exit 3 |
| `bug.md` missing | ABORT exit 4 |
| severity/scope unparseable | ABORT exit 5 |
| Template not found | ABORT with message |
| Story file write failure | ABORT with message |
| `jq` not available | Exit 127 with "jq is required" |

## Performance Contract

Target: ≤ 5 seconds P95 for typical `bug.md` (≤ 200 lines). All operations are local file I/O + string processing. No network I/O.

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-create-bug` | caller | Invoked after scaffold commit |
| `x-refine-bug` | caller | Can invoke to re-validate decomposition |
| `_TEMPLATE-BUG-STORY.md` | dependency | Template at `.claude/templates/_TEMPLATE-BUG-STORY.md` |
| `_TEMPLATE-BUG.md` | sibling | Parent template used by `x-create-bug` |

## References

- story-0080-0002, task-0080-0002-002
- Template: `.claude/templates/_TEMPLATE-BUG-STORY.md`
- Decomposition rules table: story-0080-0002 §5.2

## Full Protocol

Minimum viable contract above. Detailed bash for all 6 steps, regex-based path-traversal guard, per-story `case` mapping (4 kinds × KIND/TITLE/OBJECTIVE/VALUE/BLOCKED_BY), and 9-token template substitution live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
