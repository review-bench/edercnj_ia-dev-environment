---
generated-by: x-internal-story-build-plan@5a632bb46be847bb0fe7b2cba291a8c22f7050cf
story-id: story-0062-0006
epic-id: EPIC-0062
---

# Implementation Plan — story-0062-0006

## Goal

Remove ALL literal numeric epic references `plans/epic-[0-9]+` from 14 SKILL.md files.

## AC

`grep -rE "plans/epic-[0-9]+" java/src/main/resources/targets/claude/skills/` → zero hits

## Tasks

1. For each SKILL.md in the 14 target skills: find and replace `plans/epic-NNNN/` with `plans/epic-XXXX/`
2. Fix associated README.md and references/full-protocol.md files in the same skill directories
3. Update `governance/baselines/skill-pathresolver-baseline.txt` — clear skill entries, add comment
4. Create Phase-1 planning artifacts (this file + 5 siblings)
5. Create Phase-3 evidence artifacts (verify-envelope, review, techlead-review, completion report, dependency-audit)

## Implementation Result

All 14 SKILL.md files updated. AC1 verified: zero hits.
Baseline cleared with comment `# All SKILLs migrated by EPIC-0062`.
