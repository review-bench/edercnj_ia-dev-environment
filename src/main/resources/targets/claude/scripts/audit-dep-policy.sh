#!/usr/bin/env bash
# Layer:      2 (CI script — fires on PR open/sync)
# Rule:       Rule 32 §Dependency Policy Gate (EPIC-0074 story-0074-0004)
# Exit codes: 0=OK  1=DEPENDENCY_POLICY_VIOLATION  2=OPERATIONAL_ERROR  3=BASELINE_CORRUPT
# Latency:    < 5s wall-clock
set -euo pipefail

BASELINE_PATH="${BASELINE_PATH:-governance/baselines/dep-policy-baseline.txt}"
DEPENDENCY_POLICY_ENABLED="${DEPENDENCY_POLICY_ENABLED:-false}"
SKILLS_DIR="${SKILLS_DIR:-.claude/skills}"
TEMPLATES_DIR="${TEMPLATES_DIR:-.claude/templates}"
RULES_DIR="${RULES_DIR:-.claude/rules}"
EPIC_DIR="${EPIC_DIR:-ai/epics}"

log_info()  { echo "[audit-dep-policy] INFO  $*" >&2; }
log_warn()  { echo "[audit-dep-policy] WARN  $*" >&2; }
log_error() { echo "[audit-dep-policy] ERROR $*" >&2; }

# --self-check: verify prerequisites
case "${1:-}" in
  --self-check)
    command -v git >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: git required but not found on PATH" >&2; exit 2; }
    [[ -d "governance/baselines" ]] || { echo "OPERATIONAL_ERROR: governance/baselines directory not found" >&2; exit 2; }
    [[ -f "${RULES_DIR}/32-dependency-policy-gate.md" ]] || {
      echo "OPERATIONAL_ERROR: Rule 32 not found at ${RULES_DIR}/32-dependency-policy-gate.md" >&2; exit 2;
    }
    exit 0
    ;;
esac

# Validate prerequisites
command -v git >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: git required but not found on PATH"; exit 2; }

# Skip gate when dependency policy is not enabled
if [[ "${DEPENDENCY_POLICY_ENABLED}" != "true" ]]; then
  log_info "governance.dependency-policy capability not active — skipping dep-policy gate (exit 0)"
  exit 0
fi

# Validate baseline file integrity if present
if [[ -f "${BASELINE_PATH}" ]]; then
  if [[ -L "${BASELINE_PATH}" ]]; then
    log_error "OPERATIONAL_ERROR: symlink rejected — baseline must be a regular file: ${BASELINE_PATH}"
    exit 2
  fi
  if grep -qvE '^(#|story-[0-9]+-[0-9]+|$)' "${BASELINE_PATH}" 2>/dev/null; then
    log_error "BASELINE_CORRUPT: ${BASELINE_PATH} contains invalid entries"
    exit 3
  fi
fi

# Load baseline exemptions
BASELINE_STORIES=()
if [[ -f "${BASELINE_PATH}" ]]; then
  while IFS= read -r line; do
    [[ "${line}" =~ ^# ]] && continue
    [[ -z "${line}" ]] && continue
    BASELINE_STORIES+=("${line%% *}")
  done < "${BASELINE_PATH}"
fi

# --- Structural integrity checks ---

VIOLATIONS=()

# 1. x-dep-policy-validate skill must exist when capability is active
SKILL_PATH="${SKILLS_DIR}/x-dep-policy-validate/SKILL.md"
if [[ ! -f "${SKILL_PATH}" ]]; then
  VIOLATIONS+=("SKILL_MISSING: ${SKILL_PATH} not found — governance.dependency-policy capability requires this skill")
fi

# 2. Templates must exist
for tpl in "_TEMPLATE-DEP-POLICY-REPORT.md" "_TEMPLATE-DEP-POLICY-DECLARATION.md"; do
  if [[ ! -f "${TEMPLATES_DIR}/${tpl}" ]]; then
    VIOLATIONS+=("TEMPLATE_MISSING: ${TEMPLATES_DIR}/${tpl} not found")
  fi
done

# 3. Rule 32 must exist
if [[ ! -f "${RULES_DIR}/32-dependency-policy-gate.md" ]]; then
  VIOLATIONS+=("RULE_MISSING: ${RULES_DIR}/32-dependency-policy-gate.md not found")
fi

# --- Evidence artifact checks for merged story PRs ---
# When DEPENDENCY_POLICY_ENABLED=true, every story implementation must produce a
# dep-policy-validation-report artifact (Rule 27 Surface 13, story-0074-0005).

if command -v git >/dev/null 2>&1 && [[ -d "${EPIC_DIR}" ]]; then
  # Find story IDs from merged PRs targeting develop or epic/* since last tag
  MERGED_STORIES=()
  while IFS= read -r branch_name; do
    # Extract story ID from branch pattern feat/story-XXXX-YYYY
    if [[ "${branch_name}" =~ feat/story-([0-9]+-[0-9]+) ]]; then
      MERGED_STORIES+=("story-${BASH_REMATCH[1]}")
    fi
  done < <(git log --merges --pretty=format:"%s" 2>/dev/null \
    | grep -oE 'feat/story-[0-9]+-[0-9]+' || true)

  for story_id in "${MERGED_STORIES[@]}"; do
    # Skip baseline-exempted stories
    EXEMPTED=false
    for baseline_story in "${BASELINE_STORIES[@]}"; do
      if [[ "${baseline_story}" == "${story_id}" ]]; then
        EXEMPTED=true
        break
      fi
    done
    [[ "${EXEMPTED}" == "true" ]] && continue

    # Derive epic ID from story ID (story-XXXX-YYYY → epic-XXXX)
    EPIC_ID="epic-${story_id#story-}"
    EPIC_ID="${EPIC_ID%-*}"

    # Look for dep-policy-validation-report artifact under any matching epic dir
    REPORT_FOUND=false
    while IFS= read -r report_path; do
      if [[ -f "${report_path}" ]]; then
        # Path traversal guard
        if echo "${report_path}" | grep -q '\.\.'; then
          log_warn "Skipping suspicious report path: ${report_path}"
          continue
        fi
        REPORT_FOUND=true
        break
      fi
    done < <(find "${EPIC_DIR}" -name "dep-policy-validation-report-${story_id}.md" 2>/dev/null || true)

    if [[ "${REPORT_FOUND}" == "false" ]]; then
      VIOLATIONS+=("EVIDENCE_MISSING: dep-policy-validation-report-${story_id}.md not found under ${EPIC_DIR}/ (Rule 27 Surface 13)")
    fi
  done
fi

# --- Report results ---

if [[ ${#VIOLATIONS[@]} -eq 0 ]]; then
  log_info "All dependency policy checks passed — exit 0 (OK)"
  exit 0
fi

log_error "DEPENDENCY_POLICY_VIOLATION: ${#VIOLATIONS[@]} violation(s) detected:"
for v in "${VIOLATIONS[@]}"; do
  log_error "  - ${v}"
done
exit 1
