#!/usr/bin/env bash
# requires-capabilities: [governance.refinement-gate]
# enforce-refinement-gate.sh — Camada 0 PreToolUse hook (Rule 26 §Camada 0 + Rule 29)
#
# Layer:      0 (preventive — fires during LLM turn)
# Trigger:    PreToolUse
# Event:      PreToolUse (tool_name=Skill)
# Exit codes: 0=OK (allow), 33=REFINEMENT_REQUIRED (block)
# Latency:    < 500ms p95
# Telemetry:  recovery_mode_used event appended when CLAUDE_RECOVERY_MODE=1
#
# Intercepted skills:
#   x-story-implement, x-epic-implement, x-task-implement, x-epic-orchestrate
#
# Bypass exceptions:
#   1. CLAUDE_RECOVERY_MODE=1 — sole accepted bypass variable (Rule 27 §RULE-059-07)
#   2. hotfix/* branches — Rule 27 Exception 2
#   3. flowVersion=1 — Rule 19 legacy fallback (hook is no-op, warning emitted)
#   4. refinementVerdict absent on flowVersion < 2 — Rule 19 fallback
#
# Fail-open contract: jq absent, stdin malformed, or state file unreadable →
# exit 0 (allow) with a warning — avoids false blocks in degraded environments.
#
# See: Rule 29 (enforce-refinement-gate), Rule 26 §Camada 0, Rule 27 §Zero-Bypass
# Introduced by: EPIC-0069 story-0069-0005

set -uo pipefail

# ── Constants ─────────────────────────────────────────────────────────────────

HOOK_NAME="enforce-refinement-gate.sh"
EXIT_REFINEMENT_REQUIRED=33

# Orchestrators guarded by the refinement gate
GUARDED_SKILLS="x-story-implement x-epic-implement x-task-implement x-epic-orchestrate"

# ── Self-check mode (Rule 26 §self-check) ─────────────────────────────────────

case "${1:-}" in
  --self-check)
    command -v jq >/dev/null 2>&1 || {
      echo "OPERATIONAL_ERROR: jq required for ${HOOK_NAME}" >&2
      exit 2
    }
    PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
    BASELINE="${PROJECT_DIR}/governance/baselines/refinement-gate-baseline.txt"
    if [ ! -f "${BASELINE}" ]; then
      echo "OPERATIONAL_ERROR: baseline not found: ${BASELINE}" >&2
      exit 2
    fi
    echo "${HOOK_NAME}: self-check passed"
    exit 0
    ;;
esac

# ── Fail-open guard ───────────────────────────────────────────────────────────

command -v jq >/dev/null 2>&1 || exit 0

# ── Parse stdin payload ───────────────────────────────────────────────────────

PAYLOAD="${1:-}"
if [ -z "${PAYLOAD}" ] && [ ! -t 0 ]; then
  PAYLOAD=$(cat)
fi

if [ -z "${PAYLOAD}" ]; then
  exit 0
fi

TOOL_NAME=$(printf '%s' "${PAYLOAD}" | jq -r '.tool_name // empty' 2>/dev/null || true)
if [ "${TOOL_NAME}" != "Skill" ]; then
  exit 0
fi

SKILL_NAME=$(printf '%s' "${PAYLOAD}" | jq -r '.tool_input.skill // empty' 2>/dev/null || true)
if [ -z "${SKILL_NAME}" ]; then
  exit 0
fi

# ── Guard: only intercepted skills ────────────────────────────────────────────

is_guarded=false
for guarded in ${GUARDED_SKILLS}; do
  if [ "${SKILL_NAME}" = "${guarded}" ]; then
    is_guarded=true
    break
  fi
done
"${is_guarded}" || exit 0

# ── CLAUDE_RECOVERY_MODE bypass ───────────────────────────────────────────────

if [ "${CLAUDE_RECOVERY_MODE:-}" = "1" ]; then
  echo "WARN [${HOOK_NAME}] CLAUDE_RECOVERY_MODE=1 — refinement gate bypassed for ${SKILL_NAME}" >&2
  exit 0
fi

