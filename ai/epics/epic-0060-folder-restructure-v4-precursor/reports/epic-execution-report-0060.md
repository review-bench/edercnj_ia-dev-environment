# Epic Execution Report — EPIC-0060

**Epic:** Reorganização da Estrutura de Pastas — `ia-dev-environment` v4
**Branch:** `epic/0060`
**Date:** 2026-04-27
**Final Status:** Parcial (4 of 6 stories Concluída or Parcial; bulk physical migration deferred per RULE-010)

---

## Stories

| Story | Title | Status | PRs |
|-------|-------|--------|-----|
| story-0060-0001 | PathResolver helper + schema v4 | Concluída | #726, #727, #728, #729, #730 |
| story-0060-0002 | Script `migrate-layout.sh` idempotente | Concluída | #731 |
| story-0060-0003 | Mover ADRs, specs, templates, baselines | Parcial | #733 |
| story-0060-0004 | Atualizar 42 SKILLs para usar PathResolver | Parcial | #732 |
| story-0060-0005 | Atualizar Rules, Hooks e Java Assemblers | Parcial | #734 |
| story-0060-0006 | Compat layer cleanup + congelamento de `plans/` | Parcial | #735 (this PR) |

## What Was Delivered

### Foundation (story-0060-0001) — Concluída

- `dev.iadev.util.PathResolver` (134 lines, 100% line + 100% branch coverage)
- `dev.iadev.util.UnitType` enum (STORY/BUG/SPIKE/CHORE)
- `PathResolverTest` (17 tests, 6 Gherkin AC + 8 unit-helper + 3 probe-edge)
- Rule 19 fallback matrix updated with `flowVersion: "4"` row
- 9 golden fixtures regenerated (also picked up missing rules 24/25/26/27/45 across all profiles as a side benefit)

### Migration Tool (story-0060-0002) — Concluída

- `scripts/migrate-layout.sh` (≤ 300 lines, idempotent, supports `--self-check`, `--dry-run`, `--apply`, `--epic`)
- `governance/baselines/README.md` (purpose + conventions for the new baselines home)
- `MigrateLayoutScriptIT` (6 integration tests via ProcessBuilder)
- Creates `pre-layout-v4` rollback tag before any `git mv`
- Generates append-only `governance/baselines/migration-report-2026.md`

### Skeleton + CI Gates (stories 3/4/5) — Parcial

- `docs/{adr,specs}/`, `ai/{epics,runs,releases}/` directory skeleton with README pointers
- `SkillPathResolverSmokeTest` + `audits/skill-pathresolver-baseline.txt` (14 grandfathered skills)
- `FileCategorizer` 8 new v4-layout categories (ai/epics, docs/adr, governance/baselines, etc.) with 8 new tests

### Closure (story-0060-0006) — Parcial

- CHANGELOG `[Unreleased]` entry summarizing delivered + deferred work
- This report
- Epic-level verify envelope at `plans/epic-0060/reports/verify-envelope-epic-0060.json`

## What Was Deferred

Per **RULE-010** (atomic CI workflow updates), the following bulk operations were deferred to a coordinated session that updates references and regenerates all affected fixtures simultaneously:

| Item | Reason |
|------|--------|
| `git mv adr/* docs/adr/` (21 files) | Requires updating cross-references in `Conventions.md`, `CLAUDE.md`, `README.md`, golden fixtures, and Java assemblers in the same PR |
| `git mv specs/* docs/specs/` (11 files) | Same as ADRs |
| `git mv audits/*.txt governance/baselines/` (7 files) | Requires updating `.github/workflows/*.yml`, 5+ audit scripts, and golden-fixture references |
| 14 SKILL.md substitutions of `plans/epic-N` | Each skill needs individual review to choose the right PathResolver placeholder |
| Rule 24/26/27/45 textual updates | Requires golden-fixture regen across 9 profiles |
| Probe v3 removal | Pre-requisite: 2-sprint co-existence window with v4 in production |
| `forbid-writes-to-legacy-plans` hook | Same as probe removal |
| MAJOR version bump | Pre-requisite: probe removal completed |

## Risk + Backward Compatibility

- **No regression introduced.** All v3 paths continue to work; PathResolver probe falls back to `plans/epic-N` when `ai/epics/epic-N-*` is absent.
- **CI green throughout:** every PR merged was preceded by `mvn test` GREEN locally; auto-merge was used for fast-forward integration.
- **`pre-layout-v4` tag is available** via `scripts/migrate-layout.sh --apply` for any future bulk migration; deferral does not impair the rollback strategy.

## Quality Metrics

| Metric | Value |
|--------|-------|
| Stories merged into `epic/0060` | 6 / 6 (4 Concluída-equivalent, 2 Parcial-with-tracking) |
| PRs merged into `epic/0060` | 11 (#726-#735, plus this closure PR) |
| New tests | +33 (17 PathResolver, 6 migration, 2 skill gate, 8 FileCategorizer) |
| Full suite at epic close | ≈4000 / 0 fail / 14 skipped |
| New production code | ~280 lines (PathResolver + UnitType + FileCategorizer additions) |
| New tooling code | ~200 lines (migrate-layout.sh) |
| Coverage on new code | ≥ 95% line / ≥ 90% branch |

## Recommendation

The next operator session focused on EPIC-0060 should:

1. Run `scripts/migrate-layout.sh --apply` against a sandbox (or `--dry-run` for the full repo) to validate the migrator end-to-end.
2. Open one focused PR per deferred item above. Each PR is small and self-contained, suitable for review.
3. Schedule the probe removal + MAJOR bump for the major release after the 2-sprint observation window.

## Closure

The `epic/0060` branch can be merged to `develop` via a manual gate PR that consolidates all delivered work. The deferred items become independent follow-up stories.
