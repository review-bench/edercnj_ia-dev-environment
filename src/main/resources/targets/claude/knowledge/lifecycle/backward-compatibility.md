---
name: kp-lifecycle-backward-compatibility
description: "Full reference for Rule 19 Backward Compatibility: flowVersion fallback matrix, taskTracking fallback, interactiveMode fallback, refinementVerdict fallback, productFirstLifecycle fallback, deprecation window, orphan stories, skill renaming."
requires-capabilities: []
---

# Knowledge Pack: Backward Compatibility (Rule 19 — Full Reference)

## `flowVersion` Fallback Matrix

| Condition on field | Resolved value | Behavior | Warning? |
| :--- | :--- | :--- | :--- |
| Field absent (legacy pre-EPIC-0049) | `"1"` | Legacy flow | **Yes** |
| Field = `"1"` (explicit) | `"1"` | Legacy flow | No |
| Field = `"2"` (explicit) | `"2"` | New flow, v3 layout (`plans/`) | No |
| Field = `"3"` (explicit) | `"3"` | **EPIC-0061 Local-First** — non-interactive default | No |
| Field = `"4"` (explicit) | `"4"` | New flow, v4 layout (`ai/epics/`) | No |
| Field = `"5"` (explicit) | `"5"` | **EPIC-0077 Product-First** — task tracking mandatory, refinement gate active | No |
| Any other value (typo/unknown) | `"1"` | Legacy flow | **Yes** |

**Warning format:**

```
WARN [flowVersion-fallback] execution-state.json has flowVersion=<value>;
     defaulting to legacy flow (v1).
```

## `flowVersion` Version Semantics

| Version | Introduced | Key features |
| :--- | :--- | :--- |
| `"1"` | EPIC-0042 | Story PRs → develop; no epic branch; no auto-merge gate |
| `"2"` | EPIC-0049 | Story PRs → epic/XXXX; sequential default; task tracking required |
| `"3"` | EPIC-0061 | Non-interactive default; Java audits in CI; bash audits per stack |
| `"4"` | EPIC-0060 | v4 layout: `ai/epics/<epic-slug>/` via PathResolver |
| `"5"` | EPIC-0077 | Product-First: C4 gates, RNF gate, `productFirstLifecycle: true` |

## `taskTracking` Fallback Matrix

| Condition on `taskTracking` | `flowVersion` | Resolved behavior | Warning? |
| :--- | :--- | :--- | :--- |
| Field absent | `"1"` or absent | `enabled=false` — tracking disabled (legacy) | **Yes** |
| Field absent | `"2"` | **FAIL: `TASK_TRACKING_REQUIRED`** | N/A — hard fail |
| `taskTracking.enabled = false` | `"1"` | Tracking skipped, gates no-ops | No |
| `taskTracking.enabled = false` | `"2"` | Tracking skipped — **WARN: suspicious** | **Yes** |
| `taskTracking.enabled = true` | any | Full tracking active | No |

Migration: run `scripts/migrate-task-tracking-v2.sh` before enabling `audit-flow-version.sh`.

## `interactiveMode` Fallback Matrix

| Condition on `interactiveMode` | Resolved | Behavior | Warning? |
| :--- | :--- | :--- | :--- |
| Field absent (pre-EPIC-0068) | `"interactive"` | Hook no-op — preserves legacy | No |
| `"interactive"` (explicit) | `"interactive"` | Hook no-op | No |
| `"non-interactive"` (explicit) | `"non-interactive"` | Hook active — nudge on stall | No |
| Any other value (typo) | `"interactive"` | Fallback safe — hook no-op | **Yes** |

## `refinementVerdict` Fallback Matrix

| Condition on `refinementVerdict` | `flowVersion` | Resolved | Behavior | Warning? |
| :--- | :--- | :--- | :--- | :--- |
| Field absent | `"1"` or absent | `{status: "tbd"}` | Hook no-op | No |
| Field absent | `"2"` or `"4"` | `{status: "tbd"}` | Hook blocks: `REFINEMENT_REQUIRED` (exit 33) | **Yes** |
| `status = "tbd"` | `"1"` | `"tbd"` | Hook no-op (legacy) | No |
| `status = "tbd"` | `"2"` or `"4"` | `"tbd"` | Hook blocks | **Yes** |
| `status = "rejected"` | any | `"rejected"` | Hook blocks | No |
| `status = "approved"` | any | `"approved"` | Gate passed | No |
| Any other value (typo) | any | `"tbd"` | Hook blocks | **Yes** |

Exception: `hotfix/*` branches → `enforce-refinement-gate.sh` is no-op regardless.

## `productFirstLifecycle` Fallback Matrix

| Condition on field | `flowVersion` | Resolved | Behavior | Warning? |
| :--- | :--- | :--- | :--- | :--- |
| Field absent | `"1"`–`"4"` | `false` | No-op | No |
| Field absent | `"5"` | `false` | Warn — v5 expects flag | **Yes** `[productFirstLifecycle-absent]` |
| `true` (explicit) | `"5"` | `true` | Product-First mode active | No |
| `false` (explicit) | `"5"` | `false` | Unusual — warn | **Yes** |
| Non-boolean | any | `false` | Schema error surfaced | **Yes** |

## Deprecation Window

| Phase | Duration | Behavior |
| :--- | :--- | :--- |
| **Window open** | 2 releases after EPIC-0049 merges | Both flows supported; missing `flowVersion` defaults to legacy with warning |
| **Window closing** | Start of 3rd release | Missing/unrecognized fails fast with `LEGACY_FLOW_UNSUPPORTED` |
| **Window closed** | After 3rd release | `--legacy-flow` removed; only `flowVersion: "2"` accepted |

## Skill Renaming

Old names remain in dispatch table with `DEPRECATED` warning for **one release** after rename, then removed. Exception: **hard-cut** authorized when rename represents fundamental semantic role change (visibility change, taxonomic merge, semantic redefinition). Hard-cuts skip deprecation window and must be documented under `## Removed` in CHANGELOG.

## Orphan Stories

Stories whose PR was merged into `develop` before the epic branch was introduced:
- `storyStatuses[storyId].flowVersion` set to `"1"` retroactively.
- Treated as complete (not subject to epic-branch routing).
- Epic's aggregate `flowVersion` remains `"2"` — mixed mode supported during window.
