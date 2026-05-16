# x-cleanup-git-branches — Full Protocol

Detailed reference for `x-cleanup-git-branches`. The SKILL.md body holds the minimum viable contract per ADR-0012; this document is the canonical procedural reference.

## Step 1 — Parse Flags

```bash
DRY_RUN=false
ASSUME_YES=false
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=true ;;
    --yes|-y)  ASSUME_YES=true ;;
    -h|--help) echo "Usage: x-cleanup-git-branches [--dry-run] [--yes]"; exit 0 ;;
    *) echo "ERROR: unknown flag: $arg" >&2; exit 2 ;;
  esac
done

if [ "$DRY_RUN" = "true" ] && [ "$ASSUME_YES" = "true" ]; then
  echo "ERROR: --dry-run and --yes are mutually exclusive" >&2
  exit 2
fi
```

## Step 2 — Detect Worktree Context (abort if inside one)

This skill MUST run from the main repository. Running from inside a worktree would attempt to remove the host worktree while executing — unsafe.

This skill extends the canonical `detect_worktree_context()` check from `x-manage-worktrees` (Rule 14, non-nesting invariant). The canonical snippet only recognises worktrees under `.claude/worktrees/*`; because this skill enumerates and removes **all** non-main worktrees via `git worktree list --porcelain` (any path), the guard also compares `git rev-parse --show-toplevel` to the main worktree path and inspects `git rev-parse --git-dir` for a `worktrees/` suffix, so a linked worktree in any location triggers the abort.

```bash
detect_worktree_context() {
  local toplevel git_dir main_repo wt_path in_wt="false"
  toplevel=$(git rev-parse --show-toplevel 2>/dev/null) || {
    echo '{"error":"NOT_A_REPO"}' >&2
    return 1
  }
  git_dir=$(git rev-parse --git-dir 2>/dev/null) || {
    echo '{"error":"NOT_A_REPO"}' >&2
    return 1
  }
  json_escape() {
    printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'
  }

  # Resolve main repo path (first `worktree` entry, stripping the
  # `worktree ` prefix so paths containing spaces are preserved).
  if ! main_repo=$(git worktree list --porcelain 2>/dev/null \
              | sed -n 's/^worktree //p' | head -n 1) \
       || [ -z "$main_repo" ]; then
    main_repo="$toplevel"
  fi

  # Classifier 1 — Rule 14 non-nesting invariant (substring check).
  if printf '%s' "$toplevel" | grep -q "/\.claude/worktrees/"; then
    in_wt="true"
  fi
  # Classifier 2 — git-dir of a linked worktree lives under
  # `<main>/.git/worktrees/<id>/`.
  case "$git_dir" in
    */worktrees/*|.git/worktrees/*) in_wt="true" ;;
  esac
  # Classifier 3 — toplevel differs from the main repo path.
  if [ "$toplevel" != "$main_repo" ]; then
    in_wt="true"
  fi

  if [ "$in_wt" = "true" ]; then
    wt_path=$(json_escape "$toplevel")
    main_repo=$(json_escape "$main_repo")
    printf '{"inWorktree":%s,"worktreePath":"%s","mainRepoPath":"%s"}\n' \
      "$in_wt" "$wt_path" "$main_repo"
  else
    main_repo=$(json_escape "$main_repo")
    printf '{"inWorktree":%s,"worktreePath":null,"mainRepoPath":"%s"}\n' \
      "$in_wt" "$main_repo"
  fi
}

CONTEXT_JSON=$(detect_worktree_context) || exit 1
IN_WT=$(printf '%s' "$CONTEXT_JSON" | grep -o '"inWorktree":[^,]*' | cut -d: -f2)

if [ "$IN_WT" = "true" ]; then
  echo "ERROR: IN_WORKTREE_UNSAFE — must run from main repo, not a worktree" >&2
  exit 1
fi
```

## Step 3 — Resolve Current Branch

```bash
CURRENT_BRANCH=$(git symbolic-ref --short -q HEAD || true)
# Empty string => detached HEAD (safe; no switch needed later)
```

## Step 4 — Fetch With Prune

```bash
if git remote | grep -q '^origin$'; then
  echo "→ git fetch --prune origin"
  git fetch --prune origin || echo "WARNING: fetch failed, continuing"
else
  echo "WARNING: no 'origin' remote configured, skipping fetch"
fi
```

## Step 5 — Enumerate Non-Main Worktrees

The first `worktree <path>` entry in `git worktree list --porcelain` is always the main repository. All subsequent entries are removal candidates. Paths in that output can legally contain spaces, so extract them by stripping the literal `worktree ` prefix (nine characters) rather than by whitespace-splitting — `sed` preserves the full path.

```bash
WORKTREE_CANDIDATES=$(git worktree list --porcelain \
  | sed -n 's/^worktree //p' \
  | tail -n +2)
```

## Step 6 — Enumerate Candidate Branches

```bash
PROTECTED_REGEX='^(main|master|develop)$'
# Also preserve epic/* branches (Rule 21) and docs/* branches with open PRs (EPIC-0065 D-R6)
RAW_CANDIDATES=$(git for-each-ref --format='%(refname:short)' refs/heads/ \
  | grep -Ev "$PROTECTED_REGEX" || true)

# Filter out epic/* (always protected per Rule 21 §Anti-Patterns)
# Filter out docs/* branches that have an open PR (preserve until PR is merged/closed)
BRANCH_CANDIDATES=""
while IFS= read -r br; do
  [ -n "$br" ] || continue
  # epic/* branches: always skip (Rule 21)
  if [[ "$br" == epic/* ]]; then continue; fi
  # docs/* branches: skip if an open PR exists (gh CLI check)
  # When gh is unavailable, treat docs/* as protected by default (fail-safe — avoids deleting a branch with an open PR)
  if [[ "$br" == docs/* ]]; then
    if ! command -v gh &>/dev/null; then continue; fi
    open_prs=$(gh pr list --head "$br" --state open --json number --jq '. | length' 2>/dev/null || echo 0)
    [ "$open_prs" -gt 0 ] && continue
  fi
  BRANCH_CANDIDATES="${BRANCH_CANDIDATES}${br}"$'\n'
done <<< "$RAW_CANDIDATES"
```

