# x-merge-branches — Full Protocol

Detailed reference for `x-merge-branches`. The SKILL.md body holds the minimum viable contract per ADR-0012; this document is the canonical procedural reference.

## Step 1 — Parse Flags

```bash
SOURCE=""; TARGET=""; STRATEGY="merge"; MESSAGE=""; NO_PUSH="false"
while [ $# -gt 0 ]; do
  case "$1" in
    --source)   SOURCE="$2";   shift 2 ;;
    --target)   TARGET="$2";   shift 2 ;;
    --strategy) STRATEGY="$2"; shift 2 ;;
    --message)  MESSAGE="$2";  shift 2 ;;
    --no-push)  NO_PUSH="true"; shift ;;
    *) echo "ERROR: unknown flag: $1" >&2; exit 1 ;;
  esac
done
[ -z "$SOURCE" ] && { echo "ERROR: --source is required" >&2; exit 1; }
[ -z "$TARGET" ] && { echo "ERROR: --target is required" >&2; exit 1; }
case "$STRATEGY" in
  merge|squash|rebase) ;;
  *) echo "ERROR: INVALID_STRATEGY — must be merge|squash|rebase" >&2; exit 1 ;;
esac
```

## Step 2 — Pre-Checks

```bash
# Working tree must be clean (RULE-004 preserves history; dirty tree risks loss)
if [ -n "$(git status --porcelain)" ]; then
  echo "ERROR: WORKING_TREE_DIRTY — Working tree must be clean before merge" >&2
  exit 1
fi

# Source branch must exist locally
if ! git rev-parse --verify --quiet "$SOURCE" >/dev/null; then
  echo "ERROR: SOURCE_NOT_FOUND — Source branch '$SOURCE' not found" >&2
  exit 2
fi

# Target branch must exist locally
if ! git rev-parse --verify --quiet "$TARGET" >/dev/null; then
  echo "ERROR: TARGET_NOT_FOUND — Target branch '$TARGET' not found" >&2
  exit 3
fi
```

## Step 3 — Checkout Target

```bash
git checkout "$TARGET"
```

## Step 4 — Idempotency (No-Op Check)

```bash
# If target already contains source HEAD, nothing to merge
if git merge-base --is-ancestor "$SOURCE" "$TARGET"; then
  printf '{"mergeSha":null,"conflicts":false,"conflictedFiles":[],"rolledBack":false,"noOp":true}\n'
  exit 0
fi
```

## Step 5 — Attempt Merge (Dispatch on Strategy)

```bash
CONFLICTS="false"
case "$STRATEGY" in
  merge)
    if [ -n "$MESSAGE" ]; then
      git merge --no-ff --no-edit -m "$MESSAGE" "$SOURCE" 2>/tmp/merge.err || CONFLICTS="true"
    else
      git merge --no-ff --no-edit "$SOURCE" 2>/tmp/merge.err || CONFLICTS="true"
    fi
    ;;
  squash)
    if ! git merge --squash "$SOURCE" 2>/tmp/merge.err; then
      CONFLICTS="true"
    else
      MSG="${MESSAGE:-squash: merge $SOURCE into $TARGET}"
      git commit -m "$MSG" 2>/tmp/merge.err || CONFLICTS="true"
    fi
    ;;
  rebase)
    git rebase "$SOURCE" 2>/tmp/merge.err || CONFLICTS="true"
    ;;
esac
```

## Step 6 — Decide (Success or Conflict Rollback)

```bash
if [ "$CONFLICTS" = "true" ]; then
  # Capture unmerged paths BEFORE aborting (diff-filter=U)
  FILES=$(git diff --name-only --diff-filter=U 2>/dev/null | tr '\n' ' ' | sed 's/ $//')
  CONFLICTED_FILES=$(printf '%s' "$FILES" | awk 'BEGIN{printf "["} \
    {for(i=1;i<=NF;i++){if(i>1)printf ",";printf "\"%s\"",$i}} END{print "]"}')

  # Abort the partial operation
  ABORT_CMD="git merge --abort"
  [ "$STRATEGY" = "rebase" ] && ABORT_CMD="git rebase --abort"
  if ! $ABORT_CMD 2>/tmp/abort.err; then
    echo "ERROR: ROLLBACK_FAILED — $ABORT_CMD failed; manual cleanup needed" >&2
    cat /tmp/abort.err >&2
    exit 11
  fi
  printf '{"mergeSha":null,"conflicts":true,"conflictedFiles":%s,"rolledBack":true,"noOp":false}\n' \
    "$CONFLICTED_FILES"
  exit 10
fi

MERGE_SHA=$(git rev-parse HEAD)
```

## Step 7 — Optional Push

```bash
if [ "$NO_PUSH" != "true" ]; then
  git push origin "$TARGET" || {
    echo "WARN: push failed; merge is committed locally but not pushed" >&2
  }
fi
```

## Step 8 — Emit Structured Result

```bash
printf '{"mergeSha":"%s","conflicts":false,"conflictedFiles":[],"rolledBack":false,"noOp":false}\n' \
  "$MERGE_SHA"
```

## Worked Examples

```
# Happy path — merge develop into epic/0049, push after
/x-merge-branches --source develop --target epic/0049
# -> {"mergeSha":"abc123...","conflicts":false,...,"noOp":false}

# No-op — target already contains source HEAD
/x-merge-branches --source develop --target epic/0049
# -> {"mergeSha":null,...,"noOp":true}

# Conflict with automatic rollback (exit 10)
/x-merge-branches --source develop --target epic/0049
# -> {"mergeSha":null,"conflicts":true,"conflictedFiles":["a.md"],"rolledBack":true,...}

# Squash with custom commit message
/x-merge-branches --source feat/foo --target develop --strategy squash --message "feat(foo): batch"

# Rebase feature branch onto develop (linear history, no push)
/x-merge-branches --source develop --target feat/foo --strategy rebase --no-push
```

## Rule References

- **Rule 09** (Branching Model) — merge-direction rules and target-branch conventions.
- **RULE-004** (EPIC-0049 — Estratégia de merge: preserva history) — default strategy `merge` with `--no-ff` preserves per-task TDD commits for bisect.
- **RULE-005** (EPIC-0049 — Thin orchestrator) — `x-implement-epic` and `x-implement-story` delegate local merges here; no inline `git merge` blocks.
- **RULE-010** (EPIC-0049 — Skills internas pequenas) — this SKILL.md stays under 250 lines.

(These RULE-NNN refs are EPIC-0049 story-local invariants — not the project-wide Rules in `.claude/rules/`.)
