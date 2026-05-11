---
name: x-review-pr
description: "Tech Lead holistic review with 45-point checklist; produces GO/NO-GO verdict."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, AskUserQuestion, Skill
argument-hint: "[PR-number or STORY-ID] [--no-auto-remediation] [--interactive] [--resume-review <pr>]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Review PR (Tech Lead Review — slim — ADR-0012)

## Purpose

Execute a senior-level holistic review with a `{review_max_score}`-point rubric. This is the standalone version of Phase 6 from `x-implement-story`. The Tech Lead reviews the consolidated PR diff for cross-file consistency and overall quality.

## Triggers

- `/x-review-pr` — review current branch against main
- `/x-review-pr NNN` — review PR #NNN
- `/x-review-pr STORY-ID` — review by story ID
- `/x-review-pr --resume-review <pr> --interactive` — resume an exhausted-retry gate session

## Prerequisites

- Code must be committed
- Branch should have changes relative to main
- Ideally, specialist reviews (`/x-review-codebase`) have already been run

## Parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| Positional | No | current branch | `PR-NNN`, `story-XXXX-YYYY`, or empty (current branch vs main) |
| `--no-auto-remediation` | No | `false` | Skip auto-remediation agents on NO-GO; route directly to Step 8.4 |
| `--interactive` | No | `false` | Enable the Step 8.4 `AskUserQuestion` 3-option gate (default: HALT non-interactively) |
| `--resume-review <pr>` | No | — | Resume gate from `plans/review/<pr>/state.json` |

## Output Contract

| Artifact | Path | Produced By |
|----------|------|-------------|
| Tech Lead report | `ai/epics/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md` | Phase 1 (Step 4) |
| Updated dashboard | `ai/epics/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md` | Phase 2 (Step 5; cumulative — RULE-006) |
| Updated remediation tracker | `ai/epics/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md` | Phase 3 (Step 6) |
| Frontmatter on Tech Lead report | YAML block per `governance/schemas/review-frontmatter-1.0.json` | Phase 5 (MANDATORY — Rule 24 §Camada-1) |
| Gate state (opt-in) | `plans/review/<pr-number>/state.json` | Step 8.4 FIX-PR slot only |

## CRITICAL EXECUTION RULE

**5 phases (Rule 25 REGRA-001, EPIC-0055).** Phase 5 (frontmatter emission) is **NON-NEGOTIABLE** per Rule 24 §Camada-1 — absent frontmatter aborts with `REVIEW_FRONTMATTER_INVALID` (no fallback).

## Workflow Overview

```text
Phase 0: CONTEXT      -> Idempotency check + diff detect + template detect (Steps 0–3)
Phase 1: REVIEW       -> Execute 45-point rubric, run tests + coverage + smoke (Step 4)
Phase 2: VERDICT      -> Compile GO/NO-GO; update consolidated dashboard (Steps 5, 7)
Phase 3: REMEDIATION  -> Iterative auto-remediation cycles (max 2; then gate); update tracker (Steps 6, 8)
Phase 4: APPROVAL     -> Final approval, console summary, FINAL phase gate
Phase 5: FRONTMATTER  -> Emit YAML frontmatter conforming to schema (MANDATORY)
```

Each phase opens with `TaskCreate` + `--mode pre` gate and closes with `TaskUpdate(completed)` + POST/FINAL gate. Phase 3 iterates sub-tasks per cycle (max 3 per Rule 20 with `addBlockedBy`).

## {review_max_score}-Point Rubric

| Section                  | Points | What it checks                                                      |
| ------------------------ | ------ | ------------------------------------------------------------------- |
| A. Code Hygiene          | 8      | Unused imports/vars, dead code, warnings, method signatures, magic  |
| B. Naming                | 4      | Intention-revealing, no disinformation, meaningful distinctions      |
| C. Functions             | 5      | Single responsibility, size <= 25 lines, max 4 params, no flags     |
| D. Vertical Formatting   | 4      | Blank lines between concepts, Newspaper Rule, class size <= 250     |
| E. Design                | 3      | Law of Demeter, CQS, DRY                                           |
| F. Error Handling        | 3      | Rich exceptions, no null returns, no generic catch                  |
| G. Architecture          | 5      | SRP, DIP, architecture layer boundaries (per project rules), follows plan |
| H. Framework & Infra     | 4      | DI, externalized config, native-compatible, observability           |
| I. Tests & Execution     | 6      | ALL tests pass, coverage >= 95%/90%, smoke tests pass, test quality |
| J. Security & Production | 1      | Sensitive data protected, thread-safe                               |
| K. TDD Process           | 5      | Test-first commits, Double-Loop TDD, TPP progression, atomic cycles |
{review_conditional_rubric}

