#!/usr/bin/env bash
# Layer:      2 (detectivo — CI audit)
# Rule:       Rule 26 (Audit Gate Lifecycle), Rule 13 (Skill Invocation Protocol)
# Introduced: EPIC-0063 (Local-First Pre-Flight Gates) — story-0063-0014
#
# audit-wave-dispatch.sh — Sub-Skill Wave Dispatch Audit
#
# Validates that when x-implement-story Phase 1 dispatches parallel planning agents
# (Batch A), the telemetry NDJSON shows subagent.start events for the expected wave
# members. Detects cases where agents were supposed to run in parallel but were
# instead run serially or skipped.
#
# Parallel detection algorithm:
#   - Extract all subagent.start events for the given story-id
#   - Count events: fewer than --expected-wave-size → WAVE_DISPATCH_INCOMPLETE
#   - Check clustering: all events must fall within a 30-second window
#     (i.e., max(ts) - min(ts) < 30s across the wave batch)
#   - If any subagent.start follows a subagent.end by > 30s → serial dispatch detected
#
# Exit codes (Rule 26 §Standardized Exit Codes):
#   0 — OK (wave dispatched with enough parallel agents within time window)
#   1 — WAVE_DISPATCH_INCOMPLETE (fewer agents than expected OR all serial)
#   2 — OPERATIONAL_ERROR (missing args, file not found, missing jq)
#
# Usage:
#   audit-wave-dispatch.sh --ndjson-file <path> --story-id <id> [--expected-wave-size <N>]
#   audit-wave-dispatch.sh --self-check
#
# Examples:
#   audit-wave-dispatch.sh --ndjson-file ai/epics/epic-0063/telemetry/events.ndjson \
#       --story-id story-0063-0014 --expected-wave-size 5
#   audit-wave-dispatch.sh --self-check

set -uo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"

# Default time window (seconds) within which starts must cluster to be "parallel"
PARALLEL_WINDOW_SECONDS=30

# ── Epoch converter (portability: Linux date vs macOS date) ───────────────────
ts_to_epoch() {
    local ts="$1"
    if date -d "$ts" +%s >/dev/null 2>&1; then
        date -d "$ts" +%s
    else
        # macOS / BSD date
        date -jf "%Y-%m-%dT%H:%M:%SZ" "$ts" +%s 2>/dev/null \
            || date -jf "%Y-%m-%dT%H:%M:%S" "${ts%Z}" +%s 2>/dev/null \
            || { printf 'OPERATIONAL_ERROR: cannot parse timestamp: %s\n' "$ts" >&2; exit 2; }
    fi
}

# ── Arg parse ─────────────────────────────────────────────────────────────────
NDJSON_FILE=""
STORY_ID=""
EXPECTED_WAVE_SIZE=3
SELF_CHECK=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ndjson-file)       NDJSON_FILE="$2";         shift 2 ;;
        --story-id)          STORY_ID="$2";             shift 2 ;;
        --expected-wave-size) EXPECTED_WAVE_SIZE="$2"; shift 2 ;;
        --self-check)        SELF_CHECK=true;           shift ;;
        --help)
            printf 'Usage: %s --ndjson-file <path> --story-id <id> [--expected-wave-size <N>]\n' "$SCRIPT_NAME"
            printf '       %s --self-check\n' "$SCRIPT_NAME"
            exit 0
            ;;
        *)
            printf 'OPERATIONAL_ERROR: unknown flag: %s\n' "$1" >&2
            exit 2
            ;;
    esac
done

# ── Self-check mode ───────────────────────────────────────────────────────────
if $SELF_CHECK; then
    ok=true
    if ! command -v jq >/dev/null 2>&1; then
        printf 'OPERATIONAL_ERROR: jq is required but not found on PATH\n' >&2
        ok=false
    fi
    $ok && exit 0 || exit 2
fi

# ── Validate required args ────────────────────────────────────────────────────
if [[ -z "$NDJSON_FILE" ]]; then
    printf 'OPERATIONAL_ERROR: --ndjson-file is required\n' >&2
    exit 2
fi

if [[ -z "$STORY_ID" ]]; then
    printf 'OPERATIONAL_ERROR: --story-id is required\n' >&2
    exit 2
fi

if [[ ! -f "$NDJSON_FILE" ]]; then
    printf 'OPERATIONAL_ERROR: NDJSON file not found: %s\n' "$NDJSON_FILE" >&2
    exit 2
fi

if ! command -v jq >/dev/null 2>&1; then
    printf 'OPERATIONAL_ERROR: jq is required but not found on PATH\n' >&2
    exit 2
fi

# ── Extract subagent.start timestamps for the story ──────────────────────────
# Filter events: type == "subagent.start" AND storyId matches
mapfile -t START_TIMESTAMPS < <(
    jq -r --arg sid "$STORY_ID" \
        'select(.type == "subagent.start" and .storyId == $sid) | .ts' \
        "$NDJSON_FILE" 2>/dev/null
)

ACTUAL_COUNT="${#START_TIMESTAMPS[@]}"

# ── Check count ───────────────────────────────────────────────────────────────
if [[ "$ACTUAL_COUNT" -lt "$EXPECTED_WAVE_SIZE" ]]; then
    printf 'WAVE_DISPATCH_INCOMPLETE: story=%s expected=%d actual=%d subagent.start events\n' \
        "$STORY_ID" "$EXPECTED_WAVE_SIZE" "$ACTUAL_COUNT" >&2
    exit 1
fi

# ── Check clustering (parallel detection) ────────────────────────────────────
# Convert all timestamps to epoch seconds and find min/max of first EXPECTED_WAVE_SIZE
MIN_EPOCH=""
MAX_EPOCH=""

# We look at the first EXPECTED_WAVE_SIZE start events (the Batch A wave)
wave_count=0
for ts in "${START_TIMESTAMPS[@]}"; do
    [[ $wave_count -ge $EXPECTED_WAVE_SIZE ]] && break
    epoch=$(ts_to_epoch "$ts") || exit 2

    if [[ -z "$MIN_EPOCH" ]]; then
        MIN_EPOCH="$epoch"
        MAX_EPOCH="$epoch"
    else
        [[ "$epoch" -lt "$MIN_EPOCH" ]] && MIN_EPOCH="$epoch"
        [[ "$epoch" -gt "$MAX_EPOCH" ]] && MAX_EPOCH="$epoch"
    fi
    wave_count=$((wave_count + 1))
done

SPREAD=$(( MAX_EPOCH - MIN_EPOCH ))

if [[ "$SPREAD" -ge "$PARALLEL_WINDOW_SECONDS" ]]; then
    printf 'WAVE_DISPATCH_INCOMPLETE: story=%s wave spread=%ds >= %ds threshold; agents appear serial\n' \
        "$STORY_ID" "$SPREAD" "$PARALLEL_WINDOW_SECONDS" >&2
    exit 1
fi

printf 'OK: story=%s wave_size=%d spread=%ds (parallel threshold=%ds)\n' \
    "$STORY_ID" "$ACTUAL_COUNT" "$SPREAD" "$PARALLEL_WINDOW_SECONDS"
exit 0
