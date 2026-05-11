---
name: x-execute-performance-tests
description: "Conditional performance-tests gate: SLA budget validation per quality.performance."
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

# Skill: x-execute-performance-tests (slim — ADR-0012)

Stack-aware CI performance gate. Reads `QualityConfig.performance` from the project YAML, dispatches to the correct load-testing tool per stack, compares p50/p95/p99 results against `governance/baselines/performance-baseline.json`, and exits non-zero when regression exceeds `baseline-tolerance-pct`.

## Triggers

- `/x-execute-performance-tests story-0072-0002` — auto-detects stack from `interfaces[]`
- `/x-execute-performance-tests story-0072-0002 --stack rest` — force REST dispatch
- `/x-execute-performance-tests story-0072-0002 --stack grpc` — force gRPC dispatch
- `/x-execute-performance-tests story-0072-0002 --update-baseline` — write new baseline (explicit opt-in)
- `/x-execute-performance-tests story-0072-0002 --smoke-only` — run smoke test, skip SLO comparison

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

`quality.performance.enabled=true` must be set in the project YAML. Exit `50 PERF_DISABLED` immediately when `enabled=false`.

## Stack Dispatch Matrix

| Stack | Condition | Tool | Container image |
|-------|-----------|------|----------------|
| `rest` | `interfaces[].type = rest` | Newman ≥ 6.0 | `postman/newman:6` |
| `grpc` | `interfaces[].type = grpc` | ghz ≥ 0.120 | `ghz:0.120` |
| `cli` | `architecture.style = library` or `interfaces[].type = cli` | hyperfine ≥ 1.18 | `hyperfine:1.18` |
| `graphql` | `interfaces[].type = graphql` | Artillery ≥ 2.0 | `artilleryio/artillery:2` |
| `socket` | `interfaces[].type = tcp-custom` or `websocket` | custom harness | see KP `performance-socket` |

Auto-detection priority: `grpc > rest > graphql > socket > cli`. When multiple stacks are active, dispatch once per detected stack; report is merged.

## Workflow Overview

```text
1. CONFIG       -> Read quality.performance.{enabled, baseline-tolerance-pct, slo.<stack>.*}; exit 50 when disabled
2. DETECT       -> --stack OR auto-detect from interfaces[].type / architecture.style; priority gRPC>REST>GraphQL>Socket>CLI
3. TOOL_CHECK   -> docker run --rm <image> --version; exit 2 TOOL_NOT_FOUND on failure
4. RUN          -> Newman / ghz / hyperfine / Artillery / custom socket harness; collect p50/p95/p99 to .perf-results.json
5. PARSE        -> Normalize to ms; build {endpoint: {p50Ms, p95Ms, p99Ms, throughputRps}}
6. COMPARE      -> Read baseline.json; deltaP95/P99 vs baseline; mark REGRESSION when >tolerance-pct; SLO_VIOLATION when p95 > slo.p95_ms
7. REPORT       -> ai/epics/epic-XXXX/reports/perf-report-STORY-ID.md (sanitized — no abs paths/hostnames/credentials)
8. BASELINE     -> --update-baseline writes new entries with measuredAt + gitSha; append-only contract
```

Detailed bash for each step, per-tool dispatch commands, baseline schema, sanitized report template, and SLO/tolerance defaults live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): full `QualityConfig.performance` field list; no-SLO degraded-mode WARN with smoke-only fallback.
- **Step 2** (§Step 2): dispatch-priority resolution; multi-stack merge semantics.
- **Step 3** (§Step 3): `docker run --rm <image> --version` per stack; exit-2 on each failure.
- **Step 4** (§Step 4): per-tool full invocation (Newman `run --reporters json`; ghz `--proto --call --duration --rps`; hyperfine `--export-json --runs 100`; Artillery `run --output`).
- **Step 5** (§Step 5): result-map shape `{endpoint: {p50Ms, p95Ms, p99Ms, throughputRps}}`.
- **Step 6** (§Step 6): delta computation formula; REGRESSION + SLO_VIOLATION classification rules.
- **Step 7** (§Step 7): full sanitized Markdown report template (Summary + Results per Endpoint + Baseline Comparison + Tooling).
- **Step 8** (§Step 8): append-only baseline contract; ISO-8601 timestamp + git SHA per entry.
- **SLO Config Fallbacks** (§SLO Config Fallbacks): default p50/p95/p99 (100/500/1000 ms) and tolerance (10%).
- **Tooling Version Pinning** (§Tooling Version Pinning): minimum versions + container images per tool.

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
| `x-implement-story` Phase 3 | Invokes this skill as MANDATORY conditional (story-0072-0008) |
| `audit-performance-baseline.sh` | Camada 2 CI gate verifying baseline schema (story-0072-0005) |
| KP `performance-rest` / `performance-grpc` / `performance-cli` / `performance-graphql` / `performance-socket` | Stack-specific tooling reference |

## Full Protocol

Minimum viable contract above. Detailed bash for all 8 steps, per-tool dispatch commands with full flag matrix, baseline schema, sanitized report template, SLO/tolerance defaults, and review checklist live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
