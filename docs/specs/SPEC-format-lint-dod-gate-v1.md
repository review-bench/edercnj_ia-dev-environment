# SPEC-format-lint-dod-gate-v1 — Format/Lint DoD Gate Enforcement

> **Status:** Draft  
> **Author:** Eder Celeste Nunes Junior  
> **Date:** 2026-04-30  
> **Branch:** docs/feature-format-lint-dod-gate

---

## Sistema

This epic addresses a critical governance gap in the lifecycle: while the CI pre-commit-chain job (EPIC-0059 story-0059-0006) validates code formatting and linting on every PR, the Definition of Done (DoD) for stories and tasks does not explicitly mandate that these checks pass **before** code is committed and pushed. Consequently, LLM agents and human developers frequently discover format/lint failures only in CI, forcing a round-trip fix-and-retry cycle.

The system role is to enforce code-formatting and linting standards across all enforcement layers:
- **Camada 1 (Normative):** Update Rule 05 Merge Checklist and story/task templates to make format/lint a formal DoD mandate.
- **Camada 0 (Preventive, New):** Extend the preflight gate (EPIC-0063) with a local `mvn spotless:check` enforcement that blocks `git push` before CI sees the PR, closing the feedback loop.
- **Camada 2 (CI, Existing):** Leverage the existing `pre-commit-chain` job from EPIC-0059 as a secondary safety net.
- **Camada 4 (Observability):** Track telemetry so we measure the reduction in "fix CI" PRs post-deployment.

Stakeholders: Tech Leads (code quality oversight), LLM agents (x-implement-task, x-implement-story), Human developers, CI operators, and auditors verifying governance compliance.

---

## Escopo

### Incluído

- **Rule 05 Merge Checklist update:** Add explicit item `mvn spotless:check passou (ou x-format-code --check)` and `mvn checkstyle:check passou (ou x-lint-code)` so reviewers have a formal criterion to check before merge.
- **Story & Task template DoD:** Update `_TEMPLATE-STORY.md` and `_TEMPLATE-TASK-BREAKDOWN.md` so every generated story/task plan includes a format/lint DoD bullet, making the obligation visible to agents from plan generation.
- **Camada 0 preflight gate (NEW):** Create `scripts/audit-format-check.sh` that runs `mvn spotless:check -q` on local files before `git push` or `gh pr create`. Integrate via existing `.claude/hooks/enforce-preflight-gates.sh` (Rule 24 Camada-0, EPIC-0063). Exit code 0=pass, non-zero=fail. Bypass only via `CLAUDE_RECOVERY_MODE=1` (Rule 27).
- **Skill alignment verification (NO NEW SKILLS):** Confirm `x-format-code` and `x-lint-code` already wrap `mvn spotless:apply/check` + `checkstyle` correctly, and document the contract in the new Rule frontmatter.
- **x-implement-task audit:** Verify that the pre-commit step (RULE-007 chain order: format → lint → compile → commit) invokes `x-format-code --apply` by default, so TDD Green-phase output is formatted automatically.
- **Governance catalog entry:** Register `audit-format-check.sh` in `docs/audit-gates-catalog.md` (Rule 26 RULE-004) with exit codes, self-check contract, and Camada 0 characteristics.

### Excluído

- Introducing new linting tools, Spotless plugin upgrades, or Java formatter version bumps (v2.44.5 + Google Java Format v1.25.2 already optimal).
- Modifying or replacing the CI `pre-commit-chain` job from EPIC-0059 — it remains the secondary defense layer.
- Overlap with EPIC-0069 (Refinement & DoR Gate) or EPIC-0071 (Documentation as DoD) — those epics handle independent gates.
- Support for non-Java languages or non-Maven build systems; scope restricted to `mvn spotless:check/apply` and `checkstyle`.

---

## Regras

| ID | Regra | Impacto |
|----|-------|---------|
| RULE-TBD-01 | **Format & Lint DoD mandate:** Every story/task PR MUST pass `mvn spotless:check + checkstyle` locally before push. Enforced across Camada 0 (preflight), Camada 1 (Rule 05 Merge Checklist), Camada 2 (CI pre-commit-chain). | Rule 05 Quality Gates, Rule 24 Execution Integrity, Rule 26 Audit Gate Lifecycle |
| RULE-TBD-02 | **Camada 0 format gate contract:** `scripts/audit-format-check.sh` exits 0 on pass, non-zero on fail. Registered in `.claude/hooks/enforce-preflight-gates.sh` via PreToolUse event. Bypass via `CLAUDE_RECOVERY_MODE=1` only. | Rule 24 Camada-0, Rule 26 exit-code taxonomy, Rule 27 recovery mode |
| RULE-TBD-03 | **x-implement-task TDD-loop alignment:** Pre-commit step MUST invoke `x-format-code --apply` (not --check) so Green-phase output is auto-formatted before x-commit-changes. Smoke test validates. | RULE-007 (pre-commit chain), x-implement-task SKILL.md Phase structure |
| RULE-TBD-04 | **Template DoD inheritance:** `_TEMPLATE-STORY.md` and `_TEMPLATE-TASK-BREAKDOWN.md` include explicit format/lint DoD bullet, ensuring every generated plan inherits the obligation. | PlanTemplatesAssembler, golden file regeneration, template versioning |

---

## Histórias

