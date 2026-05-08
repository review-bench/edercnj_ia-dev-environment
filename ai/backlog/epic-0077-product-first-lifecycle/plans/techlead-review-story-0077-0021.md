---
decision: GO
---

# Tech-Lead Review — story-0077-0021

**Story:** RNF Root Validator Gate
**Commit reviewed:** `377f7890e` on `epic/0077`
**Decision:** GO

---

## PR Readiness

The implementation is mergeable as-is. All four files examined are coherent, consistent, and correctly wired:

- `RNFRootValidator` is a pure domain service with no external dependencies — appropriate for its layer.
- `CreateProductOrchestrationUseCase` wires the validator correctly as the 4th constructor parameter and fires the gate at the right point in the execution flow (after transform, before C1 stub creation).
- `XCreateProductCommand` instantiates the use case with all four collaborators and correctly propagates `EXIT_VALIDATION` on gate failure.
- `RNFRootValidatorTest` covers the full boundary surface with 9 concrete scenarios.

No regression risk to existing flows: the gate only activates on the non-dry-run path, and dry-run is unchanged. CI green per specialist review report.

---

## Architecture Decision

**Gate placement is correct.** Firing after `IdeationToProductTransformer.transform()` and before `CapabilityStubFactory.createC1Stub()` is the only viable position for this check:

- Pre-transform validation belongs to `IdeationValidator` (already in place). The RNF gate validates the *result* of the transformation — a fully populated `Product` with `List<RNFRoot>`. Firing before transform would require validating raw ideation sections, coupling the gate to markdown parsing rather than domain semantics.
- Post-transform but pre-stub is the correct fail-fast boundary: creating a C1 stub for a product that fails RNF coverage is wasteful and would require rollback. Blocking here is strictly cheaper.
- The gate returns a structured `RNFRootValidationResult` value object rather than throwing an exception, which is consistent with how `IdeationValidationResult` is handled. The symmetry is a deliberate and correct pattern.

**Validator decomposition** (`validateDescriptions` / `validateMandatoryCategories` / `validateTotalCount`) is clean. Three private methods, each ≤ 10 lines, accumulate into a single shared `errors` list — no duplication, no leakage between concerns. The dual check in `validateMandatoryCategories` (per-category missing error + aggregate count error) is intentional: it gives operators actionable per-category feedback rather than a raw count alone.

**No domain pollution.** `RNFRootValidator` has zero external imports — only `java.util.*` types and same-package domain objects. This is exactly right for a domain validator.

---

## Test Strategy

Test naming follows the project convention (`[scenario]_[expectedBehavior]`). Coverage of the boundary surface is thorough:

| Test | Boundary probed |
| :--- | :--- |
| `completeProduct_allMandatoryCategories_passes` | Happy path — gate passes |
| `missingSecurity_reportsSpecificError` | Missing mandatory category reports named category |
| `missingPerformance_reportsSpecificError` | Symmetry check — different mandatory category |
| `onlyTwoMandatoryCategories_reportsCountError` | Mandatory count sub-threshold |
| `allSixMandatoryOnly_noOptionals_failsTotalCountGate` | Boundary: 6 of 10 (exactly at total-count floor minus 4) |
| `nineCategories_failsTotalCountGate` | Boundary: 9 of 10 (one below total floor) |
| `tenCategories_sixMandatoryFourOptional_passes` | Boundary: exactly 10 — minimum compliant |
| `blankRNFDescription_reportsContentError` | Blank description caught with category name in error |
| `multipleViolations_reportsAll` | Empty product reports ≥ 2 errors (accumulation works) |

The boundary pair `nineCategories_failsTotalCountGate` / `tenCategories_sixMandatoryFourOptional_passes` directly probes the `< MIN_TOTAL_CATEGORIES` condition at n-1 and n. This is the most important gate boundary and both sides are covered. No off-by-one risk.

Assertions are specific: `anyMatch(e -> e.contains("SECURITY"))` pins the error message to a category string, not a generic "validation failed". The multi-violation test uses `hasSizeGreaterThanOrEqualTo(2)` which is the correct form — it does not over-constrain the exact count, accommodating future addition of new mandatory categories without breaking the test.

**One minor observation (non-blocking):** `mandatoryRnfs()` helper returns a mutable `ArrayList` and is mutated by callers (`rnfs.set(0, ...)`, `rnfs.removeIf(...)`). This is safe because each test gets a fresh instance via the helper call. However, the raw `java.util.ArrayList` type reference on lines 141 and 155–156 bypasses the `var` idiom used elsewhere and the `List` return type would be cleaner. ADVISORY — negligible in practice.

---

## Specialist Issues Assessment

| Issue | Severity | Decision | Rationale |
| :--- | :--- | :--- | :--- |
| `call()` method 38 lines (Rule 03 limit 25) | MEDIUM | ADVISORY — acceptable for CLI adapter | The method consists of three sequential blocks: arg validation, dry-run short-circuit, and use-case execution. The logic is linear — no branching complexity beyond the early-return guards. Extracting `executeUseCase()` would be the right refactor but does not affect correctness or testability. Defer to a future hardening story. |
| Missing null guard on `RNFRootValidator.validate(Product)` | LOW | ADVISORY — unreachable in production path | `CreateProductOrchestrationUseCase.execute()` already guards `ideation != null` (line 35–37), and `IdeationToProductTransformer.transform()` is not expected to return null by contract. Adding `Objects.requireNonNull(product, "product must not be null")` as the first line of `validate()` is good defensive coding and takes one line — ADVISORY to add in the same hardening story. |
| 121-char annotation string in `@Command.description` | LOW | ADVISORY — negligible | Violates the 120-char line width limit by 1 character. No functional impact. Wrap at next opportunity. |

---

## Merge Conditions

- [x] All tests passing (specialist review confirms `mvn verify` BUILD SUCCESS)
- [x] `mvn verify` BUILD SUCCESS on `epic/0077`
- [x] No breaking API changes — `CreateProductOrchestrationUseCase` is an internal application class not part of any public API surface
- [x] Specialist review GO
- [x] Gate placement reviewed and confirmed correct (application layer, post-transform, pre-stub)
- [x] Domain purity maintained — `RNFRootValidator` has zero external imports

---

## Decision

**GO — mergeable as-is.**

The three advisory issues (method length, missing null guard, annotation line width) are correctly classified as tech debt. None affects correctness, testability, or domain integrity. They are tracked above and should be addressed in the next hardening cycle. The core implementation — validator logic, gate wiring, test boundary coverage — is solid.
