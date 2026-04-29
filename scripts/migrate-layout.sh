#!/usr/bin/env bash
# migrate-layout.sh — idempotent v3 → v4 layout migrator (EPIC-0060, story-0060-0002)
#
# Migrates epic directories from `plans/epic-XXXX/` (v3) to
# `ai/epics/epic-XXXX-<slug>/` (v4). Reads `flowVersion` from each epic's
# execution-state.json: epics with flowVersion <= 2 are LEFT in `plans/`
# (Rule 19 / RULE-001 backward compat); epics with flowVersion 3+ or
# missing flowVersion are migrated to v4.
#
# Usage:
#   bash scripts/migrate-layout.sh --self-check
#   bash scripts/migrate-layout.sh --dry-run                  # default
#   bash scripts/migrate-layout.sh --apply
#   bash scripts/migrate-layout.sh --apply --epic 0060
#
# Exit codes (Rule 26 audit gate lifecycle):
#   0 = success
#   1 = migration error (filesystem / git mv failure)
#   2 = OPERATIONAL_ERROR (missing dependency)
#   3 = INVALID_ARGS (unknown / conflicting flags)

set -euo pipefail

# ── globals ────────────────────────────────────────────────────────────────
MODE="dry-run"
TARGET_EPIC=""
SELF_CHECK="false"
ROLLBACK_TAG="pre-layout-v4"
REPORT_PATH="governance/baselines/migration-report-2026.md"
REPO_ROOT=""

# ── argument parsing ───────────────────────────────────────────────────────
parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --self-check) SELF_CHECK="true"; shift ;;
            --dry-run)    MODE="dry-run"; shift ;;
            --apply)      MODE="apply"; shift ;;
            --epic)
                if [[ ! "${2:-}" =~ ^[0-9]{4}$ ]]; then
                    echo "INVALID_ARGS: --epic must be 4 digits, got: ${2:-}" >&2
                    exit 3
                fi
                TARGET_EPIC="$2"; shift 2 ;;
            -h|--help)
                grep '^#' "$0" | sed 's/^# \{0,1\}//'
                exit 0 ;;
            *)
                echo "INVALID_ARGS: unknown flag: $1" >&2
                exit 3 ;;
        esac
    done
}

# ── self-check ─────────────────────────────────────────────────────────────
self_check() {
    command -v jq >/dev/null 2>&1 || {
        echo "OPERATIONAL_ERROR: jq required" >&2
        exit 2
    }
    command -v git >/dev/null 2>&1 || {
        echo "OPERATIONAL_ERROR: git required" >&2
        exit 2
    }
    git rev-parse --git-dir >/dev/null 2>&1 || {
        echo "OPERATIONAL_ERROR: not a git repository" >&2
        exit 2
    }
    echo "self-check: OK (jq, git, repository present)"
    exit 0
}

# ── slug derivation ────────────────────────────────────────────────────────
# Reads epic-XXXX.md and derives a kebab-case slug from the title's last
# meaningful tokens. Fallback: epic-XXXX (no slug suffix).
derive_slug() {
    local epic_id="$1"
    local epic_md="plans/epic-${epic_id}/epic-${epic_id}.md"
    if [[ ! -f "$epic_md" ]]; then
        echo "epic-${epic_id}"
        return
    fi
    local title
    title=$(grep -m1 '^# ' "$epic_md" | sed 's/^# //; s/[—:].*$//' \
            | tr '[:upper:]' '[:lower:]' \
            | tr -cs '[:alnum:]' '-' \
            | sed 's/^-//; s/-$//')
    if [[ -z "$title" ]]; then
        echo "epic-${epic_id}"
    else
        # take last 4 tokens at most
        local short
        short=$(echo "$title" | awk -F'-' '{
            n=NF; start=(n>4?n-3:1);
            out="";
            for(i=start;i<=n;i++){out=(out==""?$i:out"-"$i)}
            print out
        }')
        echo "epic-${epic_id}-${short}"
    fi
}

# ── flow version probe ─────────────────────────────────────────────────────
# Returns flowVersion as integer; 0 when absent (treated as legacy v0/v1).
read_flow_version() {
    local epic_id="$1"
    local state="plans/epic-${epic_id}/execution-state.json"
    if [[ ! -f "$state" ]]; then
        echo "0"
        return
    fi
    local fv
    fv=$(jq -r '.flowVersion // "0"' "$state" 2>/dev/null || echo "0")
    # strip quotes, default to 0
    case "$fv" in
        "1"|1) echo "1" ;;
        "2"|2) echo "2" ;;
        "3"|3) echo "3" ;;
        "4"|4) echo "4" ;;
        *)     echo "0" ;;
    esac
}

