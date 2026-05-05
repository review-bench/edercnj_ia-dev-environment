# Task Breakdown — story-0077-0010

## TASK-0077-0010-001: CLI command + input parsing
- `XCreateCapabilityCommand.java`, `CapabilityInteractiveInputParser.java`
- Tests: help text, required args, auto-decompose flag

## TASK-0077-0010-002: Decomposition + transformation
- `ProductToCapabilityTransformer.java`, `AutoDecomposeHeuristic.java`, `ProductCapabilityDecomposition.java`, `CapabilityDecompositionUseCase.java`
- Tests: 3-7 capabilities output, RNF inheritance, deterministic heuristic

## TASK-0077-0010-003: RNF inheritance + artifact writers
- `CreateCapabilitiesOrchestrationUseCase.java`, `CapabilityArtifactWriter.java`, `RNFInheritanceWriter.java`, `CapabilityNumbering.java`
- Tests: write artifacts, no-relax markers, RNF readonly enforcement

## TASK-0077-0010-004: E2E smoke + fixture
- `XCreateCapabilityE2ETest.java`, `x-create-capability-smoke.sh`, fixture product file
- Tests: full pipeline, idempotency
