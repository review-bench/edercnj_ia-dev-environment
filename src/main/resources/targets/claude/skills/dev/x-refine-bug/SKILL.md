---
requires-capabilities: [governance.bug-lifecycle]
skill-version: "1.0"
user-invocable: true
visibility: public
description: "Refine a bug report and set Refinement Verdict approved/rejected"
---

# Skill: x-refine-bug

## Purpose

Refines a bug report by validating completeness of all 9 RA9 sections, enforcing
acceptance criteria quality (Gherkin format), verifying reproduction recipe, assigning
severity and scope classifications, and setting the Refinement Verdict to `approved` or
`rejected` with structured blocker feedback.

## Triggers

- `/x-refine-bug bug-000001` — refine bug by ID
- `/x-refine-bug ai/bugs/bug-000001/bug.md` — refine bug by file path

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `bug-id` or `path` | Yes | Bug ID (`bug-NNNNNN`) or path to `bug.md` |
| `--non-interactive` | No | Skip interactive gate; emit JSON result only |

## Workflow

```
Phase 0   VALIDATE        -> Parse and validate bug-id format
Phase 1   LOAD            -> Read bug.md; verify 9 RA9 sections present
Phase 2   ASSESS          -> Check each section for completeness
Phase 3   GATE            -> Compute verdict (approved / rejected)
Phase 4   WRITE           -> Update Refinement Verdict block in bug.md
Phase 5   EMIT            -> Output JSON result
```

### Phase 0 — Validate

Validate bug-id format:

```bash
if [[ ! "$BUG_ID" =~ ^bug-[0-9]{6}$ ]]; then
  echo "ABORT [ARGS_INVALID]: Bug ID must match bug-NNNNNN"
  exit 2
fi
```

Locate bug file at `ai/bugs/${BUG_ID}/bug.md`. If absent, exit 3 (`BUG_NOT_FOUND`).

### Phase 1 — Load

Read `bug.md`. Verify all 9 mandatory sections are present:

1. `## 1. Visão (Vision)`
2. `## 2. Persona & Cenário de Uso`
3. `## 3. Entrega de Valor`
4. `## 4. Critérios de Aceite`
5. `## 5. Reproduction Recipe`
6. `## 6. Root-Cause Hypothesis`
7. `## 7. Regression Test Slot`
8. `## 8. Dependências`
9. `## 9. Histórico de Decisão`

Missing sections are collected as blockers.

### Phase 2 — Assess

For each section, validate content quality:

**Section 1 — Visão:** Must have ≥ 1 non-empty line after header.

**Section 4 — Critérios de Aceite:** Must contain at least one `Dado`/`Given` + `Quando`/`When`
+ `Então`/`Then` (Gherkin triplet). Missing triplet → blocker `AC_MISSING_GHERKIN`.

**Section 5 — Reproduction Recipe:** Must contain:
- `### 5.1 Environment` (or `5.1`) — environment spec
- `### 5.2 Steps to Reproduce` — numbered steps (at least 2)
- `### 5.3 Observed vs Expected` — both `Observed:` and `Expected:` fields
- `### 5.4 Artifacts` — log/screenshot reference

Missing sub-sections → blockers `RECIPE_MISSING_ENV`, `RECIPE_MISSING_STEPS`,
`RECIPE_MISSING_OBSERVED_EXPECTED`, `RECIPE_MISSING_ARTIFACTS`.

**Section 6 — Root-Cause Hypothesis:** Must contain hypothesis text (≥ 10 chars).

**Section 7 — Regression Test Slot:** Must reference a test file path or `TBD`.

**Severity + Scope:** The `**Status:** Pendente` line must exist. Severity and scope are
read from frontmatter (`severity`, `scope` fields) or body. If missing, warn but do not
block (they may be set during investigation).

### Phase 3 — Gate

If `blockers` is empty → verdict = `approved`.
If any blockers exist → verdict = `rejected`.

Compute `verdictHash`:

```bash
VERDICT_HASH=$(echo "${BUG_ID}:${verdict}:$(date -u +%Y-%m-%dT%H:%M:%SZ)" | sha256sum | cut -c1-64)
```

### Phase 4 — Write

Update the `## Refinement Verdict` block in `bug.md`. The block format:

````markdown
## Refinement Verdict

```yaml
status: approved
verdictHash: "abc123..."
refinedAt: "2026-05-07T14:00:00Z"
blockers: []
```
````

When `rejected`:

````markdown
## Refinement Verdict

```yaml
status: rejected
verdictHash: "abc123..."
refinedAt: "2026-05-07T14:00:00Z"
blockers:
  - AC_MISSING_GHERKIN
  - RECIPE_MISSING_STEPS
```
````

Write atomically via temp file + rename (POSIX rename is atomic on same filesystem):

```bash
TMP=$(mktemp "${bug_file}.XXXXXX")
# ... build content ...
mv "$TMP" "$bug_file"
```

If `status` transitions to `approved`, also update `**Status:** Pendente` →
`**Status:** Refinada` in the body.

### Phase 5 — Emit

Emit JSON to stdout:

```json
{
  "bugId": "bug-000001",
  "verdict": "approved",
  "verdictHash": "abc123...",
  "refinedAt": "2026-05-07T14:00:00Z",
  "blockers": [],
  "statusTransition": "Pendente→Refinada"
}
```

When `rejected`, `statusTransition` is `null` (status unchanged).

## Exit Codes

| Code | Name | Condition |
|------|------|-----------|
| 0 | SUCCESS | Verdict written (approved or rejected) |
| 2 | ARGS_INVALID | Bug ID does not match `^bug-[0-9]{6}$` |
| 3 | BUG_NOT_FOUND | `ai/bugs/${BUG_ID}/bug.md` not found |
| 4 | BUG_FILE_NOT_FOUND | Bug file path argument does not exist |
| 5 | WRITE_FAILED | Could not write verdict to bug.md |

## Refinement Gate Contract

A bug whose Refinement Verdict is `rejected` or `pending` MUST NOT:
- Be decomposed (x-internal-decompose-bug exits with gate error)
- Progress to `Em Investigação` status
- Have an implementation story created for it

`x-create-bug` sets initial Refinement Verdict to `status: pending`. The gate is:

```
pending   → rejected|approved  (via x-refine-bug)
rejected  → approved           (re-run x-refine-bug after fixing blockers)
approved  → (locked — no further status changes to verdict)
```

Once `approved`, the verdict hash is immutable. Re-running `x-refine-bug` on an
already-approved bug exits 0 with `{"verdict":"approved","idempotent":true}`.

## Error Handling

| Scenario | Action |
|----------|--------|
| Bug ID invalid format | ABORT exit 2: "Bug ID must match bug-NNNNNN" |
| Bug directory absent | ABORT exit 3: "Bug not found: ai/bugs/<id>" |
| Bug file path not found | ABORT exit 4: "Bug file not found: <path>" |
| Atomic write fails | ABORT exit 5: "Write failed: <reason>" |
| Already approved | Exit 0: `{"verdict":"approved","idempotent":true}` |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-create-bug` | predecessor | Sets initial `status: pending` verdict |
| `x-internal-decompose-bug` | blocked by gate | Refuses to run if verdict ≠ approved |
| `x-internal-map-bug` | blocked by gate | Only runs after decomposition (after approval) |
