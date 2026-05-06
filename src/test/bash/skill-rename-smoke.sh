#!/usr/bin/env bash
# Layer:      2 (smoke test — detective)
# Purpose:    Verify x-create-feature skill exists and x-feature-create is absent
# Exit codes: 0=PASS, 1=FAIL
# Story:      story-0077-0003 (TASK-0077-0003-002)

set -euo pipefail

SKILLS_DIR=".claude/skills"
NEW_SKILL="x-create-feature"
OLD_SKILL="x-feature-create"

failures=0

check() {
  local desc="$1"
  local result="$2"
  if [[ "$result" == "PASS" ]]; then
    echo "  PASS: $desc"
  else
    echo "  FAIL: $desc" >&2
    failures=$((failures + 1))
  fi
}

echo "=== skill-rename-smoke.sh ==="

# S1: new skill directory exists
if [[ -d "$SKILLS_DIR/$NEW_SKILL" ]]; then
  check "$SKILLS_DIR/$NEW_SKILL/ exists" "PASS"
else
  check "$SKILLS_DIR/$NEW_SKILL/ exists" "FAIL"
fi

# S2: new skill has SKILL.md
if [[ -f "$SKILLS_DIR/$NEW_SKILL/SKILL.md" ]]; then
  check "$SKILLS_DIR/$NEW_SKILL/SKILL.md present" "PASS"
else
  check "$SKILLS_DIR/$NEW_SKILL/SKILL.md present" "FAIL"
fi

# S3: old skill directory absent
if [[ ! -d "$SKILLS_DIR/$OLD_SKILL" ]]; then
  check "$SKILLS_DIR/$OLD_SKILL/ absent" "PASS"
else
  check "$SKILLS_DIR/$OLD_SKILL/ absent" "FAIL"
fi

# S4: new skill has correct name in frontmatter
if grep -q "name: $NEW_SKILL" "$SKILLS_DIR/$NEW_SKILL/SKILL.md" 2>/dev/null; then
  check "frontmatter name: $NEW_SKILL" "PASS"
else
  check "frontmatter name: $NEW_SKILL" "FAIL"
fi

echo ""
if [[ $failures -eq 0 ]]; then
  echo "RESULT: PASS (4/4 checks)"
  exit 0
else
  echo "RESULT: FAIL ($failures check(s) failed)" >&2
  exit 1
fi
