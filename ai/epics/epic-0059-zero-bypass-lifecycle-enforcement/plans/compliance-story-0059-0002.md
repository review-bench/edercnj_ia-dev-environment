---
generated-by: x-story-implement-compliance@0792be069d39537b5f4c7c76d7e68372b585697f
generated-at: 2026-04-27T16:50:49Z
story-id: story-0059-0002
---

# Compliance Assessment — story-0059-0002: Origin Markers in Artifacts + Anti-Backfill Audit

## Regulatory Scope

**Applicable regulations:** None (internal tooling — no PII, no regulated data).

## Internal Policy Compliance

### Rule 24 — Execution Integrity

**Compliance:** FULL
- The origin marker frontmatter is a direct extension of Rule 24's mandatory evidence artifact contract.
- `check_frontmatter_origin()` strengthens Camada 3 (CI audit) by validating artifact authenticity, not just presence.
- `check_anti_backfill()` addresses bypass surface `I` (retroactive backfill) documented in the EPIC-0059 spec.

### Rule 26 — Audit Gate Lifecycle

**Compliance:** FULL
- `audit-execution-integrity.sh` already follows the `audit-` prefix convention.
- New functions are internal helpers within the existing script — no new script created, no taxonomy change required.
- `--self-check` extended to verify new functions are present.

### Rule 06 — Security Baseline

**Compliance:** FULL
- No secrets introduced.
- No external network calls.
- SHA passed as positional argument (no injection surface).
- Temp git repos in smoke tests use `mktemp -d` with cleanup in `trap`.

### Rule 05 — Quality Gates

**Target:** ≥ 95% line coverage, ≥ 90% branch coverage
**Scope:** Bash smoke tests cover all 8 acceptance scenarios (AT-01 to AT-08).
**Note:** Bash coverage tooling (kcov/bashcov) is not required by the project; scenario coverage is the proxy metric.

## DoD Compliance Checklist

| DoD Item | Status | Evidence |
| :--- | :--- | :--- |
| Generated-by frontmatter emitted by all 6 planning skills | Addressed in TASK-0059-0002-001 and -002 | SKILL.md modifications |
| Audit validates presence and SHA authenticity | Addressed in TASK-0059-0002-003 | `check_frontmatter_origin()` |
| Anti-backfill check implemented | Addressed in TASK-0059-0002-003 | `check_anti_backfill()` |
| Exemption `<!-- audit-exempt: backfill <url> -->` accepted | Addressed in TASK-0059-0002-003 | Exemption handler update |
| Smoke test: missing frontmatter → exit 1 | Addressed in TASK-0059-0002-003 | AT-01 |
| Smoke test: fictitious SHA → exit 1 | Addressed in TASK-0059-0002-003 | AT-03 |
| At least 1 automated test | Addressed | `audit-anti-backfill-smoke.sh` |

## Data Handling

**Personal Data:** None. Planning artifacts are technical documentation files.
**Sensitive Data:** None. Git SHAs are public repository metadata.
**Retention:** Planning artifacts are committed to the repository and follow standard Git retention.
