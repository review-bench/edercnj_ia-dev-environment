#!/usr/bin/env bash
# audit-perf-baseline.sh — Camada 2 (CI) audit for EPIC-0072 (Comprehensive Test Strategy).
#
# Validates the integrity of governance/baselines/performance-baseline.json:
#   1. JSON is parse-valid and schema-conformant (has _format_version field).
#   2. File size guard: rejects files > 1MB (resource exhaustion protection).
#   3. Symlink guard: rejects symlinks (path traversal protection).
#   4. Path traversal guard: rejects PERF_BASELINE_PATH values with `..` (security).
#   5. Silent overwrite detection: if baseline was modified in this PR (git diff HEAD~1)
#      without a corresponding entry in perf-baseline-updates.log, exits 1.
#
# NOT this script's responsibility: detecting performance regressions at runtime
#   (that is x-test-performance's job). This script guards baseline integrity only.
#
# Layer:   2 — CI Script (Rule 26 §Taxonomy)
# Rule:    EPIC-0072 story-0072-0005
# Catalog: docs/audit-gates-catalog.md
#
# Exit codes (Rule 26 §Standardized):
#   0 — OK (no violations, or baseline absent on first run)
#   1 — PERF_BASELINE_VIOLATION (silent overwrite or JSON schema violation)
#   2 — OPERATIONAL_ERROR (jq missing, path traversal, symlink, or file too large)
#   3 — BASELINE_CORRUPT (JSON parse error)
#
# Usage:
#   audit-perf-baseline.sh                      # full audit
#   audit-perf-baseline.sh --self-check         # verify wiring (Rule 26)

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

DEFAULT_BASELINE_PATH="governance/baselines/performance-baseline.json"
LOG_FILE="governance/baselines/perf-baseline-updates.log"
MAX_BASELINE_BYTES=1048576  # 1MB

# ── helpers ───────────────────────────────────────────────────────────────────

log_violation() {
    echo "PERF_BASELINE_VIOLATION: $*" >&2
}

log_error() {
    echo "OPERATIONAL_ERROR: $*" >&2
}

log_warn() {
    echo "WARN [audit-perf-baseline] $*" >&2
}

# ── self-check ────────────────────────────────────────────────────────────────

if [[ "${1:-}" == "--self-check" ]]; then
    if ! command -v jq >/dev/null 2>&1; then
        log_error "jq not found on PATH — required for JSON validation"
        exit 2
    fi
    if [[ ! -d "governance/baselines" ]]; then
        log_error "governance/baselines/ directory not found"
        exit 2
    fi
    echo "OK — audit-perf-baseline.sh self-check passed"
    exit 0
fi

# ── path validation ───────────────────────────────────────────────────────────

# Respect env override but reject path traversal
BASELINE_PATH="${PERF_BASELINE_PATH:-${DEFAULT_BASELINE_PATH}}"

# Security: reject paths with '..'
if echo "${BASELINE_PATH}" | grep -q '\.\.'; then
    log_error "path traversal attempt rejected: PERF_BASELINE_PATH='${BASELINE_PATH}'"
    exit 2
fi

# Resolve and verify the path stays inside REPO_ROOT
RESOLVED_BASELINE="$(cd "${REPO_ROOT}" && realpath -m "${BASELINE_PATH}" 2>/dev/null || echo "")"
if [[ -z "${RESOLVED_BASELINE}" ]] || [[ "${RESOLVED_BASELINE}" != "${REPO_ROOT}"* ]]; then
    log_error "path traversal attempt rejected: resolved path is outside repo root"
    exit 2
fi

# ── degenerate case: baseline absent ─────────────────────────────────────────

if [[ ! -e "${BASELINE_PATH}" ]]; then
    log_warn "no baseline yet — first run will create it (exit 0)"
    echo "OK — no performance baseline found; skipping integrity check"
    exit 0
fi

# ── security guards ───────────────────────────────────────────────────────────

