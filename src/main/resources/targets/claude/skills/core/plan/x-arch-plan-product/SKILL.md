---
name: x-arch-plan-product
model: opus
description: "Generate C4 Context + Container + Component diagrams for a product in Mermaid or PlantUML."
user-invocable: true
allowed-tools: Read, Write, Bash
argument-hint: "--product-id <ID> [--output-format mermaid|plantuml]"
requires-capabilities: []
---

## Output Policy

- **Language**: Portuguese (pt-BR) for content. English for technical terms and code identifiers.
- **Tone**: Technical, Direct, and Concise.

# Skill: x-arch-plan-product

## Purpose

Generate **C4 architecture diagrams** (Context, Container, Component levels) for a Product artifact. Diagrams are written in Mermaid (default) or PlantUML. Placeholder diagrams are generated when the product is new and structural information is sparse; they are marked `[placeholder]` in the output summary.

## When to Use

- After `x-create-product` creates the product artifact
- When the tech lead or architect needs a C4 overview of the product
- Before story planning sessions that require architectural context

## CLI Command

```bash
ia-dev-env x-arch-plan-product --product-id product-0001
ia-dev-env x-arch-plan-product --product-id product-0001 --output-format plantuml
```

## Triggers

- `/x-arch-plan-product --product-id <ID>`
- `/x-arch-plan-product --product-id <ID> --output-format plantuml`

## Arguments

| Flag | Required | Description |
| :--- | :--- | :--- |
| `--product-id` | yes | Product identifier (e.g. `product-0001`) |
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
| 1 | Validation error (invalid product-id, unknown format) |
| 2 | Execution error |

## Examples

```bash
# Mermaid (default)
ia-dev-env x-arch-plan-product --product-id product-0001

# PlantUML
ia-dev-env x-arch-plan-product --product-id product-0001 --output-format plantuml
```

## Related Skills

| Skill | Relationship |
| :--- | :--- |
| `x-arch-plan-capability` | Same for capability level |
| `x-arch-plan-feature` | Same for feature level |
| `x-plan-architecture` | Full architecture planning skill (story-level) |

## References

- [EPIC-0077 — Product-First Lifecycle](../../../../../../../../ai/epics/epic-0077-product-first-lifecycle/)
- [story-0077-0013 — x-arch-plan C4 mandatory](../../../../../../../../ai/epics/epic-0077-product-first-lifecycle/story-0077-0013.md)
