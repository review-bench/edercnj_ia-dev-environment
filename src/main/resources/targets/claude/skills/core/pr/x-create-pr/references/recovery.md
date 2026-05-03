# x-create-pr — Recovery (Render Skill Fallback)

> **Scope:** EPIC-0066 / story-0066-0005 — fail-open contract for `x-internal-render-pr-body`.
> **Referenced by:** `SKILL.md` `## Recovery` section (link only — full detail lives here per ADR-0007).

## When the Fallback Triggers

When `x-internal-render-pr-body` returns exit ≠ 0 in Phase 3, this skill **falls back to the legacy inline body generator** to ensure PR creation never aborts. The fallback body does NOT contain the `<!-- template-version: 1.0 -->` marker — `audit-pr-template.sh` (story-0066-0007) will report `PR_TEMPLATE_VIOLATION` for PRs created via this fallback path. This is intentional fail-open behavior (RULE-004) — the audit gate downstream will block the merge if the marker is missing.

## Fallback Inline Body (legacy generator — preserved verbatim from pre-EPIC-0066)

```markdown
## Summary

{description derived from commits or --description flag}

## Task Details

| Field | Value |
|-------|-------|
| Task ID | TASK-XXXX-YYYY-NNN |
| Story | story-XXXX-YYYY |
| Epic | epic-XXXX |
| Task Plan | `ai/epics/epic-XXXX/tasks/task-plan-XXXX-YYYY-NNN.md` |

## Changes

{list each commit in the branch using: git log --oneline develop..HEAD}

## Review Checklist

- [ ] Tests pass locally
- [ ] Coverage thresholds met (>=95% line, >=90% branch)
- [ ] TDD commits present (RED -> GREEN -> REFACTOR)
- [ ] No TODO/FIXME/HACK comments
- [ ] Conventional Commits format followed
```

The fallback body lacks `## Orchestrator Evidence` — Phase 3.5 dedup logic detects the absence and injects the section manually (legacy path). The final body therefore always contains exactly one `## Orchestrator Evidence` section.

## Exit Code → Fallback Action Matrix

| Render exit code | Cause | Fallback action |
| :--- | :--- | :--- |
| 1 (`INVALID_KIND`) | Bug in x-create-pr — should never happen | Use fallback + emit WARN |
| 2 (`OPERATIONAL_ERROR`) | Template missing, write permission denied | Use fallback + emit WARN |
| 3 (`INVALID_SCOPE`) | story-id derivation failed | Use fallback + emit WARN |

The WARN message includes the exit code so the operator can diagnose the underlying issue (e.g., re-run `mvn process-resources` if the template is missing from `.claude/templates/`).

## RULE-004 Fail-Open Contract

The fallback preserves the pre-EPIC-0066 behavior integrally. The WARN signals that the template was not applied without aborting PR creation. This follows RULE-004 (fail-open) and ensures EPIC-0066 does not break in-flight flows if the render skill has a bug.

The downstream audit (`audit-pr-template.sh` — story-0066-0007) is responsible for blocking the merge of any PR whose body lacks the `<!-- template-version: 1.0 -->` marker — this is the **detective** layer that complements the **preventive** render skill. Together, they form the EPIC-0066 PR-body integrity contract.
