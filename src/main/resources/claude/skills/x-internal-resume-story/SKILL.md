---
name: x-internal-resume-story
description: "Detects resumable state of a story from execution-state.json (read-only envelope)."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--story-id <story-XXXX-YYYY> --epic-id <XXXX>"
category: internal-plan
context-budget: light
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL** — Invoked only by other skills (orchestrators) via the Skill tool. Not user-invocable. Caller: `x-implement-story` (Phase 0 resume detection carve-out).

# Skill: x-internal-resume-story (slim — ADR-0012)

## Purpose

Carve out the story-resume detection logic currently duplicated in `x-implement-story` Phase 0 (step 8, "Resume detection") into a single, single-responsibility skill. The ~120 inline lines the orchestrator previously used to classify in-flight task state become a single `Skill(skill: "x-internal-resume-story", …)` invocation; the orchestrator shrinks to a read-the-envelope consumer that drives its branch-creation and task-dispatch decisions off the four response fields.

**Responsibilities (single):**

1. Resolve and read `ai/epics/epic-XXXX/execution-state.json` via `x-internal-update-status --read-only` so the concurrency contract (shared `flock -s`) is honoured identically to every other read consumer.
2. Locate `stories.<id>` and its `tasks.*` sub-nodes; when absent, exit `2` (`STORY_NOT_IN_STATE`).
3. Classify each task by `status`: DONE synonyms (`DONE` / `MERGED` / `COMPLETE` / `Concluída` / `Concluida`) → `tasksCompleted`; everything else → `tasksPending`.
4. Determine `resumePoint`: `fresh-start` when nothing DONE; `all-done` when nothing pending; `phase-2-task-<N>` (1-based index of first PENDING/IN_PROGRESS task in task-order) otherwise.
5. Extract `lastCommitSha`: `commitSha` of the most recently completed task; `null` when empty.
6. Compute `staleWarnings`: one warning per DONE task whose `completedAt` is older than the story file's mtime; empty when none apply or mtime unavailable.

