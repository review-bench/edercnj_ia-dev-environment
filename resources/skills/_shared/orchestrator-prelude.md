# Orchestrator Prelude — Steps P1 + P2

> Canonical entry sequence for **every multi-phase orchestrator** (`x-orchestrate-epic`, `x-implement-epic`, `x-implement-story`, `x-create-feature`, `x-release`, etc.).
>
> Reference this file from a slim two-line section in the host SKILL.md instead of inlining the worktree-detect + epic-branch-ensure boilerplate. The exact bash and `Skill(...)` invocations below are normative — copy them verbatim, substituting only `<HOST-SKILL>` and the resolved `<EPIC-ID>`.

## Step P1 — Detect Worktree Context (EPIC-0049 / RULE-001)

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start <HOST-SKILL> Phase-P1-Worktree-Detect
```

Invoke `x-manage-worktrees` in detect-context mode. Result is **advisory only** — `x-internal-ensure-epic-branch` (Step P2) makes the authoritative decision.

```text
Skill(skill: "x-manage-worktrees", args: "detect-context")
```

Continue on any detect-context failure (**fail-open**, RULE-006).

```text
<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end <HOST-SKILL> Phase-P1-Worktree-Detect ok
```

## Step P2 — Ensure `epic/<ID>` Branch (EPIC-0049 / RULE-001)

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start <HOST-SKILL> Phase-P2-Epic-Branch-Ensure
```

Use the resolved `EPIC-ID` argument (4-digit zero-padded, e.g. `0042`).

Invoke `x-internal-ensure-epic-branch` so the canonical `epic/<ID>` branch exists locally AND on `origin` (idempotent):

```text
Skill(skill: "x-internal-ensure-epic-branch", args: "--epic-id <EPIC-ID>")
```

On non-zero exit, abort with `EPIC_BRANCH_ENSURE_FAILED` (see [`error-handling-orchestrator.md`](error-handling-orchestrator.md)).

When `--dry-run` is set, **skip this step**.

This step runs **once** at orchestrator entry. Child planning/implementation skills invoked downstream (e.g., `x-plan-story`, `x-implement-task`) MUST receive `--no-commit` so they do not re-ensure the branch nor issue per-step commits — wave-level commit aggregation is the orchestrator's responsibility.

```text
<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end <HOST-SKILL> Phase-P2-Epic-Branch-Ensure ok
```

## How to reference this file from a host orchestrator SKILL.md

Replace the two inline `Step P1` / `Step P2` blocks (≈ 30 lines combined) with:

```markdown
## Steps P1 + P2 — Worktree + Epic Branch Prelude

Apply the canonical [orchestrator prelude](../_shared/orchestrator-prelude.md) using
`<HOST-SKILL>` = `<this-skill-name>` and `<EPIC-ID>` = the resolved epic id argument.
```

Every orchestrator that delegates to this file gets identical lifecycle/telemetry behavior at zero per-skill maintenance cost. Drift across orchestrators is impossible because there is one source of truth.
