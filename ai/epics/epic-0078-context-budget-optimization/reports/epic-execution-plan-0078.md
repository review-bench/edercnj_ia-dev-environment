# Epic Execution Plan — EPIC-0078 Context Budget Optimization

**Generated:** 2026-05-06
**Flow Version:** 4 (v4 layout — ai/epics/)
**Mode:** sequential
**Epic Branch:** epic/0078
**Total Stories:** 17

---

## Execution Phases

| Phase | Stories | Parallelism Notes |
|-------|---------|-------------------|
| 0 — Instrumentação | story-0078-0001 | serial (foundation) |
| 1 — Cleanup + Skills | story-0078-0002, 0003, 0004, 0010 | 0002→0003→0010 serial; 0004 independent |
| 2 — Slim Rules + KPs | story-0078-0005, 0006, 0007, 0008, 0009, 0012 | 0005→0012 serial; 0006/0007/0008/0009 serial (hotspot: KnowledgePacksAssembler) |
| 3 — Lifecycle Consolidation | story-0078-0011, 0015 | serial (0011 first, 0015 after 0012) |
| 4 — Stubs + Capability | story-0078-0013, 0014 | 0013→0014 serial (hotspot: rules frontmatter) |
| 5 — Hardening | story-0078-0016 | serial |
| 6 — Sunset (gated) | story-0078-0017 | requires 2 releases after 0013 |

## Critical Path

```
0001 → 0010 → 0012 → 0011 → 0013 → [2 releases] → 0017
                          ↘
                           0014 → 0016
```

## Dependency Order (Topological Sort)

1. story-0078-0001 (no deps)
2. story-0078-0002 (after 0001)
3. story-0078-0003 (after 0001)
4. story-0078-0004 (after 0001)
5. story-0078-0010 (after 0001)
6. story-0078-0005 (after 0010)
7. story-0078-0006 (after 0010)
8. story-0078-0007 (after 0010)
9. story-0078-0008 (after 0010)
10. story-0078-0009 (after 0003, 0010)
11. story-0078-0012 (after 0010)
12. story-0078-0011 (after 0010, 0012)
13. story-0078-0015 (after 0012)
14. story-0078-0013 (after 0011)
15. story-0078-0014 (after 0011)
16. story-0078-0016 (after 0014, 0015)
17. story-0078-0017 (after 0013 + 2 releases)

## Hotspot Constraints (EPIC-0041 RULE-004)

- `RulesAssembler.java`: stories 0002, 0003, 0011, 0013 — serialize
- `KnowledgePacksAssembler.java`: stories 0005, 0012 — serialize
- `src/test/resources/golden/**`: stories 0002, 0003, 0010, 0011, 0013 — serialize per regen phase
- Rule frontmatter (24/27/29/45): stories 0013, 0014 — serialize

## Story Status at Plan Time

All 17 stories: `refinementVerdict.status = approved` ✓
Epic `refinementVerdict.status = approved` ✓
