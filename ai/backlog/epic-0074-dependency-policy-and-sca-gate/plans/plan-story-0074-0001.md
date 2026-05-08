# Implementation Plan — story-0074-0001

## Tasks

### task-0074-0001-001: Resolve Rule/ADR numbers (D-R3, D-R4)
- Grep existing rules, confirm Rule 32 and ADR-0027 are free
- Output: pinned numbers used in all subsequent tasks

### task-0074-0001-002: Create DependencyPolicyConfig + sub-records
Files:
- `src/main/java/dev/iadev/domain/model/DependencyPolicyConfig.java`
- Sub-records nested or as separate files: `VersionConstraint`, `LicenseWhitelist`, `BlockOnPolicy`, `ScopePolicy`, `BlockAction`
Pattern: follow `DocumentationConfig.fromMap(Map)` and `QualityConfig.fromMap(Map)` style.

### task-0074-0001-003: Add dependencyPolicy to Governance + ProjectConfig
- Extend `Governance.java` record with `DependencyPolicyConfig dependencyPolicy` param
- Add compact constructor default (`DependencyPolicyConfig.DEFAULT`)
- Add `ProjectConfig.dependencyPolicy()` delegating accessor
- Add `ProjectConfig.parseDependencyPolicy(Map)` static helper

### task-0074-0001-004: Create capabilities YAML files (6 files)
- `capabilities/governance/dependency-policy.yaml` (parent, `requires-capabilities: []`)
- `capabilities/governance/dependency-policy.maven.yaml`
- `capabilities/governance/dependency-policy.gradle.yaml`
- `capabilities/governance/dependency-policy.npm.yaml`
- `capabilities/governance/dependency-policy.pip.yaml`
- `capabilities/governance/dependency-policy.gomod.yaml`
- Update `capabilities/_index.yaml`

### task-0074-0001-005: Create Rule 32
- `src/main/resources/targets/claude/rules/32-dependency-policy-gate.md`
- Also copy to `.claude/rules/32-dependency-policy-gate.md` (generated output)

### task-0074-0001-006: Create ADR-0027
- `docs/adr/ADR-0027-dependency-policy-gate.md`
- Update `docs/adr/README.md`

### task-0074-0001-007: Create KP dependency-policy-playbook.md
- `src/main/resources/targets/claude/knowledge/security/dependency-policy-playbook.md`
- frontmatter v3.0, model: haiku

### task-0074-0001-008: Write tests
- `DependencyPolicyConfigTest.java` — happy, degenerate, schema validation error, wildcard
- Validate ≥ 5 malformed YAML inputs are rejected

### task-0074-0001-009: Golden file updates
- Regenerate golden files for profiles that include governance capabilities
- Run `GoldenFileRegenerator` or update golden files manually
