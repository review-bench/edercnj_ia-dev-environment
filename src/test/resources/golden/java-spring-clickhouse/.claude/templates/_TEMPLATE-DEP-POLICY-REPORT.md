## Header

| Field | Value |
|-------|-------|
| Story | {{STORY_ID}} |
| Timestamp | {{TIMESTAMP}} |
| Policy Enabled | {{POLICY_ENABLED}} |
| Dimensions Checked | {{DIMENSIONS_CHECKED}} |
| Manifest(s) | {{MANIFESTS}} |
| Total Dependencies Scanned | {{TOTAL_DEPS}} |

## Summary

| Dimension | BLOCK | WARN_ONLY | IGNORE | SKIP |
|-----------|-------|-----------|--------|------|
| Denied CVE | {{CVE_BLOCK}} | — | — | {{CVE_SKIP}} |
| License | {{LIC_BLOCK}} | {{LIC_WARN}} | {{LIC_IGNORE}} | {{LIC_SKIP}} |
| Min-Version | {{MIN_BLOCK}} | {{MIN_WARN}} | {{MIN_IGNORE}} | — |
| Max-Version | {{MAX_BLOCK}} | {{MAX_WARN}} | {{MAX_IGNORE}} | — |
| Freshness | {{FRESH_BLOCK}} | {{FRESH_WARN}} | {{FRESH_IGNORE}} | {{FRESH_SKIP}} |
| **TOTAL** | **{{TOTAL_BLOCK}}** | **{{TOTAL_WARN}}** | **{{TOTAL_IGNORE}}** | |

**Gate result:** {{GATE_RESULT}} (`{{EXIT_CODE}}`)

## Blocking Violations

{{#if HAS_BLOCKING_VIOLATIONS}}
| Dependency | Version | Scope | Type | Detail | Final Action |
|------------|---------|-------|------|--------|--------------|
{{BLOCKING_VIOLATIONS_TABLE}}
{{else}}
_No blocking violations._
{{/if}}

## Warning Violations

{{#if HAS_WARNING_VIOLATIONS}}
| Dependency | Version | Scope | Type | Detail | Final Action |
|------------|---------|-------|------|--------|--------------|
{{WARNING_VIOLATIONS_TABLE}}
{{else}}
_No warning violations._
{{/if}}

## Suppressed (IGNORE)

{{SUPPRESSED_COUNT}} violation(s) suppressed by scope-policy (IGNORE action). Details omitted.

## Policy Snapshot

```yaml
dependencies:
  policy:
    enabled: {{POLICY_ENABLED}}
    denied-cves: {{DENIED_CVES}}
    allowed-licenses: {{ALLOWED_LICENSES}}
    freshness-window-days: {{FRESHNESS_WINDOW_DAYS}}
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

## Tooling

| Tool | Purpose | Version |
|------|---------|---------|
| {{AUDIT_TOOL}} | Dependency resolution | {{AUDIT_TOOL_VERSION}} |
| {{CVE_TOOL}} | CVE lookup | {{CVE_TOOL_VERSION}} |
| {{LICENSE_TOOL}} | License resolution | {{LICENSE_TOOL_VERSION}} |
