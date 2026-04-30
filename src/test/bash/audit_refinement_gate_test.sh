#!/usr/bin/env bash
# audit_refinement_gate_test.sh — Smoke tests for EPIC-0069 story-0069-0006.
# Tests audit-refinement-gate.sh CI script (Camada 2, Rule 29).
#
# Usage:
#   src/test/bash/audit_refinement_gate_test.sh
#
# Exit codes:
#   0 — all tests passed
#   1 — one or more tests failed

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
SCRIPT_SRC="${REPO_ROOT}/src/main/resources/targets/claude/scripts/audit-refinement-gate.sh"

PASS=0
FAIL=0

pass() { echo "  ✅ PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  ❌ FAIL: $1 — $2" >&2; FAIL=$((FAIL + 1)); }

# ─── fixture builder ──────────────────────────────────────────────────────

setup_repo() {
  # $1=tmpdir  $2=epic-num  $3=verdict-status (or "absent")
  local tmpdir="$1" epic_num="$2" verdict_status="${3:-absent}"
  cd "$tmpdir"
  git init -q . 2>&1 >/dev/null
  git -c user.name=test -c user.email=t@t.t commit --allow-empty -q -m "init" 2>&1 >/dev/null
  mkdir -p ".claude/rules" ".claude/hooks" "governance/baselines" "capabilities/governance" \
           "ai/epics/epic-${epic_num}-test"
  : > ".claude/rules/29-refinement-gate.md"
  : > ".claude/hooks/enforce-refinement-gate.sh"
  : > "capabilities/governance/refinement-gate.yaml"
  cat > "governance/baselines/refinement-gate-baseline.txt" <<'EOF'
# refinement-gate-baseline.txt
EOF
  if [[ "$verdict_status" == "absent" ]]; then
    cat > "ai/epics/epic-${epic_num}-test/execution-state.json" <<EOF
{
  "flowVersion": "4",
  "epicId": "EPIC-${epic_num}",
  "taskTracking": {"enabled": true}
}
EOF
  else
    cat > "ai/epics/epic-${epic_num}-test/execution-state.json" <<EOF
{
  "flowVersion": "4",
  "epicId": "EPIC-${epic_num}",
  "refinementVerdict": {"status": "$verdict_status", "scope": "story", "blockers": []},
  "taskTracking": {"enabled": true}
}
EOF
  fi
}

# ─── scenarios ─────────────────────────────────────────────────────────────

echo "=============================================="
echo "audit_refinement_gate_test.sh — EPIC-0069"
echo "=============================================="
echo ""

# Scenario 1: --self-check passes
echo "--- Scenario 1: --self-check passes ---"
TMP=$(mktemp -d); (setup_repo "$TMP" "0099" "absent")
RC=0; OUT=$(cd "$TMP" && bash "$SCRIPT_SRC" --self-check 2>&1) || RC=$?
if [[ "$RC" -eq 0 ]]; then pass "self-check exit 0"; else fail "self-check exit 0" "got $RC: $OUT"; fi
rm -rf "$TMP"

# Scenario 2: PR with verdict approved passes
echo "--- Scenario 2: --story with approved verdict passes ---"
TMP=$(mktemp -d); (setup_repo "$TMP" "0099" "approved")
RC=0; OUT=$(cd "$TMP" && bash "$SCRIPT_SRC" --story story-0099-0001 2>&1) || RC=$?
if [[ "$RC" -eq 0 ]]; then pass "exit 0 on approved"; else fail "exit 0 on approved" "got $RC: $OUT"; fi
rm -rf "$TMP"

# Scenario 3: PR with verdict rejected fails
echo "--- Scenario 3: --story with rejected verdict fails ---"
TMP=$(mktemp -d); (setup_repo "$TMP" "0099" "rejected")
RC=0; STDERR=$(cd "$TMP" && bash "$SCRIPT_SRC" --story story-0099-0001 2>&1 >/dev/null) || RC=$?
if [[ "$RC" -eq 1 ]]; then pass "exit 1 on rejected"; else fail "exit 1 on rejected" "got $RC: $STDERR"; fi
if echo "$STDERR" | grep -q "REFINEMENT_GATE_VIOLATION"; then
  pass "stderr contains REFINEMENT_GATE_VIOLATION"
