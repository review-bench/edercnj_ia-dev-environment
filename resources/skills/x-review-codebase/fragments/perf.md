---
name: x-review-fragment-perf
description: Performance specialist review fragment — always active regardless of project capabilities.
fragment-slot: { slot: review-specialist, fragment-id: perf, fragment-order: 20 }
requires-capabilities: []
---

### Performance Specialist (`/x-review-performance`)

| Attribute | Value |
|-----------|-------|
| Max Score | /26 |
| Condition | Always active |
| Skill | `x-review-performance` |

Reviews: N+1 query detection, connection pool sizing, async patterns (non-blocking I/O), pagination on collections, caching strategy, timeout configuration, circuit breaker usage, thread safety, resource cleanup (try-with-resources), lazy loading, and batch operations.
