---
name: x-test-performance
description: "Stack-aware performance skill: reads QualityConfig.performance SLOs, dispatches to Newman (REST), ghz (gRPC), hyperfine (CLI), Artillery (GraphQL), or custom harness (Socket), compares against governance/baselines/performance-baseline.json, and blocks merge on regression > tolerance."
visibility: public
user-invocable: true
model: sonnet
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
requires-capabilities: []
requires-any:
  - quality.performance.rest
  - quality.performance.grpc
  - quality.performance.cli
  - quality.performance.graphql
  - quality.performance.socket
argument-hint: "<STORY-ID> [--stack rest|grpc|cli|graphql|socket] [--update-baseline] [--smoke-only]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: x-test-performance

Stack-aware CI performance gate. Reads `QualityConfig.performance` from the project YAML,
dispatches to the correct load-testing tool per stack, compares p50/p95/p99 results
against `governance/baselines/performance-baseline.json`, and exits non-zero when
regression exceeds `baseline-tolerance-pct`.

## Triggers

- `/x-test-performance story-0072-0002` — auto-detects stack from `interfaces[]`
- `/x-test-performance story-0072-0002 --stack rest` — force REST dispatch
- `/x-test-performance story-0072-0002 --stack grpc` — force gRPC dispatch
- `/x-test-performance story-0072-0002 --update-baseline` — write new baseline (explicit opt-in)
- `/x-test-performance story-0072-0002 --smoke-only` — run smoke test, skip SLO comparison

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `STORY-ID` | String | Yes | — | story-XXXX-YYYY for report path resolution |
| `--stack` | Enum | No | auto-detect | Override stack: `rest`, `grpc`, `cli`, `graphql`, `socket` |
| `--update-baseline` | Flag | No | false | Write new baseline after successful run |
| `--smoke-only` | Flag | No | false | Execute smoke only; skip SLO comparison and baseline check |

## Exit Codes

| Exit | Code | Meaning |
|------|------|---------|
| 0 | `SUCCESS` | All SLO checks passed (or smoke-only) |
| 1 | `PERF_REGRESSION_DETECTED` | p95 or p99 exceeds baseline + tolerance |
| 2 | `TOOL_NOT_FOUND` | Required load-testing tool not on PATH or container unreachable |
| 3 | `NO_SLO_DECLARED` | `quality.performance.enabled=true` but no SLO block found — exits 0 with WARN |
| 4 | `BASELINE_NOT_FOUND` | Baseline file absent; runs measurement only (no comparison) |
| 50 | `PERF_DISABLED` | `quality.performance.enabled=false`; skill exits 0 silently |

## Activation Condition

`quality.performance.enabled=true` must be set in the project YAML.
Exit `50 PERF_DISABLED` immediately when `enabled=false`.

## Stack Dispatch Matrix

| Stack | Condition | Tool | Container image |
|-------|-----------|------|----------------|
| `rest` | `interfaces[].type = rest` | Newman ≥ 6.0 | `postman/newman:6` |
| `grpc` | `interfaces[].type = grpc` | ghz ≥ 0.120 | `ghz:0.120` |
| `cli` | `architecture.style = library` or `interfaces[].type = cli` | hyperfine ≥ 1.18 | `hyperfine:1.18` |
| `graphql` | `interfaces[].type = graphql` | Artillery ≥ 2.0 | `artilleryio/artillery:2` |
| `socket` | `interfaces[].type = tcp-custom` or `websocket` | custom harness | see KP `performance-socket` |

Auto-detection priority: `grpc > rest > graphql > socket > cli`.
When multiple stacks are active, dispatch once per detected stack; report is merged.

## Workflow

### Step 1 — Read Config

Read `quality.performance.*` from the project YAML (via QualityConfig record):

```
enabled            → if false: exit 50 PERF_DISABLED (silent)
baseline-tolerance-pct  → regression threshold (default 10)
slo.<stack>.p50_ms
slo.<stack>.p95_ms
slo.<stack>.p99_ms
```

When `slo.<stack>` block is absent: emit `WARN no SLO declared — running smoke only`
and set `--smoke-only=true` implicitly. **Exit 0** (non-blocking smoke).

### Step 2 — Detect Stack

If `--stack` flag provided, use it directly.
Otherwise, read `interfaces[].type` and `architecture.style` from project YAML.
Apply dispatch priority from the matrix above.

### Step 3 — Tool Availability Check

For each detected stack, verify the tool is available:

```bash
# REST
docker run --rm postman/newman:6 --version 2>/dev/null || { echo "TOOL_NOT_FOUND: newman"; exit 2; }

# gRPC
docker run --rm ghz:0.120 --version 2>/dev/null || { echo "TOOL_NOT_FOUND: ghz"; exit 2; }

# CLI
docker run --rm hyperfine:1.18 --version 2>/dev/null || { echo "TOOL_NOT_FOUND: hyperfine"; exit 2; }

# GraphQL
docker run --rm artilleryio/artillery:2 --version 2>/dev/null || { echo "TOOL_NOT_FOUND: artillery"; exit 2; }
```

Read the KP for the detected stack for container image + command reference.

### Step 4 — Run Load Test

Dispatch to the tool. Collect p50/p95/p99 per endpoint/method/command.

**REST (Newman):**
```bash
docker run --rm -v "$PWD":/workspace postman/newman:6 run \
  /workspace/performance/collection.json \
  --reporters json \
  --reporter-json-export /workspace/.perf-results.json
```

**gRPC (ghz):**
```bash
docker run --rm -v "$PWD":/workspace ghz:0.120 \
  --proto /workspace/proto/service.proto \
  --call <service>.<method> \
  --duration 30s --rps 50 \
  --output json > .perf-results.json
```

