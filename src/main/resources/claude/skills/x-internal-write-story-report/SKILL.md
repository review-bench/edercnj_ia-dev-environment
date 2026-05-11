---
name: x-internal-write-story-report
description: "Generates the consolidated story-completion report from execution-state.json."
visibility: internal
user-invocable: false
allowed-tools: Bash, Skill
argument-hint: "--story-id <story-XXXX-YYYY> --epic-id <XXXX> --output <path>"
category: internal-plan
context-budget: light
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL** — Invoked only by other skills (orchestrators) via the Skill tool. Not user-invocable. Caller: `x-implement-story` Phase 3 final-report carve-out.

# Skill: x-internal-write-story-report (slim — ADR-0012)

## Purpose

Carve out the "final story report" block that `x-implement-story` inlines at the end of Phase 3 into a single, single-responsibility skill. The orchestrator passes three flags (`--story-id`, `--epic-id`, `--output`) and consumes a compact `{reportPath, summary}` envelope; the skill handles data collection, template resolution, and delegates rendering to `x-internal-write-report`.

**Responsibilities (single):**

1. Locate and read `ai/epics/epic-XXXX/execution-state.json` (exit 1 `STATE_NOT_FOUND` when absent).
2. Extract the story node (`.stories.<story-id>`) and its tasks map.
3. Compute the `summary` object (`tasksCount`, `tasksDone` via DONE synonym set, `commitsCount`, `prNumber`, `prState`, `coverageLine`, `coverageBranch`).
4. Build a structured JSON data payload (scalar summary + per-task arrays + findings).
5. Resolve template `.claude/templates/_TEMPLATE-STORY-COMPLETION-REPORT.md` (exit 2 `TEMPLATE_MISSING` when absent).
6. Delegate render to `Skill(skill: "x-internal-write-report", ...)`.
7. Emit `{reportPath, summary}` envelope on stdout as a single JSON line.

**Non-responsibilities (explicit):** does NOT mutate `execution-state.json`, run tests/coverage/reviews, create branches/PRs, emit telemetry markers, or implement its own render engine. All disk mutations are delegated to `x-internal-write-report` (atomic tmp+rename).

## Triggers

Bare-slash form intentionally omitted — invoked only by orchestrators via Rule 13 INLINE-SKILL:

```text
Skill(skill: "x-internal-write-story-report",
      args: "--story-id story-0049-0001 --epic-id 0049 --output ai/epics/epic-XXXX/reports/story-XXXX-YYYY-report.md")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--story-id <id>` | M | — | Story identifier (`story-XXXX-YYYY` canonical form; lowercase-normalised) |
| `--epic-id <id>` | M | — | 4-digit epic identifier (`XXXX`) — resolves `ai/epics/epic-XXXX/` (zero-padded) |
| `--output <path>` | M | — | Target path for the rendered report; forwarded verbatim to `x-internal-write-report --output` |

All three flags support both `--key value` and `--key=value` forms. Unknown/missing flags exit `64` (sysexits `EX_USAGE`). `--story-id` validated against `^story-[0-9]{4}-[0-9]{4}$`; `--epic-id` zero-padded to 4 digits and validated against `^[0-9]{4}$`.

## Response Contract

Single-line JSON on stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `reportPath` | `String` | yes | Absolute path of the written report (echoed from `x-internal-write-report`) |
| `summary` | `Object` | yes | Compact summary used by the orchestrator for final logging |

### `summary` sub-schema

| Field | Type | Description |
| :--- | :--- | :--- |
| `tasksCount` | `Integer` | Total tasks under `.stories.<id>.tasks` (0 when map is empty) |
| `tasksDone` | `Integer` | Subset of `tasksCount` in the DONE synonym set |
| `commitsCount` | `Integer` | Count of distinct non-empty `commitSha` values across tasks |
| `prNumber` | `Integer \| Null` | Story-level PR number; `null` when no PR exists |
| `prState` | `String \| Null` | `MERGED`, `OPEN`, or `CLOSED`; `null` when `prNumber=null` |
| `coverageLine` | `Number \| Null` | Line-coverage percentage; `null` when verification has not run |
| `coverageBranch` | `Number \| Null` | Branch-coverage percentage; `null` when verification has not run |

### Example envelope (happy path)

```json
{"reportPath":"/repo/ai/epics/epic-XXXX/reports/story-XXXX-YYYY-report.md","summary":{"tasksCount":5,"tasksDone":5,"commitsCount":5,"prNumber":42,"prState":"MERGED","coverageLine":96.4,"coverageBranch":92.1}}
```

## Exit Codes

| Code | Name | Condition |
| :--- | :--- | :--- |
| 0 | SUCCESS | Report rendered and response envelope emitted |
| 1 | STATE_NOT_FOUND | `ai/epics/epic-XXXX/execution-state.json` missing or unreadable / story node absent |
| 2 | TEMPLATE_MISSING | `.claude/templates/_TEMPLATE-STORY-COMPLETION-REPORT.md` absent |
| 64 | EX_USAGE | Unknown or malformed flag |
| 127 | DEPENDENCY_MISSING | `jq` absent on `PATH` |

