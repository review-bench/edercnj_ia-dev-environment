#!/usr/bin/env bash
# Layer:      2 (detectivo — CI audit)
# Trigger:    PR open/sync to epic/* or develop
# Rule:       Rule 26 (Audit Gate Lifecycle), Rule 24 (Execution Integrity)
# Introduced: EPIC-0063 (Local-First Pre-Flight Gates) — story-0063-0020
#
# audit-epic-review-reconciliation.sh — Epic-Review Reconciliation
#
# Validates that all story-level tech-lead reviews are consistent: scans
# techlead-review-story-*.md files in a plans directory and reports any
# that contain NO-GO decisions. A single NO-GO is a reconciliation failure.
#
# Exit codes (Rule 26 §Standardized Exit Codes):
#   0 — OK (all reviews are GO, or no review files found)
#   1 — RECONCILIATION_FAILED (at least one review is NO-GO)
#   2 — OPERATIONAL_ERROR (missing --plans-dir, directory not found, etc.)
#
# Usage:
#   audit-epic-review-reconciliation.sh --epic-id <id>
#   audit-epic-review-reconciliation.sh --plans-dir <path>
#   audit-epic-review-reconciliation.sh --self-check

set -uo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"

# ── Arg parse ─────────────────────────────────────────────────────────────────
EPIC_ID=""
PLANS_DIR=""
SELF_CHECK=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --epic-id)    EPIC_ID="$2"; shift 2 ;;
        --plans-dir)  PLANS_DIR="$2"; shift 2 ;;
        --self-check) SELF_CHECK=true; shift ;;
        *)
            printf 'OPERATIONAL_ERROR: unknown flag: %s\n' "$1" >&2
            exit 2
            ;;
    esac
done

# ── Self-check ────────────────────────────────────────────────────────────────
if "$SELF_CHECK"; then
    command -v grep >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: grep required\n' >&2; exit 2; }
    printf 'SELF_CHECK_OK: %s prerequisites satisfied\n' "$SCRIPT_NAME" >&2
    exit 0
fi

# ── Resolve plans directory ───────────────────────────────────────────────────
if [[ -z "$PLANS_DIR" ]]; then
    if [[ -n "$EPIC_ID" ]]; then
        # Try canonical layout locations
        for candidate in \
            "ai/epics/epic-${EPIC_ID}/plans" \
            "ai/epics/epic-${EPIC_ID}-*/plans"; do
            for d in $candidate; do
                if [[ -d "$d" ]]; then
                    PLANS_DIR="$d"
                    break 2
                fi
            done
        done
        if [[ -z "$PLANS_DIR" ]]; then
            printf 'OPERATIONAL_ERROR: could not find plans directory for epic %s\n' "$EPIC_ID" >&2
            exit 2
        fi
    else
        printf 'OPERATIONAL_ERROR: --plans-dir or --epic-id is required\n' >&2
        exit 2
    fi
fi

# ── Validate plans directory ──────────────────────────────────────────────────
if [[ ! -d "$PLANS_DIR" ]]; then
    printf 'OPERATIONAL_ERROR: plans directory not found: %s\n' "$PLANS_DIR" >&2
    exit 2
fi

# Canonicalize path
PLANS_DIR_REAL="$(realpath "$PLANS_DIR" 2>/dev/null \
    || readlink -f "$PLANS_DIR" 2>/dev/null \
    || echo "$PLANS_DIR")"

# ── Scan tech-lead review files ───────────────────────────────────────────────
REVIEW_FILES=()
while IFS= read -r -d '' f; do
    REVIEW_FILES+=("$f")
done < <(find "$PLANS_DIR_REAL" -maxdepth 1 -name "techlead-review-story-*.md" -print0 2>/dev/null | sort -z)

TOTAL="${#REVIEW_FILES[@]}"

if [[ "$TOTAL" -eq 0 ]]; then
    printf '=== Epic Review Reconciliation ===\n'
    printf 'Plans directory: %s\n' "$PLANS_DIR_REAL"
    printf 'Tech-lead review files found: 0\n'
    printf 'Status: OK (nothing to reconcile)\n'
    printf '\n'
    printf 'AUDIT_OK: no review files found — nothing to reconcile\n' >&2
    exit 0
fi

# ── Check each review for NO-GO ──────────────────────────────────────────────
NOGO_FILES=()
GO_FILES=()

for review_file in "${REVIEW_FILES[@]}"; do
    fname="$(basename "$review_file")"
    # A NO-GO decision is detected when the file contains "NO-GO" (case-sensitive)
    # Must appear as the decision marker, not in prose context
    if grep -qE '^(NO-GO|\*\*NO-GO\*\*)' "$review_file" 2>/dev/null; then
        NOGO_FILES+=("$fname")
    else
        GO_FILES+=("$fname")
    fi
done

# ── Emit dashboard ────────────────────────────────────────────────────────────
printf '=== Epic Review Reconciliation Dashboard ===\n'
printf 'Plans directory: %s\n' "$(basename "$PLANS_DIR_REAL")"
printf 'Tech-lead review files scanned: %d\n' "$TOTAL"
printf '\n'
printf 'GO reviews:    %d\n' "${#GO_FILES[@]}"
printf 'NO-GO reviews: %d\n' "${#NOGO_FILES[@]}"
printf '\n'

if [[ "${#NOGO_FILES[@]}" -eq 0 ]]; then
    printf 'Status: ALL REVIEWS CONSISTENT (all GO)\n'
    printf '\n'
    printf 'AUDIT_OK: all %d tech-lead reviews are GO\n' "$TOTAL" >&2
    exit 0
fi

# ── Report NO-GO findings ─────────────────────────────────────────────────────
printf 'Status: RECONCILIATION_FAILED\n'
printf '\n'
printf 'NO-GO findings:\n'
for f in "${NOGO_FILES[@]}"; do
    printf '  - %s\n' "$f"
done
printf '\n'
printf 'RECONCILIATION_FAILED: %d of %d tech-lead reviews have NO-GO decision\n' \
    "${#NOGO_FILES[@]}" "$TOTAL" >&2
exit 1
