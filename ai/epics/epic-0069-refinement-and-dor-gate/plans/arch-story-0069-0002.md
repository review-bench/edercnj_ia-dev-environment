# Architecture Plan — story-0069-0002

**Story:** Skill `/x-story-refine`

## Architecture
Content-layer skill (SKILL.md only). No Java code. Follows Rule 13 Pattern 2 (SUBAGENT-GENERAL) for persona dispatch and Pattern 1 (INLINE-SKILL) for x-internal-status-update.

## Phase Flow
```
Phase A: 5-7 sibling Agent() in one message → collect gap-reports
Phase B: dedup + group → one batch → operator responds → distribute
Phase C: 5-7 sibling Agent() in one message (with answers) → collect proposedSections
Phase D: 1 Agent() Architect (opus) → merge + Refinement Verdict → dual-write
```

## Dependency Direction
Skill → agents/core/*.md (read-only) + x-internal-status-update (delegation)
