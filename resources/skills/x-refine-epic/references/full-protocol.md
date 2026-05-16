# x-refine-epic — Full Protocol Reference

> Verbose detail moved out of `SKILL.md` per ADR-0012. SKILL.md remains the minimum viable behavioral contract; this document carries the full phase-by-phase protocol, persona `Agent()` prompts, telemetry sub-markers, schemas, and editorial rules. Loaded on-demand via `Read` only when the slim contract is insufficient.

---

## §1 — Persona NO-GO Rules (Extended)

### Product Owner (PO) NO-GOs

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `hypothesis` | Value hypothesis lacks measurable KPI | `"hypothesis: missing measurable indicator (Rule 05 quality-gate implicit)"` |
| `okrs` | OKR declared without baseline + target + horizon | `"okrs: OKR missing baseline, target, or horizon (unmeasurable)"` |
| `persona` | Fewer than 2 distinguishable roles identified | `"persona: fewer than 2 distinguishable roles (heuristic)"` |

### Tech Lead NO-GOs

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `feasibility` | Circular epic dependency detected | `"feasibility: circular dependency in epic dependency graph"` |
| `feasibility` | Unresolvable technical blocker named | `"feasibility: unresolvable technical blocker declared"` |

### Architect NO-GOs

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `strategic-alternatives` | Zero alternatives documented | `"alternatives: no strategic alternatives considered (Rule 29 §Epic Dimensions)"` |
| `out-of-scope` | Out-of-scope section empty or < 3 explicit items | `"out-of-scope: empty or < 3 explicit items (heuristic D4)"` |

### Security NO-GOs

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `security-posture` | Epic touches PCI/LGPD/HIPAA domain without declared compliance triggers | `"compliance triggers required for sensitive domain: {detected domain}"` |
| `security-posture` | Cross-domain epic (multiple bounded contexts) without threat-modeling scope | `"cross-domain epic requires threat-modeling scope declaration"` |

### QA NO-GOs

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `quality-strategy` | No "how do we know it shipped" criterion (Definition of Done at epic level) | `"quality-strategy: no epic-level Done criterion declared"` |
| `smoke-scope` | No smoke strategy declared for production-touching epic | `"smoke-scope: no smoke strategy for production-touching epic"` |

### SRE/DevOps NO-GOs (conditional: `flag.has_infra_capability`)

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `rollback-strategy` | Production-touching epic without rollback plan | `"rollback-strategy: production-touching epic requires rollback plan"` |

---

## §A — Phase A: Parallel Strategic Analysis

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-refine-epic Phase-A-StrategicAnalysis

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-refine-epic --phase Phase-A-StrategicAnalysis")

