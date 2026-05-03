# Implementation Plan — story-0076-0005

**Story:** Renomear skills internas e libs  
**Epic:** EPIC-0076  
**Date:** 2026-05-03

## Scope

Rename 18 internal skills and 2 lib skills in source of truth using `git mv` (history preserved) and update `name:` frontmatter. Body cross-references NOT updated here — handled atomically in story-0076-0006.

## Renames Applied

### 6.9 Internals (18 skills)

| Old name | New name | Source path |
|---|---|---|
| x-internal-args-normalize | x-internal-normalize-args | core/internal/ops |
| x-internal-epic-branch-ensure | x-internal-ensure-epic-branch | core/internal/git |
| x-internal-epic-build-plan | x-internal-build-epic-plan | core/internal/plan |
| x-internal-epic-integrity-gate | x-internal-verify-epic-integrity | core/internal/plan |
| x-internal-epic-create | x-internal-create-epic | core/internal/plan |
| x-internal-epic-map | x-internal-map-epic | core/internal/plan |
| x-internal-epic-summary | x-internal-summarize-epic | core/internal/memory |
| x-internal-phase-gate | x-internal-verify-phase-gates | core/internal/plan |
| x-internal-report-write | x-internal-write-report | core/internal/ops |
| x-internal-status-update | x-internal-update-status | core/internal/ops |
| x-internal-story-build-plan | x-internal-build-story-plan | core/internal/plan |
| x-internal-story-create | x-internal-create-story | core/internal/plan |
| x-internal-story-load-context | x-internal-load-story-context | core/internal/plan |
| x-internal-story-report | x-internal-write-story-report | core/internal/plan |
| x-internal-story-resume | x-internal-resume-story | core/internal/plan |
| x-internal-story-verify | x-internal-verify-story | core/internal/plan |
| x-internal-worktree-precheck | x-internal-precheck-worktree | core/internal/git |
| x-internal-pr-body-render | x-internal-render-pr-body | core/internal/pr |

### 6.10 Libs (2 renames, 1 unchanged)

| Old name | New name | Source path |
|---|---|---|
| x-lib-group-verifier | x-lib-verify-group | core/lib |
| x-lib-task-decomposer | x-lib-decompose-task | core/lib |

*Unchanged: x-lib-audit-rules (already verb-first: "audit" is the verb)*

## Summary

**Total renames: 20 skills**

## Notes

- Body references (Skill(...) calls) NOT updated here — handled atomically in story-0076-0006
- git mv used for all renames (history preserved)
- x-lib-audit-rules: "audit" is the verb + "rules" is the object — already in verb-first form, no change needed

## DoD Check

- [ ] 20/20 internal and lib skills renamed via git mv
- [ ] name: frontmatter updated in all 20 SKILL.md files
- [ ] Git status shows rename (RM) for all SKILL.md files
