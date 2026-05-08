#!/usr/bin/env bash
# audit-rnf-gates.sh — Camada 2 audit for inherited RNF validity in stories.
#
# Exit codes:
#   0 — OK
#   1 — RNF_GATE_VIOLATION
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
  command -v awk >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: awk required" >&2; exit 2; }
  echo "${SCRIPT_NAME}: self-check OK"
  exit 0
}

normalize_cell() {
  local value
  value=$(printf '%s' "$1" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
  if [[ -z "${value}" || "${value}" == "—" ]]; then
    printf ''
    return 0
  fi
  printf '%s' "${value}"
}

validate_story() {
  local file="$1"
  grep -q '^## 2\. RNFs Herdadas' "${file}" || {
    echo "RNF_GATE_VIOLATION (missing-rnf-section): ${file}" >&2
    return 1
  }
  while IFS= read -r row; do
    IFS='|' read -r _ raw_category _raw_original raw_no_relax _raw_override raw_justification raw_approval raw_approver _ <<< "${row}"
    local category no_relax justification approval approver
    category=$(normalize_cell "${raw_category}")
    no_relax=$(normalize_cell "${raw_no_relax}")
    justification=$(normalize_cell "${raw_justification}")
    approval=$(normalize_cell "${raw_approval}")
    approver=$(normalize_cell "${raw_approver}")
    category=$(printf '%s' "${category}" | tr '[:lower:]' '[:upper:]')
    approval=$(printf '%s' "${approval}" | tr '[:lower:]' '[:upper:]')
    [[ "${no_relax}" == "true" ]] && continue
    if [[ "${category}" == "SECURITY" || "${category}" == "COMPLIANCE" ]]; then
      echo "RNF_GATE_VIOLATION (no-relax-broken): ${file} category=${category}" >&2
      return 1
    fi
    if [[ -z "${justification}" || "${approval}" != "APPROVED" || -z "${approver}" ]]; then
      echo "RNF_GATE_VIOLATION (unapproved-relaxation): ${file} category=${category}" >&2
      return 1
    fi
  done < <(
    awk '
      /^## 2\. RNFs Herdadas/ { in_section=1; next }
      in_section && /^## / { exit }
      in_section && /^\|/ && $0 !~ /Categoria/ && $0 !~ /:---/ { print }
    ' "${file}"
  )
  return 0
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
  validate_story "${file}" || violations=$((violations + 1))
done < <(find ai/epics -type f -name 'story-*.md' 2>/dev/null | sort)

echo "audit-rnf-gates.sh: checked=${checked} violations=${violations}"
[[ ${violations} -eq 0 ]] && exit 0 || exit 1
