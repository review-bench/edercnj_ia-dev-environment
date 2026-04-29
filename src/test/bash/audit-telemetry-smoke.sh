#!/usr/bin/env bash
# Smoke tests for audit-execution-integrity.sh telemetry validation (TASK-0059-0008-001)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
AUDIT_SCRIPT="${REPO_ROOT}/scripts/audit-execution-integrity.sh"
PASS=0; FAIL=0

pass() { echo "PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "FAIL: $1"; FAIL=$((FAIL + 1)); }

setup_env() {
  local tmpdir
  tmpdir="$(mktemp -d)"
  mkdir -p "${tmpdir}/plans/epic-0059/telemetry"
  mkdir -p "${tmpdir}/plans/epic-0059/plans"
  mkdir -p "${tmpdir}/audits"
  touch "${tmpdir}/audits/execution-integrity-baseline.txt"
  echo "${tmpdir}"
}

make_ndjson() {
  local dir="$1" story="$2"
  shift 2
  local phases=("$@")
  local ndjson="${dir}/plans/epic-0059/telemetry/events.ndjson"
  for p in "${phases[@]}"; do
    local phase_name
    case "$p" in
      0) phase_name="Phase-0-Prepare" ;;
      1) phase_name="Phase-1-Plan" ;;
      2) phase_name="Phase-2-Implement" ;;
      3) phase_name="Phase-3-Verify" ;;
      *) phase_name="$p" ;;
    esac
    printf '{"event":"phase.start","skill":"x-story-implement","phase":"%s","storyId":"%s","timestamp":"2026-04-27T00:00:00Z"}\n' \
      "${phase_name}" "${story}" >> "${ndjson}"
  done
}

run_audit() {
  local repo_root="$1"; shift
  local story_ids="${1:-}"; shift || true
  local args=("$@")
  if [[ -n "${story_ids}" ]]; then
    AUDIT_TEST_STORY_IDS="${story_ids}" REPO_ROOT="${repo_root}" bash "${AUDIT_SCRIPT}" "${args[@]}" 2>&1 || true
  else
    REPO_ROOT="${repo_root}" bash "${AUDIT_SCRIPT}" "${args[@]}" 2>&1 || true
  fi
}

# AT-01: all 4 events present → exit 0
T1_DIR="$(setup_env)"
make_ndjson "${T1_DIR}" "story-0059-0008" 0 1 2 3
echo "story-0059-0008  # audit-telemetry-test" > "${T1_DIR}/audits/execution-integrity-baseline.txt"
out1="$(run_audit "${T1_DIR}" "" --scope=telemetry)"
if echo "${out1}" | grep -q "OK"; then
  pass "AT-01: all 4 events present → exit 0"
else
  fail "AT-01: all 4 events present → expected OK, got: ${out1}"
fi
rm -rf "${T1_DIR}"

# AT-02: no x-story-implement events → EIE_TELEMETRY_MISSING
T2_DIR="$(setup_env)"
printf '{"event":"tool.call","tool":"Bash","timestamp":"2026-04-27T00:00:00Z"}\n' >> "${T2_DIR}/plans/epic-0059/telemetry/events.ndjson"
out2="$(run_audit "${T2_DIR}" "story-0059-0008" --scope=telemetry)"
if echo "${out2}" | grep -q "EIE_TELEMETRY_MISSING"; then
  pass "AT-02: no x-story-implement events → EIE_TELEMETRY_MISSING"
else
  fail "AT-02: no x-story-implement events → expected EIE_TELEMETRY_MISSING, got: ${out2}"
fi
rm -rf "${T2_DIR}"

# AT-03: Phase-1 PRE_PLANNED accepted
T3_DIR="$(setup_env)"
make_ndjson "${T3_DIR}" "story-0059-0009" 0 2 3
printf '{"event":"phase.skip","skill":"x-story-implement","phase":"Phase-1-Plan","reason":"PRE_PLANNED","storyId":"story-0059-0009","timestamp":"2026-04-27T00:00:00Z"}\n' >> "${T3_DIR}/plans/epic-0059/telemetry/events.ndjson"
echo "story-0059-0009  # pre-planned test" > "${T3_DIR}/audits/execution-integrity-baseline.txt"
out3="$(run_audit "${T3_DIR}" "" --scope=telemetry)"
if echo "${out3}" | grep -q "OK"; then
  pass "AT-03: Phase-1 PRE_PLANNED → exit 0"
else
  fail "AT-03: Phase-1 PRE_PLANNED → expected OK, got: ${out3}"
fi
rm -rf "${T3_DIR}"

# AT-04: Phase-2 absent → EIE_TELEMETRY_MISSING
T4_DIR="$(setup_env)"
make_ndjson "${T4_DIR}" "story-0059-0010" 0 1 3
out4="$(run_audit "${T4_DIR}" "story-0059-0010" --scope=telemetry)"
if echo "${out4}" | grep -q "EIE_TELEMETRY_MISSING"; then
  pass "AT-04: Phase-2 absent → EIE_TELEMETRY_MISSING"
else
  fail "AT-04: Phase-2 absent → expected EIE_TELEMETRY_MISSING, got: ${out4}"
fi
rm -rf "${T4_DIR}"

# AT-05: self-check includes check_telemetry function
out5="$(run_audit "${REPO_ROOT}" "" --self-check)"
if echo "${out5}" | grep -q "check_telemetry"; then
  pass "AT-05: --self-check verifies check_telemetry present"
else
  fail "AT-05: --self-check should mention check_telemetry, got: ${out5}"
fi

# AT-06: EPIC-0057 regression — 171 non-orchestrator events → still fails
T6_DIR="$(setup_env)"
for _ in $(seq 1 171); do
  printf '{"event":"tool.call","tool":"Bash","timestamp":"2026-04-27T00:00:00Z"}\n' >> "${T6_DIR}/plans/epic-0059/telemetry/events.ndjson"
done
out6="$(run_audit "${T6_DIR}" "story-0057-0001" --scope=telemetry)"
if echo "${out6}" | grep -q "EIE_TELEMETRY_MISSING"; then
  pass "AT-06: EPIC-0057 regression detected correctly"
else
  fail "AT-06: 171 non-orchestrator events should fail, got: ${out6}"
fi
rm -rf "${T6_DIR}"

echo ""
echo "Results: ${PASS} passed, ${FAIL} failed"
[[ ${FAIL} -eq 0 ]]
