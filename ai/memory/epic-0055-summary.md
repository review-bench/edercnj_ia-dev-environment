---
epic-id: EPIC-0055
slug: task-hierarchy-phase-gates
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [governance, orchestration, observability, phase-gates]
capabilities-affected: []
rules-affected: [Rule 19, Rule 24, Rule 25]
adrs-referenced: [ADR-0014]

patterns-introduced:
  - phase-gate-pre-post
  - hierarchical-task-subject
  - wave-parallel-batch-ab
  - task-hierarchy-four-levels
antipatterns-rejected:
  - silent-skill-delegation
  - operator-blind-execution

dependencies-of: [EPIC-0049]
dependencies-for: [EPIC-0059, EPIC-0061]
---
# Memory: EPIC-0055 — Task Hierarchy & Phase Gate Enforcement

## Why this epic existed

After EPIC-0049 introduced thin orchestrators, **task granularity visibility collapsed**: only `x-review` emitted `TaskCreate`/`TaskUpdate`. All other orchestrators delegated silently via `Skill(...)` — operators could not see which phase was running, which subagent was still executing, or where the TDD cycle was stuck. Phase failures were invisible until the final Stop hook.

## Hypothesis tested

A **4-level task hierarchy** (Epic › Story › Phase › Wave/Cycle) with **formal PRE/POST phase gates** backed by a dedicated `x-internal-phase-gate` skill would restore operator visibility and enable synchronous blocking of phase transitions until evidence exists on disk. **Confirmed**: 8 orchestrators retrofitted; phase stalls surfaced immediately via TaskCreate subject hierarchy; Rule 24 artifact checks promoted to synchronous gate.

## Decisions taken (with why)

1. **Rule 25**: invariants for `TaskCreate` per phase, `TaskCreate` per wave member, `subject` hierarchy regex, `x-internal-phase-gate` PRE/POST mandatory (ADR-0014).
2. **`x-internal-phase-gate` skill**: internal, haiku-tier; 4 modes (pre/post/wave/final); exit 12 `PHASE_GATE_FAILED`; reads `metadata.expectedArtifacts` from `TaskGet`.
3. **`subject` separator = `›` (U+203A)**: ASCII `>` is forbidden; regex validated by `audit-task-hierarchy.sh`.
4. **`taskTracking` field in `execution-state.json`**: backward-compatible (Rule 19); `flowVersion=2` requires `enabled: true` (EPIC-0059 hardened this).
5. **Internal skills exempt**: `x-internal-*` do NOT emit `TaskCreate`; their calling orchestrator owns the tracking boundary (Invariant 6).
6. **wave/final as POST variants**: both validate POST plus extra checks; satisfies POST requirement.

## Alternatives rejected (with why)

- **TodoWrite/TodoRead for tracking** — legacy tool; conflicts with structured `TaskCreate`; replaced entirely.
- **Orchestrator-internal progress logs** — markdown table logs in `execution-state.json` instead of structured tasks; not queryable.
- **Phase gates as advisory only** — without blocking (exit 12), operators ignored failures; gates must hard-block.

## Reusable patterns produced

- **`phase-gate-pre-post`**: every `## Phase N` must have `--mode pre` + one POST-family gate; exception → `<!-- phase-no-gate: reason -->`.
- **`hierarchical-task-subject`**: `root › Level2 › Level3 › Level4`; max 4 levels; `›` separator mandatory.
- **`wave-parallel-batch-ab`**: Batch A = all `TaskCreate` + parallel `Agent()` in one message; Batch B = all `TaskUpdate(completed)` in one message after results.

## Anti-patterns observed

- **Silent delegation** — `Skill(...)` calls without any task tracking; operator sees nothing during execution.
- **TodoWrite in orchestrators** — produces flat list, no hierarchy, no dependency chaining.

## Links

- Epic: `ai/epics/epic-0055-task-hierarchy-phase-gates/epic-0055.md`
- ADRs: `docs/adr/ADR-0014-task-hierarchy-and-phase-gates.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0055-task-hierarchy-phase-gates/reports/`
