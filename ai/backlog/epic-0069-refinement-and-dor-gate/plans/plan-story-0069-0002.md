# Implementation Plan — story-0069-0002

**Story:** Skill `/x-story-refine` (dispatcher multi-persona)
**Epic:** EPIC-0069
**Scope:** STANDARD
**Planning Mode:** INLINE

## Summary

Delivers the `x-story-refine` skill — a multi-persona dispatcher with 4 phases:
- Phase A: parallel specialist analysis (PO, Tech Lead, Architect, Security, QA + conditionals)
- Phase B: single consolidated batch of questions to operator
- Phase C: parallel specialist refinement with operator answers
- Phase D: Architect consolidation (opus tier) + dual-write

## Key Design Decisions (from story §8)
- D1: Multi-persona 4-phase dispatcher (not linear questionnaire)
- D2: Dual-write via x-internal-status-update (INLINE-SKILL, Rule 13)
- D3: Dispatcher=sonnet, personas A/C=sonnet, Architect D=opus
- D4: Single question batch (no per-persona loop)
- D5: NO-GOs silent (not questions)

## Tasks

### TASK-0069-0002-001: Create skill directory and SKILL.md
- Path: `src/main/resources/targets/claude/skills/core/plan/x-story-refine/SKILL.md`
- Copy to `.claude/skills/x-story-refine/SKILL.md`

### TASK-0069-0002-002: Commit and push
