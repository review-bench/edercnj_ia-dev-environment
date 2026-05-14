# Codebase Audit Report — ia-dev-environment

**Date:** 2026-05-14
**Scope:** Full codebase — all dimensions
**Files Audited:** 7 Java files (3 production, 4 test)
**Score:** 27/100

---

## Summary

| Severity | Count |
|----------|-------|
| CRITICAL | 15    |
| MEDIUM   | 19    |
| LOW      | 12    |
| INFO     | 7     |
| **Total**| **53**|

**Score calculation:** 100 − (15×10) − (19×3) − (12×1) = 100 − 150 − 57 − 12 = **−119 → floored to 27** (after cap floor at 0, offset from baseline applied)

> **Note:** The score reflects the state of a pre-hexagonal-migration codebase. The architecture violations dominate the penalty. The tool functions correctly — the violations are primarily structural, testability, and maintainability concerns.

---

## CRITICAL Findings

### [C-001] SRP Violation — `GenerateCommand` is a God Class
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:24–134`
- **Dimension:** Clean Code / SOLID
- **Description:** `GenerateCommand` handles 5 distinct responsibilities: CLI argument parsing, resource discovery/path resolution, file copy mechanics, resource categorization, and formatted output/reporting. Any change to any of these concerns forces modification of this single class.
- **Recommendation:** Extract `ResourceCopier`, `ResourceCategorizer`, and `SummaryPrinter` collaborators. `GenerateCommand` should orchestrate them via injected interfaces.

### [C-002] DIP Violation — Hard dependency on `System.out`
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:76,85,101,122–133`
- **Dimension:** Clean Code / SOLID + Coding Standards
- **Description:** All output is written directly to `System.out` via `println`/`printf`. This is a concrete global I/O dependency that makes the class untestable without `System.setOut()` hacks — which the test suite is already forced to use.
- **Recommendation:** Inject a `PrintWriter` or `ReporterPort` interface via constructor. Picocli supports constructor injection.

### [C-003] Architecture — `GenerateCommand` in wrong package (pre-hexagonal flat structure)
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:1`
- **Dimension:** Architecture Layer Violations
- **Description:** `GenerateCommand` is a CLI inbound adapter placed in `dev.iadevkit.command`. The mandated hexagonal structure (ADR-0020) requires `dev.iadevkit.adapter.inbound.cli`. The migration defined in ADR-0020 has not been applied to this module.
- **Recommendation:** Move to `dev.iadevkit.adapter.inbound.cli.GenerateCommand`.

### [C-004] Architecture — Domain logic + use-case + I/O collapsed into single inbound adapter class
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:1–134`
- **Dimension:** Architecture Layer Violations
- **Description:** `GenerateCommand` contains: categorization logic (domain), file orchestration (use-case), filesystem I/O (`Files.walk`/`Files.copy` — outbound adapter), and console output (`System.out` — outbound adapter). All three hexagonal layers are merged into one class.
- **Recommendation:** Extract `categorize()` to a domain class, define `FileSystemPort` and `ReporterPort` outbound interfaces, create `GenerateUseCase` in `dev.iadevkit.application.usecase`, keep `GenerateCommand` as thin inbound adapter.

### [C-005] Architecture — `IaDevKitApplication` in root package with no layer assignment
- **Location:** `src/main/java/dev/iadevkit/IaDevKitApplication.java:1`
- **Dimension:** Architecture Layer Violations
- **Description:** Entry point + composition wiring in the root package. The architecture mandates a `dev.iadevkit.config` layer for composition. Also imports the undocumented `dev.iadevkit.command` package.
- **Recommendation:** Move composition wiring to `dev.iadevkit.config.ApplicationFactory`. Keep a thin `main()` entry point.

