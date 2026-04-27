#!/usr/bin/env bash
# Smoke tests for .claude/hooks/stage-telemetry.sh Stop hook (TASK-0059-0008-002)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
HOOK_SCRIPT="${REPO_ROOT}/.claude/hooks/stage-telemetry.sh"
PASS=0; FAIL=0

pass() { echo "PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "FAIL: $1"; FAIL=$((FAIL + 1)); }

setup_git_env() {
  local tmpdir
  tmpdir="$(mktemp -d)"
  git -C "${tmpdir}" init -q
  git -C "${tmpdir}" config user.email "test@test.com"
  git -C "${tmpdir}" config user.name "Test"
  mkdir -p "${tmpdir}/plans/epic-0059/telemetry"
  echo '{}' > "${tmpdir}/plans/epic-0059/execution-state.json"
  printf '{"event":"phase.start","skill":"x-story-implement","phase":"Phase-0-Prepare","storyId":"story-0059-0008","timestamp":"2026-04-27T00:00:00Z"}\n' \
    > "${tmpdir}/plans/epic-0059/telemetry/events.ndjson"
  git -C "${tmpdir}" add .
  git -C "${tmpdir}" commit -q -m "initial"
  # Modify events.ndjson (unstaged)
  printf '{"event":"phase.start","skill":"x-story-implement","phase":"Phase-1-Plan","storyId":"story-0059-0008","timestamp":"2026-04-27T00:01:00Z"}\n' \
    >> "${tmpdir}/plans/epic-0059/telemetry/events.ndjson"
  echo "${tmpdir}"
}

# SH-01: story Em Andamento → events.ndjson staged
T1_DIR="$(setup_git_env)"
cat > "${T1_DIR}/plans/epic-0059/execution-state.json" << 'JSON'
{"storyStatuses":{"story-0059-0008":{"status":"Em Andamento"}}}
JSON
CLAUDE_PROJECT_DIR="${T1_DIR}" bash "${HOOK_SCRIPT}" 2>/dev/null || true
if git -C "${T1_DIR}" diff --cached --name-only | grep -q "events.ndjson"; then
  pass "SH-01: story Em Andamento → events.ndjson staged"
else
  fail "SH-01: story Em Andamento → events.ndjson should be staged"
fi
rm -rf "${T1_DIR}"

# SH-02: no active story → events.ndjson NOT staged
T2_DIR="$(setup_git_env)"
cat > "${T2_DIR}/plans/epic-0059/execution-state.json" << 'JSON'
{"storyStatuses":{"story-0059-0008":{"status":"Concluída"}}}
JSON
CLAUDE_PROJECT_DIR="${T2_DIR}" bash "${HOOK_SCRIPT}" 2>/dev/null || true
if git -C "${T2_DIR}" diff --cached --name-only | grep -q "events.ndjson"; then
  fail "SH-02: no active story → events.ndjson should NOT be staged"
else
  pass "SH-02: no active story → events.ndjson not staged (correct)"
fi
rm -rf "${T2_DIR}"

# SH-03: no execution-state.json → no-op, exit 0
T3_DIR="$(setup_git_env)"
rm "${T3_DIR}/plans/epic-0059/execution-state.json"
if CLAUDE_PROJECT_DIR="${T3_DIR}" bash "${HOOK_SCRIPT}" 2>/dev/null; then
  pass "SH-03: no execution-state.json → exit 0 (no-op)"
else
  fail "SH-03: no execution-state.json → should exit 0"
fi
rm -rf "${T3_DIR}"

# SH-04: CLAUDE_TELEMETRY_DISABLED=1 → no-op
T4_DIR="$(setup_git_env)"
cat > "${T4_DIR}/plans/epic-0059/execution-state.json" << 'JSON'
{"storyStatuses":{"story-0059-0009":{"status":"Em Andamento"}}}
JSON
CLAUDE_PROJECT_DIR="${T4_DIR}" CLAUDE_TELEMETRY_DISABLED=1 bash "${HOOK_SCRIPT}" 2>/dev/null || true
if git -C "${T4_DIR}" diff --cached --name-only | grep -q "events.ndjson"; then
  fail "SH-04: CLAUDE_TELEMETRY_DISABLED=1 → should be no-op"
else
  pass "SH-04: CLAUDE_TELEMETRY_DISABLED=1 → no-op (correct)"
fi
rm -rf "${T4_DIR}"

# SH-05: settings.json includes stage-telemetry.sh as Stop hook
if python3 -c "
import json, sys
d = json.load(open('${REPO_ROOT}/.claude/settings.json'))
stops = d.get('hooks', {}).get('Stop', [])
for group in stops:
    for h in group.get('hooks', []):
        if 'stage-telemetry' in h.get('command', ''):
            sys.exit(0)
sys.exit(1)
" 2>/dev/null; then
  pass "SH-05: stage-telemetry.sh registered as Stop hook in settings.json"
else
  fail "SH-05: stage-telemetry.sh not found in settings.json Stop hooks"
fi

echo ""
echo "Results: ${PASS} passed, ${FAIL} failed"
[[ ${FAIL} -eq 0 ]]
