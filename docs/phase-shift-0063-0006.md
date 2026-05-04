# Phase Shift — story-0063-0006: x-implement-epic Phase 4.5 (Epic-Level Review)

> **Story:** story-0063-0006
> **Epic:** EPIC-0063 (Local-First Preflight Gates)
> **Introduced by:** story-0063-0006 — x-implement-epic Phase 4.5

## Summary

This document formalises **Phase 4.5** inside `x-implement-epic`. The gate invokes
`x-review-pr` on the epic branch as a whole after Phase 4 (Integrity Gate) but
**before Phase 5 (Final PR: epic/XXXX → develop)**.

Before this change, the epic-level tech-lead review was performed on the final PR after
it was already opened — meaning the review could not block the PR from being created
with an incomplete or deficient change-set.

---

## Gate Location in x-implement-epic Phases

```
x-implement-epic phases
│
├── Phase 0  — Argument normalisation (x-internal-normalize-args)
├── Phase 1  — Epic load + plan (x-internal-build-epic-plan)
├── Phase 2  — Epic branch ensure (x-internal-ensure-epic-branch)
├── Phase 3  — Sequential story loop (x-implement-story per story)
├── Phase 4  — Integrity gate (x-internal-verify-epic-integrity + x-internal-write-report)
│
├── Phase 4.5 [NEW] Epic-Level Review Gate  ◄──── gate introduced here
│           ├── Invoke x-review-pr on epic/XXXX branch HEAD
│           └── Write evidence → plans/epic-XXXX/reports/epic-review.md
│
└── Phase 5  — Final PR epic/XXXX → develop (x-merge-branches + x-create-pr)
              (PR body now INCLUDES evidence of Phase 4.5 review)
```

### Phase 4.5 — Epic-Level Review Gate (Detail)

| Step | Action | Output artifact |
| :--- | :--- | :--- |
| 4.5.1 | Checkout `epic/XXXX` branch HEAD (already current in sequential mode) | — |
| 4.5.2 | Invoke `x-review-pr` with `--scope=epic` (full 45-point checklist on aggregated diff) | `plans/epic-XXXX/reports/epic-review.md` |
| 4.5.3 | Assert `plans/epic-XXXX/reports/epic-review.md` exists on disk | Gate blocks if absent |
| 4.5.4 | If verdict is NO-GO and `--interactive` is active → gate menu (PROCEED / FIX-PR / ABORT) | `GATE_FIX_LOOP_EXCEEDED` after 3 fix cycles |
| 4.5.5 | If verdict is NO-GO and non-interactive (default, EPIC-0061) → fail fast with `PHASE_45_GATE_FAILED` | — |

---

## Evidence Artifact: `epic-review.md`

```
plans/epic-XXXX/reports/epic-review.md
```

The file is produced by `x-review-pr` with `--scope=epic` and contains:

- The aggregated diff summary (all stories merged into `epic/XXXX`)
- The 45-point checklist result
- The GO / NO-GO verdict
- Any required remediation items (if NO-GO)

This file is a **mandatory evidence artifact** under Rule 24 §Mandatory Evidence Artifacts
for any PR from `epic/XXXX` to `develop`. Absence fails `scripts/audit-execution-integrity.sh`
with `EIE_EVIDENCE_MISSING`.

---

## Why Epic-Level Review Must Precede the Final PR

1. **Aggregated diff context (Rule 24 §Camada 1):**
   Individual story reviews each cover a story-scoped diff. The epic-level review covers
   the *combined* diff across all stories — cross-story consistency, duplicate patterns,
   and architectural coherence are only visible at the aggregate level.

2. **PR body evidence chain (Rule 27 §Surface 11):**
   The `## Orchestrator Evidence` section in the epic PR body must reference `epic-review.md`.
   Creating the PR before the review means the evidence cannot be referenced.

3. **NO-GO handling before PR creation (Rule 20 §Interactive Gates):**
   A NO-GO verdict at Phase 4.5 triggers the FIX-PR slot of the gate menu (max 3 cycles).
   Routing fixes through `x-fix-epic-pr` before the PR is created is cleaner than
   amending or closing an already-open PR.

---

## Bypass Flag

```
--skip-review
```

When `--skip-review` is passed to `x-implement-epic`, Phase 4.5 is skipped entirely.
In that case, `plans/epic-XXXX/reports/epic-review.md` is NOT created, and the final PR
body will note the review was skipped.

**Constraint (Rule 24 / Rule 27):** `--skip-review` is only permitted inside a
`## Recovery` block of the calling skill's SKILL.md. Any usage outside Recovery is
caught by `scripts/audit-bypass-flags.sh` (exit 1 `BYPASS_FLAG_VIOLATION`).

---

## Backward Compatibility

- Epics with `flowVersion: "1"` (pre-EPIC-0049 legacy) are unaffected: Phase 4.5 is
  a no-op when `taskTracking.enabled = false`.
- Epics already completed before this gate is adopted: `epic-review.md` is absent by
  definition; such epics are grandfathered via `governance/baselines/execution-integrity-baseline.txt`.
- The `x-review-pr` skill accepts `--scope=epic` as a new flag; story-scoped invocations
  continue using the existing default (`--scope=story`).

---

## Evidence Artifacts

| Artifact | Path pattern |
| :--- | :--- |
| Epic-level tech-lead review | `plans/epic-XXXX/reports/epic-review.md` |
| Verify envelope | `plans/epic-XXXX/reports/verify-envelope-epic-XXXX.json` |

The `epic-review.md` artifact is validated by `scripts/audit-execution-integrity.sh`
(Camada 3). The Stop hook `verify-story-completion.sh` (Camada 2) is extended in the
epic-complete path to check for this file as well.

---

## References

- Rule 24 — Execution Integrity (`.claude/rules/24-execution-integrity.md`)
- Rule 27 — Zero-Bypass Lifecycle (`.claude/rules/27-zero-bypass-lifecycle.md`)
- Rule 20 — Interactive Gates Convention (`.claude/rules/20-interactive-gates.md`)
- `x-review-pr` skill (`.claude/skills/x-review-pr/`)
- `x-implement-epic` skill (`.claude/skills/x-implement-epic/`)
- `x-internal-verify-epic-integrity` skill (`.claude/skills/x-internal-verify-epic-integrity/`)
- ADR-0016 — Zero-Bypass Lifecycle Convention (`docs/adr/ADR-0016-zero-bypass-lifecycle.md`)
- story-0063-0005 — Phase Shift for `x-implement-story` (companion document)
