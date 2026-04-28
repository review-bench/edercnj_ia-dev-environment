# `governance/baselines/`

Centralized location for governance baseline artifacts that gate CI workflows
and audit scripts. Introduced by EPIC-0060 (folder reorganization v4) as part
of the v3 → v4 layout migration.

## Files

| File | Purpose | Owner |
| :--- | :--- | :--- |
| `migration-report-2026.md` | Append-only log of `scripts/migrate-layout.sh` runs | EPIC-0060 |
| `execution-integrity-baseline.txt` | Grandfathered stories for audit-execution-integrity.sh | EPIC-0059 |
| `pr-evidence-baseline.txt` | Grandfathered PRs for audit-pr-evidence.sh | EPIC-0059 |
| `required-checks.txt` | Required CI status checks for branch protection | EPIC-0058 |
| `rule-26-baseline.txt` | Grandfathered exceptions for Rule 26 audit | EPIC-0059 |
| `skill-pathresolver-baseline.txt` | SKILLs grandfathered for PathResolver migration | EPIC-0062 |
| `skill-size-baseline.txt` | SKILLs grandfathered for size audit | EPIC-0046 |
| `task-hierarchy-baseline.txt` | Grandfathered orchestrators for audit-task-hierarchy.sh | EPIC-0055 |
| `baseline-cutoff.sha` | EPIC-0059 amnesty cutoff commit SHA | EPIC-0059 |

> **Migration complete (EPIC-0062):** Files were physically moved
> from `audits/` → `governance/baselines/` via `git mv` (story-0062-0002).
> The transitional symlink `audits/ → governance/baselines/` was removed in
> story-0062-0008. The `audits/` directory no longer exists in the repo root.

## Conventions

- Baselines are **append-only** post-creation; CI gates verify immutability.
- Each entry MUST carry a comment with rationale and origin date.
- New baselines require a Rule update referencing this directory.

## Related

- Rule 19 (`flowVersion` discriminator)
- Rule 26 (Audit Gate Lifecycle — naming)
- EPIC-0060 specification: `specs/SPEC-folder-reorganization-v4.md`
