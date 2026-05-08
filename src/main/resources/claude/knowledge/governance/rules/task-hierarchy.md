---
name: task-hierarchy
description: Full task hierarchy and phase gate contract — TaskCreate/TaskUpdate protocol, invariants, enforcement, Anexo B orchestrators
requires-capabilities: []
---
# Task Hierarchy & Phase Gate Contract — Full Reference

> **Introduced by:** EPIC-0055. **ADR:** ADR-0014.
> **Full reference (BNF, examples, metadata schema, JSON shape):**
> `Read .claude/knowledge/lifecycle/task-hierarchy.md`

## Scope (8 canonical orchestrators — Anexo B)

`x-implement-epic`, `x-implement-story`, `x-implement-task`, `x-release`,
`x-orchestrate-epic`, `x-review-codebase`, `x-review-pr`, `x-manage-pr-merge-train`.

## Invariants

1. **`TaskCreate` per phase.** One `TaskCreate` on enter + `TaskUpdate(completed)` on exit.
2. **`TaskCreate` per wave member.** Batch A = emit, Batch B = complete.
3. **`TaskCreate` per sequential iteration.** Chain via `addBlockedBy`.
4. **Phase gates PRE/POST mandatory.** `x-internal-verify-phase-gates --mode pre` before phase, `post/wave/final` after. Exception: `<!-- phase-no-gate: <reason> -->`.
5. **`subject` hierarchy.** Separator is `›` (U+203A). Max depth: 4.
6. **Internal skills DO NOT emit tasks.** Caller owns the boundary.
7. **Gate failure aborts with exit 12.** (`PHASE_GATE_FAILED`)

## Enforcement Layers

| Layer | Mechanism | Trigger |
| :--- | :--- | :--- |
| 1 — Normative | This rule + CLAUDE.md | Every conversation |
| 2 — Stop hook | `verify-phase-gates.sh` | `Stop` event |
| 3 — PreToolUse | `enforce-phase-sequence.sh` | `PreToolUse` on `Skill(...)` |
| 4 — CI audit | `audit-task-hierarchy.sh` (exit 25) + `audit-phase-gates.sh` (exit 26) | PR to develop/epic |

## Forbidden

- `TaskCreate` from within an `x-internal-*` skill
- ASCII `>` or `-` instead of `›` as hierarchy separator
- `TodoWrite`/`TodoRead` alongside `TaskCreate` in same skill
- Adding entries to `governance/baselines/task-hierarchy-baseline.txt` after Rule 25 merges
