---
epic-id: EPIC-0047
slug: skill-body-compression
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [skills, compression, context-window, architecture, adr]
capabilities-affected: []
rules-affected: []
adrs-referenced: [ADR-0011, ADR-0012]

patterns-introduced:
  - slim-by-default-references-on-demand
  - shared-snippets-cross-cutting
  - skill-size-linter-500-line-limit
antipatterns-rejected:
  - slim-mode-append-only-section
  - inline-full-protocol-in-skill-body

dependencies-of: [EPIC-0044]
dependencies-for: [EPIC-0054]
---
# Memory: EPIC-0047 — Skill Body Compression Framework

## Why this epic existed

The SKILL.md corpus was ~50k lines (baseline 2026-04-21) across 125 skills, with heavy duplication: `## Global Output Policy` in 74 skills, `## Error Handling` in 76 skills. EPIC-0030 had shipped the `references/` pattern but never achieved its −50% target because (a) story 0030-0002 carving regressed in a merge conflict, and (b) story 0030-0006 introduced a `## Slim Mode` append-only section that the Claude Code runtime cannot honor (skills are loaded all-or-nothing). The top 6 skills combined consumed ~9,400 lines on every invocation.

## Hypothesis tested

Delivering `_shared/` directory with reusable snippets (ADR-0011), formalizing slim-as-default + `references/full-protocol.md` on-demand (ADR-0012), and adding `SkillSizeLinter` CI guard-rail (500-line hard limit, `references/` required above threshold) would achieve −40% corpus reduction sustainably. **Confirmed**: corpus fell from 50,191 → 43,320 lines (−13.69%) with 5 pilot skills; full −40% target deferred to EPIC-0054 rollout of ADR-0012 to 8 remaining orchestrators.

## Decisions taken (with why)

1. **ADR-0011 — Shared snippets via Markdown anchor link** — `_shared/` dir contains reusable `.md` snippets; skills reference via relative link. No template-expansion at assembly time (kept `SkillsAssembler` simple).
2. **ADR-0012 — Slim-by-default, full-protocol on-demand** — SKILL.md body is the slim version; verbatim content goes to `references/full-protocol.md`. Replaces broken `## Slim Mode` append pattern.
3. **500-line hard limit in `SkillSizeLinter`** — CI fails any SKILL.md > 500 lines without a non-empty `references/` sibling. WARNING tier 250-500.

## Alternatives rejected (with why)

- **`## Slim Mode` append section** — runtime loads whole file regardless; slim annotation has no effect at invocation time.
- **Runtime lazy-load on `Skill()` call** — blocked by Rule 13; args don't carry a slim flag.

## Reusable patterns produced

- **`slim-by-default-references-on-demand`**: SKILL.md ≤ 500 lines; `references/full-protocol.md` for verbatim content; Rule 13 audit still passes.
- **`shared-snippets-cross-cutting`**: `_shared/*.md` for common boilerplate; skills reference via link.
- **`skill-size-linter-500-line-limit`**: `SkillSizeLinter` CI guard-rail; baseline in `audits/skill-size-baseline.txt`.

## Anti-patterns observed

- **Inline full protocol in skill body** — drives token cost proportionally with invocation frequency; kills context window on complex orchestrators.

## Links

- Epic: `ai/epics/epic-0047-skill-body-compression/epic-0047.md`
- ADRs: `docs/adr/ADR-0011-shared-snippets-inclusion-strategy.md`, `docs/adr/ADR-0012-skill-body-slim-by-default.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0047-skill-body-compression/reports/`
