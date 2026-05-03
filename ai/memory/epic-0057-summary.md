---
epic-id: EPIC-0057
slug: rule24-execution-integrity-extension
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [governance, audit, enforcement, ci, rule24, rule45]
capabilities-affected: []
rules-affected: [Rule 24, Rule 45]
adrs-referenced: []

patterns-introduced:
  - rule45-ci-watch-first-class-rule
  - audit-bypass-flags-script
  - mandatory-evidence-table-11-entries
antipatterns-rejected:
  - ci-watch-silent-skip-in-orchestrators
  - incomplete-evidence-artifacts-table

dependencies-of: [EPIC-0053]
dependencies-for: [EPIC-0059, EPIC-0063]
---
# Memory: EPIC-0057 — Extensão da Rule 24 — Pós-mortem EPIC-0053

## Why this epic existed

Post-mortem of EPIC-0053: `x-pr-watch-ci` was silently skipped in 6 task-PRs and the final PR #619 without any Rule 24 enforcement layer detecting the omission. Root cause analysis found 14 analogous gaps across main orchestrators: (a) Rule 24 "Mandatory Evidence Artifacts" table had only 5 of 11 auditable sub-skills; (b) at least 8 critical invocation points lacked `MANDATORY — NON-NEGOTIABLE` markers; (c) the Camada 3 CI audit (`audit-execution-integrity.sh`) did not exist.

## Hypothesis tested

Expanding Rule 24's evidence table to 11 entries, adding `MANDATORY — NON-NEGOTIABLE` markers to all 8 critical points, promoting the `x-pr-watch-ci` contract to a first-class Rule 45, creating `scripts/audit-bypass-flags.sh` to detect `--no-ci-watch` / `--skip-*` outside `## Recovery` blocks, and adding `verify-story-completion.sh` Camada 2 Stop-hook checks would close the enforcement gap. **Confirmed**: Rule 45 published; `audit-bypass-flags.sh` delivered; Rule 24 expanded; orchestrators retrofitted with MANDATORY markers; Camada 2 Stop-hook extended.

## Decisions taken (with why)

1. **Rule 45 as first-class rule** (not just embedded in `x-pr-watch-ci/SKILL.md`) — orchestrators need a Rule reference to cite; embedded rules are invisible to Camada 1.
2. **`audit-bypass-flags.sh`** — scans SKILL.md files for `--no-ci-watch`/`--skip-*` outside `## Recovery` blocks; catches regression at CI time.
3. **8 stable exit codes in Rule 45** (SUCCESS/CI_PENDING_PROCEED/CI_FAILED/TIMEOUT/PR_ALREADY_MERGED/NO_CI_CONFIGURED/PR_CLOSED/PR_NOT_FOUND) — changing semantics of any code is MAJOR version bump.

## Alternatives rejected (with why)

- **Inline ad-hoc checks in each orchestrator** — does not close the enforcement gap; operators would still omit the check silently.
- **Treating Copilot timeout as CI failure** — operational absence (Copilot not yet present) is different from CI failure; `CI_PENDING_PROCEED` handles this case.

## Reusable patterns produced

- **`rule45-ci-watch-first-class-rule`**: `x-pr-watch-ci` contract formalized as Rule 45; 8 named exit codes; SemVer contract on code changes.
- **`audit-bypass-flags-script`**: `audit-bypass-flags.sh` detects `--no-ci-watch` / `--skip-*` outside `## Recovery`; exit 1 on violation.
- **`mandatory-evidence-table-11-entries`**: Rule 24 artifacts table with 11 entries covering all mandatory sub-skills.

## Anti-patterns observed

- **CI watch silent skip** — `x-pr-watch-ci` declared in SKILL.md but not marked MANDATORY; subagents skip it without triggering any enforcement.

## Links

- Epic: `ai/epics/epic-0057-rule24-execution-integrity-extension/epic-0057.md`
- ADRs: (none recorded)
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0057-rule24-execution-integrity-extension/reports/`
