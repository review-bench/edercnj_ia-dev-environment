# QA Specialist Review — story-0061-0002

ENGINEER: QA
STORY: story-0061-0002 (ScriptsAssembler Stack-Aware + Templates por Stack)
SCORE: 36/40
STATUS: PARTIAL

---

## PASSED

- [QA-02] Line coverage >= 95% — 66 tests covering all paths in StackResolver + ScriptsAssembler
- [QA-03] Branch coverage >= 90% — all branches in resolveStack() and isStackSafe() covered
- [QA-04] Test naming convention — `springBoot_generates10ScriptsWithActuatorAudit`, `knownCombination_returnsExpectedStack` all follow [scenario]_[expected] pattern
- [QA-05] AAA pattern — all tests: build config (A), assemble/resolve (A), assertThat (A)
- [QA-06] Parametrized tests — StackResolverTest uses `@ParameterizedTest(CsvSource)` for 9 stack combinations (java-maven, spring-boot, go, python, node, etc.)
- [QA-07] Exception paths — `nullLanguage_throwsIllegalArgument`, security-validated via `assertThatThrownBy`
- [QA-08] No test interdependency — `@TempDir` + fresh `StackResolver()` per test
- [QA-09] Fixtures centralized — `TestConfigBuilder` shared; no duplication across test files
- [QA-10] Unique test data — each test configures its own language/framework/buildTool triple
- [QA-12] Integration tests — `StackAuditSmokeIT` assembles end-to-end with real classpath templates
- [QA-15] TPP progression — null→known→unknown order in StackResolverTest
- [QA-16] No test-after — tests committed with implementation (same commit)
- [QA-17] Acceptance tests validate E2E — StackAuditSmokeIT validates file count, script names, and stack-specific presence
- [QA-18] TDD coverage thresholds maintained — 66 tests, 0 failures, 0 errors
- [QA-19] Smoke tests exist — StackAuditSmokeIT covers all 7 stacks (java-maven, java-gradle, spring-boot, node, python, go, _default)
- [QA-20] ALL smoke tests pass — 7/7 green, verified in task execution

---

## PARTIAL

- [QA-01] Test for each AC (1/2)
  - Finding: AC5 ("Placeholder não-resolvido falha o assembler") is only partially covered. `applyPlaceholders_leavesUnknownPlaceholderIntact` verifies the no-throw behavior but does NOT test the strict-mode `IllegalStateException` path. The `ScriptsAssembler(StackResolver, boolean strict)` constructor is wired but strict mode is not exercised.
  - Fix: Add test that invokes `ScriptsAssembler(new StackResolver(), true)` and verifies an exception is thrown when an unresolved `{{UNKNOWN_PLACEHOLDER}}` exists in a template.

- [QA-11] Edge cases covered (1/2)
  - Finding: Empty string language (`""`) not tested in `StackResolver.resolveTemplateDir()`; could NPE or return unexpected stack.
  - Fix: Add `@ParameterizedTest` case for `("", "spring-boot", "maven")` to verify fallback to `_default`.

- [QA-13] Test-first pattern (1/2)
  - Finding: All 3 tasks committed test+implementation in same commit (no separate RED commit).
  - Fix: Future tasks should commit failing test (RED) in a separate atomic commit before implementation.

- [QA-14] Explicit refactor (1/2)
  - Finding: No separate refactor commit after GREEN. `ScriptsAssembler.getKnownTemplateNames()` returns raw array — could be extracted as a constant.
  - Fix: Extract `KNOWN_TEMPLATE_NAMES` as a constant in a follow-up refactor commit.
