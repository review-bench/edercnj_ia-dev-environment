# Task Breakdown — story-0075-0001

## Tasks

### TASK-0075-0001-001 — Capability `governance.ai-memory`
- Layer: Config / Capability
- Files: `capabilities/governance/ai-memory.yaml`, append `capabilities/_index.yaml`
- AC: YAML valid against schema; audit-capability-graph.sh exit 0

### TASK-0075-0001-002 — Rule 33 (AI Memory Production)
- Layer: Doc / Rule
- Deps: TASK-0075-0001-001
- Files: `src/main/resources/targets/claude/rules/33-ai-memory-production.md`
- AC: Rule defines Phase 5 MANDATORY, self-audit section, cross-ref to Rule 24/27

### TASK-0075-0001-003 — ADR-0028
- Layer: Doc / ADR
- Deps: TASK-0075-0001-002
- Files: `docs/adr/ADR-0028-ai-memory-layer.md`
- AC: Status Accepted; decisions aligned with epic §6

### TASK-0075-0001-004 — KP playbook + `ai/memory/` skeleton
- Layer: Doc / KP / Content
- Deps: TASK-0075-0001-003
- Files: `src/main/resources/targets/claude/knowledge/governance/ai-memory-playbook/index.md`, `ai/memory/README.md`, `ai/memory/_index.yaml`
- AC: KP covers when-to-create, tagging, dont's; README documents search; _index.yaml valid

### TASK-0075-0001-005 — DocsAssembler + tests
- Layer: Application / Assembler + Test
- Deps: TASK-0075-0001-004
- Files: `src/main/java/.../DocsAssembler.java`, `src/test/java/.../DocsAssemblerMemoryInitTest.java`
- AC: capability active → creates files; absent → skips; coverage ≥ 95%/90%
