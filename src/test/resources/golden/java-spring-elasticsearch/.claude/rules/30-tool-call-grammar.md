# Rule 28 — Tool-Call Grammar

> **Related:** Rule 13 (Skill Invocation Protocol), Rule 24 (Execution Integrity), Rule 25 (Task Hierarchy & Phase Gate Contract).
> **Introduced by:** EPIC-0063 (Local-First Pre-Flight Gates) — story-0063-0012.
> **ADR:** See `docs/adr/ADR-0016-zero-bypass-lifecycle.md` for the zero-bypass context.

## Purpose

Rule 24 ensures that every declared `Skill(...)` tool call inside a SKILL.md is actually
executed as a real tool call — not simulated or skipped. However, Rule 24 cannot distinguish
"skill skipped step" from "skill never needed that step": today **no static contract** exists
at the individual invocation level.

Rule 28 closes this gap by introducing a mandatory **inline grammar marker** on every
`Skill(skill: "x-...")` and `Agent(subagent_type: "general-purpose", ...)` declaration in
an orchestrator's SKILL.md. The marker declares the invocation's obligation class, enabling
both static lint (`audit-tool-call-grammar.sh --mode=static`) and dynamic cross-check
against the telemetry NDJSON (`--mode=dynamic`).

Without this grammar, "the LLM silently skipped a step" and "the LLM legitimately skipped
an optional step" are indistinguishable from static analysis. Rule 28 eliminates the
ambiguity entirely.

## Marker Grammar (BNF)

```
marker        ::= "[" kind (":" expr)? "]"
kind          ::= "required" | "optional" | "conditional"
expr          ::= predicate (" and " predicate)*
predicate     ::= scope_pred | flag_pred
scope_pred    ::= "scope ∈ {" identifier ("," identifier)* "}"
flag_pred     ::= ("not " | "") "flag." identifier
identifier    ::= [A-Za-z][A-Za-z0-9_]*
```

### Placement

The marker MUST appear either:
- **On the same line** as the `Skill(...)` or `Agent(...)` call, after the closing `)`:
  ```
  Skill(skill: "x-plan-architecture", model: "opus", args: "...")  [required]
  ```
- **On the immediately following line**, alone (regex: `^\s*\[(required|optional|conditional[^\]]*)\]\s*$`):
  ```
  Skill(skill: "x-plan-architecture", model: "opus", args: "...")
  [required]
  ```

### Marker Kinds

| Marker | Meaning | Audit behavior |
| :--- | :--- | :--- |
| `[required]` | This invocation MUST execute. | Static: always checked. Dynamic: `tool.call` event for this skill MUST exist in the story NDJSON. Absence → `GRAMMAR_TELEMETRY_MISSING`. |
| `[optional]` | This invocation MAY execute. | Static: presence is enough. Dynamic: no cross-check performed (absent events are acceptable). |
| `[conditional: <expr>]` | This invocation executes only when `<expr>` is true. | Static: expr validated against whitelist. Dynamic: if `<expr>` evaluates true against story context → `GRAMMAR_TELEMETRY_MISSING` on absence; if false → allowed to skip. |

### Valid Examples

```markdown
Skill(skill: "x-plan-architecture", model: "opus", args: "...")  [required]

Skill(skill: "x-model-threats", model: "sonnet", args: "...")  [conditional: scope ∈ {auth, network, persistence}]

Skill(skill: "x-detect-spec-drift", model: "sonnet", args: "...")  [optional]

Agent(subagent_type: "general-purpose", description: "...", prompt: "...")  [required]

Skill(skill: "x-audit-dependencies", model: "haiku", args: "...")  [conditional: not flag.skip_audit]

Skill(skill: "x-drive-tdd", model: "sonnet", args: "...")  [conditional: scope ∈ {STANDARD, COMPLEX} and flag.tdd_required]
```

### Conditional Expression Whitelist

Only the following predicate forms are permitted inside `[conditional: ...]`:

