# Implementation Plan — story-0071-0001

**Story:** Capability + Rule 31 + ADR-0024 + parse YAML `documentation.targets`
**Epic:** EPIC-0071 (Documentation as DoD)
**Scope:** STANDARD
**Planning mode:** INLINE

## Decisions Made

| Decision | Value | Rationale |
| :--- | :--- | :--- |
| Rule number (D-R3) | 31 | Next available after Rule 30; already cross-linked in Rule 30 |
| ADR number (D-R4) | 0024 | Next after ADR-0023 (value-driven-templates) |
| D-R7 Rule merge decision | SEPARATE (Rule 31) | <30% overlap; Rule 30 governs templates structure, Rule 31 governs freshness enforcement |
| D-R9 prereq | SATISFIED | `capabilities/` dir + `governance/schemas/capabilities-1.0.json` exist |

## Artifacts to Produce

1. `capabilities/governance/doc-as-dod.yaml` — capability definition
2. `src/main/resources/targets/claude/rules/31-documentation-freshness-gate.md` — Rule source-of-truth
3. `.claude/rules/31-documentation-freshness-gate.md` — generated output (same content)
4. `docs/adr/ADR-0024-documentation-freshness-gate.md` — architectural decision record
5. `src/main/java/dev/iadev/domain/model/DocumentationConfig.java` — new record
6. `src/main/java/dev/iadev/domain/model/Governance.java` — add documentation field
7. `src/main/java/dev/iadev/domain/model/ProjectConfig.java` — add documentation() accessor + parse helper
8. `src/test/java/dev/iadev/domain/model/DocumentationConfigTest.java` — TDD tests

## Design Decisions

- `DocumentationConfig(List<String> targets, int freshnessWindowHours)` — targets=empty means auto-detect in skills
- Added to `Governance` record as 5th component (consistent with CoreStack/TechStack 5-component pattern)
- `Governance.fromMap` extended to parse `documentation` sub-map
- `ProjectConfig.documentation()` delegating accessor
- Security: targets are logical names (readme, openapi, adr) — not file paths; no traversal concern in Java record
- Interface-based auto-detection deferred to skill level (filesystem checks cannot be in domain model)
