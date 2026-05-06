# Rule 30 — Tool-Call Grammar

> **Related:** Rule 13, Rule 24, Rule 25.
> **Introduced by:** EPIC-0063 (story-0063-0012). **ADR:** ADR-0016.
> **Full reference (BNF grammar, placement, examples, conditional whitelist, audit algorithms, baseline format):**
> `Read src/main/resources/targets/claude/knowledge/governance/tool-call-grammar.md`

## Scope (8 Anexo B Orchestrators)

`x-implement-epic`, `x-implement-story`, `x-implement-task`, `x-release`,
`x-orchestrate-epic`, `x-review-codebase`, `x-review-pr`, `x-manage-pr-merge-train`.

Leaf skills and `x-internal-*` skills are out of scope.

## Marker Kinds

| Marker | Meaning | Audit behavior |
| :--- | :--- | :--- |
| `[required]` | MUST execute. | Dynamic: `tool.call` event MUST exist in story NDJSON. |
| `[optional]` | MAY execute. | Dynamic: no cross-check (absent events acceptable). |
| `[conditional: <expr>]` | Executes when `<expr>` is true. | Dynamic: if `<expr>` true → treated as `[required]`; if false → skip allowed. |

## Exit Codes (`audit-tool-call-grammar.sh`)

| Exit | Code | Condition |
| :--- | :--- | :--- |
| 0 | `OK` | All validations green. |
| 1 | `GRAMMAR_MARKER_MISSING` | A `Skill(...)` or `Agent(...)` call lacks a grammar marker. |
| 2 | `OPERATIONAL_ERROR` | Prerequisites missing, or `GRAMMAR_INVALID_EXPR`. |
| 3 | `BASELINE_CORRUPT` | Baseline file malformed. |

## Forbidden

- `Skill(...)` or `Agent(subagent_type: "general-purpose", ...)` in Anexo B SKILL.md without a grammar marker.
- `[conditional: <expr>]` expressions outside the whitelist (see KP).
- Adding new Anexo B orchestrators without applying markers to all their `Skill(...)` calls before merge.
- Adding entries to `audits/tool-call-grammar-baseline.txt` after EPIC-0063 merges.
- Hard-coding exit code numbers instead of canonical names.

## Audit

`audit-tool-call-grammar.sh --self-check` MUST verify: `grep` and `jq` on PATH; this rule file exists.
Static audit runs on every PR touching SKILL.md; dynamic audit on every `epic/* → develop` PR.
