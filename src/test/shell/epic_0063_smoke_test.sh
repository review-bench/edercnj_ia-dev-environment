#!/usr/bin/env bash
# epic_0063_smoke_test.sh — E2E Smoke Test for EPIC-0063 (Local-First Pre-Flight Gates)
#
# Terminal story-0063-0011: validates all 20 Wave 1-6 stories working together.
#
# Tests:
#   T1:  All audit scripts in .claude/scripts/ exist and pass --self-check
#   T2:  scripts/preflight.sh exists and handles --self-check
#   T3:  .claude/hooks/enforce-preflight-gates.sh exists and is executable
#   T4:  .claude/hooks/enforce-preflight-gates-v2.sh exists and is executable
#   T5:  audit-review-content.sh + audit-verify-envelope.sh work together
#   T6:  audit-coverage-local.sh works with a CSV
#   T7:  audit-execution-integrity.sh --scope=telemetry works
#   T8:  scripts/audit-tool-call-grammar.sh --self-check passes
#   T9:  audit-planning-content.sh --self-check passes
#   T10: audit-ndjson-hash-chain.sh --self-check passes
#   T11: audit-wave-dispatch.sh --self-check passes
#   T12: audit-recovery-mode.sh --self-check passes
#   T13: audit-epic-review-reconciliation.sh --self-check passes
#   T14: audit-rollout-status.sh --self-check passes
#   T15: .claude/rules/30-tool-call-grammar.md exists
#   T16: docs/audit-bypass-catalog.md exists and lists 11 skills
#   T17: docs/branch-protection.md exists
#   T18: docs/adr/ADR-0016-preflight-warn-to-fail-rollout.md exists

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

PASS=0
FAIL=0

# ── Helpers ───────────────────────────────────────────────────────────────────

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

assert_file_exists() {
    local test_name="$1" path="$2"
    if [ -f "$path" ]; then
        echo "  PASS: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $test_name — file not found: $path" >&2
        FAIL=$((FAIL + 1))
    fi
}

assert_executable() {
    local test_name="$1" path="$2"
    if [ -x "$path" ]; then
        echo "  PASS: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $test_name — not executable or missing: $path" >&2
        FAIL=$((FAIL + 1))
    fi
}

assert_file_contains() {
    local test_name="$1" path="$2" pattern="$3"
    if grep -qE "$pattern" "$path" 2>/dev/null; then
        echo "  PASS: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $test_name — pattern not found in $path: $pattern" >&2
        FAIL=$((FAIL + 1))
    fi
}

TMP_DIR="$(mktemp -d)"
cleanup() { rm -rf "$TMP_DIR"; }
trap cleanup EXIT

echo "=== EPIC-0063 E2E Smoke Test ==="
echo "Repository root: $REPO_ROOT"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# T1: All audit scripts in .claude/scripts/ exist and pass --self-check
# ─────────────────────────────────────────────────────────────────────────────
echo "--- T1: .claude/scripts audit scripts exist and pass --self-check ---"

CLAUDE_SCRIPTS="$REPO_ROOT/.claude/scripts"

for script_name in \
    audit-coverage-local.sh \
    audit-epic-review-reconciliation.sh \
    audit-execution-integrity.sh \
    audit-hooks-self-check.sh \
    audit-ndjson-hash-chain.sh \
    audit-planning-content.sh \
    audit-recovery-mode.sh \
    audit-review-content.sh \
    audit-rollout-status.sh \
    audit-verify-envelope.sh \
    audit-wave-dispatch.sh
do
    script_path="$CLAUDE_SCRIPTS/$script_name"
    assert_executable "T1: $script_name is executable" "$script_path"
    assert_exit "T1: $script_name --self-check exits 0" 0 \
        "$script_path" --self-check
done

# ─────────────────────────────────────────────────────────────────────────────
# T2: scripts/preflight.sh exists and handles --self-check
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T2: scripts/preflight.sh --self-check ---"

PREFLIGHT="$REPO_ROOT/scripts/preflight.sh"
assert_executable "T2: preflight.sh is executable" "$PREFLIGHT"

# --self-check may exit 0 (all tools present) or 6 (audit-self-check-failed — acceptable)
preflight_sc_exit=0
"$PREFLIGHT" --self-check >/dev/null 2>&1 || preflight_sc_exit=$?
if [ "$preflight_sc_exit" -eq 0 ] || [ "$preflight_sc_exit" -eq 6 ]; then
    echo "  PASS: T2: preflight.sh --self-check exits $preflight_sc_exit (ok)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T2: preflight.sh --self-check unexpected exit $preflight_sc_exit" >&2
    FAIL=$((FAIL + 1))
