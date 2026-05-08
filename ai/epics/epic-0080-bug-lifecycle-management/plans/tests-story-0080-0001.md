---
generated-by: x-internal-build-story-plan@0b3ac24e2eef2f8390557b05844ffd36f7176489
generated-at: 2026-05-08T00:27:32Z
story-id: story-0080-0001
---

# Test Plan -- story-0080-0001: x-create-bug Skill Scaffolding

## 1. Test Strategy Overview

### TDD Mandate (Red-Green-Refactor)

All test files MUST be committed before their corresponding production artefact. Commit order enforced:

1. CreateBugSlugGenerationTest.java (RED) -- all parametrized cases failing
2. Slug pipeline in SKILL.md (GREEN) -- minimal implementation to pass slug tests
3. CreateBugFrontmatterAssemblyTest.java (RED) -- frontmatter structure assertions failing
4. _TEMPLATE-BUG.md + bug-lifecycle.yaml (GREEN)
5. BugCreationSmokeIT.java (RED) -- full integration fixture failing
6. x-create-bug/SKILL.md complete body (GREEN)
7. Refactoring commit (no behavior changes; reorganize slug pipeline, trim assertions)

Weak assertions are forbidden. isNotNull() alone is never sufficient -- assert the specific string, regex, or count.

### Coverage Targets

| Metric | Threshold | Measurement |
|---|---|---|
| Line coverage | >= 95% | JaCoCo line counter on changed/new classes |
| Branch coverage | >= 90% | JaCoCo branch counter on changed/new classes |
| Mutation score | >= 80% | PITest (when enabled in CI) |

### Test File Locations

| Test Class | Absolute Path |
|---|---|
| CreateBugSlugGenerationTest | java/src/test/java/dev/iadev/adapter/inbound/cli/skills/CreateBugSlugGenerationTest.java |
| CreateBugFrontmatterAssemblyTest | java/src/test/java/dev/iadev/adapter/inbound/cli/skills/CreateBugFrontmatterAssemblyTest.java |
| BugCreationSmokeIT | java/src/test/java/dev/iadev/it/BugCreationSmokeIT.java |

### AC Measurability Declaration

Every AC below is expressed with Unit + Target + Measurement per the QA AC Measurability contract.

| AC | Unit | Target | Measurement |
|---|---|---|---|
| AC-1 | boolean (scaffold complete) | true | assert file exists + branch exists + PR URL non-empty + frontmatter contains governance.bug-lifecycle + status=pending |
| AC-2 | boolean (zero side effects) | true | assert no dir created + no branch created + exit code == 15 + stderr contains exact message |
| AC-3 | latency_p95_seconds | 90 | SLOHarness.validate(SLOSpec("bug-creation-p95", 90.0, "single smoke run"), elapsedSeconds) |
| AC-4 | boolean (CWE-22 clean) | true | branch name matches ^bug/[0-9]{6}-[a-z0-9-]{1,40}$ + empty-slug inputs exit with code 2 |

---

## 2. Unit Test Scenarios -- Slug Generation (task-0080-0001-004)

**Class:** CreateBugSlugGenerationTest
**Pattern:** Parametrized, @MethodSource, Arrange-Act-Assert
**Naming:** slugify_scenario_expectedBehavior

### AC-4 Coverage Table (12 CWE-22 Payloads)