## Decision Criteria

| Condition                              | Decision        |
| -------------------------------------- | --------------- |
| >= {review_go_threshold}/{review_max_score} + zero issues | GO              |
| < {review_go_threshold}/{review_max_score} OR any issue   | NO-GO           |
| ANY test failure (unit, integration, or smoke) | NO-GO (automatic, overrides score) |
| Coverage below 95% line OR 90% branch  | NO-GO (automatic, overrides score — **absolute gate per Rule 05 RULE-005-01; pre-existing deficits are NOT an excuse**) |

## Phases 0–5

The detailed inline protocol for each phase (Step 0 idempotency `mtime` check; Step 1 PR vs STORY detect with `gh pr view`; Step 2 KP reads; Step 3 template detect with RULE-012 fallback; Step 4 full 45-point execution including `{{TEST_COMMAND}}` + `{{COVERAGE_COMMAND}}` + `{{SMOKE_COMMAND}}` with automatic NO-GO rules; Step 5 cumulative dashboard update; Step 6 remediation tracker update; Step 7 console summary box; Step 8 auto-remediation by classification with `general-purpose` Agent dispatch and 2-cycle cap; Step 8.4 Rule 20 interactive gate with 3-option `AskUserQuestion` loop and `REVIEW_FIX_LOOP_EXCEEDED` guard-rail; Phase 5 YAML frontmatter schema with decision consolidation rule) lives in [`references/full-protocol.md`](references/full-protocol.md):

- **Phase 0 — Context & idempotency** (§Phase 0, Steps 0–3): pre-check via `mtime(report) >= mtime(latest commit)` (skip to Step 5 on reuse); detect PR vs STORY context; read 4 knowledge packs (`coding-conventions`, `architecture-principles`, `05-quality-gates`, `testing-philosophy`); template gate (`_TEMPLATE-TECH-LEAD-REVIEW.md` per RULE-012 fallback); persist `interactiveMode` to `execution-state.json`.
- **Phase 1 — Execute review** (§Phase 1, Step 4): list all modified files; full diff; per-file 45-point checklist with cross-file consistency focus; mandatory `{{TEST_COMMAND}}` (any failure → automatic NO-GO); mandatory `{{COVERAGE_COMMAND}}` (≥95% line / ≥90% branch absolute gate per Rule 05); conditional `{{SMOKE_COMMAND}}` (when `testing.smoke_tests == true`); cross-check vs specialist reports if present.
- **Phase 2 — Verdict & dashboard** (§Phase 2, Steps 5, 7): compile GO/NO-GO; update cumulative dashboard (replace `--/{review_max_score} | Pending` placeholder with actual Tech Lead Score; append new Round to Review History); console summary box with Test Execution Results sub-section.
- **Phase 3 — Remediation loop** (§Phase 3, Steps 6, 8): iterative; max 2 auto-remediation cycles via `Agent(general-purpose)` dispatch by classification (`TEST_FAILURE` / `COVERAGE_GAP` / `CODE_QUALITY`); cycles chained via `addBlockedBy`; update remediation tracker FIXED vs OPEN. **Step 8.4 Exhausted-Retry Gate** (Rule 20): non-interactive default HALT; interactive 3-option `AskUserQuestion` menu (Proceed / Fix-PR / Abort) with `gateAttempts < 3` loop and `REVIEW_FIX_LOOP_EXCEEDED` guard-rail.
- **Phase 4 — Final approval & report** (§Phase 4): FINAL phase gate with `--expected-artifacts` covering all 3 review artifacts.
- **Phase 5 — Emit Frontmatter** (§Phase 5, MANDATORY — Rule 24 §Camada-1): pre-gate, assemble body, prepend YAML frontmatter conforming to `review-frontmatter-1.0.json` schema (`schema-version: "1.0"`, `generated-by: x-review-pr@<sha>`, `decision` GO/NO-GO/GO-WITH-RESERVATIONS, `score-max: 55`, `checklist.passed`/`total: 45`/`failed-sections`); validate via `audit-review-frontmatter.sh` → `REVIEW_FRONTMATTER_INVALID` on non-zero. No fallback.

