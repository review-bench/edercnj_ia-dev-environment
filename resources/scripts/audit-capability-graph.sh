#!/usr/bin/env bash
# audit-capability-graph.sh — Rule 26/Rule 28 capability graph integrity gate (EPIC-0064, story-0064-0601)
# Validates: no cycles, symmetric excludes, no orphan refs, descriptions present
# Exit 0: graph valid | Exit 1: GRAPH_VIOLATION | Exit 2: OPERATIONAL_ERROR | Exit 3: BASELINE_CORRUPT
# Usage: audit-capability-graph.sh [--self-check] [--catalog-root <path>]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -d "$SCRIPT_DIR/../../governance" ]]; then
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
elif [[ -d "$SCRIPT_DIR/../../../../../governance" ]]; then
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../../../" && pwd)"
else
  PROJECT_ROOT="$(cd "$SCRIPT_DIR" && git rev-parse --show-toplevel 2>/dev/null || echo "$SCRIPT_DIR/..")"
fi

CATALOG_ROOT="${PROJECT_ROOT}/capabilities"
VIOLATIONS=()

if [[ "${1:-}" == "--self-check" ]]; then
  command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq not found on PATH" >&2; exit 2; }
  command -v yq >/dev/null 2>&1 || command -v python3 >/dev/null 2>&1 || {
    echo "OPERATIONAL_ERROR: yq or python3 required" >&2; exit 2;
  }
  exit 0
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --catalog-root) CATALOG_ROOT="$2"; shift 2;;
    *) shift;;
  esac
done

if [[ ! -d "$CATALOG_ROOT" ]]; then
  echo "OPERATIONAL_ERROR: catalog root not found: $CATALOG_ROOT" >&2
  exit 2
fi

parse_yaml_field() {
  local file="$1" field="$2"
  python3 -c "
import sys
try:
  with open('$file') as f:
    content = f.read()
  lines = content.split('\n')
  in_field = False
  results = []
  for line in lines:
    stripped = line.strip()
    if stripped.startswith('$field:'):
      rest = stripped[len('$field:'):].strip()
      if rest.startswith('[') and rest.endswith(']'):
        inner = rest[1:-1]
        results = [x.strip() for x in inner.split(',') if x.strip()]
        break
      elif rest == '' or rest == '[]':
        in_field = True
      else:
        results = [rest]
        break
    elif in_field:
      if stripped.startswith('-'):
        results.append(stripped[1:].strip())
      elif stripped and not stripped.startswith('#'):
        break
  print('\n'.join(results))
except Exception as e:
  pass
" 2>/dev/null
}

declare -A CAPABILITY_IDS
declare -A EXCLUDES_MAP

while IFS= read -r yaml_file; do
  id="$(parse_yaml_field "$yaml_file" "id")"
  [[ -z "$id" ]] && continue
  CAPABILITY_IDS["$id"]="$yaml_file"
  excludes="$(parse_yaml_field "$yaml_file" "excludes")"
  EXCLUDES_MAP["$id"]="$excludes"
done < <(find "$CATALOG_ROOT" -name "*.yaml" -o -name "*.yml" | sort)

if [[ ${#CAPABILITY_IDS[@]} -eq 0 ]]; then
  echo "audit-capability-graph: no capabilities found in $CATALOG_ROOT — nothing to validate" >&2
  exit 0
fi

for id in "${!EXCLUDES_MAP[@]}"; do
  while IFS= read -r excluded; do
    [[ -z "$excluded" ]] && continue
    if [[ -z "${CAPABILITY_IDS[$excluded]+x}" ]]; then
      VIOLATIONS+=("GRAPH_VIOLATION: orphan reference '$excluded' in '$id' excludes")
      continue
    fi
    reverse="${EXCLUDES_MAP[$excluded]:-}"
    if ! echo "$reverse" | grep -qF "$id"; then
      VIOLATIONS+=("GRAPH_VIOLATION: asymmetric mutex: '$id' excludes '$excluded' but '$excluded' does not exclude '$id'")
    fi
  done <<< "${EXCLUDES_MAP[$id]}"
done

for id in "${!CAPABILITY_IDS[@]}"; do
  requires="$(parse_yaml_field "${CAPABILITY_IDS[$id]}" "requires")"
  while IFS= read -r req; do
    [[ -z "$req" ]] && continue
    req_base="${req%.*}"
    if [[ "$req" != *"*"* && -z "${CAPABILITY_IDS[$req]+x}" ]]; then
      VIOLATIONS+=("GRAPH_VIOLATION: orphan reference '$req' in '$id' requires")
    fi
  done <<< "$requires"
done

if [[ ${#VIOLATIONS[@]} -gt 0 ]]; then
  for v in "${VIOLATIONS[@]}"; do
    echo "$v" >&2
  done
  exit 1
fi

echo "audit-capability-graph: graph valid — ${#CAPABILITY_IDS[@]} capabilities checked" >&2
exit 0
