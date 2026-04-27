# `docs/` — v4 Documentation Root (EPIC-0060)

Centralized documentation root introduced by EPIC-0060 as part of the
v3 → v4 layout reorganization.

## Sub-directories

| Path | Purpose | Source (legacy v3) |
| :--- | :--- | :--- |
| `docs/adr/` | Architecture Decision Records | `adr/` (root) |
| `docs/specs/` | Specifications and design documents | `specs/` (root) |

## Migration Status (story-0060-0003)

**Phase 1 — Directory skeleton (this PR):** Empty directories with README pointers created.

**Phase 2 — Bulk content move (deferred):** The actual `git mv` of 21 ADRs and 11 SPECs requires a coordinated session that simultaneously:
- updates `.github/workflows/*.yml` references,
- regenerates 9 golden fixtures,
- updates `Conventions.md`, `CLAUDE.md`, `README.md` cross-references,
- updates `DocsAdrAssembler.java` and related Java assemblers.

This deferral is consistent with RULE-010 (atomic CI workflow updates).

**Phase 3 — Legacy roots removed:** After Phase 2, `adr/` and `specs/` at the repo root are removed; only `docs/{adr,specs}/` remains.

## Cross-references

- Specification: `specs/SPEC-folder-reorganization-v4.md`
- Epic: `plans/epic-0060/epic-0060.md`
- Migration script: `scripts/migrate-layout.sh`
