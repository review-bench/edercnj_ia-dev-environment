---
name: x-review-observability
description: "Observability review: tracing, metrics naming, structured logging, health, correlation."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash, Agent
argument-hint: "[PR number or file paths]"
requires-capabilities: []
fragment-slot: { slot: review-specialist, fragment-id: obs, fragment-order: 40 }
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Observability Specialist Review

## Purpose

Review code changes for observability best practices: distributed tracing with proper span attributes, metrics naming conventions, structured logging with mandatory fields, health check implementation, correlation ID propagation, and alerting configuration.

## Activation Condition

Include this skill when `observability.tool != "none"` in the project configuration.

## When to Use

- Pre-PR quality validation for observability concerns
- Reviewing logging and tracing implementations
- Checking health check endpoints
- Validating metrics and alerting configuration

## Triggers

- `/x-review-observability 42` -- review PR #42 for observability
- `/x-review-observability src/main/java/com/example/config/` -- review specific paths
- `/x-review-observability` -- review all current observability changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| observability | `skills/observability/SKILL.md` | Tracing, metrics, logging, health checks, correlation IDs |

## Checklist (9 Items, Max Score: /18)

Each item scores 0 (missing), 1 (partial), or 2 (fully compliant).

### Distributed Tracing (OBS-01 to OBS-03)

| # | Item | Score |
|---|------|-------|
| OBS-01 | Spans created for key operations (inbound requests, outbound calls, DB queries) | /2 |
| OBS-02 | Span attributes include mandatory fields (service, operation, status) | /2 |
| OBS-03 | Trace context propagated across service boundaries (W3C Trace Context headers) | /2 |

### Structured Logging (OBS-04 to OBS-06)

| # | Item | Score |
|---|------|-------|
| OBS-04 | Logs are structured JSON with mandatory fields (timestamp, level, message, trace_id, span_id, service) | /2 |
| OBS-05 | No sensitive data in logs (PII, credentials, tokens masked or excluded) | /2 |
| OBS-06 | Log levels appropriate (DEBUG for development, INFO for operations, WARN/ERROR for issues) | /2 |

### Health Checks & Metrics (OBS-07 to OBS-09)

| # | Item | Score |
|---|------|-------|
| OBS-07 | Health check endpoints implemented (liveness, readiness, startup) | /2 |
| OBS-08 | Custom metrics follow naming convention ({service}_{subsystem}_{metric}_{unit}) | /2 |
| OBS-09 | Correlation ID generated or propagated on every inbound request | /2 |

## Workflow

### Step 1 -- Gather Context

Collect the review target: PR number or file paths from args. Run:
```bash
git diff --name-only HEAD~1..HEAD 2>/dev/null || git diff --name-only --cached
```

### Step 2 -- Dispatch to Observability Engineer Agent

    Agent(
      subagent_type: "observability-engineer",
      description: "Observability specialist review for {target}",
      prompt: "Review the code changes for observability best practices. Target: {target}. Run `git diff HEAD~1..HEAD` to get the diff. Read `skills/observability/SKILL.md` for project observability patterns. Apply your full observability checklist (tracing, structured logging, health probes, metrics, correlation IDs). Produce output in this exact format:\n\nENGINEER: Observability\nSTORY: {target}\nSCORE: XX/18\nSTATUS: Approved | Rejected | Partial\n---\nPASSED:\n- [OBS-XX] Description (2/2)\nFAILED:\n- [OBS-XX] Description (0/2) -- file:line -- Fix: suggestion [SEVERITY]\nPARTIAL:\n- [OBS-XX] Description (1/2) -- file:line -- Improvement: suggestion [SEVERITY]"
    )

## Output Format

```
ENGINEER: Observability
STORY: [story-id or change description]
SCORE: XX/18

STATUS: PASS | FAIL | PARTIAL

### PASSED
- [OBS-XX] [Item description]

### FAILED
- [OBS-XX] [Item description]
  - Finding: [file:line] [issue description]
  - Fix: [remediation guidance]

### PARTIAL
- [OBS-XX] [Item description]
  - Finding: [partial compliance details]
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No observability code found | Report INFO: no observability code discovered |
| OpenTelemetry not configured | Warn and check for alternative tracing libraries |
| Health check endpoints missing | Report as FAILED for OBS-07 |
