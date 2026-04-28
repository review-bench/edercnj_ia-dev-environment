#!/usr/bin/env bash
set -euo pipefail
# audit-go-mod-tidy.sh — Go: go.mod tidy check
# Verifies go.mod has no spurious dependencies (go mod tidy idempotent)
# Lock file: {{LOCK_FILE}} | Test command: {{TEST_COMMAND}}
case "${1:-}" in
  --self-check)
    command -v go >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: go required" >&2; exit 2; }
    [[ -f "{{LOCK_FILE}}" ]] || { echo "OPERATIONAL_ERROR: {{LOCK_FILE}} not found" >&2; exit 2; }
    exit 0 ;;
esac
cp go.mod go.mod.bak && cp go.sum go.sum.bak
{{BUILD_TOOL}} mod tidy 2>/dev/null
if ! diff -q go.mod go.mod.bak > /dev/null 2>&1 || ! diff -q go.sum go.sum.bak > /dev/null 2>&1; then
  mv go.mod.bak go.mod && mv go.sum.bak go.sum
  echo "GO_MOD_TIDY_VIOLATION: go.mod or go.sum not tidy — run 'go mod tidy'" >&2
  exit 1
fi
rm -f go.mod.bak go.sum.bak
exit 0
