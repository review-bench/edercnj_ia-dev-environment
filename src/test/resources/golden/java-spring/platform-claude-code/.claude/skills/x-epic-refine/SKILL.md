---
name: x-epic-refine
description: "Multi-persona 4-phase strategic epic refinement dispatcher. Phase A: 5-6 parallel specialist agents analyse the epic for strategic gaps. Phase B: single consolidated question batch to the operator. Phase C: 5-6 parallel specialists refine with answers. Phase D: Architect (opus) consolidates a Refinement Verdict with scope=epic and dual-writes to execution-state.json + epic markdown."
visibility: public
user-invocable: true
model: sonnet
allowed-tools: [Read, Write, Edit, Skill, Agent, TaskCreate, TaskUpdate, Bash]
argument-hint: "<EPIC-ID> [--non-interactive] [--dry-run] [--legacy-refinement]"
context-budget: heavy
requires-capabilities: [governance.refinement-gate]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, Concise.
- **Efficiency**: Remove all conversational fillers. Start directly with technical content.

## Triggers

```
/x-epic-refine epic-0069                        — full 4-phase strategic refinement (interactive)
/x-epic-refine epic-0069 --non-interactive       — phases A+D only (no operator questions)
/x-epic-refine epic-0069 --dry-run               — run A-D without writing markdown or state
/x-epic-refine epic-0069 --legacy-refinement     — skip all phases; return verdict.status="tbd"
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `EPIC-ID` | positional | (required) | `epic-XXXX` or bare `0069` format |
| `--epic-id` | String | auto-derived | 4-digit epic ID override |
| `--non-interactive` | boolean | `false` | Skip Phase B+C (no operator questions); verdict based on Phases A+D only |
| `--dry-run` | boolean | `false` | Execute Phases A–D without writing markdown or state |
| `--legacy-refinement` | boolean | `false` | Skip all phases; emit WARNING; return `verdict.status="tbd"` |

## Output Contract

| Field | Type | Description |
|-------|------|-------------|
| `epicId` | String | Resolved epic ID |
| `verdict` | Enum | `approved \| rejected \| tbd` |
| `verdictHash` | String | SHA-256 of verdict JSON (for CI drift detection) |
| `blockers` | List\<String\> | NO-GO dimensions (empty if approved) |
| `refinedAt` | ISO-8601 | Timestamp of final verdict |
| `artifactPath` | String | Path to epic markdown `## Refinement Verdict` section |

## Error Codes

| Exit | Code | Condition |
|------|------|-----------|
| 1 | `EPIC_NOT_FOUND` | Epic markdown not found at expected path |
| 2 | `EPIC_STATE_MISSING` | `execution-state.json` not found for epic |
| 3 | `PHASE_A_EMPTY` | All personas returned empty gap-reports |
| 4 | `VERDICT_WRITE_FAILED` | x-internal-status-update returned non-zero |

---

## CRITICAL EXECUTION RULE

**4 phases (A–D). ALL phases through Phase D are mandatory unless `--non-interactive` skips Phase B+C.** Print `>>> Phase X completed. Proceeding to Phase Y...` between phases. `>>> Phase D completed. Refinement verdict written.` at end.

`--legacy-refinement`: emit WARNING and return immediately with `verdict.status="tbd"` — no phases execute.

---

## Phase A — Parallel Strategic Analysis

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-refine Phase-A-StrategicAnalysis`

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-epic-refine --phase Phase-A-StrategicAnalysis")

Open phase tracker:

    TaskCreate(subject: "epic-XXXX › Phase A - Strategic Analysis", activeForm: "Running parallel strategic persona analysis")

### A.1 Resolve epic path and state

Probe for epic markdown using glob `ai/epics/epic-XXXX-*/epic-XXXX.md`. Non-zero → exit `EPIC_NOT_FOUND`.
Probe for `ai/epics/epic-XXXX-*/execution-state.json`. Non-zero → exit `EPIC_STATE_MISSING`.

Read the dimensions KP for epic dimension rules:

    Read: .claude/knowledge/refinement/dimensions.md

### A.2 Launch 5-6 parallel strategic persona-agents

**MANDATORY — emit ALL persona Agent() calls as SIBLING tool calls in ONE assistant message.**

Resolve active personas: 5 fixed always active + conditional SRE/DevOps (activate when capability `infra.observability.*` OR `infra.deploy.*` is active in the project profile — read from `settings.json` or project YAML).

Always active (5 core personas):

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-refine PO`

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "PO strategic analysis for epic-XXXX",
  prompt: "You are a Product Owner specialist performing strategic DoR gap analysis for an EPIC.
