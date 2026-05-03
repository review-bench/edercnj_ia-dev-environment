# Retro-Seed Quality Rubric

> Used by `scripts/retro-seed-memory.sh` and `RetroSeedSmokeIT` to spot-check
> summaries produced during story-0075-0006 (EPIC-0075 AI Memory Layer).

## Rubric Criteria

### C1 — Frontmatter Completeness

Every `ai/memory/epic-XXXX-summary.md` MUST contain YAML frontmatter with ALL
required fields:

| Field | Required | Type |
| :--- | :--- | :--- |
| `epic-id` | yes | String, format `EPIC-XXXX` |
| `slug` | yes | String, kebab-case |
| `summary-version` | yes | String, e.g. `"1.0"` |
| `created` | yes | String, ISO date `"YYYY-MM-DD"` |
| `last-updated` | yes | String, ISO date `"YYYY-MM-DD"` |
| `indexable` | yes | Boolean |
| `archived` | yes | Boolean |
| `superseded-by` | yes | String or `null` |
| `tags` | yes | List (may be empty `[]`) |
| `capabilities-affected` | yes | List (may be empty `[]`) |
| `rules-affected` | yes | List (may be empty `[]`) |
| `adrs-referenced` | yes | List (may be empty `[]`) |
| `patterns-introduced` | yes | List (may be empty `[]`) |
| `antipatterns-rejected` | yes | List (may be empty `[]`) |
| `dependencies-of` | yes | List (may be empty `[]`) |
| `dependencies-for` | yes | List (may be empty `[]`) |

**Check:** `RetroSeedSmokeIT.validateFrontmatter()` parses YAML frontmatter and
asserts all fields present.

---

### C2 — Section Coverage

Every summary MUST contain these 7 sections (exact H2 heading text):

1. `## Why this epic existed`
2. `## Hypothesis tested`
3. `## Decisions taken (with why)`
4. `## Alternatives rejected (with why)`
5. `## Reusable patterns produced`
6. `## Anti-patterns observed`
7. `## Links`

**Check:** `RetroSeedSmokeIT.validateSections()` greps for each heading.

---

### C3 — Line Cap

Every summary MUST be ≤ 200 lines (frontmatter included). Summaries exceeding
this cap burden context windows.

**Check:** `RetroSeedSmokeIT.validateLineCap()` counts lines via `wc -l`.

---

### C4 — Index Consistency

Every `ai/memory/epic-XXXX-summary.md` on disk MUST have a corresponding entry
in `ai/memory/_index.yaml` with matching `summary-path` value.

Conversely, every entry in `_index.yaml` MUST correspond to an existing file on disk.

**Check:** `RetroSeedSmokeIT.validateIndexConsistency()` cross-references.

---

### C5 — Hypothesis Tested Non-Empty

The `## Hypothesis tested` section MUST contain at least one sentence beyond
the header line. A summary with an empty hypothesis section provides no value.

**Check:** `RetroSeedSmokeIT.validateHypothesisNonEmpty()`.

---

### C6 — Superseded Consistency

If `superseded-by` is non-null, the referenced epic ID MUST also have a summary
on disk. Dangling `superseded-by` references are flagged as inconsistencies.

**Check:** `RetroSeedSmokeIT.validateSupersededConsistency()`.

---

## Spot-Check Procedure

Run against the full memory directory:

```bash
mvn test -Dtest=RetroSeedSmokeIT -pl java
```

Or dry-run the seed script:

```bash
scripts/retro-seed-memory.sh --dry-run
```

Self-check the script:

```bash
scripts/retro-seed-memory.sh --self-check
```
