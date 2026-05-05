#!/usr/bin/env bash
# Layer:      2 (CI script — detective)
# Trigger:    PR open/sync, manual invocation
# Exit codes: 0=OK (example passes), 1=FEATURE_TEMPLATE_VIOLATION, 2=OPERATIONAL_ERROR
# Description: Validates that _TEMPLATE-FEATURE.md contains all 7 sections
#              and that example features have required structure

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TEMPLATES_DIR="${REPO_ROOT}/ai/templates"
EXAMPLES_DIR="${REPO_ROOT}/ai/examples"
TEMPLATE_FILE="${TEMPLATES_DIR}/_TEMPLATE-FEATURE.md"

REQUIRED_SECTIONS=(
    "## 1. Feature Statement"
    "## 2. Casos de Uso"
    "## 3. Requisitos Funcionais"
    "## 4. Interfaces Exposed"
    "## 5. Acceptance Criteria Detalhados"
    "## 6. Estimativa & Roadmap"
    "## 7. Riscos & Mitigação"
)

case "${1:-}" in
    --self-check)
        command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
        [[ -d "${TEMPLATES_DIR}" ]] || { echo "OPERATIONAL_ERROR: ${TEMPLATES_DIR} not found" >&2; exit 2; }
        [[ -d "${EXAMPLES_DIR}" ]] || { echo "OPERATIONAL_ERROR: ${EXAMPLES_DIR} not found" >&2; exit 2; }
        exit 0
        ;;
esac

violations=0

if [[ ! -f "${TEMPLATE_FILE}" ]]; then
    echo "FEATURE_TEMPLATE_VIOLATION: ${TEMPLATE_FILE} not found" >&2
    exit 1
fi

for section in "${REQUIRED_SECTIONS[@]}"; do
    if ! grep -q "${section}" "${TEMPLATE_FILE}"; then
        echo "FEATURE_TEMPLATE_VIOLATION: missing section '${section}' in template" >&2
        violations=$((violations + 1))
    fi
done

if ! grep -q "Gherkin\|gherkin\|Given\|DADO" "${TEMPLATE_FILE}"; then
    echo "FEATURE_TEMPLATE_VIOLATION: section 5 must include Gherkin acceptance criteria" >&2
    violations=$((violations + 1))
fi

for file in "${EXAMPLES_DIR}"/example-feature-*.md; do
    [[ -f "$file" ]] || continue
    name="$(basename "${file}")"
    for section in "${REQUIRED_SECTIONS[@]}"; do
        if ! grep -q "${section}" "${file}"; then
            echo "FEATURE_TEMPLATE_VIOLATION: ${name} missing section '${section}'" >&2
            violations=$((violations + 1))
        fi
    done
    uc_count=$(grep -c "^### UC-" "${file}" 2>/dev/null || echo 0)
    if [[ ${uc_count} -lt 3 ]]; then
        echo "FEATURE_TEMPLATE_VIOLATION: ${name} has ${uc_count} use cases (minimum 3)" >&2
        violations=$((violations + 1))
    fi
done

if [[ ${violations} -gt 0 ]]; then
    echo "RESULT: FAIL (${violations} violation(s))" >&2
    exit 1
fi

echo "RESULT: PASS (all 7 sections present, Gherkin ACs validated, examples validated)"
exit 0
