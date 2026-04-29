#!/usr/bin/env bash
# audit-verify-envelope.sh — Camada 2 (detectivo) — Rule 24 / EPIC-0063
#
# Validates verify-envelope.json artifact structure via 4 independent schema checks:
#   S1. failures field is array (not null, not string)
#   S2. if passed=true then acCheckResults.length >= acCheckCount
#   S3. coverage fields (Line, Branch) are numeric
#   S4. timestamp is ISO8601 format
#
# Exit codes (Rule 26 §Standardized):
#   0 — OK
#   1 — ENVELOPE_CONTENT_INVALID
#   2 — OPERATIONAL_ERROR
#   3 — BASELINE_CORRUPT (unused, reserved)
#
# Usage:
#   audit-verify-envelope.sh --envelope-file <path> [--strict]
#   audit-verify-envelope.sh --self-check

set -euo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"

# ── Arg parse ─────────────────────────────────────────────────────────────────
ENVELOPE_FILE=""
STRICT=false
SELF_CHECK=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --envelope-file) ENVELOPE_FILE="$2"; shift 2 ;;
        --strict)        STRICT=true; shift ;;
        --self-check)    SELF_CHECK=true; shift ;;
        *) printf 'OPERATIONAL_ERROR: unknown flag: %s\n' "$1" >&2; exit 2 ;;
    esac
done

# ── Self-check ─────────────────────────────────────────────────────────────────
if "$SELF_CHECK"; then
    command -v jq >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: jq required\n' >&2; exit 2; }
    printf 'SELF_CHECK_OK: %s prerequisites satisfied\n' "$SCRIPT_NAME" >&2
    exit 0
fi

# ── Prereqs ────────────────────────────────────────────────────────────────────
if [[ -z "$ENVELOPE_FILE" ]]; then
    printf 'OPERATIONAL_ERROR: --envelope-file is required\n' >&2
    exit 2
fi

if [[ ! -f "$ENVELOPE_FILE" ]]; then
    printf 'OPERATIONAL_ERROR: envelope file not found: %s\n' "$ENVELOPE_FILE" >&2
    exit 2
fi

# Canonicalize path to prevent traversal
ENVELOPE_FILE_REAL="$(realpath "$ENVELOPE_FILE" 2>/dev/null || readlink -f "$ENVELOPE_FILE" 2>/dev/null || echo "$ENVELOPE_FILE")"

# ── Parse JSON and extract fields ──────────────────────────────────────────────
if ! JSON_CONTENT=$(jq -r '.' "$ENVELOPE_FILE_REAL" 2>/dev/null); then
    printf 'OPERATIONAL_ERROR: invalid JSON: %s\n' "$(basename "$ENVELOPE_FILE_REAL")" >&2
    exit 2
fi

VIOLATIONS=()

# ── S1: failures must be array (not null, not string) ─────────────────────────
failures_type=$(jq -r '.failures | type' <<<"$JSON_CONTENT" 2>/dev/null || echo "error")
if [[ "$failures_type" != "array" ]]; then
    VIOLATIONS+=("S1_FAILURES_TYPE: failures type=$failures_type, expected array")
fi

# ── S2: if passed=true then acCheckResults.length >= acCheckCount ────────────
passed=$(jq -r '.passed' <<<"$JSON_CONTENT" 2>/dev/null || echo "null")
if [[ "$passed" == "true" ]]; then
    ac_count=$(jq -r '.acCheckCount // 0' <<<"$JSON_CONTENT" 2>/dev/null || echo "0")
    ac_results_len=$(jq -r '.acCheckResults | length' <<<"$JSON_CONTENT" 2>/dev/null || echo "0")
    if [[ "$ac_results_len" -lt "$ac_count" ]]; then
        VIOLATIONS+=("S2_AC_COUNT_MISMATCH: results=$ac_results_len, required=$ac_count")
    fi
fi

# ── S3: coverageLine and coverageBranch must be numeric ──────────────────────
coverage_line_type=$(jq -r '.coverageLine | if . == null then "null" else type end' <<<"$JSON_CONTENT" 2>/dev/null || echo "error")
if [[ "$coverage_line_type" != "number" && "$coverage_line_type" != "null" ]]; then
    VIOLATIONS+=("S3_COVERAGE_LINE_TYPE: coverageLine type=$coverage_line_type, expected number or null")
fi

coverage_branch_type=$(jq -r '.coverageBranch | if . == null then "null" else type end' <<<"$JSON_CONTENT" 2>/dev/null || echo "error")
if [[ "$coverage_branch_type" != "number" && "$coverage_branch_type" != "null" ]]; then
    VIOLATIONS+=("S3_COVERAGE_BRANCH_TYPE: coverageBranch type=$coverage_branch_type, expected number or null")
fi

# ── S4: timestamp must be ISO8601 format ───────────────────────────────────────
timestamp=$(jq -r '.timestamp // empty' <<<"$JSON_CONTENT" 2>/dev/null || echo "")
if [[ -n "$timestamp" ]]; then
    # Basic ISO8601 check: YYYY-MM-DDTHH:MM:SSZ or +/-HH:MM
    if ! echo "$timestamp" | grep -qE '^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]+)?(Z|[+-][0-9]{2}:[0-9]{2})$'; then
        VIOLATIONS+=("S4_TIMESTAMP_FORMAT: timestamp=$timestamp, expected ISO8601")
    fi
fi

# ── Result ─────────────────────────────────────────────────────────────────────
if [[ ${#VIOLATIONS[@]} -eq 0 ]]; then
    printf 'AUDIT_OK: %s envelope valid\n' "$(basename "$ENVELOPE_FILE_REAL")" >&2
    exit 0
fi

printf 'ENVELOPE_CONTENT_INVALID: %s\n' "$(basename "$ENVELOPE_FILE_REAL")" >&2
for v in "${VIOLATIONS[@]}"; do
    printf '  - %s\n' "$v" >&2
done
exit 1
