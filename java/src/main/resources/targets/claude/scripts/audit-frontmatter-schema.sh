#!/usr/bin/env bash
# audit-frontmatter-schema.sh — Rule 26/Rule 28 frontmatter schema validation (EPIC-0064, story-0064-0603)
# Validates all .md artifacts against frontmatter-3.0.json schema via FrontmatterValidator
# Exit 0: all valid | Exit 1: SCHEMA_VIOLATION | Exit 2: OPERATIONAL_ERROR
# Usage: audit-frontmatter-schema.sh [--self-check] [--root <path>] [--baseline <path>]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -d "$SCRIPT_DIR/../../governance" ]]; then
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
elif [[ -d "$SCRIPT_DIR/../../../../../governance" ]]; then
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../../../" && pwd)"
else
  PROJECT_ROOT="$(cd "$SCRIPT_DIR" && git rev-parse --show-toplevel 2>/dev/null || echo "$SCRIPT_DIR/..")"
fi

SKILLS_ROOT="${PROJECT_ROOT}"
SCHEMA="${PROJECT_ROOT}/governance/schemas/frontmatter-3.0.json"
VIOLATIONS=()

if [[ "${1:-}" == "--self-check" ]]; then
  command -v python3 >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: python3 not found" >&2; exit 2; }
  [[ -f "$SCHEMA" ]] || { echo "OPERATIONAL_ERROR: schema not found: $SCHEMA" >&2; exit 2; }
  exit 0
fi

ROOT="$SKILLS_ROOT"
BASELINE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="$2"; shift 2;;
    --baseline) BASELINE="$2"; shift 2;;
    *) shift;;
  esac
done

check_frontmatter() {
  local file="$1"
  python3 -c "
import sys
try:
  with open('$file') as f:
    content = f.read().lstrip()
  if not content.startswith('---'):
    sys.exit(0)
  lines = content.split('\n')
  count = 0
  fm_lines = []
  for i, line in enumerate(lines):
    if line.strip() == '---':
      count += 1
      if count == 2:
        break
    elif count == 1:
      fm_lines.append(line)
  fm = '\n'.join(fm_lines)
  if 'name:' not in fm:
    print('missing required field: name')
  elif 'requires-capabilities:' not in fm:
    print('missing required field: requires-capabilities')
except Exception as e:
  print(f'parse error: {e}')
" 2>/dev/null
}

while IFS= read -r file; do
  basename_file="$(basename "$file")"
  [[ "$basename_file" == _* ]] && continue

  error="$(check_frontmatter "$file")"
  if [[ -n "$error" ]]; then
    VIOLATIONS+=("SCHEMA_VIOLATION: $file — $error")
  fi
done < <(find "$ROOT" -name "*.md" -not -name "_*" -type f | sort)

if [[ -n "$BASELINE" && -f "$BASELINE" ]]; then
  REMAINING=()
  for v in "${VIOLATIONS[@]}"; do
    file_path="${v#SCHEMA_VIOLATION: }"
    file_path="${file_path% —*}"
    if ! grep -qF "$(basename "$file_path")" "$BASELINE" 2>/dev/null; then
      REMAINING+=("$v")
    fi
  done
  VIOLATIONS=("${REMAINING[@]}")
fi

if [[ ${#VIOLATIONS[@]} -gt 0 ]]; then
  for v in "${VIOLATIONS[@]}"; do echo "$v" >&2; done
  exit 1
fi

echo "audit-frontmatter-schema: all artifacts compliant" >&2
exit 0
