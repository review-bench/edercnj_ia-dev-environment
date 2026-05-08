# Task Breakdown — story-0069-0003

**Story:** Skill `/x-epic-refine` (multi-persona strategic dispatcher)
**Epic:** EPIC-0069

## File Footprint

```
write:
  - src/main/resources/targets/claude/skills/core/plan/x-epic-refine/SKILL.md
  - .claude/skills/x-epic-refine/SKILL.md
read:
  - .claude/rules/29-refinement-gate.md
  - .claude/agents/*.md (PO, Tech Lead, Architect, Security, QA, SRE/DevOps personas)
  - src/main/resources/targets/claude/knowledge/refinement/dimensions.md
regen: []
```

## Tasks

### TASK-0069-0003-001: Create SKILL.md at source-of-truth path

**Path:** `src/main/resources/targets/claude/skills/core/plan/x-epic-refine/SKILL.md`

**Content:**

- Frontmatter v3.0 (Rule 28): `name: x-epic-refine`, `model: sonnet`, `visibility: public`,
  `user-invocable: true`, `requires-capabilities: [governance.refinement-gate]`,
  `allowed-tools: [Read, Write, Edit, Skill, Agent, TaskCreate, TaskUpdate, Bash]`
- Global Output Policy block (English only, no filler)
- Triggers section: `/x-epic-refine epic-XXXX`, `--non-interactive`, `--dry-run`, `--legacy-refinement`
- Parameters table: `EPIC-ID` (positional), `--epic-id`, `--non-interactive`, `--dry-run`, `--legacy-refinement`
- Output Contract table: `epicId`, `verdict`, `verdictHash`, `blockers`, `refinedAt`, `artifactPath`
- Error Codes table: `EPIC_NOT_FOUND`, `EPIC_STATE_MISSING`, `PHASE_A_EMPTY`, `VERDICT_WRITE_FAILED`
- CRITICAL EXECUTION RULE block (4 phases A–D mandatory unless `--non-interactive`)

**Phase A — Parallel Specialist Analysis (strategic):**
  - Telemetry `phase.start` + `x-internal-phase-gate --mode pre`
  - `TaskCreate(subject: "epic-XXXX › Phase A - Specialist Analysis", ...)`
  - A.1: Resolve epic path (`ai/epics/epic-XXXX-*/epic-XXXX.md`) and `execution-state.json`; read `dimensions.md` KP (§Epic Dimensions)
  - A.2: Launch 5–6 sibling `Agent(subagent_type: "general-purpose", model: "sonnet", ...)` calls in ONE assistant message:
    - PO: `[problem, persona-broad, hypothesis, okrs]` — NO-GOs: hypothesis without measurable KPI; OKR without baseline+target+horizon; persona < 2 distinguishable roles
    - Tech Lead: `[feasibility, epic-dependencies]` — NO-GOs: circular epic dependencies; unresolvable technical blockers
    - Architect: `[strategic-alternatives, architectural-impact, out-of-scope]` — NO-GOs: zero alternatives; `out-of-scope` empty or < 3 explicit items (D4 preserved as Architect NO-GO)
    - Security: `[security-posture, compliance-triggers]` — NO-GOs: sensitive domain (PCI/LGPD/HIPAA) without declared compliance; cross-domain epic without threat-modeling scope
    - QA: `[quality-strategy, smoke-scope]` — NO-GOs: no "how do we know it shipped" criterion; no smoke strategy declared
    - SRE/DevOps [conditional: `flag.has_infra_capability`]: `[operational-impact, rollback-strategy]` — NO-GOs: production-touching epic without rollback plan
  - Each persona agent prompt: read epic markdown + `dimensions.md` §Epic Dimensions; return gap-report JSON `{persona, gaps:[{dimension, issue, question, severity:"question|noGo"}]}`
  - Telemetry `subagent.start/end` per persona (5–6 pairs)
  - Collect reports; separate `questions` (severity=question) vs `noGos` (severity=noGo); if ALL empty → exit `PHASE_A_EMPTY`
  - `TaskUpdate(completed)` + `x-internal-phase-gate --mode post` + `phase.end`

**Phase B — Consolidate & Operator Q&A (strategic):**
  - `<!-- phase-no-gate: Phase B is skipped entirely when --non-interactive -->` marker
  - Telemetry `phase.start`
  - Skip entirely when `--non-interactive`; set `answers = {}`
  - `TaskCreate(subject: "epic-XXXX › Phase B - Consolidate", ...)`
  - B.1: Dedup + group strategic questions by category: `Problema`, `Hipótese/OKRs`, `Alternativas`, `Segurança`, `Qualidade`, `Operações`
  - B.2: EXACTLY ONE `AskUserQuestion` call with grouped batch; no per-persona loops; skip B.2 if zero questions
  - `TaskUpdate(completed)` + `phase.end`

**Phase C — Parallel Refinement (strategic):**
  - Telemetry `phase.start` + `x-internal-phase-gate --mode pre`
  - `TaskCreate(subject: "epic-XXXX › Phase C - Refinement", ...)`
  - Re-dispatch 5–6 sibling `Agent(subagent_type: "general-purpose", model: "sonnet", ...)` in ONE assistant message, each with original gap-report + operator answers + prompt to produce revised section content
  - Telemetry `subagent.start/end` per persona (5–6 pairs)
  - Collect `proposedSections` per persona
  - `TaskUpdate(completed)` + `x-internal-phase-gate --mode post` + `phase.end`

