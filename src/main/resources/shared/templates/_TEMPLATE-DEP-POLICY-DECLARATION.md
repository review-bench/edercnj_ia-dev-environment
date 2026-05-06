# Dependency Policy Declaration — {{PROJECT_NAME}}

> **Generated from:** `dependencies.policy` block in project YAML  
> **Rule:** [Rule 32 — Dependency Policy Gate](../.claude/rules/32-dependency-policy-gate.md)  
> **Last updated:** {{TIMESTAMP}}

## Policy Status

| Field | Value |
|-------|-------|
| Enabled | {{POLICY_ENABLED}} |
| Freshness Window | {{FRESHNESS_WINDOW_DAYS}} days |
| Min-Version Constraints | {{MIN_VERSION_COUNT}} |
| Max-Version Constraints | {{MAX_VERSION_COUNT}} |
| Denied CVEs | {{DENIED_CVE_COUNT}} |
| Allowed Licenses | {{ALLOWED_LICENSE_COUNT}} |

## Enforcement Matrix (D-R10)

| Violation Type | Action | Override by Scope |
|----------------|--------|-------------------|
| Denied CVE | **BLOCK** (unconditional — RULE-074-01) | No — hard-block regardless of scope |
| License not in whitelist | {{BLOCK_ON_LICENSE}} | Yes |
| Dependency below min-version | {{BLOCK_ON_MIN_VERSION}} | Yes |
| Dependency above max-version | {{BLOCK_ON_MAX_VERSION}} | Yes |
| Dependency older than freshness window | {{BLOCK_ON_FRESHNESS}} | Yes |

## Scope Policy (D-R11)

| Dependency Scope | Action |
|------------------|--------|
| compile | {{SCOPE_COMPILE}} |
| runtime | {{SCOPE_RUNTIME}} |
| test | {{SCOPE_TEST}} |
| dev | {{SCOPE_DEV}} |
| provided | {{SCOPE_PROVIDED}} |
| build | {{SCOPE_BUILD}} |

## Denied CVEs (RULE-074-01 — Hard-Block)

{{#if HAS_DENIED_CVES}}
The following CVEs are unconditionally blocked regardless of scope, severity threshold, or patch availability:

| CVE ID | Added |
|--------|-------|
{{DENIED_CVES_TABLE}}
{{else}}
_No CVEs explicitly denied. Standard severity-cve enforcement applies._
{{/if}}

## License Whitelist

{{#if HAS_LICENSE_WHITELIST}}
Only the following licenses are permitted. Dependencies with other licenses will be {{BLOCK_ON_LICENSE}}:

{{ALLOWED_LICENSES_LIST}}
{{else}}
_No license restriction configured. All licenses are permitted._
{{/if}}

## Version Constraints

### Minimum Versions

{{#if HAS_MIN_VERSIONS}}
| Coordinate | Required Minimum |
|------------|-----------------|
{{MIN_VERSIONS_TABLE}}
{{else}}
_No minimum version constraints configured._
{{/if}}

### Maximum Versions

{{#if HAS_MAX_VERSIONS}}
| Coordinate | Maximum Allowed |
|------------|----------------|
{{MAX_VERSIONS_TABLE}}
{{else}}
_No maximum version constraints configured._
{{/if}}

## YAML Configuration Reference

To modify this policy, edit the `dependencies.policy` block in the project YAML:

```yaml
dependencies:
  policy:
    enabled: {{POLICY_ENABLED}}
    freshness-window-days: {{FRESHNESS_WINDOW_DAYS}}
    denied-cves:
      {{DENIED_CVES_YAML}}
    allowed-licenses:
      {{ALLOWED_LICENSES_YAML}}
    min-versions:
      {{MIN_VERSIONS_YAML}}
    max-versions:
      {{MAX_VERSIONS_YAML}}
    block-on:
      severity-cve: {{BLOCK_ON_CVE}}
      license: {{BLOCK_ON_LICENSE}}
      min-version: {{BLOCK_ON_MIN_VERSION}}
      max-version: {{BLOCK_ON_MAX_VERSION}}
      freshness: {{BLOCK_ON_FRESHNESS}}
    scope-policy:
      compile: {{SCOPE_COMPILE}}
      runtime: {{SCOPE_RUNTIME}}
      test: {{SCOPE_TEST}}
      dev: {{SCOPE_DEV}}
      provided: {{SCOPE_PROVIDED}}
      build: {{SCOPE_BUILD}}
```

## Validation

Run `x-validate-dependency-policy` to validate all dependencies against this policy:

```bash
/x-validate-dependency-policy
```

Or combined with the standard dependency audit:

```bash
/x-audit-dependencies --scope all --policy
```
