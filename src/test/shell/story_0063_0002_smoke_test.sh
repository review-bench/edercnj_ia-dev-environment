#!/usr/bin/env bash
# story_0063_0002_smoke_test.sh — Smoke test for story-0063-0002 (audit gates)
# Validates both audit-review-content.sh and audit-verify-envelope.sh together
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_REVIEW="$REPO_ROOT/.claude/scripts/audit-review-content.sh"
AUDIT_ENVELOPE="$REPO_ROOT/.claude/scripts/audit-verify-envelope.sh"
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

echo "=== story-0063-0002 smoke test ==="

# Setup: Create a complete story envelope + review pair
review_file="$TMP_DIR/review-complete.md"
envelope_file="$TMP_DIR/envelope-complete.json"

# Legitimate review (50+ lines with 2+ file refs)
cat > "$review_file" <<'EOF'
# Story Review — 0063-0002: Content-Quality Audits

## Architecture Review

The implementation follows domain-driven design principles with clear separation of concerns throughout the layer hierarchy.
The new audit gates are properly isolated from business logic and correctly integrated into the CLI adapter.

### File Analysis
- src/main/java/dev/iadev/adapter/inbound/cli/GenerateCommand.java: proper command dispatch
- src/main/java/dev/iadev/adapter/inbound/cli/ValidateCommand.java: correct validation pattern
- java/src/main/resources/targets/claude/scripts/audit-review-content.sh: heuristic implementation sound
- src/test/shell/audit_review_content_test.sh: comprehensive test coverage

### Design Patterns
- No hardcoded paths or secrets discovered
- Proper use of dependency injection throughout
- Layer boundaries respected and validated
- Exception handling follows established patterns

## Code Quality Assessment

The implementation quality is high, with careful attention to edge cases and security concerns.

### Per-File Review
1. GenerateCommand.java: 24 lines, constructor injection, proper error handling
2. ValidateCommand.java: 18 lines, clean delegation to domain service
3. audit-review-content.sh: strict bash mode, input canonicalization applied

### Test Coverage Analysis
- Unit test coverage: 96% line, 91% branch (thresholds: 95%, 90%)
- All acceptance criteria tests passing
- Edge case scenarios covered (stub, exempt marker, --strict flag)
- Integration tests validate end-to-end flow

## Compliance Validation

### Rule 03 (Coding Standards)
✓ Maximum method length: 24 lines
✓ Constructor injection pattern used consistently
✓ No train-wreck dependencies observed

### Rule 05 (Quality Gates)
✓ Coverage: 96% line, 91% branch (meets thresholds)
✓ All tests passing
✓ No weak assertions

### Rule 06 (Security)
✓ No hardcoded credentials
✓ Path canonicalization: realpath + readlink -f applied
✓ Input validation: all file operations protected

## Recommendations for Future Work

1. Add user-facing CLI documentation in docs/cli-guide.md
2. Consider adding a --dry-run flag for safer exploration
3. Update CHANGELOG.md before merge
4. Monitor hook latency via telemetry

## Implementation Highlights

The implementation demonstrates excellent software craftsmanship with attention to detail.
All edge cases have been considered and properly handled.
The test coverage validates both happy path and error scenarios comprehensively.

## Conclusion

This story is production-ready. All acceptance criteria met. Code is clean, well-tested, and secure.

**GO**
EOF

# Valid envelope
cat > "$envelope_file" <<'EOF'
{
  "passed": true,
  "failures": [],
  "acCheckCount": 3,
  "acCheckResults": ["H1_NONZERO", "H2_NONZERO", "H3_NONZERO"],
  "coverageLine": 96.0,
  "coverageBranch": 91.0,
  "timestamp": "2026-04-28T18:45:33Z"
}
EOF

# T1: Both scripts pass on valid artifacts
assert_exit "T1a audit-review-content on legitimate review" 0 \
    "$AUDIT_REVIEW" --review-file "$review_file"
assert_exit "T1b audit-verify-envelope on valid envelope" 0 \
    "$AUDIT_ENVELOPE" --envelope-file "$envelope_file"

# T2: Review audit rejects stub
stub_review="$TMP_DIR/review-stub.md"
echo "# Review" > "$stub_review"
echo "GO" >> "$stub_review"
assert_exit "T2 stub review rejected" 1 \
    "$AUDIT_REVIEW" --review-file "$stub_review"

# T3: Envelope audit rejects invalid JSON
bad_envelope="$TMP_DIR/envelope-bad.json"
echo "{invalid json" > "$bad_envelope"
assert_exit "T3 invalid envelope rejected" 2 \
    "$AUDIT_ENVELOPE" --envelope-file "$bad_envelope"

# T4: Self-check passes for both scripts
assert_exit "T4a audit-review-content --self-check" 0 \
    "$AUDIT_REVIEW" --self-check
assert_exit "T4b audit-verify-envelope --self-check" 0 \
    "$AUDIT_ENVELOPE" --self-check

# T5: Missing files detected
assert_exit "T5a missing review file" 2 \
    "$AUDIT_REVIEW" --review-file "/nonexistent/review.md"
assert_exit "T5b missing envelope file" 2 \
    "$AUDIT_ENVELOPE" --envelope-file "/nonexistent/envelope.json"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
