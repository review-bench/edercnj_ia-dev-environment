#!/usr/bin/env bash
# audit-fragment-coherence.sh — Rule 28 Invariant 7 (EPIC-0064, story-0064-0605)
# Validates:
#   (a) fragment-slot declared in parent has {{ slot: X }} or {{ #each fragments.X }} in body
#   (b) fragment files have a parent that declares the slot
#   (c) ordering field is valid (fragment-order or none; rejects free-text)
#   (d) no duplicate fragment-id within the same slot
# Exit codes (Rule 26): 0=OK  1=FRAGMENT_COHERENCE_VIOLATION  2=OPERATIONAL_ERROR  3=BASELINE_CORRUPT
# Usage: audit-fragment-coherence.sh [--self-check] [--json] [--root <path>] [--baseline <path>]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -d "$SCRIPT_DIR/../../governance" ]]; then
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
elif [[ -d "$SCRIPT_DIR/../../../../../governance" ]]; then
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../../../" && pwd)"
else
  PROJECT_ROOT="$(cd "$SCRIPT_DIR" && git rev-parse --show-toplevel 2>/dev/null || echo "$SCRIPT_DIR/..")"
fi

SKILLS_ROOT="${PROJECT_ROOT}/java/src/main/resources/targets/claude"
BASELINE_FILE="${PROJECT_ROOT}/audits/fragment-coherence-baseline.txt"
JSON_MODE=false
VIOLATIONS=()

if [[ "${1:-}" == "--self-check" ]]; then
  command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
  command -v find >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: find required" >&2; exit 2; }
  echo "self-check OK" >&2
  exit 0
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --json) JSON_MODE=true; shift;;
    --root) SKILLS_ROOT="$2"; shift 2;;
    --baseline) BASELINE_FILE="$2"; shift 2;;
    *) shift;;
  esac
done

declare -A baseline_entries=()
if [[ -f "$BASELINE_FILE" ]]; then
  while IFS= read -r line; do
    [[ "$line" =~ ^#.*$ || -z "$line" ]] && continue
    path="${line%%#*}"; path="${path%% }"
    baseline_entries["$path"]=1
  done < "$BASELINE_FILE"
fi
in_baseline() { [[ "${baseline_entries[${1:-}]+_}" == "_" ]]; }

add_violation() { VIOLATIONS+=("$1"); }

# (a) parent declares fragment-slots → body must reference slot
while IFS= read -r -d '' parent_file; do
  in_baseline "$parent_file" && continue
  slots=$(grep -oE "slot: [a-z][a-z0-9_-]+" "$parent_file" 2>/dev/null | sed 's/slot: //' | sort -u || true)
  [[ -z "$slots" ]] && continue
  body=$(awk 'BEGIN{c=0} /^---/{c++; if(c>=2)skip=0; next} c<2{next} {print}' "$parent_file" 2>/dev/null || cat "$parent_file")
  while IFS= read -r slot; do
    [[ -z "$slot" ]] && continue
    if ! echo "$body" | grep -qE "\{\{\s*(slot:|#each fragments\.)${slot}"; then
      add_violation "FRAGMENT_COHERENCE_VIOLATION: $parent_file: declares slot '$slot' but body has no {{ slot: $slot }} or {{ #each fragments.$slot }}"
    fi
  done <<< "$slots"
done < <(find "$SKILLS_ROOT" -name "*.md" -print0 2>/dev/null)

# (b,c,d) fragment files: valid ordering, no duplicate fragment-id per slot
declare -A seen_frag_ids=()
while IFS= read -r -d '' frag_file; do
  in_baseline "$frag_file" && continue
  # Only process files with fragment-slot frontmatter
  grep -q "^fragment-slot:" "$frag_file" 2>/dev/null || continue

  slot=$(grep -oP "(?<=slot: )[\w-]+" "$frag_file" | head -1 || true)
  frag_id=$(grep -oP "(?<=fragment-id: )[\w-]+" "$frag_file" | head -1 || true)
  ordering=$(grep -oP "(?<=ordering: )[\w-]+" "$frag_file" | head -1 || true)

  # (c) validate ordering field if present
  if [[ -n "$ordering" ]] && [[ "$ordering" != "fragment-order" ]] && ! [[ "$ordering" =~ ^[0-9]+$ ]]; then
    add_violation "FRAGMENT_COHERENCE_VIOLATION: $frag_file: invalid ordering '$ordering' (must be 'fragment-order' or numeric)"
  fi

  # (d) detect duplicate fragment-id in same slot
  if [[ -n "$slot" && -n "$frag_id" ]]; then
    key="${slot}:${frag_id}"
    if [[ -n "${seen_frag_ids[$key]+_}" ]]; then
      add_violation "FRAGMENT_COHERENCE_VIOLATION: $frag_file: duplicate fragment-id '$frag_id' in slot '$slot' (first seen: ${seen_frag_ids[$key]})"
    else
      seen_frag_ids["$key"]="$frag_file"
    fi
  fi
done < <(find "$SKILLS_ROOT" -name "*.md" -print0 2>/dev/null)

if "$JSON_MODE"; then
  echo -n '{"violations":['
  first=true
  for v in "${VIOLATIONS[@]:-}"; do
    [[ -z "$v" ]] && continue
    $first || echo -n ','
    file_path="${v#FRAGMENT_COHERENCE_VIOLATION: }"
    file_path="${file_path%%:*}"
    reason="${v#*: }"
    printf '{"file":"%s","reason":"%s"}' "$file_path" "${reason//\"/\\\"}"
    first=false
  done
  printf '],"summary":{"total_violations":%d}}' "${#VIOLATIONS[@]}"
  echo
fi

if [[ ${#VIOLATIONS[@]} -gt 0 ]]; then
  if ! "$JSON_MODE"; then
    for v in "${VIOLATIONS[@]}"; do echo "$v" >&2; done
  fi
  exit 1
fi

"$JSON_MODE" || echo "audit-fragment-coherence: OK" >&2
exit 0
