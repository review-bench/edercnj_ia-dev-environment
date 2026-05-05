# Implementation Plan — story-0077-0017

## TASK-0077-0017-001: PhaseGateC4Validator + ExecuteC4PhaseGateUseCase
- `domain/quality/PhaseGateC4Validator` + `PhaseGateResult` record
- `application/quality/ExecuteC4PhaseGateUseCase`
- Unit tests for both

## TASK-0077-0017-002: C4ValidationReportGenerator
- `adapter/outbound/reporting/C4ValidationReportGenerator`
- Unit tests for report format

## TASK-0077-0017-003: Smoke test
- Full E2E: validator → use case → report generator
- `test/adapter/inbound/cli/C4PhaseGateSmokeTest`
