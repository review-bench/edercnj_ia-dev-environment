#!/usr/bin/env bash
# enforce_refinement_gate_test.sh — Smoke tests for EPIC-0069 story-0069-0005.
# Tests enforce-refinement-gate.sh PreToolUse hook decision matrix (Gherkin scenarios).
#
# Usage:
#   src/test/bash/enforce_refinement_gate_test.sh
#
# Exit codes:
#   0 — all tests passed
#   1 — one or more tests failed

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
HOOK_SRC="${REPO_ROOT}/src/main/resources/targets/claude/hooks/enforce-refinement-gate.sh"

PASS=0
FAIL=0

pass() { echo "  ✅ PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  ❌ FAIL: $1 — $2" >&2; FAIL=$((FAIL + 1)); }

# ─── fixture builders ──────────────────────────────────────────────────────

setup_epic_dir() {
  # $1=tmpdir  $2=epic-num  $3=flowVersion  $4=verdict-status (or "absent" to omit)
  local tmpdir="$1" epic_num="$2" flow_version="$3" verdict_status="${4:-absent}"
  local epic_dir="$tmpdir/ai/epics/epic-${epic_num}-test"
  mkdir -p "$epic_dir"
  if [[ "$verdict_status" == "absent" ]]; then
    cat > "$epic_dir/execution-state.json" <<EOF
{
  "flowVersion": "$flow_version",
  "epicId": "EPIC-${epic_num}",
  "taskTracking": {"enabled": true}
}
EOF
  else
    cat > "$epic_dir/execution-state.json" <<EOF
{
  "flowVersion": "$flow_version",
  "epicId": "EPIC-${epic_num}",
  "refinementVerdict": {"status": "$verdict_status", "scope": "story", "blockers": []},
  "taskTracking": {"enabled": true}
}
EOF
  fi
  echo "$epic_dir"
}

write_story_md() {
  # $1=epic_dir  $2=story-id  $3=rnf_row(optional)
  local epic_dir="$1" story_id="$2" rnf_row="${3:-}"
  cat > "${epic_dir}/${story_id}.md" <<EOF
# Story fixture

## 9. Refinement Verdict

> approved fixture
EOF
  if [[ -n "${rnf_row}" ]]; then
    cat >> "${epic_dir}/${story_id}.md" <<EOF

## 2. RNFs Herdadas

| Categoria | RNF Original (Produto) | no-relax? | Override Value | Justificação | Approval Status | Approver |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
${rnf_row}

## 3. Next Section
EOF
  fi
}

build_payload() {
  # $1=skill  $2=args
  local skill="$1" args="$2"
  printf '{"tool_name":"Skill","tool_input":{"skill":"%s","args":"%s"}}' "$skill" "$args"
}

run_hook() {
  # NOTE: invoke hook on RIGHT side of pipe so CLAUDE_PROJECT_DIR reaches it.
  # $1=tmpdir  $2=payload  out: $rc, $stderr_out
  local tmpdir="$1" payload="$2" rc=0 stderr_out
  stderr_out=$(printf '%s' "$payload" | CLAUDE_PROJECT_DIR="$tmpdir" bash "$HOOK_SRC" 2>&1 >/dev/null) || rc=$?
  echo "$rc"
  echo "$stderr_out"
}

# ─── scenarios ─────────────────────────────────────────────────────────────

echo "=============================================="
echo "enforce_refinement_gate_test.sh — EPIC-0069"
echo "=============================================="
echo ""

# Scenario 1: --self-check passes when prerequisites are present
echo "--- Scenario 1: --self-check passes ---"
TMP=$(mktemp -d)
mkdir -p "$TMP/governance/baselines"
touch "$TMP/governance/baselines/refinement-gate-baseline.txt"
RC=0; OUT=$(CLAUDE_PROJECT_DIR="$TMP" bash "$HOOK_SRC" --self-check 2>&1) || RC=$?
if [[ "$RC" -eq 0 ]]; then pass "self-check exit 0"; else fail "self-check exit 0" "got $RC: $OUT"; fi
rm -rf "$TMP"

