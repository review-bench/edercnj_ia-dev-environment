#!/usr/bin/env bash
set -euo pipefail
# audit-epic-branches.sh — Rule 21 (Epic Branch Model) CI audit
# Build tool: {{BUILD_TOOL}}
case "${1:-}" in
  --self-check)
    command -v gh >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: gh required" >&2; exit 2; }
    exit 0 ;;
esac
exit 0
