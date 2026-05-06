# Execution Plan — EPIC-0070 (Value-Driven Templates v2)

**Generated:** 2026-04-30
**Flow Version:** 4
**Mode:** sequential
**Epic Branch:** epic/0070

## Execution Waves

| Wave | Story | Title | Depends On |
|------|-------|-------|-----------|
| 0 | story-0070-0001 | Capability + Rule 30 + ADR-0019 | — |
| 1 | story-0070-0002 | Reescrever `_TEMPLATE-EPIC.md` v2 | 0001 |
| 1 | story-0070-0003 | Reescrever `_TEMPLATE-STORY.md` v2 | 0001 |
| 1 | story-0070-0004 | `_TEMPLATE-ARCHITECTURE-SYSTEM.md` | 0001 |
| 2 | story-0070-0005 | Atualizar x-epic-create/x-story-create v2 | 0002, 0003 |
| 2 | story-0070-0006 | Skill `/x-arch-system-update` | 0004 |
| 2 | story-0070-0007 | Skill `/x-template-migrate` (v1→v2) | 0002, 0003, 0004 |
| 3 | story-0070-0008 | Smoke + audit + CHANGELOG + supersede EPIC-0056 | 0005, 0006, 0007 |

## Critical Path

story-0070-0001 → story-0070-0004 → story-0070-0006 → story-0070-0008 (4 hops)

## Parallelism Analysis

Sequential mode: 0 parallel waves (all waves run serially).
Stories within each wave run one-at-a-time.
