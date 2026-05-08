# Task Breakdown — story-0077-0021

## Story
RNF Root table mandatory gate in x-create-product (gate falha sem 10 categorias)

## Tasks

### TASK-0077-0021-001
**Title:** Extend RNFRootValidator with minimum total category count gate
**Layer:** domain/product
**Estimate:** S
**Description:** Add total count validation (≥10) to `RNFRootValidator`. Canonical error string becomes a constant. Unit tests covering boundary (9 → fail, 10 → pass).

Files:
- `src/main/java/dev/iadev/domain/product/RNFRootValidator.java` (modify)
- `src/test/java/dev/iadev/domain/product/RNFRootValidatorTest.java` (new or modify)

### TASK-0077-0021-002
**Title:** Wire RNFRootValidator into use case and surface errors in CLI
**Layer:** application/product + adapter/inbound/cli
**Estimate:** S
**Depends on:** TASK-0077-0021-001
**Description:** Inject `RNFRootValidator` into `CreateProductOrchestrationUseCase`; invoke after transform; return failure on RNF violation. In `XCreateProductCommand`, print errors and return `EXIT_VALIDATION` when use case fails.

Files:
- `src/main/java/dev/iadev/application/product/CreateProductOrchestrationUseCase.java` (modify)
- `src/main/java/dev/iadev/adapter/inbound/cli/XCreateProductCommand.java` (modify)
- `src/test/java/dev/iadev/application/product/CreateProductOrchestrationUseCaseIT.java` (modify)
- `src/test/java/dev/iadev/adapter/inbound/cli/XCreateProductCommandTest.java` (modify)