Render-time failures (e.g., `UNRESOLVED_PLACEHOLDER`) propagate the `x-internal-write-report` exit code unchanged so the caller branches on the original semantic. No envelope is emitted on any non-zero exit.

## Workflow Overview

```text
1. PARSE          -> Argument parsing + path resolution + traversal guard
2. READ STATE     -> Best-effort jq read of .stories[<id>] (exit 1 on miss)
3. COMPUTE SUMMARY -> DONE-synonym classification + 7 summary scalars
4. BUILD PAYLOAD  -> jq -n assembly of full render payload (scalars + arrays)
5. TEMPLATE GATE  -> Local template existence check (exit 2 on miss)
6. DELEGATE       -> Skill(x-internal-write-report, --template ... --output ... --data ...)
7. ENVELOPE       -> Emit {reportPath, summary} JSON to stdout
```

## Workflow Detail

The full step-by-step protocol (argument parser rejection matrix, `execution-state.json` jq paths used, DONE synonym set, payload assembly `jq -n` expression, render-payload schema in TypeScript, PR-state `ascii_upcase` normalization rules, coverage edge cases, template-version compatibility policy, performance profile by step, acceptance test scenarios, and consumer/forward-compatibility contract) lives in [`references/full-protocol.md`](references/full-protocol.md). The slim contract above is sufficient for happy-path operation.

## Convention Anchors (x-internal-*)

| Aspect | Value |
| :--- | :--- |
| Path | `internal/plan/x-internal-write-story-report/` |
| Frontmatter `visibility` | `internal` |
| Frontmatter `user-invocable` | `false` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block |
| Allowed tools | `Bash, Skill` |
| Naming | `x-internal-{subject}-{action}` |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under `internal/**` satisfies all 6 anchors above. Violations fail `LifecycleIntegrityAuditTest`.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 |
| Unknown flag | Print `unknown flag <name>; usage:`; exit 64 |
| `--output` empty or traversal-unsafe | Print `output path unsafe`; exit 64 |
| `jq` absent on PATH | Print `dependency missing: jq`; exit 127 |
| `execution-state.json` missing | Exit 1 (`STATE_NOT_FOUND`); no envelope |
| Story node absent from state | Exit 1; stderr distinguishes via message |
| `.tasks` / `.pr` / `.verification.coverage` / `.reviews.findings` keys absent | Treat as empty / null; exit 0 |
| Template file absent | Exit 2 (`TEMPLATE_MISSING`); no envelope |
| `x-internal-write-report` UNRESOLVED_PLACEHOLDER (exit 3) | Propagate exit code 3 unchanged |
| `x-internal-write-report` WRITE_FAILED (exit 4) | Propagate exit code 4 unchanged |

## Performance Contract

Target: < 500 ms end-to-end for a typical story (5–10 tasks). Memory bounded by state-file size + 2× payload size; the skill never loads the rendered report into memory. Idempotent under repeated invocations with identical state file and `--output` path (renderer's atomic tmp+rename). Detailed budget per step in [§7 of full-protocol](references/full-protocol.md#7-performance-profile-measured-on-laptop-class).

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with `visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills are still copied into `.claude/skills/` (flat layout) so `Skill(skill: "x-internal-write-story-report")` invocations from other skills resolve correctly. The invariant: **user cannot see it; orchestrators can invoke it.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers — telemetry is produced by the invoking orchestrator (the phase wrapping the orchestrator's own step — Phase 3 in this case — is the correct aggregation boundary). Passive hooks still capture `tool.call` for the underlying `Bash` invocations (`jq`) and the nested `Skill(x-internal-write-report)` dispatch.

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts internal skills from the 3-option menu contract).

## Knowledge Pack References

Read `.claude/knowledge/lifecycle/execution-integrity.md`.

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-implement-story` | caller (primary) | Phase 3 final-report carve-out: orchestrator passes `--story-id --epic-id --output` and consumes `{reportPath, summary}` |
| `x-internal-write-report` | downstream (delegated render) | Resolves `{{KEY}}` placeholders and `{{#each tasks}} / {{#each findings}}` loops against the payload built in Step 4 |
| `x-internal-update-status` | peer (read-only consumer) | Both read `execution-state.json`; this skill does NOT write to it |
| `x-internal-verify-story` | upstream producer | Writes `.verification.coverage` consumed in Step 3 |
| `x-internal-resume-story` | peer | Shares the DONE synonym set and task-classification logic; no runtime coupling |
| `x-review-codebase` / `x-review-pr` | upstream producers | Write `.reviews.findings` consumed in Step 4 |
| `x-reconcile-status` | downstream reader | May read the rendered report for epic-wide status reconciliation |

## Full Protocol

Minimum viable contract above. Detailed workflow (argument-parser rejection matrix; complete `execution-state.json` jq paths consumed; DONE synonym set vs everything else; full render-payload schema in TypeScript; PR-state `ascii_upcase` normalization rules; coverage field edge cases; template-version compatibility policy; performance profile measured per step; testing matrix with concrete fixtures; consumer + forward-compatibility contracts) lives in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
