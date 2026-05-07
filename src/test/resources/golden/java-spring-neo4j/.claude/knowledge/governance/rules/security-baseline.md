---
name: security-baseline
description: Full security baseline reference — secure defaults, defensive coding, OWASP controls
requires-capabilities: []
---
# Security Baseline — Full Reference

> **Always-loaded summary:** `.claude/rules/00-essentials.md §4 Forbidden Top-Level`
> **See also:** `.claude/knowledge/security/index.md` for OWASP Top 10 and pentest readiness
> **See also:** `.claude/knowledge/security/anti-patterns-java.md` for Java CWE code examples

## Secure Defaults (Non-Negotiable)

| Practice | Requirement |
|----------|-------------|
| Input deserialization | Explicit safe/strict mode (e.g., SafeConstructor, strict JSON parser) |
| String escaping | Full spec compliance (RFC 8259 for JSON, OWASP for HTML/XML) |
| Temp files/directories | Explicit restrictive permissions (owner-only: 700/600) |
| Path operations | Normalize + reject traversal (`..`, symlinks) before any I/O |
| Error messages | Never expose internal paths, stack traces, or class names to end users |

## Forbidden

- Deserializing untrusted input without explicit safe mode
- Partial escaping (escaping only some special characters)
- Following symlinks in file operations without explicit opt-in
- Hardcoded secrets, tokens, or credentials anywhere in source
- `Math.random()` / `rand()` for security-sensitive values (use cryptographic RNG)

## Defensive Coding

- All path inputs: canonicalize, then verify prefix against allowed base directory
- Filename sanitization: multi-pass until idempotent (single pass of `..` removal is insufficient)
- Temp directories: always set explicit permissions, always clean up in `finally` / `defer`

## Automated Verification

When security scanning is enabled in project YAML:

| Flag | Skill | Trigger |
|------|-------|---------|
| `quality.scanning.sast: true` | `x-run-sast` | Pre-merge |
| `quality.scanning.secretScan: true` | `x-scan-secrets` | Pre-commit |
| `quality.scanning.dast: true` | `x-evaluate-hardening` | Post-deploy |

> Read `.claude/knowledge/security/index.md` for input validation patterns, secrets management, and security headers.
