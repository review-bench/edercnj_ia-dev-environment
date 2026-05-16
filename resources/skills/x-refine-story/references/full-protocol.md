# x-refine-story — Full Protocol Reference

> Verbose detail moved out of `SKILL.md` per ADR-0012. SKILL.md remains the minimum viable behavioral contract; this document carries the full phase-by-phase protocol, persona `Agent()` prompts, telemetry sub-markers, schemas, and editorial rules. Loaded on-demand via `Read` only when the slim contract is insufficient.

---

## §1 — Persona NO-GO Rules (Extended)

### Product Owner (PO) NO-GOs

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `persona` | Role stated as "sistema" or generic system actor | `"persona: generic system actor; must be a real user role"` |
| `value` | Value proposition is not falsifiable (e.g., "melhora performance" without unit/target) | `"value: non-falsifiable proposition — add measurable unit and target"` |
| `ac` | Fewer than 4 Gherkin scenario categories present | `"ac: missing scenario categories (required: happy-path, error/boundary, performance/SLA, security/auth)"` |

### Tech Lead NO-GOs

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `contracts` | Request/response uses `Object` or `Map<String, Any>` without typed schema | `"contracts: untyped request/response (Object/Map) — define typed schema"` |
| `contracts` | Event schema absent when story declares event-producer/consumer interface | `"contracts: event schema required for event-driven interfaces"` |
| `ac` | AC references undefined external system behavior | `"ac: AC references undefined external system — add contract or mock boundary"` |

### Architect NO-GOs

All Architect work in story-level refinement is consolidation (Phase D) — no silent NO-GOs for story scope. Architect provides cross-cutting review and may add advisory notes.

### Security NO-GOs

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `risks` | Story handles PII/credentials without declared data-handling rule | `"risks: PII/credential handling without data-handling rule declaration"` |
| `risks` | Story calls external service without declared auth/token handling | `"risks: external service call without auth/token handling declared"` |

### QA NO-GOs

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `ac` | No Gherkin scenario covers error/boundary case | `"ac: no error/boundary Gherkin scenario present (Rule 05 §mandatory scenario categories)"` |
| `risks` | No risk identified despite story touching shared state or DB | `"risks: shared-state/DB story with no risk identified"` |

### Performance Engineer NO-GOs (conditional: `flag.has_sla_declared`)

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `metrics` | No latency/throughput SLO declared for API-facing story | `"metrics: API-facing story requires latency SLO (p50/p95/p99 + timeout)"` |

### SRE/DevOps NO-GOs (conditional: `infra.*` or `runtime.*` capability)

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `risks` | Deployment-affecting story without rollback consideration | `"risks: deployment change without rollback consideration"` |

---

## §A — Phase A: Parallel Specialist Analysis

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-refine-story Phase-A-SpecialistAnalysis

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-refine-story --phase Phase-A-SpecialistAnalysis")

