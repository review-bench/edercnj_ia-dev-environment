---
name: x-generate-security-pipeline
description: "Generate CI/CD pipeline configurations with conditional security stages based on SecurityConfig flags. Support GitHub Actions, GitLab CI, and Azure DevOps with minimal and full stage modes, configurable severity thresholds, and SARIF artifact upload."
user-invocable: true
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Agent
argument-hint: "[--ci github|gitlab|azure] [--stages all|minimal] [--trigger push|pr|schedule] [--fail-on-findings true|false] [--severity-threshold CRITICAL|HIGH|MEDIUM]"
context-budget: light
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## Triggers

```
/x-generate-security-pipeline                          — GitHub Actions, all stages, pr trigger (default)
/x-generate-security-pipeline --ci github              — GitHub Actions security pipeline
/x-generate-security-pipeline --ci gitlab              — GitLab CI security pipeline
/x-generate-security-pipeline --ci azure               — Azure DevOps security pipeline
/x-generate-security-pipeline --stages minimal         — SAST + secret scan + dependency audit only
/x-generate-security-pipeline --stages all             — full pipeline with all 9 conditional stages
/x-generate-security-pipeline --trigger schedule       — scheduled trigger
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--ci` | Enum | `github` | CI platform: `github` (GitHub Actions), `gitlab` (GitLab CI), `azure` (Azure DevOps) |
| `--stages` | Enum | `all` | Stage mode: `all` (9 conditional stages) or `minimal` (3 stages: SAST, secret scan, dependency audit) |
| `--trigger` | Enum | `pr` | Pipeline trigger: `push`, `pr`, `schedule` |
| `--fail-on-findings` | Boolean | `true` | Exit non-zero when findings at or above severity threshold detected |
| `--severity-threshold` | Enum | `HIGH` | Minimum severity to fail: `CRITICAL`, `HIGH`, `MEDIUM` |

## Output Contract

| Platform | Output File |
|----------|-------------|
| `github` | `.github/workflows/security.yml` |
| `gitlab` | `.gitlab-ci.yml` (security stages appended) |
| `azure` | `azure-pipelines-security.yml` |

All platforms support SARIF artifact upload for GitHub Advanced Security integration. Each enabled stage is a CI job wrapping the corresponding atomic scanning skill reference (RULE-011 — Composability). 9 stages available; conditions and skill references:

| Stage | Skill | Condition |
|-------|-------|-----------|
| Secret Scan | x-scan-secrets | `security.scanning.secrets = true` |
| SAST | x-run-sast | `security.scanning.sast = true` |
| Dependency Audit | x-audit-dependencies | Always |
| SonarQube | x-run-sonar-security | `security.scanning.sonar = true` |
| Container Scan | x-scan-container-security | `infrastructure.container != none` |
| DAST Passive | x-run-dast | `security.scanning.dast = true` |
| OWASP Scan | x-scan-owasp | `security.frameworks contains "owasp"` |
| Hardening Eval | x-evaluate-hardening | `security.scanning.hardening = true` |
| Quality Gate | x-run-sonar-security | `security.scanning.sonar = true` |

## Error Envelope

| Scenario | Action |
|----------|--------|
| Unknown CI platform | Default to `github`; warn user |
| No SecurityConfig flags set | Generate Dependency Audit only (always enabled) |
| All conditions disabled | Generate pipeline with only Dependency Audit |
| Container scan without Dockerfile | Exclude container scan; add to `disabledStages` |
| Invalid `--severity-threshold` | Default to `HIGH`; warn user |
| Invalid `--stages` mode | Default to `all`; warn user |
| Validation tool missing | Warn and skip validation (non-blocking) |

## Full Protocol

> Complete per-platform YAML templates (GitHub Actions, GitLab CI, Azure DevOps), stage-by-stage configuration details (SecurityConfig key→stage mapping, SARIF upload for each platform, caching strategies), severity threshold semantics, tool mapping (Semgrep for SAST, Trivy for container scan, Syft/Grype for dependency audit, detect-secrets for secret scan), composability notes (RULE-011), and integration notes in [`references/full-protocol.md`](references/full-protocol.md).