| Form | Example | Semantics |
| :--- | :--- | :--- |
| `scope ∈ {id, ...}` | `scope ∈ {auth, network}` | True when story scope matches any listed identifier |
| `flag.<name>` | `flag.threat_model_required` | True when the named flag is set in the orchestrator's invocation context |
| `not flag.<name>` | `not flag.skip_audit` | True when the named flag is NOT set |
| Conjunction: `<pred> and <pred>` | `scope ∈ {COMPLEX} and flag.tdd_required` | Both predicates must be true |

Any expression outside this whitelist → audit exit `2` (`GRAMMAR_INVALID_EXPR`).

## Scope

This rule applies to the **8 Anexo B orchestrators** (Rule 25 §Scope):

| Orchestrator | SKILL.md |
| :--- | :--- |
| `x-implement-epic` | `skills/core/dev/x-implement-epic/SKILL.md` |
| `x-implement-story` | `skills/core/dev/x-implement-story/SKILL.md` |
| `x-implement-task` | `skills/core/dev/x-implement-task/SKILL.md` |
| `x-release` | `skills/core/dev/x-release/SKILL.md` |
| `x-orchestrate-epic` | `skills/core/dev/x-orchestrate-epic/SKILL.md` |
| `x-review-codebase` | `skills/core/review/x-review-codebase/SKILL.md` |
| `x-review-pr` | `skills/core/review/x-review-pr/SKILL.md` |
| `x-manage-pr-merge-train` | `skills/core/pr/x-manage-pr-merge-train/SKILL.md` |

**Out of scope:** Leaf skills (e.g., `x-format-code`, `x-commit-changes`) and internal skills
(`x-internal-*`) — they do not declare sub-skill invocations in their SKILL.md bodies.
Future epics may extend scope to leaf skills.

## Audit: `scripts/audit-tool-call-grammar.sh`

### Modes

| Mode | Description | When used |
| :--- | :--- | :--- |
| `--mode=static` | Lints every Skill/Agent declaration in the SKILL.md for presence of a grammar marker. | Every PR (pre-merge gate). |
| `--mode=dynamic` | Cross-checks `[required]` markers against `events.ndjson` telemetry for a specific story. | PRs from `epic/* → develop`. |
| `--mode=both` | Combines static and dynamic checks. | Recommended for epic-to-develop PRs. |

### CLI Contract

```bash
# Check prerequisites
scripts/audit-tool-call-grammar.sh --self-check

# Static lint on one SKILL.md
scripts/audit-tool-call-grammar.sh --skill-file <path/to/SKILL.md> [--baseline <path>]

# Static lint across all skills under a root
scripts/audit-tool-call-grammar.sh --skills-root <path> [--baseline <path>]

# Dynamic cross-check for a story
scripts/audit-tool-call-grammar.sh --mode=dynamic --story-id=0063-0012 [--baseline <path>]
```

### Exit Codes (Rule 26 §Standardized + Rule 28)

| Exit | Code | Condition |
| :--- | :--- | :--- |
| 0 | `OK` | All validations green. |
| 1 | `GRAMMAR_MARKER_MISSING` | A `Skill(...)` or `Agent(...)` call lacks a grammar marker in a non-grandfathered SKILL.md. |
| 2 | `OPERATIONAL_ERROR` | Prerequisites missing (jq, grep), file not found, or invalid arguments. |
| 3 | `BASELINE_CORRUPT` | Baseline file is malformed or non-readable. |

### Static Check Algorithm

For each SKILL.md in scope:

1. Extract every line matching `Skill(skill: "x-[a-z-]+"` or `Agent(subagent_type: "general-purpose"`.
2. For each match, look for a grammar marker on the same line or the immediately following line.
3. If marker absent AND the SKILL.md's skill name is NOT in the baseline → exit 1 `GRAMMAR_MARKER_MISSING`.
4. If marker is `[conditional: ...]`, validate expr against whitelist → exit 2 `GRAMMAR_INVALID_EXPR` if invalid.

