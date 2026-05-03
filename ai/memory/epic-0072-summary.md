---
epic-id: EPIC-0072
slug: comprehensive-test-strategy
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [quality, testing, performance, mutation, contract, governance]
capabilities-affected: [quality.performance, quality.mutation, quality.contract]
rules-affected: [Rule 05, Rule 24, Rule 26]
adrs-referenced: [ADR-0025]

patterns-introduced:
  - conditional-quality-gate-yaml-config
  - d-r11-fast-fail-sequence
  - stack-aware-quality-dispatcher
  - stage-policy-warn-then-fail
antipatterns-rejected:
  - quality-gate-always-on-breaks-existing-projects
  - single-monolithic-quality-gate

dependencies-of: [EPIC-0061, EPIC-0064]
dependencies-for: [EPIC-0074, EPIC-0075]
---
# Memory: EPIC-0072 — Comprehensive Test Strategy

## Why this epic existed

Only unit and integration tests existed as quality gates. No performance baseline, mutation score, or contract test verified meaningful behavior. High line coverage could coexist with weak assertions that never caught regressions (mutation survivors). No gate stopped contract-breaking API changes from merging silently.

## Hypothesis tested

Three **conditional quality gates** (performance, mutation, contract) inserted into `x-story-implement` Phase 3 in a **D-R11 fast-fail sequence** would provide deeper quality signal without blocking existing projects (safe default `enabled: false`, Rule 19). **Confirmed**: 3 stack-aware skills (`x-test-performance`, `x-test-mutation`, `x-test-contract`); 3 audit scripts; `QualityConfig` domain record; Rule 05 §Quality Gates extended; ADR-0025.

## Decisions taken (with why)

1. **D-R11 fast-fail sequence**: performance → mutation → contract → dep-policy (EPIC-0074); first non-zero exit cancels remaining; performance first (cheapest), contract last (needs build artifacts).
2. **Safe default `quality.*.enabled: false`** (Rule 19): existing projects see zero change; each gate opted into independently via YAML.
3. **Stack-aware dispatchers**: `x-test-performance` → Newman/ghz/hyperfine/Artillery; `x-test-mutation` → PIT/Stryker/mutmut/go-mutesting; `x-test-contract` → openapi-diff/buf/SCC/schema-registry.
4. **Stage policy (WARN → FAIL)**: first release with gate enabled → WARN only + baseline initialized; `release_count ≥ 1` → hard FAIL. Prevents adoption shock.
5. **Non-colliding exit codes**: 14 (`x-story-implement` PERF_REGRESSION), 17 (MUTATION_SCORE_BELOW_THRESHOLD), 18 (CONTRACT_BREAKING_CHANGE); chosen to avoid 12/15/33/40-70 ranges.
6. **`governance/baselines/mutation-baseline.json`**: tracks mutation score per release; regression tolerance configurable; tampering detected via `--strict-baseline` against `git tag` count.

## Alternatives rejected (with why)

- **Single quality gate toggle** — can't independently disable expensive mutation for fast feedback; gates must be independently togglable.
- **Mutation gate always enabled** — PIT takes 5-15min on large codebases; opt-in prevents CI latency regressions.

## Reusable patterns produced

- **`conditional-quality-gate-yaml-config`**: gate active only when `quality.X.enabled: true`; absent = disabled.
- **`d-r11-fast-fail-sequence`**: fixed order; first failure cancels remaining; evidence artifact produced per gate.
- **`stack-aware-quality-dispatcher`**: same skill interface; dispatches to stack-appropriate tool internally.
- **`stage-policy-warn-then-fail`**: first release = WARN + initialize baseline; second release = FAIL.

## Anti-patterns observed

- **Quality gate always-on for new features** — blocks adoption; always use safe default `enabled: false` for new gates.
- **Single quality gate covering all dimensions** — one broken dimension blocks everything; gates must be independently controlled.

## Links

- Epic: `ai/epics/epic-0072-comprehensive-test-strategy/epic-0072.md`
- ADRs: `docs/adr/ADR-0025-comprehensive-test-strategy.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0072-comprehensive-test-strategy/reports/`
