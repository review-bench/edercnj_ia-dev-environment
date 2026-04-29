---
name: x-review-fragment-security
description: Security specialist review fragment — always active regardless of project capabilities.
fragment-slot: { slot: review-specialist, fragment-id: security, fragment-order: 30 }
requires-capabilities: []
---

### Security Specialist (`/x-review-security`)

| Attribute | Value |
|-----------|-------|
| Max Score | /30 |
| Condition | Always active |
| Skill | `x-review-security` |

Reviews: OWASP Top 10 vulnerabilities (SQL injection, XSS, SSRF, path traversal), hardcoded credentials, insecure deserialization, weak cryptography (`Math.random()` for tokens), trust-all TLS, CORS misconfiguration, and sensitive data in logs or error responses.
