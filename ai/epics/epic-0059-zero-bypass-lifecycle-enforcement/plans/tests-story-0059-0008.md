---
generated-by: x-test-plan@unknown
generated-at: 2026-04-27T18:00:00Z
story-id: story-0059-0008
---

# Test Plan — story-0059-0008: Telemetria como Prova-de-Vida do Orquestrador

## Outer Loop — Acceptance Tests

Mapped directly to the 6 Gherkin scenarios defined in story Section 7.

| # | Gherkin Scenario | Test Type | TPP Order | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| AT-01 | Audit passes when all 4 events are present | Smoke | 1 | exit 0; no EIE error |
| AT-02 | Audit fails when no x-story-implement events exist | Smoke | 2 | exit 1; `EIE_TELEMETRY_MISSING`; message "no x-story-implement telemetry for story-0059-0008" |
| AT-03 | Audit accepts Phase-1 as PRE_PLANNED | Smoke | 3 | exit 0; PRE_PLANNED marker accepted |
| AT-04 | Audit fails when Phase-2 absent but others present | Smoke | 4 | exit 1; `EIE_TELEMETRY_MISSING`; message "Phase-2-Implement missing" |
| AT-05 | Stop hook stages events.ndjson when story Em Andamento | Smoke | 5 | git status shows events.ndjson staged |
| AT-06 | EPIC-0057 regression: 171 events none x-story-implement → exit 1 | Smoke | 6 | exit 1; `EIE_TELEMETRY_MISSING` for each story |

## Inner Loop — Unit Tests (Transformation Priority Premise order)

### `check_telemetry()` function — `audit-execution-integrity.sh`

| # | Test Case | Input | Expected |
| :--- | :--- | :--- | :--- |
| UT-01 | events.ndjson does not exist | missing file | return 1, "events.ndjson not found" |
| UT-02 | events.ndjson exists but empty | empty file | return 1, "Phase-0-Prepare missing" |
| UT-03 | Only Phase-0 present | 1 event | return 1, multiple phases missing |
| UT-04 | Phase-0 + Phase-1 + Phase-3 present, Phase-2 absent | 3 events | return 1, "Phase-2-Implement missing" |
| UT-05 | All 4 events for wrong story ID | different storyId in events | return 1 (story ID mismatch) |
| UT-06 | All 4 events for correct story ID | 4 matching events | return 0 |
| UT-07 | Phase-1 absent but `[phase-1] skipped — PRE_PLANNED` present | PRE_PLANNED marker | return 0 |
| UT-08 | Phase-1 absent, no PRE_PLANNED marker | no Phase-1 coverage | return 1, "Phase-1-Plan missing" |
| UT-09 | All 4 events for correct story, plus 171 irrelevant events | mixed events.ndjson | return 0 (not affected by noise) |
| UT-10 | Story in grandfathered baseline | grandfathered story ID | return 0 (skip, no check) |

### `discover_story_ids_from_commits()` function

| # | Test Case | Git Log Output | Expected |
| :--- | :--- | :--- | :--- |
| DS-01 | No commits on branch | empty log | empty output |
| DS-02 | Commits reference one story | "feat(story-0059-0008): implement X" | `story-0059-0008` |
| DS-03 | Commits reference multiple stories | mixed commit messages | all unique story IDs, sorted |
| DS-04 | Commits mention story ID in body, not subject | "%s %b" covers both | story ID extracted from body |
| DS-05 | Commit message with partial match | "story-59-8" (wrong format) | no match (pattern requires 4 digits each) |

### `stage-telemetry.sh` Stop hook

| # | Test Case | execution-state.json | events.ndjson | Expected |
| :--- | :--- | :--- | :--- | :--- |
| SH-01 | Story Em Andamento + events.ndjson exists | status=Em Andamento | present | git add succeeds; file staged |
| SH-02 | No story Em Andamento | all stories Pendente or Concluída | any | no git add; exit 0 |
| SH-03 | Story Em Andamento but events.ndjson absent | status=Em Andamento | absent | no git add; exit 0 (no-op) |
| SH-04 | execution-state.json not found | file missing | any | exit 0 (no-op) |
| SH-05 | git add fails (locked index) | status=Em Andamento | present | logs WARN to stderr; exit 0 (fail-open) |

## Smoke Test Script Locations

| Script | Tests Covered |
| :--- | :--- |
| `src/test/bash/audit-telemetry.bats` | AT-01 through AT-06, UT-01 through UT-10, DS-01 through DS-05 |
| `src/test/bash/stage-telemetry.bats` | AT-05, SH-01 through SH-05 |

## Coverage Targets

| Component | Line Coverage Target | Branch Coverage Target |
| :--- | :--- | :--- |
| `check_telemetry()` function | ≥ 95% scenario coverage | ≥ 90% scenario coverage |
| `discover_story_ids_from_commits()` | ≥ 95% scenario coverage | ≥ 90% scenario coverage |
| `stage-telemetry.sh` | ≥ 95% scenario coverage | ≥ 90% scenario coverage |

