---
name: x-review-fragment-api
description: API specialist review fragment — active when a web framework capability is configured.
fragment-slot: { slot: review-specialist, fragment-id: api, fragment-order: 60 }
requires-any: [web.spring.boot, web.quarkus.framework, web.micronaut.framework, web.helidon.framework]
requires-capabilities: []
---

### API Specialist (`/x-review-api`)

| Attribute | Value |
|-----------|-------|
| Max Score | /16 |
| Condition | REST interface present |
| Skill | `x-review-api` |

Reviews: REST resource naming (nouns plural, lowercase-hyphen), HTTP method semantics (GET idempotent, POST non-idempotent), status code correctness (201 on create, 204 on delete, 422 on validation), pagination shape (`page`/`size`/`totalElements`), error response contract (RFC 7807 `application/problem+json`), versioning strategy (URL path or header), and request/response DTO separation from domain model.
