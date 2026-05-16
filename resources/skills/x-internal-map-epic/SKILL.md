---
name: x-internal-map-epic
description: "Generates Implementation Map from Epic and Stories: deps, phases, critical path."
visibility: internal
user-invocable: false
model: sonnet
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
requires-capabilities: []
context-budget: medium
---

> 🔒 **INTERNAL SKILL** — Invoked only by other skills via the Skill tool. Not user-invocable.

## Output Policy

- **Language**: Portuguese (pt-BR) for all content. English for technical terms (cache, timeout, handler, endpoint) and code identifiers.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Implementation Map Generator (slim — ADR-0012)

## Purpose

Take the Epic and all Story files and compute the implementation plan: which stories can run in parallel, what the minimum implementation time is, where the bottlenecks are, and how to optimize team allocation. Produce ASCII phase diagrams, Mermaid dependency graphs, and strategic observations for sprint planning.

## Triggers

- `/x-epic-map <epic_file>` — generate implementation map from epic and stories
- User asks to create an implementation map, generate a dependency graph, or compute implementation phases
- User mentions sequencing stories, finding bottlenecks, computing parallel work streams, or building a roadmap from an epic

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `<EPIC_FILE>` | Path | Yes | — | Path to the Epic file (with story index and dependency declarations) |
| `--dry-run` | Boolean | No | `false` | When true, IMPLEMENTATION-MAP.md is written to disk but Steps P4/P5 (planning-commit / push) become no-ops (EPIC-0049 / RULE-007). |

## Prerequisites

**Template (output structure):**
- `.claude/templates/_TEMPLATE-IMPLEMENTATION-MAP.md` — Exact structure to follow

**Required inputs:**
- The Epic file (with story index and dependency declarations)
- All Story files (with their Blocked By / Blocks tables)

## Workflow Overview

```text
P1: WORKTREE_DETECT   -> Skill x-manage-worktrees detect-context (advisory)
P2: EPIC_BRANCH       -> Skill x-internal-ensure-epic-branch --epic-id <XXXX>
 1: DAG_BUILD         -> Dependency matrix; validate symmetry, cycles, roots
 2: PHASE_COMPUTE     -> Topological grouping (phase N = deps in 0..N-1); ASCII diagram
 3: CRITICAL_PATH     -> Longest chain of dependencies; ASCII rendering
 4: MERMAID_GRAPH     -> graph TD with phase classDef coloring
 5: PHASE_TABLE       -> Phase summary (camada, paralelismo, pre-requisito)
 6: PHASE_DETAILS     -> Per-phase artifact + delivery tables
 7: STRATEGIC_OBS     -> Gargalo, leaves, otimização, convergência, marco arquitetural
 8: TASK_DAG          -> (Optional) Task-level cross-story graph with topo sort
 8.5: PARALLELISM     -> Skill x-evaluate-parallelism (fail-open per RULE-006)
 9: SAVE_REPORT       -> Write IMPLEMENTATION-MAP.md and emit summary
P4: PLANNING_COMMIT   -> Skill x-commit-planning (skipped on --dry-run)
P5: PUSH_BRANCH       -> Skill x-push-branch (skipped on --dry-run; WARN on failure)
```

Each step's full procedural detail (bash blocks, ASCII templates, Mermaid classDef palette, task-graph topological sort, parallelism analysis fail-open contract, planning status propagation CLI) lives in [`references/full-protocol.md`](references/full-protocol.md):

- **Steps P1–P2** (§Workflow Step-by-Step): worktree context detection (fail-open) + canonical `epic/<ID>` branch ensure via `x-internal-ensure-epic-branch`.
- **Step 1** (§Step 1 Build the Dependency Matrix): full matrix shape (`Story | Titulo | Chave Jira | Blocked By | Blocks | Status`), symmetry/cycle/roots validation rules, `> **Nota:**` implicit-dependency callouts.
- **Step 2** (§Step 2 Compute Phases): topological grouping algorithm; ASCII box-drawing template; phase naming convention with "(paralelo)" suffix.
- **Step 3** (§Step 3 Critical Path): longest-chain ASCII rendering example.
- **Step 4** (§Step 4 Mermaid): full `classDef` palette (fase0..fase3, faseQE, faseTD, faseCR) with hex colors; Jira-key node label format; `%% Fase N -> N+1` edge grouping convention.
- **Steps 5–7** (§Step 5/6/7): phase summary table; per-phase artifact subsections; strategic observation framework (gargalo, folhas, otimização, dependências cruzadas, marco arquitetural).
- **Step 8 (8A–8D)** (§Step 8 Task-Level Dependency Graph): cross-story task dependency extraction; RULE-012 consistency validation; topological sort with deterministic tie-break; Mermaid `graph LR` with subgraph-per-story coloring and `-.->|cross-story|` edges.
- **Step 8.5** (§Step 8.5 Parallelism Conflict Analysis): `/x-evaluate-parallelism` invocation; fail-open body when skill unavailable; deterministic pair-ordering (Phase ASC → Story-A → Story-B); degenerate case for zero conflicts.
- **Step 9** (§Step 9 Save and Report): output location and summary metrics.
- **Steps P4–P5** (§Step P4/P5): `x-commit-planning` invocation with idempotency contract (`noOp=true` on byte-identical content); push fail-open WARN policy.

