---
epic-id: EPIC-0040
slug: telemetry-skill-execution
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [observability, telemetry, performance, skills]
capabilities-affected: []
rules-affected: [Rule 13]
adrs-referenced: [ADR-0005]

patterns-introduced:
  - hybrid-telemetry-hooks-plus-markers
  - semantic-phase-markers
  - ndjson-per-epic-telemetry
antipatterns-rejected:
  - passive-hooks-only-no-semantic-context

dependencies-of: []
dependencies-for: [EPIC-0041, EPIC-0049]
---
# Memory: EPIC-0040 — Telemetria de Execução de Skills

## Why this epic existed

No visibility into which skill phases were bottlenecks, how long each TDD cycle took, or whether subagents in planning waves were balanced. Skills executed silently with no structured performance data. Identifying regressions across epics required manual inspection of commit timestamps.

## Hypothesis tested

A **hybrid telemetry architecture** (passive hooks for all executions + semantic in-skill phase markers for key orchestrators) would produce rich, queryable NDJSON event streams per-epic without requiring per-skill boilerplate everywhere. **Confirmed**: ADR-0005 delivered; 5 hooks (PreToolUse/PostToolUse/SessionStart/Stop/SubagentStop); two analysis skills (`/x-telemetry-analyze` → Mermaid Gantt; `/x-telemetry-trend` → P95 regression detection).

## Decisions taken (with why)

1. **Hybrid capture** — passive hooks catch all tool calls; explicit `telemetry-phase.sh start/end` markers around numbered phases provide semantic context (which phase, not just which tool call).
2. **NDJSON per-epic** (`ai/epics/epic-XXXX/telemetry/events.ndjson`) — per-epic isolation enables targeted analysis without global index scan.
3. **Fail-open contract** — `telemetry-phase.sh` exits 0 on any error (bad args, missing peer scripts, `CLAUDE_TELEMETRY_DISABLED=1`); skills never abort due to telemetry failures.
4. **`telemetry-phase.sh subagent-start/end`** — planning skills with parallel dispatch emit subagent markers so overlap windows can be computed.

## Alternatives rejected (with why)

- **Passive hooks only** — capture tool calls but not "which phase this call belongs to"; semantic aggregation impossible.
- **External OTEL export** — adds runtime dependency for generated projects; scope is internal analysis only.

## Reusable patterns produced

- **`hybrid-telemetry-hooks-plus-markers`**: hooks = passive coverage; markers = semantic context for phase-level analysis.
- **`semantic-phase-markers`**: `## Phase N` sections get `<!-- TELEMETRY: phase.start -->` + `telemetry-phase.sh` call pairs.
- **`ndjson-per-epic-telemetry`**: isolated, queryable, not a global index.

## Anti-patterns observed

- **Passive-hooks-only**: hooks capture `tool.call` but not "which workflow phase this belongs to"; semantic aggregation becomes impossible.

## Links

- Epic: `ai/epics/epic-0040-telemetry-skill-execution/epic-0040.md`
- ADRs: `docs/adr/ADR-0005-telemetry-architecture.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0040-telemetry-skill-execution/reports/`
