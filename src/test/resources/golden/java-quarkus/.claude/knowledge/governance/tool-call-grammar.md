---
name: kp-governance-tool-call-grammar
description: "Full reference for Rule 30 Tool-Call Grammar: BNF grammar, marker placement, valid examples, conditional expression whitelist, static/dynamic audit algorithms, baseline format, and integration notes."
requires-capabilities: []
---

# Knowledge Pack: Tool-Call Grammar (Rule 30 — Full Reference)

## BNF Grammar

```
marker        ::= "[" kind (":" expr)? "]"
kind          ::= "required" | "optional" | "conditional"
expr          ::= predicate (" and " predicate)*
predicate     ::= scope_pred | flag_pred
scope_pred    ::= "scope ∈ {" identifier ("," identifier)* "}"
flag_pred     ::= ("not " | "") "flag." identifier
identifier    ::= [A-Za-z][A-Za-z0-9_]*
```

## Marker Placement

The marker MUST appear either:

- **On the same line**, after the closing `)`:
  ```
  Skill(skill: "x-plan-architecture", model: "opus", args: "...")  [required]
  ```
- **On the immediately following line**, alone (regex: `^\s*\[(required|optional|conditional[^\]]*)\]\s*$`):
  ```
  Skill(skill: "x-plan-architecture", model: "opus", args: "...")
  [required]
  ```

## Valid Examples

```markdown
Skill(skill: "x-plan-architecture", model: "opus", args: "...")  [required]

Skill(skill: "x-model-threats", model: "sonnet", args: "...")  [conditional: scope ∈ {auth, network, persistence}]

Skill(skill: "x-detect-spec-drift", model: "sonnet", args: "...")  [optional]

Agent(subagent_type: "general-purpose", description: "...", prompt: "...")  [required]

Skill(skill: "x-audit-dependencies", model: "haiku", args: "...")  [conditional: not flag.skip_audit]

Skill(skill: "x-drive-tdd", model: "sonnet", args: "...")  [conditional: scope ∈ {STANDARD, COMPLEX} and flag.tdd_required]
```

## Conditional Expression Whitelist

Only the following predicate forms are permitted inside `[conditional: ...]`:

| Form | Example | Semantics |
| :--- | :--- | :--- |
| `scope ∈ {id, ...}` | `scope ∈ {auth, network}` | True when story scope matches any listed identifier |
| `flag.<name>` | `flag.threat_model_required` | True when the named flag is set |
| `not flag.<name>` | `not flag.skip_audit` | True when the named flag is NOT set |
| Conjunction: `<pred> and <pred>` | `scope ∈ {COMPLEX} and flag.tdd_required` | Both predicates must be true |

Any expression outside this whitelist → audit exit `2` (`GRAMMAR_INVALID_EXPR`).

## Audit Modes

| Mode | Description | When used |
| :--- | :--- | :--- |
| `--mode=static` | Lints every Skill/Agent declaration in SKILL.md for presence of a grammar marker. | Every PR |
| `--mode=dynamic` | Cross-checks `[required]` markers against `events.ndjson` telemetry for a specific story. | PRs from `epic/* → develop` |
| `--mode=both` | Combines static and dynamic checks. | Recommended for epic-to-develop PRs |

## CLI Contract

```bash
scripts/audit-tool-call-grammar.sh --self-check
scripts/audit-tool-call-grammar.sh --skill-file <path/to/SKILL.md> [--baseline <path>]
scripts/audit-tool-call-grammar.sh --skills-root <path> [--baseline <path>]
scripts/audit-tool-call-grammar.sh --mode=dynamic --story-id=0063-0012 [--baseline <path>]
```

## Static Check Algorithm

For each SKILL.md in scope:

1. Extract every line matching `Skill(skill: "x-[a-z-]+"` or `Agent(subagent_type: "general-purpose"`.
2. Look for a grammar marker on the same line or the immediately following line.
3. If marker absent AND skill name is NOT in the baseline → exit 1 `GRAMMAR_MARKER_MISSING`.
4. If marker is `[conditional: ...]`, validate expr against whitelist → exit 2 `GRAMMAR_INVALID_EXPR` if invalid.

## Dynamic Check Algorithm

For the given `--story-id`:

1. Locate `events.ndjson` via PathResolver (`ai/epics/epic-XXXX-*/telemetry/`).
2. Extract all skills with a `tool.call` event where `storyId` matches `story-XXXX-YYYY`.
3. For each orchestrator SKILL.md in the execution chain, collect all `[required]` and `[conditional: <expr>]` declarations.
4. For `[required]`: verify presence in step 2. Absent → `GRAMMAR_TELEMETRY_MISSING`.
5. For `[conditional: <expr>]`: evaluate against story context. If true → apply `[required]` check; if false → skip is allowed.

## Baseline (Grandfather List)

`audits/tool-call-grammar-baseline.txt` grandfathers skills created before Rule 30 was introduced.

```
# tool-call-grammar-baseline.txt
<skill-name>  # <reason>
```

- Skills in the baseline are exempted from static and dynamic checks.
- File is **append-only** and **immutable** after EPIC-0063 merges to `develop`.
- New epics MUST NOT add entries — use the proper marker instead.

Initial baseline is empty: all Anexo B orchestrators have markers applied by TASK-0063-0012-004.

## Integration Notes

- **Rule 13** defines 3 permitted delegation patterns. Rule 30 adds grammar overlay on Pattern 1 (INLINE-SKILL) and Pattern 2 (SUBAGENT-GENERAL). Pattern 3 (SUBAGENT-RESEARCH, `Explore`) is out of scope.
- **Rule 24** ensures declared sub-skills are actually invoked. Rule 30 extends Rule 24 by making the obligation class explicit and machine-checkable.
- **Rule 25** introduced `[required]`/`[optional]` at phase level. Rule 30 operates at individual invocation level — complementary, not redundant.
- `enforce-phase-sequence.sh` (Camada 0 PreToolUse) warns when a `Skill(...)` or `Agent(...)` call in an orchestrator turn lacks a grammar marker.
