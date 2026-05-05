# Architecture Plan — story-0077-0021

## Story
RNF Root table mandatory gate in x-create-product: reject products with < 10 RNF categories (6 mandatory + 4 optional minimum).

## Scope: SIMPLE

## Hexagonal Layers Touched

| Layer | Component | Change |
|-------|-----------|--------|
| domain/product | `RNFRootValidator` | Add total-count gate (≥10) with canonical error message |
| application/product | `CreateProductOrchestrationUseCase` | Inject `RNFRootValidator`; invoke after transform; return failure on violation |
| adapter/inbound/cli | `XCreateProductCommand` | Surface `RNFRootValidationResult.errors()` when use case returns failure |

## Dependency Direction (unchanged)
```
XCreateProductCommand → CreateProductOrchestrationUseCase → RNFRootValidator
                                                          → IdeationValidator
                                                          → IdeationToProductTransformer
```

Domain (`RNFRootValidator`) has zero outbound dependencies — pure validation logic.

## Decision Rationale

| Decision | Chosen | Rejected | Why |
|----------|--------|----------|-----|
| Where to enforce gate | `CreateProductOrchestrationUseCase` after transform | Transformer itself | Use case owns orchestration; domain transformer must be pure |
| Error format | Single canonical string in `RNFRootValidator` constant | Ad-hoc string in use case | Message is a domain invariant — owns its own text |
| Validation call order | After ideation validation, after transform | Before ideation validation | Cannot validate RNF roots before product exists |

## File Footprint

write:
- `src/main/java/dev/iadev/domain/product/RNFRootValidator.java`
- `src/main/java/dev/iadev/application/product/CreateProductOrchestrationUseCase.java`
- `src/main/java/dev/iadev/adapter/inbound/cli/XCreateProductCommand.java`
- `src/test/java/dev/iadev/domain/product/RNFRootValidatorTest.java`
- `src/test/java/dev/iadev/application/product/CreateProductOrchestrationUseCaseIT.java`
- `src/test/java/dev/iadev/adapter/inbound/cli/XCreateProductCommandTest.java`

read:
- `src/main/java/dev/iadev/domain/product/RNFCategory.java`
- `src/main/java/dev/iadev/domain/product/RNFRootValidationResult.java`
- `src/main/java/dev/iadev/application/product/CreateProductResult.java`
