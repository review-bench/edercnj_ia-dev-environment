---
name: x-merge-branches
description: "Merges source into target locally with strategy, conflict rollback, and idempotent no-op."
user-invocable: true
allowed-tools: Bash, Read
argument-hint: "--source <branch> --target <branch> [--strategy merge|squash|rebase] [--message <msg>] [--no-push]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Local Git Merge with Conflict Rollback (slim — ADR-0012)

## Purpose

Single, idempotent entry point for merging one local branch into another with three strategies (`merge` / `squash` / `rebase`), automatic conflict detection and rollback, and a structured result payload. Replaces ~120 lines of inline Bash in `x-implement-epic` Phase 1.4e (auto-rebase between parallel stories + `develop → epic/XXXX` sync). Callers receive either `{mergeSha, conflicts:false}` or `{conflicts:true, rolledBack:true, conflictedFiles:[...]}` — never a half-merged working tree.

## Triggers

- `/x-merge-branches --source develop --target epic/0049` — default `merge` strategy, push after success
- `/x-merge-branches --source develop --target epic/0049 --strategy squash --message "sync develop"` — squash with custom message
- `/x-merge-branches --source feat/foo --target develop --strategy rebase` — linear history via rebase
- `/x-merge-branches --source develop --target epic/0049 --no-push` — merge locally only

## Parameters

| Argument | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `--source` | string | Yes | — | Source branch to merge from. Must exist locally. |
| `--target` | string | Yes | — | Target branch that receives the merge. Must exist locally. |
| `--strategy` | enum | No | `merge` | One of `merge` (default, `git merge --no-ff`), `squash` (`git merge --squash` + commit), `rebase` (`git rebase`). |
| `--message` | string | No | auto | Commit message for `merge` / `squash`. Ignored for `rebase`. Max 255 chars. |
| `--no-push` | boolean | No | `false` | Skip `git push origin <target>` after a successful merge. |

## Output Contract

Single-line JSON to stdout:

| Field | Type | Description |
|-------|------|-------------|
| `mergeSha` | string(40) \| null | SHA of resulting merge/squash/rebase-HEAD commit; `null` on conflict or no-op |
| `conflicts` | boolean | `true` when any conflict was detected during the merge attempt |
| `conflictedFiles` | string[] | List of unmerged paths captured before rollback; `[]` on success / no-op |
| `rolledBack` | boolean | `true` when `git merge --abort` / `git rebase --abort` executed successfully |
| `noOp` | boolean | `true` when target already contained source HEAD (ancestor check) |

## Exit Codes

| Exit | Code | Condition |
|------|------|-----------|
| 0 | SUCCESS / NO_OP | Merge committed (or target already contains source) |
| 1 | `WORKING_TREE_DIRTY` / `INVALID_STRATEGY` / missing required flag | Argument or precondition error |
| 2 | `SOURCE_NOT_FOUND` | Source branch missing locally |
| 3 | `TARGET_NOT_FOUND` | Target branch missing locally |
| 10 | `MERGE_CONFLICT_ROLLED_BACK` | Conflict detected, rollback succeeded |
| 11 | `ROLLBACK_FAILED` | `git merge --abort` / `git rebase --abort` failed; manual cleanup needed |

## Workflow Overview

```text
1. PARSE_FLAGS      -> extract --source/--target/--strategy/--message/--no-push; validate enum
2. PRE_CHECKS       -> git status --porcelain clean; rev-parse source + target exist
3. CHECKOUT_TARGET  -> git checkout <target>
4. IDEMPOTENCY      -> git merge-base --is-ancestor → emit noOp envelope + exit 0
5. ATTEMPT_MERGE    -> dispatch on strategy: merge --no-ff / squash + commit / rebase
6. DECIDE           -> success → capture mergeSha; conflict → git diff --diff-filter=U + abort
7. PUSH (optional)  -> git push origin <target> unless --no-push (WARN on failure, not abort)
8. EMIT_RESULT      -> single-line JSON envelope
```

Detailed bash per step, per-strategy invocation, conflict-capture awk pipeline, and rollback semantics in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): `case` flag parsing; enum validation for `--strategy`.
- **Step 2** (§Step 2): clean-working-tree guard (RULE-004 — preserve history); source/target existence via `git rev-parse --verify --quiet`.
- **Step 3** (§Step 3): bare `git checkout`.
- **Step 4** (§Step 4): `git merge-base --is-ancestor` ancestor check; noOp envelope emission.
- **Step 5** (§Step 5): three-way dispatch (`merge --no-ff --no-edit`, `merge --squash` + `commit`, `rebase`); per-strategy conflict flag setting.
- **Step 6** (§Step 6): `git diff --diff-filter=U` to capture unmerged paths BEFORE abort; `awk` JSON-array formatting; `git merge --abort` or `git rebase --abort` per strategy; exit 11 on rollback failure.
- **Step 7** (§Step 7): conditional push; WARN-only on push failure (local merge preserved).
- **Step 8** (§Step 8): `printf` of single-line JSON envelope with success fields.
- **Worked Examples** (§Worked Examples): 5 invocations covering happy path, no-op, conflict, squash with message, rebase no-push.

## Error Handling

| Scenario | Action |
|----------|--------|
| `--source` or `--target` missing | exit 1; print usage hint |
| `--strategy` invalid | exit 1 `INVALID_STRATEGY`; print accepted values |
| Dirty working tree | exit 1 `WORKING_TREE_DIRTY`; no side effects |
| Source/target branch missing | exit 2 / 3 with suggestion `git fetch origin` |
| Target already contains source HEAD | exit 0 with `noOp:true`; no commit, no push |
| Conflict during merge/squash/rebase | capture `conflictedFiles`, abort, exit 10 `MERGE_CONFLICT_ROLLED_BACK` |
| `git merge --abort` / `git rebase --abort` fails | exit 11 `ROLLBACK_FAILED`; stderr carried verbatim; manual cleanup |
| `git push` fails | WARN only; local merge is preserved; caller can retry push |

## Integration Notes

| Skill | Relationship | Context |
|-------|--------------|---------|
| `x-implement-epic` | caller (future refactor — story-0049-0018) | Phase 1.4e auto-rebase between parallel stories; `develop → epic/XXXX` sync |
| `x-implement-story` | caller (future refactor — story-0049-0019) | Optional auto-sync of story branch with parent epic branch |
| `x-internal-ensure-epic-branch` | related (story-0049-0008) | Ensures `epic/XXXX` exists before this skill merges `develop` into it |
| `x-merge-pr` | sibling (story-0049-0003) | Remote PR merge via `gh pr merge`; this skill handles local-only merges |
| `x-create-git-branch` | sibling (story-0049-0001) | Bare branch creation; this skill assumes both branches exist |

## Full Protocol

Minimum viable contract above. Detailed bash for all 8 steps (flag parsing, pre-checks, idempotency check, per-strategy dispatch, conflict-capture with `git diff --diff-filter=U` + `awk` JSON formatting, rollback semantics, optional push, JSON emission), worked examples, and EPIC-0049 story-local rule references live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
