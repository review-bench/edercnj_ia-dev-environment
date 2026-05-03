---
epic-id: EPIC-0049
slug: epic-flow-refactor-sequential-default
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [orchestration, branching, governance, architecture]
capabilities-affected: []
rules-affected: [Rule 19, Rule 21, Rule 22]
adrs-referenced: [ADR-0004]

patterns-introduced:
  - thin-orchestrator-use-case
  - epic-branch-single-integration
  - x-internal-star-convention
  - oo-style-flag-propagation
  - flow-version-discriminator
antipatterns-rejected:
  - parallel-default-to-develop
  - inline-shell-in-orchestrator
  - implicit-flag-derivation

dependencies-of: []
dependencies-for: [EPIC-0055, EPIC-0059, EPIC-0061, EPIC-0064]
---
# Memory: EPIC-0049 — Epic Flow Refactor (Sequential Default, Branch Única, Thin Orchestrators)

## Why this epic existed

Epic execution defaulted to **parallel + auto-merge to `develop`**, causing chronic merge conflicts when sibling stories touched overlapping files. Orchestrators `x-epic-implement` and `x-story-implement` embedded hundreds of lines of inline shell — git, gh, jq, mvn — preventing extraction, reuse, and testing. No single integration branch isolated epic work from `develop`.

## Hypothesis tested

Switching to **sequential default + single `epic/XXXX` branch + thin orchestrators** (UseCase pattern) would eliminate conflicts, produce an auditable manual gate, and reduce orchestrator size by 60%+. **Confirmed**: 15 sub-skills extracted (4 public + 11 `x-internal-*`); conflict rate on `develop` dropped to zero for multi-story epics.

## Decisions taken (with why)

1. **`epic/XXXX` branch per epic** — single integration point; story PRs auto-merge into `epic/XXXX`, not `develop`; manual PR gate `epic/XXXX → develop` (Rule 21).
2. **Sequential default** — `--parallel` is explicit opt-in; avoids file-footprint collisions without pre-analysis.
3. **`x-internal-*` prefix convention** — internal skills must declare `visibility: internal` + `user-invocable: false`; filtered from `/help` (Rule 22).
4. **OO-style flag propagation** — `--target-branch`, `--auto-merge`, `--epic-id` flow explicitly: `x-epic-implement` → `x-story-implement` → `x-pr-create`; never derived implicitly (RULE-009).
5. **`flowVersion` discriminator** — `execution-state.json` carries `"1"` (legacy) or `"2"` (new); absent = legacy + warning; deprecation window = 2 releases (Rule 19).
6. **Auto-merge strategy = merge commit** — preserves per-task TDD commits (RED/GREEN/REFACTOR) for bisect; squash loses granularity.

## Alternatives rejected (with why)

- **Parallel default with conflict analysis** — EPIC-0041 (file footprint) was added later; too heavyweight as default.
- **Squash auto-merge** — loses TDD commit granularity; bisect would land on story-level commit rather than task level.
- **Single flag `--use-epic-branch`** — less safe than making sequential+epic-branch the default; legacy flows already had `--legacy-flow`.

## Reusable patterns produced

- **`thin-orchestrator-use-case`**: orchestrator = arg parse + sequence of `Skill()` calls; zero inline shell.
- **`epic-branch-single-integration`**: `epic/XXXX` is the sole merge target for stories; `develop` is touched only by the final manual PR.
- **`x-internal-star-convention`**: skills invoked only by orchestrators use `x-internal-` prefix + frontmatter fields.
- **`flow-version-discriminator`**: `execution-state.json` top-level `flowVersion` field controls behavior; absent = legacy; explicit = new.

## Anti-patterns observed

- **Parallel-to-develop default** — caused chronic conflicts; never use as default for multi-story epics.
- **Inline shell in orchestrators** — `x-epic-implement` had 1100 lines; unextractable, untestable; always thin-orchestrate.

## Links

- Epic: `ai/epics/epic-0049-epic-flow-refactor-sequential-default/epic-0049.md`
- ADRs: `docs/adr/ADR-0004-worktree-first-branch-creation.md`
- PRs: (sequential series merged into develop)
- Reports: `ai/epics/epic-0049-epic-flow-refactor-sequential-default/reports/`
