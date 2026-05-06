---
name: x-create-capability
model: sonnet
description: "Decompose a Product into Capabilities with explicit RNF inheritance and no-relax markers."
user-invocable: true
allowed-tools: Read, Write, Bash
argument-hint: "--product-id <ID> [--capabilities '<JSON>'] [--auto-decompose] [--output-dir <PATH>]"
requires-capabilities: []
---

## Output Policy

- **Language**: Portuguese (pt-BR) for content. English for technical terms and code identifiers.
- **Tone**: Technical, Direct, and Concise.

# Skill: x-create-capability

## Purpose

Decompose a Product artifact into **Capability artifacts** with explicit RNF inheritance from the parent product. Each capability carries a `## Inherited RNFs` section with `no-relax` markers for non-negotiable constraints. Optionally generates a C2 stub (second capability) as a starting point for further decomposition.

Position in hierarchy: Ideation → Product → **Capability** → Feature → Epic → Story → Task.

## When to Use

- After `x-create-product` produces a `ai/products/product-NNNN.md`
- When decomposing a product into its capability areas (3–7 capabilities recommended)
- Before `x-create-feature`

## CLI Command

```bash
ia-dev-env x-create-capability --capability-id capability-c1 --capabilities '[{"name":"Auth","description":"..."}]'
ia-dev-env x-create-capability --capability-id capability-c1 --auto-decompose
```

## Triggers

- `/x-create-capability --capability-id <ID> --capabilities '<JSON>'`
- `/x-create-capability --capability-id <ID> --auto-decompose`

## Arguments

| Flag | Required | Description |
| :--- | :--- | :--- |
| `--capability-id` | yes | Capability identifier (e.g. `capability-c1`) |
| `--capabilities` | cond. | JSON array `[{"name":"...","description":"..."},...]` (4–8 entries) |
| `--auto-decompose` | cond. | Auto-decompose product into capabilities using heuristic |
| `--output-dir` | no | Output directory (default: `ai/capabilities/`) |

Either `--capabilities` or `--auto-decompose` is required.

## Output

Produces:
- `ai/capabilities/capability-NNNN.md` — Capability artifact with 7 sections + Inherited RNFs + no-relax markers
- Console summary with capability-id and inherited RNF count

## Exit Codes

| Code | Meaning |
| :--- | :--- |
| 0 | Success |
| 1 | Validation error |
| 2 | Execution error |

## Examples

```bash
# Explicit capabilities
ia-dev-env x-create-capability --capability-id capability-c1 \
  --capabilities '[{"name":"Authentication","description":"User auth flows"}]'

# Auto-decompose
ia-dev-env x-create-capability --capability-id capability-c1 --auto-decompose
```

## Related Skills

| Skill | Relationship |
| :--- | :--- |
| `x-create-product` | Produces the product artifact this skill reads |
| `x-create-feature` | Next step — decomposes a capability into features |
| `x-internal-validate-rnf` | Validates no-relax markers on capability RNFs |

## References

- [EPIC-0077 — Product-First Lifecycle](../../../../../../../../ai/epics/epic-0077-product-first-lifecycle/)
