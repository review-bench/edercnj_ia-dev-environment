# Architecture Plan — story-0070-0001

**Story:** Capability + Rule 30 + ADR-0023 + decisão substituição EPIC-0056
**Scope:** STANDARD — governance artifact creation (YAML, Markdown, no Java production code)

## Decisions

### Rule Number: 30
- Existing rules: 01, 03-09, 12-14, 19-29, 45. Next free after 29 is **30**.
- EPIC-0071 reserves palpite 31 — no collision.

### ADR Number: 0023
- Existing ADRs: 0001-0022, 0048. Next sequential free is **0023**.
- ADR-0019 (preflight) and ADR-0022 (refinement-gate) are taken.

### D-R7 Decision: Rule Separada
- EPIC-0070 governs template structure (value-driven content) → Rule 30
- EPIC-0071 governs documentation freshness gate (temporal enforcement) → Rule 31
- Invariant overlap < 30% — distinct subject matters → separate rules + cross-links

## Artifact Map

| Artifact | Path | Action |
|----------|------|--------|
| Capability YAML | `capabilities/governance/value-driven-templates.yaml` | CREATE |
| Capability index | `capabilities/_index.yaml` | UPDATE (add entry) |
| Rule (source-of-truth) | `src/main/resources/targets/claude/rules/30-value-driven-templates.md` | CREATE |
| Rule (generated output) | `.claude/rules/30-value-driven-templates.md` | CREATE |
| ADR | `docs/adr/ADR-0023-value-driven-templates.md` | CREATE |
| SUPERSEDED block | `ai/epics/epic-0056-*/epic-0056.md` | UPDATE |
| Audit gates catalog | `docs/audit-gates-catalog.md` | UPDATE (reserve audit-template-version.sh) |

## Architecture Constraints
- Capability YAML MUST validate against `governance/schemas/capabilities-1.0.json`
- Rule MUST have frontmatter `requires-capabilities: []` (universal)
- Sections in Rule: `## Purpose`, `## Forbidden`, `## Audit`
- ADR MUST follow canonical schema (H1, Context, Decision, Consequences, Status: Accepted)
- SUPERSEDED block MUST be inserted after H1, before metadata fields (D-R9 format)
- Path: capability ID = `governance.value-driven-templates`
