---
name: x-internal-update-status
description: "Atomic read-modify-write of execution-state.json with flock and schema validation."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--file <path> --type <epic|story|task> --id <id> --field <name> --value <value> [--initialize] [--read-only]"
category: internal-ops
context-budget: light
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL** — Invoked only by orchestrators (callers: `x-implement-epic`, `x-implement-story`, `x-fix-epic-pr`). Not for direct user invocation. This is the PILOT story (story-0049-0005) of the `x-internal-*` convention.

# Skill: x-internal-update-status (slim — ADR-0012)

## Purpose

Perform atomic read-modify-write mutations of `ai/epics/epic-XXXX/execution-state.json` (the telemetry checkpoint file consumed by every orchestrator). The operation:

1. Acquires a `flock`-based advisory lock with 30s timeout.
2. Reads the full JSON document.
3. Validates the resolved path exists in the schema.
4. Compares `previousValue` vs `newValue` — emits `noOp=true` when equal (idempotency per RULE-002).
5. Writes via tmp-file + rename (atomic on POSIX).
6. Releases the lock.

Replaces ad-hoc `Edit`-tool invocations from inside orchestrators, which historically lost updates under `--parallel` execution (EPIC-0042 post-mortems).

## Convention Anchors (x-internal-* PILOT)

| Aspect | Value |
| :--- | :--- |
| Path | `internal/ops/x-internal-update-status/` |
| Frontmatter `visibility` | `internal` |
| Frontmatter `user-invocable` | `false` |
| Body marker | `> 🔒 **INTERNAL SKILL**` as first non-frontmatter content |
| Allowed tools | `Bash` only (single shell pipeline) |
| Naming | `x-internal-{subject}-{action}` (subject = `status`, action = `update`) |

