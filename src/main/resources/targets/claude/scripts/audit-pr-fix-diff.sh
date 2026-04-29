#!/usr/bin/env bash
# audit-pr-fix-diff.sh — Camada 0 — EPIC-0063 story-0063-0021
#
# Validates that an x-pr-fix iteration produced real, on-target diff before
# allowing re-review. Prevents "LLM finge fix" loop where attempts terminate
# without actual file changes but trigger re-review with same NO-GO.
#
# Exit codes:
#   0 — OK (HEAD changed AND ≥1 finding-path touched)
#   1 — FIX_NO_DIFF (HEAD unchanged)
#   2 — FIX_TRIVIAL_DIFF (only whitespace/comments changed; --strict-trivial only)
#   3 — FIX_OFF_TARGET (HEAD changed but no finding-path touched)
#   4 — OPERATIONAL_ERROR (git/jq missing, files unreadable)
#   5 — INVALID_ARGS

set -uo pipefail

PRE_SHA=""
REVIEW_FILE=""
STRICT_TRIVIAL=false
SELF_CHECK=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --pre-sha=*)        PRE_SHA="${1#--pre-sha=}"; shift ;;
        --pre-sha)          PRE_SHA="$2"; shift 2 ;;
        --review-file=*)    REVIEW_FILE="${1#--review-file=}"; shift ;;
        --review-file)      REVIEW_FILE="$2"; shift 2 ;;
        --strict-trivial)   STRICT_TRIVIAL=true; shift ;;
        --self-check)       SELF_CHECK=true; shift ;;
        *) printf 'INVALID_ARGS: unknown flag: %s\n' "$1" >&2; exit 5 ;;
    esac
done

# Self-check
if "$SELF_CHECK"; then
    command -v git >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: git required\n' >&2; exit 4; }
    command -v grep >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: grep required\n' >&2; exit 4; }
    printf 'SELF_CHECK_OK: audit-pr-fix-diff prerequisites satisfied\n' >&2
    exit 0
fi

# Validate args
if [[ -z "$PRE_SHA" ]]; then
    printf 'INVALID_ARGS: --pre-sha is required\n' >&2; exit 5
fi
if [[ -z "$REVIEW_FILE" ]]; then
    printf 'INVALID_ARGS: --review-file is required\n' >&2; exit 5
fi
if [[ ! -f "$REVIEW_FILE" ]]; then
    printf 'OPERATIONAL_ERROR: review file not found: %s\n' "$REVIEW_FILE" >&2; exit 4
fi

# Get current HEAD
CURRENT_SHA=$(git rev-parse HEAD 2>/dev/null) || {
    printf 'OPERATIONAL_ERROR: failed to get HEAD sha\n' >&2; exit 4
}

# Check 1: HEAD didn't change → FIX_NO_DIFF
if [[ "$PRE_SHA" == "$CURRENT_SHA" ]]; then
    printf 'FIX_NO_DIFF: HEAD unchanged (pre=%s, current=%s) — LLM did not commit any fix\n' \
        "${PRE_SHA:0:7}" "${CURRENT_SHA:0:7}" >&2
    exit 1
fi

# Get diff files (between pre and current HEAD)
DIFF_FILES=$(git diff --name-only "${PRE_SHA}..${CURRENT_SHA}" 2>/dev/null) || {
    printf 'OPERATIONAL_ERROR: failed to compute diff %s..%s\n' "$PRE_SHA" "$CURRENT_SHA" >&2; exit 4
}

if [[ -z "$DIFF_FILES" ]]; then
    printf 'FIX_NO_DIFF: no files changed between %s and %s\n' "${PRE_SHA:0:7}" "${CURRENT_SHA:0:7}" >&2
    exit 1
fi

# Check 2: trivial diff (whitespace only) — only if --strict-trivial
if "$STRICT_TRIVIAL"; then
    SUBSTANTIVE=$(git diff --ignore-all-space --ignore-blank-lines "${PRE_SHA}..${CURRENT_SHA}" 2>/dev/null | head -1)
    if [[ -z "$SUBSTANTIVE" ]]; then
        printf 'FIX_TRIVIAL_DIFF: changes are whitespace/blank-only between %s and %s\n' \
            "${PRE_SHA:0:7}" "${CURRENT_SHA:0:7}" >&2
        exit 2
    fi
fi

# Check 3: extract finding-paths from review file
# Look for lines matching pattern: `path/to/file.ext:LINE` or "**path:** ...:line"
FINDING_PATHS=$(grep -oE '[a-zA-Z0-9_/-]+\.(java|md|sh|json|yml|yaml|xml|properties)' "$REVIEW_FILE" 2>/dev/null | sort -u || true)

if [[ -z "$FINDING_PATHS" ]]; then
    printf 'AUDIT_OK_NO_FINDINGS: no specific paths in review (allowing diff %s)\n' "${CURRENT_SHA:0:7}" >&2
    exit 0
fi

# Check 4: at least one finding-path was touched
TOUCHED=0
while IFS= read -r path; do
    if echo "$DIFF_FILES" | grep -qF "$path"; then
        TOUCHED=$((TOUCHED + 1))
    fi
done <<< "$FINDING_PATHS"

if [[ "$TOUCHED" -eq 0 ]]; then
    printf 'FIX_OFF_TARGET: diff touches files but no review finding-path was addressed\n' >&2
    printf '  diff files: %s\n' "$(echo "$DIFF_FILES" | tr '\n' ' ')" >&2
    printf '  finding paths: %s\n' "$(echo "$FINDING_PATHS" | tr '\n' ' ')" >&2
    exit 3
fi

printf 'AUDIT_OK: fix touched %d/%d finding-paths (diff %s..%s)\n' \
    "$TOUCHED" "$(echo "$FINDING_PATHS" | wc -l | tr -d ' ')" \
    "${PRE_SHA:0:7}" "${CURRENT_SHA:0:7}" >&2
exit 0
