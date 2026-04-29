#!/usr/bin/env bash
# audit-review-frontmatter.sh — Rule 26 §CI script — EPIC-0067
#
# Validates that review-story-*.md and techlead-review-story-*.md files
# contain well-formed YAML frontmatter v1.0 conforming to
# governance/schemas/review-frontmatter-1.0.json.
#
# Exit codes (Rule 26 §Standardized):
#   0 — OK (all artifacts valid or grandfathered)
#   1 — REVIEW_FRONTMATTER_VIOLATION (frontmatter absent or invalid field)
#   2 — OPERATIONAL_ERROR (yq/jq missing, schema not found)
#   3 — INVALID_EXEMPTION (audit-exempt marker without reason)
#
# Usage:
#   audit-review-frontmatter.sh --all
#   audit-review-frontmatter.sh --epic EPIC-0067
#   audit-review-frontmatter.sh --story story-0067-0002
#   audit-review-frontmatter.sh --self-check

set -u

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

SCHEMA_FILE="${REPO_ROOT}/governance/schemas/review-frontmatter-1.0.json"
BASELINE_DIR="${BASELINE_DIR:-${REPO_ROOT}/governance/baselines}"
BASELINE_FILE="${BASELINE_DIR}/review-frontmatter-baseline.txt"

# ── Self-check ─────────────────────────────────────────────────────────────────
if [[ "${1:-}" == "--self-check" ]]; then
    exit_code=0
    for tool in yq jq; do
        if ! command -v "${tool}" >/dev/null 2>&1; then
            printf 'OPERATIONAL_ERROR: %s not found on PATH\n' "${tool}" >&2
            exit_code=2
        fi
    done
    if [[ ! -f "${SCHEMA_FILE}" ]]; then
        printf 'OPERATIONAL_ERROR: schema file not found: %s\n' "${SCHEMA_FILE}" >&2
        exit_code=2
    fi
    if [[ -f "${BASELINE_FILE}" && ! -r "${BASELINE_FILE}" ]]; then
        printf 'OPERATIONAL_ERROR: baseline file not readable: %s\n' "${BASELINE_FILE}" >&2
        exit_code=2
    fi
    [[ "${exit_code}" -eq 0 ]] && printf 'SELF_CHECK_OK: %s prerequisites satisfied\n' "${SCRIPT_NAME}" >&2
    exit "${exit_code}"
fi

# ── Prereq tools ───────────────────────────────────────────────────────────────
for tool in yq jq; do
    if ! command -v "${tool}" >/dev/null 2>&1; then
        printf 'OPERATIONAL_ERROR: %s not found on PATH\n' "${tool}" >&2
        exit 2
    fi
done

if [[ ! -f "${SCHEMA_FILE}" ]]; then
    printf 'OPERATIONAL_ERROR: schema file not found: %s\n' "${SCHEMA_FILE}" >&2
    exit 2
fi

# ── Arg parse ──────────────────────────────────────────────────────────────────
MODE=""
FILTER=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --all)   MODE="all"; shift ;;
        --epic)  MODE="epic"; FILTER="$2"; shift 2 ;;
        --story) MODE="story"; FILTER="$2"; shift 2 ;;
        --repo)  shift 2 ;;  # ignored: context only
        *) printf 'OPERATIONAL_ERROR: unknown flag: %s\n' "$1" >&2; exit 2 ;;
    esac
done

if [[ -z "${MODE}" ]]; then
    printf 'OPERATIONAL_ERROR: one of --all, --epic, --story required\n' >&2
    exit 2
fi

# ── Baseline loader (bash 3.2-compatible — no associative arrays) ──────────────
is_grandfathered() {
    local story_id="$1"
    [[ -f "${BASELINE_FILE}" ]] || return 1
    # strip comments, collapse whitespace, grep for exact story-id
    grep -v '^[[:space:]]*#' "${BASELINE_FILE}" 2>/dev/null \
        | sed 's/#.*//' \
        | tr -d '[:space:]' \
        | grep -q "^${story_id}$"
}

# ── File collector ─────────────────────────────────────────────────────────────
collect_files() {
    local -a files=()

    # v3 layout: plans/epic-*/plans/
    while IFS= read -r f; do files+=("$f"); done < <(
        find "${REPO_ROOT}/plans" -maxdepth 3 \
            \( -name "review-story-*.md" -o -name "techlead-review-story-*.md" \) \
            2>/dev/null | sort
    )

    # v4 layout: ai/epics/*/plans/
    while IFS= read -r f; do files+=("$f"); done < <(
        find "${REPO_ROOT}/ai/epics" -maxdepth 3 \
            \( -name "review-story-*.md" -o -name "techlead-review-story-*.md" \) \
            2>/dev/null | sort
    )

    # bash 3.2 compat: guard empty array under set -u
    printf '%s\n' "${files[@]+"${files[@]}"}"
}

filter_files() {
    local -a all_files=("${@+"$@"}")
    local -a filtered=()

    case "${MODE}" in
        all) filtered=("${all_files[@]+"${all_files[@]}"}") ;;
        epic)
            local epic_lower="${FILTER,,}"
            local epic_num="${epic_lower#epic-}"
            for f in "${all_files[@]+"${all_files[@]}"}"; do
                if [[ "${f}" == *"epic-${epic_num}"* || "${f,,}" == *"${epic_lower}"* ]]; then
                    filtered+=("$f")
                fi
            done
            ;;
        story)
            for f in "${all_files[@]+"${all_files[@]}"}"; do
                if [[ "${f}" == *"${FILTER}"* ]]; then
                    filtered+=("$f")
                fi
            done
            ;;
    esac

    printf '%s\n' "${filtered[@]+"${filtered[@]}"}"
}