Preliminary story index (to be refined in x-create-feature):

| # | Título | Stakeholder | Justificativa |
|---|--------|-------------|---------------|
| 1 | Update Rule 05 Merge Checklist with Spotless/Checkstyle DoD items | Tech Lead | Normalize vague "zero warnings" into concrete gate |
| 2 | Add format/lint DoD bullet to `_TEMPLATE-STORY.md` | Story Author / Generator | Every story plan inherits the obligation automatically |
| 3 | Add format/lint DoD bullet to `_TEMPLATE-TASK-BREAKDOWN.md` | Task Author / Generator | Every task plan inherits symmetrically with stories |
| 4 | Create `scripts/audit-format-check.sh` Camada 0 gate | SRE / Governance | Block local push on spotless:check failure; first-line defense |
| 5 | Integrate `audit-format-check.sh` into `scripts/preflight.sh` and `enforce-preflight-gates.sh` | DevOps / Governance | Wire PreToolUse hook to Camada 0 gate |
| 6 | Verify x-implement-task pre-commit step invokes `x-format-code --apply` | Architect / QA | Ensure TDD Green output is auto-formatted |
| 7 | Document format/lint gate in governance catalog (`docs/audit-gates-catalog.md`) | Technical Writer / Auditor | Track audit gate Camada 0 characteristics, exit codes |
| 8 | Skill alignment verification: x-format-code & x-lint-code audit | Architect | Confirm existing skills wrap spotless/checkstyle correctly |

---

## DoR / DoD

### Definition of Ready
- [ ] Spotless plugin v2.44.5 and Google Java Format v1.25.2 confirmed in pom.xml (no version changes required).
- [ ] Skills `x-format-code` and `x-lint-code` identified and their Bash wrapping verified against source-of-truth.
- [ ] Rule 05, Rule 26, Rule 27 cross-reference documented; ADR number reserved (TBD pending in-flight epic branches).
- [ ] Current versions of `_TEMPLATE-STORY.md`, `_TEMPLATE-TASK-BREAKDOWN.md` identified under `shared/templates/` for diff-baseline.
- [ ] Camada 0 gate naming convention (kebab-case, `audit-` prefix) and exit-code taxonomy (0/1/2/3 per Rule 26) confirmed available for reuse.

### Definition of Done
- [ ] Rule 05 `.claude/rules/05-quality-gates.md` Merge Checklist updated with explicit Spotless and Checkstyle items; golden files regenerated and `GoldenFileTest` passes.
- [ ] `_TEMPLATE-STORY.md` and `_TEMPLATE-TASK-BREAKDOWN.md` include format/lint DoD bullet; PlanTemplatesAssembler re-run and all 12 templates validated.
- [ ] `scripts/audit-format-check.sh` implemented with full `--self-check` contract, registered in `scripts/preflight.sh`, and catalogued in `docs/audit-gates-catalog.md`.
- [ ] `.claude/hooks/enforce-preflight-gates.sh` integration verified: PreToolUse hook triggers on `Skill x-create-pr` and `Bash git push`, audit-format-check blocks on non-zero exit.
- [ ] `x-implement-task` SKILL.md Phase 0 contract audit: pre-commit step confirmed to call `x-format-code --apply` by default; smoke test `Epic0070FeatureSmokesIT` validates the assertion.
- [ ] Integration tests verify all Camada layers (0=PreToolUse hook, 1=Rule 05, 2=CI pre-commit-chain, 4=telemetry) green on a representative task PR; no merge regressions on develop (line coverage ≥95%, branch ≥90%).
- [ ] CHANGELOG.md entry under `## Added` documents the new Camada 0 gate and Rule updates.

---

## Riscos

| Risco | Impacto | Mitigação |
|-------|---------|-----------|
| **Latency regression:** `mvn spotless:check -q` may exceed 500ms Camada 0 budget (Rule 26 <500ms SLA) on large source trees, degrading UX. | Alto | Scope `audit-format-check.sh` to changed files only via `git diff HEAD --name-only`, falling back to full-tree check only if staged file count exceeds 50. Measure and document latency baseline before merge. |
| **In-flight epic rebase conflict:** Existing in-flight branches (EPIC-0069, EPIC-0071) may commit format drift before this epic merges, triggering immediate Camada 0 blocks on rebase. | Médio | Run `mvn spotless:apply` once on `develop` baseline pre-epic-merge to normalize formatting; document rebase guidance in epic README for affected teams. |
| **Agent TDD-loop format failure:** `x-implement-task` may already invoke `x-format-code --check` (not --apply), causing Green-phase commits to fail the new format gate retroactively. | Médio | Audit `x-implement-task` SKILL.md and any orchestrator delegating to `x-format-code` during Phase 0/1; guarantee `--apply` default and `--check` reserved for verification-only contexts. Add smoke test assertion. |

---

## Próximos passos (após revisão humana)

1. Revisar o spec e iterar conforme necessário via PR.
2. Invocar `/x-create-feature docs/specs/SPEC-format-lint-dod-gate-v1.md --epic-id <NNNN>` para gerar Epic + Stories + Implementation Map.
3. Execute a epic via `/x-implement-epic epic-NNNN --non-interactive` para completar Phase 1 (planning) e Phase 2-3 (implementation).

---

**Spec prepared by:** x-ideate-feature skill  
**Date:** 2026-04-30
