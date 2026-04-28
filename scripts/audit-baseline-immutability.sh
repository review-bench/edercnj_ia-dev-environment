#!/usr/bin/env bash
# audit-baseline-immutability.sh — Immutability enforcement for baseline files.
#
# Introduced by EPIC-0059 story-0059-0011 (Rule 27 — Zero-Bypass Lifecycle).
#
# PURPOSE:
#   After EPIC-0059 merges, the amnesty baseline files (execution-integrity-baseline.txt,
#   rule-26-baseline.txt, etc.) are frozen. This script detects any lines added AFTER the
#   EPIC-0059 cutoff commit (the commit that introduced the amnesty via story-0059-0011)
#   and fails the build with BASELINE_IMMUTABILITY_VIOLATION.
#
# HOW THE CUTOFF WORKS:
#   The cutoff is the last commit that touched audits/rule-26-baseline.txt as part of
#   the authorized EPIC-0059 amnesty window (story-0059-0011). The commit SHA is
#   stored in audits/baseline-cutoff.sha. If that file is absent, the script looks
#   for the most recent commit that added audits/rule-26-baseline.txt.
#
#   Override: BASELINE_CUTOFF_SHA env var bypasses all detection (for testing).
#
# Exception path:
#   A line in a baseline file with `<!-- baseline-correction: <reason> -->` immediately
#   before or on the same line as the new story ID is exempt from immutability.
#   This escape hatch requires human review via CODEOWNERS.
#
# Exit codes (Rule 26 standard):
#   0 — OK: no lines added after cutoff
#   1 — BASELINE_IMMUTABILITY_VIOLATION: new entries detected after cutoff
#   2 — OPERATIONAL_ERROR: missing dependency or invalid argument
#   3 — BASELINE_CORRUPT: baseline file malformed
#   4 — ENFORCEMENT_BROKEN: cutoff commit not found
#
# Usage:
#   scripts/audit-baseline-immutability.sh                     # audit all baseline files
#   scripts/audit-baseline-immutability.sh --self-check        # verify infrastructure
#   scripts/audit-baseline-immutability.sh --json              # JSON output envelope
#   scripts/audit-baseline-immutability.sh --file <path>       # audit a single file
#   BASELINE_CUTOFF_SHA=<sha> scripts/audit-baseline-immutability.sh  # override cutoff
#
# Catalogued at: docs/audit-gates-catalog.md
# Rule: .claude/rules/27-zero-bypass-lifecycle.md (EPIC-0059)

set -uo pipefail

REPO_ROOT="${REPO_ROOT:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
cd "${REPO_ROOT}"

# BASELINE_DIR: directory containing baseline *.txt files (story-0062-0001).
# Default kept at "audits" for backward compat; override via env var to
# point at "governance/baselines" after the v4 physical migration.
BASELINE_DIR="${BASELINE_DIR:-audits}"

# Baseline files to audit (story-ID format; frozen after EPIC-0059 cutoff).
# Note: ${BASELINE_DIR}/pr-evidence-baseline.txt uses PR numbers (not story IDs) and
# is excluded here — it is governed by audit-pr-evidence.sh independently.
BASELINE_FILES=(
    "${BASELINE_DIR}/execution-integrity-baseline.txt"
    "${BASELINE_DIR}/rule-26-baseline.txt"
    "${BASELINE_DIR}/task-hierarchy-baseline.txt"
)

# The canonical cutoff marker file: created by story-0059-0011 to record the SHA
CUTOFF_SHA_FILE="${BASELINE_DIR}/baseline-cutoff.sha"

# Story ID pattern
STORY_ID_PATTERN="^story-[0-9]{4}-[0-9]{4}"

# Exemption marker
EXEMPTION_MARKER="baseline-correction:"

# Output mode
OUTPUT_MODE="text"
SINGLE_FILE=""

# ─────────────────────────────────────────────────────────────────────────────
info()    { echo "INFO  $*"; }
warn()    { echo "WARN  $*" >&2; }
error()   { echo "ERROR $*" >&2; }
ok_msg()  { echo "OK    $*"; }

