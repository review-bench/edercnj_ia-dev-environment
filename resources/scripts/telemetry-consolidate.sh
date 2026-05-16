#!/usr/bin/env bash
# Aggregates telemetry NDJSON events by scope (story/task/epic) and emits
# JSON, Markdown, or ASCII-table output consumed by x-internal-render-pr-body.
# Exit codes: 0=OK  1=NO_TELEMETRY  2=OPERATIONAL_ERROR
set -euo pipefail

SCOPE=""
SCOPE_ID=""
FORMAT="json"

# ── arg parsing ───────────────────────────────────────────────────────────────
parse_args() {
    for arg in "$@"; do
        case "$arg" in
            --self-check) self_check; exit 0 ;;
            --story=*)    SCOPE="story"; SCOPE_ID="${arg#--story=}" ;;
            --task=*)     SCOPE="task";  SCOPE_ID="${arg#--task=}" ;;
            --epic=*)     SCOPE="epic";  SCOPE_ID="${arg#--epic=}" ;;
            --format=*)   FORMAT="${arg#--format=}" ;;
            --help)       usage; exit 0 ;;
            *) echo "OPERATIONAL_ERROR: unknown flag: $arg" >&2; exit 2 ;;
        esac
    done
    if [[ -z "$SCOPE" ]]; then
        echo "OPERATIONAL_ERROR: one of --story=, --task=, or --epic= is required" >&2; exit 2
    fi
    case "$FORMAT" in
        json|md|table) ;;
        *) echo "OPERATIONAL_ERROR: --format must be json, md, or table" >&2; exit 2 ;;
    esac
}

usage() { echo "Usage: $(basename "$0") --story=<id>|--task=<id>|--epic=<id> [--format=json|md|table]"; }

# ── self-check ────────────────────────────────────────────────────────────────
self_check() {
    command -v jq >/dev/null 2>&1 \
        || { echo "OPERATIONAL_ERROR: jq not found on PATH" >&2; exit 2; }
    local plans_dir="${CLAUDE_PROJECT_DIR:-$PWD}/ai/epics"
    [[ -d "$plans_dir" ]] \
        || { echo "OPERATIONAL_ERROR: plans directory not found: $plans_dir" >&2; exit 2; }
    [[ -x "$0" ]] \
        || { echo "OPERATIONAL_ERROR: script not executable: $0" >&2; exit 2; }
    echo "OK: telemetry-consolidate.sh self-check passed"
}

# ── NDJSON resolution ─────────────────────────────────────────────────────────
resolve_ndjson() {
    local base="${CLAUDE_PROJECT_DIR:-$PWD}"
    local ndjson
    case "$SCOPE" in
        story)
            local epic_id; epic_id=$(echo "$SCOPE_ID" | sed 's/story-\([0-9]*\)-.*/\1/')
            ndjson=$(find "$base/ai/epics" -name "events.ndjson" \
                     -path "*epic-${epic_id}*" 2>/dev/null | head -1)
            ;;
        task)
            local epic_id; epic_id=$(echo "$SCOPE_ID" | sed 's/TASK-\([0-9]*\)-.*/\1/')
            ndjson=$(find "$base/ai/epics" -name "events.ndjson" \
                     -path "*epic-${epic_id}*" 2>/dev/null | head -1)
            ;;
        epic)
            local epic_id; epic_id=$(echo "$SCOPE_ID" | sed 's/epic-\([0-9]*\).*/\1/')
            ndjson=$(find "$base/ai/epics" -name "events.ndjson" \
                     -path "*epic-${epic_id}*" 2>/dev/null | head -1)
            ;;
    esac
    echo "${ndjson:-}"
}

