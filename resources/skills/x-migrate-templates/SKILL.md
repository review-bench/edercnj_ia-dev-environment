---
name: x-migrate-templates
description: "Migrates a v1 epic to v2 value-driven template (EPIC-0070), with optional --interactive."
user-invocable: true
model: sonnet
allowed-tools: Read, Edit, Write, Skill, AskUserQuestion, Bash
argument-hint: "<epic-id>"
requires-capabilities: [governance.value-driven-templates]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: x-migrate-templates

## Purpose

Assists the migration of a v1 epic document (`ai/epics/<epic-id>/epic-<epic-id>.md` or `ai/epics/<epic-id>-*/epic-<epic-id>.md`) to the v2 value-driven template introduced in EPIC-0070. The migration is **information-preserving** — no technical decision from v1 is silently discarded.

## Triggers

- `/x-migrate-templates <epic-id>` — migrate the epic to v2 (non-interactive by default — applies safe heuristics automatically)
- `/x-migrate-templates <epic-id> --interactive` — migrate with per-block confirmation prompts
- `/x-migrate-templates <epic-id> --dry-run` — preview diff and classification without writing

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `EPIC-ID` | String | Yes | — | 4-digit zero-padded epic identifier (e.g., `0050`). Also accepts full form `epic-0050`. |
| `--interactive` | Boolean | No | false | Show per-block confirmation prompts (`AskUserQuestion`). Default is non-interactive (Rule 20 — EPIC-0061). |
| `--dry-run` | Boolean | No | false | Show diff + simulated answers (default: "manter") without writing any files. |
| `--resume` | Boolean | No | false | Resume from a previous interrupted session state-file at `.claude/state/template-migrate-<epic-id>.json`. |

## Workflow

### Step 1 — Validate & Detect

1. Resolve `EPIC-ID` from argv (strip `epic-` prefix if present, zero-pad to 4 digits).
2. Locate epic folder via glob: `ai/epics/epic-<EPIC-ID>*/`. If not found, abort with `EPIC_DIR_MISSING`.
3. Read `ai/epics/epic-<EPIC-ID>*/epic-<EPIC-ID>.md`.
4. **Detect template version:**
   - If document contains `## 3. Hipótese & OKRs` or `## Refinement Verdict` block → already v2. Exit 0 with: `INFO: epic already in v2 — nothing to do.`
   - Otherwise → treat as v1.
5. If `--resume` flag is set, load state from `.claude/state/template-migrate-<EPIC-ID>.json`:
   - Verify `originalHash` matches SHA-256 of current epic.md content. Mismatch → abort with `EXTERNAL_EDIT_DETECTED`.
   - Restore `decisions[]` (blocks already processed), set resume point.

### Step 2 — Parse v1 Blocks

Parse the v1 epic markdown and extract the following technical block types:

| v1 Block Pattern | Detected By |
|------------------|-------------|
| Packages / Hexagonal | `## Packages`, `## Arquitetura`, section containing package paths |
| Contratos / Endpoints | `## Contratos`, `## API`, `## Endpoints` |
| SOLID principles | `## SOLID`, `## Princípios`, section referencing SRP/OCP/LSP/ISP/DIP |
| Observabilidade | `## Observabilidade`, `## Métricas`, `## Logs` |
| Segurança técnica | `## Segurança`, `## Security`, `## PCI` |
| Decisões inline | Any section containing `**Decisão:**`, `**ADR:**`, `decisão arquitetural` |
| Outros blocos | Any remaining section not mapped to v2 structure |

If a section fails to parse (e.g., malformed markdown, unclosed code fence), abort with:
```
ERROR PARSER_ERROR: section "<heading>" at line <N> could not be parsed.
Epic.md NOT modified — state preserved.
```

### Step 3 — Classify Blocks (Auto-Heuristics)

For each extracted block, apply classification heuristics:

| Block Type | Default Classification | Reasoning |
|------------|----------------------|-----------|
| Packages / Hexagonal | `move-to-system-md` → §1-3 (Stack/Persistência/Comunicação) | Architectural topology belongs in system.md |
| Contratos / Endpoints | `move-to-system-md` → §3 (Integrações) | Interface contracts persist beyond a single epic |
| SOLID | `discard` | Governed by Rule 03/04 — always applicable, no per-epic value |
| Observabilidade | `move-to-system-md` → §4 (Decisões de Observabilidade) | SLO/alert decisions are cross-epic |
| Segurança técnica | `move-to-system-md` → §2 (Segurança) or `create-adr` | Security decisions warrant ADR when novel |
| Decisões inline | `create-adr` | Inline decisions are best canonicalized as formal ADRs |
| Outros blocos | `keep-in-epic` | Unknown content defaults to preservation |

