# .claude/ -- Usage Guide

This directory contains all Claude Code configuration for the **ia-dev-environment** project.
It includes coding rules, skills (slash commands), knowledge packs, agents, and hooks.

> **Note:** The `.claude/` directory is a **generated output** produced by `ia-dev-env`.
> Do not edit it manually -- regenerate instead.

> **CRITICAL — Source of Truth:**
> The source of truth for skills, knowledge packs, agents, rules, and templates is `java/src/main/resources/targets/claude/`.
> The directories `.claude/` and `src/test/resources/golden/` are generated outputs — NEVER edit them directly.

> The `CLAUDE.md` file at the project root provides an executive summary loaded automatically in EVERY conversation.

> **Concluded — EPIC-0074 (Dependency Policy & SCA Final Gate).** Projetos gerados por `ia-dev-env` agora suportam um gate de política de dependências bloqueante via `dependencies.policy` YAML block. Quando `enabled: true`, o skill `x-dep-policy-validate` é invocado como **MANDATORY TOOL CALL** em Phase 3 do `x-story-implement` (após os gates de qualidade EPIC-0072/0073). O gate enforça em 5 dimensões: CVEs hard-block (`denied-cves` — RULE-074-01, ignora scope-policy), severidade CVE (threshold `block-on.severity-cve`, default HIGH), whitelist de licenças SPDX, constraints de versão cross-stack (JVM groupId/artifactId, NPM/PyPI name, Go module), e freshness window (default 365 dias, default WARN_ONLY). Safe default: `enabled: false` — projetos existentes não são afetados (Rule 19). Rule 27 expandida para **13 surfaces** (Surface 13: dependency policy gate). Rule 24 evidence artifact: `dep-policy-validation-report-STORY-ID.md`. Camada 2: `audit-dep-policy.sh`. Tag `dependency-policy-gate-frozen` marca closure do epic.
> - Rule: [`.claude/rules/32-dependency-policy-gate.md`](.claude/rules/32-dependency-policy-gate.md)
> - Skill: [`x-dep-policy-validate`](src/main/resources/targets/claude/skills/conditional/security/x-dep-policy-validate/SKILL.md)
> - Audit script: [`audit-dep-policy.sh`](src/main/resources/targets/claude/scripts/audit-dep-policy.sh)
> - ADR: [`docs/adr/ADR-0027-dependency-policy-gate.md`](docs/adr/ADR-0027-dependency-policy-gate.md)
> - Epic index: [`ai/epics/epic-0074-dependency-policy-and-sca-gate/`](ai/epics/epic-0074-dependency-policy-and-sca-gate/)

> **Concluded — EPIC-0041 (File-Conflict-Aware Parallelism Analysis).**
> Planning now emits a structured `## File Footprint` / `## Story File Footprint` block on every task/story plan (`write:` / `read:` / `regen:`). The new skill `/x-parallel-eval --scope=epic|story|task` consumes those footprints, produces a collision matrix (hard / regen / soft), and recommends demotions to serial when two plans touch the same hotspot. `x-epic-map` Step 8.5 annotates the Implementation Map with "Restrições de Paralelismo"; `x-epic-implement` Phase 0.5.0 and `x-story-implement` Phase 1.5 run the gate and **degrade waves to serial with a visible warning** when a collision is detected (`ExecutionState.parallelismDowngrades`). Hotspots catalogued in RULE-004 (`SettingsAssembler.java`, `HooksAssembler.java`, `CLAUDE.md`, `CHANGELOG.md`, `pom.xml`, `.gitignore`, `src/test/resources/golden/**`). Plans predating this epic are treated as "footprint unknown" — warn, do not block (RULE-006).
> - Decision record: [`docs/adr/ADR-0006-file-conflict-aware-parallelism.md`](docs/adr/ADR-0006-file-conflict-aware-parallelism.md)
> - Retroactive diff patches for epics 0036–0040: [`ai/epics/epic-0041/migrations/`](ai/epics/epic-0041/migrations/) (EPIC-0040 flagged HIGH — hard conflict on `telemetry-phase.sh`).
> - Skill inventory gained `/x-parallel-eval` (category `plan/`).

