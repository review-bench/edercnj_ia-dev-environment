---
epic-id: EPIC-0044
slug: deprecated-code-removal
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [maintenance, refactor, java, cleanup]
capabilities-affected: []
rules-affected: []
adrs-referenced: []

patterns-introduced:
  - mechanical-caller-migration-before-removal
antipatterns-rejected:
  - leaving-deprecated-symbols-in-production

dependencies-of: []
dependencies-for: [EPIC-0048]
---
# Memory: EPIC-0044 — Deprecated Code Removal

## Why this epic existed

Six Java symbols marked `@Deprecated(forRemoval = true)` in `StackMapping.java` and `ResourceResolver.java` had replacement APIs but continued to be consumed by 24 production files and 29+ test files. This produced 40+ `[removal]` compiler warnings on every build. The deprecated symbols would eventually be removed but callers were never migrated.

## Hypothesis tested

Mechanical migration of all callers to replacement APIs followed by physical removal of deprecated symbols would eliminate all `[removal]` warnings without functional regression or MAJOR version bump. **Confirmed**: 24 production files + 29+ test files migrated; 6 symbols removed; golden files regenerated where deterministic output changed.

## Decisions taken (with why)

1. **No MAJOR bump** — change is internal, public API surface preserved; deprecated removal is pre-announced.
2. **Golden file regeneration** — where assembler output changed deterministically, goldens regenerated rather than grandfathered.
3. **Test migration included** — test files referencing deprecated APIs updated to use replacements; no `@SuppressWarnings("removal")` bandaids.

## Alternatives rejected (with why)

- **Keep deprecated + add `@SuppressWarnings`** — defers the problem; compile noise continues.

## Reusable patterns produced

- **`mechanical-caller-migration-before-removal`**: find all callers with grep/IDE → migrate all → remove symbol; no partial removal.

## Anti-patterns observed

- **Leaving deprecated symbols** with callers indefinitely — compile noise trains operators to ignore warnings.

## Links

- Epic: `ai/epics/epic-0044-deprecated-code-removal/epic-0044.md`
- ADRs: (none recorded)
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0044-deprecated-code-removal/reports/`
