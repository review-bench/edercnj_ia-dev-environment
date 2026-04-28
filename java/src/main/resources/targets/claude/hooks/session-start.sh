#!/usr/bin/env bash
# Layer:      0 (preventive — fires during LLM turn)
# Trigger:    SessionStart
# Event:      session_start
# Exit codes: 0=OK (always — SessionStart hook must not block)
# Latency:    < 50ms
# Telemetry:  no emission (session-start hook itself)
#
# session-start.sh — Records session start timestamp in .claude/state/.
#
# Written as ISO-8601 epoch seconds to .claude/state/session-start.txt.
# Used by verify-story-completion.sh to scope Signal A+B to THIS session only,
# fixing the false-positive storm (EPIC-0061 story-0061-0006 TASK-0061-0006-005).
#
# Fail-open: any error exits 0 without blocking the session.

set +e

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
STATE_DIR="${PROJECT_DIR}/.claude/state"

mkdir -p "${STATE_DIR}" 2>/dev/null || true

SESSION_EPOCH="$(date +%s 2>/dev/null || echo "")"

if [[ -n "${SESSION_EPOCH}" ]]; then
    printf '%s\n' "${SESSION_EPOCH}" > "${STATE_DIR}/session-start.txt"
fi

exit 0