> **Concluded — EPIC-0045 (CI Watch no Fluxo de PR).**
> Delivered `x-pr-watch-ci` skill (CI polling + Copilot review detection, 8 stable exit codes — RULE-045-05), Rule 21 (CI-Watch, RULE-045-01) with fallback matrix and opt-out via `--no-ci-watch`, and retrofits to `x-story-implement` (Phase 2.2.8.5), `x-task-implement --worktree` (Step 4.5), and `x-release` (flag `--ci-watch`). `Epic0045SmokeTest` validates end-to-end contract. `PrWatchStatusClassifier` + `PrWatchExitCode` (zero-I/O, fully testable). All 6 stories merged to develop.
> - Story index: [`ai/epics/epic-0045/`](ai/epics/epic-0045/)

> **Concluded — EPIC-0055 (Task Hierarchy & Phase Gate Enforcement).**
> Introduces Rule 25 (hierarchical task tracking, 4-level depth via `›` separator: Epic › Story › Phase › Wave/Cycle), skill `x-internal-phase-gate` (internal, `haiku`, 4 modes: pre/post/wave/final), and ADR-0014. Phase gates block `## Phase N` transitions until child tasks are `completed` AND expected artifacts exist on disk. Operators can see `"EPIC-0065 › Phase 3 › story-0065-0001 (in_progress)"` during execution. 4-layer enforcement: normative (Rule 25 + CLAUDE.md), Stop hook (`verify-phase-gates.sh`), PreToolUse hook (`enforce-phase-sequence.sh`), CI audit (`audit-task-hierarchy.sh` + `audit-phase-gates.sh`). All 8 canonical orchestrators retrofitted: `x-task-implement`, `x-story-implement`, `x-epic-implement`, `x-release`, `x-epic-orchestrate`, `x-review`, `x-review-pr`, `x-pr-merge-train`. Backward compatible via Rule 19: `taskTracking.enabled` defaults `false` for legacy epics (gates become no-ops); 28 legacy execution-state.json files migrated. `Epic0055FoundationSmokeTest` validates end-to-end.
> - Rule: [`.claude/rules/25-task-hierarchy.md`](.claude/rules/25-task-hierarchy.md)
> - Skill (internal): `x-internal-phase-gate` (not user-invocable)
> - Decision record: [`docs/adr/ADR-0014-task-hierarchy-and-phase-gates.md`](docs/adr/ADR-0014-task-hierarchy-and-phase-gates.md)
> - Epic index: [`ai/epics/epic-0055/`](ai/epics/epic-0055/)

> **Concluded — EPIC-0061 (Local-First Lifecycle & Stack-Aware Governance).** `flowVersion: "3"` marks epics born in local-first lifecycle. Key changes: (1) Rule 20 flipped — non-interactive is DEFAULT; (2) ScriptsAssembler stack-aware (7 stacks × bash templates); (3) Java audit harness (8 `*Auditor` + `AuditEquivalenceSmokeIT`); (4) `audit.yml` deleted; (5) Rule 26 extended with Camada 0 (preventivo, during LLM turn); (6) ADR-0017 published; (7) `migrate-to-local-first.sh` for legacy projects. Tag `local-first-lifecycle-frozen` marks epic closure.

> **Concluded — Folder Cleanup Post-v4 (2026-04-29).** Finishes the EPIC-0062 migration by relocating the 60 legacy epics (`plans/epic-0001..0060`) into `ai/epics/epic-NNNN-<slug>/` via `git mv` (history preserved); merges runtime artifacts of epics 0062–0064 into their canonical `ai/epics/` siblings; removes `plans/` and the orphan `/adr/` folder. ADR housekeeping: the orphan `ADR-0017-local-first-lifecycle.md` is consolidated into `docs/adr/`, four duplicate-numbering ADRs are renumbered to free slots (`ADR-0015-zero-bypass→ADR-0018`, `ADR-0016-preflight→ADR-0019`, `ADR-001-hexagonal→ADR-0020`, `ADR-0048-B→ADR-0021`), and the `docs/adr/README.md` index is rebuilt with all 22 ADRs. Skill `x-adr-generate` is fixed to write to `docs/adr/` (was the root cause of the orphan). Hooks/scripts (`telemetry-*`, `verify-*`, `enforce-preflight-*`, `audit-*`) and 85 source-of-truth skills/rules/agents/templates retrofitted from `plans/epic-*/` to `ai/epics/epic-*/`. 920 golden fixtures regenerated from updated templates; `GoldenFileTest` + `PlatformGoldenFileTest` green. `release-state-X.Y.Z.json` lifecycle path repointed to `ai/releases/` (with corresponding `.gitignore` update).

