#!/usr/bin/env bash
# audit_ndjson_hash_chain_test.sh — Unit tests for audit-ndjson-hash-chain.sh
# TDD RED phase: all tests expected to fail until implementation exists
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-ndjson-hash-chain.sh"
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

echo "=== audit-ndjson-hash-chain.sh tests ==="

# T1: --self-check exits 0 when sha256sum or shasum available
assert_exit "T1 --self-check ok" 0 \
    "$AUDIT_SCRIPT" --self-check

# T2: --init on an existing (empty) NDJSON file → exit 0, creates chain anchor
EMPTY_NDJSON="$TMP_DIR/empty-events.ndjson"
touch "$EMPTY_NDJSON"
STATE_DIR="$TMP_DIR/state"
mkdir -p "$STATE_DIR"
assert_exit "T2 --init on empty NDJSON exits 0" 0 \
    "$AUDIT_SCRIPT" --ndjson-file "$EMPTY_NDJSON" --state-dir "$STATE_DIR" --epic-id epic-test --init

# T3: missing NDJSON file → exit 2
assert_exit "T3 missing NDJSON file exits 2" 2 \
    "$AUDIT_SCRIPT" --ndjson-file "/nonexistent/events.ndjson" --state-dir "$STATE_DIR" --epic-id epic-test --verify

# T4: unknown flag → exit 2
assert_exit "T4 unknown flag exits 2" 2 \
    "$AUDIT_SCRIPT" --unknown-flag

# T5: --verify with a valid single-line NDJSON after --init → exit 0
SINGLE_NDJSON="$TMP_DIR/single-events.ndjson"
echo '{"type":"phase.start","skill":"x-story-implement","ts":"2026-04-28T10:00:00Z"}' > "$SINGLE_NDJSON"
STATE_DIR2="$TMP_DIR/state2"
mkdir -p "$STATE_DIR2"
# init first, then verify
"$AUDIT_SCRIPT" --ndjson-file "$SINGLE_NDJSON" --state-dir "$STATE_DIR2" --epic-id epic-test2 --init >/dev/null 2>&1 || true
assert_exit "T5 --verify with valid single-line NDJSON exits 0" 0 \
    "$AUDIT_SCRIPT" --ndjson-file "$SINGLE_NDJSON" --state-dir "$STATE_DIR2" --epic-id epic-test2 --verify

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
