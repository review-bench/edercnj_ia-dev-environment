---
name: x-audit-supply-chain
description: "Supply-chain audit: maintainer risk, typosquatting, EPSS, SLSA; SARIF + report."
user-invocable: true
allowed-tools: Read, Write, Bash, Grep, Glob
argument-hint: "[--depth shallow|deep] [--include-dev-deps] [--risk-threshold 0-100] [--focus all|maintainer|typosquatting|phantom|age|epss|slsa]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Enhanced Supply Chain Audit (slim — ADR-0012)

## Purpose

Performs advanced supply chain security analysis for {{PROJECT_NAME}} that complements (does NOT replace) `x-audit-dependencies`. While `x-audit-dependencies` focuses on known CVEs, outdated versions, and license compliance, this skill identifies deeper supply chain risks: single-maintainer dependencies, typosquatting suspects, phantom dependencies, stale packages, EPSS exploit prediction, and SLSA integrity assessment.

## Relationship with x-audit-dependencies

| Capability | x-audit-dependencies | x-audit-supply-chain |
|------------|-------------------|----------------------|
| Known CVEs | Yes | No (defers to x-audit-dependencies) |
| Outdated versions | Yes | No (defers to x-audit-dependencies) |
| License compliance | Yes | Extends with copyleft risk scoring |
| SBOM generation | Yes (CycloneDX) | No (defers to x-audit-dependencies) |
| Maintainer risk | No | Yes (bus factor analysis) |
| Typosquatting | No | Yes (Levenshtein distance) |
| Phantom dependencies | No | Yes (AST scan vs manifest diff) |
| Dependency age | No | Yes (registry metadata) |
| EPSS scoring | No | Yes (FIRST.org API) |
| SLSA assessment | No | Yes (provenance verification) |
| Risk scoring | Basic (severity only) | Multi-dimensional (5 weighted factors) |

Both skills can be executed independently. Results from both feed into the security dashboard.

## Triggers

- `/x-audit-supply-chain` — full supply chain audit (all 6 capabilities)
- `/x-audit-supply-chain --depth deep` — deep analysis including transitive dependencies
- `/x-audit-supply-chain --focus maintainer` — maintainer risk analysis only
- `/x-audit-supply-chain --focus typosquatting` — typosquatting detection only
- `/x-audit-supply-chain --focus phantom` — phantom dependency detection only
- `/x-audit-supply-chain --focus age` — dependency age analysis only
- `/x-audit-supply-chain --focus epss` — EPSS exploit prediction only
- `/x-audit-supply-chain --focus slsa` — SLSA level assessment only
- `/x-audit-supply-chain --risk-threshold 50` — filter findings below score 50
- `/x-audit-supply-chain --include-dev-deps` — include dev dependencies

## Parameters

| Parameter | Type | Default | Validation | Description |
|-----------|------|---------|------------|-------------|
| `--depth` | String | shallow | enum: shallow, deep | shallow = direct deps only; deep = includes transitive |
| `--include-dev-deps` | boolean | false | — | Include development dependencies in analysis |
| `--risk-threshold` | int | 0 | 0-100 | Exclude findings with risk score below this value |
| `--focus` | String | all | enum: all, maintainer, typosquatting, phantom, age, epss, slsa | Analyze specific risk category only |

## Output Contract

| Artifact | Path |
|----------|------|
| SARIF report | `results/audits/supply-chain-audit-YYYY-MM-DD.sarif.json` |
| Markdown report | `results/audits/supply-chain-audit-YYYY-MM-DD.md` |
| Overall score | 0-100 with grade A..F |
| Findings | Per-rule (`SCA-MAINT-001`, `SCA-TYPO-001`, etc.) with severity + recommendation |

## Workflow Overview

```text
1. DETECT   -> Parse manifest per build tool ({{BUILD_TOOL}})
2. RESOLVE  -> Build dependency graph (direct + transitive when --depth=deep)
3. ANALYZE  -> 6 capabilities: Maintainer / Typosquatting / Phantom / Age / EPSS / SLSA
4. SCORE    -> Weighted formula (CVE 40% / Depth 20% / Maintainer 15% / License 15% / Popularity 10%)
5. FILTER   -> Apply --focus, --risk-threshold, --include-dev-deps, --depth
6. REPORT   -> SARIF 2.1.0 + Markdown
```

Per-stack manifest/graph commands, 6 analysis capabilities with scoring rules, registry API endpoints, severity classification, and report templates in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): manifest+lock-file mapping per build tool (9 stacks).
- **Step 2** (§Step 2): per-stack dependency-graph commands (`npm ls --all`, `mvn dependency:tree`, `cargo tree`, etc.).
- **Step 3.1** (§Step 3.1 Maintainer Risk): registry API endpoints per ecosystem; bus-factor scoring (1→100, 2-3→50, 4+→0).
- **Step 3.2** (§Step 3.2 Typosquatting): Levenshtein distance < 2 against top-N popular packages; distance=1→100 CRITICAL, same-name-different-scope→75 HIGH.
- **Step 3.3** (§Step 3.3 Phantom): per-language import-pattern scan vs manifest cross-reference; phantom→75 MEDIUM.
- **Step 3.4** (§Step 3.4 Age): registry last-release timestamp; >2y→100, 1-2y→75, 6-12m→25, <6m→0.
- **Step 3.5** (§Step 3.5 EPSS): FIRST.org API per CVE; ≥0.5→100 CRITICAL.
- **Step 3.6** (§Step 3.6 SLSA): provenance/signing/reproducible-build verification; SLSA 0→100, SLSA 3→25.
- **Step 4** (§Step 4): full weighted risk formula and 5-bucket severity classification (CRITICAL≥80, INFO<20).
- **Step 5** (§Step 5): 4-stage filter chain (focus → risk-threshold → dev-deps → depth).
- **Step 6.1** (§Step 6.1): SARIF 2.1.0 schema with 6 rule IDs (`SCA-{MAINT,TYPO,PHANTOM,AGE,EPSS,SLSA}-NNN`).
- **Step 6.2** (§Step 6.2): full Markdown report template with Risk Distribution + Findings + Scoring Methodology + Recommendations.

## Error Handling

| Scenario | Action |
|----------|--------|
| Registry API unavailable | Skip that capability, continue with others, note in report |
| No manifest file found | Report error and exit |
| No dependencies declared | Report score 100, grade A, zero findings |
| EPSS API unavailable | Skip EPSS scoring, continue with other capabilities |
| AST scan fails for language | Skip phantom detection, note unsupported language |
| Rate limited by registry | Implement exponential backoff, partial results if timeout |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-audit-dependencies` | complementary | Handles CVEs, outdated versions, licenses, and SBOM generation |
| `x-generate-security-dashboard` | reads | Dashboard aggregates results from this skill |
| `x-generate-ci` | called-by | Security pipeline may invoke supply chain audit |

## Knowledge Pack References

| Pack | File | Purpose |
|------|------|---------|
| security | `.claude/knowledge/security/sarif-template.md` | SARIF 2.1.0 output format |
| security | `.claude/knowledge/security/security-scoring.md` | Scoring model and grade scale |
| security | `.claude/knowledge/security/supply-chain-hardening.md` | SLSA framework and hardening patterns |
| security | `.claude/knowledge/security/sbom-generation-guide.md` | SBOM format reference |

## Full Protocol

Minimum viable contract above. Detailed per-stack manifest/graph commands, 6 analysis capabilities (Maintainer/Typosquatting/Phantom/Age/EPSS/SLSA) with registry API endpoints and scoring rules, weighted risk formula, SARIF schema with 6 rule IDs, and full Markdown report template live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
