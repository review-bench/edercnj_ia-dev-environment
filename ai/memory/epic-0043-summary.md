---
epic-id: EPIC-0043
slug: interactive-gates-standardization
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [ux, orchestration, gates, interactive, standardization]
capabilities-affected: []
rules-affected: [Rule 20]
adrs-referenced: [ADR-0010]

patterns-introduced:
  - three-option-gate-menu
  - fix-pr-loop-back
antipatterns-rejected:
  - open-ended-halt-text-in-orchestrators
  - opt-in-interactive-flags-patchwork

dependencies-of: [EPIC-0042]
dependencies-for: [EPIC-0045, EPIC-0061]
---
# Memory: EPIC-0043 — Interactive Gates Standardization

## Why this epic existed

Orchestrators paused with open-ended text messages at decision gates ("Skill pausada. Após merge da PR, rode..."), leaving operators to memorize resume commands. No structured `PROCEED/FIX-PR/ABORT` pattern existed. Some skills used `AskUserQuestion` under ad-hoc opt-in flags (`--interactive`, `--manual-task-approval`). The inconsistency made automation and human interaction both fragile.

## Hypothesis tested

A **formal 3-option gate menu** (PROCEED / FIX-PR / ABORT) as the standard with `FIX-PR` looping back after invoking `x-pr-fix`/`x-pr-fix-epic`, combined with ADR-0010 + Rule 20, would standardize all interactive decision points. **Confirmed** — note: EPIC-0061 later flipped the default to **non-interactive**, making `--interactive` opt-in. EPIC-0043 established the menu structure; EPIC-0061 made it opt-in.

## Decisions taken (with why)

1. **Fixed 3-option menu** (PROCEED / FIX-PR / ABORT): structured, auditable; `FIX-PR` invokes fix skill via Rule 13 Pattern 1 and loops back (max 3 cycles, `GATE_FIX_LOOP_EXCEEDED`).
2. **Menu as default behavior** at time of EPIC-0043 — later changed to opt-in by EPIC-0061.
3. **`--non-interactive` replaces patchwork opt-in flags** — `--interactive`, `--manual-task-approval`, `--manual-batch-approval` deprecated.
4. **ADR-0010 + Rule 20** — formal normative backing; CI audit rejects HALT text without `AskUserQuestion`.

## Alternatives rejected (with why)

- **Open-ended HALT text** — operator must memorize resume command; no structured choice.
- **Different menus per skill** — inconsistency increases cognitive load.

## Reusable patterns produced

- **`three-option-gate-menu`**: PROCEED/FIX-PR/ABORT; FIX-PR loops back; max 3 fix cycles.
- **`fix-pr-loop-back`**: invoke fix skill via Rule 13 Pattern 1, then re-present same menu.

## Anti-patterns observed

- **Open-ended halt text in orchestrators** — cognitive burden; no structured automation path.
- **Patchwork of opt-in interactive flags** — `--interactive`/`--manual-task-approval`/`--manual-batch-approval` all diverge; consolidate into one.

## Links

- Epic: `ai/epics/epic-0043-interactive-gates-standardization/epic-0043.md`
- ADRs: `docs/adr/ADR-0010-interactive-gates-convention.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0043-interactive-gates-standardization/reports/`