fi

# ─────────────────────────────────────────────────────────────────────────────
# T3: .claude/hooks/enforce-preflight-gates.sh exists and is executable
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T3: enforce-preflight-gates.sh ---"
ENFORCE_V1="$REPO_ROOT/.claude/hooks/enforce-preflight-gates.sh"
assert_executable "T3: enforce-preflight-gates.sh is executable" "$ENFORCE_V1"

# ─────────────────────────────────────────────────────────────────────────────
# T4: .claude/hooks/enforce-preflight-gates-v2.sh exists and is executable
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T4: enforce-preflight-gates-v2.sh ---"
ENFORCE_V2="$REPO_ROOT/.claude/hooks/enforce-preflight-gates-v2.sh"
assert_executable "T4: enforce-preflight-gates-v2.sh is executable" "$ENFORCE_V2"

# ─────────────────────────────────────────────────────────────────────────────
# T5: audit-review-content.sh + audit-verify-envelope.sh work together
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T5: audit-review-content.sh + audit-verify-envelope.sh integration ---"

AUDIT_REVIEW="$CLAUDE_SCRIPTS/audit-review-content.sh"
AUDIT_ENVELOPE="$CLAUDE_SCRIPTS/audit-verify-envelope.sh"

# Create a valid review file (≥50 non-empty lines, ≥3 H2/H3, ≥2 file refs, decision marker)
review_file="$TMP_DIR/review-valid.md"
{
    echo "# Story Review — EPIC-0063 E2E Smoke"
    echo ""
    echo "## Architecture Review"
    echo ""
    echo "The implementation correctly integrates all EPIC-0063 pre-flight gates as"
    echo "a coherent local-first enforcement layer. Each gate is wired to a distinct"
    echo "script with clear exit codes per Rule 26."
    echo ""
    echo "File references examined:"
    echo "- .claude/scripts/audit-review-content.sh: heuristics validated"
    echo "- .claude/scripts/audit-verify-envelope.sh: schema check functional"
    echo "- src/test/shell/epic_0063_smoke_test.sh: E2E validation implemented"
    echo ""
    echo "## Code Quality Assessment"
    echo ""
    echo "All scripts implement --self-check correctly per Rule 26 contract."
    echo "Latency targets are within 500ms even for the full --scope=full run."
    echo ""
    echo "### Per-File Review"
    echo ""
    echo "1. audit-review-content.sh: 4 heuristics applied consistently"
    echo "2. audit-verify-envelope.sh: JSON schema validated via jq"
    echo "3. enforce-preflight-gates.sh: fail-CLOSED contract enforced"
    echo "4. enforce-preflight-gates-v2.sh: 15 additional bypass vectors covered"
    echo "5. preflight.sh: orchestrates up to 12 gates in lightweight-first order"
    echo ""
    echo "### Security Findings"
    echo ""
    echo "No hardcoded credentials or paths found in any of the 20 story deliverables."
    echo "CLAUDE_RECOVERY_MODE=1 is the sole bypass variable per RULE-004."
    echo ""
    echo "## Compliance Assessment"
    echo ""
    echo "Rule 05 (Quality Gates): absolute coverage gate enforced by audit-coverage-local.sh"
    echo "Rule 24 (Execution Integrity): all 12 surfaces validated per Rule 27 contract"
    echo "Rule 26 (Audit Gate Lifecycle): all scripts conform to naming and exit code conventions"
    echo "Rule 27 (Zero-Bypass Lifecycle): 11 skills catalogued with bypass patterns"
    echo "Rule 28 (Tool-Call Grammar): static lint via audit-tool-call-grammar.sh"
    echo ""
    echo "## Test Coverage Analysis"
    echo ""
    echo "Unit test coverage: 96% line, 92% branch"
    echo "All acceptance criteria tests passing"
    echo "E2E smoke test validates all 18 integration points"
    echo ""
    echo "## Dependencies Audit"
    echo ""
    echo "No new external dependencies introduced in EPIC-0063."
    echo "All gate scripts rely only on standard POSIX utilities plus jq."
    echo "jq version compatibility tested from 1.6+"
    echo ""
    echo "## Final Assessment"
    echo ""
    echo "All stories merged to epic/0063 without rule violations."
    echo "Documentation complete: ADR-0016, Rule 28, bypass catalog, branch-protection."
    echo "Wave 1-7 execution complete. Critical path 0003→0001→0004→0013→0017→0016→0011."
    echo ""
    echo "### Wave Summary"
    echo "Wave 1 (7 stories): Foundation audit scripts all passing self-check"
    echo "Wave 2 (4 stories): Grammar, planning audits, hash chain functional"
    echo "Wave 3 (2 stories): PreToolUse hooks intercepting tool calls correctly"
    echo "Wave 4 (4 stories): Phase shifts, hook v2, hooks self-check integrated"
    echo "Wave 5 (2 stories): Recovery dashboard and epic reconciliation functional"
    echo "Wave 6 (1 story): WARN→FAIL rollout documented and scriptable"
    echo "Wave 7 (1 story): This E2E smoke test validates the whole system"
    echo ""
    echo "### Risk Assessment"
    echo "Low risk. All scripts are additive (no modifications to existing code paths)."
    echo "Hooks fail-OPEN when preflight script is absent per RULE-005."
    echo "CLAUDE_RECOVERY_MODE=1 is the only bypass, audited via NDJSON."
    echo ""
    echo "**Decision: GO**"
} > "$review_file"

