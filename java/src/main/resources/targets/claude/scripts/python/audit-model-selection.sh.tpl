#!/usr/bin/env bash
set -euo pipefail
# audit-model-selection.sh — Rule 23 (Model Selection) CI audit for {{BUILD_TOOL}} projects
# Exit: 0=OK 1=VIOLATION 2=OPERATIONAL_ERROR
SKILLS_ROOT="${CLAUDE_PROJECT_DIR:-$PWD}/java/src/main/resources/targets/claude/skills"
case "${1:-}" in
  --self-check)
    [[ -d "$SKILLS_ROOT" ]] || { echo "OPERATIONAL_ERROR: skills root not found" >&2; exit 2; }
    exit 0 ;;
esac
violations=0
while IFS= read -r -d '' skill; do
  if grep -q "model: sonnet\|model: haiku\|model: opus" "$skill" 2>/dev/null; then continue; fi
  if grep -q "^user-invocable: true" "$skill" 2>/dev/null; then
    echo "MISSING_MODEL: $skill" >&2; violations=$((violations + 1))
  fi
done < <(find "$SKILLS_ROOT" -name "SKILL.md" -print0 2>/dev/null)
exit $((violations > 0 ? 1 : 0))
