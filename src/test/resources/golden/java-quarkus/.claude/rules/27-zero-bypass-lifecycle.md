# Rule 27 — Zero-Bypass Lifecycle

> **Related:** Rule 13 (Skill Invocation Protocol), Rule 22 (Skill Visibility), Rule 24 (Execution Integrity), Rule 25 (Task Hierarchy & Phase Gate Contract), Rule 45 (CI-Watch Integrity).
> **Introduced by:** EPIC-0059 (Zero-Bypass Lifecycle Enforcement).
> **ADR:** ADR-0016 (Zero-Bypass Lifecycle Convention).

## Purpose

Rule 24 (Execution Integrity) governs **sub-skill invocations**: it ensures that every
`Skill(skill: "...", args: "...")` declaration inside a SKILL.md body is executed as a
real tool call — not simulated, not summarized, not skipped without a flag.

Rule 27 governs the **root orchestrator invocation**: it ensures that the **operator
(human or CI agent) does not bypass the orchestrator entirely**. The distinction is
critical:

- Rule 24 assumes the orchestrator is running and prevents inline execution of its
  declared sub-skills.
- Rule 27 prevents the operator from bypassing the orchestrator before it even starts.

**Concrete bypass examples that Rule 27 blocks:**

| Bypass pattern | Why it is prohibited |
| :--- | :--- |
| Implementing a story via direct `git commit` + `gh pr create` without `x-story-implement` | Skips 6 Phase-1 planning artifacts, 4 Phase-3 evidence artifacts, telemetry, and the review gate |
| Manually committing implementation files to an epic branch without a task PR | Skips CI-watch, specialist reviews, and coverage validation |
| Creating a PR with `gh pr create` and body not containing "## Orchestrator Evidence" | Evidence of orchestrator execution is absent — CI audit blocks merge |
| Implementing a task by hand and marking it DONE in `execution-state.json` | Circumvents the TDD loop, coverage gate, and diff-based reviews |
| Rerunning only Phase 3 (`x-internal-story-verify`) on a story that had no Phase 1/2 | Evidence chain is incomplete — verify envelope cannot prove planning |

## Non-bypass Contract

Every story implementation MUST be traceable to an invocation of `x-story-implement`.
Every task implementation MUST be traceable to an invocation of `x-task-implement`.
No PR targeting `epic/*` or `develop` may be merged without all required evidence
artifacts present on disk and referenced in the PR body.

The 12 surfaces (orchestration points) where bypass is explicitly catalogued and
prohibited are:

| # | Surface | Required orchestrator | Evidence artifact |
| :--- | :--- | :--- | :--- |
| 01 | Story implementation | `x-story-implement` | `ai/epics/epic-XXXX/reports/story-completion-report-STORY-ID.md` |
| 02 | Task implementation | `x-task-implement` | Git log on `feat/task-*` branch (Camada 4) |
| 03 | Story verification gate | `x-internal-story-verify` | `ai/epics/epic-XXXX/reports/verify-envelope-STORY-ID.json` |
| 04 | Specialist review | `x-review` | `ai/epics/epic-XXXX/plans/review-story-STORY-ID.md` |
| 05 | Tech-lead review | `x-review-pr` | `ai/epics/epic-XXXX/plans/techlead-review-story-STORY-ID.md` |
| 06 | PR CI-watch | `x-pr-watch-ci` | `.claude/state/pr-watch-{PR_NUMBER}.json` |
| 07 | Architecture plan | `x-arch-plan` | `ai/epics/epic-XXXX/plans/arch-story-STORY-ID.md` |
| 08 | Dependency audit | `x-dependency-audit` | `ai/epics/epic-XXXX/reports/dependency-audit-STORY-ID.md` |
| 09 | Phase-1 planning wave | `x-internal-story-build-plan` | 6 artifacts under `ai/epics/epic-XXXX/plans/` |
| 10 | Epic integrity gate | `x-internal-epic-integrity-gate` | `ai/epics/epic-XXXX/reports/verify-envelope-epic-XXXX.json` |
| 11 | Story-level PR body | `x-pr-create` (structured body) | `## Orchestrator Evidence` section in PR description |
| 12 | Telemetry stream | `telemetry-phase.sh` markers | `ai/epics/epic-XXXX/telemetry/events.ndjson` (phase.start + phase.end pairs) |

## Enforcement Layers

Four defense-in-depth layers — a violation caught by any layer fails the lifecycle.

### Camada 1 — Normative (this rule + CLAUDE.md + assertive SKILL.md language)

This rule is loaded into every conversation (rules are always active).
`CLAUDE.md` carries a top-level **"ZERO-BYPASS LIFECYCLE — INEGOCIÁVEL"** block (added
by EPIC-0059, story-0059-0010) that makes the contract visible to every LLM turn.
Orchestrator SKILL.md files phrase every mandatory invocation as
**MANDATORY TOOL CALL** (Rule 24 §Camada-1 — same phrasing, complementary enforcement).

### Camada 2 — Runtime Stop hook

