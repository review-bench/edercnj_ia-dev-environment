#!/usr/bin/env bash
# migrate-epic-v2-to-v3.sh — idempotent migration: adds Source Feature + Inherited RNFs
# to existing v2 epic markdown files that lack those v3 fields.
#
# Usage:
#   ./ai/scripts/migrate-epic-v2-to-v3.sh [--dry-run] [--epic-dir <path>]
#
# Exit codes:
#   0  OK — all epics already at v3 or successfully migrated
#   1  MIGRATION_ERROR — at least one file could not be migrated
#   2  OPERATIONAL_ERROR — missing dependencies or bad arguments
set -euo pipefail

DRY_RUN=false
EPIC_DIR="ai/epics"
MIGRATED=0
SKIPPED=0
FAILED=0

usage() {
    echo "Usage: $0 [--dry-run] [--epic-dir <path>]" >&2
    exit 2
}

case "${1:-}" in
    --self-check)
        command -v sed >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: sed required" >&2; exit 2; }
        command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep required" >&2; exit 2; }
        [[ -d "$EPIC_DIR" ]] || { echo "OPERATIONAL_ERROR: $EPIC_DIR directory not found" >&2; exit 2; }
        exit 0
        ;;
esac

while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run) DRY_RUN=true ;;
        --epic-dir) EPIC_DIR="${2:?missing value for --epic-dir}"; shift ;;
        *) usage ;;
    esac
    shift
done

[[ -d "$EPIC_DIR" ]] || { echo "OPERATIONAL_ERROR: directory not found: $EPIC_DIR" >&2; exit 2; }

SOURCE_FEATURE_FIELD="**Source Feature:** N/A  _(optional — N/A when epic was not originated from a Feature)_"
SOURCE_FEATURE_LINK_FIELD="**Source Feature Link:** —"

INHERITED_RNFS_SECTION='## 0.6 Inherited RNFs

> **Read-only — inherited from Feature → Capability → Product chain.**
> These non-functional requirements are propagated from the source feature declared above (field `Source Feature`).
> They cannot be overridden at the epic level — contact the product owner to change them at source.
> If this epic has no source feature (`Source Feature: N/A`), mark all rows as `(none)`.

| RNF ID | Source Level | Requirement | Waivable? |
| :--- | :--- | :--- | :--- |
| (none — source feature N/A) | — | — | — |

---

'

migrate_file() {
    local file="$1"

    # Skip if Source Feature field already present (idempotent)
    if grep -q "^\*\*Source Feature:\*\*" "$file" 2>/dev/null; then
        echo "SKIP (already v3): $file"
        ((SKIPPED++)) || true
        return 0
    fi

    echo "MIGRATE: $file"
    if [[ "$DRY_RUN" == "true" ]]; then
        ((MIGRATED++)) || true
        return 0
    fi

    # Insert Source Feature + Source Feature Link after the Status line
    sed -i.bak \
        "s|^\(\*\*Status:\*\*.*\)$|\1\n${SOURCE_FEATURE_FIELD}\n${SOURCE_FEATURE_LINK_FIELD}|" \
        "$file"

    # Insert ## 0.6 Inherited RNFs section before ## 1. Visão & Problema
    python3 - "$file" <<'PYEOF'
import sys, re

path = sys.argv[1]
with open(path) as f:
    content = f.read()

inherited_section = """## 0.6 Inherited RNFs

> **Read-only — inherited from Feature → Capability → Product chain.**
> These non-functional requirements are propagated from the source feature declared above (field `Source Feature`).
> They cannot be overridden at the epic level — contact the product owner to change them at source.
> If this epic has no source feature (`Source Feature: N/A`), mark all rows as `(none)`.

| RNF ID | Source Level | Requirement | Waivable? |
| :--- | :--- | :--- | :--- |
| (none — source feature N/A) | — | — | — |

---

"""

marker = "## 1. Visão & Problema"
if marker in content and "## 0.6 Inherited RNFs" not in content:
    content = content.replace(marker, inherited_section + marker, 1)

with open(path, 'w') as f:
    f.write(content)
PYEOF

    # Remove backup
    rm -f "${file}.bak"
    ((MIGRATED++)) || true
}

# Find all epic markdown files (top-level epic.md or epic-XXXX.md patterns)
while IFS= read -r -d '' file; do
    migrate_file "$file" || ((FAILED++)) || true
done < <(find "$EPIC_DIR" -maxdepth 3 -name "epic-*.md" -print0 2>/dev/null)

echo ""
echo "Migration complete: migrated=$MIGRATED skipped=$SKIPPED failed=$FAILED"
[[ "$DRY_RUN" == "true" ]] && echo "(dry-run — no files were modified)"

[[ $FAILED -eq 0 ]] && exit 0 || exit 1
