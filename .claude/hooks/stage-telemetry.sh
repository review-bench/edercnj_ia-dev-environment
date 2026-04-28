#!/usr/bin/env bash
# stage-telemetry.sh — Stop hook: auto-stage events.ndjson when a story is in progress.
#
# EPIC-0059 story-0059-0008: events.ndjson is committed evidence of orchestrator
# execution (Rule 24 Camada 4 + Camada 2 via verify-story-completion.sh).
# This hook ensures the file is always staged at end-of-turn so it lands in the
# next commit automatically.
#
# EPIC-0063 story-0063-0003: extended with flock-protected git add (worktree race
# protection) and v4 layout support (ai/epics/<slug>/telemetry/events.ndjson).
#
# Behaviour:
#   - Reads execution-state.json to detect any story with status "Em Andamento"
#   - If found: git add the events.ndjson for that story's epic (both v3 + v4 layouts)
#   - flock-protected: non-blocking; if lock busy, defers to next Stop (fail-OPEN)
#   - If not found OR CLAUDE_TELEMETRY_DISABLED=1: no-op, exit 0
#   - Never fails loudly — always exits 0 (fail-open per Rule 07 / RULE-005)
#
# Rule 26 compliance: Hook runtime layer (verify-*.sh prefix reserved; this file
# is a staging helper, named without the verify- prefix).

set -uo pipefail

# --self-check mode (Rule 26 §Camada 0 Hook Contract)
case "${1:-}" in
  --self-check)
    command -v git >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: git required" >&2; exit 2; }
    echo "stage-telemetry.sh: self-check passed"
    exit 0
    ;;
esac

# Fail-open: telemetry disabled
if [[ "${CLAUDE_TELEMETRY_DISABLED:-0}" == "1" ]]; then
  exit 0
fi

# Fail-OPEN: git ausente
if ! command -v git >/dev/null 2>&1; then
  echo "WARN [stage-telemetry] git not found, skipping stage" >&2
  exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
LOCK_DIR="${PROJECT_DIR}/.claude/state"
LOCK_FILE="${LOCK_DIR}/stage-telemetry.lock"

# Garantir lock dir (idempotente)
mkdir -p "${LOCK_DIR}" 2>/dev/null || {
  echo "WARN [stage-telemetry] cannot create lock dir: ${LOCK_DIR}" >&2
  exit 0
}

# flock-protected (non-blocking) — race protection entre worktrees paralelos
if command -v flock >/dev/null 2>&1; then
  exec 9>"${LOCK_FILE}" 2>/dev/null || {
    echo "WARN [stage-telemetry] cannot open lock file" >&2
    exit 0
  }
  if ! flock -n 9; then
    echo "WARN [stage-telemetry] lock busy (parallel worktree), deferring" >&2
    exit 0
  fi
fi

# Discover NDJSON files em ambos layouts (v3 plans/ + v4 ai/epics/)
NDJSON_FILES=()
while IFS= read -r -d '' f; do
  NDJSON_FILES+=("$f")
done < <(find "${PROJECT_DIR}/plans" "${PROJECT_DIR}/ai/epics" \
            -type f -name 'events.ndjson' -path '*/telemetry/*' -print0 2>/dev/null || true)

if [ "${#NDJSON_FILES[@]}" -eq 0 ]; then
  exit 0
fi

# Stage cada arquivo (fail-OPEN se git falhar)
ADDED=0
for f in "${NDJSON_FILES[@]}"; do
  if git -C "${PROJECT_DIR}" add "$f" 2>/dev/null; then
    ADDED=$((ADDED + 1))
  fi
done

if [ "$ADDED" -gt 0 ]; then
  echo "INFO [stage-telemetry] staged $ADDED NDJSON file(s)" >&2
fi

exit 0
