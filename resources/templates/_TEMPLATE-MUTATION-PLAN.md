## Header

- **Story ID:** {{STORY_ID}}
- **Tool:** {{TOOL_NAME}} {{TOOL_VERSION}}
- **Stack:** {{STACK}}
- **Date:** {{DATE}}
- **Status:** PASS | FAIL

## Summary

- Stack: {{STACK}}
- Tool: {{TOOL_NAME}} {{TOOL_VERSION}}
- Status: {{STATUS}}
- Mutation Score: {{SCORE}}%
- Threshold: {{THRESHOLD}}%
- Total Mutants: {{TOTAL}}
- Killed: {{KILLED}}
- Survived: {{SURVIVED}}
- Runtime: {{RUNTIME_MIN}}m / {{CAP_MIN}}m cap

## Scope

| Package | Score % | Threshold % | Verdict |
|---------|---------|-------------|---------|
| {{PACKAGE_1}} | {{SCORE_1}} | {{THRESHOLD_1}} | PASS/FAIL |

## Surviving Mutants (Top 20)

| Class | Method | Line | Mutation |
|-------|--------|------|----------|
| {{CLASS}} | {{METHOD}} | {{LINE}} | {{MUTATION_TYPE}} |

## Configuration

```yaml
quality:
  mutation:
    enabled: {{ENABLED}}
    threshold: {{THRESHOLD}}
    runtime-cap-min: {{CAP_MIN}}
    exclude-packages: {{EXCLUDE_LIST}}
```

## Tooling

- Tool: {{TOOL_NAME}} {{TOOL_VERSION}}
- Container: {{CONTAINER_IMAGE}}
- Command: {{SANITIZED_COMMAND}}
- Execution time: {{RUNTIME_MIN}}m

## Risks and Gaps

{{RISKS_AND_GAPS}}

## Recommended Action

{{RECOMMENDED_ACTION}}
