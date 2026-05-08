---
name: x-review-performance
description: "Performance review: N+1, pools, async, pagination, cache, timeouts, circuit breakers."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash, Agent
argument-hint: "[PR number or file paths]"
requires-capabilities: []
fragment-slot: { slot: review-specialist, fragment-id: perf, fragment-order: 110 }
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Performance Specialist Review

## Purpose

Review code changes for performance best practices: N+1 query detection, connection pool sizing, async patterns, pagination on collections, caching strategy, timeout configuration, circuit breaker usage, thread safety, resource cleanup, lazy loading, batch operations, and index usage.

## When to Use

- Pre-PR quality validation for performance concerns
- Reviewing database query patterns
- Checking resilience patterns (timeouts, circuit breakers)
- Validating resource management

## Triggers

- `/x-review-performance 42` -- review PR #42 for performance
- `/x-review-performance src/main/java/com/example/repository/` -- review specific paths
- `/x-review-performance` -- review all current changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| resilience | `knowledge/resilience/index.md` | Circuit breaker, rate limiting, timeout, retry, bulkhead patterns |

## Checklist (13 Items, Max Score: /26)

Each item scores 0 (missing), 1 (partial), or 2 (fully compliant).

### Query Performance (PERF-01 to PERF-02)

| # | Item | Score |
|---|------|-------|
| PERF-01 | No N+1 queries (eager fetching or batch loading where needed) | /2 |
| PERF-02 | Connection pool sized appropriately for expected load | /2 |

### Async & Concurrency (PERF-03, PERF-09)

| # | Item | Score |
|---|------|-------|
| PERF-03 | Async processing where applicable (non-blocking I/O) | /2 |
| PERF-09 | Thread safety verified (no shared mutable state without synchronization) | /2 |

### Collection & Data (PERF-04 to PERF-06)

| # | Item | Score |
|---|------|-------|
| PERF-04 | Pagination on collection endpoints (no unbounded result sets) | /2 |
| PERF-05 | Caching strategy defined for frequently accessed data | /2 |
| PERF-06 | No unbounded lists in memory (streams or pagination for large datasets) | /2 |

### Resilience (PERF-07 to PERF-08)

| # | Item | Score |
|---|------|-------|
| PERF-07 | Timeout configured on all external calls (HTTP, DB, message broker) | /2 |
| PERF-08 | Circuit breaker on external service calls | /2 |

### Resource Management (PERF-10 to PERF-13)

| # | Item | Score |
|---|------|-------|
| PERF-10 | Resource cleanup in finally/try-with-resources (connections, streams, files) | /2 |
| PERF-11 | Lazy loading for expensive initializations | /2 |
| PERF-12 | Batch operations for bulk data processing (not row-by-row) | /2 |
| PERF-13 | Database indexes used for queried columns | /2 |

## Workflow

### Step 1 -- Gather Context

Collect the review target: PR number or file paths from args. Run:
```bash
git diff --name-only HEAD~1..HEAD 2>/dev/null || git diff --name-only --cached
```

### Step 2 -- Dispatch to Performance Engineer Agent

    Agent(
      subagent_type: "performance-engineer",
      description: "Performance specialist review for {target}",
      prompt: "Review the code changes for performance compliance. Target: {target}. Run `git diff HEAD~1..HEAD` to get the diff. Read `knowledge/resilience/index.md` for resilience patterns. Apply your full performance checklist (N+1 queries, connection pools, async patterns, pagination, caching, timeouts, circuit breakers, thread safety, resource cleanup). Produce output in this exact format:\n\nENGINEER: Performance\nSTORY: {target}\nSCORE: XX/26\nSTATUS: Approved | Rejected | Partial\n---\nPASSED:\n- [PERF-XX] Description (2/2)\nFAILED:\n- [PERF-XX] Description (0/2) -- file:line -- Fix: suggestion [SEVERITY]\nPARTIAL:\n- [PERF-XX] Description (1/2) -- file:line -- Improvement: suggestion [SEVERITY]"
    )

## Output Format

```
ENGINEER: Performance
STORY: [story-id or change description]
SCORE: XX/26

STATUS: PASS | FAIL | PARTIAL

### PASSED
- [PERF-XX] [Item description]

### FAILED
- [PERF-XX] [Item description]
  - Finding: [file:line] [issue description]
  - Fix: [remediation guidance]

### PARTIAL
- [PERF-XX] [Item description]
  - Finding: [partial compliance details]
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No repository/service code found | Report INFO: no performance-relevant code discovered |
| No external calls detected | Skip PERF-07, PERF-08 and note N/A |
| No database queries detected | Skip PERF-01, PERF-02, PERF-13 and note N/A |
