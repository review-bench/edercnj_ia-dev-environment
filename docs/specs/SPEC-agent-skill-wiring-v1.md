# SPEC-agent-skill-wiring-v1 — Native Agent–Skill Wiring & Cleanup

> **Status:** Draft  
> **Author:** x-ideate-feature  
> **Date:** 2026-05-07  
> **Branch:** docs/feature-agent-skill-wiring

---

## Sistema

The ia-dev-env CLI generator ships 29 agent files under `src/main/resources/targets/claude/agents/` but no skill currently uses Claude Code's native agent dispatch mechanism (`subagent_type: "<agent-name>"`). Instead, all persona-bearing skills dispatch via `subagent_type: "general-purpose"` with the persona duplicated inline in the `prompt:` argument. This breaks Rule 23 (per-agent `model:` selection), Rule 13 (explicit invocation protocol), and creates maintenance burden when a persona evolves.

The goal of this feature is to operationalize the agent.md mechanism — formalize agent frontmatter, wire five orchestrator skills to dispatch agents by name instead of by inline persona, migrate eight specialist-reviewer skills to their respective agents, clean up eleven structurally-orphan checklist agents, and add a CI audit gate to prevent regression.

---

## Escopo

### Incluído

- Formalize agent.md frontmatter with `description`, `tools` (least-privilege whitelist), `model` (per Rule 23). Add JSON Schema validation.
- Migrate `x-plan-story`, `x-refine-epic`, `x-refine-story`, `x-internal-build-story-plan` (4 parallel-planning skills with 25+ Agent() invocations) from inline personas to named subagent dispatch (`subagent_type: "architect"`, `"qa-engineer"`, etc.).
- Migrate `x-review-codebase` dependency chain: wire 8 specialist-review skills (`x-review-qa`, `x-review-performance`, `x-review-database`, `x-review-devops`, `x-review-security`, `x-review-api`, `x-review-observability`, `x-review-events`) to their matching agents.
- Reclassify `checklists/` directory (11 files: graphql-api, grpc-api, helm-devops, hipaa-security, iac-devops, mesh-devops, pci-dss-security, privacy-security, registry-devops, sox-security, websocket-api) as knowledge packs under `src/main/resources/targets/claude/knowledge/checklists/` — drop agent artifact generation.
- Add `audit-agent-skill-wiring.sh` (Rule 26 Camada 2) to flag inline personas in skills and fail on regression.
- Decide per-agent what to do with 5 remaining orphans (`appsec-engineer`, `compliance-auditor`, `devsecops-engineer`, `sre-engineer`, `java-developer`).
- Update Rule 13 KP to document named-subagent dispatch variant.

### Excluído

- Modifying non-feature-creation orchestrators (`x-epic-create`, `x-epic-decompose`, `x-plan-task`, `x-implement-*`).
- Adding new agents; using only the 17 existing core/conditional/developers agents.
- Changing flowVersion semantics or refinement-gate states.
- Refactoring Java classes; agent-wiring is a generated-artifact concern.
- Altering template structure for Epic v2 or Story v2.

---

## Regras

| ID | Regra | Impacto |
|----|-------|---------|
| RULE-013 | Skill Invocation Protocol — Pattern 2 SUBAGENT-GENERAL needs named-subagent variant | 4 orchestrator SKILLs (x-plan-story, x-refine-epic, x-refine-story, x-internal-build-story-plan) must update invocation blocks |
| RULE-022 | Skill Visibility — agents are not user-invocable; dispatch only via Skill/Agent tool calls | No new agent-invocation entry points; agents remain internal personas |
| RULE-023 | Model Selection Strategy — agent.md MUST declare `Recommended Model:` per tier; `Adaptive` forbidden | Architect=Opus, QA/Security/PO/TechLead/Performance=Sonnet, Haiku forbidden for design personas |
| RULE-028 | Capability Frontmatter Contract — agents must declare `requires-capabilities` in frontmatter v3.0 | All 17 agents (core/conditional/developers) must carry capability gate; checklists excluded |
| RULE-026 | Audit Gate Lifecycle — Camada 2 CI scripts must follow naming, exit codes, `--self-check` contract | `audit-agent-skill-wiring.sh` emits `INLINE_PERSONA_VIOLATION`, exit 1 on regression |
| RULE-024 | Execution Integrity — all Skill() tool calls declared in SKILL.md must execute | Named-subagent dispatches are real Agent() calls, not simulated; grammar markers per Rule 28 required |

---

## Histórias

