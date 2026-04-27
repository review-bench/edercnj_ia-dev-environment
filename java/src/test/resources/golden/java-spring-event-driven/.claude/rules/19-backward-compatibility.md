# Rule 19 — Backward Compatibility

> **Related:** Rule 21 (Epic Branch Model), Rule 22 (Skill Visibility), Rule 08 (Release Process).
> **Introduced by:** EPIC-0049 (Refatoração do Fluxo de Épico) — RULE-008 (`flowVersion` + `--legacy-flow`).
> **Extended by:** EPIC-0060 (Folder Reorganization v4) — `flowVersion: "4"` for v4 layout (`ai/epics/`).

## Purpose

When the epic / story / task workflow changes its shape (new default behaviors, renamed skills, new required fields in `execution-state.json`), in-flight epics MUST continue to complete successfully under their original semantics. Rule 19 defines the deprecation window, the discriminator field, and the fallback matrix that keep legacy epics working while the new flow rolls out.

## `flowVersion` Field

Every `execution-state.json` produced by the orchestrators carries a top-level discriminator:

```json
{
  "flowVersion": "2",
  "epicId": "EPIC-0049",
  "storyStatuses": { ... }
}
```

| Version | Meaning |
| :--- | :--- |
| `"1"` | Legacy flow — story PRs target `develop`, no `epic/XXXX` branch, auto-merge to `develop` per EPIC-0042 default. |
| `"2"` | New flow (EPIC-0049+) — story PRs target `epic/XXXX`, manual gate to `develop`, sequential default. v3 layout (`plans/`). |
| `"4"` | New layout (EPIC-0060+) — same flow as `"2"` but artifacts live under `ai/epics/<epic>-<slug>/` (v4 layout). PathResolver auto-detects via filesystem probe. |

## Fallback Matrix

Whenever an orchestrator reads `execution-state.json` (or creates a new one during resume), it applies this matrix to resolve the effective `flowVersion`:

| Condition on field | Resolved value | Behavior | Warning? |
| :--- | :--- | :--- | :--- |
| Field absent (legacy state file pre-EPIC-0049) | `"1"` | Legacy flow | **Yes** — visible warning |
| Field = `"1"` (explicit) | `"1"` | Legacy flow | No |
| Field = `"2"` (explicit) | `"2"` | New flow, v3 layout (`plans/`) | No |
| Field = `"4"` (explicit) | `"4"` | New flow, v4 layout (`ai/epics/`) — paths resolved via `PathResolver` | No |
| Field = any other value (typo, `"3"`, future-version `"5"`, etc.) | `"1"` | Legacy flow + warning | **Yes** — visible warning |

**Warning format:**

```
WARN [flowVersion-fallback] execution-state.json has flowVersion=<value>;
     defaulting to legacy flow (v1). To opt into the new flow, set flowVersion="2"
     explicitly, or delete the state file and re-run with --no-legacy-flow.
```

## `--legacy-flow` Flag

Orchestrators (`x-epic-implement`, `x-story-implement`, `x-epic-orchestrate`) accept `--legacy-flow` as an explicit opt-in to legacy mode:

- Forces `flowVersion: "1"` on new state files regardless of defaults.
- Overrides any explicit `flowVersion: "2"` in an existing state file (emits warning).
- Resets target branches to `develop` (disables Rule 21 epic-branch routing).

Used when:

- An operator needs to complete an in-flight legacy epic without re-planning it.
- A regression is discovered in the new flow and a quick rollback per-invocation is needed.
- CI is pinned to legacy behavior during the deprecation window.

## Deprecation Window

| Phase | Duration | Behavior |
| :--- | :--- | :--- |
| **Window open** | 2 releases after EPIC-0049 merges into `main` | Both flows are supported. Missing `flowVersion` defaults to legacy with warning. |
| **Window closing** | At the start of the 3rd release | Missing or unrecognized `flowVersion` fails fast with `LEGACY_FLOW_UNSUPPORTED`; operators must add `flowVersion: "1"` + `--legacy-flow` explicitly. |
| **Window closed** | After the 3rd release | `--legacy-flow` flag is removed. Only `flowVersion: "2"` is accepted. Legacy state files MUST be migrated via `x-internal-epic-migrate-flow` (future epic). |

Release counting: each tagged release on `main` that includes EPIC-0049 or a successor counts. Hotfixes (`hotfix/*`) do not advance the counter.

## Orphan Stories

Stories whose PR has already been merged into `develop` before the epic branch was introduced are treated as legacy:

- Their `storyStatuses[storyId].flowVersion` field is set to `"1"` retroactively.
- `x-status-reconcile` treats them as complete (not subject to epic-branch routing).
- The epic's aggregate `flowVersion` remains `"2"` even when individual stories are `"1"` — mixed mode is supported during the window.

## Skill Renaming

When a skill is renamed (e.g., EPIC-0036 taxonomy refactor), both names MUST continue to resolve for **one release** after the rename:

