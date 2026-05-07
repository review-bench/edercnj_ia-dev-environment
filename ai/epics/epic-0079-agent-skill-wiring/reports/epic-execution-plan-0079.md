# Epic Execution Plan — EPIC-0079 Native Agent–Skill Wiring & Cleanup

**Generated:** 2026-05-07  
**Flow Version:** 4  
**Epic Branch:** epic/0079  
**Mode:** Sequential  

## Execution Order (Topological Sort)

| Order | Story | Blocked By | Phase |
| :--- | :--- | :--- | :--- |
| 1 | story-0079-0001 | — | 0 (Foundation) |
| 2 | story-0079-0002 | 0001 | 1 (Migration Wave) |
| 3 | story-0079-0003 | 0001 | 1 (Migration Wave) |
| 4 | story-0079-0004 | 0001 | 1 (Migration Wave) |
| 5 | story-0079-0005 | 0001 | 1 (Migration Wave) |
| 6 | story-0079-0006 | 0001, 0002, 0003 | 2 (Closure Wave) |
| 7 | story-0079-0007 | 0002, 0003 | 2 (Closure Wave) |

## Critical Path

`story-0079-0001 → story-0079-0002 → story-0079-0006`  
Length: 3 stories (3 phases)

## DAG Summary

- Phase 0 (Foundation): 1 story — 0001
- Phase 1 (Migration Wave): 4 stories — 0002, 0003, 0004, 0005 (sequential in this run)
- Phase 2 (Closure Wave): 2 stories — 0006, 0007 (sequential in this run)

## Status

Generated at plan build time. Updated during Phase 3 story execution.
