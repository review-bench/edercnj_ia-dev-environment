# Implementation Plan — story-0070-0004

## Objective

Create `_TEMPLATE-ARCHITECTURE-SYSTEM.md` (11 sections, frontmatter v3.0), wire `DocsAssembler.assembleSystemArchitecture()`, and add 1 golden file for the `java-spring` profile.

## Steps

### 1. Create template `_TEMPLATE-ARCHITECTURE-SYSTEM.md`

File: `src/main/resources/shared/templates/_TEMPLATE-ARCHITECTURE-SYSTEM.md`

- Add frontmatter v3.0: `requires-capabilities: [governance.value-driven-templates]`, `template-version: "2.0"`
- HTML comment at top documenting auto-fill vs narrative sections and placeholder syntax
- 11 sections with headings H2 (`##`) matching the canonical order from Refinement Notes:
  1. Stack Resolvida — auto-fill: `{{language_name}} {{language_version}}`, `{{framework_name}} {{framework_version}}`, `{{build_tool}}`, `{{architecture_style}}`
  2. Persistência — auto-fill: `{{database_name}}` / `{{migration_name}}` / `{{cache_name}}`; fallback text when `none`
  3. Comunicação — auto-fill: `{{message_broker}}`, `{{interfaces_list}}`
  4. Observabilidade — auto-fill placeholder (EPIC-0072 delivers; manual before)
  5. Resilience — manual/narrative section
  6. Performance Budget — manual placeholder (EPIC-0072 auto-fill pending)
  7. Segurança Baseline — auto-fill: `{{compliance}}`
  8. Dependency Policy — manual placeholder (EPIC-0074 auto-fill pending)
  9. Documentação Targets — manual placeholder (EPIC-0071 auto-fill pending)
  10. Integrações Externas — manual/narrative section
  11. Decision Log do Sistema — cumulative, populated by `/x-arch-system-update`

### 2. Add `assembleSystemArchitecture()` to `DocsAssembler.java`

File: `src/main/java/dev/iadev/application/assembler/DocsAssembler.java`

- Method signature: `public void assembleSystemArchitecture(ProjectConfig config, TemplateEngine engine, Path outputDir)`
- Check if `outputDir.resolve("docs/architecture/system.md")` exists; skip if so (idempotent)
- Build context via `ContextBuilder.buildContext(config)`
- Add "stateless" flag to context: if database_name == "none" AND cache_name == "none", add `persistence_none=true`
- Render template `shared/templates/_TEMPLATE-ARCHITECTURE-SYSTEM.md`
- Write to `outputDir.resolve("docs/architecture/system.md")` (create parent dirs if needed)

### 3. Create golden file for `java-spring` profile

File: `src/test/resources/golden/java-spring/docs/architecture/system.md`

- Manually author the expected output for `java-spring` profile (Java 21, Spring Boot 3.x, PostgreSQL, Flyway, Maven, Kafka)
- Must be bytewise-consistent with what `assembleSystemArchitecture()` will produce
- Covered by existing GoldenFileTest tree comparison

## File Footprint

```
write:
  - src/main/resources/shared/templates/_TEMPLATE-ARCHITECTURE-SYSTEM.md
  - src/main/java/dev/iadev/application/assembler/DocsAssembler.java
  - src/test/resources/golden/java-spring/docs/architecture/system.md
read:
  - src/main/java/dev/iadev/config/ContextBuilder.java
  - src/main/java/dev/iadev/application/assembler/DocsAssembler.java
regen:
  - src/test/resources/golden/java-spring/docs/architecture/system.md
```
