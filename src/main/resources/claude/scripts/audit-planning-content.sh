#!/usr/bin/env bash
# audit-planning-content.sh — Camada 2 (detectivo) — Rule 24 / EPIC-0063
# Layer:      2 (CI script — detectivo, fires on PR open/sync)
# Trigger:    PR to epic/* or develop
# Exit codes: 0=OK, 1=PLANNING_CONTENT_INSUFFICIENT, 2=OPERATIONAL_ERROR
# Latency:    stateless filesystem check
#
# Validates 6 Phase 1 planning artifacts for a story have real content (not stubs).
# Heuristics per artifact type:
#   arch-story-*.md    : >= 30 non-empty lines, >= 2 H2/H3 sections, >= 1 Mermaid diagram
#   plan-story-*.md    : >= 20 non-empty lines, >= 2 sections, >= 1 TASK- reference
#   tests-story-*.md   : >= 15 non-empty lines, >= 2 sections, >= 1 Cenario/Scenario
#   tasks-story-*.md   : >= 10 non-empty lines, >= 1 TASK- reference
#   security-story-*.md: >= 10 non-empty lines
#   compliance-story-*.md: >= 10 non-empty lines
#
# Usage:
#   audit-planning-content.sh --artifact-dir <path> --story-id <id>
#   audit-planning-content.sh --self-check
#
# Escape hatch: <!-- audit-exempt-content: <reason> --> in artifact file.

set -uo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"

# ── Arg parse ─────────────────────────────────────────────────────────────────
ARTIFACT_DIR=""
STORY_ID=""
SELF_CHECK=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --artifact-dir) ARTIFACT_DIR="$2"; shift 2 ;;
        --story-id)     STORY_ID="$2"; shift 2 ;;
        --self-check)   SELF_CHECK=true; shift ;;
        *) printf 'OPERATIONAL_ERROR: unknown flag: %s\n' "$1" >&2; exit 2 ;;
    esac
done

# ── Self-check ─────────────────────────────────────────────────────────────────
if "$SELF_CHECK"; then
    command -v grep >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: grep required\n' >&2; exit 2; }
    command -v wc   >/dev/null 2>&1 || { printf 'OPERATIONAL_ERROR: wc required\n'   >&2; exit 2; }
    printf 'SELF_CHECK_OK: %s prerequisites satisfied\n' "$SCRIPT_NAME" >&2
    exit 0
fi

# ── Prereqs ────────────────────────────────────────────────────────────────────
if [[ -z "$ARTIFACT_DIR" ]]; then
    printf 'OPERATIONAL_ERROR: --artifact-dir is required\n' >&2
    exit 2
fi

if [[ -z "$STORY_ID" ]]; then
    printf 'OPERATIONAL_ERROR: --story-id is required\n' >&2
    exit 2
fi

if [[ ! -d "$ARTIFACT_DIR" ]]; then
    printf 'OPERATIONAL_ERROR: artifact directory not found: %s\n' "$ARTIFACT_DIR" >&2
    exit 2
fi

# Canonicalize path to prevent traversal
ARTIFACT_DIR_REAL="$(realpath "$ARTIFACT_DIR" 2>/dev/null || readlink -f "$ARTIFACT_DIR" 2>/dev/null || echo "$ARTIFACT_DIR")"

VIOLATIONS=()

# ── Helper: count non-empty lines ─────────────────────────────────────────────
count_nonempty() {
    local file="$1"
    local n=0
    n=$(grep -c '[^[:space:]]' "$file" 2>/dev/null) || n=0
    printf '%s' "$n"
}

# ── Helper: count pattern matches (grep -c returns exit 1 on zero matches) ────
count_pattern() {
    local pattern="$1" file="$2"
    local n=0
    n=$(grep -cE "$pattern" "$file" 2>/dev/null) || n=0
    printf '%s' "$n"
}

# ── Helper: count fixed-string matches ────────────────────────────────────────
count_fixed() {
    local pattern="$1" file="$2"
    local n=0
    n=$(grep -cF "$pattern" "$file" 2>/dev/null) || n=0
    printf '%s' "$n"
}