# Scenario 2: hook blocks x-implement-story when refinementVerdict absent (flowVersion=4)
echo "--- Scenario 2: blocks when verdict absent (flowVersion=4) ---"
TMP=$(mktemp -d); EPIC_DIR=$(setup_epic_dir "$TMP" "0099" "4" "absent")
PAYLOAD=$(build_payload "x-implement-story" "story-0099-0001")
RC=0; STDERR=$(printf '%s' "$PAYLOAD" | CLAUDE_PROJECT_DIR="$TMP" bash "$HOOK_SRC" 2>&1 >/dev/null) || RC=$?
if [[ "$RC" -eq 33 ]]; then pass "exit 33 on absent verdict"; else fail "exit 33 on absent verdict" "got $RC"; fi
if echo "$STDERR" | grep -q "REFINEMENT_REQUIRED"; then
  pass "stderr contains REFINEMENT_REQUIRED"
else
  fail "stderr contains REFINEMENT_REQUIRED" "stderr: $STDERR"
fi
rm -rf "$TMP"

# Scenario 3: hook allows x-implement-story when refinementVerdict.status=approved
echo "--- Scenario 3: allows when verdict approved ---"
TMP=$(mktemp -d); EPIC_DIR=$(setup_epic_dir "$TMP" "0099" "4" "approved")
PAYLOAD=$(build_payload "x-implement-story" "story-0099-0001 --target-branch epic/0099")
RC=0; printf '%s' "$PAYLOAD" | CLAUDE_PROJECT_DIR="$TMP" bash "$HOOK_SRC" >/dev/null 2>&1 || RC=$?
if [[ "$RC" -eq 0 ]]; then pass "exit 0 on approved verdict"; else fail "exit 0 on approved verdict" "got $RC"; fi
rm -rf "$TMP"

# Scenario 4: hook blocks when verdict status=rejected
echo "--- Scenario 4: blocks when verdict rejected ---"
TMP=$(mktemp -d); EPIC_DIR=$(setup_epic_dir "$TMP" "0099" "4" "rejected")
PAYLOAD=$(build_payload "x-implement-story" "story-0099-0001")
RC=0; STDERR=$(printf '%s' "$PAYLOAD" | CLAUDE_PROJECT_DIR="$TMP" bash "$HOOK_SRC" 2>&1 >/dev/null) || RC=$?
if [[ "$RC" -eq 33 ]]; then pass "exit 33 on rejected verdict"; else fail "exit 33 on rejected verdict" "got $RC"; fi
rm -rf "$TMP"

# Scenario 5: legacy flowVersion=1 fallback (Rule 19)
echo "--- Scenario 5: flowVersion=1 fallback (no-op) ---"
TMP=$(mktemp -d); EPIC_DIR=$(setup_epic_dir "$TMP" "0099" "1" "absent")
PAYLOAD=$(build_payload "x-implement-story" "story-0099-0001")
RC=0; STDERR=$(printf '%s' "$PAYLOAD" | CLAUDE_PROJECT_DIR="$TMP" bash "$HOOK_SRC" 2>&1 >/dev/null) || RC=$?
if [[ "$RC" -eq 0 ]]; then pass "exit 0 on flowVersion=1 (legacy)"; else fail "exit 0 on flowVersion=1" "got $RC"; fi
if echo "$STDERR" | grep -q "Rule 19 legacy fallback"; then
  pass "stderr contains Rule 19 fallback warning"
else
  fail "stderr contains Rule 19 warning" "stderr: $STDERR"
fi
rm -rf "$TMP"

