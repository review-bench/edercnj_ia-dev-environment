# setup-environment

> Dev Environment Setup -- sets up the local development environment including container orchestrator, database, and build tools.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `orchestrator != "none"` |
| **Invocation** | `/setup-environment [--start \| --stop \| --status \| --build]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when the project includes a container orchestrator (Docker Compose, Minikube, kind, or similar) in its infrastructure configuration.

## What It Does

Manages the local development environment lifecycle: starting and stopping the container orchestrator, provisioning the database, building the application image, and verifying health checks. Supports four operations -- start (full environment bootstrap), stop (teardown and cleanup), status (running services and health), and build (rebuild and restart). Reports missing prerequisites with installation instructions.

## Usage

```
/setup-environment --start
/setup-environment --stop
/setup-environment --status
/setup-environment --build
```

## See Also

- [x-execute-api-smoke-tests](../x-execute-api-smoke-tests/) -- REST API smoke tests against deployed environment
- [x-execute-socket-smoke-tests](../x-execute-socket-smoke-tests/) -- TCP socket smoke tests against deployed environment
- [x-assess-infrastructure-security](../x-assess-infrastructure-security/) -- Infrastructure-as-Code security scanning
