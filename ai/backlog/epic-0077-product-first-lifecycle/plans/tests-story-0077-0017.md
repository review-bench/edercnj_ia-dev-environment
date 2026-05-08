# Test Plan — story-0077-0017

## PhaseGateC4ValidatorTest
- valid diagrams → gateResult.passed=true
- missing CONTEXT level → passed=false, violation present
- invalid CODE dependencies → passed=false, OUTWARD_DEPENDENCY violation

## ExecuteC4PhaseGateUseCaseTest
- delegates to validator, propagates result
- null diagrams → exception

## C4ValidationReportGeneratorTest
- passed result → "PASSED" in report
- failed result with violations → violations listed

## C4PhaseGateSmokeTest (E2E)
- valid full stack → passed
- domain outward dep → blocked
