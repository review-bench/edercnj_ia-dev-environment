# Architecture Plan — story-0077-0001

**Story:** Rule 19 update + 5 Product-First capabilities + ADR  
**Epic:** EPIC-0077  
**Phase:** 1A — Architecture  
**Scope:** STANDARD

---

## §1 — Bounded Context

story-0077-0001 delivers the governance foundation for the Product-First Lifecycle:

1. **5 capability YAML files** in `capabilities/product-first/` — registers the 5 Product-First capabilities in the capability catalog (EPIC-0064 system).
2. **Product-First domain records** — `Product`, `ProductId`, `ProductStatus` in `domain/products/`; `Capability` wrapper and `CapabilityId` (product-first specific) in `domain/capabilities/`. Authorized by ADR-0030 / Rule 14 §Product-First Domain Extension.
3. **`capabilities/_index.yaml` update** — new `product-first` category entry.
4. **ADR-0031** — architecture decision record for the Product-First capability registration.

## §2 — Dependency Direction

```
domain/products/           ← zero external deps (Java standard only)
domain/capabilities/       ← zero external deps
capabilities/product-first/ ← YAML catalog (no Java dependency)
```

The domain packages depend only on the Java standard library, satisfying Rule 04 §Domain Purity.

## §3 — Package Layout

| Package | Classes | Layer |
| :--- | :--- | :--- |
| `domain/products/` | `Product`, `ProductId`, `ProductStatus` | domain |
| `domain/capabilities/` | `Capability`, `CapabilityId` | domain |
| `capabilities/product-first/` | 5 YAML capability files | catalog (non-Java) |

**Note:** `domain/capabilities/CapabilityId` in the Product-First context is a simple String wrapper (distinct from `domain/capability/CapabilityId` in EPIC-0064 which is a complex glob/pattern resolver). No package collision — different packages, different purposes.

## §4 — Architecture Decision

Product-First capabilities are registered in the same `capabilities/` catalog as EPIC-0064 capabilities (consistent with Rule 28 frontmatter contract). They do NOT use Java CapabilityId from EPIC-0064 — they define lightweight String-wrapped identity types for the planning hierarchy domain.

## §5 — Risk Assessment

| Risk | Probability | Mitigation |
| :--- | :--- | :--- |
| Package naming confusion `domain/capability` vs `domain/capabilities` | LOW | Clear Javadoc distinguishing composition-system vs planning-hierarchy |
| `_index.yaml` format mismatch | LOW | Follow existing entry format exactly |
| ADR-0031 numbering collision | NONE | ADR-0030 is highest; 0031 is free |
