# Implementation Plan — story-0076-0003

**Story:** Renomear skills públicas de criação, planejamento, refinement e implementação  
**Epic:** EPIC-0076  
**Date:** 2026-05-03

## Scope

Rename 17 lifecycle public skills in source of truth using `git mv` (history preserved) and update `name:` frontmatter.

## Renames Applied

| Old name | New name | Category |
|---|---|---|
| x-feature-ideate | x-ideate-feature | core/plan |
| x-feature-create | x-create-feature | core/plan |
| x-epic-orchestrate | x-orchestrate-epic | core/plan |
| x-arch-plan | x-plan-architecture | core/plan |
| x-arch-update | x-update-architecture | core/plan |
| x-arch-system-update | x-update-system-architecture | core/plan |
| x-adr-generate | x-generate-adr | core/plan |
| x-story-plan | x-plan-story | core/plan |
| x-task-plan | x-plan-task | core/plan |
| x-epic-implement | x-implement-epic | core/dev |
| x-story-implement | x-implement-story | core/dev |
| x-task-implement | x-implement-task | core/dev |
| x-story-refine | x-refine-story | core/plan |
| x-epic-refine | x-refine-epic | core/plan |
| x-threat-model | x-model-threats | core/plan |
| x-spec-drift | x-detect-spec-drift | core/dev |
| x-parallel-eval | x-evaluate-parallelism | core/plan |

## Notes

- Body references (Skill(...) calls) NOT updated here — handled atomically in story-0076-0006
- Knowledge packs untouched (out of scope per RULE-006)
- git mv used for all renames (history preserved)

## DoD Check

- [x] 17/17 lifecycle skills renamed via git mv
- [x] name: frontmatter updated in all 17 SKILL.md files
- [x] Git status shows rename (RM) for all SKILL.md files
