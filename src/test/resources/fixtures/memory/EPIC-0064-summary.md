---
epic-id: EPIC-0064
slug: capability-driven-composition
summary-version: "1.0"
created: "2026-04-28"
last-updated: "2026-04-28"

indexable: true
archived: false
superseded-by: null

tags: [capability, compose, assembler, governance, architecture, breaking]
capabilities-affected: [governance.ai-memory, governance.dependency-policy, governance.refinement-gate]
rules-affected: [Rule 28]
adrs-referenced: [ADR-0016]

patterns-introduced:
  - capability-aware-skill-via-frontmatter
  - requires-capabilities-universal-declaration
  - output-pruner-capability-filter
antipatterns-rejected:
  - blind-copy-all-artifacts
  - per-stack-if-else-in-assembler

dependencies-of: []
dependencies-for: [EPIC-0075]
---
# Memory: EPIC-0064 — Capability-Driven Composition Refactor

## Why this epic existed

Before EPIC-0064 the generator performed a blind copy of all `.claude/` artifacts to every
generated project, regardless of the project's declared capabilities. A Java-only CLI project
received gRPC knowledge packs, security KPs for stacks it did not use, and skills conditioned on
databases it did not declare. The result was generated output that was noisy, confusing, and
potentially misleading.

The epic introduced `requires-capabilities: [...]` frontmatter (schema v3.0) on every artifact and
a `CapabilityAwareComposer` pipeline that prunes the output to only include artifacts whose
capability predicates are satisfied by the project's resolved capability set.

## Hypothesis tested

**Hypothesis:** adding a declarative capability predicate on every artifact and filtering at
generation time would eliminate irrelevant artifacts without breaking existing golden-file tests.

**Result:** confirmed. 920 golden fixtures regenerated successfully; `GoldenFileTest` and
`PlatformGoldenFileTest` passed after Phase 2 migration. Zero capability-unrelated regressions.

## Decisions taken (with why)

- **Use v3.0 frontmatter over runtime feature flags** — static frontmatter is declarative,
  self-documenting, and auditable by CI (`audit-frontmatter-schema.sh`). Feature flags would
  require runtime state management.
- **Universal declaration (`requires-capabilities: []`) for core artifacts** — explicit empty list
  communicates intent (always include) rather than inferring from absence.
- **`OutputPruner` as a separate pipeline stage** — keeps `CapabilityAwareComposer` free of
  pruning logic, which is independently testable.
- **Breaking change in v5.0.0** — no dual-mode v2/v3 fallback; forces migration in one release
  window rather than accumulating legacy paths.

## Alternatives rejected (with why)

- **Per-assembler if/else branching** — would scatter capability checks across 15 assembler classes,
  making the logic invisible from the artifact itself. Frontmatter keeps capability intent co-located
  with the artifact.
- **Capability enum in ProjectConfig** — enum constants would need updating for every new capability;
  YAML files extend the catalog without code changes.

## Reusable patterns produced

- `capability-aware-skill-via-frontmatter` — declare `requires-capabilities` in the artifact
  frontmatter; the pipeline handles inclusion/exclusion without any assembler code.
- `requires-capabilities-universal-declaration` — use `requires-capabilities: []` (explicit empty)
  on core artifacts to signal universal inclusion.
- `output-pruner-capability-filter` — single pipeline stage that filters assembled artifacts based on
  capability predicates; reusable for any new artifact type.

## Anti-patterns observed

- `blind-copy-all-artifacts` — copying artifacts without checking capability predicates produces
  bloated, confusing generated output.
- `per-stack-if-else-in-assembler` — scattering capability-conditional logic inside assembler Java
  code instead of declaring it in frontmatter; increases cognitive load and breaks the open/closed
  principle.

## Links

- Epic: `ai/epics/epic-0064-capability-driven-composition/epic-0064.md`
- ADRs: `docs/adr/ADR-0016-capability-driven-composition.md`
- PRs: epic/0064 → develop (PR #874)
- Reports: `ai/epics/epic-0064-capability-driven-composition/reports/`
