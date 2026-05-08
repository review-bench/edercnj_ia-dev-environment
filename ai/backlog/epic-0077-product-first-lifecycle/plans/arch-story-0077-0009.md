# Architecture Plan — story-0077-0009

## Summary

Implement `x-create-product` CLI skill: reads an ideation markdown, validates it,
generates a product ID, creates `_PRODUCT.md` with 12+ RNFs Root and a C1 capability stub.

## Hexagonal Placement

| Class | Layer |
| :--- | :--- |
| `XCreateProductCommand` | adapter/inbound/cli |
| `XCreateProductArgumentParser` | adapter/inbound/cli |
| `CreateProductOrchestrationUseCase` | application/product |
| `IdeationToProductTransformer` | domain/ideation |
| `CapabilityStubFactory` | domain/capability (new subpkg) |
| `ProductArtifactWriter` | adapter/outbound/file |
| `CapabilityStubWriter` | adapter/outbound/file |
| `IdempotencyHash` | domain/product |

## Dependency Direction

```
XCreateProductCommand → CreateProductOrchestrationUseCase
  → IdeationToProductTransformer (domain)
  → CapabilityStubFactory (domain)
  → ProductArtifactWriter (outbound port impl)
  → CapabilityStubWriter (outbound port impl)
```

## Idempotency Strategy

SHA-256 of canonical ideation content → stored in `_PRODUCT.md` header.
On rerun: if hash matches, skip write. If mismatch (updated ideation), overwrite.

## Key Decisions

- Reuse `IdeationValidator` (story-0077-0004) for validation before transformation
- Reuse `ProductNumbering` (story-0077-0002) for sequential product ID assignment
- `_PRODUCT.md` format: 8 mandatory sections mirroring `_TEMPLATE-PRODUCT.md`