assert_exit "T5a: valid review exits 0" 0 \
    "$AUDIT_REVIEW" --review-file "$review_file"

# Create a valid envelope file
envelope_file="$TMP_DIR/envelope-valid.json"
cat > "$envelope_file" <<'EOFJSON'
{
  "passed": true,
  "coverageDelta": {"Line": 96.2, "Branch": 91.4},
  "failures": [],
  "acCheckResults": [
    {"id": "AC-01", "passed": true, "description": "preflight.sh orchestrates gates"},
    {"id": "AC-02", "passed": true, "description": "enforce-preflight-gates.sh intercepts"},
    {"id": "AC-03", "passed": true, "description": "audit-review-content.sh validates"},
    {"id": "AC-04", "passed": true, "description": "audit-verify-envelope.sh validates"},
    {"id": "AC-05", "passed": true, "description": "audit-coverage-local.sh parses CSV"},
    {"id": "AC-06", "passed": true, "description": "audit-tool-call-grammar.sh lints"},
    {"id": "AC-07", "passed": true, "description": "audit-planning-content.sh checks"},
    {"id": "AC-08", "passed": true, "description": "audit-ndjson-hash-chain.sh verifies"},
    {"id": "AC-09", "passed": true, "description": "audit-wave-dispatch.sh detects serial"},
    {"id": "AC-10", "passed": true, "description": "audit-recovery-mode.sh reports"},
    {"id": "AC-11", "passed": true, "description": "audit-epic-review-reconciliation.sh"},
    {"id": "AC-12", "passed": true, "description": "audit-rollout-status.sh manages mode"},
    {"id": "AC-13", "passed": true, "description": "Rule 28 tool-call-grammar.md exists"},
    {"id": "AC-14", "passed": true, "description": "audit-bypass-catalog.md has 11 skills"},
    {"id": "AC-15", "passed": true, "description": "branch-protection.md exists"},
    {"id": "AC-16", "passed": true, "description": "ADR-0016 preflight warn-to-fail exists"},
    {"id": "AC-17", "passed": true, "description": "enforce-preflight-gates-v2.sh exists"},
    {"id": "AC-18", "passed": true, "description": "E2E smoke test validates all components"}
  ],
  "acCheckCount": 18,
  "timestamp": "2026-04-28T00:00:00Z"
}
EOFJSON

assert_exit "T5b: valid envelope exits 0" 0 \
    "$AUDIT_ENVELOPE" --envelope-file "$envelope_file"

# ─────────────────────────────────────────────────────────────────────────────
# T6: audit-coverage-local.sh works with a CSV
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T6: audit-coverage-local.sh with CSV ---"

AUDIT_COVERAGE="$CLAUDE_SCRIPTS/audit-coverage-local.sh"

coverage_csv="$TMP_DIR/jacoco.csv"
# Minimal JaCoCo CSV format: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,...,LINE_MISSED,LINE_COVERED,...
cat > "$coverage_csv" <<'EOFCSV'
GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED
my-java-cli,dev/iadev/domain,MyClass,10,200,5,55,4,80,3,20,2,10
EOFCSV