# ── aggregation ───────────────────────────────────────────────────────────────
aggregate_json() {
    local ndjson="$1"
    command -v jq >/dev/null 2>&1 \
        || { echo "OPERATIONAL_ERROR: jq not found on PATH" >&2; exit 2; }
    [[ -f "$ndjson" && -s "$ndjson" ]] \
        || { echo "NO_TELEMETRY: no events found for ${SCOPE}=${SCOPE_ID}" >&2; exit 1; }

    local filter
    case "$SCOPE" in
        story) filter=".storyId == \"$SCOPE_ID\" or (.metadata.storyId // empty) == \"$SCOPE_ID\"" ;;
        task)  filter="(.metadata.taskId // empty) == \"$SCOPE_ID\"" ;;
        epic)  filter=".epicId == \"$SCOPE_ID\" or (.metadata.epicId // empty) == \"$SCOPE_ID\"" ;;
    esac

    local tmp; tmp=$(mktemp)
    # Parse valid lines only; skip malformed with WARNING
    local line_num=0 valid=0
    while IFS= read -r line; do
        line_num=$((line_num+1))
        if echo "$line" | jq -e . >/dev/null 2>&1; then
            echo "$line" >> "$tmp"
            valid=$((valid+1))
        else
            echo "WARN: skipping malformed line $line_num in $ndjson" >&2
        fi
    done < "$ndjson"

    [[ $valid -gt 0 ]] || { echo "NO_TELEMETRY: no valid events in $ndjson" >&2; rm -f "$tmp"; exit 1; }

    # Determine epicId from first event
    local epic_id
    epic_id=$(jq -rs '.[0].epicId // .[0].metadata.epicId // "unknown"' "$tmp")

    jq -rs --arg scope "$SCOPE" --arg id "$SCOPE_ID" --arg eid "$epic_id" \
       --argjson filter_expr "null" '
    # filter relevant events
    ( . | map(select(
        (.storyId == $id) or
        ((.metadata.storyId // "") == $id) or
        ((.metadata.taskId // "") == $id) or
        (.epicId == $id) or
        ((.metadata.epicId // "") == $id)
    )) ) as $evts |
    ($evts | length) as $total |
    ($evts | map(select(.durationMs? // 0 > 0)) | map(.durationMs) | add // 0) as $active |
    ($evts | map(.timestamp) | (max // 0) - (min // 0)) as $elapsed |

    # tasks breakdown
    ($evts | group_by(.metadata.taskId // "")
     | map(select(.[0].metadata.taskId? and .[0].metadata.taskId != ""))
     | map({
         id: .[0].metadata.taskId,
         activeMs: (map(select(.durationMs? // 0 > 0)) | map(.durationMs) | add // 0),
         elapsedMs: (map(.timestamp) | (max // 0) - (min // 0)),
         events: length
       })
    ) as $tasks |

    # top-5 tools
    ($evts | group_by(.tool // "")
     | map(select(.[0].tool? and .[0].tool != ""))
     | map({
         tool: .[0].tool,
         calls: length,
         totalMs: (map(select(.durationMs? // 0 > 0)) | map(.durationMs) | add // 0),
         p95Ms: (if length > 0 then
                   (sort_by(.durationMs) |
                    .[ [0, ((length * 95 / 100 | floor) - 1)] | max ]
                    | .durationMs // 0)
                 else 0 end)
       })
     | sort_by(-.totalMs) | .[0:5]
    ) as $tools |

    {
      scope: $scope,
      id: $id,
      epicId: $eid,
      events: $total,
      activeMs: ($active | round),
      elapsedMs: ($elapsed | round),
      tasks: $tasks,
      topTools: $tools
    }
    ' "$tmp"
    rm -f "$tmp"
}

# ── output formatting ─────────────────────────────────────────────────────────
format_output() {
    local json="$1"
    case "$FORMAT" in
        json) echo "$json" ;;
        md)
            local id scope active elapsed events
            id=$(echo "$json" | jq -r '.id')
            scope=$(echo "$json" | jq -r '.scope')
            active=$(echo "$json" | jq -r '(.activeMs/60000)|floor')
            elapsed=$(echo "$json" | jq -r '(.elapsedMs/60000)|floor')
            events=$(echo "$json" | jq -r '.events')
            echo "## Telemetria — $id"
            echo ""
            echo "| Scope | Active time | Elapsed time | Events |"
            echo "| :--- | :--- | :--- | :--- |"
            echo "| ${scope^} \`$id\` | ${active} min | ${elapsed} min | ${events} |"
            echo "$json" | jq -r '.tasks[]? | "| Task `\(.id)` | \(.activeMs/60000|floor) min | \(.elapsedMs/60000|floor) min | \(.events) |"'
            echo ""
            echo "**Top tools:**"
            echo "$json" | jq -r '.topTools[]? | "- `\(.tool)`: \(.totalMs/1000|floor) s (\(.calls) calls, P95 \(.p95Ms) ms)"'
            ;;
        table)
            local id scope active elapsed events
            id=$(echo "$json" | jq -r '.id')
            scope=$(echo "$json" | jq -r '.scope')
            active=$(echo "$json" | jq -r '(.activeMs/60000)|floor')
            elapsed=$(echo "$json" | jq -r '(.elapsedMs/60000)|floor')
            events=$(echo "$json" | jq -r '.events')
            printf "%-12s %-32s %10s %12s %8s\n" "SCOPE" "ID" "ACTIVE_MIN" "ELAPSED_MIN" "EVENTS"
            printf "%-12s %-32s %10s %12s %8s\n" "$scope" "$id" "$active" "$elapsed" "$events"
            echo "$json" | jq -r '.tasks[]? | "task            \(.id)                 \(.activeMs/60000|floor)            \(.elapsedMs/60000|floor)          \(.events)"'
            ;;
    esac
}

# ── main ──────────────────────────────────────────────────────────────────────
main() {
    parse_args "$@"
    local ndjson; ndjson=$(resolve_ndjson)
    if [[ -z "$ndjson" ]]; then
        echo "NO_TELEMETRY: events.ndjson not found for ${SCOPE}=${SCOPE_ID}" >&2
        exit 1
    fi
    local json; json=$(aggregate_json "$ndjson")
    format_output "$json"
}

main "$@"
