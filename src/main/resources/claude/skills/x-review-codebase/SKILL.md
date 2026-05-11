---
name: x-review-codebase
model: sonnet
description: "Parallel code review with specialist engineers (Security, QA, Perf, DB, Obs, DevOps)."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, TaskCreate, TaskUpdate
argument-hint: "[STORY-ID or --scope reviewer1,reviewer2] [--no-auto-fix-story]"
context-budget: heavy
requires-capabilities: []
fragment-slots: [{ slot: review-specialist, ordering: fragment-order }]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Specialist Review (Orchestrator — slim — ADR-0012)

## Purpose

Perform parallel specialist code reviews across multiple engineering dimensions by delegating to individual review skills (`/x-review-qa`, `/x-review-performance`, `/x-review-database`, etc.), consolidating findings into a scored dashboard, and optionally generating correction stories for critical findings.

## When to Use

- `/x-review-codebase` — review current branch
- `/x-review-codebase STORY-ID` — review specific story
- `/x-review-codebase --scope security,qa` — run only specific reviewers
- `/x-review-codebase --no-auto-fix-story` — disable auto-generation of correction story (Phase 4)

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `STORY-ID` | No | Story to review (pattern: `story-XXXX-YYYY`). Defaults to current-branch detection. |
| `--scope <list>` | No | Comma-separated list of specialists to run (filter against the Specialist Reference Table). |
| `--no-auto-fix-story` | No | Disable automatic correction-story generation in Phase 4; fall back to `AskUserQuestion` prompt (EPIC-0042). |
| `--interactive` | No | Set `interactiveMode=interactive` on `execution-state.json`; default is `non-interactive` (Rule 20 / EPIC-0068). |

## Output Contract

| Artifact | Path | Produced By |
|----------|------|-------------|
| Per-specialist report | `ai/epics/epic-XXXX/reviews/review-{specialist}-story-XXXX-YYYY.md` | Phase 3c (Write) |
| Consolidated dashboard | `ai/epics/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md` | Phase 3d (cumulative — RULE-006) |
| Remediation tracker | `ai/epics/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md` | Phase 3e |
| Threat-model update | `results/security/threat-model.md` | Phase 3f (incremental, STRIDE) |
| Correction story (optional) | `ai/epics/epic-XXXX/reviews/correction-story-XXXX-YYYY.md` | Phase 4 (conditional) |
| Consolidated review artifact (frontmatter) | `ai/epics/epic-XXXX/reviews/review-story-XXXX-YYYY.md` | Phase 5 (MANDATORY — Rule 24 §Camada-1) |

## CRITICAL EXECUTION RULE

**6 phases (0–5). ALL mandatory in default mode. Phase 5 (frontmatter emission) is **NON-NEGOTIABLE** per Rule 24 §Camada-1 — absent frontmatter fails CI audit with `EIE_EVIDENCE_MISSING`.**

After each phase: `>>> Phase N/5 completed. Proceeding to Phase N+1...`

## Workflow Overview

```
Phase 0: PRE-CHECK     -> Idempotency (skip if reports exist + code unchanged)
Phase 1: DETECT        -> Identify branch, diff, applicable specialists
Phase 2: REVIEW        -> Invoke N review skills in parallel (SINGLE batch message)
Phase 3: CONSOLIDATE   -> Collect reports, score, dashboard, remediation, STRIDE
Phase 4: STORY         -> If CRITICAL/HIGH/MEDIUM findings: generate correction story (auto for CRITICAL/HIGH; AskUserQuestion for MEDIUM-only)
Phase 5: FRONTMATTER   -> Emit YAML frontmatter (Rule 24 §Camada-1, mandatory)
```

## Specialist Reference Table

| Specialist | Skill | Max Score | Condition |
|------------|-------|-----------|-----------|
| QA | `/x-review-qa` | /36 | Always |
| Performance | `/x-review-performance` | /26 | Always |
| Database | `/x-review-database` | /40 | database != none |
| Observability | `/x-review-observability` | /18 | observability != none |
| DevOps | `/x-review-devops` | /20 | container != none |
| Data Modeling | `/x-review-data-modeling` | /20 | database != none AND architecture in [hexagonal, ddd, cqrs] |
| Security | `/x-review-security` | /30 | security frameworks configured |
| API | `/x-review-api` | /16 | REST interface present |
| Event | `/x-review-events` | /28 | event-driven or event interfaces |

> Each individual skill contains its own checklist, knowledge pack references, and scoring logic. The orchestrator does NOT duplicate these — it delegates entirely.

## Composition Pattern (RULE-007 — EPIC-0064)

Fragment-slot composition — only specialists relevant to active capabilities are included. See [`references/composition-architecture.md`](references/composition-architecture.md) for the canonical fragment table and contribution guide.

