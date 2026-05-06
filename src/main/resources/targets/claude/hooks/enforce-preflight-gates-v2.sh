#!/usr/bin/env bash
# requires-capabilities: []
# enforce-preflight-gates-v2.sh — Camada 0 PreToolUse hook v2 (Rule 26 §Camada 0)
#
# Layer:      0 (preventive — fires during LLM turn)
# Trigger:    PreToolUse
# Event:      PreToolUse (tool_name=Bash or Skill)
# Exit codes: 0=OK (allow), 2=BLOCKED (surfaced to LLM as blocking notification)
# Latency:    < 200ms for intercepted calls, < 50ms for non-intercepted
# Telemetry:  recovery_mode_used event (with vector field) appended to NDJSON
#             when CLAUDE_RECOVERY_MODE=1
#
# This is the v2 companion to enforce-preflight-gates.sh (story-0063-0004).
# It adds 15 additional bypass vectors beyond the base patterns:
#
# Build bypass vectors (mvn-*):
#   - mvn -DskipTests (skips test execution)
#   - mvn -DskipITs (skips integration tests)
#   - mvn -Dspotless.check.skip=true (skips format check)
#   - mvn -Dmaven.test.skip=true (skips all tests)
#   - mvn -Pno-tests (activates no-tests profile)
#
# Commit bypass vectors:
#   - git commit --amend (amending pushed commits)
#   - git rebase --skip (discards commits during rebase conflict)
#
# Release bypass vectors:
#   - git tag -d <release-tag> (deletes release tag)
#   - git push --delete origin <release-tag> (remote tag deletion)
#   - gh release delete <release> (deletes published release)
#   - mvn release:perform (direct release bypass, without x-release)
#   - git push --force/--force-with-lease to protected branches (main/develop/epic/*)
#
# Merge bypass vectors:
#   - gh pr merge --rebase --admin (combined flags)
#   - Skill x-merge-pr (merging PRs without review)
#   - Skill x-manage-pr-merge-train (merge train bypass)
#
# Note on gh pr close: handled in warn-mode (exit 0 + WARNING) per §Decision Rationale
# (PREFLIGHT_PHASE rollout: warn before blocking to calibrate false positives)
#
# Fail-CLOSED contract (RULE-005): intercepted tool calls that cannot be evaluated
# exit 2 (block) — never fail-open.
#
# CLAUDE_RECOVERY_MODE=1 is the ONLY bypass variable (RULE-004, Rule 27 §RULE-059-07).
# Recovery events include the `vector` field for audit analysis (story-0063-0017).
#
# All interceptions are logged to stderr for audit trail.
#
# See: story-0063-0013, Rule 26 §Camada 0, Rule 27 §Zero-Bypass Lifecycle

set -uo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"

# Chain detection window: seconds to look back in NDJSON for a parent skill invocation
PREFLIGHT_CHAIN_WINDOW_S="${PREFLIGHT_CHAIN_WINDOW_S:-2}"

# Rollout phase: "warn" = log but allow; "block" = enforce exit 2
# Used for vectors that may have false positives (e.g., gh pr close)
PREFLIGHT_PHASE="${PREFLIGHT_PHASE:-block}"

# ── Helpers ───────────────────────────────────────────────────────────────────

# Resolve the active NDJSON telemetry file for a given epic_id.
resolve_ndjson_path() {
    local epic_id="${1:-}"
    if [ -z "$epic_id" ]; then
        local branch
        branch=$(git -C "$PROJECT_DIR" symbolic-ref HEAD 2>/dev/null \
            | sed 's|refs/heads/||' || echo "")
        if [[ "$branch" =~ feat/story-([0-9]{4}) ]]; then
            epic_id="${BASH_REMATCH[1]}"
        elif [[ "$branch" =~ feat/task-([0-9]{4}) ]]; then
            epic_id="${BASH_REMATCH[1]}"
        elif [[ "$branch" =~ epic/([0-9]{4}) ]]; then
            epic_id="${BASH_REMATCH[1]}"
        fi
    fi

    if [ -n "$epic_id" ]; then
        local v4_path
        v4_path=$(find "${PROJECT_DIR}/ai/epics" -maxdepth 3 \
            -name "events.ndjson" \
            -path "*/epic-${epic_id}-*/telemetry/*" 2>/dev/null | head -1 || echo "")
        if [ -n "$v4_path" ]; then
            echo "$v4_path"
            return 0
        fi
        local v3_path="${PROJECT_DIR}/ai/epics/epic-${epic_id}/telemetry/events.ndjson"
        if [ -f "$v3_path" ]; then
            echo "$v3_path"
            return 0
        fi
    fi

    echo "${PROJECT_DIR}/plans/unknown/telemetry/events.ndjson"
}

