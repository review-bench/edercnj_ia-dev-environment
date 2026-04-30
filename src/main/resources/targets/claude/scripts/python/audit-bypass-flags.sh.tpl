#!/usr/bin/env bash
set -euo pipefail
# audit-bypass-flags.sh — Rule 45 (CI-Watch Integrity) bypass flag audit
# Exit: 0=OK 1=BYPASS_FLAG_VIOLATION 2=OPERATIONAL_ERROR
SKILLS_ROOT="${CLAUDE_PROJECT_DIR:-$PWD}/src/main/resources/targets/claude/skills"
case "${1:-}" in
  --self-check)
    [[ -d "$SKILLS_ROOT" ]] || { echo "OPERATIONAL_ERROR: skills root not found" >&2; exit 2; }
    exit 0 ;;
esac
violations=0
while IFS= read -r -d '' skill; do
  if grep -q "\-\-no-ci-watch\|\-\-skip-verification" "$skill" 2>/dev/null; then
    if ! grep -B1 "\-\-no-ci-watch\|\-\-skip-verification" "$skill" 2>/dev/null | grep -q "## Recovery\|audit-exempt"; then
      echo "BYPASS_FLAG_VIOLATION: $skill" >&2; violations=$((violations + 1))
    fi
  fi
done < <(find "$SKILLS_ROOT" -name "SKILL.md" -print0 2>/dev/null)
exit $((violations > 0 ? 1 : 0))
