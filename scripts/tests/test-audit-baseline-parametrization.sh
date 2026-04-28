#!/usr/bin/env bash
# test-audit-baseline-parametrization.sh
# Smoke test for story-0062-0001: verifies BASELINE_DIR parametrization works
# in audit scripts using default path and custom override.
#
# Exit codes:
#   0 — all tests pass
#   1 — one or more tests failed

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

PASS=0
FAIL=0

pass() { echo "PASS: $*"; PASS=$((PASS + 1)); }
fail() { echo "FAIL: $*" >&2; FAIL=$((FAIL + 1)); }

# ── Scenario 1: default BASELINE_DIR reads from audits/ ──────────────────────
if bash "${REPO_ROOT}/scripts/audit-baseline-immutability.sh" --self-check >/dev/null 2>&1; then
    pass "audit-baseline-immutability.sh --self-check (default BASELINE_DIR=audits)"
else
    fail "audit-baseline-immutability.sh --self-check failed with default BASELINE_DIR"
fi

if bash "${REPO_ROOT}/scripts/audit-execution-integrity.sh" --self-check >/dev/null 2>&1; then
    pass "audit-execution-integrity.sh --self-check (default)"
else
    fail "audit-execution-integrity.sh --self-check failed with default BASELINE_DIR"
fi

# ── Scenario 2: BASELINE_DIR override ────────────────────────────────────────
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

# Copy baseline files to temp location
cp "${REPO_ROOT}/audits/"*.txt "${TMP_DIR}/" 2>/dev/null || true
cp "${REPO_ROOT}/audits/"*.sha "${TMP_DIR}/" 2>/dev/null || true

if [[ -z "$(ls "${TMP_DIR}"/*.txt 2>/dev/null)" ]]; then
    fail "No baseline .txt files found to copy to temp dir"
else
    if BASELINE_DIR="${TMP_DIR}" \
        bash "${REPO_ROOT}/scripts/audit-baseline-immutability.sh" --self-check \
        >/dev/null 2>&1; then
        pass "audit-baseline-immutability.sh --self-check (BASELINE_DIR override)"
    else
        fail "audit-baseline-immutability.sh --self-check failed with BASELINE_DIR=${TMP_DIR}"
    fi
fi

# ── Scenario 3: verify no literal audits/ in quotes remain ───────────────────
LITERAL_HITS=$(grep -rE "['\"]audits/['\"]" \
    "${REPO_ROOT}/scripts/audit-"*.sh \
    "${REPO_ROOT}/scripts/setup-branch-protection.sh" \
    2>/dev/null | wc -l)

if [[ "${LITERAL_HITS}" -eq 0 ]]; then
    pass "No literal \"audits/\" or 'audits/' found in audit scripts (AC1)"
else
    fail "Found ${LITERAL_HITS} literal \"audits/\" references — parametrization incomplete"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "Results: ${PASS} passed, ${FAIL} failed"

if [[ "${FAIL}" -gt 0 ]]; then
    exit 1
fi
exit 0