# Emit a recovery_mode_used event to the NDJSON telemetry file.
emit_recovery_event() {
    local vector="${1:-unknown}"
    local command_truncated="${2:-}"
    local ndjson_path
    ndjson_path=$(resolve_ndjson_path "")

    mkdir -p "$(dirname "$ndjson_path")" 2>/dev/null || true

    local timestamp
    timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || echo "")

    local session_id="${CLAUDE_SESSION_ID:-}"
    local pid="$$"

    # Derive story/epic context from branch
    local branch
    branch=$(git -C "$PROJECT_DIR" symbolic-ref HEAD 2>/dev/null \
        | sed 's|refs/heads/||' || echo "")
    local story_id="" epic_id=""
    if [[ "$branch" =~ feat/story-([0-9]{4}-[0-9]{4}) ]]; then
        story_id="${BASH_REMATCH[1]}"
        epic_id="${story_id%%-*}"
    elif [[ "$branch" =~ feat/task-([0-9]{4})-([0-9]{4}) ]]; then
        story_id="${BASH_REMATCH[1]}-${BASH_REMATCH[2]}"
        epic_id="${BASH_REMATCH[1]}"
    fi

    local cmd_safe
    cmd_safe=$(echo "${command_truncated}" | head -c 200 | tr '\n' ' ')

    printf '{"timestamp":"%s","event":"recovery_mode_used","session_id":"%s","pid":%s,"vector":"%s","command":"%s","story_id":"%s","epic_id":"%s"}\n' \
        "$timestamp" "$session_id" "$pid" "$vector" \
        "$(echo "$cmd_safe" | sed 's/"/\\"/g')" \
        "$story_id" "$epic_id" \
        >> "$ndjson_path" 2>/dev/null || true
}

# Block a tool call with a structured error message to stderr.
# Args: $1=vector_name, $2=detail_message, $3=alternative
block_with_vector() {
    local vector="${1:-UNKNOWN_VECTOR}"
    local detail="${2:-bypass detected}"
    local alternative="${3:-use the orchestrator skill instead}"

    echo "" >&2
    echo "⛔ BLOCKED — enforce-preflight-gates-v2.sh" >&2
    echo "   PREFLIGHT_BYPASS_BLOCKED [vector=${vector}]" >&2
    echo "   Reason: ${detail}" >&2
    echo "   Alternative: ${alternative}" >&2
    echo "   Override: set CLAUDE_RECOVERY_MODE=1 (audit-logged, <5% threshold)" >&2
    echo "   Refs: Rule 24 §Camada 0, Rule 27 §Zero-Bypass Lifecycle, story-0063-0013" >&2
    echo "" >&2
    exit 2
}

# ── Read stdin (PreToolUse JSON payload) ─────────────────────────────────────

PAYLOAD=""
if ! IFS= read -r -t 5 PAYLOAD 2>/dev/null; then
    # Non-interactive or no input — not a real PreToolUse event
    exit 0
fi

[ -z "$PAYLOAD" ] && exit 0

# Validate JSON
if ! echo "$PAYLOAD" | jq empty 2>/dev/null; then
    # Malformed — but we're v2 companion: let v1 handle OPERATIONAL_ERROR
    # For our vectors, still exit 0 on malformed (v1 will block)
    exit 0
fi

# Extract tool_name
TOOL_NAME=$(echo "$PAYLOAD" | jq -r '.tool_name // empty' 2>/dev/null || echo "")
[ -z "$TOOL_NAME" ] && exit 0

# ── Extract command string for Bash calls ─────────────────────────────────────

CMD=""
if [ "$TOOL_NAME" = "Bash" ]; then
    CMD=$(echo "$PAYLOAD" | jq -r '.tool_input.command // empty' 2>/dev/null || echo "")
fi

SKILL_NAME=""
if [ "$TOOL_NAME" = "Skill" ]; then
    SKILL_NAME=$(echo "$PAYLOAD" | jq -r '.tool_input.skill // empty' 2>/dev/null || echo "")
fi

# ── VECTOR DETECTION ──────────────────────────────────────────────────────────
# Each vector: detect pattern, then either block or warn depending on PREFLIGHT_PHASE.
# CLAUDE_RECOVERY_MODE=1 always bypasses (with WARNING + telemetry event).

