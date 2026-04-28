#!/usr/bin/env bash
# audit_tool_call_grammar_test.sh — Shell tests for audit-tool-call-grammar.sh
# Story: story-0063-0012 (SKILL.md Tool-Call Grammar / Rule 28)
# TDD: RED phase — tests authored before implementation

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
SCRIPT="$REPO_ROOT/scripts/audit-tool-call-grammar.sh"
TMP_DIR="$(mktemp -d)"
PASS=0
FAIL=0

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

echo "=== audit-tool-call-grammar.sh tests ==="

# T1: --self-check exits 0 when script is present and prerequisites are met
assert_exit "T1 --self-check exits 0" 0 \
    "$SCRIPT" --self-check

# T2: skill file with all Skill() calls annotated with markers → exit 0
SKILL_FILE="$TMP_DIR/skill_with_markers.md"
cat > "$SKILL_FILE" <<'EOF'
---
name: x-story-implement
description: Story orchestrator
---

## Phase 1

Invoke the `x-arch-plan` skill via the Skill tool:

    Skill(skill: "x-arch-plan", model: "opus", args: "--story-id STORY-0001")  [required]

Invoke the `x-threat-model` skill:

    Skill(skill: "x-threat-model", model: "sonnet", args: "--story-id STORY-0001")  [conditional: scope ∈ {auth, network, persistence}]

Spawn a subagent:

    Agent(subagent_type: "general-purpose", description: "Plan implementation", prompt: "...")  [optional]
EOF
BASELINE_FILE="$TMP_DIR/baseline.txt"
echo "# empty baseline" > "$BASELINE_FILE"
assert_exit "T2 all markers present → exit 0" 0 \
    "$SCRIPT" --skill-file="$SKILL_FILE" --baseline="$BASELINE_FILE"

# T3: skill file with a Skill() call missing a marker → exit 1
SKILL_MISSING="$TMP_DIR/skill_missing_marker.md"
cat > "$SKILL_MISSING" <<'EOF'
---
name: x-epic-implement
description: Epic orchestrator
---

## Phase 1

Invoke the `x-arch-plan` skill:

    Skill(skill: "x-arch-plan", model: "opus", args: "--epic-id EPIC-0001")

Invoke the `x-release` skill:

    Skill(skill: "x-release", model: "sonnet", args: "--epic-id EPIC-0001")  [required]
EOF
assert_exit "T3 missing marker on Skill() → exit 1" 1 \
    "$SCRIPT" --skill-file="$SKILL_MISSING" --baseline="$BASELINE_FILE"

# T4: missing skill file → exit 2 (OPERATIONAL_ERROR)
assert_exit "T4 missing skill file → exit 2" 2 \
    "$SCRIPT" --skill-file="/nonexistent/path/SKILL.md" --baseline="$BASELINE_FILE"

# T5: --self-check validates prerequisites (jq and grep on PATH)
# This re-validates T1 with explicit prerequisites check
assert_exit "T5 --self-check prerequisites: jq and grep on PATH" 0 \
    bash -c "command -v jq >/dev/null && command -v grep >/dev/null && '$SCRIPT' --self-check"

# T6: Agent() call without marker also triggers exit 1
SKILL_AGENT="$TMP_DIR/skill_agent_missing.md"
cat > "$SKILL_AGENT" <<'EOF'
---
name: x-epic-implement
description: Epic orchestrator
---

## Phase 2

    Skill(skill: "x-arch-plan", model: "opus", args: "...")  [required]

    Agent(subagent_type: "general-purpose", description: "Implementation plan", prompt: "...")
EOF
assert_exit "T6 Agent() missing marker → exit 1" 1 \
    "$SCRIPT" --skill-file="$SKILL_AGENT" --baseline="$BASELINE_FILE"

# T7: skill file grandfathered in baseline → exit 0 even without markers
SKILL_LEGACY="$TMP_DIR/skill_legacy.md"
cat > "$SKILL_LEGACY" <<'EOF'
---
name: x-legacy-skill
description: Legacy skill (pre-EPIC-0063)
---

## Phase 1

    Skill(skill: "x-arch-plan", model: "opus", args: "...")

    Agent(subagent_type: "general-purpose", description: "old plan", prompt: "...")
EOF
BASELINE_WITH_LEGACY="$TMP_DIR/baseline_with_legacy.txt"
cat > "$BASELINE_WITH_LEGACY" <<'EOF'
# tool-call-grammar-baseline.txt — grandfather list for pre-EPIC-0063 skills
x-legacy-skill # pre-EPIC-0063, no markers applied yet
EOF
assert_exit "T7 skill in baseline → exit 0 (grandfathered)" 0 \
    "$SCRIPT" --skill-file="$SKILL_LEGACY" --baseline="$BASELINE_WITH_LEGACY"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
