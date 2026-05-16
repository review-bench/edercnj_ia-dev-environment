---
name: performance-cli
description: "CLI performance testing playbook: hyperfine tooling, invocation modes, baseline format, and container reference."
requires-capabilities: [quality.performance.cli]
---

# CLI Performance Testing Playbook

## Default Tool: hyperfine ≥ 1.18

hyperfine is a command-line benchmarking tool that runs shell commands repeatedly,
measuring wall-clock time (min/mean/max/stddev). It exports JSON with per-run timings.

## Minimum Version

`hyperfine:1.18` — container image with Alpine base.

## Command Reference

```bash
# Basic: 100 runs, JSON export
docker run --rm --platform=linux/amd64 \
  -v "$PWD":/workspace \
  hyperfine:1.18 \
    --runs 100 \
    --export-json /workspace/.perf-results.json \
    '/workspace/target/my-cli subcommand --flag arg'

# Parallel: 10 concurrent (cold-cache stress)
docker run --rm --platform=linux/amd64 \
  -v "$PWD":/workspace \
  hyperfine:1.18 \
    --runs 100 \
    --jobs 10 \
    --export-json /workspace/.perf-results.json \
    '/workspace/target/my-cli subcommand'
```

## Result Format (hyperfine JSON output)

```json
{
  "results": [
    {
      "command": "my-cli subcommand",
      "mean": 0.045,
      "stddev": 0.005,
      "median": 0.042,
      "user": 0.038,
      "system": 0.004,
      "min": 0.020,
      "max": 0.200,
      "times": [0.041, 0.043, 0.042, ...]
    }
  ]
}
```

`x-test-performance` computes p50/p95/p99 from the `times` array (sorted percentile).
Times are in seconds — multiply by 1000 for milliseconds.

## Load Scenario Reference

| Scenario | Runs | Jobs | Purpose |
|----------|------|------|---------|
| Baseline | 10 sequential | 1 | Establish floor |
| Sustained | 100 sequential | 1 | SLO validation |
| Parallel | 100 total | 10 | Concurrency stress |

## Baseline JSON Schema (CLI)

```json
{
  "cli": {
    "my-cli subcommand": {
      "p50Ms": 42, "p95Ms": 95, "p99Ms": 180,
      "throughputRps": 22.2,
      "measuredAt": "2026-05-01T00:00:00Z",
      "gitSha": "abc123..."
    }
  }
}
```

## Tooling Pinning

| Tool | Minimum | Container |
|------|---------|-----------|
| hyperfine | 1.18.0 | `hyperfine:1.18` |

## Integration Notes

- CLI binary must be a compiled artifact — build before running benchmark
- Warmup: `--warmup 5` runs before measurement (avoids JVM/OS cold-start noise)
- Prepare command: `--prepare 'rm -f /tmp/output.txt'` for cleanup between runs
- Export also supports `--export-csv` and `--export-markdown` for human review
- Cold-start Docker pull: ~10s on first run; subsequent runs use layer cache
