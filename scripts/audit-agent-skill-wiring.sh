#!/usr/bin/env bash
# audit-agent-skill-wiring.sh — Camada 2 CI gate (Rule 26) for EPIC-0079
# Detects SKILL.md files that use Agent(subagent_type: "general-purpose") with
# an inline persona heuristic: "You are a (Senior|Specialist|Principal)" within
# 10 lines of the general-purpose dispatch.
# Exit codes: 0=OK, 1=INLINE_PERSONA_VIOLATION, 2=OPERATIONAL_ERROR, 3=BASELINE_CORRUPT

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DEFAULT_SKILLS_ROOT="${PROJECT_ROOT}/src/main/resources/targets/claude/skills"
DEFAULT_BASELINE="${PROJECT_ROOT}/audits/agent-skill-wiring-baseline.txt"

SKILLS_ROOT="$DEFAULT_SKILLS_ROOT"
BASELINE_FILE="$DEFAULT_BASELINE"
STRICT=false
WARN=false
SELF_CHECK=false

for arg in "$@"; do
  case "$arg" in
    --self-check) SELF_CHECK=true ;;
    --strict) STRICT=true ;;
    --warn) WARN=true ;;
    --skills-root=*) SKILLS_ROOT="${arg#--skills-root=}" ;;
    --skills-root) SKILLS_ROOT="${2:-}"; shift ;;
    --baseline=*) BASELINE_FILE="${arg#--baseline=}" ;;
    --baseline) BASELINE_FILE="${2:-}"; shift ;;
  esac
done

# ── self-check ──────────────────────────────────────────────────────────────
if [[ "$SELF_CHECK" == "true" ]]; then
  errors=0
  command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep not found on PATH" >&2; errors=$((errors+1)); }
  [[ -f "${BASH_SOURCE[0]}" ]] || { echo "OPERATIONAL_ERROR: this script file not found" >&2; errors=$((errors+1)); }
  [[ -f "$BASELINE_FILE" ]] || { echo "OPERATIONAL_ERROR: baseline file not found: $BASELINE_FILE" >&2; errors=$((errors+1)); }
  if [[ $errors -eq 0 ]]; then
    echo "audit-agent-skill-wiring.sh --self-check: OK"
    exit 0
  else
    exit 2
  fi
fi

# ── operational prerequisites ────────────────────────────────────────────────
if ! command -v grep >/dev/null 2>&1; then
  echo "OPERATIONAL_ERROR: grep not found on PATH" >&2
  exit 2
fi

if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "OPERATIONAL_ERROR: baseline file not found: $BASELINE_FILE" >&2
  exit 2
fi

# ── load baseline ────────────────────────────────────────────────────────────
declare -A BASELINE_ENTRIES
while IFS= read -r line; do
  # Skip comments and empty lines
  [[ "$line" =~ ^[[:space:]]*# ]] && continue
  [[ -z "${line// }" ]] && continue
  # Format: <skill-name>  # <reason>
  skill_name=$(echo "$line" | awk '{print $1}')
  if [[ -n "$skill_name" ]]; then
    BASELINE_ENTRIES["$skill_name"]=1
  fi
done < "$BASELINE_FILE"

# ── scan skill files ─────────────────────────────────────────────────────────
VIOLATIONS=0
SCANNED=0

while IFS= read -r skill_file; do
  SCANNED=$((SCANNED+1))
  skill_dir=$(dirname "$skill_file")
  skill_name=$(basename "$skill_dir")

  # Find line numbers of general-purpose dispatches
  while IFS= read -r match; do
    linenum=$(echo "$match" | cut -d: -f1)
    # Check lines linenum..(linenum+10) for inline persona pattern
    if sed -n "${linenum},$((linenum+10))p" "$skill_file" 2>/dev/null | \
        grep -qE '"You are a (Senior|Specialist|Principal)'; then

      # Check if grandfathered in baseline
      if [[ -n "${BASELINE_ENTRIES[$skill_name]+_}" ]]; then
        continue
      fi

      echo "INLINE_PERSONA_VIOLATION: $skill_name ($skill_file:$linenum)" >&2
      VIOLATIONS=$((VIOLATIONS+1))
    fi
  done < <(grep -n 'subagent_type: "general-purpose"' "$skill_file" 2>/dev/null || true)

done < <(find "$SKILLS_ROOT" -name "SKILL.md" | sort)

# ── report ───────────────────────────────────────────────────────────────────
echo "audit-agent-skill-wiring: scanned=$SCANNED violations=$VIOLATIONS"

if [[ $VIOLATIONS -gt 0 ]]; then
  if [[ "$WARN" == "true" ]]; then
    echo "WARN: $VIOLATIONS INLINE_PERSONA_VIOLATION(s) detected (warn mode — not blocking)"
    exit 0
  fi
  exit 1
fi

exit 0
