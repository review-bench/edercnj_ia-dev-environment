#!/usr/bin/env bash
# audit_wave_dispatch_test.sh — Unit tests for audit-wave-dispatch.sh
# TDD RED phase: all tests expected to fail until implementation exists
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-wave-dispatch.sh"
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

echo "=== audit-wave-dispatch.sh tests ==="

# T1: --self-check exits 0 when jq is available
assert_exit "T1 --self-check ok" 0 \
    "$AUDIT_SCRIPT" --self-check

# T2: NDJSON with 3 subagent.start events clustered (< 30s apart) → exit 0
CLUSTERED_NDJSON="$TMP_DIR/clustered-events.ndjson"
cat > "$CLUSTERED_NDJSON" <<'NDJSON'
{"type":"subagent.start","skill":"x-story-implement","role":"Architect","storyId":"story-0063-0014","ts":"2026-04-28T10:00:00Z"}
{"type":"subagent.start","skill":"x-story-implement","role":"QA","storyId":"story-0063-0014","ts":"2026-04-28T10:00:10Z"}
{"type":"subagent.start","skill":"x-story-implement","role":"Security","storyId":"story-0063-0014","ts":"2026-04-28T10:00:20Z"}
{"type":"phase.end","skill":"x-story-implement","storyId":"story-0063-0014","ts":"2026-04-28T10:02:00Z"}
NDJSON
assert_exit "T2 3 clustered subagent.start events exits 0" 0 \
    "$AUDIT_SCRIPT" --ndjson-file "$CLUSTERED_NDJSON" --story-id "story-0063-0014" --expected-wave-size 3

# T3: NDJSON with 0 subagent.start events → exit 1 (WAVE_DISPATCH_INCOMPLETE)
EMPTY_NDJSON="$TMP_DIR/empty-events.ndjson"
cat > "$EMPTY_NDJSON" <<'NDJSON'
{"type":"phase.start","skill":"x-story-implement","storyId":"story-0063-0014","ts":"2026-04-28T10:00:00Z"}
{"type":"phase.end","skill":"x-story-implement","storyId":"story-0063-0014","ts":"2026-04-28T10:02:00Z"}
NDJSON
assert_exit "T3 0 subagent.start events exits 1" 1 \
    "$AUDIT_SCRIPT" --ndjson-file "$EMPTY_NDJSON" --story-id "story-0063-0014" --expected-wave-size 3

# T4: missing NDJSON file → exit 2 (OPERATIONAL_ERROR)
assert_exit "T4 missing NDJSON file exits 2" 2 \
    "$AUDIT_SCRIPT" --ndjson-file "/nonexistent/events.ndjson" --story-id "story-0063-0014" --expected-wave-size 3

# T5: unknown flag → exit 2 (OPERATIONAL_ERROR)
assert_exit "T5 unknown flag exits 2" 2 \
    "$AUDIT_SCRIPT" --unknown-flag

# T6: NDJSON with agents dispatched serially (> 30s apart) → exit 1 (WAVE_DISPATCH_INCOMPLETE)
SERIAL_NDJSON="$TMP_DIR/serial-events.ndjson"
cat > "$SERIAL_NDJSON" <<'NDJSON'
{"type":"subagent.start","skill":"x-story-implement","role":"Architect","storyId":"story-0063-0014","ts":"2026-04-28T10:00:00Z"}
{"type":"subagent.end","skill":"x-story-implement","role":"Architect","storyId":"story-0063-0014","ts":"2026-04-28T10:01:00Z"}
{"type":"subagent.start","skill":"x-story-implement","role":"QA","storyId":"story-0063-0014","ts":"2026-04-28T10:02:00Z"}
{"type":"subagent.end","skill":"x-story-implement","role":"QA","storyId":"story-0063-0014","ts":"2026-04-28T10:03:00Z"}
{"type":"subagent.start","skill":"x-story-implement","role":"Security","storyId":"story-0063-0014","ts":"2026-04-28T10:04:00Z"}
NDJSON
assert_exit "T6 3 serial subagent.start events exits 1" 1 \
    "$AUDIT_SCRIPT" --ndjson-file "$SERIAL_NDJSON" --story-id "story-0063-0014" --expected-wave-size 3

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
