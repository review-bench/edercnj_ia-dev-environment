# Architecture Plan — story-0077-0016

## Decision

`C4IntegrityValidator` and `HexagonalArchitectureValidator` live in `domain/architecture/` (zero external deps). `ValidateC4IntegrityUseCase` in `application/architecture/` orchestrates both. `XInternalC4ValidateCommand` in `adapter.inbound.cli` is the CLI entry point.

## Dependency Direction

```
adapter.inbound.cli → application.architecture → domain.architecture
```

| Layer | Classes |
| :--- | :--- |
| domain.architecture | C4IntegrityValidator, HexagonalArchitectureValidator |
| application.architecture | ValidateC4IntegrityUseCase |
| adapter.inbound.cli | XInternalC4ValidateCommand |

## Key Decisions

- Read-only validation only — no auto-fix
- `C4IntegrityValidator` validates C4 diagram structure per level; `HexagonalArchitectureValidator` delegates to `C4CodeLevelValidator` for CODE level checks
- `Violation` record carries `message`, `type` (enum), `severity` (ERROR/WARN)
- `ValidateC4IntegrityUseCase` aggregates violations from both validators into a single result
