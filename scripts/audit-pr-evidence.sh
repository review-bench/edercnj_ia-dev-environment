#!/usr/bin/env bash
# audit-pr-evidence.sh — story-0059-0007 (EPIC-0059).
#
# Validates that a PR body contains a filled-in ## Orchestrator Evidence
# section (as required by EPIC-0059, Rule 26, story-0059-0007).
#
# PRs created manually (without x-pr-create) will have placeholder values
# in the section (or the section will be absent entirely) and will be
# rejected by this script.
#
# Exit codes (RULE-059-06 / Rule 26 standardized):
#   0 — OK (section present, no placeholders, SHA valid, artifacts exist)
#   1 — PRE_EVIDENCE_MISSING (section absent or placeholders not substituted)
#   2 — PRE_BASELINE_CORRUPT (baseline of grandfathered PRs malformed)
#   3 — PRE_INVALID_EXEMPTION (audit-exempt marker missing a reason)
#   4 — PRE_ENFORCEMENT_BROKEN (--self-check failed: structural prerequisite missing)
#
# Usage:
#   scripts/audit-pr-evidence.sh --pr <PR_NUMBER>
#   scripts/audit-pr-evidence.sh --pr <PR_NUMBER> --repo <owner/repo>
#   scripts/audit-pr-evidence.sh --self-check
#   scripts/audit-pr-evidence.sh --help

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

BASELINE_FILE="audits/pr-evidence-baseline.txt"

# Placeholder patterns that indicate an unfilled section
PLACEHOLDER_STORY_ID="story-XXXX-YYYY"
PLACEHOLDER_SHA="abc123def456..."
PLACEHOLDER_SKILL="SKILL-NAME-HERE"

usage_error() {
    cat >&2 <<EOF
usage: $(basename "$0") --pr <PR_NUMBER> [--repo <owner/repo>]
                        [--self-check] [--help]

  --pr <PR_NUMBER>    GitHub PR number to validate
  --repo <owner/repo> GitHub repository (default: inferred from git remote)
  --self-check        Verify script structural prerequisites; exit 0 or 4
  --help              Show this banner

Validates that a PR body contains a filled ## Orchestrator Evidence
section. PRs created manually (without x-pr-create) will have
placeholder values and will be rejected (exit 1 PRE_EVIDENCE_MISSING).

Exit codes:
  0  OK — evidence present and valid
  1  PRE_EVIDENCE_MISSING — section absent or placeholders not substituted
  2  PRE_BASELINE_CORRUPT — baseline file malformed
  3  PRE_INVALID_EXEMPTION — audit-exempt without reason
  4  PRE_ENFORCEMENT_BROKEN — self-check failed
EOF
    exit 2
}

# ------------------------------------------------------------------ self-check

if [[ "${1:-}" == "--self-check" ]]; then
    exit_code=0

    # Check for required tools
    if ! command -v gh >/dev/null 2>&1; then
        echo "PRE_ENFORCEMENT_BROKEN: gh CLI not found on PATH" >&2
        exit_code=4
    fi

    if ! command -v jq >/dev/null 2>&1; then
        echo "PRE_ENFORCEMENT_BROKEN: jq not found on PATH" >&2
        exit_code=4
    fi

    # Check baseline file is readable (or absent, which is acceptable)
    if [[ -f "${BASELINE_FILE}" ]]; then
        # Baseline must be readable
        if [[ ! -r "${BASELINE_FILE}" ]]; then
            echo "PRE_ENFORCEMENT_BROKEN: baseline file ${BASELINE_FILE} not readable" >&2
            exit_code=4
        fi
    fi

    exit "${exit_code}"
fi

# ------------------------------------------------------------------ help

if [[ "${1:-}" == "--help" ]]; then
    usage_error
fi

# ------------------------------------------------------------------ arg parse

PR_NUMBER=""
REPO_OVERRIDE=""

