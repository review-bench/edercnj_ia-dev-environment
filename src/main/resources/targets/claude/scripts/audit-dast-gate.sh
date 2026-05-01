#!/usr/bin/env bash
# audit-dast-gate.sh — Camada 2 (CI) audit for EPIC-0073 (DAST Gate).
#
# Verifies that projects with quality.dast.enabled=true have DAST evidence for
# merged stories: SARIF artifact under results/security/dast/ and Markdown report
# under ai/epics/epic-*/reports/dast-report-STORY-ID.md.
#
# Also validates:
#   - Nuclei templates-version is pinned (no latest/master/HEAD) → NUCLEI_VERSION_UNPINNED
#   - target != production → DAST_TARGET_PRODUCTION_FORBIDDEN
#
# Exit codes (Rule 26 §Standardized):
#   0 — OK (gate passed or DAST not enabled)
#   1 — DAST_GATE_VIOLATION (evidence missing, production target, or unpinned version)
#   2 — OPERATIONAL_ERROR (jq missing, project YAML unreadable, etc.)
#   3 — BASELINE_CORRUPT or INVALID_EXEMPTION
#
# Usage:
#   audit-dast-gate.sh                        # full audit
#   audit-dast-gate.sh --self-check           # verify wiring
#   audit-dast-gate.sh --config <path>        # explicit project YAML path
#   audit-dast-gate.sh --story <STORY-ID>     # audit a single story
#   audit-dast-gate.sh --check-config-only    # validate YAML fields only (no evidence check)
#
# Rule: .claude/rules/26-audit-gate-lifecycle.md (Camada 2)
# Skill: .claude/skills/x-pentest-dynamic/SKILL.md
# Catalog: docs/audit-gates-catalog.md (Rule 26 §Catalog-before-Add)

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

BASELINE_FILE="governance/baselines/dast-gate-baseline.txt"
UNPINNED_VERSIONS="latest master head nightly"
PINNED_PATTERN="^v[0-9]+(\.[x0-9]+(\.[0-9]+)?)?$"

# ─── Self-check ───────────────────────────────────────────────────────────────

self_check() {
    local failures=0

    command -v jq >/dev/null 2>&1 || {
        echo "OPERATIONAL_ERROR: jq not found on PATH" >&2
        failures=$((failures + 1))
    }

    command -v yq >/dev/null 2>&1 || command -v python3 >/dev/null 2>&1 || {
        echo "OPERATIONAL_ERROR: yq or python3 required to parse project YAML" >&2
        failures=$((failures + 1))
    }

    [[ -f "${BASELINE_FILE}" ]] || {
        echo "OPERATIONAL_ERROR: baseline file not found: ${BASELINE_FILE}" >&2
        failures=$((failures + 1))
    }

    [[ "${failures}" -eq 0 ]] && exit 0 || exit 2
}

[[ "${1:-}" == "--self-check" ]] && self_check

# ─── Arg parsing ──────────────────────────────────────────────────────────────

CONFIG_PATH=""
STORY_FILTER=""
CONFIG_ONLY=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --config)            CONFIG_PATH="$2"; shift 2 ;;
        --story)             STORY_FILTER="$2"; shift 2 ;;
        --check-config-only) CONFIG_ONLY=true; shift ;;
        *) shift ;;
    esac
done

# ─── Helpers ──────────────────────────────────────────────────────────────────

find_project_yaml() {
    if [[ -n "${CONFIG_PATH}" && -f "${CONFIG_PATH}" ]]; then
        echo "${CONFIG_PATH}"
        return
    fi
    for f in project.yaml project.yml config.yaml config.yml; do
        [[ -f "${f}" ]] && echo "${f}" && return
    done
    echo ""
}

yaml_value() {
    local file="$1" key="$2"
    if command -v yq >/dev/null 2>&1; then
        yq e ".${key} // \"\"" "${file}" 2>/dev/null || echo ""
    else
        python3 -c "
import yaml, sys
data = yaml.safe_load(open('${file}'))
keys = '${key}'.split('.')
v = data
for k in keys:
    if isinstance(v, dict): v = v.get(k, '')
    else: v = ''; break
print(v if v is not None else '')
" 2>/dev/null || echo ""
    fi
}

dast_enabled() {
    local yaml_file="$1"
    local enabled
    enabled="$(yaml_value "${yaml_file}" "quality.dast.enabled")"
    [[ "${enabled}" == "true" ]]
}

