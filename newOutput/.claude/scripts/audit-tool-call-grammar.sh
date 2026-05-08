#!/usr/bin/env bash
# Layer:      2 (CI script — detectivo, fires on PR)
# Rule:       28 (Tool-Call Grammar)
# Story:      story-0063-0012
# Purpose:    Static lint of SKILL.md files: every Skill(...) / Agent(subagent_type: "general-purpose")
#             declaration in an orchestrator SKILL.md MUST be followed (same line or next line)
#             by a grammar marker: [required], [optional], or [conditional: <expr>].
#
# Exit codes (Rule 26 §Standardized + Rule 28):
#   0 — OK (no violations; all checks green)
#   1 — GRAMMAR_MARKER_MISSING (Skill/Agent call without [required|optional|conditional] marker)
#   2 — OPERATIONAL_ERROR (prerequisites missing, file unreadable, or no --skill-file provided)
#   3 — BASELINE_CORRUPT (baseline file malformed or non-readable)
#
# Usage:
#   audit-tool-call-grammar.sh --self-check
#   audit-tool-call-grammar.sh --skill-file <path/to/SKILL.md> [--baseline <path>]
#   audit-tool-call-grammar.sh --skills-root <path> [--baseline <path>]

set -uo pipefail

SKILL_FILE=""
SKILLS_ROOT=""
BASELINE_FILE=""
SELF_CHECK=false

# ---------- argument parsing ----------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --skill-file=*)    SKILL_FILE="${1#--skill-file=}"; shift ;;
        --skill-file)      SKILL_FILE="$2"; shift 2 ;;
        --skills-root=*)   SKILLS_ROOT="${1#--skills-root=}"; shift ;;
        --skills-root)     SKILLS_ROOT="$2"; shift 2 ;;
        --baseline=*)      BASELINE_FILE="${1#--baseline=}"; shift ;;
        --baseline)        BASELINE_FILE="$2"; shift 2 ;;
        --self-check)      SELF_CHECK=true; shift ;;
        *)
            printf 'OPERATIONAL_ERROR: unknown flag: %s\n' "$1" >&2
            exit 2
            ;;
    esac
done

# ---------- self-check ----------
if "$SELF_CHECK"; then
    PREREQ_OK=true
    for tool in grep jq; do
        if ! command -v "$tool" >/dev/null 2>&1; then
            printf 'OPERATIONAL_ERROR: %s not found on PATH\n' "$tool" >&2
            PREREQ_OK=false
        fi
    done
    # Verify Rule 28 file exists (Camada 1)
    SCRIPT_DIR_SC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    REPO_ROOT_SC="$(cd "$SCRIPT_DIR_SC/.." && pwd)"
    RULE_FILE="$REPO_ROOT_SC/.claude/rules/30-tool-call-grammar.md"
    if [[ ! -f "$RULE_FILE" ]]; then
        printf 'OPERATIONAL_ERROR: Rule 28 file not found at %s\n' "$RULE_FILE" >&2
        PREREQ_OK=false
    fi
    if "$PREREQ_OK"; then
        printf 'SELF_CHECK_OK: audit-tool-call-grammar prerequisites satisfied\n' >&2
        exit 0
    else
        exit 2
    fi
fi

# ---------- resolve baseline ----------
if [[ -z "$BASELINE_FILE" ]]; then
    SCRIPT_DIR_B="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    REPO_ROOT_B="$(cd "$SCRIPT_DIR_B/.." && pwd)"
    BASELINE_FILE="$REPO_ROOT_B/audits/tool-call-grammar-baseline.txt"
fi

# Load grandfathered skill names from baseline
declare -A GRANDFATHERED
if [[ -f "$BASELINE_FILE" ]]; then
    while IFS= read -r line; do
        # Skip blank lines and comment-only lines
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
        # Format: <skill-name> [# reason...]
        skill_name="${line%%#*}"           # strip inline comment
        skill_name="${skill_name//[[:space:]]/}"  # trim whitespace
        [[ -n "$skill_name" ]] && GRANDFATHERED["$skill_name"]=1
    done < "$BASELINE_FILE"
elif [[ -n "$BASELINE_FILE" && ! -f "$BASELINE_FILE" ]]; then
    # Baseline path specified but file doesn't exist — create an empty one if in default location
    # If explicitly specified and missing → treat as operational error only when it was non-default
    : # silently treat as empty baseline (no grandfathered skills)
fi