> **ZERO-BYPASS LIFECYCLE — INEGOCIÁVEL:** Toda story/task DEVE ser implementada
> via `/x-story-implement`. Nenhum PR pode ser mergeado sem:
> (1) 6 artefatos de Fase 1 em `ai/epics/epic-XXXX/plans/`;
> (2) 4 artefatos de Fase 3 em `ai/epics/epic-XXXX/reports/`;
> (3) Eventos de telemetria de `x-story-implement` em `events.ndjson`;
> (4) Seção "## Orchestrator Evidence" preenchida no PR body.
> Bypass é detectado e bloqueado em CI. Não há escape hatch para happy-path.
> Ver [Rule 27](.claude/rules/27-zero-bypass-lifecycle.md) e [EPIC-0059](ai/epics/epic-0059/).

> **EXECUTION INTEGRITY — Camada 0 (NEW — EPIC-0063):** Gates locais pré-flight via
> PreToolUse hook `enforce-preflight-gates.sh`. `git push`, `gh pr create`, `Skill x-pr-create`
> são interceptados e bloqueados se `scripts/preflight.sh` falha. Único bypass:
> `CLAUDE_RECOVERY_MODE=1` (com WARNING visível). Camada 0 é **preventiva** — bloqueia
> antes do remote op; Camadas 1-4 (Rule 24) são **detectivas** — pegam após o fato.
> Ver [Rule 24 §Camada 0](.claude/rules/24-execution-integrity.md).

> **Concluded — EPIC-0070 (Value-Driven Templates v2).** `flowVersion: "4"` epics born after 2026-04-30 MUST use v2 value-driven template format. Key changes: (1) `_TEMPLATE-STORY.md` and `_TEMPLATE-EPIC.md` now include `## 3. Hipótese & OKRs` + `## Refinement Verdict`; (2) `x-internal-epic-create` and `x-internal-story-create` emit v2 by default (`--legacy-template-v1` deprecated, 2-release window); (3) `_TEMPLATE-ARCHITECTURE-SYSTEM.md` with §11 Decision Log; (4) `/x-arch-system-update` (incremental `system.md` updater, idempotent); (5) `/x-template-migrate` (v1→v2 assistant with atomic write, `PARSER_ERROR` abort, `--dry-run`); (6) `audit-template-version.sh` (Camada 2 CI gate) + empty baseline `governance/baselines/template-version-baseline.txt`; (7) EPIC-0056 marked `## ⛔ SUPERSEDED`. Tag `value-driven-templates-v2-frozen` marks epic closure.
> - Skills: [`/x-arch-system-update`](src/main/resources/targets/claude/skills/core/plan/x-arch-system-update/SKILL.md) · [`/x-template-migrate`](src/main/resources/targets/claude/skills/core/plan/x-template-migrate/SKILL.md)
> - Audit script: [`audit-template-version.sh`](src/main/resources/targets/claude/scripts/audit-template-version.sh)
> - Superseded: [`epic-0056.md`](ai/epics/epic-0056-ra9-planning-templates/epic-0056.md) → EPIC-0070

