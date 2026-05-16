# Application Security

Concrete controls for web and service-layer security, organized around OWASP Top 10 (2021) and verified against ASVS Level 2.

## OWASP Top 10 (2021) — Coverage

### A01: Broken Access Control

- Enforce authorization at the **use-case layer**, not the controller layer; controllers route, use-cases authorize.
- Deny by default: every protected resource requires an explicit allow rule.
- Reject requests where the caller is not the resource owner, even if authenticated.
- Indirect Object Reference: never trust client-supplied IDs for authorization scope; resolve scope from the session/token, not from the request body.

### A02: Cryptographic Failures

See `cryptography.md`. Summary:
- TLS 1.2 minimum, 1.3 preferred; no SSL, no TLS 1.0/1.1.
- AES-256-GCM for symmetric; RSA-3072 or Ed25519 for asymmetric.
- bcrypt(cost ≥ 12), scrypt, or Argon2id for password hashing.
- Never SHA-1, MD5, DES, 3DES, RC4 for any purpose.

### A03: Injection

- SQL: parameterized queries only; never string concatenation. ORMs that produce parameterized queries are acceptable.
- LDAP/XPath/NoSQL: equivalent parameterization or escape per driver guidance.
- Shell: never invoke `Runtime.exec(String)` with user input; use `ProcessBuilder` with a `List<String>` and verify the binary path.
- Template injection: server-side templates must not render user input as template code (e.g., Velocity, Freemarker `Template.evaluate`).

### A04: Insecure Design

- Threat-model significant new flows; capture in `docs/threat-models/`.
- Rate limit destructive operations (delete, refund) at a different bucket than reads.
- Anti-automation on registration, password reset, and any endpoint that issues credentials.

### A05: Security Misconfiguration

- Containers run as non-root with read-only root filesystem (Rule 06).
- Management endpoints (Actuator, JMX, gRPC reflection) are disabled in production or bound only to internal interfaces.
- Disable directory listing, server banner, version disclosure.
- Default credentials removed at provisioning time.

### A06: Vulnerable and Outdated Components

- Dependency scan runs in CI (`x-audit-dependencies`); CRITICAL/HIGH CVEs block merge.
- SBOM generated per release (`x-audit-supply-chain`).
- License compatibility validated by `x-validate-dependency-policy`.

### A07: Identification and Authentication Failures

- Brute-force protection: account lockout after N failures, exponential backoff.
- Session tokens are cryptographically random (`SecureRandom`, ≥128 bits).
- Session timeout: 15 min idle for sensitive paths (PCI), 24h max absolute.
- Password rules per NIST SP 800-63B: ≥8 chars, no composition rules, check against breach corpus.

### A08: Software and Data Integrity Failures

- All artifacts signed (cosign for containers, GPG for jars).
- CI pipelines verify signatures before deploy.
- No `ObjectInputStream` without `ObjectInputFilter` (see Rule 12 J3).

### A09: Security Logging and Monitoring Failures

- Authentication, authorization decisions on Restricted resources, and admin actions emit audit events.
- Logs forwarded to SIEM with append-only retention.
- Alert on: failed-login bursts, sudden permission escalation, deny rate spikes.

### A10: Server-Side Request Forgery (SSRF)

- Outbound HTTP from the application uses an allowlist of destinations.
- Block private IP ranges (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.169.254`) for user-supplied URLs.
- Resolve hostnames once, validate, then use the resolved IP — prevent DNS rebinding.

## Security Headers (HTTP responses)

| Header | Value | Purpose |
|--------|-------|---------|
| `Strict-Transport-Security` | `max-age=63072000; includeSubDomains; preload` | Force HTTPS |
| `Content-Security-Policy` | `default-src 'self'; frame-ancestors 'none'` (tune per app) | XSS / clickjacking |
| `X-Content-Type-Options` | `nosniff` | MIME confusion |
| `X-Frame-Options` | `DENY` | Clickjacking (legacy fallback for CSP) |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Referrer leakage |
| `Permissions-Policy` | minimum required features | Browser feature control |
| `Cache-Control` | `no-store` on responses with sensitive data | Avoid caching tokens |

CORS: `Access-Control-Allow-Origin` must list specific origins, never `*` when credentials are involved.

## Secrets Management

| Where it lives | Allowed? |
|----------------|----------|
| Source code, committed config | Never |
| Environment variable (12-factor) | OK for non-prod and short-lived runtime injection |
| Container image layer | Never |
| Vault / AWS Secrets Manager / GCP Secret Manager | Preferred for production |
| K8s `Secret` resource | OK only if backed by external store via CSI driver or sealed-secrets; raw `Secret` is base64, not encrypted |

Rotation: secrets MUST be rotatable without code change. Application reloads on a schedule or signal; no rebuilds required.

## Session and Token Handling

- JWT signed with EdDSA or RS256 (never `none`); `exp` ≤ 15 min for access tokens; refresh tokens rotated on use.
- Cookies for browser sessions: `HttpOnly`, `Secure`, `SameSite=Strict` (or `Lax` when cross-site links are needed).
- Logout invalidates the server-side session; for JWT, maintain a revocation list for high-impact operations.

## Input Sanitization

| Risk | Defense | Library |
|------|---------|---------|
| HTML injection (XSS) | Output encoding context-aware | OWASP Java Encoder, DOMPurify |
| SQL injection | Parameterized queries | JDBC PreparedStatement, JPA |
| Command injection | Avoid shell; use `ProcessBuilder` with arg list | java.lang.ProcessBuilder |
| Path traversal | Normalize + verify under base | java.nio.file.Path::normalize |
| XML XXE | Disable external entities | `XMLInputFactory` with secure features |
| Deserialization | Restrict class graph | `ObjectInputFilter` |

## Cross-References

- `security-principles.md` (foundational principles)
- `cryptography.md` (algorithm choices)
- Rule 12 `security-anti-patterns.java.md` (concrete CWE examples)
- `knowledge/owasp-asvs/chapters-v1-v7.md` and `chapters-v8-v14.md`
