# resources/ -- Usage Guide

This repository is the **source of truth store** for all Claude Code configuration:
coding rules, skills (slash commands), knowledge packs, agents, and templates.
The canonical content lives in the `resources/` directory at the repository root.

> **Note:** The `.claude/` directory is a local, gitignored install produced by
> `bin/install-claude-resources.sh --output .`. Do not edit `.claude/` manually —
> edit `resources/` and re-run the installer.

> **CRITICAL — Source of Truth:**
> The source of truth for skills, knowledge packs, agents, rules, and templates is the `resources/` directory at the repository root.
> The `.claude/` directory is a local, gitignored install produced by `bin/install-claude-resources.sh --output .` — NEVER edit it directly; edit `resources/` instead.

> The `CLAUDE.md` file at the project root provides an executive summary loaded automatically in EVERY conversation.

> See [docs/epics-history.md](./docs/epics-history.md) for concluded epics history.

> **ZERO-BYPASS LIFECYCLE — INEGOCIÁVEL:** Toda story/task DEVE ser implementada
> via `/x-implement-story`. Nenhum PR pode ser mergeado sem:
> (1) 6 artefatos de Fase 1 em `ai/epics/epic-XXXX/plans/`;
> (2) 4 artefatos de Fase 3 em `ai/epics/epic-XXXX/reports/`;
> (3) Eventos de telemetria de `x-implement-story` em `events.ndjson`;
> (4) Seção "## Orchestrator Evidence" preenchida no PR body.
> Bypass é detectado e bloqueado em CI. Não há escape hatch para happy-path.
> Ver [Rule 27](.claude/rules/27-zero-bypass-lifecycle.md) e [EPIC-0059](ai/epics/epic-0059/).

> **EXECUTION INTEGRITY — Camada 0 (NEW — EPIC-0063):** Gates locais pré-flight via
> PreToolUse hook `enforce-preflight-gates.sh`. `git push`, `gh pr create`, `Skill x-create-pr`
> são interceptados e bloqueados se `scripts/preflight.sh` falha. Único bypass:
> `CLAUDE_RECOVERY_MODE=1` (com WARNING visível). Camada 0 é **preventiva** — bloqueia
> antes do remote op; Camadas 1-4 (Rule 24) são **detectivas** — pegam após o fato.
> Ver [Rule 24 §Camada 0](.claude/rules/24-execution-integrity.md).

> **REFINEMENT GATE — INEGOCIÁVEL (NEW — EPIC-0069):** Toda story/epic DEVE
> ser refinada via `/x-refine-story STORY-ID` ou `/x-refine-epic EPIC-ID` antes
> de invocar `x-implement-story`, `x-implement-epic`, `x-implement-task`, ou
> `x-orchestrate-epic`. PreToolUse hook `enforce-refinement-gate.sh` (Camada 0)
> bloqueia com exit `33 REFINEMENT_REQUIRED` quando o `refinementVerdict.status`
> do alvo não é `"approved"`. CI script `audit-refinement-gate.sh` (Camada 2,
> Rule 26) confirma post-merge — também detecta divergência state↔markdown via
> `verdictHash`. Bypasses aceitos: `CLAUDE_RECOVERY_MODE=1` (Rule 27), branches
> `hotfix/*` (Rule 27 Exception 2), `flowVersion=1` (Rule 19 fallback). Sem
> outro escape hatch.
> Ver [Rule 29](.claude/rules/29-refinement-gate.md) e [EPIC-0069](ai/epics/epic-0069-refinement-and-dor-gate/).

> **Retired — EPIC-0046 (Lifecycle Integrity Phase 2).**
> The Maven CI-blocking `LifecycleIntegrityAuditTest` and the `LifecycleAuditCli` Java tooling were removed together with the Java/Maven codebase when this repository became a pure resource store. The Rule 22 regression concepts it guarded (`ORPHAN_PHASE`, `WRITE_WITHOUT_COMMIT`, `SKIP_IN_HAPPY_PATH`) remain valid authoring guidance for `SKILL.md` files under `resources/skills/`; enforcement is now manual review until a shell-based audit replaces it.
> - Story: [`ai/epics/epic-0046/story-0046-0007.md`](ai/epics/epic-0046/story-0046-0007.md)

> **In progress — EPIC-0043 (Interactive Gates Convention).**
> Standardizes interactive decision gates across orchestrating skills (`x-release`, `x-implement-story`, `x-implement-epic`, `x-review-pr`) with a fixed 3-option menu (PROCEED / FIX-PR / ABORT) as the default behavior. Menu is now default; `--non-interactive` replaces the patchwork of opt-in flags for CI/automation. FIX-PR slot invokes `x-fix-pr`/`x-fix-epic-pr` via Rule 13 INLINE-SKILL and loops back to the same menu. Guard-rail caps 3 consecutive fix attempts with `GATE_FIX_LOOP_EXCEEDED`. Rule 20 + ADR-0010 published in story-0043-0001; retrofits follow in stories 0043-0002 through 0043-0006.
> - Decision record: [`docs/adr/ADR-0010-interactive-gates-convention.md`](docs/adr/ADR-0010-interactive-gates-convention.md)
> - Story index: [`ai/epics/epic-0043/`](ai/epics/epic-0043/)

