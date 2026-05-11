---
name: x-internal-ensure-epic-branch
description: "Ensures epic/XXXX branch exists idempotently, locally and on origin."
visibility: internal
user-invocable: false
allowed-tools: Bash
argument-hint: "--epic-id <XXXX> [--base <branch>] [--push <true|false>]"
category: internal-git
context-budget: light
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL** — Invoked only by orchestrators (callers: `x-epic-create`, `x-epic-decompose`, `x-orchestrate-epic`, `x-implement-epic`, `x-epic-map`). Not for direct user invocation.

# Skill: x-internal-ensure-epic-branch (slim — ADR-0012)

## Purpose

Single, idempotent entry point for the RULE-001 convention "one branch per epic (`epic/<ID>`), always exists, always pushed". Orchestrators MUST call this skill at step 0 instead of inlining `git checkout -b` / `git push -u origin` to eliminate three regressions: duplicate branches in parallel waves, local-only branches lagging origin, and divergent base commits across entry-points.

Responsibilities (single):

1. Validate `--epic-id` against `^\d{4}$`.
2. Resolve branch name → `epic/<ID>`.
3. Detect local existence (`git rev-parse --verify`).
4. Detect remote existence (`git ls-remote`).
5. Apply 3-state decision:
   - **new** (neither) ⇒ delegate to `Skill x-create-git-branch --push`;
   - **local-only** ⇒ emit complementary `git push -u origin`;
   - **local + remote** ⇒ idempotent no-op.
6. Emit single-line JSON envelope.

This skill does NOT create branches directly — all creation goes through `x-create-git-branch` (story-0049-0001) to preserve RULE-001's "one creation path" invariant.

## Convention Anchors (x-internal-* 7th skill)

| Aspect | Value |
| :--- | :--- |
| Path | `internal/git/x-internal-ensure-epic-branch/` |
| Frontmatter `visibility` | `internal` |
| Frontmatter `user-invocable` | `false` |
| Body marker | `> 🔒 **INTERNAL SKILL**` as first non-frontmatter content |
| Allowed tools | `Bash` only (delegates via Skill tool) |
| Naming | `x-internal-{subject}-{action}` (subject = `epic-branch`, action = `ensure`) |

Audit: Rule 22 (Lifecycle Integrity) validates these 6 anchors. Violations fail `LifecycleIntegrityAuditTest`.

## Triggers

Bare-slash form intentionally omitted — never invoked by a human. All invocations follow Rule 13 INLINE-SKILL pattern:

```markdown
Skill(skill: "x-internal-ensure-epic-branch",
      args: "--epic-id 0049")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--epic-id <XXXX>` | M | — | 4-digit numeric epic ID. Must match `^\d{4}$`. |
| `--base <branch>` | O | `develop` | Base branch for delegated creation. Must exist locally. |
| `--push <true\|false>` | O | `true` | When `true`, guarantees presence on `origin`. When `false`, creation is local-only and remote-check is skipped. |

## Response Contract

Single-line JSON object on stdout:

| Field | Type | Description |
| :--- | :--- | :--- |
| `branchName` | `String` | Resolved branch name (`epic/<ID>`). |
| `baseSha` | `String(40)` | SHA of `--base` at invocation time. |
| `created` | `Boolean` | `true` when branch was created in this invocation. |
| `alreadyExisted` | `Boolean` | `true` when branch already existed locally before. |
| `pushedNow` | `Boolean` | `true` when this invocation issued `git push -u origin`. |

Invariant: exactly one of (`created`, `alreadyExisted`) is `true`. `pushedNow` is independent.

## Exit Codes

| Code | Name | Condition |
| :--- | :--- | :--- |
| 0 | SUCCESS | Ensure completed (created, no-op, or complementary push) |
| 1 | INVALID_EPIC_ID | `--epic-id` missing or fails `^\d{4}$` |
| 2 | BASE_NOT_FOUND | `--base` does not resolve locally |
| 3 | DELEGATION_FAILED | `x-create-git-branch` returned non-zero |
| 4 | PUSH_FAILED | Complementary `git push` returned non-zero |
| 64 | EX_USAGE | Unknown flag |
| 127 | NO_GIT | `git` absent on `PATH` |

## Workflow Overview

