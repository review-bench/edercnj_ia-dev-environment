# Specialist Review — story-0072-0001

**Epic:** EPIC-0072 — Comprehensive Test Strategy  
**Story:** story-0072-0001 — QualityConfig domain model + capability YAML foundation  
**Reviewer:** Senior Java Architect  
**Review date:** 2026-05-01  
**Branch reviewed:** `origin/epic/0072` (tasks 001–008 merged)  
**Verdict:** GO with mandatory follow-up items (2 blocking, 1 advisory)

---

## Summary

story-0072-0001 delivers the domain-model foundation for EPIC-0072: a new `QualityConfig`
record hierarchy, integration into `Governance` and `ProjectConfig`, 21 tests (18 in
`QualityConfigTest` + 3 in `GovernanceQualityTest`), and 13 capability YAML files under
`capabilities/quality/`. The implementation is architecturally clean and follows established
patterns. Two structural issues require follow-up before the epic branch merges to `develop`:

1. **BLOCKING** — All 13 capability YAML files declare `category: quality`, which is not in the
   `capabilities-1.0.json` schema enum. The enum permits `testing` as the closest match; `quality`
   is not listed. This will fail `audit-frontmatter-schema.sh`.
2. **BLOCKING** — The `quality` category is not registered in `capabilities/_index.yaml`. Rule 28
   §Invariant 3 requires every capability ID to be reachable from `_index.yaml` for the graph audit
   to treat it as a declared (not orphan) capability.
3. **ADVISORY** — `ProjectConfig.java` grows to 339 lines, nine more than before this story (was
   330), on a class that already exceeded the Rule 03 ≤250 line limit. The violation is pre-existing
   but this story widens the gap. A follow-up refactor task should be tracked.

---

## Architecture

### Domain purity

`QualityConfig.java` has a single import: `java.util.Map`. The only peer dependency is
`MapHelper`, which lives in the same `dev.iadev.domain.model` package. No imports from
`application`, `adapter`, or any framework layer. Domain purity per Rule 04 is fully respected.

`Governance.java` and `ProjectConfig.java` likewise import only `java.util.*` and sibling
domain-model classes. `parseQuality(Map<String, Object>)` delegates entirely to
`QualityConfig.fromMap(MapHelper.optionalMap(...))` — no config-layer coupling.

### Aggregate placement

`QualityConfig` hangs off `Governance`, which is the correct grouping per the comment in
`Governance.java`: "policy / meta fields that govern how the generated output is assembled but
do not describe the technology stack itself." Quality-gate thresholds are governance policy, not
tech-stack description — placement is correct.

The delegation chain `ProjectConfig.quality() → governance.quality()` follows the established
pattern set by `documentation()` in EPIC-0071. Symmetry is maintained; no new structural
precedents are introduced.

### Backward-compatible constructor

The 14-argument `ProjectConfig` constructor explicitly sets `QualityConfig.DEFAULT` — no null
is passed to `Governance`. This is a correct use of the safe-default pattern established by
EPIC-0071 for `DocumentationConfig.DEFAULT`.

---

## Code Quality

### `QualityConfig.java` (205 lines)

Passes Rule 03 ≤250 line limit.

All `fromMap` factory methods are ≤10 lines. The outer `QualityConfig.fromMap` is 6 lines.
No method exceeds the ≤25 line limit.

**Compact constructor null-coalescing (correct):**

- `QualityConfig` outer: coalescences all three sub-configs to their DEFAULT instances. Correct.
- `PerformanceConfig`: coalescences `slo` to `SloConfig.DEFAULT` and defensively copies
  `toolVersions` via `Map.copyOf(...)`. This prevents mutation of an external map being reflected
  in the immutable record. Correct.
- `SloConfig`: coalescences `rest`, `grpc`, `cli` to their respective DEFAULT instances. Correct.
- `MutationConfig`: coalescences `toolVersions` only. Note that `threshold` and `runtimeCapMin`
  are primitive `int` — cannot be null. Correct.
- `ContractConfig`: no compact constructor declared. All four fields are primitive `boolean` or
  non-null primitives — no null-coalescing needed. Correct.

**Missing numeric-range validation (advisory):**

