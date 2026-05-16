---
visibility: public
user-invocable: true
requires-capabilities: [governance.bug-lifecycle]
description: "Create a structured bug report from the RA9 template"
---

# Skill: Create Bug Report (slim — ADR-0012)

Create a new bug report from the standard template with automatic slug generation, status initialization, and structured metadata.

## Triggers

- `/x-create-bug "Login form crashes on invalid email"` — create bug with description only
- `/x-create-bug "Login form crashes on invalid email" --severity HIGH` — create with severity
- `/x-create-bug "Login form crashes on invalid email" --scope SIMPLE --severity HIGH` — full invocation
- `/x-create-bug "Session token leaks to browser console" --severity CRITICAL` — security bug

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `description` | String | Yes | — | Bug description (8–200 characters) |
| `--severity` | Enum | No | MEDIUM | One of: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `--scope` | Enum | No | STANDARD | One of: `SIMPLE`, `STANDARD`, `COMPLEX` |
| `--output` | String | No | `docs/bugs/` | Directory for bug file (must exist) |
| `--id` | String | No | auto | Auto-generated as next integer; override if needed |

## Output Contract

On success, the skill creates:

1. **Bug file:** `{output}/bug-{ID}-{SLUG}.md` with complete template and metadata
2. **Status field:** `**Status:** Pendente` (initial state)
3. **Metadata:** YAML frontmatter + all 9 RA9 sections with defaults

Returns JSON to stdout:

```json
{
  "bugId": "bug-0001",
  "slug": "login-form-crashes-on-invalid-email",
  "bugFile": "docs/bugs/bug-0001-login-form-crashes-on-invalid-email.md",
  "severity": "HIGH",
  "scope": "STANDARD",
  "status": "Pendente",
  "created": "2026-05-07T14:23:15Z"
}
```

## Workflow Overview

```text
1. PARSE_VALIDATE -> Validate description (8–200 chars), severity enum, scope enum, output dir
2. SLUG           -> NFKD normalize + strip non-alphanumeric + lowercase + collapse hyphens + truncate-40
3. ASSIGN_ID      -> Auto-detect MAX(existing bug-NNNN) + 1, or validate user-provided --id
4. LOAD_TEMPLATE  -> Read .claude/templates/_TEMPLATE-BUG.md (abort on missing)
5. INSTANTIATE    -> sed-substitute {{BUG_DESCRIPTION}}/{{BUG_ID}}/{{SLUG}}/{{SEVERITY}}/{{SCOPE}}/... 
6. WRITE          -> Write to {output}/bug-{ID}-{SLUG}.md
7. INIT_STATUS    -> Ensure **Status:** Pendente is present (inject after header if missing)
8. EMIT           -> jq response envelope (bugId, slug, bugFile, severity, scope, status, created)
```

Detailed bash for each step, slug-generation pipeline, template substitution map, and worked examples in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): full validation bash with exit-code-1 on each guard.
- **Step 2** (§Step 2): `iconv` + `sed` + `tr` slug-generation pipeline; 40-char truncation.
- **Step 3** (§Step 3): auto-increment from `ls bug-*.md` + `sort -n | tail -1`; `bug-NNNN` regex for user-provided IDs.
- **Step 4** (§Step 4): template path `.claude/templates/_TEMPLATE-BUG.md`; abort on missing.
- **Step 5** (§Step 5): 10-substitution map (`{{BUG_DESCRIPTION}}`, `{{BUG_ID}}`, `{{SLUG}}`, `{{SEVERITY}}`, `{{SCOPE}}`, `{{PERSONA}}`, `{{VERSION_OR_SHA}}`, `{{OS_RUNTIME}}`, `{{JAVA_VERSION}}`, `{{DATE}}`).
- **Steps 6–7** (§Step 6/7): atomic write + Pendente initialization with `sed` injection fallback.
- **Step 8** (§Step 8): `jq -n` envelope construction.
- **Worked Examples** (§Examples): 3 end-to-end invocations with JSON outputs.

## Error Handling

| Scenario | Exit Code | Message |
|----------|-----------|---------|
| Description too short (< 8 chars) | 1 | "Description must be 8–200 characters" |
| Description too long (> 200 chars) | 1 | "Description must be 8–200 characters" |
| Invalid severity | 1 | "Severity must be LOW\|MEDIUM\|HIGH\|CRITICAL" |
| Invalid scope | 1 | "Scope must be SIMPLE\|STANDARD\|COMPLEX" |
| Output directory missing | 1 | "Output directory not found: {path}" |
| Template not found | 1 | "Template not found: {path}" |
| Slug generation failed | 1 | "Slug generation failed (no alphanumeric content)" |
| Invalid user-provided ID | 1 | "Bug ID must match pattern bug-NNNN" |
| File write failed | 1 | "Failed to write bug file: {path}" |
| jq not available | 127 | "jq is required" |

## Performance Contract

Target: < 500 ms for slug generation + file creation. No network I/O; all operations are local file and string processing.

## Knowledge Pack References

Read `.claude/knowledge/governance/bug-lifecycle.md` for the canonical bug lifecycle state machine.

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-refine-bug` | called after | Refine workflow accepts created bugs |
| `x-implement-epic` | consumes | Epic lifecycle can invoke to create test bugs |
| `_TEMPLATE-BUG.md` | dependency | Template must exist at `.claude/templates/_TEMPLATE-BUG.md` |
| `bug-lifecycle.yaml` | dependency | Capability declaration must be present |

## References

- Capability: `config/capabilities/bug-lifecycle.yaml`
- Template: `.claude/templates/_TEMPLATE-BUG.md`
- Refinement: `/x-refine-bug`
- EPIC-0080, story-0080-0001, task-0080-0001-003

## Full Protocol

Minimum viable contract above. Detailed bash for all 8 steps, slug-generation pipeline, template substitution map, worked examples, and acceptance test scenarios live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
