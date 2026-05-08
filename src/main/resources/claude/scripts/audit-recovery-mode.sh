#!/usr/bin/env bash
# Layer:      2 (detectivo — CI audit)
# Trigger:    PR open/sync to epic/* or develop
# Rule:       Rule 26 (Audit Gate Lifecycle), Rule 27 (Zero-Bypass Lifecycle)
# Introduced: EPIC-0063 (Local-First Pre-Flight Gates) — story-0063-0017
#
# audit-recovery-mode.sh — Recovery-Mode Periodic Audit Dashboard
#
# Reads events.ndjson (NDJSON telemetry) and produces a dashboard showing
# how often CLAUDE_RECOVERY_MODE=1 bypass events have been used, broken
# down by story and by bypass vector.
#
# Exit codes (Rule 26 §Standardized Exit Codes):
#   0 — OK (dashboard produced, including 0-bypass case)
#   2 — OPERATIONAL_ERROR (missing file, missing jq, invalid args)
#
# Usage:
#   audit-recovery-mode.sh --ndjson-file <path>
#   audit-recovery-mode.sh [--ndjson-file <path>]  # auto-discover if omitted
#   audit-recovery-mode.sh --self-check

set -uo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"

# ── Arg parse ─────────────────────────────────────────────────────────────────
NDJSON_FILE=""
SELF_CHECK=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ndjson-file) NDJSON_FILE="$2"; shift 2 ;;
        --self-check)  SELF_CHECK=true; shift ;;
        *)
            printf 'OPERATIONAL_ERROR: unknown flag: %s\n' "$1" >&2
            exit 2
            ;;
    esac
done

# ── Self-check ────────────────────────────────────────────────────────────────
if "$SELF_CHECK"; then
    command -v jq >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: jq required\n' >&2; exit 2; }
    printf 'SELF_CHECK_OK: %s prerequisites satisfied\n' "$SCRIPT_NAME" >&2
    exit 0
fi

# ── Prereqs ───────────────────────────────────────────────────────────────────
command -v jq >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: jq required\n' >&2; exit 2; }

# ── Auto-discover NDJSON if not supplied ──────────────────────────────────────
if [[ -z "$NDJSON_FILE" ]]; then
    # Look for events.ndjson in common locations relative to CWD
    for candidate in \
        "ai/epics/epic-*/telemetry/events.ndjson" \
        "ai/epics/*/telemetry/events.ndjson"; do
        # Use glob expansion carefully
        for f in $candidate; do
            if [[ -f "$f" ]]; then
                NDJSON_FILE="$f"
                break 2
            fi
        done
    done
    if [[ -z "$NDJSON_FILE" ]]; then
        printf 'OPERATIONAL_ERROR: --ndjson-file not specified and no events.ndjson found\n' >&2
        exit 2
    fi
fi

# ── Validate file ─────────────────────────────────────────────────────────────
if [[ ! -f "$NDJSON_FILE" ]]; then
    printf 'OPERATIONAL_ERROR: NDJSON file not found: %s\n' "$NDJSON_FILE" >&2
    exit 2
fi

# Canonicalize path
NDJSON_FILE_REAL="$(realpath "$NDJSON_FILE" 2>/dev/null \
    || readlink -f "$NDJSON_FILE" 2>/dev/null \
    || echo "$NDJSON_FILE")"

# ── Parse NDJSON for recovery mode events ────────────────────────────────────
# An event is a recovery bypass when metadata.CLAUDE_RECOVERY_MODE == "1"
# We extract: storyId, bypassVector
RECOVERY_JSON="$(jq -rn \
    --argjson events "$(jq -s '.' "$NDJSON_FILE_REAL" 2>/dev/null || echo '[]')" '
    $events
    | map(select(
        (.metadata.CLAUDE_RECOVERY_MODE // "") == "1"
      ))
    | {
        total: length,
        byStory: (group_by(.storyId // "unknown")
            | map({ key: (.[0].storyId // "unknown"), value: length })
            | from_entries),
        byVector: (group_by(.metadata.bypassVector // "unknown")
            | map({ key: (.[0].metadata.bypassVector // "unknown"), value: length })
            | from_entries)
      }
' 2>/dev/null)" || {
    printf 'OPERATIONAL_ERROR: failed to parse NDJSON: %s\n' "$NDJSON_FILE_REAL" >&2
    exit 2
}

TOTAL="$(printf '%s' "$RECOVERY_JSON" | jq -r '.total')"
BY_STORY="$(printf '%s' "$RECOVERY_JSON" | jq -r '.byStory | to_entries[] | "  \(.key): \(.value) bypass(es)" ' 2>/dev/null || true)"
BY_VECTOR="$(printf '%s' "$RECOVERY_JSON" | jq -r '.byVector | to_entries[] | "  \(.key): \(.value) bypass(es)"' 2>/dev/null || true)"

# ── Emit dashboard ─────────────────────────────────────────────────────────────
printf '=== Recovery Mode Audit Dashboard ===\n'
printf 'Source: %s\n' "$(basename "$NDJSON_FILE_REAL")"
printf '\n'
printf 'Total CLAUDE_RECOVERY_MODE=1 bypass events: %s\n' "$TOTAL"
printf '\n'

if [[ "$TOTAL" -eq 0 ]]; then
    printf 'No recovery-mode bypasses detected. All executions followed the standard lifecycle.\n'
else
    printf '--- By Story ---\n'
    if [[ -n "$BY_STORY" ]]; then
        printf '%s\n' "$BY_STORY"
    else
        printf '  (no story breakdown available)\n'
    fi
    printf '\n'
    printf '--- By Bypass Vector ---\n'
    if [[ -n "$BY_VECTOR" ]]; then
        printf '%s\n' "$BY_VECTOR"
    else
        printf '  (no vector breakdown available)\n'
    fi
fi

printf '\n'
printf 'AUDIT_OK: recovery-mode dashboard generated\n' >&2
exit 0
