# Specialist Review — story-0059-0011

**Story:** story-0059-0011 — Anistia Formal de EPIC-0054–0057 + Immutability Check
**Epic:** EPIC-0059 (Zero-Bypass Lifecycle Enforcement)
**Review Date:** 2026-04-27
**Reviewer:** Specialist Review Panel (Security, QA, Audit)

## Overall Score: 9.0/10 — GO

---

## Security Review

**Score: 9/10**

### Findings

- The `audit-baseline-immutability.sh` script uses `set -uo pipefail` and handles undefined variables safely.
- No hardcoded credentials or sensitive data.
- Input validation: `--file` argument checked for non-empty; unknown args rejected.
- The exemption marker (`baseline-correction:`) requires CODEOWNERS review — appropriate access control.
- `git show` is used with a specific SHA (not user-controlled input), preventing injection.
- `comm` usage is safe: inputs are piped through `sort -u` before comparison.

### Recommendations

- **LOW**: Consider adding `shellcheck` to the CI step for the new script (consistent with existing telemetry hook policy). Non-blocking.
- **INFO**: The `BASELINE_CUTOFF_SHA` env override for testing is appropriate and clearly documented.

---

## QA Review

**Score: 9/10**

### Acceptance Criteria Coverage

| Criterion | Status |
| :--- | :--- |
| Inventário via git log + execution-state.json | PASS — entries present in baseline |
| Todas as stories dos 4 epics na baseline | PASS — 32 stories (4+12+8+8) |
| Formato correto com comentário de amnesty | PASS — `# amnesty EPIC-0059: <reason> (date)` |
| Detecta entries adicionadas após o cutoff commit | PASS — exit 1 for new IDs |
| Exit 1 com BASELINE_IMMUTABILITY_VIOLATION | PASS — correct exit code |
| `--self-check` valida que o cutoff commit existe | PASS — `audits/baseline-cutoff.sha` loaded |
| ADR-0015 com Context, Decision, Consequences | PASS — all sections present |
| Job de CI roda audit-baseline-immutability | PASS — step added to ci-release.yml |

### Gherkin Scenario Coverage

All 4 Gherkin scenarios validated:
1. ✓ Audit passes for grandfathered stories (execution-integrity-baseline.txt + rule-26-baseline.txt)
2. ✓ Immutability check blocks new entry after cutoff (exit 1)
3. ✓ ADR-0015 exists and well-formed
4. ✓ Baseline covers all stories of EPIC-0054-0057 (32 total)

---

## Audit / Lifecycle Review

**Score: 9/10**

### Rule Compliance

| Rule | Status |
| :--- | :--- |
| Rule 24 (Execution Integrity) | COMPLIANT — baseline format follows Rule 24 contract |
| Rule 26 (Audit Gate Lifecycle) | COMPLIANT — script uses `audit-` prefix, has `--self-check`, uses exit codes 0-4 |
| Rule 27 (Zero-Bypass Lifecycle) | COMPLIANT — immutability enforcement per story-0059-0011 deliverable |
| Rule 19 (Backward Compatibility) | COMPLIANT — amnesty is additive; no retroactive breaking changes |

### Catalog Entry Required

`scripts/audit-baseline-immutability.sh` should be added to `docs/audit-gates-catalog.md` if that catalog exists. Non-blocking for this story (catalog maintenance is a separate concern).

---

## Final Decision: GO

All DoD criteria met. Three tasks completed and merged (PRs #717, #718, #719). No blocking findings.
