# Review-wide error matrix

> Canonical error and degradation conventions shared by skills that perform code/PR/codebase review:
> `x-review-pr`, `x-review-codebase`, `x-review-qa`, `x-review-security`, `x-review-api`, `x-review-database`, `x-audit-code`, `x-audit-dependencies`, and the rest of the `x-review-*` / `x-audit-*` family.
>
> Skill-specific rows (rubric thresholds, framework-specific checks) live in each skill's own `## Error Handling`. The rows below apply to **every** review/audit skill.

## Empty-input handling

| Scenario | Action |
|----------|--------|
| No diff between current branch and base (`x-review-pr`) | Abort with `"No changes detected between current branch and base. Nothing to review."` |
| No source files in scope (`x-audit-*`) | Abort with `"No source files found for audit"` |
| No specialist reports found (`x-review-pr`) | Proceed with Tech Lead review only; note absence in dashboard |
| No findings (audit / scan) | Report success (exit `0`) with `"No findings"`; do NOT manufacture issues |

## Template fallback (RULE-012 — Graceful template fallback)

Review and audit skills depend on markdown templates under `.claude/templates/`. When a template is missing:

| Severity | Action |
|----------|--------|
| **Required** template missing (e.g., the skill's own primary report template) | Log `WARNING: template <name> missing — using inline format`. Continue with a minimal inline format. Skip secondary outputs (dashboard, remediation) that require additional templates. |
| **Optional** template missing (dashboard, remediation tracking) | Log warning, skip the optional output, continue. |

The skill MUST NEVER abort solely because a template is missing. Inline fallback is always available as a degraded mode.

## Specialist subagent failure (parallel review skills only)

| Failure mode | Action |
|-------------|--------|
| Specialist returns invalid output (missing SCORE or STATUS) | Mark specialist as `FAILED`, score `0`, continue with remaining specialists |
| Specialist subagent crashes or times out | Mark dimension as `"Unable to audit"` with the error captured; continue |
| **All** specialists return FAILED | Overall status `REJECTED`; report saved with `0` scores; operator must investigate before retry |

Do not wait for human input. The review must produce a report even when partial — partial information is still actionable.

## Resume / state-file handling

| Scenario | Action |
|----------|--------|
| `execution-state.json` missing on `--resume` | Start gate fresh; emit WARNING `STATE_FILE_MISSING_RESTART` |
| State file schema invalid | Emit `GATE_SCHEMA_INVALID`; start gate fresh (do NOT silently consume corrupt state) |
| State file present and valid | Resume from recorded checkpoint |

## Build/test outcomes that override the rubric

The following outcomes are **automatic NO-GO** regardless of rubric scores. They cannot be down-weighted:

| Outcome | Recorded as |
|---------|-------------|
| Compilation or build failure | CRITICAL finding; deduct full Framework & Infra section |
| Test suite failure (unit/integration) | NO-GO; record every failing test in the report |
| Coverage below project threshold | NO-GO; record coverage gap as CRITICAL finding |
| Smoke test failure | NO-GO; record failing smoke tests as CRITICAL findings |

These rules are project-wide: a green rubric does not absolve a red build.

## Idempotency contract

Review skills MUST be idempotent against the same code state:

- Compare report mtime vs. last commit timestamp (`stat` + `git log -1 --format=%ct`).
- If report is newer than the last code change, **skip re-review** and emit `IDEMPOTENT_SKIP` with the existing report path.
- Re-review on demand with explicit `--force` flag.
