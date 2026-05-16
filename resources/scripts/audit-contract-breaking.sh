#!/usr/bin/env bash
# Layer:      2 (CI script — fires on PR open/sync)
# Rule:       Rule 26 §Audit Gate Lifecycle (EPIC-0072 story-0072-0007)
# Exit codes: 0=OK  1=CONTRACT_BREAKING_VIOLATION  2=OPERATIONAL_ERROR  3=BASELINE_CORRUPT
# Latency:    < 5s wall-clock (SLA: story-0072-0007 AC §Performance)
# Future-wired: when EPIC-0071 ships audit-changelog-presence.sh, detection of
#              "## Breaking" may be delegated to that audit instead of this local regex.
set -euo pipefail

BREAKING_LOG_PATH="${BREAKING_LOG_PATH:-governance/audits/contract-breaking-history.log}"
BASELINE_PATH="${BASELINE_PATH:-governance/baselines/contract-breaking-baseline.txt}"
BASE_BRANCH="${BASE_BRANCH:-origin/develop}"
CONTRACT_ENABLED="${CONTRACT_ENABLED:-false}"

log_info()  { echo "[audit-contract-breaking] INFO  $*" >&2; }
log_warn()  { echo "[audit-contract-breaking] WARN  $*" >&2; }
log_error() { echo "[audit-contract-breaking] ERROR $*" >&2; }

# --self-check: verify prerequisites
case "${1:-}" in
  --self-check)
    command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required but not found on PATH" >&2; exit 2; }
    command -v git >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: git required but not found on PATH" >&2; exit 2; }
    if ! command -v openapi-diff >/dev/null 2>&1; then
      echo "WARN: openapi-diff not found — will use regex heuristics for OpenAPI analysis" >&2
    fi
    if ! command -v buf >/dev/null 2>&1; then
      echo "WARN: buf not found — will use regex heuristics for protobuf analysis" >&2
    fi
    [[ -d "governance" ]] || { echo "OPERATIONAL_ERROR: governance/ directory not found" >&2; exit 2; }
    exit 0
    ;;
esac

# Validate prerequisites
command -v git >/dev/null 2>&1 || { log_error "OPERATIONAL_ERROR: git required but not found on PATH"; exit 2; }

# Skip gate when contract testing is not enabled
if [[ "${CONTRACT_ENABLED}" != "true" ]]; then
  log_info "quality.contract.enabled=false — skipping contract breaking gate (exit 0)"
  exit 0
fi

# Detect shallow clone and handle
if git rev-parse --is-shallow-repository 2>/dev/null | grep -q "true"; then
  log_warn "Shallow clone detected — fetching full history for accurate diff"
  git fetch --unshallow 2>/dev/null || {
    log_error "OPERATIONAL_ERROR: shallow clone — git fetch --unshallow failed; cannot perform contract diff"
    exit 2
  }
fi

# Check baseline file integrity if it exists
if [[ -f "${BASELINE_PATH}" ]]; then
  if [[ -L "${BASELINE_PATH}" ]]; then
    log_error "OPERATIONAL_ERROR: symlink rejected — baseline must be a regular file: ${BASELINE_PATH}"
    exit 2
  fi
  # Basic format check: lines should be story IDs or comments
  if grep -qvE '^(#|story-[0-9]+-[0-9]+|$)' "${BASELINE_PATH}" 2>/dev/null; then
    log_error "BASELINE_CORRUPT: ${BASELINE_PATH} contains invalid entries"
    exit 3
  fi
fi

# Detect contract artifacts changed in the PR vs base branch
CHANGED_CONTRACTS=()
while IFS= read -r f; do
  # Path traversal guard: reject any path with '..'
  if echo "${f}" | grep -q '\.\.'; then
    log_error "OPERATIONAL_ERROR: invalid contract artifact path: traversal detected in '${f}'"
    exit 2
  fi
  # Filename sanitizer: reject filenames with command injection characters
  BASENAME="$(basename "${f}")"
  if echo "${BASENAME}" | grep -qE '[;&|`$(){}]'; then
    log_error "OPERATIONAL_ERROR: invalid contract artifact filename: rejected by sanitizer '${BASENAME}'"
    exit 2
  fi
  CHANGED_CONTRACTS+=("${f}")
