---
name: skill-visibility
description: Full skill visibility reference — public vs internal naming convention, frontmatter contract, body marker, audit
requires-capabilities: []
---
# Skill Visibility — Full Reference

> **Introduced by:** EPIC-0049

## Visibility Classes

| Class | Prefix | User-Invocable? | Shows in `/help`? |
| :--- | :--- | :--- | :--- |
| **Public** | `x-{subject}-{action}` | Yes | Yes |
| **Internal** | `x-internal-{subject}-{action}` | **No** | **No** |

Internal skills are implementation details — invoked **only** by other skills via the Skill tool.

## Naming Convention

- Public skills: `x-{subject}-{action}` — 2-to-3-token kebab-case
- Internal skills: MUST carry the `x-internal-` prefix exactly
- Subdir convention (source of truth): `skills/core/internal/{group}/x-internal-{name}/SKILL.md`
  where `{group}` ∈ `plan`, `git`, `ops`
- Generated output: **flat** under `.claude/skills/`

## Frontmatter Contract

Every internal skill's SKILL.md MUST start with:

```yaml
---
name: x-internal-{subject}-{action}
description: <one-line description>
visibility: internal
user-invocable: false
allowed-tools: [...]
---
```

Both `visibility: internal` AND `user-invocable: false` are required.

## Body Marker

Every internal skill's SKILL.md body MUST open with:

```markdown
> 🔒 **INTERNAL SKILL** — Invoked only by other skills via the Skill tool. Not user-invocable.
```

## Forbidden

- Documenting an `x-internal-*` skill as a user command
- Prose in user-facing files instructing the user to "run `/x-internal-foo`"
- Calling an internal skill from a non-skill context (e.g., CI scripts)
- Renaming a public skill to `x-internal-*` without a deprecation announcement

## Permitted

- A public skill's INLINE-SKILL delegation invoking an internal skill
- An internal skill invoking another internal skill
- `## Integration Notes` in a public SKILL.md listing internal skills it depends on

## Audit Script

CI script `scripts/audit-skill-visibility.sh` verifies:

1. **Prefix/frontmatter consistency.** `x-internal-*` directories must have `visibility: internal` + `user-invocable: false`.
2. **Body marker present.** Internal skills must contain the `🔒 **INTERNAL SKILL**` marker in first 10 lines.
3. **No user-facing trigger.** Internal skills' `## Triggers` section must not list bare-slash commands.
4. **No cross-reference in user-facing docs.** README, CHANGELOG, `docs/` must not contain `/x-internal-` slash in prose.

Any violation exits with `SKILL_VISIBILITY_VIOLATION` (exit 22).

## EPIC-0065 Chain Internals

Three internal skills introduced by EPIC-0065 (Feature Creation Chain Refactor):

| Internal skill | Replaced public skill | Source path | Invoked by |
| :--- | :--- | :--- | :--- |
| `x-internal-create-epic` | `x-epic-create` (hard-cut) | `core/internal/plan/x-internal-create-epic/SKILL.md` | `x-create-feature`, `x-epic-create` |
| `x-internal-map-epic` | `x-epic-map` (hard-cut) | `core/internal/plan/x-internal-map-epic/SKILL.md` | `x-create-feature` |
| `x-internal-create-story` | `x-story-create` (hard-cut) | `core/internal/plan/x-internal-create-story/SKILL.md` | `x-create-feature`, `x-story-create` |

## Migration Path

1. Pick name: `x-internal-{subject}-{action}`
2. Create `skills/core/internal/{group}/x-internal-{name}/SKILL.md`
3. Replace inline code in orchestrator with an INLINE-SKILL invocation
4. Regenerate `.claude/skills/` via `mvn process-resources`
5. Verify: `scripts/audit-skill-visibility.sh`
