# Story Completion Report — story-0063-0018

**Story:** story-0063-0018 — Hooks --self-check Contract
**Epic:** EPIC-0063
**Completed:** 2026-04-28
**Status:** DONE

## Deliverables

| Artifact | Path | Status |
|----------|------|--------|
| Audit script (source of truth) | `java/src/main/resources/targets/claude/scripts/audit-hooks-self-check.sh` | Created |
| Audit script (generated copy) | `.claude/scripts/audit-hooks-self-check.sh` | Created |
| Test suite (5 tests, all GREEN) | `src/test/shell/audit_hooks_self_check_test.sh` | 5/5 PASS |
| Specialist review | `ai/epics/epic-0063-local-first-preflight-gates/plans/review-story-story-0063-0018.md` | GO |
| Tech-lead review | `ai/epics/epic-0063-local-first-preflight-gates/plans/techlead-review-story-story-0063-0018.md` | GO |
| Verify envelope | `ai/epics/epic-0063-local-first-preflight-gates/reports/verify-envelope-story-0063-0018.json` | passed=true |
| Dependency audit | `ai/epics/epic-0063-local-first-preflight-gates/reports/dependency-audit-story-0063-0018.md` | OK |

## Hooks Updated with --self-check

12 hooks registered in settings.json now implement `--self-check`:

| Hook | Had --self-check before | Status after |
|------|------------------------|--------------|
| enforce-no-bypass-flags.sh | YES | unchanged |
| enforce-phase-sequence.sh | no | ADDED |
| enforce-preflight-gates.sh | no | ADDED |
| post-compile-check.sh | no | ADDED |
| stage-telemetry.sh | no | ADDED |
| telemetry-posttool.sh | no | ADDED |
| telemetry-pretool.sh | no | ADDED |
| telemetry-session.sh | no | ADDED |
| telemetry-stop.sh | no | ADDED |
| telemetry-subagent.sh | no | ADDED |
| verify-phase-gates.sh | no | ADDED |
| verify-story-completion.sh | no | ADDED |

## Summary

The `audit-hooks-self-check.sh` script reads `.claude/settings.json` to discover all registered
hook scripts and validates each one: file exists, is executable, references `--self-check`,
and exits 0 when called with `--self-check`. All 12 registered hooks now satisfy the
Rule 26 §Camada 0 Hook Contract.