`grep -Ev … || true` prevents a non-match (exit 1) from aborting the script under `set -e` style shells.

**Epic branch protection (Rule 21):** `epic/*` branches are NEVER deleted by this skill — they are protected until the manual epic-to-develop PR gate is merged.

**Docs branch protection (EPIC-0065):** `docs/*` branches with open PRs are preserved until the PR is merged or closed. Once merged, they become cleanup candidates on the next run.

## Step 7 — Print Plan

```bash
echo ""
echo "=== Cleanup Plan ==="
echo ""
echo "Worktrees to remove (main worktree preserved):"
if [ -z "$WORKTREE_CANDIDATES" ]; then
  echo "  (none)"
else
  while IFS= read -r wt; do
    [ -n "$wt" ] || continue
    printf '  - %s\n' "$wt"
  done <<EOF
$WORKTREE_CANDIDATES
EOF
fi

echo ""
echo "Local branches to delete (protected: main, master, develop):"
if [ -z "$BRANCH_CANDIDATES" ]; then
  echo "  (none)"
else
  while IFS= read -r br; do
    [ -n "$br" ] || continue
    printf '  - %s\n' "$br"
  done <<EOF
$BRANCH_CANDIDATES
EOF
fi

if [ -z "$WORKTREE_CANDIDATES" ] && [ -z "$BRANCH_CANDIDATES" ]; then
  echo ""
  echo "Nothing to clean. Exiting."
  exit 0
fi

if [ "$DRY_RUN" = "true" ]; then
  echo ""
  echo "Dry-run complete — no changes applied."
  exit 0
fi
```

## Step 8 — Confirmation Gate

```bash
if [ "$ASSUME_YES" != "true" ]; then
  echo ""
  read -r -p "Proceed with deletion? [y/N] " ANS
  case "$ANS" in
    y|Y|yes|YES) ;;
    *) echo "Aborted by user."; exit 0 ;;
  esac
fi
```

## Step 9 — Switch Away From a Candidate HEAD

If the current branch is about to be deleted, git refuses `branch -D`. Switch to `develop` (fallback `main`) first.

```bash
needs_switch=false
if [ -n "$CURRENT_BRANCH" ]; then
  while IFS= read -r b; do
    [ -n "$b" ] || continue
    if [ "$b" = "$CURRENT_BRANCH" ]; then
      needs_switch=true
      break
    fi
  done <<EOF
$BRANCH_CANDIDATES
EOF
fi

if [ "$needs_switch" = "true" ]; then
  if git show-ref --verify --quiet refs/heads/develop; then
    echo "→ git checkout develop (HEAD was on a candidate branch)"
    git checkout develop
  elif git show-ref --verify --quiet refs/heads/main; then
    echo "→ git checkout main (develop missing; falling back)"
    git checkout main
  else
    echo "ERROR: NO_SAFE_FALLBACK_BRANCH — neither develop nor main exists; cannot switch away from $CURRENT_BRANCH" >&2
    exit 1
  fi
fi
```

## Step 10 — Remove Worktrees

Iterate the candidate list with `while IFS= read -r` over a heredoc, so that worktree paths containing spaces (or glob metacharacters) are preserved as a single token. A plain `for` loop would word-split them.

```bash
WT_REMOVED=0
while IFS= read -r wt; do
  [ -n "$wt" ] || continue
  echo "→ git worktree remove --force $wt"
  if git worktree remove --force "$wt"; then
    WT_REMOVED=$((WT_REMOVED + 1))
  else
    echo "WARNING: failed to remove worktree: $wt" >&2
  fi
done <<EOF
$WORKTREE_CANDIDATES
EOF
git worktree prune
```

`--force` ensures worktrees with uncommitted changes are removed. This is intentional — the Print Plan step already showed them to the user.

## Step 11 — Delete Local Branches

```bash
BR_DELETED=0
while IFS= read -r br; do
  [ -n "$br" ] || continue
  echo "→ git branch -D $br"
  if git branch -D "$br"; then
    BR_DELETED=$((BR_DELETED + 1))
  else
    echo "WARNING: failed to delete branch: $br" >&2
  fi
done <<EOF
$BRANCH_CANDIDATES
EOF
```

## Step 12 — Report Summary

```bash
echo ""
echo "=== Summary ==="
echo "Worktrees removed: $WT_REMOVED"
echo "Branches deleted:  $BR_DELETED"
exit 0
```

## Security & Safety Notes

- **Blast radius is local-only:** `git fetch` reads from origin; no `push`, no `--force-push`, no tag/remote-branch mutation. Remote state is untouched.
- **Uncommitted work:** `git worktree remove --force` discards uncommitted changes inside secondary worktrees. This is surfaced in the Print Plan step and the user can decline at the confirmation gate.
- **Protected set is literal:** filters use exact regex `^(main|master|develop)$` — no substring matches, no accidental protection of `my-develop-fix`.
- **HEAD in main worktree is preserved implicitly:** `git worktree list` reports the main worktree first and the enumeration skips it.
