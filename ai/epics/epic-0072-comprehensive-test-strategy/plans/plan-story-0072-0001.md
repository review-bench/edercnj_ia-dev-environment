# Implementation Plan — story-0072-0001

## Task Order (DAG)

1. **task-0072-0001-001** Determine ADR number (no deps)
2. **task-0072-0001-002** Create ADR-0025 (needs 001)
3. **task-0072-0001-003** Create QualityConfig.java records (needs 001 for ADR ref)
4. **task-0072-0001-004** Extend Governance + ProjectConfig + tests (needs 003)
5. **task-0072-0001-005** Performance capabilities YAMLs (parallel with 006, 007)
6. **task-0072-0001-006** Mutation capabilities YAMLs (parallel)
7. **task-0072-0001-007** Contract capabilities YAMLs (parallel)
8. **task-0072-0001-008** Update config-template + regenerate golden (needs 004)
9. **task-0072-0001-009** Update audit-gates-catalog template (optional, no hard dep)

## Files Changed

| File | Action |
| :--- | :--- |
| `docs/adr/ADR-0025-comprehensive-test-strategy.md` | CREATE |
| `src/main/java/dev/iadev/domain/model/QualityConfig.java` | CREATE |
| `src/main/java/dev/iadev/domain/model/Governance.java` | MODIFY (add quality field) |
| `src/main/java/dev/iadev/domain/model/ProjectConfig.java` | MODIFY (quality() + parseQuality()) |
| `src/test/java/dev/iadev/domain/model/QualityConfigTest.java` | CREATE |
| `src/test/java/dev/iadev/domain/model/GovernanceQualityTest.java` | CREATE |
| `capabilities/quality/performance/rest.yaml` | CREATE |
| `capabilities/quality/performance/grpc.yaml` | CREATE |
| `capabilities/quality/performance/cli.yaml` | CREATE |
| `capabilities/quality/performance/graphql.yaml` | CREATE |
| `capabilities/quality/performance/socket.yaml` | CREATE |
| `capabilities/quality/mutation/java-pit.yaml` | CREATE |
| `capabilities/quality/mutation/stryker.yaml` | CREATE |
| `capabilities/quality/mutation/mutmut.yaml` | CREATE |
| `capabilities/quality/mutation/go-mutesting.yaml` | CREATE |
| `capabilities/quality/contract/openapi-diff.yaml` | CREATE |
| `capabilities/quality/contract/pact.yaml` | CREATE |
| `capabilities/quality/contract/buf.yaml` | CREATE |
| `capabilities/quality/contract/scc.yaml` | CREATE |
| `capabilities/_index.yaml` | MODIFY (add quality category) |
| `src/main/resources/shared/config-templates/setup-config.java-spring.yaml` | MODIFY (add quality block) |
| `src/test/resources/config-templates/setup-config.java-spring.yaml` | MODIFY (add quality block) |
| Golden files for java-spring profile | REGEN (if quality block affects output) |
| `java/src/main/resources/shared/templates/_TEMPLATE-AUDIT-GATES-CATALOG.md` | MODIFY (reserve 3 entries) |
