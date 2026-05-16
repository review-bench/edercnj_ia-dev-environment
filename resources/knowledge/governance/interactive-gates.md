---
name: interactive-gates
description: Full interactive gates convention — 3-option menu (PROCEED/FIX-PR/ABORT), default non-interactive, affected skills
requires-capabilities: []
---
# Interactive Gates Convention — Full Reference

> **Introduced by:** EPIC-0043. **Amended by:** EPIC-0061.
> **ADR:** ADR-0010

## Purpose

Orchestrating skills expose decision gates at critical lifecycle points. A gate presents a 3-option menu (PROCEED / FIX-PR / ABORT).

**EPIC-0061 default flip:** The gate menu is **opt-in**, not opt-out. Invocations without `--interactive` skip all menus and proceed automatically. This eliminates "menu hang" in LLM sessions where no human is waiting.

## Default Behavior

| Flag | Behavior |
| :--- | :--- |
| *(no flag)* | Non-interactive — menus suppressed; proceed automatically |
| `--interactive` | Interactive — 3-option menu shown at each gate |
| `--non-interactive` | **DEPRECATED** — emits warn, behavior identical to default. Removed in 2 releases. |

**Env escape hatch:** `CLAUDE_LEGACY_INTERACTIVE=1` restores pre-EPIC-0061 behavior (interactive is default) for the deprecation window.

## Gate Menu (when `--interactive` is active)

```
┌─ Gate: <phase name> ───────────────────────────────────┐
│                                                         │
│  [1] PROCEED — merge/continue                          │
│  [2] FIX-PR  — invoke x-fix-pr / x-fix-epic-pr        │
│  [3] ABORT   — stop and preserve state                 │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

- FIX-PR slot invokes `x-fix-pr` (single PR) or `x-fix-epic-pr` (epic batch)
- After a fix cycle the menu loops back (max 3 consecutive FIX-PR cycles — `GATE_FIX_LOOP_EXCEEDED`)

## Deprecation Timeline

| Release | `--non-interactive` behavior |
| :--- | :--- |
| EPIC-0061 (current) | WARN + proceed (same as default) |
| Next release | WARN escalated to ERROR (build log only) |
| Release +2 | Flag removed; `ARGS_INVALID` if passed |

## Skills Affected

| Skill | Gate location |
| :--- | :--- |
| `x-implement-epic` | Phase 5 (final PR epic/XXXX → develop) |
| `x-implement-story` | Phase 2 (story-level PR after tasks) |
| `x-release` | Step 8 (release PR approval) |
| `x-review-pr` | After Tech-Lead GO/NO-GO verdict |

## Forbidden

- Invoking `AskUserQuestion` inside an orchestrator gate without `--interactive` in scope
- Treating absence of `--interactive` as an error — it is the valid default state
- Passing both `--interactive` and `--non-interactive` in the same invocation (`ARGS_INVALID`)
