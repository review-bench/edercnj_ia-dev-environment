#!/usr/bin/env bash
set -euo pipefail
# audit-skill-visibility.sh — Rule 22 (Skill Visibility) CI audit
# Exit: 0=OK 1=SKILL_VISIBILITY_VIOLATION 2=OPERATIONAL_ERROR
SKILLS_ROOT="${CLAUDE_PROJECT_DIR:-$PWD}/java/src/main/resources/targets/claude/skills"
case "${1:-}" in
  --self-check)
    [[ -d "$SKILLS_ROOT" ]] || { echo "OPERATIONAL_ERROR: skills root not found" >&2; exit 2; }
    exit 0 ;;
esac
violations=0
while IFS= read -r -d '' skill; do
  if [[ "$skill" == *"/internal/"* ]]; then
    if ! grep -q "visibility: internal" "$skill" 2>/dev/null; then
      echo "SKILL_VISIBILITY_VIOLATION: $skill missing 'visibility: internal'" >&2
      violations=$((violations + 1))
    fi
  fi
done < <(find "$SKILLS_ROOT" -name "SKILL.md" -print0 2>/dev/null)
exit $((violations > 0 ? 1 : 0))
