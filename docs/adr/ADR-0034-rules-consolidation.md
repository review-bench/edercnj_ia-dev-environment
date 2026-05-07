# ADR-0034 — Rules Consolidation: Single 00-essentials.md + Lazy-Loaded KPs

**Status:** Accepted  
**Date:** 2026-05-07  
**Branch:** `chore/rules-consolidation-essentials`  
**Epic:** EPIC-0078 (Context Budget Optimization)

---

## Context

Before this change, the `.claude/rules/` directory contained **28–29 always-loaded numbered rule files** (~140 KB / ~2909 lines). Every Claude Code conversation loaded all of them into the system prompt, spending roughly 35–37K tokens on rules regardless of what the operator was doing. Many of these rules were long reference documents (security anti-patterns, capability frontmatter schemas, CI audit exit codes) that are only consulted when doing specific specialized work.

The root cause was a historical accident: rules were added incrementally over EPIC-0046 through EPIC-0078, each time appending a new file to the always-loaded set without accounting for cumulative context cost.

## Decision

Replace all 28 numbered rule source files with:

1. **A single `00-essentials.md`** generated from `shared/templates/_TEMPLATE-ESSENTIALS-RULE.md`. It contains only what every session needs:
   - §1 Project Identity (injected from YAML via `{PROJECT_IDENTITY_SECTION}`)
   - §2 Hard Limits (method ≤ 25 lines, class ≤ 250 lines, etc.)
   - §3 Architecture Golden Rule (dependency direction)
   - §4 Forbidden — Top Level (null returns, hardcoded creds, etc.)
   - §5 Lifecycle Integrity Contract (flowVersion, 3 bypass exceptions, zero-bypass)
   - §6 Skill Invocation Protocol (3 permitted delegation patterns)
   - §7 Knowledge Pack Index (Read links for all 23 governance KPs + specialist KPs)
   
   Target: ≤ 400 lines.

2. **23 Knowledge Pack files** under `knowledge/governance/rules/` (lazy-loaded — only fetched when the LLM or operator explicitly reads them):
   - `lifecycle-contract.md`, `coding-standards-rule.md`, `quality-gates.md`, `architecture-summary.md`, `security-baseline.md`, `branching.md`, `skill-invocation.md`, `skill-visibility.md`, `task-hierarchy.md`, `tool-call-grammar.md`, `audit-gate-lifecycle.md`, `model-selection.md`, `capability-frontmatter.md`, `epic-branch-model.md`, `interactive-gates.md`, `project-scope.md`, `release-process.md`, `operations-baseline.md`, `doc-freshness-gate.md`, `dependency-policy.md`, `value-driven-templates.md`, `ai-memory-production.md`, `domain-template.md`

3. **`knowledge/security/anti-patterns-java.md`** — consolidated Java security anti-patterns (migrated from `12-security-anti-patterns.md`).

4. **Conditional rules preserved** — `rules/conditional/` is untouched: `09-data-management.md`, `10-anti-patterns.md`, `11-security-pci.md`, `12-security-anti-patterns.md` remain as conditional outputs.

## Alternatives Considered

### Alternative 1: Keep numbered files but trim each to ≤ 10 lines
Rejected: Still multiplies the always-loaded token count by 28 files × 10 lines = 280 minimum. The per-file overhead (filename, headers, section titles) alone pushes the real number higher.

### Alternative 2: Merge all rules into a single mega-rules file
Rejected: Unreadable for humans. Also doesn't enable lazy loading — the full content would still be in the system prompt.

### Alternative 3: Move rules to CLAUDE.md instead of 00-essentials.md
Rejected: CLAUDE.md is user-maintained and should not be overwritten by the generator.

## Consequences

**Positive:**
- Context budget for always-loaded rules drops from ~35K tokens to ~5K tokens — a ~86% reduction.
- Detailed rule content is available on demand via `Read knowledge/governance/rules/<name>.md`.
- `RulesAssembler.java` is simplified: `EssentialsRuleWriter.write()` replaces 8+ individual copy operations.

**Negative:**
- Sessions that need deep rule detail must explicitly load the relevant KP. This is intentional (lazy loading), but operators must know the KPs exist.
- The `governance/baselines/kp-references-baseline.txt` was extended with 24 new entries for the new governance KPs (these KPs are referenced via `00-essentials.md`'s §7 Read links, not via skill YAML frontmatter, so the orphan auditor requires explicit baseline entries).

## Enforcement

- `audit-essentials-rule.sh` (Camada 2 CI script) verifies: `00-essentials.md` exists; ≤ 400 lines; §1-§7 sections present; no old numbered rule files present; required governance KPs exist.
- `FatJarContentTest.coreRules_essentialsTemplate_exists()` verifies `_TEMPLATE-ESSENTIALS-RULE.md` is bundled.
- Golden files regenerated for all 9 profiles — CI golden-file test confirms exact output shape.

---

> **Catalogado em:** [`docs/audit-gates-catalog.md`](../audit-gates-catalog.md) (entry `audit-essentials-rule.sh`)
