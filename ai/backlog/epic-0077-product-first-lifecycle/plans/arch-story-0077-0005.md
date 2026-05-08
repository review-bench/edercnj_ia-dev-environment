# Architecture Plan — story-0077-0005

**Story:** _TEMPLATE-PRODUCT.md (8 seções, RNFs Root)  
**Date:** 2026-05-04

---

## Decision

Introduce `dev.iadev.domain.product` package with immutable `Product` entity, `RNFRoot` value object, `RNFCategory` enum (10 mandatory categories), and `RNFRootValidator`. Application layer gets `ProductCreationUseCase` and `RNFInheritanceUseCase`.

## New Packages

```
dev.iadev.domain.product/
  Product.java              — Entity with id, name, rnfRoots, constraints
  RNFRoot.java              — VO: category, description, measurable target, mandatory flag
  RNFCategory.java          — Enum: 10 mandatory categories
  RNFRootValidator.java     — Pure validator: enforces 6+ mandatory categories present

dev.iadev.application.product/
  ProductCreationUseCase.java   — thin orchestrator
  RNFInheritanceUseCase.java    — computes effective RNFs for a capability
  RNFInheritanceContext.java    — VO carrying product + capability overrides
```

## Key Design Decisions

1. `RNFRoot` is immutable (Java record) — category + description + target + mandatory flag
2. `Product` is a `final class` with defensive copy of `List<RNFRoot>`
3. `RNFCategory` enum enforces exactly 10 categories; validator checks mandatory ones
4. 6 mandatory categories: PERFORMANCE, SCALABILITY, RELIABILITY, SECURITY, COMPLIANCE, OBSERVABILITY
5. `RNFInheritanceContext` records capability's `noRelax` flags per category

## File Footprint

```
write:
  - ai/templates/_TEMPLATE-PRODUCT.md
  - ai/templates/rnf-categories.yaml
  - ai/examples/example-product-saas.md
  - src/main/java/dev/iadev/domain/product/Product.java
  - src/main/java/dev/iadev/domain/product/RNFRoot.java
  - src/main/java/dev/iadev/domain/product/RNFCategory.java
  - src/main/java/dev/iadev/domain/product/RNFRootValidator.java
  - src/main/java/dev/iadev/application/product/ProductCreationUseCase.java
  - src/main/java/dev/iadev/application/product/RNFInheritanceUseCase.java
  - src/main/java/dev/iadev/domain/product/RNFInheritanceContext.java
  - src/test/java/dev/iadev/domain/product/RNFRootValidatorTest.java
  - src/test/java/dev/iadev/application/product/RNFInheritanceUseCaseIT.java
  - ci/smoke/product-template-smoke.sh
read:
  - ai/templates/_TEMPLATE-IDEATION.md
```
