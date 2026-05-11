# Security Principles

Foundational principles that guide secure design and implementation across all services. Apply these before reaching for specific controls or libraries.

## Defense in Depth

No single control should be the last line of defense. Stack independent layers so that one failure does not breach the system.

| Layer | Examples |
|-------|----------|
| Network | VPC isolation, security groups, mTLS between services |
| Perimeter | WAF, rate limiting, bot protection, DDoS shields |
| Identity | OAuth2/OIDC, MFA, short-lived tokens |
| Application | Input validation, output encoding, parameterized queries |
| Data | Encryption at rest, field-level encryption for restricted data |
| Audit | Immutable logs, anomaly detection, alerting |

A path-traversal bug at the application layer must not yield filesystem access; the OS user, container, and filesystem permissions are independent gates.

## Principle of Least Privilege

Every component, user, and credential gets the minimum permissions needed to perform its task and no more.

- Service accounts scoped per service, never shared
- Database users with `SELECT` on required tables only (no `*.*`)
- Container processes run as non-root; capabilities dropped via `securityContext`
- Short-lived credentials (≤1h) preferred over long-lived API keys
- Admin operations require step-up authentication (re-auth + MFA), not just role membership

When designing a new component, ask "what is the smallest IAM policy that lets this work?" — start there, not from a broad template.

## Fail Secure (Default Deny)

When a check fails, errors out, or returns ambiguous state, **deny the operation**.

```java
// WRONG — fails open: any exception grants access
public boolean canAccess(User u, Resource r) {
    try {
        return authzService.check(u, r);
    } catch (Exception e) {
        return true; // catastrophic — assume admin if check breaks
    }
}

// RIGHT — fails closed: errors deny
public boolean canAccess(User u, Resource r) {
    try {
        return authzService.check(u, r);
    } catch (Exception e) {
        log.error("Authz check failed for user={}, resource={}", u.id(), r.id(), e);
        return false;
    }
}
```

The same applies to feature flags, configuration loading, and policy evaluation.

## Input Validation at Trust Boundaries

Validate every input that crosses a trust boundary — HTTP endpoints, message consumers, file uploads, configuration sources. Internal calls between trusted modules need only contract checks, not full validation.

| Validation type | What it checks | Where |
|-----------------|----------------|-------|
| Size | Body/field length, collection cardinality | First, before parsing |
| Type | Numeric, date format, enum membership | Bean Validation / schema |
| Format | Regex for IDs, emails, URLs | Bean Validation |
| Semantic | Range, business invariants | Use-case layer |
| Authorization | Caller may act on this resource | Use-case layer |

**Allowlist over denylist.** "Only these characters are allowed" beats "these characters are blocked" — denylists miss future encodings (Unicode normalization, double encoding).

## Data Classification Drives Controls

Every field belongs to one of four classes (see Rule 09). The class determines protection.

| Class | Examples | At rest | In transit | In logs |
|-------|----------|---------|------------|---------|
| Public | API docs, changelogs | None | TLS | OK |
| Internal | Metrics, request IDs | At-rest encryption | TLS | OK |
| Confidential | Email, address, phone | At-rest encryption | TLS 1.3 | Masked |
| Restricted | Password, token, PAN, PHI | Field-level encryption | TLS 1.3 + mTLS | NEVER |

When in doubt, classify as Restricted and downgrade only with explicit justification.

## Secure Defaults

The out-of-the-box behavior is secure. Insecure modes require explicit opt-in with audit trail.

- New endpoints require authentication by default; public endpoints are explicitly annotated
- TLS is enforced; plain HTTP listeners exist only on internal management ports
- New database columns default `NOT NULL` for required fields
- Feature flags default to `off`; rollout is opt-in
- Containers default to read-only root filesystem; writable paths are explicit mounts

## Separation of Concerns

Security logic lives in one place per concern; business code calls it.

- Authentication: one filter/interceptor, not scattered across controllers
- Authorization: one policy evaluator, not duplicated `if (role.equals("ADMIN"))` checks
- Crypto: one wrapper that hides algorithm choice and rotation; never direct `Cipher.getInstance(...)` in business code
- Secrets: one client (Vault/AWS Secrets Manager); never `@Value("${plain.password}")` in services

Centralization makes audit, rotation, and incident response tractable.

## Threat Modeling at Design Time

For every new component or significant change, walk through STRIDE:

| Letter | Threat | Mitigation example |
|--------|--------|---------------------|
| **S** | Spoofing | Mutual TLS, signed tokens |
| **T** | Tampering | Integrity hashing, signed payloads |
| **R** | Repudiation | Audit logs with tamper-evident storage |
| **I** | Information disclosure | Encryption, field-level masking |
| **D** | Denial of service | Rate limits, timeouts, circuit breakers |
| **E** | Elevation of privilege | Least privilege, role separation |

Capture decisions in `docs/threat-models/{component}.md` per the `x-model-threats` skill.

## Auditability

Security-relevant events generate immutable audit records:

- Authentication: success/failure, MFA challenge result
- Authorization: deny decisions on Restricted resources
- Crypto: key rotation, decryption attempts on Restricted data
- Admin: configuration changes, credential creation, role grants

Audit logs are write-once, retained per regulatory requirements (PCI-DSS: 1 year online + 1 year archive; HIPAA: 6 years), and forwarded to a SIEM out of the application's control.

## Cross-References

- Rule 06 — `security-baseline.md` (project-wide security baseline)
- Rule 11 — `security-pci.md` (PCI-DSS prohibitions)
- Rule 12 — `security-anti-patterns.java.md` (Java-specific bad patterns with CWE mappings)
- `knowledge/security/application-security.md` (OWASP Top 10 controls)
- `knowledge/security/cryptography.md` (algorithm choice, key management)
- `knowledge/owasp-asvs/` (Application Security Verification Standard)
