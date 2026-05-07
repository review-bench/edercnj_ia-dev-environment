---
name: model-selection
description: Full model selection strategy — tiered matrix (Opus/Sonnet/Haiku), enforcement points, Haiku eligibility, audit contract
requires-capabilities: []
---
# Model Selection Strategy — Full Reference

> **Introduced by:** EPIC-0050

## Purpose

Every skill/agent MUST declare `model:` explicitly at the point of invocation. Implicit inheritance cascades premium cost. Target: ≤50% Opus / ≥35% Sonnet / ≥12% Haiku.

## Matrix

| Layer | Default Tier | Examples | Justification |
| :--- | :--- | :--- | :--- |
| Orchestrator | `sonnet` | `x-implement-epic`, `x-implement-story`, `x-release` | Dispatches other skills; no deep design reasoning inline |
| Deep Planner | `opus` | `x-plan-architecture`, Architect subagent | Architecture plans, ADR content, trade-off analysis |
| Reviewer / Validator | `sonnet` | `x-review-qa`, `x-review-pr`, reviewer subagents | Applies checklists; structured reasoning suffices |
| Executor | `sonnet` | `x-implement-task`, `x-drive-tdd` | TDD cycle is procedural |
| Utility | `haiku` | `x-manage-worktrees`, `x-commit-changes`, `x-format-code` | Git ops, formatting, linting — zero design reasoning |
| Knowledge Pack (KP) | `haiku` | `architecture`, `coding-standards`, `testing` | Read-only reference; no reasoning performed by the KP |

## Enforcement Points

### 1. Frontmatter YAML on SKILL.md

```yaml
---
name: x-implement-epic
model: sonnet
---
```

Every classified skill MUST declare `model:` in frontmatter.

### 2. `Agent(...)` with explicit `model:`

```text
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "...",
  prompt: "..."
)
```

Every `Agent(subagent_type: "general-purpose", ...)` MUST pass `model:` explicitly.

### 3. `Skill(...)` with explicit `model:`

```text
Skill(
  skill: "x-implement-story",
  model: "sonnet",
  args: "..."
)
```

Every `Skill(skill: "x-...", ...)` inside an orchestrator MUST pass `model:` when callee tier differs.

## Agent Metadata Contract

Every `.claude/agents/*.md` MUST declare:

```markdown
**Recommended Model:** Opus
```

Allowed values: `Opus`, `Sonnet`, `Haiku`. Value `Adaptive` is **forbidden**.

| Agent role | Recommended Model |
| :--- | :--- |
| `architect` | Opus |
| `product-owner` | Sonnet |
| `qa`, `security`, `sre`, `performance`, `tech-lead`, `devops` | Sonnet |

## Haiku Eligibility Criteria

A skill is eligible for `model: haiku` if it satisfies at least one of:

- **(a)** Utility without design reasoning — git operations, formatting, linting, status-file mutations
- **(b)** Read-only knowledge pack — consumed as reference context

Initial eligibility list: `x-manage-worktrees`, `x-commit-changes`, `x-format-code`, `x-lint-code`, `architecture`, `coding-standards`, `testing`, `layer-templates`, `patterns`, `dockerfile`.

## Audit Contract

CI runs `scripts/audit-model-selection.sh`. Fails when:

- **(a)** An orchestrator SKILL.md without `model:` in frontmatter
- **(b)** An `Agent(subagent_type: "general-purpose", ...)` block missing `model:`
- **(c)** A `Skill(skill: "x-...", ...)` call missing `model:` when callee tier differs
- **(d)** An agent file with `Recommended Model: Adaptive` or missing declaration

## Exceptions

- **User-invoked entry-point skills** MAY omit `model:` — the user's session tier applies
- **Internal skills (`x-internal-*`)** MAY omit `model:` when the parent guarantees the tier
- **Experimental skills** under `.claude/skills/experimental/` are out of scope
