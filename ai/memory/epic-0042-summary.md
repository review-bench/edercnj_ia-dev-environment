---
epic-id: EPIC-0042
slug: merge-train-auto-pr-fix
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [automation, pr, merge, git, workflow]
capabilities-affected: []
rules-affected: []
adrs-referenced: []

patterns-introduced:
  - pr-merge-train-sequential-auto-rebase
  - auto-pr-fix-after-tl-go
antipatterns-rejected:
  - manual-sequential-rebase-per-pr

dependencies-of: []
dependencies-for: [EPIC-0043, EPIC-0045]
---
# Memory: EPIC-0042 — Merge Train Auto-PR-Fix

## Why this epic existed

Epic closure required 1–2 hours of manual sequential work: merging 3–5 chained PRs, rebasing each subsequent PR over the updated `develop`, resolving golden file conflicts via `git checkout --ours` + regen, and force-pushing one by one. Additionally, after Tech Lead Review GO, PR comments scattered across task PRs needed manual `/x-pr-fix <pr>` invocations.

## Hypothesis tested

A `x-pr-merge-train` skill automating sequential PR merges with rebase/regen/push workers and an automatic `x-pr-fix` pass after TL GO would eliminate this manual toil. **Confirmed**: skill delivered; progress persisted in `plans/merge-train/<id>/state.json` for `--resume`; auto-fix hook added to `x-story-implement` Step 3.6.5.

## Decisions taken (with why)

1. **VETO conditions**: draft PR, CI red, wrong base, missing approval, or merge conflict → hard abort; no silent merge.
2. **Parallel rebase workers with `--max-parallel`** — pre-checks for file overlap before dispatching parallel workers.
3. **Auto-PR-fix after TL=GO** (single-pass, abort on `PR_FIX_COMPILE_REGRESSION`) — captures Copilot comments before merge.
4. **`--resume` via state file** — merge trains can be interrupted and resumed without losing progress.

## Alternatives rejected (with why)

- **Squash all PRs to one commit** — loses TDD granularity (RED/GREEN/REFACTOR visible per task commit).
- **No VETO checks** — merging with CI red would introduce broken commits to develop/epic.

## Reusable patterns produced

- **`pr-merge-train-sequential-auto-rebase`**: merge list consumed from `--prs`/`--epic`/`--pattern`; rebase + regen + push per wave.
- **`auto-pr-fix-after-tl-go`**: single-pass fix after TL review GO; hard abort on compile regression.

## Anti-patterns observed

- **Manual sequential rebase per PR** — 1-2h per epic closure; error-prone (missed golden regen causes test failures).

## Links

- Epic: `ai/epics/epic-0042-merge-train-auto-pr-fix/epic-0042.md`
- ADRs: (none recorded)
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0042-merge-train-auto-pr-fix/reports/`
