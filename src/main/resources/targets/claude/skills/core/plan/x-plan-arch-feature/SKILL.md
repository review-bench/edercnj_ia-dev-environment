---
name: x-plan-arch-feature
model: opus
description: "Generate C4 Context + Container diagrams for a feature in Mermaid or PlantUML."
user-invocable: true
allowed-tools: Read, Write, Bash
argument-hint: "--feature-id <ID> [--output-format mermaid|plantuml]"
requires-capabilities: []
---

## Output Policy

- **Language**: Portuguese (pt-BR) for content. English for technical terms and code identifiers.
- **Tone**: Technical, Direct, and Concise.

# Skill: x-plan-arch-feature

## Purpose

Generate **C4 architecture diagrams** (Context, Container, Component levels) for a Feature artifact. Diagrams are written in Mermaid (default) or PlantUML. This skill operates at the feature level — below capability, above epic.

## When to Use

- After `x-create-feature` creates a feature artifact
- When the architect needs a C4 view scoped to a single feature before story breakdown
- To document the architectural boundary of an `x-ideate-feature` output

## CLI Command

```bash
ia-dev-env x-plan-arch-feature --feature-id feature-oauth2
ia-dev-env x-plan-arch-feature --feature-id feature-0001 --output-format plantuml
```

## Triggers

- `/x-plan-arch-feature --feature-id <ID>`
- `/x-plan-arch-feature --feature-id <ID> --output-format plantuml`

## Arguments

| Flag | Required | Description |
| :--- | :--- | :--- |
| `--feature-id` | yes | Feature identifier (e.g. `feature-oauth2` or `oauth2-integration`) |
| `--output-format` | no | `mermaid` (default) or `plantuml` |

## Output

Produces C4 diagram blocks printed to stdout:
- `CONTEXT` — System context diagram
- `CONTAINER` — Container diagram
- `COMPONENT` — Component diagram

Each entry is marked `OK` or `[placeholder]`.

## Exit Codes

| Code | Meaning |
| :--- | :--- |
| 0 | Success |
| 1 | Validation error (feature-id not found or format unknown) |
| 2 | Execution error |

## Examples

```bash
ia-dev-env x-plan-arch-feature --feature-id feature-oauth2
ia-dev-env x-plan-arch-feature --feature-id feature-0001 --output-format plantuml
```

## Related Skills

| Skill | Relationship |
| :--- | :--- |
| `x-plan-arch-capability` | Parent level — run before this |
| `x-create-feature` | Creates the feature artifact consumed here |
| `x-plan-architecture` | Story-level arch planning — runs after feature decomposition |

## References

- [EPIC-0077 — Product-First Lifecycle](../../../../../../../../ai/epics/epic-0077-product-first-lifecycle/)
- [story-0077-0026 — x-arch-plan com Feature input](../../../../../../../../ai/epics/epic-0077-product-first-lifecycle/story-0077-0026.md)
