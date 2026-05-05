#!/usr/bin/env bash
set -euo pipefail
# audit-flow-version.sh — Rule 19 (Backward Compatibility) flow-version audit
# Checks every execution-state.json under ai/epics/ for valid flowVersion
# Build tool: {{BUILD_TOOL}} | Lock file: {{LOCK_FILE}}
case "${1:-}" in
  --self-check)
    command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required" >&2; exit 2; }
    exit 0 ;;
esac
violations=0
while IFS= read -r -d '' state; do
  fv=$(jq -r '.flowVersion // empty' "$state" 2>/dev/null)
  case "$fv" in "1"|"2"|"3"|"4"|"5") ;; *)
    echo "FLOW_VERSION_VIOLATION: $state has flowVersion='$fv'" >&2
    violations=$((violations + 1)) ;;
  esac
done < <(find "${CLAUDE_PROJECT_DIR:-$PWD}" -name "execution-state.json" -print0 2>/dev/null)
exit $((violations > 0 ? 1 : 0))
