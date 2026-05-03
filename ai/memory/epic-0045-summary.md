---
epic-id: EPIC-0045
slug: ci-watch-pr-flow
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [ci, pr, automation, review, copilot, governance]
capabilities-affected: []
rules-affected: [Rule 21, Rule 45]
adrs-referenced: []

patterns-introduced:
  - pr-watch-ci-stable-exit-codes
  - copilot-review-detection
antipatterns-rejected:
  - pr-merge-before-ci-green-and-copilot-review

dependencies-of: [EPIC-0043]
dependencies-for: [EPIC-0049, EPIC-0057]
---
# Memory: EPIC-0045 — CI Watch no Fluxo de PR

## Why this epic existed

Orchestrators proceeded directly from `x-pr-create` to the APPROVE/MERGE gate without waiting for CI checks or the Copilot automated review (latency 30–180s). The FIX-PR slot in the gate menu invoked `x-pr-fix` which found zero comments because Copilot hadn't posted yet. The entire purpose of the interactive gate — acting on automated feedback before merge — was defeated.

## Hypothesis tested

A dedicated `x-pr-watch-ci` skill encapsulating CI check polling + Copilot review detection with **8 stable named exit codes** would make CI-first merge the standard flow. **Confirmed**: Rule 21 formalized with fallback matrix; `PrWatchStatusClassifier` + `PrWatchExitCode` delivered (zero-I/O, fully testable); retrofits to `x-story-implement`, `x-task-implement --worktree`, and `x-release`. Rule 45 promoted the contract to a first-class rule.

## Decisions taken (with why)

1. **8 stable named exit codes** (SUCCESS/CI_PENDING_PROCEED/CI_FAILED/TIMEOUT/PR_ALREADY_MERGED/NO_CI_CONFIGURED/PR_CLOSED/PR_NOT_FOUND) — orchestrators dispatch by name, never raw int; changing a code is MAJOR.
2. **Copilot review detection** as a separate condition from CI checks — PR may have green CI but no Copilot review yet.
3. **`--no-ci-watch` restricted to `## Recovery` blocks** — any occurrence outside Recovery in a SKILL.md fails `audit-bypass-flags.sh`.
4. **`.claude/state/pr-watch-{PR}.json` state file** — IS the proof that the skill ran; absence on a merged PR fails Camada 2/3 audit.

## Alternatives rejected (with why)

- **Inline `gh pr checks --watch` in orchestrators** — bypasses state-file contract; no resume; no named exit codes.
- **Copilot timeout = CI failure** — operational absence is different from CI failure; different handler needed.

## Reusable patterns produced

- **`pr-watch-ci-stable-exit-codes`**: 8 named codes; SemVer contract; orchestrators dispatch by name.
- **`copilot-review-detection`**: separate from CI checks; `CI_PENDING_PROCEED` when Copilot absent after timeout.

## Anti-patterns observed

- **Merging before CI + Copilot review** — defeats the purpose of automated review gates.

## Links

- Epic: `ai/epics/epic-0045-ci-watch-pr-flow/epic-0045.md`
- ADRs: (none recorded)
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0045-ci-watch-pr-flow/reports/`