> **Concluded — EPIC-0072 (Comprehensive Test Strategy).** Three conditional quality gates now exist in `x-story-implement` Phase 3 (§3.Q): `x-test-performance` → `x-test-mutation` → `x-test-contract` (D-R11 fast-fail sequence). All gates are conditional on `quality.*.enabled=true` in project YAML — existing projects are unaffected (Rule 19). Key deliverables: (1) 3 stack-aware skills (`x-test-performance` dispatches Newman/ghz/hyperfine/Artillery; `x-test-mutation` dispatches PIT/Stryker/mutmut/go-mutesting; `x-test-contract` dispatches openapi-diff/buf/SCC/schema-registry); (2) 1 KP `performance-engineering` (9 docs); (3) 3 plan templates (`_TEMPLATE-PERFORMANCE-PLAN.md`, `_TEMPLATE-PERFORMANCE-BASELINE.md`, `_TEMPLATE-CONTRACT-PLAN.md`); (4) 3 Camada 2 audit scripts (`audit-perf-baseline.sh`, `audit-mutation-score.sh`, `audit-contract-breaking.sh`); (5) `QualityConfig` domain record + parser; (6) Rule 05 §Quality Gates extended with 3 conditional gates; (7) Rule 24 §Mandatory Evidence Artifacts extended with 3 conditional entries; (8) `audit-execution-integrity.sh` extended with `QUALITY_*_ENABLED` env vars; (9) ADR-0025 (Comprehensive Test Strategy); (10) 8-scenario `Epic0072TestStrategySmokeIT` E2E smoke.
> - Skills: [`x-test-performance`](src/main/resources/targets/claude/skills/conditional/test/x-test-performance/SKILL.md) · [`x-test-mutation`](src/main/resources/targets/claude/skills/conditional/test/x-test-mutation/SKILL.md) · [`x-test-contract`](src/main/resources/targets/claude/skills/conditional/test/x-test-contract/SKILL.md)
> - Audit scripts: [`audit-perf-baseline.sh`](src/main/resources/targets/claude/scripts/audit-perf-baseline.sh) · [`audit-mutation-score.sh`](src/main/resources/targets/claude/scripts/audit-mutation-score.sh) · [`audit-contract-breaking.sh`](src/main/resources/targets/claude/scripts/audit-contract-breaking.sh)
> - ADR: [`docs/adr/ADR-0025-comprehensive-test-strategy.md`](docs/adr/ADR-0025-comprehensive-test-strategy.md)
> - Epic index: [`ai/epics/epic-0072-comprehensive-test-strategy/`](ai/epics/epic-0072-comprehensive-test-strategy/)

> **Concluded — EPIC-0071 (Documentation as DoD).** Documentation updates are now a blocking gate in the story lifecycle. Key changes: (1) `x-doc-validate` invoked as **MANDATORY TOOL CALL** in `x-story-implement` Phase 3 (before verify gate) — `--skip-doc` confined to `## Recovery` blocks only; (2) stack-aware targets: README always, OpenAPI/asyncapi/gRPC conditional on project YAML; (3) `x-release-changelog` v2 generates hybrid `### Highlights` narrative block from epic "Entrega de Valor" + Keep-a-Changelog sections; (4) `audit-doc-freshness.sh` (Camada 2 CI gate, Rule 26) + `governance/baselines/doc-freshness-baseline.txt` (empty, immutable); (5) Rule 31 (Documentation Freshness Gate) + ADR-0024; (6) Rule 24 §Mandatory Evidence Artifacts extended: `x-doc-validate` → `doc-validate-report-STORY-ID.md`; (7) `verify-story-completion.sh` extended to check doc-validate artifact; (8) all 7 `audit-bypass-flags.sh` templates detect `--skip-doc` outside Recovery blocks. Tag `documentation-as-dod-frozen` marks epic closure.
> - Rule: [`.claude/rules/31-documentation-freshness-gate.md`](.claude/rules/31-documentation-freshness-gate.md)
> - Skill: [`x-doc-validate`](src/main/resources/targets/claude/skills/core/ops/x-doc-validate/SKILL.md)
> - Audit script: [`audit-doc-freshness.sh`](src/main/resources/targets/claude/scripts/audit-doc-freshness.sh)

