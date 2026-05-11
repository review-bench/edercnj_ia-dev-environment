# Observability Principles

Three pillars (logs, metrics, traces) plus the practices that make them useful in production. Designed to enable on-call engineers to diagnose problems they did not author.

## Three Pillars

| Pillar | Best at | Storage shape |
|--------|---------|---------------|
| **Logs** | Discrete events with rich context | Append-only, indexed text |
| **Metrics** | Aggregates over time | Numeric time series |
| **Traces** | Causal flow across services | Distributed graph |

Each pillar complements the others. A latency spike on a metric → drill into traces of slow requests → read logs of those traces' steps.

## Logs

### Structured Logging — Mandatory

All logs are emitted as structured records (JSON in production). Never log free-form strings concatenated with values.

```java
// WRONG — free-form, ungreppable, unparseable
log.info("User " + userId + " bought " + count + " items");

// RIGHT — structured key-value
log.info("order.placed", kv("user_id", userId), kv("item_count", count));
```

Required fields per record (set automatically by logging framework / MDC):

| Field | Source | Example |
|-------|--------|---------|
| `timestamp` | UTC ISO-8601 with millis | `2026-05-11T14:32:01.234Z` |
| `level` | Logger | `INFO`, `WARN`, `ERROR` |
| `logger` | Class name | `com.example.OrderService` |
| `message` | Event name | `order.placed` |
| `trace_id` | OpenTelemetry context | hex traceId |
| `span_id` | OpenTelemetry context | hex spanId |
| `service` | Build config | `order-service` |
| `version` | Build config | `1.4.2` |
| `env` | Runtime config | `prod`, `staging` |

### Log Levels

| Level | When | Volume |
|-------|------|--------|
| `ERROR` | Operation failed; user impact | Low — alert on rate |
| `WARN` | Recoverable issue, degraded state | Low — investigate trends |
| `INFO` | Significant business event | Medium — audit-friendly |
| `DEBUG` | Internal state for diagnosis | High — disabled in prod by default |
| `TRACE` | Verbose internals | Very high — short-window only |

ERROR for actual errors (not "user typo"). WARN for transient retries and known-degraded paths. INFO for "X happened" — not for "entered method Y". DEBUG/TRACE never default-on in production.

### Forbidden in Logs

- Restricted data (PAN, CVV, password, token, PHI) at any level — see Rule 11 PRH-05
- Confidential data unmasked (email, full address, full SSN)
- Stack traces in user-visible logs (HTTP responses, error pages) — Rule 12 J7
- Whole request/response bodies for production paths (mask first)

## Metrics

### Naming Convention

Pattern: `{namespace}_{subject}_{unit}_{aggregation}`

```
http_server_request_duration_seconds        # histogram, unit in name
http_server_requests_total                  # counter, "_total" suffix
order_placed_total                          # counter
cache_hits_total                            # counter
db_connection_pool_utilization_ratio        # gauge, 0..1
```

Rules:
- Lower case with underscores (Prometheus convention).
- Always include unit in the name when not dimensionless.
- Use seconds, bytes, ratios (0..1) — never milliseconds or percentages.
- Counters end in `_total`; histograms in `_seconds`, `_bytes`; gauges describe state.

### Labels (Cardinality Control)

| Allowed label | Reason |
|---------------|--------|
| `method` (GET, POST, ...) | Small, bounded set |
| `route` (`/orders/:id`, template form) | Bounded by API surface |
| `status_class` (`2xx`, `4xx`, `5xx`) | 5 values total |
| `error_type` (enum) | Bounded by error catalog |

| Forbidden label | Why |
|-----------------|-----|
| `user_id`, `customer_id`, `order_id` | Unbounded — explodes time series |
| Full URL with path params | Same |
| Free-form error message | Same |
| Timestamps | Same |

A label set with cardinality > 1000 unique values per service is a bug.

### Four Golden Signals

For every service, emit:

| Signal | Metric example |
|--------|----------------|
| **Latency** | `http_server_request_duration_seconds` (histogram) |
| **Traffic** | `http_server_requests_total` (counter, rate at query time) |
| **Errors** | `http_server_requests_total{status_class="5xx"}` |
| **Saturation** | `db_connection_pool_utilization_ratio`, `jvm_memory_used_bytes / jvm_memory_max_bytes` |

Plus business KPIs (`order_placed_total`, `payment_authorized_total`) — these are the user-visible signals.

## Traces

### Span Conventions

One span per logical operation crossing a process or component boundary:

- HTTP server: one span per incoming request, started by the framework
- HTTP client: one span per outgoing call
- DB query: one span per query (slow paths only — sampling needed for high-volume)
- Cache hit/miss: span attribute on the parent, not a child span
- In-process method calls: usually no span unless the method is async/concurrent

### Attributes — Use Semantic Conventions

Follow OpenTelemetry semantic conventions. Examples:

| Attribute | Meaning |
|-----------|---------|
| `http.method` | `GET`, `POST` |
| `http.route` | Templated route, not the resolved URL |
| `http.status_code` | Numeric |
| `db.system` | `postgresql`, `redis` |
| `db.statement` | Parameterized query (no values) |
| `messaging.system` | `kafka`, `rabbitmq` |
| `messaging.destination` | Topic/queue |

Custom attributes prefix with service domain: `order.payment_method`, `inventory.warehouse_id`. Never put Restricted data in attributes.

### Sampling

| Strategy | When |
|----------|------|
| **Always sample errors** | Yes — keep failed requests in full |
| Head-based parent | Default, low overhead, good for steady-state baselines |
| Tail-based on edge | Better for finding outliers; needs collector pipeline |
| Adaptive rate | Production with high traffic — keep ~1% of normal, 100% of errors |

## Correlation

All three pillars must be correlatable.

- Logs carry `trace_id` and `span_id` (already required above)
- Metrics include exemplars (trace IDs of representative samples) when the format supports it (OpenMetrics)
- Traces reference log queries via service + trace_id

A user-reported issue with a request ID should let on-call jump from log → trace → metric in one click.

## Health and Readiness

Distinct probes (Rule 07):

| Probe | Returns OK when |
|-------|-----------------|
| `/health/liveness` | Process is alive; never depend on downstream |
| `/health/readiness` | Service is ready to accept traffic (DB pool warm, caches loaded, downstream dependencies reachable) |
| `/health/startup` | Initial setup complete; for slow-starting apps |

Failing liveness restarts the pod. Failing readiness removes it from load balancer rotation without restart.

## SLIs and SLOs

| Term | Meaning |
|------|---------|
| **SLI** (indicator) | Measured metric — e.g., "% of requests with latency < 500ms" |
| **SLO** (objective) | Target — e.g., "99.5% of requests in 30 days" |
| **Error budget** | `1 - SLO` — the allowed failure rate |
| **Burn rate** | How fast the budget is consumed; alert when burn > threshold |

Page on **fast burn** of the error budget, not on the raw metric value. A 5xx spike that consumes a week of budget in 1 hour pages; the same spike spread over 30 days does not.

## Cross-References

- Rule 07 — `operations-baseline.md` (health probes, SLOs)
- `knowledge/observability/alerting-patterns.md` (alerting patterns and anti-patterns)
- `knowledge/sre-practices/error-budget-calculator.md`
- OpenTelemetry Semantic Conventions specification
