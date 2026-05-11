---
name: x-internal-write-report
description: "Renders _TEMPLATE-*.md from a JSON payload and writes the result atomically."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--template <name> --output <path> --data <json-or-@path> [--append]"
category: internal-ops
context-budget: light
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL** — Invoked only by other skills (orchestrators) via the Skill tool. Not user-invocable. Callers: `x-implement-epic`, `x-orchestrate-epic`, `x-implement-story`.

# Skill: x-internal-write-report (slim — ADR-0012)

## Purpose

Render a template file under `.claude/templates/` (or an explicit path) by:

1. Reading the template body from disk.
2. Parsing a structured JSON payload (inline or `@path/to/data.json`).
3. Substituting `{{KEY}}` placeholders, including nested dot-paths (`{{stories.story-0049-0001.status}}`).
4. Expanding `{{#each <collection>}}…{{/each}}` blocks with block-local placeholder resolution against each array element.
5. Writing the rendered output to a target path — either overwriting, or appending with per-ID deduplication when `--append=true`.

Replaces the ad-hoc `Read template + Write output` pattern previously inlined in `x-implement-epic`, `x-orchestrate-epic`, and `x-implement-story` (EPIC-0049 S9/S10/S15), centralising report rendering so templates can evolve without editing N orchestrator skills.

## Triggers

Bare-slash form intentionally omitted — invoked only by orchestrators via Rule 13 INLINE-SKILL:

```text
Skill(skill: "x-internal-write-report",
      args: "--template _TEMPLATE-EPIC-EXECUTION-PLAN.md \
             --output ai/epics/epic-XXXX/reports/exec-plan.md \
             --data @ai/epics/epic-XXXX/reports/exec-plan.data.json")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--template <name>` | M | — | Template file name resolved under `.claude/templates/<name>`. Absolute path accepted verbatim |
| `--output <path>` | M | — | Target path for the rendered file. Created with parent directories |
| `--data <json-or-@path>` | M | — | Structured payload: inline JSON string, or `@path/to/data.json` to read from disk |
| `--append` | O | `false` | When `true`, merge the rendered output into an existing file using `## ID: <value>` section markers for deduplication |

## Response Contract

Single-line JSON object on stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `outputPath` | `String` | yes | Absolute path of the written file |
| `bytesWritten` | `Integer` | yes | Final size of the output file in bytes |
| `placeholdersReplaced` | `Integer` | yes | Total count of `{{KEY}}` substitutions (loop expansions count each inner substitution once) |
| `entriesAppended` | `Integer\|Null` | only on `--append=true` | Number of new `## ID:` sections appended (existing sections updated in place do not increment this count) |

## Exit Codes

| Code | Name | Condition |
| :--- | :--- | :--- |
| 0 | SUCCESS | Render and write completed |
| 1 | TEMPLATE_NOT_FOUND | Template file cannot be read |
| 2 | INVALID_JSON | `--data` payload fails to parse |
| 3 | UNRESOLVED_PLACEHOLDER | Strict-mode placeholder has no value in data |
| 4 | WRITE_FAILED | Atomic `mv` of tmp file failed |

## Placeholder Grammar (concise)

- `{{key}}` — top-level scalar lookup against the data object.
- `{{a.b.c}}` — dot-path into nested objects. Arrays indexable via `{{stories.0.id}}` but discouraged outside tests; use `{{#each}}` for list rendering.
- Missing keys in strict mode (default) → `UNRESOLVED_PLACEHOLDER` (exit 3) with the exact placeholder token in the message.
- `{{#each <collection>}} … {{/each}}` — collection selector dot-path resolves to array of objects. Inside the block, placeholders resolve against the current element first, falling back to root data. Blocks MUST balance. Nested loops up to depth 3. Empty collection → zero iterations, no error.

Full grammar with edge cases (escaping, whitespace handling, nested loop fallback rules) in [full protocol §Placeholder Grammar](references/full-protocol.md).

## Workflow Overview

