# Rule 32 — Dependency Policy Gate

> **Related:** Rule 05 (Quality Gates), Rule 06 (Security Baseline), Rule 19 (Backward Compatibility), Rule 24 (Execution Integrity), Rule 26 (Audit Gate Lifecycle), Rule 27 (Zero-Bypass Lifecycle).
> **Introduced by:** EPIC-0074 (Dependency Policy & SCA Final Gate) — story-0074-0001.
> **ADR:** [ADR-0027 — Dependency Policy Gate](../../docs/adr/ADR-0027-dependency-policy-gate.md).
> **Capability:** `governance.dependency-policy` (`capabilities/governance/dependency-policy.yaml`).

## Purpose

Third-party dependencies are an unaudited attack surface. Before EPIC-0074, projects could
declare any dependency version regardless of known CVEs, banned licenses, or staleness — no
gate existed to enforce organizational policy at CI time. The result: discovered vulnerabilities
accumulate silently, license compliance is checked manually (if at all), and freshness drifts
until an audit forces reactive remediation.

Rule 32 introduces a **blocking dependency policy gate** that projects opt into via the
`dependencies.policy` YAML block. When enabled, the gate:

1. **Validates version constraints** (min-versions / max-versions, cross-stack — D-R9).
2. **Checks CVE severity** against the configured threshold (default HIGH, CVSS ≥ 7.0) and
   hard-blocks any dependency in the `denied-cves` list regardless of patch availability
   (RULE-074-01).
3. **Enforces license whitelisting** against SPDX identifiers in `allowed-licenses`.
4. **Warns or blocks stale dependencies** outside the `freshness-window-days` window
   (default 365 days, default action WARN_ONLY per D-R10).
5. **Applies per-scope overrides** via `scope-policy` (D-R11): compile and runtime default
   to BLOCK; test, dev, provided, and build default to WARN_ONLY.

**Safe default (Rule 19):** `dependencies.policy.enabled: false` — existing projects are
completely unaffected until they opt in.

## `dependencies.policy` YAML Block

```yaml
dependencies:
  policy:
    enabled: true
    min-versions:
      - { groupId: org.springframework.boot, artifactId: "*", version: "3.2.0" }
      - { name: lodash, version: "4.17.21" }
      - { module: github.com/foo/bar, version: "v1.2.0" }
    max-versions:
      - { groupId: org.springframework.boot, artifactId: "*", version: "3.x" }
    allowed-licenses:
      - Apache-2.0
      - MIT
      - BSD-3-Clause
    denied-cves:
      - CVE-2024-12345
    freshness-window-days: 365
    block-on:
      severity-cve: HIGH
      license: any-violation
      min-version: any-violation
      max-version: warn-only
      freshness: warn-only
    scope-policy:
      compile: block
      runtime: block
      test: warn-only
      dev: warn-only
      provided: warn-only
      build: warn-only
```

| Field | Type | Default | Semantics |
| :--- | :--- | :--- | :--- |
| `enabled` | boolean | `false` | Gate active; safe default is no-op (Rule 19) |
| `min-versions` | VersionConstraint[] | `[]` | Minimum required versions per dependency (D-R9) |
| `max-versions` | VersionConstraint[] | `[]` | Maximum allowed versions per dependency (D-R9) |
| `allowed-licenses` | string[] | `[]` | SPDX identifiers; empty = license check disabled |
| `denied-cves` | string[] | `[]` | Hard-block CVEs regardless of patch (RULE-074-01) |
| `freshness-window-days` | integer ≥ 0 | `365` | Days before a dependency is stale |
| `block-on` | BlockOnPolicy | D-R10 defaults | Per-dimension enforcement actions |
| `scope-policy` | ScopePolicy | D-R11 defaults | Per-scope enforcement overrides |

## Version Constraint Formats (D-R9)

Three disjoint formats — fields `groupId`, `name`, and `module` are mutually exclusive
per entry. Combining them is a `ConfigValidationException`.

| Format | Stack | Required fields | Optional | Wildcard |
| :--- | :--- | :--- | :--- | :--- |
| JVM | Maven / Gradle | `groupId`, `version` | `artifactId` (default `"*"`) | `"*"` in `artifactId` only |
| NPM/PyPI | npm, pip | `name`, `version` | — | `"*"` in `name` **forbidden** |
| Go | Go modules | `module`, `version` | — | — |

## Default Enforcement Matrix (D-R10)

