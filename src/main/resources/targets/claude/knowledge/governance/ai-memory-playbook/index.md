---
name: ai-memory-playbook
description: Playbook for producing and maintaining ai/memory/ epic summaries
requires-capabilities: [governance.ai-memory]
---

# AI Memory Playbook

> Referenced by: Rule 33, `x-internal-epic-summary`, `x-memory-search`

## When to Create a Memory Entry

Create `ai/memory/epic-XXXX-summary.md` when:

- An epic reaches `Status: Concluída` AND `x-epic-implement` Phase 5 completes.
- Retroactively: for completed epics that predate EPIC-0075 (use story-0075-0006 pattern).

**Do NOT create** entries for:
- In-progress epics (partial decisions may change).
- Pure tooling/infra changes with no architectural decisions (still create, but brief).
- Stories (granularity is per-epic, not per-story).

## Tagging Guide

Use flat, lowercase, hyphenated tags in the frontmatter `tags:` field.

| Tag | Use when |
|-----|----------|
| `governance` | Epic introduced a rule, gate, or lifecycle contract |
| `capabilities` | Epic introduced or refactored capabilities |
| `testing` | Epic improved test strategy |
| `security` | Epic addressed a security concern |
| `performance` | Epic introduced performance gates |
| `documentation` | Epic improved docs as DoD |
| `refactor` | Epic was purely structural refactoring |
| `templates` | Epic introduced or modified planning templates |
| `skills` | Epic added or restructured skills |
| `memory` | Epic touched ai/memory/ itself |
| `breaking` | Epic introduced breaking changes |

Multiple tags are encouraged. Use the most specific applicable tags.

## How to Tag Capabilities and Rules

```yaml
capabilities-affected: [governance.ai-memory, governance.refinement-gate]
rules-affected: [Rule 22, Rule 24, Rule 33]
```

List all capabilities that the epic ADDED, MODIFIED, or EXTENDED. List all rules that the epic INTRODUCED or EXTENDED.

## Patterns and Anti-Patterns

```yaml
patterns-introduced:
  - capability-aware-skill-via-frontmatter
  - dual-write-state-and-markdown
antipatterns-rejected:
  - inline-validation-instead-of-hook
  - single-skill-with-switch-giant
```

Use short, kebab-case identifiers. These are used by `/x-memory-search --by-pattern`.

## Body Sections (Required)

Every summary MUST include all 7 sections from the template:

1. **Why this epic existed** — 1-2 paragraphs max
2. **Hypothesis tested** — stated hypothesis + actual result
3. **Decisions taken** — list: decision → why → consequence
4. **Alternatives rejected** — list: alternative → rejection reason
5. **Reusable patterns** — patterns usable in future epics
6. **Anti-patterns observed** — things to avoid going forward
7. **Links** — epic doc, ADRs, PRs, reports

## The 200-Line Cap

`x-internal-epic-summary` aborts if the output exceeds 200 lines. This is intentional:

- Forces compactness. If you need more space, you're probably writing prose, not retrieval-optimized memory.
- The detailed narrative lives in the epic document. The summary links to it.

If a summary genuinely needs more than 200 lines, split it into multiple linked entries or archive the older "Decisions taken" items.

## What NOT to Include

- **Secrets, tokens, API keys.** Never. The summary is committed to a public git history.
- **PII.** No names, emails, or personal identifiers — use roles ("the tech lead", "the operator").
- **Step-by-step implementation details.** That's in the story completion reports. Link to them.
- **Debugging session transcripts.** Not useful for retrieval.
- **Provisional ideas that were dropped early.** Only decisions that survived.

## Manual Archiving

To exclude an entry from `/x-memory-search` results without deleting the file:

```yaml
# in ai/memory/_index.yaml
- epic-id: EPIC-0040
  indexable: false   # excluded from search
  archived: true     # flagged as archived
```

The summary file is preserved. This is useful for decisions that are fully superseded but may still be historically interesting.

## Updating a Summary

Summaries are generated once, not continuously updated. If an epic is significantly revisited by a successor epic:

1. Update the original summary's `superseded-by: EPIC-YYYY` in frontmatter.
2. The successor epic's summary references the original as "Extended by: EPIC-XXXX".
3. `/x-memory-search` will surface both; the caller can filter by `superseded-by: null` for "current" decisions.
