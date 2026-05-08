# SPEC-bug-lifecycle-management-v1 — Bug Lifecycle Management

> **Status:** Draft  
> **Author:** User  
> **Date:** 2026-05-07  
> **Branch:** docs/feature-bug-lifecycle-management

---

## Sistema

Bug Lifecycle Management is a parallel planning and execution track dedicated to capturing, refining, and executing bug fixes through the same governance pipeline as features. Bugs are first-class planning artifacts (analogous to epics) that decompose into stories and pass through refinement and implementation gates without requiring capability declarations. This feature allows development teams to handle defects with the same rigor, traceability, and orchestration as feature development, creating a unified lifecycle for both enhancements and fixes.

The bug system scaffolds the `ai/bugs/` directory tree, automatically generates stories from bug descriptions, produces implementation maps for orchestration, and enforces refinement gates to prevent unvalidated work from entering implementation. Bugs flow through the same skill orchestrators as epics, reusing x-implement-story, x-refine-story, and review chains with minimal forking.

---

## Escopo

### Incluído

- New `/x-create-bug` skill that scaffolds `ai/bugs/bug-XXXXXX/` directory and `bug-XXXXXX.md` artifact
- Automatic generation of one or more stories under the bug directory after bug creation
- Implementation map artifact produced for each bug (epic-equivalent decomposition)
- New `/x-refine-bug` skill mirroring the `x-refine-story`/`x-refine-epic` refinement gate pattern
- Refinement gate enforcement: bug stories cannot be implemented until `refinementVerdict.status = approved`
- Integration with existing orchestration skills (`x-implement-story`, `x-review-pr`, `x-manage-pr-merge-train`)

### Excluído

- Capability declarations or capability frontmatter on bug artifacts (bugs are not features)
- Reuse of `ai/epics/` tree — bugs live under a dedicated `ai/bugs/` root
- Auto-generation of ADRs or system-architecture updates for bug fixes
- Separate bug-specific PR merge trains or CI pipelines (reuse existing gates)

---

## Regras

| ID | Regra | Impacto |
|----|-------|---------|
| RULE-001 | Bug artifacts MUST live under `ai/bugs/bug-XXXXXX/` where XXXXXX is zero-padded bug code | Directory structure and artifact discovery |
| RULE-002 | Every bug MUST produce: `bug-XXXXXX.md` root, ≥1 story file, implementation map | Mandatory artifacts for orchestration |
| RULE-003 | Bug stories MUST pass `/x-refine-bug` refinement gate before `x-implement-story` invocation | Refinement gate enforcement (Rule 29 compliance) |
| RULE-004 | Bug artifacts MUST NOT declare `requires-capabilities` (exempt from Rule 28 capability frontmatter) | Simplifies lifecycle, no capability resolution needed |
| RULE-005 | Bug lifecycle MUST flow through same orchestration pipeline as epics (planning, implementation, review, PR gates) | Unified governance, consistent audit gates |
| RULE-006 | Bug code XXXXXX format: 4-digit zero-padded integer (BUG-0001, BUG-0042, etc.) | Consistent naming convention |

---

## Histórias

Preliminary story list (will be refined in `/x-create-bug` execution and further detailed in `/x-refine-bug`):

| # | Título | Stakeholder |
|---|--------|-------------|
| 1 | As a developer, I need `/x-create-bug` to scaffold a bug directory so that I can document a defect with the same rigor as a feature | Developer |
| 2 | As a developer, I need `/x-create-bug` to auto-generate stories under the bug folder so that fixes decompose into implementable units | Developer |
| 3 | As a developer, I need `/x-create-bug` to produce an implementation map so that bugs behave as epic-equivalents for orchestration | Developer |
| 4 | As a tech lead, I need `/x-refine-bug` to validate bug stories so that only refined work enters implementation | Tech Lead |
| 5 | As an orchestrator, I need bug stories blocked from `x-implement-story` until refined so that the refinement gate is enforced uniformly | Orchestrator |
| 6 | As an engineer, I need bug artifacts to flow through the standard PR review and merge gates so that defects have the same governance as features | Engineer |

---

## DoR / DoD

### Definition of Ready

- [ ] Bug code XXXXXX is allocated and unique under `ai/bugs/`
- [ ] Bug description prose is provided by the operator at invocation time
- [ ] Target epic branch or develop branch is identified for the bug fix flow
- [ ] Reproducibility criteria for the bug are documented (steps, environment, expected vs actual)

### Definition of Done

- [ ] `ai/bugs/bug-XXXXXX/bug-XXXXXX.md` exists with documented problem statement and reproducibility
- [ ] At least one `story-XXXXXX-NNNN.md` file exists under the bug directory
- [ ] Implementation map artifact is generated and committed
- [ ] `/x-refine-bug` skill is registered and operational, blocking unrefined stories from implementation
- [ ] Bug lifecycle is exercised end-to-end through orchestration in a smoke test
- [ ] PathResolver and audit scripts recognize `ai/bugs/` as a sibling root with capability-exemption
- [ ] CI gates (Rule 26, Rule 27) recognize bug artifacts as evidence-bearing equivalent to epics

---

## Riscos

| Risco | Impacto | Mitigação |
|-------|---------|-----------|
| Divergence between bug and epic lifecycles increases maintenance burden of two parallel orchestration pipelines | Alto | Reuse `x-internal-*` skills and treat bug as typed alias of epic with capability-exempt flag rather than forking the orchestrator |
| `ai/bugs/` tree bypasses Rule 28 capability frontmatter contract and may break audit scripts that assume `ai/epics/` layout | Médio | Extend PathResolver and audit scripts to recognize `ai/bugs/` as sibling root with explicit capability-exemption rule |
| Refinement gate for bugs duplicates logic from `x-refine-story`/`x-refine-epic` causing drift | Médio | Implement `/x-refine-bug` as thin wrapper delegating to shared `x-internal-refine` core logic |
| Operators may file bugs that should be features (or vice versa), mis-categorizing planning artifacts | Baixo | Add triage prompt at start of `/x-create-bug` confirming defect-vs-enhancement classification |

---

**Next Steps:** This spec is ready for human review. Once approved, invoke `/x-create-feature docs/specs/SPEC-bug-lifecycle-management-v1.md --epic-id <NNNN>` to launch full feature decomposition.