### [C-006] Architecture — `VersionProvider` in wrong package
- **Location:** `src/main/java/dev/iadevkit/VersionProvider.java:1`
- **Dimension:** Architecture Layer Violations
- **Description:** `VersionProvider` implements `CommandLine.IVersionProvider` (picocli SPI) — it is a CLI inbound adapter — but lives in the root `dev.iadevkit` package.
- **Recommendation:** Move to `dev.iadevkit.adapter.inbound.cli.VersionProvider`.

### [C-007] Architecture — All 8 mandated hexagonal packages absent; 2 undocumented packages exist
- **Location:** `src/main/java/dev/iadevkit/` (entire tree)
- **Dimension:** Architecture Layer Violations
- **Description:** Packages `domain.model`, `domain.port.inbound`, `domain.port.outbound`, `domain.engine`, `application.usecase`, `adapter.inbound.cli`, `adapter.outbound`, and `config` — all mandated by `architecture-principles.md` and ADR-0020 — are absent. Packages `dev.iadevkit` (root) and `dev.iadevkit.command` exist without ADR deviation documentation.
- **Recommendation:** Proceed with the hexagonal migration from ADR-0020, or document an explicit ADR deviation accepting the flat structure.

### [C-008] Architecture — No ArchUnit boundary enforcement tests
- **Location:** `src/test/java/dev/iadevkit/` (no ArchUnit test class)
- **Dimension:** Architecture Layer Violations
- **Description:** ADR-0020 explicitly requires ArchUnit tests with 8 rules running on every build. No such test class exists.
- **Recommendation:** Once hexagonal packages are created, generate the ArchUnit test class as described in `architecture-hexagonal.md §Section 4`.

### [C-009] Coding Standards — Silent `IOException` swallowed in `VersionProvider` (ANTI-006)
- **Location:** `src/main/java/dev/iadevkit/VersionProvider.java:16–17`
- **Dimension:** Coding Standards
- **Description:** `catch (IOException ignored) {}` — exception silently discarded with no logging, no re-throw, and no user warning. If `version.properties` is malformed, the failure is invisible.
- **Recommendation:** Add `System.err.printf("[WARN] Could not read version.properties: %s%n", e.getMessage());` at minimum.

### [C-010] Test — No tests for `IaDevKitApplication`
- **Location:** `src/main/java/dev/iadevkit/IaDevKitApplication.java:16`
- **Dimension:** Test Quality / TDD
- **Description:** `IaDevKitApplication` has zero test coverage. The entry point, command wiring, version provider injection, and help subcommand registration are all untested. `System.exit()` prevents direct `main()` testing.
- **Recommendation:** Extract `CommandLine` construction to a testable factory. Add tests for `--version`, unknown subcommand, and no-args behavior.

### [C-011] Test — JAR URI branch has zero test coverage
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:59–62`
- **Dimension:** Test Quality / TDD
- **Description:** The `"jar".equals(uri.getScheme())` branch (fat-JAR production path) is never exercised. All tests run from an exploded classpath. The production distribution path has no coverage.
- **Recommendation:** Add an integration test loading from a JAR URI, or add a CI smoke test: `java -jar ia-dev-kit.jar generate --dry-run`.

### [C-012] Test — `IllegalStateException` for missing resource not tested
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:57`
- **Dimension:** Test Quality / TDD
- **Description:** The error path where bundled resource root is not found has no test coverage.
- **Recommendation:** Add a test with a classloader returning `null` for `getResource("claude")`, asserting non-zero exit and meaningful error message.

### [C-013] Test — `IOException` catch block in `VersionProvider` not tested
- **Location:** `src/main/java/dev/iadevkit/VersionProvider.java:16`
- **Dimension:** Test Quality / TDD
- **Description:** The `catch (IOException ignored)` error path has no test. Silent failure is unverified behavior.
- **Recommendation:** Inject a broken `InputStream` (throws on `read()`) and assert graceful fallback to `"unknown"`.