# ─────────────────────────────────────────────────────────────────────────────
# Find the EPIC-0059 cutoff commit SHA
# ─────────────────────────────────────────────────────────────────────────────
find_cutoff_sha() {
    # 1. Env override (smoke-tests, CI debugging)
    if [[ -n "${BASELINE_CUTOFF_SHA:-}" ]]; then
        echo "${BASELINE_CUTOFF_SHA}"
        return 0
    fi

    # 2. Canonical file audits/baseline-cutoff.sha
    if [[ -f "${CUTOFF_SHA_FILE}" ]]; then
        local sha
        sha=$(cat "${CUTOFF_SHA_FILE}" | tr -d '[:space:]')
        if [[ -n "${sha}" ]]; then
            echo "${sha}"
            return 0
        fi
    fi

    # 3. Fallback: SHA of commit that first created ${BASELINE_DIR}/rule-26-baseline.txt
    local sha
    sha=$(git log --all --diff-filter=A --format="%H" \
        -- "${BASELINE_DIR}/rule-26-baseline.txt" 2>/dev/null \
        | tail -1 || true)
    if [[ -n "${sha}" ]]; then
        echo "${sha}"
        return 0
    fi

    # 4. Fallback: look for story-0059-0011 commit
    sha=$(git log --all --format="%H %s" 2>/dev/null \
        | grep "story-0059-0011" \
        | head -1 \
        | awk '{print $1}' || true)
    if [[ -n "${sha}" ]]; then
        echo "${sha}"
        return 0
    fi

    echo ""
}

# ─────────────────────────────────────────────────────────────────────────────
# Get story IDs added to a file AFTER the cutoff commit
# ─────────────────────────────────────────────────────────────────────────────
get_new_stories_since_cutoff() {
    local file="$1"
    local cutoff_sha="$2"

    # Content at cutoff
    local cutoff_content=""
    cutoff_content=$(git show "${cutoff_sha}:${file}" 2>/dev/null || true)

    # Current content
    local current_content=""
    if [[ -f "${file}" ]]; then
        current_content=$(cat "${file}")
    fi

    if [[ -z "${current_content}" ]]; then
        return 0
    fi

    # Extract story IDs from both versions (first token only, e.g. "story-0054-0001")
    local cutoff_ids=""
    if [[ -n "${cutoff_content}" ]]; then
        cutoff_ids=$(echo "${cutoff_content}" \
            | grep -E "${STORY_ID_PATTERN}" \
            | awk '{print $1}' | sort -u || true)
    fi

    local current_ids
    current_ids=$(echo "${current_content}" \
        | grep -E "${STORY_ID_PATTERN}" \
        | awk '{print $1}' | sort -u || true)

    if [[ -z "${current_ids}" ]]; then
        return 0
    fi

    # IDs present now but absent at cutoff
    if [[ -n "${cutoff_ids}" ]]; then
        comm -23 \
            <(echo "${current_ids}") \
            <(echo "${cutoff_ids}") 2>/dev/null || true
    else
        # File didn't exist at cutoff — all current IDs are "new"
        echo "${current_ids}"
    fi
}

# ─────────────────────────────────────────────────────────────────────────────
# Check if a story line has an exemption marker
# ─────────────────────────────────────────────────────────────────────────────
has_exemption() {
    local story_id="$1"
    local file="$2"

    local lineno
    lineno=$(grep -n "^${story_id}" "${file}" 2>/dev/null | cut -d: -f1 | head -1)
    [[ -z "${lineno}" ]] && return 1

    # Check preceding line
    if [[ ${lineno} -gt 1 ]]; then
        local prev_line
        prev_line=$(sed -n "$((lineno - 1))p" "${file}" 2>/dev/null || true)
        if echo "${prev_line}" | grep -q "${EXEMPTION_MARKER}"; then
            return 0
        fi
    fi

    # Check same line (inline marker)
    local same_line
    same_line=$(sed -n "${lineno}p" "${file}" 2>/dev/null || true)
    if echo "${same_line}" | grep -q "${EXEMPTION_MARKER}"; then
        return 0
    fi

    return 1
}

