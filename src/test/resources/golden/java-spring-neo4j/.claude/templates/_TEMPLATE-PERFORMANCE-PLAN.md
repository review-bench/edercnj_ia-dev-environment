---
name: _TEMPLATE-PERFORMANCE-PLAN
description: "Template for performance test plans produced by x-test-performance. Runtime-filled by LLM."
requires-capabilities:
  - quality.performance.rest
  - quality.performance.grpc
  - quality.performance.cli
  - quality.performance.graphql
  - quality.performance.socket
---

# Performance Test Plan — {{STORY_ID}}

## Header

| Field | Value |
|-------|-------|
| Story | {{STORY_ID}} |
| Stack | {{STACK}} |
| Tool | {{TOOL}} {{TOOL_VERSION}} |
| SLO Source | `quality.performance.slo.{{STACK}}` |
| Baseline | `governance/baselines/performance-baseline.json` |
| Tolerance | {{TOLERANCE_PCT}}% |
| Date | {{DATE}} |

## Summary

{{SUMMARY_NARRATIVE}}

| Metric | Target | Actual | Verdict |
|--------|--------|--------|---------|
| p50 latency | ≤ {{P50_TARGET_MS}} ms | {{P50_ACTUAL_MS}} ms | {{P50_VERDICT}} |
| p95 latency | ≤ {{P95_TARGET_MS}} ms | {{P95_ACTUAL_MS}} ms | {{P95_VERDICT}} |
| p99 latency | ≤ {{P99_TARGET_MS}} ms | {{P99_ACTUAL_MS}} ms | {{P99_VERDICT}} |
| Error rate | ≤ {{ERROR_RATE_TARGET_PCT}}% | {{ERROR_RATE_ACTUAL_PCT}}% | {{ERROR_RATE_VERDICT}} |

**Overall: {{OVERALL_VERDICT}}**

## Load Scenarios

| Scenario | RPS / Concurrency | Duration | Purpose |
|----------|-------------------|----------|---------|
| Baseline | 1 user, 100 sequential | — | Establish latency floor |
| Sustained | {{SUSTAINED_RPS}} RPS | 30s | SLO validation |
| Spike | {{SPIKE_MULTIPLIER}}× sustained | 10s | Regression detection |

## Results per Endpoint

| Endpoint / Method | p50 ms | p95 ms | p99 ms | Throughput RPS | Baseline p95 | Delta % | Verdict |
|-------------------|--------|--------|--------|----------------|-------------|---------|---------|
{{RESULTS_TABLE_ROWS}}

## Baseline Comparison

| Endpoint | Baseline p95 ms | Current p95 ms | Delta % | Threshold | Status |
|----------|----------------|----------------|---------|-----------|--------|
{{BASELINE_COMPARISON_ROWS}}

**Tolerance threshold:** {{TOLERANCE_PCT}}%
**Regressions detected:** {{REGRESSION_COUNT}}

## Tooling

| Field | Value |
|-------|-------|
| Tool | {{TOOL}} |
| Version | {{TOOL_VERSION}} |
| Container | {{CONTAINER_IMAGE}} |
| Command | `{{SANITIZED_COMMAND}}` |
| Platform | `linux/amd64` |

## Risks and Gaps

{{RISKS_AND_GAPS}}

## Recommended Action

{{RECOMMENDED_ACTION}}
