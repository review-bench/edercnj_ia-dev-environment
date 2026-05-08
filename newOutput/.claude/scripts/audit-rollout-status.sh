#!/usr/bin/env bash
# Layer:      2 (detectivo — CI audit)
# Trigger:    PR open/sync to epic/* or develop; also manual invocation
# Rule:       Rule 26 (Audit Gate Lifecycle), Rule 27 (Zero-Bypass Lifecycle)
# Introduced: EPIC-0063 (Local-First Pre-Flight Gates) — story-0063-0016
#
# audit-rollout-status.sh — Reports and manages the WARN→FAIL rollout mode
#
# Reports current enforce mode, transition date, and bypass count since rollout start.
# Mode is read from PREFLIGHT_ENFORCE_MODE env var or .claude/state/rollout-mode.json.
#
# Exit codes (Rule 26 §Standardized Exit Codes):
#   0 — OK (status reported or mode written successfully)
#   2 — OPERATIONAL_ERROR (invalid args, invalid mode, I/O error)
#
# Usage:
#   audit-rollout-status.sh                            # report current mode
#   audit-rollout-status.sh --set-mode <warn|fail>     # write mode to state file
#   audit-rollout-status.sh --set-mode <warn|fail> --state-file <path>
#   audit-rollout-status.sh --self-check               # verify prerequisites

set -uo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"

# ── Defaults ──────────────────────────────────────────────────────────────────
SELF_CHECK=false
SET_MODE=""
# Default state file path (can be overridden via --state-file for testing)
PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
DEFAULT_STATE_FILE="${PROJECT_DIR}/.claude/state/rollout-mode.json"
STATE_FILE=""

# ── Arg parse ─────────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --self-check)
            SELF_CHECK=true
            shift
            ;;
        --set-mode)
            if [[ $# -lt 2 ]]; then
                printf 'OPERATIONAL_ERROR: --set-mode requires a value (warn|fail)\n' >&2
                exit 2
            fi
            SET_MODE="$2"
            shift 2
            ;;
        --set-mode=*)
            SET_MODE="${1#--set-mode=}"
            shift
            ;;
        --state-file)
            if [[ $# -lt 2 ]]; then
                printf 'OPERATIONAL_ERROR: --state-file requires a path\n' >&2
                exit 2
            fi
            STATE_FILE="$2"
            shift 2
            ;;
        --state-file=*)
            STATE_FILE="${1#--state-file=}"
            shift
            ;;
        *)
            printf 'OPERATIONAL_ERROR: unknown flag: %s\n' "$1" >&2
            exit 2
            ;;
    esac
done

# Resolve state file path
if [[ -z "$STATE_FILE" ]]; then
    STATE_FILE="$DEFAULT_STATE_FILE"
fi

# ── Self-check ────────────────────────────────────────────────────────────────
if "$SELF_CHECK"; then
    printf 'SELF_CHECK_OK: %s prerequisites satisfied\n' "$SCRIPT_NAME" >&2
    exit 0
fi

# ── Validate mode value ───────────────────────────────────────────────────────
if [[ -n "$SET_MODE" ]]; then
    if [[ "$SET_MODE" != "warn" && "$SET_MODE" != "fail" ]]; then
        printf 'OPERATIONAL_ERROR: invalid mode "%s" — must be warn or fail\n' "$SET_MODE" >&2
        exit 2
    fi
fi

# ── Resolve current mode ──────────────────────────────────────────────────────
resolve_current_mode() {
    # Precedence: env var > state file > default (warn during rollout)
    if [[ -n "${PREFLIGHT_ENFORCE_MODE:-}" ]]; then
        printf '%s' "$PREFLIGHT_ENFORCE_MODE"
        return 0
    fi

    if [[ -f "$STATE_FILE" ]]; then
        local file_mode
        file_mode=$(grep -o '"mode"\s*:\s*"[^"]*"' "$STATE_FILE" 2>/dev/null \
            | grep -o '"[^"]*"$' | tr -d '"' || echo "")
        if [[ "$file_mode" == "warn" || "$file_mode" == "fail" ]]; then
            printf '%s' "$file_mode"
            return 0
        fi
    fi

    # Default: warn (safe default during rollout window)
    printf 'warn'
}

