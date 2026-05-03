# x-execute-tests

> Runs tests with coverage reporting and threshold validation. Use whenever writing, running, or analyzing tests. Triggers on: test, coverage, TDD, unit test, integration test, test failure, coverage gap, or Definition of Done validation.

| | |
|---|---|
| **Category** | Testing |
| **Invocation** | `/x-execute-tests [ClassName or package or --coverage]` |
| **Reads** | testing |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Executes project tests and enforces strict coverage thresholds (line >= 95%, branch >= 90%). Supports running all tests, targeting a specific class or package, and generating per-class coverage reports. Also provides a traceability matrix mode that maps Gherkin scenarios to test methods, identifying unmapped requirements and orphan tests.

## Usage

```
/x-execute-tests
/x-execute-tests MyServiceTest
/x-execute-tests --coverage
/x-execute-tests --traceability STORY-0007-0003
```

## Workflow

1. **Execute** -- Run tests using the project build tool (all tests or filtered by class/package)
2. **Analyze** -- Parse coverage report and generate per-class line/branch coverage table
3. **Validate** -- Check coverage against thresholds (95% line, 90% branch)
4. **Report** -- Output pass/fail counts, coverage summary, and gap identification

## Outputs

| Artifact | Path |
|----------|------|
| Traceability matrix | `results/traceability/traceability-{ID}-{YYYY-MM-DD}.md` |

## See Also

- [x-detect-spec-drift](../x-detect-spec-drift/) -- Detects drift between story specs and implemented code
- [x-execute-e2e-tests](../../conditional/x-execute-e2e-tests/) -- End-to-end tests with real database
