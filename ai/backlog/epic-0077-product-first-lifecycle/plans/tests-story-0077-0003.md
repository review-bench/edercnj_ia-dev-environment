# Test Plan — story-0077-0003

**Story:** story-0077-0003 — Coordenação EPIC-0065: rename x-feature-create  
**Date:** 2026-05-04

---

## Test Strategy

This is a coordination/documentation story. Tests are bash-script-level verifications, not JUnit.

## Test Cases

### TASK-0077-0003-002: audit-skill-references.sh

| # | Test | Expected |
| :--- | :--- | :--- |
| T1 | Run `audit-skill-references.sh` on current codebase | Exit 0, "0 legacy references found" |
| T2 | Verify `x-create-feature/SKILL.md` exists | File present |
| T3 | Verify `x-feature-create/` directory absent | Not found |
| T4 | `skill-rename-smoke.sh` exits 0 | Smoke passes |

### TASK-0077-0003-003: DEPRECATIONS.md

| # | Test | Expected |
| :--- | :--- | :--- |
| D1 | `DEPRECATIONS.md` contains `x-feature-create` entry | Grep finds entry |
| D2 | Migration guide exists at expected path | File present |
| D3 | Migration guide contains before/after comparison | Both `x-feature-create` and `x-create-feature` sections present |

## Acceptance Criteria Mapping

| Scenario | Covered by |
| :--- | :--- |
| Skill rename executado sem quebra semântica | T1, T2, T3 |
| Zero referências a x-feature-create em paths ativos | T1 |
| Audit script reporta clean | T4 |
| Backward compat via DEPRECATIONS.md | D1, D3 |
