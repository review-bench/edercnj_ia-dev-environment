#!/usr/bin/env bash
# audit_epic_review_reconciliation_test.sh — Unit tests for audit-epic-review-reconciliation.sh
# Tests map 1-to-1 to Gherkin scenarios in story-0063-0020
# Layer: 3 (detectivo — CI audit)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-epic-review-reconciliation.sh"
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

make_review() {
    local dir="$1" story_id="$2" decision="$3"
    mkdir -p "$dir"
    {
        echo "# Tech-Lead Review — $story_id"
        echo ""
        echo "## Overview"
        echo "Review of $story_id"
        echo ""
        echo "## Decision"
        echo ""
        echo "$decision"
    } > "$dir/techlead-review-story-$story_id.md"
}

echo "=== audit-epic-review-reconciliation.sh tests ==="

# T1: --self-check ok
assert_exit "T1 --self-check exits 0" 0 \
    "$AUDIT_SCRIPT" --self-check

# T2: all reviews GO → exit 0
EPIC_GO="$TMP_DIR/epic-all-go"
PLANS_GO="$EPIC_GO/plans"
make_review "$PLANS_GO" "story-0099-0001" "GO"
make_review "$PLANS_GO" "story-0099-0002" "GO"
make_review "$PLANS_GO" "story-0099-0003" "GO"
assert_exit "T2 all reviews GO exits 0" 0 \
    "$AUDIT_SCRIPT" --plans-dir "$PLANS_GO"

# T3: one review NO-GO → exit 1
EPIC_NOGO="$TMP_DIR/epic-one-nogo"
PLANS_NOGO="$EPIC_NOGO/plans"
make_review "$PLANS_NOGO" "story-0099-0001" "GO"
make_review "$PLANS_NOGO" "story-0099-0002" "NO-GO"
make_review "$PLANS_NOGO" "story-0099-0003" "GO"
assert_exit "T3 one review NO-GO exits 1" 1 \
    "$AUDIT_SCRIPT" --plans-dir "$PLANS_NOGO"

# T4: missing plans dir → exit 2
assert_exit "T4 missing plans dir exits 2" 2 \
    "$AUDIT_SCRIPT" --plans-dir "/nonexistent/plans"

# T5: empty reviews dir → exit 0 (nothing to fail)
EPIC_EMPTY="$TMP_DIR/epic-empty"
PLANS_EMPTY="$EPIC_EMPTY/plans"
mkdir -p "$PLANS_EMPTY"
assert_exit "T5 empty reviews dir exits 0" 0 \
    "$AUDIT_SCRIPT" --plans-dir "$PLANS_EMPTY"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
