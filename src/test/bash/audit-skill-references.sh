#!/usr/bin/env bash
# Layer:      2 (CI script — detective, runs on PR)
# Purpose:    Verify no legacy x-feature-create references remain in active skill paths
# Exit codes: 0=OK (clean), 1=SKILL_RENAME_VIOLATION (legacy refs found), 2=OPERATIONAL_ERROR
# Story:      story-0077-0003 (TASK-0077-0003-002)

set -euo pipefail

SKILLS_DIR="${1:-src/main/resources/targets/claude/skills}"
LEGACY_PATTERN="x-feature-create"

case "${1:-}" in
  --self-check)
    command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
    [[ -d ".claude/skills" ]] || { echo "OPERATIONAL_ERROR: .claude/skills directory not found" >&2; exit 2; }
    exit 0
    ;;
esac

if [[ ! -d "$SKILLS_DIR" && ! -d ".claude/skills" ]]; then
  echo "OPERATIONAL_ERROR: skills directory not found at $SKILLS_DIR or .claude/skills" >&2
  exit 2
fi

violations=0

# Check .claude/skills/ for legacy directory name
if [[ -d ".claude/skills/$LEGACY_PATTERN" ]]; then
  echo "SKILL_RENAME_VIOLATION: .claude/skills/$LEGACY_PATTERN/ directory still exists" >&2
  violations=$((violations + 1))
fi

# Check source-of-truth skills directory for legacy directory name
if [[ -d "$SKILLS_DIR" ]]; then
  if find "$SKILLS_DIR" -name "$LEGACY_PATTERN" -type d 2>/dev/null | grep -q .; then
    echo "SKILL_RENAME_VIOLATION: Legacy skill directory found in $SKILLS_DIR" >&2
    violations=$((violations + 1))
  fi
fi

# Check SKILL.md files in active paths for references claiming to BE x-feature-create
if [[ -d ".claude/skills" ]]; then
  while IFS= read -r file; do
    if grep -q "^name: $LEGACY_PATTERN$" "$file" 2>/dev/null; then
      echo "SKILL_RENAME_VIOLATION: $file has name: $LEGACY_PATTERN in frontmatter" >&2
      violations=$((violations + 1))
    fi
  done < <(find ".claude/skills" -name "SKILL.md" 2>/dev/null)
fi

if [[ $violations -eq 0 ]]; then
  echo "OK: 0 legacy $LEGACY_PATTERN references found in active skill paths"
  exit 0
else
  echo "SKILL_RENAME_VIOLATION: $violations violation(s) found" >&2
  exit 1
fi
