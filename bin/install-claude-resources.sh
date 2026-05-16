#!/usr/bin/env bash
# install-claude-resources.sh — Install the Claude Code resource store into a target project.
#
# Replaces the former Java CLI (`ia-dev-env generate`). It performs the exact
# same operation that GenerateCommand did: a verbatim recursive copy of this
# repository's `resources/` tree into `<target>/.claude/`, plus the consumer
# `CLAUDE.md` stub. No templating — `{{PLACEHOLDER}}` tokens are resolved at
# skill runtime by Claude itself.
#
# Usage:
#   bin/install-claude-resources.sh [-o DIR] [-f] [--dry-run] [-v]
#
# Options:
#   -o, --output DIR   Target project directory (default: current directory)
#   -f, --force        Overwrite existing files (default: skip existing)
#       --dry-run       Simulate without writing any files
#   -v, --verbose      List each copied/skipped file
#   -h, --help         Show this help and exit
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESOURCE_ROOT="${REPO_ROOT}/resources"
CLAUDE_TEMPLATE="${REPO_ROOT}/CLAUDE.template.md"

output_dir="."
force=0
dry_run=0
verbose=0

usage() { sed -n '2,20p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    -o|--output) output_dir="${2:?--output requires a directory}"; shift 2;;
    -f|--force) force=1; shift;;
    --dry-run) dry_run=1; shift;;
    -v|--verbose) verbose=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "error: unknown argument: $1" >&2; usage >&2; exit 2;;
  esac
done

if [[ ! -d "${RESOURCE_ROOT}" ]]; then
  echo "error: resource store not found: ${RESOURCE_ROOT}" >&2
  exit 1
fi

mkdir -p "${output_dir}"
target="$(cd "${output_dir}" && pwd)"
claude_dir="${target}/.claude"

# Mirror GenerateCommand.validateDestContainment: a resolved destination must
# never escape the target directory. Lexical (filesystem-free) so it works in
# --dry-run before any directory exists. `base` is already an absolute,
# normalized path (built via `cd … && pwd`).
assert_contained() {
  local dest="$1" base="$2"
  # Reject any parent-traversal segment outright.
  case "/${dest}/" in
    */../*) echo "error: path traversal detected in '${dest}'" >&2; exit 1;;
  esac
  case "${dest}/" in
    "${base}/"*) : ;;
    *) echo "error: path traversal detected: '${dest}' escapes '${base}'" >&2; exit 1;;
  esac
}

declare -A counts=()
total=0

categorize() {
  case "${1%%/*}" in
    agents) echo "Agents";; hooks) echo "Hooks";; knowledge) echo "Knowledge";;
    rules) echo "Rules";; scripts) echo "Scripts";; skills) echo "Skills";;
    templates) echo "Templates";; settings.json) echo "Settings";; *) echo "Other";;
  esac
}

copy_one() {
  local src="$1" dest="$2" category="$3" label="$4" verb
  assert_contained "${dest}" "${target}"
  if [[ ${force} -eq 0 && -e "${dest}" ]]; then
    if [[ ${verbose} -eq 1 ]]; then echo "  skip   ${label}"; fi
    return 0
  fi
  if [[ ${dry_run} -eq 0 ]]; then
    mkdir -p "$(dirname "${dest}")"
    cp -p "${src}" "${dest}"
  fi
  if [[ ${verbose} -eq 1 ]]; then
    verb=$([[ ${dry_run} -eq 1 ]] && echo 'would copy' || echo 'copy  ')
    echo "  ${verb} ${label}"
  fi
  counts["${category}"]=$(( ${counts["${category}"]:-0} + 1 ))
  total=$(( total + 1 ))
  return 0
}

start_ms=$(( $(date +%s%N 2>/dev/null || echo "$(date +%s)000000000") / 1000000 ))

# 1. resources/ -> <target>/.claude/
while IFS= read -r src; do
  rel="${src#"${RESOURCE_ROOT}/"}"
  copy_one "${src}" "${claude_dir}/${rel}" "$(categorize "${rel}")" ".claude/${rel}"
done < <(find "${RESOURCE_ROOT}" -type f | LC_ALL=C sort)

# 2. CLAUDE.template.md -> <target>/CLAUDE.md
if [[ -f "${CLAUDE_TEMPLATE}" ]]; then
  copy_one "${CLAUDE_TEMPLATE}" "${target}/CLAUDE.md" "Root Files" "CLAUDE.md"
fi

end_ms=$(( $(date +%s%N 2>/dev/null || echo "$(date +%s)000000000") / 1000000 ))

action=$([[ ${dry_run} -eq 1 ]] && echo "Dry Run" || echo "Success")
printf '\nPipeline: %s (%dms)\n\n' "${action}" "$(( end_ms - start_ms ))"
sep="  $(printf '%0.s─' {1..22})  $(printf '%0.s─' {1..5})"
printf '  %-22s  %5s\n%s\n' "Category" "Count" "${sep}"
if [[ ${#counts[@]} -gt 0 ]]; then
  while IFS= read -r k; do
    [[ -n "${k}" ]] && printf '  %-22s  %5d\n' "${k}" "${counts[$k]}"
  done < <(printf '%s\n' "${!counts[@]}" | LC_ALL=C sort)
fi
printf '%s\n  %-22s  %5d\n' "${sep}" "Total" "${total}"
