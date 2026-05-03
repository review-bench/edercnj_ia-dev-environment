# Test Plan — story-0075-0001

**Story:** story-0075-0001
**Scope:** Governance artifacts + DocsAssembler extension

## Test Coverage

### Unit Tests — DocsAssemblerMemoryInitTest

| Scenario | Expected |
|----------|----------|
| capability active → README + _index.yaml written | Files exist in output dir |
| capability absent → ai/memory/ NOT created | Directory absent |
| idempotent: run twice with active capability | Files not overwritten, same content |
| path traversal blocked | SecurityException |

### Verification Tests (manual)

| Check | Tool |
|-------|------|
| `audit-capability-graph.sh` exit 0 | bash |
| Rule 33 file exists in .claude/rules/ after regen | ls |
| `_index.yaml` valid YAML with `entries: []` | yq/python |

## Coverage Target
≥ 95% line / ≥ 90% branch on `DocsAssembler` new method.
