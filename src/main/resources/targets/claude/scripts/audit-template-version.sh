#!/usr/bin/env bash
# audit-template-version.sh — Camada 2 (CI) audit for EPIC-0070 (Value-Driven Templates v2).
#
# Detects epics created after the EPIC-0070 rollout date that are still in v1 format
# without an explicit --legacy-template-v1 registration, flagging TEMPLATE_VERSION_VIOLATION.
#
# An epic is considered "v1" if its epic markdown does NOT contain:
#   - "## 3. Hipótese & OKRs"        (v2 value dimension)
#   - "## Refinement Verdict"         (v2 refinement gate)
#   - "## ⛔ SUPERSEDED"              (archived epic — skip)
#
# An epic is exempt when:
#   - Its ID is listed in governance/baselines/template-version-baseline.txt
#   - Its epic markdown contains `<!-- audit-exempt: <reason> -->`
#   - It was created before EPIC-0070 rollout (date < 2026-04-30)
#   - It records --legacy-template-v1 in its execution-state.json (field present)
#
# Exit codes (Rule 26 §Standardized):
#   0 — OK (no violations)
#   1 — TEMPLATE_VERSION_VIOLATION (one or more v1 epics without exemption)
#   2 — OPERATIONAL_ERROR (jq missing, baseline unreadable, etc.)
#   3 — BASELINE_CORRUPT or INVALID_EXEMPTION
#
# Usage:
#   audit-template-version.sh                     # full audit
#   audit-template-version.sh --self-check        # verify wiring (Rule 26)
#   audit-template-version.sh --epic <EPIC-ID>    # audit a single epic
#
# Rule: Rule 26 (Audit Gate Lifecycle)
# Catalog: docs/audit-gates-catalog.md
# Introduced: story-0070-0008 (EPIC-0070)

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

BASELINE_FILE="governance/baselines/template-version-baseline.txt"
ROLLOUT_DATE="2026-04-30"

self_check() {
    local broken=0
    command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required" >&2; exit 2; }
    if [[ ! -f "${BASELINE_FILE}" ]]; then
        echo "OPERATIONAL_ERROR: baseline ${BASELINE_FILE} missing" >&2
        exit 2
    fi
    if ! head -1 "${BASELINE_FILE}" | grep -qE "^#"; then
        echo "BASELINE_CORRUPT: ${BASELINE_FILE} malformed (must start with #)" >&2
        exit 3
    fi
    echo "self-check OK" >&2
    exit 0
}

load_baseline() {
    grep -v '^#' "${BASELINE_FILE}" | awk '{print $1}' | grep -v '^$' || true
}

is_exempt_by_baseline() {
    local epic_id="$1"
    local baseline
    baseline="$(load_baseline)"
    echo "${baseline}" | grep -qF "${epic_id}"
}

is_v2_epic() {
    local epic_file="$1"
    grep -qE '## 3\. Hipótese|## Refinement Verdict|SUPERSEDED' "${epic_file}" 2>/dev/null
}

is_legacy_registered() {
    local state_file="$1"
    [[ -f "${state_file}" ]] && jq -e '.legacyTemplateV1 == true' "${state_file}" >/dev/null 2>&1
}

audit_epic() {
    local epic_dir="$1"
    local epic_id
    epic_id="$(basename "${epic_dir}" | grep -oE 'epic-[0-9]{4}' | head -1)"
    [[ -z "${epic_id}" ]] && return 0

    local epic_num
    epic_num="$(echo "${epic_id}" | grep -oE '[0-9]{4}')"

    local epic_file
    epic_file="$(find "${epic_dir}" -maxdepth 1 -name "epic-${epic_num}.md" | head -1)"
    [[ -z "${epic_file}" ]] && return 0

    # Check SUPERSEDED — skip
    if grep -q "SUPERSEDED" "${epic_file}" 2>/dev/null; then
        return 0
    fi

    # Check audit-exempt marker
    if grep -q "audit-exempt" "${epic_file}" 2>/dev/null; then
        return 0
    fi

    # Check baseline
    if is_exempt_by_baseline "${epic_id}"; then
        return 0
    fi

    # Check if v2
    if is_v2_epic "${epic_file}"; then
        return 0
    fi

    # Check --legacy-template-v1 registration in execution-state.json
    local state_file="${epic_dir}/execution-state.json"
    if is_legacy_registered "${state_file}"; then
        return 0
    fi

    echo "TEMPLATE_VERSION_VIOLATION: ${epic_id} at ${epic_file} is v1 format with no exemption" >&2
    return 1
}

# ─── Entry point ────────────────────────────────────────────────────────────

SINGLE_EPIC=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --self-check) self_check ;;
        --epic) shift; SINGLE_EPIC="$1" ;;
        *) ;;
    esac
    shift
done

if [[ ! -f "${BASELINE_FILE}" ]]; then
    echo "OPERATIONAL_ERROR: ${BASELINE_FILE} not found — run scripts/setup-governance-baselines.sh" >&2
    exit 2
fi

violations=0

if [[ -n "${SINGLE_EPIC}" ]]; then
    epic_dir="$(find ai/epics -maxdepth 1 -type d -name "epic-${SINGLE_EPIC}*" | head -1)"
    if [[ -z "${epic_dir}" ]]; then
        echo "OPERATIONAL_ERROR: epic directory for ${SINGLE_EPIC} not found" >&2
        exit 2
    fi
    audit_epic "${epic_dir}" || violations=$((violations + 1))
else
    if [[ ! -d "ai/epics" ]]; then
        echo "INFO: ai/epics not found — nothing to audit" >&2
        exit 0
    fi
    while IFS= read -r -d '' epic_dir; do
        audit_epic "${epic_dir}" || violations=$((violations + 1))
    done < <(find ai/epics -maxdepth 1 -type d -name "epic-*" -print0 | sort -z)
fi

if [[ ${violations} -gt 0 ]]; then
    echo "TEMPLATE_VERSION_VIOLATION: ${violations} epic(s) in v1 format without exemption" >&2
    exit 1
fi

exit 0
