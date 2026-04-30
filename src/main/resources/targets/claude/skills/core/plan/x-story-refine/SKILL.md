---
name: x-story-refine
description: "Multi-persona 4-phase story refinement dispatcher. Phase A: 5-7 parallel specialist agents analyse the story for gaps. Phase B: single consolidated question batch to the operator. Phase C: 5-7 parallel specialists refine with answers. Phase D: Architect (opus) consolidates a Refinement Verdict and dual-writes to execution-state.json + story markdown."
visibility: public
user-invocable: true
model: sonnet
allowed-tools: [Read, Write, Edit, Skill, Agent, TaskCreate, TaskUpdate, Bash]
argument-hint: "<STORY-ID> [--epic-id <ID>] [--non-interactive]"
requires-capabilities: [governance.refinement-gate]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, Concise.
- **Efficiency**: Remove all conversational fillers. Start directly with technical content.

## Triggers

```
/x-story-refine story-XXXX-YYYY                        — full 4-phase refinement (interactive)
/x-story-refine story-XXXX-YYYY --non-interactive       — phases A+D only (no operator questions)
/x-story-refine story-XXXX-YYYY --epic-id 0069          — explicit epic ID override
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `STORY-ID` | positional | (required) | `story-XXXX-YYYY` format |
| `--epic-id` | String | auto-derived from STORY-ID prefix | 4-digit epic ID |
| `--non-interactive` | boolean | `false` | Skip Phase B (no questions); verdict based on Phases A+D only |

## Output Contract

| Field | Type | Description |
|-------|------|-------------|
| `storyId` | String | Input story ID |
| `epicId` | String | Resolved epic ID |
| `verdict` | Enum | `approved \| rejected \| tbd` |
| `verdictHash` | String | SHA-256 of verdict JSON (for CI drift detection) |
| `blockers` | List\<String\> | NO-GO dimensions (empty if approved) |
| `refinedAt` | ISO-8601 | Timestamp of final verdict |
| `artifactPath` | String | Path to story markdown `## Refinement Verdict` section |

## Error Codes

| Exit | Code | Condition |
|------|------|-----------|
| 1 | `STORY_NOT_FOUND` | Story markdown not found at expected path |
| 2 | `EPIC_STATE_MISSING` | `execution-state.json` not found for epic |
| 3 | `PHASE_A_EMPTY` | All personas returned empty gap-reports |
| 4 | `VERDICT_WRITE_FAILED` | x-internal-status-update returned non-zero |

---

## CRITICAL EXECUTION RULE

**4 phases (A–D). ALL phases through Phase D are mandatory unless `--non-interactive` skips Phase B+C.** Print `>>> Phase X completed. Proceeding to Phase Y...` between phases. `>>> Phase D completed. Refinement verdict written.` at end.

---

## Phase A — Parallel Specialist Analysis

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-refine Phase-A-SpecialistAnalysis`

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-story-refine --phase Phase-A-SpecialistAnalysis")

Open phase tracker:

    TaskCreate(subject: "story-XXXX-YYYY › Phase A - Specialist Analysis", activeForm: "Running parallel specialist analysis")

### A.1 Resolve story path and epic state

Read the story markdown from:
```
ai/epics/epic-XXXX-<slug>/story-XXXX-YYYY.md
```
(Use PathResolver probe: check `ai/epics/epic-XXXX-*/` glob). Non-zero → exit `STORY_NOT_FOUND`.

Read the dimensions KP for persona responsibilities:

    Read: .claude/knowledge/refinement/dimensions.md

### A.2 Launch 5-7 parallel persona-agents

**MANDATORY — emit ALL persona Agent() calls as SIBLING tool calls in ONE assistant message.**

Always active (5 core personas):

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-story-refine PO`

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "PO gap analysis for {STORY_ID}",
  prompt: "You are a Product Owner specialist performing DoR gap analysis.
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
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Tech Lead gap analysis for {STORY_ID}",
  prompt: "You are a Tech Lead performing DoR gap analysis.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY these dimensions for Tech Lead: contracts (interfaces, events, APIs), metrics (observability), risks.
