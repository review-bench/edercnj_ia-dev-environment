# Test Plan — story-0077-0010

## Unit Tests
- `XCreateCapabilityCommandTest`: help, required args, invalid args
- `AutoDecomposeHeuristicTest`: deterministic 4-capability output

## Integration Tests
- `CapabilityDecompositionUseCaseIT`: end-to-end decomposition from Product
- `RNFInheritanceWriterIT`: inherited RNFs with no-relax markers

## E2E
- `XCreateCapabilityE2ETest`: full pipeline smoke
