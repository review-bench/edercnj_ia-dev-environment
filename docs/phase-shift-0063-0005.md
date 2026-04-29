# Phase Shift — story-0063-0005: Reviews ANTES do PR (Phase 2.7 Gate)

> **Story:** story-0063-0005
> **Epic:** EPIC-0063 (Local-First Preflight Gates)
> **Introduced by:** story-0063-0005 — x-story-implement Phase Shift

## Summary

This document formalises a new execution gate — **Phase 2.7** — inside `x-story-implement`.
The gate ensures that specialist reviews (`x-review`) and tech-lead review (`x-review-pr`)
are invoked **after all tasks complete but BEFORE the story-level PR is created**.

Before this change, reviews were optionally invoked after the PR was already open.
That ordering meant that the PR body could not reference completed review artifacts,
weakening the evidence chain required by Rule 24 and Rule 27.

---

## Gate Location in Phase 2 Loop

```
Phase 2 — Task Execution Loop
│
├── 2.1  Load task list from plan-story-XXXX-YYYY.md
├── 2.2  For each task → x-task-implement (TDD cycle)
│        ↑ loop until all tasks DONE
│
├── 2.7  [NEW] Pre-PR Review Gate  ◄──── gate introduced here
│        ├── Invoke x-review        (specialist reviews)
│        └── Invoke x-review-pr     (tech-lead 45-point checklist)
│
└── 2.8  Create story-level PR via x-pr-create
         (PR body now INCLUDES evidence of 2.7 reviews)
```

### Phase 2.7 — Pre-PR Review Gate (Detail)

| Step | Action | Output artifact |
| :--- | :--- | :--- |
| 2.7.1 | Invoke `x-review` on the story branch (parallel specialist wave) | `plans/epic-XXXX/plans/review-story-STORY-ID.md` |
| 2.7.2 | Invoke `x-review-pr` on the story branch (tech-lead 45-point) | `plans/epic-XXXX/plans/techlead-review-story-STORY-ID.md` |
| 2.7.3 | Assert both artifacts exist on disk | Gate blocks if either is missing |
| 2.7.4 | If `x-review-pr` verdict is NO-GO → surface to operator (interactive) or fail-fast (non-interactive) | — |

The gate is a synchronous blocker: step 2.8 (`x-pr-create`) MUST NOT be reached unless
phase 2.7 returns `passed=true`.

---

## Why Reviews Must Precede the PR

1. **PR body evidence chain (Rule 27 §Surface 11):**
   The `## Orchestrator Evidence` section in every story PR body must reference the review
   artifacts. If reviews run after PR creation, the PR body is created before the evidence
   exists — the chain is broken.

2. **CI audit integrity (Rule 24 §Camada 3):**
   `scripts/audit-execution-integrity.sh` verifies `review-story-STORY-ID.md` and
   `techlead-review-story-STORY-ID.md` exist for every merged story. Running them after
   the PR creates a race condition where the CI check fires before the artifacts land.

3. **NO-GO handling (Rule 20 §Interactive Gates):**
   If `x-review-pr` emits NO-GO, the operator must decide (PROCEED / FIX-PR / ABORT).
   In non-interactive mode (default, EPIC-0061), the orchestrator fails fast. Both
   paths require the review to complete before the PR is opened — otherwise `FIX-PR`
   would re-open the PR unnecessarily.

---

## Bypass Flag

```
--skip-review
```

When `--skip-review` is passed to `x-story-implement`, Phase 2.7 is skipped entirely.
The flag is inherited by the orchestrator from the caller; `x-epic-implement` propagates
it unchanged.

**Constraint (Rule 24 / Rule 27):** `--skip-review` is only permitted inside a
`## Recovery` block of the calling skill's SKILL.md. Any usage outside Recovery is
caught by `scripts/audit-bypass-flags.sh` (exit 1 `BYPASS_FLAG_VIOLATION`).

---

## Backward Compatibility

- Epics with `flowVersion: "1"` (pre-EPIC-0049 legacy) are unaffected: Phase 2.7 is
  a no-op when `taskTracking.enabled = false`.
- Epics already in-flight at the time this gate is adopted: existing DONE stories are
  grandfathered via `governance/baselines/execution-integrity-baseline.txt` (Rule 24
  §Baseline).
- The `--skip-review` flag existed before this gate; its semantics are unchanged.
  The gate simply moves the mandatory invocation point earlier.

---

## Evidence Artifacts

| Artifact | Path pattern |
| :--- | :--- |
| Specialist review | `plans/epic-XXXX/plans/review-story-STORY-ID.md` |
| Tech-lead review | `plans/epic-XXXX/plans/techlead-review-story-STORY-ID.md` |
| Verify envelope | `plans/epic-XXXX/reports/verify-envelope-STORY-ID.json` |

These artifacts are validated by `scripts/audit-execution-integrity.sh` (Camada 3) and
checked by `.claude/hooks/verify-story-completion.sh` (Camada 2, Stop hook).

---

## References

- Rule 24 — Execution Integrity (`.claude/rules/24-execution-integrity.md`)
- Rule 27 — Zero-Bypass Lifecycle (`.claude/rules/27-zero-bypass-lifecycle.md`)
- Rule 20 — Interactive Gates Convention (`.claude/rules/20-interactive-gates.md`)
- `x-review` skill (`.claude/skills/x-review/`)
- `x-review-pr` skill (`.claude/skills/x-review-pr/`)
- ADR-0016 — Zero-Bypass Lifecycle Convention (`docs/adr/ADR-0016-zero-bypass-lifecycle.md`)
