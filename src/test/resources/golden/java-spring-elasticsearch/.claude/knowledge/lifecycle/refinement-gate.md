---
name: kp-lifecycle-refinement-gate
description: "Full reference for Rule 29 Refinement Gate: state machine, 7 story dimensions, 7 epic dimensions, personas table, refinementVerdict JSON schema, and audit enforcement details."
requires-capabilities: [governance.refinement-gate]
---

# Knowledge Pack: Refinement Gate (Rule 29 — Full Reference)

## State Machine (Extended)

| State | Meaning |
| :--- | :--- |
| `Pendente` | Story/epic created; not yet refined |
| **`Refinada`** | Refinement gate passed; `refinementVerdict.status = "approved"` |
| `Planejada` | Planning artifacts generated |
| `Em Andamento` | Implementation orchestrator running |
| `Concluída` | All tasks done; PR merged; evidence artifacts present |
| `Falha` | Orchestrator terminated with error |
| `Bloqueada` | Dependency unresolved |

**Invariant:** `Pendente → Em Andamento` transition is **forbidden** after EPIC-0069. `enforce-refinement-gate.sh` blocks it at Camada 0. Exception: `flowVersion=1` legacy epics and `hotfix/*` branches.

## `refinementVerdict` JSON Shape

```json
{
  "refinementVerdict": {
    "status": "approved" | "rejected" | "tbd",
    "scope": "story" | "epic",
    "checkedAt": "<ISO-8601>",
    "verdictHash": "<sha256 of ## Refinement Verdict block in the markdown>",
    "dimensions": {
      "persona":       { "checked": true, "blocker": null },
      "value":         { "checked": true, "blocker": null },
      "ac":            { "checked": true, "blocker": null },
      "contracts":     { "checked": true, "blocker": null },
      "metrics":       { "checked": true, "blocker": null },
      "alternatives":  { "checked": true, "blocker": null },
      "risks":         { "checked": true, "blocker": null }
    },
    "blockers": []
  }
}
```

| Field | Type | Description |
| :--- | :--- | :--- |
| `status` | Enum | `approved` = all dimensions checked, no blockers; `rejected` = ≥1 blocker; `tbd` = not run |
| `verdictHash` | sha256 | Hash of `## Refinement Verdict` block — used by CI to detect manual divergence |
| `blockers` | String[] | Aggregated blockers across all personas |

## Story Refinement Personas (`/x-refine-story`)

| Persona | Dimensions owned | Conditional on |
| :--- | :--- | :--- |
| Product Owner | persona, value, ac | always |
| Tech Lead | contracts, ac (typed) | always |
| Architect | consolidation (Phase D) | always |
| Security Engineer | risks (security angle) | always |
| QA Engineer | ac (Gherkin completeness), risks (quality) | always |
| Performance Engineer | metrics (latency/throughput), risks (perf) | capability `build.maven.standard` + perf story scope |
| SRE/DevOps | risks (operational), metrics (SLOs) | capability `infra.*` or `runtime.*` |

**Minimum for `approved`:** PO + Tech Lead + Architect + Security + QA must all pass (no blockers).

## Story Dimensions (6 Required + 1 Optional)

| # | Dimension | Check |
| :--- | :--- | :--- |
| 1 | **persona** | Role is specific; not "sistema"; maps to a real user |
| 2 | **value** | Value proposition is falsifiable; not "melhora X" |
| 3 | **ac** | AC in Gherkin; ≥4 categories: happy-path, error/boundary, performance/SLA, security/auth |
| 4 | **contracts** | Request/response typed; no `Object`/`Map<String, Any>` |
| 5 | **metrics** | ≥1 measurable metric with unit and target |
| 6 | **alternatives** | ≥1 alternative considered with rejection rationale |
| 7 | **risks** | ≥1 risk identified; dependency/external service/data migration noted |

## Epic Refinement Personas (`/x-refine-epic`)

| Persona | Dimensions owned | Conditional on |
| :--- | :--- | :--- |
| Product Owner | problem, persona (broad), value hypothesis | always |
| Tech Lead | OKRs/KPIs, strategic alternatives | always |
| Architect | out-of-scope framing, consolidation | always |
| Security Engineer | security posture, risk | always |
| QA Engineer | quality posture, testability | always |
| SRE/DevOps | operational risks, SLOs | capability `infra.*` or `runtime.*` |

## Epic Dimensions (7 Required)

| # | Dimension | Check |
| :--- | :--- | :--- |
| 1 | **problem** | Problem stated as observable pain with evidence |
| 2 | **persona** | Affected persona is specific |
| 3 | **value** | Hypothesis in `If…then…because` form |
| 4 | **okrs** | ≥1 OKR/KPI with unit and measurement method |
| 5 | **alternatives** | ≥2 alternatives with rejection rationale |
| 6 | **risks** | ≥1 product risk + ≥1 technical risk |
| 7 | **scope** | In-scope list AND out-of-scope list (≥3 out-of-scope items) |

## Enforcement

| Camada | Mechanism | Trigger | Exit |
| :--- | :--- | :--- | :--- |
| **0** | `enforce-refinement-gate.sh` | PreToolUse on `x-implement-*`, `x-orchestrate-epic` | 33 `REFINEMENT_REQUIRED` |
| **1** | Rule 29 + CLAUDE.md | Every conversation | — |
| **2** | `audit-refinement-gate.sh` | PR open/sync to `develop` or `epic/*` | 1 `REFINEMENT_GATE_VIOLATION` |
| **3** | `Epic0069RefinementGateSmokeIT` | `mvn verify` | JUnit failure |
