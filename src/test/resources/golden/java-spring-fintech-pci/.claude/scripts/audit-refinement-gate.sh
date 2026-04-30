#!/usr/bin/env bash
# audit-refinement-gate.sh — Camada 2 (CI) audit for Rule 29 (Refinement Gate, EPIC-0069).
#
# Verifies that every story/epic implemented and merged has
# refinementVerdict.status == "approved" registered in execution-state.json,
# OR is covered by a valid exception:
#   - hotfix/* branches (Rule 27 Exception 2)
#   - entries in governance/baselines/refinement-gate-baseline.txt
#   - per-line `<!-- audit-exempt: <reason> -->` in story markdown
#   - flowVersion=1 (Rule 19 legacy fallback — implicit no-op)
#
# Also detects state↔markdown divergence: if state.refinementVerdict.verdictHash
# is present, the SHA-256 of the `## Refinement Verdict` block in the markdown
# MUST match (sub-code: "verdict-mismatch").
#
# Exit codes (Rule 26 §Standardized):
#   0 — OK
#   1 — REFINEMENT_GATE_VIOLATION (sub-code on stderr)
#   2 — OPERATIONAL_ERROR (jq missing, baseline unreadable, etc.)
#   3 — BASELINE_CORRUPT or INVALID_EXEMPTION
#   4 — RULE_29_ENFORCEMENT_BROKEN (--self-check failure)
#
# Usage:
#   audit-refinement-gate.sh                       # full audit
#   audit-refinement-gate.sh --self-check          # verify wiring
#   audit-refinement-gate.sh --since <ref>         # audit merges since git ref
#   audit-refinement-gate.sh --story <STORY-ID>    # audit a single story
#
# Rule: .claude/rules/29-refinement-gate.md
# Hook (Camada 0): .claude/hooks/enforce-refinement-gate.sh
# Catalog: docs/audit-gates-catalog.md (Rule 26 §Catalog-before-Add)

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

BASELINE_FILE="governance/baselines/refinement-gate-baseline.txt"
RULE_FILE=".claude/rules/29-refinement-gate.md"
HOOK_FILE=".claude/hooks/enforce-refinement-gate.sh"
RULE_SOT="src/main/resources/targets/claude/rules/29-refinement-gate.md"
HOOK_SOT="src/main/resources/targets/claude/hooks/enforce-refinement-gate.sh"
CAPABILITY_FILE="capabilities/governance/refinement-gate.yaml"

self_check() {
    local broken=0
    command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required" >&2; exit 2; }
    if [[ ! -f "${RULE_FILE}" && ! -f "${RULE_SOT}" ]]; then
        echo "SELF_CHECK_FAIL: ${RULE_FILE} (and SOT ${RULE_SOT}) missing" >&2
        broken=1
    fi
    if [[ ! -f "${HOOK_FILE}" && ! -f "${HOOK_SOT}" ]]; then
        echo "SELF_CHECK_FAIL: ${HOOK_FILE} (and SOT ${HOOK_SOT}) missing" >&2
        broken=1
    fi
    if [[ ! -f "${BASELINE_FILE}" ]]; then
        echo "SELF_CHECK_FAIL: ${BASELINE_FILE} missing" >&2
        broken=1
    fi
    if [[ ! -f "${CAPABILITY_FILE}" ]]; then
        echo "SELF_CHECK_FAIL: ${CAPABILITY_FILE} missing" >&2
        broken=1
    fi
    if [[ ${broken} -eq 1 ]]; then
        echo "RULE_29_ENFORCEMENT_BROKEN" >&2
        exit 4
    fi
    echo "audit-refinement-gate.sh: self-check OK"
    exit 0
}

load_baseline() {
    if [[ ! -f "${BASELINE_FILE}" ]]; then
        echo "BASELINE_CORRUPT: ${BASELINE_FILE} missing" >&2
        exit 3
    fi
    grep -oE '^(story|epic)-[0-9]{4}(-[0-9]{4})?' "${BASELINE_FILE}" 2>/dev/null | sort -u || true
}

is_grandfathered() {
    local target_id="$1"
    grep -qxF "${target_id}" <<< "${BASELINE_IDS}"
}

resolve_state_file() {
    local target_id="$1"
    local epic_num
    if [[ "${target_id}" =~ ^story-([0-9]{4}) ]]; then
        epic_num="${BASH_REMATCH[1]}"
    elif [[ "${target_id}" =~ ^epic-([0-9]{4}) ]]; then
        epic_num="${BASH_REMATCH[1]}"
    else
        return 1
    fi
    # v4 layout
    local found
    found=$(find ai/epics -maxdepth 2 -name execution-state.json -path "*/epic-${epic_num}-*/*" 2>/dev/null | head -1 || true)
    if [[ -z "${found}" ]]; then
        # v3 layout fallback
        local v3="ai/epics/epic-${epic_num}/execution-state.json"
        [[ -f "${v3}" ]] && found="${v3}"
    fi
    echo "${found}"
}

resolve_story_md() {
    local target_id="$1"
    find ai/epics -maxdepth 3 -name "${target_id}.md" 2>/dev/null | head -1 || true
}

