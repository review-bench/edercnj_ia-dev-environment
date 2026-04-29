#!/usr/bin/env bash
# audit_recovery_mode_test.sh — Unit tests for audit-recovery-mode.sh
# Tests map 1-to-1 to Gherkin scenarios in story-0063-0017
# Layer: 3 (detectivo — CI audit)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-recovery-mode.sh"
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

echo "=== audit-recovery-mode.sh tests ==="

# T1: --self-check ok (jq on PATH)
assert_exit "T1 --self-check exits 0 when jq present" 0 \
    "$AUDIT_SCRIPT" --self-check

# T2: NDJSON with recovery events → exit 0 with report
NDJSON_WITH_EVENTS="$TMP_DIR/events_with_recovery.ndjson"
cat > "$NDJSON_WITH_EVENTS" <<'EOF'
{"type":"tool.call","timestamp":"2026-04-28T10:00:00Z","skill":"x-story-implement","storyId":"story-0063-0001","metadata":{"CLAUDE_RECOVERY_MODE":"1","bypassVector":"direct-commit"}}
{"type":"phase.start","timestamp":"2026-04-28T10:01:00Z","skill":"x-story-implement","storyId":"story-0063-0001","metadata":{}}
{"type":"tool.call","timestamp":"2026-04-28T11:00:00Z","skill":"x-task-implement","storyId":"story-0063-0002","metadata":{"CLAUDE_RECOVERY_MODE":"1","bypassVector":"skip-review"}}
EOF
assert_exit "T2 NDJSON with recovery events exits 0" 0 \
    "$AUDIT_SCRIPT" --ndjson-file "$NDJSON_WITH_EVENTS"
assert_output_contains "T2 output reports bypass count" "total.*bypass|bypass.*total|2" \
    "$AUDIT_SCRIPT" --ndjson-file "$NDJSON_WITH_EVENTS"

# T3: empty NDJSON → exit 0 (0 bypasses)
EMPTY_NDJSON="$TMP_DIR/events_empty.ndjson"
printf '' > "$EMPTY_NDJSON"
assert_exit "T3 empty NDJSON exits 0" 0 \
    "$AUDIT_SCRIPT" --ndjson-file "$EMPTY_NDJSON"
assert_output_contains "T3 output shows 0 bypasses" "0" \
    "$AUDIT_SCRIPT" --ndjson-file "$EMPTY_NDJSON"

# T4: missing NDJSON → exit 2
assert_exit "T4 missing NDJSON exits 2" 2 \
    "$AUDIT_SCRIPT" --ndjson-file "/nonexistent/events.ndjson"

# T5: --help / unknown flag handled (exit 2)
assert_exit "T5 unknown flag exits 2" 2 \
    "$AUDIT_SCRIPT" --unknown-flag

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
