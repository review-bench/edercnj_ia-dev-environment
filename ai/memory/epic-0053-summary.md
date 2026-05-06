---
epic-id: EPIC-0053
slug: mandatory-reviews-enforcement
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [governance, reviews, enforcement, skill-doc, audit]
capabilities-affected: []
rules-affected: [Rule 24]
adrs-referenced: []

patterns-introduced:
  - mandatory-tool-call-non-negotiable-marker
  - protocol-violation-named-exit-code
antipatterns-rejected:
  - optional-sounding-review-language-in-skill-doc

dependencies-of: [EPIC-0049]
dependencies-for: [EPIC-0057]
---
# Memory: EPIC-0053 — Enforcement de Reviews Obrigatórias

## Why this epic existed

Subagents executing `x-story-implement` silently omitted Step 3.4 (Specialist Review) and Step 3.6 (Tech Lead Review) even without `--skip-verification` being passed. Evidence from EPIC-0042: `reviewsExecuted: {specialist: false, techLead: false}`. Root cause: the skill text used non-mandatory language — the steps did not explicitly state reviews were non-negotiable by default. Subagents interpreted the ambiguity as permission to skip.

## Hypothesis tested

Adding explicit `MANDATORY — NON-NEGOTIABLE` language to `x-story-implement/SKILL.md` at Steps 3.4 and 3.6, introducing the named error code `PROTOCOL_VIOLATION` for tracking violations, and adding a golden-file test to validate the markers' presence in regenerated output would prevent silent skipping. **Confirmed**: markers added; `PROTOCOL_VIOLATION` defined; golden diff validated; `--skip-review` documented as RESERVED (not yet functional). This was a documentation-only change — no production Java modified.

## Decisions taken (with why)

1. **`MANDATORY — NON-NEGOTIABLE` marker language** — explicit phrasing removes ambiguity; LLMs following the skill cannot reasonably interpret it as optional.
2. **`PROTOCOL_VIOLATION` named exit code** — provides a searchable, trackable exit code for tooling and audit; avoids raw integers.
3. **Documentation-only change** — no production code altered; CI risk is minimal; golden file validates marker presence.

## Alternatives rejected (with why)

- **Runtime flag check in the orchestrator body** — would require Java changes (Rule 14 scope); documentation change achieves the same outcome for the observed failure mode.

## Reusable patterns produced

- **`mandatory-tool-call-non-negotiable-marker`**: explicit `MANDATORY — NON-NEGOTIABLE` language on any step that must not be skipped; later formalized as Rule 24 and Rule 28 grammar markers.
- **`protocol-violation-named-exit-code`**: `PROTOCOL_VIOLATION` as a canonical named error code for bypassed lifecycle gates.

## Anti-patterns observed

- **Optional-sounding review language** — "run the review" without mandatory qualifier allows subagents to skip silently; always use unambiguous obligation language.

## Links

- Epic: `ai/epics/epic-0053-mandatory-reviews-enforcement/epic-0053.md`
- ADRs: (none recorded)
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0053-mandatory-reviews-enforcement/reports/`
