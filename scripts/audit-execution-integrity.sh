#!/usr/bin/env bash
# audit-execution-integrity.sh — Camada 3 of Rule 24 enforcement.
#
# Scans git history for merged story PRs (branches matching feat/story-*)
# and verifies that each merged story has the mandatory evidence artifacts
# produced by the x-story-implement pipeline:
#
# Phase 1 artifacts (x-internal-story-build-plan wave — EPIC-0059):
#   - plans/epic-XXXX/plans/arch-story-STORY-ID.md        (x-arch-plan)
#   - plans/epic-XXXX/plans/plan-story-STORY-ID.md        (x-internal-story-build-plan)
#   - plans/epic-XXXX/plans/tests-story-STORY-ID.md       (x-test-plan)
#   - plans/epic-XXXX/plans/tasks-story-STORY-ID.md       (x-lib-task-decomposer)
#   - plans/epic-XXXX/plans/security-story-STORY-ID.md    (x-threat-model)
#   - plans/epic-XXXX/plans/compliance-story-STORY-ID.md  (compliance assessment)
#
# Phase 3 artifacts (x-story-implement verification wave):
#   - plans/epic-XXXX/reports/verify-envelope-STORY-ID.json  (x-internal-story-verify)
#   - plans/epic-XXXX/plans/review-story-STORY-ID.md         (x-review)
#   - plans/epic-XXXX/plans/techlead-review-story-STORY-ID.md (x-review-pr)
#   - plans/epic-XXXX/reports/story-completion-report-STORY-ID.md (x-internal-story-report)
#
# Grandfathered stories (merged before Rule 24) listed in
# audits/execution-integrity-baseline.txt are exempted. Per-story exemption
# via `<!-- audit-exempt: reason -->` in the story markdown.
#
# Exit codes:
#   0 — OK
#   1 — EIE_EVIDENCE_MISSING
#   2 — EIE_BASELINE_CORRUPT
#   3 — EIE_INVALID_EXEMPTION
#   4 — EIE_ENFORCEMENT_BROKEN (self-check failure)
#
# Usage:
#   scripts/audit-execution-integrity.sh                       # audit all merged stories
#   scripts/audit-execution-integrity.sh --self-check          # verify enforcement is wired
#   scripts/audit-execution-integrity.sh --since <ref>         # audit merges since git ref
#   scripts/audit-execution-integrity.sh --scope=fase1         # audit Phase 1 artifacts only
#   scripts/audit-execution-integrity.sh --scope=fase3         # audit Phase 3 artifacts only

set -u

# Allow REPO_ROOT override for smoke-test isolation (AUDIT_TEST_STORY_IDS usage)
REPO_ROOT="${REPO_ROOT:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
cd "${REPO_ROOT}"

BASELINE_FILE="audits/execution-integrity-baseline.txt"
RULE_FILE=".claude/rules/24-execution-integrity.md"
HOOK_FILE=".claude/hooks/verify-story-completion.sh"
# Source-of-truth fallback paths used when the runtime .claude/ tree is
# absent (CI checkouts — .claude/ is gitignored as a generated output).
RULE_SOT="java/src/main/resources/targets/claude/rules/24-execution-integrity.md"
HOOK_SOT="java/src/main/resources/targets/claude/hooks/verify-story-completion.sh"

# EPIC-0059: Phase 1 mandatory planning artifacts (x-internal-story-build-plan wave).
# Template tokens:
#   EPIC_ID  — 4-digit epic number (e.g., 0059)
#   STORY_ID — numeric story suffix without the "story-" prefix (e.g., 0059-0001)
# Array must have exactly 6 entries — self-check enforces this.
REQUIRED_PHASE_1_ARTIFACT_TEMPLATES=(
    "plans/epic-EPIC_ID/plans/arch-story-STORY_ID.md"
    "plans/epic-EPIC_ID/plans/plan-story-STORY_ID.md"
    "plans/epic-EPIC_ID/plans/tests-story-STORY_ID.md"
    "plans/epic-EPIC_ID/plans/tasks-story-STORY_ID.md"
    "plans/epic-EPIC_ID/plans/security-story-STORY_ID.md"
    "plans/epic-EPIC_ID/plans/compliance-story-STORY_ID.md"
)

