#!/usr/bin/env bash
# requires-capabilities: []
# enforce-preflight-gates.sh — Camada 0 PreToolUse hook (Rule 26 §Camada 0)
#
# Layer:      0 (preventive — fires during LLM turn)
# Trigger:    PreToolUse
# Event:      PreToolUse (tool_name=Bash or Skill)
# Exit codes: 0=OK (allow), 2=BLOCKED (surfaced to LLM as blocking notification)
# Latency:    < 500ms (preflight target < 100ms for non-intercepted calls)
# Telemetry:  recovery_mode_used event appended to NDJSON when CLAUDE_RECOVERY_MODE=1
#
# Intercepts:
#   Bash tool calls:
#     - git push origin feat/story-* | feat/task-* | feat/epic-* | fix/* | hotfix/*
#     - gh pr create --base epic/* (story-level PR)
#     - gh pr create --base develop (epic-level PR)
#     - git commit -n | git commit --no-verify (bypass blocked unconditionally)
#     - gh pr merge --admin (bypass blocked unconditionally)
#   Skill tool calls:
#     - skill=x-create-pr
#
# Fail-CLOSED contract (RULE-005): if preflight script is absent or stdin is malformed,
# exit 2 (block) — never fail-open for intercepted tool calls.
#
# CLAUDE_RECOVERY_MODE=1 is the ONLY bypass variable (RULE-004, Rule 27 §RULE-059-07).
# All other env vars (CLAUDE_SKIP_AUDIT, CLAUDE_NO_ENFORCE, etc.) are IGNORED.
#
# See: story-0063-0004, Rule 26 §Camada 0, Rule 27 §Zero-Bypass Lifecycle

set -uo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
PREFLIGHT="${PROJECT_DIR}/scripts/preflight.sh"

# ── Helpers ───────────────────────────────────────────────────────────────────

# Resolve the active NDJSON telemetry file for a given epic_id (v4 then v3 layout).
resolve_ndjson_path() {
    local epic_id="${1:-}"
    if [ -z "$epic_id" ]; then
        # Try to resolve from branch
        local branch
        branch=$(git -C "$PROJECT_DIR" symbolic-ref HEAD 2>/dev/null | sed 's|refs/heads/||' || echo "")
        if [[ "$branch" =~ feat/story-([0-9]{4}) ]]; then
            epic_id="${BASH_REMATCH[1]}"
        elif [[ "$branch" =~ feat/task-([0-9]{4}) ]]; then
            epic_id="${BASH_REMATCH[1]}"
        elif [[ "$branch" =~ epic/([0-9]{4}) ]]; then
            epic_id="${BASH_REMATCH[1]}"
        fi
    fi

    if [ -n "$epic_id" ]; then
        # v4 layout: ai/epics/epic-XXXX-*/telemetry/events.ndjson
        local v4_path
        v4_path=$(find "${PROJECT_DIR}/ai/epics" -maxdepth 3 \
            -name "events.ndjson" \
            -path "*/epic-${epic_id}-*/telemetry/*" 2>/dev/null | head -1 || echo "")
        if [ -n "$v4_path" ]; then
            echo "$v4_path"
            return 0
        fi

        # v3 layout: ai/epics/epic-XXXX/telemetry/events.ndjson
        local v3_path="${PROJECT_DIR}/ai/epics/epic-${epic_id}/telemetry/events.ndjson"
        if [ -f "$v3_path" ]; then
            echo "$v3_path"
            return 0
        fi
    fi

    # Fallback
    echo "${PROJECT_DIR}/plans/unknown/telemetry/events.ndjson"
}

