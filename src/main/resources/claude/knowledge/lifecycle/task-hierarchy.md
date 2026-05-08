---
name: kp-lifecycle-task-hierarchy
description: "Full reference for Rule 25 Task Hierarchy: subject regex BNF, valid/invalid examples, activeForm convention, metadata schema, taskTracking JSON shape, and enforcement integration details."
requires-capabilities: []
---

# Knowledge Pack: Task Hierarchy (Rule 25 — Full Reference)

## Subject Regex BNF

```
marker        ::= root (" › " level)*
root          ::= uppercase-id | canonical-id | "Phase N"
uppercase-id  ::= [A-Z][A-Z0-9-]+         (e.g., EPIC-0060, TASK-0060-0001-003, QA)
canonical-id  ::= "epic-" 4DIGIT | "story-" 4DIGIT "-" 4DIGIT | "task-" 4DIGIT "-" 4DIGIT ("-" 3DIGIT)?
level         ::= [A-Za-z0-9_\-\.:() ]+
separator     ::= " › "   (U+203A with surrounding spaces; ASCII ">" is INVALID)
max-depth     ::= 4 levels (root counts as level 1)
```

### Full Canonical Regex

```
^(?P<root>(?:[A-Z][A-Z0-9-]+|epic-[0-9]{4}|story-[0-9]{4}-[0-9]{4}|task-[0-9]{4}-[0-9]{4}(?:-[0-9]{3})?|Phase [0-9]+))(?: › (?P<levelN>[A-Za-z0-9_\-\.:() ]+))*$
```

## Valid/Invalid Examples

### Valid

| Subject | Depth | Notes |
| :--- | :--- | :--- |
| `story-0060-0001 › Phase 1 › Arch plan` | 3 | Standard story phase |
| `TASK-0060-0001-003 › Step 2 › Cycle 1 › Red` | 4 | Max depth — TDD inner loop |
| `EPIC-0060 › Phase 3 › story-0060-0001` | 3 | Epic → story transition |
| `QA › Review story-0060-0001` | 2 | Review skill legacy pattern |

### Invalid

| Subject | Why invalid |
| :--- | :--- |
| `QA review` | No root prefix → regex mismatch |
| `EPIC-0060 > Phase 3 > story` | ASCII `>` instead of `›` (U+203A) |
| `EPIC-0060 › Phase 3 › story › Phase 1 › Arch` | Depth 5 — exceeds max |
| `epic_0060 › Phase 1` | Underscore forbidden in root; use `epic-0060` |

## `activeForm` Convention

Gerund of `subject` without the root prefix, < 40 characters:

| `subject` | `activeForm` |
| :--- | :--- |
| `story-0060-0001 › Phase 1 › Arch plan` | `Planning arch for story-0060-0001` |
| `TASK-0060-0001-003 › Red cycle › UT-2` | `Running Red cycle UT-2` |
| `EPIC-0060 › Phase 4 › Integrity gate` | `Running integrity gate` |

## `metadata` Convention

```json
{
  "phase": "Phase 1",
  "parentSkill": "x-implement-story",
  "storyId": "story-0060-0001",
  "epicId": "EPIC-0060",
  "expectedArtifacts": [
    "ai/epics/epic-0060/plans/arch-story-0060-0001.md",
    "ai/epics/epic-0060/plans/plan-story-0060-0001.md",
    "ai/epics/epic-0060/plans/tests-story-0060-0001.md",
    "ai/epics/epic-0060/plans/tasks-story-0060-0001.md",
    "ai/epics/epic-0060/plans/security-story-0060-0001.md",
    "ai/epics/epic-0060/plans/compliance-story-0060-0001.md"
  ]
}
```

| Key | Required? | Description |
| :--- | :--- | :--- |
| `phase` | yes | Exact phase string (matches `## Phase N — <Name>`) |
| `parentSkill` | yes | Orchestrator name emitting this task |
| `storyId` / `epicId` / `taskId` | context-dependent | Whichever is relevant |
| `expectedArtifacts` | yes for POST gates | Relative paths from repo root |

## `taskTracking` JSON Shape

```json
{
  "flowVersion": "2",
  "epicId": "EPIC-0060",
  "taskTracking": {
    "enabled": true,
    "rootTaskId": 42,
    "phaseGateResults": [
      {
        "phase": "Phase 1",
        "mode": "post",
        "passed": true,
        "missingArtifacts": [],
        "missingTasks": []
      },
      {
        "phase": "Phase 2",
        "mode": "post",
        "passed": true,
        "missingArtifacts": [],
        "missingTasks": []
      }
    ]
  }
}
```

`taskTracking.enabled` defaults to `true` when absent. Explicit `enabled: false` is the opt-out for legacy epics.

## Integration with `x-internal-verify-phase-gates`

The `--mode post` gate on the **last evidence-producing phase** MUST include Rule 24 mandatory artifacts in `--expected-artifacts`. This makes Rule 24 enforcement synchronous rather than detectivo-only.

| Orchestrator | Phase | Required artifacts |
| :--- | :--- | :--- |
| `x-implement-story` | Phase 3 | `verify-envelope-STORY-ID.json`, `review-story-STORY-ID.md`, `techlead-review-story-STORY-ID.md`, `story-completion-report-STORY-ID.md` |

## Audit Scripts

- `scripts/audit-task-hierarchy.sh` (exit 25) — validates `TaskCreate` per phase, matching `TaskUpdate`, PRE/POST gates
- `scripts/audit-phase-gates.sh` (exit 26) — validates `phaseGateResults` in `execution-state.json`