`.claude/hooks/verify-story-completion.sh` fires on every `Stop` event.
- Checks for `.claude/state/pr-watch-{PR}.json` (Rule 45 contract).
- Checks that planning and report artifacts exist for any story recently opened as PR.
- Emits a visible WARNING on stderr and exits with code 2 when evidence is absent,
  which Claude Code surfaces as a blocking notification.

### Camada 3 — CI audit

`scripts/audit-execution-integrity.sh` runs on every PR to `develop` or `epic/*`.
- Verifies all 12 surface evidence artifacts for each merged story.
- Fails with `EIE_EVIDENCE_MISSING` when any mandatory artifact is absent.
- Extended in EPIC-0059 (story-0059-0003) to include bypass-flag checks:
  `scripts/audit-bypass-flags.sh` scans every merged SKILL.md change for
  `--no-ci-watch` or `--skip-*` used outside `## Recovery` blocks.

### Camada 4 — Observability

Telemetry NDJSON under `ai/epics/epic-XXXX/telemetry/events.ndjson` provides a
continuous audit trail. The `/x-telemetry-analyze` skill consumes it to produce
Gantt timelines and phase aggregates. Absence of `phase.start`/`phase.end` pairs
for `x-story-implement` phases is a Camada 4 signal that the orchestrator was
not invoked — flagged as a WARNING in the telemetry report.

## Exceptions

Two and only two legitimate paths exist to bypass orchestrator enforcement:

### Exception 1: `--legacy-flow` for pre-EPIC-0049 epics (Rule 19)

Epics with `flowVersion: "1"` (or absent) in `execution-state.json` were created
before the orchestrator model was introduced. For these epics, `--legacy-flow` on
`x-epic-implement` or `x-story-implement` disables Rule 21 branch routing AND
produces a backward-compatibility opt-out. No new epics may use `--legacy-flow`
after EPIC-0059 merges.

### Exception 2: Documented hotfixes on `hotfix/*` branches (Rule 09)

A critical production fix on a `hotfix/*` branch (Rule 09 §Hotfix Workflow) MAY
bypass the full story-implement orchestrator when:

1. The fix is a single-file, single-commit patch with no architectural changes.
2. The `hotfix/*` PR body contains the section `## Hotfix Bypass Justification` with
   a written rationale.
3. The tech lead approves via `x-review-pr` with an explicit hotfix exception marker.
4. The bypass is recorded in `governance/baselines/execution-integrity-baseline.txt` with a
   `# hotfix-exception` comment before the PR is merged.

No other bypass path exists. `CLAUDE_SKIP_AUDIT=1`, `CLAUDE_NO_ENFORCE=1`, or
any environment variable not documented in Rule 27 does NOT constitute a bypass
exception and will be blocked by the PreToolUse hook (`enforce-no-bypass-flags.sh`,
EPIC-0059, story-0059-0005).

## Forbidden

- Direct `git commit` + `gh pr create` for a story without invoking `x-story-implement`.
- Direct `git commit` + `gh pr create` for a task without invoking `x-task-implement`.
- Marking a story `COMPLETE` in `execution-state.json` without the evidence artifact set.
- Creating a PR with a body that lacks `## Orchestrator Evidence` (catches manual PRs).
- Merging a PR that fails `scripts/audit-execution-integrity.sh` (bypassing the CI gate).
- Using `--force-merge` or `--admin` merge overrides on `epic/*` or `develop` branches.
- Adding stories to `governance/baselines/execution-integrity-baseline.txt` after EPIC-0059 merges
  (baseline is immutable post-EPIC-0059 — use the hotfix exception path instead).
- Introducing a new bypass environment variable without a Rule 27 amendment and a
  SemVer MINOR bump.

## Audit

`scripts/audit-execution-integrity.sh` (extended in EPIC-0059 story-0059-0003) is
the primary CI gate for Rule 27. It verifies all 12 surfaces listed in the
Non-bypass Contract. Exit codes follow the Rule 26 §Standardized Exit Codes matrix:

| Exit | Code | Condition |
| :--- | :--- | :--- |
| 0 | `OK` | All merged stories have complete evidence; no bypass flags detected. |
| 1 | `EIE_EVIDENCE_MISSING` | At least one surface artifact is absent for a merged story. |
| 2 | `EIE_BASELINE_CORRUPT` | `governance/baselines/execution-integrity-baseline.txt` is malformed. |
| 3 | `EIE_INVALID_EXEMPTION` | `audit-exempt` marker is missing a reason. |

Self-check: `scripts/audit-execution-integrity.sh --self-check` MUST verify:
1. This rule file (`27-zero-bypass-lifecycle.md`) exists.
2. The Stop hook `verify-story-completion.sh` is registered in `settings.json`.
3. The baseline file `governance/baselines/execution-integrity-baseline.txt` is present.

Missing any of the three fails CI with `RULE_27_ENFORCEMENT_BROKEN`.

---

> **Catalogado em:** [`docs/audit-gates-catalog.md`](../../docs/audit-gates-catalog.md)
