#!/usr/bin/env bash
set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
SCRIPT_SRC="${REPO_ROOT}/src/main/resources/targets/claude/scripts/audit-c4-completeness.sh"

PASS=0
FAIL=0
pass() { echo "  ✅ PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  ❌ FAIL: $1 — $2" >&2; FAIL=$((FAIL + 1)); }

setup_repo() {
  local tmpdir="$1"
  mkdir -p "${tmpdir}/ai/epics/epic-0099-test"
}

echo "=============================================="
echo "audit_c4_completeness_test.sh"
echo "=============================================="

TMP=$(mktemp -d); setup_repo "${TMP}"
RC=0; OUT=$(cd "${TMP}" && bash "${SCRIPT_SRC}" --self-check 2>&1) || RC=$?
if [[ "${RC}" -eq 0 ]]; then pass "self-check exit 0"; else fail "self-check exit 0" "${OUT}"; fi
rm -rf "${TMP}"

TMP=$(mktemp -d); setup_repo "${TMP}"
cat > "${TMP}/ai/epics/epic-0099-test/story-0099-0001.md" <<'EOF'
## C4 Context
## C4 Container
## C4 Component
## C4 Code
EOF
RC=0; OUT=$(cd "${TMP}" && bash "${SCRIPT_SRC}" 2>&1) || RC=$?
if [[ "${RC}" -eq 0 ]]; then pass "complete c4 exit 0"; else fail "complete c4 exit 0" "${OUT}"; fi
rm -rf "${TMP}"

TMP=$(mktemp -d); setup_repo "${TMP}"
cat > "${TMP}/ai/epics/epic-0099-test/story-0099-0001.md" <<'EOF'
## C4 Context
## C4 Container
EOF
RC=0; ERR=$(cd "${TMP}" && bash "${SCRIPT_SRC}" 2>&1 >/dev/null) || RC=$?
if [[ "${RC}" -eq 1 ]]; then pass "missing c4 exit 1"; else fail "missing c4 exit 1" "${ERR}"; fi
if echo "${ERR}" | grep -q "missing Component Code"; then pass "reports missing component/code"; else fail "reports missing component/code" "${ERR}"; fi
rm -rf "${TMP}"

echo "=============================================="
echo "Tests passed: $PASS"
echo "Tests failed: $FAIL"
echo "=============================================="
if [[ "${FAIL}" -gt 0 ]]; then exit 1; fi
