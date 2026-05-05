# Implementation Plan — story-0077-0004

**Story:** story-0077-0004 — _TEMPLATE-IDEATION.md (7 seções)  
**Phase:** Phase 1 — Product-First Lifecycle

---

## Sequence

### TASK-0077-0004-001 — Template estrutura + exemplos

**Branch:** `feat/task-0077-0004-001-ideation-template`  
**Target:** `epic/0077`

1. Create `ai/templates/_TEMPLATE-IDEATION.md` with 7 canonical sections, each with:
   - Section heading (numbered `## N. Section Title`)
   - Instruction block (italicized guidance for author)
   - Required fields table
   - Example placeholder rows
2. Create `ai/examples/example-ideation-ecommerce.md` — fully filled e-commerce ideation
3. Create `ai/examples/example-ideation-saas.md` — fully filled SaaS ideation
4. Commit + push + open PR → epic/0077 with auto-merge

### TASK-0077-0004-002 — Validador schema + integration

**Branch:** `feat/task-0077-0004-002-ideation-validator`  
**Target:** `epic/0077`  
**Depends on:** TASK-0077-0004-001

TDD Red-Green-Refactor cycle:

1. **Red:** Write `IdeationValidatorTest` — 7 unit scenarios:
   - `completeIdeation_allSections_passes()`
   - `missingSection_reportsSpecificError()`
   - `emptyTitle_reportsError()`
   - `belowMinimumRequirements_failsCount()`
   - `aboveMaximumStakeholders_stillValid()`
   - `allSectionsPresent_noErrors()`
   - `multipleViolations_reportsAll()`
2. **Green:** Implement `IdeationSection`, `IdeationTemplate`, `IdeationValidator`, `IdeationValidationResult`, `IdeationValidationUseCase`
3. **Refactor:** Extract parsing logic; ensure immutability
4. Run `mvn test -pl . -Dtest=IdeationValidatorTest`; verify coverage ≥ 95% line, ≥ 90% branch
5. Commit + push + open PR → epic/0077

### TASK-0077-0004-003 — Pilot ideações + smoke test

**Branch:** `feat/task-0077-0004-003-pilot-ideations`  
**Target:** `epic/0077`  
**Depends on:** TASK-0077-0004-002

1. Create 3 pilot ideation files under `ai/examples/`:
   - `pilot-ideation-001.md` — FinTech payment product ideation
   - `pilot-ideation-002.md` — Healthcare scheduling product ideation
   - `pilot-ideation-003.md` — EdTech learning platform ideation
2. Create `ci/smoke/ideation-template-smoke.sh`:
   - Checks 7 sections present in each pilot
   - Exits 0 on full pass, 1 on any failure
3. Run smoke script; verify PASS 3/3
4. Run full test suite: `mvn test`
5. Commit + push + open PR → epic/0077
