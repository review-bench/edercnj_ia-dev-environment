---
name: x-review-fragment-devops
description: DevOps specialist review fragment — active when Docker or CI/CD is configured.
fragment-slot: { slot: review-specialist, fragment-id: devops, fragment-order: 40 }
requires-any: [infra.docker.standard, infra.cicd.github-actions]
requires-capabilities: []
---

### DevOps Specialist (`/x-review-devops`)

| Attribute | Value |
|-----------|-------|
| Max Score | /20 |
| Condition | `container != none` OR `cicd != none` |
| Skill | `x-review-devops` |

Reviews: Dockerfile multi-stage builds, non-root user in final stage, minimal base image (distroless/alpine/slim), `.dockerignore` configuration, no secrets in image layers, pinned image digest, resource limits (CPU/memory), health probe configuration (liveness/readiness/startup), graceful shutdown (SIGTERM handling), and externalized configuration (no hardcoded values).