> **REFINEMENT GATE — INEGOCIÁVEL (NEW — EPIC-0069):** Toda story/epic DEVE
> ser refinada via `/x-story-refine STORY-ID` ou `/x-epic-refine EPIC-ID` antes
> de invocar `x-story-implement`, `x-epic-implement`, `x-task-implement`, ou
> `x-epic-orchestrate`. PreToolUse hook `enforce-refinement-gate.sh` (Camada 0)
> bloqueia com exit `33 REFINEMENT_REQUIRED` quando o `refinementVerdict.status`
> do alvo não é `"approved"`. CI script `audit-refinement-gate.sh` (Camada 2,
> Rule 26) confirma post-merge — também detecta divergência state↔markdown via
> `verdictHash`. Bypasses aceitos: `CLAUDE_RECOVERY_MODE=1` (Rule 27), branches
> `hotfix/*` (Rule 27 Exception 2), `flowVersion=1` (Rule 19 fallback). Sem
> outro escape hatch.
> Ver [Rule 29](.claude/rules/29-refinement-gate.md) e [EPIC-0069](ai/epics/epic-0069-refinement-and-dor-gate/).

> **In progress — EPIC-0046 (Lifecycle Integrity Phase 2 — CI enforcement).**
> Story-0046-0007 ships `LifecycleIntegrityAuditTest` (Maven CI-blocking). The audit scans every `SKILL.md` under `java/src/main/resources/targets/claude/skills/` for three Rule 22 regressions: `ORPHAN_PHASE` (dotted sub-section documented but not referenced elsewhere), `WRITE_WITHOUT_COMMIT` (write to `ai/epics/epic-*/reports/` with no `x-git-commit` in the next 20 lines), and `SKIP_IN_HAPPY_PATH` (`--skip-verification` / `--skip-status-sync` used outside `## Recovery` / `## Error Handling`). Baseline at `audits/lifecycle-integrity-baseline.txt` tolerates current TOC-style sub-sections; any NEW violation fails the build with `LIFECYCLE_AUDIT_REGRESSION`. Escape hatch: place `<!-- audit-exempt -->` on the line immediately before (or on) the intentional violation; keep usage rare (reviewed exceptions only). Standalone CLI: `java -cp target/test-classes:target/classes dev.iadev.adapter.inbound.cli.LifecycleAuditCli scan [--skills-root <path>] [--json]` (exit 0 / 11 / 2).
> - Story: [`ai/epics/epic-0046/story-0046-0007.md`](ai/epics/epic-0046/story-0046-0007.md)

> **In progress — EPIC-0043 (Interactive Gates Convention).**
> Standardizes interactive decision gates across orchestrating skills (`x-release`, `x-story-implement`, `x-epic-implement`, `x-review-pr`) with a fixed 3-option menu (PROCEED / FIX-PR / ABORT) as the default behavior. Menu is now default; `--non-interactive` replaces the patchwork of opt-in flags for CI/automation. FIX-PR slot invokes `x-pr-fix`/`x-pr-fix-epic` via Rule 13 INLINE-SKILL and loops back to the same menu. Guard-rail caps 3 consecutive fix attempts with `GATE_FIX_LOOP_EXCEEDED`. Rule 20 + ADR-0010 published in story-0043-0001; retrofits follow in stories 0043-0002 through 0043-0006.
> - Decision record: [`docs/adr/ADR-0010-interactive-gates-convention.md`](docs/adr/ADR-0010-interactive-gates-convention.md)
> - Story index: [`ai/epics/epic-0043/`](ai/epics/epic-0043/)

> **Concluded — EPIC-0058 (Audit Scripts Lifecycle & Generation).**
> Formalizes the lifecycle of governance audit gates: creates Rule 26 "Audit Gate Lifecycle" + ADR-0015 (4-layer taxonomy: Hook/CI script/Java test/Workflow); creates 3 missing CI scripts referenced in Rules 19/21/22 (`audit-flow-version.sh`, `audit-epic-branches.sh`, `audit-skill-visibility.sh`); introduces `ScriptsAssembler` so generated projects inherit governance gates; regenerates golden files for 9 profiles. **Note (EPIC-0061 story-0061-0005):** `audit.yml` CI workflow removed — audits now run inline via `mvn verify` through Java `*AuditorTest` classes (RULE-007, RULE-008).
> - Rule: [`.claude/rules/26-audit-gate-lifecycle.md`](.claude/rules/26-audit-gate-lifecycle.md)
> - Decision record: [`docs/adr/ADR-0015-audit-gate-lifecycle.md`](docs/adr/ADR-0015-audit-gate-lifecycle.md)
> - Epic index: [`ai/epics/epic-0058/`](ai/epics/epic-0058/)

