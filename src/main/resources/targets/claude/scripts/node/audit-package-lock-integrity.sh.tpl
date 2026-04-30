#!/usr/bin/env bash
set -euo pipefail
# audit-package-lock-integrity.sh — Node.js: package-lock.json integrity check
# Build tool: {{BUILD_TOOL}} | Lock file: {{LOCK_FILE}}
case "${1:-}" in
  --self-check)
    command -v npm >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: npm required" >&2; exit 2; }
    exit 0 ;;
esac
[[ -f "{{LOCK_FILE}}" ]] || { echo "OPERATIONAL_ERROR: {{LOCK_FILE}} not found" >&2; exit 2; }
npm audit --audit-level=high 2>&1
exit $?