# ── Frontmatter extractor ──────────────────────────────────────────────────────
extract_frontmatter() {
    local file="$1"
    local -a lines=()
    local in_block=false
    local found=false

    while IFS= read -r line; do
        if ! "${in_block}" && [[ "${line}" == "---" ]]; then
            in_block=true
            found=true
            continue
        fi
        if "${in_block}"; then
            if [[ "${line}" == "---" || "${line}" == "..." ]]; then
                break
            fi
            lines+=("${line}")
        fi
    done < "${file}"

    if ! "${found}"; then
        return 1
    fi
    printf '%s\n' "${lines[@]+"${lines[@]}"}"
}

# ── Per-file validator ─────────────────────────────────────────────────────────
violations=()

validate_file() {
    local file="$1"
    local rel_path="${file#${REPO_ROOT}/}"

    # extract story-id from filename for baseline lookup
    local basename_f
    basename_f="$(basename "${file}" .md)"
    local story_id=""
    if [[ "${basename_f}" =~ (story-[0-9]+-[0-9]+) ]]; then
        story_id="${BASH_REMATCH[1]}"
    fi

    # check audit-exempt marker
    if grep -qF '<!-- audit-exempt:' "${file}" 2>/dev/null; then
        local reason
        reason="$(grep -m1 'audit-exempt:' "${file}" | sed 's/.*audit-exempt: *//' | sed 's/ *-->.*//')"
        if [[ -z "${reason}" ]]; then
            printf 'INVALID_EXEMPTION: audit-exempt marker missing reason at %s\n' "${rel_path}" >&2
            violations+=("INVALID_EXEMPTION:${rel_path}")
            return
        fi
        return  # legitimately exempt
    fi

    # check baseline
    if [[ -n "${story_id}" ]] && is_grandfathered "${story_id}"; then
        return  # grandfathered
    fi

    # extract frontmatter
    local frontmatter
    frontmatter="$(extract_frontmatter "${file}")"
    if [[ $? -ne 0 || -z "${frontmatter}" ]]; then
        printf 'REVIEW_FRONTMATTER_VIOLATION: %s: frontmatter absent\n' "${rel_path}" >&2
        violations+=("${rel_path}")
        return
    fi

    # skip the template-version comment line if present before frontmatter
    # (files start with <!-- template-version: ... --> then ---)

    # convert YAML to JSON
    local frontmatter_json
    frontmatter_json="$(printf '%s\n' "${frontmatter}" | yq -o=json '.' 2>/dev/null)"
    if [[ $? -ne 0 || -z "${frontmatter_json}" ]]; then
        printf 'REVIEW_FRONTMATTER_VIOLATION: %s: frontmatter not valid YAML\n' "${rel_path}" >&2
        violations+=("${rel_path}")
        return
    fi

    # validate required fields from schema (bash 3.2-compatible)
    local -a required_fields
    required_fields=()
    while IFS= read -r _field; do required_fields+=("${_field}"); done \
        < <(jq -r '.required[]' "${SCHEMA_FILE}" 2>/dev/null)
    for field in "${required_fields[@]}"; do
        if ! jq -e "has(\"${field}\")" <<< "${frontmatter_json}" >/dev/null 2>&1; then
            printf "REVIEW_FRONTMATTER_VIOLATION: %s: required field '%s' missing\n" \
                "${rel_path}" "${field}" >&2
            violations+=("${rel_path}")
            return
        fi
    done

    # validate decision enum
    local decision_value
    decision_value="$(jq -r '."decision"' <<< "${frontmatter_json}" 2>/dev/null)"
    case "${decision_value}" in
        GO|NO-GO|GO-WITH-RESERVATIONS) ;;
        *)
            printf "REVIEW_FRONTMATTER_VIOLATION: %s: invalid decision value '%s' (expected GO|NO-GO|GO-WITH-RESERVATIONS)\n" \
                "${rel_path}" "${decision_value}" >&2
            violations+=("${rel_path}")
            ;;
    esac
}

# ── Main (bash 3.2-compatible array loading) ───────────────────────────────────
all_files=()
while IFS= read -r _f; do all_files+=("${_f}"); done < <(collect_files)

target_files=()
while IFS= read -r _f; do target_files+=("${_f}"); done \
    < <(filter_files "${all_files[@]+"${all_files[@]}"}")

for f in "${target_files[@]+"${target_files[@]}"}"; do
    [[ -f "${f}" ]] || continue
    # prevent path traversal
    real_f="$(realpath "${f}" 2>/dev/null || readlink -f "${f}" 2>/dev/null || echo "${f}")"
    if [[ "${real_f}" != "${REPO_ROOT}"* ]]; then
        printf 'OPERATIONAL_ERROR: path outside repo root: %s\n' "${f}" >&2
        exit 2
    fi
    validate_file "${real_f}"
done

# ── Result ─────────────────────────────────────────────────────────────────────
has_invalid_exemption=false
# bash 3.2 compat: guard empty array expansion under set -u
for v in "${violations[@]+"${violations[@]}"}"; do
    if [[ "${v}" == INVALID_EXEMPTION:* ]]; then
        has_invalid_exemption=true
        break
    fi
done

if "${has_invalid_exemption}"; then
    exit 3
fi

if [[ ${#violations[@]} -gt 0 ]]; then
    exit 1
fi

exit 0