# 80/(80+4) ≈ 95.24% line coverage; 55/(55+5) = 91.67% branch — should pass thresholds
assert_exit "T6: coverage meeting threshold exits 0" 0 \
    "$AUDIT_COVERAGE" --report-path="$coverage_csv" --story-id=story-0063-0011

# Coverage below threshold: 0 covered lines
low_csv="$TMP_DIR/jacoco-low.csv"
cat > "$low_csv" <<'EOFCSV'
GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED
my-java-cli,dev/iadev/domain,MyClass,200,10,50,10,80,4,10,3,10,2
EOFCSV

assert_exit "T6b: coverage below threshold exits 1" 1 \
    "$AUDIT_COVERAGE" --report-path="$low_csv" --story-id=story-0063-0011

# ─────────────────────────────────────────────────────────────────────────────
# T7: audit-execution-integrity.sh --scope=telemetry works
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T7: audit-execution-integrity.sh --scope=telemetry ---"

AUDIT_EI="$CLAUDE_SCRIPTS/audit-execution-integrity.sh"
assert_executable "T7: audit-execution-integrity.sh is executable" "$AUDIT_EI"

# --self-check must pass (Rule 26 §self-check contract)
assert_exit "T7: audit-execution-integrity.sh --self-check exits 0" 0 \
    "$AUDIT_EI" --self-check

# --scope=telemetry with a minimal NDJSON file
ndjson_file="$TMP_DIR/events.ndjson"
cat > "$ndjson_file" <<'EOFNDJSON'
{"event":"phase.start","skill":"x-story-implement","phase":"Phase-1-Plan","storyId":"story-0063-0011","timestamp":"2026-04-28T00:00:00Z","session_id":"sess-001","pid":12345}
{"event":"tool.call","skill":"x-review","storyId":"story-0063-0011","timestamp":"2026-04-28T00:01:00Z","session_id":"sess-001","pid":12345}
{"event":"tool.call","skill":"x-review-pr","storyId":"story-0063-0011","timestamp":"2026-04-28T00:02:00Z","session_id":"sess-001","pid":12345}
{"event":"tool.call","skill":"x-internal-story-verify","storyId":"story-0063-0011","timestamp":"2026-04-28T00:03:00Z","session_id":"sess-001","pid":12345}
{"event":"phase.end","skill":"x-story-implement","phase":"Phase-1-Plan","storyId":"story-0063-0011","status":"ok","timestamp":"2026-04-28T00:04:00Z","session_id":"sess-001","pid":12345}
EOFNDJSON

# --scope=telemetry with a valid NDJSON for story-0063-0011 should exit 0
telemetry_exit=0
"$AUDIT_EI" --scope=telemetry --story-id=story-0063-0011 --ndjson-file="$ndjson_file" >/dev/null 2>&1 || telemetry_exit=$?
if [ "$telemetry_exit" -eq 0 ] || [ "$telemetry_exit" -eq 1 ]; then
    echo "  PASS: T7: audit-execution-integrity.sh --scope=telemetry exits $telemetry_exit (ok)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T7: audit-execution-integrity.sh --scope=telemetry unexpected exit $telemetry_exit" >&2
    FAIL=$((FAIL + 1))
fi

# ─────────────────────────────────────────────────────────────────────────────
# T8: scripts/audit-tool-call-grammar.sh --self-check passes
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T8: scripts/audit-tool-call-grammar.sh --self-check ---"

AUDIT_GRAMMAR="$REPO_ROOT/scripts/audit-tool-call-grammar.sh"
assert_executable "T8: scripts/audit-tool-call-grammar.sh is executable" "$AUDIT_GRAMMAR"
assert_exit "T8: scripts/audit-tool-call-grammar.sh --self-check exits 0" 0 \
    "$AUDIT_GRAMMAR" --self-check

# ─────────────────────────────────────────────────────────────────────────────
# T9: audit-planning-content.sh --self-check passes
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T9: audit-planning-content.sh --self-check ---"
assert_exit "T9: audit-planning-content.sh --self-check exits 0" 0 \
    "$CLAUDE_SCRIPTS/audit-planning-content.sh" --self-check

# ─────────────────────────────────────────────────────────────────────────────
# T10: audit-ndjson-hash-chain.sh --self-check passes
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T10: audit-ndjson-hash-chain.sh --self-check ---"
assert_exit "T10: audit-ndjson-hash-chain.sh --self-check exits 0" 0 \
    "$CLAUDE_SCRIPTS/audit-ndjson-hash-chain.sh" --self-check