# Derive story/epic ID from current git branch
# Sets: DERIVED_SCOPE, DERIVED_STORY_ID, DERIVED_EPIC_ID
derive_ids_from_branch() {
    DERIVED_SCOPE=""
    DERIVED_STORY_ID=""
    DERIVED_EPIC_ID=""

    local branch
    branch=$(git -C "$PROJECT_DIR" symbolic-ref HEAD 2>/dev/null | sed 's|refs/heads/||' || echo "")
    [ -z "$branch" ] && return 0

    # story branch: feat/story-XXXX-YYYY[-suffix]
    if [[ "$branch" =~ ^feat/story-([0-9]{4})-([0-9]{4})(-.*)?$ ]]; then
        DERIVED_SCOPE="story"
        DERIVED_STORY_ID="${BASH_REMATCH[1]}-${BASH_REMATCH[2]}"
        DERIVED_EPIC_ID="${BASH_REMATCH[1]}"
        return 0
    fi

    # task branch: feat/task-XXXX-YYYY-NNN[-suffix]
    if [[ "$branch" =~ ^feat/task-([0-9]{4})-([0-9]{4})-([0-9]{3})(-.*)?$ ]]; then
        DERIVED_SCOPE="story"
        DERIVED_STORY_ID="${BASH_REMATCH[1]}-${BASH_REMATCH[2]}"
        DERIVED_EPIC_ID="${BASH_REMATCH[1]}"
        return 0
    fi

    # fix branch: fix/XXXX-YYYY[-suffix]
    if [[ "$branch" =~ ^fix/([0-9]{4})-([0-9]{4})(-.*)?$ ]]; then
        DERIVED_SCOPE="story"
        DERIVED_STORY_ID="${BASH_REMATCH[1]}-${BASH_REMATCH[2]}"
        DERIVED_EPIC_ID="${BASH_REMATCH[1]}"
        return 0
    fi

    # hotfix branch: hotfix/XXXX-YYYY[-suffix]
    if [[ "$branch" =~ ^hotfix/([0-9]{4})-([0-9]{4})(-.*)?$ ]]; then
        DERIVED_SCOPE="story"
        DERIVED_STORY_ID="${BASH_REMATCH[1]}-${BASH_REMATCH[2]}"
        DERIVED_EPIC_ID="${BASH_REMATCH[1]}"
        return 0
    fi

    # epic branch: epic/XXXX
    if [[ "$branch" =~ ^epic/([0-9]{4})$ ]]; then
        DERIVED_SCOPE="epic"
        DERIVED_EPIC_ID="${BASH_REMATCH[1]}"
        return 0
    fi

    # Branch not matched → no-op zone (main, develop, custom branches)
    return 0
}

# Block with a structured error message
block_with_message() {
    local reason="$1"
    local detail="${2:-}"
    echo "" >&2
    echo "⛔ BLOCKED — enforce-preflight-gates.sh (Rule 26 Camada 0 / EPIC-0063)" >&2
    echo "   PREFLIGHT_FAILED: ${reason}" >&2
    if [ -n "$detail" ]; then
        echo "   Detail: ${detail}" >&2
    fi
    echo "" >&2
    echo "   To bypass for recovery purposes only, set:" >&2
    echo "     export CLAUDE_RECOVERY_MODE=1" >&2
    echo "   Refs: Rule 27 §RULE-059-07, story-0063-0004" >&2
    echo "" >&2
    exit 2
}

# Emit a recovery_mode_used telemetry event
emit_recovery_event() {
    local tool_pattern="$1"
    local branch="$2"
    local ndjson_path
    ndjson_path=$(resolve_ndjson_path "" || echo "")
    if [ -n "$ndjson_path" ]; then
        local dir
        dir="$(dirname "$ndjson_path")"
        mkdir -p "$dir" 2>/dev/null || true
        printf '{"timestamp":"%s","event":"recovery_mode_used","tool":"%s","branch":"%s","pid":%d}\n' \
            "$(date -u +%FT%TZ 2>/dev/null || echo "unknown")" \
            "$tool_pattern" "$branch" "$$" >> "$ndjson_path" 2>/dev/null || true
    fi
}

# ── Main ──────────────────────────────────────────────────────────────────────

# Require jq
command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required for enforce-preflight-gates.sh" >&2; exit 2; }

# Read and parse stdin
PAYLOAD=$(cat 2>/dev/null || echo "")

# Validate JSON — fail-CLOSED on malformed stdin (RULE-005)
if ! echo "$PAYLOAD" | jq empty 2>/dev/null; then
    echo "OPERATIONAL_ERROR: invalid stdin JSON — tool call blocked (fail-CLOSED)" >&2
    exit 2