```text
Step 1: PARSE_ARGS     -> validate --epic-id regex; exit 1 on fail
Step 2: VERIFY_BASE    -> git rev-parse --verify; capture BASE_SHA; exit 2 on fail
Step 3: COMPUTE_NAME   -> BRANCH_NAME = "epic/${EPIC_ID}"
Step 4: DETECT_LOCAL   -> git rev-parse --verify --quiet (ALREADY_EXISTED)
Step 5: DETECT_REMOTE  -> git ls-remote (skipped when --push=false)
Step 6: APPLY_STATE    -> State A (no-op) | State B (push only) | State C (delegate)
Step 7: EMIT_JSON      -> printf single-line response on stdout
```

Each step's full bash and the State A/B/C decision matrix live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1–3** (§Workflow Step-by-Step): argument parsing with `case` loop, 4-digit regex validation, base SHA capture, branch name composition with hard-coded `epic/` prefix.
- **Step 4–5** (§Workflow Step-by-Step): local-then-remote detection ordering; `--push=false` short-circuits remote check; rationale for treating local as authoritative.
- **Step 6** (§Workflow Step-by-Step Step 6): 3-state matrix with mutually exclusive branches; State C delegation envelope translation to `x-create-git-branch`.
- **Step 7** (§Workflow Step-by-Step Step 7): single-line JSON `printf` format and rationale for O(1) parsing cost.
- **Worked examples 1–7** (§Worked Examples): every state + edge case (new epic, idempotent no-op, complementary push, custom base, missing base, malformed ID, local-only).

## Idempotency Contract

| State before | First call | Second call |
| :--- | :--- | :--- |
| Branch absent (local + remote) | `created=true, alreadyExisted=false, pushedNow=true` | `created=false, alreadyExisted=true, pushedNow=false` |
| Branch local, not remote | `created=false, alreadyExisted=true, pushedNow=true` | `created=false, alreadyExisted=true, pushedNow=false` |
| Branch local + remote | `created=false, alreadyExisted=true, pushedNow=false` | `created=false, alreadyExisted=true, pushedNow=false` |

Two concurrent invocations serialize on git's `.git/index.lock`. No `flock` required.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| `--epic-id` missing or fails regex | Exit 1 (`INVALID_EPIC_ID`); stderr carries the offending value. |
| `--base` absent locally | Exit 2 (`BASE_NOT_FOUND`); suggest `git fetch origin`. |
| `x-create-git-branch` delegation fails (State C) | Exit 3 (`DELEGATION_FAILED`); stderr carries child stderr verbatim. |
| `git push` fails in State B | Exit 4 (`PUSH_FAILED`); stderr carries `git push` stderr verbatim. |
| `git ls-remote` times out or auth fails | Exit 4 (`PUSH_FAILED`); remote-check failure treated as push-side failure. |
| Unknown flag | Exit 64 (sysexits EX_USAGE); print `usage:` banner. |
| `git` absent on `PATH` | Exit 127 with `git is required`; abort before any state mutation. |

## Telemetry

Internal skills do NOT emit `phase.start` / `phase.end` markers — telemetry is produced by the invoking orchestrator. Passive hooks still capture `tool.call` for the underlying `Bash` invocation.

## Rule References

- **RULE-001** (EPIC-0049) — Branch única por épico (`epic/<ID>`). Canonical enforcement point.
- **RULE-005** (EPIC-0049) — Thin orchestrator (UseCase pattern).
- **RULE-006** (EPIC-0049) — `x-internal-*` convention.
- **Rule 09** (Branching Model) — `epic/<ID>` canonical prefix.
- **Rule 13** (Skill Invocation Protocol) — INLINE-SKILL pattern.
- **Rule 22** (Lifecycle Integrity Audit) — validates the 6 convention anchors.
- **ADR-0010** — exempts internal skills from the 3-option gate menu.

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-create-git-branch` | delegate (State C) | The only skill this one calls. |
| `x-epic-create` | caller | Step 0 before writing epic metadata. |
| `x-epic-decompose` | caller | Step 0 before writing stories / IMPLEMENTATION-MAP. |
| `x-orchestrate-epic` | caller | Step 0 before per-story planning loop. |
| `x-implement-epic` | caller | Step 0 before Phase 0 preparation. |
| `x-epic-map` | caller | Step 0 before IMPLEMENTATION-MAP refresh. |
| `x-internal-update-status` | peer (internal/ops/) | Independent — mutates execution-state. |
| `x-internal-normalize-args` | peer (internal/ops/) | Future refactor candidate for flag parsing. |

## Full Protocol

Minimum viable contract above. Detailed step-by-step bash (all 7 steps with full commands), 7 worked examples (all states + boundaries), performance budgets per state, idempotency matrix, acceptance test scenarios, and generator filter contract live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
