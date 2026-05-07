#!/usr/bin/env bash
# Layer:      2 (CI script — called by audit-context-budget.sh and directly)
# Rule:       EPIC-0078 story-0078-0001 (Context Budget Baseline Tooling)
# Exit codes: 0=OK  2=OPERATIONAL_ERROR
# Latency:    < 1s
# Tokenization heuristic: bytes/4 (±10%, consistent relative measurement — see story §8)
set -euo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
REPO_ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"

log_info()  { echo "[measure-context-budget] INFO  $*" >&2; }
log_error() { echo "[measure-context-budget] ERROR $*" >&2; }

# --self-check: verify prerequisites
case "${1:-}" in
  --self-check)
    command -v wc  >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: wc required"   >&2; exit 2; }
    command -v find >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: find required" >&2; exit 2; }
    command -v jq  >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required"   >&2; exit 2; }
    exit 0
    ;;
  -h|--help)
    echo "Usage: ${SCRIPT_NAME} [--root <path>] [--format json|text]"
    echo "  --root    Base directory to scan (default: .claude)"
    echo "  --format  Output format: json (default) or text"
    exit 0
    ;;
esac

# Parse arguments
ROOT_ARG=".claude"
FORMAT_ARG="json"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --root)
      ROOT_ARG="${2:-}"
      shift 2
      ;;
    --format)
      FORMAT_ARG="${2:-}"
      shift 2
      ;;
    *)
      log_error "Unknown argument: $1"
      exit 2
      ;;
  esac
done

# Validate format
if [[ "${FORMAT_ARG}" != "json" && "${FORMAT_ARG}" != "text" ]]; then
  log_error "OPERATIONAL_ERROR: --format must be 'json' or 'text', got: ${FORMAT_ARG}"
  exit 2
fi

# Validate prerequisites
command -v wc   >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: wc not found on PATH";   exit 2; }
command -v find >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: find not found on PATH"; exit 2; }

# Resolve and validate --root (CWE-22 path traversal guard)
CANDIDATE_ROOT="${ROOT_ARG}"
if [[ "${CANDIDATE_ROOT}" != /* ]]; then
  CANDIDATE_ROOT="${REPO_ROOT}/${CANDIDATE_ROOT}"
fi

if command -v realpath >/dev/null 2>&1; then
  RESOLVED_ROOT="$(realpath "${CANDIDATE_ROOT}" 2>/dev/null || true)"
else
  RESOLVED_ROOT="$(cd "${CANDIDATE_ROOT}" 2>/dev/null && pwd || true)"
fi

if [[ -z "${RESOLVED_ROOT}" ]]; then
  log_error "OPERATIONAL_ERROR: --root path does not exist: ${ROOT_ARG}"
  exit 2
fi

# Reject path traversal: resolved path must be within repo root
if [[ "${RESOLVED_ROOT}" != "${REPO_ROOT}"* ]]; then
  log_error "OPERATIONAL_ERROR: invalid --root (path traversal detected): ${ROOT_ARG}"
  exit 2
fi

# Compute alwaysLoaded tokens (CLAUDE.md root + rules/*.md)
ALWAYS_BYTES=0

CLAUDE_MD="${REPO_ROOT}/CLAUDE.md"
if [[ -f "${CLAUDE_MD}" ]]; then
  ALWAYS_BYTES=$(( ALWAYS_BYTES + $(wc -c < "${CLAUDE_MD}") ))
fi

RULES_DIR="${RESOLVED_ROOT}/rules"
if [[ -d "${RULES_DIR}" ]]; then
  while IFS= read -r rule_file; do
    ALWAYS_BYTES=$(( ALWAYS_BYTES + $(wc -c < "${rule_file}") ))
  done < <(find "${RULES_DIR}" -maxdepth 1 -name "*.md" -type f 2>/dev/null || true)
fi

ALWAYS_TOKENS=$(( ALWAYS_BYTES / 4 ))

# Compute perSkill tokens (each SKILL.md under .claude/skills/)
SKILLS_DIR="${RESOLVED_ROOT}/skills"
PERSKILL_JSON="{}"

if [[ -d "${SKILLS_DIR}" ]]; then
  while IFS= read -r skill_file; do
    skill_name="$(basename "$(dirname "${skill_file}")")"
    skill_bytes="$(wc -c < "${skill_file}")"
    skill_tokens=$(( skill_bytes / 4 ))
    if [[ "${FORMAT_ARG}" == "json" ]]; then
      PERSKILL_JSON="$(echo "${PERSKILL_JSON}" | jq --arg k "${skill_name}" --argjson v "${skill_tokens}" '. + {($k): $v}')"
    else
      echo "  skill ${skill_name}: ${skill_tokens}"
    fi
  done < <(find "${SKILLS_DIR}" -name "SKILL.md" -type f 2>/dev/null | sort || true)
fi

MEASURED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u +%Y-%m-%dT%H:%M:%SZ)"

if [[ "${FORMAT_ARG}" == "text" ]]; then
  echo "alwaysLoaded: ${ALWAYS_TOKENS}"
  echo "measuredAt: ${MEASURED_AT}"
else
  # Build JSON output using jq (required for valid JSON construction)
  jq -n \
    --argjson alwaysLoaded "${ALWAYS_TOKENS}" \
    --argjson perSkill "${PERSKILL_JSON}" \
    --arg measuredAt "${MEASURED_AT}" \
    '{alwaysLoaded: $alwaysLoaded, perSkill: $perSkill, measuredAt: $measuredAt}'
fi
