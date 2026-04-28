#!/usr/bin/env bash
# enforce_preflight_gates_v2_test.sh — TDD tests for enforce-preflight-gates-v2.sh
# Story: story-0063-0013 (PreToolUse Hook Coverage Expansion — Bypass Vectors v2)
# Phase: RED → GREEN (write tests before implementation)
#
# Tests:
#   T1: hook is executable
#   T2: hook uses bash strict mode (set -uo pipefail)
#   T3: CLAUDE_RECOVERY_MODE=1 bypasses all checks (exit 0)
#   T4: hook references additional intercept patterns (mvn-skipTests)
#   T5: git push --force to protected branch → exit 2 (blocked)
#   T6: mvn -DskipTests → exit 2 (blocked)
#   T7: git commit --amend → exit 2 (blocked)
#   T8: git rebase --skip → exit 2 (blocked)
#   T9: mvn release:perform → exit 2 (blocked)
#   T10: gh release delete → exit 2 (blocked)
#   T11: git push --force to feat/ branch → exit 0 (allowed, non-protected)
#   T12: git tag -d v1.0.0 → exit 2 (blocked, release tag)
#   T13: mvn -Dspotless.check.skip=true → exit 2 (blocked)
#   T14: non-matched tool call → exit 0 (no-op)
#   T15: CLAUDE_RECOVERY_MODE=1 emits warning to stderr

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
HOOK="${REPO_ROOT}/.claude/hooks/enforce-preflight-gates-v2.sh"

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

echo "=== enforce-preflight-gates-v2.sh tests ==="

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

# T3: CLAUDE_RECOVERY_MODE=1 → exit 0 for a matched payload
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

PUSH_FORCE_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"git push --force origin main"}}'

actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE=1 \
    bash "$HOOK" <<< "$PUSH_FORCE_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 0 ]; then
    echo "  PASS: T3 CLAUDE_RECOVERY_MODE=1 → exit 0 (bypass works)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T3 CLAUDE_RECOVERY_MODE=1 → expected exit 0, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T4: hook references additional intercept patterns (mvn-skipTests keyword)
if grep -qE "skipTests|DskipTests" "$HOOK" 2>/dev/null; then
    echo "  PASS: T4 hook references mvn-skipTests intercept pattern"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T4 hook does not reference mvn-skipTests pattern" >&2
    FAIL=$((FAIL + 1))
fi

# T5: git push --force to protected branch (main) → exit 2
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$PUSH_FORCE_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 2 ]; then
    echo "  PASS: T5 git push --force to main → exit 2 (blocked)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T5 git push --force to main → expected exit 2, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T6: mvn -DskipTests → exit 2
MVN_SKIP_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"mvn install -DskipTests"}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$MVN_SKIP_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 2 ]; then
    echo "  PASS: T6 mvn -DskipTests → exit 2 (blocked)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T6 mvn -DskipTests → expected exit 2, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T7: git commit --amend → exit 2
AMEND_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"git commit --amend -m \"fix typo\""}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$AMEND_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 2 ]; then
    echo "  PASS: T7 git commit --amend → exit 2 (blocked)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T7 git commit --amend → expected exit 2, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T8: git rebase --skip → exit 2
REBASE_SKIP_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"git rebase --skip"}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$REBASE_SKIP_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 2 ]; then
    echo "  PASS: T8 git rebase --skip → exit 2 (blocked)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T8 git rebase --skip → expected exit 2, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T9: mvn release:perform → exit 2
MVN_RELEASE_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"mvn release:perform"}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$MVN_RELEASE_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 2 ]; then
    echo "  PASS: T9 mvn release:perform → exit 2 (blocked)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T9 mvn release:perform → expected exit 2, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T10: gh release delete → exit 2
GH_RELEASE_DELETE_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"gh release delete v1.5.0"}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$GH_RELEASE_DELETE_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 2 ]; then
    echo "  PASS: T10 gh release delete → exit 2 (blocked)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T10 gh release delete → expected exit 2, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T11: git push --force to feat/ branch → exit 0 (allowed, non-protected)
PUSH_FORCE_FEAT_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"git push --force origin feat/wip-experiment"}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$PUSH_FORCE_FEAT_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 0 ]; then
    echo "  PASS: T11 git push --force to feat/ branch → exit 0 (not protected)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T11 git push --force to feat/ branch → expected exit 0, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T12: git tag -d v1.0.0 → exit 2 (release tag deletion blocked)
TAG_DELETE_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"git tag -d v1.0.0"}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$TAG_DELETE_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 2 ]; then
    echo "  PASS: T12 git tag -d v1.0.0 → exit 2 (blocked)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T12 git tag -d v1.0.0 → expected exit 2, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T13: mvn -Dspotless.check.skip=true → exit 2
SPOTLESS_SKIP_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"mvn verify -Dspotless.check.skip=true"}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE="" \
    bash "$HOOK" <<< "$SPOTLESS_SKIP_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 2 ]; then
    echo "  PASS: T13 mvn -Dspotless.check.skip=true → exit 2 (blocked)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T13 mvn -Dspotless.check.skip=true → expected exit 2, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T14: non-matched tool call (git status) → exit 0 (no-op)
STATUS_PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"git status"}}'
actual_exit=0
CLAUDE_PROJECT_DIR="$TMP_DIR" \
    bash "$HOOK" <<< "$STATUS_PAYLOAD" >/dev/null 2>&1 || actual_exit=$?

if [ "$actual_exit" -eq 0 ]; then
    echo "  PASS: T14 non-matched tool call → exit 0 (no-op)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T14 non-matched tool call → expected exit 0, got $actual_exit" >&2
    FAIL=$((FAIL + 1))
fi

# T15: CLAUDE_RECOVERY_MODE=1 emits warning to stderr
MVN_SKIP_PAYLOAD2='{"tool_name":"Bash","tool_input":{"command":"mvn install -DskipTests"}}'
stderr_output=""
stderr_output=$(CLAUDE_PROJECT_DIR="$TMP_DIR" CLAUDE_RECOVERY_MODE=1 \
    bash "$HOOK" <<< "$MVN_SKIP_PAYLOAD2" 2>&1 || true)

if echo "$stderr_output" | grep -q "WARN\|recovery_mode\|RECOVERY_MODE"; then
    echo "  PASS: T15 CLAUDE_RECOVERY_MODE=1 emits warning to stderr"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T15 CLAUDE_RECOVERY_MODE=1 should emit warning to stderr" >&2
    echo "  FAIL: actual stderr: $stderr_output" >&2
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
