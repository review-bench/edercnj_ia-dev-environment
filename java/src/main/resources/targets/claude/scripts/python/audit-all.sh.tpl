#!/usr/bin/env bash
set -euo pipefail
# audit-all.sh — Aggregate audit runner for {{BUILD_TOOL}} projects
# Runs all 9 audits and reports aggregated results
# Coverage: {{COVERAGE_REPORT_PATH}} | Lock file: {{LOCK_FILE}}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
failures=0
for audit in [0maudit-bypass-flags.sh [0maudit-epic-branches.sh [0maudit-execution-integrity.sh [0maudit-flow-version.sh [0maudit-model-selection.sh [0maudit-phase-gates.sh [0maudit-requirements-pin.sh [0maudit-skill-visibility.sh [0maudit-task-hierarchy.sh ; do
  if "$SCRIPT_DIR/$audit" "$@"; then
    echo "PASS $audit"
  else
    echo "FAIL $audit" >&2
    failures=$((failures + 1))
  fi
done
echo "--- audit-all complete: $failures failure(s) ---"
exit $((failures > 0 ? 1 : 0))
