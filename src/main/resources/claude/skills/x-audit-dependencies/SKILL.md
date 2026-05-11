---
name: x-audit-dependencies
description: "Audits dependencies for CVEs, outdated versions, and license issues per stack."
user-invocable: true
allowed-tools: Read, Write, Bash, Grep, Glob
argument-hint: "[--scope all|vulnerabilities|outdated|licenses|sbom|license-report|tree] [--policy]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Dependency Audit (slim — ADR-0012)

## Purpose

Audits all dependencies of {{PROJECT_NAME}} for security vulnerabilities, outdated versions, and license compliance. Generates a structured report with severity-categorized findings and remediation recommendations. Also supports SBOM generation, license attribution reports, and dependency tree visualization.

## Triggers

- `/x-audit-dependencies` — full audit (vulnerabilities + outdated + licenses)
- `/x-audit-dependencies --scope vulnerabilities` — security vulnerabilities only
- `/x-audit-dependencies --scope outdated` — outdated packages only
- `/x-audit-dependencies --scope licenses` — license compliance only
- `/x-audit-dependencies --scope sbom` — generate CycloneDX SBOM only
- `/x-audit-dependencies --scope license-report` — generate license attribution report
- `/x-audit-dependencies --scope tree` — generate dependency tree visualization
- `/x-audit-dependencies --scope all --policy` — full audit + dependency policy validation (requires `dependencies.policy.enabled: true`)

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--scope` | Enum | `all` | Audit scope: all, vulnerabilities, outdated, licenses, sbom, license-report, tree |
| `--policy` | Boolean | `false` | After standard audit, invoke `x-validate-dependency-policy` to enforce `DependencyPolicyConfig` (EPIC-0074, Rule 32) |

## Output Contract

| Scope | Artifact |
|-------|----------|
| `vulnerabilities` / `outdated` / `licenses` / `all` | `results/audits/dependency-audit-YYYY-MM-DD.md` |
| `sbom` | `results/audits/sbom-YYYY-MM-DD.json` (CycloneDX 1.6) + summary `.md` |
| `license-report` | `results/audits/license-attribution-YYYY-MM-DD.md` |
| `tree` | `results/audits/dependency-tree-YYYY-MM-DD.md` |
| `--policy` (any scope) | Plus `x-validate-dependency-policy` report; exit non-zero on `DEP_POLICY_BLOCK` |

Exit code: 0 on success; non-zero when audit tool fails OR `--policy` validation blocks.

## Workflow Overview

```text
1. DETECT     -> Identify build tool ({{BUILD_TOOL}}) and lock file
2. AUDIT      -> Run per-stack commands for vulnerabilities / outdated / licenses
3. PARSE      -> Extract package + version + CVE + severity + fix recommendation
4. CATEGORIZE -> Assign CRITICAL / HIGH / MEDIUM / LOW per CVSS + license type
5. REPORT     -> Markdown to results/audits/dependency-audit-YYYY-MM-DD.md
[6. POLICY]   -> Skill x-validate-dependency-policy when --policy is set

`--scope=all` covers ONLY the 3 standard dimensions (vulnerabilities + outdated + licenses) — it does NOT include SBOM, license-report, or tree. Each of those is opt-in via its own dedicated `--scope` value, with its own artifact path (see Output Contract above).
```

Per-stack command tables (npm/yarn/pnpm/maven/gradle/cargo/pip/poetry/go), parse contracts, SBOM/license-report/tree sub-workflows, risk scoring, and full report templates live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1 Detect Build Tool): build-tool→lock-file mapping table (9 stacks).
- **Step 2** (§Step 2 Run Audit Commands): per-stack command tables for vulnerabilities (`npm audit`/`mvn ossindex`/`govulncheck`/etc.), outdated (`npm outdated`/`mvn versions:display`/`go list -m -u`/etc.), and license-check (`license-checker`/`mvn license:third-party-report`/`go-licenses report`/etc.).
- **Step 3** (§Step 3 Parse Results): extracted fields per dimension (package, CVE, severity, fixed version, dep chain for vulns; version delta + breaking-change risk for outdated; SPDX + copyleft risk for licenses).
- **Step 4** (§Step 4 Categorize Findings): CVSS-anchored severity table (CRITICAL=exploited RCE, HIGH=CVSS≥7.0 OR GPL in proprietary, MEDIUM=CVSS 4.0-6.9 OR LGPL OR major-behind, LOW=CVSS<4.0 OR minor/patch).
- **Step 5** (§Step 5 Generate Report): full Markdown template with Summary + Vulnerabilities + Outdated + License Issues + Recommendations sections.
- **SBOM Generation** (§SBOM Generation): per-stack CycloneDX commands; required component fields (name/version/purl/licenses/hashes/scope); summary template.
- **License Attribution Report** (§License Attribution Report): permissive/weak-copyleft/strong-copyleft classification; copyleft review table.
- **Dependency Tree Visualization** (§Dependency Tree Visualization): per-stack tree commands; 5-factor risk scoring (CVE 40% / depth 20% / maintainer 15% / license 15% / popularity 10%); ASCII tree output with per-node risk.
- **`--policy` Flag** (§`--policy` Flag): standard audit → `Skill(x-validate-dependency-policy)` → exit-code propagation; no-op when policy capability not declared.

## Error Handling

| Scenario | Action |
|----------|--------|
| Audit tool not installed | Suggest installation command, continue with available tools |
| No lock file found | Warn and attempt audit without lock file |
| Audit command fails | Report error, continue with other dimensions |
| No dependencies found | Report "No dependencies found" |
| Offline mode | Skip vulnerability check, proceed with outdated and license |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-audit-supply-chain` | complementary | Handles deeper supply chain risks (maintainer, typosquatting, SLSA) |
| `x-generate-ci` | called-by | Dependency audit pipeline references audit commands from this skill |
| `x-generate-security-dashboard` | reads | Dashboard aggregates results from this skill |
| `x-validate-dependency-policy` | delegates-to | When `--policy` flag is set, delegates policy enforcement after standard audit completes |

## Full Protocol

Minimum viable contract above. Detailed per-stack command tables (9 build tools × 3 audit dimensions + SBOM + tree), parse contracts, full Markdown report templates, risk scoring formula, and `--policy` integration semantics live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
