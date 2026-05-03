---
epic-id: EPIC-0054
slug: adr-0012-slim-orchestrators-rollout
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [skills, compression, orchestrators, adr-rollout, context-window]
capabilities-affected: []
rules-affected: []
adrs-referenced: [ADR-0011, ADR-0012]

patterns-introduced:
  - adr-0012-applied-to-all-orchestrators
  - baseline-entry-removal-on-slim-completion
antipatterns-rejected:
  - 500-plus-line-orchestrator-skill-bodies

dependencies-of: [EPIC-0047]
dependencies-for: [EPIC-0061]
---
# Memory: EPIC-0054 — ADR-0012 Rollout aos 8 Orchestrators Restantes

## Why this epic existed

EPIC-0047 established the ADR-0012 (slim-by-default) framework and applied it to 5 pilot skills, bringing the corpus from 50,191 → 43,320 lines (−13.69%), but the −40% target was not reached. The gap was concentrated in 8 orchestrator skills still > 500 lines: `x-release` (2,811), `x-epic-implement` (2,377), `x-pr-fix-epic` (1,296), `x-story-plan` (1,199), `x-pr-merge-train` (873), `x-task-implement` (821), `x-security-pipeline` (576), `x-git-worktree` (568). These 8 combined were 10,521 lines.

## Hypothesis tested

Deliberately applying ADR-0012 to all 8 remaining orchestrators — rewriting each SKILL.md body to the 5-section canonical contract (`## Triggers`, `## Parameters`, `## Output Contract`, `## Error Envelope`, `## Full Protocol`) with verbose content carved to `references/full-protocol.md` — would achieve a net −6,121 lines reduction in the hot-path and bring the corpus to ~37,200 lines. **Confirmed**: all 8 orchestrators slim-rewritten; corpus reduction achieved; `audits/skill-size-baseline.txt` updated with 8 entries removed; `SkillSizeLinter` passes with no orchestrator in ERROR tier.

## Decisions taken (with why)

1. **No new Java code** — this epic is markdown-edits + golden regeneration only; Rule 14 compliance; no scope creep.
2. **Remove baseline entries for successfully slimmed orchestrators** — baseline only lists skills with legitimate exemptions; removing entries after slim completion keeps the baseline honest.
3. **`references/full-protocol.md` per orchestrator** — each slim orchestrator gets its own `full-protocol.md`; no shared references file to avoid coupling.

## Alternatives rejected (with why)

- **Redesign ADR-0012** — contract is consolidated and working; this epic applies it, not redesigns it.
- **Inline the 15 knowledge packs > 500 lines** — those are handled differently via EPIC-0051 (relocation, not compression).

## Reusable patterns produced

- **`adr-0012-applied-to-all-orchestrators`**: all 8 canonical orchestrators follow slim-by-default; `references/full-protocol.md` carries verbatim content; body ≤ 500 lines.
- **`baseline-entry-removal-on-slim-completion`**: when a skill successfully reaches ≤ 500 lines, remove its baseline entry; baseline only lists legitimate ongoing exemptions.

## Anti-patterns observed

- **500+ line orchestrator skill bodies** — token cost scales with invocation frequency; top orchestrators being invoked multiple times per epic means context pressure compounds quickly.

## Links

- Epic: `ai/epics/epic-0054-adr-0012-slim-orchestrators-rollout/epic-0054.md`
- ADRs: `docs/adr/ADR-0012-skill-body-slim-by-default.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0054-adr-0012-slim-orchestrators-rollout/reports/`
