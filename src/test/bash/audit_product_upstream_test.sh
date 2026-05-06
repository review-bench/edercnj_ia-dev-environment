#!/usr/bin/env bash
set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
SCRIPT_SRC="${REPO_ROOT}/src/main/resources/targets/claude/scripts/audit-product-upstream.sh"

PASS=0
FAIL=0
pass() { echo "  ✅ PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "  ❌ FAIL: $1 — $2" >&2; FAIL=$((FAIL + 1)); }

setup_repo() {
  local tmpdir="$1"
  mkdir -p "${tmpdir}/ai/products" "${tmpdir}/ai/features"
}

echo "=============================================="
echo "audit_product_upstream_test.sh"
echo "=============================================="

TMP=$(mktemp -d); setup_repo "${TMP}"
RC=0; OUT=$(cd "${TMP}" && bash "${SCRIPT_SRC}" --self-check 2>&1) || RC=$?
if [[ "${RC}" -eq 0 ]]; then pass "self-check exit 0"; else fail "self-check exit 0" "${OUT}"; fi
rm -rf "${TMP}"

TMP=$(mktemp -d); setup_repo "${TMP}"
cat > "${TMP}/ai/products/product-0001-product.json" <<'EOF'
{"productId":"product-0001"}
EOF
cat > "${TMP}/ai/products/product-0001-capability-c1.json" <<'EOF'
{"capabilityId":"capability-c1","productId":"product-0001"}
EOF
cat > "${TMP}/ai/features/capability-c1-feature-0001.json" <<'EOF'
{"featureId":"feature-0001","capabilityId":"capability-c1"}
EOF
RC=0; OUT=$(cd "${TMP}" && bash "${SCRIPT_SRC}" 2>&1) || RC=$?
if [[ "${RC}" -eq 0 ]]; then pass "valid hierarchy exit 0"; else fail "valid hierarchy exit 0" "${OUT}"; fi
rm -rf "${TMP}"

TMP=$(mktemp -d); setup_repo "${TMP}"
cat > "${TMP}/ai/products/product-0001-product.json" <<'EOF'
{"productId":"product-0001"}
EOF
RC=0; ERR=$(cd "${TMP}" && bash "${SCRIPT_SRC}" 2>&1 >/dev/null) || RC=$?
if [[ "${RC}" -eq 1 ]]; then pass "product without capability exit 1"; else fail "product without capability exit 1" "${ERR}"; fi
if echo "${ERR}" | grep -q "product-without-capability"; then pass "reports product violation"; else fail "reports product violation" "${ERR}"; fi
rm -rf "${TMP}"

echo "=============================================="
echo "Tests passed: $PASS"
echo "Tests failed: $FAIL"
echo "=============================================="
if [[ "${FAIL}" -gt 0 ]]; then exit 1; fi
