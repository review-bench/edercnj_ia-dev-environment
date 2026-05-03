---
epic-id: EPIC-0060
slug: folder-restructure-v4-precursor
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [layout, restructure, path-resolver, v4, migration]
capabilities-affected: []
rules-affected: [Rule 19]
adrs-referenced: []

patterns-introduced:
  - path-resolver-v3-v4-probe
  - v4-layout-skeleton-first
  - flowversion-4-discriminator
antipatterns-rejected:
  - big-bang-migration-without-path-resolver

dependencies-of: [EPIC-0058]
dependencies-for: [EPIC-0062]
---
# Memory: EPIC-0060 — Reorganização da Estrutura de Pastas v4 (Precursor)

## Why this epic existed

The repository's folder structure had grown organically over 60 epics and accumulated structural problems: reviewers had to open 4 directories (`plans/`, `audits/`, `results/`, `/adr/`) to review a single story. Skills resolved paths via hardcoded strings. CI verification was non-uniform. The v3 layout (`plans/`, `audits/`, `results/`, `/adr/`, `/specs/`) needed migration to v4 (`ai/`, `docs/`, `governance/`). However, moving files atomically without updating all cross-references in the same PR would break CI — this epic delivered the foundation (skeleton + `PathResolver`) without moving existing files.

## Hypothesis tested

Delivering the v4 layout skeleton (`ai/epics/`, `ai/runs/`, `ai/releases/`, `docs/adr/`, `docs/specs/`, `governance/baselines/`), `PathResolver` (probe v3↔v4 automatically via filesystem check), and `scripts/migrate-layout.sh` (idempotent migrator) would enable incremental v3→v4 migration in EPIC-0062 without breaking in-flight v3 epics. **Confirmed (partial)**: skeleton, PathResolver, and migrator delivered; physical file migration and reference updates deferred to EPIC-0062 per RULE-010 (atomic CI updates).

## Decisions taken (with why)

1. **PathResolver probe-based (v3↔v4 auto-detect)** — reads filesystem to detect which layout is active; orchestrators remain agnostic to layout version; no explicit flag needed per invocation.
2. **Skeleton-first without moving files** — create the v4 directory structure first; move files in EPIC-0062; prevents broken references during transition.
3. **`flowVersion: "4"` discriminator** — added to Rule 19 fallback matrix; new epics born after EPIC-0060 use v4 layout; legacy epics (0001-0060) remain in `plans/` read-only.

## Alternatives rejected (with why)

- **Big-bang migration in one PR** — moving all 60 epics + updating all references + regenerating goldens in one PR is too large; CI risk is unacceptable.
- **Explicit `--layout-version v4` flag on every skill call** — PathResolver probe is transparent; explicit flags add noise to every orchestrator invocation.

## Reusable patterns produced

- **`path-resolver-v3-v4-probe`**: `PathResolver.resolve()` probes filesystem for `ai/epics/` vs `plans/`; returns canonical path regardless of layout version.
- **`v4-layout-skeleton-first`**: create directory structure + PathResolver first; migrate files in a follow-up epic; prevents broken references during transition.
- **`flowversion-4-discriminator`**: `flowVersion: "4"` in `execution-state.json` marks v4 layout; PathResolver uses this + filesystem probe to resolve paths.

## Anti-patterns observed

- **Big-bang migration** — moving all files + updating all references in a single PR exceeds reviewable scope and breaks CI intermittently.

## Links

- Epic: `ai/epics/epic-0060-folder-restructure-v4-precursor/epic-0060.md`
- ADRs: (none recorded)
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0060-folder-restructure-v4-precursor/reports/`
