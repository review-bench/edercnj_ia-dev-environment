#!/usr/bin/env bash
# Layer:      0 (preventive — fires during LLM turn)
# Trigger:    Stop
# Event:      stop
# Exit codes: 0=OK (no story evidence issues), 2=WARNING (missing evidence artifact)
# Latency:    < 500ms
# Telemetry:  reads NDJSON events for session activity detection
#
# verify-story-completion.sh — Claude Code `Stop` hook, EIE Camada 0.
#
# Detects when a turn just completed story-level work (PR created, merge done,
# or git commit on a feat/story-* branch) and verifies that the required
# sub-skills produced their evidence artifacts in plans/epic-*/plans/ and
# plans/epic-*/reports/.
#
# On missing evidence, exits with code 2 and emits a visible warning on stderr
# — Claude Code surfaces this to the LLM as a blocking notification that MUST
# be addressed before the next turn.
#
# Fail-open for non-EIE-relevant turns: if no story activity detected in this
# turn, exit 0 silently. See .claude/rules/24-execution-integrity.md.

set +e
set -u

if [[ "${CLAUDE_EIE_DISABLED:-0}" == "1" ]]; then
    cat >/dev/null 2>&1
    exit 0
fi

# Consume hook payload (JSON from Claude Code)
if command -v timeout >/dev/null 2>&1; then
    HOOK_PAYLOAD="$(timeout 3 cat 2>/dev/null)"
elif command -v gtimeout >/dev/null 2>&1; then
    HOOK_PAYLOAD="$(gtimeout 3 cat 2>/dev/null)"
else
    HOOK_PAYLOAD="$(cat 2>/dev/null)"
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "${PROJECT_DIR}" 2>/dev/null || exit 0

# Discover most recent epic telemetry file
TELEMETRY="$(ls -t plans/epic-*/telemetry/events.ndjson 2>/dev/null | head -1)"
[[ -z "${TELEMETRY}" ]] && exit 0

# Heuristic: story-completion signals SCOPED TO THIS SESSION ONLY.
# (EPIC-0061 story-0061-0006 false-positive storm fix — TASK-0061-0006-005)
SESSION_START_FILE="${PROJECT_DIR}/.claude/state/session-start.txt"
if [[ -f "${SESSION_START_FILE}" ]]; then
    SESSION_EPOCH="$(cat "${SESSION_START_FILE}" 2>/dev/null || echo "")"
else
    SESSION_EPOCH=""
fi
if [[ -z "${SESSION_EPOCH}" || ! "${SESSION_EPOCH}" =~ ^[0-9]+$ ]]; then
    SESSION_EPOCH="$(date -d '1 hour ago' +%s 2>/dev/null \
        || date -v -1H +%s 2>/dev/null \
        || echo "$(( $(date +%s) - 3600 ))")"
fi
SESSION_ISO="$(date -d "@${SESSION_EPOCH}" --iso-8601=seconds 2>/dev/null \
    || date -r "${SESSION_EPOCH}" -u +%Y-%m-%dT%H:%M:%SZ 2>/dev/null \
    || echo "")"