# ── per-epic migration decision ────────────────────────────────────────────
# Side-effect: emits one line of telemetry to stdout.
process_epic() {
    local epic_id="$1"
    local source_dir="plans/epic-${epic_id}"
    local fv
    fv=$(read_flow_version "$epic_id")

    # Legacy: flowVersion <= 2 stays in plans/
    if [[ "$fv" =~ ^[0-2]$ ]]; then
        echo "[${MODE^^}] skipped: ${source_dir}/: flowVersion=${fv} (legacy, not migrating)"
        return 0
    fi

    local slug
    slug=$(derive_slug "$epic_id")
    local dest_dir="ai/epics/${slug}"

    # Idempotency: target already exists
    if [[ -d "$dest_dir" ]]; then
        echo "[${MODE^^}] already migrated: ${source_dir}/ → ${dest_dir}/ (skipping)"
        return 0
    fi

    if [[ "$MODE" == "dry-run" ]]; then
        echo "[DRY-RUN] would move: ${source_dir}/ → ${dest_dir}/"
        return 0
    fi

    # apply mode
    mkdir -p "$(dirname "$dest_dir")"
    git mv "$source_dir" "$dest_dir" 2>&1 || {
        echo "[ERROR] git mv failed for ${source_dir} → ${dest_dir}" >&2
        return 1
    }
    echo "[APPLY] moved: ${source_dir}/ → ${dest_dir}/"
    return 0
}

# ── tag creation (apply only) ──────────────────────────────────────────────
ensure_rollback_tag() {
    if git rev-parse --verify --quiet "${ROLLBACK_TAG}" >/dev/null 2>&1; then
        echo "[APPLY] tag ${ROLLBACK_TAG} already exists; skipping"
        return 0
    fi
    git tag -a "${ROLLBACK_TAG}" -m "Snapshot before EPIC-0060 layout v3→v4 migration"
    echo "[APPLY] tag ${ROLLBACK_TAG} created at $(git rev-parse "${ROLLBACK_TAG}")"
}

# ── report writer ──────────────────────────────────────────────────────────
init_report() {
    mkdir -p "$(dirname "${REPORT_PATH}")"
    if [[ -f "${REPORT_PATH}" ]]; then return 0; fi
    cat > "${REPORT_PATH}" <<'EOF'
# Migration Report — Layout v3 → v4 (EPIC-0060)

This document records the per-epic outcome of `scripts/migrate-layout.sh`.
The file is append-only; multiple runs add new sections.

EOF
}

append_report_section() {
    local timestamp
    timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    local mode_lc="${MODE}"
    cat >> "${REPORT_PATH}" <<EOF

## Run at ${timestamp} (${mode_lc})

| Epic | flowVersion | Source | Destination | Action |
| :--- | :--- | :--- | :--- | :--- |
EOF
}

append_report_row() {
    local epic="$1" fv="$2" src="$3" dest="$4" action="$5"
    echo "| ${epic} | ${fv} | ${src} | ${dest} | ${action} |" >> "${REPORT_PATH}"
}

# ── main loop ──────────────────────────────────────────────────────────────
main_loop() {
    REPO_ROOT=$(git rev-parse --show-toplevel)
    cd "${REPO_ROOT}"

    [[ "${MODE}" == "apply" ]] && ensure_rollback_tag
    init_report
    append_report_section

    local epic_dirs=()
    if [[ -n "${TARGET_EPIC}" ]]; then
        epic_dirs+=("plans/epic-${TARGET_EPIC}")
    else
        while IFS= read -r d; do
            [[ -n "$d" ]] && epic_dirs+=("$d")
        done < <(ls -d plans/epic-[0-9][0-9][0-9][0-9] 2>/dev/null || true)
    fi

    local count=0
    for source_dir in "${epic_dirs[@]}"; do
        local epic_id
        epic_id=$(basename "$source_dir" | sed 's/^epic-//')
        local fv
        fv=$(read_flow_version "$epic_id")
        local slug
        slug=$(derive_slug "$epic_id")
        local dest="ai/epics/${slug}"
        local action

        if [[ "$fv" =~ ^[0-2]$ ]]; then
            action="skipped (legacy v${fv})"
        elif [[ -d "$dest" ]]; then
            action="already-migrated"
        elif [[ "${MODE}" == "dry-run" ]]; then
            action="would-move"
        else
            action="moved"
        fi

        process_epic "$epic_id" || return 1
        append_report_row "$epic_id" "$fv" "$source_dir" "$dest" "$action"
        count=$((count + 1))
    done

    echo ""
    echo "[${MODE^^}] processed ${count} epic(s); report: ${REPORT_PATH}"
}

# ── entry ──────────────────────────────────────────────────────────────────
main() {
    parse_args "$@"
    [[ "${SELF_CHECK}" == "true" ]] && self_check
    main_loop
}

main "$@"
