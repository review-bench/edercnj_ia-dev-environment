---
name: x-review-devops
description: "DevOps specialist review: Dockerfile, CI/CD, resource limits, health probes, deploy."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash, Agent
argument-hint: "[PR number or file paths]"
context-budget: light
requires-capabilities: []
fragment-slot: { slot: review-specialist, fragment-id: devops, fragment-order: 70 }
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: DevOps Specialist Review

## Purpose

Review code changes for DevOps best practices: Dockerfile multi-stage builds, container security hardening, CI/CD pipeline configuration, resource limits, health probe configuration, graceful shutdown implementation, and deployment manifests.

## Activation Condition

Include this skill when `container != "none"` in the project configuration.

## When to Use

- Pre-PR quality validation for infrastructure changes
- Reviewing Dockerfile and container configuration
- Checking deployment manifests
- Validating CI/CD pipeline changes

## Triggers

- `/x-review-devops 42` -- review PR #42 for DevOps patterns
- `/x-review-devops Dockerfile` -- review Dockerfile specifically
- `/x-review-devops` -- review all current infrastructure changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| infrastructure | `skills/infrastructure/SKILL.md` | Docker, Kubernetes, 12-Factor, graceful shutdown, resource management |
| checklists | `knowledge/checklists/helm-devops.md` | Helm chart review checklist |
| checklists | `knowledge/checklists/iac-devops.md` | Infrastructure-as-code review checklist |
| checklists | `knowledge/checklists/mesh-devops.md` | Service mesh review checklist |
| checklists | `knowledge/checklists/registry-devops.md` | Container registry review checklist |

## Checklist (10 Items, Max Score: /20)

Each item scores 0 (missing), 1 (partial), or 2 (fully compliant).

### Dockerfile (DEVOPS-01 to DEVOPS-04)

| # | Item | Score |
|---|------|-------|
| DEVOPS-01 | Multi-stage build (separate build and runtime stages) | /2 |
| DEVOPS-02 | Non-root user in final stage (no running as root) | /2 |
| DEVOPS-03 | Minimal base image (distroless, alpine, or slim) | /2 |
| DEVOPS-04 | .dockerignore configured (excludes build artifacts, tests, docs) | /2 |

### Container Security (DEVOPS-05 to DEVOPS-06)

| # | Item | Score |
|---|------|-------|
| DEVOPS-05 | No secrets in image layers (use runtime env vars or secrets manager) | /2 |
| DEVOPS-06 | Image pinned to specific digest or version (no :latest tag) | /2 |

### Deployment & Operations (DEVOPS-07 to DEVOPS-10)

| # | Item | Score |
|---|------|-------|
| DEVOPS-07 | Resource limits defined (CPU, memory) for container/pod | /2 |
| DEVOPS-08 | Health probes configured (liveness, readiness, startup) in deployment manifest | /2 |
| DEVOPS-09 | Graceful shutdown implemented (SIGTERM handling, connection draining) | /2 |
| DEVOPS-10 | Environment-specific configuration externalized (no hardcoded values) | /2 |

## Workflow

### Step 1 -- Gather Context

Collect the review target: PR number or file paths from args. Run:
```bash
git diff --name-only HEAD~1..HEAD 2>/dev/null || git diff --name-only --cached
```

### Step 2 -- Dispatch to DevOps Engineer Agent

    Agent(
      subagent_type: "devops-engineer",
      description: "DevOps specialist review for {target}",
      prompt: "Review the code changes for DevOps best practices. Target: {target}. Run `git diff HEAD~1..HEAD` to get the diff. Read `skills/infrastructure/SKILL.md` for infrastructure patterns. Apply your full DevOps checklist (Dockerfile, container security, deployment manifests, CI/CD, resource limits, health probes). Produce output in this exact format:\n\nENGINEER: DevOps\nSTORY: {target}\nSCORE: XX/20\nSTATUS: Approved | Rejected | Partial\n---\nPASSED:\n- [DEVOPS-XX] Description (2/2)\nFAILED:\n- [DEVOPS-XX] Description (0/2) -- file:line -- Fix: suggestion [SEVERITY]\nPARTIAL:\n- [DEVOPS-XX] Description (1/2) -- file:line -- Improvement: suggestion [SEVERITY]"
    )

## Output Format

```
ENGINEER: DevOps
STORY: [story-id or change description]
SCORE: XX/20

STATUS: PASS | FAIL | PARTIAL

### PASSED
- [DEVOPS-XX] [Item description]

### FAILED
- [DEVOPS-XX] [Item description]
  - Finding: [file:line] [issue description]
  - Fix: [remediation guidance]

### PARTIAL
- [DEVOPS-XX] [Item description]
  - Finding: [partial compliance details]
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No Dockerfile found | Report INFO: no container configuration discovered |
| No deployment manifests found | Skip DEVOPS-07, DEVOPS-08 and note N/A |
| No CI/CD config found | Warn and proceed with available files |
