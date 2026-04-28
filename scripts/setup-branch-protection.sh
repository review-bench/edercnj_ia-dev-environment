#!/usr/bin/env bash
# setup-branch-protection.sh — Idempotent GitHub branch protection setup
#
# Configures required status checks and review requirements on develop and main
# using the canonical list from audits/required-checks.txt.
#
# Usage:
#   ./scripts/setup-branch-protection.sh [--dry-run] [--branches develop,main]
#
# Options:
#   --dry-run       Print the JSON payload without calling the GitHub API
#   --branches      Comma-separated list of branches to protect (default: develop,main)
#   --self-check    Verify script prerequisites and exit
#
# Exit codes:
#   0  Success
#   1  OPERATIONAL_ERROR (missing gh, jq, required-checks.txt, or API failure)
#   2  ARGS_INVALID
#
# Requires:
#   - gh CLI authenticated (gh auth status)
#   - jq on PATH
#   - audits/required-checks.txt present

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
# BASELINE_DIR: directory containing governance baseline files (story-0062-0001).
BASELINE_DIR="${BASELINE_DIR:-governance/baselines}"
REQUIRED_CHECKS_FILE="${REPO_ROOT}/${BASELINE_DIR}/required-checks.txt"

# --- argument parsing ---
DRY_RUN=false
BRANCHES="develop,main"
APPLY_STRICT=false  # EPIC-0063 story-0063-0008: Camada 4 strict mode

while [[ $# -gt 0 ]]; do
  case "${1:-}" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --branches)
      BRANCHES="${2:-}"
      shift 2
      ;;
    --apply-strict)
      APPLY_STRICT=true
      shift
      ;;
    --self-check)
      # Verify prerequisites
      if ! command -v gh >/dev/null 2>&1; then
        echo "OPERATIONAL_ERROR: gh CLI not found on PATH" >&2; exit 1
      fi
      if ! command -v jq >/dev/null 2>&1; then
        echo "OPERATIONAL_ERROR: jq not found on PATH" >&2; exit 1
      fi
      if [[ ! -f "${REQUIRED_CHECKS_FILE}" ]]; then
        echo "OPERATIONAL_ERROR: ${REQUIRED_CHECKS_FILE} not found" >&2; exit 1
      fi
      echo "self-check: OK"
      exit 0
      ;;
    *)
      echo "ARGS_INVALID: unknown argument '${1}'" >&2
      exit 2
      ;;
  esac
done

# --- prerequisites ---
if ! command -v gh >/dev/null 2>&1; then
  echo "OPERATIONAL_ERROR: gh CLI not found on PATH" >&2; exit 1
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "OPERATIONAL_ERROR: jq not found on PATH" >&2; exit 1
fi
if [[ ! -f "${REQUIRED_CHECKS_FILE}" ]]; then
  echo "OPERATIONAL_ERROR: ${REQUIRED_CHECKS_FILE} not found" >&2; exit 1
fi

# --- resolve repo (owner/repo) ---
REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner' 2>/dev/null || true)
if [[ -z "${REPO}" ]]; then
  echo "OPERATIONAL_ERROR: could not resolve repository via 'gh repo view'" >&2; exit 1
fi

# --- read required checks (strip comments and blank lines) ---
mapfile -t CHECKS < <(grep -v '^\s*#' "${REQUIRED_CHECKS_FILE}" | grep -v '^\s*$')
if [[ ${#CHECKS[@]} -eq 0 ]]; then
  echo "OPERATIONAL_ERROR: no checks found in ${REQUIRED_CHECKS_FILE}" >&2; exit 1
fi

# Build JSON array of check contexts
CONTEXTS_JSON=$(printf '%s\n' "${CHECKS[@]}" | jq -R . | jq -cs .)

# Build the full protection payload
# EPIC-0063 story-0063-0008: --apply-strict adds linear history, no force-push, no deletions
if [[ "${APPLY_STRICT}" == "true" ]]; then
  PROTECTION_PAYLOAD=$(jq -n \
    --argjson contexts "${CONTEXTS_JSON}" \
    '{
      required_status_checks: {
        strict: true,
        contexts: $contexts
      },
      enforce_admins: true,
      required_pull_request_reviews: {
        required_approving_review_count: 1,
        dismiss_stale_reviews: true
      },
      required_linear_history: true,
      allow_force_pushes: false,
      allow_deletions: false,
      restrictions: null
    }')
else
  PROTECTION_PAYLOAD=$(jq -n \
    --argjson contexts "${CONTEXTS_JSON}" \
    '{
      required_status_checks: {
        strict: true,
        contexts: $contexts
      },
      enforce_admins: true,
      required_pull_request_reviews: {
        required_approving_review_count: 1,
        dismiss_stale_reviews: true
      },
      restrictions: null
    }')
fi

echo "=== Branch Protection Setup ==="
echo "Repository : ${REPO}"
echo "Branches   : ${BRANCHES}"
echo "Checks     : ${#CHECKS[@]}"
printf '  - %s\n' "${CHECKS[@]}"
echo ""

if [[ "${DRY_RUN}" == "true" ]]; then
  echo "=== DRY RUN — Payload that would be sent ==="
  echo "${PROTECTION_PAYLOAD}" | jq .
  echo ""
  echo "=== DRY RUN complete. No API calls made. ==="
  exit 0
fi

# --- apply to each branch ---
IFS=',' read -ra BRANCH_LIST <<< "${BRANCHES}"
FAILED=0

for BRANCH in "${BRANCH_LIST[@]}"; do
  BRANCH="${BRANCH// /}"  # trim spaces
  echo "Configuring protection for branch: ${BRANCH}"

  HTTP_STATUS=$(gh api \
    "repos/${REPO}/branches/${BRANCH}/protection" \
    --method PUT \
    --input - \
    --silent \
    --include \
    <<< "${PROTECTION_PAYLOAD}" 2>&1 | grep -E '^HTTP/' | awk '{print $2}' || echo "0")

  # gh api returns non-zero on HTTP error, so check exit status
  if gh api \
    "repos/${REPO}/branches/${BRANCH}/protection" \
    --method PUT \
    --input - \
    --silent \
    <<< "${PROTECTION_PAYLOAD}" >/dev/null 2>&1; then
    echo "  [OK] Protection applied to ${BRANCH}"
  else
    echo "  [ERROR] Failed to apply protection to ${BRANCH}" >&2
    FAILED=$((FAILED + 1))
  fi
done

if [[ ${FAILED} -gt 0 ]]; then
  echo ""
  echo "OPERATIONAL_ERROR: ${FAILED} branch(es) failed to configure" >&2
  exit 1
fi

echo ""
echo "=== Branch protection setup complete ==="
echo "Verify with:"
echo "  gh api repos/${REPO}/branches/develop/protection | jq '.required_status_checks.contexts'"
exit 0
