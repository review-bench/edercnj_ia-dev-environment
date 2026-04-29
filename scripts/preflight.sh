#!/usr/bin/env bash
# preflight.sh — Camada 0 Pre-Flight Runner (EPIC-0063 story-0063-0001)
#
# Single deterministic entry-point for local pre-flight validation.
# Orchestrates up to 12 gates in lightweight-first order:
#   5(format) → 3(review-content) → 4(telemetry) → 9(planning) →
#   7(grammar) → 6(self-check) → 10(hooks-integrity) →
#   1(tests) → 2(coverage) → 8(wave-dispatch) → 11(chain) → 12(phase-gates)
#
# Exit codes:
#   0  SUCCESS
#   1  TESTS_FAILED
#   2  COVERAGE_BELOW_THRESHOLD
#   3  REVIEW_CONTENT_INSUFFICIENT
#   4  TELEMETRY_EVIDENCE_MISSING
#   5  FORMAT_VIOLATION
#   6  AUDIT_SELF_CHECK_FAILED
#   7  GRAMMAR_VIOLATION           (stub — story-0063-0012)
#   8  WAVE_DISPATCH_INCOMPLETE    (stub — story-0063-0014)
#   9  PLANNING_CONTENT_INSUFFICIENT (stub — story-0063-0015)
#  10  HOOKS_INTEGRITY_FAILED      (stub — story-0063-0018)
#  11  TELEMETRY_CHAIN_BROKEN      (stub — story-0063-0019)
#  12  PHASE_GATE_AUDIT_FAILED
#  13  INVALID_ARGS
#  14  OPERATIONAL_ERROR

set -uo pipefail

# ── Constants ──────────────────────────────────────────────────────────────────
readonly E_SUCCESS=0
readonly E_TESTS_FAILED=1
readonly E_COVERAGE=2
readonly E_REVIEW_CONTENT=3
readonly E_TELEMETRY=4
readonly E_FORMAT=5
readonly E_SELF_CHECK=6
readonly E_GRAMMAR=7
readonly E_WAVE_DISPATCH=8
readonly E_PLANNING=9
readonly E_HOOKS=10
readonly E_CHAIN=11
readonly E_PHASE_GATES=12
readonly E_INVALID_ARGS=13
readonly E_OPERATIONAL=14

SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPTS_DIR}/.." && pwd)"
CLAUDE_SCRIPTS="${REPO_ROOT}/.claude/scripts"

# ── Args ───────────────────────────────────────────────────────────────────────
SCOPE=""
STORY_ID=""
EPIC_ID=""
SELF_CHECK=false

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --scope=*)   SCOPE="${1#--scope=}"; shift ;;
            --scope)     SCOPE="$2"; shift 2 ;;
            --story-id=*) STORY_ID="${1#--story-id=}"; shift ;;
            --story-id)  STORY_ID="$2"; shift 2 ;;
            --epic-id=*) EPIC_ID="${1#--epic-id=}"; shift ;;
            --epic-id)   EPIC_ID="$2"; shift 2 ;;
            --self-check) SELF_CHECK=true; shift ;;
            --help|-h)   print_help; exit 0 ;;
            *) printf 'PREFLIGHT_FAILED: INVALID_ARGS — unknown flag: %s\n' "$1" >&2
               exit $E_INVALID_ARGS ;;
        esac
    done
}

print_help() {
    printf 'Usage: preflight.sh --scope=story --story-id=XXXX-YYYY\n' >&2
    printf '       preflight.sh --scope=epic --epic-id=XXXX\n' >&2
    printf '       preflight.sh --self-check\n' >&2
}

validate_args() {
    if [[ -z "$SCOPE" ]]; then
        printf 'PREFLIGHT_FAILED: INVALID_ARGS — missing required --scope\n' >&2
        exit $E_INVALID_ARGS
    fi
    if [[ "$SCOPE" == "story" && -z "$STORY_ID" ]]; then
        printf 'PREFLIGHT_FAILED: INVALID_ARGS — missing required --story-id for scope=story\n' >&2
        exit $E_INVALID_ARGS
    fi
    if [[ "$SCOPE" == "epic" && -z "$EPIC_ID" ]]; then
        printf 'PREFLIGHT_FAILED: INVALID_ARGS — missing required --epic-id for scope=epic\n' >&2
        exit $E_INVALID_ARGS
    fi
    if [[ "$SCOPE" != "story" && "$SCOPE" != "epic" ]]; then
        printf 'PREFLIGHT_FAILED: INVALID_ARGS — --scope must be story or epic\n' >&2
        exit $E_INVALID_ARGS
    fi
    normalize_epic_id
}

