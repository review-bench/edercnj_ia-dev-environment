#!/usr/bin/env bash
set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
SCRIPT_SRC="${REPO_ROOT}/src/main/resources/targets/claude/scripts/audit-rnf-gates.sh"

PASS=0
FAIL=0
pass() { echo "  ✅ PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  ❌ FAIL: $1 — $2" >&2; FAIL=$((FAIL + 1)); }

setup_repo() {
  local tmpdir="$1"
  mkdir -p "${tmpdir}/ai/epics/epic-0099-test"
}

write_story() {
  local file="$1" row="$2"
  cat > "${file}" <<EOF
## 2. RNFs Herdadas

| Categoria | RNF Original (Produto) | no-relax? | Override Value | Justificação | Approval Status | Approver |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
${row}

## 9. Refinement Verdict
EOF
}

echo "=============================================="
echo "audit_rnf_gates_test.sh"
echo "=============================================="

TMP=$(mktemp -d); setup_repo "${TMP}"
RC=0; OUT=$(cd "${TMP}" && bash "${SCRIPT_SRC}" --self-check 2>&1) || RC=$?
if [[ "${RC}" -eq 0 ]]; then pass "self-check exit 0"; else fail "self-check exit 0" "${OUT}"; fi
rm -rf "${TMP}"

TMP=$(mktemp -d); setup_repo "${TMP}"
write_story "${TMP}/ai/epics/epic-0099-test/story-0099-0001.md" \
"| PERFORMANCE | P99 < 500ms | false | P99 < 300ms | tighter SLA | APPROVED | architect |"
RC=0; OUT=$(cd "${TMP}" && bash "${SCRIPT_SRC}" 2>&1) || RC=$?
if [[ "${RC}" -eq 0 ]]; then pass "approved RNF exit 0"; else fail "approved RNF exit 0" "${OUT}"; fi
rm -rf "${TMP}"

TMP=$(mktemp -d); setup_repo "${TMP}"
write_story "${TMP}/ai/epics/epic-0099-test/story-0099-0001.md" \
"| SECURITY | MFA required | false | optional MFA | rollout | APPROVED | architect |"
RC=0; ERR=$(cd "${TMP}" && bash "${SCRIPT_SRC}" 2>&1 >/dev/null) || RC=$?
if [[ "${RC}" -eq 1 ]]; then pass "security relax exit 1"; else fail "security relax exit 1" "${ERR}"; fi
if echo "${ERR}" | grep -q "no-relax-broken"; then pass "reports no-relax broken"; else fail "reports no-relax broken" "${ERR}"; fi
rm -rf "${TMP}"

TMP=$(mktemp -d); setup_repo "${TMP}"
write_story "${TMP}/ai/epics/epic-0099-test/story-0099-0001.md" \
"| PERFORMANCE | P99 < 500ms | false | P99 < 300ms | — | APPROVED | architect |"
RC=0; ERR=$(cd "${TMP}" && bash "${SCRIPT_SRC}" 2>&1 >/dev/null) || RC=$?
if [[ "${RC}" -eq 1 ]]; then pass "em dash justification exit 1"; else fail "em dash justification exit 1" "${ERR}"; fi
if echo "${ERR}" | grep -q "unapproved-relaxation"; then pass "reports em dash as empty"; else fail "reports em dash as empty" "${ERR}"; fi
rm -rf "${TMP}"

echo "=============================================="
echo "Tests passed: $PASS"
echo "Tests failed: $FAIL"
echo "=============================================="
if [[ "${FAIL}" -gt 0 ]]; then exit 1; fi
