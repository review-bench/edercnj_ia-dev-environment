---
name: x-arch-system-update
description: "Incrementally updates docs/architecture/system.md after an epic completes. Appends new entries to the Decision Log (§11) using x-internal-report-write --append (dedup by ## ID: marker) and surgically inserts component/integration changes into sections 1-10 via Edit. Idempotent: re-running with the same epic produces a byte-identical system.md."
user-invocable: true
model: sonnet
allowed-tools: Read, Edit, Skill, Glob, Bash
argument-hint: "<epic-id>"
requires-capabilities: [governance.value-driven-templates]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: x-arch-system-update

## Purpose

Incrementally updates `docs/architecture/system.md` after an epic completes. Uses two complementary strategies to preserve manually-authored content:

1. **Decision Log (§11):** append-with-dedup via `x-internal-report-write --append` (dedup key: `## ID: <epic-id>` marker).
2. **Sections 1-10:** surgical `Edit` via `<!-- AUTO-FILL: <key> -->` HTML markers inserted by the template.

This skill is **never regenerative** — it never rewrites `system.md` from scratch.

## Triggers

- `/x-arch-system-update <epic-id>` — update `system.md` from the given epic's artifacts
- After Phase 4 (Integrity Gate) of `x-epic-implement` completes for a new epic

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `EPIC-ID` | String | Yes | — | 4-digit zero-padded epic identifier (e.g., `0099`). Also accepts full ID form `epic-0099`. |
| `--system-md-path` | String | No | `docs/architecture/system.md` | Override path to system architecture doc. |
| `--dry-run` | Boolean | No | false | Print the diff without writing to `system.md`. |

## Workflow

### Step 1 — Validate Preconditions

1. Resolve `EPIC-ID` from argv (strip `epic-` prefix if present, zero-pad to 4 digits).
2. Confirm `docs/architecture/system.md` exists (or `--system-md-path` value). If absent, abort with:
   ```
   ERROR SYSTEM_MD_MISSING: docs/architecture/system.md not found.
   Suggestion: run `ia-dev-env generate` first (story-0070-0004 SystemArchAssembler).
   ```
3. Locate epic folder via glob: `ai/epics/epic-<EPIC-ID>-*/`. If not found, abort with `EPIC_DIR_MISSING`.
4. Read `ai/epics/epic-<EPIC-ID>-*/epic-<EPIC-ID>.md` (the epic markdown). If absent, skip to Step 5 with empty architectural decisions.

### Step 2 — Extract Architectural Decisions from Epic

1. Scan the epic markdown for:
   - **ADR references:** `docs/adr/ADR-NNNN-*.md` links mentioned in the text.
   - **New components:** entries under any "## Components" or "## Stack" section.
   - **Integrations:** entries under "## Integrations" or similar.
2. For each ADR reference found, read the ADR file and extract:
   - ADR title (first `# ` heading).
   - Decision summary (content under `## Decision`).
3. If **no architectural decisions detected** (no ADR references, no new components/integrations):
   - Print: `INFO: no architectural decisions detected for EPIC-<EPIC-ID> — system.md unchanged.`
   - Exit 0 without modifying `system.md`.

### Step 3 — Build Decision Log Entry

Compose a Decision Log block for this epic:

```markdown
### EPIC-<EPIC-ID> — <Epic Title> (<YYYY-MM-DD>)

## ID: epic-<EPIC-ID>

- **Decisão:** <summary of architectural decisions from ADRs and epic scope>
- **ADR:** [ADR-NNNN](<relative path>) _(repeat per ADR)_
- **Impacto em system.md:** §<N> atualizada
```

The `## ID: epic-<EPIC-ID>` marker (or `## ID: epic-<EPIC-ID>-<adr-id>` when multiple decisions) enables idempotent append: `x-internal-report-write --append` skips insertion when the marker already exists in `system.md`.

### Step 4 — Append Decision Log

Invoke `x-internal-report-write` to append the Decision Log entry:

    Skill(skill: "x-internal-report-write", model: "haiku", args: "--template-inline \"<entry>\" --output docs/architecture/system.md --append --section \"## 11. Decision Log do Sistema\"")  [required]

If `x-internal-report-write` reports `DUPLICATE_SKIPPED` (marker already present), the step is a no-op — idempotency is preserved.

### Step 5 — Surgical Updates to Sections 1-10 (Conditional)

For each new component or integration detected in Step 2:

1. Locate the relevant section in `system.md` by searching for `<!-- AUTO-FILL: <key> -->` markers.
2. Compute the SHA-256 hash of the block to insert.
3. Search `system.md` for the block's content hash. If already present (identical content found after the marker), skip (no-op).
4. Otherwise, apply a surgical `Edit` inserting the new block immediately after the `<!-- AUTO-FILL: <key> -->` marker.

Content preservation rules (mirrors `x-arch-update` RULE-008):
- NEVER remove existing content from any section.
- NEVER rewrite a section — only insert or append.
- If a component was refactored: mark old as `deprecated`, add new entry.

### Step 6 — Idempotency Verification (Conditional on `--dry-run`)

When `--dry-run` is active:
1. Print the computed diff (sections and Decision Log entry to be appended).
2. Print `DRY RUN — no changes written.`
3. Exit 0.

When not `--dry-run`, after all writes: print `DONE — system.md updated for EPIC-<EPIC-ID>.`

## Idempotency Contract

Running `/x-arch-system-update <epic-id>` twice consecutively with the same epic produces a byte-identical `system.md`. The contract relies on two mechanisms:

| Strategy | Mechanism |
|----------|-----------|
| Decision Log (§11) | `x-internal-report-write --append` dedup by `## ID:` marker |
| Sections 1-10 | SHA-256 content hash check before each `Edit` |

## Error Codes

| Code | Condition |
|------|-----------|
| `SYSTEM_MD_MISSING` | `docs/architecture/system.md` not found |
| `EPIC_DIR_MISSING` | `ai/epics/epic-<ID>-*/` not found |

## Examples

```bash
# Default — update system.md from EPIC-0099
/x-arch-system-update 0099

# Full ID form also accepted
/x-arch-system-update epic-0099

# Dry-run — preview the diff without writing
/x-arch-system-update 0099 --dry-run

# Override path (non-standard repo layout)
/x-arch-system-update 0099 --system-md-path steering/system-arch.md
```

## Integration Notes

- Depends on `x-internal-report-write` (internal/ops) for Decision Log append-with-dedup.
- `x-doc-generate` (EPIC-0071) may invoke this skill as one of its doc targets.
- `_TEMPLATE-ARCHITECTURE-SYSTEM.md` (story-0070-0004) should include `<!-- AUTO-FILL: <key> -->` markers at appropriate section locations to enable Step 5 surgical inserts.
- Rule 45 (CI-Watch): **not applicable** — this skill does not create PRs.
- Rule 22 (Skill Visibility): **public** — user-invocable, appears in `/help`.
