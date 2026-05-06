# x-execute-e2e-tests

> Runs integration tests that validate the complete flow from request through all application layers to response, using a real database.

| | |
|---|---|
| **Category** | Testing |
| **Invocation** | `/x-execute-e2e-tests [scenario: happy-path\|error\|timeout\|persistent\|all]` |
| **Reads** | testing |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

> **Conditional skill**: This skill is included only for projects with integration test infrastructure (framework test extension and database configured).

## What It Does

Runs or implements end-to-end tests that exercise the complete application flow: inbound request, parsing, validation, business logic, persistence, and response. Tests use a real or in-memory database and cover mandatory scenarios including happy path, validation errors, not-found, duplicate/conflict, concurrent requests, and error recovery. Each test is isolated with unique identifiers and uses async polling instead of sleep for resource readiness.

## Usage

```
/x-execute-e2e-tests
/x-execute-e2e-tests happy-path
/x-execute-e2e-tests persistent
/x-execute-e2e-tests all
```

## Workflow

1. **Verify** -- Check test infrastructure prerequisites (framework test extension, database, assertion library)
2. **Execute** -- Run E2E tests filtered by scenario tag if specified
3. **Validate** -- Confirm all tests pass, no test pollution, database state verified
4. **Report** -- Output pass/fail/skip counts, duration per class, and error details

## See Also

- [x-execute-tests](../../../core/test/x-execute-tests/) -- General test execution with coverage thresholds
- [x-detect-spec-drift](../../../core/dev/x-detect-spec-drift/) -- Validates spec-code alignment before E2E runs
