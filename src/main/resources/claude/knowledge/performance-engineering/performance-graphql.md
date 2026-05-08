---
name: performance-graphql
description: "GraphQL performance testing playbook: Artillery tooling, query scenarios, baseline format, and container reference."
requires-capabilities: [quality.performance.graphql]
---

# GraphQL Performance Testing Playbook

## Default Tool: Artillery ≥ 2.0

Artillery is a load-testing framework with native GraphQL support via HTTP POST to the
`/graphql` endpoint. It supports scenario YAML definitions with variables and per-query
latency measurement.

## Minimum Version

`artilleryio/artillery:2` — container image with Node.js 18 base.

## Command Reference

```bash
docker run --rm --platform=linux/amd64 \
  -v "$PWD":/workspace \
  artilleryio/artillery:2 run \
    --output /workspace/.perf-results.json \
    /workspace/performance/graphql-scenario.yml
```

## Artillery Scenario Format (`performance/graphql-scenario.yml`)

```yaml
config:
  target: "http://host.docker.internal:8080"
  phases:
    - duration: 30
      arrivalRate: 50
      name: Sustained load

scenarios:
  - name: Simple query
    flow:
      - post:
          url: "/graphql"
          json:
            query: |
              query GetUser($id: ID!) {
                user(id: $id) { id name email }
              }
            variables: { id: "1" }
          expect:
            - statusCode: 200
            - hasProperty: "data.user"

  - name: Nested query (3 levels)
    flow:
      - post:
          url: "/graphql"
          json:
            query: |
              query GetUserWithOrders {
                user(id: "1") {
                  id name
                  orders { id total items { sku qty price } }
                }
              }
          expect:
            - statusCode: 200
```

## Result Format (Artillery JSON output)

```json
{
  "aggregate": {
    "latency": {
      "min": 5, "max": 420,
      "median": 40, "p95": 95, "p99": 200
    },
    "rps": { "mean": 49.2 },
    "codes": { "200": 1476 }
  }
}
```

`x-test-performance` reads `aggregate.latency.median` (p50), `p95`, `p99` and `rps.mean`.

## Load Scenario Reference

| Scenario | Arrival Rate | Duration |
|----------|-------------|----------|
| Baseline | 1 user/s | 10s |
| Sustained | 50/s | 30s |
| Spike | 200/s | 10s |

## Baseline JSON Schema (GraphQL)

```json
{
  "graphql": {
    "GetUser": {
      "p50Ms": 40, "p95Ms": 95, "p99Ms": 200,
      "throughputRps": 49.2,
      "measuredAt": "2026-05-01T00:00:00Z",
      "gitSha": "abc123..."
    }
  }
}
```

## Tooling Pinning

| Tool | Minimum | Container |
|------|---------|-----------|
| Artillery | 2.0.0 | `artilleryio/artillery:2` |
| Node.js | 18 (bundled) | — |

## Integration Notes

- Scenario file at `performance/graphql-scenario.yml`
- Variables: use Artillery data files (`--variables`) for parameterized queries
- Authentication: set `config.http.headers.Authorization` to Bearer token template
- N+1 detection: include nested queries (3 levels) to surface resolver performance
- Cold-start Docker pull: ~45s on first run; subsequent runs use layer cache
