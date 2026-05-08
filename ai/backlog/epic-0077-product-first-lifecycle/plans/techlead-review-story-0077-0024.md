---
decision: GO
---

# Tech-Lead Review — story-0077-0024

**Story:** x-epic-create --from-feature (v5 obrigatório); drops Sections 2/4/8
**Commit reviewed:** `a3ba39c1a` on `epic/0077`
**Decision:** GO

---

## PR Readiness

The implementation is mergeable. All examined files are coherent and correctly wired:

- `CreateEpicFromFeatureUseCase` is a clean orchestrator — delegates to loader and writer
  with no I/O of its own, which is correct for the application layer.
- `FeatureEpicSourceLoader` correctly encapsulates the RNF inheritance traversal
  (feature → capability → product) without polluting the use case with chain logic.
- `EpicFromFeatureArtifactWriter` owns all file I/O and markdown generation — correct
  placement in `adapter.outbound`.
- `XEpicCreateCommand` wires all collaborators at the CLI boundary and maps results to
  exit codes consistently with other commands in the module.
- 8 tests across 4 classes — all green. Golden file parity confirmed (9/9 profiles).

No regression risk to existing `x-create-feature` / `x-feature-create` flows. The new
`--from-feature` flag is additive; the existing `x-create-feature` skill remains
unchanged. CI green per specialist review report.

---

## Architecture Decision

**Section drop (Sections 2, 4, 8) is correct.** When an epic is derived from a Feature
artifact via `--from-feature`, three template sections become redundant:

- **Section 2 (Persona & Stakeholders):** already defined in the Feature spec at the
  product-level — duplicating in every derived epic creates drift risk.
- **Section 4 (Critérios de Aceite):** epics derived from features inherit AC categories
  from the Feature; story-level ACs are populated per-story. Epic-level AC duplication
  adds noise.
- **Section 8 (Quality Gates):** inherited from the parent Feature → Capability → Product
  chain via the RNF mechanism introduced in story-0077-0022. Redundant at epic level.

This decision is consistent with the Product-First Lifecycle principle: the Feature is
the single source of truth for persona, AC shape, and quality posture. Epics are
implementation groupings — they should not re-document what the Feature already owns.

**RNF inheritance chain** (`FeatureEpicSourceLoader`) is correctly implemented:

1. Parse feature markdown → extract `capability-id` and `product-id` references.
2. Load capability file → extract inherited RNFs.
3. Load product file → extract product-level RNF roots.
4. Compose `FeatureEpicSource` with the full chain.

This matches the chain established by `x-internal-rnf-validate` (story-0077-0022) and
the `enforce-refinement-gate.sh` extension (story-0077-0023). The three stories form a
coherent RNF lifecycle: validate (0022) → gate (0023) → propagate to epic (0024).

**`InheritedRnfLine` model** is a well-scoped value object. It carries `category`,
`description`, and `source` (which layer declared the RNF — feature, capability, or
product). The `source` field enables transparency in the generated epic: readers can
trace each inherited RNF back to its originating layer.

---

## Test Strategy

| Test class | Tests | Boundary coverage |
|---|---|---|
| `XEpicCreateCommandTest` | 4 | Help text, missing `--from-feature`, dry-run, valid run with artifact write |
| `EpicFromFeatureArtifactWriterTest` | 1 | Content correctness (sourceFeature + RNF sections present) |
| `FeatureEpicSourceLoaderTest` | 2 | Happy path load + missing feature file error |
| `FeatureMarkdownParserTest` | 1 | Section extraction correctness |

Coverage is sufficient for the scope: the critical path (load feature → inherit RNFs → write epic) is fully exercised. The section-drop behavior is verified indirectly via the artifact content test.

**One minor gap (non-blocking):** no test probes the case where the feature file references a capability that does not exist on disk. `FeatureEpicSourceLoader` returns a partial `FeatureEpicSource` in this case (RNF chain stops at feature level). This is a graceful degradation — acceptable behavior — but an explicit test for this scenario would strengthen the coverage. Deferred to a follow-up hardening story.

---

## Naming and Convention Check

| File | Convention | Status |
|---|---|---|
| `XEpicCreateCommand` | Inbound adapter `X` prefix, command suffix | ✓ |
| `CreateEpicFromFeatureUseCase` | `Create...UseCase` application orchestrator | ✓ |
| `FeatureMarkdownParser` | Parser suffix for file-parsing utility | ✓ |
| `FeatureEpicSourceLoader` | Loader suffix for data-loading service | ✓ |
| `EpicFromFeatureArtifactWriter` | `...Writer` outbound adapter suffix | ✓ |
| `FeatureEpicSource` | Record with `Source` suffix | ✓ |
| `InheritedRnfLine` | Value object — descriptive, domain-appropriate | ✓ |

All names are consistent with established conventions.

---

**Decision: GO.** No blocking issues. Implementation follows Product-First Lifecycle
design intent. RNF inheritance chain is correctly implemented and composable with
story-0077-0022 and 0077-0023.
