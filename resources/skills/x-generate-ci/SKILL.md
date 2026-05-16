---
name: x-generate-ci
description: "Generates or updates CI/CD pipelines per project stack with actionlint validation."
user-invocable: true
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Agent
argument-hint: "[ci|cd|release|security|all] [--monorepo] [--force]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: CI/CD Pipeline Generation (slim — ADR-0012)

## Purpose

Generates or updates CI/CD pipeline configurations for {{PROJECT_NAME}} based on detected project stack. Analyzes existing workflows to avoid duplication, generates customized GitHub Actions workflows for CI, CD, release, and security scanning, validates generated YAML with actionlint, and supports monorepo path-based triggers.

## Triggers

- `/x-generate-ci` — generate all pipelines (default: all)
- `/x-generate-ci ci` — generate CI pipeline (build + test + security scan)
- `/x-generate-ci cd` — generate CD pipeline (deploy staging + production + rollback)
- `/x-generate-ci release` — generate release pipeline (semantic versioning + changelog)
- `/x-generate-ci security` — generate security scan pipeline (scheduled SAST + dependency audit)
- `/x-generate-ci all` — generate all pipeline types
- `/x-generate-ci ci --monorepo` — generate with path-based triggers for monorepo
- `/x-generate-ci ci --force` — overwrite existing workflows

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `type` | Enum | `all` | Pipeline type: ci, cd, release, security, all |
| `--monorepo` | Flag | false | Activate path-based triggers for monorepo |
| `--force` | Flag | false | Overwrite existing workflow files |

## Generated Artifacts

| Pipeline | File | Purpose |
|----------|------|---------|
| CI | `.github/workflows/ci.yml` | Build + test + security scan on `develop`/`release/*`/`hotfix/*`/PRs |
| CD Staging | `.github/workflows/deploy-staging.yml` | Deploy to staging on push to `develop` |
| CD Production | `.github/workflows/deploy-production.yml` | Deploy to production on `main` push or `v*` tags (with approval gate) |
| Rollback | `.github/workflows/rollback.yml` | Manual rollback with version input |
| Release | `.github/workflows/release.yml` | Tag-driven changelog + GitHub Release + artifact publish |
| Security Scan | `.github/workflows/security-scan.yml` | Weekly + push SAST/CodeQL/Semgrep + container scan; SARIF upload |
| Dependency Audit | `.github/workflows/dependency-audit.yml` | Daily CVE + outdated check; auto-create issues for criticals |

## Workflow Overview

```text
1. DETECT   -> Identify language + build tool from config files (pom.xml/package.json/go.mod/...)
                + Dockerfile/docker-compose/Helm/Terraform for deployment-step detection
2. ANALYZE  -> Scan .github/workflows/ for existing files; apply conflict-resolution rules
3. GENERATE -> Render per-type YAML using language-specific setup actions and cache paths
4. VALIDATE -> Run actionlint (fail-open: warn + continue when not installed)
5. REPORT   -> Markdown table of generated/updated files with validation status
```

Per-stack build steps, per-pipeline YAML templates, monorepo path-based trigger logic, and conflict-resolution rules in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): 6-row stack detection table (Java/Maven, Java/Gradle, Node.js, Go, Rust, Python); ancillary detection (Dockerfile, docker-compose, Helm/k8s, Terraform).
- **Step 2** (§Step 2): conflict resolution matrix (file exists same purpose vs different purpose, with/without `--force`).
- **Step 3.1–3.5** (§Step 3): full YAML templates per pipeline kind (CI with build+security jobs, CD staging/prod/rollback trio, release on `v*` tags, scheduled security scan, daily dependency audit); language-specific setup action + cache path + build cmd + test cmd table.
- **Step 4** (§Step 4): actionlint invocation; fail-open warn-and-continue when not installed.
- **Step 5** (§Step 5): full report template with per-pipeline file/status table.
- **Monorepo Support** (§Monorepo Support): path-based trigger YAML; service-directory detection strategy (`services/`, `packages/`, `apps/`).

## Error Handling

| Scenario | Action |
|----------|--------|
| Language not detected | List supported languages, ask user to specify |
| Workflow file exists (no `--force`) | Report "file exists, use --force to overwrite" |
| actionlint not installed | Warn and skip validation (non-blocking) |
| Invalid type argument | Default to "all", warn user |
| No Dockerfile found (CD requested) | Generate CD without container steps, warn user |
| No project config found | Report error with setup instructions |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `devops-engineer` agent | calls | Used for advanced pipeline customization via Agent tool |
| `ci-cd-patterns` KP | reads | Pipeline templates and best practices |
| `x-audit-dependencies` | reads | Dependency audit pipeline references audit commands |
| `x-generate-security-pipeline` | reads | Security pipeline references scanning configurations |

## Knowledge Pack References

| Pack | File | Purpose |
|------|------|---------|
| ci-cd-patterns | `.claude/knowledge/ci-cd-patterns/index.md` | Pipeline templates and best practices |
| ci-cd-patterns | `.claude/knowledge/ci-cd-patterns/github-actions-patterns.md` | GitHub Actions reusable workflows |
| ci-cd-patterns | `.claude/knowledge/ci-cd-patterns/pipeline-security.md` | Security gates in CI pipelines |

## Full Protocol

Minimum viable contract above. Detailed 6-stack detection rules, full YAML templates per pipeline kind, language-specific build matrix, monorepo path-trigger logic, conflict-resolution rules, and report template live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
