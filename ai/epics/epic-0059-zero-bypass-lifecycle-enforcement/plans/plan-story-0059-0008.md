---
generated-by: x-internal-story-build-plan@unknown
generated-at: 2026-04-27T18:00:00Z
story-id: story-0059-0008
---

# Implementation Plan — story-0059-0008: Telemetria como Prova-de-Vida do Orquestrador

## Scope

- **Epic:** EPIC-0059 (Zero-Bypass Lifecycle Enforcement)
- **Story:** story-0059-0008
- **Scope Classification:** STANDARD (3 tasks, Bash scripts only, no Java)
- **Planning Mode:** PRE_PLANNED

## Task Summary

| Task ID | Description | Layer | Size | Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| TASK-0059-0008-001 | Extend audit-execution-integrity.sh with telemetry validation | Adapter (CI script) | L | — |
| TASK-0059-0008-002 | Create .claude/hooks/stage-telemetry.sh Stop hook | Adapter (hook script) | M | TASK-0059-0008-001 |
| TASK-0059-0008-003 | Document events.ndjson as committed evidence artifact | Doc | S | TASK-0059-0008-002 |

## Implementation Order

Inner layers first per Rule 04:

1. **TASK-0059-0008-001** — Extend the audit script with `check_telemetry()`. This is the core enforcement mechanism; the Stop hook in task 002 is only useful once the audit is in place to catch missing evidence.
2. **TASK-0059-0008-002** — Create `stage-telemetry.sh` and register it in `settings.json`. Depends on task 001 because the hook is the mechanism by which telemetry becomes committed evidence for the audit.
3. **TASK-0059-0008-003** — Documentation and CHANGELOG. Can only be written accurately once tasks 001 and 002 are implemented and the contract is stable.

## Key Implementation Details

### TASK-0059-0008-001: Extend audit-execution-integrity.sh with check_telemetry()

**Target file:** `scripts/audit-execution-integrity.sh`

**New function `check_telemetry()`:**

```bash
check_telemetry() {
    local story_id="$1"
    local events_file

    # Locate events.ndjson for this story's epic
    local epic_id
    epic_id=$(echo "${story_id}" | grep -oP "story-(\d{4})-\d{4}" | sed 's/story-//;s/-.*//')
    events_file="plans/epic-${epic_id}/telemetry/events.ndjson"

    if [[ ! -f "${events_file}" ]]; then
        echo "EIE_TELEMETRY_MISSING: ${story_id} — events.ndjson not found at ${events_file}" >&2
        return 1
    fi

    local failures=0

    # Phase-0-Prepare: mandatory, no alternative
    if ! grep -q "\"phase\":\"Phase-0-Prepare\"" "${events_file}" || \
       ! grep -q "\"skill\":\"x-story-implement\"" "${events_file}" || \
       ! grep -q "\"storyId\":\"${story_id}\"" "${events_file}"; then
        # Combined check using single grep for efficiency
        if ! grep -P "\"phase\.start\".*\"x-story-implement\".*\"Phase-0-Prepare\".*\"${story_id}\"" "${events_file}" 2>/dev/null | grep -q .; then
            echo "EIE_TELEMETRY_MISSING: ${story_id} — Phase-0-Prepare missing" >&2
            ((failures++))
        fi
    fi

    # Phase-1-Plan: mandatory OR [phase-1] skipped — PRE_PLANNED
    local phase1_ok=0
    if grep -P "\"phase\.start\".*\"x-story-implement\".*\"Phase-1-Plan\".*\"${story_id}\"" "${events_file}" 2>/dev/null | grep -q .; then
        phase1_ok=1
    elif grep -q "\[phase-1\] skipped.*PRE_PLANNED" "${events_file}" 2>/dev/null; then
        phase1_ok=1
    fi
    if [[ "${phase1_ok}" -eq 0 ]]; then
        echo "EIE_TELEMETRY_MISSING: ${story_id} — Phase-1-Plan missing (and no PRE_PLANNED marker)" >&2
        ((failures++))
    fi

    # Phase-2-Implement: mandatory, no alternative
    if ! grep -P "\"phase\.start\".*\"x-story-implement\".*\"Phase-2-Implement\".*\"${story_id}\"" "${events_file}" 2>/dev/null | grep -q .; then
        echo "EIE_TELEMETRY_MISSING: ${story_id} — Phase-2-Implement missing" >&2
        ((failures++))
    fi

    # Phase-3-Verify: mandatory, no alternative
    if ! grep -P "\"phase\.start\".*\"x-story-implement\".*\"Phase-3-Verify\".*\"${story_id}\"" "${events_file}" 2>/dev/null | grep -q .; then
        echo "EIE_TELEMETRY_MISSING: ${story_id} — Phase-3-Verify missing" >&2
        ((failures++))
    fi

    if [[ "${failures}" -gt 0 ]]; then
        return 1
    fi
    return 0
}
```

**Story ID extraction from PR commits:**

```bash
discover_story_ids_from_commits() {
    git log origin/develop..HEAD --format="%s %b" \
        | grep -oP "story-\d{4}-\d{4}" \
        | sort -u
}
```

**Integration into main audit flow:**
After the existing `check_phase1_evidence()` calls, iterate over discovered story IDs:

```bash
while IFS= read -r story_id; do
    if is_grandfathered "${story_id}"; then
        continue
    fi
    if ! check_telemetry "${story_id}"; then
        story_failed=1
    fi
done < <(discover_story_ids_from_commits)
```

**Exit codes (additions):**
- Exit 1 `EIE_TELEMETRY_MISSING` — at least one story missing mandatory telemetry event(s)

