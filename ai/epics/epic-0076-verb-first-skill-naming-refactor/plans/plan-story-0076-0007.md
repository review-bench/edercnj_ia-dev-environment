# Implementation Plan — story-0076-0007

**Story:** Guard anti-legado, smoke tests e documentação de migração  
**Epic:** EPIC-0076 — Verb-First Skill Naming Refactor  
**Phase:** 4 — Verification and Rollout  

---

## Summary

Introduce the anti-legacy CI guard script, update CHANGELOG.md with the full rename table, and create the allow-list baseline — ensuring the verb-first convention is protected against regressions.

## Approach

1. **`audit-skill-naming.sh`** (Camada 2 CI Script): Scans all `.md`, `.sh`, `.json`, `.yaml` files in source-of-truth and generated `.claude/` output for legacy noun-first skill names. Uses PCRE word-boundary regex (`(?<![a-z0-9-])old(?![a-z0-9-])`). 84 legacy patterns listed. Allow-list excludes: CHANGELOG.md, SPEC file, ADR-0003, ai/epics/epic-0036, ai/epics/epic-0076.
2. **`governance/baselines/skill-naming-baseline.txt`**: Immutable allow-list for files that legitimately reference old names (historical records).
3. **CHANGELOG.md**: Highlights + Added + Changed sections documenting all 84 renames by category.

## File Footprint

**write:** `src/main/resources/targets/claude/scripts/audit-skill-naming.sh`  
**write:** `governance/baselines/skill-naming-baseline.txt`  
**write:** `CHANGELOG.md`  
**write:** `ai/epics/epic-0076-verb-first-skill-naming-refactor/plans/plan-story-0076-0007.md` (this file)

## Validation

- `bash src/main/resources/targets/claude/scripts/audit-skill-naming.sh --self-check` → exit 0
- `audit-skill-naming.sh` on current repo state → exit 0 (all updated by story-0076-0006)
- CHANGELOG.md contains all 84 rename pairs in the Changed section
