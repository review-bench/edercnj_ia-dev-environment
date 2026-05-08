---
name: x-validate-dependency-policy
description: "Conditional dep-policy gate: CVEs, licenses, versions, freshness; SARIF + report."
user-invocable: true
allowed-tools: Read, Write, Bash, Grep, Glob
argument-hint: "[--story-id STORY-XXXX-YYYY] [--report <path>] [--dry-run]"
requires-capabilities: [governance.dependency-policy]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Dependency Policy Validator

## Purpose

Validates all project dependencies against the `dependencies.policy` block declared in the project YAML (parsed into `DependencyPolicyConfig` — EPIC-0074, Rule 32). Applies the D-R10 enforcement matrix (block-on) and the D-R11 scope policy to each finding, emitting:

- **Exit 0** — gate disabled or all findings are WARN/IGNORE only; no BLOCK-level violations
- **Exit 1** (`DEP_POLICY_BLOCK`) — at least one BLOCK-level violation (denied CVE, license, min-version, or scope escalation)
- **Exit 2** (`DEP_POLICY_WARN`) — only WARN-level violations; no blocking failures

Produces evidence artifact at `ai/epics/epic-XXXX/reports/dep-policy-validation-report-STORY-ID.md` (required by Rule 27 Surface 13 — conditional on `quality.dependencyPolicy.enabled=true`).

## Triggers

- `/x-validate-dependency-policy` — validate all dependency dimensions
- `/x-validate-dependency-policy --story-id story-0074-0001` — emit evidence artifact for the given story
- `/x-validate-dependency-policy --dry-run` — enumerate violations without applying BLOCK/WARN exits

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--story-id` | String | — | Story identifier for evidence artifact path resolution |
| `--report` | Path | auto-resolved | Override artifact output path |
| `--dry-run` | Boolean | `false` | Run validation without producing exit-code enforcement |

## Activation Condition

Activated when `dependencies.policy.enabled: true` in project YAML. When disabled (or field absent), skill emits `PERF_DISABLED`-style log and exits 0 immediately — no validation performed, no artifact produced.

## YAML Schema (Reference)

```yaml
dependencies:
  policy:
    enabled: true
    min-versions:
      - groupId: com.fasterxml.jackson.core
        artifactId: jackson-databind
        version: "2.15.0"
    max-versions: []
    allowed-licenses: [Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause]
    denied-cves: [CVE-2021-44228, CVE-2023-1234567]
    freshness-window-days: 365
    block-on:
      severity-cve: BLOCK
      license: BLOCK
      min-version: BLOCK
      max-version: WARN_ONLY
      freshness: WARN_ONLY
    scope-policy:
      compile: BLOCK
      runtime: BLOCK
      test: WARN_ONLY
      dev: WARN_ONLY
      provided: WARN_ONLY
      build: WARN_ONLY
