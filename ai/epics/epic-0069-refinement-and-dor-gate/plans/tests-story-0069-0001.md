# Test Plan — story-0069-0001

**Story:** Capability `governance.refinement-gate` + Rule 29 + ADR-0022

## Test Approach
This story produces documentation/content artifacts (YAML, Markdown). Tests validate structural contracts, not runtime behavior.

## Acceptance Tests (via CI audit scripts)

| Test | Script | Expected |
|------|--------|----------|
| Capability schema valid | `audit-frontmatter-schema.sh` | exit 0 |
| Capability coverage complete | `audit-capability-coverage.sh` | exit 0 |
| Capability graph integrity | `audit-capability-graph.sh` | exit 0 |
| Rule 29 exists at correct path | manual file check | exists |
| ADR-0022 exists at correct path | manual file check | exists |
| KP dimensions.md exists | manual file check | exists |
| `capabilities/_index.yaml` lists `governance.refinement-gate` | grep check | found |

## Manual Validation Checklist
- [ ] `capabilities/governance/refinement-gate.yaml` — schema-valid YAML with `id: governance.refinement-gate`
- [ ] Rule 29 contains `## State Machine Extension` section
- [ ] Rule 29 defines transitions: `Pendente → Refinada`, `Refinada → Planejada`, `Refinada → Em Andamento | Bloqueada`
- [ ] ADR-0022 follows Nygard format with Context/Decision/Consequences sections
- [ ] KP dimensions.md has entries for all 6 story dimensions and 7 epic dimensions
