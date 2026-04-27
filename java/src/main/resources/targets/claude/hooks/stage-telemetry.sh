#!/usr/bin/env bash
# stage-telemetry.sh — Stop hook: auto-stage events.ndjson when a story is in progress.
#
# EPIC-0059 story-0059-0008: events.ndjson is committed evidence of orchestrator
# execution (Rule 24 Camada 4 + Camada 2 via verify-story-completion.sh).
# This hook ensures the file is always staged at end-of-turn so it lands in the
# next commit automatically.
#
# Behaviour:
#   - Reads execution-state.json to detect any story with status "Em Andamento"
#   - If found: git add the events.ndjson for that story's epic
#   - If not found OR CLAUDE_TELEMETRY_DISABLED=1: no-op, exit 0
#   - Never fails loudly — always exits 0 (fail-open per Rule 07)
#
# Rule 26 compliance: Hook runtime layer (verify-*.sh prefix reserved; this file
# is not a verify-* hook — it's a staging helper, named without the verify- prefix).
set -uo pipefail

# Fail-open: telemetry disabled
if [[ "${CLAUDE_TELEMETRY_DISABLED:-0}" == "1" ]]; then
  exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"

# Find active story across all epic execution-state.json files
active_story=""
active_epic=""
for state_file in "${PROJECT_DIR}"/plans/epic-*/execution-state.json; do
  [[ -f "${state_file}" ]] || continue
  # Extract epic id from path
  epic_id="$(basename "$(dirname "${state_file}")" | grep -oE '[0-9]{4}$' || true)"
  [[ -z "${epic_id}" ]] && continue
  # Check if any story is Em Andamento (in progress)
  if python3 -c "
import json, sys
try:
    d = json.load(open('${state_file}'))
    statuses = d.get('storyStatuses', {})
    for sid, sv in statuses.items():
        status = sv.get('status', '') if isinstance(sv, dict) else ''
        if status == 'Em Andamento':
            print(sid)
            sys.exit(0)
    sys.exit(1)
except Exception:
    sys.exit(1)
" 2>/dev/null; then
    active_story="found"
    active_epic="${epic_id}"
    break
  fi
done

if [[ -z "${active_story}" ]]; then
  exit 0
fi

# Stage events.ndjson for the active epic
ndjson_path="${PROJECT_DIR}/plans/epic-${active_epic}/telemetry/events.ndjson"
if [[ -f "${ndjson_path}" ]]; then
  git -C "${PROJECT_DIR}" add "${ndjson_path}" 2>/dev/null || true
fi

exit 0
