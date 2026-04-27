#!/usr/bin/env bash
# enforce-no-bypass-flags-smoke.sh — Smoke tests for story-0059-0003:
# PreToolUse hook blocks --skip-* flags outside recovery mode.
#
# Tests the enforce-no-bypass-flags.sh hook directly by feeding crafted
# JSON payloads via stdin and validating exit codes + stderr messages.
#
# Usage:
#   src/test/bash/enforce-no-bypass-flags-smoke.sh
#
# Exit codes:
#   0 — all tests passed
#   1 — one or more tests failed

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
HOOK="${REPO_ROOT}/.claude/hooks/enforce-no-bypass-flags.sh"
PASS=0
FAIL=0
ERRORS=()

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

pass() { local name="$1"; echo "  ✅ PASS: ${name}"; PASS=$((PASS + 1)); }
fail() { local name="$1" msg="$2"; echo "  ❌ FAIL: ${name} — ${msg}" >&2; FAIL=$((FAIL + 1)); ERRORS+=("${name}: ${msg}"); }

run_hook() {
  local payload="$1"
  local recovery="${2:-0}"
  CLAUDE_RECOVERY_MODE="${recovery}" CLAUDE_PROJECT_DIR="${REPO_ROOT}" \
    bash "${HOOK}" <<< "${payload}" 2>&1
  echo "exit:$?"
}

run_hook_exitcode() {
  local payload="$1"
  local recovery="${2:-0}"
  local result
  result=$(CLAUDE_RECOVERY_MODE="${recovery}" CLAUDE_PROJECT_DIR="${REPO_ROOT}" \
    bash "${HOOK}" <<< "${payload}" 2>&1; echo "exit:$?")
  echo "${result}" | grep "exit:" | tail -1 | sed 's/exit://'
}

make_payload() {
  local skill="$1"
  local args="$2"
  printf '{"tool_name":"Skill","tool_input":{"skill":"%s","args":"%s"}}' "${skill}" "${args}"
}

# ---------------------------------------------------------------------------
# Test suite
# ---------------------------------------------------------------------------

echo "============================================"
echo "enforce-no-bypass-flags-smoke.sh — EPIC-0059"
echo "============================================"
echo ""

# Verify hook exists and is executable
if [ ! -x "${HOOK}" ]; then
  echo "FATAL: hook not found or not executable: ${HOOK}" >&2
  exit 2
fi

# AT-01: Non-Skill tool call → allowed (exit 0)
at01() {
  local payload='{"tool_name":"Write","tool_input":{"file_path":"/tmp/test.txt","content":"test"}}'
  local exitcode
  exitcode=$(run_hook_exitcode "${payload}" "0")
  if [ "${exitcode}" = "0" ]; then
    pass "AT-01: non-Skill tool call → exit 0"
  else
    fail "AT-01: non-Skill tool call → exit 0" "got exit ${exitcode}"
  fi
}
at01

# AT-02: Normal Skill invocation (no blocked flags) → allowed (exit 0)
at02() {
  local payload
  payload=$(make_payload "x-story-implement" "--story-id story-0059-0001")
  local exitcode
  exitcode=$(run_hook_exitcode "${payload}" "0")
  if [ "${exitcode}" = "0" ]; then
    pass "AT-02: no blocked flags → exit 0"
  else
    fail "AT-02: no blocked flags → exit 0" "got exit ${exitcode}"
  fi
}
at02

# AT-03: --skip-verification on x-story-implement → BLOCKED (exit 1)
at03() {
  local payload
  payload=$(make_payload "x-story-implement" "--skip-verification --story-id story-0059-0001")
  local result
  result=$(run_hook "${payload}" "0")
  local exitcode
  exitcode=$(echo "${result}" | grep "exit:" | tail -1 | sed 's/exit://')
  if [ "${exitcode}" = "1" ] && echo "${result}" | grep -q "BLOCKED"; then
    pass "AT-03: --skip-verification on x-story-implement → exit 1 BLOCKED"
  else
    fail "AT-03: --skip-verification → BLOCKED" "exitcode=${exitcode}, output=${result}"
  fi
}
at03

# AT-04: --no-ci-watch on x-story-implement → BLOCKED (exit 1)
at04() {
  local payload
  payload=$(make_payload "x-story-implement" "story-0059-0001 --no-ci-watch")
  local exitcode
  exitcode=$(run_hook_exitcode "${payload}" "0")
  if [ "${exitcode}" = "1" ]; then
    pass "AT-04: --no-ci-watch on x-story-implement → exit 1"
  else
    fail "AT-04: --no-ci-watch → BLOCKED" "got exit ${exitcode}"
  fi
}
at04

