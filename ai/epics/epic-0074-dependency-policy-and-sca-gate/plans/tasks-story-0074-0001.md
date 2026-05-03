# Task Breakdown — story-0074-0001

## Tasks

### task-0074-0001-001: Resolve TBD numbers
- Grep rules dir for last rule number → Rule 32 confirmed
- Grep ADRs for last number → ADR-0027 confirmed
- No files created; numbers pinned for subsequent tasks

### task-0074-0001-002: DependencyPolicyConfig domain model
Files:
- `src/main/java/dev/iadev/domain/model/DependencyPolicyConfig.java`
- `src/main/java/dev/iadev/domain/model/VersionConstraint.java`
- `src/main/java/dev/iadev/domain/model/BlockOnPolicy.java`
- `src/main/java/dev/iadev/domain/model/BlockAction.java`
- `src/main/java/dev/iadev/domain/model/ScopePolicy.java`
- `src/main/java/dev/iadev/domain/model/LicenseWhitelist.java`
Tests: `DependencyPolicyConfigTest.java`

### task-0074-0001-003: Extend Governance + ProjectConfig
- Add `dependencyPolicy` field to `Governance` record
- Add `parseDependencyPolicy` to `ProjectConfig`
- Add `dependencyPolicy()` accessor to `ProjectConfig`
Tests: existing `GovernanceTest` + `ProjectConfigTest` coverage

### task-0074-0001-004: Capability YAML files
6 files under `capabilities/governance/`
Update `capabilities/_index.yaml`

### task-0074-0001-005: Rule 32
`src/main/resources/targets/claude/rules/32-dependency-policy-gate.md`
Mirror to `.claude/rules/32-dependency-policy-gate.md`

### task-0074-0001-006: ADR-0027
`docs/adr/ADR-0027-dependency-policy-gate.md`
Update `docs/adr/README.md`

### task-0074-0001-007: KP
`src/main/resources/targets/claude/knowledge/security/dependency-policy-playbook.md`

### task-0074-0001-008: Regenerate golden files
All profiles that include security KPs → add dep-policy-playbook to index

## Sequence
1→2→3→4→5→6→7→8 (strictly sequential — each depends on previous)

## File Footprint
write: src/main/java/dev/iadev/domain/model/DependencyPolicyConfig.java
write: src/main/java/dev/iadev/domain/model/VersionConstraint.java
write: src/main/java/dev/iadev/domain/model/BlockOnPolicy.java
write: src/main/java/dev/iadev/domain/model/BlockAction.java
write: src/main/java/dev/iadev/domain/model/ScopePolicy.java
write: src/main/java/dev/iadev/domain/model/LicenseWhitelist.java
write: src/main/java/dev/iadev/domain/model/Governance.java
write: src/main/java/dev/iadev/domain/model/ProjectConfig.java
write: capabilities/governance/dependency-policy.yaml
write: capabilities/governance/dependency-policy.maven.yaml
write: capabilities/governance/dependency-policy.gradle.yaml
write: capabilities/governance/dependency-policy.npm.yaml
write: capabilities/governance/dependency-policy.pip.yaml
write: capabilities/governance/dependency-policy.gomod.yaml
write: capabilities/_index.yaml
write: src/main/resources/targets/claude/rules/32-dependency-policy-gate.md
write: .claude/rules/32-dependency-policy-gate.md
write: docs/adr/ADR-0027-dependency-policy-gate.md
write: docs/adr/README.md
write: src/main/resources/targets/claude/knowledge/security/dependency-policy-playbook.md
write: src/test/java/dev/iadev/domain/model/DependencyPolicyConfigTest.java
regen: src/test/resources/golden/**
