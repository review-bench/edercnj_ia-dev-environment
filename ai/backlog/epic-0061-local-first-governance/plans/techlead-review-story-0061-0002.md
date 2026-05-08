# Tech Lead Review — story-0061-0002

**Story:** story-0061-0002 (ScriptsAssembler Stack-Aware + Templates por Stack)
**Date:** 2026-04-28
**Author:** Tech Lead (x-review-pr)
**Template Version:** EPIC-0061 inline format
**PR Base:** epic/0061
**Round:** 1

---

## Test Execution Results (EPIC-0042)

| Metric | Value |
| :--- | :--- |
| Test Suite | **PASS** — 66 tests, 0 failures, 0 errors, 0 skipped |
| Coverage (new code) | ~95%+ on StackResolver + ScriptsAssembler (all paths exercised) |
| Smoke Tests | **PASS** — 7/7 (StackAuditSmokeIT covers all stacks) |

Coverage gate (Rule 05): **PASSED**. Test gate: **PASSED**.

---

## 45-Point Rubric

| Section | Score | Max |
| :--- | :--- | :--- |
| A. Code Hygiene | 6 | 8 |
| B. Naming | 4 | 4 |
| C. Functions | 4 | 5 |
| D. Vertical Formatting | 4 | 4 |
| E. Design | 3 | 3 |
| F. Error Handling | 3 | 3 |
| G. Architecture | 5 | 5 |
| H. Framework & Infra | 3 | 4 |
| I. Tests & Execution | 6 | 6 |
| J. Security & Production | 1 | 1 |
| K. TDD Process | 2 | 5 |
| **TOTAL** | **41** | **45** |

---

## Decision: GO

**Score:** 41/45 (91%) — comfortably exceeds ≥38 threshold
**Critical Issues:** 0
**Medium Issues:** 0
**Low Issues:** 6 (code hygiene + TDD discipline)

---

## Findings

### Low

- **TL-001: `strict` boolean parameter on constructor.** `ScriptsAssembler(StackResolver, boolean strict)` violates Rule 03 anti-pattern "boolean flags as function parameters". The `strict` parameter is unused (comment: "ignored — reserved for future"). *Section: A2, C4*
  - Fix: Remove the `strict` constructor or replace with a builder pattern when strict mode is actually implemented.

- **TL-002: `getKnownTemplateNames()` returns raw `String[]`.** Rest of the API uses `List<String>` — inconsistent signature. *Section: A4*
  - Fix: Return `List<String>` and update callers.

- **TL-003: `PLACEHOLDER_TABLE` declared inline as nested `Map.of()` calls.** Large static initializer (~30 lines). *Section: D*
  - Fix: Extract to a companion class `PlaceholderMaps` or a static factory method — minor readability improvement.

- **TL-004: Observability gap.** Story §7.2 plans `scripts_assembler.duration_ms` histogram but not implemented. *Section: H4*
  - Fix: Defer to future story; leave story §7.2 as documented intent.

- **TL-005: No separate RED commit.** All 3 tasks combined test+implementation in one commit per task. *Section: K4*

- **TL-006: No refactor commit after GREEN.** *Section: K5*

---

## PASSED (Highlights)

- **B: Naming** — `StackResolver`, `resolveTemplateDir`, `buildPlaceholders`, `applyPlaceholders` all intent-revealing
- **E: Design** — DRY: `PLACEHOLDER_TABLE` centralizes all 4 placeholder values for 7 stacks; no duplication
- **F: Error Handling** — `IllegalArgumentException` with message context; `UncheckedIOException` wraps `IOException`; no null returns
- **G: Architecture** — `StackResolver` injected via constructor; `application/assembler/` layer respected
- **I: Tests** — 66 passing, 7 smoke, @ParameterizedTest for 9 stack combinations
- **J: Security** — `isStackSafe()` regex blocks path traversal; `String.replace()` literal substitution (no regex injection)
- **K3: TPP** — null→known→unknown test order in StackResolverTest

---

## Cross-File Consistency

- `StackResolver.resolveTemplateDir()` return values match `PLACEHOLDER_TABLE` keys 1:1 (verified: "java-maven", "java-gradle", "spring-boot", "node", "python", "go", "_default").
- `ScriptsAssembler.buildPlaceholders()` delegates to `StackResolver` before looking up `PLACEHOLDER_TABLE` — consistent single path.
- Test `StackResolverTest.resolveTemplateDir` covers 9 combos; `ScriptsAssemblerStackAwareTest.buildPlaceholders` validates 5 stacks — adequate overlap.

---

## Recommendation

**GO** — merge to epic/0061.

6 LOW findings, all below blocking threshold. story-0061-0003 (Catálogo Dinâmico) and story-0061-0004 (Java Audit Harness) can now proceed — both depend on this story.