```

## Workflow

```
1. LOAD     -> Read DependencyPolicyConfig from project YAML
2. DETECT   -> Resolve manifest files (pom.xml, package.json, go.mod)
3. RESOLVE  -> Parse dependency list with versions and scopes
4. VALIDATE -> Apply 5 validation dimensions per dependency
5. CLASSIFY -> Apply D-R10 block-on + D-R11 scope-policy per finding
6. REPORT   -> Write structured report; exit with highest severity
```

### Step 1 — Load Policy

Read project YAML (`project.yaml`, `.ia-dev-env.yaml`, or the root config file). Parse the `dependencies.policy` block. If `enabled: false` or block absent → log `DEP_POLICY_DISABLED` and exit 0.

Key fields to extract:
- `denied-cves` list (RULE-074-01: always BLOCK regardless of scope/severity-threshold)
- `allowed-licenses` list (empty = no restriction)
- `min-versions` / `max-versions` list of VersionConstraints (JVM: groupId+artifactId; NPM: name; Go: module)
- `freshness-window-days` (default 365)
- `block-on` map → BlockOnPolicy (D-R10 defaults)
- `scope-policy` map → ScopePolicy (D-R11 defaults)

### Step 2 — Detect Manifests

| Build Tool | Manifest | Dependency Command |
|------------|----------|--------------------|
| Maven | `pom.xml` | `mvn dependency:list -DincludeScope=compile,runtime,test,provided` |
| Gradle | `build.gradle` / `build.gradle.kts` | `./gradlew dependencies --configuration runtimeClasspath,testRuntimeClasspath` |
| npm | `package.json` + `package-lock.json` | `npm list --json --all --depth 0` |
| yarn | `yarn.lock` | `yarn list --json --depth 0` |
| pnpm | `pnpm-lock.yaml` | `pnpm list --json --depth 0` |
| Go modules | `go.mod` | `go list -m -json all` |

Multiple manifests detected → validate each independently; merge findings; deduplicate by coordinate.

### Step 3 — Resolve Dependencies

Parse each manifest to extract a flat list of:

```
Dependency {
  coordinate: String   // "groupId:artifactId" | "name@version" | "module@version"
  version: String
  scope: String        // compile | runtime | test | dev | provided | build
  age_days: Integer    // resolved via mvn versions:display-dependency-updates or npm outdated
}
```

For JVM: use `mvn dependency:list` output (format `[INFO]    {groupId}:{artifactId}:{type}:{version}:{scope}`).
For NPM/yarn/pnpm: parse JSON list output; map `devDependencies` to scope `dev`, `dependencies` to scope `compile`.
For Go: `go list -m -json all` → `Module.Path`, `Module.Version`; scope always `compile`.

### Step 4 — Validate (5 Dimensions per Dependency)

For each resolved dependency, run the following checks in order:

#### 4.1 Denied CVE Check (RULE-074-01 — Always BLOCK)

```
if any CVE in denied-cves matches this dependency's known CVEs:
  VIOLATION(type=denied-cve, severity=BLOCK, cve=CVE-XXXX-XXXXXX)
  # Hard-block: scope-policy does NOT apply; block-on.severity-cve does NOT apply
  # The denied-cves list bypasses all override mechanisms
```

CVE lookup strategy: use output from `mvn dependency:resolve -Dsecurity=true` (OWASP plugin if available), `npm audit --json`, or `trivy image` JSON output. If no CVE scanner is available, skip 4.1 and log `CVE_SCAN_UNAVAILABLE`.

#### 4.2 License Check

```
if allowed-licenses is not empty:
  if dependency.license NOT IN allowed-licenses:
    VIOLATION(type=license, severity=block-on.license, license=<actual>)
```

License resolution: Maven → read `pom.xml` `<licenses>` section or use `mvn license:aggregate-download-licenses --json`; NPM → `package.json` `license` field; Go → `go-licenses` tool.
If license cannot be resolved → log `LICENSE_UNRESOLVABLE` as WARN.

#### 4.3 Min-Version Check

```
for each constraint in min-versions:
  if constraint matches dependency:
    if dependency.version < constraint.version:
      VIOLATION(type=min-version, severity=block-on.min-version, required=constraint.version)
```

JvmConstraint: match on `groupId` exact + `artifactId` exact or `"*"` wildcard (matches all artifacts in group).
NpmConstraint: match on `name` exact.
GoConstraint: match on `module` prefix (e.g., `golang.org/x/net` matches `golang.org/x/net/http2`).

#### 4.4 Max-Version Check

```
for each constraint in max-versions:
  if constraint matches dependency:
    if dependency.version > constraint.version:
      VIOLATION(type=max-version, severity=block-on.max-version, ceiling=constraint.version)
```

Same matching rules as 4.3.

#### 4.5 Freshness Check

```
if freshness-window-days > 0:
  if dependency.age_days > freshness-window-days:
    VIOLATION(type=freshness, severity=block-on.freshness, age_days=dependency.age_days)