# ─────────────────────────────────────────────────────────────────────────────
# Validate baseline file format
# ─────────────────────────────────────────────────────────────────────────────
validate_format() {
    local file="$1"
    local broken=0

    while IFS= read -r line; do
        [[ -z "${line}" ]] && continue
        [[ "${line}" =~ ^# ]] && continue
        [[ "${line}" =~ "${EXEMPTION_MARKER}" ]] && continue

        if ! echo "${line}" | grep -qE "${STORY_ID_PATTERN}"; then
            error "BASELINE_CORRUPT: invalid line in ${file}: '${line}'"
            broken=1
        fi
    done < "${file}"

    return ${broken}
}

# ─────────────────────────────────────────────────────────────────────────────
# Audit a single baseline file
# ─────────────────────────────────────────────────────────────────────────────
audit_file() {
    local file="$1"
    local cutoff_sha="$2"
    local file_violations=0

    if [[ ! -f "${file}" ]]; then
        warn "Baseline file not found (skipping): ${file}"
        return 0
    fi

    # Format check
    if ! validate_format "${file}"; then
        return 3
    fi

    # Get new story IDs since cutoff
    local new_ids
    new_ids=$(get_new_stories_since_cutoff "${file}" "${cutoff_sha}")

    if [[ -z "${new_ids}" ]]; then
        ok_msg "${file}: immutable (no new entries since cutoff)"
        return 0
    fi

    while IFS= read -r story_id; do
        [[ -z "${story_id}" ]] && continue

        if has_exemption "${story_id}" "${file}"; then
            warn "${file}: ${story_id} added after cutoff — has exemption marker (allowed)"
        else
            error "BASELINE_IMMUTABILITY_VIOLATION: ${story_id} added to ${file} after EPIC-0059 cutoff"
            file_violations=$((file_violations + 1))
        fi
    done <<< "${new_ids}"

    if [[ ${file_violations} -gt 0 ]]; then
        return 1
    fi

    ok_msg "${file}: immutable (${new_ids} new exempted entries)"
    return 0
}

# ─────────────────────────────────────────────────────────────────────────────
# --self-check
# ─────────────────────────────────────────────────────────────────────────────
self_check() {
    local broken=0

    if ! command -v git >/dev/null 2>&1; then
        echo "OPERATIONAL_ERROR: git not found on PATH" >&2
        broken=1
    fi

    for f in "${BASELINE_DIR}/execution-integrity-baseline.txt" "${BASELINE_DIR}/rule-26-baseline.txt"; do
        if [[ ! -f "${f}" ]]; then
            echo "OPERATIONAL_ERROR: baseline file missing: ${f}" >&2
            broken=1
        fi
    done

    local sha
    sha=$(find_cutoff_sha)
    if [[ -z "${sha}" ]]; then
        echo "ENFORCEMENT_BROKEN: cannot determine EPIC-0059 cutoff commit" >&2
        echo "  Hint: create ${BASELINE_DIR}/baseline-cutoff.sha with the story-0059-0011 commit SHA" >&2
        broken=1
    else
        echo "self-check: cutoff SHA = ${sha}"
    fi

    if [[ ${broken} -ne 0 ]]; then
        exit 2
    fi

    echo "self-check: OK"
    exit 0
}

# ─────────────────────────────────────────────────────────────────────────────
# JSON helper
# ─────────────────────────────────────────────────────────────────────────────
emit_json() {
    local status="$1"
    local violations_json="$2"
    local cutoff="$3"
    printf '{"status":"%s","cutoffSha":"%s","violations":%s}\n' \
        "${status}" "${cutoff}" "${violations_json}"
}

# ─────────────────────────────────────────────────────────────────────────────
# Argument parsing
# ─────────────────────────────────────────────────────────────────────────────
usage_error() {
    cat >&2 <<EOF
Usage: $(basename "$0") [--self-check] [--json] [--file <path>]

  --self-check    Verify infrastructure is wired
  --json          Emit single-line JSON envelope
  --file <path>   Audit only the specified file

Environment:
  BASELINE_CUTOFF_SHA=<sha>   Override auto-detected cutoff SHA
  REPO_ROOT=<path>            Override repository root

Exit codes:
  0  OK
  1  BASELINE_IMMUTABILITY_VIOLATION
  2  OPERATIONAL_ERROR
  3  BASELINE_CORRUPT
  4  ENFORCEMENT_BROKEN (cutoff not found)
EOF
    exit 2
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --self-check) self_check ;;
        --json)       OUTPUT_MODE="json"; shift ;;
        --file)
            if [[ $# -lt 2 || -z "${2:-}" ]]; then
                echo "error: --file requires a path argument" >&2
                usage_error
            fi
            SINGLE_FILE="$2"
            shift 2
            ;;
        *) echo "error: unknown argument: $1" >&2; usage_error ;;
    esac
done

# ─────────────────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────────────────
main() {
    local cutoff_sha
    cutoff_sha=$(find_cutoff_sha)

    if [[ -z "${cutoff_sha}" ]]; then
        error "ENFORCEMENT_BROKEN: cannot determine EPIC-0059 cutoff commit"
        error "Create audits/baseline-cutoff.sha with the story-0059-0011 merge SHA, or"
        error "set BASELINE_CUTOFF_SHA=<sha> to override."
        if [[ "${OUTPUT_MODE}" == "json" ]]; then
            emit_json "ENFORCEMENT_BROKEN" "[]" ""
        fi
        exit 4
    fi

    # Verify the cutoff SHA is actually reachable in the local repository.
    # Shallow clones (e.g. actions/checkout default fetch-depth: 1) leave the
    # cutoff SHA unreachable, which would silently make `git show <sha>:file`
    # return empty content and report ALL current entries as false-positive
    # violations. Fail loud with ENFORCEMENT_BROKEN instead.
    if ! git cat-file -e "${cutoff_sha}^{commit}" 2>/dev/null; then
        error "ENFORCEMENT_BROKEN: cutoff SHA ${cutoff_sha} not reachable in local repo"
        error "This usually means the workspace is a shallow clone."
        error "In CI, set 'fetch-depth: 0' on actions/checkout. Locally, run 'git fetch --unshallow'."
        if [[ "${OUTPUT_MODE}" == "json" ]]; then
            emit_json "ENFORCEMENT_BROKEN" "[]" "${cutoff_sha}"
        fi
        exit 4
    fi

    info "Cutoff SHA: ${cutoff_sha}"
    info "Auditing baseline immutability..."

    local files_to_audit=()
    if [[ -n "${SINGLE_FILE}" ]]; then
        files_to_audit=("${SINGLE_FILE}")
    else
        files_to_audit=("${BASELINE_FILES[@]}")
    fi

    local overall_exit=0
    local all_violation_files=()

    for file in "${files_to_audit[@]}"; do
        local ec=0
        audit_file "${file}" "${cutoff_sha}" || ec=$?
        case ${ec} in
            0) ;;
            1) overall_exit=1; all_violation_files+=("${file}") ;;
            2) exit 2 ;;
            3) overall_exit=3 ;;
            4) overall_exit=4 ;;
        esac
    done

    if [[ "${OUTPUT_MODE}" == "json" ]]; then
        local status="OK"
        [[ ${overall_exit} -eq 1 ]] && status="BASELINE_IMMUTABILITY_VIOLATION"
        [[ ${overall_exit} -eq 3 ]] && status="BASELINE_CORRUPT"
        [[ ${overall_exit} -eq 4 ]] && status="ENFORCEMENT_BROKEN"

        local vj="["
        local first=1
        for v in "${all_violation_files[@]}"; do
            [[ ${first} -eq 0 ]] && vj+=","
            vj+="\"${v}\""
            first=0
        done
        vj+="]"

        emit_json "${status}" "${vj}" "${cutoff_sha}"
    fi

    if [[ ${overall_exit} -eq 0 ]]; then
        info "All baseline files are immutable after EPIC-0059 cutoff — OK"
    fi

    exit ${overall_exit}
}

main "$@"