**Phase D — Architect Consolidation:**
  - Telemetry `phase.start` + `x-internal-phase-gate --mode pre`
  - `TaskCreate(subject: "epic-XXXX › Phase D - Architect Consolidation", ...)`
  - ONE `Agent(subagent_type: "general-purpose", model: "opus", ...)` — Architect consolidator:
    - Merges persona contributions into epic markdown sections: §1.3 Escopo, §1.4 Fora do escopo, §6 Decisões Arquiteturais
    - Generates `## Refinement Verdict` block (canonical single block)
    - Applies merge via Edit tool on epic markdown
  - `Skill(skill: "x-internal-status-update", model: "haiku", args: "...")` [required] — dual-write `refinementVerdict` to `execution-state.json` with `scope: "epic"`, `dimensions: {po, techLead, architect, security, qa, sre?}`, `blockers`, `verdictHash`
  - `--dry-run`: phases A–D run but no Write/Edit/Skill calls for state or markdown
  - `--legacy-refinement`: skip all phases; emit WARNING; return `verdict.status="tbd"`
  - `TaskUpdate(completed)` (all N phase tasks) + `x-internal-phase-gate --mode final` + `phase.end`
  - Print `>>> Phase D completed. Refinement verdict written.`

**Accepted Criteria:**
- File exists at the source-of-truth path
- Frontmatter passes schema v3.0 (name, model, visibility, requires-capabilities all present)
- Phase A dispatches 5–6 sibling Agent() calls in a single assistant message
- Each persona prompt covers only its owned dimensions; NO-GOs are silent (not surfaced as questions)
- Phase B emits exactly one `AskUserQuestion` (skipped under `--non-interactive`)
- Phase C re-dispatches 5–6 sibling Agent() calls in a single assistant message
- Phase D uses `model: opus` for the Architect consolidator Agent()
- `x-internal-status-update` called with `scope: "epic"` in Phase D [required marker present]
- Telemetry phase.start/end pairs present for all 4 phases (A, B, C, D)
- Telemetry subagent.start/end pairs present per persona in Phase A and Phase C
- All Skill/Agent calls carry Rule 28 grammar markers (`[required]`, `[optional]`, or `[conditional: ...]`)
- `--dry-run` and `--legacy-refinement` flags handled

**File Footprint:**
- Write: `src/main/resources/targets/claude/skills/core/plan/x-epic-refine/SKILL.md`
- Read: `.claude/rules/29-refinement-gate.md`, `.claude/agents/*.md`, `dimensions.md` KP, `x-story-refine/SKILL.md` (reference for structural parity)

**Dependencies:** story-0069-0001 (dimensions.md KP + Rule 29 + capability yaml), story-0069-0002 (structural reference for 4-phase dispatcher pattern)

---

### TASK-0069-0003-002: Copy SKILL.md to .claude/skills/

**Path:** `.claude/skills/x-epic-refine/SKILL.md`

Copy byte-identical from the source-of-truth written in TASK-0069-0003-001. `.claude/` is a generated directory (gitignored) — the copy must be applied explicitly via Write tool.

**Accepted Criteria:**
- Directory `.claude/skills/x-epic-refine/` exists
- `.claude/skills/x-epic-refine/SKILL.md` is byte-identical to the source-of-truth file
- `diff src/main/resources/targets/claude/skills/core/plan/x-epic-refine/SKILL.md .claude/skills/x-epic-refine/SKILL.md` exits 0

**File Footprint:**
- Write: `.claude/skills/x-epic-refine/SKILL.md`
- Read: `src/main/resources/targets/claude/skills/core/plan/x-epic-refine/SKILL.md`

---

### TASK-0069-0003-003: Commit to epic/0069 branch

Stage and commit both files; push to `epic/0069`.

**Commit message:**
```
feat(epic-0069): x-epic-refine skill — multi-persona strategic 4-phase dispatcher
```

**Accepted Criteria:**
- `git status` is clean after commit
- Commit contains exactly: `src/main/resources/targets/claude/skills/core/plan/x-epic-refine/SKILL.md`
- Branch is `epic/0069`; push succeeds (or dry-run confirms pushable)

**File Footprint:**
- Write: git history on `epic/0069`
- Read: staged files from TASK-0069-0003-001 and -002

---

### TASK-0069-0003-004 (Optional): Local smoke validation

Run two synthetic scenarios against the skill to verify the dispatcher contract end-to-end:

**Scenario A — multi-persona approve:** Synthetic epic with all 7 Epic Dimensions well-formed (problem, persona ≥ 2, hypothesis + KPI, OKRs with baseline+target+horizon, ≥ 2 alternatives, ≥ 3 out-of-scope items, compliance declared). Expected: `refinementVerdict.status = "approved"`, zero blockers.

**Scenario B — PO silent NO-GO:** Synthetic epic where hypothesis is stated without a measurable indicator (`"melhora performance"` without unit/target). Expected: `refinementVerdict.status = "rejected"`, `dimensions.po.blocker = "hypothesis: missing measurable indicator"`.

**Accepted Criteria:**
- Scenario A produces `approved` verdict with `scope: "epic"`
- Scenario B produces `rejected` verdict with PO blocker populated
- Phase A dispatches all N siblings in a single message (verified by task tracker output)
- Phase D Architect agent invoked with `model: opus`
- `x-internal-status-update` called (state file updated in both scenarios)
- `--dry-run` flag prevents any file writes (verify no markdown changes on disk)

**File Footprint:**
- Read: synthetic epic markdowns (created inline during smoke); `execution-state.json` for synthetic epic
- Write: none (dry-run mode) or synthetic epic markdown (smoke only — not committed)
