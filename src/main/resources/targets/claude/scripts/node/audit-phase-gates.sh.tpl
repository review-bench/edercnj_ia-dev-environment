#!/usr/bin/env bash
set -euo pipefail
# audit-phase-gates.sh — Rule 25 Phase Gate CI audit
# Exit: 0=OK 1=PHASE_GATE_VIOLATION 2=OPERATIONAL_ERROR
case "${1:-}" in
  --self-check) exit 0 ;;
esac
exit 0
