#!/usr/bin/env bash
# Layer:      2 (CI script — detective)
# Trigger:    PR open/sync, manual invocation
# Exit codes: 0=OK, 1=EPIC_V3_VIOLATION, 2=OPERATIONAL_ERROR
# Description: Validates that _TEMPLATE-EPIC.md is at v3 (contains Source Feature
#              and Inherited RNFs fields) and that the migration script is present.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TEMPLATE_FILE="${REPO_ROOT}/src/main/resources/shared/templates/_TEMPLATE-EPIC.md"
MIGRATION_SCRIPT="${REPO_ROOT}/ai/scripts/migrate-epic-v2-to-v3.sh"

V3_REQUIRED_FIELDS=(
    "template-version: \"3.0\""
    "Source Feature:"
    "Source Feature Link:"
    "## 0.6 Inherited RNFs"
)

case "${1:-}" in
    --self-check)
        command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
        [[ -f "${TEMPLATE_FILE}" ]] || { echo "OPERATIONAL_ERROR: ${TEMPLATE_FILE} not found" >&2; exit 2; }
        [[ -f "${MIGRATION_SCRIPT}" ]] || { echo "OPERATIONAL_ERROR: ${MIGRATION_SCRIPT} not found" >&2; exit 2; }
        exit 0
        ;;
esac

violations=0

if [[ ! -f "${TEMPLATE_FILE}" ]]; then
    echo "EPIC_V3_VIOLATION: ${TEMPLATE_FILE} not found" >&2
    exit 1
fi

for field in "${V3_REQUIRED_FIELDS[@]}"; do
    if ! grep -q "${field}" "${TEMPLATE_FILE}"; then
        echo "EPIC_V3_VIOLATION: missing v3 field '${field}' in template" >&2
        violations=$((violations + 1))
    fi
done

if [[ ! -f "${MIGRATION_SCRIPT}" ]]; then
    echo "EPIC_V3_VIOLATION: migration script not found at ${MIGRATION_SCRIPT}" >&2
    violations=$((violations + 1))
elif [[ ! -x "${MIGRATION_SCRIPT}" ]]; then
    echo "EPIC_V3_VIOLATION: ${MIGRATION_SCRIPT} is not executable" >&2
    violations=$((violations + 1))
else
    "${MIGRATION_SCRIPT}" --self-check || {
        echo "EPIC_V3_VIOLATION: migration script --self-check failed" >&2
        violations=$((violations + 1))
    }
fi

if [[ ${violations} -gt 0 ]]; then
    echo "RESULT: FAIL (${violations} violation(s))" >&2
    exit 1
fi

echo "RESULT: PASS (epic template v3 fields verified, migration script validated)"
exit 0
