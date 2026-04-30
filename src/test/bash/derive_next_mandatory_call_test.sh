#!/usr/bin/env bash
# derive_next_mandatory_call_test.sh — Unit tests for story-0068-0003 function.
# Sources enforce-continuous-flow.sh (BASH_SOURCE guard skips main logic)
# and tests derive_next_mandatory_call directly.
#
# Usage:
#   src/test/bash/derive_next_mandatory_call_test.sh
#
# Exit codes:
#   0 — all tests passed
#   1 — one or more tests failed

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
HOOK_SRC="${REPO_ROOT}/src/main/resources/targets/claude/hooks/enforce-continuous-flow.sh"

# Source the hook to load only the functions (main block is guarded by BASH_SOURCE check)
# shellcheck source=/dev/null
source "$HOOK_SRC"

PASS=0
FAIL=0

pass() { echo "  ✅ PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  ❌ FAIL: $1 — $2" >&2; FAIL=$((FAIL + 1)); }

# ─── fixture builders ──────────────────────────────────────────────────────

make_skill_md() {
  local dir="$1" phase="${2:-Phase 3}"
  mkdir -p "$dir"
  cat > "$dir/SKILL.md" <<EOF
## $phase - Review

    Skill(skill: "x-review", model: "sonnet", args: "...")  [required]
    Skill(skill: "x-review-pr", model: "sonnet", args: "...")  [required]
    Skill(skill: "x-internal-story-report", model: "haiku", args: "...")  [required]
    Skill(skill: "x-spec-drift", model: "sonnet", args: "...")  [optional]

## Phase 4 - Finalize

    Skill(skill: "x-git-commit", model: "haiku", args: "...")  [required]
EOF
}

make_ndjson() {
  local file="$1"
  shift
  : > "$file"
  for skill in "$@"; do
    echo "{\"type\":\"tool.call\",\"skill\":\"$skill\"}" >> "$file"
  done
}

# ─── tests ─────────────────────────────────────────────────────────────────

echo "=============================================="
echo "derive_next_mandatory_call_test.sh — EPIC-0068"
echo "=============================================="
echo ""

# Test 1: returns first missing required skill
echo "--- Test 1: returns first missing required (x-review-pr) ---"
TMP=$(mktemp -d); trap "rm -rf $TMP" EXIT
make_skill_md "$TMP" "Phase 3"
make_ndjson "$TMP/events.ndjson" "x-review"
RESULT="$(derive_next_mandatory_call "$TMP/SKILL.md" "Phase 3" "$TMP/events.ndjson")"
if [[ "$RESULT" == "x-review-pr" ]]; then pass "returns x-review-pr"; else fail "returns x-review-pr" "got: $RESULT"; fi
rm -rf "$TMP"; trap - EXIT

# Test 2: returns PHASE_COMPLETE when all required emitted
echo "--- Test 2: PHASE_COMPLETE when all required emitted ---"
TMP=$(mktemp -d); trap "rm -rf $TMP" EXIT
make_skill_md "$TMP" "Phase 3"
make_ndjson "$TMP/events.ndjson" "x-review" "x-review-pr" "x-internal-story-report"
RESULT="$(derive_next_mandatory_call "$TMP/SKILL.md" "Phase 3" "$TMP/events.ndjson")"
if [[ "$RESULT" == "PHASE_COMPLETE" ]]; then pass "returns PHASE_COMPLETE"; else fail "returns PHASE_COMPLETE" "got: $RESULT"; fi
rm -rf "$TMP"; trap - EXIT

# Test 3: exit 1 when no [required] markers in SKILL.md
echo "--- Test 3: exit 1 when no markers (use fallback) ---"
TMP=$(mktemp -d); trap "rm -rf $TMP" EXIT
mkdir -p "$TMP"
cat > "$TMP/SKILL.md" <<'EOF'
## Phase 3 - Review

    Skill(skill: "x-review", model: "sonnet", args: "...")
    Skill(skill: "x-review-pr", model: "sonnet", args: "...")  [optional]
EOF
make_ndjson "$TMP/events.ndjson" "x-review"
derive_next_mandatory_call "$TMP/SKILL.md" "Phase 3" "$TMP/events.ndjson" && RC=$? || RC=$?
if [[ "$RC" -eq 1 ]]; then pass "exit 1 for no markers"; else fail "exit 1 for no markers" "got exit $RC"; fi
rm -rf "$TMP"; trap - EXIT

# Test 4: exit 1 when SKILL.md not found
echo "--- Test 4: exit 1 when SKILL.md missing ---"
TMP=$(mktemp -d); trap "rm -rf $TMP" EXIT
make_ndjson "$TMP/events.ndjson" "x-review"
derive_next_mandatory_call "$TMP/nonexistent.md" "Phase 3" "$TMP/events.ndjson" && RC=$? || RC=$?
if [[ "$RC" -eq 1 ]]; then pass "exit 1 for missing SKILL.md"; else fail "exit 1 for missing SKILL.md" "got exit $RC"; fi
rm -rf "$TMP"; trap - EXIT

# Test 5: first required skill (none emitted yet)
echo "--- Test 5: returns first required when nothing emitted yet ---"
TMP=$(mktemp -d); trap "rm -rf $TMP" EXIT
make_skill_md "$TMP" "Phase 3"
make_ndjson "$TMP/events.ndjson"  # empty NDJSON
RESULT="$(derive_next_mandatory_call "$TMP/SKILL.md" "Phase 3" "$TMP/events.ndjson")"
if [[ "$RESULT" == "x-review" ]]; then pass "returns first required x-review"; else fail "returns first required x-review" "got: $RESULT"; fi
rm -rf "$TMP"; trap - EXIT

# Test 6: phase not in SKILL.md → exit 1
echo "--- Test 6: exit 1 when phase not found in SKILL.md ---"
TMP=$(mktemp -d); trap "rm -rf $TMP" EXIT
make_skill_md "$TMP" "Phase 3"
make_ndjson "$TMP/events.ndjson" "x-review"
derive_next_mandatory_call "$TMP/SKILL.md" "Phase 99" "$TMP/events.ndjson" && RC=$? || RC=$?
if [[ "$RC" -eq 1 ]]; then pass "exit 1 for phase not found"; else fail "exit 1 for phase not found" "got exit $RC"; fi
rm -rf "$TMP"; trap - EXIT

# ─── summary ──────────────────────────────────────────────────────────────
echo ""
echo "Results: $PASS passed, $FAIL failed"
[[ $FAIL -eq 0 ]]
