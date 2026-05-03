#!/usr/bin/env bash
# Layer:      2 (CI script / operational utility)
# Purpose:    Retroactively seed ai/memory/ with summaries for concluded epics
# Usage:      scripts/retro-seed-memory.sh [--from XXXX] [--to YYYY] [--dry-run] [--continue-on-error]
# Exit codes: 0=OK, 1=SEED_VIOLATION, 2=OPERATIONAL_ERROR, 3=BASELINE_CORRUPT
# Part of:    EPIC-0075 (AI Memory Layer) — story-0075-0006

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MEMORY_DIR="${REPO_ROOT}/ai/memory"
EPICS_DIR="${REPO_ROOT}/ai/epics"
INDEX_FILE="${MEMORY_DIR}/_index.yaml"
RUBRIC_FILE="${MEMORY_DIR}/_retro-seed-rubric.md"

# Defaults
FROM_EPIC=""
TO_EPIC=""
DRY_RUN=false
CONTINUE_ON_ERROR=false
VIOLATIONS=0
PROCESSED=0
SKIPPED=0

# ── parse args ────────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --from)       FROM_EPIC="$2"; shift 2 ;;
    --to)         TO_EPIC="$2"; shift 2 ;;
    --dry-run)    DRY_RUN=true; shift ;;
    --continue-on-error) CONTINUE_ON_ERROR=true; shift ;;
    --self-check)
      command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
      command -v find >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: find required" >&2; exit 2; }
      [[ -d "${MEMORY_DIR}" ]] || { echo "OPERATIONAL_ERROR: ai/memory/ directory missing" >&2; exit 2; }
      [[ -f "${RUBRIC_FILE}" ]] || { echo "OPERATIONAL_ERROR: _retro-seed-rubric.md missing" >&2; exit 2; }
      echo "self-check OK"
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      echo "Usage: $0 [--from XXXX] [--to YYYY] [--dry-run] [--continue-on-error]" >&2
      exit 2
      ;;
  esac
done

# ── validate prerequisites ────────────────────────────────────────────────────
if [[ ! -d "${MEMORY_DIR}" ]]; then
  echo "OPERATIONAL_ERROR: ${MEMORY_DIR} does not exist" >&2
  exit 2
fi

if [[ ! -f "${RUBRIC_FILE}" ]]; then
  echo "OPERATIONAL_ERROR: ${RUBRIC_FILE} missing — run story-0075-0006 first" >&2
  exit 2
fi

# ── collect concluded epics ───────────────────────────────────────────────────
echo "Scanning ${EPICS_DIR} for concluded epics..."

declare -a TARGET_EPICS=()

while IFS= read -r -d '' epic_file; do
  epic_dir="$(dirname "${epic_file}")"
  dir_name="$(basename "${epic_dir}")"

  # Extract epic ID (4-digit number after epic-)
  if [[ "${dir_name}" =~ ^epic-([0-9]{4})- ]]; then
    epic_num="${BASH_REMATCH[1]}"
  else
    continue
  fi

  # Apply --from / --to filters
  if [[ -n "${FROM_EPIC}" ]] && [[ "${epic_num}" -lt "${FROM_EPIC}" ]]; then
    continue
  fi
  if [[ -n "${TO_EPIC}" ]] && [[ "${epic_num}" -gt "${TO_EPIC}" ]]; then
    continue
  fi

  # Check status
  if grep -q "^\*\*Status:\*\* Concluída" "${epic_file}" 2>/dev/null; then
    TARGET_EPICS+=("${epic_num}:${epic_file}")
  fi
done < <(find "${EPICS_DIR}" -name "epic-[0-9][0-9][0-9][0-9].md" -print0 | sort -z)

echo "Found ${#TARGET_EPICS[@]} concluded epic(s) in range"

# ── process each epic ─────────────────────────────────────────────────────────
for entry in "${TARGET_EPICS[@]}"; do
  epic_num="${entry%%:*}"
  epic_file="${entry#*:}"
  summary_file="${MEMORY_DIR}/epic-${epic_num}-summary.md"

  if [[ -f "${summary_file}" ]]; then
    echo "[SKIP] epic-${epic_num}: summary already exists at ${summary_file}"
    ((SKIPPED++)) || true
    continue
  fi

  echo "[PROCESS] epic-${epic_num}: ${epic_file}"
  ((PROCESSED++)) || true

  # Quality check per rubric
  violation=false

  # Rubric check 1: epic file exists and is readable
  if [[ ! -r "${epic_file}" ]]; then
    echo "  [WARN] epic-${epic_num}: epic file not readable"
    violation=true
  fi

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "  [DRY-RUN] Would create ${summary_file}"
    continue
  fi

  if [[ "${violation}" == "true" ]]; then
    ((VIOLATIONS++)) || true
    if [[ "${CONTINUE_ON_ERROR}" == "false" ]]; then
      echo "SEED_VIOLATION: epic-${epic_num} failed rubric check" >&2
      exit 1
    fi
  fi
done

# ── report ────────────────────────────────────────────────────────────────────
echo ""
echo "Retro-seed complete:"
echo "  Processed : ${PROCESSED}"
echo "  Skipped   : ${SKIPPED}"
echo "  Violations: ${VIOLATIONS}"

if [[ "${VIOLATIONS}" -gt 0 ]]; then
  echo "SEED_VIOLATION: ${VIOLATIONS} epic(s) failed rubric check" >&2
  exit 1
fi

# ── validate _index.yaml consistency ─────────────────────────────────────────
if [[ -f "${INDEX_FILE}" ]]; then
  missing=0
  while IFS= read -r summary_file; do
    basename_summary="$(basename "${summary_file}")"
    if ! grep -q "summary-path: ${basename_summary}" "${INDEX_FILE}" 2>/dev/null; then
      echo "[WARN] ${basename_summary} exists but has no entry in _index.yaml"
      ((missing++)) || true
    fi
  done < <(find "${MEMORY_DIR}" -name "epic-*-summary.md" | sort)

  if [[ "${missing}" -gt 0 ]]; then
    echo "SEED_VIOLATION: ${missing} summary file(s) missing from _index.yaml" >&2
    exit 1
  fi
fi

echo "OK: retro-seed validation passed"
exit 0
