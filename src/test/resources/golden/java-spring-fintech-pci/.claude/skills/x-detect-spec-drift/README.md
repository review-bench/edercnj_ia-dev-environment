# x-detect-spec-drift

> Detects spec-code drift by comparing story data contracts, endpoints, and Gherkin scenarios against implemented code. Supports standalone mode (full report) and inline mode (compact output for TDD loop integration in x-implement-story Phase 2).

| | |
|---|---|
| **Category** | Testing |
| **Invocation** | `/x-detect-spec-drift [STORY-ID] [--mode standalone\|inline]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Compares story specifications against implemented source code to detect drift. Validates that mandatory data contract fields exist in DTOs, declared endpoints are present in controllers, and Gherkin scenarios have corresponding acceptance tests. Operates in standalone mode (full itemized report) or inline mode (compact single-line output integrated into the TDD loop of x-implement-story Phase 2).

## Usage

```
/x-detect-spec-drift STORY-0001-0002
/x-detect-spec-drift STORY-0001-0002 --mode inline
```

## Workflow

1. **Parse** -- Read story file and extract data contracts, endpoints, and Gherkin scenarios
2. **Scan** -- Search source code for matching fields, route annotations, and test references
3. **Check** -- Compare spec vs code, classify each item as PASS, WARN, or FAIL
4. **Report** -- Generate itemized report with severity (standalone) or compact summary (inline)

## See Also

- [x-execute-tests](../x-execute-tests/) -- Runs tests and validates coverage thresholds
- [x-implement-story](../x-implement-story/) -- Calls inline mode during Phase 2 TDD loop
