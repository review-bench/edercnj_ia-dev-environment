# Epic Execution Plan — EPIC-0061: Local-First Lifecycle & Stack-Aware Governance

**Generated:** 2026-04-28  
**Flow Version:** 4 (v4 layout — `ai/epics/epic-0061-local-first-governance/`)  
**Mode:** Sequential (worktree isolation on `epic/0061`)  
**Epic Branch:** `epic/0061` → `develop` (manual gate)  

---

## Execution Order (Sequential)

| Order | Story | Title | Blocked By | Status |
| :--- | :--- | :--- | :--- | :--- |
| 1 | story-0061-0001 | Non-Interactive Default + Working-Tree Guard | — | PENDING |
| 2 | story-0061-0002 | ScriptsAssembler Stack-Aware + Templates por Stack | — | PENDING |
| 3 | story-0061-0003 | Catálogo Dinâmico + DocsAssembler | 0002 | PENDING |
| 4 | story-0061-0004 | Java Audit Harness + Smoke Equivalência | 0002 | PENDING |
| 5 | story-0061-0005 | Migração: Remoção de `scripts/audit-*.sh` + `audit.yml` | 0004 | PENDING |
| 6 | story-0061-0006 | Camada 0 + Rule 26 Amendment + ADR-0017 | 0005 | PENDING |
| 7 | story-0061-0007 | flowVersion `"3"` + Migration Script para Legados | 0006 | PENDING |

> Note: Per IMPLEMENTATION-MAP, stories 0001‖0002 CAN run in parallel (disjoint footprint),
> and stories 0003‖0004 CAN run in parallel (disjoint footprint). Sequential execution chosen
> for safety (user has in-progress changes in develop working tree).

---

## Phase Mapping

| Phase | Stories | Type | Parallelism Available |
| :--- | :--- | :--- | :--- |
| Phase 0 | 0001 | Non-blocking independent | ‖ Phase 1 |
| Phase 1 | 0002 | Critical path | Sequential |
| Phase 2 | 0003, 0004 | Critical path (0004 is bottleneck) | ‖ each other |
| Phase 3 | 0005 | Critical path | Sequential |
| Phase 4 | 0006 | Critical path | Sequential |
| Phase 5 | 0007 | Terminal | Sequential |

---

## Critical Path

`S2 → S4 → S5 → S6 → S7` (5 sequential stories)

**Bottleneck:** Story 0004 (Java Audit Harness — 8 Auditor classes + AuditEquivalenceSmokeIT)

---

## Worktree Strategy

```
develop (origin) ─────────────────────────────────────────────────────► (manual PR gate at end)
                  │
                  ↓ (Phase 2: epic/0061 branch from origin/develop)
epic/0061 ────────●──[S1 PR]──[S2 PR]──[S3 PR]──[S4 PR]──[S5 PR]──[S6 PR]──[S7 PR]──●
                                                                                       │
                                                       worktree: .claude/worktrees/epic-0061/
```

Each story PR:
- Branch: `feat/story-0061-XXXX` (off `epic/0061`)
- Target: `epic/0061`
- Auto-merge strategy: `merge`

---

## Artifacts

| Artifact | Path |
| :--- | :--- |
| Epic doc | `ai/epics/epic-0061-local-first-governance/epic-0061.md` |
| Implementation map | `ai/epics/epic-0061-local-first-governance/IMPLEMENTATION-MAP.md` |
| Execution state | `ai/epics/epic-0061-local-first-governance/execution-state.json` |
| This plan | `ai/epics/epic-0061-local-first-governance/reports/epic-execution-plan-0061.md` |
| Per-story plans | `ai/epics/epic-0061-local-first-governance/plans/` |
| Per-story reports | `ai/epics/epic-0061-local-first-governance/reports/` |
| Telemetry | `ai/epics/epic-0061-local-first-governance/telemetry/events.ndjson` |
