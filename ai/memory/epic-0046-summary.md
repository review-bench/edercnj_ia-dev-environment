---
epic-id: EPIC-0046
slug: lifecycle-integrity-status-propagation
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [lifecycle, status-sync, git, audit, governance]
capabilities-affected: []
rules-affected: [Rule 22]
adrs-referenced: []

patterns-introduced:
  - atomic-report-commit-after-write
  - status-field-propagation-v2-gated
antipatterns-rejected:
  - orphan-report-files-without-commit
  - stale-markdown-status-vs-execution-state

dependencies-of: [EPIC-0045]
dependencies-for: [EPIC-0059, EPIC-0063]
---
# Memory: EPIC-0046 — Lifecycle Integrity — Propagação de Status e Commits Atômicos

## Why this epic existed

Two integrity gaps coexisted in the SDD lifecycle: (GAP-1) planning and execution skills did NOT update the `**Status:**` field in Epic/Story/Task markdown files after state changes — files remained `Concluída` in `execution-state.json` but the markdown showed stale status. (GAP-2) `x-epic-implement` wrote `execution-plan-epic-*.md` and `phase-report-epic-*.md` to `plans/epic-XXXX/reports/` without invoking `x-git-commit`, leaving those files as untracked orphans. The `VALIDATE_DIRTY_WORKDIR` precondition in `x-release` (line 277-308) aborted the release train on every dirty worktree caused by these orphan files.

## Hypothesis tested

Introducing Rule 22 (lifecycle-integrity), a status field propagation matrix, and retrofitting 10 SKILL.md files to propagate status and commit reports atomically would close both gaps without breaking legacy epics (Rule 19). **Confirmed**: `StatusFieldParser`, `LifecycleTransitionMatrix`, `LifecycleAuditRunner` delivered; `LifecycleIntegrityAuditTest` CI-blocking added; `x-status-reconcile` skill for backfilling legacy epics delivered (opt-in); status sync is V2-gated via `SchemaVersionResolver` — v1 epics unaffected.

## Decisions taken (with why)

1. **V2-gated status sync** — SchemaVersionResolver checks `flowVersion`; v1 epics do not execute new phases. Prevents Rule 19 breakage on 60+ legacy epics.
2. **Atomic write `.tmp` + rename in `StatusFieldParser`** — prevents corruption when process is killed mid-write.
3. **`x-status-reconcile` as opt-in** — never automatically touches `execution-state.json`; operators must run `--apply` explicitly. Preserves Rule 19 invariant.

## Alternatives rejected (with why)

- **`@SuppressWarnings` on stale status** — defers the gap; orphan files still fail `x-release`.
- **Automatic backfill of all v1 epics** — violates Rule 19; too risky for in-flight epics.

## Reusable patterns produced

- **`atomic-report-commit-after-write`**: every skill that writes to `reports/` invokes `x-git-commit` in the same step; no orphan files.
- **`status-field-propagation-v2-gated`**: `SchemaVersionResolver.resolve() == V2` guards new lifecycle transitions; v1 epics see no change.

## Anti-patterns observed

- **Orphan report files** — writing to `plans/epic-*/reports/` without committing breaks `VALIDATE_DIRTY_WORKDIR` in `x-release`.
- **Stale markdown status** — `execution-state.json` says DONE but markdown says Pendente; causes confusion during manual audit.

## Links

- Epic: `ai/epics/epic-0046-lifecycle-integrity-status-propagation/epic-0046.md`
- ADRs: (none recorded)
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0046-lifecycle-integrity-status-propagation/reports/`