done < <(git diff --name-only "${BASE_BRANCH}"...HEAD 2>/dev/null | grep -E '\.(yaml|yml|proto|avsc|json)$' | grep -E '(openapi|api|proto|schema|contract|avro)' || true)

if [[ ${#CHANGED_CONTRACTS[@]} -eq 0 ]]; then
  log_info "No contract artifacts changed — exit 0 (OK)"
  exit 0
fi

log_info "Contract artifacts changed: ${#CHANGED_CONTRACTS[@]} file(s)"
for f in "${CHANGED_CONTRACTS[@]}"; do
  log_info "  - ${f}"
done

# Classify breaking changes per artifact type
BREAKING_DETECTED=false
BREAKING_TYPES=()

for artifact in "${CHANGED_CONTRACTS[@]}"; do
  if [[ ! -f "${artifact}" ]]; then
    log_warn "Artifact '${artifact}' not found in working tree — may have been deleted (potential breaking change)"
    BREAKING_DETECTED=true
    BREAKING_TYPES+=("ARTIFACT_DELETED:${artifact}")
    continue
  fi

  DIFF_CONTENT="$(git diff "${BASE_BRANCH}"...HEAD -- "${artifact}" 2>/dev/null || true)"

  case "${artifact}" in
    *.proto)
      # Protobuf: check for field number changes or message removals
      if echo "${DIFF_CONTENT}" | grep -qE '^-\s*(repeated\s+)?[a-zA-Z_][a-zA-Z0-9_.]*\s+[a-zA-Z_][a-zA-Z0-9_]*\s*=\s*[0-9]+'; then
        BREAKING_DETECTED=true
        BREAKING_TYPES+=("FIELD_REMOVED:${artifact}")
      fi
      if echo "${DIFF_CONTENT}" | grep -qE '^-message\s+[A-Z]'; then
        BREAKING_DETECTED=true
        BREAKING_TYPES+=("MESSAGE_REMOVED:${artifact}")
      fi
      if command -v buf >/dev/null 2>&1; then
        # Use buf for authoritative proto breaking check if available
        if buf breaking --against ".git#branch=${BASE_BRANCH#origin/}" "${artifact}" 2>/dev/null; then
          : # buf says no breaking — trust it over regex
        else
          BREAKING_DETECTED=true
          BREAKING_TYPES+=("BUF_BREAKING:${artifact}")
        fi
      fi
      ;;
    *.yaml|*.yml)
      if echo "${artifact}" | grep -qiE '(openapi|api)'; then
        # OpenAPI: check for path/field removals
        if echo "${DIFF_CONTENT}" | grep -qE '^-\s+/(|v[0-9]+/)[a-zA-Z]'; then
          BREAKING_DETECTED=true
          BREAKING_TYPES+=("ENDPOINT_REMOVED:${artifact}")
        fi
        if echo "${DIFF_CONTENT}" | grep -qE '^-\s+(required:|  - [a-zA-Z])' ; then
          BREAKING_DETECTED=true
          BREAKING_TYPES+=("FIELD_REMOVED:${artifact}")
        fi
        if command -v openapi-diff >/dev/null 2>&1; then
          # Use openapi-diff for authoritative check if available
          OLD_VERSION="$(git show "${BASE_BRANCH}:${artifact}" 2>/dev/null || true)"
          if [[ -n "${OLD_VERSION}" ]]; then
            OPENDIFF_OUT="$(echo "${OLD_VERSION}" | openapi-diff - "${artifact}" 2>/dev/null || true)"
            if echo "${OPENDIFF_OUT}" | grep -qi "breaking"; then
              BREAKING_DETECTED=true
              BREAKING_TYPES+=("OPENAPI_DIFF_BREAKING:${artifact}")
            fi
          fi
        fi
      fi
      ;;
    *.avsc|*.json)
      # Avro schema: check for field removals or type changes
      if echo "${DIFF_CONTENT}" | grep -qE '^-\s+"name"\s*:'; then
        BREAKING_DETECTED=true
        BREAKING_TYPES+=("FIELD_REMOVED:${artifact}")
      fi
      if echo "${DIFF_CONTENT}" | grep -qE '^-\s+"type"\s*:'; then
        BREAKING_DETECTED=true
        BREAKING_TYPES+=("FIELD_TYPE_CHANGED:${artifact}")
      fi
      ;;
  esac
