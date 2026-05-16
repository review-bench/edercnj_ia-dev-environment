#!/usr/bin/env bash
# audit-coverage-local.sh — Camada 0 (preventive) — Rule 05 / EPIC-0063
#
# Local-first coverage gate: parses JaCoCo CSV (or HTML fallback) and validates
# coverage against thresholds (default ≥95% line, ≥90% branch — Rule 05 absolute gate).
#
# Exit codes (Rule 26 §Standardized):
#   0 — OK
#   1 — COVERAGE_BELOW_THRESHOLD
#   2 — OPERATIONAL_ERROR
#   3 — INVALID_ARGS
#
# Usage:
#   audit-coverage-local.sh --report-path=<path> --story-id=<id> [--line-min=N] [--branch-min=N]
#   audit-coverage-local.sh --self-check

set -uo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"

# Defaults (Rule 05 absolute gate)
LINE_MIN="95"
BRANCH_MIN="90"
REPORT_PATH=""
STORY_ID=""
SELF_CHECK=false

# ── Arg parse (supports --flag value AND --flag=value) ────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --line-min=*)     LINE_MIN="${1#--line-min=}"; shift ;;
        --line-min)       LINE_MIN="$2"; shift 2 ;;
        --branch-min=*)   BRANCH_MIN="${1#--branch-min=}"; shift ;;
        --branch-min)     BRANCH_MIN="$2"; shift 2 ;;
        --report-path=*)  REPORT_PATH="${1#--report-path=}"; shift ;;
        --report-path)    REPORT_PATH="$2"; shift 2 ;;
        --story-id=*)     STORY_ID="${1#--story-id=}"; shift ;;
        --story-id)       STORY_ID="$2"; shift 2 ;;
        --self-check)     SELF_CHECK=true; shift ;;
        *) printf 'INVALID_ARGS: unknown flag: %s\n' "$1" >&2; exit 3 ;;
    esac
done

# ── Self-check ────────────────────────────────────────────────────────────────
if "$SELF_CHECK"; then
    command -v awk >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: awk required\n' >&2; exit 2; }
    command -v grep >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: grep required\n' >&2; exit 2; }
    printf 'SELF_CHECK_OK: %s prerequisites satisfied\n' "$SCRIPT_NAME" >&2
    exit 0
fi

# ── Validate args ──────────────────────────────────────────────────────────────
if [[ -z "$REPORT_PATH" ]]; then
    printf 'INVALID_ARGS: --report-path is required\n' >&2
    exit 3
fi

if [[ ! -f "$REPORT_PATH" ]]; then
    printf 'OPERATIONAL_ERROR: report not found: %s\n' "$REPORT_PATH" >&2
    exit 2
fi

# ── Parse coverage ─────────────────────────────────────────────────────────────
LINE_PCT=""
BRANCH_PCT=""

if [[ "$REPORT_PATH" == *.csv ]]; then
    # CSV: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED(4),INSTRUCTION_COVERED(5),
    #      BRANCH_MISSED(6),BRANCH_COVERED(7),LINE_MISSED(8),LINE_COVERED(9),...
    # Force LC_ALL=C to ensure '.' decimal separator (avoid pt-BR ',' issue)
    read -r LINE_PCT BRANCH_PCT < <(LC_ALL=C awk -F, '
        NR>1 {
            bm += $6; bc += $7
            lm += $8; lc += $9
        }
        END {
            line_total = lm + lc
            branch_total = bm + bc
            line_pct = (line_total > 0) ? (lc * 100 / line_total) : 100
            branch_pct = (branch_total > 0) ? (bc * 100 / branch_total) : 100
            printf "%.1f %.1f", line_pct, branch_pct
        }
    ' "$REPORT_PATH")
elif [[ "$REPORT_PATH" == *.html || "$REPORT_PATH" == *.htm ]]; then
    # HTML fallback — parse "Total" row percentages
    LINE_PCT=$(grep -oE 'Total[^%]*?([0-9]+)%[^%]*?([0-9]+)%' "$REPORT_PATH" | head -1 | grep -oE '[0-9]+' | head -1 || echo "0")
    BRANCH_PCT=$(grep -oE 'Total[^%]*?([0-9]+)%[^%]*?([0-9]+)%' "$REPORT_PATH" | head -1 | grep -oE '[0-9]+' | sed -n '2p' || echo "0")
else
    printf 'OPERATIONAL_ERROR: unsupported report format (need .csv or .html): %s\n' "$REPORT_PATH" >&2
    exit 2
fi

if [[ -z "$LINE_PCT" || -z "$BRANCH_PCT" ]]; then
    printf 'OPERATIONAL_ERROR: failed to parse coverage from: %s\n' "$REPORT_PATH" >&2
    exit 2
fi

# ── Compare against thresholds (using awk for float comparison) ────────────────
RESULT=$(LC_ALL=C awk -v lp="$LINE_PCT" -v bp="$BRANCH_PCT" -v lm="$LINE_MIN" -v bm="$BRANCH_MIN" \
    'BEGIN { print (lp+0 >= lm+0 && bp+0 >= bm+0) ? "PASS" : "FAIL" }')

if [[ "$RESULT" == "PASS" ]]; then
    printf 'COVERAGE_OK: story=%s line=%s%% (≥%s) branch=%s%% (≥%s)\n' \
        "${STORY_ID:-N/A}" "$LINE_PCT" "$LINE_MIN" "$BRANCH_PCT" "$BRANCH_MIN" >&2
    exit 0
fi

printf 'COVERAGE_BELOW_THRESHOLD: story=%s line=%s%% min=%s%% branch=%s%% min=%s%% — fix: add tests\n' \
    "${STORY_ID:-N/A}" "$LINE_PCT" "$LINE_MIN" "$BRANCH_PCT" "$BRANCH_MIN" >&2
exit 1