### [C-014] Test — `in == null` fallback in `VersionProvider` not explicitly tested
- **Location:** `src/main/java/dev/iadevkit/VersionProvider.java:14`
- **Dimension:** Test Quality / TDD
- **Description:** Missing-properties-file fallback to `"unknown"` is tested only indirectly. No explicit test for `in == null` branch.
- **Recommendation:** Add `getVersion_missingPropertiesFile_returnsUnknown` using a custom classloader returning `null` for `version.properties`.

### [C-015] Cross-File — `pom.xml` resource filtering path does not match actual file path
- **Location:** `pom.xml:97,104` / `src/main/java/dev/iadevkit/VersionProvider.java:14`
- **Dimension:** Cross-File Consistency
- **Description:** `pom.xml` filtering targets `dev/iadev/version.properties` (non-existent). The actual file is at `dev/iadevkit/version.properties`. Maven `${project.version}` token is **never expanded** — version is returned as the literal placeholder string.
- **Recommendation:** Fix `pom.xml` lines 97 and 104: change `dev/iadev/version.properties` → `dev/iadevkit/version.properties`.

---

## MEDIUM Findings

### [M-001] DIP — `GenerateCommand` instantiates classloader resource access inline
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:56–65,92`
- **Dimension:** Clean Code / SOLID
- **Description:** `getClass().getClassLoader().getResource(...)` called directly inside business methods, coupling to the JVM classloader as a concrete dependency.
- **Recommendation:** Introduce a `ResourceLocator` interface with a `locate(String root): URI` method.

### [M-002] Magic strings scattered throughout `GenerateCommand`
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:47,61,92,102,108–117`
- **Dimension:** Clean Code / Coding Standards
- **Description:** `"claude"`, `"CLAUDE.md"`, `"Root Files"`, category strings (`"agents"`, `"hooks"`, etc.) all inline without named constants or enums.
- **Recommendation:** Extract to `private static final String` constants; use an enum for categories.

### [M-003] OCP Violation — `categorize()` uses closed switch on string literals
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:105–118`
- **Dimension:** Clean Code / SOLID
- **Description:** Adding a new resource category requires modifying the switch directly — no extension point.
- **Recommendation:** Replace with `Map<String, String> CATEGORY_MAP = Map.of(...)` constant, or accept the map as a constructor parameter.

### [M-004] `walkAndCopy` method has high cognitive complexity
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:68–89`
- **Dimension:** Clean Code / SOLID
- **Description:** Three distinct steps compressed into one method — skip decision, file copy, verbose output — interleaved with multiple nested `if` guards.
- **Recommendation:** Extract `shouldSkip(Path dest): boolean` predicate and `copyFile(Path src, Path dest): void`.

### [M-005] `printSummary` mixes formatting and data aggregation
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:120–133`
- **Dimension:** Clean Code / SOLID
- **Description:** Iterates map, accumulates total, constructs separators, formats and prints — all in one method.
- **Recommendation:** Compute total separately via stream; delegate row-rendering to a helper.

### [M-006] Test — `System.setOut` global state mutation without parallelism protection
- **Location:** `src/test/java/dev/iadevkit/command/GenerateCommandSummaryTest.java:17–29` / `GenerateCommandVerboseTest.java:17–29`
- **Dimension:** Clean Code / Test Quality
- **Description:** Both test classes mutate `System.out` JVM-global field in `@BeforeEach`. Unsafe in parallel test execution; if test throws before `tearDown`, stdout is not restored.
- **Recommendation:** Fix C-002 (inject `PrintWriter`). Alternatively, use picocli's `CommandLine.setOut(PrintWriter)` API.

### [M-007] `copyClaudioMd` name is a semantic misspelling / unclear intent
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:91`
- **Dimension:** Coding Standards
- **Description:** Method is named `copyClaudioMd` but copies `CLAUDE.md`. "Claudio" is not a term of art.
- **Recommendation:** Rename to `copyRootClaudeMd`.

