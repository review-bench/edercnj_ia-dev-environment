#!/usr/bin/env bash
# Layer:      2 (CI script — fires on PR open/sync)
# Rule:       EPIC-0078 RULE-002 (KP Orphan Detector)
# Exit codes: 0=OK  1=KP_ORPHAN  2=OPERATIONAL_ERROR  3=BASELINE_CORRUPT  4=SECURITY_KP_MISSING  5=KP_READ_MALFORMED
# Latency:    < 3s wall-clock
set -euo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
REPO_ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"

KNOWLEDGE_ROOT="${KNOWLEDGE_ROOT:-${REPO_ROOT}/src/main/resources/targets/claude/knowledge}"
SKILLS_ROOT="${SKILLS_ROOT:-${REPO_ROOT}/src/main/resources/targets/claude/skills}"
SECURITY_STRICT="${SECURITY_STRICT:-false}"
MODE="${MODE:-full}"
FORMAT="${FORMAT:-text}"
BASELINE_PATH="${BASELINE_PATH:-${REPO_ROOT}/governance/baselines/kp-references-baseline.txt}"

log_info()  { echo "[audit-kp-references] INFO  $*" >&2; }
log_warn()  { echo "[audit-kp-references] WARN  $*" >&2; }
log_error() { echo "[audit-kp-references] ERROR $*" >&2; }

usage() {
    cat >&2 <<EOF
Usage: ${SCRIPT_NAME} [--self-check] [--knowledge-root <path>] [--skills-root <path>]
                      [--security-strict] [--mode=full|kp-orphan|security-coverage]
                      [--format=text|json|sarif]

Exit codes: 0=OK  1=KP_ORPHAN  2=OPERATIONAL_ERROR  3=BASELINE_CORRUPT  4=SECURITY_KP_MISSING
EOF
}

# Parse flags
while [[ $# -gt 0 ]]; do
    case "$1" in
        --self-check)
            command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
            command -v find >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: find required" >&2; exit 2; }
            [[ -d "${KNOWLEDGE_ROOT}" ]] || { echo "OPERATIONAL_ERROR: knowledge-root not found: ${KNOWLEDGE_ROOT}" >&2; exit 2; }
            [[ -d "${SKILLS_ROOT}" ]] || { echo "OPERATIONAL_ERROR: skills-root not found: ${SKILLS_ROOT}" >&2; exit 2; }
            exit 0 ;;
        --knowledge-root)
            shift
            # Reject path traversal
            if [[ "$1" == *".."* ]]; then
                echo "OPERATIONAL_ERROR: knowledge-root path traversal rejected" >&2; exit 2
            fi
            KNOWLEDGE_ROOT="$(realpath "$1" 2>/dev/null)" || { echo "OPERATIONAL_ERROR: knowledge-root path invalid" >&2; exit 2; }
            shift ;;
        --skills-root)
            shift
            if [[ "$1" == *".."* ]]; then
                echo "OPERATIONAL_ERROR: skills-root path traversal rejected" >&2; exit 2
            fi
            SKILLS_ROOT="$(realpath "$1" 2>/dev/null)" || { echo "OPERATIONAL_ERROR: skills-root path invalid" >&2; exit 2; }
            shift ;;
        --security-strict)
            SECURITY_STRICT="true"; shift ;;
        --mode=*)
            MODE="${1#--mode=}"; shift ;;
        --format=*)
            FORMAT="${1#--format=}"; shift ;;
        --help|-h)
            usage; exit 0 ;;
        *)
            log_warn "Unknown flag: $1"; shift ;;
    esac
done

# Validate prerequisites
command -v grep >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: grep required"; exit 2; }
command -v find >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: find required"; exit 2; }

# Validate roots
if [[ "${KNOWLEDGE_ROOT}" == *".."* ]]; then
    log_error "OPERATIONAL_ERROR: knowledge-root path traversal rejected"; exit 2
fi
if [[ "${SKILLS_ROOT}" == *".."* ]]; then
    log_error "OPERATIONAL_ERROR: skills-root path traversal rejected"; exit 2
fi

[[ -d "${KNOWLEDGE_ROOT}" ]] || { log_info "knowledge-root not found — no KPs to check (exit 0)"; exit 0; }
[[ -d "${SKILLS_ROOT}" ]] || { log_error "OPERATIONAL_ERROR: skills-root not found: ${SKILLS_ROOT}"; exit 2; }

# Validate baseline if present
EXEMPTED_KPS=()
if [[ -f "${BASELINE_PATH}" ]]; then
    if [[ -L "${BASELINE_PATH}" ]]; then
        log_error "OPERATIONAL_ERROR: symlink rejected — baseline must be regular file"
        exit 2
    fi
    if grep -qvE '^(#|knowledge/|$)' "${BASELINE_PATH}" 2>/dev/null; then
        log_error "BASELINE_CORRUPT: ${BASELINE_PATH} contains invalid entries"
        exit 3
    fi
    while IFS= read -r line; do
        [[ "${line}" =~ ^# ]] && continue
        [[ -z "${line}" ]] && continue
        EXEMPTED_KPS+=("${line%% *}")
    done < "${BASELINE_PATH}"
fi

ORPHAN_COUNT=0
TOTAL_COUNT=0
EXIT_CODE=0

# ── KP Orphan check ──────────────────────────────────────────────────────────
if [[ "${MODE}" == "full" || "${MODE}" == "kp-orphan" ]]; then
    while IFS= read -r kp_abs; do
        # Reject symlinks (security — no symlink following)
        [[ -L "${kp_abs}" ]] && { log_warn "Symlink skipped: ${kp_abs}"; continue; }

        TOTAL_COUNT=$((TOTAL_COUNT + 1))
        kp_rel="${kp_abs#${KNOWLEDGE_ROOT}/}"
        kp_search="knowledge/${kp_rel}"

        # Check baseline exemption
        local_exempt=false
        for exempted in "${EXEMPTED_KPS[@]:-}"; do
            [[ "${exempted}" == "${kp_search}" ]] && { local_exempt=true; break; }
        done
        [[ "${local_exempt}" == "true" ]] && continue

        # Search for reference in any SKILL.md
        if ! grep -rl "${kp_search}" "${SKILLS_ROOT}" --include="SKILL.md" --include="*.md" -q 2>/dev/null; then
            log_error "KP_ORPHAN: ${kp_search} not referenced in any skill"
            ORPHAN_COUNT=$((ORPHAN_COUNT + 1))
            EXIT_CODE=1
        fi
    done < <(find "${KNOWLEDGE_ROOT}" -type f -not -lname '*' -name "*.md" | sort)
fi

# ── Security strict check ────────────────────────────────────────────────────
if [[ "${SECURITY_STRICT}" == "true" && "${MODE}" != "kp-orphan" ]]; then
    while IFS= read -r skill_file; do
        [[ -L "${skill_file}" ]] && continue
        skill_name="$(basename "$(dirname "${skill_file}")")"
        if grep -qiE "(auth|persistence|network|security)" "${skill_file}" 2>/dev/null; then
            if ! grep -q "Read knowledge/security/" "${skill_file}" 2>/dev/null; then
                log_error "SECURITY_KP_MISSING: skill ${skill_name} touches sensitive layer without security KP Read"
                EXIT_CODE=4
            fi
        fi
    done < <(find "${SKILLS_ROOT}" -type f -name "SKILL.md" | sort)
fi

log_info "kp_validation_complete total_kps=${TOTAL_COUNT} orphans=${ORPHAN_COUNT}"
exit "${EXIT_CODE}"
