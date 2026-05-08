# x-refine-epic — Full Protocol Reference

> This file extends `SKILL.md` (body-slim pattern, ADR-0012). Reading this file is only required
> for atypical scenarios or when extending the persona set. Happy-path invocations need only
> the SKILL.md.

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

## §2 — Phase A: Parallel Dispatch Prompt Templates

### Common preamble (injected into every persona agent prompt)

```
You are the {PERSONA} persona reviewing epic {EPIC_ID} for strategic refinement quality.
Epic markdown: {EPIC_PATH}
Dimensions you own: {OWNED_DIMENSIONS}
Knowledge pack: .claude/knowledge/refinement/dimensions.md §Epic Dimensions

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

IMPORTANT: NO-GOs are SILENT — they are never surfaced as questions.
Set severity="noGo" and question=null when applying a NO-GO rule.
Return gaps: [] (empty array) when all dimensions pass.
```

### Phase A result aggregation

After all persona agents return:
1. Collect all gap-reports into `allGaps` array.
2. Separate: `questions = gaps.filter(g => g.severity == "question")`.
3. Separate: `noGos = gaps.filter(g => g.severity == "noGo")`.
4. If `questions.length == 0 AND noGos.length == 0` → jump to Phase D with `status="approved"`.
5. If `noGos.length > 0` → the final verdict will be `status="rejected"` regardless of Q&A answers.
6. If `questions.length > 0` → proceed to Phase B for operator Q&A.

---

## §3 — Phase B: Question Grouping Categories

Phase B deduplicates and groups strategic questions by category before emitting the single
`AskUserQuestion` call. Category mapping:

| Category label | Dimensions that map to it |
| :--- | :--- |
| `Problema` | `problem`, `persona-broad` |
| `Hipótese/OKRs` | `hypothesis`, `okrs`, `value` |
| `Alternativas` | `strategic-alternatives`, `feasibility` |
| `Segurança` | `security-posture`, `compliance-triggers` |
| `Qualidade` | `quality-strategy`, `smoke-scope` |
| `Operações` | `operational-impact`, `rollback-strategy` |

Deduplication rule: two questions are considered duplicates when their `dimension` field is
identical and their `issue` fields have >60% token overlap (heuristic). Keep the one with
more specific `question` wording.

---

## §4 — Phase D: Architect Consolidation Algorithm

The Architect agent in Phase D receives:
- All persona `proposedSections` from Phase C
- The original epic markdown
- The current `## Refinement Verdict` block (if any)
- Instruction: merge contributions + generate single canonical `## Refinement Verdict`

**Section merge targets** in epic markdown:
| Source persona | Target section |
| :--- | :--- |
| PO | §1.3 Escopo / §1.4 Fora do escopo (value hypothesis, persona list) |
| Tech Lead | §7 Dependências (epic-level technical blockers + inter-epic dependencies) |
| Architect | §1.4 Fora do escopo, §6 Decisões Arquiteturais (alternatives table) |
| Security | §8 Segurança / §9 Compliance (posture + compliance triggers) |
| QA | §5.2 Acceptance Criteria (epic-level DoD) |
| SRE/DevOps | §10 Operações (rollback strategy, SLO baseline) |

**Verdict hash computation:**

```bash
# Extract the ## Refinement Verdict block from the epic markdown and hash it
verdict_block=$(awk '/^## Refinement Verdict/,/^## /' epic.md | head -n -1)
verdict_hash=$(echo -n "$verdict_block" | sha256sum | awk '{print $1}')
```

---

## §5 — Error Codes Detail

| Code | Condition | Recovery |
| :--- | :--- | :--- |
| `EPIC_NOT_FOUND` | `ai/epics/epic-XXXX*/epic-XXXX.md` absent on disk | Run `x-create-feature` or create epic manually |
| `EPIC_STATE_MISSING` | `execution-state.json` absent for the epic | Create with `flowVersion: "4"` and retry |
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

Gate check: `status == "approved" AND scope == "epic"`. Both conditions required. A story-scoped
verdict (`scope: "story"`) does NOT unblock `x-implement-epic` — the scope discriminator prevents
confusing story-level refinement with epic-level refinement.

If the gate fails, `enforce-refinement-gate.sh` (Camada 0 PreToolUse hook) exits 33
(`REFINEMENT_REQUIRED`) and blocks invocation.
