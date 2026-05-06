#!/usr/bin/env bash
# audit-c4-completeness.sh — Camada 2 audit for C4 section coverage in epic/story markdown.
#
# Exit codes:
#   0 — OK
#   1 — C4_COMPLETENESS_VIOLATION
#   2 — OPERATIONAL_ERROR / INVALID_ARGS

set -euo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

usage() {
  cat <<EOF
Usage: ${SCRIPT_NAME} [--self-check]
EOF
}

self_check() {
  command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
  echo "${SCRIPT_NAME}: self-check OK"
  exit 0
}

has_marker() {
  local file="$1" marker="$2"
  grep -qi "${marker}" "${file}"
}

case "${1:-}" in
  --self-check) self_check ;;
  "") ;;
  -h|--help) usage; exit 0 ;;
  *) echo "INVALID_ARGS: unknown flag ${1}" >&2; exit 2 ;;
esac

violations=0
checked=0
while IFS= read -r file; do
  checked=$((checked + 1))
  missing=()
  has_marker "${file}" "C4 Context" || missing+=("Context")
  has_marker "${file}" "C4 Container" || missing+=("Container")
  has_marker "${file}" "C4 Component" || missing+=("Component")
  has_marker "${file}" "C4 Code" || missing+=("Code")
  if [[ ${#missing[@]} -gt 0 ]]; then
    echo "C4_COMPLETENESS_VIOLATION: ${file} missing ${missing[*]}" >&2
    violations=$((violations + 1))
  fi
done < <(find ai/epics -type f \( -name 'epic-*.md' -o -name 'story-*.md' \) 2>/dev/null | sort)

echo "audit-c4-completeness.sh: checked=${checked} violations=${violations}"
[[ ${violations} -eq 0 ]] && exit 0 || exit 1
