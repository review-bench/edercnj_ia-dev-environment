# Task Breakdown — story-0070-0004

## TASK-0070-0004-001 — Create `_TEMPLATE-ARCHITECTURE-SYSTEM.md`

File: `src/main/resources/shared/templates/_TEMPLATE-ARCHITECTURE-SYSTEM.md`
- Frontmatter v3.0: `requires-capabilities: [governance.value-driven-templates]`, `template-version: "2.0"`
- HTML comment documenting auto-fill vs narrative placeholder syntax
- 11 sections with canonical order from Refinement Notes
- Auto-fill placeholders for: stack, persistência, comunicação, observabilidade, segurança baseline
- Narrative placeholders for: resilience, performance budget, dependency policy, doc targets, integrações externas, decision log

## TASK-0070-0004-002 — Add `assembleSystemArchitecture()` to DocsAssembler

File: `src/main/java/dev/iadev/application/assembler/DocsAssembler.java`
- New public method following existing `assemble()` pattern
- Idempotent: skip if output file already exists
- Context from `ContextBuilder.buildContext(config)`
- Output: `outputDir.resolve("docs/architecture/system.md")`
- Create parent directory if not exists

## TASK-0070-0004-003 — Create golden file for `java-spring` profile

File: `src/test/resources/golden/java-spring/docs/architecture/system.md`
- Authored to match assembleSystemArchitecture() output for java-spring config
- Covered by GoldenFileTest bytewise tree comparison

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
