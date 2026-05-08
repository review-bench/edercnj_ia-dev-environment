#!/usr/bin/env bash
# Layer:      2 (CI script — fires on PR open/sync)
# Rule:       Rule 05 §Mutation Score Threshold (EPIC-0072 story-0072-0006)
# Exit codes: 0=OK  1=MUTATION_SCORE_VIOLATION|MUTATION_REGRESSION  2=OPERATIONAL_ERROR  3=BASELINE_CORRUPT
# Latency:    < 5s wall-clock (SLA: story-0072-0006 AC §Performance)
set -euo pipefail

BASELINE_PATH="${PERF_BASELINE_DIR:-governance/baselines}/mutation-baseline.json"
MUTATION_REPORT_PATH="${MUTATION_REPORT_PATH:-mutation-report.json}"
THRESHOLD="${MUTATION_THRESHOLD:-80}"
REGRESSION_TOLERANCE="${MUTATION_REGRESSION_TOLERANCE:-5}"
STRICT_BASELINE="${STRICT_BASELINE:-false}"

log_info()  { echo "[audit-mutation-score] INFO  $*" >&2; }
log_warn()  { echo "[audit-mutation-score] WARN  $*" >&2; }
log_error() { echo "[audit-mutation-score] ERROR $*" >&2; }

# --self-check: verify prerequisites
case "${1:-}" in
  --self-check)
    command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required but not found on PATH" >&2; exit 2; }
    [[ -d "governance/baselines" ]] || { echo "OPERATIONAL_ERROR: governance/baselines directory not found" >&2; exit 2; }
    exit 0
    ;;
esac

# Ensure jq is available
command -v jq >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: jq required but not found on PATH"; exit 2; }

# Path traversal guard on MUTATION_REPORT_PATH
if echo "${MUTATION_REPORT_PATH}" | grep -q '\.\.'; then
  log_error "OPERATIONAL_ERROR: path traversal rejected: MUTATION_REPORT_PATH='${MUTATION_REPORT_PATH}'"
  exit 2
fi

# Resolve to absolute path and verify it stays within project root
PROJECT_ROOT="$(pwd)"
REPORT_ABS="$(realpath -m "${MUTATION_REPORT_PATH}" 2>/dev/null || echo "${PROJECT_ROOT}/${MUTATION_REPORT_PATH}")"
if [[ "${REPORT_ABS}" != "${PROJECT_ROOT}"* ]]; then
  log_error "OPERATIONAL_ERROR: path traversal rejected: resolved path escapes project root"
  exit 2
fi

# Symlink guard on report
if [[ -L "${MUTATION_REPORT_PATH}" ]]; then
  log_error "OPERATIONAL_ERROR: symlink rejected — report must be a regular file: ${MUTATION_REPORT_PATH}"
  exit 2
fi

# Check if mutation gate is enabled (advisory only — CI should pass when not configured)
MUTATION_ENABLED="${MUTATION_ENABLED:-false}"
if [[ "${MUTATION_ENABLED}" != "true" ]]; then
  log_info "quality.mutation.enabled=false — skipping mutation gate (exit 0)"
  exit 0
fi

# If report does not exist, check whether this is a first-run scenario
if [[ ! -f "${MUTATION_REPORT_PATH}" ]]; then
  if [[ ! -f "${BASELINE_PATH}" ]]; then
    log_warn "No mutation report and no baseline — first project run (no mutation analysis configured yet)"
    exit 0
  fi
  log_error "OPERATIONAL_ERROR: mutation report not found at '${MUTATION_REPORT_PATH}' but baseline exists; run mutation analysis first"
  exit 2
fi

# Symlink guard on baseline
if [[ -L "${BASELINE_PATH}" ]]; then
  log_error "OPERATIONAL_ERROR: symlink rejected — baseline must be a regular file: ${BASELINE_PATH}"
  exit 2
fi

