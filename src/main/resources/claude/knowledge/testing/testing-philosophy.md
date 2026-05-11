# Testing Philosophy

How and when to test, what to test against, and where the line between unit and integration sits in this project.

## Why We Test

1. **Specification.** Tests describe what the code is supposed to do, in code. Reading tests is the fastest way to understand a module.
2. **Regression net.** Changes that break behavior fail visibly, not silently in production.
3. **Design pressure.** Code that is hard to test is usually hard to use. Untestable code is a design smell.
4. **Refactoring safety.** Without tests, refactoring is rewriting.

Tests are not optional artifacts produced after code. They are part of the implementation.

## TDD — Red, Green, Refactor

The default cycle for new behavior:

1. **Red** — write a failing test that describes the next piece of behavior. The test must fail because the behavior is missing, not because the test is wrong.
2. **Green** — write the smallest production code change that makes the test pass. Do not generalize prematurely.
3. **Refactor** — improve the design while tests stay green. Rename, extract, inline. No new behavior.

Each cycle produces one atomic commit (Rule 25). The `/x-drive-tdd` skill enforces this loop.

### Why TDD, not test-after

- Test-after tends to encode whatever the code already does, missing edge cases the author did not think of.
- TDD forces the API to be designed from the consumer's perspective.
- TDD-produced suites have higher mutation kill rates than test-after suites of comparable line coverage.

## Test Pyramid

| Layer | Volume | Speed | Scope |
|-------|--------|-------|-------|
| **Unit** | Most (~70%) | <50ms each | One class, no I/O |
| **Integration** | Medium (~20%) | <2s each | Module with real DB / message broker via Testcontainers |
| **End-to-End** | Few (~10%) | <30s each | Full stack via HTTP / gRPC, real downstream stubs |

Avoid the **ice-cream cone** (lots of E2E, few units). E2E tests are slow and flaky; they buy little design pressure.

Avoid the **hourglass** (lots of units, lots of E2E, no integration). Without integration, real I/O bugs (transactions, serialization, drivers) escape to production.

## FIRST Principles

Good tests are:

- **Fast** — A single test runs in milliseconds. A full unit suite runs in seconds.
- **Independent** — Tests do not share state. Order does not matter.
- **Repeatable** — Same input → same result. No flaky network, no clock dependencies (use a `Clock` abstraction).
- **Self-validating** — Tests assert outcomes; no manual log inspection required.
- **Timely** — Tests are written alongside the code, not weeks later.

## What to Test

| Worth testing | Skip |
|---------------|------|
| Business rules (calculations, decisions, state transitions) | Plain getters / setters |
| Error paths and edge cases | Trivial delegation to a tested library |
| Public API contracts (preconditions, postconditions, error types) | Logging side effects |
| Boundary values (0, max, empty, null, very large) | Generated code |
| State-changing operations (creation, mutation, deletion) | DTO field declarations |
| Concurrent behavior when concurrency matters | Spring/CDI config that the framework validates |

If a test would only assert "the framework calls my method" — skip it. If it asserts "given X, my method returns Y," keep it.

## Mocking — When, What, How

| When to mock | When NOT to mock |
|--------------|------------------|
| Slow dependencies (network, disk for unit tests) | Value objects, DTOs, simple data classes |
| Non-deterministic dependencies (time, random) | Pure functions |
| External SaaS / third-party with rate limits | Your own domain model |
| Hard-to-trigger error paths (timeouts, network failures) | Methods you own and could test directly |

### What to mock

- Outbound ports (interfaces at hexagonal boundary) — never the SUT itself.
- Mock at architectural seams, not at every internal collaborator. Over-mocking produces tests that pass while the system is broken.

### Don't mock what you can't change

External libraries — wrap them in your own port, mock the port. Mocking another team's class is brittle and ignores upgrade-time breakage.

## Integration vs Unit — The Line

A test is **integration** if it:
- Loads the Spring/CDI/Quarkus context
- Talks to a real database via Testcontainers (not H2 imposter)
- Talks to a real broker (Kafka, RabbitMQ) via Testcontainers
- Exercises a real HTTP client/server pair

Everything else is **unit**. The DB connection rule is strict: a "unit test" that uses H2 to fake Postgres is not a unit test — it's a mediocre integration test. Use Testcontainers for the real engine (Rule 09).

## Test Doubles Terminology

| Type | Definition |
|------|------------|
| **Dummy** | Passed but never used (e.g., null logger) |
| **Stub** | Returns canned responses (e.g., `when(x).thenReturn(y)`) |
| **Spy** | Real object with some calls verifiable |
| **Mock** | Verifies interactions (e.g., `verify(x).method()`) |
| **Fake** | Working but simplified implementation (e.g., in-memory repository) |

Prefer **fakes** for collaborators with simple semantics; prefer **stubs** for return values; reserve **mocks** for interaction-critical paths. Excessive `verify()` calls indicate the test is over-coupled to implementation.

## Coverage as a Floor, Not a Goal

| Threshold | Source |
|-----------|--------|
| Project minimum (line) | ≥ 95% (Rule 05 — absolute gate, RULE-005-01) |
| Project minimum (branch) | ≥ 90% (Rule 05 — absolute gate, RULE-005-01) |
| Mutation kill rate (when enabled) | 60% (`x-execute-mutation-tests`) |

Coverage is necessary, not sufficient. 100% line coverage with no assertions catches nothing. Use mutation testing to confirm tests actually fail when the code breaks (`x-execute-mutation-tests`).

## Production Test Code Standards

Test code IS production code:
- Linted, formatted, type-checked
- No `Thread.sleep` (Rule 10 ANTI-009 — use `Awaitility`)
- No hardcoded test data with real PII or real card numbers
- No `@Disabled` / `@Ignore` without a tracking ticket
- No commented-out tests

## Cross-References

- Rule 05 — `quality-gates.md` (coverage thresholds, forbidden patterns)
- Rule 09 — `data-management.md` (Testcontainers requirement for DB tests)
- Rule 10 — ANTI-009 (`Thread.sleep` in tests)
- `knowledge/testing/testing-conventions.md` (naming, fixtures, parametrized tests)
- `knowledge/testing/contract-openapi.md`, `contract-events.md`, `contract-grpc.md`
- `knowledge/testing/mutation-{java,go,python,js}.md`
