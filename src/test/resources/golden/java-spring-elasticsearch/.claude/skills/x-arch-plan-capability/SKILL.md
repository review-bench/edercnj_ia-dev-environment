---
name: x-arch-plan-capability
model: opus
description: "Generate C4 Container + Component diagrams for a capability in Mermaid or PlantUML."
user-invocable: true
allowed-tools: Read, Write, Bash
argument-hint: "--capability-id <ID> [--output-format mermaid|plantuml]"
context-budget: light
requires-capabilities: []
---

## Output Policy

- **Language**: Portuguese (pt-BR) for content. English for technical terms and code identifiers.
- **Tone**: Technical, Direct, and Concise.

# Skill: x-arch-plan-capability

## Purpose

Generate **C4 architecture diagrams** (Context, Container, Component levels) for a Capability artifact. Diagrams are written in Mermaid (default) or PlantUML. Placeholder diagrams are generated when structural information is sparse.

## When to Use

- After `x-create-capability` creates the capability artifact
- When planning architectural boundaries for a specific capability area
- Before feature decomposition — establishes technical context

## CLI Command

```bash
ia-dev-env x-arch-plan-capability --capability-id capability-0001
ia-dev-env x-arch-plan-capability --capability-id capability-0001 --output-format plantuml
```

## Triggers

- `/x-arch-plan-capability --capability-id <ID>`
- `/x-arch-plan-capability --capability-id <ID> --output-format plantuml`

## Arguments

| Flag | Required | Description |
| :--- | :--- | :--- |
| `--capability-id` | yes | Capability identifier (e.g. `capability-0001`) |
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
| 1 | Validation error |
| 2 | Execution error |

## Examples

```bash
ia-dev-env x-arch-plan-capability --capability-id capability-0001
ia-dev-env x-arch-plan-capability --capability-id capability-auth --output-format plantuml
```

## Related Skills

| Skill | Relationship |
| :--- | :--- |
| `x-arch-plan-product` | Product-level C4 — run before this |
| `x-arch-plan-feature` | Feature-level C4 — run after this |
| `x-create-capability` | Creates the capability artifact consumed here |

## References

- [EPIC-0077 — Product-First Lifecycle](../../../../../../../../ai/epics/epic-0077-product-first-lifecycle/)