- The old name remains in the dispatch table with a `DEPRECATED` warning.
- Any skill invocation via the old name emits a one-time warning directing the user / orchestrator to the new name.
- After one release, the old name is removed. Documentation, CHANGELOG, and `/help` update simultaneously.

## Field Additions to `execution-state.json`

New fields added to `execution-state.json` (e.g., `parallelismDowngrades` in EPIC-0041, `flowVersion` in EPIC-0049) MUST:

1. Be **optional** — absence is interpreted as the legacy default (empty list, absent enum value, etc.).
2. Be documented in the companion ADR and in this rule's fallback matrix.
3. Have a fallback entry defined here before any orchestrator reads them in production.

### `taskTracking` Field (EPIC-0055 / EPIC-0059)

Added by EPIC-0055 (Rule 25 — Task Hierarchy & Phase Gate Enforcement). Controls whether orchestrators emit `TaskCreate`/`TaskUpdate` calls and invoke `x-internal-phase-gate`.

**EPIC-0059 enforcement:** `flowVersion=2` now requires `taskTracking.enabled=true`. Absence of `taskTracking` on a `flowVersion=2` state file is a `TASK_TRACKING_REQUIRED` error — no silent no-op. Run `scripts/migrate-task-tracking-v2.sh` before enabling `audit-flow-version.sh` to migrate all active epics.

#### Fallback Matrix (updated by EPIC-0059 — no deprecation window)

| Condition on `taskTracking` | `flowVersion` | Resolved behavior | Warning? |
| :--- | :--- | :--- | :--- |
| Field absent | `"1"` or absent | `enabled=false` — tracking disabled (legacy default) | **Yes** — visible warning |
| Field absent | `"2"` | **FAIL: `TASK_TRACKING_REQUIRED`** — enforcement immediate (EPIC-0059) | N/A — hard fail |
| `taskTracking.enabled = false` (explicit) | `"1"` or absent | Tracking skipped, gates are no-ops (legacy opt-out) | No |
| `taskTracking.enabled = false` (explicit) | `"2"` | Tracking skipped — **WARN: suspicious for flowVersion=2** (first release: WARN; second release: FAIL) | **Yes** — visible warning |
| `taskTracking.enabled = true` (explicit) | any | Full tracking active — `TaskCreate`/`TaskUpdate` emitted, gates enforced | No |

**Error code:** `TASK_TRACKING_REQUIRED` — emitted by `scripts/audit-flow-version.sh` (exit 1) when `flowVersion=2` and `taskTracking` is absent or `enabled=false`.

**WARN format (enabled=false + flowVersion=2):**

```
WARN [taskTracking-flowVersion2] execution-state.json has flowVersion=2 but taskTracking.enabled=false;
     this is suspicious — all flowVersion=2 epics should have full tracking active.
     Run scripts/migrate-task-tracking-v2.sh to migrate, or set taskTracking.enabled=true explicitly.
```

**Opt-out:** To preserve pre-EPIC-0055 behavior on a specific epic, use `flowVersion=1` with `--legacy-flow`. Setting `taskTracking.enabled=false` on a `flowVersion=2` epic is permitted in the first release (WARN only) but will become a hard fail in the second release after EPIC-0059.

**Migration:** Run `scripts/migrate-task-tracking-v2.sh` to migrate all active `flowVersion=2` epics to `taskTracking.enabled=true` before activating the CI audit. The script is idempotent — re-execution is safe.

## Forbidden

- Removing `flowVersion` resolution logic from orchestrators during the deprecation window.
- Silently upgrading `flowVersion: "1"` to `"2"` mid-execution (breaks resume; forces re-plan).
- Shipping a new required field in `execution-state.json` without an entry in the Fallback Matrix above.
- Removing a renamed skill's old name in the same release as the rename.

## Audit

CI script `scripts/audit-flow-version.sh` checks every `execution-state.json` under `plans/epic-*/`:

- Field `flowVersion` present and in `{"1", "2"}`.
- If the enclosing epic uses Rule 21 (`epic/XXXX` branch exists on the remote), `flowVersion` MUST be `"2"` unless `--legacy-flow` was recorded in the epic's metadata.
- **EPIC-0059 addition:** If `flowVersion=2`, `taskTracking.enabled` MUST be `true`. Absence or `false` → exit 1 `FLOW_VERSION_VIOLATION` with message `taskTracking required for flowVersion=2`.
- `--self-check`: validates `jq` on PATH and existence of `plans/` directory.

**Pre-requisite:** Run `scripts/migrate-task-tracking-v2.sh` before activating this check in CI. The migration script adds `taskTracking.enabled=true` to all `flowVersion=2` state files that are missing the field.

Violations fail the CI build with `FLOW_VERSION_VIOLATION`.

---

> **Catalogado em:** [`docs/audit-gates-catalog.md`](../../docs/audit-gates-catalog.md)