# Signal A — commits with story references made IN THIS SESSION (not historical telemetry)
HAS_PR_CREATE=0
if [[ -n "${SESSION_ISO}" ]]; then
    SESSION_STORY_COMMITS="$(git log --since="${SESSION_ISO}" --format=%B 2>/dev/null \
        | grep -cE "feat\(story-|chore\(story-|fix\(story-" 2>/dev/null || echo 0)"
    if [[ "${SESSION_STORY_COMMITS}" -gt 0 ]]; then
        HAS_PR_CREATE=1
    fi
fi

# Signal B — most recent commit from THIS SESSION (not pre-session HEAD)
HAS_STORY_COMMIT=0
LATEST_COMMIT_MSG=""
if [[ -n "${SESSION_ISO}" ]]; then
    LATEST_COMMIT_MSG="$(git log --since="${SESSION_ISO}" -1 --format=%B 2>/dev/null || true)"
fi
[[ -z "${LATEST_COMMIT_MSG}" ]] && LATEST_COMMIT_MSG="$(git log -1 --format=%B 2>/dev/null || true)"
if [[ "${LATEST_COMMIT_MSG}" =~ feat\(story-[0-9]{4}-[0-9]{4} ]]; then
    HAS_STORY_COMMIT=1
fi

# If neither signal present, this turn was not a story-completion turn
if [[ "${HAS_PR_CREATE}" -eq 0 && "${HAS_STORY_COMMIT}" -eq 0 ]]; then
    exit 0
fi

# Extract story ID from session-scoped commit
STORY_ID=""
if [[ "${LATEST_COMMIT_MSG}" =~ (story-[0-9]{4}-[0-9]{4}) ]]; then
    STORY_ID="${BASH_REMATCH[1]}"
fi
if [[ -z "${STORY_ID}" ]]; then
    BRANCH="$(git branch --show-current 2>/dev/null || true)"
    if [[ "${BRANCH}" =~ (story-[0-9]{4}-[0-9]{4}) ]]; then
        STORY_ID="${BASH_REMATCH[1]}"
    fi
fi
[[ -z "${STORY_ID}" ]] && exit 0

# Extract epic ID from story ID (story-XXXX-YYYY → epic XXXX)
if [[ "${STORY_ID}" =~ story-([0-9]{4})-[0-9]{4} ]]; then
    EPIC_ID="${BASH_REMATCH[1]}"
else
    exit 0
fi

PLANS_DIR="plans/epic-${EPIC_ID}/plans"
REPORTS_DIR="plans/epic-${EPIC_ID}/reports"

# Check mandatory evidence artifacts (Rule 24 §4.1)
MISSING=()

if [[ ! -f "${REPORTS_DIR}/verify-envelope-${STORY_ID}.json" ]]; then
    MISSING+=("x-internal-story-verify → ${REPORTS_DIR}/verify-envelope-${STORY_ID}.json")
fi

if ! ls "${PLANS_DIR}/review-"*"-${STORY_ID}.md" >/dev/null 2>&1 \
        && ! [[ -f "${PLANS_DIR}/review-story-${STORY_ID}.md" ]]; then
    MISSING+=("x-review → ${PLANS_DIR}/review-story-${STORY_ID}.md")
fi

if ! ls "${PLANS_DIR}/techlead-review-"*"-${STORY_ID}.md" >/dev/null 2>&1 \
        && ! [[ -f "${PLANS_DIR}/techlead-review-story-${STORY_ID}.md" ]]; then
    MISSING+=("x-review-pr → ${PLANS_DIR}/techlead-review-story-${STORY_ID}.md")
fi

if [[ ! -f "${REPORTS_DIR}/story-completion-report-${STORY_ID}.md" ]]; then
    MISSING+=("x-internal-story-report → ${REPORTS_DIR}/story-completion-report-${STORY_ID}.md")
fi

# EPIC-0057 story-0057-0006 — extended artefact checks (Rule 24 §32-42).
# Hard artefacts: x-pr-watch-ci state file (only when PR exists for current
# branch) and x-dependency-audit report. Soft artefacts: test-run + threat
# model — logged as NOTICE but do not trigger exit 2.
SOFT_NOTES=()

# x-pr-watch-ci — derive PR number from current branch via gh; skip if gh
# absent or PR not found (legitimate when story has not pushed yet).
if command -v gh >/dev/null 2>&1; then
    PR_NUMBER="$(gh pr view --json number -q .number 2>/dev/null || true)"
    if [[ -n "${PR_NUMBER}" ]]; then
        if [[ ! -f ".claude/state/pr-watch-${PR_NUMBER}.json" ]]; then
            MISSING+=("x-pr-watch-ci → .claude/state/pr-watch-${PR_NUMBER}.json")
        fi
    fi
fi

# x-dependency-audit — hard artefact; absence is a violation.
if [[ ! -f "${REPORTS_DIR}/dependency-audit-${STORY_ID}.md" ]]; then
    MISSING+=("x-dependency-audit → ${REPORTS_DIR}/dependency-audit-${STORY_ID}.md")
fi

# x-test-tdd / x-test-run — soft artefact.
if [[ ! -f "${REPORTS_DIR}/test-run-${STORY_ID}.txt" ]]; then
    SOFT_NOTES+=("x-test-tdd/x-test-run → ${REPORTS_DIR}/test-run-${STORY_ID}.txt (soft)")
fi

# x-threat-model — soft artefact.
if [[ ! -f "${PLANS_DIR}/threat-model-story-${STORY_ID}.md" ]]; then
    SOFT_NOTES+=("x-threat-model → ${PLANS_DIR}/threat-model-story-${STORY_ID}.md (soft)")
fi

# Emit soft notices (non-blocking)
if [[ ${#SOFT_NOTES[@]} -gt 0 ]]; then
    {
        echo ""
        echo "ℹ NOTICE [verify-story-completion] Soft artifacts missing for ${STORY_ID}:"
        for n in "${SOFT_NOTES[@]}"; do
            printf "  ⚠ %s\n" "${n}"
        done
        echo "[Rule 24 — Camada 2 soft]"
    } >&2
fi

if [[ ${#MISSING[@]} -gt 0 ]]; then
    cat >&2 <<EOF

🔒 EXECUTION INTEGRITY WARNING (Rule 24 — Camada 2)
=====================================================

Story: ${STORY_ID}
Detected: story-completion turn (commit or PR) but evidence artifacts MISSING.

The following MANDATORY sub-skills appear to have been SKIPPED or INLINED
instead of invoked as real tool calls:

EOF
    for m in "${MISSING[@]}"; do
        printf "  ❌ %s\n" "${m}" >&2
    done
    cat >&2 <<EOF

ACTION REQUIRED before proceeding:
  1. Go back to ${STORY_ID}
  2. Invoke the missing skills as REAL tool calls:
     Skill(skill: "x-internal-story-verify", args: "--story-id ${STORY_ID} ...")
     Skill(skill: "x-review", args: "${STORY_ID}")
     Skill(skill: "x-review-pr", args: "${STORY_ID}")
     Skill(skill: "x-internal-story-report", args: "...")
  3. Each must produce its evidence file (listed above).

LEGITIMATE BYPASS: pass --skip-review / --skip-verification explicitly via
the calling skill's argv. Any other skip is a Rule 24 violation.

This warning will be enforced as a HARD FAILURE by CI when the PR is
merged to develop — see scripts/audit-execution-integrity.sh.

To temporarily disable this hook locally (not recommended):
  export CLAUDE_EIE_DISABLED=1
=====================================================
EOF
    exit 2
fi

exit 0
