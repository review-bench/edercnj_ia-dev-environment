# Epic Execution Plan -- EPIC-0060

> **Epic ID:** EPIC-0060
> **Title:** Reorganização da Estrutura de Pastas — ia-dev-environment v4
> **Date:** 2026-04-27
> **Total Stories:** 6
> **Total Phases:** 5
> **Author:** x-internal-epic-build-plan
> **Template Version:** 1.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Sequential |
| Max Parallelism | 2 (Phase 1 only) |
| Checkpoint Frequency | Per story completion |
| Dry Run | false |

Sequential execution with one parallelism window (Phase 1: story-0060-0002 and story-0060-0004 are independent and can run in parallel, but sequential is the default mode for this run).

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Estimated Duration | Dependencies |
|-------|------|---------|-------------|-------------------|--------------|
| 0 | Foundation: PathResolver | story-0060-0001 | 1 (serial) | M | — |
| 1 | Migration + Skills Update | story-0060-0002, story-0060-0004 | 2 (parallelizable) | L | Phase 0 complete |
| 2 | Physical Moves | story-0060-0003 | 1 (serial) | L | story-0060-0002 merged |
| 3 | Governance Update | story-0060-0005 | 1 (serial) | L | story-0060-0003 AND story-0060-0004 merged |
| 4 | Finalization | story-0060-0006 | 1 (serial) | M | Phase 3 complete + 2-sprint co-existence window |

> **Total estimated duration:** ~2 sprints (8 weeks) including testing, freeze window, and post-migration observation period.

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0060-0001 | PathResolver helper + schema v4 | 0 | — | Yes | M |
| 2 | story-0060-0002 | Script `migrate-layout.sh` idempotente | 1 | story-0060-0001 | Yes | L |
| 3 | story-0060-0004 | Atualizar 42 SKILLs para usar PathResolver | 1 | story-0060-0001 | No | L |
| 4 | story-0060-0003 | Mover ADRs, specs, templates, baselines | 2 | story-0060-0002 | Yes | L |
| 5 | story-0060-0005 | Atualizar Rules, Hooks e Java Assemblers | 3 | story-0060-0003, story-0060-0004 | Yes | L |
| 6 | story-0060-0006 | Compat layer cleanup + freeze plans/ | 4 | story-0060-0005 | Yes | M |

> **Critical Path Legend:** `Yes` = story is on the critical path (delay impacts epic deadline); `No` = story has slack.
> **Estimated Effort:** `S` (small), `M` (medium), `L` (large), `XL` (extra-large).

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | PASS | All 6 story files found under plans/epic-0060/ |
| Dependencies resolved | PASS | All blockedBy references resolve to existing story files |
| Circular dependencies | PASS | Kahn's algorithm completed — no cycle detected (6 stories emitted == 6 total) |
| Implementation map valid | PASS | IMPLEMENTATION-MAP.md present and dependency matrix consistent with story declarations |

DAG validation: in-degrees computed (S1=0, S2=1, S3=1, S4=1, S5=2, S6=1). Kahn topological sort produced 5 phases. All 6 stories emitted.

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Estimated tokens | ~500k–800k | 6 stories × TDD cycles + specialist reviews |
| Estimated wall time | ~8 weeks | Includes 1-day freeze window (Story 2) and 2-sprint co-existence (Story 6) |
| Max parallel subagents | 5 | x-story-plan parallel planning wave per story |
| Peak memory estimate | ~4GB | During migrate-layout.sh --apply on full repo |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| PathResolver probe incorrect v3/v4 detection | High | Possible | Thorough unit tests in story-0060-0001 before downstream stories start |
| migrate-layout.sh not idempotent | High | Possible | story-0060-0002 acceptance tests verify idempotency (double-run test) |
| CHANGELOG.md merge conflict (Stories 3+6) | Medium | Likely | Stories are in different phases (2 and 4) — sequential execution prevents conflict |
| Story-0060-0005 blocked waiting for Story-0060-0004 | Medium | Likely | Monitor Story-0060-0004 (42 SKILL.md updates) as it is not on the critical path but is required for Story-0060-0005 |
| CI breakage from baseline path changes (Story 3 → Story 5) | High | Likely | Rule-010: .github/workflows/*.yml updated atomically in same PR as baseline moves |
| 2-sprint co-existence period creates ambiguity | Low | Unlikely | PathResolver probe is deterministic; dual-path resolution is by design |

> **Severity levels:** Critical, High, Medium, Low.
> **Likelihood levels:** Very Likely, Likely, Possible, Unlikely.

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | Per story completion |
| Save on phase completion | true |
| Save on story completion | true |
| Save on integrity gate failure | true |
| State file location | plans/epic-0060/execution-state.json |

### Recovery Procedures

On story failure: fix the issue, then re-run `x-epic-implement EPIC-0060 --resume --story <failed-story-id>`. The `execution-state.json` checkpoint preserves the status of completed stories.

On integrity gate failure: the orchestrator offers a FIX-PR / ABORT gate menu. Use `--revert-on-failure` to automatically revert the last story merge.

### Resume Behavior

`--resume` reads `execution-state.json.storyStatuses`, skips stories with `status=SUCCESS`, and re-enters the story loop from the first `PENDING` or `FAILED` story. Phase gates verify artifact presence before entering each phase.
