#!/usr/bin/env bash
# preflight_test.sh — Tests for scripts/preflight.sh
# Tests map to Gherkin ACs in story-0063-0001 §5.2
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
PREFLIGHT="$REPO_ROOT/scripts/preflight.sh"
PASS=0; FAIL=0

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

echo "=== preflight.sh tests ==="

# T1: missing --scope → INVALID_ARGS (13)
assert_exit "T1 missing --scope → INVALID_ARGS" 13 \
    "$PREFLIGHT"

# T2: --scope=story sem --story-id → INVALID_ARGS (13)
assert_exit "T2 scope=story sem story-id → INVALID_ARGS" 13 \
    "$PREFLIGHT" --scope=story

# T3: --self-check exit 0
result=0
"$PREFLIGHT" --self-check >/dev/null 2>&1 || result=$?
if [ "$result" -eq 0 ] || [ "$result" -eq 6 ]; then
    echo "  PASS: T3 --self-check ok (exit $result)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T3 --self-check unexpected exit $result" >&2
    FAIL=$((FAIL + 1))
fi

# T4: --help or -h exit 0
assert_exit "T4 --help exits ok" 0 \
    "$PREFLIGHT" --help

# T5: unknown flag → INVALID_ARGS (13)
assert_exit "T5 unknown flag → INVALID_ARGS" 13 \
    "$PREFLIGHT" --invalid-flag

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
