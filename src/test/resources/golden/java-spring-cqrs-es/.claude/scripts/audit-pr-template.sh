#!/usr/bin/env bash
# audit-pr-template.sh — Hard gate for PR body template-version marker (EPIC-0066, story-0066-0007).
# Layer: CI script (Rule 26 §Taxonomy).
# Exit codes (Rule 26 §Standardized): 0=OK 1=PR_TEMPLATE_VIOLATION 2=OPERATIONAL_ERROR 3=INVALID_EXEMPTION
set -euo pipefail

PR_NUMBER=""
REPO=""
HOTFIX_BYPASS="false"
SELF_CHECK="false"

usage() {
    cat <<USAGE
Usage: $(basename "$0") --pr <PR_NUMBER> [--repo <owner/repo>]
       $(basename "$0") --self-check
       $(basename "$0") --help

Validates that a PR body contains the <!-- template-version: --> marker and the
mandatory sections for its kind (implementation: 9 sections, backlog: 12 sections).

Exit codes:
  0  OK (compliant or grandfathered or audit-exempt)
  1  PR_TEMPLATE_VIOLATION
  2  OPERATIONAL_ERROR (gh/jq missing, PR not found)
  3  INVALID_EXEMPTION (audit-exempt without reason, baseline corrupt)
USAGE
}

# ── arg parsing ───────────────────────────────────────────────────────────────
parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --pr) PR_NUMBER="$2"; shift 2 ;;
            --repo) REPO="$2"; shift 2 ;;
            --hotfix-bypass) HOTFIX_BYPASS="true"; shift ;;
            --self-check) SELF_CHECK="true"; shift ;;
            --help) usage; exit 0 ;;
            *) echo "OPERATIONAL_ERROR: unknown flag: $1" >&2; exit 2 ;;
        esac
    done
}

# ── self-check ────────────────────────────────────────────────────────────────
self_check() {
    command -v gh >/dev/null 2>&1 \
        || { echo "OPERATIONAL_ERROR: gh CLI not found on PATH" >&2; exit 2; }
    command -v jq >/dev/null 2>&1 \
        || { echo "OPERATIONAL_ERROR: jq not found on PATH" >&2; exit 2; }
    local baseline="${CLAUDE_PROJECT_DIR:-$PWD}/governance/baselines/pr-template-baseline.txt"
    if [[ -e "$baseline" && ! -r "$baseline" ]]; then
        echo "OPERATIONAL_ERROR: baseline not readable: $baseline" >&2; exit 2
    fi
    echo "OK: audit-pr-template.sh self-check passed"
}

# ── baseline check ────────────────────────────────────────────────────────────
is_in_baseline() {
    local pr="$1"
    local baseline="${CLAUDE_PROJECT_DIR:-$PWD}/governance/baselines/pr-template-baseline.txt"
    [[ -f "$baseline" ]] || return 1
    grep -qE "^\s*${pr}\s*(#.*)?$" "$baseline"
}

# ── audit-exempt check ────────────────────────────────────────────────────────
has_audit_exempt() {
    local body="$1"
    if echo "$body" | grep -qE "<!--\s*audit-exempt:\s*[^>]+\s*-->"; then
        # has audit-exempt with non-empty reason
        return 0
    fi
    if echo "$body" | grep -qE "<!--\s*audit-exempt:\s*-->"; then
        # audit-exempt with empty reason — INVALID
        echo "INVALID_EXEMPTION: audit-exempt marker requires a non-empty reason" >&2
        exit 3
    fi
    return 1
}

# ── kind detection + section validation ───────────────────────────────────────
detect_kind() {
    local body="$1"
    if echo "$body" | head -5 | grep -q -- "--kind=backlog"; then
        echo "backlog"
    else
        echo "implementation"
    fi
}

implementation_sections=(
    "## Summary"
    "## Story / Task Context"
    "## Acceptance Criteria"
    "## Review Status"
    "## Verify Gate"
    "## Telemetry"
    "## Changes"
    "## Orchestrator Evidence"
)

backlog_sections=(
    "## Summary"
    "## Backlog Entregue"
    "## Lista de Stories"
    "## Estrutura do DAG"
    "## Coordenação com Epics em Andamento"
    "## Métricas de Sucesso"
    "## Test Plan"
    "## Hard-Cut Authorization"
    "## Out of Scope"
    "## Origem (Plan Mode)"
    "## Orchestrator Evidence"
)

validate_sections() {
    local body="$1"
    local kind="$2"
    local -n sections_ref
    if [[ "$kind" == "backlog" ]]; then
        sections_ref=backlog_sections
    else
        sections_ref=implementation_sections
    fi

    if ! echo "$body" | grep -qE "^# "; then
        echo "PR_TEMPLATE_VIOLATION: title heading (^# ) missing" >&2; exit 1
    fi
    for section in "${sections_ref[@]}"; do
        if ! grep -qF "$section" <<< "$body"; then
            echo "PR_TEMPLATE_VIOLATION: required section missing: $section" >&2; exit 1
        fi
    done
}

# ── main audit ────────────────────────────────────────────────────────────────
audit_pr() {
    [[ -n "$PR_NUMBER" ]] \
        || { echo "OPERATIONAL_ERROR: --pr is required" >&2; exit 2; }
    [[ "$PR_NUMBER" =~ ^[0-9]+$ ]] \
        || { echo "OPERATIONAL_ERROR: --pr must be numeric" >&2; exit 2; }

    if [[ "$HOTFIX_BYPASS" == "true" ]]; then
        echo "WARN: --hotfix-bypass active — PR ${PR_NUMBER} marked exempt"
        echo "${PR_NUMBER}  # hotfix-bypass" \
            >> "${CLAUDE_PROJECT_DIR:-$PWD}/governance/baselines/pr-template-baseline.txt"
        exit 0
    fi

    if is_in_baseline "$PR_NUMBER"; then
        echo "OK: PR ${PR_NUMBER} grandfathered in baseline"
        exit 0
    fi

    command -v gh >/dev/null 2>&1 \
        || { echo "OPERATIONAL_ERROR: gh CLI not found" >&2; exit 2; }

    local body
    if [[ -n "$REPO" ]]; then
        body=$(gh pr view "$PR_NUMBER" --repo "$REPO" --json body -q '.body' 2>/dev/null) || {
            echo "OPERATIONAL_ERROR: PR ${PR_NUMBER} not found in $REPO" >&2; exit 2
        }
    else
        body=$(gh pr view "$PR_NUMBER" --json body -q '.body' 2>/dev/null) || {
            echo "OPERATIONAL_ERROR: PR ${PR_NUMBER} not found" >&2; exit 2
        }
    fi

    if has_audit_exempt "$body"; then
        echo "OK: PR ${PR_NUMBER} has valid audit-exempt marker"
        exit 0
    fi

    if ! echo "$body" | head -5 | grep -q "<!-- template-version:"; then
        echo "PR_TEMPLATE_VIOLATION: template-version marker missing in first 5 lines" >&2
        exit 1
    fi

    local kind
    kind=$(detect_kind "$body")
    validate_sections "$body" "$kind"

    echo "OK: PR ${PR_NUMBER} body conforms to template (kind=${kind})"
}

# ── main ──────────────────────────────────────────────────────────────────────
main() {
    parse_args "$@"
    if [[ "$SELF_CHECK" == "true" ]]; then
        self_check
        exit 0
    fi
    audit_pr
}

main "$@"