# ── Set mode ─────────────────────────────────────────────────────────────────
if [[ -n "$SET_MODE" ]]; then
    # Create parent directory if needed
    STATE_DIR="$(dirname "$STATE_FILE")"
    if [[ ! -d "$STATE_DIR" ]]; then
        mkdir -p "$STATE_DIR" 2>/dev/null || {
            printf 'OPERATIONAL_ERROR: cannot create directory: %s\n' "$STATE_DIR" >&2
            exit 2
        }
    fi

    TIMESTAMP="$(date -u +%FT%TZ 2>/dev/null || echo "unknown")"
    PREVIOUS_MODE="$(resolve_current_mode)"

    # Write JSON state file
    printf '{\n  "mode": "%s",\n  "set_at": "%s",\n  "previous_mode": "%s",\n  "script": "%s"\n}\n' \
        "$SET_MODE" "$TIMESTAMP" "$PREVIOUS_MODE" "$SCRIPT_NAME" > "$STATE_FILE" 2>/dev/null || {
        printf 'OPERATIONAL_ERROR: cannot write state file: %s\n' "$STATE_FILE" >&2
        exit 2
    }

    printf 'ROLLOUT_STATUS: mode set to "%s" (was: "%s") at %s\n' \
        "$SET_MODE" "$PREVIOUS_MODE" "$TIMESTAMP"
    printf 'ROLLOUT_STATUS: state file: %s\n' "$STATE_FILE"
    exit 0
fi

# ── Report current status ─────────────────────────────────────────────────────
CURRENT_MODE="$(resolve_current_mode)"

# Determine mode source
MODE_SOURCE="default"
if [[ -n "${PREFLIGHT_ENFORCE_MODE:-}" ]]; then
    MODE_SOURCE="env:PREFLIGHT_ENFORCE_MODE"
elif [[ -f "$STATE_FILE" ]]; then
    MODE_SOURCE="file:${STATE_FILE}"
fi

# Read transition date and bypass count from state file if available
TRANSITION_DATE="(not set)"
if [[ -f "$STATE_FILE" ]]; then
    TS=$(grep -o '"set_at"\s*:\s*"[^"]*"' "$STATE_FILE" 2>/dev/null \
        | grep -o '"[^"]*"$' | tr -d '"' || echo "")
    if [[ -n "$TS" ]]; then
        TRANSITION_DATE="$TS"
    fi
fi

# Count bypass events in NDJSON telemetry if discoverable
BYPASS_COUNT=0
for ndjson_candidate in \
    "${PROJECT_DIR}/ai/epics/"*/telemetry/events.ndjson \
    "${PROJECT_DIR}/ai/epics/epic-"*/telemetry/events.ndjson; do
    if [[ -f "$ndjson_candidate" ]]; then
        count=$(grep -c '"recovery_mode_used"' "$ndjson_candidate" 2>/dev/null || echo 0)
        BYPASS_COUNT=$((BYPASS_COUNT + count))
    fi
done

printf '=== Rollout Status Report ===\n'
printf 'Current mode:     %s\n' "$CURRENT_MODE"
printf 'Mode source:      %s\n' "$MODE_SOURCE"
printf 'Transition date:  %s\n' "$TRANSITION_DATE"
printf 'State file:       %s\n' "$STATE_FILE"
printf 'Bypass count:     %d (recovery_mode_used events across all epics)\n' "$BYPASS_COUNT"
printf '\n'

if [[ "$CURRENT_MODE" == "warn" ]]; then
    printf 'INFO: WARN mode — hooks log violations but do not block\n'
    printf 'INFO: Set PREFLIGHT_ENFORCE_MODE=fail or run --set-mode fail to activate blocking\n'
else
    printf 'INFO: FAIL mode — hooks block violations; CLAUDE_RECOVERY_MODE=1 is the only escape\n'
fi

printf '\nAUDIT_OK: rollout-status report generated\n' >&2
exit 0
