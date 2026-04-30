#!/usr/bin/env bash
# requires-capabilities: []
# Layer:      0 (preventive — fires during LLM turn)
# Trigger:    Stop
# Event:      stop
# Exit codes: 0=OK (no nudge needed), 2=CONTINUOUS_FLOW_INTERRUPT (nudge emitted)
# Latency:    < 500ms
# Telemetry:  no direct emission; reads state + NDJSON (read-only, idempotent)
#
# enforce-continuous-flow.sh — Camada 0 (Stop hook) EPIC-0068 Continuous-Flow Heartbeat.
#
# Detects when a non-interactive orchestrator stalls mid-phase (last NDJSON event
# was tool.result, phase still open, no subsequent tool.call) and emits a
# CONTINUOUS_FLOW_INTERRUPT nudge so Claude Code surfaces it to the LLM.
# Distinct from verify-story-completion.sh (Rule 24 Camada 2) — that checks
# evidence at story boundary; this checks intra-phase progress.
#
# Decision matrix (short-circuits in order):
#   (a) no execution-state.json found            → exit 0
#   (b) branch is hotfix/*                        → exit 0 (Rule 27 Exception 2)
#   (c) interactiveMode resolves to "interactive" → exit 0 (Rule 19 fallback)
#   (d) taskTracking.openTasks empty or absent    → exit 0
#   (e) last NDJSON event is finding.critical/high→ exit 0 (legitimate pause)
#   (f) last NDJSON event is error/tool.error     → exit 0 (other hook handles)
#   (g) last NDJSON event is tool.call (in-flight)→ exit 0 (still processing)
#   (h) otherwise: stall detected                 → exit 2 + nudge
#
# See: EPIC-0068 story-0068-0002 §3.3, story-0068-0003 §3.1
# See: .claude/rules/19-backward-compatibility.md §interactiveMode Field
# See: .claude/rules/26-audit-gate-lifecycle.md §Camada 0

set -euo pipefail

# ─── --self-check (Rule 26) ────────────────────────────────────────────────
case "${1:-}" in
  --self-check)
    command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required" >&2; exit 2; }
    _sc_dir="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
    [[ -d "$_sc_dir/ai/epics" ]] || [[ -d "$_sc_dir/plans" ]] \
      || { echo "OPERATIONAL_ERROR: no epic dirs found" >&2; exit 2; }
    exit 0
    ;;
esac