| # | Test Name | Input | Expected Output | Exit Code |
|---|---|---|---|---|
| 1 | slugify_happyPath_producesKebabCase | checkout total wrong when promo applied | checkout-total-wrong-when-promo-applied | n/a |
| 2 | slugify_pathTraversalDotDotSlash_stripsTraversal | ../../../etc/passwd injection | etc-passwd-injection | n/a |
| 3 | slugify_backslashTraversal_stripsBackslash | ..\\..\\windows\\system32 | windows-system32 | n/a |
| 4 | slugify_mixedSeparators_stripsAll | a/b\\c../d | a-b-c-d (no path seps) | n/a |
| 5 | slugify_unicodeControlChars_stripped | bug (null-byte) | bug-null | n/a |
| 6 | slugify_purePathSeparatorsOnly_emptySlug_exitsArgs | /// | (empty) | 2 (ARGS_INVALID) |
| 7 | slugify_onlyDotsAndSlashes_emptySlug_exitsArgs | ../../.. | (empty) | 2 (ARGS_INVALID) |
| 8 | slugify_emptyString_exitsArgs | (empty string) | (empty) | 2 (ARGS_INVALID) |
| 9 | slugify_unicodeTitle_nfkdNormalized | Regression login (accented) | regression-login | n/a |
| 10 | slugify_maxLengthTruncation_truncatesAt40 | a repeated 80 times | a repeated 40 times | n/a |
| 11 | slugify_trailingAndLeadingHyphens_trimmed | -leading and trailing- | leading-and-trailing | n/a |
| 12 | slugify_consecutiveHyphens_collapsed | bug  --  two | bug-two | n/a |

### Parametrized Test Method Shape

```java
// Naming: slugify_<scenario>_<expectedBehavior>
@ParameterizedTest(name = "[{index}] {0}")
@MethodSource("slugVectors")
void slugify_allCwe22Vectors_producesCompliantSlug(
        String displayName, String input, String expectedSlug, boolean expectEmpty) {
    // Arrange -- input is the raw description string from skill arg
    // Act
    String result = SlugGenerator.slugify(input);
    // Assert
    if (expectEmpty) {
        assertThat(result).isEmpty();
    } else {
        assertThat(result).matches("[a-z0-9-]{1,40}");
        assertThat(result).isEqualTo(expectedSlug);
        // Guard: no path separators survive
        assertThat(result).doesNotContain("/", "\\", "..");
    }
}
```

### Branch-Name Regex Assertion

```java
@Test
void branchName_happyPath_matchesContractRegex() {
    String bugId = "000001";
    String slug = SlugGenerator.slugify("checkout total wrong when promo applied");
    String branch = "bug/" + bugId + "-" + slug;
    assertThat(branch).matches("^bug/[0-9]{6}-[a-z0-9-]{1,40}$");
}
```

### Folder-Name Regex Assertion

```java
@Test
void folderName_happyPath_matchesContractRegex() {
    String bugId = "000001";
    String folder = "bug-" + bugId;
    assertThat(folder).matches("^bug-[0-9]{6}$");
}
```

---

## 3. Unit Test Scenarios -- Frontmatter Assembly (task-0080-0001-004)

**Class:** CreateBugFrontmatterAssemblyTest
**Pattern:** Golden-file comparison against rendered _TEMPLATE-BUG.md output
**Naming:** frontmatter_scenario_expectedBehavior

### 3.1 Capability Reference Present

```
Test: frontmatter_capabilityDeclaration_containsGovernanceBugLifecycle
Arrange: render _TEMPLATE-BUG.md with bugId=000001, severity=high, scope=single-module
Act:     parse frontmatter block
Assert:  frontmatter.getList("requires-capabilities")
             .contains("governance.bug-lifecycle")  // Rule 28 compliance
```

### 3.2 Status Field Default

```
Test: frontmatter_statusField_defaultsToOpen
Assert: frontmatter.getString("status").isEqualTo("open")
```

### 3.3 Refinement Verdict Section Default

```
Test: frontmatterRefinementVerdict_statusField_defaultsToPending
Arrange: parse rendered bug.md
Assert: section "## Refinement Verdict" block contains "status: pending"
```

### 3.4 Bug-ID Injection

```
Test: frontmatter_bugId_matchesAllocatedId
Assert: frontmatter.getString("bug-id").isEqualTo("bug-000001")
```

### 3.5 Created-At ISO-8601

```
Test: frontmatter_createdAt_isIso8601Format
Assert: frontmatter.getString("created-at")
            matches ISO-8601 pattern: \d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z
```

### 3.6 Severity Enum Written

```
Test: frontmatter_severity_reflectsInput
Arrange: render with severity=critical
Assert: frontmatter.getString("severity").isEqualTo("critical")
```

### 3.7 Golden File Comparison