No bounds checks on `threshold` (should be 0–100), `runtimeCapMin` (should be > 0),
`baselineTolerancePct` (should be ≥ 0), or SLO millisecond values (should be > 0). The domain
accepts `threshold=-5` or `p95Ms=-100` silently. Given that `fromMap` applies `optionalInt`
defaults and YAML inputs are author-controlled, this is low risk for the current scope.
Recommend tracking as a future-scope item (e.g., story-0072-0009) rather than a blocking issue.

### `Governance.java` (66 lines)

Passes Rule 03 ≤250 line limit. `fromMap` is 9 lines. The compact constructor is 5 lines.

Adding `QualityConfig quality` as the sixth parameter of `Governance` keeps it within the
4-parameter guideline for records? No — records are exempt from the 4-parameter method guideline
(the guideline targets methods, not record components). The existing pattern was established
with five fields by EPIC-0071; adding a sixth is consistent. No violation.

### `ProjectConfig.java` (339 lines) — ADVISORY

Rule 03 hard limit: ≤250 lines per class. `ProjectConfig.java` was 330 lines before this story
and is now 339 lines. The violation is pre-existing (established before EPIC-0072), but this story
contributes 9 additional lines without addressing the underlying issue. No emergency, but the class
should be scheduled for a size-reduction refactor. The convenience accessors section (lines 171–240)
and the `parseXxx` static helper section (lines 260–339) are natural extraction candidates.

The two new methods `quality()` and `documentation()` are identical in shape (one-line delegators
to `governance.XYZ()`). Correct pattern.

`parseQuality` is 2 lines and follows the exact pattern of `parseDocumentation`. No issues.

---

## Tests

### `QualityConfigTest` — 18 test methods

The story description states 21 tests; 18 are present. The count difference is likely a draft
artifact; 18 is the actual count on the reviewed branch.

**Coverage assessment by category:**

| Category | Tests present | Assessment |
|---|---|---|
| Happy path — `fromMap` with full block | Yes (perf, mutation, contract enabled blocks) | Complete |
| Happy path — partial block (only some keys) | Yes (e.g., `baselineTolerancePct`, `runtimeCap`) | Complete |
| Happy path — nested SLO sub-blocks | Yes (REST, gRPC, CLI each tested individually) | Complete |
| Backward compat — empty map → all disabled | Yes (`fromMap_emptyMap_returnsAllDisabled`) | Complete |
| Backward compat — missing sub-block → defaults | Yes (3 tests for missing blocks) | Complete |
| Null-safety — outer constructor | Yes (3 tests: nullPerformance, nullMutation, nullContract) | Complete |
| Null-safety — inner compact constructors | Not tested (`new PerformanceConfig(null slo, ...)`) | Gap |
| Error/boundary — negative or zero numeric values | Not tested | Gap |
| Contract flags — `pact`, `openapiBreaking`, `protoBreaking` | Yes (explicit false values tested) | Complete |

**Null-safety gap:** The compact constructors of `SloConfig`, `PerformanceConfig.slo`, and
`SloConfig.rest/grpc/cli` all coalesce null to DEFAULT. These are exercised indirectly through
`fromMap` (which calls `MapHelper.optionalMap` returning an empty map, never null), but not
tested by direct construction. This is a minor gap acceptable for the current scope given that
the compact constructors are defensive programming, not exposed API.

**Test naming convention (Rule 05):** All 18 methods follow the
`[methodUnderTest]_[scenario]_[expectedBehavior]` pattern. Compliant.

**`@Nested` + `@DisplayName` organization:** The nested class structure (`Defaults`,
`PerformanceBlock`, `MutationBlock`, `ContractBlock`, `NullSafety`) is clean and readable.
File is 212 lines, well under the 250-line limit.

### `GovernanceQualityTest` — 3 test methods

All three tests pass:

1. `fromMap_noQualityBlock_defaultsAllDisabled` — verifies the absent-block path returns
   `QualityConfig.DEFAULT` and that `performance`, `mutation`, `contract` are all `enabled=false`.
   Correct integration test for the backward-compatibility contract.
2. `fromMap_qualityBlockPresent_parsedCorrectly` — verifies full block round-trip through
   `Governance.fromMap`. Covers the `mutation.threshold=90` non-default value.
3. `constructor_nullQuality_defaultsToDefault` — verifies null-coalescing in `Governance`'s
   compact constructor. Direct test of `quality = quality == null ? QualityConfig.DEFAULT : quality`.

Integration coverage of `ProjectConfig.quality()` is provided by the existing
`GovernanceTest` (9 methods) which validates round-trip through `ProjectConfig.fromMap`.

