#!/usr/bin/env bash
# audit_review_content_test.sh — Unit tests for audit-review-content.sh
# Tests map 1-to-1 to Gherkin scenarios in story-0063-0002 §5.2
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-review-content.sh"
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
    local path="$1" lines="$2" sections="$3" file_refs="$4" decision="$5"
    local f="$TMP_DIR/$path"
    mkdir -p "$(dirname "$f")"
    {
        # sections H2
        for i in $(seq 1 "$sections"); do echo "## Section $i"; done
        # file refs
        for i in $(seq 1 "$file_refs"); do echo "- src/main/java/Foo$i.java was reviewed"; done
        # decision marker
        [ -n "$decision" ] && echo "$decision"
        # pad to line count
        local current
        current=$(wc -l < "$f" 2>/dev/null || echo 0)
        for _ in $(seq "$current" "$lines"); do echo "filler line"; done
    } > "$f"
}

echo "=== audit-review-content.sh tests ==="

# T1: stub review (2 lines) → exit 1
f1="$TMP_DIR/review-stub.md"
printf '# Review\nVerdict: GO\n' > "$f1"
assert_exit "T1 stub review rejected" 1 \
    "$AUDIT_SCRIPT" --review-file "$f1"

# T2: legitimate review (87 lines, 4 H2 sections, 6 file refs, GO) → exit 0
f2="$TMP_DIR/review-legit.md"
{
    echo "## Findings"; echo "## Decision"; echo "## Per-file observations"; echo "## Recommendations"
    for i in 1 2 3 4 5 6; do echo "- src/main/java/Class$i.java reviewed"; done
    echo "GO"
    for _ in $(seq 1 74); do echo "detailed review text here"; done
} > "$f2"
assert_exit "T2 legitimate review accepted" 0 \
    "$AUDIT_SCRIPT" --review-file "$f2"

# T3: verbose but no decision marker → exit 1
f3="$TMP_DIR/review-no-decision.md"
{
    echo "## Findings"; echo "## Observations"; echo "## Details"
    for i in 1 2; do echo "- src/main/java/Foo$i.java checked"; done
    for _ in $(seq 1 94); do echo "detailed analysis without verdict"; done
} > "$f3"
assert_exit "T3 no decision marker rejected" 1 \
    "$AUDIT_SCRIPT" --review-file "$f3"

# T4: exemption marker without --strict → exit 0 (with warning on stderr)
f4="$TMP_DIR/review-exempt.md"
{
    echo "<!-- audit-exempt-content: SIMPLE — 23 lines justified by 1-task scope -->"
    echo "## Decision"
    echo "GO"
    echo "- src/main/java/Foo.java"
    echo "- src/main/java/Bar.java"
    for _ in $(seq 1 17); do echo "text"; done
} > "$f4"
assert_exit "T4 exempt marker without --strict accepted" 0 \
    "$AUDIT_SCRIPT" --review-file "$f4"

# T5: exemption marker with --strict → exit 1
assert_exit "T5 --strict ignores exempt marker" 1 \
    "$AUDIT_SCRIPT" --review-file "$f4" --strict

# T6: --self-check with jq on PATH → exit 0
assert_exit "T6 --self-check ok" 0 \
    "$AUDIT_SCRIPT" --self-check

# T7: missing file → exit 2
assert_exit "T7 missing review file" 2 \
    "$AUDIT_SCRIPT" --review-file "/nonexistent/review.md"

# T8: no decision in 100-line verbose review → exit 1 with clear message
f8="$TMP_DIR/review-verbose-no-verdict.md"
{
    echo "## Section1"; echo "## Section2"; echo "## Section3"
    echo "- src/main/java/A.java"; echo "- src/main/java/B.java"
    for _ in $(seq 1 94); do echo "analysis without verdict marker"; done
} > "$f8"
assert_exit "T8 100-line review without decision rejected" 1 \
    "$AUDIT_SCRIPT" --review-file "$f8"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