# Audit scope: "full" (default) | "fase1" | "fase3" | "telemetry"
AUDIT_SCOPE="full"

# Mandatory x-story-implement phases to verify in events.ndjson (EPIC-0059 story-0059-0008)
REQUIRED_TELEMETRY_PHASES=("Phase-0-Prepare" "Phase-1-Plan" "Phase-2-Implement" "Phase-3-Verify")

self_check() {
    local broken=0
    if [[ ! -f "${RULE_FILE}" && ! -f "${RULE_SOT}" ]]; then
        echo "SELF_CHECK_FAIL: ${RULE_FILE} (and SOT ${RULE_SOT}) missing" >&2
        broken=1
    fi
    if [[ ! -f "${HOOK_FILE}" && ! -f "${HOOK_SOT}" ]]; then
        echo "SELF_CHECK_FAIL: ${HOOK_FILE} (and SOT ${HOOK_SOT}) missing" >&2
        broken=1
    fi
    if [[ ! -f "${BASELINE_FILE}" ]]; then
        echo "SELF_CHECK_FAIL: ${BASELINE_FILE} missing" >&2
        broken=1
    fi
    if [[ ${broken} -eq 1 ]]; then
        echo "EIE_ENFORCEMENT_BROKEN" >&2
        exit 4
    fi
    # EPIC-0059: validate artifact counts — Phase 1 must have 6, Phase 3 must have 4
    local phase1_count=${#REQUIRED_PHASE_1_ARTIFACT_TEMPLATES[@]}
    # Phase 3 artifacts: verify-envelope, review, techlead-review, story-completion-report
    local phase3_count=4
    local total_count=$((phase1_count + phase3_count))
    if [[ "${phase1_count}" -ne 6 || "${total_count}" -ne 10 ]]; then
        echo "SELF_CHECK_FAIL: expected 6 Phase-1 + 4 Phase-3 = 10 total artifacts; got Phase-1=${phase1_count} total=${total_count}" >&2
        echo "EIE_ENFORCEMENT_BROKEN" >&2
        exit 4
    fi
    # EPIC-0059 story-0059-0002: verify anti-backfill functions are defined in this script
    local self_path="${BASH_SOURCE[0]}"
    if ! grep -q 'check_frontmatter_origin()' "${self_path}" 2>/dev/null; then
        echo "SELF_CHECK_FAIL: check_frontmatter_origin() function not found in ${self_path}" >&2
        echo "EIE_ENFORCEMENT_BROKEN" >&2
        exit 4
    fi
    if ! grep -q 'check_anti_backfill()' "${self_path}" 2>/dev/null; then
        echo "SELF_CHECK_FAIL: check_anti_backfill() function not found in ${self_path}" >&2
        echo "EIE_ENFORCEMENT_BROKEN" >&2
        exit 4
    fi
    if ! grep -q 'check_telemetry()' "${self_path}" 2>/dev/null; then
        echo "SELF_CHECK_FAIL: check_telemetry() function not found in ${self_path}" >&2
        echo "EIE_ENFORCEMENT_BROKEN" >&2
        exit 4
    fi
    echo "OK: 10 required artifacts configured (Phase-1=${phase1_count}, Phase-3=${phase3_count}). Anti-backfill functions: present. check_telemetry: present."
    exit 0
}

load_baseline() {
    if [[ ! -f "${BASELINE_FILE}" ]]; then
        echo "EIE_BASELINE_CORRUPT: ${BASELINE_FILE} missing" >&2
        exit 2
    fi
    # Extract STORY-IDs (strip comments and blanks)
    grep -oE '^story-[0-9]{4}-[0-9]{4}' "${BASELINE_FILE}" 2>/dev/null | sort -u
}

is_grandfathered() {
    local story_id="$1"
    grep -qxF "${story_id}" <<< "${BASELINE_STORIES}"
}

has_audit_exempt() {
    # Returns:
    #   0 — marker present with a non-empty reason
    #   1 — marker absent (or story file not found)
    #   3 — marker present but malformed (empty / whitespace-only reason)
    local story_id="$1"
    local story_file
    story_file="$(find plans -name "${story_id}.md" 2>/dev/null | head -1)"
    [[ -z "${story_file}" ]] && return 1
    # Look for any audit-exempt marker (valid or malformed)
    if ! grep -qE '<!--\s*audit-exempt' "${story_file}"; then
        return 1
    fi
    # Valid form MUST have a non-empty reason:
    #   <!-- audit-exempt: <at least one non-space char> -->
    if grep -qE '<!--\s*audit-exempt:\s*[^[:space:]-][^-]*-->' "${story_file}"; then
        return 0
    fi
    # Marker present but malformed
    echo "EIE_INVALID_EXEMPTION: ${story_file} has audit-exempt marker without a reason" >&2
    return 3
}

discover_merged_stories() {
    # Merge-strategy-agnostic story discovery:
    # scans first-parent commit subjects for `feat(story-XXXX-YYYY...)` OR
    # `fix(story-XXXX-YYYY...)` AND also inspects `--merges` commits for
    # `feat/story-*` merged branch names. Covers squash/rebase (conventional
    # commit subject) and merge-commit strategies uniformly.
    local since_ref="${1:-}"
    local range_arg=""
    if [[ -n "${since_ref}" ]]; then
        range_arg="${since_ref}..HEAD"
    fi
    {
        # Squash/rebase: conventional commit subjects on first-parent
        git log --first-parent ${range_arg} --format='%s' 2>/dev/null \
            | grep -oE '(feat|fix)\(story-[0-9]{4}-[0-9]{4}' \
            | grep -oE 'story-[0-9]{4}-[0-9]{4}'
        # Merge commits referencing feat/story-* branches
        git log --merges --first-parent ${range_arg} \
            --format='%H %s' 2>/dev/null \
            | grep -oE 'feat/story-[0-9]{4}-[0-9]{4}[a-zA-Z0-9-]*' \
            | sed -E 's|^feat/||' \
            | grep -oE 'story-[0-9]{4}-[0-9]{4}'
    } | sort -u
}

check_frontmatter_origin() {
    # EPIC-0059 (story-0059-0002): validate the YAML frontmatter origin marker.
    # Returns:
    #   0 — frontmatter present, format valid, SHA exists in git history
    #   1 — any validation failure (prints diagnostic to stderr)
    local artifact_path="$1"
    # 1. Check frontmatter delimiter present as first line
    local first_line
    first_line=$(head -1 "${artifact_path}" 2>/dev/null || true)
    if [[ "${first_line}" != "---" ]]; then
        echo "EIE_EVIDENCE_MISSING: ${artifact_path} — missing generated-by frontmatter (first line is not '---')" >&2
        return 1
    fi
    # 2. Extract generated-by field from frontmatter (between first --- and second ---)
    local generated_by
    generated_by=$(awk '/^---/{f++; next} f==1 && /^generated-by:/{print $2; exit}' "${artifact_path}" 2>/dev/null || true)
    if [[ -z "${generated_by}" ]]; then
        echo "EIE_EVIDENCE_MISSING: ${artifact_path} — generated-by field absent in frontmatter" >&2
        return 1
    fi
    # 3. Validate format: <skill-name>@<40-hex-chars>
    if ! echo "${generated_by}" | grep -qE '^[a-z-]+@[0-9a-f]{40}$'; then
        echo "EIE_EVIDENCE_MISSING: ${artifact_path} — generated-by format invalid: '${generated_by}' (expected '<skill>@<40-hex-sha>')" >&2
        return 1
    fi
    # 4. Validate SHA exists in git history (fail-open on git errors)
    local sha="${generated_by##*@}"
    local cat_file_result
    cat_file_result=$(git cat-file -t "${sha}" 2>/dev/null || true)
    if [[ "${cat_file_result}" != "commit" ]]; then
        echo "EIE_EVIDENCE_MISSING: ${artifact_path} — SHA not found in git history: ${sha}" >&2
        return 1
    fi
    return 0
}

check_has_backfill_exempt() {
    # Check if an artifact has a backfill-specific audit-exempt marker.
    # Returns:
    #   0 — valid backfill exemption present (non-empty incident link)
    #   1 — no backfill exemption present
    #   3 — backfill exemption present but malformed (empty link)
    local artifact_path="$1"
    # Look for <!-- audit-exempt: backfill <link> --> pattern
    if ! grep -qE '<!--\s*audit-exempt:\s*backfill' "${artifact_path}" 2>/dev/null; then
        return 1
    fi
    # Valid form: <!-- audit-exempt: backfill <url> --> (non-empty URL after "backfill")
    if grep -qE '<!--\s*audit-exempt:\s*backfill\s+https?://[^[:space:]]+[^-]*-->' "${artifact_path}" 2>/dev/null; then
        return 0
    fi
    # Marker present but malformed (empty or missing URL)
    echo "EIE_INVALID_EXEMPTION: ${artifact_path} has backfill audit-exempt marker without incident URL" >&2
    return 3
}

check_anti_backfill() {
    # EPIC-0059 (story-0059-0002): detect artifacts committed after the story's PR merged.
    # Returns:
    #   0 — artifact committed before merge (or cannot determine — fail-open)
    #   1 — EIE_BACKFILL_DETECTED (artifact committed after merge)
    local story_id="$1"
    local artifact_path="$2"
    # Get timestamp when artifact was first introduced to git (oldest commit for this path)
    local artifact_first_commit_ts
    artifact_first_commit_ts=$(git log --diff-filter=A --pretty=format:'%ct' -- "${artifact_path}" 2>/dev/null | tail -1)
    # If file not yet in git history (untracked or brand-new), skip check (fail-open)
    [[ -z "${artifact_first_commit_ts}" ]] && return 0
    # Find the merge commit timestamp for this story's PR (first-parent merge referencing the story)
    local story_num="${story_id#story-}"
    local merge_ts
    merge_ts=$(git log --first-parent --merges \
        --pretty=format:'%ct %s' 2>/dev/null \
        | grep -i "story-${story_num}" \
        | awk '{print $1}' \
        | sort -n | head -1)
    # Cannot determine merge time — skip check (fail-open per ADR-003)
    [[ -z "${merge_ts}" ]] && return 0
    if [[ "${artifact_first_commit_ts}" -gt "${merge_ts}" ]]; then
        echo "EIE_BACKFILL_DETECTED: ${artifact_path} — artifact committed after story merge (artifact_ts=${artifact_first_commit_ts} > merge_ts=${merge_ts})" >&2
        return 1
    fi
    return 0
}

check_phase1_evidence() {
    # EPIC-0059: verify the 6 mandatory Phase-1 planning artifacts.
    # Also validates origin marker (frontmatter) and anti-backfill for non-grandfathered stories.
    # Returns 0 if all present and valid, 1 if any check fails (prints diagnostics to stderr).
    # Artifact naming convention: arch-story-XXXX-YYYY.md where XXXX-YYYY is the
    # story numeric suffix (e.g., story-0059-0001 → artifact uses "0059-0001").
    local story_id="$1"
    local epic_id
    epic_id="$(echo "${story_id}" | grep -oE '[0-9]{4}' | head -1)"
    # Extract the numeric suffix of story_id: "story-0059-0001" → "0059-0001"
    local story_suffix
    story_suffix="$(echo "${story_id}" | sed 's/^story-//')"
    local missing=()
    local artifact_path
    local phase1_failed=0
    for template in "${REQUIRED_PHASE_1_ARTIFACT_TEMPLATES[@]}"; do
        artifact_path="${template/EPIC_ID/${epic_id}}"
        artifact_path="${artifact_path/STORY_ID/${story_suffix}}"
        if [[ ! -f "${artifact_path}" ]]; then
            missing+=("${artifact_path}")
            phase1_failed=1
            continue
        fi
        # Origin marker validation (EPIC-0059, story-0059-0002)
        # Check for backfill-specific exemption first
        check_has_backfill_exempt "${artifact_path}"
        local exempt_rc=$?
        if [[ ${exempt_rc} -eq 3 ]]; then
            phase1_failed=1
            continue
        elif [[ ${exempt_rc} -eq 0 ]]; then
            # Accepted backfill exemption — skip origin + anti-backfill checks
            printf "  ⚪ %s — %s: backfill-exempt\n" "${story_id}" "${artifact_path}"
            continue
        fi
        # No exemption — validate frontmatter origin marker
        if ! check_frontmatter_origin "${artifact_path}"; then
            phase1_failed=1
            continue
        fi
        # Anti-backfill check: artifact must not have been committed after the story's PR merge
        if ! check_anti_backfill "${story_id}" "${artifact_path}"; then
            phase1_failed=1
        fi
    done
    if [[ ${#missing[@]} -gt 0 ]]; then
        printf "  ❌ %s — missing Phase-1: %s\n" "${story_id}" "$(IFS=,; echo "${missing[*]}")" >&2
    fi
    return ${phase1_failed}
}

check_evidence() {
    local story_id="$1"
    local epic_id
    epic_id="$(echo "${story_id}" | grep -oE '[0-9]{4}' | head -1)"
    local plans_dir="plans/epic-${epic_id}/plans"
    local reports_dir="plans/epic-${epic_id}/reports"
    local missing=()

    if [[ ! -f "${reports_dir}/verify-envelope-${story_id}.json" ]]; then
        missing+=("verify-envelope")
    fi

    if ! compgen -G "${plans_dir}/review-*${story_id}*.md" >/dev/null 2>&1 \
            && [[ ! -f "${plans_dir}/review-story-${story_id}.md" ]]; then
        missing+=("review (x-review)")
    fi

    if ! compgen -G "${plans_dir}/techlead-review-*${story_id}*.md" >/dev/null 2>&1 \
            && [[ ! -f "${plans_dir}/techlead-review-story-${story_id}.md" ]]; then
        missing+=("techlead-review (x-review-pr)")
    fi

    if [[ ! -f "${reports_dir}/story-completion-report-${story_id}.md" ]]; then
        missing+=("story-completion-report")
    fi

    # EPIC-0057 — hard artefact for x-dependency-audit (Camada 3, Rule 24
    # §32-42 expanded). Mirrors the .conf HARD_DEPENDENCY_AUDIT pattern.
    if [[ ! -f "${reports_dir}/dependency-audit-${story_id}.md" ]]; then
        missing+=("dependency-audit (x-dependency-audit)")
    fi

    # EPIC-0057 — hard artefact for x-pr-watch-ci (Rule 45). State file
    # is keyed by PR number; we resolve it from execution-state.json
    # storyStatuses.<id>.prNumber when present and fall back to a
    # directory-presence check otherwise. The .claude/state/ directory
    # itself is a precondition — its absence indicates the runtime tree
    # has never been generated, in which case the check short-circuits
    # (CI checkouts that don't generate .claude/ are not penalised).
    local pr_state_dir=".claude/state"
    local pr_number=""
    local epic_num
    epic_num=$(echo "${story_id}" | grep -oE '^story-[0-9]{4}' | sed 's/story-//')
    local exec_state="plans/epic-${epic_num}/execution-state.json"
    if [[ -f "${exec_state}" ]] && command -v jq >/dev/null 2>&1; then
        pr_number=$(jq -r --arg sid "${story_id}" \
            '.storyStatuses[$sid].prNumber // empty' \
            "${exec_state}" 2>/dev/null || true)
    fi
    if [[ -d "${pr_state_dir}" ]]; then
        if [[ -n "${pr_number}" ]]; then
            if [[ ! -f "${pr_state_dir}/pr-watch-${pr_number}.json" ]]; then
                missing+=("pr-watch (x-pr-watch-ci) → ${pr_state_dir}/pr-watch-${pr_number}.json")
            fi
        else
            # PR number unknown — fall back to directory-level presence.
            if ! compgen -G "${pr_state_dir}/pr-watch-[0-9]*.json" >/dev/null 2>&1; then
                missing+=("pr-watch (x-pr-watch-ci) — no pr-watch-*.json under ${pr_state_dir}")
            fi
        fi
    fi

    if [[ ${#missing[@]} -gt 0 ]]; then
        printf "  ❌ %s — missing: %s\n" "${story_id}" "$(IFS=,; echo "${missing[*]}")" >&2
        return 1
    fi
    return 0
}

usage_error() {
    cat >&2 <<EOF
usage: $(basename "$0") [--self-check] [--since <git-ref>]
                        [--story-id <story-XXXX-YYYY>] [--json]
                        [--scope=<full|fase1|fase3>]

  --self-check        verify enforcement infrastructure is wired (counts 10 artifacts)
  --since <ref>       limit scan to merges since the given git ref
  --story-id <id>     audit only the specified story (skips git log)
  --json              emit a single-line JSON envelope on stdout
                      (status, storiesAudited, storiesPassed,
                       storiesFailed, failures[])
  --scope=fase1       audit Phase-1 planning artifacts only (6 artifacts)
  --scope=fase3       audit Phase-3 evidence artifacts only (legacy behavior)
  --scope=full        audit both Phase-1 and Phase-3 artifacts (default)
EOF
    exit 2
}

# discover_story_ids_from_commits — extract story IDs mentioned in PR commits.
# Uses AUDIT_TEST_STORY_IDS env var when set (for smoke-test isolation).
# Falls back to git log for CI usage.
discover_story_ids_from_commits() {
    if [[ -n "${AUDIT_TEST_STORY_IDS:-}" ]]; then
        echo "${AUDIT_TEST_STORY_IDS}" | tr ' ,' '\n' | grep -E '^story-[0-9]{4}-[0-9]{4}$' | sort -u
        return 0
    fi
    # Extract story IDs from commits not yet in origin/develop (PR branch scope)
    git log "origin/develop..HEAD" --format="%s %b" 2>/dev/null \
        | grep -oE 'story-[0-9]{4}-[0-9]{4}' | sort -u || true
}

# check_telemetry — validate that events.ndjson contains mandatory phase.start events
# for each story ID referenced in the PR commits (EPIC-0059 story-0059-0008).
#
# Returns:
#   0 — all mandatory events present for all stories
#   1 — one or more mandatory events missing (prints EIE_TELEMETRY_MISSING to stderr)
check_telemetry() {
    local story_ids
    story_ids="$(discover_story_ids_from_commits)"
    if [[ -z "${story_ids}" ]]; then
        # No story IDs detected — nothing to validate
        return 0
    fi

    local telemetry_violations=0
    for story_id in ${story_ids}; do
        # Derive epic ID from story ID (story-XXXX-YYYY → XXXX)
        local epic_id
        epic_id="$(echo "${story_id}" | grep -oE '[0-9]{4}' | head -1)"
        local ndjson="${REPO_ROOT}/plans/epic-${epic_id}/telemetry/events.ndjson"

        if [[ ! -f "${ndjson}" ]]; then
            echo "EIE_TELEMETRY_MISSING: events.ndjson not found at ${ndjson} for ${story_id}" >&2
            telemetry_violations=$((telemetry_violations + 1))
            continue
        fi

        local story_violations=0
        for required_phase in "${REQUIRED_TELEMETRY_PHASES[@]}"; do
            if [[ "${required_phase}" == "Phase-1-Plan" ]]; then
                # Accept either phase.start Phase-1-Plan OR phase.skip PRE_PLANNED
                if grep -q "\"x-story-implement\"" "${ndjson}" 2>/dev/null && \
                   (grep -q "\"${required_phase}\"" "${ndjson}" 2>/dev/null || \
                    grep -q "\"Phase-1-Plan\"" "${ndjson}" 2>/dev/null || \
                    grep -q "PRE_PLANNED" "${ndjson}" 2>/dev/null); then
                    continue
                fi
            else
                if grep -q "\"phase.start\"" "${ndjson}" 2>/dev/null && \
                   grep -q "\"x-story-implement\"" "${ndjson}" 2>/dev/null && \
                   grep -q "\"${required_phase}\"" "${ndjson}" 2>/dev/null; then
                    continue
                fi
            fi
            echo "EIE_TELEMETRY_MISSING: ${required_phase} missing for ${story_id} in ${ndjson}" >&2
            story_violations=$((story_violations + 1))
        done

        if [[ ${story_violations} -gt 0 ]]; then
            echo "EIE_TELEMETRY_MISSING: no x-story-implement telemetry for ${story_id} (${story_violations} phases missing)" >&2
            telemetry_violations=$((telemetry_violations + 1))
        fi
    done

    return $((telemetry_violations > 0 ? 1 : 0))
}

emit_json_envelope() {
    local status="$1"
    local total="$2"
    local passed="$3"
    local failed="$4"
    local failures_json="$5"
    printf '{"status":"%s","storiesAudited":%d,"storiesPassed":%d,"storiesFailed":%d,"failures":%s}\n' \
        "${status}" "${total}" "${passed}" "${failed}" "${failures_json}"
}

main() {
    local since_ref=""
    local single_story=""
    local json_mode="false"
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --self-check) self_check ;;
            --since)
                if [[ $# -lt 2 || -z "${2:-}" || "${2:0:2}" == "--" ]]; then
                    echo "error: --since requires a git-ref argument" >&2
                    usage_error
                fi
                since_ref="$2"
                shift 2
                ;;
            --story-id)
                if [[ $# -lt 2 || -z "${2:-}" || "${2:0:2}" == "--" ]]; then
                    echo "error: --story-id requires a story id argument" >&2
                    usage_error
                fi
                if ! [[ "$2" =~ ^story-[0-9]{4}-[0-9]{4}$ ]]; then
                    echo "error: --story-id must match story-XXXX-YYYY pattern" >&2
                    usage_error
                fi
                single_story="$2"
                shift 2
                ;;
            --scope=fase1)
                AUDIT_SCOPE="fase1"
                shift
                ;;
            --scope=fase3)
                AUDIT_SCOPE="fase3"
                shift
                ;;
            --scope=full)
                AUDIT_SCOPE="full"
                shift
                ;;
            --scope=telemetry)
                AUDIT_SCOPE="telemetry"
                shift
                ;;
            --scope=*)
                echo "error: --scope must be one of: full, fase1, fase3, telemetry" >&2
                usage_error
                ;;
            --json)
                json_mode="true"
                shift
                ;;
            -h|--help) usage_error ;;
            *)
                echo "error: unknown flag '$1'" >&2
                usage_error
                ;;
        esac
    done

    BASELINE_STORIES="$(load_baseline)"

    # Telemetry-only scope: validate events.ndjson proof-of-life (EPIC-0059 story-0059-0008)
    if [[ "${AUDIT_SCOPE}" == "telemetry" ]]; then
        if [[ "${json_mode}" != "true" ]]; then
            echo "EIE audit — Rule 24 Camada 3 (scope: telemetry)"
            echo "============================"
        fi
        if check_telemetry; then
            if [[ "${json_mode}" == "true" ]]; then
                emit_json_envelope "OK" 0 0 0 "[]"
            else
                echo "OK — telemetry integrity preserved."
            fi
            exit 0
        else
            if [[ "${json_mode}" == "true" ]]; then
                emit_json_envelope "EIE_TELEMETRY_MISSING" 0 0 1 "[{\"status\":\"EIE_TELEMETRY_MISSING\"}]"
            fi
            exit 1
        fi
    fi

    local stories
    if [[ -n "${single_story}" ]]; then
        stories="${single_story}"
    else
        stories="$(discover_merged_stories "${since_ref}")"
    fi
    if [[ -z "${stories}" ]]; then
        if [[ "${json_mode}" == "true" ]]; then
            emit_json_envelope "OK" 0 0 0 "[]"
        else
            echo "EIE audit: no merged story branches found in scope."
        fi
        exit 0
    fi

    local violations=0
    local passed=0
    local total=0
    local exempted=0
    local grandfathered=0
    local invalid_exemptions=0
    local failures_json="["
    local failures_first="true"

    if [[ "${json_mode}" != "true" ]]; then
        echo "EIE audit — Rule 24 Camada 3 (scope: ${AUDIT_SCOPE})"
        echo "============================"
    fi
    for story in ${stories}; do
        total=$((total + 1))
        if is_grandfathered "${story}"; then
            [[ "${json_mode}" != "true" ]] && \
                printf "  ⚪ %s — grandfathered (baseline)\n" "${story}"
            grandfathered=$((grandfathered + 1))
            passed=$((passed + 1))
            continue
        fi
        has_audit_exempt "${story}"
        local exempt_rc=$?
        if [[ ${exempt_rc} -eq 0 ]]; then
            [[ "${json_mode}" != "true" ]] && \
                printf "  ⚪ %s — audit-exempt\n" "${story}"
            exempted=$((exempted + 1))
            passed=$((passed + 1))
            continue
        elif [[ ${exempt_rc} -eq 3 ]]; then
            [[ "${json_mode}" != "true" ]] && \
                printf "  ❌ %s — malformed audit-exempt marker\n" "${story}" >&2
            invalid_exemptions=$((invalid_exemptions + 1))
            # Append to failures envelope so JSON consumers see WHICH
            # story has the malformed marker (Copilot review feedback —
            # PR #653 review comment 3).
            if [[ "${failures_first}" == "true" ]]; then
                failures_first="false"
            else
                failures_json+=","
            fi
            failures_json+="{\"storyId\":\"${story}\",\"status\":\"EIE_INVALID_EXEMPTION\"}"
            continue
        fi

        local story_failed=0
        # EPIC-0059: check Phase 1 planning artifacts when scope is full or fase1
        if [[ "${AUDIT_SCOPE}" == "full" || "${AUDIT_SCOPE}" == "fase1" ]]; then
            if ! check_phase1_evidence "${story}"; then
                story_failed=1
            fi
        fi
        # Check Phase 3 evidence artifacts when scope is full or fase3
        if [[ "${AUDIT_SCOPE}" == "full" || "${AUDIT_SCOPE}" == "fase3" ]]; then
            if ! check_evidence "${story}"; then
                story_failed=1
            fi
        fi

        if [[ "${story_failed}" -eq 0 ]]; then
            [[ "${json_mode}" != "true" ]] && printf "  ✅ %s\n" "${story}"
            passed=$((passed + 1))
        else
            violations=$((violations + 1))
            if [[ "${failures_first}" == "true" ]]; then
                failures_first="false"
            else
                failures_json+=","
            fi
            failures_json+="{\"storyId\":\"${story}\",\"status\":\"EIE_EVIDENCE_MISSING\"}"
        fi
    done
    failures_json+="]"

    if [[ "${json_mode}" != "true" ]]; then
        echo "----------------------------"
        echo "Total: ${total} | grandfathered: ${grandfathered} | exempt: ${exempted} | invalid-exempt: ${invalid_exemptions} | violations: ${violations}"
    fi

    if [[ ${invalid_exemptions} -gt 0 ]]; then
        if [[ "${json_mode}" == "true" ]]; then
            emit_json_envelope "EIE_INVALID_EXEMPTION" \
                "${total}" "${passed}" "${invalid_exemptions}" "${failures_json}"
        else
            cat >&2 <<EOF

EIE_INVALID_EXEMPTION — ${invalid_exemptions} story(ies) have malformed '<!-- audit-exempt -->' markers.

Fix: the marker MUST carry a non-empty reason, e.g.:
  <!-- audit-exempt: reason=migration-only-no-runtime-code approved-by=tech-lead date=YYYY-MM-DD -->
EOF
        fi
        exit 3
    fi

    if [[ ${violations} -gt 0 ]]; then
        if [[ "${json_mode}" == "true" ]]; then
            emit_json_envelope "EIE_EVIDENCE_MISSING" \
                "${total}" "${passed}" "${violations}" "${failures_json}"
        else
            cat >&2 <<EOF

EIE_EVIDENCE_MISSING — ${violations} merged story(ies) lack mandatory evidence artifacts.

Fix by either:
  (a) Running x-internal-story-verify / x-review / x-review-pr / x-internal-story-report
      as REAL Skill(...) tool calls on the affected story, committing the resulting
      artifacts, and amending the story PR.
  (b) Adding '<!-- audit-exempt: <reason> -->' to the story markdown (reviewed exceptions only).

See .claude/rules/24-execution-integrity.md §3 (Camada 3).
EOF
        fi
        exit 1
    fi

    if [[ "${json_mode}" == "true" ]]; then
        emit_json_envelope "OK" "${total}" "${passed}" 0 "[]"
    else
        echo "OK — execution integrity preserved."
    fi
    exit 0
}

main "$@"