**`--self-check` extension:**
Add check that `check_telemetry` function is defined in the script:
```bash
grep -q "^check_telemetry()" "${BASH_SOURCE[0]}" || {
    echo "OPERATIONAL_ERROR: check_telemetry function missing from audit script" >&2
    exit 2
}
```

**Test file:** `src/test/bash/audit-telemetry.bats`

### TASK-0059-0008-002: Create .claude/hooks/stage-telemetry.sh

**New file:** `.claude/hooks/stage-telemetry.sh`

```bash
#!/usr/bin/env bash
# stage-telemetry.sh — Stop hook: stage events.ndjson when a story is in progress
# Registered in .claude/settings.json as a Stop hook (EPIC-0059, story-0059-0008)

set -euo pipefail

CLAUDE_PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || echo ".")}"

# Find the active execution-state.json
STATE_FILE=""
for state_candidate in "${CLAUDE_PROJECT_DIR}"/plans/epic-*/execution-state.json; do
    if [[ -f "${state_candidate}" ]]; then
        # Check if any story is Em Andamento
        if command -v jq >/dev/null 2>&1; then
            active_story=$(jq -r '
                .storyStatuses
                | to_entries[]
                | select(.value.status == "Em Andamento")
                | .key
            ' "${state_candidate}" 2>/dev/null | head -1)
        else
            active_story=$(grep -o '"[^"]*": *{"status": *"Em Andamento"' "${state_candidate}" \
                | head -1 | grep -oP '"story-\d{4}-\d{4}"' | tr -d '"')
        fi

        if [[ -n "${active_story}" ]]; then
            STATE_FILE="${state_candidate}"
            break
        fi
    fi
done

if [[ -z "${STATE_FILE}" ]]; then
    # No active story — no-op
    exit 0
fi

# Derive epic directory from state file path
EPIC_DIR=$(dirname "${STATE_FILE}")

EVENTS_FILE="${EPIC_DIR}/telemetry/events.ndjson"

if [[ ! -f "${EVENTS_FILE}" ]]; then
    # No telemetry file yet — no-op (will be created when telemetry runs)
    exit 0
fi

# Stage events.ndjson
if ! git -C "${CLAUDE_PROJECT_DIR}" add "${EVENTS_FILE}" 2>/dev/null; then
    echo "WARN stage-telemetry.sh: failed to stage ${EVENTS_FILE}" >&2
fi

exit 0
```

**Registration in `.claude/settings.json`:**
Add `stage-telemetry.sh` to the `Stop` hook array in settings.json. The hook registration follows the pattern of existing Stop hooks in the file.

**Test file:** `src/test/bash/stage-telemetry.bats`

### TASK-0059-0008-003: Documentation

**Files to update:**

1. `java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md`:
   - Add to the "Mandatory Evidence Artifacts" section (or create it if absent):
     ```markdown
     | `events.ndjson` | `plans/epic-XXXX/telemetry/events.ndjson` | Camada 2 (Stop hook) + Camada 3 (CI audit) |
     ```
   - Add note: "The `stage-telemetry.sh` Stop hook ensures this file is committed to the repository at the end of each LLM turn."

2. `CHANGELOG.md` — add entry under `[Unreleased]`:
   ```markdown
   ### Added
   - Telemetry as proof-of-life: `audit-execution-integrity.sh` now validates presence of 4 mandatory `phase.start` events for `x-story-implement` in `events.ndjson` per story (EPIC-0059, story-0059-0008)
   - `stage-telemetry.sh` Stop hook auto-stages `events.ndjson` when a story is in progress
   ```

## TDD Approach

All tests are Bash smoke tests (no BATS framework required — standalone shell scripts usable in CI).

### TASK-0059-0008-001 TDD cycles

1. **Red:** Write AT-01 — PR with story-0059-0008 commits, no events.ndjson → exit 1 `EIE_TELEMETRY_MISSING`
2. **Green:** Add `check_telemetry()` skeleton: detect missing file → exit 1
3. **Red:** Write AT-02 — all 4 events present → expect exit 0; currently fails (function body incomplete)
4. **Green:** Implement all 4 phase event checks
5. **Red:** Write AT-03 — Phase-1 with PRE_PLANNED alternative → expect exit 0
6. **Green:** Add PRE_PLANNED alternative check for Phase-1
7. **Red:** Write AT-04 — Phase-2 absent, others present → expect exit 1
8. **Green:** Verify phase-2 check logic; already covered by step 4
9. **Red:** Write AT-06 — EPIC-0057 regression: 171 events, none x-story-implement → exit 1
10. **Green:** Confirm `check_telemetry()` correctly fails when no `x-story-implement` events exist despite large event count
11. **Refactor:** Extract `_grep_phase_event()` helper to reduce duplication across the 4 phase checks

### TASK-0059-0008-002 TDD cycles

1. **Red:** Write AT-05 — execution-state.json shows story Em Andamento, events.ndjson exists → expect git staging
2. **Green:** Implement `stage-telemetry.sh` with jq detection and git add
3. **Red:** Write AT-05b — no active story → expect no-op (events.ndjson not staged)
4. **Green:** Add "no active story → exit 0" guard
5. **Refactor:** Normalize STATE_FILE discovery loop; add fallback for environments without jq

## File Footprint

### write:
- `scripts/audit-execution-integrity.sh`
- `.claude/hooks/stage-telemetry.sh`
- `.claude/settings.json`
- `java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md`
- `.claude/skills/x-story-implement/SKILL.md`
- `CHANGELOG.md`
- `src/test/bash/audit-telemetry.bats`
- `src/test/bash/stage-telemetry.bats`

### read:
- `plans/epic-0059/story-0059-0008.md`
- `plans/epic-*/execution-state.json`
- `plans/epic-*/telemetry/events.ndjson`
- `audits/execution-integrity-baseline.txt`
- `.claude/settings.json` (current hook registrations)