else
  fail "stderr violation message" "stderr: $STDERR"
fi
if echo "$STDERR" | grep -q "rejected-verdict"; then
  pass "stderr contains rejected-verdict sub-code"
else
  fail "rejected-verdict sub-code" "stderr: $STDERR"
fi
rm -rf "$TMP"

# Scenario 4: PR with verdict absent (tbd) fails
echo "--- Scenario 4: --story with absent verdict fails ---"
TMP=$(mktemp -d); (setup_repo "$TMP" "0099" "absent")
RC=0; STDERR=$(cd "$TMP" && bash "$SCRIPT_SRC" --story story-0099-0001 2>&1 >/dev/null) || RC=$?
if [[ "$RC" -eq 1 ]]; then pass "exit 1 on absent verdict"; else fail "exit 1 on absent" "got $RC: $STDERR"; fi
if echo "$STDERR" | grep -q "missing-verdict"; then
  pass "stderr contains missing-verdict sub-code"
else
  fail "missing-verdict sub-code" "stderr: $STDERR"
fi
rm -rf "$TMP"

# Scenario 5: hotfix branch is exempt
echo "--- Scenario 5: hotfix/* branch exempt ---"
TMP=$(mktemp -d); (setup_repo "$TMP" "0099" "rejected")
(cd "$TMP" && git checkout -q -b hotfix/CRIT-001 2>&1 >/dev/null)
RC=0; OUT=$(cd "$TMP" && bash "$SCRIPT_SRC" 2>&1) || RC=$?
if [[ "$RC" -eq 0 ]]; then pass "exit 0 on hotfix branch"; else fail "exit 0 on hotfix" "got $RC: $OUT"; fi
rm -rf "$TMP"

# Scenario 6: baseline grandfathers a story
echo "--- Scenario 6: baseline grandfathers a story ---"
TMP=$(mktemp -d); (setup_repo "$TMP" "0099" "rejected")
echo "story-0099-0001  # pre-EPIC-0069" >> "$TMP/governance/baselines/refinement-gate-baseline.txt"
RC=0; OUT=$(cd "$TMP" && bash "$SCRIPT_SRC" --story story-0099-0001 2>&1) || RC=$?
if [[ "$RC" -eq 0 ]]; then pass "exit 0 on grandfathered story"; else fail "grandfathered" "got $RC: $OUT"; fi
if echo "$OUT" | grep -q "grandfathered"; then
  pass "stdout shows grandfathered marker"
else
  fail "grandfathered marker" "stdout: $OUT"
fi
rm -rf "$TMP"

# Scenario 7: legacy flowVersion=1 is no-op
echo "--- Scenario 7: flowVersion=1 is no-op ---"
TMP=$(mktemp -d); (setup_repo "$TMP" "0099" "absent")
# Override state file to flowVersion=1
cat > "$TMP/ai/epics/epic-0099-test/execution-state.json" <<'EOF'
{"flowVersion": "1", "epicId": "EPIC-0099", "taskTracking": {"enabled": false}}
EOF
RC=0; OUT=$(cd "$TMP" && bash "$SCRIPT_SRC" --story story-0099-0001 2>&1) || RC=$?
if [[ "$RC" -eq 0 ]]; then pass "exit 0 on flowVersion=1"; else fail "flowVersion=1" "got $RC: $OUT"; fi
rm -rf "$TMP"

# ─── summary ───────────────────────────────────────────────────────────────

echo ""
echo "=============================================="
echo "Tests passed: $PASS"
echo "Tests failed: $FAIL"
echo "=============================================="

if [[ "$FAIL" -gt 0 ]]; then exit 1; fi
exit 0
