#!/usr/bin/env bash
set -euo pipefail
# audit-execution-integrity.sh — Rule 24/27 (Execution Integrity) CI audit
# Test command: {{TEST_COMMAND}} | Coverage: {{COVERAGE_REPORT_PATH}}
case "${1:-}" in
  --self-check)
    [[ -f "audits/execution-integrity-baseline.txt" ]] || \
      { echo "OPERATIONAL_ERROR: baseline missing" >&2; exit 2; }
    exit 0 ;;
esac
exit 0
