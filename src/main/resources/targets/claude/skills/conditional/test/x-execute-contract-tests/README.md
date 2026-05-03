# x-execute-contract-tests

> Runs consumer-driven contract tests (Pact, Spring Cloud Contract) to verify API compatibility between services.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `testing.contract_tests = true` |
| **Invocation** | `/x-execute-contract-tests [--provider \| --consumer \| --all]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when `testing.contract_tests = true` in the project configuration.

## What It Does

Runs consumer-driven contract tests to verify API compatibility between services. Supports both consumer-side (pact generation) and provider-side (pact verification) workflows using Pact or Spring Cloud Contract. Consumer mode generates pact files from consumer expectations, provider mode verifies the service against published pacts, and combined mode runs the full cycle.

## Usage

```
/x-execute-contract-tests --consumer
/x-execute-contract-tests --provider
/x-execute-contract-tests --all
```

## See Also

- [x-lint-contract-tests](../x-lint-contract-tests/) -- API contract validation (OpenAPI, AsyncAPI, Protobuf)
- [x-review-api](../x-review-api/) -- REST API design review
- [x-execute-e2e-tests](../x-execute-e2e-tests/) -- End-to-end integration tests
