---
name: x-evaluate-runtime
description: "Evaluates runtime protection (rate limits, WAF, CSP) with SARIF + ASVS scoring."
user-invocable: true
allowed-tools: Read, Write, Bash, Glob, Grep
argument-hint: "--target <url> [--scope all|rate-limit|waf|bot-protection|account-lockout|brute-force|csp|permissions] [--intensity passive|moderate|aggressive] [--login-endpoint /path]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Runtime Protection Eval (slim — ADR-0012)

## Purpose

Evaluate runtime protection controls for {{PROJECT_NAME}} by analyzing active defense mechanisms including rate limiting, WAF rules, bot protection, account lockout, brute force mitigation, CSP enforcement, and permissions policy. Produce SARIF 2.1.0 output with ASVS compliance mapping and a scored Markdown report.

## Triggers

- `/x-evaluate-runtime --target https://app.example.com` — evaluate all dimensions
- `/x-evaluate-runtime --target https://app.example.com --scope rate-limit` — rate limiting only
- `/x-evaluate-runtime --target https://app.example.com --scope waf` — WAF rules only
- `/x-evaluate-runtime --target https://app.example.com --scope account-lockout --login-endpoint /api/auth/login` — account lockout
- `/x-evaluate-runtime --target https://app.example.com --intensity passive` — observe headers only
- `/x-evaluate-runtime --target https://app.example.com --intensity aggressive` — full volume testing (local/dev only)

## Parameters

| Parameter | Type | Required | Default | Validation | Example |
|-----------|------|----------|---------|------------|---------|
| `--target` | URL | Yes | — | Valid HTTP/HTTPS URL | `https://app.example.com` |
| `--scope` | Enum | No | `all` | `all`, `rate-limit`, `waf`, `bot-protection`, `account-lockout`, `brute-force`, `csp`, `permissions` | `rate-limit` |
| `--intensity` | Enum | No | `moderate` | `passive`, `moderate`, `aggressive` | `passive` |
| `--login-endpoint` | Path | No | — | Relative path starting with `/` | `/api/auth/login` |

## Intensity Levels

| Level | Behavior | Allowed Environments |
|-------|----------|---------------------|
| **passive** | Observe headers and configurations only; no payloads sent | All environments |
| **moderate** | Send non-destructive test payloads (default) | All environments |
| **aggressive** | Test limits with higher volume of requests | Local and dev only |

`--intensity=aggressive` in production is automatically downgraded to `passive` with a warning.

## Workflow Overview

```text
1. VALIDATE     -> Verify --target URL, reachability, scope, login-endpoint dependency
2. INTENSITY    -> Resolve effective intensity (downgrade aggressive→passive in prod)
3. BASELINE     -> Single GET request to capture security-relevant headers
4. DIMENSIONS   -> Execute checks per scope (Rate-Limit, WAF, Bot, Lockout, BruteForce, CSP, Permissions)
5. SCORE        -> Per-dimension (PROTECTED@80+/PARTIAL@40+/UNPROTECTED) + overall + grade A..F
6. REPORT       -> SARIF 2.1.0 + Markdown to results/security/runtime-protection-{ts}.{sarif.json,md}
```

7 dimension check matrices, SARIF/Markdown templates, and ASVS mapping in [`references/full-protocol.md`](references/full-protocol.md):

- **Steps 1–3** (§Step 1/2/3): parameter validation; intensity downgrade rule; baseline GET probe with header extraction (`X-RateLimit-*`, CSP, Permissions-Policy, X-Content-Type-Options, HSTS).
- **Step 4 sub-sections 4.1–4.7** (§Step 4): full passive/moderate/aggressive check matrices for Rate Limiting (V4.3 — 100-req burst test, Retry-After validation), WAF/OWASP CRS (V5.1 — SQLi/XSS/path-traversal/cmd-inject non-destructive payloads), Bot Protection (V13.1 — CAPTCHA markers, challenge pages), Account Lockout (V2.2 — 10-attempt sequence, CRITICAL finding on no-lockout), Brute Force (V2.2/V11.1 — timing-attack mitigation), CSP Enforcement (V14.4 — directive severity table), Permissions Policy (V14.4 — feature restriction matrix).
- **Step 5** (§Step 5.1–5.2): per-dimension scoring formula and status thresholds; overall scoring with severity weights (CRITICAL=10, HIGH=5, MEDIUM=2, LOW=1); grade mapping (A=90+, F<60).
- **Step 6** (§Step 6.1–6.2): SARIF 2.1.0 output schema with `properties.dimension`/`asvs-ref`/`severity`/`fix-recommendation`; Markdown report template with Dimension Summary table + Critical Gaps + Detailed Findings + Recommendations.
- **ASVS Mapping Summary** (§ASVS Mapping Summary): full dimension→ASVS chapter/section/requirements crosswalk.

## Scoring Quick Reference

| Dimension Score | Status |
|-----------------|--------|
| 80–100 | PROTECTED |
| 40–79 | PARTIAL |
| 0–39 | UNPROTECTED |
| N/A | SKIPPED |

| Overall Score | Grade |
|---------------|-------|
| 90-100 | A |
| 80-89 | B |
| 70-79 | C |
| 60-69 | D |
| 0-59 | F |

Overall: `overallScore = max(0, 100 - sum(severityWeight × findingCount))`.

## Error Handling

| Scenario | Action |
|----------|--------|
| Target unreachable | Emit `"Target unreachable: {url}"`, abort with no score |
| Connection timeout | Retry once with doubled timeout, then report error |
| Target returns 5xx | Report as finding (server error under load) |
| Login endpoint not provided for `--scope=account-lockout` | Warn and skip account-lockout dimension |
| Aggressive in production | Downgrade to passive, emit warning |
| Partial dimension failure | Complete other dimensions, mark failed as SKIPPED |
| SSL certificate error | Warn and continue with `--insecure` flag |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-generate-security-dashboard` | called-by | Dashboard aggregates runtime protection results |
| `x-evaluate-hardening` | complements | Hardening evaluates static config; runtime evaluates live behavior |
| `x-run-dast` | complements | DAST tests vulnerabilities; runtime tests defensive controls |

## Knowledge Pack References

| Knowledge Pack | Usage |
|----------------|-------|
| `.claude/knowledge/security/` | OWASP ASVS compliance mapping, remediation recommendations |
| `.claude/knowledge/security/application-security.md` | Security headers and CSP guidance |

## Full Protocol

Minimum viable contract above. Detailed 7-dimension check matrices (passive/moderate/aggressive per dimension), non-destructive test payloads, scoring formulas, SARIF 2.1.0 schema, and Markdown report template live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
