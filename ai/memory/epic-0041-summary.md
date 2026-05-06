---
epic-id: EPIC-0041
slug: file-conflict-aware-parallelism
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [orchestration, parallelism, planning, conflict-detection]
capabilities-affected: []
rules-affected: [Rule 04]
adrs-referenced: [ADR-0006]

patterns-introduced:
  - file-footprint-declaration
  - parallelism-collision-matrix
  - wave-demotion-to-serial
antipatterns-rejected:
  - topological-dag-only-parallelism

dependencies-of: [EPIC-0040]
dependencies-for: [EPIC-0049, EPIC-0055]
---
# Memory: EPIC-0041 — File-Conflict-Aware Parallelism Analysis

## Why this epic existed

Parallelism analysis was purely topological (DAG of `Blocked By`). Stories without declared dependencies were treated as parallelizable even when both wrote to the same files (e.g., `SettingsAssembler.java`, golden files, `CLAUDE.md`), causing recurrent merge conflicts during parallel worktree execution.

## Hypothesis tested

Requiring planning skills to declare a structured **File Footprint** (`write:/read:/regen:`) and introducing `/x-parallel-eval` to compute a **collision matrix** would allow automatic demotion of conflicting pairs to serial execution within the same phase. **Confirmed**: ADR-0006 delivered; hotspot catalog in RULE-004; `ExecutionState.parallelismDowngrades` field tracks demotions.

## Decisions taken (with why)

1. **`File Footprint` in every task/story plan** — `write:`, `read:`, `regen:` blocks become mandatory output of `x-task-plan` and `x-story-plan`.
2. **`/x-parallel-eval --scope=epic|story|task`** — consumes footprints, produces collision matrix (hard/regen/soft), recommends serial demotion; new skill in `plan/` category.
3. **`x-epic-map` Step 8.5** — invokes analysis and annotates map with "Restrições de Paralelismo".
4. **Plans predating EPIC-0041** treated as "footprint unknown" — warn, do not block (RULE-006); retroactive patches generated for epics 0036–0040.

## Alternatives rejected (with why)

- **No parallelism by default** — eliminates conflicts but kills throughput; most stories don't conflict.
- **Manual conflict declaration** — error-prone; operators forget; automated analysis required.

## Reusable patterns produced

- **`file-footprint-declaration`**: plans declare `write:/read:/regen:` blocks; enables automated conflict analysis.
- **`parallelism-collision-matrix`**: hard/regen/soft collision types; hard = must serialize.
- **`wave-demotion-to-serial`**: visible warning + `parallelismDowngrades` record when parallel wave demoted.

## Anti-patterns observed

- **Topological-DAG-only parallelism**: `Blocked By` is necessary but not sufficient for parallelism safety; file footprint required.

## Links

- Epic: `ai/epics/epic-0041-file-conflict-aware-parallelism/epic-0041.md`
- ADRs: `docs/adr/ADR-0006-file-conflict-aware-parallelism.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0041-file-conflict-aware-parallelism/reports/`
