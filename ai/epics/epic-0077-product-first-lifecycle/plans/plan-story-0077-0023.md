# Implementation Plan — story-0077-0023

**Story:** Gate em DoR (estende EPIC-0069): RNF_INHERITANCE_VIOLATION
**Status:** Concluída
**Planned at:** 2026-05-05T18:00:00Z

## 1. Scope

Extend `enforce-refinement-gate.sh` (Camada 0 hook) with a new exit code `34` (`RNF_INHERITANCE_VIOLATION`) that fires when a story targets `Em Andamento` but has unapproved RNF relaxations in `SECURITY` or `COMPLIANCE` categories.

## 2. File Footprint

**write:**
- `src/main/resources/targets/claude/hooks/enforce-refinement-gate.sh`
- `src/test/bash/enforce_refinement_gate_test.sh`
- `src/test/resources/golden/*/\.claude/hooks/enforce-refinement-gate.sh`

**read:**
- `ai/epics/epic-0077-product-first-lifecycle/story-0077-0023.md`
- `src/main/java/dev/iadev/domain/capability/RNFNoRelaxValidator.java` (reference)

## 3. Approach

1. Add `EXIT_RNF_INHERITANCE_VIOLATION=34` constant to hook
2. Add `validate_rnf_inheritance()` function parsing `## 2. RNFs Herdadas` table from story markdown
3. Invoke validation inside `"approved"` case branch, before `exit 0`
4. Update Bash unit tests
5. Regenerate golden files

## 4. Test Strategy

| Scenario | Expected |
|---|---|
| Story with approved RNF relaxation | exit 0 |
| Story with SECURITY RNF silently relaxed | exit 34 |
| Story with unapproved COMPLIANCE override | exit 34 |
| Story without `## 2. RNFs Herdadas` section | exit 0 (no-op) |
| flowVersion=1 story | exit 0 (legacy) |

## 5. Acceptance Criteria Coverage

- AC1 (happy-path): refinement approved, no violations → gate passes
- AC2 (error): SECURITY category relaxed without approval → exit 34, message shown
- AC3 (boundary): missing justification → exit 34
