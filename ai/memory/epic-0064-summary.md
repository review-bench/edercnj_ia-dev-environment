---
epic-id: EPIC-0064
slug: capability-driven-composition
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [architecture, generator, capabilities, composition, breaking]
capabilities-affected: [governance.capability-composition]
rules-affected: [Rule 28]
adrs-referenced: [ADR-0016]

patterns-introduced:
  - capability-aware-skill-via-frontmatter
  - requires-capabilities-universal-declaration
  - fragment-slot-polymorphic-assembly
  - capability-resolver-deterministic
antipatterns-rejected:
  - copy-blind-all-artifacts-to-all-stacks
  - hardcoded-skill-registry-list
  - implicit-capability-inheritance

dependencies-of: [EPIC-0061, EPIC-0059]
dependencies-for: [EPIC-0072, EPIC-0074, EPIC-0075]
---
# Memory: EPIC-0064 — Capability-Driven Composition Refactor

## Why this epic existed

Generator `ia-dev-env` was **copy-blind**: a Java CLI project without a database received `data-management/`, `database-patterns/`, `data-modeling/` knowledge packs, `database-engineer` agent, `x-review-db` skill, and DB-inline rule sections — all dead content. Spring Boot and Quarkus received the same `stack-patterns/` content with contradictory instructions. `SkillRegistry.CORE_KNOWLEDGE_PACKS:43` hardcoded `data-management` as "core", contradicting partial conditional filtering elsewhere.

## Hypothesis tested

A **declarative capability model** (`capabilities/<category>/<id>.yaml`) + **frontmatter v3.0** (`requires-capabilities: [...]` on every artifact) + `CapabilityAwareComposer` replacing ~20 assemblers would eliminate dead content and enable polymorphic artifact assembly from fragments. **Confirmed**: schema v3.0 mandatory (breaking, no v2 fallback); 182 artifacts migrated; `flowVersion: "4"` for v4 layout; 6 audit scripts enforcing the contract.

## Decisions taken (with why)

1. **`requires-capabilities: []` universal declaration** — every artifact outside `_common/` must declare; absence = build error `audit-capability-coverage.sh`; `[]` = universal (always included).
2. **Schema v3.0 — no dual-mode v2/v3** — backward compat with schema v2 would create dual-path complexity forever; MAJOR version bump + explicit announcement (Rule 19 §Forbidden).
3. **`capabilities/<category>/<id>.yaml`** — capability files in `capabilities/` (not embedded in skill frontmatter) enables graph analysis, cycle detection, mutex symmetry enforcement.
4. **`excludes-capabilities` symmetry invariant** — if A excludes B, B must exclude A; `audit-capability-graph.sh` exit 2 `AsymmetricMutex` on violation.
5. **Fragment-slot composition** — polymorphic skills (e.g., `x-review`) composed from specialist fragments; `fragment-slot` declares which slot to fill; `composition-priority` resolves conflicts.
6. **`flowVersion: "4"`** — artifacts under `ai/epics/epic-XXXX-<slug>/`; `PathResolver` auto-detects v3 vs v4 via filesystem probe.

## Alternatives rejected (with why)

- **Runtime capability injection (no frontmatter)** — requires assembler code changes for every new capability; frontmatter is declarative and self-describing.
- **Optional `requires-capabilities`** — optional fields drift to absent; enforcement requires mandatory presence.
- **Dual-mode v2/v3 frontmatter** — dual-path complexity is permanent tech debt; better to take the breaking bump once.

## Reusable patterns produced

- **`capability-aware-skill-via-frontmatter`**: skill declares `requires-capabilities: [X]`; composer includes only when X active in profile.
- **`requires-capabilities-universal-declaration`**: universal artifacts declare `requires-capabilities: []`; never absent field.
- **`fragment-slot-polymorphic-assembly`**: parent skill declares `fragment-slots: [...]`; fragments fill slots with `fragment-slot: { slot, fragment-id, fragment-order }`.
- **`capability-resolver-deterministic`**: same profile YAML + same capability set → identical `.claude/` output bytewise.

## Anti-patterns observed

- **Hardcoded skill registry list** — `SkillRegistry.CORE_KNOWLEDGE_PACKS:43` was root cause; never hardcode artifact inclusion; use `requires-capabilities: []`.
- **Implicit capability inheritance** — child assemblers should not inherit parent's capability context implicitly; always explicit.

## Links

- Epic: `ai/epics/epic-0064-capability-driven-composition/epic-0064.md`
- ADRs: `docs/adr/ADR-0016-capability-driven-composition.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0064-capability-driven-composition/reports/`
