---
name: performance-grpc
description: "gRPC performance testing playbook: ghz tooling, proto discovery modes, SLO comparison, and container reference."
requires-capabilities: [quality.performance.grpc]
---

# gRPC Performance Testing Playbook

## Default Tool: ghz ≥ 0.120

ghz is a gRPC benchmarking tool that supports proto file or server reflection for service discovery.
It outputs JSON with histogram data (p50/p95/p99) per method.

## Minimum Version

`ghz:0.120` — container image with Alpine base.

## Proto3 Service Discovery Modes

| Mode | When to use | Config key |
|------|-------------|------------|
| Proto file path | Proto files are in the project repo | `performance.grpc.protoPath` |
| Server reflection | gRPC server has reflection enabled | `performance.grpc.useReflection=true` |

Discovery priority: proto file path → server reflection. `x-test-performance` checks both
before emitting `TOOL_NOT_FOUND`.

## Command Reference

```bash
# Mode A: Proto file
docker run --rm --platform=linux/amd64 \
  -v "$PWD":/workspace \
  ghz:0.120 \
    --proto /workspace/proto/service.proto \
    --call mypackage.MyService/MyMethod \
    --host host.docker.internal:50051 \
    --duration 30s --rps 50 \
    --format json \
    --output /workspace/.perf-results.json

# Mode B: Server reflection
docker run --rm --platform=linux/amd64 \
  ghz:0.120 \
    --reflect-metadata '' \
    --call mypackage.MyService/MyMethod \
    --host host.docker.internal:50051 \
    --duration 30s --rps 50 \
    --format json \
    --output /workspace/.perf-results.json
```

## Result Format (ghz JSON output)

```json
{
  "details": [
    { "latency": 45000000, "status": "OK" },
    { "latency": 62000000, "status": "OK" }
  ],
  "latencyDistribution": [
    { "percentage": 50, "latency": 42000000 },
    { "percentage": 95, "latency": 98000000 },
    { "percentage": 99, "latency": 150000000 }
  ],
  "rps": 49.8
}
```

Latency is in nanoseconds — divide by 1,000,000 for milliseconds.

## Load Scenario Reference

| Scenario | RPS | Duration |
|----------|-----|----------|
| Baseline | 1 | 30s sequential |
| Sustained | 50 RPS | 30s |
| Spike | 200 RPS | 10s |

## Baseline JSON Schema (gRPC)

```json
{
  "grpc": {
    "MyService/MyMethod": {
      "p50Ms": 10, "p95Ms": 30, "p99Ms": 60,
      "throughputRps": 49.8,
      "measuredAt": "2026-05-01T00:00:00Z",
      "gitSha": "abc123..."
    }
  }
}
```

## Tooling Pinning

| Tool | Minimum | Container |
|------|---------|-----------|
| ghz | 0.120.0 | `ghz:0.120` |

## Integration Notes

- Proto files at `proto/` relative to project root
- TLS: add `--insecure` for local/test environments; production requires CA cert
- Payload data: `--data '{"key":"value"}'` or `--data-file /workspace/perf-payload.json`
- Cold-start Docker pull: ~15s on first run; subsequent runs use layer cache
