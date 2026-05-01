#!/usr/bin/env bash
# audit-doc-freshness.sh — Camada 2 (CI) audit for EPIC-0071 (Documentation as DoD).
#
# Detects PRs that modify code requiring a documentation update but whose
# documentation targets were NOT updated in the same change-set.
#
# Detection heuristics (stack-aware):
#   1. Java/Spring REST annotation added/modified → require openapi.yaml / openapi.json update
#   2. New epic markdown under ai/epics/ referencing an ADR → require matching docs/adr/ADR-*.md
#   3. New/modified SKILL.md under .claude/skills/ or src/.../skills/ → require README + skill-docs update
#   4. New Java package under application/ or new subdir in adapter/inbound/ → require system.md update
#
# PRs are auto-skipped when all changes are doc-only:
#   - docs/, CHANGELOG.md, README.md, ai/epics/**, *.md (top-level)
# PRs with test-only changes are also auto-skipped:
#   - **/test/**, **/__tests__/**, **/*Test.java, **/*Spec.*
#
# `<!-- audit-exempt: <reason> -->` in PR body exempts the PR (reason mandatory).
#
# Layer: 2 — CI Script (Rule 26 §Taxonomy)
# Rule:  Rule 31 (Documentation Freshness Gate, EPIC-0071)
# Layer: 2 — CI Script
# Catalog: docs/audit-gates-catalog.md
# Introduced: story-0071-0005 (EPIC-0071)
#
# Exit codes (Rule 26 §Standardized):
#   0 — OK (no violations or all changes doc-only)
#   1 — DOC_FRESHNESS_VIOLATION (code change without required doc update)
#   2 — OPERATIONAL_ERROR (git missing, baseline unreadable, etc.)
#   3 — BASELINE_CORRUPT or INVALID_EXEMPTION
#
# Usage:
#   audit-doc-freshness.sh                          # full audit (diff HEAD~1..HEAD or BASE..HEAD in CI)
#   audit-doc-freshness.sh --self-check             # verify wiring (Rule 26)
#   audit-doc-freshness.sh --base <ref>             # custom base ref for diff
#   audit-doc-freshness.sh --pr-body-file <path>    # path to PR body text for audit-exempt check

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

BASELINE_FILE="governance/baselines/doc-freshness-baseline.txt"
RULE_FILE=".claude/rules/31-documentation-freshness-gate.md"
BASE_REF="${BASE_REF:-HEAD~1}"
PR_BODY_FILE=""
VIOLATIONS=0

# ── helpers ──────────────────────────────────────────────────────────────────

log_violation() {
    echo "DOC_FRESHNESS_VIOLATION: $*" >&2
    VIOLATIONS=$((VIOLATIONS + 1))
}

log_warn() {
    echo "WARN [audit-doc-freshness] $*" >&2
}

# ── self-check ────────────────────────────────────────────────────────────────

self_check() {
    local broken=0
    command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; broken=1; }
    command -v git  >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: git required"  >&2; broken=1; }
    command -v jq   >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required"   >&2; broken=1; }
    if [[ ! -f "${BASELINE_FILE}" ]]; then
        echo "OPERATIONAL_ERROR: baseline ${BASELINE_FILE} missing" >&2
        broken=1
    fi
    if [[ ! -f "${RULE_FILE}" ]]; then
        log_warn "rule file ${RULE_FILE} not found — gate wiring incomplete"
        broken=1
    fi
    [[ ${broken} -eq 0 ]] && exit 0 || exit 2
}

# ── argument parsing ─────────────────────────────────────────────────────────

while [[ $# -gt 0 ]]; do
    case "$1" in
        --self-check)   self_check ;;
        --base)         BASE_REF="$2"; shift ;;
        --pr-body-file) PR_BODY_FILE="$2"; shift ;;
        *) echo "OPERATIONAL_ERROR: unknown argument '$1'" >&2; exit 2 ;;
    esac
    shift
done

