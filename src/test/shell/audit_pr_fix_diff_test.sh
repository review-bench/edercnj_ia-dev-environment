#!/usr/bin/env bash
# audit_pr_fix_diff_test.sh — Unit tests for audit-pr-fix-diff.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
SCRIPT="$REPO_ROOT/scripts/audit-pr-fix-diff.sh"
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

echo "=== audit-pr-fix-diff.sh tests ==="

# T1: --self-check exit 0
assert_exit "T1 --self-check ok" 0 \
    "$SCRIPT" --self-check

# T2: missing --pre-sha → exit 5
assert_exit "T2 missing --pre-sha rejected" 5 \
    "$SCRIPT" --review-file=/tmp/foo

# T3: missing --review-file → exit 5
assert_exit "T3 missing --review-file rejected" 5 \
    "$SCRIPT" --pre-sha=abc123

# T4: review-file missing → exit 4
assert_exit "T4 review-file ausente rejected" 4 \
    "$SCRIPT" --pre-sha=abc123 --review-file=/nonexistent.md

# T5: HEAD unchanged → FIX_NO_DIFF (exit 1)
echo "## Review" > "$TMP_DIR/review.md"
echo "src/main/java/Foo.java:42 has issue" >> "$TMP_DIR/review.md"
CURRENT_SHA=$(cd "$REPO_ROOT" && git rev-parse HEAD)
assert_exit "T5 HEAD unchanged → FIX_NO_DIFF" 1 \
    bash -c "cd '$REPO_ROOT' && '$SCRIPT' --pre-sha='$CURRENT_SHA' --review-file='$TMP_DIR/review.md'"

# T6: invalid args → exit 5
assert_exit "T6 invalid args rejected" 5 \
    "$SCRIPT" --invalid-flag

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
