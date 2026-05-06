---
epic-id: EPIC-0058
slug: audit-scripts-lifecycle
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [governance, audit, scripts, assembler, lifecycle]
capabilities-affected: []
rules-affected: [Rule 26]
adrs-referenced: [ADR-0015]

patterns-introduced:
  - scripts-assembler-generated-governance
  - self-check-flag-contract
  - audit-catalog-before-add
antipatterns-rejected:
  - rule-references-nonexistent-audit-script
  - ad-hoc-audit-layer-selection

dependencies-of: [EPIC-0049, EPIC-0057]
dependencies-for: [EPIC-0061, EPIC-0063]
---
# Memory: EPIC-0058 — Audit Scripts Lifecycle & Generation

## Why this epic existed

Three audit scripts referenced in Rules 19, 21, and 22 (`audit-flow-version.sh`, `audit-epic-branches.sh`, `audit-skill-visibility.sh`) did not exist on disk. Rules cited them as enforcement mechanisms but the scripts were never created. Additionally, decisions about which layer to use for new governance gates (Hook? CI script? Java test? Workflow?) were made ad-hoc per epic without formal guidance, leading to fragmentation: EPIC-0046 used a Java test, EPIC-0050 used a CI script, EPIC-0052 used a hybrid — no unified taxonomy.

## Hypothesis tested

Introducing Rule 26 "Audit Gate Lifecycle" with a 4-layer taxonomy (Hook runtime / CI script / Java test / CI workflow), creating the 3 missing scripts, and adding `ScriptsAssembler` so generated projects inherit governance gates would formalize the audit infrastructure. **Confirmed**: Rule 26 published (ADR-0015); 3 scripts created (`audit-flow-version.sh`, `audit-epic-branches.sh`, `audit-skill-visibility.sh`); `ScriptsAssembler` delivered; `docs/audit-gates-catalog.md` as canonical catalog. Note: EPIC-0061 later deleted `audit.yml` (CI workflow), migrating audits to `mvn verify` via Java `*AuditorTest` classes.

## Decisions taken (with why)

1. **4-layer taxonomy in Rule 26** — Hook runtime / CI script / Java test / CI Workflow; each layer has a decision criterion matrix; prevents ad-hoc layer selection.
2. **`--self-check` flag mandatory on all CI scripts** — script validates its own structural prerequisites before the main audit run; exit 2 (`OPERATIONAL_ERROR`) if prerequisites missing.
3. **Catalog-before-add (RULE-004)** — no gate may be introduced in any Rule, ADR, or SKILL.md without a simultaneous `docs/audit-gates-catalog.md` entry.
4. **`ScriptsAssembler`** — generates audit scripts into target projects via source-of-truth in `java/src/main/resources/targets/claude/scripts/`; projects inherit governance gates.

## Alternatives rejected (with why)

- **Keeping scripts in root `scripts/` dir without `ScriptsAssembler`** — generated projects would not inherit governance; generator and generated projects would drift.
- **Single monolithic audit script** — impossible to run specific gate independently; harder to debug; violates single-responsibility.

## Reusable patterns produced

- **`scripts-assembler-generated-governance`**: `ScriptsAssembler` copies audit scripts to generated projects; every new project inherits governance gates from source-of-truth.
- **`self-check-flag-contract`**: every `audit-*.sh` must implement `--self-check` returning 0 (ok) or 2 (OPERATIONAL_ERROR); CI pre-flight runs `--self-check` before main audit matrix.
- **`audit-catalog-before-add`**: new governance gates MUST have a `docs/audit-gates-catalog.md` entry before they are referenced in any Rule or skill.

## Anti-patterns observed

- **Rule references nonexistent audit script** — Rules 19, 21, 22 cited scripts that had never been created; silent enforcement gap.

## Links

- Epic: `ai/epics/epic-0058-audit-scripts-lifecycle/epic-0058.md`
- ADRs: `docs/adr/ADR-0015-audit-gate-lifecycle.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0058-audit-scripts-lifecycle/reports/`