done

if [[ "${BREAKING_DETECTED}" == "false" ]]; then
  log_info "No breaking changes detected in contract artifacts — exit 0 (OK)"
  exit 0
fi

log_warn "Breaking change(s) detected:"
for bt in "${BREAKING_TYPES[@]}"; do
  log_warn "  - ${bt}"
done

# Check CHANGELOG.md diff for "## Breaking" entry
# Regex per story-0072-0007 §6.3: ^##\s+(Breaking(\s+Changes?)?|BREAKING)\s*$
CHANGELOG_BREAKING=false
CHANGELOG_DIFF="$(git diff "${BASE_BRANCH}"...HEAD -- CHANGELOG.md 2>/dev/null || true)"

if echo "${CHANGELOG_DIFF}" | grep -qiE '^\+##\s+(Breaking(\s+Changes?)?|BREAKING)\s*$'; then
  CHANGELOG_BREAKING=true
fi

# Also check for Conventional Commits BREAKING CHANGE: footer (secondary signal)
COMMIT_BREAKING=false
if git log "${BASE_BRANCH}"...HEAD --format="%B" 2>/dev/null | grep -qE '^BREAKING CHANGE:'; then
  COMMIT_BREAKING=true
fi

if [[ "${CHANGELOG_BREAKING}" == "true" || "${COMMIT_BREAKING}" == "true" ]]; then
  log_warn "CONTRACT_BREAKING_VIOLATION (stage=WARN): breaking change detected — documented migration accepted"
  log_warn "Migration documentation found in: $([ "${CHANGELOG_BREAKING}" == "true" ] && echo "CHANGELOG.md" || echo "commit message BREAKING CHANGE: footer")"

  # Append to audit history log
  mkdir -p "$(dirname "${BREAKING_LOG_PATH}")"
  if [[ ! -L "${BREAKING_LOG_PATH}" ]]; then
    GIT_SHA="$(git rev-parse HEAD 2>/dev/null || echo "unknown")"
    AUTHOR_EMAIL="$(git log -1 --format='%ae' 2>/dev/null || echo "unknown")"
    FIRST_BREAKING="${BREAKING_TYPES[0]:-UNKNOWN:unknown}"
    BREAKING_TYPE_ENUM="${FIRST_BREAKING%%:*}"
    ARTIFACT_PATH_LOG="${FIRST_BREAKING#*:}"
    JUSTIFICATION="documented via $([ "${CHANGELOG_BREAKING}" == "true" ] && echo "CHANGELOG ## Breaking" || echo "BREAKING CHANGE: footer")"
    TS="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf '%s\t%s\t%s\t%s\t%s\t"%s"\n' \
      "${TS}" "${GIT_SHA}" "${AUTHOR_EMAIL}" \
      "${BREAKING_TYPE_ENUM}" "${ARTIFACT_PATH_LOG}" "${JUSTIFICATION}" \
      >> "${BREAKING_LOG_PATH}"
    log_info "Audit record appended to ${BREAKING_LOG_PATH}"
  fi
  exit 0
fi

# Breaking detected with no migration documentation
log_error "CONTRACT_BREAKING_VIOLATION: breaking change detected; CHANGELOG.md needs '## Breaking' entry"
log_error "Add '## Breaking' or '## Breaking Changes' header to CHANGELOG.md in this PR"
log_error "Detected breaking types:"
for bt in "${BREAKING_TYPES[@]}"; do
  log_error "  - ${bt}"
done
exit 1
