# x-execute-performance-tests — Full Protocol

Detailed reference for `x-execute-performance-tests`. The SKILL.md body holds the minimum viable contract per ADR-0012; this document is the canonical procedural reference.

## Step 1 — Read Config

Read `quality.performance.*` from the project YAML (via QualityConfig record):

```
enabled                 → if false: exit 50 PERF_DISABLED (silent)
baseline-tolerance-pct  → regression threshold (default 10)
slo.<stack>.p50_ms
slo.<stack>.p95_ms
slo.<stack>.p99_ms
```

When `slo.<stack>` block is absent: emit `WARN no SLO declared — running smoke only` and set `--smoke-only=true` implicitly. **Exit 0** (non-blocking smoke).

## Step 2 — Detect Stack

If `--stack` flag provided, use it directly. Otherwise, read `interfaces[].type` and `architecture.style` from project YAML. Apply dispatch priority `grpc > rest > graphql > socket > cli`.

When multiple stacks are active, dispatch once per detected stack; report is merged.

## Step 3 — Tool Availability Check

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

## Step 4 — Run Load Test

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

## Step 5 — Parse Results

Extract p50/p95/p99 from `.perf-results.json` for each endpoint/method/command. Normalize all values to milliseconds. Build internal result map:

```
{ "<endpoint>": { "p50Ms": N, "p95Ms": N, "p99Ms": N, "throughputRps": N } }
```

## Step 6 — Baseline Comparison (skip when --smoke-only or baseline absent)

Read `governance/baselines/performance-baseline.json`. If absent: emit `WARN baseline not found — recording initial measurement` and proceed to Step 8.

For each endpoint, compute delta vs baseline:

```
deltaP95Pct = ((current.p95Ms - baseline.p95Ms) / baseline.p95Ms) * 100
deltaP99Pct = ((current.p99Ms - baseline.p99Ms) / baseline.p99Ms) * 100
```

When `deltaP95Pct > baseline-tolerance-pct` OR `deltaP99Pct > baseline-tolerance-pct`: mark endpoint as `REGRESSION` → set `exitCode = 1 PERF_REGRESSION_DETECTED`.

When current p95 > `slo.<stack>.p95_ms` (absolute SLO): mark endpoint as `SLO_VIOLATION`.

## Step 7 — Write Report

Write `ai/epics/epic-XXXX/reports/perf-report-STORY-ID.md` (sanitized — no absolute paths, hostnames, IP addresses, or credentials):

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

## Step 8 — Update Baseline (only with --update-baseline)

Write new entries to `governance/baselines/performance-baseline.json` (append-only contract): set `measuredAt` = ISO-8601 timestamp, `gitSha` = current HEAD SHA. Never overwrite entries from a different git SHA in the same run — append new keys.

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

## Review Checklist

- [ ] SLOs read from QualityConfig.performance.slo.*
- [ ] Stack correctly auto-detected from project interfaces
- [ ] Newman dispatched for REST; ghz for gRPC; hyperfine for CLI; Artillery for GraphQL
- [ ] Baseline comparison uses configurable tolerance-pct
- [ ] Report contains no absolute paths, hostnames, or credentials
- [ ] Exit 1 on regression; exit 0 on smoke-only or no-SLO
- [ ] Baseline update gated by explicit `--update-baseline` flag
- [ ] Docker container fallback documented in KP per stack