DETECTED_VECTOR=""
DETECTED_DETAIL=""
DETECTED_ALTERNATIVE=""

if [ "$TOOL_NAME" = "Bash" ] && [ -n "$CMD" ]; then

    # ── Build bypass vectors (mvn-*) ─────────────────────────────────────────

    # Vector: mvn-skipTests
    if echo "$CMD" | grep -qE 'mvn\b.*-DskipTests(=true)?(\s|$)'; then
        DETECTED_VECTOR="mvn-skipTests"
        DETECTED_DETAIL="mvn -DskipTests bypasses the test execution quality gate"
        DETECTED_ALTERNATIVE="invoke /x-execute-tests for scoped test execution"
    fi

    # Vector: mvn-skipITs
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE 'mvn\b.*-DskipITs(=true)?(\s|$)'; then
        DETECTED_VECTOR="mvn-skipITs"
        DETECTED_DETAIL="mvn -DskipITs skips integration tests — bypasses Camada 0 gate"
        DETECTED_ALTERNATIVE="run integration tests via /x-execute-e2e-tests"
    fi

    # Vector: mvn-spotlessSkip
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE 'mvn\b.*-Dspotless\.check\.skip=true'; then
        DETECTED_VECTOR="mvn-spotlessSkip"
        DETECTED_DETAIL="skipping Spotless check bypasses the format quality gate"
        DETECTED_ALTERNATIVE="fix formatting via /x-format-code before committing"
    fi

    # Vector: mvn-testSkip
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE 'mvn\b.*-Dmaven\.test\.skip=true'; then
        DETECTED_VECTOR="mvn-testSkip"
        DETECTED_DETAIL="mvn -Dmaven.test.skip=true disables all tests — bypasses quality gate"
        DETECTED_ALTERNATIVE="invoke /x-execute-tests for scoped test execution"
    fi

    # Vector: mvn-noTestsProfile
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE 'mvn\b.*-Pno-tests(\s|$)'; then
        DETECTED_VECTOR="mvn-noTestsProfile"
        DETECTED_DETAIL="mvn -Pno-tests activates a test-bypass profile — prohibited"
        DETECTED_ALTERNATIVE="remove -Pno-tests and run tests via /x-execute-tests"
    fi

    # ── Commit bypass vectors ────────────────────────────────────────────────

    # Vector: git-amend-pushed (git commit --amend)
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE '^\s*git commit\s.*--amend'; then
        DETECTED_VECTOR="git-amend-pushed"
        DETECTED_DETAIL="git commit --amend may rewrite pushed history — prohibited"
        DETECTED_ALTERNATIVE="create a new commit with the fix and use /x-commit-changes"
    fi

    # Vector: git-rebase-skip
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE '^\s*git rebase\s.*--skip'; then
        DETECTED_VECTOR="git-rebase-skip"
        DETECTED_DETAIL="git rebase --skip discards commits — may lose work"
        DETECTED_ALTERNATIVE="resolve the conflict properly and use git rebase --continue"
    fi

    # ── Release bypass vectors ───────────────────────────────────────────────

    # Vector: git-tag-delete (release tag)
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE '^\s*git tag\s.*-d\s.*v[0-9]+\.[0-9]+\.[0-9]+'; then
        DETECTED_VECTOR="git-tag-delete"
        DETECTED_DETAIL="deleting a release tag (vX.Y.Z) reverts a published release — prohibited"
        DETECTED_ALTERNATIVE="open a hotfix PR via /x-release --hotfix instead"
    fi

    # Vector: git-push-delete-tag
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE '^\s*git push\s.*--delete\s.*v[0-9]+\.[0-9]+\.[0-9]+'; then
        DETECTED_VECTOR="git-push-delete-tag"
        DETECTED_DETAIL="deleting remote release tag bypasses the release lifecycle"
        DETECTED_ALTERNATIVE="open a hotfix PR via /x-release --hotfix instead"
    fi

    # Vector: gh-release-delete
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE '^\s*gh release delete\b'; then
        DETECTED_VECTOR="gh-release-delete"
        DETECTED_DETAIL="gh release delete permanently removes a published release"
        DETECTED_ALTERNATIVE="use /x-release to manage releases through proper lifecycle"
    fi

    # Vector: mvn-release-perform
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE '^\s*mvn\s.*release:perform'; then
        DETECTED_VECTOR="mvn-release-perform"
        DETECTED_DETAIL="mvn release:perform bypasses the /x-release orchestrator"
        DETECTED_ALTERNATIVE="use /x-release to orchestrate the full release lifecycle"
    fi

    # Vector: git-push-force-protected
    # Blocks --force and --force-with-lease to main, develop, and epic/* branches.
    # Allows --force to feature branches (feat/, fix/, hotfix/) — non-protected.
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE '^\s*git push\b.*(--force-with-lease|--force)\b'; then
        if echo "$CMD" | grep -qE '\b(main|master|develop)\b|epic/'; then
            DETECTED_VECTOR="git-push-force-protected"
            DETECTED_DETAIL="force-pushing to protected branch (main/develop/epic/*) is prohibited"
            DETECTED_ALTERNATIVE="rebase locally and open a PR through the standard workflow"
        fi
        # If targeting feat/ or other non-protected branches: allow (no DETECTED_VECTOR set)
    fi

    # ── Merge bypass vectors ─────────────────────────────────────────────────

    # Vector: gh-pr-merge-rebase-admin (combined --rebase --admin)
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE '^\s*gh pr merge\b.*--rebase\b.*--admin\b'; then
        DETECTED_VECTOR="gh-pr-merge-rebase-admin"
        DETECTED_DETAIL="combining --rebase and --admin bypasses review and approval"
        DETECTED_ALTERNATIVE="use standard PR merge via /x-merge-pr without --admin"
    fi

    # Vector: gh-pr-close-merged (warn-mode: log but allow in phase=warn)
    if [ -z "$DETECTED_VECTOR" ] && echo "$CMD" | grep -qE '^\s*gh pr close\b'; then
        if [ "${PREFLIGHT_PHASE:-block}" = "warn" ]; then
            # Warn-mode: log and allow
            echo "" >&2
            echo "⚠ WARN — enforce-preflight-gates-v2.sh [vector=gh-pr-close-merged]" >&2
            echo "  gh pr close may hide a merged-PR revert — verify intent before proceeding" >&2
            echo "  PREFLIGHT_PHASE=warn: allowed in rollout phase 1 (will become BLOCKED in phase 2)" >&2
            echo "" >&2
            exit 0
        fi
        # In block mode: treat as bypass
        DETECTED_VECTOR="gh-pr-close-merged"
        DETECTED_DETAIL="closing a PR may conceal a merged-PR revert"
        DETECTED_ALTERNATIVE="verify the PR status and use /x-merge-pr for standard merge flow"
    fi

