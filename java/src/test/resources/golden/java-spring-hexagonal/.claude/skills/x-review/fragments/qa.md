---
name: x-review-fragment-qa
description: QA specialist review fragment — always active regardless of project capabilities.
fragment-slot: { slot: review-specialist, fragment-id: qa, fragment-order: 10 }
requires-capabilities: []
---

### QA Specialist (`/x-review-qa`)

| Attribute | Value |
|-----------|-------|
| Max Score | /36 (or /40 when smoke tests enabled) |
| Condition | Always active |
| Skill | `x-review-qa` |

Reviews: test coverage thresholds (≥95% line / ≥90% branch), TDD compliance, test naming conventions (`[method]_[scenario]_[expected]`), AAA pattern, fixture centralization, parametrized tests, and smoke test execution.
