#!/usr/bin/env bash
# audit_planning_content_test.sh — Unit tests for audit-planning-content.sh
# Tests map 1-to-1 to Gherkin scenarios in story-0063-0015 §5.2
# TDD: written RED first, then GREEN after implementation
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-planning-content.sh"
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

# ── Helpers ───────────────────────────────────────────────────────────────────
make_planning_dir() {
    local dir="$1"
    local story_id="$2"
    mkdir -p "$dir"

    # arch-story-*.md: >= 30 non-empty lines, >= 2 H2/H3 sections, >= 1 Mermaid diagram
    local arch="$dir/arch-$story_id.md"
    {
        echo "# Architecture Plan"
        echo "## Overview"
        echo "This section describes the architecture."
        echo "## Components"
        echo "Component details go here."
        echo '```mermaid'
        echo 'graph TD'
        echo '  A[Component A] --> B[Component B]'
        echo '  B --> C[Component C]'
        echo '```'
        for _ in $(seq 1 25); do echo "architecture detail line here"; done
    } > "$arch"

    # plan-story-*.md: >= 20 lines, >= 2 sections, >= 1 TASK- reference
    local plan="$dir/plan-$story_id.md"
    {
        echo "# Implementation Plan"
        echo "## Tasks"
        echo "- TASK-0063-0015-001: Main implementation task"
        echo "## Notes"
        echo "Implementation notes go here."
        for _ in $(seq 1 16); do echo "plan detail line here"; done
    } > "$plan"

    # tests-story-*.md: >= 15 lines, >= 2 sections, >= 1 Cenario/Scenario
    local tests="$dir/tests-$story_id.md"
    {
        echo "# Test Plan"
        echo "## Acceptance Tests"
        echo "Scenario: valid planning dir produces exit 0"
        echo "  Given a directory with all 6 planning artifacts"
        echo "  When audit-planning-content.sh is run"
        echo "  Then exit code should be 0"
        echo "## Unit Tests"
        echo "Additional test scenarios here."
        for _ in $(seq 1 9); do echo "test detail line here"; done
    } > "$tests"

    # tasks-story-*.md: >= 10 lines, >= 1 TASK- reference
    local tasks="$dir/tasks-$story_id.md"
    {
        echo "# Task Breakdown"
        echo "## Tasks"
        echo "- TASK-0063-0015-001: Create audit-planning-content.sh"
        echo "- TASK-0063-0015-002: Create shell tests"
        for _ in $(seq 1 7); do echo "task detail line here"; done
    } > "$tasks"

    # security-story-*.md: >= 10 lines
    local security="$dir/security-$story_id.md"
    {
        echo "# Security Assessment"
        echo "## Risk Analysis"
        echo "No security risks identified for this story."
        for _ in $(seq 1 8); do echo "security detail line here"; done
    } > "$security"

    # compliance-story-*.md: >= 10 lines
    local compliance="$dir/compliance-$story_id.md"
    {
        echo "# Compliance Assessment"
        echo "## Regulatory Impact"
        echo "No compliance issues identified."
        for _ in $(seq 1 8); do echo "compliance detail line here"; done
    } > "$compliance"
}

echo "=== audit-planning-content.sh tests ==="

# T1: --self-check → exit 0
assert_exit "T1 --self-check ok" 0 \
    "$AUDIT_SCRIPT" --self-check

# T2: valid planning dir with all 6 artifacts → exit 0
VALID_DIR="$TMP_DIR/valid-plans"
make_planning_dir "$VALID_DIR" "story-0063-0015"
assert_exit "T2 valid planning dir exit 0" 0 \
    "$AUDIT_SCRIPT" --artifact-dir "$VALID_DIR" --story-id "story-0063-0015"

# T3: stub arch (2 lines) → exit 1 (PLANNING_CONTENT_INSUFFICIENT)
STUB_ARCH_DIR="$TMP_DIR/stub-arch"
make_planning_dir "$STUB_ARCH_DIR" "story-0063-0015"
printf '# Arch\nStub content only.\n' > "$STUB_ARCH_DIR/arch-story-0063-0015.md"
assert_exit "T3 stub arch rejected" 1 \
    "$AUDIT_SCRIPT" --artifact-dir "$STUB_ARCH_DIR" --story-id "story-0063-0015"

# T4: missing artifact directory → exit 2 (OPERATIONAL_ERROR)
assert_exit "T4 missing dir exit 2" 2 \
    "$AUDIT_SCRIPT" --artifact-dir "/nonexistent/planning-dir" --story-id "story-0063-0015"

# T5: stub plan (no TASK- references) → exit 1
STUB_PLAN_DIR="$TMP_DIR/stub-plan"
make_planning_dir "$STUB_PLAN_DIR" "story-0063-0015"
{
    echo "# Implementation Plan"
    echo "## Tasks"
    echo "## Notes"
    for _ in $(seq 1 18); do echo "plan detail without any task reference"; done
} > "$STUB_PLAN_DIR/plan-story-0063-0015.md"
assert_exit "T5 stub plan (no tasks) rejected" 1 \
    "$AUDIT_SCRIPT" --artifact-dir "$STUB_PLAN_DIR" --story-id "story-0063-0015"

# T6: stub tests (no Scenario/Cenario) → exit 1
STUB_TESTS_DIR="$TMP_DIR/stub-tests"
make_planning_dir "$STUB_TESTS_DIR" "story-0063-0015"
{
    echo "# Test Plan"
    echo "## Tests"
    echo "## Notes"
    for _ in $(seq 1 14); do echo "test detail without scenario keyword"; done
} > "$STUB_TESTS_DIR/tests-story-0063-0015.md"
assert_exit "T6 stub tests (no scenario) rejected" 1 \
    "$AUDIT_SCRIPT" --artifact-dir "$STUB_TESTS_DIR" --story-id "story-0063-0015"

# T7: arch without mermaid diagram → exit 1
NO_MERMAID_DIR="$TMP_DIR/no-mermaid"
make_planning_dir "$NO_MERMAID_DIR" "story-0063-0015"
{
    echo "# Architecture Plan"
    echo "## Overview"
    echo "Architecture description without diagram."
    echo "## Components"
    echo "Components described in text only, no diagram."
    for _ in $(seq 1 27); do echo "architecture detail line here"; done
} > "$NO_MERMAID_DIR/arch-story-0063-0015.md"
assert_exit "T7 arch without mermaid rejected" 1 \
    "$AUDIT_SCRIPT" --artifact-dir "$NO_MERMAID_DIR" --story-id "story-0063-0015"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
