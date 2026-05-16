---
name: x-evaluate-hardening
description: "Evaluates hardening posture (headers, TLS, CORS, cookies) with weighted SARIF scoring."
user-invocable: true
allowed-tools: Read, Write, Bash, Grep, Glob, Agent
argument-hint: "--target <url> [--scope all|headers|tls|cors|cookies|errors|limits|disclosure] [--benchmark cis|owasp] [--level L1|L2|L3]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Application Hardening Eval (slim — ADR-0012)

## Purpose

Evaluates the defensive posture of {{PROJECT_NAME}} against recognized benchmarks (CIS, OWASP). Analyzes 7 hardening dimensions with weighted scoring: HTTP security headers, TLS configuration, CORS policy, cookie security, error handling, input limits, and information disclosure. Produces SARIF 2.1.0 output and a Markdown report with per-dimension scores.

## Triggers

- `/x-evaluate-hardening --target <url>` — full hardening evaluation (all dimensions, OWASP benchmark, L1)
- `/x-evaluate-hardening --target <url> --scope headers` — evaluate HTTP security headers only
- `/x-evaluate-hardening --target <url> --scope tls` — evaluate TLS configuration only
- `/x-evaluate-hardening --target <url> --benchmark cis` — evaluate against CIS benchmark
- `/x-evaluate-hardening --target <url> --level L2` — evaluate against ASVS Level 2

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--target` | URL | Yes | — | Target application URL (HTTP/HTTPS) |
| `--scope` | Enum | No | `all` | Dimension filter: all, headers, tls, cors, cookies, errors, limits, disclosure |
| `--benchmark` | Enum | No | `owasp` | Benchmark: cis (infrastructure-focused) or owasp (application-focused) |
| `--level` | Enum | No | `L1` | ASVS verification level: L1, L2, L3 |

## Dimension Weights (overall score)

| Dimension | Weight |
|-----------|--------|
| headers | 0.25 |
| tls | 0.20 |
| cors | 0.15 |
| cookies | 0.15 |
| errors | 0.10 |
| limits | 0.10 |
| disclosure | 0.05 |

`overall_score = sum(dimension_score × weight) / sum(applicable_weights)` — `--scope` filtering only counts included dimensions in the denominator.

## Workflow Overview

```text
1. VALIDATE   -> Parse and validate CLI parameters; abort on unreachable target
2. CONFIGURE  -> Load benchmark checks (OWASP|CIS) at ASVS level (L1|L2|L3)
3. PROBE      -> curl response headers + OPTIONS preflight + openssl TLS + 404 error probe
4. EVALUATE   -> 7 dimensions per --scope filter (headers/tls/cors/cookies/errors/limits/disclosure)
5. SCORE      -> Per-dimension % + weighted overall + grade A..F
6. REPORT     -> SARIF 2.1.0 + Markdown to results/security/hardening-eval-YYYY-MM-DD.{sarif.json,md}
```

7 dimension check matrices (with CIS/OWASP/ASVS-level applicability columns), SARIF rule-ID convention per dimension, Markdown heatmap template, and CI integration snippets in [`references/full-protocol.md`](references/full-protocol.md):

- **Steps 1–3** (§Step 1/2/3): CLI validation, benchmark loading (OWASP ASVS V14 + Secure Headers vs CIS web-server hardening), ASVS level mapping (L1/L2/L3), probe commands (`curl -sI`, `curl -X OPTIONS`, `openssl s_client`, 404 probe).
- **Step 4 sub-sections 4.1–4.7** (§Step 4): full dimension check matrices for HTTP Security Headers (V14 — HSTS/XFO/XCTO/CSP/CORP/Permissions-Policy/Referrer-Policy with severity), TLS Configuration (TLS 1.2+/AEAD ciphers/OCSP/cert chain/HSTS preload), CORS Policy (origin whitelist/credentials/preflight cache/exposed headers/methods), Cookie Security (Secure/HttpOnly/SameSite/Path/__Secure-prefix), Error Handling (stack-trace suppression/RFC 7807/custom pages/debug mode), Input Limits (body/URL/header/upload size + rate-limit headers), Information Disclosure (Server/X-Powered-By/version/directory listing).
- **Step 5** (§Step 5): dimension scoring formula and grade mapping (A=90+, F<60).
- **Step 6** (§Step 6.1–6.2): SARIF schema with `properties.dimension/benchmark/asvsLevel/severity/fixRecommendation`; SARIF rule-ID prefix per dimension (`HARDEN-HDR-NNN`, `HARDEN-TLS-NNN`, etc.); Markdown report with Score Heatmap + Findings + Recommendations.
- **CI Integration** (§CI Integration): GitHub Actions snippet (with SARIF upload via gh api) and GitLab CI snippet (sast artifacts).

## Scoring Quick Reference

| Score | Grade |
|-------|-------|
| 90-100 | A |
| 80-89 | B |
| 70-79 | C |
| 60-69 | D |
| 0-59 | F |

## Error Handling

| Scenario | Action |
|----------|--------|
| Target unreachable | Report error with URL, no score calculated |
| HTTPS target with invalid certificate | Warn, proceed with evaluation, note in findings |
| Non-HTTP target | Reject with "Target must be HTTP or HTTPS URL" |
| TLS probing unavailable | Skip TLS dimension, adjust weights |
| Partial response | Evaluate available data, note gaps in report |
| Unknown scope value | Reject with "Invalid scope: <value>" |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `security-engineer` agent | calls | Used for in-depth analysis via Agent tool |
| `x-generate-security-dashboard` | reads | Dashboard aggregates results from this skill |
| `x-scan-owasp` | complementary | OWASP scan covers application-level vulnerabilities; hardening covers infrastructure posture |
| `x-evaluate-runtime` | complements | Hardening evaluates static config; runtime evaluates live behavior |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| security | `.claude/knowledge/security/security-principles.md` | Data classification, input validation, fail-secure patterns |
| security | `.claude/knowledge/security/application-security.md` | OWASP Top 10, security headers, secrets management |
| security | `.claude/knowledge/security/cryptography.md` | TLS requirements, cipher suites, certificate management |

## Full Protocol

Minimum viable contract above. Detailed 7-dimension check matrices (CIS + OWASP applicability per check, severity per finding), probe commands, scoring formula, SARIF 2.1.0 schema with rule-ID convention, Markdown report template with Score Heatmap, and CI integration snippets live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
