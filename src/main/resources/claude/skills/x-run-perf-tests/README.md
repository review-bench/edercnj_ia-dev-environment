# x-run-perf-tests

> Runs performance tests to validate latency SLAs, throughput targets, and resource stability under load. Supports baseline, normal, peak, and sustained scenarios.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | Performance testing requirements configured |
| **Invocation** | `/x-run-perf-tests [scenario: baseline\|normal\|peak\|sustained\|all]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when performance testing requirements are defined in the project configuration.

## What It Does

Runs or implements performance tests to validate the application meets latency SLAs, throughput targets, and resource stability requirements under various load conditions. Measures p50/p95/p99 latency, transactions per second (TPS), error rates, and memory stability across four scenarios: baseline (single user), normal (expected daily load), peak (maximum concurrent load), and sustained (constant load over 30+ minutes for stability).

## Usage

```
/x-run-perf-tests
/x-run-perf-tests baseline
/x-run-perf-tests peak
/x-run-perf-tests all
```

## See Also

- [x-execute-e2e-tests](../x-execute-e2e-tests/) -- End-to-end integration tests
- [x-execute-api-smoke-tests](../x-execute-api-smoke-tests/) -- REST API smoke tests
- [x-instrument-observability](../x-instrument-observability/) -- OpenTelemetry instrumentation for performance metrics
