#!/usr/bin/env bash
# Layer:      2 (CI script — detective)
# Trigger:    PR open/sync, manual invocation
# Exit codes: 0=OK (example passes), 1=CAPABILITY_TEMPLATE_VIOLATION, 2=OPERATIONAL_ERROR
# Description: Validates that example-capability-auth.md contains all 7 sections
#              and demonstrates no-relax override mechanism

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
EXAMPLES_DIR="${REPO_ROOT}/ai/examples"
EXAMPLE_FILE="${EXAMPLES_DIR}/example-capability-auth.md"

REQUIRED_SECTIONS=(
    "## 1. Definição & Escopo"
    "## 2. RNFs Herdadas"
    "## 3. Requisitos Funcionais"
    "## 4. Interfaces & Portas"
    "## 5. Constraints Técnicos"
    "## 6. Plano de Testes"
    "## 7. Roadmap de Entrega"
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
    echo "CAPABILITY_TEMPLATE_VIOLATION: ${EXAMPLE_FILE} not found" >&2
    exit 1
fi

for section in "${REQUIRED_SECTIONS[@]}"; do
    if ! grep -q "${section}" "${EXAMPLE_FILE}"; then
        echo "CAPABILITY_TEMPLATE_VIOLATION: missing section '${section}'" >&2
        violations=$((violations + 1))
    fi
done

if ! grep -q "no-relax" "${EXAMPLE_FILE}"; then
    echo "CAPABILITY_TEMPLATE_VIOLATION: no-relax mechanism not documented" >&2
    violations=$((violations + 1))
fi

if [[ ${violations} -gt 0 ]]; then
    echo "RESULT: FAIL (${violations} violation(s))" >&2
    exit 1
fi

echo "RESULT: PASS (all 7 sections present, no-relax mechanism validated)"
exit 0