# ─── derive_next_mandatory_call (story-0068-0003) ─────────────────────────
# Input:  $1=SKILL.md path  $2=phase name (e.g. "Phase 3")  $3=events.ndjson path
# Output stdout: next [required] skill not yet in NDJSON, "PHASE_COMPLETE", or empty
# Exit:   0=result ready, 1=no markers/phase found (use fallback), 2=parse error
derive_next_mandatory_call() {
  local skill_md="$1" phase="$2" ndjson="$3"
  [[ -f "$skill_md" ]] || return 1
  [[ -f "$ndjson" ]]   || return 1
  local in_phase=0
  local -a required_skills=()
  while IFS= read -r line; do
    if [[ "$line" =~ ^##[[:space:]]"$phase"([[:space:]]|$) ]]; then in_phase=1; continue; fi
    [[ $in_phase -eq 1 && "$line" =~ ^##[[:space:]] ]] && break
    if [[ $in_phase -eq 1 && "$line" =~ Skill\(skill:[[:space:]]*\"([^\"]+)\".*\[required\] ]]; then
      required_skills+=("${BASH_REMATCH[1]}")
    fi
  done < "$skill_md"
  [[ ${#required_skills[@]} -eq 0 ]] && return 1
  local emitted
  emitted="$(jq -r 'select(.type=="tool.call") | .skill // .metadata.skill // ""' \
             "$ndjson" 2>/dev/null)" || return 2
  for skill in "${required_skills[@]}"; do
    grep -qxF "$skill" <<< "$emitted" || { echo "$skill"; return 0; }
  done
  echo "PHASE_COMPLETE"
  return 0
}

# ─── helpers ──────────────────────────────────────────────────────────────
resolve_interactive_mode() {
  local val
  val="$(jq -r '.interactiveMode // "interactive"' "$1" 2>/dev/null || echo "interactive")"
  case "$val" in
    non-interactive) echo "non-interactive" ;;
    *)               echo "interactive" ;;
  esac
}

find_state_file() {
  local project_dir="$1" result="" candidate
  while IFS= read -r candidate; do
    [[ -n "$candidate" && -f "$candidate" ]] || continue
    if [[ -z "$result" || "$candidate" -nt "$result" ]]; then
      result="$candidate"
    fi
  done < <(
    find "$project_dir/ai/epics" -maxdepth 3 -name "execution-state.json" 2>/dev/null
    find "$project_dir/plans"    -maxdepth 3 -name "execution-state.json" 2>/dev/null
  )
  echo "$result"
}

# ─── main — skipped when script is sourced for unit testing ───────────────
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then

  PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"

  # (b) hotfix branch guard
  BRANCH="$(git -C "$PROJECT_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")"
  [[ "$BRANCH" == hotfix/* ]] && exit 0

  # (a) find most recent execution-state.json
  STATE_FILE="$(find_state_file "$PROJECT_DIR")"
  [[ -z "$STATE_FILE" ]] && exit 0
  command -v jq >/dev/null 2>&1 || exit 0

  # (c) interactiveMode guard
  [[ "$(resolve_interactive_mode "$STATE_FILE")" != "non-interactive" ]] && exit 0

  # (d) open tasks guard
  OPEN_TASKS="$(jq -r '.taskTracking.openTasks // [] | length' "$STATE_FILE" 2>/dev/null || echo 0)"
  [[ "$OPEN_TASKS" -eq 0 ]] && exit 0

  # locate NDJSON for current story
  EPIC_DIR="$(dirname "$STATE_FILE")"
  NDJSON="$(find "$EPIC_DIR/telemetry" -maxdepth 1 -name "events.ndjson" 2>/dev/null | head -1 || true)"
  [[ -z "$NDJSON" ]] && exit 0

  # get last NDJSON event fields (read once to avoid repeated I/O)
  LAST_EVENT_JSON="$(tail -1 "$NDJSON" 2>/dev/null || echo "")"
  LAST_EVENT_TYPE="$(jq -r '.type // ""' <<< "$LAST_EVENT_JSON" 2>/dev/null || echo "")"
  LAST_EVENT_HAS_DURATION="$(jq -r 'has("durationMs")' <<< "$LAST_EVENT_JSON" 2>/dev/null || echo "false")"
  [[ -z "$LAST_EVENT_TYPE" ]] && exit 0

  # (e) legitimate pause
  [[ "$LAST_EVENT_TYPE" == "finding.critical" || "$LAST_EVENT_TYPE" == "finding.high" ]] && exit 0
  # (f) error events handled by other hooks
  [[ "$LAST_EVENT_TYPE" == "error" || "$LAST_EVENT_TYPE" == "tool.error" ]] && exit 0
  # (g) tool.call in-flight only when no durationMs; completed tool.call (has durationMs) may be a stall
  [[ "$LAST_EVENT_TYPE" == "tool.call" && "$LAST_EVENT_HAS_DURATION" != "true" ]] && exit 0

  # (h) stall detected — gather nudge info
  ORCHESTRATOR="$(jq -r '.epicId // "unknown"' "$STATE_FILE" 2>/dev/null || echo "unknown")"
  CURRENT_PHASE="$(jq -r '.taskTracking.phaseGateResults[-1].phase // "unknown"' \
                    "$STATE_FILE" 2>/dev/null || echo "unknown")"

  # Try Tool-Call Grammar derive (story-0068-0003); fall back to generic
  NEXT_CALL=""
  SKILL_MD="$(find "$PROJECT_DIR/.claude/skills" -name "SKILL.md" -path "*x-epic-implement*" \
              2>/dev/null | head -1 || true)"
  if [[ -n "$SKILL_MD" ]]; then
    NEXT_CALL="$(derive_next_mandatory_call "$SKILL_MD" "$CURRENT_PHASE" "$NDJSON" 2>/dev/null || true)"
  fi
  [[ "$NEXT_CALL" == "PHASE_COMPLETE" ]] && exit 0
  [[ -z "$NEXT_CALL" ]] && NEXT_CALL="see SKILL.md of orchestrator, ## ${CURRENT_PHASE}"

  {
    echo ""
    echo "CONTINUOUS_FLOW_INTERRUPT"
    echo "Orchestrator: $ORCHESTRATOR"
    echo "Open tasks: $OPEN_TASKS"
    echo "Phase: $CURRENT_PHASE"
    echo "Next mandatory tool call: $NEXT_CALL"
    echo "Action: emit the next tool call immediately. Do NOT generate prose."
    echo "Reference: feedback memory continuous-flow-non-interactive, Rule 24/27, EPIC-0068."
    echo ""
  } >&2

  exit 2
fi
