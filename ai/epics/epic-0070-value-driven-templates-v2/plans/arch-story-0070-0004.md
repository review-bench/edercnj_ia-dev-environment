# Architecture Plan — story-0070-0004

## Context

Story-0070-0004 introduces `_TEMPLATE-ARCHITECTURE-SYSTEM.md` and the `DocsAssembler.assembleSystemArchitecture()` method that renders it. This is a new output document type (docs/architecture/system.md) added to the generator pipeline.

## Architectural Decision

**Pattern:** Follow the existing `DocsAssembler.assemble()` pattern — same context map (ContextBuilder.buildContext), same TemplateEngine rendering, same output-dir resolution. A new method `assembleSystemArchitecture(ProjectConfig, TemplateEngine, Path)` avoids modifying the existing `assemble()` signature.

**Template approach:** 11 sections with `{{KEY}}` placeholders. Auto-fill keys resolved from ContextBuilder context; narrative sections use instructional placeholder text (not empty). Handlebars-style `{{#each}}` used for lists (interfaces_list).

**Golden file:** One reference profile (`java-spring`) at `src/test/resources/golden/java-spring/docs/architecture/system.md`. Covered by the existing GoldenFileTest bytewise tree comparison without harness changes.

## Layer Impact

| Layer | File | Change |
|---|---|---|
| Template | `shared/templates/_TEMPLATE-ARCHITECTURE-SYSTEM.md` | NEW — 11 sections + frontmatter v3.0 |
| Application | `application/assembler/DocsAssembler.java` | ADD method `assembleSystemArchitecture()` |
| Test resource | `test/resources/golden/java-spring/docs/architecture/system.md` | NEW golden leaf |

## Constraints

- Template MUST use only context keys available from `ContextBuilder.buildContext()`: `language_name`, `language_version`, `framework_name`, `framework_version`, `build_tool`, `architecture_style`, `database_name`, `migration_name`, `cache_name`, `message_broker`, `container`, `orchestrator`, `interfaces_list`, `compliance`, `event_driven`, `domain_driven`
- `docs/architecture/system.md` MUST NOT be overwritten on re-runs (only created if absent) — the `assembleSystemArchitecture()` implementation must check file existence first
- Template line count MUST stay ≤ 800 lines (Refinement Notes split-modular threshold)
- Sensitive YAML values must not flow into the template — the context map only carries non-sensitive structural config

## No Deviations

All changes follow established patterns; no new ADR required.
