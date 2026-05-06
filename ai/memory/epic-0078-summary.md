---
epic-id: EPIC-0078
slug: context-budget-optimization
summary-version: "1.0"
tags:
  - context-budget
  - rules-slimming
  - knowledge-packs
  - capability-composition
  - governance
capabilities-affected:
  - governance.capability-frontmatter
  - governance.audit-gate-lifecycle
  - governance.ai-memory
rules-affected:
  - Rule 03 (Coding Standards)
  - Rule 04 (Architecture Summary)
  - Rule 05 (Quality Gates)
  - Rule 06 (Security Baseline)
  - Rule 07 (Operations Baseline)
  - Rule 08 (Release Process)
  - Rule 09 (Branching Model)
  - Rule 12 (Security Anti-Patterns)
  - Rule 19 (Backward Compatibility)
  - Rule 24 (Execution Integrity)
  - Rule 25 (Task Hierarchy)
  - Rule 26 (Audit Gate Lifecycle)
  - Rule 27 (Zero-Bypass Lifecycle)
  - Rule 28 (Capability Frontmatter)
  - Rule 29 (Refinement Gate)
  - Rule 30 (Value-Driven Templates)
  - Rule 45 (CI-Watch Integrity)
---

# EPIC-0078 — Context Budget Optimization

## Problem

The always-loaded rule set consumed **59,591 tokens** per LLM conversation — 2.4× the 25,000-token target. Rules had grown from concise contracts into fully-detailed reference documents. Every conversation injected entire fallback matrices, complete code examples, and extended rationale sections that are rarely needed inline.

## Hypothesis

If we split rules into compact stub contracts (≤50 lines, ≤2K tokens each) pointing to lifecycle KPs for full detail, and add `requires-capabilities` frontmatter to enable capability-aware pruning, then always-loaded context drops below 25,000 tokens without losing any governance enforcement.

## Decisions (With Rationale)

1. **KP split over inline compression.** Compressing rule prose would lose precision. Extracting to KPs preserves fidelity while removing content from the always-loaded layer. KPs are loaded on-demand by skills that need the detail.

2. **5 lifecycle KPs + 5 governance KPs.** Content grouped by lifecycle phase (task-hierarchy, backward-compat, exec-integrity, zero-bypass, refinement-gate, ci-watch) and governance type (audit-gate-lifecycle, capability-composition, tool-call-grammar) for targeted retrieval.

3. **`requires-capabilities` frontmatter — advisory mode first.** Hard-fail capability pruning risks removing content on projects that haven't declared capabilities. Advisory mode (WARN_ONLY) ships first; hard-fail follows in a later release after adoption validates.

4. **Rule 02 (Domain Template) fully removed.** The domain template is a skeleton that teams immediately customize — shipping it as an always-loaded rule added no value. Deleted from generator output entirely.

5. **`audit-context-budget.sh` as hard-fail CI gate.** Prevents future budget regression. Baseline committed at post-EPIC-0078 token count; any PR that increases the count fails CI.

## Alternatives Rejected

- **Per-profile rule trimming:** Would require separate rule files per capability profile. The KP pointer approach works for all profiles with a single rule file.
- **Context window increase:** Symptom treatment, not root cause. Doesn't help token cost or improve response quality on shorter problems.

## Reusable Patterns

- **Stub-pointer pattern:** `## Full Detail → [lifecycle KP](../knowledge/lifecycle/X.md)` in a rule allows compact rules to stay normatively complete.
- **YAML frontmatter on rules:** `requires-capabilities: []` marks universally-loaded rules; non-empty enables pruning. Pattern now standard for all rule files.
- **KP naming:** `knowledge/lifecycle/` for execution-flow KPs; `knowledge/governance/` for meta-governance KPs.

## Anti-Patterns Observed

- Rules that grew into full reference documents over multiple epics — each epic added "one more row" to tables without auditing total size.
- Tests that checked rule content verbatim — when rules slim, tests break. Pattern: tests should check either the KP (for detail) or the stub (for pointer/section headers).

## Test Adaptation Key

When a rule is slimmed:
1. Tests checking **evidence tables / detailed sections** → redirect to the corresponding lifecycle KP.
2. Tests checking **section presence** → adapt to new (shorter) section list in slim rule.
3. Tests checking **line count** → may need limit adjustment (actual vs. theoretical target).
