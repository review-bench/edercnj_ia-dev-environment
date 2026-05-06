---
name: x-create-product
model: sonnet
description: "Transform an ideation file into a Product artifact with RNF roots and C1 capability stub."
user-invocable: true
allowed-tools: Read, Write, Bash
argument-hint: "--ideation-file <PATH> [--output-dir <PATH>] [--product-id <ID>] [--dry-run]"
requires-capabilities: []
---

## Output Policy

- **Language**: Portuguese (pt-BR) for content. English for technical terms and code identifiers.
- **Tone**: Technical, Direct, and Concise.

# Skill: x-create-product

## Purpose

Transform a validated ideation artifact (`_TEMPLATE-IDEATION.md`) into a **Product artifact** (`_PRODUCT.md`) with 8 sections, Root RNFs inherited from the ideation, and a stub for the first Capability (C1). This is the entry point of the Product-First Lifecycle hierarchy: Ideation → **Product** → Capability → Feature → Epic → Story → Task.

Execution is idempotent: re-running with the same `--product-id` regenerates the same product without creating duplicates.

## When to Use

- After `x-ideate-feature` produces and validates a transient ideation
- When converting a persistent ideation (`ai/ideations/ideation-NNNN.md`) into a Product
- First step before `x-create-capability`

## CLI Command

```bash
ia-dev-env x-create-product --ideation-file ai/ideations/ideation-0001.md
ia-dev-env x-create-product --ideation-file ai/ideations/ideation-0001.md --product-id product-0002
ia-dev-env x-create-product --ideation-file ai/ideations/ideation-0001.md --dry-run
```

## Triggers

- `/x-create-product --ideation-file <PATH>`
- `/x-create-product --ideation-file <PATH> --dry-run` — validate without writing files

## Arguments

| Flag | Required | Description |
| :--- | :--- | :--- |
| `--ideation-file` | yes | Path to the ideation markdown file |
| `--product-id` | no | Override product identifier (e.g. `product-0002`). Default: auto-numbered |
| `--output-dir` | no | Output directory (default: `ai/products/`) |
| `--dry-run` | no | Validate inputs without writing files |

## Output

Produces:
- `ai/products/product-NNNN.md` — Product artifact with 8 sections + RNF Root table + C1 stub
- Console summary with product-id, RNF count, and C1 stub path

## Exit Codes

| Code | Meaning |
| :--- | :--- |
| 0 | Success |
| 1 | Validation error (invalid ideation format, pattern mismatch) |
| 2 | Execution error (I/O failure) |

## Examples

```bash
# Create product from ideation
ia-dev-env x-create-product --ideation-file ai/ideations/ideation-0001.md

# Dry-run — validate only
ia-dev-env x-create-product --ideation-file ai/ideations/ideation-0001.md --dry-run

# Explicit product id
ia-dev-env x-create-product --ideation-file ai/ideations/ideation-0001.md --product-id product-0003
```

## Related Skills

| Skill | Relationship |
| :--- | :--- |
| `x-ideate-feature` | Produces the ideation input consumed by this skill |
| `x-promote-ideation` | Promotes transient ideation to persistent before this skill runs |
| `x-create-capability` | Next step — decomposes this product into capabilities |

## References

- [EPIC-0077 — Product-First Lifecycle](../../../../../../../../ai/epics/epic-0077-product-first-lifecycle/)
- [Rule 14 §Product-First Domain Extension](../../../rules/14-project-scope.md)