# ── Helper: check exempt marker ───────────────────────────────────────────────
is_exempt() {
    local file="$1"
    grep -qF '<!-- audit-exempt-content:' "$file" 2>/dev/null
}

# ── Helper: check artifact file existence ────────────────────────────────────
require_file() {
    local prefix="$1" artifact_path="$2" label="$3"
    if [[ ! -f "$artifact_path" ]]; then
        VIOLATIONS+=("MISSING_ARTIFACT: $label not found at $artifact_path")
        return 1
    fi
    return 0
}

# ── arch-story-*.md ───────────────────────────────────────────────────────────
# Heuristics: >= 30 non-empty lines, >= 2 H2/H3 sections, >= 1 Mermaid diagram
check_arch() {
    local file="$ARTIFACT_DIR_REAL/arch-$STORY_ID.md"
    require_file "arch" "$file" "arch-$STORY_ID.md" || return

    if is_exempt "$file"; then
        printf 'WARNING: audit-exempt-content on arch artifact — skipping\n' >&2
        return
    fi

    local nonempty sections mermaid_blocks
    nonempty="$(count_nonempty "$file")"
    sections="$(count_pattern '^#{2,3} ' "$file")"
    mermaid_blocks="$(count_fixed '```mermaid' "$file")"

    if [[ "$nonempty" -lt 30 ]]; then
        VIOLATIONS+=("ARCH_H1_LINES: non-empty lines=$nonempty < 30 in arch-$STORY_ID.md")
    fi
    if [[ "$sections" -lt 2 ]]; then
        VIOLATIONS+=("ARCH_H2_SECTIONS: h2/h3 sections=$sections < 2 in arch-$STORY_ID.md")
    fi
    if [[ "$mermaid_blocks" -lt 1 ]]; then
        VIOLATIONS+=("ARCH_H3_MERMAID: mermaid diagram count=$mermaid_blocks < 1 in arch-$STORY_ID.md")
    fi
}

# ── plan-story-*.md ───────────────────────────────────────────────────────────
# Heuristics: >= 20 non-empty lines, >= 2 sections, >= 1 TASK- reference
check_plan() {
    local file="$ARTIFACT_DIR_REAL/plan-$STORY_ID.md"
    require_file "plan" "$file" "plan-$STORY_ID.md" || return

    if is_exempt "$file"; then
        printf 'WARNING: audit-exempt-content on plan artifact — skipping\n' >&2
        return
    fi

    local nonempty sections task_refs
    nonempty="$(count_nonempty "$file")"
    sections="$(count_pattern '^#{2,3} ' "$file")"
    task_refs="$(count_pattern 'TASK-[0-9]' "$file")"

    if [[ "$nonempty" -lt 20 ]]; then
        VIOLATIONS+=("PLAN_H1_LINES: non-empty lines=$nonempty < 20 in plan-$STORY_ID.md")
    fi
    if [[ "$sections" -lt 2 ]]; then
        VIOLATIONS+=("PLAN_H2_SECTIONS: h2/h3 sections=$sections < 2 in plan-$STORY_ID.md")
    fi
    if [[ "$task_refs" -lt 1 ]]; then
        VIOLATIONS+=("PLAN_H3_TASKS: TASK- references=$task_refs < 1 in plan-$STORY_ID.md")
    fi
}

# ── tests-story-*.md ──────────────────────────────────────────────────────────
# Heuristics: >= 15 non-empty lines, >= 2 sections, >= 1 Cenario/Scenario
check_tests() {
    local file="$ARTIFACT_DIR_REAL/tests-$STORY_ID.md"
    require_file "tests" "$file" "tests-$STORY_ID.md" || return

    if is_exempt "$file"; then
        printf 'WARNING: audit-exempt-content on tests artifact — skipping\n' >&2
        return
    fi

    local nonempty sections scenario_refs n_scen
    nonempty="$(count_nonempty "$file")"
    sections="$(count_pattern '^#{2,3} ' "$file")"
    n_scen=0
    n_scen=$(grep -ciE 'cenario:|scenario:' "$file" 2>/dev/null) || n_scen=0
    scenario_refs="$n_scen"

    if [[ "$nonempty" -lt 15 ]]; then
        VIOLATIONS+=("TESTS_H1_LINES: non-empty lines=$nonempty < 15 in tests-$STORY_ID.md")
    fi
    if [[ "$sections" -lt 2 ]]; then
        VIOLATIONS+=("TESTS_H2_SECTIONS: h2/h3 sections=$sections < 2 in tests-$STORY_ID.md")
    fi
    if [[ "$scenario_refs" -lt 1 ]]; then
        VIOLATIONS+=("TESTS_H3_SCENARIO: Cenario/Scenario count=$scenario_refs < 1 in tests-$STORY_ID.md")
    fi
}