fi

# Extract tool_name
TOOL_NAME=$(echo "$PAYLOAD" | jq -r '.tool_name // empty' 2>/dev/null || echo "")
[ -z "$TOOL_NAME" ] && exit 0  # No tool_name means not a real PreToolUse payload

# ── Bypass-vector detection (unconditional — even CLAUDE_RECOVERY_MODE cannot bypass) ──
# git commit -n or git commit --no-verify
if [ "$TOOL_NAME" = "Bash" ]; then
    CMD=$(echo "$PAYLOAD" | jq -r '.tool_input.command // empty' 2>/dev/null || echo "")
    if echo "$CMD" | grep -qE '^\s*git commit\b.*(-n\b|--no-verify\b)'; then
        echo "" >&2
        echo "⛔ BLOCKED — enforce-preflight-gates.sh (bypass-vector detection)" >&2
        echo "   BYPASS_FLAG_BLOCKED: --no-verify detected in 'git commit' command" >&2
        echo "   Bypass flags are prohibited — use 'git commit' without -n/--no-verify" >&2
        echo "   Refs: Rule 24 §Forbidden, Rule 27 §Forbidden, story-0063-0004" >&2
        echo "" >&2
        exit 2
    fi

    # gh pr merge --admin
    if echo "$CMD" | grep -qE '^\s*gh pr merge\b.*--admin\b'; then
        echo "" >&2
        echo "⛔ BLOCKED — enforce-preflight-gates.sh (bypass-vector detection)" >&2
        echo "   BYPASS_FLAG_BLOCKED: --admin detected in 'gh pr merge' command" >&2
        echo "   Force-merge is prohibited — use standard PR merge process" >&2
        echo "   Refs: Rule 27 §Forbidden, story-0063-0004" >&2
        echo "" >&2
        exit 2
    fi
fi

# ── Pattern matching — determine if this call should trigger preflight ──

SHOULD_INTERCEPT=false
INTERCEPT_SCOPE=""
INTERCEPT_CONTEXT=""

if [ "$TOOL_NAME" = "Bash" ]; then
    CMD=$(echo "$PAYLOAD" | jq -r '.tool_input.command // empty' 2>/dev/null || echo "")

    # Pattern 1: git push origin feat/story-* | feat/task-* | feat/epic-* | fix/* | hotfix/*
    if echo "$CMD" | grep -qE 'git push\b.*\borigin\b.*(feat/story-|feat/task-|feat/epic-|fix/|hotfix/)'; then
        SHOULD_INTERCEPT=true
        INTERCEPT_CONTEXT="git-push"
    fi

    # Pattern 2: gh pr create --base epic/...  (story-level PR)
    if echo "$CMD" | grep -qE 'gh pr create\b.*--base[ =]epic/'; then
        SHOULD_INTERCEPT=true
        INTERCEPT_CONTEXT="gh-pr-create-epic-base"
    fi

    # Pattern 3: gh pr create --base develop (epic-level PR — when on an epic branch)
    if echo "$CMD" | grep -qE 'gh pr create\b.*--base[ =]develop'; then
        SHOULD_INTERCEPT=true
        INTERCEPT_CONTEXT="gh-pr-create-develop-base"
    fi

elif [ "$TOOL_NAME" = "Skill" ]; then
    SKILL_NAME=$(echo "$PAYLOAD" | jq -r '.tool_input.skill // empty' 2>/dev/null || echo "")

    # Pattern 4: Skill x-create-pr
    if [ "$SKILL_NAME" = "x-create-pr" ]; then
        SHOULD_INTERCEPT=true
        INTERCEPT_CONTEXT="skill-x-pr-create"
    fi
fi

# No interception needed — exit 0 (allow)
[ "$SHOULD_INTERCEPT" = "true" ] || exit 0

# ── Derive context from branch ──

derive_ids_from_branch

CURRENT_BRANCH=$(git -C "$PROJECT_DIR" symbolic-ref HEAD 2>/dev/null | sed 's|refs/heads/||' || echo "unknown")

