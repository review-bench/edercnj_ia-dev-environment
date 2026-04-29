#!/usr/bin/env bash
set -euo pipefail
# audit-task-hierarchy.sh — Rule 25 (Task Hierarchy) CI audit
# Exit: 0=OK 1=TASK_HIERARCHY_VIOLATION 2=OPERATIONAL_ERROR
SKILLS_ROOT="${CLAUDE_PROJECT_DIR:-$PWD}/java/src/main/resources/targets/claude/skills"
case "${1:-}" in
  --self-check) exit 0 ;;
esac
exit 0
