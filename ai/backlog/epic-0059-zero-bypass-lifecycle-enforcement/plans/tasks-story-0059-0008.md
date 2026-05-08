---
generated-by: x-task-plan@unknown
generated-at: 2026-04-27T18:00:00Z
story-id: story-0059-0008
---

# Task Breakdown — story-0059-0008: Telemetria como Prova-de-Vida do Orquestrador

## Task List

| Task ID | Title | Layer | Size | Depends On | Branch |
| :--- | :--- | :--- | :--- | :--- | :--- |
| TASK-0059-0008-001 | Extend audit-execution-integrity.sh with telemetry validation | Adapter (CI script) | L | — | `feat/task-0059-0008-001-audit-telemetry` |
| TASK-0059-0008-002 | Create .claude/hooks/stage-telemetry.sh Stop hook | Adapter (hook script) | M | TASK-0059-0008-001 | `feat/task-0059-0008-002-stage-telemetry-hook` |
| TASK-0059-0008-003 | Document events.ndjson as committed evidence artifact | Doc | S | TASK-0059-0008-002 | `feat/task-0059-0008-003-telemetry-docs` |

---

## TASK-0059-0008-001: Extend audit-execution-integrity.sh with telemetry validation

**Layer:** Adapter (CI script)
**Test Type:** Smoke
**Size:** L
**Dependencies:** none

**Files to modify:**
1. `scripts/audit-execution-integrity.sh`
   - Add `check_telemetry()` function — validates 4 mandatory `phase.start` events per story
   - Add `discover_story_ids_from_commits()` function — extracts story IDs from PR commit messages
   - Integrate both into main audit flow after existing `check_phase1_evidence()` calls
   - Update `--self-check` to verify `check_telemetry` function is defined

**Files to create:**
1. `src/test/bash/audit-telemetry.bats` — smoke test script for telemetry validation

**Acceptance Criteria:**
- [ ] `discover_story_ids_from_commits()` extracts story IDs via `git log origin/develop..HEAD --format="%s %b" | grep -oP "story-\d{4}-\d{4}" | sort -u`
- [ ] `check_telemetry()` validates Phase-0-Prepare (mandatory, no alternative)
- [ ] `check_telemetry()` validates Phase-1-Plan with `[phase-1] skipped — PRE_PLANNED` alternative
- [ ] `check_telemetry()` validates Phase-2-Implement (mandatory, no alternative)
- [ ] `check_telemetry()` validates Phase-3-Verify (mandatory, no alternative)
- [ ] Exit 1 `EIE_TELEMETRY_MISSING` when any mandatory event is absent
- [ ] Grandfathered stories (in `audits/execution-integrity-baseline.txt`) skip telemetry check
- [ ] AT-06 regression: 171 events with no `x-story-implement` events → exit 1
- [ ] `--self-check` verifies `check_telemetry` is defined

**TDD Cycles:**
1. RED: UT-01 — missing events.ndjson → `check_telemetry` function does not exist
2. GREEN: add `check_telemetry()` skeleton with file existence check
3. RED: UT-02 — empty file → "Phase-0-Prepare missing"; skeleton only handles missing file
4. GREEN: add Phase-0-Prepare event grep check
5. RED: UT-06 — all 4 events present → expect return 0; only Phase-0 check exists
6. GREEN: add Phase-1, Phase-2, Phase-3 grep checks
7. RED: UT-07 — PRE_PLANNED alternative → expect return 0; fails
8. GREEN: add PRE_PLANNED alternative check for Phase-1
9. RED: UT-10 — grandfathered story → expect return 0 (skip); fails
10. GREEN: add `is_grandfathered()` call at start of `check_telemetry()`
11. RED: DS-01 through DS-05 — commit extraction
12. GREEN: implement `discover_story_ids_from_commits()` and integrate into main loop
13. REFACTOR: extract `_grep_phase_event()` helper; consolidate 4 phase checks into phase-array loop

---

## TASK-0059-0008-002: Create .claude/hooks/stage-telemetry.sh Stop hook

