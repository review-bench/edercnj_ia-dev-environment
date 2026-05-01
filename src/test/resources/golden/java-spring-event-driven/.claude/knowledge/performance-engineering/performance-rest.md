---
name: performance-rest
description: "REST performance testing playbook: Newman tooling, load scenarios, baseline format, and container reference."
requires-capabilities: [quality.performance.rest]
---

# REST Performance Testing Playbook

## Default Tool: Newman ≥ 6.0

Newman executes Postman collections as load drivers. It supports JSON reporters for
integration with `governance/baselines/performance-baseline.json`.

## Minimum Version

`postman/newman:6` — container image with Node.js 18 base.

## Load Scenario Reference

| Scenario | RPS | Duration | Concurrency |
|----------|-----|----------|-------------|
| Baseline | 1 user | 100 requests sequential | 1 |
| Sustained | 100 RPS | 30s | auto |
| Spike | 500 RPS | 10s | auto |

## Command Reference

```bash
docker run --rm --platform=linux/amd64 \
  -v "$PWD":/workspace \
  postman/newman:6 run \
    /workspace/performance/rest-collection.json \
    --reporters json \
    --reporter-json-export /workspace/.perf-results.json \
    --iteration-count 100
```

## Result Format (JSON reporter output)

```json
{
  "run": {
    "stats": { "iterations": { "total": 100 } },
    "timings": { "responseAverage": 45, "responseMin": 20, "responseMax": 200 },
    "executions": [
      {
        "item": { "name": "GET /api/users" },
        "response": { "responseTime": 42 }
      }
    ]
  }
}
```

`x-test-performance` extracts per-endpoint p50/p95/p99 from the `executions` array.

## Baseline JSON Schema (REST)

```json
{
  "rest": {
    "GET /api/users": {
      "p50Ms": 20, "p95Ms": 45, "p99Ms": 90,
      "throughputRps": 98.5,
      "measuredAt": "2026-05-01T00:00:00Z",
      "gitSha": "abc123..."
    }
  }
}
```

## Tooling Pinning

| Tool | Minimum | Container |
|------|---------|-----------|
| Newman | 6.0.0 | `postman/newman:6` |
| Node.js | 18 (bundled in container) | — |

## Integration Notes

- Place Postman collection at `performance/rest-collection.json`
- Collection variables: `{{BASE_URL}}` injected by Newman environment file
- Environment file: `performance/env-local.json` (gitignored for security)
- Cold-start Docker pull: ~30s on first run; subsequent runs use layer cache

## Smoke Test Validation

CI smoke validates Newman exits 0 on a 1-endpoint collection with 10 requests.
Smoke does not require a running service — uses mock-server or WireMock stub.
