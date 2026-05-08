# Architecture Plan — story-0077-0000

**Story:** ADR Amendment — Rule 14 Extension for Product-First Runtime Domain

## Architecture Context

No production Java code introduced. Normative/documentation story.
Impact: Rule 14 source-of-truth file + new ADR.

## Structural Changes

| File | Change | Layer |
| :--- | :--- | :--- |
| `docs/adr/ADR-0030-rule14-product-first-domain.md` | New ADR (Accepted) | Documentation |
| `docs/adr/README.md` | Add ADR-0030 entry | Documentation |
| `src/main/resources/targets/claude/rules/14-project-scope.md` | Add Product-First Extension section | Rule (source-of-truth) |
| `CHANGELOG.md` | Add entry under [Added] | Release artifact |

## OCP Design

Rule 14 extended by **addition** of `## Product-First Domain Extension (EPIC-0077)` section.
No existing sections removed or altered.