> **In progress — EPIC-0036 (Skill Taxonomy Refactor).**
> The source of truth for skills under `java/src/main/resources/targets/claude/skills/` is being reorganized into 10 category subfolders (`plan/`, `dev/`, `test/`, `review/`, `security/`, `code/`, `git/`, `pr/`, `ops/`, `jira/`), and ~19 skills will be renamed to a consistent `x-{subject}-{action}` scheme. The generated output `.claude/skills/` remains **flat** — user-facing invocation paths are preserved.
> - Decision record: [`docs/adr/ADR-0003-skill-taxonomy-and-naming.md`](docs/adr/ADR-0003-skill-taxonomy-and-naming.md)
> - Rename staging checklist: [`ai/epics/epic-0036/skill-renames.md`](ai/epics/epic-0036/skill-renames.md)
> - Current skill names are the renamed forms (e.g., `/x-epic-create`, `/x-task-implement`, `/x-test-e2e`). Do not use the old pre-rename names.

## Structure

```
CLAUDE.md                   <-- Executive summary (project root, loaded automatically)
.claude/
|-- README.md               <-- Usage guide
|-- settings.json           <-- Shared settings (committed to git)
|-- settings.local.json     <-- Local overrides (gitignored)
|-- hooks/                  <-- Automations (post-compile, etc.)
|-- rules/                  <-- Project rules (loaded into system prompt)
|-- skills/                 <-- Skills invocable via /command
|   +-- {knowledge-packs}/  <-- Knowledge packs (not invocable, referenced internally)
+-- agents/                 <-- AI personas (used by skills and lifecycle)
```

### settings.json vs settings.local.json

- **`settings.json`**: Team settings (permissions, hooks). Committed to git.
- **`settings.local.json`**: Local overrides. In `.gitignore`. Overrides `settings.json`.

---

## Rules

Rules are loaded automatically into the system prompt of EVERY conversation.
They define mandatory standards that Claude MUST follow when generating code.

| # | File | Scope |
|---|------|-------|
| 01 | `01-project-identity.md` | project identity |
| 02 | `02-domain.md` | domain |
| 03 | `03-coding-standards.md` | coding standards |
| 04 | `04-architecture-summary.md` | architecture summary |
| 05 | `05-quality-gates.md` | quality gates |
| 06 | `06-security-baseline.md` | security baseline |
| 07 | `07-operations-baseline.md` | operations baseline |
| 08 | `08-release-process.md` | release process |
| 09 | `09-branching-model.md` | branching model (Git Flow) |
| 13 | `13-skill-invocation-protocol.md` | skill invocation protocol (delegation syntax) |
| 23 | `23-model-selection.md` | model selection strategy (Opus/Sonnet/Haiku tiers, enforcement points, CI audit contract) |
| 25 | `25-task-hierarchy.md` | task hierarchy (4-level) + phase gates contract (EPIC-0055) |
| 26 | `26-audit-gate-lifecycle.md` | audit gate lifecycle (4-layer taxonomy, naming, exit codes, self-check contract — EPIC-0058) |
| 27 | `27-zero-bypass-lifecycle.md` | zero-bypass lifecycle contract (Rule 24 vs Rule 27 distinction, 12 surfaces, 4 enforcement layers — EPIC-0059) |

**Total: 14 rules** (gaps at 10, 11, 12 reserved for conditional rules: `10-anti-patterns.*`, `11-security-pci`, `12-security-anti-patterns`; gap at 24 reserved for Rule 24 "Execution Integrity" tracked separately)

### Numbering

- Gaps in numbering allow future insertion without renumbering existing rules.

---

## Skills (Slash Commands)

Skills are invoked by the user via `/name` in chat. They are lazy-loaded (only load when invoked).

A complete list of skills with descriptions is generated in `.claude/README.md` by the `ia-dev-env` generator.

