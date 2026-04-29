---
name: x-review-fragment-compliance
description: Compliance specialist review fragment — active when a compliance capability is configured.
fragment-slot: { slot: review-specialist, fragment-id: compliance, fragment-order: 80 }
requires-capabilities: [compliance.*]
---

### Compliance Specialist

| Attribute | Value |
|-----------|-------|
| Max Score | /20 |
| Condition | Compliance frameworks configured (PCI, HIPAA, LGPD, GDPR) |
| Skill | `x-review-security` (extended for compliance) |

Reviews: audit trail completeness (immutable logs for every state change), data retention policy enforcement, PII fields identified and protected (encryption at rest, masked in logs), access control (RBAC, principle of least privilege), cryptographic key rotation policy, and regulatory reporting artifact availability.
