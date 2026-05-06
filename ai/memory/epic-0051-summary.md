---
epic-id: EPIC-0051
slug: knowledge-packs-dedicated-dir
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [generator, knowledge-packs, architecture, assembler, refactor]
capabilities-affected: []
rules-affected: []
adrs-referenced: []

patterns-introduced:
  - knowledge-assembler-mirrors-rules-assembler
  - knowledge-packs-in-dedicated-dir
antipatterns-rejected:
  - knowledge-packs-as-fake-skills-in-skills-dir

dependencies-of: [EPIC-0048]
dependencies-for: [EPIC-0054, EPIC-0061]
---
# Memory: EPIC-0051 — Knowledge Packs fora de `.claude/skills/`

## Why this epic existed

Knowledge Packs (KPs) lived in `.claude/skills/{name}/SKILL.md` alongside invocable skills. This treated KPs as "fake skills": they were only there because Claude Code scans `.claude/skills/**/SKILL.md`; they are not invocable, have no useful triggers, and violated the minimum skill contract (e.g., `patterns/SKILL.md` had no frontmatter at all). The result: ~32 "skill" folders that are not skills, fragile validation (each assembler needed to handle the special case), and documentation confusion. Consumer skills in `core/**/SKILL.md` referenced KPs via `Read("skills/{name}/SKILL.md")` paths.

## Hypothesis tested

Moving all KPs to `.claude/knowledge/` (a dedicated sibling of `skills/` and `rules/`) with a new `KnowledgeAssembler.java` mirroring `RulesAssembler` would eliminate the semantic noise, fix consumer references, and make validation straightforward. **Confirmed**: `KnowledgeAssembler` delivered; 30+ KPs relocated to `targets/claude/knowledge/`; ~30 consumer skills updated to reference `knowledge/{name}.md`; 7 rules with "Full reference" citations updated; golden files regenerated for all 9 Java profiles.

## Decisions taken (with why)

1. **Big-bang migration within this epic** — no compatibility shim for old paths; Rule 19 only applies to `flowVersion` discriminator and execution-state, not to internal assembly paths. Users regenerate via `ia-dev-env generate`.
2. **`KnowledgeAssembler` mirrors `RulesAssembler`** — same structural pattern; reuses proven design; easy to audit.
3. **KP files are plain `.md` without skill frontmatter** — KPs are not invocable; frontmatter requirement removed; `KnowledgeAssembler` does not check for `name:` frontmatter field.

## Alternatives rejected (with why)

- **MCP resource server for KPs** — overengineering; KPs are read-only context, not a service.
- **Compatibility path alias from `skills/` to `knowledge/`** — adds maintenance complexity; all known consumers are in this repo and can be updated atomically.

## Reusable patterns produced

- **`knowledge-assembler-mirrors-rules-assembler`**: `KnowledgeAssembler` follows the same structural pattern as `RulesAssembler`; consistent assembly pipeline.
- **`knowledge-packs-in-dedicated-dir`**: `.claude/knowledge/` is the canonical output for KPs; `skills/` contains only invocable skills.

## Anti-patterns observed

- **Knowledge packs as fake skills** — `skills/{kp}/SKILL.md` without invocation frontmatter confuses validation and documentation; ~32 folders that are not skills.

## Links

- Epic: `ai/epics/epic-0051-knowledge-packs-dedicated-dir/epic-0051.md`
- ADRs: (none recorded)
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0051-knowledge-packs-dedicated-dir/reports/`