# ── baseline / exemption helpers ─────────────────────────────────────────────

load_baseline() {
    if [[ ! -f "${BASELINE_FILE}" ]]; then
        echo "OPERATIONAL_ERROR: ${BASELINE_FILE} missing" >&2
        exit 2
    fi
    # Validate baseline format: lines must be blank, comment (#), or PR-number
    if grep -qE "^[^#[:space:]][^0-9]" "${BASELINE_FILE}" 2>/dev/null; then
        echo "BASELINE_CORRUPT: ${BASELINE_FILE} contains unexpected lines" >&2
        exit 3
    fi
}

is_baseline_exempt() {
    local pr_number="$1"
    [[ -z "${pr_number}" ]] && return 1
    grep -qE "^${pr_number}([[:space:]]|$)" "${BASELINE_FILE}" 2>/dev/null
}

check_audit_exempt() {
    [[ -z "${PR_BODY_FILE}" ]] && return 1
    [[ ! -f "${PR_BODY_FILE}" ]] && return 1
    local marker
    marker=$(grep -o '<!--[[:space:]]*audit-exempt:[^->]*-->' "${PR_BODY_FILE}" | head -1)
    if [[ -z "${marker}" ]]; then
        return 1
    fi
    # Extract reason (text after 'audit-exempt:' and before '-->')
    local reason
    reason=$(echo "${marker}" | sed 's/<!--[[:space:]]*audit-exempt:[[:space:]]*//' | sed 's/[[:space:]]*-->//' | xargs)
    if [[ -z "${reason}" ]]; then
        echo "INVALID_EXEMPTION: audit-exempt marker requires a reason" >&2
        exit 3
    fi
    log_warn "audit-exempt applied: ${reason}"
    return 0
}

# ── diff helpers ──────────────────────────────────────────────────────────────

get_changed_files() {
    git diff --name-only "${BASE_REF}"..HEAD 2>/dev/null
}

# Returns 0 if all changes are doc-only (skip gate entirely)
is_doc_only_change() {
    local changed_files="$1"
    # Files that are NOT doc-only
    local non_doc
    non_doc=$(echo "${changed_files}" | grep -vE \
        '(^docs/|^CHANGELOG\.md$|^README\.md$|^ai/epics/|\.md$|^\.github/)' \
        | grep -vE '(^src/test/|/test/|/__tests__/|Test\.java$|Spec\.(ts|js|py|rb|go)$)' \
        || true)
    [[ -z "${non_doc}" ]]
}

# ── detection heuristics ──────────────────────────────────────────────────────

# Heuristic 1: Java/Spring REST annotation → require OpenAPI update
check_rest_endpoints() {
    local changed_files="$1"
    local java_changes
    java_changes=$(echo "${changed_files}" | grep -E '\.java$' || true)
    [[ -z "${java_changes}" ]] && return 0

    local has_rest_annotation=false
    while IFS= read -r file; do
        [[ ! -f "${file}" ]] && continue
        if git diff "${BASE_REF}"..HEAD -- "${file}" 2>/dev/null \
            | grep -qE '^\+.*@(RestController|RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|Path|Operation)'; then
            has_rest_annotation=true
            break
        fi
    done <<< "${java_changes}"

    [[ "${has_rest_annotation}" == false ]] && return 0

    # Check if openapi.yaml / openapi.json was touched
    local openapi_updated
    openapi_updated=$(echo "${changed_files}" | grep -iE '(openapi\.ya?ml|openapi\.json|swagger\.ya?ml|swagger\.json)' || true)
    if [[ -z "${openapi_updated}" ]]; then
        log_violation "REST annotation added/modified but OpenAPI spec not updated (openapi.yaml / openapi.json)"
    fi
}

