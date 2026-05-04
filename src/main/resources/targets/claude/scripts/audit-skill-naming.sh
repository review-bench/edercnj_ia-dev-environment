#!/usr/bin/env bash
# audit-skill-naming.sh — Camada 2 (CI) guard anti-legado para EPIC-0076.
#
# Verifica que nenhum nome legado de skill (padrão noun-first) é reintroduzido
# em novos SKILL.md, rules, agents, hooks ou docs fora da allow-list histórica.
#
# Convenção alvo (SPEC-verb-first-skill-naming-v1.md):
#   Public:   x-<verb>-<object>          (ex: x-implement-story, x-review-codebase)
#   Internal: x-internal-<verb>-<object> (ex: x-internal-verify-story)
#   Lib:      x-lib-<verb>-<object>      (ex: x-lib-verify-group)
#
# Padrões legados detectados (noun-first):
#   x-story-implement, x-epic-implement, x-task-implement,
#   x-story-plan, x-epic-refine, x-story-refine,
#   x-review (bare — mas NÃO x-review-codebase, x-review-pr etc),
#   x-pr-create, x-pr-merge, x-pr-fix, x-pr-watch-ci,
#   x-git-commit, x-git-push, x-git-merge, x-git-branch,
#   x-test-run, x-test-tdd, x-test-e2e, x-test-contract, x-test-mutation,
#   x-test-performance, x-test-perf, x-test-smoke-api, x-test-smoke-socket,
#   x-test-regression-shell, x-test-plan, x-test-contract-lint,
#   x-code-format, x-code-lint, x-code-audit,
#   x-doc-generate, x-doc-validate,
#   x-dependency-audit, x-supply-chain-audit,
#   x-internal-story-verify, x-internal-story-report, x-internal-story-resume,
#   x-internal-story-build-plan, x-internal-story-create,
#   x-internal-epic-integrity-gate, x-internal-epic-branch-ensure,
#   x-internal-epic-build-plan, x-internal-epic-summary, x-internal-epic-create,
#   x-internal-epic-map, x-internal-story-load-context,
#   x-internal-worktree-precheck, x-internal-pr-body-render,
#   x-internal-phase-gate, x-internal-report-write, x-internal-status-update,
#   x-internal-args-normalize,
#   x-lib-group-verifier, x-lib-task-decomposer
#
# Allow-list histórica (CHANGELOG.md, ADRs, SPEC, migration docs — tolerados):
#   Ver ALLOWLIST_FILES abaixo.
#
# Layer: 2 — CI Script (Rule 26 §Taxonomy)
# Rule:  EPIC-0076 (Verb-First Skill Naming Refactor)
# Introduced: story-0076-0007
#
# Exit codes (Rule 26 §Standardized):
#   0 — OK (nenhuma violação)
#   1 — SKILL_NAMING_VIOLATION (nome legado detectado fora da allow-list)
#   2 — OPERATIONAL_ERROR (git ou grep ausente)
#   3 — BASELINE_CORRUPT (arquivo de baseline ilegível)
#
# Usage:
#   audit-skill-naming.sh                 # varredura completa
#   audit-skill-naming.sh --self-check    # valida integridade do script
#   audit-skill-naming.sh --diff-only     # varre apenas diff HEAD~1..HEAD

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${REPO_ROOT}"

BASELINE_FILE="governance/baselines/skill-naming-baseline.txt"
VIOLATIONS=0
DIFF_ONLY=false

# Arquivos/diretórios onde nomes legados são tolerados (histórico necessário)
ALLOWLIST_DIRS=(
    "CHANGELOG.md"
    "docs/specs/SPEC-verb-first-skill-naming-v1.md"
    "docs/adr/ADR-0003-skill-taxonomy-and-naming.md"
    "ai/epics/epic-0036"
    "ai/epics/epic-0076"
    "src/main/resources/targets/claude/scripts/audit-skill-naming.sh"
    ".claude/skills/x-migrate-templates"
    ".claude/skills/x-migrate-frontmatter"
)