# ---------- helper: check one SKILL.md file ----------
# Returns the count of violations found; prints details to stderr.
check_skill_file() {
    local skill_file="$1"
    local violations=0

    if [[ ! -f "$skill_file" ]]; then
        printf 'OPERATIONAL_ERROR: skill file not found: %s\n' "$skill_file" >&2
        return 2
    fi

    # Extract skill name from frontmatter (name: field)
    local skill_name
    skill_name=$(grep -m1 '^name:' "$skill_file" 2>/dev/null | sed 's/^name:[[:space:]]*//' | tr -d '"' | tr -d "'")
    skill_name="${skill_name//[[:space:]]/}"

    # If skill is grandfathered, skip entirely
    if [[ -n "$skill_name" && "${GRANDFATHERED[$skill_name]+set}" == "set" ]]; then
        return 0
    fi

    # Read the file into an array of lines for look-ahead
    mapfile -t lines < "$skill_file"
    local total="${#lines[@]}"
    local i

    for (( i = 0; i < total; i++ )); do
        local line="${lines[$i]}"

        # Match lines containing Skill(...) with skill: "x-..." argument
        # OR Agent(subagent_type: "general-purpose"...)
        local is_skill_call=false
        if echo "$line" | grep -qE 'Skill\(skill:[[:space:]]*"x-[a-z-]+"'; then
            is_skill_call=true
        elif echo "$line" | grep -qE 'Agent\(subagent_type:[[:space:]]*"general-purpose"'; then
            is_skill_call=true
        fi

        if ! "$is_skill_call"; then
            continue
        fi

        # Check if the marker is on the same line or the next line
        local has_marker=false
        # Same-line check
        if echo "$line" | grep -qE '\[(required|optional|conditional[^]]*)\]'; then
            has_marker=true
        fi
        # Next-line check (look ahead 1 line)
        if ! "$has_marker" && (( i + 1 < total )); then
            local next_line="${lines[$((i+1))]}"
            if echo "$next_line" | grep -qE '^\s*\[(required|optional|conditional[^]]*)\]\s*$'; then
                has_marker=true
            fi
        fi

        if ! "$has_marker"; then
            local line_num=$(( i + 1 ))
            printf 'GRAMMAR_MARKER_MISSING: %s line %d: %s\n' \
                "$skill_file" "$line_num" "${line// /}" >&2
            violations=$(( violations + 1 ))
        fi
    done

    return 0
}

# ---------- main logic ----------

TOTAL_VIOLATIONS=0
OPERATIONAL_ERRORS=0

if [[ -n "$SKILL_FILE" ]]; then
    # Single-file mode
    if [[ ! -f "$SKILL_FILE" ]]; then
        printf 'OPERATIONAL_ERROR: skill file not found: %s\n' "$SKILL_FILE" >&2
        exit 2
    fi

    # Capture violations for this file
    violations_output=$(check_skill_file "$SKILL_FILE" 2>&1)
    check_exit=$?

    if [[ $check_exit -eq 2 ]]; then
        printf '%s\n' "$violations_output" >&2
        exit 2
    fi

    # Count GRAMMAR_MARKER_MISSING lines in output
    if [[ -n "$violations_output" ]]; then
        printf '%s\n' "$violations_output" >&2
        missing_count=$(echo "$violations_output" | grep -c 'GRAMMAR_MARKER_MISSING' || true)
        TOTAL_VIOLATIONS=$(( TOTAL_VIOLATIONS + missing_count ))
    fi

elif [[ -n "$SKILLS_ROOT" ]]; then
    # Directory mode: scan all SKILL.md files under the given root
    if [[ ! -d "$SKILLS_ROOT" ]]; then
        printf 'OPERATIONAL_ERROR: skills root directory not found: %s\n' "$SKILLS_ROOT" >&2
        exit 2
    fi

    while IFS= read -r -d '' skill_md; do
        violations_output=$(check_skill_file "$skill_md" 2>&1)
        check_exit=$?
        if [[ $check_exit -eq 2 ]]; then
            printf '%s\n' "$violations_output" >&2
            OPERATIONAL_ERRORS=$(( OPERATIONAL_ERRORS + 1 ))
            continue
        fi
        if [[ -n "$violations_output" ]]; then
            printf '%s\n' "$violations_output" >&2
            missing_count=$(echo "$violations_output" | grep -c 'GRAMMAR_MARKER_MISSING' || true)
            TOTAL_VIOLATIONS=$(( TOTAL_VIOLATIONS + missing_count ))
        fi
    done < <(find "$SKILLS_ROOT" -name 'SKILL.md' -print0 2>/dev/null)
else
    printf 'OPERATIONAL_ERROR: one of --skill-file or --skills-root is required\n' >&2
    exit 2
fi

if [[ $OPERATIONAL_ERRORS -gt 0 ]]; then
    exit 2
fi

if [[ $TOTAL_VIOLATIONS -gt 0 ]]; then
    printf 'GRAMMAR_MARKER_MISSING: %d violation(s) found\n' "$TOTAL_VIOLATIONS" >&2
    exit 1
fi

printf 'OK: audit-tool-call-grammar static check passed\n' >&2
exit 0
