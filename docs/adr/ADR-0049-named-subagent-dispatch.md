# ADR-0049 — Named Subagent Dispatch (EPIC-0079)

## Status

Accepted

## Context

Before EPIC-0079, all subagent dispatches in skills used `subagent_type: "general-purpose"` and embedded the agent's persona inline in the `prompt:` field (e.g., `"You are a Senior Architect. ..."`). This caused two problems:

1. **Persona drift:** Each skill independently described the same persona, with no guarantee of consistency across the codebase. An update to the Architect persona required finding every occurrence in every skill.
2. **Audit blind spot:** `audit-agent-skill-wiring.sh` could not verify whether a skill was using the right persona for a given task, because the persona was hidden inside an opaque prompt string.

EPIC-0079 introduced named agent files (`.claude/agents/core/<name>.md`) that carry the canonical persona as a system prompt loaded by the Claude Code runtime. Skills dispatch them by setting `subagent_type` to the agent name rather than `"general-purpose"`.

## Decision

Introduce **Pattern 2b — SUBAGENT-NAMED** as an official variant of Pattern 2 in Rule 13:

```
Agent(
  subagent_type: "<agent-name>",
  description: "<short 3-7 word summary>",
  prompt: "<task instructions only — NO persona text>"
)
```

When `subagent_type` is a registered agent name (a file in `.claude/agents/`), the Claude Code runtime automatically loads that agent's `<name>.md` body as the system prompt. The `prompt:` field must contain only task instructions — duplicating persona text is both redundant and a maintenance hazard.

**Decision criteria — named dispatch vs. general-purpose:**

| Situation | Pattern |
| :--- | :--- |
| A registered agent file exists for the role (e.g., `architect`, `sre-engineer`) | **2b — named** |
| No agent file exists; role is ad-hoc or one-off | **2a — general-purpose** with inline persona |
| Multiple heterogeneous agents in one parallel wave | Mix: named for registered, general-purpose for ad-hoc |

## Consequences

- **Persona consistency:** Agent persona is defined once in `<name>.md`; all skills that dispatch that agent inherit the same persona automatically.
- **Auditability:** `audit-agent-skill-wiring.sh --check-orphans` can enumerate all registered agents and verify each has at least one skill callsite — orphan agents are detected and flagged.
- **Rule 13 extension:** Pattern 2 gains a 2b sub-variant. Rule 28 grammar markers (`[required]`, `[optional]`, `[conditional: ...]`) apply to named dispatch calls identically to general-purpose calls.
- **Telemetry:** The `metadata.role` field in telemetry events produced by `subagent-start`/`subagent-end` markers receives the agent name (e.g., `"sre-engineer"`) instead of a free-form string, enabling per-role latency analysis in `/x-analyze-telemetry`.

## Alternatives Considered

**Alternative 1 — Keep `general-purpose` everywhere, update persona in prompt.**
Rejected: persona drift is not preventable; no audit path exists.

**Alternative 2 — Create Pattern 4 (separate numbered pattern) for named dispatch.**
Rejected: named dispatch is syntactically identical to Pattern 2; adding a fourth pattern number would break Rule 28 references that explicitly cite "Pattern 2" and confuse maintainers.

**Alternative 3 — Use a separate `persona:` field in `Agent(...)` calls.**
Rejected: requires runtime API change; `subagent_type` already serves as the persona selector when a matching agent file exists.

## References

- EPIC-0079 implementation map: `ai/epics/epic-0079-agent-skill-wiring/`
- Rule 13 (updated): `src/main/resources/targets/claude/rules/13-skill-invocation-protocol.md`
- Audit script: `scripts/audit-agent-skill-wiring.sh`