# Parse mutation report — validate required fields
REPORT_VALID=$(jq -e '
  has("score") and
  has("total_mutations") and
  has("killed") and
  has("survived")
' "${MUTATION_REPORT_PATH}" 2>/dev/null) || true

if [[ "${REPORT_VALID}" != "true" ]]; then
  MISSING=$(jq -r '
    [ "score","total_mutations","killed","survived" ] |
    map(select(. as $f | input | has($f) | not)) |
    join(", ")
  ' "${MUTATION_REPORT_PATH}" 2>/dev/null || echo "unparseable JSON")
  log_error "OPERATIONAL_ERROR: malformed mutation report: missing required field(s): ${MISSING}"
  exit 2
fi

SCORE=$(jq '.score' "${MUTATION_REPORT_PATH}" 2>/dev/null)
TOTAL=$(jq '.total_mutations' "${MUTATION_REPORT_PATH}" 2>/dev/null)
KILLED=$(jq '.killed' "${MUTATION_REPORT_PATH}" 2>/dev/null)
SURVIVED=$(jq '.survived' "${MUTATION_REPORT_PATH}" 2>/dev/null)

# Validate score is a number in [0,100]
if ! echo "${SCORE}" | grep -qE '^[0-9]+(\.[0-9]+)?$'; then
  log_error "OPERATIONAL_ERROR: malformed mutation report: score '${SCORE}' is not a valid number"
  exit 2
fi

SCORE_INT=$(echo "${SCORE}" | awk '{print int($1)}')

log_info "Mutation report: score=${SCORE}% total=${TOTAL} killed=${KILLED} survived=${SURVIVED}"

# Stage policy: check release_count for WARN-then-FAIL (D-R3)
RELEASE_COUNT=0
if [[ -f "${BASELINE_PATH}" ]]; then
  # Symlink guard already done above; check baseline integrity
  if ! jq -e . "${BASELINE_PATH}" >/dev/null 2>&1; then
    log_error "BASELINE_CORRUPT: ${BASELINE_PATH} is not valid JSON"
    exit 3
  fi
  if ! jq -e 'has("score") and has("release_count")' "${BASELINE_PATH}" >/dev/null 2>&1; then
    log_error "BASELINE_CORRUPT: ${BASELINE_PATH} missing required fields (score, release_count)"
    exit 3
  fi
  RELEASE_COUNT=$(jq '.release_count // 0' "${BASELINE_PATH}")
  BASELINE_SCORE=$(jq '.score' "${BASELINE_PATH}")

  # --strict-baseline: cross-check release_count vs git tags
  if [[ "${STRICT_BASELINE}" == "true" ]]; then
    GIT_TAG_COUNT=$(git tag --list 2>/dev/null | wc -l | tr -d ' ')
    if [[ "${RELEASE_COUNT}" -gt "${GIT_TAG_COUNT}" ]]; then
      log_error "OPERATIONAL_ERROR: baseline release_count ${RELEASE_COUNT} inconsistent with tagged releases ${GIT_TAG_COUNT}"
      exit 2
    fi
  fi

  log_info "Baseline: score=${BASELINE_SCORE}% release_count=${RELEASE_COUNT}"

  # Check regression vs baseline
  REGRESSION=$(echo "${BASELINE_SCORE} ${SCORE}" | awk '{delta = $1 - $2; if (delta < 0) delta = 0; print int(delta)}')
  if [[ "${REGRESSION}" -gt "${REGRESSION_TOLERANCE}" ]]; then
    log_error "MUTATION_REGRESSION: regression ${REGRESSION}% exceeds tolerance ${REGRESSION_TOLERANCE}% (baseline=${BASELINE_SCORE}% current=${SCORE}%)"
    # Report top survived mutations if available
    if jq -e 'has("survived_details")' "${MUTATION_REPORT_PATH}" >/dev/null 2>&1; then
      log_info "Top 5 surviving mutants:"
      jq -r '.survived_details[:5][] | "  \(.mutator) @ \(.class):\(.line) — \(.description)"' "${MUTATION_REPORT_PATH}" 2>/dev/null || true
    fi
    exit 1
  fi
fi

# Check absolute threshold
if [[ "${SCORE_INT}" -lt "${THRESHOLD}" ]]; then
  if [[ "${RELEASE_COUNT}" -eq 0 ]]; then
    # First release with mutation enabled: WARN only (stage policy)
    log_warn "MUTATION_SCORE_VIOLATION (stage=WARN): score ${SCORE}% below threshold ${THRESHOLD}% — first release, violation tolerated for one cycle"
    log_warn "Initializing baseline with current score. Next release will enforce FAIL."
    # Create/update baseline with release_count=1
    TMP_BASELINE="${BASELINE_PATH}.tmp"
    GIT_SHA=$(git rev-parse HEAD 2>/dev/null || echo "unknown")
    jq -n \
      --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
      --arg sha "${GIT_SHA}" \
      --argjson score "${SCORE}" \
      --argjson total "${TOTAL}" \
      --argjson killed "${KILLED}" \
      --argjson survived "${SURVIVED}" \
      '{
        timestamp: $ts,
        git_sha: $sha,
        score: $score,
        total_mutations: $total,
        killed: $killed,
        survived: $survived,
        release_count: 1,
        exclusions: []
      }' > "${TMP_BASELINE}" && mv "${TMP_BASELINE}" "${BASELINE_PATH}"
    log_info "Baseline initialized at ${BASELINE_PATH}"
    exit 0
  fi
  log_error "MUTATION_SCORE_VIOLATION: score ${SCORE}% below threshold ${THRESHOLD}% (release_count=${RELEASE_COUNT})"
  exit 1
fi

log_info "Mutation gate PASSED: score=${SCORE}% >= threshold=${THRESHOLD}% regression=${REGRESSION:-0}% <= tolerance=${REGRESSION_TOLERANCE}%"

# Update baseline if score improved
if [[ -f "${BASELINE_PATH}" ]]; then
  BASELINE_SCORE_INT=$(jq '.score | tonumber | floor' "${BASELINE_PATH}")
  if [[ "${SCORE_INT}" -gt "${BASELINE_SCORE_INT}" ]]; then
    NEW_RELEASE_COUNT=$((RELEASE_COUNT + 1))
    TMP_BASELINE="${BASELINE_PATH}.tmp"
    GIT_SHA=$(git rev-parse HEAD 2>/dev/null || echo "unknown")
    jq \
      --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
      --arg sha "${GIT_SHA}" \
      --argjson score "${SCORE}" \
      --argjson total "${TOTAL}" \
      --argjson killed "${KILLED}" \
      --argjson survived "${SURVIVED}" \
      --argjson rc "${NEW_RELEASE_COUNT}" \
      '. + {timestamp: $ts, git_sha: $sha, score: $score, total_mutations: $total, killed: $killed, survived: $survived, release_count: $rc}' \
      "${BASELINE_PATH}" > "${TMP_BASELINE}" && mv "${TMP_BASELINE}" "${BASELINE_PATH}"
    log_info "Baseline updated: score ${BASELINE_SCORE} -> ${SCORE}%"
  fi
else
  # Create baseline on first passing run
  TMP_BASELINE="${BASELINE_PATH}.tmp"
  GIT_SHA=$(git rev-parse HEAD 2>/dev/null || echo "unknown")
  jq -n \
    --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --arg sha "${GIT_SHA}" \
    --argjson score "${SCORE}" \
    --argjson total "${TOTAL}" \
    --argjson killed "${KILLED}" \
    --argjson survived "${SURVIVED}" \
    '{
      timestamp: $ts,
      git_sha: $sha,
      score: $score,
      total_mutations: $total,
      killed: $killed,
      survived: $survived,
      release_count: 1,
      exclusions: []
    }' > "${TMP_BASELINE}" && mv "${TMP_BASELINE}" "${BASELINE_PATH}"
  log_info "Baseline created at ${BASELINE_PATH}"
fi

exit 0