| # | Título | Stakeholder |
|---|--------|-------------|
| 1 | Formalize agent.md frontmatter and JSON Schema validation for all core/conditional/developers agents (exclude checklists/) | Architect / CI Maintainer |
| 2 | Migrate `x-plan-story`, `x-refine-epic`, `x-refine-story`, `x-internal-build-story-plan` from inline personas to named-subagent dispatch | Orchest. Skill Maintainer |
| 3 | Migrate 8 specialist-review skills to agent-matched dispatch (qa, performance, database, devops, security, api, observability, events) | Review Skill Maintainer |
| 4 | Reclassify 11 checklist-agents as knowledge packs under `src/main/resources/targets/claude/knowledge/checklists/` | Generator Architect |
| 5 | Add `audit-agent-skill-wiring.sh` Camada-2 gate with baseline tolerance and pre-commit integration | CI Maintainer |
| 6 | Audit and resolve 5 orphan agents (appsec-engineer, compliance-auditor, devsecops-engineer, sre-engineer, java-developer) | Architect / Tech Lead |
| 7 | Update Rule 13 KP to document named-subagent Pattern 2 variant and telemetry implications | Documentation Maintainer |

---

## DoR / DoD

### Definition of Ready

- [ ] All 29 agent files audited for current frontmatter state; gaps documented (which agents missing `description`, `tools`, `model`?)
- [ ] Inventory of 25+ Agent() invocation sites in parallel-planning skills created (x-plan-story, x-refine-epic, x-refine-story, x-internal-build-story-plan)
- [ ] Specialist-review skill chain (x-review-codebase → 8 sibling skills) mapped with 1:1 agent assignments
- [ ] Governance schema `agent-frontmatter-1.0.json` drafted; `requires-capabilities` enum matches agent categories
- [ ] CLI generator codebase scanned for `PlanAgentsAssembler` or similar — understand how agent artifacts are composed and emitted
- [ ] Decision matrix for 5 orphans drafted (Delete/Wire/Document reasoning per agent)
- [ ] Rule 13 KP marked for update with clear named-subagent Pattern 2 definition and telemetry expectations

### Definition of Done

- [ ] All 17 core/conditional/developers agents carry complete frontmatter: `name`, `description` (proactive trigger form), `tools` (whitelist), `model` (Opus/Sonnet), `requires-capabilities` (frontmatter v3.0)
- [ ] JSON Schema `governance/schemas/agent-frontmatter-1.0.json` exists; all agents validated
- [ ] 4 parallel-planning skills migrated: `x-plan-story`, `x-refine-epic`, `x-refine-story`, `x-internal-build-story-plan` — all Agent() calls use named dispatch (`subagent_type: "architect"`, `"qa-engineer"`, etc.) instead of `general-purpose`
- [ ] 8 specialist-review skills wired: each calls its matching agent (no more inline personas in `x-review-qa`, `x-review-performance`, etc.)
- [ ] `checklists/` directory deleted from `agents/`; all 11 files moved to `knowledge/checklists/` without agent frontmatter
- [ ] `audit-agent-skill-wiring.sh` implemented (Camada 2, Rule 26 contract), passes `--self-check`, integrated pre-commit
- [ ] 5 orphan agents resolved: either wired (callsite added) or deleted with documented reason
- [ ] Rule 13 KP updated with named-subagent Pattern 2, telemetry implications, and link to this feature's ADR
- [ ] Generator regenerates `.claude/` output deterministically; `audit-capability-determinism.sh` exit 0
- [ ] AI memory summary produced via `x-internal-summarize-epic` on merge (Rule 33)

---

## Riscos

| Risco | Impacto | Mitigação |
|-------|---------|-----------|
| Agent invocation omitted from migrated skills, causing PROTOCOL_VIOLATION under Rule 24 Camada 3 | Alto | Add tool-call grammar marker `[required]` per Rule 28 to every Agent() block; cross-checked by `audit-tool-call-grammar.sh` |
| `Recommended Model: Adaptive` declared on any agent, defaulting silently to Opus and inflating token baseline | Médio | Hard-code Opus/Sonnet/Haiku only; `audit-model-selection.sh` exit 1 on Adaptive |
| Capability ID typo in `requires-capabilities` causing `UnknownCapability` exit 3 in `audit-capability-graph.sh` | Médio | Validate `capabilities/planning/ideation.yaml` schema before reference; run audit pre-flight |
| Inline-persona heuristic in `audit-agent-skill-wiring.sh` false-positives on legitimate prose | Baixo | Use baseline file to grandfather acceptable exceptions; document per-entry in commit message |
| Specification drift between agent.md and prompt: if agent updates but prompt is not removed, persona duplication lingers | Médio | Code review gate: grep "You are a Senior" in prompt blocks of migrated skills; fail if found |
| Checklists/ move to knowledge/ breaks existing imports or capability gates referencing them as agents | Médio | Audit all references to agents/checklists/* before deletion; remap references to knowledge/checklists/* |

---

## Next Step

1. Review and edit this spec at the PR.
2. Once approved, invoke:
   ```bash
   /x-create-feature docs/specs/SPEC-agent-skill-wiring-v1.md --epic-id <NNNN>
   ```
