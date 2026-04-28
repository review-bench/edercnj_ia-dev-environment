#!/usr/bin/env bash
# audit-review-content.sh — Camada 2 (detectivo) — Rule 24 / EPIC-0063
#
# Validates review artifact content via 4 independent syntactic heuristics:
#   H1. Non-empty lines >= 50
#   H2. H2/H3 section headings >= 3
#   H3. File references (*.java|*.md|*.sh paths) >= 2
#   H4. Decision marker: GO | NO-GO | GO-WITH-RESERVATIONS (no escape)
#
# Exit codes (Rule 26 §Standardized):
#   0 — OK
#   1 — REVIEW_CONTENT_INSUFFICIENT
#   2 — OPERATIONAL_ERROR
#   3 — BASELINE_CORRUPT (unused, reserved)
#
# Usage:
#   audit-review-content.sh --review-file <path> [--strict]
#   audit-review-content.sh --self-check
#
# Escape hatch: <!-- audit-exempt-content: <reason> --> in review file.
# --strict disables the marker (forced full heuristic check).

set -euo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"

# ── Arg parse ─────────────────────────────────────────────────────────────────
REVIEW_FILE=""
STRICT=false
SELF_CHECK=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --review-file) REVIEW_FILE="$2"; shift 2 ;;
        --strict)      STRICT=true; shift ;;
        --self-check)  SELF_CHECK=true; shift ;;
        *) printf 'OPERATIONAL_ERROR: unknown flag: %s\n' "$1" >&2; exit 2 ;;
    esac
done

# ── Self-check ─────────────────────────────────────────────────────────────────
if "$SELF_CHECK"; then
    command -v grep >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: grep required\n' >&2; exit 2; }
    command -v wc   >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: wc required\n'   >&2; exit 2; }
    printf 'SELF_CHECK_OK: %s prerequisites satisfied\n' "$SCRIPT_NAME" >&2
    exit 0
fi

# ── Prereqs ────────────────────────────────────────────────────────────────────
if [[ -z "$REVIEW_FILE" ]]; then
    printf 'OPERATIONAL_ERROR: --review-file is required\n' >&2
    exit 2
fi

if [[ ! -f "$REVIEW_FILE" ]]; then
    printf 'OPERATIONAL_ERROR: review file not found: %s\n' "$REVIEW_FILE" >&2
    exit 2
fi

# Canonicalize path to prevent traversal
REVIEW_FILE_REAL="$(realpath "$REVIEW_FILE" 2>/dev/null || readlink -f "$REVIEW_FILE" 2>/dev/null || echo "$REVIEW_FILE")"

# ── Exempt marker (skipped under --strict) ─────────────────────────────────────
if ! "$STRICT"; then
    if grep -qF '<!-- audit-exempt-content:' "$REVIEW_FILE_REAL" 2>/dev/null; then
        reason="$(grep -m1 'audit-exempt-content:' "$REVIEW_FILE_REAL" | sed 's/.*audit-exempt-content: *//' | sed 's/ *-->.*//')"
        printf 'WARNING: audit-exempt-content marker found — skipping heuristics. Reason: %s\n' "$reason" >&2
        exit 0
    fi
fi

VIOLATIONS=()

# ── H1: Non-empty lines >= 50 ──────────────────────────────────────────────────
nonempty=$(grep -c '[^[:space:]]' "$REVIEW_FILE_REAL" 2>/dev/null || echo 0)
if [[ "$nonempty" -lt 50 ]]; then
    VIOLATIONS+=("H1_LINES: non-empty lines=$nonempty < 50")
fi

# ── H2: Section headings (## or ###) >= 3 ─────────────────────────────────────
sections=$(grep -cE '^#{2,3} ' "$REVIEW_FILE_REAL" 2>/dev/null || echo 0)
if [[ "$sections" -lt 3 ]]; then
    VIOLATIONS+=("H2_SECTIONS: h2/h3 sections=$sections < 3")
fi

# ── H3: File references >= 2 ──────────────────────────────────────────────────
file_refs=$(grep -cE '[a-z][a-z0-9/_-]*\.(java|md|sh)' "$REVIEW_FILE_REAL" 2>/dev/null || echo 0)
if [[ "$file_refs" -lt 2 ]]; then
    VIOLATIONS+=("H3_FILE_REFS: file references=$file_refs < 2")
fi

# ── H4: Decision marker (mandatory, no escape) ────────────────────────────────
if ! grep -qE '\b(GO|NO-GO|GO-WITH-RESERVATIONS)\b' "$REVIEW_FILE_REAL" 2>/dev/null; then
    VIOLATIONS+=("H4_DECISION: decision marker missing (expected GO|NO-GO|GO-WITH-RESERVATIONS)")
fi

# ── Result ─────────────────────────────────────────────────────────────────────
if [[ ${#VIOLATIONS[@]} -eq 0 ]]; then
    printf 'AUDIT_OK: %s content valid (lines=%d sections=%d refs=%d)\n' \
        "$(basename "$REVIEW_FILE_REAL")" "$nonempty" "$sections" "$file_refs" >&2
    exit 0
fi

printf 'REVIEW_CONTENT_INSUFFICIENT: %s\n' "$(basename "$REVIEW_FILE_REAL")" >&2
for v in "${VIOLATIONS[@]}"; do
    printf '  - %s\n' "$v" >&2
done
exit 1
