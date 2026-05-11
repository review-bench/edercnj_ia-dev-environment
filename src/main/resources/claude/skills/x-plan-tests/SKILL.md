---
name: x-plan-tests
description: "Generates a Double-Loop TDD test plan with TPP-ordered acceptance and unit scenarios."
user-invocable: true
allowed-tools: Read, Grep, Glob
argument-hint: "[STORY-ID]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Plan Tests (slim — ADR-0012)

## Purpose

Produces a Double-Loop TDD test plan that drives implementation order. With ≥95% line / ≥90% branch coverage enforced (Rule 05 RULE-005-01), the plan serves as the implementation roadmap — each test scenario maps to one Red-Green-Refactor cycle, ordered by Transformation Priority Premise (TPP).

## Triggers

- `/x-plan-tests STORY-ID` — generate test plan for a specific story

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `STORY-ID` | Yes | Story identifier to plan tests for. If not provided, prompt for it. |

## Output Contract

| Artifact | Path |
|----------|------|
| Test plan | `ai/epics/epic-XXXX/plans/tests-story-XXXX-YYYY.md` |
| Origin marker | YAML frontmatter (`generated-by: x-plan-tests@<sha>`, `generated-at`, `story-id`) — mandatory per EPIC-0059 Rule 24 |
| Status side-effect (v2 epics only) | Story `**Status:**` transitions `Pendente` → `Planejada` (idempotent) |

Idempotent on staleness: when `mtime(plan) >= mtime(story)`, returns the existing plan without invoking any subagent (RULE-002).

## Workflow Overview

```text
0. PRE-CHECK       -> mtime(story) vs mtime(plan); skip generation if plan is fresh
1. GATHER_CONTEXT  -> general-purpose subagent (model: opus) reads KPs + story + existing code
2. PLAN_TESTS      -> Acceptance Tests (Outer Loop) + Unit Tests in TPP order (Inner Loop) + IT
3. ESTIMATE        -> Coverage estimation table + 10-point quality checks
4. WRITE           -> Emit Markdown with origin-marker frontmatter; (v2) transition status
```

Detailed Step 0 mtime algorithm, Step 1 subagent prompt with KP read list and 12-item context output, Step 2 TPP Level 1–6 catalog with transforms, AT/UT/IT field tables, Step 3 quality checks, full Markdown output template, anti-patterns catalog, RULE-012 template fallback, and Planning Status Propagation (Rule 22 / EPIC-0046) live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 0** (§Step 0): mtime comparison rules; first-generation log; fresh-plan reuse without subagent.
- **Step 1** (§Step 1): subagent invocation with `model: "opus"` (Rule 23 RULE-002); full read list (`testing-philosophy.md`, `testing-conventions.md`, `architecture.md`, `testing.md`); 12-item context summary contract (test categories, naming, frameworks, fixture pattern, ACs, business rules, exceptions, layer boundaries, existing patterns, contract/chaos applicability, template sections).
- **Step 2.1** (§Step 2.1): Acceptance Test field table (ID/Gherkin/Status/Components/Test Type/Depends on/Parallel).
- **Step 2.2** (§Step 2.2): full TPP Level 1–6 catalog with transform rules per level; Unit Test field table (ID/Test/Implementation/Transform/TPP Level/Components/Depends on/Parallel).
- **Step 2.3** (§Step 2.3): Integration Test field table (ID/Test/Components/Depends on/Parallel).
- **Step 2.4** (§Step 2.4): CRUD-only story optimization (max TPP Level 2 unless business rules demand more).
- **Step 3** (§Step 3.1–3.2): coverage estimation table flagging <95%/<90% classes; 10-point quality check checklist.
- **Output** (§Output): origin-marker frontmatter contract (mandatory per EPIC-0059 Rule 24 — absent block fails `audit-execution-integrity.sh` with `EIE_EVIDENCE_MISSING`); full Markdown output template.
- **Anti-Patterns** (§Anti-Patterns): 7-item list (no test code, no category organization, no skipped error paths, no skipped boundaries, etc.).
- **Template Fallback** (§Template Fallback): RULE-012 graceful degradation when `_TEMPLATE-TEST-PLAN.md` is absent.
- **Planning Status Propagation** (§Planning Status Propagation): v2-gated `Pendente → Planejada` transition via `StatusFieldParserCli`; commit via `Skill(x-commit-changes)`.

## Error Handling

| Scenario | Action |
|----------|--------|
| No story ID provided | Prompt user for story identifier |
| Story file not found | Abort with file-not-found message |
| Existing plan is fresh (RULE-002) | Return existing plan, skip generation |
| Template not found (RULE-012) | Log warning, use inline format, continue |
| No Gherkin scenarios in story | Generate tests from acceptance criteria text |
| No acceptance criteria found | Abort with warning, request story refinement |

## Knowledge Pack References

| Pack | File | Purpose |
|------|------|---------|
| testing | `.claude/knowledge/testing/testing-philosophy.md` | 8 test categories, fixture patterns, data uniqueness |
| testing | `.claude/knowledge/testing/testing-conventions.md` | {{LANGUAGE}}-specific frameworks, naming, assertions |
| testing | `.claude/knowledge/testing.md` | {{LANGUAGE}}-specific patterns |
| architecture | `.claude/knowledge/architecture.md` | Exception hierarchy, layer boundaries |
| security | `.claude/knowledge/security/anti-patterns-java.md` | Java-specific CWE-mapped anti-patterns to mirror in error-path tests |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-implement-story` | called-by | Invoked during Phase 1B |
| `x-implement-task` | reads | Output consumed as TDD roadmap |

- Pre-check (RULE-002) prevents redundant regeneration when story has not changed.
- Template reference (RULE-007) ensures consistent 8-section output format when available.
- Subagent uses explicit `model: "opus"` (Rule 23 RULE-002) for deep test planning quality.
- Output uses Double-Loop TDD format with TPP-ordered scenarios.
- Output consumed by Phase 2 (developers) and Phase 3 (QA engineer validates coverage).
- Can be used standalone before any implementation task.

## Full Protocol

Minimum viable contract above. Detailed Step 0 idempotency algorithm, Step 1 subagent prompt with 12-item context contract, Step 2 TPP Level 1–6 catalog with transform rules, full AT/UT/IT field tables, Step 3 quality-check checklist, origin-marker frontmatter contract, anti-patterns, template fallback, and Planning Status Propagation procedure live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
