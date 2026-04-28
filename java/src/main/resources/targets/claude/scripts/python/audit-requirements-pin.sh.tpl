#!/usr/bin/env bash
set -euo pipefail
# audit-requirements-pin.sh — Python: dependency pin audit
# Verifies all deps in requirements.txt use == (pinned), not >= (unpinned)
# Lock file: {{LOCK_FILE}}
case "${1:-}" in
  --self-check)
    [[ -f "{{LOCK_FILE}}" ]] || { echo "OPERATIONAL_ERROR: {{LOCK_FILE}} not found" >&2; exit 2; }
    exit 0 ;;
esac
violations=0
while IFS= read -r line; do
  [[ "$line" =~ ^# ]] && continue
  [[ -z "$line" ]] && continue
  if [[ "$line" =~ ">=" ]]; then
    echo "REQUIREMENTS_PIN_VIOLATION: unpinned dependency: $line" >&2
    violations=$((violations + 1))
  fi
done < "{{LOCK_FILE}}"
exit $((violations > 0 ? 1 : 0))
