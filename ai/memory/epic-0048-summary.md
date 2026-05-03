---
epic-id: EPIC-0048
slug: java-only-generator-bugfixes
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [generator, java, bugfix, breaking, cleanup]
capabilities-affected: []
rules-affected: []
adrs-referenced: []

patterns-introduced:
  - claude-md-assembler-dedicated
  - empty-dir-prune-post-assembly
  - java-only-scope-restriction
antipatterns-rejected:
  - multi-language-generator-with-dead-profiles
  - filecategorizer-without-producer

dependencies-of: [EPIC-0044]
dependencies-for: [EPIC-0055]
---
# Memory: EPIC-0048 — Java-Only Generator + Bug A/B Fixes

## Why this epic existed

The generator supported 6 languages (Java, Python, Go, Kotlin, TypeScript, Rust) but 100% of actual usage was Java. The multi-language support generated maintenance cost: ~2,835 non-Java golden files, 17 smoke test profiles (only 9 Java), and constant duplication in agents/hooks/rules. Simultaneously, two confirmed bugs existed: **Bug A** — `ia-dev-env generate` created empty directories (`.github/`, `.codex/`, `.cursor/`) in the output; root cause in `CopyHelpers.copyDirectory#preVisitDirectory` calling `Files.createDirectories` before knowing if content would follow. **Bug B** — `FileCategorizer.isRootFile` (line 88) recognized `CLAUDE.md` as a root file, but no assembler produced it — generated projects had no executive-summary file.

## Hypothesis tested

Dropping all non-Java language profiles (v4.0.0 MAJOR), fixing Bug A with `pruneEmptyDirs` post-assembly in `AssemblerPipeline`, and adding a dedicated `ClaudeMdAssembler` (ADR-0048-B) would eliminate the maintenance burden and make all generated projects standards-compliant. **Confirmed**: 8 non-Java profiles removed; 2,835 golden files removed; Bug A + Bug B fixed; 9 Java profiles retained; `ClaudeMdAssembler` registered as last group in `AssemblerFactory`; `legacy/v3` branch preserved read-only.

## Decisions taken (with why)

1. **MAJOR version bump (v4.0.0)** — dropping non-Java support is a breaking change; users on Python/Go/etc. pin v3.x. `legacy/v3` branch kept for pinned users.
2. **`ClaudeMdAssembler` as dedicated assembler** (ADR-0048-B) — single-responsibility; registered last so it can aggregate metadata from all prior assemblers.
3. **`pruneEmptyDirs` in `AssemblerPipeline` post-assembly** — scan output dir after generation and remove any empty subdirectories; prevents Bug A recurrence.

## Alternatives rejected (with why)

- **Keep multi-language profiles as no-op** — maintenance cost continues; compile-time cost; golden files bloat.
- **Inline `CLAUDE.md` in `SkillsAssembler`** — violates single responsibility; `SkillsAssembler` should not know about root-level documentation.

## Reusable patterns produced

- **`claude-md-assembler-dedicated`**: root-level summary files get a dedicated assembler; registered last in `AssemblerFactory` pipeline.
- **`empty-dir-prune-post-assembly`**: `pruneEmptyDirs` pass after all assemblers run; prevents empty directories in generated output.
- **`java-only-scope-restriction`**: remove dead profiles rather than maintaining stubs; `GoldenFileCoverageTest` enforces symmetry.

## Anti-patterns observed

- **Multi-language generator with dead profiles** — 100% of usage is Java but 8 non-Java profiles generate 2,835 golden files and slow every CI run.
- **FileCategorizer recognizing file without producer** — `CLAUDE.md` classified as root file but no assembler generates it; causes silent project misconfiguration.

## Links

- Epic: `ai/epics/epic-0048-java-only-generator-bugfixes/epic-0048.md`
- ADRs: `docs/adr/ADR-0048-A-java-only-scope.md`, `docs/adr/ADR-0048-B-claude-md-contract.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0048-java-only-generator-bugfixes/reports/`
