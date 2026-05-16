---
name: x-review-api
description: "Validates REST endpoints: RFC 7807, pagination, versioning, OpenAPI, status codes, DTOs."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash, Agent
argument-hint: "[endpoint-path or feature-name]"
requires-capabilities: []
fragment-slot: { slot: review-specialist, fragment-id: api, fragment-order: 50 }
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: REST API Design Review

## Purpose

Review REST API design for compliance with best practices: RFC 7807 error responses, pagination wrappers, URL versioning, OpenAPI annotations, proper HTTP status codes, and DTO separation from domain models.

## Activation Condition

Include this skill when the project uses REST protocol.

## Triggers

- `/x-review-api merchants` -- review API endpoints for a specific feature
- `/x-review-api /api/v1/transactions` -- review a specific endpoint path
- `/x-review-api` -- review all REST endpoints

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `endpoint-or-feature` | String | No | (all) | Endpoint path or feature name to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| api-design | `.claude/knowledge/api-design/api-design-principles.md` | URL structure, status codes, error format, pagination |
| api-design | `.claude/knowledge/api-design/rest-conventions.md` | REST resource naming, HTTP methods, versioning, RFC 7807 |
| checklists | `.claude/knowledge/checklists/graphql-api.md` | GraphQL-specific review checklist |
| checklists | `.claude/knowledge/checklists/grpc-api.md` | gRPC-specific review checklist |
| checklists | `.claude/knowledge/checklists/websocket-api.md` | WebSocket-specific review checklist |

## Prerequisites

- REST API endpoints exist in the codebase
- {{FRAMEWORK}} is configured with REST/HTTP support
- OpenAPI/Swagger dependency is available

## Workflow

### Step 1 -- Gather Context

Collect the review target: endpoint path or feature name from args. Run:
```bash
git diff --name-only HEAD~1..HEAD 2>/dev/null || git diff --name-only --cached
```

### Step 2 -- Dispatch to API Engineer Agent

    Agent(
      subagent_type: "api-engineer",
      description: "REST API specialist review for {target}",
      prompt: "Review the REST API design for best practices. Target: {target}. Run `git diff HEAD~1..HEAD` to get the diff. Read `.claude/knowledge/api-design/api-design-principles.md` and `.claude/knowledge/api-design/rest-conventions.md`. Apply your full API checklist (URL structure, status codes, RFC 7807 error responses, pagination, DTOs, OpenAPI documentation). Produce output in this exact format:\n\nENGINEER: API\nSTORY: {target}\nSCORE: XX/16\nSTATUS: Approved | Rejected | Partial\n---\nPASSED:\n- [API-XX] Description (2/2)\nFAILED:\n- [API-XX] Description (0/2) -- file:line -- Fix: suggestion [SEVERITY]\nPARTIAL:\n- [API-XX] Description (1/2) -- file:line -- Improvement: suggestion [SEVERITY]"
    )

## Error Handling

| Scenario | Action |
|----------|--------|
| No REST endpoints found | Report INFO: no endpoints discovered in the codebase |
| OpenAPI dependency missing | Warn about missing documentation support |
| Endpoint missing error handling | Report violation with file path and remediation guidance |

## Review Checklist

- [ ] URLs follow RESTful pattern (nouns, no verbs)
- [ ] Proper versioning in URL path (/api/v1/)
- [ ] Correct HTTP status codes per operation
- [ ] Request DTOs have validation annotations
- [ ] Response DTOs are immutable, no domain entities exposed
- [ ] Error responses follow RFC 7807 (ProblemDetail)
- [ ] Pagination implemented for list endpoints
- [ ] Sensitive data masked in responses
- [ ] OpenAPI/Swagger documentation generated
- [ ] ExceptionMapper covers all domain exceptions
- [ ] No stack traces in production error responses
- [ ] Rate limit responses return 429 + Retry-After header
