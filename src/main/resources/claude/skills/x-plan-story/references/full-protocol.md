<!--
Returns to [slim body](../SKILL.md) after reading the required phase.
TASK_PROPOSAL format and consolidation rules: see `planning-guide.md`.
-->

# x-plan-story — Full Protocol

## Phase 0 — Input Resolution

### 0.1 Parse Story Argument

Extract `XXXX` and `YYYY` from `story-XXXX-YYYY`. If argument does not match → abort with format error.

### 0.2 Resolve Epic Directory

Glob: `ai/epics/epic-XXXX` or `ai/epics/epic-XXXX-*`. If both exist, prefer exact match. No match → abort.

### 0.3 Resolve Paths

| Path | Pattern |
|------|---------|
| Story file | `<EPIC_DIR>/story-XXXX-YYYY.md` |
| Epic file | `<EPIC_DIR>/epic-XXXX.md` |
| Implementation map | `<EPIC_DIR>/IMPLEMENTATION-MAP.md` |
| Output dir | `<EPIC_DIR>/plans/` |
| **Story Plan (PRIMARY)** | **`<EPIC_DIR>/plans/plan-story-XXXX-YYYY.md`** |
| Tasks file | `<EPIC_DIR>/plans/tasks-story-XXXX-YYYY.md` |
| Planning report | `<EPIC_DIR>/plans/planning-report-story-XXXX-YYYY.md` |
| DoR checklist | `<EPIC_DIR>/plans/dor-story-XXXX-YYYY.md` |

`mkdir -p <EPIC_DIR>/plans` before writing.

### 0.4 Staleness Check (RULE-002)

| Condition | Action | Log |
|-----------|--------|-----|
| Tasks file absent | Generate new | `"Generating story plan for {id}"` |
| `mtime(story) > mtime(tasks)` | Regenerate | `"Regenerating stale story plan"` |
| `mtime(story) <= mtime(tasks)` | Reuse | `"Reusing existing story plan from {date}"` |
| `--force` flag | Always regenerate | `"Force-regenerating story plan"` |

If reusing: skip to Phase 5 (DoR only). Do NOT invoke subagents.

### 0.5 Verify Story File

`test -f ai/epics/epic-XXXX/story-XXXX-YYYY.md` → NOT_FOUND aborts.

---

## Phase 0b — Schema Version Detection

1. Read `ai/epics/epic-XXXX/execution-state.json` via `SchemaVersionResolver`.
2. `planningSchemaVersion == "2.0"` → v2 path (run Phases 4a-4c after Phase 4).
3. Absent / `"1.0"` / malformed → v1 path (standard flow through Phase 5).

---

## Phase 1 — Context Gathering

Read story file (title, description, acceptance criteria, data contracts, dependencies, sub-tasks, non-functional requirements), epic file (cross-cutting rules, DoR/DoD), implementation map (phase assignment, dependency graph), and existing plan artifacts (arch, test, impl, security) as optional context.

Missing files → log WARNING and continue without that context.

---

## Phase 2 — Parallel Planning

### Template Detection (before dispatching)

```bash
test -f .claude/templates/_TEMPLATE-TASK-BREAKDOWN.md && echo "TB_AVAILABLE" || echo "TB_MISSING"
test -f .claude/templates/_TEMPLATE-STORY-PLANNING-REPORT.md && echo "SPR_AVAILABLE" || echo "SPR_MISSING"
test -f .claude/templates/_TEMPLATE-DOR-CHECKLIST.md && echo "DOR_AVAILABLE" || echo "DOR_MISSING"
```

### Subagent Context Scope

Each subagent receives the story file content, relevant context from Phase 1, and instructions to produce TASK_PROPOSAL entries per [`planning-guide.md`](planning-guide.md).

| Agent | Model | Scope |
|-------|-------|-------|
| Architect | Opus | Architecture decisions, layer design, component structure, ADRs |
| QA Engineer | Sonnet | Test strategies, AT/UT scenarios, coverage requirements |
| Security Engineer | Sonnet | Threat model, OWASP items, sensitive data handling |
| Tech Lead | Sonnet | Code quality, SOLID, refactoring, complexity limits |
| Product Owner | Sonnet | Business value, acceptance criteria, DoD alignment |

---

## Phase 3 — Consolidation

Apply deterministic merge rules from [`planning-guide.md §Consolidation Rules`](planning-guide.md):
1. Group proposals by layer and component overlap.
2. Majority-vote on duplicate proposals (same component, same type → keep highest-voted).
3. Preserve all unique proposals.
4. Topological sort by declared dependencies.
5. Assign sequential `TASK-XXXX-YYYY-NNN` IDs.

