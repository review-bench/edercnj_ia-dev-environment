---
requires-capabilities: []
---
# Rule 24 — Execution Integrity

> **Consolidated by:** EPIC-0078 (Context Budget Optimization). **Authoritative contract:** Rule 19 (Lifecycle Integrity Contract).
> **Full detail — mandatory evidence artifacts, non-inlining contract, Camadas 0–4, audit exit codes, baseline format:**
> `Read src/main/resources/targets/claude/knowledge/lifecycle/execution-integrity.md`

## Contract (Non-Negotiable)

Every `Skill(skill: "...", args: "...")` in a SKILL.md body MUST be executed as a real tool call — never simulated, summarized, or skipped without an explicit `--skip-*` flag inside a `## Recovery` block. Absence of a mandatory evidence artifact on a merged story PR fails CI (`EIE_EVIDENCE_MISSING`).

## Forbidden

- Inlining what a sub-skill would do instead of emitting the Skill tool call.
- Using `--skip-*` flags outside `## Recovery` blocks.
- Bypassing `scripts/audit-execution-integrity.sh` via `--no-verify`.