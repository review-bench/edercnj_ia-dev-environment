#!/usr/bin/env bash
# Layer:      2 (CI script — detective)
# Trigger:    PR open/sync, manual invocation
# Exit codes: 0=OK, 1=RNF_OVERRIDE_AUDIT_VIOLATION, 2=OPERATIONAL_ERROR
# Description: Lists all RNF overrides found in capability examples with approval status

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
EXAMPLES_DIR="${REPO_ROOT}/ai/examples"

case "${1:-}" in
    --self-check)
        command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
        [[ -d "${EXAMPLES_DIR}" ]] || { echo "OPERATIONAL_ERROR: ${EXAMPLES_DIR} not found" >&2; exit 2; }
        exit 0
        ;;
    --list)
        echo "=== RNF Override Audit ==="
        echo ""
        for file in "${EXAMPLES_DIR}"/example-capability-*.md; do
            [[ -f "$file" ]] || continue
            name="$(basename "${file}")"
            echo "--- ${name} ---"
            grep -E "^\| (PERFORMANCE|SCALABILITY|RELIABILITY|SECURITY|COMPLIANCE|OBSERVABILITY)" "${file}" 2>/dev/null \
                | grep -v "| true |" \
                | while IFS='|' read -r _ category original norelax override justification approval approver _; do
                    printf "  Category: %-15s  Status: %-10s  Approver: %s\n" \
                        "$(echo "${category}" | xargs)" \
                        "$(echo "${approval}" | xargs)" \
                        "$(echo "${approver}" | xargs)"
                done
            echo ""
        done
        exit 0
        ;;
esac

pending_count=0

for file in "${EXAMPLES_DIR}"/example-capability-*.md; do
    [[ -f "$file" ]] || continue
    pending=$(grep -E "^\| (PERFORMANCE|SCALABILITY|RELIABILITY|OBSERVABILITY)" "${file}" 2>/dev/null \
        | grep -v "| true |" \
        | grep "pending" \
        | wc -l | xargs)
    pending_count=$((pending_count + pending))
done

if [[ ${pending_count} -gt 0 ]]; then
    echo "RNF_OVERRIDE_AUDIT_VIOLATION: ${pending_count} RNF override(s) pending approval" >&2
    echo "  Run with --list to see details" >&2
    exit 1
fi

echo "RESULT: PASS (no pending RNF overrides)"
exit 0
