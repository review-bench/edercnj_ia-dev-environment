# AI Memory — Strategic Decision Archive

This directory contains compact, indexed summaries of completed epics, optimized for retrieval by both humans and LLM sessions.

## What is this?

`ai/memory/` is a **retrieval-optimized layer** complementing:
- `docs/adr/` — individual architectural decisions (formal, detailed)
- `ai/epics/*/` — full epic narrative (extensive, not retrieval-friendly)
- `docs/architecture/system.md` — current state snapshot

`ai/memory/` answers: *"Why did we decide X? What alternatives were rejected? What patterns came from EPIC-YYYY?"*

## Structure

```
ai/memory/
├── _index.yaml              # flat index of all entries (tags, indexable flag)
├── README.md                # this file
├── epic-0040-summary.md     # one summary per completed epic
├── epic-0041-summary.md
└── ...
```

## How to Search

Use the `/x-memory-search` skill:

```
/x-memory-search --by-tag governance
/x-memory-search --by-capability governance.refinement-gate
/x-memory-search --by-rule 24
/x-memory-search --by-pattern capability-aware-skill-via-frontmatter
/x-memory-search --by-epic EPIC-0069
```

Each result shows: epic slug + 1-3 sentence "why" + links.

## How Entries Are Created

`x-internal-epic-summary` is invoked **automatically** at Phase 5 of `x-epic-implement` (Rule 33). You do not create entries manually.

For retroactive entries (completed epics pre-dating EPIC-0075), run:

```
/x-internal-epic-summary <EPIC-ID>
```

## Manual Archiving

To exclude an entry from search without deleting:

```yaml
# _index.yaml
- epic-id: EPIC-0040
  indexable: false
  archived: true
```

File is preserved; future searches exclude it. See `knowledge/governance/ai-memory-playbook/` for full tagging and archiving guide.

## Constraints

- Each summary is ≤ 200 lines (enforced by generator).
- Summaries MUST NOT contain secrets, tokens, or PII.
- Do not edit summaries manually; regenerate via `x-internal-epic-summary`.
