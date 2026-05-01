#!/usr/bin/env bash
# audit-regression-shell.sh — Camada 2 (CI) audit for EPIC-0073 (Regression Shell Gate).
#
# Verifies that every PR in a project with quality.regression.enabled=true AND
# mode=service has evidence that x-test-regression-shell --service was executed.
# Evidence is detected via:
#   1. Presence of telemetry NDJSON entry (tool.call for x-test-regression-shell) in
#      ai/epics/epic-*/telemetry/events.ndjson for the story being merged.
#   2. Presence of marker artifact ai/epics/epic-*/reports/regression-report-*.md
#      for the story branch.
#
# If quality.regression.enabled=false or mode=self, the check is a no-op (exit 0).
# If scenarios.yaml is absent from tests/regression/, exits 1 (precondition unmet).
#
# Exit codes (Rule 26 §Standardized):
#   0 — OK (gate passed or regression not enabled/service mode)
#   1 — REGRESSION_SHELL_VIOLATION (service mode declared but not executed)
#   2 — OPERATIONAL_ERROR (jq missing, project YAML unreadable, etc.)
#   3 — BASELINE_CORRUPT or INVALID_EXEMPTION
#
# Usage:
#   audit-regression-shell.sh                       # full audit (auto-detect YAML)
#   audit-regression-shell.sh --self-check          # verify wiring
#   audit-regression-shell.sh --config <path>       # explicit project YAML path
#   audit-regression-shell.sh --story <STORY-ID>    # audit a single story
#
# Rule: .claude/rules/26-audit-gate-lifecycle.md (Camada 2)
# Skill: .claude/skills/x-test-regression-shell/SKILL.md
# Catalog: docs/audit-gates-catalog.md (Rule 26 §Catalog-before-Add)

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

BASELINE_FILE="governance/baselines/regression-shell-baseline.txt"
SCENARIOS_FILE="tests/regression/scenarios.yaml"

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

while [[ $# -gt 0 ]]; do
    case "$1" in
        --config) CONFIG_PATH="$2"; shift 2 ;;
        --story)  STORY_FILTER="$2"; shift 2 ;;
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

regression_enabled() {
    local yaml_file="$1"
    local enabled
    enabled="$(yaml_value "${yaml_file}" "quality.regression.enabled")"
    [[ "${enabled}" == "true" ]]
}

regression_mode() {
    local yaml_file="$1"
    yaml_value "${yaml_file}" "quality.regression.mode"
}

has_regression_evidence() {
    local story_id="$1"
    # Check marker artifact
    local report
    report="$(find ai/epics -name "regression-report-${story_id}.md" 2>/dev/null | head -1)"
    [[ -n "${report}" ]] && return 0

    # Check telemetry NDJSON
    local ndjson
    ndjson="$(find ai/epics -name "events.ndjson" 2>/dev/null | head -5)"
    for f in ${ndjson}; do
        if grep -q "x-test-regression-shell" "${f}" 2>/dev/null; then
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

    if ! regression_enabled "${yaml_file}"; then
        echo "INFO: quality.regression.enabled=false — regression-shell gate skipped" >&2
        exit 0
    fi

    local mode
    mode="$(regression_mode "${yaml_file}")"
    if [[ "${mode}" != "service" ]]; then
        echo "INFO: quality.regression.mode=${mode} — service mode required for gate; skipped" >&2
        exit 0
    fi

    # Verify scenarios.yaml exists
    if [[ ! -f "${SCENARIOS_FILE}" ]]; then
        echo "REGRESSION_SHELL_VIOLATION: quality.regression.enabled=true && mode=service but ${SCENARIOS_FILE} not found." >&2
        echo "  Regenerate via: ia-dev-env generate (or invoke renderRegressionScenarios)" >&2
        exit 1
    fi

    # Find stories to audit
    local stories=()
    if [[ -n "${STORY_FILTER}" ]]; then
        stories=("${STORY_FILTER}")
    else
        # Auto-detect stories from recent PRs merged to develop/epic/*
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

        if ! has_regression_evidence "${story_id}"; then
            echo "REGRESSION_SHELL_VIOLATION: story ${story_id} — quality.regression.mode=service declared but x-test-regression-shell not executed." >&2
            echo "  Invoke: /x-test-regression-shell ${story_id} --service" >&2
            violations=$((violations + 1))
        fi
    done

    [[ "${violations}" -eq 0 ]] && exit 0 || exit 1
}

main "$@"
