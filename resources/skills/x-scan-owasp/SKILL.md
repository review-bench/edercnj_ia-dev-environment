---
name: x-scan-owasp
description: "Automated OWASP Top 10 (2021) verification mapped to ASVS L1/L2/L3 with SARIF output."
user-invocable: true
allowed-tools: Read, Write, Bash, Grep, Glob, Agent
argument-hint: "[--level L1|L2|L3] [--category A01-A10|all] [--report-format markdown|sarif|both]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: OWASP Top 10 Verification (slim — ADR-0012)

## Purpose

Verifies {{PROJECT_NAME}} against the OWASP Top 10 (2021) with verification items mapped to ASVS chapters and levels. Produces per-category pass/fail results, an overall score (0-100), ASVS coverage percentage, and generates both SARIF 2.1.0 and Markdown reports.

## Triggers

- `/x-scan-owasp` — full L1 scan, all categories
- `/x-scan-owasp --level L2` — L2 scan (standard defense)
- `/x-scan-owasp --level L3` — L3 scan (advanced/critical apps)
- `/x-scan-owasp --category A03` — single category scan
- `/x-scan-owasp --report-format sarif` — SARIF output only
- `/x-scan-owasp --report-format markdown` — Markdown only
- `/x-scan-owasp --report-format both` — both formats (default)

## Parameters

| Parameter | Type | Default | Values | Description |
|-----------|------|---------|--------|-------------|
| `--level` | String | L1 | L1, L2, L3 | ASVS verification depth |
| `--category` | String | all | A01-A10, all | OWASP category filter |
| `--report-format` | String | both | markdown, sarif, both | Output format |

## ASVS Level Definitions

| Level | Name | Target | Description |
|-------|------|--------|-------------|
| L1 | Opportunistic | Any application | Minimum verification; automated checks only |
| L2 | Standard | Most applications | Defensive depth; automated + manual review |
| L3 | Advanced | Critical systems (health, finance, infra) | Maximum assurance; comprehensive review |

## OWASP Top 10 → ASVS Mapping

| OWASP Category | ID | ASVS Chapter(s) | Focus Areas |
|----------------|-----|-----------------|-------------|
| Broken Access Control | A01 | V4 | RBAC, path traversal, CORS, IDOR |
| Cryptographic Failures | A02 | V6, V9 | Encryption at rest, TLS, key management |
| Injection | A03 | V5 | Input validation, output encoding, parameterized queries |
| Insecure Design | A04 | V1 | Threat modeling, secure architecture |
| Security Misconfiguration | A05 | V14 | Hardening, default configs, error handling |
| Vulnerable Components | A06 | N/A | **DELEGATED** to `x-audit-dependencies` (RULE-011) |
| Auth Failures | A07 | V2, V3 | Auth mechanisms, session management |
| Software/Data Integrity | A08 | V10 | Deserialization, CI/CD security |
| Logging Failures | A09 | V7 | Structured logging, monitoring |
| SSRF | A10 | V5, V13 | URL validation, internal IP protection |

## Workflow Overview

```text
1. PARSE     -> Validate --level, --category, --report-format
2. LOAD      -> Read .claude/knowledge/security/ ASVS items
3. MAP       -> OWASP→ASVS chapter resolution (see table above)
4. VERIFY    -> Execute L1/L2/L3 checks per category (A01..A05, A07..A10)
5. DELEGATE  -> Skill x-audit-dependencies for A06 (RULE-011 MANDATORY)
6. SCORE     -> Per-category (PASS@≥70%) + overall + grade A..F + ASVS coverage
7. REPORT    -> SARIF 2.1.0 + Markdown to results/security/owasp-scan-YYYY-MM-DD.{sarif.json,md}
```

Full L1/L2/L3 check lists per category (4.1–4.9), SARIF schema, Markdown report template, and CI integration in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1–2** (§Step 1, §Step 2): parameter validation; ASVS knowledge-pack loading from `.claude/knowledge/security/`.
- **Step 3** (§Step 3): full OWASP→ASVS chapter mapping table.
- **Step 4** (§Step 4 sub-sections 4.1–4.9): complete L1/L2/L3 check lists for A01 (V4), A02 (V6/V9), A03 (V5), A04 (V1), A05 (V14), A07 (V2/V3), A08 (V10), A09 (V7), A10 (V5/V13). Each category has 3–4 L1 checks, 3–4 L2 additions, 3–4 L3 additions.
- **Step 5** (§Step 5): A06 delegation via `Skill(x-audit-dependencies)` — **MANDATORY** per Rule 24 (silent omission fails Camada 3 audit).
- **Step 6** (§Step 6.1–6.4): per-category score formula, overall score aggregation, grade mapping (A=90+, F=<50), ASVS coverage percentage.
- **Step 7** (§Step 7.1–7.2): SARIF 2.1.0 schema with `properties.asvsChapter`/`asvsRequirement`/`asvsLevel`/`owaspCategory`/`fixRecommendation`; Markdown report template with Summary table + Per-Category Details + Findings + Scoring.

## Scoring Quick Reference

| Score | Grade | Status |
|-------|-------|--------|
| 90-100 | A | Excellent |
| 80-89 | B | Good |
| 70-79 | C | Passing |
| 50-69 | D | Below threshold |
| 0-49 | F | Critical |

Per-category PASS requires ≥70%. Failing any category → overall NO-PASS, exit code 1.

## Error Handling

| Scenario | Action |
|----------|--------|
| Invalid `--level` value | Error with valid options list |
| Invalid `--category` value | Error with valid options list |
| Knowledge pack not found | Warn, continue with built-in checks |
| `x-audit-dependencies` unavailable | Mark A06 as SKIPPED, note in report |
| Partial scan (some categories fail to verify) | Report verified categories, mark others SKIPPED |
| No source files found | Report "No source files found for verification" |

## Knowledge Pack References

| # | Knowledge Pack | Purpose |
|---|----------------|---------|
| 1 | `.claude/knowledge/security/index.md` | OWASP ASVS verification items |
| 2 | `.claude/knowledge/security/application-security.md` | Detailed ASVS chapter mappings |
| 3 | `.claude/knowledge/security/anti-patterns-java.md` | Java-specific CWE-mapped anti-patterns |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-audit-dependencies` | delegates to (A06) | RULE-011 — Vulnerable Components delegation (MANDATORY per Rule 24) |
| `x-generate-security-dashboard` | consumed by | SARIF output aggregated into security posture dashboard |
| `x-model-threats` | complements | Threat model informs A04 (Insecure Design) checks |

## Full Protocol

Minimum viable contract above. Complete L1/L2/L3 check lists for all 9 verifiable categories, SARIF 2.1.0 output schema, Markdown report template, scoring formulas, and CI integration patterns live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