fi

# ── Skill bypass vectors ──────────────────────────────────────────────────────

if [ "$TOOL_NAME" = "Skill" ] && [ -n "$SKILL_NAME" ]; then

    # Vector: skill-x-pr-merge (direct skill invocation bypassing CI watch)
    if [ -z "$DETECTED_VECTOR" ] && [ "$SKILL_NAME" = "x-merge-pr" ]; then
        DETECTED_VECTOR="skill-x-pr-merge"
        DETECTED_DETAIL="x-merge-pr invoked directly — bypasses CI-watch gate (Rule 45)"
        DETECTED_ALTERNATIVE="use /x-implement-story or /x-implement-epic orchestrators which enforce CI-watch"
    fi

    # Vector: skill-x-pr-merge-train (direct invocation without proper review)
    if [ -z "$DETECTED_VECTOR" ] && [ "$SKILL_NAME" = "x-manage-pr-merge-train" ]; then
        DETECTED_VECTOR="skill-x-pr-merge-train"
        DETECTED_DETAIL="x-manage-pr-merge-train invoked directly — may bypass per-PR review gates"
        DETECTED_ALTERNATIVE="invoke merge train via /x-implement-epic Phase 5 which includes review gates"
    fi

fi

# No interception needed — exit 0 (allow)
[ -n "$DETECTED_VECTOR" ] || exit 0

# ── CLAUDE_RECOVERY_MODE bypass (Rule 27 §RULE-059-07) ───────────────────────

if [ "${CLAUDE_RECOVERY_MODE:-}" = "1" ]; then
    echo "" >&2
    echo "WARN [CLAUDE_RECOVERY_MODE] vector=${DETECTED_VECTOR} — bypass allowed (audit-logged)" >&2
    echo "WARN [CLAUDE_RECOVERY_MODE] Rule 27 §RULE-059-07 exception — telemetry event emitted" >&2
    echo "" >&2
    emit_recovery_event "$DETECTED_VECTOR" "$CMD"
    exit 0
fi

# ── Block the tool call ───────────────────────────────────────────────────────

block_with_vector "$DETECTED_VECTOR" "$DETECTED_DETAIL" "$DETECTED_ALTERNATIVE"