# Normalize EPIC_ID so downstream path expansions never produce
# double-prefixed paths like "plans/epic-epic-0074". Accepts both bare
# numbers ("0074") and prefixed forms ("epic-0074", "EPIC-0074") emitted
# by callers such as enforce-preflight-gates.sh. Derives EPIC_ID from
# STORY_ID (XXXX-YYYY -> XXXX) when scope=story leaves EPIC_ID empty.
normalize_epic_id() {
    if [[ -z "$EPIC_ID" && "$SCOPE" == "story" && "$STORY_ID" =~ ^([0-9]{4})- ]]; then
        EPIC_ID="${BASH_REMATCH[1]}"
    fi
    [[ -n "$EPIC_ID" ]] || return 0
    # strip case-insensitive "epic-" prefix if present
    EPIC_ID="${EPIC_ID#epic-}"
    EPIC_ID="${EPIC_ID#EPIC-}"
}

# ── Self-check ─────────────────────────────────────────────────────────────────
run_self_check() {
    local failed=0
    for tool in mvn jq git; do
        command -v "$tool" >/dev/null 2>&1 || {
            printf 'PREFLIGHT_SELF_CHECK: %s not found on PATH\n' "$tool" >&2
            failed=1
        }
    done
    for script in audit-review-content.sh audit-execution-integrity.sh audit-coverage-local.sh; do
        local path="${CLAUDE_SCRIPTS}/${script}"
        if [[ -x "$path" ]]; then
            "$path" --self-check >/dev/null 2>&1 || {
                printf 'PREFLIGHT_SELF_CHECK: %s --self-check failed\n' "$script" >&2
                failed=1
            }
        fi
    done
    [[ $failed -eq 0 ]] || exit $E_SELF_CHECK
    printf 'PREFLIGHT_SELF_CHECK_OK: all prerequisites satisfied\n' >&2
    exit 0
}

# ── Gate runners ───────────────────────────────────────────────────────────────

# Resolves the base ref for the Java-diff probe. Echoes the ref name on stdout.
# Exits with E_OPERATIONAL when neither origin/develop nor HEAD@{upstream} is
# resolvable — must never silently degrade to "no Java diff", which would let
# Java gates be skipped on a misconfigured remote (shallow clone, missing
# fetch, branch with no upstream, etc.).
resolve_java_diff_base_ref() {
    if git -C "${REPO_ROOT}" rev-parse --verify --quiet origin/develop >/dev/null; then
        printf '%s\n' 'origin/develop'
        return 0
    fi
    if git -C "${REPO_ROOT}" rev-parse --verify --quiet 'HEAD@{upstream}' >/dev/null; then
        git -C "${REPO_ROOT}" rev-parse --abbrev-ref 'HEAD@{upstream}'
        return 0
    fi
    printf 'PREFLIGHT_FAILED: OPERATIONAL_ERROR — unable to resolve git base ref (origin/develop or HEAD@{upstream})\n' >&2
    exit $E_OPERATIONAL
}

# Returns 0 if the branch contains any change under java/ vs the resolved base
# ref; non-zero otherwise. Used to skip Java-only gates on documentation /
# refinement PRs that touch nothing under the Java module. Matches the entire
# java/ subtree (not only .java + pom.xml) so changes to src/main/resources/,
# build configs, and other Java-module assets still trigger format + test
# gates as expected.
java_sources_changed() {
    local base_ref
    local changed_files

    base_ref="$(resolve_java_diff_base_ref)"
    if ! changed_files="$(git -C "${REPO_ROOT}" diff --name-only "${base_ref}...HEAD" 2>&1)"; then
        printf 'PREFLIGHT_FAILED: OPERATIONAL_ERROR — git diff failed for base ref %s\n' "${base_ref}" >&2
        exit $E_OPERATIONAL
    fi

    grep -E '^java/' >/dev/null <<<"${changed_files}"
}

run_gate_format() {
    printf '[Gate 5] format check\n' >&2
    # Repo layout: pom.xml lives under java/, not at the repo root. Skip the gate
    # when java/pom.xml is absent (e.g., docs-only branches in nested checkouts).
    local java_pom="${REPO_ROOT}/java/pom.xml"
    if [[ ! -f "${java_pom}" ]]; then
        printf '[Gate 5] no java/pom.xml — skipping format check\n' >&2
        return 0
    fi
    if ! java_sources_changed; then
        printf '[Gate 5] no Java source changes vs origin/develop — skipping format check\n' >&2
        return 0
    fi
    mvn -f "${java_pom}" spotless:check -q 2>/dev/null || {
        printf 'PREFLIGHT_FAILED: FORMAT_VIOLATION — fix: (cd java && mvn spotless:apply)\n' >&2
        exit $E_FORMAT
    }
}

run_gate_review_content() {
    printf '[Gate 3] review content audit\n' >&2
    local review_file
    review_file=$(find "${REPO_ROOT}/plans" "${REPO_ROOT}/ai/epics" \
        -name "review-story-${STORY_ID}.md" -o \
        -name "review-story-story-${STORY_ID}.md" 2>/dev/null | head -1 || true)
    [[ -z "$review_file" ]] && { printf '[Gate 3] review file not found — skip\n' >&2; return 0; }
    "${CLAUDE_SCRIPTS}/audit-review-content.sh" --review-file "$review_file" 2>/dev/null || {
        printf 'PREFLIGHT_FAILED: REVIEW_CONTENT_INSUFFICIENT — fix: invoke /x-review for %s\n' "$STORY_ID" >&2
        exit $E_REVIEW_CONTENT
    }
}