> **In progress — EPIC-0036 (Skill Taxonomy Refactor).**
> The source of truth for skills under `resources/skills/` is being reorganized into 10 category subfolders (`plan/`, `dev/`, `test/`, `review/`, `security/`, `code/`, `git/`, `pr/`, `ops/`, `jira/`), and ~19 skills will be renamed to a consistent `x-{subject}-{action}` scheme. The generated output `.claude/skills/` remains **flat** — user-facing invocation paths are preserved.
> - Decision record: [`docs/adr/ADR-0003-skill-taxonomy-and-naming.md`](docs/adr/ADR-0003-skill-taxonomy-and-naming.md)
> - Rename staging checklist: [`ai/epics/epic-0036/skill-renames.md`](ai/epics/epic-0036/skill-renames.md)
> - Current skill names are the renamed forms (e.g., `/x-epic-create`, `/x-implement-task`, `/x-execute-e2e-tests`). Do not use the old pre-rename names.

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
| 23 | `23-model-selection.md` | model selection strategy |
| 25 | `25-task-hierarchy.md` | task hierarchy (4-level) + phase gates contract |
| 26 | `26-audit-gate-lifecycle.md` | audit gate lifecycle (4-layer taxonomy) |
| 27 | `27-zero-bypass-lifecycle.md` | zero-bypass lifecycle contract |

**Total: 14 rules** (gaps at 10–12 reserved for conditional rules; gap at 24 reserved for Rule 24)

---

## Skills (Slash Commands)

Skills are invoked via `/name` in chat — lazy-loaded (only when invoked).
Each skill lives under `resources/skills/<name>/SKILL.md`.
To author a new skill, start from `resources/templates/_TEMPLATE-SKILL.md`.
See `.claude/rules/13-skill-invocation-protocol.md` for the invocation markers contract.

---

## Knowledge Packs, Agents, Hooks

- **Knowledge Packs** (`user-invocable: false`): referenced internally by agents and skills; do not appear in `/`.
- **Agents**: system prompts defining specialized personas; used by skills via Task tool, not invoked directly.
- **Hooks**: scripts executed on Claude Code events, configured in `settings.json` under `hooks`.

---

## Telemetry

Skill executions, phase boundaries, subagent lifecycles, and tool calls are recorded as NDJSON under
`ai/epics/epic-*/telemetry/events.ndjson`. Design: [`ADR-0005`](docs/adr/ADR-0005-telemetry-architecture.md).
Opt out: `CLAUDE_TELEMETRY_DISABLED=1` or `telemetry.enabled: false` in the generator YAML.
Consume with `/x-analyze-telemetry` (Gantt + aggregates) and `/x-analyze-telemetry-trends` (P95 regression).

---

## Capability Composition

**EPIC-0064 (v5.0.0 — Breaking).** The generator is capability-driven: every artefact declares
`requires-capabilities` in frontmatter v3.0. Schema v3.0 is mandatory — no fallback v2.
See [Rule 28](.claude/rules/28-capability-frontmatter-contract.md) and
[ADR-0016](docs/adr/ADR-0016-capability-driven-composition.md) for invariants, audit scripts, and migration.

---

## Settings & Artifact Conventions

- `settings.json` (committed): team permissions and hooks.
- `settings.local.json` (gitignored): personal overrides.
- Rules: `NN-name.md` (numbered, no frontmatter). Skills: `skills/{name}/SKILL.md` with YAML frontmatter.
- Agents: `{name}.md`. Hooks: `.sh` / `.json`.

---

## Plan & Review Templates

12 plan & review templates are generated to `.claude/templates/` — implementation, test, architecture,
task-breakdown, security, compliance, specialist/TL reviews, epic plan, and phase report.
Source: `shared/templates/_TEMPLATE-*.md`. Content is copied verbatim by `PlanTemplatesAssembler` (RULE-003).

---

## Tips

- Rules are always active -- no invocation needed.
- Skills are lazy -- load when you type `/name`.
- Knowledge Packs do not appear in `/` -- used internally by agents.
- Hooks run automatically on events configured in `resources/settings.json`.
- To add a skill / rule, create the file under the matching directory in `resources/`.
- `.claude/` is a local install -- run `bin/install-claude-resources.sh --output . --force` to refresh it from `resources/`.

This repository is the Claude Code resource store. Source of truth: `resources/`.