{{ #each fragments.review-specialist }}

## Phases 0–5

The detailed inline protocol for each phase (sub-step prompts, full Batch A/B/C invocation patterns, all `TaskCreate`/`TaskUpdate` markers, `<!-- TELEMETRY -->` hooks, dashboard/remediation/STRIDE schemas, Phase 5 frontmatter schema) lives in [`references/full-protocol.md`](references/full-protocol.md):

- **Phase 0 — Idempotency Pre-Check** (§Phase 0): extract story ID, derive epic dir, check report `mtime` vs `git log -1 --format=%ct HEAD`; on freshness, skip directly to Phase 3d (dashboard regeneration); persist `interactiveMode` to `execution-state.json` (EPIC-0068 — consumed by Stop hook `enforce-continuous-flow.sh`).
- **Phase 1 — Detect Context** (§Phase 1): extract story ID, get `git diff main --stat`, abort on no changes, apply the Specialist Reference Table conditions (filter further if `--scope`).
- **Phase 2 — Parallel Reviews** (§Phase 2): **CONTEXT ISOLATION** (specialists receive metadata only); PRE gate (Rule 25 Invariant 4); **Batch A** in a SINGLE assistant message bundling all `TaskCreate` + all `Skill(x-review-*)` sibling tool calls — only emit pairs for ACTIVE specialists; **Batch B** sibling `TaskUpdate` calls; **Batch C** wave POST gate (`--mode wave` with `--expected-tasks` and `--expected-artifacts`).
- **Phase 3 — Consolidation** (§Phase 3): 3a Collect & Score → 3b Issue Summary by severity → 3c Save per-specialist reports via `Write` → 3d Dashboard (template-gated; cumulative across rounds RULE-006) → 3e Remediation tracker (parse FAILED/PARTIAL items, init Open) → 3f Threat-model STRIDE update (severity-based auto-add) → 3g Console summary box (EPIC-0042).
- **Phase 4 — Story Generation for Findings** (§Phase 4): runs **only** if CRITICAL/HIGH/MEDIUM findings exist; 4a Check; 4b Auto-generate (DEFAULT — pause for `AskUserQuestion` only on CRITICAL Security findings or when `--no-auto-fix-story`); 4c Build correction story from template with one Gherkin scenario per finding, save to `correction-story-XXXX-YYYY.md`.
- **Phase 5 — Emit Frontmatter** (§Phase 5, MANDATORY): pre-gate (`--mode pre`), assemble body, prepend YAML frontmatter conforming to `governance/schemas/review-frontmatter-1.0.json` (decision consolidation rule: NO-GO > GO-WITH-RESERVATIONS > GO), validate via `audit-review-frontmatter.sh`, post-gate (`--mode post`). Absent frontmatter aborts with `REVIEW_FRONTMATTER_INVALID` — no fallback (Rule 24 §Camada-1).

## Error Envelope

> Canonical orchestrator-wide error codes (`COMMIT_FAILED`, fail-open vs fail-closed conventions, `--dry-run` semantics) live in [`_shared/error-handling-orchestrator.md`](../_shared/error-handling-orchestrator.md). Review-specific behavior delegates to [`_shared/error-handling-review.md`](../_shared/error-handling-review.md) for empty input / RULE-012 template fallback / specialist failure / idempotency. Rows below are skill-specific.

| Scenario | Action |
|----------|--------|
| No changes found relative to main | Abort with message: `No changes found relative to main.` |
| Skill returns invalid output (missing SCORE or STATUS) | Mark specialist as `FAILED`, score 0, continue with remaining specialists |
| Dashboard template not found | Log warning, skip dashboard generation, continue to next phase |
| Remediation template not found | Log warning, skip remediation tracking generation, continue |
| All specialists return FAILED | Overall status `REJECTED`, report saved with 0 scores |
| Phase 5 frontmatter audit exit ≠ 0 | Abort with `REVIEW_FRONTMATTER_INVALID` — no fallback (Rule 24 §Camada-1) |
| PRE gate fails (Rule 25 Invariant 4) | Exit 12; resolve stale `execution-state.json` or predecessor phase before retry |
| Wave POST gate fails (Phase 2 Batch C) | Surface failure and return — Phase 3 skipped until the broken specialist is resolved |

## Template Fallback

Templates referenced by this skill follow RULE-012. When a template file does not exist (e.g., pre-EPIC-0024 projects), the skill degrades gracefully:

- `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` — dashboard generation skipped
- `_TEMPLATE-REVIEW-REMEDIATION.md` — remediation tracking skipped

When templates are absent, dashboard/remediation are skipped (continuation, not abort).

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-implement-story` | called-by (Phase 4) | Produces the same artifacts as lifecycle Phase 4 |
| `x-review-pr` | followed by | Recommended flow: `/x-review-codebase` then fix criticals then `/x-review-pr` |
| `x-story-create` | reads format | Correction stories (Phase 4) follow the story template |
| `x-implement-task` | followed by | Correction stories can be picked up by `/x-implement-task` |
| `x-internal-verify-phase-gates` | calls (Phase 2 PRE/WAVE, Phase 5 PRE/POST) | Rule 25 Invariant 4 gate enforcement |
| `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` | reads | Dashboard format, cumulative across rounds (RULE-006) |
| `_TEMPLATE-REVIEW-REMEDIATION.md` | reads | Remediation tracking format |
| `PlanTemplatesAssembler` | depends on | Templates copied verbatim — not rendered by the engine |
| `audit-review-frontmatter.sh` | calls (Phase 5) | CI/runtime validation of YAML frontmatter |

## Full Protocol

Minimum viable contract above. Detailed phase-by-phase workflow (Batch A/B/C invocation patterns with all 9 specialist `Skill` + `TaskCreate` calls; PRE/WAVE/POST gate signatures; full standard review output format with PASSED/FAILED/PARTIAL rules; Phase 3a–3g consolidation steps with dashboard, remediation, STRIDE mapping and severity auto-add table; Phase 4a–4c story-generation flow with AskUserQuestion blocks and finding-to-Gherkin transform; Phase 5 frontmatter YAML schema with decision consolidation rule and validation command) lives in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
