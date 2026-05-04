#!/usr/bin/env bash
# Layer:      2 (CI script — detective)
# Trigger:    PR open/sync, manual invocation
# Exit codes: 0=OK (example passes), 1=PRODUCT_TEMPLATE_VIOLATION, 2=OPERATIONAL_ERROR
# Description: Validates that example-product-saas.md contains all 8 sections
#              and at least 6 mandatory RNF categories

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
EXAMPLES_DIR="${REPO_ROOT}/ai/examples"
EXAMPLE_FILE="${EXAMPLES_DIR}/example-product-saas.md"

REQUIRED_SECTIONS=(
    "## 1. Propósito"
    "## 2. Personas"
    "## 3. Requisitos Funcionais"
    "## 4. RNFs Root"
    "## 5. Constraints"
    "## 6. KPIs"
    "## 7. Arquitetura"
    "## 8. Roadmap"
)

MANDATORY_RNF_CATEGORIES=(
    "PERFORMANCE"
    "SCALABILITY"
    "RELIABILITY"
    "SECURITY"
    "COMPLIANCE"
    "OBSERVABILITY"
)

case "${1:-}" in
    --self-check)
        command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
        [[ -d "${EXAMPLES_DIR}" ]] || { echo "OPERATIONAL_ERROR: ${EXAMPLES_DIR} not found" >&2; exit 2; }
        exit 0
        ;;
esac

violations=0

if [[ ! -f "${EXAMPLE_FILE}" ]]; then
    echo "PRODUCT_TEMPLATE_VIOLATION: ${EXAMPLE_FILE} not found" >&2
    exit 1
fi

for section in "${REQUIRED_SECTIONS[@]}"; do
    if ! grep -q "${section}" "${EXAMPLE_FILE}"; then
        echo "PRODUCT_TEMPLATE_VIOLATION: missing section '${section}'" >&2
        violations=$((violations + 1))
    fi
done

for category in "${MANDATORY_RNF_CATEGORIES[@]}"; do
    if ! grep -q "${category}" "${EXAMPLE_FILE}"; then
        echo "PRODUCT_TEMPLATE_VIOLATION: missing mandatory RNF category '${category}'" >&2
        violations=$((violations + 1))
    fi
done

if [[ ${violations} -gt 0 ]]; then
    echo "RESULT: FAIL (${violations} violation(s))" >&2
    exit 1
fi

echo "RESULT: PASS (all 8 sections present, all 6 mandatory RNF categories validated)"
exit 0
