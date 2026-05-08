# Task Breakdown — story-0073-0001

| ID | Description | Layer | Depends On |
|----|-------------|-------|-----------|
| task-0073-0001-001 | Verify ADR number (check docs/adr/ + in-flight branches) | Governance | — |
| task-0073-0001-002 | Apply D-R3 criterion: Rule vs dispensar | Governance | 001 |
| task-0073-0001-003 | Create capabilities/quality/regression/{self,service}.yaml | Domain | — |
| task-0073-0001-004 | Create capabilities/quality/dast/{zap-passive,zap-active,nuclei}.yaml | Domain | — |
| task-0073-0001-005 | Update capabilities/_index.yaml (5 new IDs) | Domain | 003, 004 |
| task-0073-0001-006 | Extend QualityConfig.java with RegressionConfig + DastConfig | Domain | — |
| task-0073-0001-007 | Add unit tests: happy + boundary (target=production rejected, nuclei pinning) | Test | 006 |
| task-0073-0001-008 | Create docs/adr/ADR-0026-regression-shell-and-dast.md | Governance | 001, 002 |
| task-0073-0001-009 | Update docs/audit-gates-catalog.md (reserved entry audit-regression-shell.sh) | Governance | — |
