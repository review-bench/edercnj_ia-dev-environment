# Tech Lead Review — story-0064-0601

**Story:** story-0064-0601 — audit-capability-graph.sh (Phase 6, EPIC-0064)
**Date:** 2026-04-29
**Author:** Tech Lead (x-review-pr)
**Branch:** epic/0064 vs develop

---

## Decision: NO-GO

**Score: 34/45** (threshold: ≥ 38 — BELOW)
**Automatic NO-GO triggers:** Coverage 93.2%/86.5% (Rule 05 §RULE-005-01 absolute gate)

---

## Test Execution Results

| Metric | Result | Threshold | Status |
|--------|--------|-----------|--------|
| Test suite | 4,184 tests | all pass | ✅ PASS |
| Failures | 0 | 0 | ✅ PASS |
| Line coverage | 93.2% | ≥ 95% | ❌ FAIL |
| Branch coverage | 86.5% | ≥ 90% | ❌ FAIL |
| Smoke tests | PASS (Epic0064CapabilityResolution + Epic0064ComposerIntegration) | all pass | ✅ PASS |

**Coverage gap packages:**
- `application.composition`: 72% line / 61% branch (major contributor)
- `domain.capability`: 74% line / 59% branch
- `infrastructure.adapter.output`: 79% line / 63% branch

---

## 45-Point Rubric

| Section | Score | Notes |
|---------|-------|-------|
| A. Code Hygiene | 7/8 | Bash `parse_yaml_field` embeds python3 inline (exceeds function size convention); all Java clean |
| B. Naming | 4/4 | All classes intention-revealing: CapabilityAwareComposer, ArtifactScanner, CapabilityMatcher, OutputPruner |
| C. Functions | 3/5 | CapabilityGraph.topologicalSort() ~55 lines (exceeds 25); CapabilityDefinition record has 12 components (violates ≤4 params rule) |
| D. Vertical Formatting | 3/4 | Good blank-line discipline; some classes could apply Newspaper Rule more strictly |
| E. Design | 2/3 | CQS clean (plan=query, execute=command); DRY violated in tests (def() helper in 4 files); LoD OK |
| F. Error Handling | 2/3 | Sealed CapabilityError with structured context is excellent; bash `except Exception: pass` (generic catch in embedded python) |
| G. Architecture | 5/5 | Domain clean (zero framework imports); ports/adapters pattern correct; CapabilityCatalogRepository port proper |
| H. Framework & Infra | 3/4 | DI via constructor OK; config externalized; TelemetryCapabilityResolver uses java.util.logging (not structured JSON — Rule 07) |
| I. Tests & Execution | 3/6 | Tests pass ✅; coverage ❌ 93.2%/86.5%; smoke ✅; test quality partial (no @ParameterizedTest, def() duplication) |
| J. Security & Production | 1/1 | No secrets; immutable records ensure thread safety |
| K. TDD Process | 1/5 | Tests bundled with implementation in single commits; no test-first commits; no refactoring commits; TPP progression partial |

**Total: 34/45**

---

## Critical / High Findings

### CRITICAL — Coverage below absolute gate (Rule 05 §RULE-005-01)

- **Line coverage: 93.2%** (need: ≥ 95%). Missed lines primarily in:
  - `application.composition`: CompositionEngine regex edge cases, CapabilityMatcher glob branches, ArtifactScanner error paths
  - `domain.capability`: CapabilityDefinition profile-constructor validation, CapabilityGlob double-star path, ParameterSpec ENUM validation
  - `infrastructure.adapter.output`: YamlCapabilityCatalogAdapter requires-list parsing, kind-switch default branch
- **Branch coverage: 86.5%** (need: ≥ 90%). Gap is pre-existing AND widened by new packages in this epic.
- **Fix:** Add targeted tests covering identified uncovered branches. Minimum to close: ~120 new covered lines in composition/capability packages.

### HIGH — CapabilityDefinition record has 12 constructor params (Rule 03 §≤4 params)

- **Finding:** `CapabilityDefinition(CapabilityId, CapabilityKind, String, Optional<String>, String, String, Map, List, List, List, List, List)` — 12 parameters violates "≤ 4 params per function (use parameter object if more)"
- **Fix:** Extract a `CapabilityMetadata(version, status, description)` nested record and a `CapabilityRelations(requires, provides, excludes, expandsTo, tags)` nested record; use builder or static factories.

### HIGH — CapabilityGraph.topologicalSort() exceeds 25-line method limit

- **Finding:** `topologicalSort()` is ~55 lines (Rule 03 §≤ 25 lines)
- **Fix:** Extract `buildBatch()` and `drainQueue()` private helpers to reduce method to ≤ 25 lines.

---

## Medium Findings

- **TDD commits absent:** All commits bundle tests + implementation. Rule 03 §TDD: "test-first commits — test must appear in git history before or in the same commit as its implementation". The "in the same commit" allowance is satisfied, but no separate red/green/refactor cycles are visible.
- **No refactoring commits:** No `refactor:` Conventional Commits in epic history. Explicit refactoring after green is mandatory (Rule 03 §TDD).
- **.dockerignore missing:** See DevOps review finding DEVOPS-04.
- **Generic exception catch in bash:** `except Exception as e: pass` in `parse_yaml_field()` silently swallows parse errors — should log to stderr at minimum.

---

## Low Findings

- **java.util.logging in TelemetryCapabilityResolver:** Rule 07 §Structured Logging requires structured JSON in production code. LOG.fine() is unstructured.
- **Image not digest-pinned:** eclipse-temurin:21-jdk-alpine tag is mutable (see DevOps review).

---

## Cross-File Consistency

- All capability validators (CycleDetector, MutexValidator, PrerequisiteResolver) use the same helper pattern `def(String id, List<String> requires)` copied independently → extract to `CapabilityTestFixtures` shared test class.
- Exception hierarchy is consistent: all validation errors are `CapabilityError` subtypes.
- Record immutability consistent across all domain types.

---

## GO Conditions (What Must Change for GO)

1. **Coverage MUST reach 95% line / 90% branch** — no exemption (Rule 05 absolute gate)
2. CapabilityGraph.topologicalSort() refactored to ≤ 25 lines
3. CapabilityDefinition 12-param constructor replaced with builder/nested-record pattern
4. .dockerignore created

Items 2-4 can be addressed post-merge if coverage gate is met first (item 1 is the blocker).

---

## Verdict

**NO-GO** — coverage gate fires: 93.2%/86.5% vs 95%/90% required. All items beyond coverage are fixable in a follow-up PR; coverage must be closed in this PR or an immediate predecessor.