```text
1. PARSE      -> Argument parsing + validation (reject unknown/dup flags)
2. READ TPL   -> Read template body from disk (exit 1 on miss)
3. PARSE DATA -> Parse inline JSON or @path payload (exit 2 on invalid)
4. EXPAND     -> Inside-out {{#each}} block expansion via regex state machine
5. SUBSTITUTE -> Walk remaining {{...}} tokens; strict mode → exit 3 on miss
6. WRITE      -> Atomic tmp+rename; --append mode does in-memory section merge
7. ENVELOPE   -> Emit {outputPath, bytesWritten, placeholdersReplaced, entriesAppended}
```

## Workflow Detail

The full step-by-step protocol (argument parsing edge cases, template resolution rules, data parse jq command, `{{#each}}` inside-out expansion algorithm with `expand_innermost_each` contract, strict-mode substitution rules with token-level error messages, atomic write tmp+rename in both overwrite and `--append` modes, 7 concrete examples covering simple/loop/append/error cases, performance contract, and testing matrix) lives in [`references/full-protocol.md`](references/full-protocol.md). The slim contract above is sufficient for happy-path operation.

## Convention Anchors (x-internal-*)

| Aspect | Value |
| :--- | :--- |
| Path | `internal/ops/x-internal-write-report/` |
| Frontmatter `visibility` | `internal` |
| Frontmatter `user-invocable` | `false` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block |
| Allowed tools | `Bash` only |
| Naming | `x-internal-{subject}-{action}` |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under `internal/**` satisfies all 6 anchors above. Violations fail `LifecycleIntegrityAuditTest`.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 (sysexits EX_USAGE) |
| `jq` absent on PATH | Exit 127 with `jq is required`; abort before any write |
| `--data @path` points to unreadable file | Exit 2 (`INVALID_JSON`) with the path echoed |
| `{{#each expr}}` resolves to non-array | Exit non-zero; message includes the expression and resolved type |
| Unbalanced `{{#each}}` / `{{/each}}` | Exit non-zero; message identifies the dangling token |
| Strict-mode miss on any placeholder | Exit 3 (`UNRESOLVED_PLACEHOLDER`) with the full token text |
| `mv` fails mid-write | Delete tmp file; exit 4 (`WRITE_FAILED`); existing output file untouched |
| Output parent directory not writable | Exit 4 after cleaning up the tmp file |
| Path traversal attempt (`..` above project root) | Exit non-zero; message names the offending path (Rule 06) |

## Performance Contract

- Render < 200 ms for templates < 50 KB with ≤ 3 nested `{{#each}}` levels (DoD-local target of story-0049-0006).
- Memory usage bounded by template size + 2× data-payload size; renderer streams rendered body to tmp file.
- Idempotent under repeated invocations with identical template, data, and output path (same bytes, same sha256).

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with `visibility: internal` from the `.claude/README.md` skill-inventory table, the `/help` menu, and user-facing autocomplete. Internal skills are still copied into `.claude/skills/` (flat layout) so `Skill(skill: "x-internal-write-report")` invocations resolve correctly. **User cannot see it; orchestrators can invoke it.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers — telemetry is produced by the invoking orchestrator (the phase wrapping the orchestrator's own step is the correct aggregation boundary). Passive hooks still capture `tool.call` for the underlying `Bash` invocation.

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts internal skills from the 3-option menu contract).

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-implement-epic` | caller | Phase-completion reports, epic execution plan rendering |
| `x-orchestrate-epic` | caller | Per-story planning report rendering |
| `x-implement-story` | caller | Story-level remediation / review dashboard rendering |
| `x-internal-write-story-report` | caller (Wave 2.5 sibling) | Delegates final render of story-completion report |
| `x-internal-update-status` | peer | Sibling `x-internal-*` skill; both scoped under `internal/ops/`; no shared runtime state |
| `x-reconcile-status` | consumer | Downstream reader of reports produced here; never mutates concurrently (Rule 22) |

## Full Protocol

Minimum viable contract above. Detailed workflow (full placeholder grammar with edge cases; argument parsing and template resolution; data parse with `jq -e`; `{{#each}}` inside-out expansion algorithm; `expand_innermost_each` contract steps 1–4; strict substitution and counter rules; atomic write contract; `--append` merge algorithm with `## ID:` section semantics; 7 concrete examples from simple render to error cases; performance contract; testing matrix of 10 acceptance scenarios) lives in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
