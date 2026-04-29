#!/usr/bin/env bash
# audit_rollout_status_test.sh — Unit tests for audit-rollout-status.sh
# Tests map 1-to-1 to Gherkin scenarios in story-0063-0016
# Layer: 3 (detectivo — CI audit)
# Introduced: EPIC-0063 (Local-First Pre-Flight Gates) — story-0063-0016
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-rollout-status.sh"
TMP_DIR="$(mktemp -d)"
PASS=0; FAIL=0

cleanup() { rm -rf "$TMP_DIR"; }
trap cleanup EXIT

assert_exit() {
    local test_name="$1" expected="$2"; shift 2
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
    local test_name="$1" pattern="$2"; shift 2
    local output
    output=$("$@" 2>&1 || true)
    if echo "$output" | grep -qE "$pattern"; then
        echo "  PASS: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $test_name — output did not match '$pattern'" >&2
        echo "  Actual output: $output" >&2
        FAIL=$((FAIL + 1))
    fi
}

assert_file_contains() {
    local test_name="$1" file="$2" pattern="$3"
    if [ -f "$file" ] && grep -qE "$pattern" "$file"; then
        echo "  PASS: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $test_name — file '$file' did not contain '$pattern'" >&2
        FAIL=$((FAIL + 1))
    fi
}

echo "=== audit-rollout-status.sh tests ==="

# T1: --self-check ok (exit 0)
assert_exit "T1 --self-check exits 0" 0 \
    "$AUDIT_SCRIPT" --self-check

# T2: --set-mode warn → creates state file in test tmp dir
STATE_FILE="$TMP_DIR/rollout-mode-t2.json"
assert_exit "T2 --set-mode warn exits 0" 0 \
    "$AUDIT_SCRIPT" --set-mode warn --state-file "$STATE_FILE"
assert_file_contains "T2 state file contains warn mode" "$STATE_FILE" '"mode"\s*:\s*"warn"'

# T3: --set-mode fail → updates state file
STATE_FILE_T3="$TMP_DIR/rollout-mode-t3.json"
# First set to warn
"$AUDIT_SCRIPT" --set-mode warn --state-file "$STATE_FILE_T3" >/dev/null 2>&1 || true
# Then update to fail
assert_exit "T3 --set-mode fail exits 0" 0 \
    "$AUDIT_SCRIPT" --set-mode fail --state-file "$STATE_FILE_T3"
assert_file_contains "T3 state file updated to fail mode" "$STATE_FILE_T3" '"mode"\s*:\s*"fail"'

# T4: unknown flag → exit 2
assert_exit "T4 unknown flag exits 2" 2 \
    "$AUDIT_SCRIPT" --unknown-flag-xyz

# T5: invalid mode value → exit 2
assert_exit "T5 invalid mode value exits 2" 2 \
    "$AUDIT_SCRIPT" --set-mode banana

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