# ── CLAUDE_RECOVERY_MODE bypass (Rule 27 §RULE-059-07) ──

if [ "${CLAUDE_RECOVERY_MODE:-}" = "1" ]; then
    echo "WARN [CLAUDE_RECOVERY_MODE] bypassing preflight gates for tool=${TOOL_NAME} context=${INTERCEPT_CONTEXT}" >&2
    echo "WARN [CLAUDE_RECOVERY_MODE] Rule 27 — RULE-059-07 exception — this bypass is audited" >&2
    emit_recovery_event "$INTERCEPT_CONTEXT" "$CURRENT_BRANCH"
    exit 0
fi

# ── Check preflight script exists ──

if [ ! -f "$PREFLIGHT" ]; then
    block_with_message "OPERATIONAL_ERROR: preflight runner not found" \
        "Expected at: ${PREFLIGHT} — cannot validate gates without runner"
fi

if [ ! -x "$PREFLIGHT" ]; then
    block_with_message "OPERATIONAL_ERROR: preflight runner not executable" \
        "Run: chmod +x ${PREFLIGHT}"
fi

# ── Invoke preflight with correct scope ──

PREFLIGHT_ARGS=()

if [ "$DERIVED_SCOPE" = "story" ] && [ -n "$DERIVED_STORY_ID" ]; then
    PREFLIGHT_ARGS=("--scope=story" "--story-id=story-${DERIVED_STORY_ID}")
elif [ "$DERIVED_SCOPE" = "epic" ] && [ -n "$DERIVED_EPIC_ID" ]; then
    PREFLIGHT_ARGS=("--scope=epic" "--epic-id=epic-${DERIVED_EPIC_ID}")
elif [ "$INTERCEPT_CONTEXT" = "gh-pr-create-develop-base" ]; then
    # Developing epic PR → epic scope
    if [ -n "$DERIVED_EPIC_ID" ]; then
        PREFLIGHT_ARGS=("--scope=epic" "--epic-id=epic-${DERIVED_EPIC_ID}")
    else
        PREFLIGHT_ARGS=("--scope=story")
    fi
else
    # Unknown scope but we've matched a pattern — try story scope as default
    PREFLIGHT_ARGS=("--scope=story")
fi

PREFLIGHT_EXIT=0
"$PREFLIGHT" "${PREFLIGHT_ARGS[@]}" >/dev/null 2>&1 || PREFLIGHT_EXIT=$?

if [ "$PREFLIGHT_EXIT" -eq 0 ]; then
    exit 0
fi

# Map exit code to a human-readable gate name
case "$PREFLIGHT_EXIT" in
    1)  GATE_NAME="TESTS_FAILED" ;;
    2)  GATE_NAME="COVERAGE_BELOW_THRESHOLD" ;;
    3)  GATE_NAME="REVIEW_CONTENT_INSUFFICIENT" ;;
    4)  GATE_NAME="TELEMETRY_EVIDENCE_MISSING" ;;
    5)  GATE_NAME="FORMAT_VIOLATION" ;;
    6)  GATE_NAME="AUDIT_SELF_CHECK_FAILED" ;;
    7)  GATE_NAME="GRAMMAR_VIOLATION" ;;
    8)  GATE_NAME="WAVE_DISPATCH_INCOMPLETE" ;;
    9)  GATE_NAME="PLANNING_CONTENT_INSUFFICIENT" ;;
    10) GATE_NAME="HOOKS_INTEGRITY_FAILED" ;;
    11) GATE_NAME="TELEMETRY_CHAIN_BROKEN" ;;
    12) GATE_NAME="PHASE_GATE_AUDIT_FAILED" ;;
    13) GATE_NAME="INVALID_ARGS" ;;
    14) GATE_NAME="OPERATIONAL_ERROR" ;;
    *)  GATE_NAME="PREFLIGHT_EXIT_${PREFLIGHT_EXIT}" ;;
esac

block_with_message "$GATE_NAME" \
    "preflight.sh exited $PREFLIGHT_EXIT — fix the failing gate and retry"