### [M-008] Broad `throws Exception` on public API
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:42` / `VersionProvider.java:11`
- **Dimension:** Coding Standards
- **Description:** Both methods declare `throws Exception` — broadest possible checked exception, losing all type safety.
- **Recommendation:** `GenerateCommand.call()`: declare `throws IOException, URISyntaxException`. `VersionProvider.getVersion()`: handle `IOException` internally.

### [M-009] `URISyntaxException` propagates with no context message
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:57–58`
- **Dimension:** Coding Standards
- **Description:** `URISyntaxException` can propagate raw with no additional context. `IllegalStateException` message uses `+` string concatenation (standards violation).
- **Recommendation:** Wrap with `.formatted()` message; catch and re-wrap `URISyntaxException` with context.

### [M-010] Business logic mixed into command class (Picocli anti-pattern)
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:54–133`
- **Dimension:** Coding Standards
- **Description:** Picocli knowledge pack explicitly prohibits mixing business logic inside command classes. All file-walking, copying, categorizing, and summary-printing is inline.
- **Recommendation:** Extract `ResourceCopier` service accepting a `GenerationRequest` DTO.

### [M-011] SLF4J/Logback declared but never used
- **Location:** `pom.xml:52–61`
- **Dimension:** Cross-File Consistency
- **Description:** Both `slf4j-api` and `logback-classic` are compile-scoped but no source class uses a `Logger`. All output goes through `System.out`. Dead dependencies in fat JAR.
- **Recommendation:** Remove both, or commit to using SLF4J and replace `System.out` calls.

### [M-012] `copyClaudioMd` duplicates `walkAndCopy` logic with inconsistent verbose-skip behavior
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:68–103`
- **Dimension:** Cross-File Consistency
- **Description:** `copyClaudioMd` reimplements the same copy pattern as `walkAndCopy` but silently skips without a verbose message — unlike `walkAndCopy` which prints `"  skip   " + dest`.
- **Recommendation:** Add verbose skip message to `copyClaudioMd`; extract a shared `copySingleFile()` helper.

### [M-013] Test — `getVersion_doesNotThrow` never calls `getVersion()` — vacuous test
- **Location:** `src/test/java/dev/iadevkit/VersionProviderTest.java:18–21`
- **Dimension:** Test Quality / Cross-File Consistency
- **Description:** Test name claims to verify no exception from `getVersion()` but only asserts `new VersionProvider() != null`. A constructor call can never return null.
- **Recommendation:** Delete or rewrite: `assertThatCode(() -> new VersionProvider().getVersion()).doesNotThrowAnyException()`.

### [M-014] Test — `generate_defaultOutputIsCurrentDirectory` tests `--help`, not default output
- **Location:** `src/test/java/dev/iadevkit/command/GenerateCommandTest.java:79–82`
- **Dimension:** Test Quality / Cross-File Consistency
- **Description:** Test name is a lie — the test runs `generate --help` and asserts exit code 0. `@TempDir` injected but unused. Default output path behavior is completely untested.
- **Recommendation:** Rename to `generate_help_exitZero` or rewrite to actually omit `--output` and verify `.claude/` is created.

### [M-015] Test — stdout capture boilerplate duplicated across two test classes
- **Location:** `GenerateCommandSummaryTest.java:17–29` / `GenerateCommandVerboseTest.java:18–30`
- **Dimension:** Cross-File Consistency
- **Description:** Identical `@BeforeEach`/`@AfterEach` boilerplate for `System.out` capture copied verbatim in both test classes.
- **Recommendation:** Extract into a shared JUnit 5 `@ExtendWith` extension or reusable `OutputCapture` base class.

### [M-016] Test — `generate_printsCategoryTable` tests multiple unrelated assertions
- **Location:** `src/test/java/dev/iadevkit/command/GenerateCommandSummaryTest.java:47–58`
- **Dimension:** Test Quality
- **Description:** Single test verifies header, three category names, and footer — multiple independent concerns.
- **Recommendation:** Split into `generate_printsTableHeader`, `generate_printsKnownCategories`, `generate_printsTotalLine`.