## Planning Status Propagation (Rule 22 / EPIC-0046)

> V2-gated: only runs when the epic declares `planningSchemaVersion: "2.0"` in its `execution-state.json`. For v1 epics: skip silently (Rule 19).

`x-epic-map` is **read-only** with respect to story lifecycle status. It populates two map columns by reading each story file:

- **Planejamento** — mirrors story `**Status:**` (Pendente/Planejada/Em Andamento/Concluída/Falha/Bloqueada).
- **Status** — execution lifecycle from `execution-state.json` (driven by `x-implement-story` downstream).

Both columns resolved at render time via `StatusFieldParserCli`. The skill NEVER transitions any story. Full propagation procedure (CLI invocation pattern, exit code 20 abort behavior, post-render commit via `x-commit-changes`) in [`references/full-protocol.md`](references/full-protocol.md) §Planning Status Propagation.

## Error Handling

| Scenario | Action |
|----------|--------|
| Template `_TEMPLATE-IMPLEMENTATION-MAP.md` missing | Abort: "Template not found" |
| Epic file missing or unparseable | Abort: "Epic file not found or invalid format" |
| Story files missing | Abort: "No story files found in epic directory" |
| Circular dependency detected | Warn with cycle details; break the cycle and proceed |
| Asymmetric dependency (A blocks B but B does not list A) | Fix automatically; note the correction |
| No root stories (all have blockers) | Warn: "All stories have blockers — review for missing root stories" |
| Cross-story task dep without story-level dep (RULE-012) | Abort with inconsistent dependency listing |
| Cycle in task dependency graph | Abort: "Cycle detected: TASK-A -> TASK-B -> TASK-A" |
| Stories without formal task IDs | Skip Section 8 with note; proceed with story-level map only |
| `/x-evaluate-parallelism` skill missing or exit ≥ 1 (Step 8.5) | Fail-open: emit section 8.5 with "análise pulada — não disponível"; log WARNING; continue |
| `x-internal-ensure-epic-branch` fails (Step P2) | Abort with `EPIC_BRANCH_ENSURE_FAILED` |
| `x-commit-planning` exit 4 (Step P4) | Abort with `COMMIT_FAILED` |
| `x-commit-planning` exit 0 + `noOp=true` (Step P4) | Silent no-op — idempotency confirmed; continue to Step P5 |
| `x-push-branch` fails (Step P5) | WARN only; local commit preserved; operator re-runs manually |
| `--dry-run` set | Steps P4/P5 are no-ops with log line `"dry-run, skipping ..."` |
| `StatusFieldParserCli` exit 20 on any story (status propagation) | Abort map generation (RULE-046-08 fail-loud) |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-epic-create` | reads | Reads Epic file with story index and dependencies |
| `x-story-create` | reads | Reads generated story files for dependency details |
| `x-epic-decompose` | called-by | Orchestrator invokes x-epic-map in Phase D |
| `x-implement-epic` | reads | Reads the map for execution order |
| `x-manage-worktrees` | calls (Step P1) | Detect-context (EPIC-0049 / RULE-001) |
| `x-internal-ensure-epic-branch` | calls (Step P2) | Ensure `epic/<ID>` exists locally + origin |
| `x-evaluate-parallelism` | calls (Step 8.5) | Collision detection (fail-open per RULE-006) |
| `x-commit-planning` | calls (Step P4) | Batch-commit map without code pre-commit chain |
| `x-push-branch` | calls (Step P5) | Push canonical epic branch to origin (optional) |
| `x-commit-changes` | calls (status propagation) | Commit refreshed lifecycle columns |

## Knowledge Pack References

| Knowledge Pack | Usage |
|----------------|-------|
| `.claude/knowledge/story-planning.md` | Phase computation, dependency DAG, critical path analysis |
| `.claude/knowledge/parallelism-heuristics.md` | Step 8.5 collision categories (hard/regen/soft) |

## Common Mistakes (Recap)

The strategic observation section (Step 7) is where most maps go wrong: generic observations, missing convergence analysis, inconsistent status between matrix and phase diagram, and missing leaf-story identification. Full anti-pattern catalog in [`references/full-protocol.md`](references/full-protocol.md) §Common Mistakes.

## Full Protocol

Minimum viable contract above. Detailed step-by-step bash (telemetry markers per phase, full dependency-matrix validation, Mermaid classDef palette with hex colors, task-graph topological sort with deterministic tie-break, parallelism-analysis fail-open contract, planning status propagation CLI invocation), template snippets, and worked examples live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
