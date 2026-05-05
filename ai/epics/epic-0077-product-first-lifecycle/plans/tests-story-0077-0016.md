# Test Plan — story-0077-0016

## Unit Tests

### C4IntegrityValidatorTest
- `validate_codeDiagramWithOutwardDep_returnsError`
- `validate_codeDiagramWithValidHexagonal_returnsOk`
- `validate_nonCodeDiagramWithContent_returnsOk`
- `validate_nullClasses_returnsOk` (non-code levels don't need class lists)

### HexagonalArchitectureValidatorTest
- `validate_outwardDependency_returnsViolation`
- `validate_validHexagonal_returnsOk`
- `validate_domainToAdapter_isOutward`

### ValidateC4IntegrityUseCaseTest
- `validate_withViolations_returnsAggregatedResult`
- `validate_noViolations_returnsValid`

## Smoke Test
- `C4ValidateSmokeTest` — 5 scenarios: valid hexagonal, outward dep, layer crossing, large system, read-only