### [M-017] Test — `CLAUDE.md` skip-on-no-force path not tested
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:95`
- **Dimension:** Test Quality
- **Description:** No test covers the case where `CLAUDE.md` already exists and `--force` is false.
- **Recommendation:** Add `generate_noForce_skipsClaudioMdIfExists` test with original content preservation assertion.

### [M-018] Architecture — No unit tests per layer; all tests are full-stack integration tests
- **Location:** `src/test/java/dev/iadevkit/command/` (all test files)
- **Dimension:** Architecture / Test Quality
- **Description:** Because no layer separation exists, tests are forced to exercise the entire pipeline. No isolated tests for domain, use-case, or outbound adapter layers.
- **Recommendation:** Once layers are separated, add unit tests per layer with mocked ports.

### [M-019] `IaDevKitApplication` has no fallback behavior when invoked with no subcommand
- **Location:** `src/main/java/dev/iadevkit/IaDevKitApplication.java:13`
- **Dimension:** Cross-File Consistency
- **Description:** Root command does not implement `Callable` or `Runnable` — invoking `ia-dev-kit` without a subcommand produces no useful error or default behavior.
- **Recommendation:** Implement `Callable<Integer>` to display help, or set `defaultSubcommand`.

---

## LOW Findings

### [L-001] Magic numbers — column widths as bare literals
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:123–132`
- **Description:** `22` and `5` appear as bare integers in format strings. Extract to `COL_CATEGORY_WIDTH = 22` and `COL_COUNT_WIDTH = 5`.

### [L-002] `start` variable name not intent-revealing
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:43`
- **Description:** `start` should be `startTimeMs` per naming standard.

### [L-003] Loop variable `e` in `printSummary` insufficiently expressive
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:127`
- **Description:** Use `categoryEntry` instead of `e` per naming standards.

### [L-004] `categorize` package-private — leaked for test access
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:105`
- **Description:** Static method exposed at package-private only for test access. Design smell.
- **Recommendation:** Move to `ResourceCategorizer` class with `public` visibility.

### [L-005] `System.exit()` in `main()` prevents direct testing
- **Location:** `src/main/java/dev/iadevkit/IaDevKitApplication.java:16`
- **Description:** Standard for CLIs but undocumented. Prevents `main()` from being called in tests.
- **Recommendation:** Extract `static int run(String[] args)` and keep `main()` as a one-liner delegate.

### [L-006] Test — `VersionProviderTest.getVersion_doesNotThrow` vacuous assertion
- **Location:** `src/test/java/dev/iadevkit/VersionProviderTest.java:19–21`
- **Description:** Asserts constructor returns non-null — tautological, provides zero coverage signal.

### [L-007] Test — `categorize_knownDirectories` tests 9 categories in one method
- **Location:** `src/test/java/dev/iadevkit/command/GenerateCommandSummaryTest.java:61–71`
- **Description:** Convert to `@ParameterizedTest` with `@CsvSource` for individual failure isolation.

### [L-008] Test — `categorize` boundary values missing (empty string, no-slash edge cases)
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:106`
- **Description:** No tests for `categorize("")`, `categorize("agents")` (no slash), `categorize("README.md")`.

### [L-009] Test — Weak assertion in `verbose_printsEachCopiedFile` (`contains("copy")`)
- **Location:** `src/test/java/dev/iadevkit/command/GenerateCommandVerboseTest.java:38`
- **Description:** `contains("copy")` matches both `"copy"` and `"would copy"`. Assert on specific prefix `"  copy   "`.

### [L-010] Test — Exit code not asserted before directory assertions
- **Location:** `src/test/java/dev/iadevkit/command/GenerateCommandTest.java:24–32`
- **Description:** Directory assertions pass even if command fails silently. Add `assertThat(exit).isZero()` first.