# AT-05: --skip-verification on x-story-implement with RECOVERY_MODE=1 → allowed (exit 0)
at05() {
  local payload
  payload=$(make_payload "x-story-implement" "--skip-verification --story-id story-0059-0001")
  local result
  result=$(run_hook "${payload}" "1")
  local exitcode
  exitcode=$(echo "${result}" | grep "exit:" | tail -1 | sed 's/exit://')
  if [ "${exitcode}" = "0" ] && echo "${result}" | grep -q "WARNING"; then
    pass "AT-05: --skip-verification with RECOVERY_MODE=1 → exit 0 + WARNING"
  else
    fail "AT-05: recovery mode allows skip-verification" "exitcode=${exitcode}, output=${result}"
  fi
}
at05

# AT-06: CLAUDE_SKIP_AUDIT=1 does NOT bypass (exit 1 still)
at06() {
  local payload
  payload=$(make_payload "x-story-implement" "--skip-verification --story-id story-0059-0001")
  local exitcode
  exitcode=$(CLAUDE_SKIP_AUDIT=1 CLAUDE_RECOVERY_MODE=0 CLAUDE_PROJECT_DIR="${REPO_ROOT}" \
    bash "${HOOK}" <<< "${payload}" 2>/dev/null; echo $?)
  if [ "${exitcode}" = "1" ]; then
    pass "AT-06: CLAUDE_SKIP_AUDIT=1 ignored → still exit 1"
  else
    fail "AT-06: CLAUDE_SKIP_AUDIT bypass ignored" "got exit ${exitcode}"
  fi
}
at06

# AT-07: --skip-review on x-epic-implement → BLOCKED (exit 1)
at07() {
  local payload
  payload=$(make_payload "x-epic-implement" "--skip-review EPIC-0059")
  local exitcode
  exitcode=$(run_hook_exitcode "${payload}" "0")
  if [ "${exitcode}" = "1" ]; then
    pass "AT-07: --skip-review on x-epic-implement → exit 1"
  else
    fail "AT-07: --skip-review on x-epic-implement" "got exit ${exitcode}"
  fi
}
at07

# AT-08: --skip-verification on non-orchestrator (x-git-commit) → allowed (exit 0)
at08() {
  local payload
  payload=$(make_payload "x-git-commit" "--skip-verification")
  local exitcode
  exitcode=$(run_hook_exitcode "${payload}" "0")
  if [ "${exitcode}" = "0" ]; then
    pass "AT-08: --skip-verification on non-orchestrator x-git-commit → exit 0"
  else
    fail "AT-08: non-orchestrator not enforced" "got exit ${exitcode}"
  fi
}
at08

# AT-09: --skip-pr-comments on x-pr-fix-epic → BLOCKED (exit 1)
at09() {
  local payload
  payload=$(make_payload "x-pr-fix-epic" "EPIC-0059 --skip-pr-comments")
  local exitcode
  exitcode=$(run_hook_exitcode "${payload}" "0")
  if [ "${exitcode}" = "1" ]; then
    pass "AT-09: --skip-pr-comments on x-pr-fix-epic → exit 1"
  else
    fail "AT-09: --skip-pr-comments blocked" "got exit ${exitcode}"
  fi
}
at09

# AT-10: --no-ci-watch on x-release → BLOCKED (exit 1)
at10() {
  local payload
  payload=$(make_payload "x-release" "1.2.0 --no-ci-watch")
  local exitcode
  exitcode=$(run_hook_exitcode "${payload}" "0")
  if [ "${exitcode}" = "1" ]; then
    pass "AT-10: --no-ci-watch on x-release → exit 1"
  else
    fail "AT-10: --no-ci-watch on x-release blocked" "got exit ${exitcode}"
  fi
}
at10

# AT-11: Empty args → allowed (exit 0)
at11() {
  local payload
  payload=$(make_payload "x-story-implement" "")
  local exitcode
  exitcode=$(run_hook_exitcode "${payload}" "0")
  if [ "${exitcode}" = "0" ]; then
    pass "AT-11: empty args → exit 0"
  else
    fail "AT-11: empty args" "got exit ${exitcode}"
  fi
}
at11

# AT-12: --self-check passes (settings.json check requires real file)
at12() {
  local result
  result=$(CLAUDE_PROJECT_DIR="${REPO_ROOT}" bash "${HOOK}" --self-check 2>&1; echo "exit:$?")
  local exitcode
  exitcode=$(echo "${result}" | grep "exit:" | tail -1 | sed 's/exit://')
  # self-check may pass (0) or fail with OPERATIONAL_ERROR (2) if not yet registered
  # We just verify the self-check path runs without crashing (exit code is 0 or 2, not 1)
  if [ "${exitcode}" = "0" ] || [ "${exitcode}" = "2" ]; then
    pass "AT-12: --self-check runs without crash (exit ${exitcode})"
  else
    fail "AT-12: --self-check" "unexpected exit ${exitcode}, output: ${result}"
  fi
}
at12

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "============================================"
echo "Results: ${PASS} passed, ${FAIL} failed"
echo "============================================"

if [[ ${FAIL} -gt 0 ]]; then
  echo ""
  echo "Failed tests:"
  for err in "${ERRORS[@]}"; do
    echo "  - ${err}"
  done
  exit 1
fi

exit 0