```

Age resolution: run `mvn versions:display-dependency-updates -DprocessDependencies=true` → parse `[INFO] ... -> X.Y.Z available` lines; compute age as `today - release_date(latest_version)`. If update unavailable, skip freshness check.

### Step 5 — Classify Violations

For each `VIOLATION(type, severity_raw, ...)` from Step 4:

1. **Denied-CVE**: final_action = BLOCK regardless of anything (RULE-074-01).
2. **All other types**: `raw_action = severity_raw` (BlockOnPolicy field for the type).
3. **Scope escalation (D-R11)**: `scope_action = ScopePolicy.actionFor(dependency.scope)`. If `scope_action.ordinal() < raw_action.ordinal()` → demote to `scope_action`. If scope_action = IGNORE → suppress entirely.
4. **Final action**: `min(raw_action, scope_action)` by severity (BLOCK > WARN_ONLY > IGNORE).

Classification matrix example (D-R10 defaults + D-R11 test scope):

| Type | Raw action | Scope (test) | Final action |
|------|-----------|--------------|--------------|
| denied-cve | BLOCK | — | **BLOCK** (RULE-074-01 override) |
| license | BLOCK | WARN_ONLY | **WARN_ONLY** (scope demotes) |
| min-version | BLOCK | WARN_ONLY | **WARN_ONLY** |
| max-version | WARN_ONLY | WARN_ONLY | **WARN_ONLY** |
| freshness | WARN_ONLY | WARN_ONLY | **WARN_ONLY** |

### Step 6 — Report and Exit

Write report to `--report` path (or auto-resolve as `ai/epics/epic-XXXX/reports/dep-policy-validation-report-STORY-ID.md`). Use template `_TEMPLATE-DEP-POLICY-REPORT.md`.

Report sections:
- **Header**: timestamp, story-id, policy enabled, dimensions checked
- **Summary**: total dependencies scanned, violations by type and final-action
- **Blocking Violations**: table of all BLOCK final-action findings
- **Warning Violations**: table of all WARN_ONLY final-action findings
- **Suppressed**: count of IGNORE-classified findings (no detail)
- **Policy Snapshot**: rendered `DependencyPolicyConfig` as YAML for audit trail

Exit codes:
- `0` — disabled, or zero violations, or all violations are IGNORE
- `1` (`DEP_POLICY_BLOCK`) — ≥1 BLOCK final-action violation
- `2` (`DEP_POLICY_WARN`) — zero BLOCK violations; ≥1 WARN_ONLY violation

## Error Handling

| Condition | Exit | Code | Action |
|-----------|------|------|--------|
| Policy disabled (`enabled=false`) | 0 | `DEP_POLICY_DISABLED` | Log + exit 0; no artifact |
| Policy YAML parse error | 1 | `POLICY_PARSE_ERROR` | Log ConfigValidationException message |
| No manifest found | 1 | `MANIFEST_NOT_FOUND` | Log which files were probed |
| CVE scanner unavailable | 0 | `CVE_SCAN_UNAVAILABLE` | Skip CVE dimension; continue remaining checks |
| License unresolvable | — | `LICENSE_UNRESOLVABLE` | Log as WARN; continue |
| Freshness data unavailable | — | `FRESHNESS_UNAVAILABLE` | Skip freshness dimension; continue |

## Integration Notes

- Invoked as **MANDATORY TOOL CALL** by `x-implement-story` Phase 3 §3.Q.dep when `quality.dependencyPolicy.enabled=true` (EPIC-0074, Rule 27 Surface 13, Rule 24 §Mandatory Evidence Artifacts).
- Integrates with `x-audit-dependencies` via shared `--policy` flag: `x-audit-dependencies --scope all --policy` delegates policy validation to this skill after completing the standard audit.
- Evidence artifact path follows PathResolver v4: `ai/epics/epic-XXXX-<slug>/reports/dep-policy-validation-report-STORY-ID.md`.
- `audit-dep-policy.sh` (Camada 2 CI script, Rule 32) checks existence of this artifact for merged PRs when `DEPENDENCY_POLICY_ENABLED=true` in CI env.

## RULE-074-01 (Denied CVEs — Hard-Block)

Denied CVEs from `denied-cves` list are **unconditional BLOCK** regardless of:
- Scope (even `test`, `dev`, `build`)
- Severity threshold (even INFO)
- `block-on.severity-cve` setting
- Patch availability

This is the only violation type that bypasses D-R11 scope-policy demotion.
