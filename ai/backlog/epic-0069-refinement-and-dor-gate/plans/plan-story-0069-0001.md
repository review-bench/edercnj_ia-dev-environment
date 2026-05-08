# Implementation Plan — story-0069-0001

**Story:** Capability `governance.refinement-gate` + Rule 29 + ADR-0022
**Epic:** EPIC-0069 (Story Refinement & DoR Gate)
**Scope:** STANDARD
**Planning Mode:** INLINE

## Summary

Publish the normative foundation for the Refinement Gate:
1. Capability `governance.refinement-gate` YAML (universal)
2. Rule 29 — Refinement Gate (normative rule with §State Machine Extension)
3. ADR-0022 — Refinement Gate (was working-title ADR-0018; ADR-0018 taken by zero-bypass-amnesty)
4. Knowledge Pack `refinement/dimensions.md` (shared heuristics for stories 0002/0003)
5. Update `capabilities/_index.yaml` to register the new `governance` category

## D-R6 Resolution

- Rule 29: **free** → keep as-is
- ADR-0018: **taken** (`ADR-0018-zero-bypass-amnesty.md`) → use **ADR-0022**
- All cross-references updated accordingly

## Tasks

### TASK-0069-0001-001: Create `capabilities/governance/` directory and `refinement-gate.yaml`
- Path: `capabilities/governance/refinement-gate.yaml`
- Universal capability (no `requires` dependencies)
- `requires-capabilities: []`

### TASK-0069-0001-002: Register `governance` category in `capabilities/_index.yaml`
- Add `governance` category entry with `governance.refinement-gate`

### TASK-0069-0001-003: Write Rule 29 — Refinement Gate
- Path: `src/main/resources/targets/claude/rules/29-refinement-gate.md`
- Also copy to `.claude/rules/29-refinement-gate.md` (generated output)
- Includes §State Machine Extension with `Refinada` status

### TASK-0069-0001-004: Write ADR-0022 — Refinement Gate
- Path: `docs/adr/ADR-0022-refinement-gate.md`

### TASK-0069-0001-005: Write Knowledge Pack `refinement/dimensions.md`
- Path: `src/main/resources/targets/claude/knowledge/refinement/dimensions.md`

## Dependency Direction
Infrastructure-only changes (content layer). No new Java classes — story-0069-0004 handles the domain model.