# ── Hotfix branch exception (Rule 27 Exception 2) ─────────────────────────────

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
CURRENT_BRANCH=$(git -C "${PROJECT_DIR}" symbolic-ref --short HEAD 2>/dev/null || echo "")
if [[ "${CURRENT_BRANCH}" =~ ^hotfix/ ]]; then
  exit 0
fi

# ── Resolve target story/epic ID from args ────────────────────────────────────

SKILL_ARGS=$(printf '%s' "${PAYLOAD}" | jq -r '.tool_input.args // empty' 2>/dev/null || true)
TARGET_ID=$(printf '%s' "${SKILL_ARGS}" | grep -oE '(story|epic)-[0-9]{4}(-[0-9]{4})?' | head -1 || true)

if [ -z "${TARGET_ID}" ]; then
  exit 0
fi

# ── Resolve execution-state.json path ─────────────────────────────────────────

# Try v4 layout: ai/epics/epic-XXXX-*/execution-state.json
STATE_FILE=""
if [[ "${TARGET_ID}" =~ ^story-([0-9]{4}) ]]; then
  EPIC_NUM="${BASH_REMATCH[1]}"
elif [[ "${TARGET_ID}" =~ ^epic-([0-9]{4}) ]]; then
  EPIC_NUM="${BASH_REMATCH[1]}"
else
  exit 0
fi

# v4 layout probe
STATE_FILE=$(find "${PROJECT_DIR}/ai/epics" -maxdepth 2 \
  -name "execution-state.json" \
  -path "*/epic-${EPIC_NUM}-*/*" 2>/dev/null | head -1 || true)

# v3 layout fallback
if [ -z "${STATE_FILE}" ]; then
  CANDIDATE="${PROJECT_DIR}/ai/epics/epic-${EPIC_NUM}/execution-state.json"
  [ -f "${CANDIDATE}" ] && STATE_FILE="${CANDIDATE}"
fi

if [ -z "${STATE_FILE}" ] || [ ! -f "${STATE_FILE}" ]; then
  exit 0
fi

# ── Read flowVersion ──────────────────────────────────────────────────────────

FLOW_VERSION=$(jq -r '.flowVersion // "1"' "${STATE_FILE}" 2>/dev/null || echo "1")

# Rule 19 fallback: flowVersion=1 → no-op (legacy epic, pre-EPIC-0069)
if [ "${FLOW_VERSION}" = "1" ]; then
  echo "WARN [refinementVerdict-absent] ${TARGET_ID}: flowVersion=1 — refinement gate is no-op (Rule 19 legacy fallback)" >&2
  exit 0
fi

# ── Read refinementVerdict ────────────────────────────────────────────────────

VERDICT_STATUS=$(jq -r '.refinementVerdict.status // "absent"' "${STATE_FILE}" 2>/dev/null || echo "absent")

case "${VERDICT_STATUS}" in
  "approved")
    exit 0
    ;;
  "absent"|"tbd")
    echo "REFINEMENT_REQUIRED: ${TARGET_ID} não refinada — rode /x-story-refine ${TARGET_ID} antes de implementar" >&2
    echo "WARN [refinementVerdict-tbd] ${TARGET_ID}: refinementVerdict.status=${VERDICT_STATUS} — gate bloqueando (Rule 29)" >&2
    exit ${EXIT_REFINEMENT_REQUIRED}
    ;;
  "rejected")
    BLOCKERS=$(jq -r '.refinementVerdict.blockers // [] | join("; ")' "${STATE_FILE}" 2>/dev/null || true)
    echo "REFINEMENT_REQUIRED: ${TARGET_ID} rejeitada no refinement — corrija os bloqueadores e rode /x-story-refine ${TARGET_ID}" >&2
    if [ -n "${BLOCKERS}" ]; then
      echo "  Bloqueadores: ${BLOCKERS}" >&2
    fi
    exit ${EXIT_REFINEMENT_REQUIRED}
    ;;
  *)
    echo "WARN [refinementVerdict-invalid] ${TARGET_ID}: refinementVerdict.status='${VERDICT_STATUS}' inválido — tratado como tbd (Rule 19)" >&2
    echo "REFINEMENT_REQUIRED: ${TARGET_ID} — status inválido, rode /x-story-refine ${TARGET_ID}" >&2
    exit ${EXIT_REFINEMENT_REQUIRED}
    ;;
esac
