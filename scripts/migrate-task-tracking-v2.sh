#!/usr/bin/env bash
# scripts/migrate-task-tracking-v2.sh
#
# Migrates execution-state.json files with flowVersion=2 to have taskTracking.enabled=true.
# Required pre-requisite before activating audit-flow-version.sh enforcement in CI.
#
# Usage:
#   scripts/migrate-task-tracking-v2.sh [--dry-run] [--plans-root <path>]
#
# Exit codes:
#   0  Migration completed (or no files needed migration)
#   1  Migration failed for one or more files
#   2  Operational error (e.g., jq not on PATH)
#
# EPIC-0059 (Zero-Bypass Lifecycle Enforcement) — story-0059-0012

set -euo pipefail

# ── self-check ────────────────────────────────────────────────────────────────
case "${1:-}" in
  --self-check)
    command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required on PATH" >&2; exit 2; }
    exit 0
    ;;
esac

# ── argument parsing ──────────────────────────────────────────────────────────
DRY_RUN=false
PLANS_ROOT="plans"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --plans-root)
      PLANS_ROOT="$2"
      shift 2
      ;;
    --plans-root=*)
      PLANS_ROOT="${1#--plans-root=}"
      shift
      ;;
    *)
      echo "OPERATIONAL_ERROR: Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

# ── dependency checks ─────────────────────────────────────────────────────────
command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required on PATH" >&2; exit 2; }

if [[ ! -d "$PLANS_ROOT" ]]; then
  echo "OPERATIONAL_ERROR: Plans root directory not found: $PLANS_ROOT" >&2
  exit 2
fi

# ── migration ─────────────────────────────────────────────────────────────────
MIGRATED=0
SKIPPED=0
FAILED=0

echo "Scanning ${PLANS_ROOT}/epic-*/execution-state.json for flowVersion=2 files..."

while IFS= read -r -d '' state_file; do
  # Read flowVersion — default to "1" if absent
  flow_version=$(jq -r '.flowVersion // "1"' "$state_file" 2>/dev/null) || {
    echo "WARN: Failed to parse $state_file — skipping" >&2
    ((FAILED++)) || true
    continue
  }

  # Only process flowVersion=2 files
  if [[ "$flow_version" != "2" ]]; then
    continue
  fi

  # Check taskTracking status
  task_tracking_enabled=$(jq -r '.taskTracking.enabled // "ABSENT"' "$state_file" 2>/dev/null) || {
    echo "WARN: Failed to read taskTracking from $state_file — skipping" >&2
    ((FAILED++)) || true
    continue
  }

  # Already correctly configured — idempotent skip
  if [[ "$task_tracking_enabled" == "true" ]]; then
    echo "  [skip]     $state_file — taskTracking.enabled=true already set"
    ((SKIPPED++)) || true
    continue
  fi

  # Needs migration
  echo "  [migrate]  $state_file — taskTracking.enabled=${task_tracking_enabled} → true"

  if [[ "$DRY_RUN" == "true" ]]; then
    ((MIGRATED++)) || true
    continue
  fi

  # Write updated file atomically via temp file
  tmp_file="${state_file}.migrate.tmp"

  if jq '.taskTracking = (.taskTracking // {}) | .taskTracking.enabled = true' "$state_file" > "$tmp_file" 2>/dev/null; then
    mv "$tmp_file" "$state_file"
    ((MIGRATED++)) || true
  else
    rm -f "$tmp_file"
    echo "ERROR: Failed to migrate $state_file" >&2
    ((FAILED++)) || true
  fi

done < <(find "$PLANS_ROOT" -name "execution-state.json" -print0 | sort -z)

# ── summary ───────────────────────────────────────────────────────────────────
echo ""
echo "Migration complete:"
if [[ "$DRY_RUN" == "true" ]]; then
  echo "  Would migrate: ${MIGRATED} files"
else
  echo "  Migrated:  ${MIGRATED} files"
fi
echo "  Skipped (already correct): ${SKIPPED} files"
echo "  Failed:    ${FAILED} files"

if [[ "$FAILED" -gt 0 ]]; then
  echo "" >&2
  echo "ERROR: ${FAILED} file(s) failed migration. Check output above." >&2
  exit 1
fi

echo ""
echo "Next step: run scripts/audit-flow-version.sh to verify all flowVersion=2 files pass."
exit 0
