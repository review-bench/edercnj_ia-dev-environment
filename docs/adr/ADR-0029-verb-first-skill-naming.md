---
status: Accepted
date: 2026-05-03
deciders:
  - Eder Celeste Nunes Junior
story-ref: "story-0076-0001"
---

# ADR-0029: Verb-First Skill Naming Convention

## Status

Accepted | 2026-05-03

## Context

The skill catalog has undergone two previous naming waves (EPIC-0033, EPIC-0036), both of which produced a mostly `x-<noun>-<verb>` or `x-<noun>-<noun>` pattern (e.g., `x-implement-epic`, `x-plan-story`, `x-fix-pr`). This pattern places the domain object first, which means an operator must know the object category before discovering the action.

Additionally, skills introduced by later epics (EPIC-0065, EPIC-0069, EPIC-0075) followed the same noun-first pattern. The catalog now has three layers of inconsistency:
- Original skills: `x-implement-epic`, `x-fix-pr`
- Second-wave refactor: `x-create-feature`, `x-search-memory`
- Legacy holdovers: `x-commit-changes`, `x-create-git-branch`

The root problem is discoverability: an operator wanting to "implement a story" must know to type `x-implement-story` rather than the more natural `x-implement-story`. The verb-first pattern solves this by putting the action — the first thing an operator knows — at the front.

EPIC-0076 performs the second (and final) naming wave, establishing verb-first as the canonical grammar for all public, internal, and lib skills.

## Decision

### Grammar

| Skill type | Pattern | Example |
| :--- | :--- | :--- |
| Public | `x-<verb>-<object>` | `x-implement-story` |
| Internal | `x-internal-<verb>-<object>` | `x-internal-load-story-context` |
| Lib | `x-lib-<verb>-<object>` | `x-lib-decompose-task` |

### Rules

1. **Verb first** — the skill name answers "what does this do?" without requiring domain knowledge.
2. **Intentional object** — the object reflects the real target of the action, not the category folder name.
3. **No blind inversion** — renaming is not mechanical; semantic clarity overrides symmetry (e.g., `x-release` stays `x-release` because it is a well-known verb-object collapsed noun).
4. **No aliases** — the rename is a hard-cut per cluster; old names exist only in changelog and an explicit allow-list in the anti-legacy CI guard.
5. **Internal namespace preserved** — `internal` stays immediately after `x-` in internal skills.
6. **Lib namespace preserved** — `lib` stays immediately after `x-` in lib skills.
7. **Plural when natural** — use plural when the action operates on a collection (`x-audit-dependencies`, `x-cleanup-git-branches`).
8. **Knowledge packs excluded** — `*-kp` artifacts are outside this naming convention.

### Exception criteria

A skill may retain its current name (or a non-verb-first form) when:
- The current name is already verb-first and semantically clear (e.g., `x-setup-env`, `x-setup-stack`, `x-release`).
- The proposed verb-first form would be less clear than the current name (documented per-skill exception in the canonical matrix).
- The skill is a knowledge pack (`*-kp`) — explicitly excluded from this convention.

### Hard-cut vs. deprecation window

Per RULE-009 (EPIC-0076 Epic Document): there is no dual-naming transition window. Old names appear only in:
- `CHANGELOG.md` under `## Removed` with migration note
- ADRs and historical planning artifacts (read-only, not invocable)
- The CI guard allow-list (`scripts/audit-legacy-skill-names.sh`)

### Surfaces updated atomically

Every rename in Phase 2 (stories 0003–0005) triggers an atomic update to:
1. Source-of-truth `SKILL.md` directory name and frontmatter `name:` field
2. All `Skill(skill: "old-name")` calls across all SKILL.md files
3. Rules, templates, agents, and hooks that reference the old name

Phase 3 (story-0006) consolidates cross-surface updates for Java, golden files, and generated `.claude/` output.

## Consequences

### Positive

- Operator discoverability: type the verb → autocomplete shows relevant skills.
- Consistent mental model: one grammar for all skill types regardless of domain.
- Reduced cognitive load for new contributors: no need to memorize noun-first exceptions.
- CI guard prevents regression to legacy names.

### Negative

- All 7 active rules and 13 skill SKILL.md files referencing old names must be updated atomically.
- Java `SkillsAssembler` and any class that hardcodes skill names must be updated.
- Golden files under `src/test/resources/golden/` must be regenerated for all profiles.
- Any downstream documentation or team guides using old names become stale.

### Neutral

- The `.claude/skills/` generated output remains flat; only the `name:` frontmatter field changes.
- Skills already in verb-first form (`x-setup-env`, `x-setup-stack`, `x-release`, `x-review-pr`) retain their current names — no churn for those.

## Alternatives Considered

### A1: Keep noun-first convention

**Rejected.** Noun-first only helps when you know the noun. New operators and new LLM sessions default to thinking in terms of actions. Verb-first is more aligned with how Claude Code autocomplete surfaces commands.

### A2: Dual naming with one-release deprecation window

**Rejected.** Dual naming creates two sources of truth for invocation paths. Given that this is the second wave of renaming, adding a third layer (old → alias → new) would make the catalog harder to reason about than the current state. Hard-cut with a changelog migration note is operationally cleaner.

### A3: Verb-first only for public skills, keep noun-first for internals

**Rejected.** Internals are read by maintainers who benefit equally from consistent naming. Consistency across visibility levels reduces cognitive switching.

## Related ADRs

- ADR-0002: Skill Delegation Protocol (establishes that all invocations are explicit Skill(...) calls, making mechanical rename safe)
- ADR-0003: Skill Taxonomy and Naming Refactor (first wave — established noun-first; this ADR supersedes its naming decisions)

## Story Reference

- story-0076-0001 (Gramática verb-first e ADR do rename)
- EPIC-0076 (Verb-First Skill Naming Refactor)
