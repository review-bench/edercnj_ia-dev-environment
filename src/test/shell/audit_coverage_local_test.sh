#!/usr/bin/env bash
# audit_coverage_local_test.sh — Unit tests for audit-coverage-local.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-coverage-local.sh"
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

make_csv() {
    local path="$1" line_pct="$2" branch_pct="$3"
    # CSV: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,...
    # Calculate from percentages: e.g., 95% = 95 covered, 5 missed
    local lm=$(echo "100 - $line_pct" | bc 2>/dev/null || echo "5")
    local lc="$line_pct"
    local bm=$(echo "100 - $branch_pct" | bc 2>/dev/null || echo "10")
    local bc="$branch_pct"
    {
        echo "GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED"
        echo "test,com.foo,Bar,0,100,${bm},${bc},${lm},${lc},0,0,0,0"
    } > "$path"
}

echo "=== audit-coverage-local.sh tests ==="

# T1: --self-check exit 0
assert_exit "T1 --self-check ok" 0 \
    "$AUDIT_SCRIPT" --self-check

# T2: Coverage acima do threshold (95/90) → exit 0
make_csv "$TMP_DIR/jacoco-good.csv" "96" "92"
assert_exit "T2 coverage acima threshold aceito" 0 \
    "$AUDIT_SCRIPT" --report-path="$TMP_DIR/jacoco-good.csv" --story-id=story-0063-0007

# T3: Line coverage abaixo (85%) → exit 1
make_csv "$TMP_DIR/jacoco-low-line.csv" "85" "92"
assert_exit "T3 line coverage abaixo rejeitado" 1 \
    "$AUDIT_SCRIPT" --report-path="$TMP_DIR/jacoco-low-line.csv" --story-id=story-0063-0007

# T4: Branch coverage abaixo (78%) → exit 1
make_csv "$TMP_DIR/jacoco-low-branch.csv" "96" "78"
assert_exit "T4 branch coverage abaixo rejeitado" 1 \
    "$AUDIT_SCRIPT" --report-path="$TMP_DIR/jacoco-low-branch.csv" --story-id=story-0063-0007

# T5: Custom thresholds aceita coverage menor
make_csv "$TMP_DIR/jacoco-custom.csv" "85" "78"
assert_exit "T5 custom thresholds menores aceitam" 0 \
    "$AUDIT_SCRIPT" --report-path="$TMP_DIR/jacoco-custom.csv" \
        --line-min=80 --branch-min=70 --story-id=story-0063-0007

# T6: Report ausente → exit 2
assert_exit "T6 report ausente rejeitado" 2 \
    "$AUDIT_SCRIPT" --report-path="/nonexistent/jacoco.csv" --story-id=story-0063-0007

# T7: Args inválidos → exit 3
assert_exit "T7 args invalidos rejeitados" 3 \
    "$AUDIT_SCRIPT" --invalid-flag

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
