# Test Plan — story-0074-0001

## Test File
`src/test/java/dev/iadev/domain/model/DependencyPolicyConfigTest.java`

## Scenarios (Double-Loop TDD — Red/Green/Refactor)

### Happy Path
1. Full YAML block with all fields → `DependencyPolicyConfig` populated correctly
2. `enabled=true` + min-versions JVM format → `VersionConstraint.JvmForm` parsed
3. `enabled=true` + min-versions NPM format → `VersionConstraint.NpmForm` parsed
4. `enabled=true` + min-versions Go format → `VersionConstraint.GoForm` parsed
5. `allowed-licenses` list → `LicenseWhitelist` populated

### Degenerate
6. YAML with no `dependencies.policy` block → `DependencyPolicyConfig.DEFAULT` (enabled=false)
7. `dependencies.policy.enabled=false` explicit → `enabled()=false`
8. Block-on absent → defaults applied (HIGH=block, license=block, min-version=block, max-version=warn, freshness=warn)

### Error / Boundary
9. JVM + NPM fields on same entry → `ConfigValidationException` (D-R9 ambiguous)
10. Wildcard `"*"` in NPM name → `ConfigValidationException` (WILDCARD_NOT_ALLOWED)
11. Unknown scope in scope-policy → `ConfigValidationException`
12. Invalid block-on value (not block/warn-only) → `ConfigValidationException`
13. `denied-cves` as flat list → parsed correctly

### Performance/SLA
14. Parse of YAML with 200 min-versions entries completes < 50ms

### Security
15. `allowed-licenses` containing empty string → validation error (no degenerate whitelist)
16. `denied-cves` with non-CVE pattern string accepted (skill-level check, not parser)

## Governance
`Epic0074DepPolicySmokeIT` depends on these passing. Coverage target: ≥ 95% line on new classes.
