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

# Skill: Dependency Policy Validator (slim — ADR-0012)

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

Activated when `dependencies.policy.enabled: true` in project YAML. When disabled (or field absent), skill emits `DEP_POLICY_DISABLED` log line and exits 0 immediately — no validation performed, no artifact produced.

## Output Contract

| Artifact | Path |
|----------|------|
| Validation report | `ai/epics/epic-XXXX/reports/dep-policy-validation-report-STORY-ID.md` (auto-resolved or via `--report`) |
| Sections | Header / Summary / Blocking Violations / Warning Violations / Suppressed / Policy Snapshot |
| Exit codes | 0 (disabled/clean) / 1 `DEP_POLICY_BLOCK` / 2 `DEP_POLICY_WARN` |

## Workflow Overview

```text
1. LOAD     -> Read DependencyPolicyConfig (denied-cves, allowed-licenses, min/max-versions, freshness-window-days, block-on, scope-policy)
2. DETECT   -> Resolve manifests (pom.xml / package.json / go.mod / etc.) + per-stack dependency-list commands
3. RESOLVE  -> Parse to Dependency{coordinate, version, scope, age_days}; map devDependencies→dev, dependencies→compile
4. VALIDATE -> 5 dimensions per dep: denied-cve (RULE-074-01 hard-block) / license / min-version / max-version / freshness
5. CLASSIFY -> Apply D-R10 block-on + D-R11 scope-policy demotion; denied-CVE bypasses all overrides
6. REPORT   -> _TEMPLATE-DEP-POLICY-REPORT.md; exit by highest severity
```

Detailed YAML schema, per-dimension validation logic, classification matrix (D-R10 × D-R11), and report template in [`references/full-protocol.md`](references/full-protocol.md):

- **YAML Schema** (§YAML Schema): full `dependencies.policy` block with all 7 sub-keys and example values.
- **Step 1** (§Step 1): policy load + key extraction; disabled-mode exit-0 short-circuit.
- **Step 2** (§Step 2): 6-row manifest detection table (Maven/Gradle/npm/yarn/pnpm/Go) with dependency-list commands.
- **Step 3** (§Step 3): per-build-tool parse rules (Maven `dependency:list` format, npm `--json` scope mapping, Go `go list -m -json` Module.Path/Version).
- **Step 4.1** (§Step 4.1 Denied CVE): RULE-074-01 unconditional BLOCK; CVE scanner sources (OWASP plugin, `npm audit`, `trivy`).
- **Step 4.2** (§Step 4.2 License): allowed-licenses membership; license resolution per stack; `LICENSE_UNRESOLVABLE` WARN fallback.
- **Step 4.3** (§Step 4.3 Min-Version): per-stack constraint matching (JvmConstraint with `*` wildcard support; NpmConstraint exact; GoConstraint prefix).
- **Step 4.4** (§Step 4.4 Max-Version): same matching rules as min-version.
- **Step 4.5** (§Step 4.5 Freshness): age via `mvn versions:display-dependency-updates`; `FRESHNESS_UNAVAILABLE` skip fallback.
- **Step 5** (§Step 5): 4-step classification algorithm (denied-CVE override → raw block-on → scope demotion → final = `min(raw, scope)` by severity); classification matrix example for `test` scope.
- **Step 6** (§Step 6): report path resolution; 6-section template (Header/Summary/Blocking/Warning/Suppressed/Policy Snapshot); exit-code semantics.
- **RULE-074-01** (§RULE-074-01): full hard-block contract (scope/severity-threshold/block-on/patch-availability all bypassed).

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

## Full Protocol

Minimum viable contract above. Detailed YAML schema reference, 6-step procedure (load → detect manifests → resolve dependencies → validate 5 dimensions → classify with D-R10 + D-R11 → report and exit), per-stack constraint matching rules, classification algorithm with denied-CVE hard-block override, and RULE-074-01 contract live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