**Non-responsibilities (explicit):** does NOT mutate the state file, the story markdown, or any commit metadata; does NOT create branches, run PR operations, or dispatch tasks (caller's concern); does NOT resolve unknown statuses to a default — any unrecognised status is treated as PENDING with a stderr warning while the envelope stays well-formed.

## Triggers

Bare-slash form intentionally omitted — invoked only by orchestrators via Rule 13 INLINE-SKILL:

```text
Skill(skill: "x-internal-resume-story",
      args: "--story-id story-0049-0013 --epic-id 0049")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--story-id <id>` | M | — | Story identifier (`story-XXXX-YYYY` canonical form) |
| `--epic-id <id>` | M | — | 4-digit epic identifier (`XXXX`) — used to resolve `ai/epics/epic-XXXX/` |

All three argument forms (`--key value`, `--key=value`, unknown-flag rejection) are supported; unknown/missing flags exit `64` (sysexits `EX_USAGE`). `--story-id` is normalised to lowercase and validated against `^story-[0-9]{4}-[0-9]{4}$`; `--epic-id` zero-padded to 4 digits.

## Response Contract

Single-line JSON on stdout. The envelope is always well-formed; consumers MUST read `resumePoint` as the authoritative branching signal.

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `resumePoint` | `String` | yes | `fresh-start` (nothing DONE), `all-done` (every task DONE), or `phase-2-task-<N>` where `<N>` is the 1-based index of the first PENDING/IN_PROGRESS task |
| `tasksCompleted` | `Array<{id:String, commitSha:String\|Null}>` | yes | One entry per DONE task in task-order; `commitSha` is `null` when absent from the state node |
| `tasksPending` | `Array<String>` | yes | Task IDs (e.g., `TASK-0049-0013-002`) of PENDING/IN_PROGRESS/FAILED/BLOCKED tasks, in task-order |
| `lastCommitSha` | `String\|Null` | yes | `commitSha` of the most recent DONE task; `null` when `tasksCompleted` is empty |
| `staleWarnings` | `Array<String>` | yes | Human-readable warnings when story file's mtime is newer than a DONE task's `completedAt`; empty when no warning applies |

### Example envelopes

**Happy path (3 DONE of 5):**

```json
{"resumePoint":"phase-2-task-4","tasksCompleted":[{"id":"TASK-0049-0013-001","commitSha":"abc123"},{"id":"TASK-0049-0013-002","commitSha":"def456"},{"id":"TASK-0049-0013-003","commitSha":"ghi789"}],"tasksPending":["TASK-0049-0013-004","TASK-0049-0013-005"],"lastCommitSha":"ghi789","staleWarnings":[]}
```

**Fresh start:** `"resumePoint":"fresh-start","tasksCompleted":[],...,"lastCommitSha":null`.

**All done with stale warning:** `"resumePoint":"all-done","tasksPending":[],"staleWarnings":["Story file modified after task TASK-0049-0013-001 DONE"]`.

## Exit Codes

| Code | Name | Condition |
| :--- | :--- | :--- |
| 0 | SUCCESS | Envelope emitted |
| 1 | STATE_FILE_MISSING | `ai/epics/epic-XXXX/execution-state.json` absent |
| 2 | STORY_NOT_IN_STATE | Story not registered inside the state file's `stories` map |
| 64 | EX_USAGE | Unknown or malformed flag |
| 127 | DEPENDENCY_MISSING | `jq` absent on `PATH` |

No JSON is written to stdout on any non-zero exit — callers distinguish success from failure by exit code, not by parsing stdout.

## Workflow Overview

```text
1. PARSE       -> Argument parsing + path resolution
2. READ STATE  -> Delegate to x-internal-update-status --read-only (flock -s)
                  with jq fallback for bootstrap (exit 1 on miss, 2 on story miss)
3. CLASSIFY    -> Iterate story.tasks in insertion order; bucket DONE synonyms
                  vs everything else; emit stderr warn for unknown statuses
4. RESUME PT   -> fresh-start | all-done | phase-2-task-<N>
                  + lastCommitSha = commitSha of last DONE task
5. STALENESS   -> stat -f %m / stat -c %Y (BSD/GNU); compare completedAt vs mtime;
                  short-circuit empty when story mtime unknown
6. ENVELOPE    -> jq -nc assembly + single-line stdout emission
```

## Workflow Detail

The full step-by-step protocol (argument-parser rejection matrix; `x-internal-update-status --read-only` delegation rationale and bootstrap `jq` fallback; full task-classification synonym table including PR_MERGED nuance; resume-point algorithm with task-order invariant edge cases; ISO-8601 → epoch portability across BSD/GNU `date` flavours; stale-warning best-effort contract; envelope `jq -nc` assembly; 6 concrete examples covering fresh-start / mid-resume / stale / state-missing / story-missing / all-done; performance budget per step) lives in [`references/full-protocol.md`](references/full-protocol.md). The slim contract above is sufficient for happy-path operation.

## Convention Anchors (x-internal-*)

| Aspect | Value |
| :--- | :--- |
| Path | `internal/plan/x-internal-resume-story/` |
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
| Unknown flag | Print `unknown flag <name>; usage:`; exit 64 |
| `jq` absent on PATH | Print `dependency missing: jq`; exit 127 |
| `stat` flavour mismatch (GNU vs BSD) | Try `stat -f %m` (BSD) first, fall back to `stat -c %Y` (GNU); any failure treats story mtime as 0 and short-circuits Step 5 |
| `execution-state.json` absent | Exit 1 with `execution-state.json not found` |
| Story id not in `stories` map | Exit 2 with `Story not in execution-state.json` |
| `x-internal-update-status` unavailable (bootstrap) | Fall back to direct `jq -c '.stories[<id>]'` on the state file; schema validation is then best-effort |
| `completedAt` malformed on a DONE task | Skip that task in Step 5; do not emit a stale warning for it; continue |
| Story file absent (mtime unknown) | `staleWarnings=[]`; no error |
| Unknown task status | Emit stderr `warn: unknown status …`; bucket to `tasksPending`; envelope stays well-formed |
| `tasks` sub-node absent on the story | Treat as zero tasks → `resumePoint=fresh-start`, empty arrays |

## Performance Contract

Target: < 200 ms for a typical story with 5 tasks in `execution-state.json` (matches the Global DoD in story-0049-0013 §4). Reads at most 2 files (1 state file via `jq` + `flock -s`, 1 story file via `stat`) and spawns a single `jq` pass for envelope assembly. No network I/O.

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with `visibility: internal` from the `.claude/README.md` skill-inventory table, the `/help` menu, and user-facing autocomplete. Internal skills are still copied into `.claude/skills/` (flat layout) so `Skill(skill: "x-internal-…")` invocations resolve correctly. **User cannot see them; orchestrators can invoke them.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers — telemetry is produced by the invoking orchestrator (Phase 0 resume-detection is the correct aggregation boundary). Passive hooks still capture `tool.call` for the underlying `Bash` invocation.

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts internal skills from the 3-option menu contract).

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-implement-story` | caller (primary) | Phase 0 carve-out: replaces ~120-line inline "Resume detection" block before entering Phase 1 |
| `x-implement-epic` | caller (indirect) | Consumes the same envelope when iterating in-flight stories at epic scope (per-story branch reuse) |
| `x-internal-update-status` | delegate (primary, `--read-only`) | Provides the shared-lock read path into `execution-state.json`. This skill is the canonical read-only consumer — it never writes |
| `x-internal-load-story-context` | peer | Sibling `internal/plan/` skill; runs at Phase 0 alongside this skill — both idempotent |
| `x-internal-verify-story` | peer | Sibling `internal/plan/` skill; runs at END of story lifecycle (Phase 3), whereas this skill runs at Phase 0 BEFORE task dispatch |
| `x-reconcile-status` | consumer (peer) | Reads same `stories.<id>.tasks.*` nodes in diagnose mode; no shared mutation — both take `flock -s` |

## Full Protocol

Minimum viable contract above. Detailed workflow (argument-parser rejection matrix; `x-internal-update-status --read-only` delegation rationale and bootstrap fallback to direct `jq`; full task-classification synonym table; resume-point algorithm with task-order invariant edge case algorithm; ISO-8601 → epoch portability matrix BSD vs GNU `date`; stale-warning best-effort contract; envelope `jq -nc` assembly; 6 concrete examples; performance profile measured per step; acceptance test scenarios) lives in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
