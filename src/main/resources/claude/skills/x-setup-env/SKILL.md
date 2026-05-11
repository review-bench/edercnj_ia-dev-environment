---
name: x-setup-env
description: "Validates and configures local dev environment: stack detection, deps, IDE, build."
user-invocable: true
allowed-tools: Read, Bash, Glob, Grep, Write
argument-hint: "[--check-only] [--fix]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Setup Dev Environment (slim — ADR-0012)

## Purpose

Validates and configures the local development environment for {{PROJECT_NAME}}, detecting the project stack, checking prerequisites, verifying versions, validating IDE configuration, testing database connectivity, running the initial build, and reporting status with fix suggestions.

## Triggers

- `/x-setup-env` — check-only mode (default)
- `/x-setup-env --check-only` — explicitly report status without modifications
- `/x-setup-env --fix` — attempt to fix detected issues

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--check-only` | Flag | true | Report status only, do not modify anything (default) |
| `--fix` | Flag | false | Attempt to correct problems found (non-destructive, never overwrites existing files) |

## Output Contract

Report (stdout) with PASS / FAIL / WARN / SKIP per check:

| Check | What it validates |
|-------|-------------------|
| Language Runtime | `java`/`node`/`go`/`rustc`/`python3` present and version matches `{{ language_version }}` |
| Build Tool | `mvn`/`gradle`/`npm`/`cargo`/`pip` present |
| Docker | `docker --version` + daemon up (when `container != "none"`) |
| Database Client | `psql`/`mysql`/`mongosh` present (when `database_name != "none"`) |
| IDE Configuration | `.editorconfig` + `.vscode/`/`.idea/` directory presence |
| Database Connectivity | `SELECT 1` / ping against configured database |
| Initial Build | `mvn clean compile` / `gradle build` / `npm install && build` / `cargo build` / `pip install -e` |

Overall summary: `X/Y checks passed`.

## Workflow Overview

```text
1. DETECT     -> Identify stack from pom.xml/package.json/go.mod/Cargo.toml/pyproject.toml/build.gradle
2. CHECK      -> Verify runtime + build tool + Docker + DB client presence per stack
3. VERIFY     -> Compare installed version against project required version (WARN on mismatch)
4. IDE        -> .editorconfig + .vscode/ + .idea/ existence checks
5. DATABASE   -> Run SELECT 1 against configured database (skip when database=none)
6. BUILD      -> Run language-specific build command; capture exit code
7. REPORT     -> Markdown table with PASS/FAIL/WARN/SKIP per check + overall counts
```

Each step's full stack-templated bash, version detection regex, IDE configuration checks, per-database connectivity test, and fix-mode behavior live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): 6-row stack detection table; RULE-001 cross-reference with `.claude/rules/01-project-identity.md`.
- **Step 2** (§Step 2): per-stack prerequisite check (Java/Maven+Gradle, Node, Go, Rust, Python, Docker, Postgres/MySQL/MongoDB clients) using `{% if language_name %}` template gates.
- **Step 3** (§Step 3): version-mismatch detection with `grep -oP '\d+' | head -1`; project-required version from `{{ language_version }}`.
- **Step 4** (§Step 4): `.editorconfig` / `.vscode/` / `.idea/` existence checks; PASS/WARN classification.
- **Step 5** (§Step 5): per-database SELECT 1 / ping command (postgresql, mysql, mongodb); skipped when `database_name == "none"`.
- **Step 6** (§Step 6): per-build-tool initial-build command (Maven `clean compile`, Gradle `build -q`, npm `install && build`, cargo `build`, pip/poetry `install`).
- **Step 7** (§Step 7): full Markdown report template.
- **Fix Mode** (§Fix Mode Behavior): 4-row fix-action table; non-destructive guarantee (never overwrites existing files).

## Error Handling

| Scenario | Action |
|----------|--------|
| Config file not found | Report as WARN, suggest creating it |
| Tool not installed | Report as FAIL with installation URL |
| Version mismatch | Report as WARN with upgrade instructions |
| Build failure | Report as FAIL with last 20 lines of error output |
| Database unreachable | Report as FAIL with connection troubleshooting |

## Full Protocol

Minimum viable contract above. Detailed bash for all 7 steps (template-gated per stack/database/container) and fix-mode action table live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
