# Implementation Plan — story-0077-0021

## Summary
Enforce minimum-10-category RNF gate in `x-create-product`. Two-step change: (1) extend `RNFRootValidator` with a total-count check; (2) wire the validator into `CreateProductOrchestrationUseCase` and surface the result through `XCreateProductCommand`.

## Tasks

### TASK-0077-0021-001 — Extend RNFRootValidator with total category count gate
- Add constant `MIN_TOTAL_CATEGORIES = 10`
- Add constant `ERROR_MSG = "Minimum 10 RNF categories required (6 mandatory + 4 optional minimum)"`
- Add `validateTotalCount` method; emit error when `product.rnfRoots().size() < 10`
- Unit test: failure with 9 categories, pass with 10+

### TASK-0077-0021-002 — Wire RNFRootValidator into CreateProductOrchestrationUseCase + CLI surfacing
- Inject `RNFRootValidator` into use case constructor
- After `transformer.transform(ideation)`, call `validator.validate(product)` 
- On failure, return `CreateProductResult.failure(rnfResult.errors())`
- In `XCreateProductCommand.call()`, after use case execution, print errors and return `EXIT_VALIDATION` if use case failed
- Integration test: use case returns failure for product with 8 categories
- CLI test: exit code 1 when ideation produces product with < 10 categories (mocked scenario)

## Implementation Order
Domain-first: TASK-001 (pure domain) → TASK-002 (application + CLI wiring).