Audit: Rule 22 (Lifecycle Integrity) validates these 6 anchors. Violations fail `LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form intentionally omitted — never invoked by a human. All invocations follow Rule 13 INLINE-SKILL pattern:

```markdown
Skill(skill: "x-internal-update-status",
      args: "--file ai/epics/epic-XXXX/execution-state.json \
             --type story --id story-0049-0005 \
             --field status --value MERGED")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--file <path>` | O | `execution-state.json` in cwd | Target state file |
| `--type <epic\|story\|task>` | M | — | Scope of the node being mutated |
| `--id <id>` | M | — | Epic ID (`0049`), story ID (`story-0049-0005`), or task ID (`TASK-0049-0005-001`) |
| `--field <name>` | M | — | Schema field to update (e.g., `status`, `prNumber`, `commitSha`) |
| `--value <value>` | M | — | New value; coerced to the schema-declared type |
| `--initialize` | O | `false` | Create the file with an empty schema skeleton when absent |
| `--read-only` | O | `false` | Read and return the current value without acquiring the write lock |

## Response Contract

Single-line JSON object on stdout:

| Field | Type | Description |
| :--- | :--- | :--- |
| `previousValue` | `String\|Null` | Value before write; null when field was absent |
| `newValue` | `String` | Value after write (equal to previous on no-op) |
| `fileSha` | `String(64)` | sha256 of the file after the write |
| `noOp` | `Boolean` | `true` when `previousValue == newValue` |

## Exit Codes

| Code | Name | Condition |
| :--- | :--- | :--- |
| 0 | SUCCESS | Write or no-op completed |
| 1 | FILE_NOT_FOUND | File missing and `--initialize=false` |
| 2 | LOCK_TIMEOUT | `flock` timed out after 30s |
| 3 | INVALID_PATH | `--type/--id/--field` tuple does not resolve in schema |
| 4 | WRITE_FAILED | Atomic `mv` of tmp file failed |
| 64 | EX_USAGE | Missing required flag |
| 127 | NO_JQ | `jq` absent on PATH |

## Workflow Overview

```text
Step 1: PARSE_ARGS     -> validate flags; resolve schema path by --type/--id/--field
Step 2: LOCK_ACQUIRE   -> flock -w 30 on <file>.lock; exit 2 on timeout
Step 3: FILE_INIT      -> exit 1 if absent and !--initialize; else write skeleton
Step 4: PATH_VALIDATE  -> jq path resolution; exit 3 if absent
Step 5: IDEMPOTENCY    -> noOp=true short-circuit when previousValue == newValue
Step 6: ATOMIC_WRITE   -> jq update to tmp; mv tmp file; exit 4 on mv fail
Step 7: EMIT_RESPONSE  -> sha256 + printf single-line JSON; flock released via FD closure
Step 8: READ_ONLY      -> when --read-only=true, skip Steps 2/6/7; use shared flock -s
```

Each step's full bash, schema-path resolution rules per `--type`, and `flock` semantics live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): path resolution table per `--type` (`epic` → `<field>`; `story` → `stories.<id>.<field>`; `task` → `stories.<parentStoryId>.tasks.<id>.<field>`); parent-story inference from task-ID prefix.
- **Steps 2–7** (§Step 2..7): full `flock`/`jq`/`mv` bash blocks; idempotency short-circuit; atomic-rename contract on POSIX.
- **Step 8** (§Step 8): read-only short-circuit with shared lock.
- **Worked examples 1–6** (§Worked Examples): happy path (PENDING→MERGED), no-op, `--initialize`, task-level update, read-only query, INVALID_PATH error.

## Concurrency Contract

- All readers and writers acquire the same `<file>.lock` FD. Exclusive (`flock -x`) for writes; shared (`flock -s`) for `--read-only`.
- Two concurrent invocations on **different fields** of the same file serialize through the lock; both updates are preserved.
- Two concurrent invocations on **the same field** serialize; the later caller observes the earlier caller's value as `previousValue` — correct "last writer wins" semantic.
- Lock is released automatically on process exit (FD closure). No `trap`-based cleanup required.

## Trailer Injection Contract (story-0059-0004)

Every write that stages and commits `execution-state.json` MUST inject the canonical trailer so `.githooks/commit-msg` allows the commit:

```
Co-Authored-By: x-internal-update-status@<40-char-git-sha>
```

Full commit invocation pattern, trailer-validation regex, and recovery-escape semantics in [`references/full-protocol.md`](references/full-protocol.md) §Trailer Injection Contract.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 (sysexits EX_USAGE) |
| `jq` absent on PATH | Exit 127 with `jq is required`; abort before lock |
| Concurrent invocation exceeds 30s wait | Exit 2 (`LOCK_TIMEOUT`) — caller retries with backoff |
| `mv` fails mid-write | Delete tmp file; exit 4 (`WRITE_FAILED`); state file untouched |
| `--initialize` collides with existing non-JSON file | Exit 4 — do not overwrite |
| Schema version mismatch | Log warning to stderr; proceed (non-blocking per RULE-006) |

## Telemetry

Internal skills do NOT emit `phase.start` / `phase.end` markers — telemetry is produced by the invoking orchestrator. Passive hooks still capture `tool.call` for the underlying `Bash` invocation.

## Knowledge Pack References

Read `.claude/knowledge/lifecycle/refinement-gate.md` for the lifecycle context that drives most status mutations.

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-implement-epic` | caller | Phase 2 (per-story status transitions) + Phase 4 (epic finalization) |
| `x-implement-story` | caller | Phase 2 (per-task status transitions) + Phase 3 (story finalization) |
| `x-fix-epic-pr` | caller | Records per-PR correction state when fanning out across an epic |
| `x-reconcile-status` | peer | Reads the same file to diagnose drift against markdown; never mutates concurrently (Rule 22) |
| `x-evaluate-parallelism` | consumer | Reads the resulting state file to build the collision matrix |

Downstream stories: story-0049-0013, story-0049-0018, story-0049-0019.

## Full Protocol

Minimum viable contract above. Detailed step-by-step bash (8 steps with `flock`/`jq`/`mv` blocks), 6 worked examples, concurrency semantics, trailer injection contract, acceptance test scenarios, and generator filter contract live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