Note: Coverage for Bash scripts is measured as scenario coverage (all acceptance test scenarios pass = 100% scenario coverage). kcov/bashcov are not required by the project.

## TDD Cycle Order (TPP)

### TASK-0059-0008-001

1. **Red:** Write UT-01 — missing events.ndjson → `check_telemetry` does not exist yet → fails
2. **Green:** Add `check_telemetry()` with file existence check → return 1 on missing file
3. **Red:** Write UT-02 — empty file → fails (skeleton returns 1 only for missing file)
4. **Green:** Add Phase-0-Prepare event check; empty file produces "Phase-0 missing"
5. **Red:** Write UT-06 — all 4 events present → expect return 0; fails (only Phase-0 check exists)
6. **Green:** Add Phase-1, Phase-2, Phase-3 checks
7. **Red:** Write UT-07 — PRE_PLANNED alternative → expect return 0; fails (PRE_PLANNED not handled)
8. **Green:** Add PRE_PLANNED alternative check for Phase-1
9. **Red:** Write AT-06 — 171 events, none x-story-implement → expect exit 1
10. **Green:** Confirm `x-story-implement` is required in the grep pattern — already enforced by UT-05 fix
11. **Red:** Write UT-10 — grandfathered story → expect return 0 (skip)
12. **Green:** Add `is_grandfathered()` check at start of `check_telemetry()`
13. **Refactor:** Extract `_grep_phase_event()` helper; consolidate 4 nearly-identical phase checks into a loop with a required-phases array

### TASK-0059-0008-002

1. **Red:** Write SH-01 — story Em Andamento, events.ndjson present → expect git staging; hook does not exist
2. **Green:** Implement `stage-telemetry.sh` with jq detection, state file scan, git add
3. **Red:** Write SH-02 — no active story → expect no-op (no git add called)
4. **Green:** Add guard: if no active story found → exit 0
5. **Red:** Write SH-03 — events.ndjson absent → expect no-op
6. **Green:** Add guard: if events file not found → exit 0
7. **Red:** Write SH-05 — git add fails → expect WARN + exit 0 (fail-open)
8. **Green:** Wrap git add in if-clause; log WARN on failure; always exit 0
9. **Refactor:** Normalize STATE_FILE discovery; add jq-absent fallback with grep-based state parsing

## Gherkin Scenarios — Full Text

```gherkin
Cenario: Audit passes when all 4 events are present (AT-01)
  DADO que o PR menciona story-0059-0008 nos commits
  E events.ndjson contém os 4 eventos phase.start de x-story-implement:
    | Phase-0-Prepare | Phase-1-Plan | Phase-2-Implement | Phase-3-Verify |
  QUANDO audit-execution-integrity.sh é executado
  ENTÃO retorna exit 0

Cenario: Audit fails when no x-story-implement events exist (AT-02)
  DADO que o PR menciona story-0059-0008 nos commits
  MAS events.ndjson não tem nenhum evento "phase.start" de "x-story-implement"
  QUANDO audit-execution-integrity.sh é executado
  ENTÃO retorna exit 1 (EIE_TELEMETRY_MISSING)
  E a mensagem indica "no x-story-implement telemetry for story-0059-0008"

Cenario: Audit accepts Phase-1 as PRE_PLANNED (AT-03)
  DADO que events.ndjson tem Phase-0, Phase-2, Phase-3 de x-story-implement
  E tem "[phase-1] skipped — PRE_PLANNED" para a story
  QUANDO audit é executado
  ENTÃO retorna exit 0 (Phase 1 skipped é aceito)

Cenario: Audit fails when Phase-2 is absent (AT-04)
  DADO que events.ndjson tem Phase-0, Phase-1, Phase-3 mas NÃO Phase-2
  QUANDO audit é executado
  ENTÃO retorna exit 1 (EIE_TELEMETRY_MISSING)
  E indica "Phase-2-Implement missing for story-0059-0008"

Cenario: Stop hook stages events.ndjson when story is Em Andamento (AT-05)
  DADO que execution-state.json indica story-0059-0008 Em Andamento
  E events.ndjson existe em plans/epic-0059/telemetry/events.ndjson
  QUANDO stage-telemetry.sh é executado (Stop event)
  ENTÃO git status mostra events.ndjson como staged

Cenario: EPIC-0057 regression bypass detection (AT-06)
  DADO que nenhum evento de x-story-implement existe em events.ndjson
  E events.ndjson tem 171 outros eventos (como em EPIC-0057)
  QUANDO audit é executado para qualquer story mencionada nos commits do PR
  ENTÃO retorna exit 1 (EIE_TELEMETRY_MISSING) para cada story
```
