# Task Breakdown -- {{STORY_ID}}

## Header

| Field | Value |
|-------|-------|
| Story ID | {{STORY_ID}} |
| Epic ID | {{EPIC_ID}} |
| Date | {{DATE}} |
| Author | {{AUTHOR}} |
| Template Version | 1.0.0 |

## Summary

| Metric | Value |
|--------|-------|
| Total Tasks | {{TOTAL_TASKS}} |
| Parallelizable Tasks | {{PARALLEL_TASKS}} |
| Estimated Effort | {{ESTIMATED_EFFORT}} |
| Mode | {{MODE}} |

## Dependency Graph

```mermaid
graph TD
    {{DEPENDENCY_GRAPH}}
```

## Tasks Table

| Task ID | Test Scenario Ref | TPP Level | Type | Phase | Layer | Components | Parallel | Depends On | Tier | Budget | Agent | DoD |
|---------|------------------|-----------|------|-------|-------|-----------|----------|-----------|------|--------|-------|-----|
| TASK-{{EPIC_ID}}-{{STORY_ID}}-001 | {{TEST_REF}} | nil | UT | RED | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | -- | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-{{EPIC_ID}}-{{STORY_ID}}-002 | {{TEST_REF}} | nil | UT | GREEN | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-{{EPIC_ID}}-{{STORY_ID}}-001 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-{{EPIC_ID}}-{{STORY_ID}}-003 | {{TEST_REF}} | nil | UT | REFACTOR | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-{{EPIC_ID}}-{{STORY_ID}}-002 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-{{EPIC_ID}}-{{STORY_ID}}-004 | {{TEST_REF}} | constant | UT | RED | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-{{EPIC_ID}}-{{STORY_ID}}-003 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-{{EPIC_ID}}-{{STORY_ID}}-005 | {{TEST_REF}} | constant | UT | GREEN | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-{{EPIC_ID}}-{{STORY_ID}}-004 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-{{EPIC_ID}}-{{STORY_ID}}-006 | {{TEST_REF}} | constant | UT | REFACTOR | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-{{EPIC_ID}}-{{STORY_ID}}-005 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-{{EPIC_ID}}-{{STORY_ID}}-007 | {{TEST_REF}} | scalar | AT | RED | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-{{EPIC_ID}}-{{STORY_ID}}-006 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |

## Escalation Notes

| Task ID | Reason | Recommended Action |
|---------|--------|--------------------|
| {{TASK_ID}} | {{REASON}} | {{ACTION}} |

## Mandatory Review and Documentation Tasks

> These tasks are automatically included in every story. They MUST NOT be removed or marked N/A.
> They depend on all code tasks being completed. IDs follow the pattern: epic-story-REV-{ROLE}.

| Task ID | Type | Skill | Depends On |
|---------|------|-------|-----------|
| TASK-{{EPIC_ID}}-{{STORY_ID}}-REV-TL | Tech Lead Review | x-review-pr | All code tasks |
| TASK-{{EPIC_ID}}-{{STORY_ID}}-REV-SEC | Security Review | x-review-security | All code tasks |
| TASK-{{EPIC_ID}}-{{STORY_ID}}-REV-QA | QA Review | x-review-qa | All code tasks |
| TASK-{{EPIC_ID}}-{{STORY_ID}}-DOC | Documentation Update | (stack-specific) | All review tasks |
