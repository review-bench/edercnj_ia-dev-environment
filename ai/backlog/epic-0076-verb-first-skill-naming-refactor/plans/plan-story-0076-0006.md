# Implementation Plan — story-0076-0006

**Story:** Atualizar source of truth, Java, templates, docs e testes  
**Epic:** EPIC-0076 — Verb-First Skill Naming Refactor  
**Phase:** 3 — Cross-Surface Integration  

---

## Summary

Update all cross-references across source-of-truth skills, rules, agents, templates, hooks, scripts, and generated `.claude/` output to use the new verb-first skill names established in stories 0076-0003 through 0076-0005.

## Approach

1. Python script with word-boundary regex (`(?<![a-z0-9-])old(?![a-z0-9-])`) applied to all `.md`, `.sh`, `.json` files
2. Script covers all 84 rename pairs from SPEC §6.1–6.10
3. Sources updated: `src/main/resources/targets/claude/skills/**/*.md`, rules, agents, hooks, templates, `docs/**/*.md`, `CLAUDE.md`
4. Generated output `.claude/skills/` directories renamed via `mv` (gitignored)
5. Hook scripts updated: `enforce-no-bypass-flags.sh`, `enforce-phase-sequence.sh`, `enforce-refinement-gate.sh`, `verify-story-completion.sh`, `enforce-preflight-gates.sh`
6. Audit scripts updated: `audit-execution-integrity.sh`
7. args-schema.json files updated: `x-implement-story`, `x-implement-epic`

## File Footprint

**write:** `src/main/resources/targets/claude/skills/**/*.md` (221 files updated)  
**write:** `src/main/resources/targets/claude/rules/**/*.md` (0 direct; covered by skill refs)  
**write:** `src/main/resources/targets/claude/hooks/*.sh` (7 files)  
**write:** `src/main/resources/targets/claude/scripts/*.sh` (1 file)  
**write:** `src/main/resources/targets/claude/skills/**/references/*.json` (2 files)  
**write:** `.claude/skills/**/*.md` (cross-ref updates in generated output)  
**write:** `docs/**/*.md` (spec and ADR cross-refs)  
**write:** `CLAUDE.md` (skill name references)  
**write:** `ai/epics/epic-0076-verb-first-skill-naming-refactor/plans/plan-story-0076-0006.md` (this file)

## Validation

- `grep -r "x-story-implement\|x-epic-implement\|x-task-implement" src/main/resources/targets/claude/skills/ --include=SKILL.md | grep -v "x-implement-story\|x-implement-epic\|x-implement-task"` → 0 matches
- `grep -r "x-review\"" src/main/resources/targets/claude/skills/ --include=SKILL.md | grep -v "x-review-codebase\|x-review-pr\|x-review-db\|x-review-obs\|x-review-performance\|x-review-database\|x-review-observability"` → 0 matches
- `.claude/skills/` directory names all match verb-first pattern
