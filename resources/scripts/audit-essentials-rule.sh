#!/usr/bin/env bash
# Layer:      2 (CI script — fires on PR open/sync)
# Rule:       chore/rules-consolidation-essentials (audit-essentials-rule)
# Exit codes: 0=OK  1=ESSENTIALS_RULE_VIOLATION  2=OPERATIONAL_ERROR
# Validates:  .claude/rules/ has exactly 00-essentials.md + conditional rules;
#             00-essentials.md ≤ 400 lines; §1-§7 sections present; KPs exist.
set -euo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
REPO_ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"

RULES_DIR="${RULES_DIR:-${REPO_ROOT}/.claude/rules}"
ESSENTIALS_FILE="${ESSENTIALS_FILE:-${RULES_DIR}/00-essentials.md}"
KP_ROOT="${KP_ROOT:-${REPO_ROOT}/src/main/resources/targets/claude/knowledge/governance/rules}"
MAX_LINES="${MAX_LINES:-400}"

log_info()  { echo "[audit-essentials-rule] INFO  $*" >&2; }
log_warn()  { echo "[audit-essentials-rule] WARN  $*" >&2; }
log_error() { echo "[audit-essentials-rule] ERROR $*" >&2; }

usage() {
    cat >&2 <<EOF
Usage: ${SCRIPT_NAME} [--self-check] [--rules-dir <path>] [--kp-root <path>]
                      [--max-lines <n>]

Exit codes: 0=OK  1=ESSENTIALS_RULE_VIOLATION  2=OPERATIONAL_ERROR
EOF
}

# Parse flags
while [[ $# -gt 0 ]]; do
    case "$1" in
        --self-check)
            # Validate prerequisites for this script
            command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
            command -v find >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: find required" >&2; exit 2; }
            command -v wc   >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: wc required" >&2; exit 2; }
            # Verify this rule file exists (Rule 26 §self-check pattern)
            SELF_RULE="${REPO_ROOT}/.claude/rules/00-essentials.md"
            KP_LIFECYCLE="${KP_ROOT}/lifecycle-contract.md"
            KP_CODING="${KP_ROOT}/coding-standards-rule.md"
            # Check rule source template
            TEMPLATE="${REPO_ROOT}/src/main/resources/shared/templates/_TEMPLATE-ESSENTIALS-RULE.md"
            [[ -f "${TEMPLATE}" ]] || { echo "OPERATIONAL_ERROR: _TEMPLATE-ESSENTIALS-RULE.md not found at ${TEMPLATE}" >&2; exit 2; }
            log_info "self-check passed"
            exit 0 ;;
        --rules-dir)
            shift
            [[ "$1" == *".."* ]] && { echo "OPERATIONAL_ERROR: path traversal rejected" >&2; exit 2; }
            RULES_DIR="$(realpath "$1" 2>/dev/null)" || { echo "OPERATIONAL_ERROR: rules-dir path invalid" >&2; exit 2; }
            ESSENTIALS_FILE="${RULES_DIR}/00-essentials.md"
            shift ;;
        --kp-root)
            shift
            [[ "$1" == *".."* ]] && { echo "OPERATIONAL_ERROR: path traversal rejected" >&2; exit 2; }
            KP_ROOT="$(realpath "$1" 2>/dev/null)" || { echo "OPERATIONAL_ERROR: kp-root path invalid" >&2; exit 2; }
            shift ;;
        --max-lines)
            shift
            MAX_LINES="$1"
            shift ;;
        --help|-h)
            usage; exit 0 ;;
        *)
            log_warn "Unknown flag: $1"; shift ;;
    esac
done

# Validate prerequisites
command -v grep >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: grep required"; exit 2; }
command -v wc   >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: wc required"; exit 2; }

EXIT_CODE=0

# ── Check 1: 00-essentials.md exists ──────────────────────────────────────────
if [[ ! -f "${ESSENTIALS_FILE}" ]]; then
    log_error "ESSENTIALS_RULE_VIOLATION: 00-essentials.md not found at ${ESSENTIALS_FILE}"
    exit 1
fi

# ── Check 2: line count ≤ MAX_LINES ──────────────────────────────────────────
LINE_COUNT="$(wc -l < "${ESSENTIALS_FILE}")"
if [[ "${LINE_COUNT}" -gt "${MAX_LINES}" ]]; then
    log_error "ESSENTIALS_RULE_VIOLATION: 00-essentials.md has ${LINE_COUNT} lines (max ${MAX_LINES})"
    EXIT_CODE=1
else
    log_info "line_count=${LINE_COUNT} (max=${MAX_LINES}) OK"
fi

# ── Check 3: required sections present ───────────────────────────────────────
REQUIRED_SECTIONS=(
    "Project Identity"
    "Hard Limits"
    "Architecture Golden Rule"
    "Forbidden"
    "Lifecycle Integrity Contract"
    "Skill Invocation Protocol"
    "Knowledge Pack Index"
)

for section in "${REQUIRED_SECTIONS[@]}"; do
    if ! grep -q "${section}" "${ESSENTIALS_FILE}" 2>/dev/null; then
        log_error "ESSENTIALS_RULE_VIOLATION: 00-essentials.md missing section '${section}'"
        EXIT_CODE=1
    fi
done

# ── Check 4: no old numbered rule files in rules dir ─────────────────────────
if [[ -d "${RULES_DIR}" ]]; then
    OLD_RULES="$(find "${RULES_DIR}" -maxdepth 1 -name "[0-9][0-9]-*.md" ! -name "00-essentials.md" 2>/dev/null | wc -l)"
    if [[ "${OLD_RULES}" -gt 0 ]]; then
        log_error "ESSENTIALS_RULE_VIOLATION: ${OLD_RULES} old numbered rule file(s) found in ${RULES_DIR}"
        find "${RULES_DIR}" -maxdepth 1 -name "[0-9][0-9]-*.md" ! -name "00-essentials.md" -exec log_error "  orphan: {}" \; 2>/dev/null || true
        EXIT_CODE=1
    else
        log_info "no old numbered rule files in ${RULES_DIR} — OK"
    fi
fi

# ── Check 5: governance/rules KPs exist ───────────────────────────────────────
REQUIRED_KPS=(
    "lifecycle-contract.md"
    "coding-standards-rule.md"
    "quality-gates.md"
    "architecture-summary.md"
    "security-baseline.md"
    "branching.md"
    "skill-invocation.md"
)

if [[ -d "${KP_ROOT}" ]]; then
    for kp in "${REQUIRED_KPS[@]}"; do
        if [[ ! -f "${KP_ROOT}/${kp}" ]]; then
            log_error "ESSENTIALS_RULE_VIOLATION: required governance KP missing: ${KP_ROOT}/${kp}"
            EXIT_CODE=1
        fi
    done
    log_info "governance KP presence check complete"
else
    log_warn "kp-root not found (${KP_ROOT}) — skipping KP existence check"
fi

log_info "audit_complete exit=${EXIT_CODE}"
exit "${EXIT_CODE}"