### Dynamic Check Algorithm

For the given `--story-id`:

1. Locate `events.ndjson` via PathResolver (v3 = `ai/epics/epic-XXXX/telemetry/`, v4 = `ai/epics/epic-XXXX-*/telemetry/`).
2. Extract all skills with a `tool.call` event where `storyId` matches `story-XXXX-YYYY`.
3. For each orchestrator SKILL.md in the story's execution chain, collect all `[required]` and `[conditional: <expr>]` declarations.
4. For `[required]`: verify presence in step 2 result. Absent → `GRAMMAR_TELEMETRY_MISSING`.
5. For `[conditional: <expr>]`: evaluate `<expr>` against story context (`execution-state.json`). If true → apply same check as `[required]`; if false → skip is allowed.

## Baseline (Grandfather List)

`audits/tool-call-grammar-baseline.txt` grandfathers skills created before Rule 28 was introduced.

Format:
```
# tool-call-grammar-baseline.txt
<skill-name>  # <reason>
```

- Skills in the baseline are exempted from both static and dynamic checks.
- The file is **append-only** and **immutable** after EPIC-0063 merges to `develop`. A separate CI check (`audit-immutability.sh`) enforces that no entries are removed after merge day.
- New epics MUST NOT add entries to this file — use the proper marker instead.

The initial baseline is empty: all Anexo B orchestrators will have markers applied by TASK-0063-0012-004. Non-Anexo-B skills are out of scope (not added to baseline either).

## Integration with Rule 13 and Rule 24

- **Rule 13** defines the three permitted delegation patterns. Rule 28 adds a grammar overlay on top of Pattern 1 (INLINE-SKILL) and Pattern 2 (SUBAGENT-GENERAL). Pattern 3 (SUBAGENT-RESEARCH) uses `Agent(subagent_type: "Explore")` — out of scope for Rule 28 (no sub-skill invocations possible from Explore subagents).
- **Rule 24** ensures declared sub-skills are actually invoked (non-inlining contract). Rule 28 extends Rule 24 by making the *obligation class* of each invocation explicit and machine-checkable.
- **Rule 25** introduced `[required]` / `[optional]` phase markers at the phase level. Rule 28 introduces the same classification at the individual invocation level — complementary, not redundant.

## Camada 0 — Preventive Hook Integration

`enforce-phase-sequence.sh` (Rule 26 Camada 0 — PreToolUse hook) is extended to warn when
a `Skill(...)` or `Agent(...)` call is detected inside an orchestrator turn without a
grammar marker on the preceding or same line. This surfaces the violation to the LLM during
generation, before the SKILL.md artifact is committed.

## Forbidden

- Declaring a `Skill(...)` or `Agent(subagent_type: "general-purpose", ...)` in an Anexo B orchestrator SKILL.md without a grammar marker.
- Using any `[conditional: <expr>]` expression outside the whitelist defined above.
- Adding new orchestrators to the Anexo B set without applying Rule 28 markers to all their `Skill(...)` calls before merge.
- Adding entries to `audits/tool-call-grammar-baseline.txt` after EPIC-0063 merges to `develop`.
- Hard-coding the exit code number (e.g., `if [ $? -eq 1 ]`) instead of the canonical name — callers MUST use named codes.

## Audit

Self-check: `scripts/audit-tool-call-grammar.sh --self-check` MUST verify:
1. `grep` and `jq` are present on `PATH`.
2. This rule file (`30-tool-call-grammar.md`) exists at `.claude/rules/`.
3. Exit 0 when all prerequisites satisfied; exit 2 on any failure.

The full static audit runs on every PR touching any SKILL.md under `src/main/resources/targets/claude/skills/`. The dynamic audit runs on every PR targeting `develop` from an `epic/*` branch.

---

> **Catalogado em:** [`docs/audit-gates-catalog.md`](../../docs/audit-gates-catalog.md)