```
Test: template_goldenFile_matchesGeneratorOutput
Arrange: load golden from src/test/resources/golden/_TEMPLATE-BUG.md
Act:     assert rendered output equals golden byte-for-byte (excluding timestamps)
Assert:  no structural drift introduced
```

---

## 4. Integration / Smoke Test -- BugCreationSmokeIT (task-0080-0001-007)

**Class:** BugCreationSmokeIT
**Scope:** Full skill invocation in a sandboxed git repository (temp directory)
**Strategy:** JUnit 5 @TempDir, initialise bare git repo + gh mock, pipe stdin answers

### 4.1 Sandbox Setup

```java
@BeforeEach
void setUpSandbox(@TempDir Path tempDir) {
    // 1. git init tempDir
    // 2. git commit --allow-empty -m "init" (creates HEAD on develop)
    // 3. Create ai/bugs/ directory structure
    // 4. Export GIT_DIR, GH_HOST (mock) env vars
    // 5. Stub gh pr create to return a fixed URL without network call
}
```

No Thread.sleep() for synchronization. Awaitility used if async file writes are observable.

### 4.2 AC-1 -- Happy Path: Full Scaffold Assertion

```
Test: invoke_happyPath_createsAllArtefactsAndPR

Stdin answers (piped): severity=high, scope=single-module
Command: skill x-create-bug "checkout total wrong when promo applied" --non-interactive

Assertions (ALL must pass -- single test failure if any is absent):
  1. File exists: tempDir/ai/bugs/bug-000001/bug.md
  2. File content: bug.md frontmatter contains requires-capabilities: [governance.bug-lifecycle]
  3. File content: bug.md frontmatter contains status: open
  4. File content: bug.md contains ## Refinement Verdict with status: pending
  5. Git branch: git branch --list bug/000001-checkout-total-promo returns non-empty
  6. Git log: HEAD commit message contains "bug: scaffold bug-000001"
  7. Stdout JSON: contains "bugId": "bug-000001"
  8. Stdout JSON: contains "branch": "bug/000001-checkout-total-promo"
  9. Stdout JSON: contains "prUrl": matching pattern https?://.*
  10. Stdout JSON: contains "status": "scaffold-ready"
  11. Exit code: 0
```

### 4.3 AC-2 -- Dirty Worktree: Zero Side Effects

```
Test: invoke_dirtyWorktree_abortsWithExit15AndNoSideEffects

Setup: create an uncommitted file in tempDir before invocation
Command: skill x-create-bug "some description" --non-interactive

Assertions:
  1. Exit code: 15 (WORKTREE_AMBIGUOUS)
  2. Directory NOT created: tempDir/ai/bugs/ has zero bug-* subdirectories
  3. No new git branch: git branch --list "bug/*" is empty
  4. No new git commit: HEAD sha unchanged from pre-invocation sha
  5. Stderr contains: "working tree not clean -- commit or stash before creating a bug"
```

### 4.4 Idempotency -- Consecutive ID Allocation

```
Test: invoke_consecutiveCalls_allocateDistinctIds

Run skill twice with distinct descriptions.
Assert:
  1. First call produces bug-000001
  2. Second call produces bug-000002
  3. No ID collision, no exit-code error
```

### 4.5 Telemetry Completeness

```
Test: invoke_happyPath_emitsMandatoryTelemetryEvents

Post-invocation, read events.ndjson produced in tempDir.
Assert:
  1. Line count >= 4
  2. Contains event type "skill_start"
  3. Contains event type "skill_end"
  4. skill_end event JSON contains "exit_code": 0
  5. skill_end event JSON contains "bug_id": "bug-000001"
```

---

## 5. Performance Test -- AC-3 Wall-Clock SLO

**Location:** BugCreationSmokeIT (dedicated test method, labelled @Tag("slo"))

**SLO Spec:**

```
SLOSpec id:                bug-creation-p95
SLOSpec target:            90.0  (seconds)
SLOSpec windowDescription: single smoke run
SLOResult.passed:          observedSeconds <= 90.0
SLOResult.delta:           90.0 - observedSeconds  (positive = headroom)
```

**SLO Harness Usage:**

