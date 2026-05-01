---
name: dast-playbook
description: DAST guidance — smoke vs full tier, finding remediation OWASP Top 10 mapping, Nuclei template curation policy, ZAP active scan policy tuning.
visibility: internal
user-invocable: false
requires-capabilities:
  - quality.dast.zap-passive
  - quality.dast.zap-active
  - quality.dast.nuclei
---

> 🔒 **KNOWLEDGE PACK** — Referenced internally by `x-pentest-dynamic`. Not user-invocable.

# DAST Playbook

## Smoke vs. Full Tier

| Aspect | Smoke (PR gate) | Full (nightly) |
| :--- | :--- | :--- |
| **ZAP mode** | Passive (read-only spider) | Active (injection payloads) |
| **ZAP risk** | No attack payloads — safe against any target | Attack payloads — only against controlled environments |
| **Nuclei templates** | Top-50 lightweight (CVE-focused, low noise) | Full community template set |
| **Duration** | 2-5 min (ZAP) + 2-3 min (Nuclei) = ~8 min | 10-15 min (ZAP) + 10+ min (Nuclei) = ~35 min |
| **Blocks on** | CRITICAL findings | HIGH + CRITICAL findings |
| **Pipeline** | PR open/update | Nightly workflow |

## ZAP Active Scan Policies

| Policy | Focus | Use case |
| :--- | :--- | :--- |
| `default` | OWASP Top 10: SQLi, XSS, CSRF, path traversal, auth bypass | General-purpose API security |
| `custom-pci` | PCI-DSS 4.0 scope: SQLi, XSS, authentication strength, session management | Payment processing systems |
| `custom-lgpd` | LGPD/GDPR scope: data exposure, access control, logging of PII | Brazilian data-regulated systems |

Configure via `quality.dast.zap.active-scan-policy` in project YAML.

## Nuclei Template Curation Policy

Nuclei runs community templates from the configured version series. To manage noise:

- **Smoke tier**: `-tags top-50` selects the 50 highest-signal templates (low false-positive rate, fast execution).
- **Full tier**: Full template set; expect ~5-15% false positives — triage required.

**False positive management:**
Add template IDs to `governance/baselines/nuclei-false-positive-baseline.txt`:
```
CVE-2021-XXXXX  # False positive on mock endpoint /health — confirmed not vulnerable
```

The `x-pentest-dynamic` skill reads this file and suppresses matching findings from the gate decision (findings still appear in the report, marked as `[suppressed]`).

**Version pinning:**
Templates version must be `vN.x` (major series) or `vN.M.P` (full pin). `latest`/`master`/`HEAD` are rejected at parse time (`NUCLEI_VERSION_UNPINNED`). Major version upgrades (`v9.x` → `v10.x`) require:
1. Smoke test rerun on a sample project to catch template-set changes.
2. Explicit PR updating `quality.dast.nuclei.templates-version`.
3. Log entry in `governance/baselines/nuclei-version-upgrades.log`.

## Finding → Remediation Map (OWASP Top 10)

| OWASP Category | Common ZAP Alert | Common Nuclei Template | Remediation |
| :--- | :--- | :--- | :--- |
| A01 Broken Access Control | Directory Browsing, HTTP Parameter Pollution | `cve-2021-xxx-auth-bypass` | Enforce RBAC, validate object-level auth |
| A02 Cryptographic Failures | Weak TLS, Insecure Cookies | `tls-v1` | TLS 1.2+ only; `HttpOnly; Secure` cookies |
| A03 Injection | SQL Injection, XSS (reflected/stored) | `sqli-generic` | Parameterized queries; CSP headers |
| A04 Insecure Design | — | `missing-security-headers` | Security headers: HSTS, X-Frame-Options |
| A05 Security Misconfiguration | Server info disclosure, CORS wildcard | `nginx-config-exposure` | Remove version headers; restrict CORS |
| A06 Vulnerable Components | — | Nuclei CVE templates | Upgrade dependency; apply patch |
| A07 Auth Failures | Password in GET, Default Credentials | `default-credentials-generic` | Auth middleware; disable default creds |
| A08 Data Integrity Failures | — | `missing-sri-hash` | Subresource Integrity for CDN assets |
| A09 Logging Failures | — | `missing-access-log` | Enable structured access logging |
| A10 SSRF | SSRF probe | `ssrf-generic` | Allowlist outbound requests |

## Target Environments

| Target | Description | Safe for active scan? |
| :--- | :--- | :--- |
| `local-container` | Docker container started for CI | Yes — disposable |
| `preview-env` | Ephemeral preview deployment | Yes — isolated per PR |
| `staging` | Shared staging environment | Active scan: requires team notification; passive: OK |
| `production` | **FORBIDDEN** | Never — production scan is explicitly blocked by `DAST_TARGET_PRODUCTION_FORBIDDEN` |

## Post-Finding Actions

When HIGH/CRITICAL findings block the pipeline:

1. **Triage**: Determine if finding is a true positive or false positive.
2. **True positive**: Fix the vulnerability before merging. Create a separate security-fix PR.
3. **False positive**: Add to `governance/baselines/nuclei-false-positive-baseline.txt` or ZAP context file with documented rationale. Re-run the gate.
4. **Known limitation** (intentional partial coverage): Document in ADR with sunset date.

**Emergency bypass:** `CLAUDE_RECOVERY_MODE=1` + `--skip-dast` flag (Rule 27 Exception 2 for hotfix branches only). All bypasses are recorded in `governance/baselines/execution-integrity-baseline.txt`.
