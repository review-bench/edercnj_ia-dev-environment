#!/usr/bin/env bash
# settings-hook-registration-smoke.sh — Smoke tests for story-0059-0003,
# TASK-0059-0003-002: verify enforce-no-bypass-flags.sh is correctly
# registered in .claude/settings.json under PreToolUse.
#
# Also validates the --self-check flag of enforce-no-bypass-flags.sh, which
# verifies its own registration in settings.json (RULE-059-07 compliance).
#
# Usage:
#   src/test/bash/settings-hook-registration-smoke.sh
#
# Exit codes:
#   0 — all tests passed
#   1 — one or more tests failed

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
SETTINGS="${REPO_ROOT}/.claude/settings.json"
HOOK="${REPO_ROOT}/.claude/hooks/enforce-no-bypass-flags.sh"
PASS=0
FAIL=0
ERRORS=()

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

pass() { local name="$1"; echo "  ✅ PASS: ${name}"; PASS=$((PASS + 1)); }
fail() { local name="$1" msg="$2"; echo "  ❌ FAIL: ${name} — ${msg}" >&2; FAIL=$((FAIL + 1)); ERRORS+=("${name}: ${msg}"); }

# ---------------------------------------------------------------------------
# Test suite
# ---------------------------------------------------------------------------

echo "============================================"
echo "settings-hook-registration-smoke.sh — EPIC-0059"
echo "============================================"
echo ""

# Verify dependencies
if [ ! -f "${SETTINGS}" ]; then
  echo "FATAL: settings.json not found at ${SETTINGS}" >&2
  exit 2
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "FATAL: jq is required" >&2
  exit 2
fi

# AT-01: settings.json is valid JSON
at01() {
  if jq empty "${SETTINGS}" 2>/dev/null; then
    pass "AT-01: settings.json is valid JSON"
  else
    fail "AT-01: settings.json valid JSON" "jq parse failed"
  fi
}
at01

# AT-02: PreToolUse section exists in settings.json
at02() {
  local has_pretooluse
  has_pretooluse=$(jq -r '.hooks.PreToolUse // empty' "${SETTINGS}" 2>/dev/null)
  if [ -n "${has_pretooluse}" ]; then
    pass "AT-02: PreToolUse section exists in settings.json"
  else
    fail "AT-02: PreToolUse section" "PreToolUse not found in hooks section"
  fi
}
at02

# AT-03: enforce-no-bypass-flags.sh is registered under PreToolUse
at03() {
  local registered
  registered=$(jq -r '.hooks.PreToolUse[]?.hooks[]?.command // empty' "${SETTINGS}" 2>/dev/null \
    | grep "enforce-no-bypass-flags.sh" || true)
  if [ -n "${registered}" ]; then
    pass "AT-03: enforce-no-bypass-flags.sh registered under PreToolUse"
  else
    fail "AT-03: hook registration" "enforce-no-bypass-flags.sh not found in PreToolUse hooks"
  fi
}
at03

# AT-04: hook command uses \$CLAUDE_PROJECT_DIR prefix
at04() {
  local cmd
  cmd=$(jq -r '.hooks.PreToolUse[]?.hooks[]?.command // empty' "${SETTINGS}" 2>/dev/null \
    | grep "enforce-no-bypass-flags.sh" || true)
  if echo "${cmd}" | grep -q "CLAUDE_PROJECT_DIR"; then
    pass "AT-04: hook command uses CLAUDE_PROJECT_DIR prefix"
  else
    fail "AT-04: CLAUDE_PROJECT_DIR prefix" "hook command: ${cmd}"
  fi
}
at04

# AT-05: hook has timeout configured
at05() {
  local timeout
  timeout=$(jq -r '
    .hooks.PreToolUse[]?.hooks[]?
    | select(.command != null)
    | select(.command | contains("enforce-no-bypass-flags.sh"))
    | .timeout // empty
  ' "${SETTINGS}" 2>/dev/null | head -1)
  if [ -n "${timeout}" ] && [ "${timeout}" -gt 0 ] 2>/dev/null; then
    pass "AT-05: hook timeout configured (${timeout}s)"
  else
    fail "AT-05: hook timeout" "timeout not found or invalid: ${timeout}"
  fi
}
at05

# AT-06: hook script exists on disk and is executable
at06() {
  if [ -x "${HOOK}" ]; then
    pass "AT-06: enforce-no-bypass-flags.sh exists and is executable"
  else
    fail "AT-06: hook executable" "not found or not executable: ${HOOK}"
  fi
}
at06

# AT-07: hook starts with bash shebang
at07() {
  local first_line
  first_line=$(head -1 "${HOOK}" 2>/dev/null || echo "")
  if echo "${first_line}" | grep -q "#!/usr/bin/env bash"; then
    pass "AT-07: hook starts with bash shebang"
  else
    fail "AT-07: bash shebang" "first line: ${first_line}"
  fi
}
at07

# AT-08: --self-check validates registration
at08() {
  local result
  result=$(CLAUDE_PROJECT_DIR="${REPO_ROOT}" bash "${HOOK}" --self-check 2>&1; echo "exit:$?")
  local exitcode
  exitcode=$(echo "${result}" | grep "exit:" | tail -1 | sed 's/exit://')
  if [ "${exitcode}" = "0" ]; then
    pass "AT-08: --self-check passes (registration confirmed)"
  else
    fail "AT-08: --self-check" "exit ${exitcode}, output: ${result}"
  fi
}
at08

# AT-09: enforce-phase-sequence.sh is still present (no regression)
at09() {
  local registered
  registered=$(jq -r '.hooks.PreToolUse[]?.hooks[]?.command // empty' "${SETTINGS}" 2>/dev/null \
    | grep "enforce-phase-sequence.sh" || true)
  if [ -n "${registered}" ]; then
    pass "AT-09: enforce-phase-sequence.sh still registered (no regression)"
  else
    fail "AT-09: enforce-phase-sequence.sh regression" "hook missing from PreToolUse"
  fi
}
at09

# AT-10: Source-of-truth hook matches deployed hook
at10() {
  local source_hook="${REPO_ROOT}/java/src/main/resources/targets/claude/hooks/enforce-no-bypass-flags.sh"
  if [ ! -f "${source_hook}" ]; then
    fail "AT-10: source-of-truth hook" "not found at ${source_hook}"
    return
  fi
  if diff -q "${HOOK}" "${source_hook}" >/dev/null 2>&1; then
    pass "AT-10: deployed hook matches source-of-truth"
  else
    fail "AT-10: hook source-of-truth match" "deployed and source-of-truth differ"
  fi
}
at10

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
