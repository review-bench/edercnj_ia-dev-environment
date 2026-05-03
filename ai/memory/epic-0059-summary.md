---
epic-id: EPIC-0059
slug: zero-bypass-lifecycle-enforcement
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [governance, lifecycle, enforcement, audit, compliance]
capabilities-affected: []
rules-affected: [Rule 13, Rule 19, Rule 21, Rule 22, Rule 24, Rule 25, Rule 27]
adrs-referenced: []

patterns-introduced:
  - zero-bypass-13-surfaces
  - orchestrator-evidence-in-pr-body
  - baseline-immutability-pattern
  - enforce-no-bypass-flags-hook
antipatterns-rejected:
  - manual-branch-commit-without-orchestrator
  - marking-done-without-evidence

dependencies-of: [EPIC-0049, EPIC-0055]
dependencies-for: [EPIC-0061, EPIC-0064]
---
# Memory: EPIC-0059 — Zero-Bypass Lifecycle Enforcement

## Why this epic existed

Post-mortem analysis of EPIC-0054 through EPIC-0057 revealed **zero planning artifacts** in any of those 4 epics. Root cause: operators bypassed `x-story-implement` entirely — branching, coding, committing and creating PRs manually outside the controlled flow. All existing enforcement layers (CI audits, hooks, integrity tests) assumed the orchestrator was running; when the operator skipped it entirely, nothing fired.

## Hypothesis tested

A **zero-bypass enforcement layer** combining PR templates, pre-commit hooks, PreToolUse hooks, Stop hooks, and CI audit scripts checking 13 specific evidence surfaces would close the gap. **Confirmed**: Rule 27 published with 13 surfaces; `enforce-no-bypass-flags.sh` blocks `--skip-*` outside Recovery blocks; PR template requires `## Orchestrator Evidence`; `audit-bypass-flags.sh` catches violations at CI.

## Decisions taken (with why)

1. **Rule 27 with 13 surfaces** — explicit catalog of every orchestration point that requires evidence; enforcement is per-surface (Rule 27 vs Rule 24 distinction: Rule 24 = sub-skill invocations; Rule 27 = root orchestrator bypass).
2. **`## Orchestrator Evidence` section mandatory in PR body** — catches manual PRs that never ran orchestrator; `x-pr-create` writes it; `audit-pr-evidence.sh` verifies it.
3. **`governance/baselines/execution-integrity-baseline.txt` immutable post-merge** — no new entries allowed after EPIC-0059 merges; grandfathers EPIC-0054–0057 via amnesty (ADR-0015).
4. **`CLAUDE_RECOVERY_MODE=1` as sole bypass variable** — `enforce-no-bypass-flags.sh` (PreToolUse) blocks all other bypass env vars (RULE-059-07); only recognized bypass mechanism.
5. **`taskTracking.enabled=true` required for `flowVersion=2`** — silent no-op was dangerous; now hard fail `TASK_TRACKING_REQUIRED`.

## Alternatives rejected (with why)

- **Per-story opt-out flag** — any per-story escape hatch would be abused; only hotfix exception (Exception 2) and legacy-flow (Exception 1) permitted.
- **Advisory-only PR template** — advisory fields are ignored; template must gate CI via mandatory section check.
- **Environment variable whitelist broader than CLAUDE_RECOVERY_MODE** — each additional variable widens the bypass surface.

## Reusable patterns produced

- **`zero-bypass-13-surfaces`**: explicit catalog of enforcement points; each surface has a named evidence artifact; absence = CI failure.
- **`orchestrator-evidence-in-pr-body`**: PR description contains `## Orchestrator Evidence` section written by `x-pr-create`; manual PRs lack it and fail audit.
- **`baseline-immutability-pattern`**: governance baselines are append-only and immutable post-merge; a separate CI check enforces immutability.

## Anti-patterns observed

- **Manual branch + commit + PR without orchestrator** — root cause of EPIC-0054–0057 artifact gaps; prevented by Rule 27.
- **Trusting enforcement layers that assume orchestrator is running** — layers were detective, not preventive; EPIC-0063 later added Camada 0 (preventive).

## Links

- Epic: `ai/epics/epic-0059-zero-bypass-lifecycle-enforcement/epic-0059.md`
- ADRs: `docs/adr/ADR-0015-zero-bypass-amnesty.md` (amnesty for EPIC-0054–0057)
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0059-zero-bypass-lifecycle-enforcement/reports/`
