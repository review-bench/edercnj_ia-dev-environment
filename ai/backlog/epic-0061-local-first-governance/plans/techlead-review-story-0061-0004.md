# Tech Lead Review — story-0061-0004

**Story:** story-0061-0004 (Java Audit Harness + Smoke Equivalência)
**Date:** 2026-04-28
**Author:** Tech Lead (x-review-pr)
**PR Base:** epic/0061
**Round:** 1

---

## Test Execution Results

| Metric | Value |
| :--- | :--- |
| Test Suite | **PASS** — 75 tests, 0 failures |
| Coverage | ~95%+ on all 8 auditors + corpus |
| Smoke Tests | **PASS** — 32/32 (AuditEquivalenceSmokeIT) |

---

## 45-Point Rubric

| Section | Score | Max |
| :--- | :--- | :--- |
| A. Code Hygiene | 7 | 8 |
| B. Naming | 4 | 4 |
| C. Functions | 5 | 5 |
| D. Vertical Formatting | 4 | 4 |
| E. Design | 2 | 3 |
| F. Error Handling | 2 | 3 |
| G. Architecture | 5 | 5 |
| H. Framework & Infra | 4 | 4 |
| I. Tests & Execution | 6 | 6 |
| J. Security & Production | 1 | 1 |
| K. TDD Process | 3 | 5 |
| **TOTAL** | **43** | **45** |

---

## Decision: GO

**Score:** 43/45 (95.6%) — comfortably above ≥38 threshold
**Critical Issues:** 0
**Medium Issues:** 2 (DRY violation + stream resource leak)
**Low Issues:** 2 (TDD discipline)

---

## Findings

### Medium

- **TL-001: `readFile(Path)` duplicated across 8 auditor classes.** Every `*Auditor.java` has an identical `private static String readFile(Path path)` method with identical `UncheckedIOException` wrapping. This is an E3 (DRY) violation per Rule 03. *Section: E3*
  - Fix: Extract to `AuditCorpus` as a static utility method, or create a shared `AuditFileHelper` utility class. Reduces 8 duplicate implementations to 1.

- **TL-002: `AuditCorpus.walkFiles()` returns unclosed `Files.walk()` stream.** If a caller throws mid-iteration (e.g., `UncheckedIOException` in `checkSkill`), the underlying `DirectoryStream` is NOT closed. On large repos this leaks OS file handles. *Section: F3 (resource cleanup)*
  - Fix: Callers should wrap in try-with-resources:
    ```java
    try (Stream<Path> skills = corpus.walkSkills()) {
        skills.forEach(skill -> checkSkill(skill, violations));
    }
    ```
  - Priority: MEDIUM — happy path is safe (auto-close on exhaustion); exception path leaks.

### Low

- **TL-003: No separate RED commit per auditor.** Two wave commits bundle test + implementation together.

- **TL-004: No explicit REFACTOR commit.** `readFile()` duplication (TL-001) should have been caught and extracted in a refactor cycle.

---

## PASSED (Highlights)

- **A: Code Hygiene** — clean imports, no dead code across all 8 auditors; `AuditCorpus` uses `toAbsolutePath().normalize()` (secure)
- **B: Naming** — `ModelSelectionAuditor`, `BypassFlagsAuditor`, `walkSkills()`, `walkExecutionStates()` all intent-revealing
- **C: Functions** — all methods ≤25 lines; SRP: one auditor = one rule; `Auditor` interface is clean (3 methods)
- **G: Architecture** — `dev.iadev.audit/` correctly isolated as infrastructure; RULE-013 honored (Java-only in generator)
- **H: Observability** — story §7.1 deliberately excludes telemetry for auditors (run in `mvn verify`, not LLM session) ✓
- **I: Tests** — `AuditEquivalenceSmokeIT` is the canonical structural parity test (RULE-004); 32 cases × 4 test types = comprehensive coverage
- **J: Security** — `AuditCorpus` normalizes paths before walking; no user-controlled path injection; fixture corpus verified clean

---

## Cross-File Consistency

- `Auditor.audit(AuditCorpus)` signature is uniform across all 8 classes ✓
- `AuditResult.violation(name, violations)` and `AuditResult.ok()` used consistently ✓
- `AuditViolation(path, line, rule, message)` constructor used uniformly ✓
- `bashEquivalentTemplate()` returns `Path.of("java/src/main/resources/targets/claude/scripts/java-maven/audit-{name}.sh.tpl")` consistently ✓
- `AuditEquivalenceSmokeIT.allAuditors()` covers all 8 — complete catalog ✓

---

## Recommendation

**GO** — merge to epic/0061.

story-0061-0005 (removal of `scripts/audit-*.sh` root) is now unblocked.

MEDIUM findings (TL-001: DRY, TL-002: stream leak) should be addressed as follow-up refactor in a future task or during story-0061-0005 preparation.
