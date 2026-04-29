#!/usr/bin/env bash
# audit-fragment-coherence.sh — Rule 28 Invariant 7
# Validates that:
#   (a) Every fragment-slot declared in an artifact-parent has a matching {{ slot: X }} or
#       {{ #each fragments.X }} reference in the body
#   (b) Every file with fragment-slot frontmatter has a parent artifact that declares that slot
# Exit codes (Rule 26):
#   0 = OK
#   1 = FRAGMENT_COHERENCE_VIOLATION
#   2 = OPERATIONAL_ERROR
#   3 = BASELINE_CORRUPT

set -euo pipefail

SKILLS_ROOT="${SKILLS_ROOT:-java/src/main/resources/targets/claude}"
KNOWLEDGE_ROOT="${KNOWLEDGE_ROOT:-java/src/main/resources/targets/claude/knowledge}"
BASELINE_FILE="${BASELINE_FILE:-audits/fragment-coherence-baseline.txt}"

# --self-check mode
if [[ "${1:-}" == "--self-check" ]]; then
  command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
  command -v find >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: find required" >&2; exit 2; }
  [[ -d "$SKILLS_ROOT" ]] || { echo "OPERATIONAL_ERROR: skills root '$SKILLS_ROOT' not found" >&2; exit 2; }
  echo "self-check OK"
  exit 0
fi

violations=0

# Load baseline (grandfathered files)
declare -A baseline_entries=()
if [[ -f "$BASELINE_FILE" ]]; then
  while IFS= read -r line; do
    [[ "$line" =~ ^#.*$ || -z "$line" ]] && continue
    path="${line%%#*}"
    path="${path%% }"
    baseline_entries["$path"]=1
  done < "$BASELINE_FILE"
fi

in_baseline() {
  [[ "${baseline_entries[${1:-}]+_}" == "_" ]]
}

# Check (a): fragment-slot declared in parent → body contains slot reference
while IFS= read -r -d '' parent_file; do
  # Extract fragment-slots from frontmatter
  slots=$(grep -oP "(?<=slot: )[\w-]+" "$parent_file" 2>/dev/null || true)
  [[ -z "$slots" ]] && continue

  body=$(sed '1,/^---$/d; 1,/^---$/d' "$parent_file" 2>/dev/null || cat "$parent_file")

  while IFS= read -r slot; do
    [[ -z "$slot" ]] && continue
    if ! echo "$body" | grep -qE "\{\{\s*(slot:|#each fragments\.)${slot}"; then
      if ! in_baseline "$parent_file"; then
        echo "FRAGMENT_COHERENCE_VIOLATION: $parent_file declares fragment-slot '$slot' but body has no {{ slot: $slot }} or {{ #each fragments.$slot }} reference" >&2
        violations=$((violations + 1))
      fi
    fi
  done <<< "$slots"
done < <(find "$SKILLS_ROOT" "$KNOWLEDGE_ROOT" -name "*.md" -print0 2>/dev/null)

# Check (b): fragment files have slot declaration
while IFS= read -r -d '' frag_file; do
  if grep -q "^fragment-slot:" "$frag_file" 2>/dev/null; then
    slot=$(grep -oP "(?<=slot: )[\w-]+" "$frag_file" | head -1)
    [[ -z "$slot" ]] && continue
    # Verify at least one parent declares this slot
    found_parent=0
    while IFS= read -r -d '' candidate; do
      [[ "$candidate" == "$frag_file" ]] && continue
      if grep -q "slot: ${slot}" "$candidate" 2>/dev/null; then
        found_parent=1
        break
      fi
    done < <(find "$SKILLS_ROOT" "$KNOWLEDGE_ROOT" -name "*.md" -print0 2>/dev/null)

    if [[ $found_parent -eq 0 ]]; then
      if ! in_baseline "$frag_file"; then
        echo "FRAGMENT_COHERENCE_VIOLATION: $frag_file declares fragment-slot '$slot' but no parent artifact declares this slot" >&2
        violations=$((violations + 1))
      fi
    fi
  fi
done < <(find "$SKILLS_ROOT" "$KNOWLEDGE_ROOT" -name "*.md" -print0 2>/dev/null)

if [[ $violations -gt 0 ]]; then
  echo "audit-fragment-coherence: $violations violation(s) found" >&2
  exit 1
fi

echo "audit-fragment-coherence: OK"
exit 0
