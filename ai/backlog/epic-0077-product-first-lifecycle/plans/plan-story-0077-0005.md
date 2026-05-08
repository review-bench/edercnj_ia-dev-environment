# Implementation Plan — story-0077-0005

**Story:** _TEMPLATE-PRODUCT.md (8 seções, RNFs Root)

## Tasks

| Task | Description | Dependencies |
| :--- | :--- | :--- |
| TASK-0077-0005-001 | Template _TEMPLATE-PRODUCT.md + rnf-categories.yaml + example | none |
| TASK-0077-0005-002 | Domain model: Product, RNFRoot, RNFCategory, RNFRootValidator + tests | TASK-001 |
| TASK-0077-0005-003 | RNF inheritance: RNFInheritanceUseCase + RNFInheritanceContext + IT | TASK-002 |
| TASK-0077-0005-004 | Smoke test script + golden fixtures | TASK-003 |

## Implementation Order

1. Create template with 8 sections + 10 RNF categories YAML reference
2. Create example product (SaaS B2B) with all 8 sections filled
3. TDD domain: RNFCategory enum → RNFRoot record → Product entity → RNFRootValidator
4. Application layer: ProductCreationUseCase + RNFInheritanceUseCase
5. Integration test: RNF inheritance cascade (product → capability → feature)
6. Smoke script: validates template structure + 6 mandatory RNF categories in example

## Coverage Target

- Line: ≥95% for new domain/application classes
- Branch: ≥90%