**Layer:** Adapter (hook script)
**Test Type:** Smoke
**Size:** M
**Dependencies:** TASK-0059-0008-001

**Files to create:**
1. `.claude/hooks/stage-telemetry.sh` — new Stop hook script

**Files to modify:**
1. `.claude/settings.json` — add `stage-telemetry.sh` to the Stop hooks array

**Files to create (tests):**
1. `src/test/bash/stage-telemetry.bats` — smoke test script for the Stop hook

**Acceptance Criteria:**
- [ ] Hook reads `execution-state.json` to detect active story with status `Em Andamento`
- [ ] When active story found and `events.ndjson` exists: `git add plans/epic-XXXX/telemetry/events.ndjson` executed
- [ ] When no active story: exit 0 (no-op)
- [ ] When `events.ndjson` absent: exit 0 (no-op, file not yet created)
- [ ] When `git add` fails: logs `WARN` to stderr and exits 0 (fail-open — must not interrupt LLM turn)
- [ ] When `execution-state.json` not found: exit 0 (no-op)
- [ ] Hook registered in `.claude/settings.json` under `hooks.Stop`
- [ ] jq-absent fallback: grep-based state parsing when jq not on PATH

**TDD Cycles:**
1. RED: SH-01 — story Em Andamento, events.ndjson present → expect git staging; hook absent
2. GREEN: implement `stage-telemetry.sh` with jq detection, state scan, git add
3. RED: SH-02 — no active story → expect no git add; script exits 0 but runs git add
4. GREEN: add "no active story → exit 0" guard before git add
5. RED: SH-03 — events.ndjson absent → expect no-op; script attempts git add on missing file
6. GREEN: add file existence check before git add
7. RED: SH-05 — git add fails → expect WARN + exit 0; currently exits non-zero
8. GREEN: wrap git add in if-clause; emit WARN on failure; always exit 0
9. REFACTOR: normalize STATE_FILE discovery loop; add jq-absent fallback

---

## TASK-0059-0008-003: Document events.ndjson as committed evidence artifact

**Layer:** Doc
**Test Type:** Verification
**Size:** S
**Dependencies:** TASK-0059-0008-002 (stable contract before documentation)

**Files to modify:**
1. `java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md`
   - Add `events.ndjson` to the "Mandatory Evidence Artifacts" table in the skill
   - Add note about `stage-telemetry.sh` Stop hook ensuring the file is committed
   - Reference EPIC-0059 story-0059-0008 in the artifact contract

2. `.claude/skills/x-story-implement/SKILL.md` (generated copy — update in sync)

3. `CHANGELOG.md`
   - Add entry under `[Unreleased]` → `### Added`:
     - Telemetry as proof-of-life: 4 mandatory `phase.start` events validated by audit
     - `stage-telemetry.sh` Stop hook for automatic `events.ndjson` staging

**Acceptance Criteria:**
- [ ] `x-story-implement` SKILL.md documents `events.ndjson` as committed evidence artifact
- [ ] SKILL.md references `stage-telemetry.sh` Stop hook as the staging mechanism
- [ ] CHANGELOG.md entry present for telemetry as proof-of-life
- [ ] Both source-of-truth and generated copy updated for SKILL.md

**TDD Cycles:**
1. RED: grep x-story-implement SKILL.md for "events.ndjson" as evidence → not found
2. GREEN: add mandatory evidence artifact row for events.ndjson
3. RED: grep CHANGELOG.md for "stage-telemetry" → not found
4. GREEN: add CHANGELOG entry
5. REFACTOR: ensure SKILL.md wording aligns with Rule 24 evidence artifact table format

---

## Implementation Map

```
TASK-0059-0008-001 (audit check_telemetry + commit extraction)
    ↓ unblocks
TASK-0059-0008-002 (stage-telemetry.sh Stop hook + settings.json)
    ↓ unblocks
TASK-0059-0008-003 (documentation: SKILL.md + CHANGELOG)
```

Serial execution required:
- Task 002 depends on task 001 (hook is the mechanism for getting telemetry committed so audit can validate it)
- Task 003 depends on task 002 (documentation written after contract is stable)
