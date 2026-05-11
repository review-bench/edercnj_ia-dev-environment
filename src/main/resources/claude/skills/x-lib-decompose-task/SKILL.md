---
name: x-lib-decompose-task
description: "Decomposes an implementation plan into TDD tasks (Red/Green/Refactor) from test scenarios."
user-invocable: false
allowed-tools: Read, Write, Grep, Glob
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Task Decomposer Library (slim — ADR-0012)

## Purpose

Decomposes an implementation plan into granular tasks. When a test plan exists (from `x-plan-tests`), derives tasks from test scenarios using TDD structure (RED/GREEN/REFACTOR) with a per-task `Parallel` flag. Falls back to the Layer Task Catalog (G1-G7) when no test plan is available, where tasks are additionally assigned to parallelism groups. Each task is assigned a model tier (Junior/Mid/Senior) and context budget.

## When Called

| Caller Skill | Phase | Context |
|-------------|-------|---------|
| `x-implement-story` | Phase 1C | After the Architect produces the plan, BEFORE implementation |
| (standalone) | N/A | When breaking down a plan into implementable tasks |

## Inputs

| Input | Path | Required |
|-------|------|----------|
| Architect's plan | `ai/epics/epic-XXXX/plans/plan-story-XXXX-YYYY.md` | Yes |
| Story requirements | Story file | Yes |
| Test plan (from `x-plan-tests`) | `ai/epics/epic-XXXX/plans/tests-story-XXXX-YYYY.md` | No |

## Output Contract

| Artifact | Path |
|----------|------|
| Task breakdown | `ai/epics/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` |
| Structure (template available) | Header / Summary / Dependency Graph / Tasks Table / Escalation Notes |
| Structure (template fallback — RULE-012) | TDD inline format (Step 2A) or Layer Catalog inline format (Step 2B) |

Idempotent on staleness: when `mtime(tasks) >= mtime(story)`, returns the existing breakdown without regeneration (RULE-002).

## Workflow Overview

```text
0.   IDEMPOTENCY    -> mtime(story) vs mtime(tasks); skip generation if fresh
0.5. ARCH_CONTEXT   -> Read .claude/knowledge/architecture.md + layer-templates.md
1.   READ_STORY     -> Read Architect plan + story + _TEMPLATE-TASK-BREAKDOWN.md (RULE-007)
1.5. MODE_DETECT    -> Test plan with TPP markers? → 2A (TDD); else → 2B (Layer)
2A.  TDD_TASKS      -> One task per UT/IT/AT scenario; RED/GREEN/REFACTOR + tier + budget + parallel + depends-on
2B.  LAYER_TASKS    -> Layer Task Catalog G1-G7; one task per active layer
3B.  CATALOG_APPLY  -> (layer mode) fixed Junior/Mid/Senior tier per layer
4B.  TIER_TUNE      -> (layer mode) escalate complex domain logic Mid→Senior
5.   WRITE_OUTPUT   -> ai/epics/epic-XXXX/plans/tasks-story-XXXX-YYYY.md
```

Each step's detailed bash, TDD field tables, parallelism detection rules, layer-task catalog (21 task types × tier × budget × group), G1-G7 dependency graph, context budget sizing, review tier assignment, and escalation rules live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 0** (§Step 0): mtime comparison; fresh-plan reuse; staleness override.
- **Step 0.5** (§Step 0.5): KP reads (`.claude/knowledge/architecture.md` for layer structure + dependency direction; `.claude/knowledge/layer-templates.md` for code templates per layer).
- **Step 1** (§Step 1): RULE-007 template read with RULE-012 graceful fallback (inline format when `_TEMPLATE-TASK-BREAKDOWN.md` absent).
- **Step 1.5** (§Step 1.5): 3-state mode detection (TPP-marker present → TDD; markers absent but file present → layer-based with warning; file absent → layer-based with warning).
- **Step 2A** (§Step 2A): per-scenario task generation with 10-field structure; parallelism detection rules (different layers + no shared state + no dep on concurrent output); ordering rules (TPP level ascending, inner-before-outer layers, AT after all related UTs); task-type classification (UT/AT/IT with dependency rules); full TDD output format with RED/GREEN/REFACTOR/Layer Components block.
- **Steps 2B/3B/4B** (§Step 2B/3B/4B): layer-based fallback procedure (identify affected layers → apply catalog → tune tier for complex domain logic).
- **Step 5** (§Step 5): output path computation; template-vs-inline format selection.
- **Layer Task Catalog G1-G7** (§Fallback: Layer Task Catalog): 21-row catalog with Architecture Layer → Tier (Junior/Mid/Senior) → Budget (S/M/L) → Group (G1..G7) mapping.
- **Layer Dependency Graph** (§Fallback: Layer Dependency Graph): G1 FOUNDATION → G2 CONTRACTS → G3 OUTBOUND → G4 ORCHESTRATION → G5 INBOUND → G6 OBSERVABILITY → G7 TESTS.
- **Context Budget Sizes** (§Context Budget Sizes): S=100-200 / M=250-400 / L=500-800 lines, with content inclusion rules.
- **Review Tier Assignment** (§Review Tier Assignment): per-engineer task-type mapping; Tech Lead tier = story max.
- **Escalation Rules** (§Escalation Rules): Junior→Mid→Senior→manual; target <15% escalation rate.

## Error Handling

| Scenario | Action |
|----------|--------|
| Architect's plan not found | Abort: "Plan file not found at expected path. Run planning phase first." |
| Story requirements file not found | Abort: "Story file not found. Verify story ID and epic directory." |
| Template `_TEMPLATE-TASK-BREAKDOWN.md` missing | Log warning, use inline format as fallback (RULE-012) |
| Test plan has no structured TPP markers | Fall back to layer-based decomposition (G1-G7) with warning |
| Escalation threshold exceeded (>15% tasks escalate) | Flag in output report for manual review |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-implement-story` | called-by | Invoked during Phase 1C |
| `x-plan-tests` | reads | Consumes test plan (Phase 1B output) when available |
| `x-implement-story` Phase 2 | produces-for | Output consumed by group-based or TDD-based implementation |

- Works with any layered architecture (hexagonal, clean, onion) — layer names derived from project rules.
- When test plan present: generates TDD tasks with RED/GREEN/REFACTOR structure.
- When test plan absent: generates layer-based tasks using G1-G7 catalog (backward compatible).
- **Idempotency (RULE-002)**: Checks `ai/epics/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` existence and staleness before regenerating. Second invocation with unchanged story reuses existing breakdown.
- **Template reference (RULE-007)**: Reads `.claude/templates/_TEMPLATE-TASK-BREAKDOWN.md` for standardized output format.
- **Graceful fallback (RULE-012)**: Functions without template (pre-EPIC-0024 projects). Logs warning and uses inline format when template absent.

## Knowledge Pack References

| Pack | File | Purpose |
|------|------|---------|
| architecture | `.claude/knowledge/architecture.md` | Layer structure, dependency direction, package organization |
| architecture | `.claude/knowledge/layer-templates.md` | Layer catalog with package locations, code templates, checklist per layer |

## Full Protocol

Minimum viable contract above. Detailed Steps 0–5 with bash, TDD task field tables, parallelism detection, ordering rules, full Layer Task Catalog (21 task types × tier × budget × G1-G7), Layer Dependency Graph, Context Budget Sizes, Review Tier Assignment, and Escalation Rules live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
