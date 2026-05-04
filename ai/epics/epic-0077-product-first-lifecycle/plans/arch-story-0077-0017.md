# Architecture Plan — story-0077-0017

## Domain Layer
- `domain/quality/PhaseGateC4Validator` — pure validator; holds reference to `C4IntegrityValidator`; receives a list of `C4Diagram` and returns `PhaseGateResult`

## Application Layer
- `application/quality/ExecuteC4PhaseGateUseCase` — orchestrates: accepts diagrams + code context, delegates to `PhaseGateC4Validator`, returns `PhaseGateResult`

## Adapter Outbound
- `adapter/outbound/reporting/C4ValidationReportGenerator` — formats `PhaseGateResult` as Markdown report string; no I/O

## Dependency Direction
domain ← application ← adapter.outbound (for reporting only — one-way)
