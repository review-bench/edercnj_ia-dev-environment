---
requires-capabilities: []
---
# Rule 45 — CI-Watch Integrity

> **Consolidated by:** EPIC-0078 (Context Budget Optimization). **Authoritative contract:** Rule 19 (Lifecycle Integrity Contract).
> **Full detail — 8 exit codes, fallback matrix, 5 mandatory invocation sites, `--no-ci-watch` constraints:**
> `Read src/main/resources/targets/claude/knowledge/lifecycle/ci-watch-integrity.md`

## Contract (Non-Negotiable)

Every orchestrator that creates a PR via `x-create-pr` MUST follow with `Skill(skill: "x-watch-pr-ci", args: "--pr-number <PR>")`. The `.claude/state/pr-watch-{PR}.json` state file IS the evidence. Absence on a merged PR fails Camada 3 audit (`EIE_EVIDENCE_MISSING`).

## Exit Codes (8 Stable — RULE-045-05)

`0=SUCCESS`, `10=CI_PENDING_PROCEED`, `20=CI_FAILED`, `30=TIMEOUT`, `40=PR_ALREADY_MERGED`, `50=NO_CI_CONFIGURED`, `60=PR_CLOSED`, `70=PR_NOT_FOUND`. Changes are SemVer events (Rule 08).

## Forbidden

- Inlining `gh pr checks` instead of invoking `x-watch-pr-ci` (bypasses state-file contract).
- Using `--no-ci-watch` outside `## Recovery` blocks.
- Hard-coding numeric exit codes — use canonical names.