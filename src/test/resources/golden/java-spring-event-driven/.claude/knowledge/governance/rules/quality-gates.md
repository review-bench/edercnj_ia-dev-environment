---
name: quality-gates
description: Full quality gates reference — coverage thresholds, mutation score, performance budget, test categories, TDD compliance
requires-capabilities: []
---
# Quality Gates — Full Reference

> **Always-loaded summary:** `.claude/rules/00-essentials.md §2 Hard Limits`
> **See also:** `.claude/knowledge/testing.md` for test patterns and conventions

## Coverage Thresholds (Non-Negotiable — Absolute Gate)

| Metric | Minimum |
|--------|---------|
| Line Coverage | ≥ 95% |
| Branch Coverage | ≥ 90% |

### Absolute-Gate Rule (RULE-005-01)

Coverage thresholds are an **absolute gate**. A PR fails the review if the
repository's line coverage is below 95% or branch coverage is below 90%,
**regardless of whether the deficit was caused by the PR or was pre-existing
on the base branch**. There is no "pre-existing exemption".

Rationale:
- Pre-existing deficits would accumulate indefinitely if grandfathered.
- A PR that does not add Java main-source code is free to add tests to close pre-existing gaps.
- The gate keeps the `develop` baseline trustworthy.

### Operator options when the gate fires

1. **Add tests in the current PR** to close the gap.
2. **Split the concerns:** open a separate PR with tests first, then rebase the feature PR.
3. **Document an explicit exception** via an ADR with a sunset date, approved by tech lead.

**Silently overriding the gate is forbidden.**

## Mutation Score Threshold

> **Gate:** `audit-mutation-score.sh` (Camada 2 CI script).
> **Enabled by:** `quality.mutation.enabled: true` in project YAML.

| Metric | Default Minimum | Config Path |
| :--- | :--- | :--- |
| Line Coverage | ≥ 95% | n/a (fixed) |
| Branch Coverage | ≥ 90% | n/a (fixed) |
| Mutation Score | ≥ 80% | `quality.mutation.threshold` (integer 0-100) |

### Regression Tolerance

| Config Path | Default | Semantics |
| :--- | :--- | :--- |
| `quality.mutation.regression-tolerance-pct` | `5` | Max allowed regression % vs baseline before blocking |

### Stage Policy (WARN → FAIL)

1. **First release** with `quality.mutation.enabled=true` (`release_count=0`): audit emits WARN only; baseline is initialized.
2. **Second release+** (`release_count≥1`): threshold and regression violations are hard FAIL (exit 1).

Exit codes: `0=OK`, `1=MUTATION_SCORE_VIOLATION`, `1=MUTATION_REGRESSION`, `2=OPERATIONAL_ERROR`, `3=BASELINE_CORRUPT`.

## Performance Budget

> **Gate:** `audit-perf-baseline.sh` (Camada 2 CI script).
> **Enabled by:** `quality.performance.enabled: true` in project YAML.

`governance/baselines/performance-baseline.json` is the source of truth.
Updates require an entry in `governance/baselines/perf-baseline-updates.log` (append-only).

Silent overwrite detected by `audit-perf-baseline.sh` via `git diff HEAD~1..HEAD` → exit 1 `PERF_BASELINE_VIOLATION`.

## Test Categories

1. **Unit** — domain models, engines, business rules (no mocks of domain; mock only external)
2. **Integration** — database + framework (real or in-memory DB)
3. **API** — HTTP/gRPC/GraphQL endpoints (status codes, response bodies, error formats)
4. **Contract** — parametrized business rules (one row per scenario)
5. **E2E** — full flow with real database (containers)
6. **Performance** — latency SLAs, throughput, resource usage
7. **Smoke** — black-box against running environment

## Test Naming

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

`@DisplayName` / docstrings provide readability but do NOT replace method name convention.

## Merge Checklist

- [ ] All tests passing
- [ ] Coverage ≥ 95% line, ≥ 90% branch (absolute gate)
- [ ] Mutation score ≥ 80% AND regression ≤ tolerance (when mutation enabled)
- [ ] Performance baseline not silently overwritten
- [ ] Zero compiler/linter warnings
- [ ] DB migration applied and tested (if applicable)
- [ ] Security review for sensitive changes
- [ ] Commits show test-first pattern
- [ ] Explicit refactoring after green
- [ ] Tests are incremental (simple to complex via TPP)
- [ ] No cross-file consistency violations
- [ ] No weak assertions

## Forbidden

- Skipping tests to make CI pass
- Mocking domain logic
- Using production data in tests
- Depending on test execution order
- `sleep()` for async waiting (use polling with timeout)
- Weak assertions: `isNotNull()` alone is never sufficient
- Test files > 250 lines without nested class organization
- Duplicate type definitions across test files

## TDD Compliance

- **Double-Loop TDD**: Outer loop (acceptance test, failing) drives inner loop (unit tests, Red-Green-Refactor)
- **Transformation Priority Premise (TPP)**: Order tests from simple to complex
- **Atomic TDD commits**: Each Red-Green-Refactor cycle produces one or more atomic commits