# Reject symlinks
if [[ -L "${BASELINE_PATH}" ]]; then
    log_error "symlink rejected — baseline must be a regular file: ${BASELINE_PATH}"
    exit 2
fi

# File size guard (resource exhaustion protection)
BASELINE_SIZE="$(wc -c < "${BASELINE_PATH}" 2>/dev/null || echo 0)"
if [[ "${BASELINE_SIZE}" -gt "${MAX_BASELINE_BYTES}" ]]; then
    log_error "baseline file exceeds 1MB (${BASELINE_SIZE} bytes) — possible corruption or injection"
    exit 2
fi

# ── jq availability ───────────────────────────────────────────────────────────

if ! command -v jq >/dev/null 2>&1; then
    log_error "jq not found on PATH — required for JSON validation"
    exit 2
fi

# ── JSON parse validation ─────────────────────────────────────────────────────

if ! jq empty "${BASELINE_PATH}" 2>/dev/null; then
    echo "BASELINE_CORRUPT: ${BASELINE_PATH} contains invalid JSON — revert to baseline on develop" >&2
    exit 3
fi

# ── schema: _format_version required ─────────────────────────────────────────

FORMAT_VERSION="$(jq -r '._format_version // empty' "${BASELINE_PATH}" 2>/dev/null || true)"
if [[ -z "${FORMAT_VERSION}" ]]; then
    log_violation "missing required field '_format_version' in ${BASELINE_PATH} — schema violation"
    exit 1
fi

# ── silent overwrite detection ────────────────────────────────────────────────

# Check if baseline was modified in this PR (compare with HEAD~1)
if ! git rev-parse HEAD~1 >/dev/null 2>&1; then
    # Shallow clone: try to deepen by 1
    git fetch --deepen=1 >/dev/null 2>&1 || true
fi

BASELINE_CHANGED=""
if git rev-parse HEAD~1 >/dev/null 2>&1; then
    BASELINE_CHANGED="$(git diff HEAD~1..HEAD --name-only 2>/dev/null \
        | grep -F "${BASELINE_PATH}" || true)"
fi

if [[ -n "${BASELINE_CHANGED}" ]]; then
    # Baseline was modified in this PR — require a log entry
    if [[ ! -f "${LOG_FILE}" ]]; then
        log_violation "baseline updated without justification (${BASELINE_PATH} modified but ${LOG_FILE} does not exist)"
        log_warn "To fix: create ${LOG_FILE} with entry format: <ISO8601-timestamp>\\t<git-sha>\\t<author>\\t\"<reason>\""
        exit 1
    fi

    # Read the current git SHA and check for a log entry with that SHA
    CURRENT_SHA="$(git rev-parse HEAD 2>/dev/null || true)"
    LOG_ENTRY=""
    if [[ -n "${CURRENT_SHA}" ]]; then
        LOG_ENTRY="$(grep -F "${CURRENT_SHA}" "${LOG_FILE}" 2>/dev/null || true)"
    fi

    # Also accept log entries without a specific SHA (matched by recent timestamp)
    if [[ -z "${LOG_ENTRY}" ]]; then
        # Check if log was also modified in this PR as a fallback
        LOG_CHANGED="$(git diff HEAD~1..HEAD --name-only 2>/dev/null \
            | grep -F "${LOG_FILE}" || true)"
        if [[ -z "${LOG_CHANGED}" ]]; then
            log_violation "baseline updated without justification — ${BASELINE_PATH} modified but no new entry in ${LOG_FILE}"
            log_warn "Diff of baseline change:"
            git diff HEAD~1..HEAD -- "${BASELINE_PATH}" 2>/dev/null | head -40 >&2 || true
            log_warn "To fix: append entry to ${LOG_FILE} with format: <ISO8601-timestamp>\\t<git-sha>\\t<author>\\t\"<reason>\""
            exit 1
        fi
    fi
fi

# ── all checks passed ─────────────────────────────────────────────────────────

echo "OK — performance baseline integrity checks passed"
exit 0
