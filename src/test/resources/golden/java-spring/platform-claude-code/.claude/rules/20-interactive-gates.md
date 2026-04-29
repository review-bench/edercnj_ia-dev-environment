# Rule 20 — Interactive Gates Convention

> **Related:** Rule 13 (Skill Invocation Protocol), Rule 09 (Branching Model).
> **Introduced by:** EPIC-0043 (Interactive Gates Convention) — ADR-0010.
> **Amended by:** EPIC-0061 (Local-First Lifecycle) — default flip: non-interactive is now the default.

## Purpose

Orchestrating skills (`x-release`, `x-story-implement`, `x-epic-implement`, `x-review-pr`)
expose decision gates at critical lifecycle points (post-review, post-integrity-check,
pre-merge). A gate presents a 3-option menu (PROCEED / FIX-PR / ABORT) and routes the
operator's choice back into the orchestrator's flow.

**EPIC-0061 default flip (RULE-001):** As of this amendment, the gate menu is
**opt-in**, not opt-out. Invocations without `--interactive` skip all menus and execute
straight through to completion or fatal error. This eliminates "menu hang" in LLM sessions
where no human is waiting to answer a prompt.

## Default Behavior

| Flag | Behavior |
| :--- | :--- |
| *(no flag)* | Non-interactive — menus suppressed; proceed automatically on each gate |
| `--interactive` | Interactive — 3-option menu (PROCEED / FIX-PR / ABORT) shown at each gate |
| `--non-interactive` | **DEPRECATED** — emits warn, behavior identical to default. Removed in 2 releases. |

**Env escape hatch:** `CLAUDE_LEGACY_INTERACTIVE=1` restores the pre-EPIC-0061 behavior
(interactive is default) for the deprecation window (2 releases). Logs:
`interactive=true (legacy env)`.

## Gate Menu (when `--interactive` is active)

```
┌─ Gate: <phase name> ───────────────────────────────────┐
│                                                         │
│  [1] PROCEED — merge/continue                          │
│  [2] FIX-PR  — invoke x-pr-fix / x-pr-fix-epic        │
│  [3] ABORT   — stop and preserve state                 │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

- FIX-PR slot invokes `x-pr-fix` (single PR) or `x-pr-fix-epic` (epic batch) via Rule 13 INLINE-SKILL.
- After a fix cycle the menu loops back (max 3 consecutive FIX-PR cycles — `GATE_FIX_LOOP_EXCEEDED`).
- `--non-interactive` behavior now equals no-flag behavior: proceed automatically.

## Deprecation Timeline

| Release | `--non-interactive` behavior |
| :--- | :--- |
| EPIC-0061 (this release) | WARN + proceed (same as default) |
| Next release | WARN escalated to ERROR (build log only) |
| Release +2 | Flag removed; `ARGS_INVALID` if passed |

## Skills Affected

| Skill | Gate location |
| :--- | :--- |
| `x-epic-implement` | Phase 5 (final PR epic/XXXX → develop) |
| `x-story-implement` | Phase 2 (story-level PR after tasks) |
| `x-release` | Step 8 (release PR approval) |
| `x-review-pr` | After Tech-Lead GO/NO-GO verdict |

## Working-Tree Guard Integration

`x-internal-worktree-precheck` (EPIC-0061, RULE-010) provides a deterministic exit
code (`WORKTREE_AMBIGUOUS=15`) when the working tree is in an ambiguous state. In
non-interactive mode (default), orchestrators dispatch on this exit code and fail fast
instead of prompting. Use `--allow-dirty` to bypass in exceptional circumstances.

## Backward Compatibility

Pre-EPIC-0061 automations that pass `--non-interactive` continue to work without change
(deprecated but functional). No migration is required until the flag is removed.

`CLAUDE_LEGACY_INTERACTIVE=1` restores old default for scripts that relied on interactive
mode without an explicit flag — document the escape hatch in migration notes.

## Forbidden

- Invoking `AskUserQuestion` inside an orchestrator gate without `--interactive` in scope.
- Treating absence of `--interactive` as an error — it is the valid default state.
- Passing both `--interactive` and `--non-interactive` in the same invocation (`ARGS_INVALID`).
