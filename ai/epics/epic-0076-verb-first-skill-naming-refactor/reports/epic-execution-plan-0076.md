# Epic Execution Plan — EPIC-0076

**Generated:** 2026-05-03  
**Epic:** Verb-First Skill Naming Refactor  
**flowVersion:** 4  
**Mode:** Sequential  
**Epic Branch:** epic/0076

---

## Execution Waves

| Wave | Stories | Mode | Predecessors |
|------|---------|------|-------------|
| A | story-0076-0001 | serial | — |
| B | story-0076-0002 | serial | Wave A |
| C | story-0076-0003, story-0076-0004, story-0076-0005 | serial (sequential default) | Wave B |
| D | story-0076-0006 | serial | Wave C |
| E | story-0076-0007 | serial | Wave D |

## Critical Path

story-0076-0001 → story-0076-0002 → story-0076-0003 → story-0076-0006 → story-0076-0007

## Dependency Resolution

All external dependencies resolved:
- EPIC-0064: COMPLETE ✓
- EPIC-0065: COMPLETE ✓
- EPIC-0066: Concluída ✓
- EPIC-0069: approved ✓
- EPIC-0072: approved ✓
- EPIC-0073: concluída ✓
- EPIC-0075: approved ✓

## Catalog Snapshot (2026-05-03)

Total skill directories in source of truth: 117
- Core public skills: ~80
- Conditional skills: ~30
- Lib skills: 3
- Internal skills: ~15

Skills requiring rename per SPEC v1.1: ~90
Skills already in verb-first form (no rename): ~10