Read the epic markdown at {epicPath}. Read .claude/knowledge/refinement/dimensions.md §Epic Dimensions.
Analyse ONLY these dimensions for the PO persona: problem, persona (epic-level), hypothesis (value), okrs.
Apply these NO-GOs silently (do NOT ask about them — reject immediately):
- Hypothesis absent or is pure feature description (not in If/then/because form)
- No measurable KPI or OKR with unit+target+measurement-method defined
- Persona absent or is a copy of the system description
- Problem vague without observable evidence
Return a JSON gap-report:
{
  \"persona\": \"PO\",
  \"gaps\": [
    {\"dimension\": \"<name>\", \"issue\": \"<description>\", \"question\": \"<ask if severity=question>\", \"severity\": \"question|noGo\"}
  ]
}
If no gaps found, return {\"persona\": \"PO\", \"gaps\": []}.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase A › PO analysis\", activeForm: \"Analysing epic as PO\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Tech Lead strategic analysis for epic-XXXX",
  prompt: "You are a Tech Lead performing strategic DoR gap analysis for an EPIC.
Read the epic markdown at {epicPath}. Read .claude/knowledge/refinement/dimensions.md §Epic Dimensions.
Analyse ONLY these dimensions for Tech Lead: feasibility (technical viability), epic-dependencies (inter-epic blockers, circular dependencies).
Apply these NO-GOs silently:
- Circular epic dependencies declared (epic A depends on epic B which depends on A)
- Critical technical dependency flagged as unresolvable blocker
Return a JSON gap-report:
{\"persona\": \"TechLead\", \"gaps\": [{\"dimension\": ..., \"issue\": ..., \"question\": ..., \"severity\": \"question|noGo\"}]}.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase A › Tech Lead analysis\", activeForm: \"Analysing epic as Tech Lead\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Architect strategic analysis for epic-XXXX",
  prompt: "You are an Architect performing strategic DoR gap analysis for an EPIC.
Read the epic markdown at {epicPath}. Read .claude/knowledge/refinement/dimensions.md §Epic Dimensions.
Analyse ONLY these dimensions: strategic-alternatives (§5 Alternativas), architectural-impact, out-of-scope (§1.4 Fora do escopo — REQUIRED: ≥3 explicit items).
Apply these NO-GOs silently:
- Zero strategic alternatives documented (§5 absent or empty)
- Fewer than 2 alternatives with rejection rationale (§Epic Dimensions rule)
- Out-of-scope list absent OR has fewer than 3 explicit items (D4: heuristic preserved as Architect NO-GO)
Return a JSON gap-report:
{\"persona\": \"Architect\", \"gaps\": [{\"dimension\": ..., \"issue\": ..., \"question\": ..., \"severity\": \"question|noGo\"}]}.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase A › Architect analysis\", activeForm: \"Analysing epic as Architect\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Security strategic analysis for epic-XXXX",
  prompt: "You are a Security Engineer performing strategic DoR gap analysis for an EPIC.
Read the epic markdown at {epicPath}. Read .claude/knowledge/refinement/dimensions.md §Epic Dimensions.
Analyse ONLY: security-posture (does the epic address security concerns?), compliance-triggers (PCI/LGPD/HIPAA/SOC2 if domain is sensitive).
Apply these NO-GOs silently:
- Epic touches a sensitive domain (payments, healthcare, personal data, auth/identity) without declaring compliance requirements
- Cross-domain epic without threat-modeling scope declared
Return a JSON gap-report:
{\"persona\": \"Security\", \"gaps\": [{\"dimension\": ..., \"issue\": ..., \"question\": ..., \"severity\": \"question|noGo\"}]}.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase A › Security analysis\", activeForm: \"Analysing epic as Security\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "QA strategic analysis for epic-XXXX",
  prompt: "You are a QA Engineer performing strategic DoR gap analysis for an EPIC.
Read the epic markdown at {epicPath}. Read .claude/knowledge/refinement/dimensions.md §Epic Dimensions.
Analyse ONLY: quality-strategy (how do we know the epic shipped successfully?), smoke-scope (is a smoke test strategy declared?).
Apply these NO-GOs silently:
- No 'definition of done' or success criterion at epic level
- No smoke testing strategy mentioned
Return a JSON gap-report:
{\"persona\": \"QA\", \"gaps\": [{\"dimension\": ..., \"issue\": ..., \"question\": ..., \"severity\": \"question|noGo\"}]}.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase A › QA analysis\", activeForm: \"Analysing epic as QA\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

Conditional persona (activate when `infra.observability.*` OR `infra.deploy.*` capability active):

```
Agent(  [conditional: flag.has_infra_capability]
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "SRE/DevOps strategic analysis for epic-XXXX",
  prompt: "You are an SRE/DevOps Engineer performing strategic DoR gap analysis for an EPIC.
Read the epic markdown at {epicPath}. Read .claude/knowledge/refinement/dimensions.md §Epic Dimensions.
Analyse ONLY: operational-impact (how does this epic affect production operations?), rollback-strategy (is there a rollback plan?).
Apply these NO-GOs silently:
- Epic touches production systems without a rollback or feature-flag strategy declared
- Epic introduces new infrastructure without operational runbook or SLO targets
Return a JSON gap-report:
{\"persona\": \"SRE\", \"gaps\": [{\"dimension\": ..., \"issue\": ..., \"question\": ..., \"severity\": \"question|noGo\"}]}.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase A › SRE analysis\", activeForm: \"Analysing epic as SRE\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)
```

<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-refine PO ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-refine TechLead`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-refine TechLead ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-refine Architect`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-refine Architect ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-refine Security`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-refine Security ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-refine QA`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-refine QA ok`

Collect all gap-reports. Validate each is parseable JSON; discard malformed. If ALL are empty → exit `PHASE_A_EMPTY`.

Separate:
- `questions` — gaps with `severity: "question"` → feed into Phase B
- `noGos` — gaps with `severity: "noGo"` → pass directly to Phase D; **NOT surfaced as questions** (D-R15: NO-GOs silent)

    TaskUpdate(id: phaseATaskId, status: "completed")

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-epic-refine --phase Phase-A-StrategicAnalysis")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-refine Phase-A-StrategicAnalysis ok`

>>> Phase A completed. Proceeding to Phase B (Consolidation)...

---

## Phase B — Consolidate & Operator Q&A

<!-- phase-no-gate: Phase B is skipped entirely when --non-interactive; gate is inside conditional block -->

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-refine Phase-B-Consolidate`

**Skip Phase B when `--non-interactive`**: set `answers = {}` and proceed directly to Phase C.

Open phase tracker:

    TaskCreate(subject: "epic-XXXX › Phase B - Consolidate", activeForm: "Consolidating strategic questions and awaiting operator answers")

### B.1 Dedup and group strategic questions

From all Phase A `questions` (severity: "question"):
1. Deduplicate by semantic similarity (same dimension + same gap → keep one).
2. Group by strategic category: `Problema`, `Hipótese/OKRs`, `Alternativas`, `Segurança`, `Qualidade`, `Operações`.
3. Discard empty groups.

If no questions remain (all gaps were NO-GOs or all personas returned empty) → skip B.2, set `answers = {}`.

### B.2 Single batch to operator (D-R14: one batch only)

**EXACTLY ONE** `AskUserQuestion` call — no per-persona loops, no retry loops.

Present grouped strategic questions:

```
--- Epic Refinement Questions for epic-XXXX ---

The following strategic gaps were identified. Please answer each question.
(NO-GO blockers are tracked separately and will appear in the final verdict.)

[Problema]
Q1. <question from PO about problem evidence>

[Hipótese/OKRs]
Q2. <question from PO about measurable hypothesis>

[Alternativas]
Q3. <question from Architect about strategic alternatives>

[Segurança]
Q4. <question from Security about compliance posture>

[Qualidade]
Q5. <question from QA about definition of done>

[Operações]
Q6. <question from SRE about operational strategy>

Reply with answers numbered Q1, Q2, Q3, etc. Type SKIP to skip a question.
```

Parse the operator's reply into a map `{Q1: "<answer>", Q2: "<answer>", ...}`.
Map each answer back to its originating `{persona, dimension, gap}` entry.

    TaskUpdate(id: phaseBTaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-refine Phase-B-Consolidate ok`

>>> Phase B completed. Proceeding to Phase C (Strategic Refinement)...

---

## Phase C — Parallel Strategic Refinement

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-refine Phase-C-Refine`

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-epic-refine --phase Phase-C-Refine")

**Skip Phase C when `--non-interactive`**: set `proposedSections = {}` and proceed to Phase D.

Open phase tracker:

    TaskCreate(subject: "epic-XXXX › Phase C - Strategic Refinement", activeForm: "Running parallel strategic refinement with operator answers")

### C.1 Distribute answers and re-launch persona-agents

**MANDATORY — emit ALL persona Agent() calls as SIBLING tool calls in ONE assistant message.**

For each persona that had questions in Phase A, launch a refinement agent with the answers it owns:

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-refine PO-Refine`

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "PO strategic refinement for epic-XXXX",
  prompt: "You are a Product Owner specialist.
Read the epic markdown at {epicPath}.
The operator answered your strategic questions as follows: {answersForPO}.
Based on these answers, produce the proposed epic sections for your dimensions (problem, persona, hypothesis, okrs).
Return a JSON object: {\"persona\": \"PO\", \"proposedSections\": {\"problem\": \"<refined text>\", \"persona\": \"<refined text>\", \"hypothesis\": \"<refined text>\", \"okrs\": \"<refined text>\"}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase C › PO refinement\", activeForm: \"Refining epic as PO\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Tech Lead strategic refinement for epic-XXXX",
  prompt: "You are a Tech Lead specialist.
Read the epic markdown at {epicPath}.
The operator answered your strategic questions as follows: {answersForTechLead}.
Based on these answers, produce the proposed epic sections for your dimensions (feasibility, epic-dependencies).
Return a JSON object: {\"persona\": \"TechLead\", \"proposedSections\": {\"feasibility\": \"<refined text>\", \"epic-dependencies\": \"<refined text>\"}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase C › Tech Lead refinement\", activeForm: \"Refining epic as Tech Lead\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Architect strategic refinement for epic-XXXX",
  prompt: "You are an Architect specialist.
Read the epic markdown at {epicPath}.
The operator answered your strategic questions as follows: {answersForArchitect}.
Based on these answers, produce the proposed epic sections for your dimensions (strategic-alternatives, out-of-scope, architectural-impact).
Return a JSON object: {\"persona\": \"Architect\", \"proposedSections\": {\"alternatives\": \"<refined text>\", \"out-of-scope\": \"<refined text>\", \"architectural-impact\": \"<refined text>\"}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase C › Architect refinement\", activeForm: \"Refining epic as Architect\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "Security strategic refinement for epic-XXXX",
  prompt: "You are a Security specialist.
Read the epic markdown at {epicPath}.
The operator answered your strategic questions as follows: {answersForSecurity}.
Based on these answers, produce the proposed epic sections for your dimensions (security-posture, compliance-triggers).
Return a JSON object: {\"persona\": \"Security\", \"proposedSections\": {\"security-posture\": \"<refined text>\", \"compliance-triggers\": \"<refined text>\"}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase C › Security refinement\", activeForm: \"Refining epic as Security\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

```
Agent(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "QA strategic refinement for epic-XXXX",
  prompt: "You are a QA specialist.
Read the epic markdown at {epicPath}.
The operator answered your strategic questions as follows: {answersForQA}.
Based on these answers, produce the proposed epic sections for your dimensions (quality-strategy, smoke-scope).
Return a JSON object: {\"persona\": \"QA\", \"proposedSections\": {\"quality-strategy\": \"<refined text>\", \"smoke-scope\": \"<refined text>\"}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase C › QA refinement\", activeForm: \"Refining epic as QA\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-refine PO-Refine ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-refine TechLead-Refine`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-refine TechLead-Refine ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-refine Architect-Refine`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-refine Architect-Refine ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-refine Security-Refine`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-refine Security-Refine ok`

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-refine QA-Refine`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-refine QA-Refine ok`

Collect `proposedSections` from each agent. Merge into a single map keyed by section.

    TaskUpdate(id: phaseCTaskId, status: "completed")

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-epic-refine --phase Phase-C-Refine")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-refine Phase-C-Refine ok`

>>> Phase C completed. Proceeding to Phase D (Architect Consolidation)...

---

## Phase D — Architect Consolidation + Dual-Write

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-refine Phase-D-Architect`

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-epic-refine --phase Phase-D-Architect")

Open phase tracker:

    TaskCreate(subject: "epic-XXXX › Phase D - Architect Consolidation", activeForm: "Architect consolidating strategic refinement verdict")

### D.1 Architect consolidation (opus tier)

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-refine Architect-Consolidate`

```
Agent(
  subagent_type: "general-purpose",
  model: "opus",
  description: "Architect consolidation for epic-XXXX",
  prompt: "You are the Lead Architect and final consolidator for an epic strategic refinement session.

Epic: {epicPath}
Proposed section refinements from all personas: {proposedSections}
NO-GO blockers from Phase A: {noGos}
Operator answers summary: {answers}

Your job:
1. Review all proposed sections for strategic coherence.
2. Merge proposedSections into the appropriate epic markdown sections:
   - §1.3 Escopo (In-scope items)
   - §1.4 Fora do escopo (Out-of-scope items — must have ≥3 explicit items)
   - §6 Decisões Arquiteturais (add decision records for key strategic choices)
3. Evaluate all noGos — determine if each blocker is genuinely disqualifying or was resolved by operator answers.
4. Produce the final Refinement Verdict JSON:
{
  \"status\": \"approved\" | \"rejected\" | \"tbd\",
  \"scope\": \"epic\",
  \"checkedAt\": \"<ISO-8601 UTC>\",
  \"dimensions\": {
    \"problem\":    { \"checked\": true|false, \"blocker\": null | \"<reason>\" },
    \"persona\":    { \"checked\": true|false, \"blocker\": null | \"<reason>\" },
    \"value\":      { \"checked\": true|false, \"blocker\": null | \"<reason>\" },
    \"okrs\":       { \"checked\": true|false, \"blocker\": null | \"<reason>\" },
    \"alternatives\":{ \"checked\": true|false, \"blocker\": null | \"<reason>\" },
    \"risks\":      { \"checked\": true|false, \"blocker\": null | \"<reason>\" },
    \"scope\":      { \"checked\": true|false, \"blocker\": null | \"<reason>\" }
  },
  \"blockers\": [\"<dimension: reason>\"],
  \"refinedSections\": {\"<sectionName>\": \"<refined markdown text>\"},
  \"verdictRationale\": \"<1-2 sentence summary>\"
}
Rules:
- status=approved: ALL 7 dimensions checked=true AND blockers is empty.
- status=rejected: ANY dimension has blocker != null AND operator answers did not resolve it.
- status=tbd: gaps remain but no hard NO-GOs — operator interaction needed.

Apply Edit tool to the epic markdown file:
1. Update §1.3 Escopo with refined in-scope items.
2. Update §1.4 Fora do escopo with refined out-of-scope items (ensure ≥3 items).
3. Append new ADR entries to §6 Decisões Arquiteturais for key strategic decisions.
4. Append or replace the ## Refinement Verdict block (see format below).

## Refinement Verdict format to write:
---
## Refinement Verdict

**Status:** approved | rejected | tbd
**Scope:** epic
**Refined at:** <ISO-8601>
**Verdict hash:** <verdictHash — will be computed after JSON finalized>

### Dimensions

| Dimension | Status | Blocker |
|-----------|--------|---------|
| problem | passed/gap/noGo | — |
| persona | passed/gap/noGo | — |
| value | passed/gap/noGo | — |
| okrs | passed/gap/noGo | — |
| alternatives | passed/gap/noGo | — |
| risks | passed/gap/noGo | — |
| scope | passed/gap/noGo | — |

### Blockers

<none — or list of blockers>

### Rationale

<verdictRationale>
---

Return ONLY the JSON verdict object above. Apply file edits inline using the Edit tool. No prose outside JSON in return value.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase D › Architect verdict\", activeForm: \"Producing strategic Refinement Verdict\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-refine Architect-Consolidate ok`

Parse the Architect's JSON verdict. Validate `status` is one of `approved|rejected|tbd` and `scope` is `"epic"`.

### D.2 Compute verdictHash

Write the verdict JSON to compute hash (safe for non-ASCII):

Bash command: `printf '%s' '<verdict JSON>' | sha256sum | awk '{print $1}'`

### D.3 Dual-write (MANDATORY — NON-NEGOTIABLE)

**`--dry-run` guard**: skip D.3 writes when `--dry-run` is active.

**Write 1 — execution-state.json** (via x-internal-status-update, INLINE-SKILL):

    Skill(skill: "x-internal-status-update", args: "--file ai/epics/epic-XXXX/execution-state.json --type epic --id <EPIC-ID> --field refinementVerdict --value {\"status\":\"<status>\",\"scope\":\"epic\",\"checkedAt\":\"<iso>\",\"dimensions\":{...},\"blockers\":[...],\"verdictHash\":\"<hash>\"}")  [required]

Non-zero → exit `VERDICT_WRITE_FAILED`.

**Write 2 — Epic markdown** (replace `## Refinement Verdict` block):

The Architect agent in D.1 applies the Edit inline. If the Architect did not apply the edit (e.g., dry-run), the orchestrator writes it using the `Edit` tool directly — replace or append the `## Refinement Verdict` block with the computed verdictHash populated.

### D.4 Finalize

    TaskUpdate(id: phaseDTaskId, status: "completed")

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode final --skill x-epic-refine --phase Phase-D-Architect --expected-artifacts {epicPath},{epicStatePath}")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-refine Phase-D-Architect ok`

>>> Phase D completed. Refinement verdict written.

---

## Output Summary

Print a concise summary:

```
x-epic-refine completed for epic-XXXX
  Verdict: <approved|rejected|tbd>
  Blockers: <none | list>
  Verdict hash: <hash>
  State file: ai/epics/epic-XXXX.../execution-state.json (field: refinementVerdict.scope=epic)
  Epic markdown: {epicPath} (section: ## Refinement Verdict)
```

---

## Integration Notes

- **Consumed by:** `x-epic-implement` (Phase 0: checks `refinementVerdict.status == "approved"` AND `scope == "epic"` via `enforce-refinement-gate.sh` hook).
- **Parallel with:** `x-story-refine` (distinct write paths; shared `dimensions.md` KP is read-only).
- **Depends on:** `x-internal-status-update` (INLINE-SKILL, Rule 13 Pattern 1), `knowledge/refinement/dimensions.md` (KP, story-0069-0001).
- **Verdict drift detection:** `verdictHash` in state file vs `verdictHash` in epic markdown is compared by `audit-refinement-gate.sh` (story-0069-0006). Drift fails CI with `REFINEMENT_VERDICT_DRIFT`.
- **Re-run idempotency:** Running x-epic-refine again on an `approved` epic replaces the verdict. This is intentional — re-refinement after epic scope changes should update the verdict.
- **Scope discriminator:** This skill always writes `scope: "epic"`. The gate `enforce-refinement-gate.sh` checks both `status == "approved"` AND `scope == "epic"` to unblock `x-epic-implement`. Story-level `scope: "story"` verdicts do NOT unblock epic-level gates.