```java
@Test
@Tag("slo")
void invoke_nonInteractive_completesWithinSloP95() {
    SLOHarness harness = new SLOHarness();
    SLOSpec slo = new SLOSpec("bug-creation-p95", 90.0, "single smoke run");

    long startMs = System.currentTimeMillis();
    int exit = invokeSkill("x-create-bug", "--non-interactive",
            "checkout total wrong when promo applied");
    long elapsedMs = System.currentTimeMillis() - startMs;
    double elapsedSeconds = elapsedMs / 1000.0;

    SLOResult result = harness.validate(slo, elapsedSeconds);
    System.out.printf("SLO delta=%.2fs%n", result.delta());
    assertTrue(result.passed(),
            "AC-3 SLO breach: bug-creation-p95 elapsed=%.2fs target=90s delta=%.2fs"
            .formatted(elapsedSeconds, result.delta()));
}
```

**Measurement Method:** System.currentTimeMillis() wraps the full invocation (file creation, branch, commit, mocked PR). P95 aggregation over 20 real runs performed post-deployment via x-analyze-telemetry-trends on events.ndjson.

---

## 6. Error Path Tests

### 6.1 Error Catalog

All expected error codes for story-0080-0001:

| ErrorCode | Exit Code | HTTP Equivalent | Condition | Recovery |
|---|---|---|---|---|
| WORKTREE_AMBIGUOUS | 15 | 409 | git status shows uncommitted changes | Commit or stash, retry |
| ARGS_INVALID | 2 | 422 | Description normalizes to empty slug | Fix input, retry |
| COUNTER_COLLISION | 1 | 503 | 5 concurrent mkdir retries exhausted | Wait, retry manually |
| BRANCH_CONFLICT | 16 | 409 | Branch already exists after ID increment retry | Delete conflicting branch, retry |
| PR_NETWORK_FAILURE | 17 | 503 | gh pr create non-zero exit | Run gh pr create manually; scaffold retained |
| TEMPLATE_MISSING | 18 | 500 | _TEMPLATE-BUG.md not found at runtime | CI audit catches before runtime |
| BUSY | 19 | 429 | Lock held > 5s by concurrent invocation | Wait, retry after release |

Error Catalog Completeness Checklist:
- [x] Every non-0 exit code in the skill schema has a catalog entry
- [x] Recovery strategy defined for each error code
- [x] Test cases exist for exit 15 (section 4.3) and exit 2 (section 2 vectors 6-8)
- [ ] exit 1, 16, 17, 18, 19 covered by negative tests listed in section 6.2

### 6.2 Error Path Test Inventory

| Test Name | Class | ErrorCode | Exit Code | Key Assertions |
|---|---|---|---|---|
| invoke_dirtyWorktree_abortsWithExit15AndNoSideEffects | BugCreationSmokeIT | WORKTREE_AMBIGUOUS | 15 | no folder, no branch, no commit, stderr exact match |
| slugify_purePathSeparatorsOnly_emptySlug_exitsArgs | CreateBugSlugGenerationTest | ARGS_INVALID | 2 | slug empty |
| slugify_onlyDotsAndSlashes_emptySlug_exitsArgs | CreateBugSlugGenerationTest | ARGS_INVALID | 2 | slug empty |
| slugify_emptyString_exitsArgs | CreateBugSlugGenerationTest | ARGS_INVALID | 2 | slug empty |
| invoke_templateMissing_exits18 | BugCreationSmokeIT | TEMPLATE_MISSING | 18 | delete template from sandbox, assert exit 18 |
| invoke_branchAlreadyExists_incrementsAndSucceeds | BugCreationSmokeIT | BRANCH_CONFLICT | 0 | pre-create branch, assert retry succeeds with bug-000002 |

### 6.3 Negative Test Code Shape (Dirty Worktree)

