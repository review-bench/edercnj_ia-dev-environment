# Architecture Plan — story-0074-0001

## Summary
Foundation layer for EPIC-0074: domain model + capabilities + governance artifacts.

## Dependency Direction
```
adapter.inbound (parser) → application (Governance.fromMap) → domain (DependencyPolicyConfig) ← adapter.outbound
```

## New Domain Types (domain/model/)

```
DependencyPolicyConfig                     — root record
├── enabled: boolean                        — default false (Rule 19 safe)
├── minVersions: List<VersionConstraint>
├── maxVersions: List<VersionConstraint>
├── allowedLicenses: LicenseWhitelist
├── deniedCves: List<String>
├── freshnessWindowDays: int                — default 365
└── blockOn: BlockOnPolicy
    ├── severityCve: BlockAction            — HIGH|CRITICAL (default HIGH = block)
    ├── license: BlockAction                — any-violation (default block)
    ├── minVersion: BlockAction             — any-violation (default block)
    ├── maxVersion: BlockAction             — warn-only (default warn)
    └── freshness: BlockAction              — warn-only (default warn)

VersionConstraint (oneOf — D-R9):
  JVM form:   groupId, artifactId (wildcard allowed), version
  NPM/PyPI:   name, version
  Go:         module, version

LicenseWhitelist:
  allowed: List<String>                     — SPDX identifiers

ScopePolicy:
  compile: BlockAction                      — default block
  runtime: BlockAction                      — default block
  test: BlockAction                         — default warn
  dev: BlockAction                          — default warn

BlockAction enum: BLOCK | WARN_ONLY | IGNORE
```

## Integration with Governance
`Governance` record gains a `dependencyPolicy` field (7th param). Backward compat:
- existing 6-param `new Governance(...)` calls gain `DependencyPolicyConfig.DEFAULT` via compact constructor default.
- `ProjectConfig.governance()` delegates `dependencyPolicy()` accessor.

## Key Decision: Placement in Governance (not TechStack)
Policy enforcement is a governance concern (same layer as compliance, branching, documentation, quality). NOT a tech stack concern (no tooling dependency from the model itself).
