#!/usr/bin/env bash
#
# audit-epic-branches.sh — Rule 21 (Epic Branch Model) CI audit.
#
# Verifies that:
#   (a) No epic/* remote branch contains a "force-push marker" (HEAD diverged
#       from upstream by a non-fast-forward — best-effort check via reflog
#       comparison; full check requires reflog access to origin).
#   (b) Cleanup configuration excludes epic/* (any tooling that prunes branches
#       must whitelist epic/* — checked via grep over scripts/setup-hooks.sh).
#   (c) docs/<epic-id>-<slug> branches (EPIC-0065): validate that the corresponding
#       epic/XXXX branch exists. docs/feature-<slug> branches are ideation-only and
#       are NOT checked for epic/ correlation. docs/* branches are NOT violations
#       for any check that expects epic/* format.
#
# Exit codes:
#   0   All checks PASS.
#   1   EPIC_BRANCH_VIOLATION: at least one check failed.
#   2   OPERATIONAL_ERROR: gh CLI absent / git error / dependency missing.
#   3   BASELINE_CORRUPT: baseline file malformed.
#
# Flags:
#   --self-check  Validate script integrity (deps, files). Exit 0 OK / 2 broken.
#   -h|--help     Print usage and exit 0.
#
# Introduced by story-0058-0004 (EPIC-0058). See Rule 21 at
# .claude/rules/21-epic-branch-model.md for the contract.
# Extended by story-0065-0001 (EPIC-0065): adds Check C for docs/ branch type.
#
# Catalogado em: docs/audit-gates-catalog.md

set -euo pipefail

SCRIPT_VERSION="1.3.0"
SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
SETUP_HOOKS_FILE="${REPO_ROOT}/scripts/setup-hooks.sh"

usage() {
  cat <<-EOF
Usage: ${SCRIPT_NAME} [--self-check] [-h|--help]

  Audit epic/* branch governance compliance (Rule 21).

  Checks:
    A  No epic/* remote branch has divergent reflog vs upstream
       (best-effort force-push detection).
    B  Cleanup tooling (scripts/setup-hooks.sh) excludes epic/*.
    C  docs/<epic-id>-<slug> branches have a corresponding epic/XXXX branch.

  Exit codes:
    0  All checks PASS.
    1  EPIC_BRANCH_VIOLATION detected.
    2  OPERATIONAL_ERROR (git error).
    3  BASELINE_CORRUPT.
EOF
}

self_check() {
  local ok=1
  if ! command -v git &>/dev/null; then
    echo "${SCRIPT_NAME}: DEPENDENCY_MISSING: git not found" >&2; ok=0
  fi
  if ! command -v jq &>/dev/null; then
    echo "${SCRIPT_NAME}: DEPENDENCY_MISSING: jq not found" >&2; ok=0
  fi
  if [[ $ok -eq 0 ]]; then exit 2; fi
  echo "${SCRIPT_NAME}: OK (version ${SCRIPT_VERSION}, deps ok)"
  exit 0
}

# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------
for arg in "$@"; do
  case "$arg" in
    --self-check) self_check ;;
    -h|--help)    usage; exit 0 ;;
    *) echo "${SCRIPT_NAME}: INVALID_ARGS: unknown flag: $arg" >&2; exit 2 ;;
  esac
done

violations=0

# ---------------------------------------------------------------------------
# Check A — local epic/* branches: best-effort force-push detection
# ---------------------------------------------------------------------------
# Force-pushes rewrite history. A robust check requires origin reflog access,
# unavailable to clients. Best-effort: check that local epic/* branch HEAD
# is reachable from origin/<branch> (i.e., upstream is ancestor of local).
# A divergence indicates either an unpushed local rewrite or an upstream
# force-push that we should flag.
while IFS= read -r ref; do
  [[ -z "$ref" ]] && continue
  branch="${ref#origin/}"
  if [[ "$branch" =~ ^epic/[0-9]+$ ]]; then
    if git rev-parse --verify "$branch" &>/dev/null; then
      local_sha=$(git rev-parse "$branch")
      origin_sha=$(git rev-parse "origin/$branch")
      if [[ "$local_sha" != "$origin_sha" ]]; then
        # Check ancestry: if origin is NOT an ancestor of local, this is a divergence.
        if ! git merge-base --is-ancestor "origin/$branch" "$branch" 2>/dev/null; then
          echo "${SCRIPT_NAME}: EPIC_BRANCH_VIOLATION: ${branch} diverges from origin (possible force-push or local rewrite)" >&2
          violations=$((violations + 1))
        fi
      fi
    fi
  fi
done < <(git for-each-ref --format='%(refname:short)' refs/remotes/origin/ 2>/dev/null | grep "^origin/epic/" || true)

# ---------------------------------------------------------------------------
# Check B — cleanup tooling excludes epic/*
# ---------------------------------------------------------------------------
if [[ -f "$SETUP_HOOKS_FILE" ]]; then
  # If setup-hooks.sh deletes branches by pattern, it MUST whitelist epic/*.
  # We grep for any branch-delete invocation that doesn't have an exclusion.
  if grep -q "git branch -[Dd]\|git push.*--delete" "$SETUP_HOOKS_FILE" 2>/dev/null; then
    if ! grep -q "epic/" "$SETUP_HOOKS_FILE" 2>/dev/null; then
      echo "${SCRIPT_NAME}: EPIC_BRANCH_VIOLATION: ${SETUP_HOOKS_FILE} performs branch deletion but does not exclude epic/*" >&2
      violations=$((violations + 1))
    fi
  fi
fi

# ---------------------------------------------------------------------------
# Check C — docs/<epic-id>-<slug> branches: validate epic/XXXX exists (EPIC-0065)
# ---------------------------------------------------------------------------
while IFS= read -r ref; do
  [[ -z "$ref" ]] && continue
  branch="${ref#origin/}"
  # Match docs/<4-digit-epic-id>-<slug> (creation flow) but NOT docs/feature-<slug> (ideation flow)
  if [[ "$branch" =~ ^docs/([0-9]{4})-.+ ]]; then
    epic_id="${BASH_REMATCH[1]}"
    epic_branch="epic/${epic_id}"
    if ! git rev-parse --verify "origin/${epic_branch}" &>/dev/null; then
      echo "${SCRIPT_NAME}: EPIC_BRANCH_VIOLATION: ${branch} requires ${epic_branch} to exist on origin (Rule 21 §Anti-Patterns EPIC-0065 exception)" >&2
      violations=$((violations + 1))
    fi
  fi
done < <(git for-each-ref --format='%(refname:short)' refs/remotes/origin/ 2>/dev/null | grep "^origin/docs/" || true)

echo "${SCRIPT_NAME}: checked local epic/* branches + cleanup config + docs/ branches; violations: ${violations}"
[[ $violations -eq 0 ]] && exit 0 || exit 1