# ── tasks-story-*.md ──────────────────────────────────────────────────────────
# Heuristics: >= 10 non-empty lines, >= 1 TASK- reference
check_tasks() {
    local file="$ARTIFACT_DIR_REAL/tasks-$STORY_ID.md"
    require_file "tasks" "$file" "tasks-$STORY_ID.md" || return

    if is_exempt "$file"; then
        printf 'WARNING: audit-exempt-content on tasks artifact — skipping\n' >&2
        return
    fi

    local nonempty task_refs
    nonempty="$(count_nonempty "$file")"
    task_refs="$(count_pattern 'TASK-[0-9]' "$file")"

    if [[ "$nonempty" -lt 10 ]]; then
        VIOLATIONS+=("TASKS_H1_LINES: non-empty lines=$nonempty < 10 in tasks-$STORY_ID.md")
    fi
    if [[ "$task_refs" -lt 1 ]]; then
        VIOLATIONS+=("TASKS_H2_TASKS: TASK- references=$task_refs < 1 in tasks-$STORY_ID.md")
    fi
}

# ── security-story-*.md ───────────────────────────────────────────────────────
# Heuristics: >= 10 non-empty lines
check_security() {
    local file="$ARTIFACT_DIR_REAL/security-$STORY_ID.md"
    require_file "security" "$file" "security-$STORY_ID.md" || return

    if is_exempt "$file"; then
        printf 'WARNING: audit-exempt-content on security artifact — skipping\n' >&2
        return
    fi

    local nonempty
    nonempty="$(count_nonempty "$file")"

    if [[ "$nonempty" -lt 10 ]]; then
        VIOLATIONS+=("SECURITY_H1_LINES: non-empty lines=$nonempty < 10 in security-$STORY_ID.md")
    fi
}

# ── compliance-story-*.md ─────────────────────────────────────────────────────
# Heuristics: >= 10 non-empty lines
check_compliance() {
    local file="$ARTIFACT_DIR_REAL/compliance-$STORY_ID.md"
    require_file "compliance" "$file" "compliance-$STORY_ID.md" || return

    if is_exempt "$file"; then
        printf 'WARNING: audit-exempt-content on compliance artifact — skipping\n' >&2
        return
    fi

    local nonempty
    nonempty="$(count_nonempty "$file")"

    if [[ "$nonempty" -lt 10 ]]; then
        VIOLATIONS+=("COMPLIANCE_H1_LINES: non-empty lines=$nonempty < 10 in compliance-$STORY_ID.md")
    fi
}

# ── Run all checks ─────────────────────────────────────────────────────────────
check_arch
check_plan
check_tests
check_tasks
check_security
check_compliance

# ── Result ─────────────────────────────────────────────────────────────────────
if [[ ${#VIOLATIONS[@]} -eq 0 ]]; then
    printf 'AUDIT_OK: %s planning artifacts valid (story=%s dir=%s)\n' \
        "$SCRIPT_NAME" "$STORY_ID" "$(basename "$ARTIFACT_DIR_REAL")" >&2
    exit 0
fi

printf 'PLANNING_CONTENT_INSUFFICIENT: %s artifacts for %s\n' \
    "$(basename "$ARTIFACT_DIR_REAL")" "$STORY_ID" >&2
for v in "${VIOLATIONS[@]}"; do
    printf '  - %s\n' "$v" >&2
done
exit 1
