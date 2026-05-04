# Architecture Plan — story-0077-0010

## Context
Implement `x-create-capability` skill: decomposes a Product into 3-7 Capabilities with explicit RNF inheritance and C2 stub.

## Layers
- Adapter Inbound: `XCreateCapabilityCommand` (picocli)
- Application: `CreateCapabilitiesOrchestrationUseCase`
- Domain: `ProductToCapabilityTransformer`, `AutoDecomposeHeuristic`
- Adapter Outbound: `CapabilityArtifactWriter`, `RNFInheritanceWriter`

## Key Decisions
- `--auto-decompose` flag triggers heuristic; otherwise `--capabilities` JSON required
- RNF inheritance: all 6 mandatory RNFs from Product propagated to each Capability with `no-relax` markers
- `CapabilityNumbering` generates sequential IDs (capability-NNNN)
- Idempotency: skip write if hash matches existing artifact