# Scenario 6: CLAUDE_RECOVERY_MODE=1 bypass (Rule 27)
echo "--- Scenario 6: CLAUDE_RECOVERY_MODE=1 bypass ---"
TMP=$(mktemp -d); EPIC_DIR=$(setup_epic_dir "$TMP" "0099" "4" "tbd")
PAYLOAD=$(build_payload "x-implement-story" "story-0099-0001")
RC=0; STDERR=$(printf '%s' "$PAYLOAD" | CLAUDE_PROJECT_DIR="$TMP" CLAUDE_RECOVERY_MODE=1 bash "$HOOK_SRC" 2>&1 >/dev/null) || RC=$?
if [[ "$RC" -eq 0 ]]; then pass "exit 0 on recovery mode"; else fail "exit 0 on recovery mode" "got $RC"; fi
if echo "$STDERR" | grep -q "CLAUDE_RECOVERY_MODE=1"; then
  pass "stderr contains recovery warning"
else
  fail "stderr contains recovery warning" "stderr: $STDERR"
fi
rm -rf "$TMP"

# Scenario 7: non-guarded skill is no-op
echo "--- Scenario 7: non-guarded skill (x-execute-tests) is no-op ---"
TMP=$(mktemp -d); EPIC_DIR=$(setup_epic_dir "$TMP" "0099" "4" "tbd")
PAYLOAD=$(build_payload "x-execute-tests" "story-0099-0001")
RC=0; printf '%s' "$PAYLOAD" | CLAUDE_PROJECT_DIR="$TMP" bash "$HOOK_SRC" >/dev/null 2>&1 || RC=$?
if [[ "$RC" -eq 0 ]]; then pass "exit 0 on non-guarded skill"; else fail "exit 0 on non-guarded skill" "got $RC"; fi
rm -rf "$TMP"

# Scenario 8: non-Skill tool is no-op
echo "--- Scenario 8: non-Skill tool (Bash) is no-op ---"
TMP=$(mktemp -d); EPIC_DIR=$(setup_epic_dir "$TMP" "0099" "4" "tbd")
PAYLOAD='{"tool_name":"Bash","tool_input":{"command":"ls"}}'
RC=0; printf '%s' "$PAYLOAD" | CLAUDE_PROJECT_DIR="$TMP" bash "$HOOK_SRC" >/dev/null 2>&1 || RC=$?
if [[ "$RC" -eq 0 ]]; then pass "exit 0 on non-Skill tool"; else fail "exit 0 on non-Skill tool" "got $RC"; fi
rm -rf "$TMP"

# Scenario 9: approved verdict still blocks invalid RNF inheritance
echo "--- Scenario 9: approved verdict blocks invalid RNF inheritance ---"
TMP=$(mktemp -d); EPIC_DIR=$(setup_epic_dir "$TMP" "0099" "4" "approved")
write_story_md "$EPIC_DIR" "story-0099-0001" \
"| SECURITY | encrypt-data | false | tls-optional | rollout exception | APPROVED | security-team |"
PAYLOAD=$(build_payload "x-implement-story" "story-0099-0001 --target-branch epic/0099")
RC=0; STDERR=$(printf '%s' "$PAYLOAD" | CLAUDE_PROJECT_DIR="$TMP" bash "$HOOK_SRC" 2>&1 >/dev/null) || RC=$?
if [[ "$RC" -eq 34 ]]; then pass "exit 34 on RNF inheritance violation"; else fail "exit 34 on RNF inheritance violation" "got $RC"; fi
if echo "$STDERR" | grep -q "RNF_INHERITANCE_VIOLATION"; then
  pass "stderr contains RNF_INHERITANCE_VIOLATION"
else
  fail "stderr contains RNF_INHERITANCE_VIOLATION" "stderr: $STDERR"
fi
rm -rf "$TMP"

# ─── summary ───────────────────────────────────────────────────────────────

echo ""
echo "=============================================="
echo "Tests passed: $PASS"
echo "Tests failed: $FAIL"
echo "=============================================="

if [[ "$FAIL" -gt 0 ]]; then exit 1; fi
exit 0