---

## Phase 4 — Artifact Generation

1. Write `tasks-story-XXXX-YYYY.md` from consolidated task list.
2. Write `planning-report-story-XXXX-YYYY.md` with per-agent summary and conflict resolution log.
3. Commit via `Skill(skill: "x-commit-planning", ...)` unless `--no-commit`.

### Phase 4a-4c (v2 only — `planningSchemaVersion == "2.0"`)

**Phase 4a — Task files:** For each TASK-XXXX-YYYY-NNN, emit `task-TASK-XXXX-YYYY-NNN.md` with I/O contract, testability, dependencies.

**Phase 4b — Parallel task plans:** Invoke `x-plan-task` per task in parallel (batch size ≤ 4):
```
Agent(subagent_type: "general-purpose", model: "sonnet", description: "x-plan-task for {TASK-ID}",
      prompt: "Invoke x-plan-task via Skill(skill: 'x-plan-task', args: '--task-file ai/epics/epic-XXXX/plans/task-{TASK-ID}.md')")
```

**Phase 4c — Task map:** Generate `task-implementation-map-STORY-XXXX-YYYY.md` with topological sort + parallelism analysis via `x-evaluate-parallelism`.

**Phase 4d — Story Plan (PRIMARY artifact):** Generate `plan-story-XXXX-YYYY.md` using `_TEMPLATE-STORY-PLAN.md` and the Architect/QA output blocks from Phase 2.

Idempotency: if `mtime(story) <= mtime(plan-story)` and `--force` absent → log `"Reusing existing story plan"` and skip. Regenerate if stale or `--force`.

| Section | Source | Required |
|---------|--------|---------|
| 1. Visão Geral | Architect `NARRATIVE_OVERVIEW` (3-6 paragraphs, plain language) | Yes |
| 2.2-2.4 Artefatos | Architect `ARTIFACT_IMPACT` (path, action, current state, change description, reason per file) | Yes |
| 2.5 Diagrama | Architect `COMPONENT_DIAGRAM` (Mermaid NEW/MOD/DEL markers) | Yes |
| 3.1 Gherkin | QA `GHERKIN_SCENARIOS` (≥4 categories: Degenerate, Happy, Error, Security) | Yes |
| 3.2 Testes Impactados | QA `EXISTING_TESTS_IMPACT` (modify/delete/new-regression tables; explicit "none" if no impact) | Yes |
| 3.3 TDD Cycles | QA `TDD_CYCLES` (TPP order, plain-language RED/GREEN/REFACTOR per cycle) | Yes |
| 4. Tasks | Consolidated CODE_TASKS + mandatory review tasks (TL/SEC/QA) + doc task | Yes |
| 5. Critérios de Conclusão | Template verbatim — how to mark tasks and story as Concluída | Yes |
| 6. Riscos | Consolidated risk matrix from all agents | Yes |

Mandatory review/doc tasks (Seção 4.2-4.3) MUST always be present — never omit even if story seems low-risk.

Stage `plan-story-XXXX-YYYY.md` for the Step P4 batch commit.

---

## Phase 5 — DoR Validation

**12 checks (v1 and v2):**

| # | Check | Required |
|---|-------|---------|
| 1 | Story file exists | Yes |
| 2 | Title and description present | Yes |
| 3 | Acceptance criteria (≥1 Gherkin scenario) | Yes |
| 4 | Data contracts defined (or N/A documented) | Yes |
| 5 | Dependencies listed (or none) | Yes |
| 6 | Non-functional requirements documented (or N/A) | Yes |
| 7 | Tasks breakdown generated | Yes |
| 8 | Each task has DoD criteria | Yes |
| 9 | Architecture layer assignments consistent | Yes |
| 10 | No circular dependencies | Yes |
| 11 | Effort estimates present | Yes |
| 12 | Planning report saved | Yes |

**v2 adds per-task READY checks:** each `task-TASK-*.md` passes schema validation and has `plan-task-TASK-*.md` sibling.

DoR not met → emit checklist diff + `DOR_NOT_MET` (non-blocking warning; story can proceed to implementation with known gaps).

---

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-implement-story` | caller | Delegates Phase 1 planning to this skill |
| `x-plan-task` | calls (Phase 4b, v2) | Per-task implementation plan generation |
| `x-evaluate-parallelism` | calls (Phase 4c, v2) | File-overlap collision detection |
| `x-commit-planning` | calls (Phase 4) | Atomic commit of planning artifacts |
| `x-internal-ensure-epic-branch` | calls (Step P2) | Idempotent epic branch creation |
| `x-manage-worktrees` | calls (Step P1) | Detect worktree context (advisory) |
