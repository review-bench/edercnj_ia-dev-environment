---
name: tool-call-grammar
description: Full tool-call grammar contract — inline grammar markers (required/optional/conditional), BNF, audit, Anexo B scope
requires-capabilities: []
---
# Tool-Call Grammar — Full Reference

> **Introduced by:** EPIC-0063 (story-0063-0012)
> **ADR:** ADR-0016
> **Full reference (BNF grammar, placement, examples, conditional whitelist, audit algorithms, baseline format):**
> `Read .claude/knowledge/governance/tool-call-grammar.md`

## Scope (8 Anexo B Orchestrators)

`x-implement-epic`, `x-implement-story`, `x-implement-task`, `x-release`,
`x-orchestrate-epic`, `x-review-codebase`, `x-review-pr`, `x-manage-pr-merge-train`.

## Marker Grammar (BNF)

```
marker        ::= "[" kind (":" expr)? "]"
kind          ::= "required" | "optional" | "conditional"
expr          ::= predicate (" and " predicate)*
predicate     ::= scope_pred | flag_pred
scope_pred    ::= "scope ∈ {" identifier ("," identifier)* "}"
flag_pred     ::= ("not " | "") "flag." identifier
```

### Placement

The marker MUST appear on the same line or immediately following line:
```
Skill(skill: "x-plan-architecture", model: "opus", args: "...")  [required]
```

### Marker Kinds

| Marker | Meaning | Audit behavior |
| :--- | :--- | :--- |
| `[required]` | MUST execute | Dynamic: `tool.call` event MUST exist in NDJSON |
| `[optional]` | MAY execute | Dynamic: no cross-check |
| `[conditional: <expr>]` | Executes when `<expr>` is true | Dynamic: if `<expr>` true → treated as `[required]` |

### Conditional Expression Whitelist

| Form | Example |
| :--- | :--- |
| `scope ∈ {id, ...}` | `scope ∈ {auth, network}` |
| `flag.<name>` | `flag.threat_model_required` |
| `not flag.<name>` | `not flag.skip_audit` |
| Conjunction | `scope ∈ {COMPLEX} and flag.tdd_required` |

## Exit Codes (`audit-tool-call-grammar.sh`)

| Exit | Code | Condition |
| :--- | :--- | :--- |
| 0 | `OK` | All validations green |
| 1 | `GRAMMAR_MARKER_MISSING` | `Skill(...)` or `Agent(...)` call lacks a grammar marker |
| 2 | `OPERATIONAL_ERROR` | Prerequisites missing or `GRAMMAR_INVALID_EXPR` |
| 3 | `BASELINE_CORRUPT` | Baseline file malformed |

## Forbidden

- `Skill(...)` or `Agent(subagent_type: "general-purpose", ...)` in Anexo B SKILL.md without a grammar marker
- `[conditional: <expr>]` expressions outside the whitelist
- Adding new Anexo B orchestrators without applying markers to all their `Skill(...)` calls before merge
- Adding entries to `audits/tool-call-grammar-baseline.txt` after EPIC-0063 merges
- Hard-coding exit code numbers instead of canonical names