# Heuristic 2: New ADR reference in epic without matching ADR file
check_adr_references() {
    local changed_files="$1"
    local epic_files
    epic_files=$(echo "${changed_files}" | grep -E '^ai/epics/.*epic-.*\.md$' || true)
    [[ -z "${epic_files}" ]] && return 0

    while IFS= read -r file; do
        [[ ! -f "${file}" ]] && continue
        # Look for new ADR references in diff
        local new_adr_refs
        new_adr_refs=$(git diff "${BASE_REF}"..HEAD -- "${file}" 2>/dev/null \
            | grep -oE '^\+.*ADR-([0-9]{4})' \
            | grep -oE 'ADR-[0-9]{4}' \
            | sort -u || true)
        while IFS= read -r adr_ref; do
            [[ -z "${adr_ref}" ]] && continue
            local adr_file
            adr_file=$(find docs/adr -name "${adr_ref}-*.md" 2>/dev/null | head -1)
            if [[ -z "${adr_file}" ]]; then
                log_violation "Epic references ${adr_ref} but docs/adr/${adr_ref}-*.md not found"
            fi
        done <<< "${new_adr_refs}"
    done <<< "${epic_files}"
}

# Heuristic 3: New/modified SKILL.md → require README update
check_skill_docs() {
    local changed_files="$1"
    local skill_changes
    skill_changes=$(echo "${changed_files}" | grep -E 'SKILL\.md$' || true)
    [[ -z "${skill_changes}" ]] && return 0

    # Check if any README.md was touched
    local readme_updated
    readme_updated=$(echo "${changed_files}" | grep -iE '^README\.md$|^docs/.*README\.md$|^\.claude/README\.md$' || true)
    if [[ -z "${readme_updated}" ]]; then
        log_warn "SKILL.md modified but no README.md updated — docs may be stale (advisory)"
        # Advisory only — not a hard violation (skills can be internal changes)
    fi
}

# Heuristic 4: New Java subdir under application/ or adapter/inbound/ → require system.md
check_architecture_changes() {
    local changed_files="$1"
    local arch_files
    arch_files=$(echo "${changed_files}" | grep -E 'src/main/java/.*(application/|adapter/inbound/|adapter/outbound/).*\.java$' || true)
    [[ -z "${arch_files}" ]] && return 0

    # Detect new directories (files in paths not previously present)
    local new_packages
    new_packages=$(git diff "${BASE_REF}"..HEAD --diff-filter=A --name-only 2>/dev/null \
        | grep -E 'src/main/java/.*(application/|adapter/inbound/|adapter/outbound/).*\.java$' \
        | sed 's|/[^/]*\.java$||' | sort -u || true)
    [[ -z "${new_packages}" ]] && return 0

    # Check if docs/architecture/system.md was touched
    local system_updated
    system_updated=$(echo "${changed_files}" | grep -E '^docs/architecture/system\.md$' || true)
    if [[ -z "${system_updated}" ]]; then
        log_violation "New Java package(s) under application/ or adapter/ detected ($(echo "${new_packages}" | head -1 | xargs basename)) but docs/architecture/system.md not updated"
    fi
}

# ── main ──────────────────────────────────────────────────────────────────────

load_baseline

# Check audit-exempt in PR body
check_audit_exempt && exit 0

CHANGED_FILES="$(get_changed_files)"

if [[ -z "${CHANGED_FILES}" ]]; then
    echo "OK — no changes detected in range ${BASE_REF}..HEAD"
    exit 0
fi

# Auto-skip doc-only and test-only PRs
if is_doc_only_change "${CHANGED_FILES}"; then
    echo "OK — doc/test-only changes, documentation gate skipped"
    exit 0
fi

# Run all heuristics
check_rest_endpoints "${CHANGED_FILES}"
check_adr_references "${CHANGED_FILES}"
check_skill_docs "${CHANGED_FILES}"
check_architecture_changes "${CHANGED_FILES}"

if [[ ${VIOLATIONS} -gt 0 ]]; then
    echo "DOC_FRESHNESS_VIOLATION: ${VIOLATIONS} violation(s) detected — update documentation before merge" >&2
    exit 1
fi

echo "OK — all documentation freshness checks passed"
exit 0
