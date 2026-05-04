#!/usr/bin/env bash
# requires-capabilities: []
# Layer:      0 (preventive — fires during LLM turn)
# Trigger:    PreToolUse
# Event:      pre_tool_use (matcher: "Skill")
# Exit codes: 0=OK (no bypass flag), non-zero=block (bypass flag detected outside Recovery)
# Latency:    < 100ms
# Telemetry:  no direct emission
#
# enforce-no-bypass-flags.sh — PreToolUse hook for Rule 22 / Rule 24 enforcement.
#
# Registered in settings.json as PreToolUse matcher "Skill".
# Intercepts every Skill tool call and blocks invocations that carry
# --skip-* or --no-ci-watch flags on orchestrator skills outside of
# CLAUDE_RECOVERY_MODE=1.
#
# Exit codes:
#   0 — allowed (no blocked flags, or recovery mode active)
#   1 — BLOCKED: flag detected outside recovery mode
#   2 — OPERATIONAL_ERROR (jq missing, etc.)
#
# RULE-059-07 compliance:
#   - CLAUDE_RECOVERY_MODE=1 is the ONLY honoured bypass variable.
#   - CLAUDE_SKIP_AUDIT=1, CLAUDE_NO_ENFORCE=1 are ignored by design.
#
# See: story-0059-0003, Rule 22 (Skill Visibility), Rule 24 (Execution Integrity)

set -u

# --self-check mode
case "${1:-}" in
  --self-check)
    command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required" >&2; exit 2; }
    # Verify this file itself is registered in settings.json
    PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
    SETTINGS="${PROJECT_DIR}/.claude/settings.json"
    if [ -f "${SETTINGS}" ]; then
      if jq -e '.hooks.PreToolUse[]?.hooks[]?.command' "${SETTINGS}" 2>/dev/null \
          | grep -q "enforce-no-bypass-flags.sh"; then
        echo "enforce-no-bypass-flags.sh: self-check passed (registered in settings.json)"
        exit 0
      else
        echo "OPERATIONAL_ERROR: hook not registered in settings.json under PreToolUse" >&2
        exit 2
      fi
    else
      echo "OPERATIONAL_ERROR: settings.json not found at ${SETTINGS}" >&2
      exit 2
    fi
    ;;
esac

# Fail-open on missing jq
command -v jq >/dev/null 2>&1 || exit 0

# Orchestrators that accept blocked flags
ORCHESTRATORS="x-implement-story x-implement-task x-implement-epic x-fix-epic-pr x-release x-internal-verify-story"

# Blocked flag → allowed orchestrators mapping
# Format: FLAG:SKILL1,SKILL2,...
BLOCKED_FLAG_MAP=(
  "--skip-verification:x-implement-story,x-implement-task"
  "--skip-review:x-implement-story,x-implement-epic"
  "--skip-smoke:x-implement-story,x-internal-verify-story"
  "--skip-pr-comments:x-fix-epic-pr"
  "--no-ci-watch:x-implement-story,x-release"
  "--no-auto-remediation:x-implement-story"
)

# Read stdin payload
PAYLOAD=$(cat 2>/dev/null || echo "")
[ -z "$PAYLOAD" ] && exit 0

# Only intercept Skill tool calls
TOOL_NAME=$(echo "$PAYLOAD" | jq -r '.tool_name // empty' 2>/dev/null || echo "")
[ "$TOOL_NAME" = "Skill" ] || exit 0

# Extract skill name and args
TARGET_SKILL=$(echo "$PAYLOAD" | jq -r '.tool_input.skill // empty' 2>/dev/null || echo "")
TOOL_ARGS=$(echo "$PAYLOAD" | jq -r '.tool_input.args // empty' 2>/dev/null || echo "")

[ -z "$TARGET_SKILL" ] && exit 0

# Check if target is an orchestrator we enforce
is_enforced=false
for o in $ORCHESTRATORS; do
  if [ "$TARGET_SKILL" = "$o" ]; then
    is_enforced=true
    break
  fi
done
[ "$is_enforced" = "true" ] || exit 0

# Check recovery mode — the ONLY bypass variable (RULE-059-07)
RECOVERY_MODE="${CLAUDE_RECOVERY_MODE:-0}"

# Scan blocked flags
BLOCKED_FOUND=()
for entry in "${BLOCKED_FLAG_MAP[@]}"; do
  flag="${entry%%:*}"
  scoped_skills="${entry##*:}"
  # Check if this flag is scoped to the current target skill
  skill_match=false
  IFS=',' read -ra scoped_list <<< "$scoped_skills"
  for s in "${scoped_list[@]}"; do
    if [ "$TARGET_SKILL" = "$s" ]; then
      skill_match=true
      break
    fi
  done
  [ "$skill_match" = "true" ] || continue
  # Check if flag present in args
  if echo " $TOOL_ARGS " | grep -q -- " ${flag}"; then
    BLOCKED_FOUND+=("$flag")
  fi
done

# Nothing blocked
[ ${#BLOCKED_FOUND[@]} -eq 0 ] && exit 0

# Recovery mode: warn but allow
if [ "$RECOVERY_MODE" = "1" ]; then
  for flag in "${BLOCKED_FOUND[@]}"; do
    echo "WARNING: ${flag} permitido (recovery mode) em ${TARGET_SKILL}" >&2
  done
  exit 0
fi

# Block: print clear error message
{
  echo ""
  echo "⛔ BLOCKED — enforce-no-bypass-flags.sh (Rule 22 / Rule 24 PreToolUse Hook)"
  for flag in "${BLOCKED_FOUND[@]}"; do
    echo "   BLOCKED: ${flag} não permitido fora de recovery mode em '${TARGET_SKILL}'"
  done
  echo ""
  echo "   Para usar esta flag em cenário de recovery, defina:"
  echo "     export CLAUDE_RECOVERY_MODE=1"
  echo "   antes de reinvocar a skill."
  echo ""
  echo "   Refs: Rule 22 §Forbidden, Rule 24 §Non-inlining Contract, story-0059-0003"
  echo ""
} >&2

exit 1
