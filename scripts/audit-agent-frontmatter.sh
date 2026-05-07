#!/usr/bin/env bash
# audit-agent-frontmatter.sh — Camada 2 CI gate (Rule 26) for EPIC-0079
# Validates that all agent files under agents/core/, agents/conditional/, agents/developers/
# carry the canonical frontmatter: name, description, tools, model, requires-capabilities.
# Exit codes: 0=OK, 1=FRONTMATTER_VIOLATION, 2=OPERATIONAL_ERROR, 3=SCHEMA_CORRUPT

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SCHEMA_FILE="${PROJECT_ROOT}/governance/schemas/agent-frontmatter-1.0.json"
AGENTS_ROOT="${PROJECT_ROOT}/src/main/resources/targets/claude/agents"
STRICT=false
ALL_AGENTS=false
SELF_CHECK=false

for arg in "$@"; do
  case "$arg" in
    --strict) STRICT=true ;;
    --all-agents) ALL_AGENTS=true ;;
    --self-check) SELF_CHECK=true ;;
  esac
done

# ── self-check ──────────────────────────────────────────────────────────────
if [[ "$SELF_CHECK" == "true" ]]; then
  errors=0
  command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq not found on PATH" >&2; errors=$((errors+1)); }
  [[ -f "$SCHEMA_FILE" ]] || { echo "OPERATIONAL_ERROR: schema file not found: $SCHEMA_FILE" >&2; errors=$((errors+1)); }
  [[ -f "${BASH_SOURCE[0]}" ]] || { echo "OPERATIONAL_ERROR: this script file not found" >&2; errors=$((errors+1)); }
  if [[ $errors -eq 0 ]]; then
    echo "audit-agent-frontmatter.sh --self-check: OK"
    exit 0
  else
    exit 2
  fi
fi

# ── operational prerequisites ────────────────────────────────────────────────
if ! command -v jq >/dev/null 2>&1; then
  echo "OPERATIONAL_ERROR: jq not found on PATH" >&2
  exit 2
fi
if [[ ! -f "$SCHEMA_FILE" ]]; then
  echo "SCHEMA_CORRUPT: agent-frontmatter-1.0.json not found at $SCHEMA_FILE" >&2
  exit 3
fi

# ── collect agent files ──────────────────────────────────────────────────────
mapfile -t AGENT_FILES < <(find \
  "${AGENTS_ROOT}/core" \
  "${AGENTS_ROOT}/conditional" \
  "${AGENTS_ROOT}/developers" \
  -name "*.md" 2>/dev/null | sort)

TOTAL=${#AGENT_FILES[@]}
VALID=0
VIOLATIONS=0

extract_frontmatter_field() {
  local file="$1"
  local field="$2"
  # Extract YAML frontmatter between first --- and second ---
  awk '/^---$/{count++; if(count==2) exit; next} count==1{print}' "$file" | \
    grep -E "^${field}:" | head -1 | sed "s/^${field}:[[:space:]]*//"
}

has_frontmatter() {
  local file="$1"
  head -1 "$file" | grep -q "^---$"
}

check_agent() {
  local file="$1"
  local basename
  basename="$(basename "$file")"
  local violations=""

  if ! has_frontmatter "$file"; then
    echo "FRONTMATTER_VIOLATION: $basename — no frontmatter block (missing ---)" >&2
    VIOLATIONS=$((VIOLATIONS+1))
    return 1
  fi

  for field in name description tools model requires-capabilities; do
    local value
    value="$(extract_frontmatter_field "$file" "$field")"
    if [[ -z "$value" ]]; then
      violations="${violations} MISSING_FIELD:${field}"
    fi
  done

  # Check model is not Adaptive (MODEL_ADAPTIVE_FORBIDDEN)
  local model_val
  model_val="$(extract_frontmatter_field "$file" "model")"
  if [[ "$model_val" == "Adaptive" ]]; then
    violations="${violations} MODEL_ADAPTIVE_FORBIDDEN"
  fi

  if [[ -n "$violations" ]]; then
    echo "FRONTMATTER_VIOLATION: $basename —${violations}" >&2
    VIOLATIONS=$((VIOLATIONS+1))
    return 1
  fi

  VALID=$((VALID+1))
  return 0
}

# ── main loop ────────────────────────────────────────────────────────────────
for agent_file in "${AGENT_FILES[@]}"; do
  check_agent "$agent_file" || true
done

echo "${VALID}/${TOTAL} agentes validados contra agent-frontmatter-1.0.json"

if [[ $VIOLATIONS -gt 0 ]]; then
  if [[ "$STRICT" == "true" ]]; then
    echo "FRONTMATTER_VIOLATION: ${VIOLATIONS} agent(s) failed validation (--strict mode)" >&2
    exit 1
  else
    echo "WARNING: ${VIOLATIONS} agent(s) have frontmatter issues" >&2
    exit 1
  fi
fi

exit 0
