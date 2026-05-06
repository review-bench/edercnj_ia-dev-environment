# Implementation Plan — story-0077-0025

**Story:** x-story-create --from-feature + --epic-id; drops Sections 2/4/8
**Status:** Concluída
**Planned at:** 2026-05-05T19:00:00Z

## 1. Scope

Implement `x-story-create --from-feature` that creates stories from a Feature artifact:
- Source Feature populated in generated stories
- `--epic-id` flag to associate stories with a specific epic
- Sections 2, 4, 8 dropped (not applicable when Story comes from Feature)

## 2. File Footprint

**write:**
- `src/main/java/dev/iadev/adapter/inbound/cli/XStoryCreateCommand.java`
- `src/main/java/dev/iadev/adapter/outbound/feature/StoryFromFeatureArtifactWriter.java`
- `src/main/java/dev/iadev/application/feature/CreateStoriesFromFeatureUseCase.java`
- `src/main/java/dev/iadev/application/feature/CreateStoriesFromFeatureResult.java`
- `src/main/resources/targets/claude/skills/core/plan/x-story-create/SKILL.md`
- `src/main/resources/targets/claude/skills/core/internal/plan/x-internal-create-story/SKILL.md`
- `src/test/java/dev/iadev/adapter/inbound/cli/XStoryCreateCommandTest.java`
- `src/test/java/dev/iadev/adapter/outbound/feature/StoryFromFeatureArtifactWriterTest.java`
- Golden files (9 profiles × 2 skills)

## 3. Architecture

- Domain: reuses `FeatureEpicSource`, `InheritedRnfLine` from story-0077-0024
- Application: `CreateStoriesFromFeatureUseCase`, `CreateStoriesFromFeatureResult`
- Adapter inbound: `XStoryCreateCommand` (adds `--from-feature` and `--epic-id` flags)
- Adapter outbound: `StoryFromFeatureArtifactWriter`

## 4. AC Coverage

| AC | Result |
|---|---|
| --from-feature populates Source Feature section in stories | PASS |
| --epic-id links generated stories to specified epic | PASS |
| Sections 2/4/8 absent from generated stories | PASS |
| Stories use v2 template format | PASS |
