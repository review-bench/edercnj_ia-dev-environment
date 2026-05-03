# x-run-dast

> Dynamic Application Security Testing -- tests the running application for XSS, injection, misconfiguration, and information disclosure vulnerabilities using OWASP ZAP or Nuclei.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `security.scanning.dast = true` |
| **Invocation** | `/x-run-dast --target <URL> [--env local\|dev\|homolog\|prod] [--mode passive\|active\|full] [--openapi <PATH>] [--confirm-prod] [--auth-token <TOKEN>]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when `security.scanning.dast = true` in the project configuration.

## What It Does

Orchestrates Dynamic Application Security Testing against a running application, complementing SAST by testing from outside-in. Simulates real attacks to detect runtime vulnerabilities that static analysis cannot find: missing security headers, insecure cookies, CORS misconfiguration, injection flaws, and information disclosure. Automatically selects OWASP ZAP, Nuclei, or nikto based on tool availability, and supports OpenAPI-driven scanning with environment-based restrictions.

## Usage

```
/x-run-dast --target http://localhost:8080
/x-run-dast --target https://app.staging.example.com --env homolog --mode passive
/x-run-dast --target http://localhost:8080 --openapi docs/openapi.yaml
```

## See Also

- [x-run-sast](../x-run-sast/) -- Static code analysis for vulnerabilities
- [x-run-pentest](../x-run-pentest/) -- Multi-phase penetration test orchestrator
- [x-execute-api-smoke-tests](../x-execute-api-smoke-tests/) -- REST API smoke tests against deployed environments
