#!/usr/bin/env bash
# enforce_preflight_gates_test.sh — TDD tests for enforce-preflight-gates.sh
# Story: story-0063-0004 (PreToolUse Blocking Hook v1)
# Phase: RED → GREEN (write tests before implementation)
#
# Tests:
#   T1: hook is executable
#   T2: hook uses bash strict mode (set -uo pipefail or set -euo pipefail)
#   T3: hook references CLAUDE_RECOVERY_MODE bypass
#   T4: CLAUDE_RECOVERY_MODE=1 → hook exits 0 (bypass works) for a matched tool call
#   T5: when preflight script absent → exit 2 fail-CLOSED (RULE-005: fail-closed for gates)
#   T6: non-matched tool call (git status) → exit 0 (no-op)
#   T7: git commit --no-verify → exit 2 (bypass blocked unconditionally)
#   T8: Skill x-pr-create on story branch → invokes preflight (exit mirrors preflight)
#   T9: malformed stdin JSON → exit 2 (fail-CLOSED, OPERATIONAL_ERROR)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
HOOK="${REPO_ROOT}/.claude/hooks/enforce-preflight-gates.sh"

PASS=0
FAIL=0

assert_exit() {
    local test_name="$1" expected="$2"
    shift 2
    local actual=0
    "$@" >/dev/null 2>&1 || actual=$?
    if [ "$actual" -eq "$expected" ]; then
        echo "  PASS: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $test_name — expected exit $expected, got $actual" >&2
        FAIL=$((FAIL + 1))
    fi
}

assert_output_contains() {
    local test_name="$1" expected_pattern="$2"
    shift 2
    local output
    output=$("$@" 2>&1 || true)
    if echo "$output" | grep -q "$expected_pattern"; then
        echo "  PASS: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $test_name — expected output to contain '$expected_pattern'" >&2
        echo "  FAIL: actual output: $output" >&2
        FAIL=$((FAIL + 1))
    fi
}

echo "=== enforce-preflight-gates.sh tests ==="

# T1: hook is executable
if [ -x "$HOOK" ]; then
    echo "  PASS: T1 hook is executable"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T1 hook is not executable at $HOOK" >&2
    FAIL=$((FAIL + 1))
fi

# T2: hook uses bash strict mode
if grep -qE "set -[A-Za-z]*u[A-Za-z]*" "$HOOK" 2>/dev/null; then
    echo "  PASS: T2 hook uses strict mode (set with -u)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T2 hook does not use strict mode (no 'set -u*' found)" >&2
    FAIL=$((FAIL + 1))
fi

# T3: hook references CLAUDE_RECOVERY_MODE
if grep -q "CLAUDE_RECOVERY_MODE" "$HOOK" 2>/dev/null; then
    echo "  PASS: T3 hook references CLAUDE_RECOVERY_MODE"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T3 hook does not reference CLAUDE_RECOVERY_MODE" >&2
    FAIL=$((FAIL + 1))
fi

# T4: CLAUDE_RECOVERY_MODE=1 → exit 0 for a matched git push payload
# We simulate a git push payload; with CLAUDE_RECOVERY_MODE=1 it should exit 0
# even if preflight would fail (we point to a fake preflight that fails)
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

# Create a fake git repo so git symbolic-ref works inside subshell
FAKE_REPO="$TMP_DIR/fake_repo"
mkdir -p "$FAKE_REPO/.git/refs/heads"
git -C "$FAKE_REPO" init -q 2>/dev/null || true
git -C "$FAKE_REPO" checkout -q -b "feat/story-0063-0001-impl" 2>/dev/null || true

# Create a fake failing preflight
FAKE_SCRIPTS_DIR="$TMP_DIR/scripts"
mkdir -p "$FAKE_SCRIPTS_DIR"
cat > "$FAKE_SCRIPTS_DIR/preflight.sh" << 'EOF'
#!/usr/bin/env bash
exit 3  # REVIEW_CONTENT_INSUFFICIENT — would normally block
EOF
chmod +x "$FAKE_SCRIPTS_DIR/preflight.sh"

PUSH_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"git push origin feat/story-0063-0001-impl"}}'

# Run hook with CLAUDE_RECOVERY_MODE=1 — should exit 0 regardless of preflight
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE=1 \
    bash "$HOOK" <<< "$PUSH_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 0 ]; then
    echo "  PASS: T4 CLAUDE_RECOVERY_MODE=1 → exit 0 (bypass works)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T4 CLAUDE_RECOVERY_MODE=1 → expected exit 0, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T5: when preflight script absent → exit 2 (fail-CLOSED per RULE-005)
# Point CLAUDE_PROJECT_DIR to a dir with no scripts/preflight.sh
EMPTY_DIR="$TMP_DIR/empty_project"
mkdir -p "$EMPTY_DIR"

PUSH_PAYLOAD2='{"tool_name":"Bash","tool_input":{"command":"git push origin feat/story-0063-0001-impl"}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$EMPTY_DIR" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$PUSH_PAYLOAD2" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 2 ]; then
    echo "  PASS: T5 preflight absent → exit 2 (fail-CLOSED)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T5 preflight absent → expected exit 2, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T6: non-matched tool call (git status) → exit 0 (no-op)
STATUS_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"git status"}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$EMPTY_DIR" \
    bash "$HOOK" <<< "$STATUS_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 0 ]; then
    echo "  PASS: T6 non-matched tool call → exit 0 (no-op)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T6 non-matched tool call → expected exit 0, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T7: git commit --no-verify → exit 2 (bypass blocked unconditionally, even without RECOVERY_MODE)
NO_VERIFY_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"git commit --no-verify -m \"wip\""}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$EMPTY_DIR" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$NO_VERIFY_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 2 ]; then
    echo "  PASS: T7 git commit --no-verify → exit 2 (blocked unconditionally)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T7 git commit --no-verify → expected exit 2, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T8: Skill x-pr-create with matching story branch → invokes preflight
# With a real (exit 0) preflight present, hook should exit 0
mkdir -p "$TMP_DIR/good_project/scripts"
cat > "$TMP_DIR/good_project/scripts/preflight.sh" << 'EOF'
#!/usr/bin/env bash
exit 0
EOF
chmod +x "$TMP_DIR/good_project/scripts/preflight.sh"

SKILL_PAYLOAD='{"tool_name":"Skill","tool_input":{"skill":"x-pr-create","args":"--story-id story-0063-0001"}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR/good_project" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$SKILL_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 0 ]; then
    echo "  PASS: T8 Skill x-pr-create → preflight invoked, exit 0 (green)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T8 Skill x-pr-create → expected exit 0, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T9: malformed stdin JSON → exit 2 (fail-CLOSED, OPERATIONAL_ERROR per RULE-005)
actual_exit=0
CLAUDE_PROJECT_DIR="$EMPTY_DIR" \
    bash "$HOOK" <<< '{"tool_name": "Bash", invalid-json}' >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 2 ]; then
    echo "  PASS: T9 malformed stdin JSON → exit 2 (fail-CLOSED)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T9 malformed stdin JSON → expected exit 2, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