---

## Capability YAML Structure

### File inventory

All 13 files are present across three sub-directories:

| Sub-directory | Files (count) | Interface coverage |
|---|---|---|
| `quality/performance/` | rest, grpc, cli, graphql, socket (5) | REST, gRPC, CLI, GraphQL, WebSocket |
| `quality/mutation/` | java-pit, stryker, mutmut, go-mutesting (4) | Java, JS/TS, Python, Go |
| `quality/contract/` | openapi-diff, pact, buf, scc (4) | OpenAPI, Pact, Protobuf, Event schema |

### Required field compliance

All 13 files declare the five required fields (`id`, `category`, `kind`, `version`, `status`).
The `description`, `requires-capabilities`, `requires`, `excludes`, and `universal` fields are
present and consistent across files.

### `requires-capabilities: []` — correct

Rule 28 §Invariant 1 requires that every artefact outside `_common/` declares
`requires-capabilities`. All 13 files declare `requires-capabilities: []` (universal by default,
no dependency on other capabilities). Correct for atomic leaf capabilities.

### BLOCKING ISSUE 1: `category: quality` not in schema enum

The `capabilities-1.0.json` schema defines a closed enum for `category`:

```
["language", "framework", "interface", "data", "messaging",
 "architecture", "observability", "deployment", "security",
 "compliance", "testing", "cloud", "governance"]
```

`quality` is not in this list. All 13 capability files declare `category: quality`, which
will fail `audit-frontmatter-schema.sh` (Rule 28 §Invariant 2, schema v3.0 strict).

**Required action before epic merge:** Either:
- Add `"quality"` to the `category` enum in `governance/schemas/capabilities-1.0.json` with an
  ADR-0016 amendment note (preferred — `testing` is a subset; `quality` is broader), or
- Rename `category: quality` to `category: testing` in all 13 files (least-effort path, but
  semantically weaker since `quality` includes mutation and contract testing beyond test execution).

### BLOCKING ISSUE 2: `quality` category absent from `_index.yaml`

`capabilities/_index.yaml` does not have a `quality` entry. Rule 28 §Invariant 3 states that
capability IDs referenced in frontmatter must be traceable through the catalog.
`audit-capability-graph.sh` will report these as orphan or unknown capabilities.

**Required action:** Add a `quality` category entry to `_index.yaml` with the 13 IDs:

```yaml
  - name: quality
    description: Quality gates — performance SLOs, mutation testing, contract compatibility
    capabilities:
      - quality.performance.rest
      - quality.performance.grpc
      - quality.performance.cli
      - quality.performance.graphql
      - quality.performance.socket
      - quality.mutation.java-pit
      - quality.mutation.stryker
      - quality.mutation.mutmut
      - quality.mutation.go-mutesting
      - quality.contract.openapi-diff
      - quality.contract.pact
      - quality.contract.buf
      - quality.contract.scc
```

### Parameter type: `integer` vs `int` — pre-existing schema inconsistency

The schema enum for parameter types is `["string", "int", "boolean", "enum"]` (uses `int`),
while all capability files across the entire repository — including pre-EPIC-0072 files such as
`capabilities/governance/doc-as-dod.yaml` — use `type: integer`. This is a pre-existing
inconsistency: 23 files use `integer` and zero use `int`. The new quality capability files
follow the established convention. This issue predates story-0072-0001 and is out of scope for
this review; it should be tracked separately.

---

## Verdict

**GO** — with two blocking follow-up items that MUST be resolved before the `epic/0072 → develop`
PR is merged.

| # | Severity | Item |
|---|---|---|
| 1 | BLOCKING | Add `"quality"` to `category` enum in `governance/schemas/capabilities-1.0.json` and verify `audit-frontmatter-schema.sh` passes |
| 2 | BLOCKING | Add `quality` category to `capabilities/_index.yaml` with all 13 capability IDs |
| 3 | ADVISORY | Track `ProjectConfig.java` size reduction (339 lines, limit 250) as a future story |

The core domain implementation — `QualityConfig` record hierarchy, null-safety compact
constructors, `Governance` integration, `ProjectConfig` delegators, and `parseQuality` helper —
is correct, clean, and backward-compatible. The safe-default contract (absent `quality:` block
→ all disabled) is properly tested. These blocking items are catalog/governance hygiene issues
that require no changes to the Java domain model itself.
