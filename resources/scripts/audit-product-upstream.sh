#!/usr/bin/env bash
# audit-product-upstream.sh — Camada 2 audit for Product → Capability → Feature coverage.
#
# Exit codes:
#   0 — OK
#   1 — PRODUCT_UPSTREAM_VIOLATION
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
  command -v bash >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: bash required" >&2; exit 2; }
  command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
  echo "${SCRIPT_NAME}: self-check OK"
  exit 0
}

extract_json_field() {
  local file="$1" field="$2"
  grep -o "\"${field}\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" "${file}" \
    | head -1 | sed 's/.*"\([^"]*\)"$/\1/' || true
}

case "${1:-}" in
  --self-check) self_check ;;
  "") ;;
  -h|--help) usage; exit 0 ;;
  *) echo "INVALID_ARGS: unknown flag ${1}" >&2; exit 2 ;;
esac

declare -A product_caps=()
declare -A capability_features=()
declare -A seen_products=()
declare -A seen_capabilities=()

while IFS= read -r file; do
  product_id="$(extract_json_field "${file}" "productId")"
  [[ -n "${product_id}" ]] && seen_products["${product_id}"]=1
done < <(find ai -type f -name '*-product.json' 2>/dev/null | sort)

while IFS= read -r file; do
  capability_id="$(extract_json_field "${file}" "capabilityId")"
  product_id="$(extract_json_field "${file}" "productId")"
  feature_id="$(extract_json_field "${file}" "featureId")"
  if [[ -n "${capability_id}" && -n "${product_id}" && -z "${feature_id}" ]]; then
    seen_capabilities["${capability_id}"]=1
    product_caps["${product_id}"]=$(( ${product_caps["${product_id}"]:-0} + 1 ))
  fi
  if [[ -n "${capability_id}" && -n "${feature_id}" ]]; then
    capability_features["${capability_id}"]=$(( ${capability_features["${capability_id}"]:-0} + 1 ))
  fi
done < <(find ai -type f -name '*.json' 2>/dev/null | sort)

violations=0
for product_id in "${!seen_products[@]}"; do
  if [[ ${product_caps["${product_id}"]:-0} -lt 1 ]]; then
    echo "PRODUCT_UPSTREAM_VIOLATION (product-without-capability): ${product_id}" >&2
    violations=$((violations + 1))
  fi
done

for capability_id in "${!seen_capabilities[@]}"; do
  if [[ ${capability_features["${capability_id}"]:-0} -lt 1 ]]; then
    echo "PRODUCT_UPSTREAM_VIOLATION (capability-without-feature): ${capability_id}" >&2
    violations=$((violations + 1))
  fi
done

echo "audit-product-upstream.sh: products=${#seen_products[@]} capabilities=${#seen_capabilities[@]} violations=${violations}"
[[ ${violations} -eq 0 ]] && exit 0 || exit 1
