#!/usr/bin/env bash
# audit_verify_envelope_test.sh — Unit tests for audit-verify-envelope.sh
# Tests map 1-to-1 to Gherkin scenarios in story-0063-0002 §5.3
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-verify-envelope.sh"
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

make_envelope() {
    local path="$1" passed="$2" failures_type="$3" ac_count="$4" ac_results="$5" coverage_line="$6" coverage_branch="$7" timestamp="$8"
    local f="$TMP_DIR/$path"
    mkdir -p "$(dirname "$f")"
    {
        echo "{"
        echo "  \"passed\": $passed,"
        if [ "$failures_type" = "array" ]; then
            echo "  \"failures\": [],"
        elif [ "$failures_type" = "string" ]; then
            echo "  \"failures\": \"stub\","
        elif [ "$failures_type" = "null" ]; then
            echo "  \"failures\": null,"
        fi
        echo "  \"acCheckCount\": $ac_count,"
        echo "  \"acCheckResults\": [ $(printf '"%s",' $(seq 1 "$ac_results") | sed 's/,$//') ],"
        echo "  \"coverageLine\": $coverage_line,"
        echo "  \"coverageBranch\": $coverage_branch,"
        echo "  \"timestamp\": \"$timestamp\""
        echo "}"
    } > "$f"
}

echo "=== audit-verify-envelope.sh tests ==="

# T1: valid envelope (passed=true, 4 AC met, coverage numeric, timestamp ISO8601) → exit 0
f1="$TMP_DIR/envelope-valid.json"
cat > "$f1" <<'EOF'
{
  "passed": true,
  "failures": [],
  "acCheckCount": 4,
  "acCheckResults": ["H1_NONZERO_RESULT", "H2_NONZERO_RESULT", "H3_NONZERO_RESULT", "H4_NONZERO_RESULT"],
  "coverageLine": 92.5,
  "coverageBranch": 87.3,
  "timestamp": "2026-04-28T18:45:33Z"
}
EOF
assert_exit "T1 valid envelope accepted" 0 \
    "$AUDIT_SCRIPT" --envelope-file "$f1"

# T2: failures field is string (invalid type) → exit 1
f2="$TMP_DIR/envelope-failures-string.json"
make_envelope "envelope-failures-string.json" "false" "string" "4" "0" "0" "0" "2026-04-28T18:45:33Z"
assert_exit "T2 failures as string rejected" 1 \
    "$AUDIT_SCRIPT" --envelope-file "$f2"

# T3: failures field is null (invalid type) → exit 1
f3="$TMP_DIR/envelope-failures-null.json"
make_envelope "envelope-failures-null.json" "false" "null" "4" "0" "0" "0" "2026-04-28T18:45:33Z"
assert_exit "T3 failures as null rejected" 1 \
    "$AUDIT_SCRIPT" --envelope-file "$f3"

# T4: passed=true but acCheckResults.length (0) < acCheckCount (4) → exit 1
f4="$TMP_DIR/envelope-ac-mismatch.json"
cat > "$f4" <<'EOF'
{
  "passed": true,
  "failures": [],
  "acCheckCount": 4,
  "acCheckResults": [],
  "coverageLine": 92.5,
  "coverageBranch": 87.3,
  "timestamp": "2026-04-28T18:45:33Z"
}
EOF
assert_exit "T4 AC count mismatch rejected" 1 \
    "$AUDIT_SCRIPT" --envelope-file "$f4"

# T5: coverageLine is string instead of numeric → exit 1
f5="$TMP_DIR/envelope-coverage-string.json"
cat > "$f5" <<'EOF'
{
  "passed": true,
  "failures": [],
  "acCheckCount": 4,
  "acCheckResults": ["R1", "R2", "R3", "R4"],
  "coverageLine": "92.5",
  "coverageBranch": 87.3,
  "timestamp": "2026-04-28T18:45:33Z"
}
EOF
assert_exit "T5 coverage as string rejected" 1 \
    "$AUDIT_SCRIPT" --envelope-file "$f5"

# T6: timestamp is not ISO8601 (malformed) → exit 1
f6="$TMP_DIR/envelope-timestamp-bad.json"
cat > "$f6" <<'EOF'
{
  "passed": true,
  "failures": [],
  "acCheckCount": 4,
  "acCheckResults": ["R1", "R2", "R3", "R4"],
  "coverageLine": 92.5,
  "coverageBranch": 87.3,
  "timestamp": "2026-04-28 18:45:33"
}
EOF
assert_exit "T6 timestamp not ISO8601 rejected" 1 \
    "$AUDIT_SCRIPT" --envelope-file "$f6"

# T7: --self-check with jq on PATH → exit 0
assert_exit "T7 --self-check ok" 0 \
    "$AUDIT_SCRIPT" --self-check

# T8: missing envelope file → exit 2
assert_exit "T8 missing envelope file" 2 \
    "$AUDIT_SCRIPT" --envelope-file "/nonexistent/envelope.json"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
