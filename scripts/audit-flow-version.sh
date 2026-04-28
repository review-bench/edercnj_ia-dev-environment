#!/usr/bin/env bash
# scripts/audit-flow-version.sh
#
# Audits all execution-state.json files under plans/epic-*/ for:
#   1. flowVersion field is present and in {"1", "2", "4"} (EPIC-0062 adds "4")
#   2. If flowVersion=2: taskTracking.enabled MUST be true
#   3. If epic branch epic/XXXX exists on remote: flowVersion MUST be "2" or "4"
#
# Exit codes:
#   0  OK — no violations
#   1  FLOW_VERSION_VIOLATION — at least one violation detected
#   2  OPERATIONAL_ERROR — jq not on PATH or plans/ directory missing
#
# Rule 19 (Backward Compatibility) — EPIC-0059 (Zero-Bypass Lifecycle Enforcement)
# Rule 26 (Audit Gate Lifecycle) — naming convention: audit-{subject}.sh
#
# Usage:
#   scripts/audit-flow-version.sh [--self-check] [--plans-root <path>]

set -euo pipefail

# ── self-check ────────────────────────────────────────────────────────────────
case "${1:-}" in
  --self-check)
    command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required on PATH" >&2; exit 2; }
    [[ -d "plans" ]] || { echo "OPERATIONAL_ERROR: plans/ directory not found (run from repo root)" >&2; exit 2; }
    exit 0
    ;;
esac

# ── argument parsing ──────────────────────────────────────────────────────────
PLANS_ROOT="plans"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --plans-root)
      PLANS_ROOT="$2"
      shift 2
      ;;
    --plans-root=*)
      PLANS_ROOT="${1#--plans-root=}"
      shift
      ;;
    *)
      echo "OPERATIONAL_ERROR: Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

# ── dependency checks ─────────────────────────────────────────────────────────
command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required on PATH" >&2; exit 2; }

if [[ ! -d "$PLANS_ROOT" ]]; then
  echo "OPERATIONAL_ERROR: Plans root directory not found: $PLANS_ROOT" >&2
  exit 2
fi

# ── audit ─────────────────────────────────────────────────────────────────────
VIOLATIONS=0

check_remote_branch() {
  local branch="$1"
  git ls-remote --heads origin "$branch" 2>/dev/null | grep -q "$branch" && return 0 || return 1
}

while IFS= read -r -d '' state_file; do
  epic_dir=$(dirname "$state_file")
  epic_id=$(basename "$epic_dir")  # e.g., epic-0059

  # Parse flowVersion
  flow_version=$(jq -r '.flowVersion // empty' "$state_file" 2>/dev/null)

  # ── Check 1: flowVersion must be in {"1", "2", "4"} ──────────────────────
  # "4" added by EPIC-0062 (story-0062-0001) — v4 layout (ai/epics/) per Rule 19.
  if [[ -z "$flow_version" ]]; then
    # Absent = legacy "1" — OK per Rule 19
    flow_version="1"
  elif [[ "$flow_version" != "1" && "$flow_version" != "2" && "$flow_version" != "4" ]]; then
    echo "FLOW_VERSION_VIOLATION: $state_file has flowVersion=${flow_version}; expected \"1\", \"2\", or \"4\"" >&2
    ((VIOLATIONS++)) || true
    continue
  fi

  # ── Check 2: epic branch presence implies flowVersion=2 ───────────────────
  epic_num="${epic_id#epic-}"  # e.g., "0059"
  epic_branch="epic/${epic_num}"

  if [[ "$flow_version" == "1" ]] && check_remote_branch "$epic_branch"; then
    # Check if --legacy-flow was recorded in metadata
    legacy_flow=$(jq -r '.legacyFlow // false' "$state_file" 2>/dev/null)
    if [[ "$legacy_flow" != "true" ]]; then
      echo "FLOW_VERSION_VIOLATION: $state_file has flowVersion=1 but epic branch ${epic_branch} exists on remote; set flowVersion=\"2\" or \"4\" or record legacyFlow=true" >&2
      ((VIOLATIONS++)) || true
    fi
  fi

  # ── Check 3: flowVersion=2 requires taskTracking.enabled=true (EPIC-0059) ─
  if [[ "$flow_version" == "2" ]]; then
    task_tracking_present=$(jq 'has("taskTracking")' "$state_file" 2>/dev/null)
    # Use explicit string comparison to handle jq boolean "false" correctly
    task_tracking_enabled=$(jq -r 'if has("taskTracking") then (.taskTracking.enabled | tostring) else "absent" end' "$state_file" 2>/dev/null)

    if [[ "$task_tracking_present" == "false" || "$task_tracking_enabled" == "absent" || "$task_tracking_enabled" == "null" ]]; then
      echo "FLOW_VERSION_VIOLATION: $state_file has flowVersion=2 but taskTracking is absent; taskTracking required for flowVersion=2 — run scripts/migrate-task-tracking-v2.sh" >&2
      ((VIOLATIONS++)) || true
    elif [[ "$task_tracking_enabled" == "false" ]]; then
      echo "WARN [taskTracking-flowVersion2]: $state_file has flowVersion=2 but taskTracking.enabled=false; this is suspicious — all flowVersion=2 epics should have full tracking active" >&2
      echo "  Run scripts/migrate-task-tracking-v2.sh to migrate, or set taskTracking.enabled=true explicitly." >&2
      # WARN only in first release per Rule 19 / story-0059-0012 §5.2
      # (Uncomment the following lines to promote to FAIL in second release):
      # echo "FLOW_VERSION_VIOLATION: $state_file has flowVersion=2 + taskTracking.enabled=false" >&2
      # ((VIOLATIONS++)) || true
    fi
  fi

done < <(find "$PLANS_ROOT" -name "execution-state.json" -print0 | sort -z)

# ── result ────────────────────────────────────────────────────────────────────
if [[ "$VIOLATIONS" -gt 0 ]]; then
  echo ""
  echo "audit-flow-version.sh: FAILED — ${VIOLATIONS} violation(s) detected" >&2
  exit 1
fi

echo "audit-flow-version.sh: OK — all execution-state.json files pass flowVersion audit"
exit 0
