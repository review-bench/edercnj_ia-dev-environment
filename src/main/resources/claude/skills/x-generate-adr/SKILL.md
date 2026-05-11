---
name: x-generate-adr
description: "Generates ADRs from architecture-plan mini-ADRs with sequential numbering and index update."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[architecture-plan-path] [story-id]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: ADR Automation (slim — ADR-0012)

## Purpose

Automates the generation of Architecture Decision Records (ADRs) from mini-ADRs embedded in architecture plans. Extracts inline decisions, expands them to full ADR format using a standard template, assigns sequential numbering, updates the ADR index, and inserts cross-references between stories, architecture plans, and ADRs.

## Triggers

- `/x-generate-adr [architecture-plan-path] [story-id]` — extract mini-ADRs from the specified architecture plan and generate full ADR files
- After the architecture plan phase, when mini-ADRs exist inline in the plan document
- When `x-plan-architecture` has produced an architecture plan containing `### ADR:` markers
- When architectural decisions need formal documentation as standalone ADR files
- When the `docs/adr/` directory needs to be populated or updated with new decisions
- Do NOT use if ADRs have already been manually created for the same decisions

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `architecture-plan-path` | String | Yes | Path to the architecture plan file (e.g., `plans/architecture-plan-STORY-0004-0006.md`) |
| `story-id` | String | Yes | Story identifier for cross-referencing (e.g., `story-0004-0006`) |

## Input/Output Formats

**Input (mini-ADR in architecture plan):**

```markdown
### ADR: [Title of the Decision]
- **Context:** ...
- **Decision:** ...
- **Rationale:** ...
```

Required mini-ADR fields: `title`, `context`, `decision`, `rationale`. Full field semantics in [`references/full-protocol.md`](references/full-protocol.md) §Input Format.

**Output (full ADR file):** `docs/adr/ADR-NNNN-title-in-kebab-case.md` with YAML frontmatter (`status: Accepted`, `date`, `story-ref`, `deciders: AI-assisted (via x-generate-adr)`) plus Status / Context / Decision / Consequences (Positive / Negative / Neutral) sections. Full template in [`references/full-protocol.md`](references/full-protocol.md) §Output Format.

## Output Contract

| Artifact | Path |
|----------|------|
| Generated ADRs | `docs/adr/ADR-NNNN-title-in-kebab-case.md` (one per non-duplicate mini-ADR) |
| Updated index | `docs/adr/README.md` (table appended with one row per new ADR) |
| Updated architecture plan | Original plan file modified with `> Generated: [ADR-NNNN](...)` callouts after each processed mini-ADR |
| Updated service-architecture doc (if present) | Section 7 enriched with new ADR cross-references |

## Workflow Overview

```text
1. PARSE_PLAN       -> Read architecture plan; locate `### ADR:` markers; extract 4 fields per mini-ADR
2. SCAN_EXISTING    -> Glob docs/adr/ADR-*.md; compute next sequential number
3. CHECK_DUPLICATES -> Normalize titles; warn + skip on match
4. EXPAND_ADR       -> Build full ADR (frontmatter + Status + Context + Decision + Consequences)
5. WRITE_FILES      -> Ensure docs/adr/; write each ADR with 4-digit padded number
6. UPDATE_INDEX     -> Append rows to docs/adr/README.md (create if missing)
7. CROSS_REFERENCE  -> Add `> Generated: [ADR-NNNN](...)` callouts back into the architecture plan; update service-architecture Section 7 if present
```

Detailed Step 1–7 procedures, sequential numbering algorithm, duplicate-detection rules, cross-reference contract (ADR↔Story, ADR↔Plan, Plan→ADR, service-arch Section 7), index-update merge rules, and 3 worked examples (mini-ADR input, full ADR output, index entry, plan cross-reference) live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1 Parse Architecture Plan): `grep -n "### ADR:"` extraction; per-marker field parsing.
- **Step 2** (§Step 2 Scan Existing ADRs): `ls docs/adr/ADR-*.md` with `sort -t'-' -k2 -n | tail -1`; empty-dir fallback to `ADR-0001`.
- **Step 3** (§Step 3 Check for Duplicates): normalize (lowercase + strip punctuation + trim + collapse spaces); skip + warn on match; preserve manually-curated ADRs.
- **Step 4** (§Step 4 Expand Mini-ADR): 4-digit padded numbering; kebab-case title→filename; full frontmatter + sections generation.
- **Step 5** (§Step 5 Write ADR Files): `mkdir -p docs/adr/`; per-file write with verification.
- **Step 6** (§Step 6 Update the Index): create `docs/adr/README.md` with table header on first run; append rows preserving existing.
- **Step 7** (§Step 7 Add Cross-References): inject `story-ref` in frontmatter; append `> Generated: [ADR-NNNN](...)` to plan; update service-arch Section 7 if present.
- **Sequential Numbering** (§Sequential Numbering): full Glob + max + increment algorithm.
- **Duplicate Detection** (§Duplicate Detection): normalization spec; skip-warn-continue semantics.
- **Cross-Reference Rules** (§Cross-Reference Rules): 4 bidirectional links (ADR↔Story, ADR↔Plan Context, Plan callouts, service-arch Section 7).
- **Index Update** (§Index Update): table-detection + append-only contract.
- **Examples** (§Examples): full PostgreSQL mini-ADR → ADR-0004 worked example with index entry and plan cross-reference.

## Error Handling

| Scenario | Action |
|----------|--------|
| Architecture plan file not found | Report error with path and exit |
| No `### ADR:` markers found | Report "No mini-ADRs found in plan" and exit cleanly |
| Duplicate ADR title detected | Emit warning, skip the duplicate, continue processing remaining |
| `docs/adr/` directory does not exist | Create it automatically |
| `docs/adr/README.md` does not exist | Create it with table header |
| Write failure | Report error with file path and reason |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-plan-architecture` | called-by | Prerequisite: generates the architecture plan with inline mini-ADRs |
| `x-update-architecture` | calls | Updates service architecture document Section 7 with new ADR references |
| `x-implement-story` | called-by | Invoked during documentation phase to formalize architectural decisions |

## Full Protocol

Minimum viable contract above. Detailed mini-ADR input/output formats, all 7 step procedures with bash examples, sequential numbering algorithm, duplicate-detection normalization spec, 4-link cross-reference contract, index-update append-only rules, and worked examples live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
