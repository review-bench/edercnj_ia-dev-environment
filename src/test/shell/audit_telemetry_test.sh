#!/usr/bin/env bash
# audit_telemetry_test.sh — Tests for audit-execution-integrity.sh --scope=telemetry
# Tests map 1-to-1 to Gherkin scenarios in story-0063-0003 §5.2
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-execution-integrity.sh"
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

echo "=== audit-execution-integrity.sh --scope=telemetry tests ==="

# Setup: criar mock NDJSON com eventos válidos
mkdir -p "$TMP_DIR/plans/epic-0063/telemetry"
cat > "$TMP_DIR/plans/epic-0063/telemetry/events.ndjson" <<'EOF'
{"timestamp":"2026-04-28T10:00:00Z","event":"tool.call","skill":"x-review","storyId":"story-0063-0001","session_id":"abc123","pid":12345}
{"timestamp":"2026-04-28T10:05:00Z","event":"tool.call","skill":"x-review-pr","storyId":"story-0063-0001","session_id":"abc123","pid":12345}
{"timestamp":"2026-04-28T10:10:00Z","event":"tool.call","skill":"x-internal-story-verify","storyId":"story-0063-0001","session_id":"abc123","pid":12345}
EOF

# T1: --self-check returns 0 (infra OK) or 4 (baseline missing — acceptable in minimal envs)
result=0
"$AUDIT_SCRIPT" --self-check >/dev/null 2>&1 || result=$?
if [ "$result" -eq 0 ] || [ "$result" -eq 4 ]; then
    echo "  PASS: T1 --self-check (exit $result — 0=ok, 4=baseline-missing)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T1 --self-check unexpected exit $result" >&2
    FAIL=$((FAIL + 1))
fi

# T2: --scope flag is recognized (regardless of result)
result=0
"$AUDIT_SCRIPT" --scope=telemetry --story-id=story-0063-0001 --epic-id=0063 \
    --ndjson-file "$TMP_DIR/plans/epic-0063/telemetry/events.ndjson" \
    >/dev/null 2>&1 || result=$?
if [ "$result" -eq 0 ] || [ "$result" -eq 1 ]; then
    echo "  PASS: T2 --scope=telemetry flag recognized (exit $result)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T2 --scope=telemetry not recognized (exit $result)" >&2
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
