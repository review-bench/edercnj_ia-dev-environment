---
name: x-internal-precheck-worktree
description: "Classifies the git working tree state (CLEAN/DIRTY/DIVERGENT/AMBIGUOUS) and returns a stable exit code. Invoked by orchestrators before starting story/task execution to detect dirty or divergent states early. Exit 0 = CLEAN or DIRTY; exit 15 (WORKTREE_AMBIGUOUS) = DIVERGENT or AMBIGUOUS (unless --allow-dirty)."
visibility: internal
user-invocable: false
model: haiku
allowed-tools: [Bash]
argument-hint: "[--allow-dirty] [--repo-path <path>]"
context-budget: light
requires-capabilities: []
---

> 🔒 **INTERNAL SKILL** — Invoked only by other skills via the Skill tool. Not user-invocable.

## Purpose

Runs `git status --porcelain` and `git rev-list --count --left-right @{upstream}...HEAD`
to classify the working tree, returning one of 4 states:

| State | Dirty files? | Upstream divergence? | Exit code |
| :--- | :--- | :--- | :--- |
| `CLEAN` | No | No | 0 |
| `DIRTY` | Yes | No | 0 (warning emitted) |
| `DIVERGENT` | No | Yes | **15** (`WORKTREE_AMBIGUOUS`) |
| `AMBIGUOUS` | Yes | Yes | **15** (`WORKTREE_AMBIGUOUS`) |

Exit code 15 is the stable contract for orchestrators to dispatch on (RULE-010, EPIC-0061).

## Usage

    Skill(skill: "x-internal-precheck-worktree", args: "--repo-path <path>")
    Skill(skill: "x-internal-precheck-worktree", args: "--allow-dirty --repo-path <path>")

## Parameters

| Flag | Default | Description |
| :--- | :--- | :--- |
| `--allow-dirty` | `false` | When set, DIRTY/AMBIGUOUS/DIVERGENT return exit 0 with a WARNING rather than exit 15. Used in recovery contexts. |
| `--repo-path` | `.` (cwd) | Path to the git repository root. Default: current working directory. |

## Output

Emits to stdout a single-line JSON envelope:

```json
{ "state": "CLEAN|DIRTY|DIVERGENT|AMBIGUOUS", "dirtyCount": 0, "ahead": 0, "behind": 0 }
```

Stderr carries a human-readable summary for operator visibility.

## Exit Codes

| Exit | Code | Meaning |
| :--- | :--- | :--- |
| 0 | `OK` | Working tree is CLEAN or DIRTY (non-blocking). Orchestrator may proceed. |
| 15 | `WORKTREE_AMBIGUOUS` | Working tree is DIVERGENT or AMBIGUOUS. Orchestrator MUST block unless `--allow-dirty`. |
| 2 | `OPERATIONAL_ERROR` | `git` not found on PATH or `--repo-path` is not a git repository. |

## Telemetry

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-internal-precheck-worktree worktree-precheck`

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-internal-precheck-worktree worktree-precheck ok`

## Implementation

```bash
#!/usr/bin/env bash
set -euo pipefail

ALLOW_DIRTY=false
REPO_PATH="."

while [[ $# -gt 0 ]]; do
  case "$1" in
    --allow-dirty) ALLOW_DIRTY=true; shift ;;
    --repo-path)   REPO_PATH="$2"; shift 2 ;;
    *)             echo "OPERATIONAL_ERROR: unknown arg $1" >&2; exit 2 ;;
  esac
done

command -v git >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: git not found on PATH" >&2; exit 2; }
[[ -d "$REPO_PATH/.git" ]] || git -C "$REPO_PATH" rev-parse --git-dir >/dev/null 2>&1 || \
  { echo "OPERATIONAL_ERROR: $REPO_PATH is not a git repository" >&2; exit 2; }

STATUS=$(git -C "$REPO_PATH" status --porcelain 2>/dev/null || true)
UPSTREAM=$(git -C "$REPO_PATH" rev-list --count --left-right "@{upstream}...HEAD" 2>/dev/null || true)

DIRTY_COUNT=$(echo "$STATUS" | grep -c '.' || true)
BEHIND=$(echo "$UPSTREAM" | awk '{print $1}' || echo "0")
AHEAD=$(echo "$UPSTREAM" | awk '{print $2}' || echo "0")
[[ -z "$STATUS" ]] && DIRTY_COUNT=0

IS_DIRTY=$([[ "$DIRTY_COUNT" -gt 0 ]] && echo true || echo false)
IS_DIVERGENT=$([[ -n "$UPSTREAM" ]] && echo true || echo false)

if $IS_DIRTY && $IS_DIVERGENT; then STATE="AMBIGUOUS"
elif $IS_DIVERGENT;             then STATE="DIVERGENT"
elif $IS_DIRTY;                 then STATE="DIRTY"
else                                 STATE="CLEAN"
fi

echo "{ \"state\": \"$STATE\", \"dirtyCount\": $DIRTY_COUNT, \"ahead\": $AHEAD, \"behind\": $BEHIND }"

if [[ "$STATE" == "DIRTY" ]]; then
  echo "WARN [worktree-precheck] working tree has $DIRTY_COUNT uncommitted file(s) — proceeding (not divergent)" >&2
fi

if [[ "$STATE" == "DIVERGENT" || "$STATE" == "AMBIGUOUS" ]]; then
  if $ALLOW_DIRTY; then
    echo "WARN [worktree-precheck] $STATE state — allowed by --allow-dirty" >&2
    exit 0
  fi
  echo "WORKTREE_AMBIGUOUS: $STATE — use --allow-dirty to bypass or resolve with git stash/pull" >&2
  exit 15
fi
exit 0
```

## Integration Notes

- Invoked by: `x-implement-story` (Phase 0.3 worktree-first branch decision), `x-implement-task` (Step 0), `x-implement-epic` (Phase 2 pre-guard).
- Dispatching on exit 15 (`WORKTREE_AMBIGUOUS`) is the canonical non-interactive response; orchestrators MUST NOT call `AskUserQuestion` in response — they emit `WORKTREE_AMBIGUOUS` to the operator and halt.
- Java implementation: `dev.iadev.cli.skill.WorktreePrecheck` (EPIC-0061, TASK-0061-0001-001) provides the same classification logic for generator-internal use.