Return a JSON gap-report: {\"persona\": \"TechLead\", \"gaps\": [{\"dimension\": ..., \"issue\": ..., \"question\": ..., \"severity\": \"question|noGo\"}]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › Tech Lead analysis\", activeForm: \"Analysing as Tech Lead\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Architect gap analysis for {STORY_ID}",
  prompt: "You are an Architect performing DoR gap analysis.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY these dimensions: contracts (architectural), alternatives (design trade-offs), risks (technical).
Return a JSON gap-report: {\"persona\": \"Architect\", \"gaps\": [{\"dimension\": ..., \"issue\": ..., \"question\": ..., \"severity\": \"question|noGo\"}]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › Architect analysis\", activeForm: \"Analysing as Architect\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Security gap analysis for {STORY_ID}",
  prompt: "You are a Security specialist performing DoR gap analysis.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY: ac (security/auth acceptance criteria), risks (security).
Return a JSON gap-report: {\"persona\": \"Security\", \"gaps\": [{\"dimension\": ..., \"issue\": ..., \"question\": ..., \"severity\": \"question|noGo\"}]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › Security analysis\", activeForm: \"Analysing as Security\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "QA gap analysis for {STORY_ID}",
  prompt: "You are a QA specialist performing DoR gap analysis.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY: ac (all 4 mandatory categories: happy-path, error/boundary, performance/SLA, security/auth). Flag any missing category as noGo.
Return a JSON gap-report: {\"persona\": \"QA\", \"gaps\": [{\"dimension\": ..., \"issue\": ..., \"question\": ..., \"severity\": \"question|noGo\"}]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › QA analysis\", activeForm: \"Analysing as QA\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

Conditional personas (activate when story signals the domain):

```
Agent(  [conditional: flag.has_sla_declared]
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Performance gap analysis for {STORY_ID}",
  prompt: "You are a Performance engineer performing DoR gap analysis.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY: metrics (SLA thresholds, p95/p99 latency targets, throughput), ac (performance/SLA category).
Return a JSON gap-report: {\"persona\": \"Performance\", \"gaps\": [{\"dimension\": ..., \"issue\": ..., \"question\": ..., \"severity\": \"question|noGo\"}]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › Performance analysis\", activeForm: \"Analysing as Performance\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)
```

```
Agent(  [conditional: flag.has_infra_changes]
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "DevOps gap analysis for {STORY_ID}",
  prompt: "You are a DevOps engineer performing DoR gap analysis.
Read the story at {storyPath}. Read .claude/knowledge/refinement/dimensions.md §Story Dimensions.
Analyse ONLY: contracts (deployment/infrastructure), metrics (operational), risks (operational).
Return a JSON gap-report: {\"persona\": \"DevOps\", \"gaps\": [{\"dimension\": ..., \"issue\": ..., \"question\": ..., \"severity\": \"question|noGo\"}]}.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase A › DevOps analysis\", activeForm: \"Analysing as DevOps\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)
```

<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-story-refine PO ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-story-refine TechLead`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-story-refine TechLead ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-story-refine Architect`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-story-refine Architect ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-story-refine Security`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-story-refine Security ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-story-refine QA`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-story-refine QA ok`

Collect all gap-reports. Validate each is parseable JSON; discard malformed. If ALL are empty → exit `PHASE_A_EMPTY`.

Separate:
- `questions` — gaps with `severity: "question"` → feed into Phase B
- `noGos` — gaps with `severity: "noGo"` → pass directly to Phase D; **NOT surfaced as questions** (D5: NO-GOs silent)

    TaskUpdate(id: phaseATaskId, status: "completed")

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-story-refine --phase Phase-A-SpecialistAnalysis")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-refine Phase-A-SpecialistAnalysis ok`

>>> Phase A completed. Proceeding to Phase B (Consolidation)...

---

## Phase B — Consolidate & Operator Q&A

<!-- phase-no-gate: Phase B is skipped entirely when --non-interactive; gate is inside conditional block -->

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-refine Phase-B-Consolidate`

**Skip Phase B when `--non-interactive`**: set `answers = {}` and proceed directly to Phase C.

Open phase tracker:

    TaskCreate(subject: "story-XXXX-YYYY › Phase B - Consolidate", activeForm: "Consolidating questions and awaiting operator answers")

### B.1 Dedup and group questions

From all Phase A `questions` (severity: "question"):
1. Deduplicate by semantic similarity (same dimension + same gap → keep one).
2. Group by dimension: `value`, `ac`, `contracts`, `metrics`, `alternatives`, `risks`.
3. Discard empty groups.

If no questions remain (all gaps were NO-GOs or all personas returned empty) → skip B.2, set `answers = {}`.

### B.2 Single batch to operator (D4: one batch only)

**EXACTLY ONE** `AskUserQuestion` call — no per-persona loops, no retry loops.

Present grouped questions:

```
--- Story Refinement Questions for {STORY_ID} ---

The following gaps were identified. Please answer each question.
(NO-GO blockers are tracked separately and will appear in the final verdict.)

[value]
Q1. <question text from PO>

[ac]
Q2. <question text from QA>
...

[contracts]
Q3. <question text from Tech Lead>

Reply with answers numbered Q1, Q2, Q3, etc. Type SKIP to skip a question.
```

Parse the operator's reply into a map `{Q1: "<answer>", Q2: "<answer>", ...}`.
Map each answer back to its originating `{persona, dimension, gap}` entry.

    TaskUpdate(id: phaseBTaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-refine Phase-B-Consolidate ok`

>>> Phase B completed. Proceeding to Phase C (Refinement)...

---

## Phase C — Parallel Specialist Refinement

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-refine Phase-C-Refine`

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-story-refine --phase Phase-C-Refine")

**Skip Phase C when `--non-interactive`**: set `proposedSections = {}` and proceed to Phase D.

Open phase tracker:

    TaskCreate(subject: "story-XXXX-YYYY › Phase C - Refinement", activeForm: "Running parallel specialist refinement with operator answers")

### C.1 Distribute answers and re-launch persona-agents

**MANDATORY — emit ALL persona Agent() calls as SIBLING tool calls in ONE assistant message.**

For each persona that had questions in Phase A, launch a refinement agent with the answers it owns:

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-story-refine PO-Refine`

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "PO refinement for {STORY_ID}",
  prompt: "You are a Product Owner specialist.
Read the story at {storyPath}.
The operator answered your questions as follows: {answersForPO}.
Based on these answers, produce the proposed story sections for your dimensions (value, persona, alternatives).
Return a JSON object: {\"persona\": \"PO\", \"proposedSections\": {\"value\": \"<refined text>\", \"persona\": \"<refined text>\", \"alternatives\": \"<refined text>\"}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase C › PO refinement\", activeForm: \"Refining as PO\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Tech Lead refinement for {STORY_ID}",
  prompt: "You are a Tech Lead specialist.
Read the story at {storyPath}.
The operator answered your questions as follows: {answersForTechLead}.
Based on these answers, produce the proposed story sections for your dimensions (contracts, metrics, risks).
Return a JSON object: {\"persona\": \"TechLead\", \"proposedSections\": {\"contracts\": \"<refined text>\", \"metrics\": \"<refined text>\", \"risks\": \"<refined text>\"}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase C › Tech Lead refinement\", activeForm: \"Refining as Tech Lead\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Architect refinement for {STORY_ID}",
  prompt: "You are an Architect specialist.
Read the story at {storyPath}.
The operator answered your questions as follows: {answersForArchitect}.
Based on these answers, produce the proposed story sections for your dimensions (contracts, alternatives, risks).
Return a JSON object: {\"persona\": \"Architect\", \"proposedSections\": {\"contracts\": \"<refined text>\", \"alternatives\": \"<refined text>\", \"risks\": \"<refined text>\"}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase C › Architect refinement\", activeForm: \"Refining as Architect\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Security refinement for {STORY_ID}",
  prompt: "You are a Security specialist.
Read the story at {storyPath}.
The operator answered your questions as follows: {answersForSecurity}.
Based on these answers, produce the proposed story sections for your dimensions (ac security/auth, risks security).
Return a JSON object: {\"persona\": \"Security\", \"proposedSections\": {\"ac\": \"<refined text>\", \"risks\": \"<refined text>\"}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase C › Security refinement\", activeForm: \"Refining as Security\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "QA refinement for {STORY_ID}",
  prompt: "You are a QA specialist.
Read the story at {storyPath}.
The operator answered your questions as follows: {answersForQA}.
Based on these answers, produce the proposed story sections for your dimensions (ac: all 4 mandatory categories).
Return a JSON object: {\"persona\": \"QA\", \"proposedSections\": {\"ac\": \"<refined text>\"}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"story-XXXX-YYYY › Phase C › QA refinement\", activeForm: \"Refining as QA\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-story-refine PO-Refine ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-story-refine TechLead-Refine`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-story-refine TechLead-Refine ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-story-refine Architect-Refine`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-story-refine Architect-Refine ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-story-refine Security-Refine`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-story-refine Security-Refine ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-story-refine QA-Refine`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-story-refine QA-Refine ok`

Collect `proposedSections` from each agent. Merge into a single map keyed by dimension.

    TaskUpdate(id: phaseCTaskId, status: "completed")

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-story-refine --phase Phase-C-Refine")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-refine Phase-C-Refine ok`

>>> Phase C completed. Proceeding to Phase D (Architect Consolidation)...

---

## Phase D — Architect Consolidation + Dual-Write

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-refine Phase-D-Architect`

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-story-refine --phase Phase-D-Architect")

Open phase tracker:

    TaskCreate(subject: "story-XXXX-YYYY › Phase D - Architect", activeForm: "Architect consolidating refinement verdict")

### D.1 Architect consolidation (opus tier)

```
Agent(
  subagent_type: "general-purpose",
  model: "opus",
  description: "Architect consolidation for {STORY_ID}",
  prompt: "You are the Lead Architect and final consolidator for a story refinement session.

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

### D.2 Compute verdictHash

Write the verdict JSON to a temp file then hash it (safe for non-ASCII and special characters):

Bash command: `printf '%s' '<verdict JSON>' | sha256sum | awk '{print $1}'`

### D.3 Dual-write (MANDATORY — NON-NEGOTIABLE)

**Write 1 — execution-state.json** (via x-internal-status-update, INLINE-SKILL):

    Skill(skill: "x-internal-status-update", args: "--file ai/epics/epic-XXXX/execution-state.json --type story --id <STORY-ID> --field refinementVerdict --value {\"status\":\"<status>\",\"scope\":\"story\",\"checkedAt\":\"<iso>\",\"dimensions\":{...},\"blockers\":[...],\"verdictHash\":\"<hash>\"}")  [required]

Non-zero → exit `VERDICT_WRITE_FAILED`.

**Write 2 — Story markdown** (append `## Refinement Verdict` section):

Read the story markdown file. Append or replace the `## Refinement Verdict` block:

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

    TaskUpdate(id: phaseDTaskId, status: "completed")

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode final --skill x-story-refine --phase Phase-D-Architect --expected-artifacts {storyPath},{epicStatePath}")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-refine Phase-D-Architect ok`

>>> Phase D completed. Refinement verdict written.

---

## Output Summary

Print a concise summary:

```
x-story-refine completed for {STORY_ID}
  Verdict: <approved|rejected|tbd>
  Blockers: <none | list>
  Verdict hash: <hash>
  State file: ai/epics/epic-XXXX/execution-state.json (field: refinementVerdict)
  Story markdown: {storyPath} (section: ## Refinement Verdict)
```

---

## Integration Notes

- **Consumed by:** `x-story-implement` (Phase 0: checks `refinementVerdict.status == "approved"` via `enforce-refinement-gate.sh` hook).
- **Depends on:** `x-internal-status-update` (INLINE-SKILL, Rule 13 Pattern 1), `knowledge/refinement/dimensions.md` (KP, story-0069-0001).
- **Verdict drift detection:** `verdictHash` in state file vs `verdictHash` in story markdown is compared by `audit-refinement-gate.sh` (story-0069-0006). Drift fails CI with `REFINEMENT_VERDICT_DRIFT`.
- **Re-run idempotency:** Running x-story-refine again on an `approved` story replaces the verdict. This is intentional — re-refinement after story changes should update the verdict.
