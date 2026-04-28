#!/usr/bin/env bash
#
# audit-hooks-self-check.sh — Rule 26 (Audit Gate Lifecycle) CI audit.
#
# Verifies that every hook script registered in .claude/settings.json
# implements --self-check correctly:
#   (a) The hook script file exists on disk and is executable.
#   (b) The script's source text references the string "--self-check".
#   (c) Running `hook-script.sh --self-check` exits 0.
#
# Exit codes:
#   0   All registered hooks pass --self-check.
#   1   HOOKS_SELF_CHECK_FAILED: at least one hook failed a check.
#   2   OPERATIONAL_ERROR: jq absent, settings.json missing, etc.
#
# Flags:
#   --self-check  Validate script integrity (deps, files). Exit 0 OK / 2 broken.
#   -h|--help     Print usage and exit 0.
#
# Introduced by story-0063-0018 (EPIC-0063). See Rule 26 at
# .claude/rules/26-audit-gate-lifecycle.md for the --self-check contract.
#
# Catalogado em: docs/audit-gates-catalog.md

set -uo pipefail

SCRIPT_VERSION="1.0.0"
SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
# This script lives at .claude/scripts/; parent of scripts/ is .claude/
SCRIPTS_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CLAUDE_DIR="$(cd -- "${SCRIPTS_DIR}/.." && pwd)"
REPO_ROOT="$(cd -- "${CLAUDE_DIR}/.." && pwd)"
SETTINGS_FILE="${CLAUDE_DIR}/settings.json"
HOOKS_DIR="${CLAUDE_DIR}/hooks"

usage() {
  cat <<-EOF
Usage: ${SCRIPT_NAME} [--self-check] [-h|--help]

  Audit that every hook registered in .claude/settings.json implements
  --self-check correctly (Rule 26 §Camada 0 Hook Contract).

  Checks per hook:
    A  Hook script file exists on disk.
    B  Hook script is executable (chmod +x).
    C  Hook script source references '--self-check'.
    D  Running hook with --self-check exits 0.

  Exit codes:
    0  All hooks PASS.
    1  HOOKS_SELF_CHECK_FAILED: at least one hook failed.
    2  OPERATIONAL_ERROR (jq absent, settings.json missing, parse error).
EOF
}

self_check() {
  local ok=1
  if ! command -v jq &>/dev/null; then
    echo "${SCRIPT_NAME}: DEPENDENCY_MISSING: jq not found on PATH" >&2
    ok=0
  fi
  if [[ ! -f "${SETTINGS_FILE}" ]]; then
    echo "${SCRIPT_NAME}: OPERATIONAL_ERROR: settings.json not found at ${SETTINGS_FILE}" >&2
    ok=0
  fi
  if [[ ! -d "${HOOKS_DIR}" ]]; then
    echo "${SCRIPT_NAME}: OPERATIONAL_ERROR: hooks/ directory not found at ${HOOKS_DIR}" >&2
    ok=0
  fi
  if [[ $ok -eq 0 ]]; then exit 2; fi
  echo "${SCRIPT_NAME}: OK (version ${SCRIPT_VERSION}, deps ok, settings.json found)"
  exit 0
}

# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------
for arg in "$@"; do
  case "$arg" in
    --self-check) self_check ;;
    -h|--help)    usage; exit 0 ;;
    *) echo "${SCRIPT_NAME}: INVALID_ARGS: unknown flag: $arg" >&2; exit 2 ;;
  esac
done

# ---------------------------------------------------------------------------
# Pre-flight: verify dependencies
# ---------------------------------------------------------------------------
if ! command -v jq &>/dev/null; then
  echo "${SCRIPT_NAME}: OPERATIONAL_ERROR: jq not found on PATH" >&2
  exit 2
fi

if [[ ! -f "${SETTINGS_FILE}" ]]; then
  echo "${SCRIPT_NAME}: OPERATIONAL_ERROR: settings.json not found at ${SETTINGS_FILE}" >&2
  exit 2
fi

# ---------------------------------------------------------------------------
# Extract all hook command paths from settings.json
# ---------------------------------------------------------------------------
# settings.json structure:
#   { "hooks": { "EventName": [ { "hooks": [ { "command": "..." } ] } ] } }
# or with matcher:
#   { "hooks": { "EventName": [ { "matcher": "...", "hooks": [ { "command": "..." } ] } ] } }
# The top-level "hooks" key contains the event map; navigate into it first.
hook_commands=$(jq -r '
  .hooks
  | to_entries[]
  | .value[]
  | .hooks[]?
  | .command
  // empty
' "${SETTINGS_FILE}" 2>&1) || {
  echo "${SCRIPT_NAME}: OPERATIONAL_ERROR: failed to parse ${SETTINGS_FILE}: ${hook_commands}" >&2
  exit 2
}

if [[ -z "${hook_commands}" ]]; then
  echo "${SCRIPT_NAME}: INFO: no hook commands found in ${SETTINGS_FILE}; nothing to check."
  exit 0
fi

violations=0

while IFS= read -r raw_command; do
  [[ -z "${raw_command}" ]] && continue

  # Resolve $CLAUDE_PROJECT_DIR-style references to the repo root for local testing.
  # Strip quotes and variable references to get the bare script path.
  resolved_command="${raw_command//\"\$CLAUDE_PROJECT_DIR\"/${REPO_ROOT}}"
  resolved_command="${resolved_command//\$CLAUDE_PROJECT_DIR/${REPO_ROOT}}"

  # The command may include arguments after the script path; take only the first token.
  hook_script=$(echo "${resolved_command}" | awk '{print $1}')

  # Strip surrounding quotes if any.
  hook_script="${hook_script//\"/}"
  hook_script="${hook_script//\'/}"

  # Skip non-.sh entries (e.g., bare executables or platform commands).
  if [[ "${hook_script}" != *.sh ]]; then
    continue
  fi

  # Check A — script file exists
  if [[ ! -f "${hook_script}" ]]; then
    echo "${SCRIPT_NAME}: HOOKS_SELF_CHECK_FAILED: hook not found on disk: ${hook_script}" >&2
    violations=$((violations + 1))
    continue
  fi

  # Check B — script is executable
  if [[ ! -x "${hook_script}" ]]; then
    echo "${SCRIPT_NAME}: HOOKS_SELF_CHECK_FAILED: hook not executable: ${hook_script}" >&2
    violations=$((violations + 1))
    continue
  fi

  # Check C — script references --self-check in its source
  if ! grep -q -- '--self-check' "${hook_script}" 2>/dev/null; then
    echo "${SCRIPT_NAME}: HOOKS_SELF_CHECK_FAILED: hook does not reference --self-check: ${hook_script}" >&2
    violations=$((violations + 1))
    continue
  fi

  # Check D — running hook --self-check exits 0
  self_check_output=$("${hook_script}" --self-check 2>&1)
  self_check_exit=$?
  if [[ $self_check_exit -ne 0 ]]; then
    echo "${SCRIPT_NAME}: HOOKS_SELF_CHECK_FAILED: hook --self-check exited ${self_check_exit}: ${hook_script}" >&2
    echo "${SCRIPT_NAME}:   output: ${self_check_output}" >&2
    violations=$((violations + 1))
  fi

done <<< "${hook_commands}"

echo "${SCRIPT_NAME}: checked $(echo "${hook_commands}" | grep -c '\.sh' || echo 0) hook(s); violations: ${violations}"
[[ $violations -eq 0 ]] && exit 0 || exit 1
