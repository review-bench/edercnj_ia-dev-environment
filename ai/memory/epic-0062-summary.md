---
epic-id: EPIC-0062
slug: folder-migration-v4
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [layout, migration, v4, adr, git-mv, history]
capabilities-affected: []
rules-affected: [Rule 19]
adrs-referenced: []

patterns-introduced:
  - git-mv-history-preserving-migration
  - topological-story-order-for-migrations
  - adr-renumbering-canonical-procedure
antipatterns-rejected:
  - single-pr-full-repo-migration

dependencies-of: [EPIC-0060]
dependencies-for: [EPIC-0063, EPIC-0064]
---
# Memory: EPIC-0062 — Migração Física v3→v4 (Finalização do EPIC-0060)

## Why this epic existed

EPIC-0060 delivered the v4 layout skeleton and `PathResolver` but deferred physical file migration per RULE-010 (atomic CI updates). EPIC-0062 finalizes the deferred work: moving 21 ADR files, 11 spec files, 7 baseline files to their v4 paths (`docs/adr/`, `docs/specs/`, `governance/baselines/`), updating 14 SKILL.md files that still had hardcoded `plans/epic-XXXX` path strings, updating Rules 05/13/24/25/26/27/45, regenerating 11 golden fixtures, and renumbering 4 duplicate-numbered ADRs (ADR-0015→ADR-0018, ADR-0016→ADR-0019, ADR-001→ADR-0020, ADR-0048-B→ADR-0021).

## Hypothesis tested

Dividing the migration into 8 topologically ordered stories (audit script parametrization → baselines → ADRs → specs → assembler updates → SKILL conversions → Rules → cleanup) would allow incremental merges to `epic/0062` while keeping CI green at each story boundary. **Confirmed**: 8 stories executed; history preserved via `git mv`; 4 duplicate ADR numbers renumbered; `docs/adr/README.md` index rebuilt with all 22 ADRs; only epics 0001-0060 remain in `plans/` (read-only legacy).

## Decisions taken (with why)

1. **`git mv` with history preservation** — moves files without losing `git log --follow` history; operators can trace ADR lineage across the rename.
2. **Topological ordering of stories** — audit-script parametrization first (prerequisite for all other checks); baselines before ADRs; ADRs before Rules (Rules reference ADR paths); SKILL conversions after Rules.
3. **ADR renumbering** — 4 collision cases renumbered to free slots; `docs/adr/README.md` rebuilt as canonical index.

## Alternatives rejected (with why)

- **Single-PR full migration** — too large; risk of intermittent CI failures during massive golden regeneration + cross-reference rewrite.
- **Path aliases (symlinks) for old paths** — maintains confusion; doubles the maintenance surface; PathResolver probe is the clean solution.

## Reusable patterns produced

- **`git-mv-history-preserving-migration`**: use `git mv` for file relocations; `git log --follow` tracks history across renames.
- **`topological-story-order-for-migrations`**: prerequisite changes (audit parametrization) in early stories; downstream changes (SKILL/Rule updates) in later stories; each story boundary must be CI-green.
- **`adr-renumbering-canonical-procedure`**: when ADR numbers collide, renumber to next free slot; update `docs/adr/README.md` index; update all cross-references in the same PR.

## Anti-patterns observed

- **Single PR full-repo migration** — moving all files + updating all references in one PR exceeds review capacity and breaks CI during the PR.

## Links

- Epic: `ai/epics/epic-0062-folder-migration-v4/epic-0062.md`
- ADRs: (none — this epic reorganized existing ADRs rather than creating new ones)
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0062-folder-migration-v4/reports/`
