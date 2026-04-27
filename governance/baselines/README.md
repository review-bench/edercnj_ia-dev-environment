# `governance/baselines/`

Centralized location for governance baseline artifacts that gate CI workflows
and audit scripts. Introduced by EPIC-0060 (folder reorganization v4) as part
of the v3 → v4 layout migration.

## Files

| File | Purpose | Owner |
| :--- | :--- | :--- |
| `migration-report-2026.md` | Append-only log of `scripts/migrate-layout.sh` runs | EPIC-0060 |
| `*-baseline.txt` | Per-audit baselines (e.g., lifecycle-integrity, execution-integrity) | The owning epic |

## Conventions

- Baselines are **append-only** post-creation; CI gates verify immutability.
- Each entry MUST carry a comment with rationale and origin date.
- New baselines require a Rule update referencing this directory.

## Related

- Rule 19 (`flowVersion` discriminator)
- Rule 26 (Audit Gate Lifecycle — naming)
- EPIC-0060 specification: `specs/SPEC-folder-reorganization-v4.md`
