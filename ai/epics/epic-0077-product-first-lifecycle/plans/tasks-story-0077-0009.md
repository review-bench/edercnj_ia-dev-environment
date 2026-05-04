# Task Breakdown — story-0077-0009

## TASK-0077-0009-001: CLI command + argument parsing
- `XCreateProductCommand.java`, `XCreateProductArgumentParser.java`
- Tests: happy path, invalid args, help text

## TASK-0077-0009-002: Orchestration + transformation
- `CreateProductOrchestrationUseCase.java`, `IdeationToProductTransformer.java`, `CapabilityStubFactory.java`
- Tests: transformation, C1 stub generation, integration

## TASK-0077-0009-003: Artifact writers + idempotency
- `ProductArtifactWriter.java`, `CapabilityStubWriter.java`, `IdempotencyHash.java`
- Tests: write/rerun, idempotency detection

## TASK-0077-0009-004: E2E smoke + fixture
- `ci/smoke/x-create-product-smoke.sh`, `XCreateProductE2ETest.java`
- Tests: CLI invocation, idempotency smoke
