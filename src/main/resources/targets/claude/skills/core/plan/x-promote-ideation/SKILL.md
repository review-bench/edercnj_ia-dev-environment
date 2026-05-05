---
name: x-promote-ideation
model: haiku
description: "Promote a transient x-ideate-feature output to a persistent ideation artifact in ai/ideations/."
user-invocable: true
allowed-tools: Read, Write, Bash
argument-hint: "--ideation-id <ideation-NNNN> [--source stdin|<PATH>] [--dry-run]"
requires-capabilities: []
---

## Output Policy

- **Language**: Portuguese (pt-BR) for content. English for technical terms and code identifiers.
- **Tone**: Technical, Direct, and Concise.

# Skill: x-promote-ideation

## Purpose

Promote the transient output of `x-ideate-feature` into a **persistent ideation artifact** stored at `ai/ideations/ideation-NNNN.md`. Transient ideations exist only in the conversation context; this skill persists them so they can be consumed by `x-create-product` in a subsequent session.

## When to Use

- After `x-ideate-feature` produces a spec that the user wants to keep
- Before `x-create-product` — the ideation must be persistent for the product creation step
- When archiving a feature idea for later processing

## CLI Command

```bash
ia-dev-env x-promote-ideation --ideation-id ideation-0001
ia-dev-env x-promote-ideation --ideation-id ideation-0002 --source ai/tmp/draft.md
ia-dev-env x-promote-ideation --ideation-id ideation-0001 --dry-run
```

## Triggers

- `/x-promote-ideation --ideation-id <ideation-NNNN>`
- `/x-promote-ideation --ideation-id <ideation-NNNN> --dry-run`

## Arguments

| Flag | Required | Description |
| :--- | :--- | :--- |
| `--ideation-id` | yes | Ideation identifier — must match `ideation-NNNN` (e.g. `ideation-0001`) |
| `--source` | no | Source file path or `stdin` (default: `stdin`) |
| `--dry-run` | no | Validate without persisting |

## Output

Produces:
- `ai/ideations/ideation-NNNN.md` — Persistent ideation artifact

## Exit Codes

| Code | Meaning |
| :--- | :--- |
| 0 | Success |
| 1 | Validation error (invalid ideation-id pattern, source not found) |
| 2 | Execution error |

## Examples

```bash
# Promote from stdin (pipe from x-ideate-feature output)
ia-dev-env x-promote-ideation --ideation-id ideation-0001

# Promote from file
ia-dev-env x-promote-ideation --ideation-id ideation-0002 --source ai/tmp/feature-draft.md

# Validate only
ia-dev-env x-promote-ideation --ideation-id ideation-0001 --dry-run
```

## Related Skills

| Skill | Relationship |
| :--- | :--- |
| `x-ideate-feature` | Produces the transient ideation this skill promotes |
| `x-create-product` | Consumes the persistent ideation produced by this skill |

## References

- [EPIC-0077 — Product-First Lifecycle](../../../../../../../../ai/epics/epic-0077-product-first-lifecycle/)
