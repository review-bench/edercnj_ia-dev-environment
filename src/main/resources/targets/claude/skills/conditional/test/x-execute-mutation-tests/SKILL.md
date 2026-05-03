---
name: x-execute-mutation-tests
description: "Stack-aware mutation testing skill: detects language/framework, dispatches to PIT (Java/Maven or Gradle), Stryker (JS/TS), mutmut (Python), or go-mutesting (Go), enforces quality.mutation.threshold (default 80%) and runtime-cap-min, and blocks merge on score below threshold."
visibility: public
user-invocable: true
model: sonnet
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
requires-capabilities: []
requires-any:
  - quality.mutation.java-pit
  - quality.mutation.stryker
  - quality.mutation.mutmut
  - quality.mutation.go-mutesting
argument-hint: "<STORY-ID> [--stack java|js|python|go] [--dry-run] [--smoke-only]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: No conversational fillers. Start with actionable output.

# Skill: x-execute-mutation-tests

## Purpose

Runs mutation testing scoped to `application + domain` layers, validates the mutation score
against `quality.mutation.threshold`, enforces `quality.mutation.runtime-cap-min`, and
blocks merge when score < threshold. Skips gracefully when no language/framework is declared.

## Stack Dispatch Matrix

| Language / Framework | Tool | Min Version | Container Image |
| :--- | :--- | :--- | :--- |
| Java + Maven | PIT (pitest-maven) | 1.15 | `maven:3.9-eclipse-temurin-21` |
| Java + Gradle | PIT (gradle-pitest) | 1.15 | `eclipse-temurin:21` |
| JS / TypeScript | Stryker | 8.0 | `node:20-slim` |
| Python | mutmut | 2.4 | `python:3.12-slim` |
| Go | go-mutesting | 0.3 | `golang:1.22-alpine` |

When `language` is absent or unrecognized: emit WARN `"no language declared — skipping mutation"`, exit 0.

## Configuration Reference

Parsed from `quality.mutation` block (all fields optional):

```yaml
quality:
  mutation:
    enabled: true
    threshold: 80            # default 80 — minimum mutation score %
    runtime-cap-min: 20      # default 20 — hard abort limit in minutes
    exclude-packages:        # default list; append to extend
      - "**test**"
      - "**generated**"
      - "**infrastructure/adapter**"
    thresholds:
      per-package:           # optional per-package override
        "dev.iadev.domain": 90
        "dev.iadev.adapter": 70
    tool-versions:           # optional pin
      pitest: "1.15.3"
      stryker: "8.2.6"
```

Default exclusions (boilerplate generating trivial surviving mutants):
- `**/test/**` — test code itself
- `**/generated/**` — generated sources
- `**/infrastructure/adapter/**` — JDBC/HTTP adapters killed only by mocks

## Exit Codes

| Code | Numeric | Condition |
| :--- | :--- | :--- |
| `SUCCESS` | 0 | Score ≥ threshold within time cap |
| `MUTATION_SCORE_BELOW_THRESHOLD` | 1 | Score < threshold |
| `MUTATION_RUNTIME_CAP_EXCEEDED` | 2 | Execution time > runtime-cap-min |
| `TOOL_NOT_FOUND` | 3 | Required mutation tool not on PATH / not in classpath |
| `MUTATION_CONFIG_INVALID` | 4 | Invalid config (e.g., path traversal in exclude-packages) |
| `MUTATION_DISABLED` | 50 | `quality.mutation.enabled=false` — early exit, no error |

## Workflow

### Step 1 — Read Config

Load `quality.mutation` from project YAML. If `enabled=false` → exit `MUTATION_DISABLED` (50).

### Step 2 — Validate Config

Reject `exclude-packages` entries containing `../` or absolute paths:
- Emit `MUTATION_CONFIG_INVALID` with message: `"path traversal detected in exclude-packages: <value>"`.

### Step 3 — Detect Stack

Inspect project YAML `language` + `framework` → resolve tooling from dispatch matrix.
If language absent/unrecognized: emit WARN, exit 0.

### Step 4 — Tool Availability Check

Verify the resolved tool is available:
- Java/Maven: `mvn pitest:mutationCoverage --version` dry-run or `mvn help:describe -Dplugin=org.pitest:pitest-maven`
- Others: `<tool> --version`

On failure → exit `TOOL_NOT_FOUND` (3).

### Step 5 — Run Mutation Tests

Invoke tool with timeout = `runtime-cap-min` minutes:

**Java/Maven (PIT):**
```bash
timeout $((runtime_cap_min * 60)) mvn pitest:mutationCoverage \
  -DtargetClasses="dev.iadev.domain.*,dev.iadev.application.*" \
  -DexcludedClasses="$(exclusions_csv)" \
  -Dthreads=4 \
  --no-transfer-progress
```

**Stryker (JS/TS):**
```bash
timeout $((runtime_cap_min * 60)) npx stryker run \
  --mutate "src/domain/**/*.ts,src/application/**/*.ts" \
  --timeoutMS $((runtime_cap_min * 60 * 1000))
```

**mutmut (Python):**
```bash
timeout $((runtime_cap_min * 60)) mutmut run \
  --paths-to-mutate domain/,application/
```

**go-mutesting (Go):**
```bash
timeout $((runtime_cap_min * 60)) go-mutesting ./domain/... ./application/...
```

On timeout signal: exit `MUTATION_RUNTIME_CAP_EXCEEDED` (2), report remaining packages.

### Step 6 — Parse Results

Extract mutation score from tool output:

| Tool | Score field |
| :--- | :--- |
| PIT | `target/pit-reports/*/mutations.xml` → `<totals killed>/<totals total>` |
| Stryker | `reports/mutation/mutation.json` → `metrics.mutationScore` |
| mutmut | `mutmut results` stdout → `Survived:N / Total:T` |
| go-mutesting | stdout `X of Y mutations have been killed` |

Score = `(killed / total) * 100`. Apply per-package overrides when configured.

### Step 7 — Threshold Enforcement

If `score < threshold`:
- Exit `MUTATION_SCORE_BELOW_THRESHOLD` (1).
- Report top-20 surviving mutants with `class/method/line`.
- List per-package scores when per-package thresholds are configured.

### Step 8 — Write Report

Write mutation report to `ai/epics/epic-XXXX/reports/mutation-report-STORY-ID.md`
using template `_TEMPLATE-MUTATION-PLAN.md`.

Report MUST NOT contain:
- Absolute file system paths (sanitize to project-relative)
- Credentials or environment variables
- Hostname or machine-specific tokens

## Security Constraints

- `exclude-packages` values are validated before any filesystem access (Step 2).
- Path traversal characters (`../`, `/` as prefix) → hard reject.
- Reports are sanitized before writing (Step 8 contract above).

## Capability Gate

This skill activates when ANY of the following capabilities is enabled in the project profile:
- `quality.mutation.java-pit` — Java with PIT
- `quality.mutation.stryker` — JS/TS with Stryker
- `quality.mutation.mutmut` — Python with mutmut
- `quality.mutation.go-mutesting` — Go with go-mutesting

When none is present, the skill is excluded from the generated `.claude/skills/` output
(conditional composition via Rule 28 `requires-any`).
