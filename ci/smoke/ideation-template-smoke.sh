#!/usr/bin/env bash
# Layer:      2 (CI script — detective)
# Trigger:    PR open/sync, manual invocation
# Exit codes: 0=OK (all pilots pass), 1=IDEATION_TEMPLATE_VIOLATION, 2=OPERATIONAL_ERROR
# Description: Validates that 3 pilot ideations contain all 7 canonical sections

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
EXAMPLES_DIR="${REPO_ROOT}/ai/examples"

REQUIRED_SECTIONS=(
    "## 1. Visão"
    "## 2. Stakeholders"
    "## 3. Requisitos"
    "## 4. Restrições"
    "## 5. Critérios"
    "## 6. Riscos"
    "## 7. Roadmap"
)

PILOT_FILES=(
    "pilot-ideation-001.md"
    "pilot-ideation-002.md"
    "pilot-ideation-003.md"
)

case "${1:-}" in
    --self-check)
        command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
        [[ -d "${EXAMPLES_DIR}" ]] || { echo "OPERATIONAL_ERROR: ${EXAMPLES_DIR} not found" >&2; exit 2; }
        exit 0
        ;;
esac

violations=0

for pilot in "${PILOT_FILES[@]}"; do
    pilot_path="${EXAMPLES_DIR}/${pilot}"
    if [[ ! -f "${pilot_path}" ]]; then
        echo "IDEATION_TEMPLATE_VIOLATION: ${pilot} not found at ${pilot_path}" >&2
        violations=$((violations + 1))
        continue
    fi

    file_violations=0
    for section in "${REQUIRED_SECTIONS[@]}"; do
        if ! grep -q "${section}" "${pilot_path}"; then
            echo "IDEATION_TEMPLATE_VIOLATION: ${pilot} missing section '${section}'" >&2
            file_violations=$((file_violations + 1))
            violations=$((violations + 1))
        fi
    done

    if [[ ${file_violations} -eq 0 ]]; then
        echo "OK: ${pilot} — all 7 sections present"
    fi
done

if [[ ${violations} -gt 0 ]]; then
    echo "RESULT: FAIL (${violations} violation(s))" >&2
    exit 1
fi

echo "RESULT: PASS (3/3 pilot ideations validated, all 7 sections present)"
exit 0
