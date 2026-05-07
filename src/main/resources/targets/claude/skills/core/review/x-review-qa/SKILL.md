---
name: x-review-qa
description: "QA specialist review: validates test coverage, TDD compliance, test naming, fixtures, parametrized tests, and acceptance criteria coverage."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash, Agent
argument-hint: "[PR number or file paths]"
requires-capabilities: []
fragment-slot: { slot: review-specialist, fragment-id: qa, fragment-order: 100 }
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: QA Specialist Review

## Purpose

Review code changes for QA compliance: test coverage thresholds, TDD process adherence, test naming conventions, fixture centralization, parametrized tests for data-driven scenarios, and acceptance criteria coverage. Validates that tests follow the Transformation Priority Premise (TPP) and Double-Loop TDD.

## When to Use

- Pre-PR quality validation for test quality
- Verifying TDD compliance in commit history
- Ensuring coverage thresholds are met
- Checking test naming and organization

## Triggers

- `/x-review-qa 42` -- review PR #42 for QA compliance
- `/x-review-qa src/test/` -- review test files at specific paths
- `/x-review-qa` -- review all current test changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| testing | `knowledge/testing.md` | Test categories, coverage thresholds, fixture patterns, TDD workflow |

## Checklist (20 Items, Max Score: /40 full scale; adjusted max excludes N/A items)

Each applicable item scores 0 (missing), 1 (partial), or 2 (fully compliant). Items
marked N/A are excluded from BOTH the earned score and the maximum possible score.
When `testing.smoke_tests == false`, QA-19 and QA-20 are automatically marked N/A,
so the adjusted maximum is /36 instead of /40. Report the adjusted max in the final
score line (see Output Template below).

### Coverage & Criteria (QA-01 to QA-03)

| # | Item | Score |
|---|------|-------|
| QA-01 | Test exists for each acceptance criterion | /2 |
| QA-02 | Line coverage >= 95% (absolute gate — Rule 05 RULE-005-01) | /2 |
| QA-03 | Branch coverage >= 90% (absolute gate — Rule 05 RULE-005-01) | /2 |

> **Absolute-gate note (Rule 05 RULE-005-01):** QA-02 and QA-03 evaluate the
> repository's current coverage, not just the PR diff. The specialist MUST
> fail these items regardless of whether the deficit was introduced by this
> PR or was pre-existing on the base branch. Pre-existing deficits must be
> closed in the current PR or in a predecessor PR; no "pre-existing" exemption
> is permitted. The only escape path is an approved ADR that temporarily
> lowers the gate for a specific package (with sunset date).

### Test Quality (QA-04 to QA-10)

| # | Item | Score |
|---|------|-------|
| QA-04 | Test naming convention followed: `[method]_[scenario]_[expected]` | /2 |
| QA-05 | AAA pattern (Arrange-Act-Assert) in every test | /2 |
| QA-06 | Parametrized tests for data-driven scenarios | /2 |
| QA-07 | Exception paths tested with specific assertions | /2 |
| QA-08 | No test interdependency (tests run in any order) | /2 |
| QA-09 | Fixtures centralized (no duplicate records/classes across test files) | /2 |
| QA-10 | Unique test data per test (no shared mutable state) | /2 |

### Test Completeness (QA-11 to QA-12)

| # | Item | Score |
|---|------|-------|
| QA-11 | Edge cases covered (null, empty, boundary values) | /2 |
| QA-12 | Integration tests for DB/API interactions | /2 |

### TDD Compliance (QA-13 to QA-18)

| # | Item | Score |
|---|------|-------|
| QA-13 | Commits show test-first pattern (test precedes implementation in git log) | /2 |
| QA-14 | Explicit refactoring after green (separate refactor commits) | /2 |
| QA-15 | Tests follow TPP progression (simple to complex) | /2 |
| QA-16 | No test written after implementation (test-after is a violation) | /2 |
| QA-17 | Acceptance tests validate end-to-end behavior | /2 |
| QA-18 | TDD coverage thresholds maintained across all modules | /2 |

### Smoke Test Verification (QA-19 to QA-20) — EPIC-0042

| # | Item | Score |
|---|------|-------|
| QA-19 | Smoke tests exist and cover critical path (when `testing.smoke_tests == true`; N/A when false) | /2 |
| QA-20 | ALL smoke tests pass — `{{SMOKE_COMMAND}}` executed with zero failures (when `testing.smoke_tests == true`; N/A when false) | /2 |

## Workflow

### Step 1 -- Gather Context

Collect the review target: PR number or file paths from args. Run:
```bash
git diff --name-only HEAD~1..HEAD 2>/dev/null || git diff --name-only --cached
```

### Step 2 -- Dispatch to QA Engineer Agent

    Agent(
      subagent_type: "qa-engineer",
      description: "QA specialist review for {target}",
      prompt: "Review the code changes for QA compliance. Target: {target}. Run `git diff HEAD~1..HEAD` to get the diff. Read `knowledge/testing.md` for project test conventions. Apply your full QA checklist. Produce output in this exact format:\n\nENGINEER: QA\nSTORY: {target}\nSCORE: XX/36\nSTATUS: Approved | Rejected | Partial\n---\nPASSED:\n- [QA-XX] Description (2/2)\nFAILED:\n- [QA-XX] Description (0/2) -- file:line -- Fix: suggestion [SEVERITY]\nPARTIAL:\n- [QA-XX] Description (1/2) -- file:line -- Improvement: suggestion [SEVERITY]"
    )

## Output Format

```
ENGINEER: QA
STORY: [story-id or change description]
SCORE: XX/40 (or XX/36 when QA-19 and QA-20 are N/A — use the adjusted max that
       excludes N/A items; see Checklist header for the N/A rules)

STATUS: PASS | FAIL | PARTIAL

### PASSED
- [QA-XX] [Item description]

### FAILED
- [QA-XX] [Item description]
  - Finding: [file:line] [issue description]
  - Fix: [remediation guidance]

### PARTIAL
- [QA-XX] [Item description]
  - Finding: [partial compliance details]
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No test files found | Report INFO: no test code discovered |
| Coverage tool not configured | Warn and skip QA-02, QA-03 |
| Git log not available | Warn and skip QA-13, QA-14, QA-16 |
| Smoke test failure (QA-20) | STATUS becomes Rejected regardless of other scores |
| `testing.smoke_tests == false` | Mark QA-19, QA-20 as N/A (excluded from max score) |