| Dimension | Default Action |
| :--- | :--- |
| `severity-cve` | `BLOCK` |
| `license` | `BLOCK` |
| `min-version` | `BLOCK` |
| `max-version` | `WARN_ONLY` |
| `freshness` | `WARN_ONLY` |

`BlockAction` values: `BLOCK` (fails gate), `WARN_ONLY` (surfaced in report, gate passes),
`IGNORE` (suppressed).

## Scope Policy (D-R11)

| Scope | Default Action |
| :--- | :--- |
| `compile` | `BLOCK` |
| `runtime` | `BLOCK` |
| `test` | `WARN_ONLY` |
| `dev` / `devDependency` / `devDependencies` | `WARN_ONLY` |
| `provided` | `WARN_ONLY` |
| `build` | `WARN_ONLY` |

When the `scope-policy` sub-map is absent, all six fields use the D-R11 defaults. A null
scope string resolves to `compile` action (safe default for unknown scope).

## Enforcement

| Camada | Mechanism | Trigger | Exit |
| :--- | :--- | :--- | :--- |
| **0 — PreToolUse** | `enforce-preflight-gates.sh` (Rule 24 §Camada 0) | `git push`, `gh pr create`, `Skill x-create-pr` | Blocks if dep-policy evidence absent |
| **1 — Normative** | This rule + CLAUDE.md | Every conversation | — |
| **2 — CI Script** | `audit-dep-policy.sh` | PR open/sync to `develop` or `epic/*` | 1 `DEP_POLICY_VIOLATION` |
| **3 — Java Test** | `Epic0074DepPolicySmokeIT` | `mvn verify` | JUnit assertion failure |

## Mandatory Invocation in `x-implement-story`

Phase 3 of `x-implement-story` MUST conditionally invoke:

```
Skill(skill: "x-validate-dependency-policy", model: "haiku", args: "<STORY-ID>")
[conditional: flag.dep_policy_enabled]
```

The condition resolves to true when `dependencies.policy.enabled: true` in the project YAML.
Evidence artifact: `ai/epics/epic-XXXX/reports/dep-policy-validation-report-STORY-ID.md`
(Rule 27 Surface 13).

Silent omission when enabled is a `PROTOCOL_VIOLATION` under Rule 24.

## `denied-cves` Hard-Block (RULE-074-01)

CVE identifiers listed in `denied-cves` cause a hard `BLOCK` regardless of:
- The `block-on.severity-cve` setting.
- Whether a patch version exists.
- The `scope-policy` for the dependency's scope.

Rationale: `denied-cves` represents organizational policy decisions (e.g., CVEs with
incomplete patches, CVEs in actively-exploited campaigns) that override automated
severity scoring.

## Forbidden

- Setting `allowed-licenses: []` alongside `block-on.license: any-violation` — this
  blocks every dependency including those without licenses. Detected at parse time
  and raised as `ConfigValidationException`.
- Setting `freshness-window-days` to a negative value — `ConfigValidationException`.
- Using wildcard `"*"` in the NPM/PyPI `name` field — use JVM `groupId+artifactId` for
  wildcard constraints (D-R9).
- Combining `groupId`, `name`, or `module` in a single version constraint entry —
  formats are mutually exclusive (D-R9 ambiguity rule).
- Bypassing the gate by setting `enabled: false` in production CI config without a
  tech-lead–approved ADR exception.

## Backward Compatibility (Rule 19)

- `dependencies.policy` block **absent** → `DependencyPolicyConfig.DEFAULT` (`enabled=false`); gate is no-op.
- `dependencies.policy.enabled: false` → gate is no-op; no validation performed.
- Individual `block-on` or `scope-policy` fields absent → D-R10 / D-R11 defaults apply.
- New `DependencyPolicyConfig dependencyPolicy` field in `Governance` record → compact constructor applies `DependencyPolicyConfig.DEFAULT` when null; backward-compatible constructors in `ProjectConfig` also pass `DependencyPolicyConfig.DEFAULT`.

## Audit

`audit-dep-policy.sh --self-check` MUST verify:
1. This rule file (`32-dependency-policy-gate.md`) exists.
2. `capabilities/governance/dependency-policy.yaml` exists and is valid.
3. `DependencyPolicyConfig.java` exists under `domain/model/`.

Failure → `RULE_32_ENFORCEMENT_BROKEN`.

---

> **Catalogado em:** [`docs/audit-gates-catalog.md`](../../docs/audit-gates-catalog.md)
