---
name: dependency-policy
description: Full dependency policy gate reference — YAML block, version constraints, CVE hard-block, license whitelisting, scope policy
requires-capabilities: []
---
# Dependency Policy Gate — Full Reference

> **Introduced by:** EPIC-0074
> **ADR:** ADR-0027
> **Capability:** `governance.dependency-policy`

## Purpose

Blocking gate for third-party dependencies. Projects opt in via `dependencies.policy` YAML block.
When enabled, validates CVE severity, license compliance, version constraints, and staleness.
**Safe default (Rule 19):** `dependencies.policy.enabled: false` — existing projects are completely unaffected.

## `dependencies.policy` YAML Block

```yaml
dependencies:
  policy:
    enabled: true
    min-versions:
      - { groupId: org.springframework.boot, artifactId: "*", version: "3.2.0" }
      - { name: lodash, version: "4.17.21" }
    allowed-licenses: [Apache-2.0, MIT, BSD-3-Clause]
    denied-cves: [CVE-2024-12345]
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
```

## Version Constraint Formats (D-R9)

| Format | Stack | Required fields | Wildcard |
| :--- | :--- | :--- | :--- |
| JVM | Maven/Gradle | `groupId`, `version` | `"*"` in `artifactId` only |
| NPM/PyPI | npm, pip | `name`, `version` | `"*"` in `name` forbidden |
| Go | Go modules | `module`, `version` | none |

Fields `groupId`, `name`, `module` are mutually exclusive per entry.

## Default Enforcement Matrix (D-R10)

| Dimension | Default Action |
| :--- | :--- |
| `severity-cve` | `BLOCK` |
| `license` | `BLOCK` |
| `min-version` | `BLOCK` |
| `max-version` | `WARN_ONLY` |
| `freshness` | `WARN_ONLY` |

## Scope Policy (D-R11)

| Scope | Default Action |
| :--- | :--- |
| `compile` | `BLOCK` |
| `runtime` | `BLOCK` |
| `test` | `WARN_ONLY` |
| `dev` / `devDependency` | `WARN_ONLY` |
| `provided` | `WARN_ONLY` |
| `build` | `WARN_ONLY` |

## `denied-cves` Hard-Block (RULE-074-01)

CVE identifiers in `denied-cves` cause a hard `BLOCK` regardless of:
- The `block-on.severity-cve` setting
- Whether a patch version exists
- The `scope-policy` for the dependency's scope

## Enforcement

| Camada | Mechanism | Trigger | Exit |
| :--- | :--- | :--- | :--- |
| **0 — PreToolUse** | `enforce-preflight-gates.sh` | `git push`, `gh pr create` | Blocks if evidence absent |
| **2 — CI Script** | `audit-dep-policy.sh` | PR open/sync | `DEP_POLICY_VIOLATION` |
| **3 — Java Test** | `Epic0074DepPolicySmokeIT` | `mvn verify` | JUnit assertion failure |

## Mandatory Invocation in `x-implement-story`

Phase 3 MUST conditionally invoke:

```
Skill(skill: "x-validate-dependency-policy", model: "haiku", args: "<STORY-ID>")
[conditional: flag.dep_policy_enabled]
```

Evidence artifact: `ai/epics/epic-XXXX/reports/dep-policy-validation-report-STORY-ID.md`.

## Forbidden

- Setting `allowed-licenses: []` alongside `block-on.license: any-violation` — blocks every dependency
- Setting `freshness-window-days` to a negative value
- Using wildcard `"*"` in NPM/PyPI `name` field
- Combining `groupId`, `name`, or `module` in a single version constraint entry
- Bypassing the gate by setting `enabled: false` without a tech-lead–approved ADR exception
