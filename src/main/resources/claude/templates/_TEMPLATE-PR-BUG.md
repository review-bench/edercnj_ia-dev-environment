---
requires-capabilities: [governance.bug-lifecycle]
template-version: "1.0"
template-type: pr-bug
---

<!-- template-version: 1.0 -->

## Bug Fix Summary

**Bug ID:** {{BUG_ID}}
**Severity:** {{SEVERITY}}
**Scope:** {{SCOPE}}
**Story:** {{STORY_ID}} — {{STORY_KIND}}

### What Changed

{{CHANGE_DESCRIPTION}}

### Root Cause

{{ROOT_CAUSE}}

### Fix Approach

{{FIX_APPROACH}}

---

## Status Transition

| Before | After |
| :----- | :---- |
| {{STATUS_BEFORE}} | {{STATUS_AFTER}} |

> **Valid status transitions for PRs:**
> - `story-01-regression-test` PR: `Pendente` → `Em Investigação`
> - `story-02-fix` PR: `Em Investigação` → `Em Correção`
> - `story-02-fix` merge: `Em Correção` → `Concluída` (when regression test GREEN)
> - `story-03-doc-update` PR: documentation update (no status change)
> - `story-04-rollback-plan` PR: rollback plan (no status change)

---

## Review Checklist

- [ ] Regression test is RED before fix (story-01 merged first)
- [ ] Fix makes regression test GREEN (story-02)
- [ ] No new test failures introduced
- [ ] Coverage ≥ 95% line / ≥ 90% branch maintained
- [ ] `requires-capabilities: [governance.bug-lifecycle]` declared in all new files
- [ ] Blocked-By chain respected (story-02 merged after story-01)
- [ ] Refinement Verdict is `approved` in the parent bug.md
- [ ] No hardcoded credentials, tokens, or PII in the change

---

## Reproduction Verification

After merge, the following reproduction steps should NO LONGER reproduce the bug:

{{REPRODUCTION_STEPS_SUMMARY}}

---

## Orchestrator Evidence

<!-- Filled automatically by x-create-pr. Do not edit manually. -->

| Campo | Valor |
| :--- | :--- |
| Bug ID | {{BUG_ID}} |
| Story IDs | {{STORY_ID}} |
| Orchestrator Commit SHA | {{ORCHESTRATOR_SHA}} |
| Invocation Skill | x-implement-story |
| Phase 1 Artifacts | {{PHASE1_ARTIFACTS}} |
| Phase 3 Artifacts | {{PHASE3_ARTIFACTS}} |
