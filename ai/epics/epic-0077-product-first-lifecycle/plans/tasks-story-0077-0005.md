# Task Breakdown — story-0077-0005

## TASK-0077-0005-001: Template + Example

- **Branch:** `feat/task-0077-0005-001-product-template`
- **Files:** `ai/templates/_TEMPLATE-PRODUCT.md`, `ai/templates/rnf-categories.yaml`, `ai/examples/example-product-saas.md`
- **AC:** 8 sections present, 10 RNF categories in YAML, example fully filled

## TASK-0077-0005-002: Domain Model + Tests

- **Branch:** `feat/task-0077-0005-002-product-domain`
- **Files:** `domain/product/RNFCategory.java`, `domain/product/RNFRoot.java`, `domain/product/Product.java`, `domain/product/RNFRootValidator.java`, `test/domain/RNFRootValidatorTest.java`
- **AC:** 7 unit tests pass, ≥95% coverage, zero external imports in domain

## TASK-0077-0005-003: RNF Inheritance

- **Branch:** `feat/task-0077-0005-003-rnf-inheritance`
- **Files:** `domain/product/RNFInheritanceContext.java`, `application/product/ProductCreationUseCase.java`, `application/product/RNFInheritanceUseCase.java`, `test/application/RNFInheritanceUseCaseIT.java`
- **AC:** IT validates cascade inheritance, capability no-relax flag enforced

## TASK-0077-0005-004: Smoke + Golden

- **Branch:** `feat/task-0077-0005-004-product-smoke`
- **Files:** `ci/smoke/product-template-smoke.sh`
- **AC:** Smoke exit 0 on --self-check, PASS on example file