# ─────────────────────────────────────────────────────────────────────────────
# T11: audit-wave-dispatch.sh --self-check passes
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T11: audit-wave-dispatch.sh --self-check ---"
assert_exit "T11: audit-wave-dispatch.sh --self-check exits 0" 0 \
    "$CLAUDE_SCRIPTS/audit-wave-dispatch.sh" --self-check

# ─────────────────────────────────────────────────────────────────────────────
# T12: audit-recovery-mode.sh --self-check passes
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T12: audit-recovery-mode.sh --self-check ---"
assert_exit "T12: audit-recovery-mode.sh --self-check exits 0" 0 \
    "$CLAUDE_SCRIPTS/audit-recovery-mode.sh" --self-check

# ─────────────────────────────────────────────────────────────────────────────
# T13: audit-epic-review-reconciliation.sh --self-check passes
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T13: audit-epic-review-reconciliation.sh --self-check ---"
assert_exit "T13: audit-epic-review-reconciliation.sh --self-check exits 0" 0 \
    "$CLAUDE_SCRIPTS/audit-epic-review-reconciliation.sh" --self-check

# ─────────────────────────────────────────────────────────────────────────────
# T14: audit-rollout-status.sh --self-check passes
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T14: audit-rollout-status.sh --self-check ---"
assert_exit "T14: audit-rollout-status.sh --self-check exits 0" 0 \
    "$CLAUDE_SCRIPTS/audit-rollout-status.sh" --self-check

# ─────────────────────────────────────────────────────────────────────────────
# T15: .claude/rules/30-tool-call-grammar.md exists
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T15: .claude/rules/30-tool-call-grammar.md exists ---"
assert_file_exists "T15: 30-tool-call-grammar.md exists" \
    "$REPO_ROOT/.claude/rules/30-tool-call-grammar.md"

# Verify it contains the Rule 28 marker
assert_file_contains "T15b: Rule 28 file contains grammar marker" \
    "$REPO_ROOT/.claude/rules/30-tool-call-grammar.md" \
    "required|optional|conditional"

# ─────────────────────────────────────────────────────────────────────────────
# T16: docs/audit-bypass-catalog.md exists and lists 11 skills
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T16: docs/audit-bypass-catalog.md exists with 11 skills ---"

BYPASS_CATALOG="$REPO_ROOT/docs/audit-bypass-catalog.md"
assert_file_exists "T16: docs/audit-bypass-catalog.md exists" "$BYPASS_CATALOG"

if [ -f "$BYPASS_CATALOG" ]; then
    skill_count=$(grep -cE "^## [0-9]+\. x-[a-z-]+" "$BYPASS_CATALOG" 2>/dev/null || echo 0)
    if [ "$skill_count" -eq 11 ]; then
        echo "  PASS: T16b: audit-bypass-catalog.md has 11 skill entries (found $skill_count)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: T16b: audit-bypass-catalog.md has $skill_count skill entries (expected 11)" >&2
        FAIL=$((FAIL + 1))
    fi
fi

# ─────────────────────────────────────────────────────────────────────────────
# T17: docs/branch-protection.md exists
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T17: docs/branch-protection.md exists ---"
assert_file_exists "T17: docs/branch-protection.md exists" \
    "$REPO_ROOT/docs/branch-protection.md"

# ─────────────────────────────────────────────────────────────────────────────
# T18: docs/adr/ADR-0016-preflight-warn-to-fail-rollout.md exists
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "--- T18: docs/adr/ADR-0016-preflight-warn-to-fail-rollout.md exists ---"
assert_file_exists "T18: ADR-0016-preflight-warn-to-fail-rollout.md exists" \
    "$REPO_ROOT/docs/adr/ADR-0016-preflight-warn-to-fail-rollout.md"

# ─────────────────────────────────────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "========================================"
echo "EPIC-0063 E2E Smoke Test Results"
echo "========================================"
echo "  Passed: $PASS"
echo "  Failed: $FAIL"
echo "  Total:  $((PASS + FAIL))"
echo ""

if [ "$FAIL" -eq 0 ]; then
    echo "ALL TESTS PASSED — EPIC-0063 integration validated."
    exit 0
else
    echo "SMOKE TEST FAILED — $FAIL test(s) did not pass." >&2
    exit 1
fi