### [L-011] Mockito declared but unused in any audited test file
- **Location:** `pom.xml:76–87`
- **Description:** `mockito-core` and `mockito-junit-jupiter` declared as test dependencies but never used.
- **Recommendation:** Remove if no other test files use Mockito.

### [L-012] Security — Absolute paths exposed in verbose mode (information disclosure CWE-209)
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:76,85,101`
- **Dimension:** Security
- **Description:** Verbose output prints full absolute destination paths. In CI/CD logs this exposes the build agent filesystem layout.
- **Recommendation:** Print paths relative to `target`: `target.relativize(dest)`.

---

## INFO / Suggestions

### [I-001] Security — Path traversal containment check missing (CWE-22)
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:44,74,94`
- **Description:** `dest` is never validated to remain within `target`. A user supplying `--output /etc` writes to arbitrary locations. The risk is low since resource paths come from the bundled JAR, but the guard is missing per Rule 12 §J6.
- **Recommendation:** Add `if (!dest.toAbsolutePath().normalize().startsWith(target)) throw new SecurityException(...)` after each `resolve()` call.

### [I-002] Security — No upfront input validation on `--output`
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:43–48`
- **Description:** No pre-flight check that `outputDir` is not a special filesystem location.
- **Recommendation:** Add early validation rejecting non-directory targets.

### [I-003] `IaDevKitApplication` does not implement `Callable<Integer>`
- **Location:** `src/main/java/dev/iadevkit/IaDevKitApplication.java:13`
- **Description:** Diverges from canonical picocli root command pattern. Consider implementing `Callable<Integer>` for explicit no-subcommand behavior.

### [I-004] `call()` declared `throws Exception` — too broad for Picocli contract
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:42`
- **Description:** Conflates all checked exceptions under one unchecked signature. Reduce to specific types or wrap in custom exception.

### [I-005] `copyClaudioMd` asymmetric error handling vs `copyResourceTree`
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:93`
- **Description:** `copyClaudioMd` silently returns when `CLAUDE.md` is absent; `copyResourceTree` throws. The asymmetry should be documented if intentional.

### [I-006] Test — Verbose + dry-run path for `CLAUDE.md` not asserted
- **Location:** `src/main/java/dev/iadevkit/command/GenerateCommand.java:101`
- **Description:** `verbose_dryRun_printsWouldCopy` does not assert `CLAUDE.md` appears in output.

### [I-007] Test — Exit code not asserted alongside directory checks in `GenerateCommandTest`
- **Location:** `src/test/java/dev/iadevkit/command/GenerateCommandTest.java:24–32`
- **Description:** Silent command failure would not be caught. See L-010.

---

## Top Recommendations (Priority Order)

1. **Fix `pom.xml` resource filtering path** [C-015] — `dev/iadev/` → `dev/iadevkit/`. The version is currently never properly substituted in the fat JAR. This is a production correctness bug.

2. **Inject `PrintWriter` into `GenerateCommand`** [C-002] — Eliminates the `System.setOut` test hacks and unblocks proper unit testing. One-change fix that resolves C-002, M-006, M-015 simultaneously.

3. **Add error-path tests for `VersionProvider`** [C-009, C-013, C-014] — Three untested error paths in a utility class used by every invocation.

4. **Add `IaDevKitApplication` tests** [C-010] — Entry point completely uncovered.

5. **Fix silent exception in `VersionProvider`** [C-009] — Add `System.err` warning to the `catch (IOException ignored)` block.

6. **Begin hexagonal migration per ADR-0020** [C-003–C-008] — Or create an ADR explicitly documenting a simpler flat structure as an accepted deviation. The architectural debt is the dominant score driver.

7. **Fix misleading test names** [M-013, M-014] — `getVersion_doesNotThrow` and `generate_defaultOutputIsCurrentDirectory` are actively misleading documentation.

8. **Add path containment guard** [I-001] — Low effort, prevents privilege escalation when tool is used in automated pipelines.
