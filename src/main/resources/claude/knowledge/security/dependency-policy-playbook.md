---
name: dependency-policy-playbook
description: Dependency policy gate guidance — YAML schema, version constraint formats (D-R9), block-on matrix (D-R10), scope policy (D-R11), CVE hard-block rules, and remediation patterns.
visibility: internal
model: haiku
requires-capabilities:
  - governance.dependency-policy
---

> 🔒 **KNOWLEDGE PACK** — Referenced internally by `x-dep-policy-validate`. Not user-invocable.

# Dependency Policy Playbook

## YAML Schema Quick Reference

```yaml
dependencies:
  policy:
    enabled: true                       # false = gate disabled (Rule 19 safe default)
    min-versions: [...]                 # D-R9 version floor list
    max-versions: [...]                 # D-R9 version ceiling list
    allowed-licenses: [Apache-2.0, MIT] # SPDX identifiers; empty = check disabled
    denied-cves: [CVE-2024-12345]       # Hard-block regardless of patch (RULE-074-01)
    freshness-window-days: 365          # Days before staleness; 0 = always stale
    block-on:                           # D-R10 per-dimension enforcement
      severity-cve: HIGH                # BLOCK | WARN_ONLY | IGNORE
      license: any-violation            # any-violation = BLOCK
      min-version: any-violation
      max-version: warn-only
      freshness: warn-only
    scope-policy:                       # D-R11 per-scope overrides
      compile: block
      runtime: block
      test: warn-only
      dev: warn-only
      provided: warn-only
      build: warn-only
```

## Version Constraint Formats (D-R9)

Three disjoint formats. Fields `groupId`, `name`, `module` are mutually exclusive.

### JVM (Maven / Gradle)

```yaml
- { groupId: org.springframework.boot, artifactId: "*", version: "3.2.0" }
- { groupId: com.fasterxml.jackson.core, artifactId: jackson-databind, version: "2.15.0" }
```

- `artifactId` defaults to `"*"` (all artifacts under the group).
- Wildcard `"*"` is valid only in `artifactId`.

### NPM / PyPI

```yaml
- { name: lodash, version: "4.17.21" }
- { name: requests, version: "2.31.0" }
```

- `name` must be exact — wildcard `"*"` is **forbidden** (use JVM groupId for wildcard).

### Go Modules

```yaml
- { module: github.com/foo/bar, version: "v1.2.0" }
- { module: golang.org/x/crypto, version: "v0.21.0" }
```

- Full module path required (e.g. `github.com/owner/repo`, not just `repo`).

## Default Enforcement Matrix (D-R10)

| Dimension | Default `BlockAction` | Rationale |
|-----------|----------------------|-----------|
| `severity-cve` | `BLOCK` | CVE ≥ HIGH is an active risk |
| `license` | `BLOCK` | License non-compliance is legal risk |
| `min-version` | `BLOCK` | Below-floor versions likely have known issues |
| `max-version` | `WARN_ONLY` | Version ceiling is advisory (API compatibility) |
| `freshness` | `WARN_ONLY` | Staleness needs attention but rarely breaks builds |

### `BlockAction` Values

| YAML string | Java enum | Semantics |
|-------------|-----------|-----------|
| `block` / `any-violation` / `hard-block` | `BLOCK` | Gate fails; PR blocked |
| `warn-only` / `warn_only` / `warning` | `WARN_ONLY` | Surfaced in report; gate passes |
| `ignore` | `IGNORE` | Suppressed entirely |

## Scope Policy (D-R11)

| Scope | Default | Typical override |
|-------|---------|-----------------|
| `compile` | `BLOCK` | Never relax in production |
| `runtime` | `BLOCK` | Never relax in production |
| `test` | `WARN_ONLY` | May tighten for security-sensitive test suites |
| `dev` / `devDependency` / `devDependencies` | `WARN_ONLY` | OK to relax further |
| `provided` | `WARN_ONLY` | Container-supplied; relax if verified at infra level |
| `build` | `WARN_ONLY` | Build tooling; relax for internal tooling |

A null scope string resolves to the `compile` action (safe default).

## CVE Hard-Block (RULE-074-01)

CVEs in `denied-cves` always `BLOCK` regardless of:
- `block-on.severity-cve` threshold
- Scope (compile, test, dev, etc.)
- Whether a patched version exists

**Use cases for `denied-cves`:**
- CVEs with incomplete patches (CVSS says patched but bypass known)
- CVEs in actively-exploited campaigns
- CVEs where the organization has a vendor-specific risk profile

## ConfigValidationException Triggers

| Condition | Error |
|-----------|-------|
| `allowed-licenses: []` + `block-on.license: any-violation` | Every dep would be blocked — validate at parse |
| `freshness-window-days < 0` | Negative window is nonsensical |
| `name: "*"` in NPM/PyPI entry | Wildcard forbidden for NPM/PyPI (use JVM groupId) |
| `groupId` + `name` or `module` in same entry | Mutually exclusive fields — ambiguous format (D-R9) |
| `version` absent in any entry | Required field missing |

## Remediation Patterns

### CVE Found (severity ≥ HIGH)

1. Upgrade the dependency to the patched version: update `min-versions` floor if needed.
2. If no patch: add the CVE to `denied-cves` and open an issue to track vendor response.
3. If scope is `test`, consider relaxing scope-policy for test temporarily with justification ADR.

### License Violation

1. Check whether the project's OSS policy covers the license (e.g., LGPL may be acceptable).
2. If acceptable: add SPDX identifier to `allowed-licenses`.
3. If not acceptable: replace the dependency or obtain a commercial license.

### Min-Version Violation

1. Upgrade the dependency to at least the declared floor.
2. If upgrading is blocked by another constraint, create a temp `max-versions` ceiling and open
   a follow-up ticket to resolve the conflict.

### Freshness Warning

1. Check if the package is still maintained (GitHub, npm, PyPI activity).
2. If abandoned: identify a maintained fork or alternative.
3. If actively maintained but slow-release: increase `freshness-window-days` with justification.

## Integration with `x-dep-policy-validate`

The skill reads `DependencyPolicyConfig` from the assembled `ProjectConfig`, resolves the
manifest files (pom.xml, package.json, go.mod), and emits:

- `ai/epics/epic-XXXX/reports/dep-policy-validation-report-STORY-ID.md` (evidence artifact)
- Exit 0: all checks passed or gate disabled
- Exit 1: `DEP_POLICY_BLOCK` — at least one BLOCK-action finding
- Exit 2: `DEP_POLICY_WARN` — WARN_ONLY findings only (gate passes, report generated)
