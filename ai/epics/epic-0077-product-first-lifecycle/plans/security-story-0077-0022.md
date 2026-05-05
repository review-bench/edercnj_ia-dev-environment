# Security Assessment — story-0077-0022

**Story:** x-internal-rnf-validate (no-relax markers + justification gate)
**Date:** 2026-05-05

## Threat Model

| Surface | Threat | Mitigation |
| :--- | :--- | :--- |
| `--override` CLI arg | Malformed spec causes unhandled exception | parseOne() wraps in IllegalArgumentException; command catches → exit 2 |
| `--override` CLI arg | Injection via category or value fields | No shell eval; fields are used only as domain enum lookups and string comparisons |
| Empty justification bypass | Null/blank justification bypasses gate | Domain validator checks `isBlank()` — empty string is also blocked |
| SECURITY/COMPLIANCE bypass | Attacker provides justification to bypass hard-block | Hard-block ignores justification entirely (HARD_BLOCKED set in validator) |

## Security Properties Preserved

1. SECURITY and COMPLIANCE categories are **hard-blocked** — no runtime argument can relax them.
2. Other categories require non-blank justification — empty/whitespace not accepted.
3. `--dry-run` only changes exit code, not validation output; errors are still printed.
4. No file I/O or network calls — pure in-process validation.

## OWASP Concerns

- **A01 Broken Access Control**: Not applicable (CLI tool, no multi-user context).
- **A03 Injection**: Not applicable (no SQL/command execution; category parsed via enum lookup).
- **A09 Security Logging Failures**: Validation errors written to stderr or stdout via CommandSpec writer — no sensitive data in output.

## Verdict

LOW risk. Domain hard-blocks are the primary security control and they are already tested in domain layer. CLI adapter adds no new attack surface.
