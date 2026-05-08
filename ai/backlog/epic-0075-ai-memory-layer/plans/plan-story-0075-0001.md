# Implementation Plan — story-0075-0001

**Story:** Capability + Rule 33 + ADR-0028 + KP playbook + `ai/memory/` skeleton
**Epic:** EPIC-0075

## Tasks

| Task | Description | Size |
|------|-------------|------|
| TASK-0075-0001-001 | Publish capability `governance.ai-memory` | S |
| TASK-0075-0001-002 | Publish Rule 33 (AI Memory Production) | M |
| TASK-0075-0001-003 | Publish ADR-0028 | S |
| TASK-0075-0001-004 | KP playbook + `ai/memory/` skeleton | M |
| TASK-0075-0001-005 | Extend DocsAssembler + tests | M |

## Implementation Sequence

All tasks run sequentially (001→002→003→004→005) on branch `feat/story-0075-0001`.
Commit per logical unit; PR auto-merges into `epic/0075`.

## Key Implementation Notes

- Rule 33 cross-references Rule 24 (MANDATORY TOOL CALL at Phase 5) and Rule 27 (surface 14)
- ADR-0028 format: Context / Decision / Consequences / Alternatives Considered
- KP playbook: ~100 lines, when-to-create + tagging guide + dont's (PII/secrets)
- DocsAssembler: add `initializeMemoryDirectory(config, engine, outputDir)` method ≤ 25 lines
- Test: two fixtures (capability active, capability absent) via `DocsAssemblerMemoryInitTest`