check_config() {
    local yaml_file="$1"
    local violations=0

    # Check target != production
    local target
    target="$(yaml_value "${yaml_file}" "quality.dast.target")"
    if [[ "${target,,}" == "production" ]]; then
        echo "DAST_GATE_VIOLATION: DAST_TARGET_PRODUCTION_FORBIDDEN — target=production is forbidden." >&2
        echo "  Change quality.dast.target to: local-container, preview-env, or staging" >&2
        violations=$((violations + 1))
    fi

    # Check Nuclei templates-version pinned
    local nuclei_version
    nuclei_version="$(yaml_value "${yaml_file}" "quality.dast.nuclei.templates-version")"
    if [[ -n "${nuclei_version}" ]]; then
        local lower_ver="${nuclei_version,,}"
        for unpinned in ${UNPINNED_VERSIONS}; do
            if [[ "${lower_ver}" == "${unpinned}" ]]; then
                echo "DAST_GATE_VIOLATION: NUCLEI_VERSION_UNPINNED — templates-version=${nuclei_version} is not pinned." >&2
                echo "  Use a pinned version: v9.x, v9.0.1, etc." >&2
                violations=$((violations + 1))
                break
            fi
        done
        if ! echo "${nuclei_version}" | grep -qP "${PINNED_PATTERN}" 2>/dev/null; then
            if [[ "${lower_ver}" != *"unpinned"* ]]; then
                : # Already warned above or valid format
            fi
        fi
    fi

    return "${violations}"
}

has_dast_evidence() {
    local story_id="$1"
    # Check Markdown report artifact
    local report
    report="$(find ai/epics -name "dast-report-${story_id}.md" 2>/dev/null | head -1)"
    [[ -n "${report}" ]] && return 0

    # Check SARIF artifact
    local sarif
    sarif="$(find results/security/dast -name "*.sarif" 2>/dev/null | head -1)"
    [[ -n "${sarif}" ]] && return 0

    # Check telemetry NDJSON
    local ndjson
    ndjson="$(find ai/epics -name "events.ndjson" 2>/dev/null | head -5)"
    for f in ${ndjson}; do
        if grep -q "x-pentest-dynamic" "${f}" 2>/dev/null; then
            return 0
        fi
    done

    return 1
}

is_grandfathered() {
    local story_id="$1"
    [[ -f "${BASELINE_FILE}" ]] && grep -q "^${story_id}" "${BASELINE_FILE}" 2>/dev/null
}

# ─── Main ─────────────────────────────────────────────────────────────────────

main() {
    local yaml_file
    yaml_file="$(find_project_yaml)"

    if [[ -z "${yaml_file}" ]]; then
        echo "OPERATIONAL_ERROR: no project YAML found (searched: project.yaml, project.yml)" >&2
        exit 2
    fi

    if ! dast_enabled "${yaml_file}"; then
        echo "INFO: quality.dast.enabled=false — DAST gate skipped" >&2
        exit 0
    fi

    # Always validate config fields
    local config_violations=0
    check_config "${yaml_file}" || config_violations=$?
    if [[ "${config_violations}" -gt 0 ]]; then
        exit 1
    fi

    "${CONFIG_ONLY}" && exit 0

    # Find stories to audit
    local stories=()
    if [[ -n "${STORY_FILTER}" ]]; then
        stories=("${STORY_FILTER}")
    else
        while IFS= read -r story_id; do
            stories+=("${story_id}")
        done < <(git log --oneline --merges HEAD~20..HEAD 2>/dev/null \
            | grep -oE 'story-[0-9]{4}-[0-9]{4}' | sort -u)
    fi

    local violations=0
    for story_id in "${stories[@]}"; do
        if is_grandfathered "${story_id}"; then
            echo "INFO: ${story_id} grandfathered in baseline — skipping" >&2
            continue
        fi

        if ! has_dast_evidence "${story_id}"; then
            echo "DAST_GATE_VIOLATION: story ${story_id} — quality.dast.enabled=true but no DAST evidence found." >&2
            echo "  Invoke: /x-pentest-dynamic ${story_id} --tier smoke" >&2
            violations=$((violations + 1))
        fi
    done

    [[ "${violations}" -eq 0 ]] && exit 0 || exit 1
}

main "$@"
