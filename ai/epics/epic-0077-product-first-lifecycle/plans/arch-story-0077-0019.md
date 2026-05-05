# Architecture Plan — story-0077-0019

## Story
Promote pentest-engineer to core role; create capability quality.pentest-always-on.

## Components

| Component | Action | Path |
| :--- | :--- | :--- |
| pentest-engineer.md | Move from conditional to core | `src/main/resources/targets/claude/agents/core/pentest-engineer.md` |
| pentest-always-on.yaml | New capability | `capabilities/quality/pentest/pentest-always-on.yaml` |

## Design
- Capability `quality.pentest-always-on` follows same schema as `quality.performance.rest.yaml`
- Moving pentest-engineer.md to core makes it unconditionally available
- Update `requires-capabilities: []` stays as is (universal)
- Charter update: add "Promoted to Core Role" badge and always-on section

## File Footprint

write:
- capabilities/quality/pentest/pentest-always-on.yaml
- src/main/resources/targets/claude/agents/core/pentest-engineer.md
read:
- src/main/resources/targets/claude/agents/conditional/pentest-engineer.md
regen: []