# Padrões legados a detectar (word-boundary via grep -E)
# Nota: usamos \b equivalente com (^|[^a-z0-9-]) e ([^a-z0-9-]|$)
LEGACY_PATTERNS=(
    "x-story-implement"
    "x-epic-implement"
    "x-task-implement"
    "x-story-plan"
    "x-epic-refine"
    "x-story-refine"
    "x-epic-orchestrate"
    "x-feature-ideate"
    "x-feature-create"
    "x-adr-generate"
    "x-arch-plan"
    "x-arch-update"
    "x-arch-system-update"
    "x-pr-create"
    "x-pr-merge"
    "x-pr-fix-epic"
    "x-pr-fix"
    "x-pr-watch-ci"
    "x-git-commit"
    "x-git-push"
    "x-git-merge"
    "x-git-branch"
    "x-git-worktree"
    "x-git-cleanup-branches"
    "x-planning-commit"
    "x-test-run"
    "x-test-tdd"
    "x-test-e2e"
    "x-test-contract"
    "x-test-mutation"
    "x-test-performance"
    "x-test-perf"
    "x-test-smoke-api"
    "x-test-smoke-socket"
    "x-test-regression-shell"
    "x-test-plan"
    "x-test-contract-lint"
    "x-code-format"
    "x-code-lint"
    "x-code-audit"
    "x-doc-generate"
    "x-doc-validate"
    "x-dependency-audit"
    "x-supply-chain-audit"
    "x-hardening-eval"
    "x-runtime-eval"
    "x-owasp-scan"
    "x-security-dashboard"
    "x-security-pipeline"
    "x-security-secrets"
    "x-security-sast"
    "x-security-dast"
    "x-security-container"
    "x-pentest-dynamic"
    "x-security-pentest"
    "x-security-infra"
    "x-security-sonar"
    "x-dep-policy-validate"
    "x-jira-create-epic"
    "x-jira-create-stories"
    "x-telemetry-trend"
    "x-telemetry-analyze"
    "x-release-changelog"
    "x-status-reconcile"
    "x-ops-incident"
    "x-obs-instrument"
    "x-ops-troubleshoot"
    "x-perf-profile"
    "x-memory-search"
    "x-review-perf"
    "x-review-db"
    "x-review-obs"
    "x-internal-story-verify"
    "x-internal-story-report"
    "x-internal-story-resume"
    "x-internal-story-build-plan"
    "x-internal-story-create"
    "x-internal-story-load-context"
    "x-internal-epic-integrity-gate"
    "x-internal-epic-branch-ensure"
    "x-internal-epic-build-plan"
    "x-internal-epic-summary"
    "x-internal-epic-create"
    "x-internal-epic-map"
    "x-internal-worktree-precheck"
    "x-internal-pr-body-render"
    "x-internal-phase-gate"
    "x-internal-report-write"
    "x-internal-status-update"
    "x-internal-args-normalize"
    "x-lib-group-verifier"
    "x-lib-task-decomposer"
    "x-parallel-eval"
    "x-spec-drift"
    "x-threat-model"
)

# ── helpers ───────────────────────────────────────────────────────────────────

log_violation() {
    echo "SKILL_NAMING_VIOLATION: $*" >&2
    VIOLATIONS=$(( VIOLATIONS + 1 ))
}

is_allowlisted() {
    local file="$1"
    for allowed in "${ALLOWLIST_DIRS[@]}"; do
        if [[ "${file}" == "${allowed}" || "${file}" == "${allowed}/"* ]]; then
            return 0
        fi
    done
    # Also check per-file exemption: <!-- audit-exempt: ... --> in file
    if grep -qE "audit-exempt:" "${file}" 2>/dev/null; then
        return 0
    fi
    # Check baseline file
    if [[ -f "${BASELINE_FILE}" ]] && grep -qF "${file}" "${BASELINE_FILE}" 2>/dev/null; then
        return 0
    fi
    return 1
}

# ── self-check ────────────────────────────────────────────────────────────────

if [[ "${1:-}" == "--self-check" ]]; then
    ok=true
    command -v grep >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: grep not found" >&2; ok=false; }
    [[ -f "${BASELINE_FILE}" ]] || { echo "OPERATIONAL_ERROR: baseline missing: ${BASELINE_FILE}" >&2; ok=false; }
    [[ -f "docs/specs/SPEC-verb-first-skill-naming-v1.md" ]] || {
        echo "OPERATIONAL_ERROR: SPEC file missing" >&2; ok=false;
    }
    "${ok}" && exit 0 || exit 2
fi

if [[ "${1:-}" == "--diff-only" ]]; then
    DIFF_ONLY=true
fi

# ── baseline integrity ────────────────────────────────────────────────────────

if [[ ! -f "${BASELINE_FILE}" ]]; then
    echo "OPERATIONAL_ERROR: baseline file not found: ${BASELINE_FILE}" >&2
    exit 2
fi

# ── file collection ───────────────────────────────────────────────────────────

if "${DIFF_ONLY}"; then
    mapfile -t FILES < <(git diff --name-only HEAD~1..HEAD 2>/dev/null | grep -E '\.(md|sh|json|yaml|yml)$' || true)
else
    mapfile -t FILES < <(
        find src/main/resources/targets/claude/ .claude/rules/ .claude/agents/ docs/ \
             -type f \( -name "*.md" -o -name "*.sh" -o -name "*.json" -o -name "*.yaml" \) \
             2>/dev/null | sort
        [[ -f "CLAUDE.md" ]] && echo "CLAUDE.md"
        [[ -f "README.md" ]] && echo "README.md"
        true
    )
fi

# ── scan ──────────────────────────────────────────────────────────────────────

for file in "${FILES[@]}"; do
    [[ -f "${file}" ]] || continue
    is_allowlisted "${file}" && continue

    for pattern in "${LEGACY_PATTERNS[@]}"; do
        # Word-boundary: not preceded/followed by [a-z0-9-]
        if grep -qP "(?<![a-z0-9-])${pattern}(?![a-z0-9-])" "${file}" 2>/dev/null; then
            matches=$(grep -nP "(?<![a-z0-9-])${pattern}(?![a-z0-9-])" "${file}" 2>/dev/null | head -3)
            log_violation "Legacy skill name '${pattern}' found in: ${file}"$'\n'"  ${matches}"
        fi
    done
done

# ── result ────────────────────────────────────────────────────────────────────

if [[ "${VIOLATIONS}" -gt 0 ]]; then
    echo "" >&2
    echo "Total violations: ${VIOLATIONS}" >&2
    echo "Add file to allow-list in ${BASELINE_FILE} or use '<!-- audit-exempt: <reason> -->'." >&2
    exit 1
fi

echo "audit-skill-naming: OK — no legacy skill names detected."
exit 0
