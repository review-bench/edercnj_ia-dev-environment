---
epic-id: EPIC-0076
slug: verb-first-skill-naming-refactor
summary-version: "1.0"
tags:
  - skill-naming
  - refactor
  - dx
  - conventions
  - ci-guard
capabilities-affected:
  - governance.skill-visibility
  - governance.audit-gate-lifecycle
rules-affected:
  - Rule 13 (Skill Invocation Protocol)
  - Rule 22 (Skill Visibility)
  - Rule 26 (Audit Gate Lifecycle)
stories: 7
status: Concluída
completed: "2026-05-03"
pr: "https://github.com/edercnj/ia-dev-environment/pull/950"
---

## Problem

The skill catalog (~100 skills) mixed noun-first names (`x-epic-implement`, `x-story-plan`, `x-pr-create`) with verb-first names (`x-create-feature`, `x-run-sast`), creating cognitive inconsistency: users couldn't predict a skill name from its action without consulting docs. Discovery suffered — the skill list felt arbitrary.

## Solution Delivered

Standardized all public skills to `x-<verb>-<object>`, internal skills to `x-internal-<verb>-<object>`, and lib skills to `x-lib-<verb>-<object>`. 102 skills renamed across 7 stories. 311 cross-reference files updated. Anti-legacy CI guard deployed.

## Key Decisions

1. **Verb-first over noun-first**: Decided because verb-first aligns with CLI ergonomics (the action is what you're doing, not what you're operating on). Rationale in ADR-0029.
2. **Word-boundary regex for cross-references**: `(?<![a-z0-9-])old(?![a-z0-9-])` — prevents double-substitution corruption (e.g., `x-review` matching inside `x-review-codebase`). Lesson learned from a prior session where naive `str.replace` corrupted 80+ files.
3. **Regular `mv` for `.claude/skills/`**: Git considers `.claude/` untracked (gitignored); `git mv` fails with "source directory is empty". Solution: regular `mv` for the generated output, `git mv` only for the source-of-truth.
4. **`x-test-perf` vs `x-test-performance` collision**: Both existed as distinct skills. Resolution: `x-test-perf` → `x-run-perf-tests`, `x-test-performance` → `x-execute-performance-tests`.

## Alternatives Rejected

- **Keep noun-first for some categories**: Rejected — partial standardization would perpetuate the inconsistency.
- **Rename only the most-used skills**: Rejected — a partial rename creates two naming conventions in the same catalog, worse than one imperfect convention.
- **Simple string replace**: Rejected — corrupts files containing the old name as a substring of the new name (e.g., replacing `x-review` also affects `x-review-codebase`, `x-review-pr`).

## Reusable Patterns

- **Word-boundary regex for cross-reference mass renames**: `re.compile(r'(?<![a-z0-9-])' + re.escape(old) + r'(?![a-z0-9-])')` — use whenever renaming identifiers that may be substrings of other identifiers.
- **Ordering of rename pairs**: Longer/more-specific names first in the mapping list as defense-in-depth (though word-boundary regex handles overlap anyway).
- **Baseline allow-list for historical files**: `governance/baselines/skill-naming-baseline.txt` — immutable after epic merge; files that legitimately reference old names don't need to be changed.

## Anti-Patterns Observed

- **Naive `str.replace` for skill names**: Causes double-substitution corruption in files that already contain the new name. Always use word-boundary regex for skill renames.
- **Using `git mv` on gitignored directories**: Git treats them as non-existent. Use regular `mv` for gitignored paths.

## Links

- SPEC: `docs/specs/SPEC-verb-first-skill-naming-v1.md`
- ADR: `docs/adr/ADR-0029-verb-first-skill-naming.md`
- Guard: `src/main/resources/targets/claude/scripts/audit-skill-naming.sh`
- Baseline: `governance/baselines/skill-naming-baseline.txt`
- PR: https://github.com/edercnj/ia-dev-environment/pull/950
