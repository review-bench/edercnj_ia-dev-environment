# `ai/` — v4 AI Operations Root (EPIC-0060)

Top-level directory for AI-driven development artifacts in the v4 layout.

## Sub-directories

| Path | Purpose |
| :--- | :--- |
| `ai/epics/epic-XXXX-<slug>/` | Per-epic work (epic spec, stories, plans, reports, telemetry, state) — replaces `plans/epic-XXXX/` for `flowVersion >= 4` epics |
| `ai/runs/` | Per-run/session artifacts (transcripts, tool-call logs) |
| `ai/releases/` | Release-state JSON files — replaces `plans/release-state-*.json` for v4 releases |

## Coexistence with `plans/` (v3)

Per Rule 19 fallback matrix:

| `flowVersion` | Layout | Resolved by |
| :--- | :--- | :--- |
| `≤ 2` (or absent) | v3 — artifacts in `plans/epic-XXXX/` | Legacy `PathResolver` v3 fallback |
| `3` | (transitional, not used in production) | — |
| `4` | v4 — artifacts in `ai/epics/epic-XXXX-<slug>/` | `PathResolver` v4 probe |

`PathResolver.epicDir(epicId)` performs the probe automatically.

## Migration status (story-0060-0003)

This directory is currently the skeleton root. Population happens incrementally:

- New epics (`flowVersion: 4`) are created here directly.
- Existing v3 epics (`plans/epic-XXXX/`) stay in their current location until the operator runs `scripts/migrate-layout.sh --apply`.
- Story-0060-0006 freezes `plans/` for new writes after the 2-sprint observation window.

## Cross-references

- Migration script: `scripts/migrate-layout.sh`
- Layout spec: `specs/SPEC-folder-reorganization-v4.md`
- Epic: `plans/epic-0060/epic-0060.md`
