# x-implement-epic — Execution Flow (EPIC-0049 refactor)

> Visual reference for the thin-orchestrator flow delivered by story-0049-0018.
> The main SKILL.md is now ~460 lines and delegates every substantive
> responsibility to six specialized sub-skills. This README illustrates the
> delegation topology; prose specifics live in `SKILL.md` and
> `references/full-protocol.md`.

## 1. High-Level Orchestration Flow

```mermaid
flowchart TD
    START(["/x-implement-epic EPIC-ID"]) --> P0

    P0["Phase 0 — Args<br/>x-internal-normalize-args"] --> P1N["Phase 1 — Load & Plan<br/>x-internal-build-epic-plan"]

    P1N --> P2["Phase 2 — Branch Setup<br/>x-internal-ensure-epic-branch<br/>(creates epic/XXXX)"]
    P2 --> P3N

    subgraph P3N["Phase 3 — Execution Loop"]
        P3N_SEQ{"Parallel?"}
        P3N_SEQ -->|"No (default)"| SEQ["Sequential: one story at a time<br/>x-implement-story --target-branch epic/XXXX --auto-merge=merge"]
        P3N_SEQ -->|"--parallel"| PAR["Parallel within phase batch<br/>(siblings in ONE assistant message)"]
    end

    P3N --> P4["Phase 4 — Integrity Gate + Report<br/>x-internal-verify-epic-integrity +<br/>x-internal-write-report"]

    P4 --> P4B{"--skip-pr-comments?"}
    P4B -->|"No"| P4C["Phase 4b — PR-comment remediation<br/>x-fix-epic-pr"]
    P4B -->|"Yes"| P5N
    P4C --> P5N["Phase 5 — Final PR<br/>x-merge-branches develop→epic/XXXX +<br/>x-create-pr (no auto-merge)"]

    P5N --> DONE(["Return envelope"])

    style P0 fill:#16213e,color:#fff
    style P1N fill:#16213e,color:#fff
    style P2 fill:#16213e,color:#fff
    style P3N fill:#16213e,color:#fff
    style P4 fill:#16213e,color:#fff
    style P5N fill:#2d6a4f,color:#fff
    style DONE fill:#2d6a4f,color:#fff
```

## 2. Delegation Map

```mermaid
graph TD
    EPIC["x-implement-epic<br/>(thin orchestrator, ~460 lines)"] -->|Phase 0| ARGS["x-internal-normalize-args"]
    EPIC -->|Phase 1| PLAN["x-internal-build-epic-plan"]
    EPIC -->|Phase 2| BRANCH["x-internal-ensure-epic-branch"]
    EPIC -->|Phase 3 per story| STORY["x-implement-story"]
    EPIC -->|Phase 4| GATE["x-internal-verify-epic-integrity"]
    EPIC -->|Phase 4 report| REPORT["x-internal-write-report"]
    EPIC -->|Phase 5.1| MERGE["x-merge-branches<br/>(sync develop→epic)"]
    EPIC -->|Phase 5.2| PR["x-create-pr<br/>(final PR, no auto-merge)"]
    EPIC -->|all phases| STATE["x-internal-update-status<br/>(atomic state writes)"]
    EPIC -->|Phase 4b optional| PRFIX["x-fix-epic-pr"]

    BRANCH -->|delegates creation| GITBRANCH["x-create-git-branch"]
    PLAN -->|renders via| REPORT
    PLAN -->|collision eval| PAREV["x-evaluate-parallelism"]
    STORY -->|per task| TDD["x-drive-tdd + x-create-pr + x-commit-changes"]

    classDef thin fill:#16213e,stroke:#0f3460,color:#fff
    classDef internal fill:#533483,stroke:#e94560,color:#fff
    classDef primitive fill:#2d6a4f,stroke:#1b4332,color:#fff

    class EPIC,STORY thin
    class ARGS,PLAN,BRANCH,GATE,REPORT,STATE internal
    class MERGE,PR,GITBRANCH,PAREV,PRFIX,TDD primitive
```

## 3. Architecture summary

| Aspect | Value |
|--------|-------|
| Parallelism | default off (`--parallel` opts in) |
| Auto-merge target | `epic/<EPIC-ID>` |
| Final PR | `epic/<EPIC-ID> → develop` (manual gate) |
| Inline `git`/`gh`/`jq`/`mvn` | **0** (only `Read`/`Glob` + `Skill`) |

## 5. Error Code Catalogue

| Exit | Code | Phase | Cause |
|------|------|-------|-------|
| 1 | `ARGS_INVALID` | 0 | Normalizer rejects argv |
| 2 | `EPIC_DIR_MISSING` | 0 | `ai/epics/epic-XXXX/` absent |
| 3 | `STORY_FAILED` | 3 | One or more stories FAILED |
| 4 | `INTEGRITY_GATE_FAILED` | 4 | Gate failed twice after recovery |
| 5 | `FINAL_PR_CONFLICTS` | 5 | `x-merge-branches` sync conflicted |
| 6 | `BRANCH_ENSURE_FAILED` | 2 | `epic/<ID>` could not be ensured |
| 7 | `PLAN_BUILD_FAILED` | 1 | Plan build failed (non-cyclic) |
| 8 | `CYCLIC_DEPENDENCY` | 1 | DAG cycle detected |

## 6. References

- Main skill body: [`SKILL.md`](SKILL.md)
- Full protocol + retry/circuit-breaker/legacy semantics: [`references/full-protocol.md`](references/full-protocol.md)
- Args schema consumed by `x-internal-normalize-args`: [`references/args-schema.json`](references/args-schema.json)
- Parent story: `ai/epics/epic-XXXX/story-XXXX-YYYY.md`
- ADR-0006 (file-conflict-aware parallelism), ADR-0010 (interactive gates), ADR-0012 (thin-skill pattern)
