# QA Specialist Review — story-0061-0001

ENGINEER: QA
STORY: story-0061-0001 (Non-Interactive Default + Working-Tree Guard)
SCORE: 32/40
STATUS: PARTIAL

---

## PASSED

- [QA-01] Test exists for each acceptance criterion — all 8 Gherkin scenarios covered by WorktreePrecheckTest
- [QA-02] Line coverage >= 95% — 100% on WorktreePrecheck, PrecheckResult, WorktreeAmbiguousException
- [QA-03] Branch coverage >= 90% — all 4 PrecheckResult states + exception paths exercised
- [QA-05] AAA pattern — every test follows Arrange (mock setup) / Act (call) / Assert (assertThat)
- [QA-07] Exception paths tested with specific assertions — both `hasMessageContaining("WORKTREE_AMBIGUOUS")` and `"OPERATIONAL_ERROR"` asserted
- [QA-08] No test interdependency — fresh `mock(ProcessRunner.class)` per test
- [QA-09] Fixtures centralized — single test class, no cross-file duplication
- [QA-10] Unique test data per test — each test configures its own `when(runner.run(any())).thenReturn(...)`
- [QA-15] TPP progression — CLEAN → DIRTY → DIVERGENT → AMBIGUOUS, simple to complex
- [QA-16] No test-after — test and implementation in same commit; test code precedes implementation in the commit
- [QA-18] TDD coverage thresholds maintained — 100% line/branch on all new code

---

## FAILED

- [QA-19] Smoke test exists and covers critical path
  - Finding: `Rule20DefaultFlipSmokeIT` (story DoD item) not implemented
  - Fix: Create `Rule20DefaultFlipSmokeIT` that invokes orchestrator without `--interactive` and asserts no `AskUserQuestion` call appears
  - Severity: MEDIUM

- [QA-20] ALL smoke tests pass
  - Finding: Formal smoke test suite not run (no `Rule20DefaultFlipSmokeIT`)
  - Fix: Implement QA-19 smoke test and verify green before merge
  - Severity: MEDIUM

---

## PARTIAL

- [QA-04] Test naming convention `[method]_[scenario]_[expected]` (1/2)
  - Finding: `whenCalled_returnsBranchName` missing method prefix — should be `currentBranch_whenCalled_returnsBranchName`
  - Fix: Rename to include method prefix

- [QA-06] Parametrized tests for data-driven scenarios (1/2)
  - Finding: The 4 `PrecheckResult` states (CLEAN/DIRTY/DIVERGENT/AMBIGUOUS) are separate `@Test` methods; a `@ParameterizedTest` would reduce duplication
  - Fix: Refactor classify-state tests to `@ParameterizedTest(MethodSource)` — low priority, current coverage is complete

- [QA-11] Edge cases covered (1/2)
  - Finding: `null` return from `ProcessRunner.run()` not tested; behavior in that case is undefined
  - Fix: Add test for `when(runner.run(any())).thenReturn(null)` — verify NPE is not silently swallowed

- [QA-12] Integration tests for real git (1/2)
  - Finding: Tests use mock `ProcessRunner`; no integration test with real `git` binary in temp directory
  - Fix: Add `@TempDir` integration test that initializes a real git repo and verifies CLEAN/DIRTY states

- [QA-13] Commits show test-first pattern (1/2)
  - Finding: Test and implementation in same commit (`31c4f5081`); RED phase not committed separately
  - Fix: Future tasks should commit RED (failing test) separately from GREEN (implementation)

- [QA-14] Explicit refactoring after green (1/2)
  - Finding: No separate REFACTOR commit after GREEN; implementation could be cleaner with extracted helpers
  - Fix: Extract `buildStatusOutput()` and `buildUpstreamOutput()` in a separate refactor commit

- [QA-17] Acceptance tests validate end-to-end behavior (1/2)
  - Finding: `WorktreePrecheckTest` covers all Gherkin but as unit tests; no separate acceptance test class
  - Fix: Rename or mark scenario-matching tests with `@Tag("acceptance")` for traceability
