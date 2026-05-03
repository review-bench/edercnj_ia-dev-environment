---
epic-id: EPIC-0070
slug: value-driven-templates-v2
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [templates, epic-template, story-template, value-driven, v2, governance, adr]
capabilities-affected: [governance.value-driven-templates]
rules-affected: [Rule 30]
adrs-referenced: [ADR-0023]

patterns-introduced:
  - value-driven-epic-template-9-sections
  - story-template-4-category-gherkin-mandate
  - system-architecture-doc-auto-fill
  - x-arch-system-update-incremental
  - template-version-audit-gate
antipatterns-rejected:
  - mixed-value-architecture-in-same-template-sections
  - packages-hexagonal-in-epic-or-story-doc

dependencies-of: [EPIC-0056]
dependencies-for: [EPIC-0075]
---
# Memory: EPIC-0070 — Value-Driven Templates v2

## Why this epic existed

Templates for Epics and Stories (inherited from EPIC-0056 / RA9) mixed business-value narrative (problem statement, persona, hypothesis, OKRs) with hexagonal architecture declarations (package structure, SOLID materialization, coding constraints) in the same sections. An epic was simultaneously a PRD and an architecture spec, forcing authors to choose between a shallow value document or a technical-heavy doc that didn't communicate business rationale. The mixed format also made `x-review` reviewers apply architectural checklists to sections that should only contain hypothesis and persona, producing noisy NO-GOs on style rather than substance.

## Hypothesis tested

Separating business value (9 sections: Vision, Persona, Value Hypothesis + OKRs, Alternatives, Scope, Risks, Stories, Quality Gates, References) from technical architecture (`docs/architecture/system.md` auto-filled from YAML + updated incrementally via `/x-arch-system-update`) would raise the quality of value conversations without losing architectural traceability. **Confirmed:** `flowVersion: "4"` epics born after 2026-04-30 use v2 templates by default; `audit-template-version.sh` measures ≥ 90% adoption target per release; `system.md` provides a single live technical reference per project.

## Decisions taken (with why)

1. **9-section epic template v2** (Rule 30) — no `Packages — Hexagonal`, `SOLID Materialization`, or `Coding Constraints` sections; those belong exclusively in `system.md`; rationale: reviewers apply wrong checklists when architecture and value are mixed.
2. **Story template 4-category Gherkin mandate** — `happy-path`, `error/boundary`, `performance/SLA`, `security/auth` required for every story's AC; rationale: pre-v2, performance and security scenarios were routinely missing.
3. **`docs/architecture/system.md` auto-fill** — Sections 1–5 generated from YAML; Sections 6–11 (Decision Log, Performance Budget, etc.) manually maintained; rationale: avoids drift between YAML config and documented architecture.
4. **`/x-template-migrate` assistant** — v1→v2 migration with atomic write, `PARSER_ERROR` abort, `--dry-run`; rationale: existing epics should not be force-migrated; opt-in assistant lowers friction.
5. **`audit-template-version.sh` Camada 2 gate** — WARN on v1 without `--legacy-template-v1` annotation; FAIL on bare v1 (no annotation); rationale: adoption must be measurable and enforceable.

## Alternatives rejected (with why)

- **Keep architecture sections in epic/story template with "optional" marker** — rejected because "optional" sections are always filled by authors who want thoroughness; the mixed format persists in practice.
- **Merge `system.md` into CLAUDE.md** — rejected because CLAUDE.md is already overloaded; `system.md` needs a stable path for `x-arch-system-update` incremental updates.
- **Require migration of all pre-v2 epics before merge** — rejected because that would block EPIC-0065/0069/0072 which are in-flight; v1 remains valid with `--legacy-template-v1` for 2 releases.

## Reusable patterns produced

- **`value-driven-epic-template-9-sections`**: `_TEMPLATE-EPIC.md` v2 — 9 business-value sections; no package or SOLID materialization; Rule 30 governs structure.
- **`story-template-4-category-gherkin-mandate`**: `_TEMPLATE-STORY.md` v2 — 4-category Gherkin (`happy-path`, `error/boundary`, `performance/SLA`, `security/auth`) mandatory in every story AC section.
- **`system-architecture-doc-auto-fill`**: `_TEMPLATE-ARCHITECTURE-SYSTEM.md` + `docs/architecture/system.md` lifecycle — Sections 1–5 from YAML; Sections 6–11 manual; updated by `/x-arch-system-update`.
- **`x-arch-system-update-incremental`**: idempotent skill that updates Decision Log + YAML-driven fields per epic; safe to run multiple times.
- **`template-version-audit-gate`**: `audit-template-version.sh` — scans `ai/epics/` for new epic/story markdown files; FAIL on bare v1 without annotation; measures v2 adoption percentage.

## Anti-patterns observed

- **Mixed value-architecture template sections** — when same document contains both `## Persona` (value) and `## Packages — Hexagonal` (architecture), reviewers apply architectural checklists to value sections, producing spurious NO-GOs and slowing reviews.
- **`packages-hexagonal-in-epic-or-story-doc`** — epic/story docs are value-communication artifacts; hexagonal layout belongs in `system.md` only; embedding it here duplicated information across 3+ files per epic.

## Links

- Epic: `ai/epics/epic-0070-value-driven-templates-v2/epic-0070.md`
- Rule 30: `.claude/rules/30-value-driven-templates.md`
- ADR: `docs/adr/ADR-0023-value-driven-templates.md`
- Supersedes: EPIC-0056 (RA9 Planning Templates — archived)