### Authoring a New Skill

- Start from `java/src/main/resources/shared/templates/_TEMPLATE-SKILL.md` (authoring template for SKILL.md files).
- The template includes a "## Telemetry (Optional)" section with plug-and-play helper calls (`telemetry-phase.sh start/end`, `subagent-start/end`, `mcp-start/end`). Copy-paste into numbered phases of the new skill to keep telemetry coverage close to 100% as the catalog grows (EPIC-0040).
- Canonical example: `x-story-implement` — review its phase markers for a working reference.
- See `.claude/rules/13-skill-invocation-protocol.md` for the markers contract.

### Usage Examples

```bash
# Run a specific skill
/skill-name argument

# Get help on available skills
# Type / in the chat to see the full list
```

---

## Knowledge Packs, Agents, Hooks

- **Knowledge Packs** (`user-invocable: false`): referenced internally by agents and skills; do not appear in the `/` menu.
- **Agents**: system prompts defining specialized personas; used by skills via Task tool, not invoked directly.
- **Hooks**: scripts executed on Claude Code events, configured in `settings.json` under `hooks`.

---

## Telemetry

Every `ia-dev-env`-generated project ships with telemetry capture enabled by default. Skill executions, phase boundaries, subagent lifecycles, and tool calls are recorded as NDJSON under `ai/epics/epic-*/telemetry/events.ndjson`, producing an auditable timeline of how long each part of an epic / story / task actually took. The design is documented in [`docs/adr/ADR-0005-telemetry-architecture.md`](docs/adr/ADR-0005-telemetry-architecture.md); the privacy contract is enforced by [Rule 20 — Telemetry Privacy](.claude/rules/20-telemetry-privacy.md) and the scrubber at `dev.iadev.telemetry.TelemetryScrubber`.

Capture happens through two cooperating layers:

- **Hook-based (automatic).** Five Bash entrypoint scripts under `.claude/hooks/` are registered in `settings.json` and fire on `SessionStart`, `PreToolUse`, `PostToolUse`, `SubagentStop`, and `Stop`. Additional helper scripts in `.claude/hooks/` (e.g., `telemetry-emit.sh`, `telemetry-lib.sh`, `telemetry-phase.sh`) are copied alongside them but are not registered as hook events. No per-skill code is required.
- **In-skill phase markers.** Implementation, planning, and creation skills call `telemetry-phase.sh start|end` around each numbered phase; the `_TEMPLATE-SKILL.md` authoring template includes a copy-paste-ready "Telemetry (Optional)" section.

Two skills consume the NDJSON:

```bash
# Point-in-time report for one or more epics (Mermaid Gantt + aggregates)
/x-telemetry-analyze --epic EPIC-0040

# Cross-epic P95 regression detector (top-10 slowest skills)
/x-telemetry-trend --last 5 --threshold-pct 20
```

Opt out globally with `CLAUDE_TELEMETRY_DISABLED=1`, or per-project by adding the nested YAML block below to the generator YAML (requires regeneration):

```yaml
telemetry:
  enabled: false
```

(The parser `ProjectConfig.parseTelemetryEnabled` expects the nested key; a flat `telemetryEnabled` line is ignored.)

