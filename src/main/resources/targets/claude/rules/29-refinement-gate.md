# Rule 29 — Refinement Gate

> **Related:** Rule 13 (Skill Invocation Protocol), Rule 19 (Backward Compatibility), Rule 22 (Skill Visibility), Rule 24 (Execution Integrity), Rule 26 (Audit Gate Lifecycle), Rule 27 (Zero-Bypass Lifecycle).
> **Introduced by:** EPIC-0069 (Story Refinement & DoR Gate).
> **ADR:** [ADR-0022 — Refinement Gate](../../docs/adr/ADR-0022-refinement-gate.md).
> **Capability:** `governance.refinement-gate` (`capabilities/governance/refinement-gate.yaml`).

## Purpose

Stories and epics arrive at `x-implement-story` / `x-implement-epic` **without proof of refinement**. The seven planning artifacts (arch, impl, tests, tasks, security, compliance + story file) validate that the implementation is coherent — but they do not validate that the story itself is well-formed: clear persona, measurable acceptance criteria, typed contracts, defined metrics, considered alternatives, and identified risks.

Rule 29 introduces a **blocking refinement gate** that enforces:

1. Every story and epic has a `refinementVerdict.status = "approved"` entry in `execution-state.json` before `x-implement-story`, `x-implement-epic`, `x-implement-task`, or `x-orchestrate-epic` may be invoked.
2. The `refinementVerdict` is produced exclusively by `/x-refine-story` (for stories) or `/x-refine-epic` (for epics) — multi-persona dispatcher skills that apply NO-GO rules from specialist personas and consolidate via the Architect agent.
3. The gate is enforced at Camada 0 (PreToolUse hook `enforce-refinement-gate.sh`, exit code `33`) and at Camada 2 (CI script `audit-refinement-gate.sh`, exit code `REFINEMENT_GATE_VIOLATION`).

## State Machine Extension

### Extended Story/Task Status Enum

Rule 29 extends the canonical story/task status enum with the state `Refinada`:

| State | Meaning |
| :--- | :--- |
| `Pendente` | Story/epic created; not yet refined |
| **`Refinada`** | *(NEW — Rule 29)* Refinement gate passed; `refinementVerdict.status = "approved"` |
| `Planejada` | Planning artifacts generated (arch/impl/tests/tasks/security/compliance) |
| `Em Andamento` | Implementation orchestrator running |
| `Concluída` | All tasks done; PR merged; evidence artifacts present |
| `Falha` | Orchestrator terminated with error |
| `Bloqueada` | Dependency unresolved; orchestrator cannot proceed |

### Transitions (Extended)

```
Pendente ──→ Refinada    — via /x-refine-story (story) or /x-refine-epic (epic)
Refinada ──→ Planejada   — via x-plan-story or x-epic-decompose
Refinada ──→ Em Andamento — via x-implement-story or x-implement-epic (gate passed)
Refinada ──→ Bloqueada   — dependency unresolved before planning
Em Andamento ──→ Concluída
Em Andamento ──→ Falha
```

**Invariant:** The transition `Pendente → Em Andamento` is **forbidden** after EPIC-0069 merges. `enforce-refinement-gate.sh` blocks it at Camada 0. Exception: `flowVersion=1` legacy epics (Rule 19 fallback — hook is no-op) and `hotfix/*` branches (Rule 27 Exception 2).

## `refinementVerdict` Field

Added to `execution-state.json` by `/x-refine-story` or `/x-refine-epic` via `x-internal-update-status`:

```json
{
  "refinementVerdict": {
    "status": "approved" | "rejected" | "tbd",
    "scope": "story" | "epic",
    "checkedAt": "<ISO-8601>",
    "verdictHash": "<sha256 of ## Refinement Verdict block in the markdown>",
    "dimensions": {
      "persona":       { "checked": true | false, "blocker": null | "<reason>" },
      "value":         { "checked": true | false, "blocker": null | "<reason>" },
      "ac":            { "checked": true | false, "blocker": null | "<reason>" },
      "contracts":     { "checked": true | false, "blocker": null | "<reason>" },
      "metrics":       { "checked": true | false, "blocker": null | "<reason>" },
      "alternatives":  { "checked": true | false, "blocker": null | "<reason>" },
      "risks":         { "checked": true | false, "blocker": null | "<reason>" }
    },
    "blockers": ["<reason-1>", "<reason-2>"]
  }
}
```

| Field | Type | Description |
| :--- | :--- | :--- |
| `status` | Enum | `approved` = all dimensions checked, no blockers; `rejected` = ≥1 blocker; `tbd` = not yet run (legacy default) |
| `scope` | Enum | `story` for story-level refinement; `epic` for epic-level |
| `checkedAt` | ISO-8601 | Timestamp of last refinement run |
| `verdictHash` | sha256 | Hash of the `## Refinement Verdict` block in the markdown file — used by `audit-refinement-gate.sh` to detect manual divergence |
| `dimensions` | Object | One entry per dimension; `blocker` is non-null when the persona applied a silent NO-GO |
| `blockers` | String[] | Aggregated blockers across all personas |

**Backward compatibility (Rule 19):** Absence of `refinementVerdict` in a pre-EPIC-0069 state file resolves to `{ status: "tbd" }`. Hook behavior: `flowVersion=1` → hook no-op (legacy flow); `flowVersion≥2` + `status="tbd"` → hook blocks with `REFINEMENT_REQUIRED` (exit 33).

