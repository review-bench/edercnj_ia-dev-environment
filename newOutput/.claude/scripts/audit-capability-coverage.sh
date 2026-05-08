#!/usr/bin/env bash
# audit-capability-coverage.sh — Rule 28 hard-fail gate (EPIC-0064, story-0064-0215/0602)
# Exit 0: all artifacts have requires-capabilities (v3.0 compliant)
# Exit 1: RULE_28_VIOLATION — artifact missing requires-capabilities
# Exit 2: OPERATIONAL_ERROR — jq unavailable or schema missing
# Usage: audit-capability-coverage.sh [--self-check] [--root <path>] [--baseline <path>] [--json]

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
JSON_MODE=false

# --self-check mode
if [[ "${1:-}" == "--self-check" ]]; then
  command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq not found on PATH" >&2; exit 2; }
  [[ -f "$SCHEMA" ]] || { echo "OPERATIONAL_ERROR: schema not found: $SCHEMA" >&2; exit 2; }
  [[ -f "${BASH_SOURCE[0]}" ]] || { echo "OPERATIONAL_ERROR: script self-reference broken" >&2; exit 2; }
  echo "self-check OK" >&2
  exit 0
fi

# Parse args
ROOT="$SKILLS_ROOT"
BASELINE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="$2"; shift 2;;
    --baseline) BASELINE="$2"; shift 2;;
    --json) JSON_MODE=true; shift;;
    *) shift;;
  esac
done

# Scan all .md files with frontmatter
while IFS= read -r file; do
  basename_file="$(basename "$file")"
  [[ "$basename_file" == _* ]] && continue

  if ! head -1 "$file" | grep -q "^---"; then continue; fi

  if ! awk '/^---/{count++; if(count==2)exit} /requires-capabilities/{found=1} END{exit !found}' "$file" 2>/dev/null; then
    VIOLATIONS+=("RULE_28_VIOLATION: $file: missing requires-capabilities field")
  fi
done < <(find "$ROOT" -name "*.md" -not -name "_*" -type f | sort)

# Check baseline exemptions
if [[ -n "$BASELINE" && -f "$BASELINE" ]]; then
  REMAINING=()
  for v in "${VIOLATIONS[@]}"; do
    file_path="${v#RULE_28_VIOLATION: }"
    file_path="${file_path%: missing*}"
    rel_path="${file_path#"${ROOT}/"}"
    if ! grep -qF "$rel_path" "$BASELINE" 2>/dev/null; then
      REMAINING+=("$v")
    fi
  done
  VIOLATIONS=("${REMAINING[@]:-}")
fi

if "$JSON_MODE"; then
  # JSON output for CI consumers
  echo -n '{"violations":['
  first=true
  for v in "${VIOLATIONS[@]:-}"; do
    [[ -z "$v" ]] && continue
    $first || echo -n ','
    file_path="${v#RULE_28_VIOLATION: }"
    file_path="${file_path%: missing*}"
    reason="${v#*: missing}"
    printf '{"file":"%s","reason":"missing%s"}' "$file_path" "$reason"
    first=false
  done
  echo -n '],'
  printf '"warnings":[],"summary":{"total_violations":%d}}' "${#VIOLATIONS[@]}"
  echo
fi

if [[ ${#VIOLATIONS[@]} -gt 0 ]]; then
  if ! "$JSON_MODE"; then
    for v in "${VIOLATIONS[@]}"; do
      echo "$v" >&2
    done
  fi
  exit 1
fi

if ! "$JSON_MODE"; then
  echo "audit-capability-coverage: all artifacts compliant (v3.0)" >&2
fi
exit 0
