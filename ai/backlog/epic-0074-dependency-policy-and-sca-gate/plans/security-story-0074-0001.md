# Security Assessment — story-0074-0001

## Risk Level: LOW

## Findings

### Finding 1 — MEDIUM: Policy parsing accepts arbitrary string in `deniedCves`
The parser does not validate that CVE identifiers follow the `CVE-YYYY-NNNNN` pattern.
**Mitigation:** skill-level validation in `x-dep-policy-validate` (story-0074-0002); parser stores raw strings.
**Residual risk:** none — the gate validates pattern at runtime, parser is pure data.

### Finding 2 — LOW: Wildcard in JVM artifactId must not expand to shell glob
The `"*"` wildcard is a pattern marker stored as a string, never expanded via shell. Confirmed: no `ProcessBuilder` or filesystem call in `DependencyPolicyConfig`.
**Mitigation:** enforce in parser — wildcard only valid in JVM form; NPM/Go reject it.

### Finding 3 — INFO: License whitelist is opt-in
`allowed-licenses` empty = gate disabled for license dimension (not "accept all"). `BlockAction` defaults ensure missing whitelist = no allowances, not open allowance.
**Mitigation:** parser enforces `allowed-licenses` non-empty when `block-on.license = any-violation`.

## No OWASP Top 10 Impact
- No SQL (no persistence layer touched)
- No web input (pure config parser)
- No secrets in scope

## Recommendation
Proceed with LOW residual risk. Findings 1+2 are implementation constraints documented in parser validation.