EPIC-0040 shipped this stack — see the [CHANGELOG](CHANGELOG.md#380---2026-04-17) for the full release notes.

---

## Capability Composition — Como funciona

**EPIC-0064 (v5.0.0 — Breaking).** O gerador `ia-dev-env` passou de copy-cego para composição capability-driven. Todo artefato gerado (skills, rules, KPs, agents, hooks, templates) declara `requires-capabilities` em frontmatter v3.0. O pipeline:

```
profile.yaml → CapabilityResolver → ResolvedCapabilitySet
                                        ↓
                               CapabilityAwareComposer
                                        ↓
                              OutputPruner (.claude/ filtrado)
                                        ↓
                        CompositionEngine → Pebble → LLM
```

**Para adicionar uma nova capability:**

1. Crie `capabilities/<categoria>/<id>.yaml` com campos `id`, `name`, `description`, `requires`, `excludes`.
2. Declare `requires-capabilities: [<id>]` nos artefatos que dependem dela.
3. Execute `audit-capability-graph.sh --self-check` para verificar integridade.
4. Ver: [Rule 28 §Invariants](.claude/rules/28-capability-frontmatter-contract.md) · [ADR-0016](docs/adr/ADR-0016-capability-driven-composition.md)

**Para adicionar um novo framework (stack):** edite `java/src/main/resources/targets/claude/skills/conditional/` e declare as capabilities do framework. Ver story-0064-0408/0409.

> Schema v3.0 é obrigatório — não há fallback v2. Baselines vazios em `governance/baselines/capability-*.txt` declaram explicitamente zero exceções (Rule 28 §Forbidden post-merge).

---

## Settings & Artifact Conventions

- `settings.json` (committed): team permissions and hooks.
- `settings.local.json` (gitignored): personal overrides.
- Rules: `NN-name.md` (numbered, no frontmatter).
- Skills: `skills/{name}/SKILL.md` with YAML frontmatter (name, description).
- Agents: `{name}.md`. Hooks: `.sh` / `.json`.

---

## Plan & Review Templates

Templates provide standardized output formats for planning and review artifacts produced by skills.
They contain `{{PLACEHOLDER}}` tokens resolved at runtime by the LLM, not during generation.
Content is copied verbatim by `PlanTemplatesAssembler` (RULE-003).

> **Fallback:** Templates are optional -- skills degrade gracefully without them.
> If a template is not found, skills use inline formatting as fallback and log a warning.

> **Location:** Templates are written to `.claude/templates/` only. Multi-target output (e.g., `.github/templates/`) was removed in EPIC-0034.

| Template | Produced By | Saved To | Pre-Check |
|----------|-------------|----------|-----------|
| `_TEMPLATE-IMPLEMENTATION-PLAN.md` | x-story-implement (Phase 1B) | `ai/epics/epic-XXXX/plans/plan-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-TEST-PLAN.md` | x-test-plan | `ai/epics/epic-XXXX/plans/tests-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-ARCHITECTURE-PLAN.md` | x-arch-plan | `ai/epics/epic-XXXX/plans/arch-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-TASK-BREAKDOWN.md` | x-lib-task-decomposer | `ai/epics/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-SECURITY-ASSESSMENT.md` | x-story-implement (Phase 1E) | `ai/epics/epic-XXXX/plans/security-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-COMPLIANCE-ASSESSMENT.md` | x-story-implement (Phase 1F) | `ai/epics/epic-XXXX/plans/compliance-story-XXXX-YYYY.md` | Yes |
| `_TEMPLATE-SPECIALIST-REVIEW.md` | x-review | `ai/epics/epic-XXXX/plans/review-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-TECH-LEAD-REVIEW.md` | x-review-pr | `ai/epics/epic-XXXX/plans/techlead-review-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` | x-review | `ai/epics/epic-XXXX/plans/review-dashboard-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-REVIEW-REMEDIATION.md` | x-story-implement (Phase 5) | `ai/epics/epic-XXXX/plans/remediation-story-XXXX-YYYY.md` | No |
| `_TEMPLATE-EPIC-EXECUTION-PLAN.md` | x-epic-implement | `ai/epics/epic-XXXX/plans/execution-plan-epic-XXXX.md` | Yes |
| `_TEMPLATE-PHASE-COMPLETION-REPORT.md` | x-epic-implement | `ai/epics/epic-XXXX/reports/phase-report-epic-XXXX.md` | No |

**Total: 12 plan & review templates** (copied to `.claude/templates/`)

---

## Generation Summary

| Component | Count |
|-----------|-------|
| Plan Templates (.claude) | 12 |

---

## Tips

- Rules are always active -- no invocation needed.
- Skills are lazy -- load when you type `/name`.
- Knowledge Packs do not appear in `/` -- used internally by agents.
- Hooks run automatically on events like post-compile.
- To add a skill / rule, create the file under the matching directory.
- The `.claude/` directory is generated -- run `ia-dev-env generate` to regenerate.

Generated by `ia-dev-env`.
