---
name: x-internal-build-epic-plan
description: "Builds the canonical ExecutionPlan for an epic (DAG, Kahn topo, optional parallelism)."
visibility: internal
user-invocable: false
allowed-tools: Bash, Skill
argument-hint: "--epic-id <XXXX> --mode <sequential|parallel> --output <path> [--strict-overlap]"
category: internal-plan
context-budget: medium
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL** — Invoked only by other skills (orchestrators) via the Skill tool. Not user-invocable. Caller: `x-implement-epic` (Phase 0 / 0.5 carve-out).

# Skill: x-internal-build-epic-plan (slim — ADR-0012)

## Purpose

Carve out the Phase 0 / 0.5 pre-flight of `x-implement-epic` — the ~250 inline lines that load epic metadata, stories, and dependency matrices, compute the phase ordering via Kahn's algorithm, optionally analyse file overlaps, and compute the critical path — into a single invocable skill with a stable JSON envelope. The orchestrator shrinks to a read-the-envelope consumer that then dispatches waves.

**Responsibilities (single):**

1. Load `ai/epics/epic-${epic_id}/epic-${epic_id}.md` and parse the story table.
2. Load `ai/epics/epic-${epic_id}/IMPLEMENTATION-MAP.md` and extract the inter-story dependency matrix.
3. For every `story-${epic_id}-NNNN.md`, cross-validate declared dependencies; detect missing story files.
4. Build the dependency DAG, run Kahn's algorithm; detect cycles (exit `3` `CYCLIC_DEPENDENCY`).
5. When `--mode parallel`: compute file-overlap matrix across every pair co-scheduled in the same Kahn phase. Advisory by default; `--strict-overlap` escalates hard collisions to a non-zero warning in the envelope (NOT a fatal error — Phase 1.5 parallelism gate remains the orchestrator's abort point via `x-evaluate-parallelism`).
6. Compute critical path (longest dependency chain).
7. Render `${output}` via `x-internal-write-report` using `_TEMPLATE-EPIC-EXECUTION-PLAN.md`.
8. Emit stable JSON envelope on stdout; translate every error to the exit-code catalogue below.

**Non-responsibilities (explicit):** does NOT mutate `execution-state.json`, `**Status:**` headers, or IMPLEMENTATION-MAP; does NOT run the Phase 1.5 collision gate (that is `x-evaluate-parallelism`); does NOT dispatch story implementation waves (that is `x-implement-epic`); does NOT re-read individual stories' planning artifacts (that is `x-internal-load-story-context`); does NOT write the story `**Status:**` header or trigger Jira transitions.

## Triggers

Bare-slash form intentionally omitted — invoked only by orchestrators via Rule 13 INLINE-SKILL:

```text
Skill(skill: "x-internal-build-epic-plan",
      args: "--epic-id XXXX --mode sequential --output ai/epics/epic-XXXX/epic-execution-plan.md")
```

## Parameters

| Parameter | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `--epic-id <id>` | M | — | 4-digit epic identifier (`XXXX`); zero-padded to 4 digits before use |
| `--mode <tier>` | M | — | `sequential` or `parallel`; case-insensitive; controls whether the overlap matrix is computed |
| `--output <path>` | M | — | Absolute or repo-relative path where the rendered markdown is written; must be writable |
| `--strict-overlap` | O | `false` | When `true` in parallel mode, annotates hard collisions in envelope with `overlapSeverity: "hard"`; does NOT abort — the caller's Phase 1.5 gate decides |

All four argument forms (`--key value`, `--key=value`, empty, unknown-flag rejection) follow the Rule-14 rejection matrix: reject unknown flags / missing required flags with exit `64` (sysexits `EX_USAGE`). `--epic-id` normalised to 4 digits; `--mode` to lowercase.

## Response Contract

Single-line JSON on stdout:

| Field | Type | Always Present | Description |
| :--- | :--- | :--- | :--- |
| `phases` | `Array<Phase>` | yes | Ordered list of Kahn phases, each `{ "index": <int>, "stories": [<storyId>,…] }` |
| `overlapMatrix` | `Object<storyId, Array<storyId>>` \| `null` | no — `null` when `mode=sequential` | Map from each story to the list of co-scheduled peers touching the same files |
| `overlapSeverity` | `String` \| `null` | no — `null` when `mode=sequential` | One of `"none"` / `"soft"` / `"regen"` / `"hard"`; mirrors `x-evaluate-parallelism` |
| `criticalPath` | `Array<storyId>` | yes | Longest dependency chain in the DAG; ties broken by lexicographic storyId |
| `planPath` | `String` | yes | Echo of `--output` after normalisation — also path of the rendered markdown |
| `mode` | `String` | yes | Echo of the resolved mode (`sequential` / `parallel`) |
| `epicId` | `String` | yes | Echo of the 4-digit epic id |
| `storyCount` | `Integer` | yes | Count of distinct stories discovered under `ai/epics/epic-${epic_id}/story-*.md` |
| `strictOverlap` | `Boolean` | yes | Echo of `--strict-overlap` |

The envelope shape is authoritative; callers must NOT assume field order. `overlapMatrix` and `overlapSeverity` are `null` (not absent) in sequential mode so consumers can use a single deserialisation schema.

## Exit Codes

| Code | Name | Condition |
| :--- | :--- | :--- |
| 0 | SUCCESS | All steps succeeded; envelope emitted |
| 1 | EPIC_NOT_FOUND | `ai/epics/epic-${epic_id}/` directory does not exist |
| 2 | MAP_NOT_FOUND | `IMPLEMENTATION-MAP.md` is missing |
| 3 | CYCLIC_DEPENDENCY | Kahn's algorithm detected a cycle in the DAG |
| 4 | STORY_FILE_MISSING | Dependency matrix references a story whose `.md` file is absent |
| 5 | REPORT_WRITE_FAILED | Downstream `x-internal-write-report` returned non-zero or did not produce `${output}` |
| 64 | EX_USAGE | Unknown flag, missing required flag, malformed `--epic-id` / `--mode`, or unwritable `--output` |

No partial-success state: the first non-recoverable error aborts the run and yields a non-zero exit. The `--strict-overlap` flag does NOT produce a non-zero exit; it only escalates `overlapSeverity` in the envelope.

## Workflow Overview

```text
1. PARSE          -> Argument parsing + path resolution (--epic-id 4-digit, --mode lowercase)
2. LOAD           -> Parse epic file story table + IMPLEMENTATION-MAP dependency matrix
                     + enumerate on-disk story files; cross-validate
3. KAHN + CYCLE   -> Topological sort; cycle detection (exit 3 on detect)
4. OVERLAP        -> [parallel mode only] Compute per-pair file-overlap matrix from
                     ## File Footprint blocks; classify per RULE-004 hotspot table
5. CRITICAL PATH  -> Longest-path in DAG; ties broken lexicographically
6. RENDER         -> Skill(x-internal-write-report) with _TEMPLATE-EPIC-EXECUTION-PLAN.md
7. ENVELOPE       -> jq -nc assembly + single-line stdout emission
```

## Workflow Detail

The full step-by-step protocol (argument-parser Rule-14 single-file `while (($#))` loop with rejection matrix; epic-file story-table parser; IMPLEMENTATION-MAP dependency parser; on-disk story enumeration; Kahn's algorithm with lexicographic seed ordering and cycle-trace emission; overlap-matrix computation with `## File Footprint` block parsing and `footprint-unknown` advisory per RULE-006; RULE-004 hotspot table for `hard/regen/soft/none` classification; longest-path DAG algorithm; `x-internal-write-report` stdin schema; envelope `jq -nc` assembly with explicit `null` for sequential mode; 6 concrete examples covering happy/cycle/missing/boundary) lives in [`references/full-protocol.md`](references/full-protocol.md). The slim contract above is sufficient for happy-path operation.

## Convention Anchors (x-internal-*)

| Aspect | Value |
| :--- | :--- |
| Path | `internal/plan/x-internal-build-epic-plan/` |
| Frontmatter `visibility` | `internal` |
| Frontmatter `user-invocable` | `false` |
| Body marker | `> 🔒 **INTERNAL SKILL**` block |
| Allowed tools | `Bash, Skill` |
| Naming | `x-internal-{subject}-{action}` |

Audit rule: Rule 22 (Lifecycle Integrity) validates every skill under `internal/**` satisfies all 6 anchors above. Violations fail `LifecycleIntegrityAuditTest`.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| Missing required flag | Print `usage:` banner to stderr; exit 64 |
| Unknown flag | Print `usage: unknown flag …` to stderr; exit 64 |
| Malformed `--epic-id` | `usage: --epic-id must be a 4-digit integer`; exit 64 |
| `--mode` unrecognised | `usage: --mode must be sequential / parallel`; exit 64 |
| `--output` path unwritable | `usage: --output path not writable`; exit 64 |
| Missing epic dir | `Epic dir not found: ai/epics/epic-<id>`; exit 1 |
| Missing epic file | `Epic file not found: ai/epics/epic-<id>/epic-<id>.md`; exit 1 |
| Missing IMPLEMENTATION-MAP | `IMPLEMENTATION-MAP.md missing under ai/epics/epic-<id>`; exit 2 |
| Cycle detected in DAG | `Cycle detected: <chain>`; exit 3 |
| Story-file referenced by map but absent | `Story file missing: <storyId>.md`; exit 4 |
| `x-internal-write-report` non-zero | `Report write failed: <child stderr line>`; exit 5 |
| `x-internal-write-report` success but `${output}` absent | `Report write failed: expected output not produced`; exit 5 |
| `jq` absent on PATH | Exit 127 with `jq is required`; abort before any file read |
| Story has no `## File Footprint` block (parallel mode) | Emit advisory in envelope `warnings`; include story in matrix with empty peer list; do NOT abort (EPIC-0041 RULE-006) |

The skill is fail-fast on structural errors (exit 1–4); reporting failures (exit 5) preserve the computed envelope on stderr so the caller can choose to retry just the render step.

## Performance Contract

Target: plan computation strictly under 5 seconds for epics up to 30 stories on a typical developer machine — Global DoD threshold from story-0049-0009 §4. Dominant cost is the file-system scan of `story-*.md` + footprint-block parsing; DAG processing is O(V + E) and negligible. Render step (`x-internal-write-report`) measured separately, typically 0.5–2 seconds for a 22-story epic.

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with `visibility: internal` from the `.claude/README.md` skill-inventory table, the `/help` menu, and user-facing autocomplete. Internal skills are still copied into `.claude/skills/` (flat layout) so `Skill(skill: "x-internal-…")` invocations resolve correctly. **User cannot see them; orchestrators can invoke them.**

## Telemetry

Internal skills DO NOT emit `phase.start` / `phase.end` markers — telemetry is produced by the invoking orchestrator. Passive hooks still capture `tool.call` for each underlying `Skill` / `Bash` invocation.

Reference: Rule 13 (Skill Invocation Protocol), Rule 22 (Lifecycle Integrity Audit), ADR-0010 (Interactive Gates Convention — exempts internal skills from the 3-option menu contract), ADR-0006 (File-Conflict-Aware Parallelism — defines RULE-004 hotspot table and RULE-006 footprint-unknown advisory).

## Integration Notes

| Skill | Relationship | Context |
| :--- | :--- | :--- |
| `x-implement-epic` | caller (primary) | Phase 0 / 0.5 carve-out: ~250 inline lines collapse to one `Skill(skill: "x-internal-build-epic-plan", …)` invocation |
| `x-internal-write-report` | delegate (Step 6) | Renders `epic-execution-plan.md` using `_TEMPLATE-EPIC-EXECUTION-PLAN.md` |
| `x-evaluate-parallelism` | downstream peer | Phase 1.5 of the caller consumes the `overlapMatrix` to compute per-wave collision classification (EPIC-0041) |
| `x-internal-update-status` | peer | Separate concern (mutates state); never invoked from this skill |
| `x-internal-load-story-context` | peer | Sibling read-only carve-out at the story level; never invoked from this skill |
| `x-internal-build-story-plan` | peer | Sibling story-level planning carve-out; never invoked from this skill |

## Full Protocol

Minimum viable contract above. Detailed workflow (Rule-14 argument parser with full rejection matrix; epic-file and IMPLEMENTATION-MAP parser grammars; Kahn's algorithm pseudocode with lexicographic determinism rule; cycle-trace minimal-walk algorithm; overlap-matrix per-pair computation with `## File Footprint` block parsing and `footprint-unknown` warnings; RULE-004 hotspot table; longest-path DAG algorithm; `x-internal-write-report` stdin schema; envelope `jq -nc` assembly with explicit `null` for sequential mode; 6 concrete examples covering happy/parallel/cycle/missing-dir/missing-story/single-story cases; performance budget; acceptance test scenarios) lives in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
