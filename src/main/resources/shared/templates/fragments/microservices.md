---
name: claude-md-fragment-microservices
description: Microservices-specific CLAUDE.md section — active when web framework and service mesh capabilities are configured.
fragment-slot: { slot: domain-specific, fragment-id: microservices, fragment-order: 40 }
requires-any: [web.spring.boot, web.quarkus.framework, web.micronaut.framework, web.helidon.framework]
requires-capabilities: []
---

## Service Design Principles

This project is a microservice in a distributed system.

- **Resilience:** All outbound HTTP/gRPC calls must have a timeout (default: 5s), a retry policy (max 3 with exponential backoff), and a circuit breaker (Resilience4j `CircuitBreaker`).
- **Health probes:** Every service exposes `/health/liveness` (process alive), `/health/readiness` (dependencies up), and `/health/startup` (init complete). Do not include heavy I/O in the liveness probe.
- **Distributed tracing:** Propagate `X-Correlation-ID` and `traceparent` headers through all outbound calls; inject into MDC for structured logging.
- **Graceful shutdown:** Handle `SIGTERM` by stopping new connections, draining in-flight requests (default timeout: 30s), flushing caches, and closing DB/broker connections in order.
- **API versioning:** URL path prefix (`/api/v1/`) is mandatory; never break an existing version without a deprecation cycle (min 2 releases).
- **Contract first:** For every new REST endpoint, define the OpenAPI spec before implementation; generate server stubs from spec via `openapi-generator`.
