#!/usr/bin/env bash
# setup_branch_protection_test.sh — Tests for setup-branch-protection.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
SCRIPT="$REPO_ROOT/scripts/setup-branch-protection.sh"
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

echo "=== setup-branch-protection.sh tests ==="

# T1: --self-check (gh + jq + required-checks.txt presentes)
result=0
"$SCRIPT" --self-check >/dev/null 2>&1 || result=$?
if [ "$result" -eq 0 ] || [ "$result" -eq 1 ]; then
    echo "  PASS: T1 --self-check (exit $result — 0=ok, 1=missing prereq)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T1 --self-check unexpected exit $result" >&2
    FAIL=$((FAIL + 1))
fi

# T2: Args inválidos rejeitados
assert_exit "T2 args invalidos rejeitados" 2 \
    "$SCRIPT" --invalid-flag

# T3: --apply-strict é parseado (dry-run para evitar API call)
result=0
"$SCRIPT" --dry-run --apply-strict >/dev/null 2>&1 || result=$?
if [ "$result" -eq 0 ] || [ "$result" -eq 1 ]; then
    echo "  PASS: T3 --apply-strict + --dry-run (exit $result)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T3 --apply-strict unexpected exit $result" >&2
    FAIL=$((FAIL + 1))
fi

# T4: Script contém flag --apply-strict definida
if grep -q "APPLY_STRICT=" "$SCRIPT" && grep -q "required_linear_history" "$SCRIPT"; then
    echo "  PASS: T4 --apply-strict implementation present"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T4 --apply-strict implementation missing" >&2
    FAIL=$((FAIL + 1))
fi

# T5: Script suporta required_linear_history, allow_force_pushes:false, allow_deletions:false
if grep -q "allow_force_pushes: false" "$SCRIPT" && grep -q "allow_deletions: false" "$SCRIPT"; then
    echo "  PASS: T5 strict mode includes force-push + deletion blocks"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T5 strict mode incomplete" >&2
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