## State File (opt-in)

Written only when the operator selects **FIX-PR** (slot 2) in Step 8.4. Enables resume via `--resume-review <pr>`. Path: `plans/review/<pr-number>/state.json`. Schema version `"1.0"` with fields `phase`, `lastPhaseCompletedAt`, `lastGateDecision`, `fixAttempts[]`, `schemaVersion`. Full JSON schema in [full-protocol §State File](references/full-protocol.md).

## Error Envelope

> Canonical orchestrator-wide error codes (`COMMIT_FAILED`, fail-open vs fail-closed conventions) live in [`_shared/error-handling-orchestrator.md`](../_shared/error-handling-orchestrator.md). Review-specific patterns delegate to [`_shared/error-handling-review.md`](../_shared/error-handling-review.md) (empty input, RULE-012 fallback, specialist failure, idempotency). Rows below are skill-specific.

| Code / Scenario | Action |
|------|---------|
| `REVIEW_REMEDIATION_EXHAUSTED` | Operator selected ABORT in Step 8.4 gate |
| `REVIEW_FIX_LOOP_EXCEEDED` | 3 consecutive PROCEED or FIX-PR attempts without converging to GO |
| `GATE_SCHEMA_INVALID` | State file at `plans/review/<pr>/state.json` fails Rule 20 schema validation |
| `REVIEW_FRONTMATTER_INVALID` | Phase 5 `audit-review-frontmatter.sh` returned exit ≠ 0 — no fallback (Rule 24 §Camada-1) |
| No diff exists between branches | Abort: "No changes detected between current branch and base. Nothing to review." |
| Template `_TEMPLATE-TECH-LEAD-REVIEW.md` missing | Log warning, use inline format as fallback (RULE-012). Skip dashboard and remediation updates. |
| Test/Coverage/Smoke failure | Automatic NO-GO regardless of rubric score; record failures in report |
| State file missing on `--resume-review` | Start gate fresh; emit warning |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-implement-story` | called-by | Produces the same artifact as Phase 6 |
| `x-review-codebase` | reads | Reads specialist review reports for cross-validation |
| `x-review-codebase` | complements | `/x-review-codebase` = breadth (7 specialists), `/x-review-pr` = depth (1 Tech Lead) |
| `x-fix-pr` | calls (Step 8.4 FIX-PR slot) | Rule 20 interactive gate slot for fix-and-retry |
| `x-internal-verify-phase-gates` | calls (every phase) | Rule 25 Invariant 4 gate enforcement |
| `_TEMPLATE-TECH-LEAD-REVIEW.md` | reads | Required output format; RULE-012 fallback to inline |
| `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` | updates | Cumulative across rounds (RULE-006) |
| `_TEMPLATE-REVIEW-REMEDIATION.md` | updates | FIXED status tracking after Tech Lead review |
| `audit-review-frontmatter.sh` | calls (Phase 5) | Mandatory CI/runtime validation |

## Full Protocol

Minimum viable contract above. Detailed phase-by-phase workflow (Step 0 idempotency `mtime` algorithm; Step 4 with full bash blocks for tests/coverage/smoke; Step 5 dashboard cumulative-merge algorithm; Step 6 remediation FIXED-vs-OPEN logic; Step 7 console summary template; Step 8 auto-remediation Agent dispatch by classification with full prompts; Step 8.4 gate loop pseudocode with `gateAttempts` state machine and `REVIEW_FIX_LOOP_EXCEEDED` guard-rail; Phase 5 frontmatter YAML schema with `generated-by` regex contract; state file JSON schema v1.0 with all fields) lives in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).

{review_conditional_criteria}
