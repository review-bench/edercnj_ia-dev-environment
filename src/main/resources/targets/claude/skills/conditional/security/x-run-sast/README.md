# x-run-sast

> Static Application Security Testing -- scans source code for security vulnerabilities without executing the application.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `security.scanning.sast = true` |
| **Invocation** | `/x-run-sast [--scope all\|owasp\|custom-rules] [--severity-threshold CRITICAL\|HIGH\|MEDIUM\|LOW\|INFO]` |
| **Reads** | security (references: security-skill-template, sarif-template, security-scoring, security-principles) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when `security.scanning.sast = true` in the project configuration.

## What It Does

Analyzes source code to identify security vulnerabilities, coding errors, and compliance violations without executing the application. Automatically selects the appropriate scanning tool based on the project's build tool and language (SpotBugs, Bandit, ESLint security, gosec), with Semgrep as universal fallback. Produces SARIF 2.1.0 output with OWASP Top 10 mapping, CWE identifiers, severity scoring, and a Markdown summary report.

## Usage

```
/x-run-sast
/x-run-sast --scope owasp
/x-run-sast --severity-threshold HIGH
/x-run-sast --scope custom-rules --fix suggest
```

## See Also

- [x-run-dast](../x-run-dast/) -- Dynamic testing against a running application
- [x-scan-secrets](../x-scan-secrets/) -- Scans for leaked credentials and API keys
- [x-run-sonar-security](../x-run-sonar-security/) -- SonarQube quality gate enforcement
- [x-run-pentest](../x-run-pentest/) -- Multi-phase penetration test orchestrator
