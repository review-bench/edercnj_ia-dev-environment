#!/usr/bin/env bash
#
# audit-epic-branches.sh — Rule 21 (Epic Branch Model) CI audit.
#
# Verifies that:
#   (a) Every open PR targeting develop whose head is epic/* has flowVersion="2"
#       in its execution-state.json. ABSENT flowVersion is treated as a
#       violation when the state file exists (Rule 21 requires explicit "2").
#   (b) No epic/* remote branch contains a "force-push marker" (HEAD diverged
#       from upstream by a non-fast-forward — best-effort check via reflog
#       comparison; full check requires reflog access to origin).
#   (c) Cleanup configuration excludes epic/* (any tooling that prunes branches
#       must whitelist epic/* — checked via grep over scripts/setup-hooks.sh).
#
# Exit codes:
#   0   All checks PASS.
#   1   EPIC_BRANCH_VIOLATION: at least one check failed.
#   2   OPERATIONAL_ERROR: gh CLI absent / git error / dependency missing.
#   3   BASELINE_CORRUPT: baseline file malformed.
#
# Flags:
#   --self-check  Validate script integrity (deps, files). Exit 0 OK / 2 broken.
#   --skip-pr-check  Skip Check A when gh CLI is unavailable (e.g., local dev).
#                    By default, missing gh exits 2.
#   -h|--help     Print usage and exit 0.
#
# Introduced by story-0058-0004 (EPIC-0058). See Rule 21 at
# .claude/rules/21-epic-branch-model.md for the contract.
#
# Catalogado em: docs/audit-gates-catalog.md

set -euo pipefail

SCRIPT_VERSION="1.1.0"
SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
PLANS_DIR="${REPO_ROOT}/plans"
SETUP_HOOKS_FILE="${REPO_ROOT}/scripts/setup-hooks.sh"

usage() {
  cat <<-EOF
Usage: ${SCRIPT_NAME} [--self-check] [--skip-pr-check] [-h|--help]

  Audit epic/* branch governance compliance (Rule 21).

  Checks:
    A  Every open epic/* → develop PR has flowVersion="2".
       (When state file is missing flowVersion: violation.)
    B  No epic/* remote branch has divergent reflog vs upstream
       (best-effort force-push detection).
    C  Cleanup tooling (scripts/setup-hooks.sh) excludes epic/*.

  Exit codes:
    0  All checks PASS.
    1  EPIC_BRANCH_VIOLATION detected.
    2  OPERATIONAL_ERROR (gh CLI absent without --skip-pr-check, git error).
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
SKIP_PR_CHECK=0
for arg in "$@"; do
  case "$arg" in
    --self-check)    self_check ;;
    --skip-pr-check) SKIP_PR_CHECK=1 ;;
    -h|--help)       usage; exit 0 ;;
    *) echo "${SCRIPT_NAME}: INVALID_ARGS: unknown flag: $arg" >&2; exit 2 ;;
  esac
done

violations=0

# ---------------------------------------------------------------------------
# Check A — flowVersion="2" on open epic/* PRs targeting develop
# ---------------------------------------------------------------------------
if command -v gh &>/dev/null; then
  pr_list_output=$(gh pr list --base develop --json number,headRefName \
                   --jq '.[] | select(.headRefName | startswith("epic/")) | .number' 2>&1) || {
    echo "${SCRIPT_NAME}: OPERATIONAL_ERROR: gh pr list failed: ${pr_list_output}" >&2
    exit 2
  }

  while IFS= read -r pr_number; do
    [[ -z "$pr_number" ]] && continue
    epic_id=$(gh pr view "$pr_number" --json headRefName \
              --jq '.headRefName | match("epic/([0-9]+)").captures[0].string' 2>&1) || {
      echo "${SCRIPT_NAME}: OPERATIONAL_ERROR: gh pr view #${pr_number} failed: ${epic_id}" >&2
      exit 2
    }
    [[ -z "$epic_id" ]] && continue

    state_file="${PLANS_DIR}/epic-${epic_id}/execution-state.json"
    if [[ -f "$state_file" ]]; then
      flow=$(jq -r '.flowVersion // "ABSENT"' "$state_file" 2>&1) || {
        echo "${SCRIPT_NAME}: OPERATIONAL_ERROR: failed to parse ${state_file}: ${flow}" >&2
        exit 2
      }
      if [[ "$flow" != "2" ]]; then
        echo "${SCRIPT_NAME}: EPIC_BRANCH_VIOLATION: PR #${pr_number} (epic/${epic_id}) has flowVersion=\"${flow}\" (Rule 21 requires \"2\")" >&2
        violations=$((violations + 1))
      fi
    fi
  done <<< "${pr_list_output}"
elif [[ $SKIP_PR_CHECK -eq 1 ]]; then
  echo "${SCRIPT_NAME}: INFO: gh CLI absent and --skip-pr-check set; Check A skipped"
else
  echo "${SCRIPT_NAME}: OPERATIONAL_ERROR: gh CLI not available (use --skip-pr-check to bypass)" >&2
  exit 2
fi

# ---------------------------------------------------------------------------
# Check B — local epic/* branches: best-effort force-push detection
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
# Check C — cleanup tooling excludes epic/*
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

echo "${SCRIPT_NAME}: checked PRs + local epic/* branches + cleanup config; violations: ${violations}"
[[ $violations -eq 0 ]] && exit 0 || exit 1
