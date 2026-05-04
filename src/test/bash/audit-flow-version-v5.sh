#!/usr/bin/env bash
# Smoke tests for audit-flow-version.sh v5 support (TASK-0077-0029-002)
# Gherkin scenarios:
#   SC-01: flowVersion "5" passes validation
#   SC-02: flowVersion "5" absent productFirstLifecycle emits WARN, not fail
#   SC-03: flowVersion "6" (future/unknown) triggers FLOW_VERSION_VIOLATION
#   SC-04: --self-check exits 0 when jq available
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
AUDIT_SCRIPT="${REPO_ROOT}/src/main/resources/targets/claude/scripts/audit-flow-version.sh"

PASS=0; FAIL=0

pass() { echo "PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "FAIL: $1"; FAIL=$((FAIL + 1)); }

make_state_dir() {
  local tmpdir
  tmpdir="$(mktemp -d)"
  mkdir -p "${tmpdir}/ai/epics/epic-test"
  echo "$tmpdir"
}

write_state() {
  local dir="$1" content="$2"
  echo "$content" > "${dir}/ai/epics/epic-test/execution-state.json"
}

run_audit() {
  local dir="$1"; shift
  AUDIT_FLOW_VERSION_PLANS_GLOB="${dir}/ai/epics/epic-*/execution-state.json" \
    bash "${AUDIT_SCRIPT}" "$@" 2>&1
  return ${PIPESTATUS[0]}
}

# SC-01: flowVersion "5" passes validation (exit 0, no violation)
# Given an execution-state.json with flowVersion="5" and productFirstLifecycle=true
# When audit-flow-version.sh runs
# Then exit code is 0 and no FLOW_VERSION_VIOLATION is emitted
SC01() {
  local dir
  dir=$(make_state_dir)
  write_state "$dir" '{"flowVersion":"5","epicId":"EPIC-0077","productFirstLifecycle":true}'
  local out rc=0; out=$(run_audit "$dir") || rc=$?
  rm -rf "$dir"
  if [[ $rc -eq 0 ]] && ! echo "$out" | grep -q "VIOLATION"; then
    pass "SC-01: flowVersion=5 accepted (exit 0, no violation)"
  else
    fail "SC-01: flowVersion=5 rejected — exit=$rc output=$out"
  fi
}

# SC-02: flowVersion "5" without productFirstLifecycle still exits 0
# (audit-flow-version.sh does not validate productFirstLifecycle — that is Rule 19's
# runtime job; the script only validates the flowVersion field itself)
# Given an execution-state.json with flowVersion="5" but no productFirstLifecycle
# When audit-flow-version.sh runs
# Then exit code is 0
SC02() {
  local dir
  dir=$(make_state_dir)
  write_state "$dir" '{"flowVersion":"5","epicId":"EPIC-0077"}'
  local out rc=0; out=$(run_audit "$dir") || rc=$?
  rm -rf "$dir"
  if [[ $rc -eq 0 ]]; then
    pass "SC-02: flowVersion=5 without productFirstLifecycle exits 0"
  else
    fail "SC-02: unexpected exit=$rc output=$out"
  fi
}

# SC-03: flowVersion "6" (unknown future value) triggers FLOW_VERSION_VIOLATION
# Given an execution-state.json with flowVersion="6"
# When audit-flow-version.sh runs
# Then exit code is 1 and FLOW_VERSION_VIOLATION is present in stderr
SC03() {
  local dir
  dir=$(make_state_dir)
  write_state "$dir" '{"flowVersion":"6","epicId":"EPIC-9999"}'
  local out rc=0; out=$(run_audit "$dir") || rc=$?
  rm -rf "$dir"
  if [[ $rc -eq 1 ]] && echo "$out" | grep -q "FLOW_VERSION_VIOLATION"; then
    pass "SC-03: flowVersion=6 triggers FLOW_VERSION_VIOLATION (exit 1)"
  else
    fail "SC-03: expected exit=1 + VIOLATION; got exit=$rc output=$out"
  fi
}

# SC-04: --self-check exits 0 when jq is available
# Given jq is on PATH
# When audit-flow-version.sh --self-check runs
# Then exit code is 0
SC04() {
  if ! command -v jq &>/dev/null; then
    echo "SKIP: SC-04: jq not on PATH — skipping self-check smoke"
    return
  fi
  local out rc=0; out=$(bash "${AUDIT_SCRIPT}" --self-check 2>&1) || rc=$?
  if [[ $rc -eq 0 ]]; then
    pass "SC-04: --self-check exits 0 with jq available"
  else
    fail "SC-04: --self-check exit=$rc output=$out"
  fi
}

# Run all scenarios
SC01
SC02
SC03
SC04

echo ""
echo "Results: PASS=${PASS} FAIL=${FAIL}"
[[ $FAIL -eq 0 ]] && exit 0 || exit 1