```java
@Test
void invoke_dirtyWorktree_abortsWithExit15AndNoSideEffects() {
    // Arrange
    createUncommittedFile(sandbox, "dirty.txt");
    Path bugsDir = sandbox.resolve("ai/bugs");
    long branchCountBefore = countBugBranches(sandbox);
    String headBefore = gitHead(sandbox);

    // Act
    ProcessResult result = invokeSkill(sandbox,
            "x-create-bug", "--non-interactive", "some description");

    // Assert -- exit code
    assertThat(result.exitCode()).isEqualTo(15);
    // Assert -- no directory created
    assertThat(bugsDir.toFile().list()).isNullOrEmpty();
    // Assert -- no branch created
    assertThat(countBugBranches(sandbox)).isEqualTo(branchCountBefore);
    // Assert -- no new commit
    assertThat(gitHead(sandbox)).isEqualTo(headBefore);
    // Assert -- stderr message
    assertThat(result.stderr())
            .contains("working tree not clean -- commit or stash before creating a bug");
}
```

---

## 7. Coverage Targets (Non-Negotiable Gate)

| Scope | Line Coverage | Branch Coverage | Tool |
|---|---|---|---|
| CreateBugSlugGenerationTest coverage of slug pipeline | >= 95% | >= 90% | JaCoCo |
| CreateBugFrontmatterAssemblyTest coverage of template renderer | >= 95% | >= 90% | JaCoCo |
| BugCreationSmokeIT coverage of orchestration paths | >= 95% | >= 90% | JaCoCo |
| Mutation score on slug pipeline critical branches | >= 80% | n/a | PITest |

Enforcement: Maven Surefire + JaCoCo check goal fails build if thresholds are not met. No pre-existing exemption applies (RULE-005-01 Absolute Gate).

---

## 8. Task-to-Test Traceability Matrix

| Task | Test Class | Test Methods | ACs Covered |
|---|---|---|---|
| task-0080-0001-001 (_TEMPLATE-BUG.md) | CreateBugFrontmatterAssemblyTest | frontmatter_*, template_goldenFile_matchesGeneratorOutput | AC-1 |
| task-0080-0001-002 (bug-lifecycle.yaml) | CreateBugFrontmatterAssemblyTest | frontmatter_capabilityDeclaration_containsGovernanceBugLifecycle | AC-1 |
| task-0080-0001-003 (SKILL.md) | BugCreationSmokeIT | invoke_happyPath_*, invoke_dirtyWorktree_*, invoke_*_exits* | AC-1, AC-2, AC-3 |
| task-0080-0001-004 (unit tests) | CreateBugSlugGenerationTest | slugify_* (12 vectors), branchName_*, folderName_* | AC-4 |
| task-0080-0001-005 (acceptance smoke) | BugCreationSmokeIT | invoke_happyPath_createsAllArtefactsAndPR | AC-1, AC-2 |
| task-0080-0001-007 (BugCreationSmokeIT) | BugCreationSmokeIT | all IT methods + invoke_nonInteractive_completesWithinSloP95 | AC-1, AC-2, AC-3 |

---

## 9. Post-Deployment Validation Checklist

- [ ] All success metrics within target after 24h of deployment
- [ ] Error rate < 0.1% (count(exit != 0) / count(invocations) * 100)
- [ ] Latency P99 < 90s (histogram from telemetry events.ndjson skill_end.elapsedMs)
- [ ] Zero new error catalog entries discovered in production logs (no undocumented exit codes)
- [ ] SLO dashboard shows green for bug-creation-p95
- [ ] No coverage regression vs. previous release (delta(line_coverage) >= 0)
- [ ] BugCreationSmokeIT passes in CI with zero flaky failures

---

## 10. Success Metrics (KPI Tracking)

| Metric | Target | SLA | Measurement | Owner |
|---|---|---|---|---|
| Error rate (skill invocations) | < 0.1% | -- | count(exit != 0) / count(invocations) * 100, from events.ndjson | QA + SRE |
| Wall-clock P95 (AC-3) | < 90s | 90s | x-analyze-telemetry-trends on skill_end.elapsedMs over first 20 real invocations | QA + SRE |
| Slug injection block rate | 100% | -- | count(ARGS_INVALID events) / count(traversal-attempt inputs) | QA |
| Test flakiness | 0 | -- | count(non-deterministic failures per week in BugCreationSmokeIT) | QA |
| Coverage regression | 0 regressions | -- | delta(line_coverage) vs. previous release | QA |