run_gate_telemetry() {
    printf '[Gate 4] telemetry evidence audit\n' >&2
    # Scope the lookup to the specific epic being preflight'd. A bare repo-wide
    # find would pick up any events.ndjson (e.g., plans/unknown/telemetry/) and
    # audit it for the current story — guaranteed false-positive on chore PRs
    # that touch a fresh epic with no telemetry yet.
    local ndjson
    ndjson=$(find "${REPO_ROOT}/plans/epic-${EPIC_ID}" \
                  "${REPO_ROOT}/ai/epics/epic-${EPIC_ID}-"* \
        -name "events.ndjson" -path "*/telemetry/*" 2>/dev/null | head -1 || true)
    [[ -z "$ndjson" ]] && { printf '[Gate 4] no telemetry file for epic-%s — skip\n' "$EPIC_ID" >&2; return 0; }
    "${CLAUDE_SCRIPTS}/audit-execution-integrity.sh" \
        --scope=telemetry --story-id="story-${STORY_ID}" --ndjson-file "$ndjson" 2>/dev/null || {
        printf 'PREFLIGHT_FAILED: TELEMETRY_EVIDENCE_MISSING — fix: ensure x-review was invoked\n' >&2
        exit $E_TELEMETRY
    }
}

run_gate_self_check_audit() {
    printf '[Gate 6] audit self-checks\n' >&2
    for script in audit-review-content.sh audit-execution-integrity.sh audit-coverage-local.sh; do
        local path="${CLAUDE_SCRIPTS}/${script}"
        [[ -x "$path" ]] && "$path" --self-check 2>/dev/null || {
            printf 'PREFLIGHT_FAILED: AUDIT_SELF_CHECK_FAILED — %s\n' "$script" >&2
            exit $E_SELF_CHECK
        }
    done
}

run_gate_tests() {
    printf '[Gate 1] mvn test\n' >&2
    local java_pom="${REPO_ROOT}/java/pom.xml"
    if [[ ! -f "${java_pom}" ]]; then
        printf '[Gate 1] no java/pom.xml — skipping mvn test\n' >&2
        return 0
    fi
    if ! java_sources_changed; then
        printf '[Gate 1] no Java source changes vs origin/develop — skipping mvn test\n' >&2
        return 0
    fi
    mvn -f "${java_pom}" test -q 2>/dev/null || {
        printf 'PREFLIGHT_FAILED: TESTS_FAILED — fix: (cd java && mvn test) to see failures\n' >&2
        exit $E_TESTS_FAILED
    }
}

run_gate_coverage() {
    printf '[Gate 2] coverage check\n' >&2
    local csv="${REPO_ROOT}/java/target/site/jacoco/jacoco.csv"
    [[ -f "$csv" ]] || { printf '[Gate 2] jacoco.csv not found — skip\n' >&2; return 0; }
    "${CLAUDE_SCRIPTS}/audit-coverage-local.sh" \
        --report-path "$csv" --story-id="story-${STORY_ID}" 2>/dev/null || {
        printf 'PREFLIGHT_FAILED: COVERAGE_BELOW_THRESHOLD — fix: add tests\n' >&2
        exit $E_COVERAGE
    }
}

run_gate_phase_gates() {
    printf '[Gate 12] phase gate audit\n' >&2
    local script="${REPO_ROOT}/scripts/audit-task-hierarchy.sh"
    [[ -x "$script" ]] && "$script" 2>/dev/null || {
        printf '[Gate 12] audit-task-hierarchy.sh not found — skip\n' >&2
    }
}

stub_gate() { local name="$1" exit_code="$2"
    printf '[Gate %s] STUB — will be active after downstream story\n' "$name" >&2
}

# ── Main dispatch ──────────────────────────────────────────────────────────────
dispatch_gates() {
    # Ordering: 5→3→4→9→7→6→10→1→2→8→11→12
    run_gate_format
    run_gate_review_content
    run_gate_telemetry
    stub_gate "9" $E_PLANNING
    stub_gate "7" $E_GRAMMAR
    run_gate_self_check_audit
    stub_gate "10" $E_HOOKS
    run_gate_tests
    run_gate_coverage
    stub_gate "8" $E_WAVE_DISPATCH
    stub_gate "11" $E_CHAIN
    run_gate_phase_gates
    printf 'PREFLIGHT_OK: all gates passed\n' >&2
}

# ── Entry point ────────────────────────────────────────────────────────────────
parse_args "$@"

if "$SELF_CHECK"; then
    run_self_check
fi

validate_args
dispatch_gates
exit $E_SUCCESS
