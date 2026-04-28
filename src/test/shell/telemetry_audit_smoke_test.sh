#!/usr/bin/env bash
# telemetry_audit_smoke_test.sh — Smoke test for story-0063-0003
# Validates stage-telemetry.sh + audit-execution-integrity.sh --scope=telemetry together
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
HOOK_SCRIPT="$REPO_ROOT/.claude/hooks/stage-telemetry.sh"
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

echo "=== story-0063-0003 telemetry smoke test ==="

# Setup: criar NDJSON sandbox válido
mkdir -p "$TMP_DIR/plans/epic-0063/telemetry"
NDJSON="$TMP_DIR/plans/epic-0063/telemetry/events.ndjson"
cat > "$NDJSON" <<'EOF'
{"timestamp":"2026-04-28T10:00:00Z","event":"tool.call","skill":"x-review","storyId":"story-0063-0001","session_id":"session-001","pid":12345}
{"timestamp":"2026-04-28T10:05:00Z","event":"tool.call","skill":"x-review-pr","storyId":"story-0063-0001","session_id":"session-001","pid":12345}
{"timestamp":"2026-04-28T10:10:00Z","event":"tool.call","skill":"x-internal-story-verify","storyId":"story-0063-0001","session_id":"session-001","pid":12345}
EOF

# T1 Happy path: NDJSON válido, audit aceita
assert_exit "T1 happy path — todos eventos presentes" 0 \
    "$AUDIT_SCRIPT" --scope=telemetry --story-id=story-0063-0001 --ndjson-file "$NDJSON"

# T2: stage-telemetry hook executa (no-op safe quando nenhum repo git)
assert_exit "T2 stage-telemetry hook executa em stop event" 0 \
    bash -c "cd '$TMP_DIR' && CLAUDE_PROJECT_DIR='$TMP_DIR' '$HOOK_SCRIPT'"

# T3: NDJSON ausente → erro claro
assert_exit "T3 telemetry file ausente rejeitado" 1 \
    "$AUDIT_SCRIPT" --scope=telemetry --story-id=story-0063-0001 \
        --ndjson-file "/nonexistent/events.ndjson"

# T4: Story sem eventos matching → rejeitado
assert_exit "T4 story sem eventos matching rejeitada" 1 \
    "$AUDIT_SCRIPT" --scope=telemetry --story-id=story-0063-9999 --ndjson-file "$NDJSON"

# T5: NDJSON com regressão de timestamp na mesma sessão → rejeitado
BAD_NDJSON="$TMP_DIR/bad.ndjson"
cat > "$BAD_NDJSON" <<'EOF'
{"timestamp":"2026-04-28T12:00:00Z","event":"tool.call","skill":"x-review","storyId":"story-0063-0001","session_id":"abc","pid":111}
{"timestamp":"2026-04-28T12:05:00Z","event":"tool.call","skill":"x-review-pr","storyId":"story-0063-0001","session_id":"abc","pid":111}
{"timestamp":"2026-04-28T12:01:00Z","event":"tool.call","skill":"x-internal-story-verify","storyId":"story-0063-0001","session_id":"abc","pid":111}
EOF
assert_exit "T5 regressao de timestamp detectada" 1 \
    "$AUDIT_SCRIPT" --scope=telemetry --story-id=story-0063-0001 --ndjson-file "$BAD_NDJSON"

# T6: --self-check ok (tolera baseline ausente em dev env)
result=0
"$AUDIT_SCRIPT" --self-check >/dev/null 2>&1 || result=$?
if [ "$result" -eq 0 ] || [ "$result" -eq 4 ]; then
    echo "  PASS: T6 --self-check (exit $result)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T6 --self-check unexpected exit $result" >&2
    FAIL=$((FAIL + 1))
fi

# T7: stage-telemetry fail-OPEN sem CLAUDE_PROJECT_DIR
assert_exit "T7 stage-telemetry fail-OPEN sem env" 0 \
    bash -c "cd '$TMP_DIR' && unset CLAUDE_PROJECT_DIR && '$HOOK_SCRIPT'"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
