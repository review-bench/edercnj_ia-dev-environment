# ADR-0024 — Documentation Freshness Gate

**Status:** Accepted  
**Date:** 2026-04-30  
**Deciders:** Architect (EPIC-0071)  
**Epic:** EPIC-0071 (Documentation as DoD)  
**Rule:** [Rule 31 — Documentation Freshness Gate](../../.claude/rules/31-documentation-freshness-gate.md)

---

## Context

Documentation in this project accumulated silent drift: README sections became stale, OpenAPI specs missed new endpoints, ADR references in story markdown pointed to unpublished files, and skill-docs lost `## Triggers` / `## Examples` as skills evolved. The root cause was structural — `x-doc-generate` was invoked as an optional step ("if time permits") in Phase 3 of `x-story-implement`, and no CI gate checked freshness. Without a blocking gate, the rational behavior for contributors is to skip documentation under time pressure.

This mirrors the pre-TDD era: tests were optional until a gate made them the path of least resistance.

## Decision

**Documentation updates are a blocking gate in the story lifecycle (Documentation as DoD).**

Specifically:
1. `x-doc-generate` and `x-doc-validate` are promoted to **MANDATORY TOOL CALLS** (Rule 24) in Phase 3 of `x-story-implement`. The `--skip-doc` flag is removed from the happy-path; it is only accessible inside `## Recovery` blocks.
2. `audit-doc-freshness.sh` is introduced as a Camada 2 CI gate (Rule 26) running on every PR to `develop` or `epic/*`.
3. `DocumentationConfig.java` + `documentation.targets` YAML block allow per-project configuration of which targets are enforced and with what grace period.
4. Auto-detection derives effective targets from `interfaces[]` declarations when the YAML block is absent — no configuration required for the common case.

## D-R3 — Rule number

Rule 31 is assigned (next available integer after Rule 30). Rule 30 already cross-links to "Rule 31 (Documentation Freshness Gate, EPIC-0071)" — confirming the number.

## D-R7 — Rule merge decision: SEPARATE rule (not merged with Rule 30)

Overlap between Rule 30 (Value-Driven Templates) and the Doc-as-DoD invariants was measured at **<30%**:
- Rule 30 governs **structural** conventions: template format, mandatory section headers, Gherkin AC categories, `docs/architecture/system.md` lifecycle.
- Rule 31 governs **temporal enforcement**: freshness gate, CI blocking, mandatory invocation in story lifecycle, `--skip-doc` constraint.

The only overlap is the `system.md` target (Rule 30 defines it; Rule 31 enforces its freshness). The two rules are **cross-linked** but structurally separate. Merging would produce a god-rule mixing template authoring with gate enforcement — a SRP violation.

## D-R9 — Capability prerequisites

`capabilities/` directory and `governance/schemas/capabilities-1.0.json` exist (EPIC-0064 Phase 2). Capability `governance.doc-as-dod` is universal (`requires-capabilities: []`).

## Consequences

**Positive:**
- Documentation debt accumulation stops by design — contributors cannot merge code changes without a corresponding doc update (or explicit recovery-mode bypass).
- `x-doc-generate` v2 generates documentation in one step; `x-doc-validate` verifies it; contributors do not need to remember which docs to update.
- Stack-aware auto-detection means zero configuration required for most projects.

**Negative:**
- PRs touching many files will require more discipline — first run `x-doc-generate`, then validate.
- Legacy stories pre-EPIC-0071 are grandfathered via `governance/baselines/doc-freshness-baseline.txt` (introduced in story-0071-0005).

## Alternatives Considered

**A — Keep `--skip-doc` as a first-class flag.** Rejected: if skipping is easy, the gate degrades to advisory. The history of optional coverage enforcement shows the same pattern — optional → ignored.

**B — Enforce freshness only in CI (no runtime gate).** Rejected: CI-only enforcement means the feedback loop is too slow (PR already created before the failure surfaces). Camada 0 (preflight) catches it during the LLM turn.

**C — Merge with Rule 30.** Rejected: see D-R7 above — structural (templates) and temporal (freshness) concerns have different authors, different triggers, and different tooling. Merging produces a rule that is difficult to audit independently.

## Related

- [Rule 30 — Value-Driven Templates](../../.claude/rules/30-value-driven-templates.md)
- [Rule 24 — Execution Integrity](../../.claude/rules/24-execution-integrity.md)
- [Rule 26 — Audit Gate Lifecycle](../../.claude/rules/26-audit-gate-lifecycle.md)
- [EPIC-0071 Implementation Map](../../ai/epics/epic-0071-documentation-as-dod/IMPLEMENTATION-MAP.md)
