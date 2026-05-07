---
name: kp-lifecycle-zero-bypass
description: "Full reference for Rule 27 Zero-Bypass Lifecycle: 13 orchestration surfaces with evidence paths, 4 enforcement layers, 2 legitimate exception paths, and audit exit codes."
requires-capabilities: []
---

# Knowledge Pack: Zero-Bypass Lifecycle (Rule 27 — Full Reference)

## 13 Orchestration Surfaces

| # | Surface | Required orchestrator | Evidence artifact |
| :--- | :--- | :--- | :--- |
| 01 | Story implementation | `x-implement-story` | `ai/epics/epic-XXXX/reports/story-completion-report-STORY-ID.md` |
| 02 | Task implementation | `x-implement-task` | Git log on `feat/task-*` branch (Camada 4) |
| 03 | Story verification gate | `x-internal-verify-story` | `ai/epics/epic-XXXX/reports/verify-envelope-STORY-ID.json` |
| 04 | Specialist review | `x-review-codebase` | `ai/epics/epic-XXXX/plans/review-story-STORY-ID.md` |
| 05 | Tech-lead review | `x-review-pr` | `ai/epics/epic-XXXX/plans/techlead-review-story-STORY-ID.md` |
| 06 | PR CI-watch | `x-watch-pr-ci` | `.claude/state/pr-watch-{PR_NUMBER}.json` |
| 07 | Architecture plan | `x-plan-architecture` | `ai/epics/epic-XXXX/plans/arch-story-STORY-ID.md` |
| 08 | Dependency audit | `x-audit-dependencies` | `ai/epics/epic-XXXX/reports/dependency-audit-STORY-ID.md` |
| 09 | Phase-1 planning wave | `x-internal-build-story-plan` | 6 artifacts under `ai/epics/epic-XXXX/plans/` |
| 10 | Epic integrity gate | `x-internal-verify-epic-integrity` | `ai/epics/epic-XXXX/reports/verify-envelope-epic-XXXX.json` |
| 11 | Story-level PR body | `x-create-pr` (structured body) | `## Orchestrator Evidence` section in PR description |
| 12 | Telemetry stream | `telemetry-phase.sh` markers | `ai/epics/epic-XXXX/telemetry/events.ndjson` (phase.start/end pairs) |
| 13 | Dependency policy gate | `x-validate-dependency-policy` (conditional: `dependencies.policy.enabled=true`) | `ai/epics/epic-XXXX/reports/dep-policy-validation-report-STORY-ID.md` |

## Enforcement Layers

### Camada 1 — Normative

Rule 27 loaded every conversation. CLAUDE.md carries "ZERO-BYPASS LIFECYCLE — INEGOCIÁVEL" block. Orchestrator SKILL.md phrases mandatory invocations as **MANDATORY TOOL CALL**.

### Camada 2 — Runtime Stop Hook

`.claude/hooks/verify-story-completion.sh` fires on every `Stop` event:
- Checks `.claude/state/pr-watch-{PR}.json` (Rule 45 contract).
- Checks planning and report artifacts exist for recently-opened story PRs.
- Emits WARNING (exit 2) when evidence is absent.

### Camada 3 — CI Audit

`scripts/audit-execution-integrity.sh` runs on every PR to `develop` or `epic/*`:
- Verifies all 13 surfaces evidence artifacts for each merged story.
- Fails with `EIE_EVIDENCE_MISSING` when any mandatory artifact is absent.
- `scripts/audit-bypass-flags.sh` scans for `--no-ci-watch` or `--skip-*` outside `## Recovery` blocks.

### Camada 4 — Observability

Telemetry NDJSON provides continuous audit trail. `/x-analyze-telemetry` produces Gantt timelines and phase aggregates. Absence of `phase.start`/`phase.end` pairs for `x-implement-story` phases is a Camada 4 signal.

## Legitimate Exception Paths (2 Only)

### Exception 1: `--legacy-flow` for pre-EPIC-0049 epics (Rule 19)

Epics with `flowVersion: "1"` (or absent). `--legacy-flow` disables Rule 21 branch routing. No new epics may use `--legacy-flow` after EPIC-0059 merges.

### Exception 2: Documented hotfixes on `hotfix/*` branches (Rule 09)

A single-file, single-commit critical fix MAY bypass the full orchestrator when:
1. The fix is single-file, single-commit with no architectural changes.
2. The `hotfix/*` PR body contains `## Hotfix Bypass Justification` with written rationale.
3. Tech lead approves via `x-review-pr` with explicit hotfix exception marker.
4. Bypass recorded in `governance/baselines/execution-integrity-baseline.txt` with `# hotfix-exception` comment before PR is merged.

**No other bypass path exists.** `CLAUDE_SKIP_AUDIT=1`, `CLAUDE_NO_ENFORCE=1`, or any undocumented env var is blocked by `enforce-no-bypass-flags.sh`.

## Audit Script Exit Codes

| Exit | Code | Condition |
| :--- | :--- | :--- |
| 0 | `OK` | All merged stories have complete evidence; no bypass flags detected. |
| 1 | `EIE_EVIDENCE_MISSING` | At least one surface artifact is absent. |
| 2 | `EIE_BASELINE_CORRUPT` | Baseline file malformed. |
| 3 | `EIE_INVALID_EXEMPTION` | `audit-exempt` marker missing a reason. |