while [[ $# -gt 0 ]]; do
    case "${1}" in
        --pr)
            shift
            PR_NUMBER="${1:-}"
            if [[ -z "${PR_NUMBER}" ]]; then
                echo "ERROR: --pr requires a PR number" >&2
                usage_error
            fi
            ;;
        --repo)
            shift
            REPO_OVERRIDE="${1:-}"
            ;;
        --help)
            usage_error
            ;;
        *)
            echo "ERROR: Unknown argument: ${1}" >&2
            usage_error
            ;;
    esac
    shift
done

if [[ -z "${PR_NUMBER}" ]]; then
    echo "ERROR: --pr <PR_NUMBER> is required" >&2
    usage_error
fi

# ------------------------------------------------------------------ baseline

check_baseline() {
    local pr_number="${1}"

    if [[ ! -f "${BASELINE_FILE}" ]]; then
        return 1  # no baseline, not grandfathered
    fi

    # Validate baseline file format (each line: PR_NUMBER  # comment)
    while IFS= read -r line; do
        # Skip empty lines and comment-only lines
        [[ -z "${line}" || "${line}" =~ ^[[:space:]]*# ]] && continue

        # Line must match: NUMBER  # reason (number followed by whitespace or end)
        if ! [[ "${line}" =~ ^[0-9]+([[:space:]].*)?$ ]]; then
            echo "PRE_BASELINE_CORRUPT: malformed line in ${BASELINE_FILE}: ${line}" >&2
            exit 2
        fi
    done < "${BASELINE_FILE}"

    # Check if this PR is grandfathered
    grep -qE "^${pr_number}([[:space:]]|$)" "${BASELINE_FILE}" 2>/dev/null
    return $?
}

# ------------------------------------------------------------------ main logic

# Build gh repo flag
GH_REPO_FLAG=""
if [[ -n "${REPO_OVERRIDE}" ]]; then
    GH_REPO_FLAG="--repo ${REPO_OVERRIDE}"
fi

# Check if PR is in the baseline (grandfathered pre-story-0059-0007)
if check_baseline "${PR_NUMBER}"; then
    echo "INFO: PR #${PR_NUMBER} is grandfathered in ${BASELINE_FILE} — skipping evidence check"
    exit 0
fi

# Fetch PR body via gh CLI
# shellcheck disable=SC2086
PR_BODY=$(gh pr view "${PR_NUMBER}" ${GH_REPO_FLAG} --json body -q '.body' 2>/dev/null)
GH_EXIT=$?

if [[ ${GH_EXIT} -ne 0 ]]; then
    echo "ERROR: Failed to fetch PR #${PR_NUMBER} body via gh CLI (exit ${GH_EXIT})" >&2
    echo "PRE_EVIDENCE_MISSING: could not retrieve PR body for validation" >&2
    exit 1
fi

# Check for audit-exempt marker in the PR body
if echo "${PR_BODY}" | grep -q "<!-- audit-exempt:"; then
    # Validate the exemption has a reason (non-empty after colon)
    EXEMPTION=$(echo "${PR_BODY}" | grep "<!-- audit-exempt:" | head -1)
    REASON=$(echo "${EXEMPTION}" | sed 's/.*<!-- audit-exempt: *\(.*\) *-->.*/\1/' | tr -d '[:space:]')
    if [[ -z "${REASON}" ]]; then
        echo "PRE_INVALID_EXEMPTION: audit-exempt marker found but reason is empty in PR #${PR_NUMBER}" >&2
        exit 3
    fi
    echo "INFO: PR #${PR_NUMBER} has audit-exempt marker with reason: ${REASON}"
    exit 0
fi

# Check for --no-story-evidence flag acknowledgment
if echo "${PR_BODY}" | grep -q "no evidence required (--no-story-evidence)"; then
    echo "INFO: PR #${PR_NUMBER} has --no-story-evidence acknowledgment — no evidence required"
    exit 0
fi

# ---- Validate ## Orchestrator Evidence section presence ----

if ! echo "${PR_BODY}" | grep -q "## Orchestrator Evidence"; then
    echo "PRE_EVIDENCE_MISSING: ## Orchestrator Evidence section missing in PR #${PR_NUMBER}" >&2
    echo "  PRs created manually (without x-pr-create) will not have this section." >&2
    echo "  Use x-pr-create to create PRs, or add --no-story-evidence for chore/docs PRs." >&2
    exit 1
fi

# ---- Validate Story IDs field is not a placeholder ----

if echo "${PR_BODY}" | grep -q "| Story IDs | ${PLACEHOLDER_STORY_ID}"; then
    echo "PRE_EVIDENCE_MISSING: Story IDs field contains placeholder '${PLACEHOLDER_STORY_ID}' in PR #${PR_NUMBER}" >&2
    exit 1
fi

# Check that Story IDs field is present and has a value matching story pattern
STORY_IDS_LINE=$(echo "${PR_BODY}" | grep "| Story IDs |" | head -1)
if [[ -z "${STORY_IDS_LINE}" ]]; then
    echo "PRE_EVIDENCE_MISSING: Story IDs field absent from Orchestrator Evidence table in PR #${PR_NUMBER}" >&2
    exit 1
fi

# Extract the value after "| Story IDs |"
STORY_IDS_VALUE=$(echo "${STORY_IDS_LINE}" | sed 's/.*| Story IDs | *\(.*\) *|.*/\1/' | tr -d '[:space:]')
if ! echo "${STORY_IDS_VALUE}" | grep -qE "story-[0-9]{4}-[0-9]{4}"; then
    echo "PRE_EVIDENCE_MISSING: Story IDs value '${STORY_IDS_VALUE}' does not match pattern story-NNNN-NNNN in PR #${PR_NUMBER}" >&2
    exit 1
fi

# ---- Validate Orchestrator Commit SHA is not a placeholder and is 40 hex chars ----

if echo "${PR_BODY}" | grep -q "| Orchestrator Commit SHA | ${PLACEHOLDER_SHA}"; then
    echo "PRE_EVIDENCE_MISSING: Orchestrator Commit SHA contains placeholder '${PLACEHOLDER_SHA}' in PR #${PR_NUMBER}" >&2
    exit 1
fi

SHA_LINE=$(echo "${PR_BODY}" | grep "| Orchestrator Commit SHA |" | head -1)
if [[ -z "${SHA_LINE}" ]]; then
    echo "PRE_EVIDENCE_MISSING: Orchestrator Commit SHA field absent in PR #${PR_NUMBER}" >&2
    exit 1
fi

SHA_VALUE=$(echo "${SHA_LINE}" | sed 's/.*| Orchestrator Commit SHA | *\(.*\) *|.*/\1/' | tr -d '[:space:]')
if ! echo "${SHA_VALUE}" | grep -qE "^[0-9a-f]{40}$"; then
    echo "PRE_EVIDENCE_MISSING: Orchestrator Commit SHA '${SHA_VALUE}' is not a valid 40-char hex SHA in PR #${PR_NUMBER}" >&2
    exit 1
fi

# ---- Validate Invocation Skill is not a placeholder ----

if echo "${PR_BODY}" | grep -q "| Invocation Skill | ${PLACEHOLDER_SKILL}"; then
    echo "PRE_EVIDENCE_MISSING: Invocation Skill contains placeholder '${PLACEHOLDER_SKILL}' in PR #${PR_NUMBER}" >&2
    exit 1
fi

SKILL_LINE=$(echo "${PR_BODY}" | grep "| Invocation Skill |" | head -1)
if [[ -z "${SKILL_LINE}" ]]; then
    echo "PRE_EVIDENCE_MISSING: Invocation Skill field absent in PR #${PR_NUMBER}" >&2
    exit 1
fi

SKILL_VALUE=$(echo "${SKILL_LINE}" | sed 's/.*| Invocation Skill | *\(.*\) *|.*/\1/' | tr -d '[:space:]')
if [[ -z "${SKILL_VALUE}" ]]; then
    echo "PRE_EVIDENCE_MISSING: Invocation Skill value is empty in PR #${PR_NUMBER}" >&2
    exit 1
fi

# ---- All checks passed ----

echo "OK: PR #${PR_NUMBER} has valid Orchestrator Evidence section"
echo "  Story IDs: ${STORY_IDS_VALUE}"
echo "  SHA: ${SHA_VALUE}"
echo "  Skill: ${SKILL_VALUE}"
exit 0
