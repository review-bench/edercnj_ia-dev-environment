# x-refine-story — Full Protocol Reference

> This file extends `SKILL.md` (body-slim pattern, ADR-0012). Reading this file is only required
> for atypical scenarios or when extending the persona set. Happy-path invocations need only
> the SKILL.md.

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

All Architect work in story-level refinement is consolidation (Phase D) — no silent NO-GOs
for story scope. Architect provides cross-cutting review and may add advisory notes.

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

### Performance Engineer NO-GOs (conditional: `build.maven.standard` + perf story scope)

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `metrics` | No latency/throughput SLO declared for API-facing story | `"metrics: API-facing story requires latency SLO (p50/p95/p99 + timeout)"` |

### SRE/DevOps NO-GOs (conditional: `infra.*` or `runtime.*` capability)

| Dimension | Condition | NO-GO message |
| :--- | :--- | :--- |
| `risks` | Deployment-affecting story without rollback consideration | `"risks: deployment change without rollback consideration"` |

---

## §2 — Phase A: Parallel Dispatch Prompt Templates

### Common preamble (injected into every persona agent prompt)

```
You are the {PERSONA} persona reviewing story {STORY_ID} for DoR (Definition of Ready) quality.
Story markdown: {STORY_PATH}
Dimensions you own: {OWNED_DIMENSIONS}
Knowledge pack: .claude/knowledge/refinement/dimensions.md §Story Dimensions

Return a JSON gap-report:
{
  "persona": "{PERSONA_KEY}",
  "gaps": [
    {
      "dimension": "<dimension name>",
      "issue": "<brief description of the gap>",
      "question": "<question to ask operator OR null if NO-GO>",
      "severity": "question" | "noGo"
    }
  ]
}

IMPORTANT: NO-GOs are SILENT — never surfaced as questions.
Set severity="noGo" and question=null when applying a NO-GO rule.
Return gaps: [] (empty) when all dimensions pass.
```

### Phase A result aggregation

After all persona agents return:
1. Collect all gap-reports into `allGaps`.
2. `questions = gaps.filter(g => g.severity == "question")`.
3. `noGos = gaps.filter(g => g.severity == "noGo")`.
4. If both empty → jump to Phase D with `status="approved"`.
5. `noGos.length > 0` → final verdict will be `status="rejected"`.
6. `questions.length > 0` → proceed to Phase B for operator Q&A.

---

## §3 — Phase B: Question Grouping Categories

| Category label | Dimensions that map to it |
| :--- | :--- |
| `Persona & Valor` | `persona`, `value` |
| `Critérios de Aceite` | `ac` |
| `Contratos & Interfaces` | `contracts` |
| `Métricas` | `metrics` |
| `Alternativas` | `alternatives` |
| `Riscos` | `risks` |

Deduplication: two questions are duplicates when `dimension` is identical and `issue` token
overlap > 60%. Keep the one with more specific `question` wording.

---

## §4 — Phase D: Architect Consolidation Algorithm

The Architect receives all persona `proposedSections` + original story markdown + instruction
to merge contributions and generate the `## Refinement Verdict` block.

**Section merge targets** in story markdown:
| Source persona | Target section |
| :--- | :--- |
| PO | §1 Visão (value proposition + measurable metric) |
| Tech Lead | §3 Contratos & Endpoints (typed request/response) |
| Security | §6 Segurança (controls table) |
| QA | §5.2 Acceptance Criteria (completed Gherkin) |
| Performance | §7.2 Metrics (SLO table) |
| SRE/DevOps | §7 Observabilidade (operational risks) |

**Minimum for `approved`:** PO + Tech Lead + Architect + Security + QA must all pass (no blockers).
Conditional personas' verdicts are advisory unless the story explicitly touches their domain.

**Verdict hash computation:**

```bash
verdict_block=$(awk '/^## Refinement Verdict/,/^## /' story.md | head -n -1)
verdict_hash=$(echo -n "$verdict_block" | sha256sum | awk '{print $1}')
```

---

## §5 — Error Codes Detail

| Code | Condition | Recovery |
| :--- | :--- | :--- |
| `STORY_NOT_FOUND` | `ai/epics/epic-XXXX*/story-XXXX-YYYY.md` absent | Verify story file path; check `execution-state.json` for correct epic slug |
| `STORY_STATE_MISSING` | `execution-state.json` absent for the epic | Create with correct `flowVersion` and retry |
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

Gate check: `status == "approved" AND scope == "story"`. An epic-scoped verdict does NOT
unblock story implementation — scope discriminator prevents cross-level confusion.

If the gate fails, `enforce-refinement-gate.sh` (Camada 0 PreToolUse hook) exits 33
(`REFINEMENT_REQUIRED`).
