# Implementation Plan — story-0077-0024

**Story:** x-epic-create --from-feature (v5 obrigatório); drops Sections 2/4/8
**Status:** Concluída
**Planned at:** 2026-05-05T18:30:00Z

## 1. Scope

Implement `x-epic-create --from-feature` that creates an epic from a Feature artifact:
- Source Feature populated in generated epic
- Inherited RNFs loaded from feature → capability → product
- Sections 2, 4, 8 dropped (not applicable when Epic comes from Feature)

## 2. File Footprint

**write:**
- `src/main/java/dev/iadev/adapter/inbound/cli/XEpicCreateCommand.java`
- `src/main/java/dev/iadev/adapter/outbound/feature/EpicFromFeatureArtifactWriter.java`
- `src/main/java/dev/iadev/application/feature/CreateEpicFromFeatureUseCase.java`
- `src/main/java/dev/iadev/application/feature/CreateEpicFromFeatureResult.java`
- `src/main/java/dev/iadev/application/feature/FeatureEpicSource.java`
- `src/main/java/dev/iadev/application/feature/FeatureEpicSourceLoader.java`
- `src/main/java/dev/iadev/application/feature/FeatureMarkdownParser.java`
- `src/main/java/dev/iadev/application/feature/InheritedRnfLine.java`
- `src/main/resources/targets/claude/skills/core/plan/x-epic-create/SKILL.md`
- `src/main/resources/targets/claude/skills/core/internal/plan/x-internal-create-epic/SKILL.md`
- `src/test/java/dev/iadev/adapter/inbound/cli/XEpicCreateCommandTest.java`
- `src/test/java/dev/iadev/adapter/outbound/feature/EpicFromFeatureArtifactWriterTest.java`
- `src/test/java/dev/iadev/application/feature/FeatureEpicSourceLoaderTest.java`
- `src/test/java/dev/iadev/application/feature/FeatureMarkdownParserTest.java`
- Golden files (9 profiles × 2 skills)

## 3. Architecture

- Domain: `FeatureEpicSource`, `InheritedRnfLine`
- Application: `CreateEpicFromFeatureUseCase`, `CreateEpicFromFeatureResult`
- Adapter inbound: `XEpicCreateCommand` (adds `--from-feature` flag)
- Adapter outbound: `EpicFromFeatureArtifactWriter`, `FeatureEpicSourceLoader`, `FeatureMarkdownParser`

## 4. AC Coverage

| AC | Result |
|---|---|
| --from-feature populates Source Feature section | PASS |
| Inherited RNFs loaded from feature chain | PASS |
| Sections 2/4/8 absent from generated epic | PASS |
| Epic uses v5 template format | PASS |