TaskCreate(subject: "epic-XXXX › Phase A - Strategic Analysis", activeForm: "Running parallel strategic persona analysis")
```

### A.1 Resolve epic path and state

Probe for epic markdown using glob `ai/epics/epic-XXXX-*/epic-XXXX.md`. Non-zero → exit `EPIC_NOT_FOUND`. Probe for `ai/epics/epic-XXXX-*/execution-state.json`. Non-zero → exit `EPIC_STATE_MISSING`.

Read the dimensions KP for epic dimension rules:

```text
Read: .claude/knowledge/refinement/dimensions.md
```

### A.2 Launch 5–6 parallel strategic persona-agents

**MANDATORY — emit ALL persona `Agent()` calls as SIBLING tool calls in ONE assistant message.**

Resolve active personas: 5 fixed always active + conditional SRE/DevOps (activate when capability `infra.observability.*` OR `infra.deploy.*` is active in the project profile — read from `settings.json` or project YAML).

#### Always active (5 core personas)

```text
Agent(
  subagent_type: "product-owner",
  model: "sonnet",
  description: "PO strategic analysis for epic-XXXX",
  prompt: "Perform strategic DoR gap analysis for an EPIC.
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

Agent(
  subagent_type: "tech-lead",
  model: "sonnet",
  description: "Tech Lead strategic analysis for epic-XXXX",
  prompt: "Perform strategic DoR gap analysis for an EPIC.
Read the epic markdown at {epicPath}. Read .claude/knowledge/refinement/dimensions.md §Epic Dimensions.
Analyse ONLY these dimensions for Tech Lead: feasibility (technical viability), epic-dependencies (inter-epic blockers, circular dependencies).
Apply these NO-GOs silently:
- Circular epic dependencies declared (epic A depends on epic B which depends on A)
- Critical technical dependency flagged as unresolvable blocker
Return a JSON gap-report: {\"persona\": \"TechLead\", \"gaps\": [...]}.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase A › Tech Lead analysis\", activeForm: \"Analysing epic as Tech Lead\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]

Agent(
  subagent_type: "architect",
  model: "sonnet",
  description: "Architect strategic analysis for epic-XXXX",
  prompt: "Perform strategic DoR gap analysis for an EPIC.
Read the epic markdown at {epicPath}. Read .claude/knowledge/refinement/dimensions.md §Epic Dimensions.
Analyse ONLY these dimensions: strategic-alternatives (§5 Alternativas), architectural-impact, out-of-scope (§1.4 Fora do escopo — REQUIRED: ≥3 explicit items).
Apply these NO-GOs silently:
- Zero strategic alternatives documented (§5 absent or empty)
- Fewer than 2 alternatives with rejection rationale (§Epic Dimensions rule)
- Out-of-scope list absent OR has fewer than 3 explicit items (D4: heuristic preserved as Architect NO-GO)
Return a JSON gap-report: {\"persona\": \"Architect\", \"gaps\": [...]}.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase A › Architect analysis\", activeForm: \"Analysing epic as Architect\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]

Agent(
  subagent_type: "security-engineer",
  model: "sonnet",
  description: "Security strategic analysis for epic-XXXX",
  prompt: "Perform strategic DoR gap analysis for an EPIC.
Read the epic markdown at {epicPath}. Read .claude/knowledge/refinement/dimensions.md §Epic Dimensions.
Analyse ONLY: security-posture (does the epic address security concerns?), compliance-triggers (PCI/LGPD/HIPAA/SOC2 if domain is sensitive).
Apply these NO-GOs silently:
- Epic touches a sensitive domain (payments, healthcare, personal data, auth/identity) without declaring compliance requirements
- Cross-domain epic without threat-modeling scope declared
Return a JSON gap-report: {\"persona\": \"Security\", \"gaps\": [...]}.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase A › Security analysis\", activeForm: \"Analysing epic as Security\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]

Agent(
  subagent_type: "qa-engineer",
  model: "sonnet",
  description: "QA strategic analysis for epic-XXXX",
  prompt: "Perform strategic DoR gap analysis for an EPIC.
Read the epic markdown at {epicPath}. Read .claude/knowledge/refinement/dimensions.md §Epic Dimensions.
Analyse ONLY: quality-strategy (how do we know the epic shipped successfully?), smoke-scope (is a smoke test strategy declared?).
Apply these NO-GOs silently:
- No 'definition of done' or success criterion at epic level
- No smoke testing strategy mentioned
Return a JSON gap-report: {\"persona\": \"QA\", \"gaps\": [...]}.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase A › QA analysis\", activeForm: \"Analysing epic as QA\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

#### Conditional persona (activate when `infra.observability.*` OR `infra.deploy.*` capability active)

```text
Agent(  [conditional: flag.has_infra_capability]
  subagent_type: "sre-engineer",
  model: "sonnet",
  description: "SRE/DevOps strategic analysis for epic-XXXX",
  prompt: "Perform strategic DoR gap analysis for an EPIC.
Read the epic markdown at {epicPath}. Read .claude/knowledge/refinement/dimensions.md §Epic Dimensions.
Analyse ONLY: operational-impact (how does this epic affect production operations?), rollback-strategy (is there a rollback plan?).
Apply these NO-GOs silently:
- Epic touches production systems without a rollback or feature-flag strategy declared
- Epic introduces new infrastructure without operational runbook or SLO targets
Return a JSON gap-report: {\"persona\": \"SRE\", \"gaps\": [...]}.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase A › SRE analysis\", activeForm: \"Analysing epic as SRE\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)
```

Telemetry markers wrap each `subagent.start` / `subagent.end` per persona using `telemetry-phase.sh subagent-start/end x-refine-epic <PersonaKey>`.

### A.3 Aggregation

Collect all gap-reports. Validate each is parseable JSON; discard malformed. If ALL are empty → exit `PHASE_A_EMPTY`.

Separate:

- `questions` — gaps with `severity: "question"` → feed into Phase B
- `noGos` — gaps with `severity: "noGo"` → pass directly to Phase D; **NOT surfaced as questions** (D-R15: NO-GOs silent)

```text
TaskUpdate(id: phaseATaskId, status: "completed")
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode post --skill x-refine-epic --phase Phase-A-StrategicAnalysis")

<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-refine-epic Phase-A-StrategicAnalysis ok
```

### Phase A result aggregation rules

1. Collect all gap-reports into `allGaps` array.
2. Separate: `questions = gaps.filter(g => g.severity == "question")`.
3. Separate: `noGos = gaps.filter(g => g.severity == "noGo")`.
4. If `questions.length == 0 AND noGos.length == 0` → jump to Phase D with `status="approved"`.
5. If `noGos.length > 0` → the final verdict will be `status="rejected"` regardless of Q&A answers.
6. If `questions.length > 0` → proceed to Phase B for operator Q&A.

---

## §B — Phase B: Consolidate & Operator Q&A

<!-- phase-no-gate: Phase B is skipped entirely when --non-interactive; gate is inside conditional block -->

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-refine-epic Phase-B-Consolidate
```

**Skip Phase B when `--non-interactive`**: set `answers = {}` and proceed directly to Phase C.

```text
TaskCreate(subject: "epic-XXXX › Phase B - Consolidate", activeForm: "Consolidating strategic questions and awaiting operator answers")
```

### B.1 Dedup and group strategic questions

From all Phase A `questions` (severity: "question"):

1. Deduplicate by semantic similarity (same dimension + same gap → keep one).
2. Group by strategic category: `Problema`, `Hipótese/OKRs`, `Alternativas`, `Segurança`, `Qualidade`, `Operações`.
3. Discard empty groups.

If no questions remain (all gaps were NO-GOs or all personas returned empty) → skip B.2, set `answers = {}`.

#### Question grouping categories

| Category label | Dimensions that map to it |
| :--- | :--- |
| `Problema` | `problem`, `persona-broad` |
| `Hipótese/OKRs` | `hypothesis`, `okrs`, `value` |
| `Alternativas` | `strategic-alternatives`, `feasibility` |
| `Segurança` | `security-posture`, `compliance-triggers` |
| `Qualidade` | `quality-strategy`, `smoke-scope` |
| `Operações` | `operational-impact`, `rollback-strategy` |

Deduplication rule: two questions are considered duplicates when their `dimension` field is identical and their `issue` fields have >60% token overlap (heuristic). Keep the one with more specific `question` wording.

### B.2 Single batch to operator (D-R14: one batch only)

**EXACTLY ONE** `AskUserQuestion` call — no per-persona loops, no retry loops.

Present grouped strategic questions:

```text
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

Parse the operator's reply into a map `{Q1: "<answer>", Q2: "<answer>", ...}`. Map each answer back to its originating `{persona, dimension, gap}` entry.

```text
TaskUpdate(id: phaseBTaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-refine-epic Phase-B-Consolidate ok
```

---

## §C — Phase C: Parallel Strategic Refinement

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-refine-epic Phase-C-Refine

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-refine-epic --phase Phase-C-Refine")
```

**Skip Phase C when `--non-interactive`**: set `proposedSections = {}` and proceed to Phase D.

```text
TaskCreate(subject: "epic-XXXX › Phase C - Strategic Refinement", activeForm: "Running parallel strategic refinement with operator answers")
```

### C.1 Distribute answers and re-launch persona-agents

**MANDATORY — emit ALL persona `Agent()` calls as SIBLING tool calls in ONE assistant message.**

For each persona that had questions in Phase A, launch a refinement agent with the answers it owns. The prompt template is identical for all personas with the dimension list swapped in:

```text
Agent(
  subagent_type: "<persona-type>",
  model: "sonnet",
  description: "<Persona> strategic refinement for epic-XXXX",
  prompt: "Read the epic markdown at {epicPath}.
The operator answered your strategic questions as follows: {answersFor<Persona>}.
Based on these answers, produce the proposed epic sections for your dimensions (<dimensions for this persona>).
Return a JSON object: {\"persona\": \"<Key>\", \"proposedSections\": {\"<sectionName>\": \"<refined text>\", ...}}.
Only include sections where you have refinements — omit unchanged sections.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase C › <Persona> refinement\", activeForm: \"Refining epic as <Persona>\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

Active personas + their dimensions:

- **PO** (`product-owner`): `problem, persona, hypothesis, okrs`
- **TechLead** (`tech-lead`): `feasibility, epic-dependencies`
- **Architect** (`architect`): `strategic-alternatives, out-of-scope, architectural-impact`
- **Security** (`security-engineer`): `security-posture, compliance-triggers`
- **QA** (`qa-engineer`): `quality-strategy, smoke-scope`
- **SRE** (`sre-engineer`, conditional): `operational-impact, rollback-strategy`

Telemetry markers wrap each `subagent.start` / `subagent.end` per persona using `telemetry-phase.sh subagent-start/end x-refine-epic <PersonaKey>-Refine`.

Collect `proposedSections` from each agent. Merge into a single map keyed by section.

```text
TaskUpdate(id: phaseCTaskId, status: "completed")
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode post --skill x-refine-epic --phase Phase-C-Refine")

<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-refine-epic Phase-C-Refine ok
```

---

## §D — Phase D: Architect Consolidation + Dual-Write

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-refine-epic Phase-D-Architect

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-refine-epic --phase Phase-D-Architect")

TaskCreate(subject: "epic-XXXX › Phase D - Architect Consolidation", activeForm: "Architect consolidating strategic refinement verdict")
```

### D.1 Architect consolidation (opus tier)

```text
Agent(
  subagent_type: "architect",
  model: "opus",
  description: "Architect consolidation for epic-XXXX",
  prompt: "You are the final consolidator for an epic strategic refinement session.

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

Return ONLY the JSON verdict object above. Apply file edits inline using the Edit tool. No prose outside JSON in return value.
FIRST ACTION: TaskCreate(subject: \"epic-XXXX › Phase D › Architect verdict\", activeForm: \"Producing strategic Refinement Verdict\").
LAST ACTION: TaskUpdate(status: \"completed\")."
)  [required]
```

### Refinement Verdict markdown format (written into epic file)

```markdown
---
## Refinement Verdict

**Status:** approved | rejected | tbd
**Scope:** epic
**Refined at:** <ISO-8601>
**Verdict hash:** <verdictHash — computed after JSON finalized>

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
```

Parse the Architect's JSON verdict. Validate `status` is one of `approved|rejected|tbd` and `scope` is `"epic"`.

### D.2 Compute verdictHash

Write the verdict JSON to compute hash (safe for non-ASCII):

```bash
printf '%s' '<verdict JSON>' | sha256sum | awk '{print $1}'
```

Alternative (extract block from epic markdown post-write):

```bash
verdict_block=$(awk '/^## Refinement Verdict/,/^## /' epic.md | head -n -1)
verdict_hash=$(echo -n "$verdict_block" | sha256sum | awk '{print $1}')
```

### D.3 Dual-write (MANDATORY — NON-NEGOTIABLE)

**`--dry-run` guard**: skip D.3 writes when `--dry-run` is active.

**Write 1 — execution-state.json** (via x-internal-update-status, INLINE-SKILL):

```text
Skill(skill: "x-internal-update-status", args: "--file ai/epics/epic-XXXX/execution-state.json --type epic --id <EPIC-ID> --field refinementVerdict --value {\"status\":\"<status>\",\"scope\":\"epic\",\"checkedAt\":\"<iso>\",\"dimensions\":{...},\"blockers\":[...],\"verdictHash\":\"<hash>\"}")  [required]
```

Non-zero → exit `VERDICT_WRITE_FAILED`.

**Write 2 — Epic markdown** (replace `## Refinement Verdict` block):

The Architect agent in D.1 applies the Edit inline. If the Architect did not apply the edit (e.g., dry-run), the orchestrator writes it using the `Edit` tool directly — replace or append the `## Refinement Verdict` block with the computed verdictHash populated.

### D.4 Finalize

```text
TaskUpdate(id: phaseDTaskId, status: "completed")

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode final --skill x-refine-epic --phase Phase-D-Architect --expected-artifacts {epicPath},{epicStatePath}")

<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-refine-epic Phase-D-Architect ok
```

### Section merge targets in epic markdown

| Source persona | Target section |
| :--- | :--- |
| PO | §1.3 Escopo / §1.4 Fora do escopo (value hypothesis, persona list) |
| Tech Lead | §7 Dependências (epic-level technical blockers + inter-epic dependencies) |
| Architect | §1.4 Fora do escopo, §6 Decisões Arquiteturais (alternatives table) |
| Security | §8 Segurança / §9 Compliance (posture + compliance triggers) |
| QA | §5.2 Acceptance Criteria (epic-level DoD) |
| SRE/DevOps | §10 Operações (rollback strategy, SLO baseline) |

---

## §5 — Error Codes Detail

| Code | Condition | Recovery |
| :--- | :--- | :--- |
| `EPIC_NOT_FOUND` | `ai/epics/epic-XXXX*/epic-XXXX.md` absent on disk | Run `x-create-feature` or create epic manually |
| `EPIC_STATE_MISSING` | `execution-state.json` absent for the epic | Create via `x-internal-update-status --initialize` and retry |
| `PHASE_A_EMPTY` | All persona agents returned `gaps: []` with no questions and no NO-GOs but status remains tbd | Diagnostic: verify persona prompts include the `dimensions.md` KP |
| `VERDICT_WRITE_FAILED` | `x-internal-update-status` returned non-zero | Check `execution-state.json` permissions; verify `--file` path is correct |

---

## §6 — Integration with x-implement-epic Phase 0 Gate

`x-implement-epic` reads `execution-state.json` in Phase 0 to detect `refinementVerdict`:

```json
{
  "refinementVerdict": {
    "status": "approved",
    "scope": "epic"
  }
}
```

Gate check: `status == "approved" AND scope == "epic"`. Both conditions required. A story-scoped verdict (`scope: "story"`) does NOT unblock `x-implement-epic` — the scope discriminator prevents confusing story-level refinement with epic-level refinement.

If the gate fails, `enforce-refinement-gate.sh` (Camada 0 PreToolUse hook) exits 33 (`REFINEMENT_REQUIRED`) and blocks invocation.
