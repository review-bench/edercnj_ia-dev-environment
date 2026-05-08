ENGINEER: QA
STORY: story-0064-0601 (audit-capability-graph.sh + Phase 6 partial)
SCORE: 23/38 (QA-12 N/A — no DB/API)

STATUS: Rejected

### PASSED
- [QA-05] AAA pattern in every test — all test methods follow Arrange-Act-Assert cleanly
- [QA-07] Exception paths tested with specific assertions — CycleDetectorTest, MutexValidatorTest, PrerequisiteResolverTest all exercise error branches with typed assertions
- [QA-08] No test interdependency — @TempDir + immutable test data; tests pass in any order
- [QA-10] Unique test data per test — each test constructs its own fixtures inline
- [QA-11] Edge cases covered — null input, degenerate (empty graph, single node), boundary (10-level deep chain) all tested
- [QA-15] TPP progression — CapabilityIdTest progresses: null→no-dots→uppercase→canonical→glob; CycleDetectorTest: empty→single→direct cycle→indirect cycle
- [QA-17] Acceptance tests validate E2E behavior — Epic0064CapabilityResolutionSmokeTest and Epic0064ComposerIntegrationSmokeTest exercise full resolver→composer chain
- [QA-19] Smoke tests exist — 2 smoke test classes cover critical path (resolve, prune, determinism)
- [QA-20] All smoke tests pass — mvn test: BUILD SUCCESS

### FAILED
- [QA-02] Line coverage below 95% absolute gate (Rule 05 RULE-005-01)
  - Finding: jacoco report — 452 missed / 6,656 lines = 93.2% (threshold: 95%)
  - Package breakdown: application.composition 72%, domain.capability 74% — major contributors to gap
  - Fix: add tests for CapabilityGlob edge cases, CapabilityDefinition profile constructor paths, CompositionEngine regex edge cases, FrontmatterMigrationService directory walk errors
- [QA-03] Branch coverage below 90% absolute gate (Rule 05 RULE-005-01)
  - Finding: jacoco report — 325 missed / 2,408 branches = 86.5% (threshold: 90%)
  - Fix: same packages; CapabilityMatcher boolean branches and CompositionEngine each/slot empty-fragment branches
- [QA-13] Commits do not show test-first pattern
  - Finding: all commits bundle tests + implementation in a single commit (e.g., feat(story-0064-0101) adds both CapabilityId.java and CapabilityIdTest.java atomically)
  - Fix: separate commits: first add failing tests, then add implementation
- [QA-14] No explicit refactoring after green
  - Finding: no `refactor:` commits visible in epic/0064 history; Red-Green-Refactor cycles collapsed into single commits
  - Fix: introduce `refactor:` commits after green phase for extractions (e.g., CapabilityGlob extraction)
- [QA-18] TDD coverage thresholds not maintained
  - Finding: application.composition (72%/61%) and domain.capability (74%/59%) both far below thresholds, dragging overall repo below gates
  - Fix: raise package-level coverage to ≥95%/90% before merge

### PARTIAL
- [QA-01] Tests exist for each acceptance criterion
  - Finding: Gherkin ACs for audit-capability-graph.sh (bash) have no JUnit test counterpart; Java CycleDetector/MutexValidator cover the logic but not the shell script integration
  - Note: bash script not exercised by JUnit — missing at least 1 integration test calling the script
- [QA-04] Test naming convention partially followed
  - Finding: method names like `rejectsSingleSegment()`, `acceptsThreeSegments()` are readable but do not follow `[methodUnderTest]_[scenario]_[expected]`; DisplayName fills the gap but method name convention violated
- [QA-06] No @ParameterizedTest for data-driven scenarios
  - Finding: jqwik @Property used (good), but valid ID format variants (hyphens, 3-segment, numeric) not parametrized with @MethodSource/CsvSource
- [QA-09] Fixtures partially centralized
  - Finding: `def()` helper method duplicated independently in CycleDetectorTest, MutexValidatorTest, PrerequisiteResolverTest, CapabilityResolverTest; CapabilityGenerators exists but only used by property tests
  - Fix: extract shared `CapabilityTestFixtures` class
- [QA-16] Test-after risk present
  - Finding: single-commit pattern makes it impossible to verify tests came before implementation; commit history does not rule out test-after