Classification result for each block includes: `{ type, content, classification, targetSection, rationale }`.

### Step 4 — Interactive Confirmation (Conditional on `--interactive`)

When `--interactive` is active, for each classified block:

    AskUserQuestion(question: "Block: <heading>\nContent preview: <first 3 lines>\nProposed: <classification> → <targetSection>\n[1] Accept  [2] Override: move-to-system-md  [3] Override: create-adr  [4] Override: keep-in-epic  [5] Discard")

Persist each confirmed decision to the state-file immediately after confirmation.

When **not** `--interactive` (default): apply heuristics without prompting. Print a summary table of classifications at the end.

### Step 5 — Atomic Write of Epic v2

1. Compose the v2 epic document by mapping preserved/classified blocks into the v2 9-section structure:
   - Section 1: Visão (from original epic title/problem statement)
   - Section 2: Persona & Stakeholders (from original persona)
   - Section 3: Hipótese & OKRs (from original OKRs or synthesized from value statement)
   - Section 4: Alternativas Consideradas (from original alternatives)
   - Section 5: Escopo (preserved scope section)
   - Section 6: Riscos (from original risks)
   - Section 7: Índice de Histórias (from story list)
   - Section 8: Quality Gates (from v1 quality criteria)
   - Refinement Verdict: `Status: tbd` (placeholder — run `/x-refine-epic` to refine)
2. Write to temp path `epic-<EPIC-ID>.md.tmp`.
3. Move temp to final path (atomic replace).
4. Write migration log to `ai/epics/epic-<EPIC-ID>*/reports/migration-log-epic-<EPIC-ID>.md`.

If `--dry-run`: print the composed v2 document as a diff preview. Skip all writes. Exit 0.

### Step 6 — Side-Effects (Conditional)

Execute side-effects for blocks classified as `move-to-system-md` or `create-adr`:

**`move-to-system-md` blocks:**

    Skill(skill: "x-update-system-architecture", model: "sonnet", args: "<EPIC-ID>")  [conditional: not flag.dry_run]

**`create-adr` blocks:** Write a new ADR file at `docs/adr/ADR-NNNN-<slug>.md` (next available ADR number) with the extracted decision content. ADR number is determined by scanning existing `docs/adr/ADR-*.md` files.

If any side-effect fails, log a WARNING but **do not roll back** the already-written epic v2 (atomicity scoped to epic.md only; side-effects are idempotent and re-runnable).

### Step 7 — Session Cleanup

Remove `.claude/state/template-migrate-<EPIC-ID>.json` on successful completion (session no longer needed). On failure, preserve state-file for `--resume`.

## Recovery Contract

State-file schema at `.claude/state/template-migrate-<EPIC-ID>.json`:

```json
{
  "epicId": "epic-<EPIC-ID>",
  "originalHash": "<sha256-of-v1-epic-md>",
  "startedAt": "<ISO-8601>",
  "decisions": [
    { "block": "<heading>", "classification": "<type>", "targetSection": "<opt>", "confirmedAt": "<ISO-8601>" }
  ],
  "resumePoint": "<heading-of-next-unprocessed-block>"
}
```

Re-invocation with `--resume` loads this state, verifies `originalHash`, and continues from `resumePoint`.

## Error Codes

| Code | Condition |
|------|-----------|
| `EPIC_DIR_MISSING` | `ai/epics/epic-<ID>*/` not found |
| `PARSER_ERROR` | v1 section is malformed; file not written |
| `EXTERNAL_EDIT_DETECTED` | epic.md changed since session started (hash mismatch) |

## Examples

```bash
# Default non-interactive migration (apply heuristics, no prompts)
/x-migrate-templates 0050

# Interactive migration with per-block confirmation
/x-migrate-templates 0050 --interactive

# Dry-run — preview diff without writing
/x-migrate-templates 0050 --dry-run

# Resume an interrupted session
/x-migrate-templates 0050 --resume

# Full epic-id form also accepted
/x-migrate-templates epic-0036 --interactive
```

## Integration Notes

- Invokes `x-update-system-architecture` (story-0070-0006) for `move-to-system-md` side-effects — Rule 13 INLINE-SKILL.
- State-file mirrors `pr-watch-*.json` pattern (Rule 45) for session persistence.
- Rule 45 (CI-Watch): **not applicable** — this skill does not create PRs.
- Rule 22 (Skill Visibility): **public** — user-invocable, appears in `/help`.
- After migration, run `/x-refine-epic epic-<ID>` to obtain `refinementVerdict.status = approved` for the migrated epic.
