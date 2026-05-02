# ADR-0027 — Dependency Policy & SCA Final Gate

| Field | Value |
|-------|-------|
| **Status** | Accepted |
| **Date** | 2026-05-01 |
| **Epic** | EPIC-0074 (Dependency Policy & SCA Final Gate) |
| **Rule** | Rule 32 |
| **Authors** | EPIC-0074 implementation |

## Context

Third-party dependencies are an unaudited attack surface in all generated projects.
Before EPIC-0074, the `ia-dev-env` generator produced no mechanism for projects to
declare and enforce dependency hygiene — no CVE severity gate, no license whitelist,
no version floor/ceiling, no freshness check. The result: vulnerabilities accumulate
silently, license audits are manual, and dependency age drifts unbounded.

The domain model already contained `Governance` (EPIC-0044, EPIC-0071, EPIC-0072) as
the record for policy meta-configuration. The natural extension is a new sub-record
`DependencyPolicyConfig` that follows the established `fromMap(Map<String, Object>)`
pattern and safe-default philosophy (Rule 19: `enabled=false` by default).

## Decision

Introduce a **Dependency Policy & SCA Final Gate** across 6 stories:

1. **story-0074-0001** — Domain model foundation: `DependencyPolicyConfig`, `BlockAction`,
   `VersionConstraint` (sealed), `LicenseWhitelist`, `BlockOnPolicy`, `ScopePolicy`;
   `Governance` extended; Rule 32; ADR-0027; KP `dependency-policy-playbook`.
2. **story-0074-0002** — Skill `x-dep-policy-validate` + `_TEMPLATE-DEP-POLICY-REPORT.md`
   + `x-dependency-audit --policy` flag integration.
3. **story-0074-0003** — Template `_TEMPLATE-DEP-POLICY-DECLARATION.md` + DocsAssembler
   integration.
4. **story-0074-0004** — CI audit `audit-dep-policy.sh` (Rule 26, Rule 32).
5. **story-0074-0005** — `x-story-implement` Phase 3 conditional gate + Rule 27 Surface 13.
6. **story-0074-0006** — `Epic0074DepPolicySmokeIT` + CHANGELOG + CLAUDE.md "Concluded".

## Key Design Choices

### Safe Default: `enabled=false` (Rule 19)

The `dependencies.policy` block is **opt-in**. When the block is absent or
`enabled=false`, `DependencyPolicyConfig.DEFAULT` is returned and the gate is a no-op.
This preserves full backward compatibility — the 60+ epics that predated EPIC-0074
require zero migration.

### Sealed Interface `VersionConstraint` (D-R9)

Three disjoint formats — JVM (`groupId`+`artifactId`+`version`), NPM/PyPI
(`name`+`version`), Go (`module`+`version`) — are modelled as a sealed interface with
three record permits. The `fromMap` factory validates mutual exclusivity at parse time,
producing a `ConfigValidationException` on ambiguous input. JVM wildcards (`"*"` in
`artifactId`) are allowed; NPM/PyPI wildcards in `name` are explicitly rejected.

### Default Enforcement Matrix (D-R10)

The `BlockOnPolicy` default blocks CVE ≥ HIGH, license violations, and min-version
violations; it warns only on max-version and freshness. This is calibrated to block
critical security gaps while leaving headroom for gradual dependency management.

### Scope Policy (D-R11)

`ScopePolicy` allows compile/runtime to have a stricter action than test/dev/provided/build.
The default aligns with industry practice: production dependencies (compile, runtime) must
meet all constraints; development-only dependencies (test, dev) only warn. An unknown scope
resolves to the compile action (fail-safe).

### `denied-cves` Hard-Block (RULE-074-01)

CVEs in the `denied-cves` list always `BLOCK` regardless of `block-on.severity-cve`,
scope, or patch availability. This allows organizations to express out-of-band policy
decisions (active exploitation campaigns, incomplete patches) that supersede automated
CVSS scoring.

### `DependencyPolicyConfig` in `Governance`

The record is added as the 7th field of `Governance` following the compact-constructor
pattern established by `DocumentationConfig` (EPIC-0071) and `QualityConfig` (EPIC-0072).
Backward-compatible constructors in `ProjectConfig` supply `DependencyPolicyConfig.DEFAULT`,
preserving all existing call sites.

## Consequences

### Positive

- Projects can express and enforce dependency hygiene declaratively in their YAML.
- Cross-stack support (JVM, NPM/PyPI, Go) via `VersionConstraint` sealed interface.
- Zero blast radius for existing projects (opt-in gate).
- Consistent with Rule 19, Rule 24, Rule 26, Rule 27 governance contracts.

### Negative / Trade-offs

- `Governance` now has 7 fields, exceeding the Rule 03 guideline of ≤ 4 parameters.
  ADR-0009 (Wide Records Bound to External Schemas) already covers this exception pattern
  for `ProjectConfig`; `Governance` is treated under the same exemption — it is a
  structural mirror of the YAML root, not a business object.
- Actual CVE lookups (database queries) are deferred to story-0074-0002 (`x-dep-policy-validate`);
  this ADR covers the domain model and configuration parsing only.

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|-----------------|
| Flat policy fields on `Governance` directly | No composable unit; parsing is scattered; inconsistent with `QualityConfig` / `DocumentationConfig` sub-record pattern |
| Single `DependencyPolicyConfig` record with nested maps | Map-based nesting defeats type safety; `BlockAction.fromYaml()` parsing would be duplicated at every call site |
| BLOCK as default for all dimensions | Too aggressive for adoption; projects would need to disable all dimensions before enabling the gate, defeating the purpose |
| Per-file CVE deny-list | Central deny-list in YAML is easier to audit and review via PR diff |

## References

- Rule 32: `.claude/rules/32-dependency-policy-gate.md`
- Capability: `capabilities/governance/dependency-policy.yaml`
- Domain model: `src/main/java/dev/iadev/domain/model/DependencyPolicyConfig.java`
- EPIC-0074 story index: `ai/epics/epic-0074-dependency-policy-and-sca-gate/`
