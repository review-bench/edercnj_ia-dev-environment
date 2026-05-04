# Product Template — product-0000

> **NOTE:** `product-0000` is a RESERVED template directory. It is never assigned to a real product.
> Use it as a reference for the expected structure of a `product-NNNN/` directory.

---

## Product Directory Structure

Each product directory follows this layout:

```
product-NNNN/
├── _PRODUCT.md          # This file — product metadata and index
├── capabilities/        # Capability definitions for this product
│   └── *.md
├── features/            # Feature decompositions
│   └── *.md
└── stories/             # Story artifacts linked to this product
    └── *.md
```

## _PRODUCT.md Template Fields

When creating a real `product-NNNN/_PRODUCT.md`, include:

```markdown
# Product: <name>

**ID:** product-NNNN
**Status:** DRAFT | ACTIVE | DEPRECATED
**Epic Origin:** EPIC-XXXX
**Created At:** YYYY-MM-DDTHH:MM:SSZ
**Capabilities:** [capability-id-1, capability-id-2]
```

## Numbering

Products are numbered globally (not per-epic): `product-0001`, `product-0002`, etc.
The sequence is tracked by `ProductNumbering` in `domain/products/`.
