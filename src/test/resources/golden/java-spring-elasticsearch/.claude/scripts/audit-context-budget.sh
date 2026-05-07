#!/usr/bin/env bash
# Layer:      2 (CI script — fires on PR open/sync)
# Rule:       EPIC-0078 RULE-003 (Context Budget Audit — Camada 4)
# Exit codes: 0=OK  1=CONTEXT_BUDGET_VIOLATION  2=OPERATIONAL_ERROR  3=BASELINE_CORRUPT
# Latency:    < 2s wall-clock
set -euo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"

BASELINE_PATH="${BASELINE_PATH:-${REPO_ROOT}/governance/baselines/context-budget.json}"
# story-0078-0016: default switched from advisory to hard-fail
ADVISORY="${ADVISORY:-false}"

log_info()  { echo "[audit-context-budget] INFO  $*" >&2; }
log_warn()  { echo "[audit-context-budget] WARN  $*" >&2; }
log_error() { echo "[audit-context-budget] ERROR $*" >&2; }

# --self-check: verify prerequisites
case "${1:-}" in
  --self-check)
    command -v jq   >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required"   >&2; exit 2; }
    command -v wc   >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: wc required"   >&2; exit 2; }
    command -v find >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: find required" >&2; exit 2; }
    [[ -f "${BASELINE_PATH}" ]] || { echo "OPERATIONAL_ERROR: baseline not found: ${BASELINE_PATH}" >&2; exit 2; }
    exit 0
    ;;
  --advisory)
    ADVISORY="true"
    shift
    ;;
  --hard)
    ADVISORY="false"
    shift
    ;;
  -h|--help)
    echo "Usage: ${SCRIPT_NAME} [--advisory|--hard] [--self-check]"
    echo "  --advisory  Warn on violation but exit 0 (default)"
    echo "  --hard      Exit 1 on violation (story-0078-0016 activates this)"
    exit 0
    ;;
esac

# Validate prerequisites
command -v jq   >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: jq not found on PATH";   exit 2; }
command -v wc   >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: wc not found on PATH";   exit 2; }
command -v find >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: find not found on PATH"; exit 2; }

# Validate and load baseline
if [[ ! -f "${BASELINE_PATH}" ]]; then
  log_info "No baseline found at ${BASELINE_PATH} — skipping audit (exit 0)"
  exit 0
fi

if [[ -L "${BASELINE_PATH}" ]]; then
  log_error "OPERATIONAL_ERROR: symlink rejected — baseline must be a regular file"
  exit 2
fi

# Validate baseline JSON structure
if ! jq -e '.alwaysLoaded and .measuredAt and .ref' "${BASELINE_PATH}" >/dev/null 2>&1; then
  log_error "BASELINE_CORRUPT: ${BASELINE_PATH} missing required fields (alwaysLoaded, measuredAt, ref)"
  exit 3
fi

if ! jq -e 'type == "object"' "${BASELINE_PATH}" >/dev/null 2>&1; then
  log_error "BASELINE_CORRUPT: ${BASELINE_PATH} is not valid JSON"
  exit 3
fi

# Extract baseline values
BASELINE_ALWAYS=$(jq -r '.alwaysLoaded' "${BASELINE_PATH}")
TOLERANCE_PCT=$(jq -r '.tolerancePct // 10' "${BASELINE_PATH}")
LIMIT=$(jq -r '.limit // .alwaysLoaded' "${BASELINE_PATH}")

if ! [[ "${BASELINE_ALWAYS}" =~ ^[0-9]+$ ]]; then
  log_error "BASELINE_CORRUPT: alwaysLoaded must be a non-negative integer"
  exit 3
fi

# Run measurement
MEASURE_SCRIPT="${SCRIPT_DIR}/measure-context-budget.sh"
if [[ ! -f "${MEASURE_SCRIPT}" ]]; then
  log_error "OPERATIONAL_ERROR: measure-context-budget.sh not found at ${MEASURE_SCRIPT}"
  exit 2
fi

MEASUREMENT="$(bash "${MEASURE_SCRIPT}" --root "${REPO_ROOT}/.claude" --format json 2>/dev/null)"
if [[ -z "${MEASUREMENT}" ]]; then
  log_error "OPERATIONAL_ERROR: measure-context-budget.sh produced no output"
  exit 2
fi

CURRENT_ALWAYS=$(echo "${MEASUREMENT}" | jq -r '.alwaysLoaded // 0')

# Compute tolerance ceiling
TOLERANCE_CEILING=$(( LIMIT + (LIMIT * TOLERANCE_PCT / 100) ))

log_info "Baseline alwaysLoaded=${BASELINE_ALWAYS} limit=${LIMIT} tolerance=${TOLERANCE_PCT}%"
log_info "Current  alwaysLoaded=${CURRENT_ALWAYS} ceiling=${TOLERANCE_CEILING}"

if [[ "${CURRENT_ALWAYS}" -gt "${TOLERANCE_CEILING}" ]]; then
  if [[ "${ADVISORY}" == "true" ]]; then
    log_warn "CONTEXT_BUDGET_VIOLATION: current=${CURRENT_ALWAYS} exceeds ceiling=${TOLERANCE_CEILING} (advisory — exit 0)"
    exit 0
  else
    log_error "CONTEXT_BUDGET_VIOLATION: current=${CURRENT_ALWAYS} exceeds ceiling=${TOLERANCE_CEILING}"
    exit 1
  fi
fi

log_info "Context budget within limits — exit 0 (OK)"
exit 0
