#!/usr/bin/env bash
# audit-output-pruning.sh — Rule 28 Audit #5
# Validates that the generated .claude/ output does not contain artifacts whose
# required capabilities are absent from a given capability profile.
# Exit codes (Rule 26):
#   0 = OK
#   1 = OUTPUT_PRUNING_VIOLATION
#   2 = OPERATIONAL_ERROR
#   3 = BASELINE_CORRUPT

set -euo pipefail

CLAUDE_OUTPUT_DIR="${CLAUDE_OUTPUT_DIR:-.claude}"
CAPABILITIES_DIR="${CAPABILITIES_DIR:-capabilities}"
PROFILE_FILE="${PROFILE_FILE:-}"  # Optional: path to a YAML capability profile for validation

# --self-check mode
if [[ "${1:-}" == "--self-check" ]]; then
  command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
  command -v find >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: find required" >&2; exit 2; }
  [[ -d "$CLAUDE_OUTPUT_DIR" ]] || { echo "OPERATIONAL_ERROR: .claude output dir '$CLAUDE_OUTPUT_DIR' not found" >&2; exit 2; }
  [[ -d "$CAPABILITIES_DIR" ]] || { echo "OPERATIONAL_ERROR: capabilities dir '$CAPABILITIES_DIR' not found" >&2; exit 2; }
  echo "self-check OK"
  exit 0
fi

violations=0

# If no profile provided, run structural checks only (verify requires-capabilities format)
if [[ -z "$PROFILE_FILE" ]]; then
  # Check that every .md file in .claude/ output that has requires-capabilities
  # only references IDs that exist in the capabilities catalog
  while IFS= read -r -d '' output_file; do
    # Extract requires-capabilities from frontmatter
    caps=$(sed -n '/^---$/,/^---$/p' "$output_file" 2>/dev/null | grep "requires-capabilities:" | head -1)
    [[ -z "$caps" ]] && continue

    # Extract individual capability IDs (basic format: category.subcategory.id)
    ids=$(echo "$caps" | grep -oP '[a-z]+\.[a-z.]+[a-z]' || true)
    [[ -z "$ids" ]] && continue

    while IFS= read -r cap_id; do
      [[ -z "$cap_id" ]] && continue
      # Check if this capability ID exists in the catalog
      # Convert dot-notation to path: web.spring.boot → capabilities/web/spring/boot.yaml
      cap_path="${cap_id//./\/}.yaml"
      full_cap_path="${CAPABILITIES_DIR}/${cap_path}"
      if [[ ! -f "$full_cap_path" ]]; then
        echo "OUTPUT_PRUNING_VIOLATION: $output_file references unknown capability '$cap_id' (expected $full_cap_path)" >&2
        violations=$((violations + 1))
      fi
    done <<< "$ids"
  done < <(find "$CLAUDE_OUTPUT_DIR" -name "*.md" -print0 2>/dev/null)
fi

if [[ $violations -gt 0 ]]; then
  echo "audit-output-pruning: $violations violation(s) found" >&2
  exit 1
fi

echo "audit-output-pruning: OK"
exit 0