**CLI (hyperfine):**
```bash
docker run --rm -v "$PWD":/workspace hyperfine:1.18 \
  --export-json /workspace/.perf-results.json \
  --runs 100 \
  '<command>'
```

**GraphQL (Artillery):**
```bash
docker run --rm -v "$PWD":/workspace artilleryio/artillery:2 run \
  --output /workspace/.perf-results.json \
  /workspace/performance/artillery.yml
```

**Socket:** See KP `performance-socket` for custom harness commands.

### Step 5 — Parse Results

Extract p50/p95/p99 from `.perf-results.json` for each endpoint/method/command.
Normalize all values to milliseconds. Build internal result map:

```
{ "<endpoint>": { "p50Ms": N, "p95Ms": N, "p99Ms": N, "throughputRps": N } }
```

### Step 6 — Baseline Comparison (skip when --smoke-only or baseline absent)

Read `governance/baselines/performance-baseline.json`. If absent: emit
`WARN baseline not found — recording initial measurement` and proceed to Step 8.

For each endpoint, compute delta vs baseline:

```
deltaP95Pct = ((current.p95Ms - baseline.p95Ms) / baseline.p95Ms) * 100
deltaP99Pct = ((current.p99Ms - baseline.p99Ms) / baseline.p99Ms) * 100
```

When `deltaP95Pct > baseline-tolerance-pct` OR `deltaP99Pct > baseline-tolerance-pct`:
mark endpoint as `REGRESSION` → set `exitCode = 1 PERF_REGRESSION_DETECTED`.

When current p95 > `slo.<stack>.p95_ms` (absolute SLO): mark endpoint as `SLO_VIOLATION`.

### Step 7 — Write Report

Write `ai/epics/epic-XXXX/reports/perf-report-STORY-ID.md` (sanitized — no absolute paths,
hostnames, IP addresses, or credentials):

```markdown
# Performance Report — STORY-ID

## Summary
- Stack: <stack>
- Tool: <tool> <version>
- Status: PASS | FAIL
- SLO compliance: N/M endpoints pass
- Regression count: N

## Results per Endpoint

| Endpoint | p50 ms | p95 ms | p99 ms | Baseline p95 | Delta % | Verdict |
|----------|--------|--------|--------|-------------|---------|---------|
| ...      | ...    | ...    | ...    | ...         | ...     | PASS/FAIL |

## Baseline Comparison

| Threshold | Value | Exceeded |
|-----------|-------|----------|
| tolerance-pct | N% | Yes/No |

## Tooling

- Tool: <name> <version>
- Container: <image>
- Command: <sanitized command>
```

### Step 8 — Update Baseline (only with --update-baseline)

Write new entries to `governance/baselines/performance-baseline.json` (append-only contract):
set `measuredAt` = ISO-8601 timestamp, `gitSha` = current HEAD SHA.
Never overwrite entries from a different git SHA in the same run — append new keys.

## SLO Config Fallbacks

| Field | Default | Source |
|-------|---------|--------|
| `p50_ms` | 100 | QualityConfig.performance.slo.<stack>.p50Ms |
| `p95_ms` | 500 | QualityConfig.performance.slo.<stack>.p95Ms |
| `p99_ms` | 1000 | QualityConfig.performance.slo.<stack>.p99Ms |
| `baseline-tolerance-pct` | 10 | QualityConfig.performance.baselineTolerancePct |

## Tooling Version Pinning

| Tool | Minimum Version | Container image |
|------|----------------|----------------|
| Newman | 6.0 | `postman/newman:6` |
| ghz | 0.120 | `ghz:0.120` |
| hyperfine | 1.18 | `hyperfine:1.18` |
| Artillery | 2.0 | `artilleryio/artillery:2` |

Smoke tests use containers (`--platform=linux/amd64`) — no local binary installation required.

## Error Handling

| Condition | Action |
|-----------|--------|
| `quality.performance.enabled=false` | Exit 50 silently |
| No SLO declared | WARN + smoke-only + exit 0 |
| Tool not on PATH and Docker unavailable | Exit 2 TOOL_NOT_FOUND with install instructions |
| Baseline file absent (first run) | WARN + record measurement; exit 0 |
| Regression detected | Exit 1 PERF_REGRESSION_DETECTED with per-endpoint detail |
| p95 exceeds absolute SLO | Append SLO_VIOLATION to report (non-blocking by default) |

## Integration Notes

| Component | Relationship |
|-----------|-------------|
| `QualityConfig.performance` | Reads enabled, baselineTolerancePct, slo.* |
| `governance/baselines/performance-baseline.json` | Read for comparison; written with `--update-baseline` |
| `x-story-implement` Phase 3 | Invokes this skill as MANDATORY conditional (story-0072-0008) |
| `audit-performance-baseline.sh` | Camada 2 CI gate verifying baseline schema (story-0072-0005) |
| KP `performance-rest` / `performance-grpc` / `performance-cli` / `performance-graphql` / `performance-socket` | Stack-specific tooling reference |

## Review Checklist

- [ ] SLOs read from QualityConfig.performance.slo.*
- [ ] Stack correctly auto-detected from project interfaces
- [ ] Newman dispatched for REST; ghz for gRPC; hyperfine for CLI; Artillery for GraphQL
- [ ] Baseline comparison uses configurable tolerance-pct
- [ ] Report contains no absolute paths, hostnames, or credentials
- [ ] Exit 1 on regression; exit 0 on smoke-only or no-SLO
- [ ] Baseline update gated by explicit `--update-baseline` flag
- [ ] Docker container fallback documented in KP per stack
