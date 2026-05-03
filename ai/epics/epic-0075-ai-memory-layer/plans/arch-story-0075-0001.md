# Architecture Plan — story-0075-0001

**Story:** story-0075-0001 — Capability + Rule 33 + ADR-0028 + KP playbook + `ai/memory/` skeleton
**Epic:** EPIC-0075 (AI Memory Layer)
**Scope:** STANDARD
**planningMode:** PRE_PLANNED

## Architecture Decision

This story is purely additive governance — no runtime logic beyond `DocsAssembler` extension.

### New Artifacts

| Artifact | Path | Kind |
|----------|------|------|
| Capability | `capabilities/governance/ai-memory.yaml` | YAML |
| Capability index entry | `capabilities/_index.yaml` (append) | YAML |
| Rule 33 | `src/main/resources/targets/claude/rules/33-ai-memory-production.md` | Markdown |
| ADR-0028 | `docs/adr/ADR-0028-ai-memory-layer.md` | Markdown |
| KP playbook | `src/main/resources/targets/claude/knowledge/governance/ai-memory-playbook/index.md` | Markdown |
| ai/memory README | `ai/memory/README.md` | Markdown |
| ai/memory index | `ai/memory/_index.yaml` | YAML |
| DocsAssembler extension | `src/main/java/dev/iadev/application/assembler/DocsAssembler.java` (modify) | Java |
| DocsAssembler test | `src/test/java/dev/iadev/application/assembler/DocsAssemblerMemoryInitTest.java` | Java |

### Dependency Direction

`DocsAssembler` depends only on `ProjectConfig` (domain model) — domain purity maintained. Capability check via `projectConfig.hasCapability("governance.ai-memory")` (existing pattern).

### Key Decisions

1. Rule number: **33** (sequential after Rule 32 dependency-policy-gate)
2. ADR number: **ADR-0028** (sequential after ADR-0027)
3. `_index.yaml`: flat list schema (per story refinement decision)
4. No auto-archiving — manual via `indexable: false` flag