has_audit_exempt() {
    local md_file="$1"
    [[ -z "${md_file}" || ! -f "${md_file}" ]] && return 1
    if ! grep -qE '<!--\s*audit-exempt' "${md_file}"; then
        return 1
    fi
    if grep -qE '<!--\s*audit-exempt:\s*[^[:space:]-][^-]*-->' "${md_file}"; then
        return 0
    fi
    echo "INVALID_EXEMPTION: ${md_file} has audit-exempt marker without a reason" >&2
    return 3
}

extract_verdict_block_hash() {
    # Computes SHA-256 of the `## Refinement Verdict` block in the markdown.
    local md_file="$1"
    [[ -z "${md_file}" || ! -f "${md_file}" ]] && return 1
    awk '/^## Refinement Verdict/{flag=1;next} /^## /{flag=0} flag' "${md_file}" \
        | shasum -a 256 2>/dev/null \
        | awk '{print $1}'
}

audit_target() {
    local target_id="$1"
    local violations=0
    local state_file
    state_file=$(resolve_state_file "${target_id}")

    if [[ -z "${state_file}" ]]; then
        # No state file → cannot verify; skip silently (story may not be tracked yet)
        return 0
    fi

    local flow_version
    flow_version=$(jq -r '.flowVersion // "1"' "${state_file}" 2>/dev/null || echo "1")
    if [[ "${flow_version}" == "1" ]]; then
        return 0  # Rule 19 fallback: legacy flow exempt
    fi

    local verdict_status
    verdict_status=$(jq -r '.refinementVerdict.status // "absent"' "${state_file}" 2>/dev/null || echo "absent")

    case "${verdict_status}" in
        approved)
            ;;
        *)
            local md_file
            md_file=$(resolve_story_md "${target_id}")
            local exempt_rc=1
            has_audit_exempt "${md_file}" && exempt_rc=0 || exempt_rc=$?
            if [[ ${exempt_rc} -eq 0 ]]; then
                echo "  ✓ ${target_id} exempt via audit-exempt marker"
                return 0
            elif [[ ${exempt_rc} -eq 3 ]]; then
                exit 3
            fi
            local sub_code="missing-verdict"
            [[ "${verdict_status}" == "rejected" ]] && sub_code="rejected-verdict"
            echo "REFINEMENT_GATE_VIOLATION (${sub_code}): ${target_id} status='${verdict_status}'" >&2
            violations=1
            ;;
    esac

    # Verdict hash divergence check
    local declared_hash
    declared_hash=$(jq -r '.refinementVerdict.verdictHash // empty' "${state_file}" 2>/dev/null || true)
    if [[ -n "${declared_hash}" ]]; then
        local md_file actual_hash
        md_file=$(resolve_story_md "${target_id}")
        actual_hash=$(extract_verdict_block_hash "${md_file}" 2>/dev/null || echo "")
        if [[ -n "${actual_hash}" && "${declared_hash}" != "${actual_hash}" ]]; then
            echo "REFINEMENT_GATE_VIOLATION (verdict-mismatch): ${target_id} state=${declared_hash} markdown=${actual_hash}" >&2
            violations=1
        fi
    fi

    return ${violations}
}

discover_targets_since() {
    local since_ref="$1"
    git log --format='%H %s' "${since_ref}..HEAD" 2>/dev/null \
        | grep -oE '(story|epic)-[0-9]{4}(-[0-9]{4})?' \
        | sort -u
}

# ── Main ──────────────────────────────────────────────────────────────────────

case "${1:-}" in
    --self-check) self_check ;;
esac

command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required" >&2; exit 2; }

# hotfix/* branch exception (Rule 27 Exception 2)
CURRENT_BRANCH=$(git symbolic-ref --short HEAD 2>/dev/null || echo "")
if [[ "${CURRENT_BRANCH}" =~ ^hotfix/ ]]; then
    echo "audit-refinement-gate.sh: hotfix/* branch — exempt (Rule 27 Exception 2)"
    exit 0
fi

BASELINE_IDS="$(load_baseline)"

TOTAL_VIOLATIONS=0
case "${1:-}" in
    --since)
        SINCE_REF="${2:?--since requires a git ref}"
        targets=$(discover_targets_since "${SINCE_REF}")
        ;;
    --story)
        targets="${2:?--story requires a story-id}"
        ;;
    "")
        # Default: audit current branch's stories vs origin/develop
        SINCE_REF="origin/develop"
        targets=$(discover_targets_since "${SINCE_REF}" 2>/dev/null || echo "")
        ;;
    *)
        echo "usage: audit-refinement-gate.sh [--self-check|--since <ref>|--story <id>]" >&2
        exit 2
        ;;
esac

if [[ -z "${targets}" ]]; then
    echo "audit-refinement-gate.sh: no targets to audit"
    exit 0
fi

while IFS= read -r target; do
    [[ -z "${target}" ]] && continue
    if is_grandfathered "${target}"; then
        echo "  ✓ ${target} grandfathered"
        continue
    fi
    rc=0
    audit_target "${target}" || rc=$?
    if [[ ${rc} -ne 0 ]]; then
        TOTAL_VIOLATIONS=$((TOTAL_VIOLATIONS + 1))
    fi
done <<< "${targets}"

if [[ ${TOTAL_VIOLATIONS} -gt 0 ]]; then
    echo "audit-refinement-gate.sh: ${TOTAL_VIOLATIONS} violation(s) detected" >&2
    exit 1
fi

echo "audit-refinement-gate.sh: OK"
exit 0
