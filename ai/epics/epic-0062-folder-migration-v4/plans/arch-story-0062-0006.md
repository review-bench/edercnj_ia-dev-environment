---
generated-by: x-internal-story-build-plan@5a632bb46be847bb0fe7b2cba291a8c22f7050cf
story-id: story-0062-0006
epic-id: EPIC-0062
---

# Architecture Plan — story-0062-0006

## Scope

Remove all literal numeric `plans/epic-[0-9]+` references from 14 SKILL.md files,
replacing them with generic `plans/epic-XXXX/` placeholders.

## Approach

Pure text substitution across SKILL.md bodies and associated README/references files.
No architectural changes to the codebase. No new abstractions introduced.

## Files Affected

- 14 SKILL.md files under `java/src/main/resources/targets/claude/skills/`
- Associated README.md and references/full-protocol.md files
- `governance/baselines/skill-pathresolver-baseline.txt` (cleared)

## Risk

Low. Text-only changes to documentation/skill-body files.