## Personas and Dimensions

### Story Refinement Personas (`/x-refine-story`)

| Persona | Dimensions owned | Conditional on |
| :--- | :--- | :--- |
| Product Owner | persona, value, ac | always |
| Tech Lead | contracts, ac (typed) | always |
| Architect | consolidation (Phase D) | always |
| Security Engineer | risks (security angle) | always |
| QA Engineer | ac (Gherkin completeness), risks (quality) | always |
| Performance Engineer | metrics (latency/throughput), risks (perf) | capability `build.maven.standard` + perf story scope |
| SRE/DevOps | risks (operational), metrics (SLOs) | capability `infra.*` or `runtime.*` |

**Minimum for `approved`:** PO + Tech Lead + Architect + Security + QA must all pass (no blockers). Conditional personas' verdicts are advisory unless the story explicitly touches their domain.

### Epic Refinement Personas (`/x-refine-epic`)

| Persona | Dimensions owned | Conditional on |
| :--- | :--- | :--- |
| Product Owner | problem, persona (broad), value hypothesis | always |
| Tech Lead | OKRs/KPIs, strategic alternatives | always |
| Architect | out-of-scope framing, consolidation | always |
| Security Engineer | security posture, risk | always |
| QA Engineer | quality posture, testability | always |
| SRE/DevOps | operational risks, SLOs | capability `infra.*` or `runtime.*` |

### Dimensions (Story — 6 Required + 1 Optional)

| # | Dimension | Check |
| :--- | :--- | :--- |
| 1 | **persona** | Role is specific; not "sistema"; maps to a real user |
| 2 | **value** | Value proposition is falsifiable; not "melhora X" |
| 3 | **ac** | AC in Gherkin; ≥4 categories: happy-path, error/boundary, performance/SLA, security/auth |
| 4 | **contracts** | Request/response typed; no `Object`/`Map<String, Any>`; event schemas if applicable |
| 5 | **metrics** | ≥1 measurable metric with unit and target; not qualitative only |
| 6 | **alternatives** | ≥1 alternative considered with rationale for rejection |
| 7 | **risks** | ≥1 risk identified; dependency/external service/data migration noted |

### Dimensions (Epic — 7 Required)

| # | Dimension | Check |
| :--- | :--- | :--- |
| 1 | **problem** | Problem stated as observable pain with evidence |
| 2 | **persona** | Affected persona is specific |
| 3 | **value** | Hypothesis in `If…then…because` form |
| 4 | **okrs** | ≥1 OKR/KPI with unit and measurement method |
| 5 | **alternatives** | ≥2 alternatives with rejection rationale |
| 6 | **risks** | ≥1 product risk + ≥1 technical risk |
| 7 | **scope** | In-scope list AND out-of-scope list; out-of-scope ≥ 3 items (heuristic) |

## `--legacy-refinement` Flag

The only supported skip for the gate in implementation orchestrators. Permitted contexts:

1. `hotfix/*` branches (Rule 27 Exception 2) — single-file critical fix with `## Hotfix Bypass Justification` in PR body.
2. `flowVersion=1` epics (Rule 19 fallback) — hook is no-op automatically.

There is no interactive `--skip-refinement` flag. `CLAUDE_RECOVERY_MODE=1` (Rule 27) does not bypass the refinement gate — it only allows `--skip-review` and `--no-ci-watch` in recovery flows.

## Enforcement

| Camada | Mechanism | Trigger | Exit |
| :--- | :--- | :--- | :--- |
| **0 — PreToolUse** | `enforce-refinement-gate.sh` | PreToolUse on `x-implement-story`, `x-implement-epic`, `x-implement-task`, `x-orchestrate-epic` | 33 `REFINEMENT_REQUIRED` |
| **1 — Normative** | This rule + CLAUDE.md block `REFINEMENT GATE — INEGOCIÁVEL` | Every conversation | — |
| **2 — CI Script** | `audit-refinement-gate.sh` | PR open/sync to `develop` or `epic/*` | 1 `REFINEMENT_GATE_VIOLATION` |
| **3 — Java Test** | `Epic0069RefinementGateSmokeIT` | `mvn verify` | JUnit assertion failure |

## Forbidden

- Invoking `x-implement-story`, `x-implement-epic`, `x-implement-task`, or `x-orchestrate-epic` on a story/epic whose `refinementVerdict.status` is not `"approved"` — blocked at Camada 0.
- Manually editing the `## Refinement Verdict` block in a markdown file without re-running the skill — `audit-refinement-gate.sh` detects `verdictHash` divergence.
- Introducing a new bypass env var for the refinement gate — the only accepted mechanism is `CLAUDE_RECOVERY_MODE=1` for `--skip-review`/`--no-ci-watch` (not for refinement bypass).
- Adding entries to `governance/baselines/refinement-gate-baseline.txt` after EPIC-0069 merges — baseline is immutable.

## Audit

Self-check: `audit-refinement-gate.sh --self-check` MUST verify:
1. This rule file (`29-refinement-gate.md`) exists.
2. `capabilities/governance/refinement-gate.yaml` exists.
3. `enforce-refinement-gate.sh` is registered in `settings.json` under `PreToolUse` hooks.
4. Baseline file `governance/baselines/refinement-gate-baseline.txt` is present.

Failure → `RULE_29_ENFORCEMENT_BROKEN`.

---

> **Catalogado em:** [`docs/audit-gates-catalog.md`](../../docs/audit-gates-catalog.md)
