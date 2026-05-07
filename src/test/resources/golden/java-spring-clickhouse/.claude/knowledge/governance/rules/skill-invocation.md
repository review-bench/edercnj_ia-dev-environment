---
name: skill-invocation
description: Full skill invocation protocol — 3 permitted patterns (INLINE-SKILL, SUBAGENT-GENERAL, SUBAGENT-RESEARCH), forbidden bare-slash, telemetry markers
requires-capabilities: []
---
# Skill Invocation Protocol — Full Reference

> **Always-loaded summary:** `.claude/rules/00-essentials.md §6 Skill Invocation Protocol`
> **Introduced by:** EPIC-0033

## Scope

All skill templates under `skills/core/**/SKILL.md` MUST follow one of the three permitted delegation patterns below when one skill invokes another. The bare-slash shorthand `/x-foo` is **forbidden** in delegation contexts.

## Permitted Patterns

### Pattern 1 — INLINE-SKILL (preferred for direct delegation)

Use when the orchestrator wants a synchronous call to another skill.

```markdown
Invoke the `x-foo` skill via the Skill tool:

    Skill(skill: "x-foo", args: "--flag value --other thing")
```

**Requirement:** the calling skill's `allowed-tools` frontmatter MUST include `Skill`.

### Pattern 2 — SUBAGENT-GENERAL (isolated general-purpose subagent)

Use for parallel work, context isolation, or multiple independent workers.

**Required form:**

```markdown
Agent(
  subagent_type: "general-purpose",
  description: "<short 3-7 word summary>",
  prompt: "<multi-line prompt: FIRST ACTION TaskCreate + task body + LAST ACTION TaskUpdate>"
)
```

**Example (a) — subagent invokes another skill:**
```markdown
Agent(
  subagent_type: "general-purpose",
  description: "Run x-foo for {args}",
  prompt: "FIRST ACTION: TaskCreate(...). Invoke x-foo via the Skill tool: Skill(skill: \"x-foo\", args: \"...\"). LAST ACTION: TaskUpdate(...)."
)
```

**Example (b) — subagent produces an artifact (no Skill call):**
```markdown
Agent(
  subagent_type: "general-purpose",
  description: "Plan implementation for story X",
  prompt: "FIRST ACTION: TaskCreate(...). You are a Senior Architect. Read context files. Produce plan at ai/epics/epic-XXXX/plans/plan-story-XXXX-YYYY.md. LAST ACTION: TaskUpdate(...)."
)
```

**Requirement:** the parent's `allowed-tools` MUST include `Agent`.

**Parallelism:** Emit all `Agent(...)` calls as SIBLING tool calls in the SAME assistant message.

### Pattern 3 — SUBAGENT-RESEARCH (no Skill call, pure exploration)

Use when the orchestrator needs investigation that does NOT require invoking another skill.

```markdown
Agent(
  subagent_type: "Explore",
  description: "Find all references to pattern X",
  prompt: "Search the codebase for {pattern} and report file:line matches. Do not modify anything."
)
```

## Forbidden Pattern — Bare-Slash in Delegation Contexts

These forms are PROHIBITED in skill body text used for delegation:

```markdown
Invoke /x-foo with args
/x-foo {STORY_ID}
```

**Exception:** User-facing sections (`## Triggers`, `## Examples`) may use bare-slash because the user literally types it.

## Audit Command

```bash
grep -rnE "Invoke\s+\`?/?x-[a-z-]+" \
    src/main/resources/targets/claude/skills/core/ \
    --include=SKILL.md \
  | grep -v "## Triggers" \
  | grep -v "## Examples"
```

Expected result: 0 matches.

## Telemetry Markers

Skills participating in the implementation workflow MUST emit `phase.start` / `phase.end` telemetry markers around every numbered phase.

### Canonical Shape

```markdown
## Phase N — <Phase Name>

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start <skill-name> Phase-N-<Name>`

... phase body ...

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end <skill-name> Phase-N-<Name> ok`
```

### Helper Contract (`telemetry-phase.sh`)

| Argument | Values |
| :--- | :--- |
| `$1` | `start` \| `end` \| `subagent-start` \| `subagent-end` |
| `$2` | skill identifier (kebab-case) |
| `$3` | phase identifier (max 64 chars) |
| `$4` | `ok` \| `failed` \| `skipped` (required on `end`) |

Fail-open contract: invalid arguments → log to stderr and exit 0 (skills never abort because telemetry broke).

### Subagent Markers

Planning skills that dispatch parallel subagents MUST emit `subagent.start` / `subagent.end` markers:

```markdown
<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-plan-story Architect`

... subagent dispatch ...

<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-plan-story Architect ok`
```

### CI Enforcement

`dev.iadev.ci.TelemetryMarkerLint` scans every SKILL.md for balance violations:

| Violation | Meaning |
| :--- | :--- |
| `DUPLICATE_START` | Two consecutive `phase.start` for same `(skill, phase)` |
| `DUPLICATE_END` | Two consecutive `phase.end` for same `(skill, phase)` |
| `DANGLING_END` | `phase.end` with no preceding `phase.start` |
| `UNCLOSED_START` | `phase.start` with no matching `phase.end` before EOF |