TaskCreate(subject: "story-XXXX-YYYY › Phase A - Specialist Analysis", activeForm: "Running parallel specialist analysis")
```

### A.1 Resolve story path and epic state

Read the story markdown from `ai/epics/epic-XXXX-<slug>/story-XXXX-YYYY.md` (use PathResolver probe: check `ai/epics/epic-XXXX-*/` glob). Non-zero → exit `STORY_NOT_FOUND`.

Read the dimensions KP for persona responsibilities:

```text
Read: .claude/knowledge/refinement/dimensions.md
```

### A.2 Launch 5–7 parallel persona-agents

**MANDATORY — emit ALL persona `Agent()` calls as SIBLING tool calls in ONE assistant message.**

#### Always active (5 core personas)

```text
Agent(
  subagent_type: "product-owner",
  model: "sonnet",
  description: "PO gap analysis for {STORY_ID}",
  prompt: "Perform DoR gap analysis for this story.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY these dimensions for the PO persona: value, persona, alternatives.
Return a JSON gap-report with this shape:
{
  \"persona\": \"PO\",
  \"gaps\": [
    {\"dimension\": \"<name>\", \"issue\": \"<description>\", \"question\": \"<what to ask operator?\", \"severity\": \"question|noGo\"}
  ]
}
If no gaps found, return {\"persona\": \"PO\", \"gaps\": []}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › PO analysis\", activeForm: \"Analysing as PO\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]

Agent(
  subagent_type: "tech-lead",
  model: "sonnet",
  description: "Tech Lead gap analysis for {STORY_ID}",
  prompt: "Perform DoR gap analysis for this story.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY these dimensions for Tech Lead: contracts (interfaces, events, APIs), metrics (observability), risks.
Return a JSON gap-report: {\"persona\": \"TechLead\", \"gaps\": [...]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › Tech Lead analysis\", activeForm: \"Analysing as Tech Lead\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]

Agent(
  subagent_type: "architect",
  model: "sonnet",
  description: "Architect gap analysis for {STORY_ID}",
  prompt: "Perform DoR gap analysis for this story.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY these dimensions: contracts (architectural), alternatives (design trade-offs), risks (technical).
Return a JSON gap-report: {\"persona\": \"Architect\", \"gaps\": [...]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › Architect analysis\", activeForm: \"Analysing as Architect\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]

Agent(
  subagent_type: "security-engineer",
  model: "sonnet",
  description: "Security gap analysis for {STORY_ID}",
  prompt: "Perform DoR gap analysis for this story.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY: ac (security/auth acceptance criteria), risks (security).
Return a JSON gap-report: {\"persona\": \"Security\", \"gaps\": [...]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › Security analysis\", activeForm: \"Analysing as Security\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]

Agent(
  subagent_type: "qa-engineer",
  model: "sonnet",
  description: "QA gap analysis for {STORY_ID}",
  prompt: "Perform DoR gap analysis for this story.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY: ac (all 4 mandatory categories: happy-path, error/boundary, performance/SLA, security/auth). Flag any missing category as noGo.
Return a JSON gap-report: {\"persona\": \"QA\", \"gaps\": [...]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › QA analysis\", activeForm: \"Analysing as QA\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

#### Conditional personas

```text
Agent(  [conditional: flag.has_sla_declared]
  subagent_type: "performance-engineer",
  model: "sonnet",
  description: "Performance gap analysis for {STORY_ID}",
  prompt: "Perform DoR gap analysis for this story.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY: metrics (SLA thresholds, p95/p99 latency targets, throughput), ac (performance/SLA category).
Return a JSON gap-report: {\"persona\": \"Performance\", \"gaps\": [...]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › Performance analysis\", activeForm: \"Analysing as Performance\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)

Agent(  [conditional: flag.has_infra_changes]
  subagent_type: "devops-engineer",
  model: "sonnet",
  description: "DevOps gap analysis for {STORY_ID}",
  prompt: "Perform DoR gap analysis for this story.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY: contracts (deployment/infrastructure), metrics (operational), risks (operational).
Return a JSON gap-report: {\"persona\": \"DevOps\", \"gaps\": [...]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › DevOps analysis\", activeForm: \"Analysing as DevOps\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)
```

Telemetry markers wrap each `subagent.start` / `subagent.end` per persona using `telemetry-phase.sh subagent-start/end x-refine-story <PersonaKey>`.

### A.3 Aggregation

Collect all gap-reports. Validate each is parseable JSON; discard malformed. If ALL are empty → exit `PHASE_A_EMPTY`.

Separate:

- `questions` — gaps with `severity: "question"` → feed into Phase B
- `noGos` — gaps with `severity: "noGo"` → pass directly to Phase D; **NOT surfaced as questions** (D5: NO-GOs silent)

```text
TaskUpdate(id: phaseATaskId, status: "completed")
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode post --skill x-refine-story --phase Phase-A-SpecialistAnalysis")

<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-refine-story Phase-A-SpecialistAnalysis ok
```

### Phase A result aggregation rules

1. Collect all gap-reports into `allGaps`.
2. `questions = gaps.filter(g => g.severity == "question")`.
3. `noGos = gaps.filter(g => g.severity == "noGo")`.
4. If both empty → jump to Phase D with `status="approved"`.
5. `noGos.length > 0` → final verdict will be `status="rejected"`.
6. `questions.length > 0` → proceed to Phase B for operator Q&A.

---

## §B — Phase B: Consolidate & Operator Q&A

<!-- phase-no-gate: Phase B is skipped entirely when --non-interactive -->

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-refine-story Phase-B-Consolidate
```

**Skip Phase B when `--non-interactive`**: set `answers = {}` and proceed directly to Phase C.

```text
TaskCreate(subject: "story-XXXX-YYYY › Phase B - Consolidate", activeForm: "Consolidating questions and awaiting operator answers")
```

### B.1 Dedup and group questions

From all Phase A `questions` (severity: "question"):

1. Deduplicate by semantic similarity (same dimension + same gap → keep one).
2. Group by 6 categories below.
3. Discard empty groups.

If no questions remain (all gaps were NO-GOs or all personas returned empty) → skip B.2, set `answers = {}`.

#### Question grouping categories

| Category label | Dimensions that map to it |
| :--- | :--- |
| `Persona & Valor` | `persona`, `value` |
| `Critérios de Aceite` | `ac` |
| `Contratos & Interfaces` | `contracts` |
| `Métricas` | `metrics` |
| `Alternativas` | `alternatives` |
| `Riscos` | `risks` |

Deduplication: two questions are duplicates when `dimension` is identical and `issue` token overlap > 60%. Keep the one with more specific `question` wording.

### B.2 Single batch to operator (D4: one batch only)

**EXACTLY ONE** `AskUserQuestion` call — no per-persona loops, no retry loops.

Present grouped questions:

```text
--- Story Refinement Questions for {STORY_ID} ---

The following gaps were identified. Please answer each question.
(NO-GO blockers are tracked separately and will appear in the final verdict.)

[value]
Q1. <question text from PO>

[ac]
Q2. <question text from QA>

[contracts]
Q3. <question text from Tech Lead>

Reply with answers numbered Q1, Q2, Q3, etc. Type SKIP to skip a question.
```

Parse the operator's reply into a map `{Q1: "<answer>", Q2: "<answer>", ...}`. Map each answer back to its originating `{persona, dimension, gap}` entry.

```text
TaskUpdate(id: phaseBTaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-refine-story Phase-B-Consolidate ok
```

---

## §C — Phase C: Parallel Specialist Refinement

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-refine-story Phase-C-Refine

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-refine-story --phase Phase-C-Refine")
```

**Skip Phase C when `--non-interactive`**: set `proposedSections = {}` and proceed to Phase D.

```text
TaskCreate(subject: "story-XXXX-YYYY › Phase C - Refinement", activeForm: "Running parallel specialist refinement with operator answers")
```

### C.1 Distribute answers and re-launch persona-agents

**MANDATORY — emit ALL persona `Agent()` calls as SIBLING tool calls in ONE assistant message.**

For each persona that had questions in Phase A, launch a refinement agent with the answers it owns. Template:

```text
Agent(
  subagent_type: "<persona-type>",
  model: "sonnet",
  description: "<Persona> refinement for {STORY_ID}",
  prompt: "Read the story at {storyPath}.
The operator answered your questions as follows: {answersFor<Persona>}.
Based on these answers, produce the proposed story sections for your dimensions (<dimensions>).
Return a JSON object: {\"persona\": \"<Key>\", \"proposedSections\": {\"<dim>\": \"<refined text>\", ...}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase C › <Persona> refinement\", activeForm: \"Refining as <Persona>\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

Active personas + dimensions:

- **PO** (`product-owner`): `value, persona, alternatives`
- **TechLead** (`tech-lead`): `contracts, metrics, risks`
- **Architect** (`architect`): `contracts, alternatives, risks`
- **Security** (`security-engineer`): `ac, risks`
- **QA** (`qa-engineer`): `ac`
- **Performance** (conditional): `metrics`
- **DevOps** (conditional): `contracts, metrics, risks`

Telemetry markers wrap each `subagent.start` / `subagent.end` using `telemetry-phase.sh subagent-start/end x-refine-story <PersonaKey>-Refine`.

Collect `proposedSections` from each agent. Merge into a single map keyed by dimension.

```text
TaskUpdate(id: phaseCTaskId, status: "completed")
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode post --skill x-refine-story --phase Phase-C-Refine")

<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-refine-story Phase-C-Refine ok
```

---

## §D — Phase D: Architect Consolidation + Dual-Write

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-refine-story Phase-D-Architect

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-refine-story --phase Phase-D-Architect")

TaskCreate(subject: "story-XXXX-YYYY › Phase D - Architect", activeForm: "Architect consolidating refinement verdict")
```

### D.1 Architect consolidation (opus tier)

```text
Agent(
  subagent_type: "architect",
  model: "opus",
  description: "Architect consolidation for {STORY_ID}",
  prompt: "You are the final consolidator for a story refinement session.

Story: {storyPath}
Proposed section refinements: {proposedSections}
NO-GO blockers from Phase A: {noGos}
Operator answers summary: {answers}

Your job:
1. Review all proposed sections for consistency and coherence.
2. Merge proposedSections into a unified set of refined story sections.
3. Evaluate all noGos — determine if each blocker is genuinely disqualifying or was addressed implicitly by the operator answers.
4. Produce the final Refinement Verdict in this JSON shape:
{
  \"status\": \"approved\" | \"rejected\" | \"tbd\",
  \"scope\": \"story\",
  \"checkedAt\": \"<ISO-8601 UTC>\",
  \"dimensions\": {
    \"value\": \"passed|gap|noGo\",
    \"persona\": \"passed|gap|noGo\",
    \"ac\": \"passed|gap|noGo\",
    \"contracts\": \"passed|gap|noGo\",
    \"metrics\": \"passed|gap|noGo\",
    \"alternatives\": \"passed|gap|noGo\",
    \"risks\": \"passed|gap|noGo\"
  },
  \"blockers\": [\"<dimension: reason>\"],
  \"refinedSections\": {\"<dimension>\": \"<refined markdown text>\"},
  \"verdictRationale\": \"<1-2 sentence summary>\"
}
Rules:
- status=approved: ALL mandatory dimensions passed (value, persona, ac, contracts, metrics).
- status=rejected: ANY dimension is noGo AND the operator's answers did not resolve it.
- status=tbd: gaps remain but no noGos — operator interaction needed.
- alternatives and risks are advisory (gap does not trigger rejection alone).

Return ONLY the JSON object above. No prose outside JSON.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase D › Architect verdict\", activeForm: \"Producing Refinement Verdict\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

Parse the Architect's JSON verdict. Validate `status` is one of `approved|rejected|tbd`.

### Section merge targets in story markdown

| Source persona | Target section |
| :--- | :--- |
| PO | §1 Visão (value proposition + measurable metric) |
| Tech Lead | §3 Contratos & Endpoints (typed request/response) |
| Security | §6 Segurança (controls table) |
| QA | §5.2 Acceptance Criteria (completed Gherkin) |
| Performance | §7.2 Metrics (SLO table) |
| SRE/DevOps | §7 Observabilidade (operational risks) |

**Minimum for `approved`:** PO + Tech Lead + Architect + Security + QA must all pass (no blockers). Conditional personas' verdicts are advisory unless the story explicitly touches their domain.

### D.2 Compute verdictHash

```bash
printf '%s' '<verdict JSON>' | sha256sum | awk '{print $1}'
```

Alternative (extract block from story markdown post-write):

```bash
verdict_block=$(awk '/^## Refinement Verdict/,/^## /' story.md | head -n -1)
verdict_hash=$(echo -n "$verdict_block" | sha256sum | awk '{print $1}')
```

### D.3 Dual-write (MANDATORY — NON-NEGOTIABLE)

**Write 1 — execution-state.json** (via x-internal-update-status, INLINE-SKILL):

```text
Skill(skill: "x-internal-update-status", args: "--file ai/epics/epic-XXXX/execution-state.json --type story --id <STORY-ID> --field refinementVerdict --value {\"status\":\"<status>\",\"scope\":\"story\",\"checkedAt\":\"<iso>\",\"dimensions\":{...},\"blockers\":[...],\"verdictHash\":\"<hash>\"}")  [required]
```

Non-zero → exit `VERDICT_WRITE_FAILED`.

**Write 2 — Story markdown** (append/replace `## Refinement Verdict` section):

```markdown
## Refinement Verdict

**Status:** approved | rejected | tbd
**Refined at:** <ISO-8601>
**Verdict hash:** <verdictHash>

### Dimensions

| Dimension | Status |
|-----------|--------|
| value | passed |
| persona | passed |
| ac | passed |
| contracts | passed |
| metrics | passed |
| alternatives | passed |
| risks | passed |

### Blockers

<none — or list of blockers>

### Rationale

<verdictRationale from Architect>

### Refined Sections

<per-dimension refined text, if any>
```

Use Edit tool to write the `## Refinement Verdict` block. If it already exists, replace it (idempotent).

### D.4 Finalize

```text
TaskUpdate(id: phaseDTaskId, status: "completed")

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode final --skill x-refine-story --phase Phase-D-Architect --expected-artifacts {storyPath},{epicStatePath}")

<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-refine-story Phase-D-Architect ok
```

---

## §5 — Error Codes Detail

| Code | Condition | Recovery |
| :--- | :--- | :--- |
| `STORY_NOT_FOUND` | `ai/epics/epic-XXXX*/story-XXXX-YYYY.md` absent | Verify story file path; check `execution-state.json` for correct epic slug |
| `STORY_STATE_MISSING` | `execution-state.json` absent for the epic | Create via `x-internal-update-status --initialize` and retry |
| `PHASE_A_EMPTY` | All persona agents returned `gaps: []` but status remains tbd | Verify persona prompts reference `dimensions.md` KP correctly |
| `VERDICT_WRITE_FAILED` | `x-internal-update-status` returned non-zero | Check permissions on `execution-state.json`; verify `--story-id` matches state key |

---

## §6 — Integration with x-implement-story Phase 0 Gate

`x-implement-story` checks `execution-state.json` in Phase 0 for:

```json
{
  "storyStatuses": {
    "story-XXXX-YYYY": {
      "refinementVerdict": {
        "status": "approved",
        "scope": "story"
      }
    }
  }
}
```

Gate check: `status == "approved" AND scope == "story"`. An epic-scoped verdict does NOT unblock story implementation — scope discriminator prevents cross-level confusion.

If the gate fails, `enforce-refinement-gate.sh` (Camada 0 PreToolUse hook) exits 33 (`REFINEMENT_REQUIRED`).
