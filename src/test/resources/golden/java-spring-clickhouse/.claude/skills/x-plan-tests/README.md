# x-plan-tests

> Generates a Double-Loop TDD test plan with TPP-ordered scenarios before implementation. Delegates KP reading to a context-gathering subagent, then produces structured Acceptance Tests (outer loop) and Unit Tests in Transformation Priority Premise order (inner loop).

| | |
|---|---|
| **Category** | Planning |
| **Invocation** | `/x-plan-tests [STORY-ID]` |
| **Reads** | testing, architecture |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Produces a test plan that serves as the implementation roadmap for TDD. It maps each acceptance criterion to an outer-loop test and decomposes inner-loop unit tests in strict Transformation Priority Premise order (degenerate cases first, edge cases last). The plan includes coverage estimation and a completeness validation ensuring 95% line / 90% branch coverage targets are achievable.

## Usage

```
/x-plan-tests ai/epics/epic-XXXX/story-XXXX-YYYY.md
/x-plan-tests story-0012-0003
```

## Workflow

1. Check for existing test plan and assess staleness (idempotency pre-check)
2. Subagent reads testing and architecture KPs, story, template, and existing code patterns
3. Generate acceptance tests (outer loop) from Gherkin scenarios
4. Generate unit tests (inner loop) in strict TPP order across 6 levels
5. Generate integration tests for cross-component interactions
6. Estimate coverage per class and validate completeness against thresholds

## Outputs

| Artifact | Path |
|----------|------|
| Test plan | `ai/epics/epic-XXXX/plans/tests-story-XXXX-YYYY.md` |

## See Also

- [x-implement-task](../x-implement-task/) — Consumes the test plan to drive TDD implementation
- [x-implement-story](../x-implement-story/) — Invokes this skill during Phase 1B
