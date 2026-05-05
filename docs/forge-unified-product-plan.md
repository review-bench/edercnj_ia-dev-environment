# Forge — Plano Unificado para Ideação de Produto, Capabilities e Features

> **Status:** Plano consolidado e organizado para servir como entrada direta de `forge ideate --kind product|capability|feature` (e equivalentes atuais `x-feature-ideate` / `x-feature-create`).
> **Origem:** Síntese de [`forge-strategic-plan-v3.md`](forge-strategic-plan-v3.md) (estratégia) + [`forge-implementation-roadmap.md`](forge-implementation-roadmap.md) (ordem de execução), reconciliados com o estado atual do `ia-dev-environment` **até EPIC-0076 entregue + EPIC-0077 (Product-First Lifecycle) em andamento**.
> **Propósito:** Forge é um **projeto novo** que usa o `ia-dev-environment` como base. Tudo que já está implementado nele é **dado de entrada**, não escopo de novo épico.
> **Hierarquia oficial deste plano:** `Project → Product → Capability → Feature` (decomposição em Epic/Story/Task fica para refinement).
> **Audiência:** PO + Tech Lead + Architect + Security + QA + SRE/DevOps em sessão de ideação multi-persona.

---

## §0. Como ler este documento

| Seção | O que entrega | Quando consultar |
| :--- | :--- | :--- |
| §1 | Visão, tese e princípios inegociáveis | Para enquadrar a ideação. |
| §2 | Premissas herdadas do `ia-dev-environment` (até EPIC-0076 + parcial 0077) | Para saber o que **NÃO** precisa virar épico do Forge. |
| §3 | Hierarquia canônica e glossário | Para alinhar vocabulário antes de criar artefatos. |
| §4 | Modelo operacional do runtime e bounded contexts | Para enquadrar Capabilities estruturalmente. |
| §5 | Catálogo de **Products** (P-1 a P8) | Para escolher o Product de cada ciclo de ideação. |
| §6 | Catálogo de **Capabilities por Product** | Para criar `forge capability create` em sequência. |
| §7 | Catálogo de **Features V0 por Capability** | Para criar `forge feature create` em sequência. |
| §8 | Mapa de migração das 123 skills atuais → comandos/serviços/workers Forge | Para refinement de cada Feature que substitui ou herda skill atual. |
| §9 | Migração de Rules → Policies | Para refinement das Capabilities que envolvem governança. |
| §10 | Migração de templates, KPs, hooks e scripts | Para refinement das Capabilities de Foundations e Spine. |
| §11 | Wave plan executável (com ajustes pós-0076/0077) | Para sequência de releases incrementais da V0. |
| §12 | Deltas vs roadmap original (renomes, omissões e adições) | Para reconciliar este plano contra v3 e roadmap. |
| §13 | Open questions a resolver antes de gerar épicos | Para ADRs prévios à decomposição. |
| §14 | Próximos passos | Comandos a rodar imediatamente após aprovação deste plano. |

> **Regra de precedência:** este documento prevalece sobre v3/roadmap em **naming** e **estado-atual**; v3 prevalece em **conteúdo estratégico** (tese, schemas canônicos, error taxonomy); roadmap prevalece em **ordem operacional** quando ambos discutem waves.

---

## §1. Visão, Tese e Princípios

### §1.1. Visão

Construir o **Forge**: uma plataforma local-first, CLI-first, **multi-LLM e multi-IDE** que controla determinísticamente o lifecycle de entrega assistida por IA — da ideação ao release — produzindo evidências verificáveis, auditáveis e replayáveis. O LLM gera conteúdo; o **Forge controla o fluxo**.

### §1.2. Tese (4 princípios inegociáveis)

| # | Princípio | Consequência arquitetural |
| :-: | :--- | :--- |
| 1 | **Local-first, CLI-first** | Caminho feliz roda 100% offline. TUI/web/IDE/SaaS são camadas opcionais que consomem o mesmo core. |
| 2 | **Inversão de controle** | O Forge é o orquestrador determinístico; o LLM é worker invocado para tarefas pontuais. State machines, gates, commits, PRs, telemetria viram código — não markdown interpretado. |
| 3 | **Evidence-first** | Todo comando mutável produz `artifact_kind` tipado; sem evidência, não há checkpoint remoto, não há merge, não há release. |
| 4 | **Multi-provider, multi-target** | Claude/GPT/Gemini/locais via `LlmProvider` abstraction; Claude Code/Cursor/Windsurf/Aider/JetBrains/VS Code/MCP via target adapter. |

### §1.3. Posicionamento

Não competimos em "a IA faz tudo sozinha". Diferencial: **a IA faz certo, com governança, evidência e execução auditável, localmente, em qualquer IDE e com múltiplos LLMs**.

### §1.4. Camadas atuais (0-4) colapsam em duas

O `ia-dev-environment` hoje usa 5 camadas de enforcement (Camada 0 PreToolUse hook, Camada 1 normativa, Camada 2 CI script, Camada 3 Java test, Camada 4 observabilidade). No Forge:

- **Camada A — Runtime in-process.** Forge bloqueia operação ilegal antes de qualquer side-effect.
- **Camada B — `forge ci verify`.** Valida invariantes post-merge sobre evidência produzida.

Hooks shell e audit scripts deixam de ser mecanismo primário; viram serviços/políticas no runtime ou checks no `forge ci verify`.

---

## §2. Premissas herdadas do `ia-dev-environment` (até EPIC-0076 + parcial EPIC-0077)

> **CRÍTICO:** o que está nesta seção **não vira épico** do Forge — é dado de entrada. Forge nasce sobre essa base.

### §2.1. Capability composition (EPIC-0064 — Concluído)

| Premissa | Detalhe |
| :--- | :--- |
| Schema v3.0 estrito | Frontmatter universal v3.0 obrigatório em skill/rule/KP/agent/hook/template (Rule 28). |
| `CapabilityResolver`, `CapabilityAwareComposer`, `OutputPruner` | Pipeline `profile.yaml → ResolvedCapabilitySet → composed .claude/`. |
| `requires-capabilities`, `requires-any`, `excludes-capabilities` | Sintaxe primária em frontmatter. |
| Glob `category.subcategory.*` / `**` | Suportado. |
| Audit gates | `audit-capability-coverage.sh`, `audit-frontmatter-schema.sh`, `audit-capability-graph.sh`, `audit-fragment-coherence.sh`, `audit-output-pruning.sh`, `audit-capability-determinism.sh`. |

> **Implicação para Forge:** P-1.C03 (Identity & Schemas) e P-1.C04 (Profile & Capability Composition) **já existem como código Java reutilizável**. O escopo no Forge é evoluí-los para `frontmatter v4` (com `provides:`) e migrar o renderer Pebble para um Template Registry tipado.

### §2.2. Templates Value-Driven v2 (EPIC-0070 — Concluído)

| Premissa | Detalhe |
| :--- | :--- |
| `_TEMPLATE-EPIC.md` v2 e `_TEMPLATE-STORY.md` v2 | Inclui `## 3. Hipótese & OKRs` + `## Refinement Verdict` + 4-category Gherkin AC. |
| `_TEMPLATE-ARCHITECTURE-SYSTEM.md` | `system.md` único com §11 Decision Log; `/x-update-system-architecture` é incremental e idempotente. |
| `/x-migrate-templates` | Assistente v1 → v2 com `--dry-run` e PARSER_ERROR abort. |
| `audit-template-version.sh` | Gate Camada 2; baseline imutável. |
| Rule 30 | Value-Driven Templates como contrato. |

> **Implicação para Forge:** templates já têm conteúdo certo; faltam **`template_id` + `schema_version` + JSON Schemas tipados** (P-1.C04 evolução).

### §2.3. Documentation as DoD (EPIC-0071 — Concluído)

| Premissa | Detalhe |
| :--- | :--- |
| `x-validate-docs` | MANDATORY TOOL CALL em Phase 3 de `x-story-implement` (ex-`x-implement-story`). |
| Targets stack-aware | README always; OpenAPI/asyncapi/gRPC condicional via `documentation.targets`. |
| `x-generate-release-changelog` v2 | Hybrid format (Highlights + Keep-a-Changelog). |
| `audit-doc-freshness.sh` | Gate Camada 2. |
| Rule 31 | Documentation Freshness Gate. |

### §2.4. Comprehensive Test Strategy (EPIC-0072 — Concluído)

| Premissa | Detalhe |
| :--- | :--- |
| 3 gates condicionais | `x-execute-performance-tests` → `x-execute-mutation-tests` → `x-execute-contract-tests` (D-R11 fast-fail) em Phase 3 de `x-story-implement`. |
| `quality.{performance,mutation,contract}.enabled` | Opt-in YAML; safe default `false`. |
| Stack-aware dispatchers | Newman/ghz/hyperfine/Artillery; PIT/Stryker/mutmut/go-mutesting; openapi-diff/buf/SCC/schema-registry. |
| `audit-perf-baseline.sh`, `audit-mutation-score.sh`, `audit-contract-breaking.sh` | Gates Camada 2. |
| ADR-0025 + Epic0072TestStrategySmokeIT | Validações end-to-end. |

### §2.5. Dependency Policy Gate (EPIC-0074 — Concluído)

| Premissa | Detalhe |
| :--- | :--- |
| `x-validate-dependency-policy` | MANDATORY TOOL CALL em Phase 3 de `x-story-implement` quando `dependencies.policy.enabled=true`. |
| 5 dimensões | CVEs hard-block (`denied-cves`), severidade CVE, whitelist SPDX, cross-stack version constraints, freshness window. |
| `DependencyPolicyConfig` Java | Domain record com parser. |
| Rule 32, ADR-0027, `audit-dep-policy.sh` | Camada 2 + smoke. |

### §2.6. AI Memory Layer (EPIC-0075 — Concluído)

| Premissa | Detalhe |
| :--- | :--- |
| `ai/memory/epic-XXXX-summary.md` | Compact, ≤200 lines, gerado por `x-internal-summarize-epic` (haiku, deterministic) em Phase 5 de `x-epic-implement`. |
| `/x-search-memory` | 5 modos (`--by-tag`, `--by-capability`, `--by-rule`, `--by-pattern`, `--by-epic`). |
| Rule 33, `audit-memory-coverage.sh` | Gate Camada 2 + ADR-0028. |
| 27 épicos seedados retroativamente | range 0040-0075. |

### §2.7. Verb-First Naming (EPIC-0076 — Concluído pre-Forge)

| Premissa | Detalhe |
| :--- | :--- |
| Padrão `x-<verbo>-<objeto>` | públicas: `x-<verbo>-<objeto>`; internas: `x-internal-<verbo>-<objeto>`; libs: `x-lib-<verbo>-<objeto>`. |
| Renomes-chave | `x-implement-story` → `x-story-implement`; `x-implement-epic` → `x-epic-implement`; `x-implement-task` → `x-task-implement`; `x-refine-story` → `x-story-refine`; `x-refine-epic` → `x-epic-refine`; `x-create-pr` → `x-pr-create`; `x-fix-pr` → `x-pr-fix`; `x-merge-pr` → `x-pr-merge`; `x-watch-pr-ci` → `x-pr-ci-watch`; `x-create-feature` → `x-feature-create`; `x-ideate-feature` → `x-feature-ideate`; `x-plan-architecture` → `x-arch-plan`; `x-update-architecture` → `x-arch-update`; `x-update-system-architecture` → `x-arch-system-update`; `x-search-memory` → `x-memory-search`; `x-internal-summarize-epic` → `x-internal-epic-summary`; `x-internal-update-status` → `x-internal-status-update`; `x-internal-write-report` → `x-internal-report-write`; `x-internal-normalize-args` → `x-internal-args-normalize`; etc. |
| Sem aliases permanentes | Hard-cut (Rule 19 §Hard-cut autorizado, condições 1-3). |

> **Implicação para Forge:** todos os mapeamentos do roadmap original (`x-implement-story`, `x-create-pr`, …) devem ser lidos com os **nomes pós-0076**. Esta seção e §8 abaixo já refletem o estado canônico.

### §2.8. Product-First Lifecycle (EPIC-0077 — Em andamento)

| Premissa | Detalhe |
| :--- | :--- |
| Hierarquia canônica do `ia-dev-env` | `Ideation → Product → Capability → Feature → Epic → Story → Task`. |
| `flowVersion: "5"` | Marca epics/stories nascidos sob lifecycle product-first. |
| `productFirstLifecycle: true` | Field obrigatório em `execution-state.json` quando flowVersion=5. |
| Templates novos | `_TEMPLATE-PRODUCT.md` (8 seções, RNFs Root), `_TEMPLATE-CAPABILITY.md` (7 seções, RNF no-relax). |
| C4 model obrigatório | C1 System Context + C2 Container + C3 Component + C4 Code (Mermaid) em planning artifacts. |
| RNF gate (no-relax) | `x-internal-rnf-validate` + `ValidateRNFNoRelaxUseCase` + Phase Gate em refinement. |
| Skills criadas | `x-arch-plan-product`, `x-arch-plan-capability` (commands), `x-internal-rnf-validate`, `XArchPlanC4MandatorySmokeTest`, `C4DiagramGeneratorTest`, `C4LevelValidatorTest`. |
| Rule estendida (19 §Field Additions) | `productFirstLifecycle` field. |

> **Implicação CRÍTICA para Forge:** o roadmap usa **"Capacity"**. O `ia-dev-env` (EPIC-0064 capability composition + EPIC-0077 product-first) **já consolidou "Capability"** como termo canônico para ambos os planos (técnico e de produto). Este documento **adota Capability como termo único**. Todas as referências futuras a "Capacity" no v3 devem ser lidas como Capability.

### §2.9. Outras invariantes herdadas

| Rule | Estado |
| :--- | :--- |
| Rule 13 (Skill Invocation Protocol — 3 patterns) | Concluído. |
| Rule 14 (Project Scope Guard — Product-First domain extension) | Concluído. |
| Rule 19 (Backward Compat — flowVersion 1-5) | Concluído. |
| Rule 20 (Interactive Gates — non-interactive default) | Concluído. |
| Rule 21 (Epic Branch Model) | Concluído. |
| Rule 22 (Skill Visibility) | Concluído. |
| Rule 23 (Model Selection — Opus/Sonnet/Haiku tiers) | Concluído. |
| Rule 24 (Execution Integrity — 5 camadas + 14 mandatory artifacts) | Concluído. |
| Rule 25 (Task Hierarchy — 4 níveis + phase gates) | Concluído. |
| Rule 26 (Audit Gate Lifecycle — 5 camadas) | Concluído. |
| Rule 27 (Zero-Bypass Lifecycle — 13 surfaces) | Concluído. |
| Rule 28 (Capability Frontmatter Contract — schema v3.0) | Concluído. |
| Rule 28 alt (Tool-Call Grammar — `[required]/[optional]/[conditional: ...]`) | Concluído. |
| Rule 29 (Refinement Gate) | Concluído. |
| Rule 30 (Value-Driven Templates v2) | Concluído. |
| Rule 31 (Documentation Freshness Gate) | Concluído. |
| Rule 32 (Dependency Policy Gate) | Concluído. |
| Rule 33 (AI Memory Production) | Concluído. |
| Rule 45 (CI-Watch Integrity — 8 exit codes) | Concluído. |

### §2.10. Layout, telemetria e governança

- **Layout v4** (`ai/epics/epic-XXXX-<slug>/`) consolidado.
- **Telemetria NDJSON** (5 hooks SessionStart/PreToolUse/PostToolUse/SubagentStop/Stop + `telemetry-phase.sh` markers + `TelemetryScrubber`) operacional, com `/x-analyze-telemetry` + `/x-analyze-telemetry-trends`.
- **Camada 0 — preflight gates** (`enforce-preflight-gates.sh` + `scripts/preflight.sh`) já bloqueia `git push`/`gh pr create`/`Skill x-pr-create` quando preflight falha.
- **Bypass único** documentado: `CLAUDE_RECOVERY_MODE=1`.

---

## §3. Hierarquia Canônica e Glossário

### §3.1. Hierarquia oficial

```text
Project → Product → Capability → Feature
                                    ↓
                       Architecture Plan (3 levels: product, capability, feature)
                                    ↓
                                  Epic → Story → Task
                                                  ↓
                                           Commit / PR / Release
```

### §3.2. Glossário

| Elemento | Papel |
| :--- | :--- |
| **Project** | Guarda-chuva estratégico/organizacional. Raiz que agrupa um ou mais Products. |
| **Product** | Unidade de valor com público, problema, proposta, métricas e restrições comerciais. |
| **Capability** | Capacidade de negócio/plataforma do Product. Conjunto coerente de funcionalidades que habilita valor recorrente. |
| **Feature** | Entrega concreta dentro de uma Capability. Menor unidade estratégica que justifica arquitetura, épico e stories. |
| **`architecture-product-*`** | Arquitetura macro do Product (canais, plataformas, padrões, restrições transversais). |
| **`architecture-capability-*`** | Arquitetura da Capability (subdomínios, fluxos, dados, eventos, APIs). |
| **`architecture-feature-*`** | Arquitetura sistêmica da Feature (componentes, dados, NFRs, mini-ADRs). |
| **Epic / Story / Task** | Pacote implementável + fatias verificáveis + unidades atômicas TDD. |
| **`artifact_kind`** | Tipo canônico de artefato persistido com schema, gerador autorizado e consumidores declarados. |
| **`policy_id`** | Regra executável versionada com testes e ponto de execução claro. |
| **`worker-prompt`** | Prompt versionado + JSON Schema de saída; LLM é invocado como worker. |
| **Camada A** | Runtime in-process do Forge (gates bloqueantes pré-side-effect). |
| **Camada B** | `forge ci verify` post-merge sobre evidência produzida. |

### §3.3. Predecessor remote gate (invariante)

Nenhum artefato filho pode ser criado se o predecessor não estiver **commitado e sincronizado no GitHub** com `status=APPROVED` e worktree limpa. Aplica-se em toda transição (Project→Product, Product→Capability, Capability→Feature, Feature→Architecture, Feature→Epic, Epic→Story, Story→Task).

---

## §4. Modelo Operacional do Runtime

### §4.1. Arquitetura conceitual

```text
forge CLI (Picocli)
   |
   v
Runtime determinístico
   - State Machine Engine
   - Phase Gate Engine
   - Policy Engine (refinement, integrity, zero-bypass, ...)
   - Worker Dispatcher (LLM as worker)
   - Artifact Registry tipado
   - Telemetry & Audit Log
   - Provider/router de LLM
   |
   +--> Outbound adapters (Git, GitHub, Build, Test, Doc, Format, Lint, ...)
   |
   +--> Worker prompts (LLM) com JSON Schema de saída validado
```

### §4.2. Storage (Git canônico + SQLite operacional + blob store)

| Camada | Papel |
| :--- | :--- |
| **Git** | Fonte de verdade, histórico, diff, recuperação. |
| **Filesystem (Markdown + YAML)** | Working tree editável; canônico. |
| **SQLite local** | `.forge/state/index.sqlite` reconstruível via `forge sync` ou `forge index rebuild`. FTS5 para busca textual. |
| **Blob store** | `.forge/blobs/sha256/<aa>/<sha>.zst` — comprimido + dedup por SHA256. |
| **Sync journal** | `.forge/state/sync-journal.ndjson` append-only para resume cross-machine. |

> **Princípio:** YAML decide identidade/estado/relações/NFRs/lineage; Markdown explica intenção/decisões/comportamento.

### §4.3. Bounded contexts

| Contexto | Responsabilidade |
| :--- | :--- |
| **Product Design** | Project, Product, Capability, Feature, approval, ideation. |
| **Architecture Design** | Architecture Plans (3 níveis), NFRs, mini-ADRs, readiness. |
| **Delivery Backlog** | Epic, Story, Task, implementation maps, decomposition. |
| **Delivery Orchestration** | State machines, phase gates, replay, recovery, lifecycle. |
| **Delivery Governance** | Policies, guidelines, gates, recovery approval, violations. |
| **Evidence Ledger** | `artifact_kind`, schemas, freshness, lineage, SQLite index, blob snapshots. |
| **AI Workers** | Prompts, model routing, structured output, retry/fallback, budgets. |
| **Source Control** | Git, branches, commits, PRs, CI status, remote checkpoints. |
| **Telemetry & Costs** | Audit events, traces, spans, metrics, cost events. |
| **Platform Composition** | Profiles, capabilities, targets, templates, plugins, compatibility. |
| **Migration** | Importação do `ia-dev-env`, dual-mode, drift, compatibility report. |

---

## §5. Catálogo de Products (P-1 a P8)

> Ordenação reflete dependências; numeração herda do roadmap (P-1 e P0 promovidos a Products explícitos antes do Strategic Chain).

| Product | Nome | Tese curta | Pré-requisitos |
| :---: | :--- | :--- | :--- |
| **P-1** | **Forge Foundations** | Substrato técnico antes de qualquer Strategic Chain: CLI binário, layout, storage, schemas, composition, telemetria, provider abstraction, adapters Git/GitHub/Build/Test/Doc. | — |
| **P0** | **Forge Spine — Runtime & Governance** | Miolo determinístico: State Machine + Phase Gates + Policy Engine + Worker Dispatcher + Refinement Gate + Execution Integrity. Tudo o que hoje vive em hooks shell e markdown vira código. | P-1 ≥ 80% |
| **P1** | **Strategic Chain** | Cadeia `Project → Product → Capability → Feature → Architecture → Epic` como artefatos versionados, aprovados e checkpointed remotamente. | P-1, P0 |
| **P2** | **Composition Engine & Multi-Target** | Profile/Capability composition v6 + target adapters (claude-code, cursor, windsurf, aider, codex-cli, gemini-cli, generic-mcp) + multi-stack/polyglot. | P-1.C04 |
| **P3** | **Orchestration Runtime** | Comandos públicos que substituem orquestradores skill-based: `forge story implement/refine/plan`, `forge epic implement/orchestrate/refine`, `forge task implement`, `forge release`, `forge merge-train`, `forge pr {create,merge,fix,watch}`, `forge test tdd`. | P0 |
| **P4** | **Quality, Reviews, Docs & Release** | Test gates condicionais (perf/mutation/contract/e2e/smoke) + reviews especialistas + documentation/changelog + security suite. | P3 |
| **P5** | **Developer Experience** | CLI v2 (`forge` unificado, `--output text/json/ndjson`, `forge repl`), TUI (`forge tui`, `forge watch`), Web local (`forge ui`), IDE extensions, Onboarding/`forge doctor`, **Squad Operating System** (intake, grooming, scoring, sprint, review queue, daily, métricas mistas humano/IA). | P3 |
| **P6** | **Knowledge & Marketplace** | Skill marketplace + Rule/Governance Library + Template/Profile Catalog + Cross-Project Intelligence + Plugins V0 (Jira, GitHub Apps, MCP). | P5 |
| **P7** | **Observability, Analytics & FinOps** | Telemetria local+streaming opt-in + dashboards live + quality/compliance metrics + FinOps (custo de LLM, downgrade suggestion, budgets) + research/benchmarking. | P0.C6 |
| **P8** | **Governance, Security & Trust avançado** | Audit & compliance engine + refinement v2 + threat modeling contínuo + privacy/multi-tenancy/RBAC/data residency. (Built-in básico já em P0/P-1.) | P7 |

---

## §6. Catálogo de Capabilities por Product

> Para cada Capability: ID, nome, escopo, dependências internas/cruzadas, **estado herdado do `ia-dev-env`**, indicador de Wave alvo. Use este catálogo como input direto de `forge capability create <PRODUCT-CODE>`.

### §6.1. P-1 — Forge Foundations

| ID | Nome | Escopo | Herdado de `ia-dev-env`? | Wave |
| :--- | :--- | :--- | :--- | :-: |
| P-1.C01 | **Project Bootstrap** | `forge` binário (Picocli + GraalVM ou JVM); `forge init` cria control repo (`projects/`, `ai/`, `.forge/`); detecção automática de stack; `forge doctor` health-check. | Parte de `x-setup-env`. | W0 |
| P-1.C02 | **Storage Primitives** | Git canônico + SQLite operational index + blob store comprimido (zstd) + atomic write/flock + sync journal append-only + `forge sync` + `forge index rebuild`. | Parcial (`flock` em `x-internal-status-update`). | W0 |
| P-1.C03 | **Identity, Schemas & Artifact Registry** | Frontmatter v4 (evolução do v3.0 herdado de EPIC-0064); registry tipado de `artifact_kind`; lineage graph DAG; registry linter; `forge explain <id>`. | **Sim — frontmatter v3.0 (Rule 28) já é base.** | W0 |
| P-1.C04 | **Profile & Capability Composition** | Profile schema v3 com herança/overlays; CapabilityResolver/Composer/OutputPruner; **Template Registry tipado** (template_id + JSON Schemas de input/output); KP Loader on-demand; `forge migrate --from-iadev`. | **Sim — `CapabilityResolver`/`Composer`/`OutputPruner` reutilizáveis (EPIC-0064).** Falta Template Registry strict. | W0 |
| P-1.C05 | **Output, Telemetry & Audit Log** | `--output text/json/ndjson` com schema por command; in-process telemetry NDJSON (substitui 8 hooks shell); OTel adapter opcional; audit log local imutável (`.forge/audit/audit-log.ndjson` com hash chain SHA-256); session resume. | Parcial (telemetria via hooks; `TelemetryScrubber` já existe). | W0 |
| P-1.C06 | **LLM Provider Abstraction** | `LlmProvider` interface (`complete()`, `stream()`, `tool_use()`, `function_call()`); adapters Anthropic / OpenAI / Gemini / Local (Ollama/llama.cpp); model router (Haiku/Sonnet/Opus + custom); fallback automático; output schema validation; cost tracker. | Não. (Rule 23 hospeda doutrina de model selection.) | W0 |
| P-1.C07 | **Git & GitHub Adapters** | `GitAdapter.{branch,commit,push,merge,worktree,cleanup,precheck,epicBranch}()`; `GithubAdapter.{prCreate,prMerge,prWatchCi,prComments}()`. | Parcial (skills shell-based hoje em `core/git`/`core/pr`/`core/internal/git`/`core/internal/pr`). | W0 |
| P-1.C08 | **Build, Test & Doc Adapters** | `BuildAdapter` por stack (maven/gradle/npm/pip/poetry/go/cargo); `TestAdapter.run()`; `FormatAdapter`; `LintAdapter`; `DocAdapter` (OpenAPI/asyncapi/gRPC/README); stack catalog. | Parcial (templates `.sh.tpl` por stack). | W0 |

### §6.2. P0 — Forge Spine

| ID | Nome | Escopo | Herdado de `ia-dev-env`? | Wave |
| :--- | :--- | :--- | :--- | :-: |
| P0.C1 | **State Machine Engine** | Enums `Project/Product/Capability/Feature/ArchitecturePlan/Epic/Story/Task/PR/Run`; transition validator com pré-condições e evidências; persistência YAML canônica + SQLite cache; resume engine + `forge {pause,resume}`. | Parcial (`execution-state.json` e `flowVersion=5`/`productFirstLifecycle=true` em EPIC-0077). | W3 |
| P0.C2 | **Phase Gate Engine** | `PhaseGateService` 4 modos (pre/post/wave/final); pre-conditions (predecessor + child tasks + artifacts); wave gates; final gate composto. | **Sim — `x-internal-verify-phase-gates` + Rule 25 + audit-task-hierarchy/phase-gates.** Migra para Java. | W3 |
| P0.C3 | **Policy Engine** | `Policy` interface (`policy_id`, `version`, `enforcement_point`, `evidence_kind`, `test()`); registry; enforcement matrix; testes obrigatórios; **migração de Rules 14/21/22/24/27/28a/28b/29/31/32/33/45/11 para policies executáveis**. | Parcial (regras já são markdown estruturado; falta engine genérico). | W3 |
| P0.C4 | **Worker Dispatcher** | Prompt template registry (`prompt_id`, `version`, JSON Schema de saída); routing por capability via Model Router de C06; output validation + retry tipado; cost attribution; fan-out/fan-in declarativo. | Parcial (workers de planning hoje vivem como skills/markdown). | W3 |
| P0.C5 | **Refinement Gate (built-in)** | Pré-condição nativa de `forge story implement` / `forge epic implement` / `forge task implement` / `forge epic orchestrate`; `refinementVerdict.status + verdictHash`; hash divergence detection; recovery escape via `CLAUDE_RECOVERY_MODE=1`. | **Sim — Rule 29 + EPIC-0069.** Migra para nativo. | W3 |
| P0.C6 | **Execution Integrity (built-in)** | Camada A (runtime gate) + Camada B (`forge ci verify`); zero-bypass arquitetural (sem `--skip-*` no caminho oficial); continuous flow; Evidence Ledger; **14 surfaces** (13 da Rule 27 + 1 nova: `architecture-product/capability/feature` aprovado). | **Sim — Rules 24/27 + EPIC-0059 + EPIC-0063.** Migra para nativo. | W3 |

### §6.3. P1 — Strategic Chain

| ID | Nome | Escopo | Herdado de `ia-dev-env`? | Wave |
| :--- | :--- | :--- | :--- | :-: |
| P1.C1 | **Project & Product Lifecycle** | `forge project create/approve` (NOVO no Forge); `forge product create/approve`; `forge product propose-capabilities` (worker que sugere candidatas). | Parcial (templates `_TEMPLATE-PRODUCT.md` em EPIC-0077). | W10 |
| P1.C2 | **Capability & Feature Lifecycle** | `forge capability create/approve`; `forge feature create/approve`; predecessor remote gate; `forge ideate --kind product/capability/feature` (multi-round, personas, comparação de alternativas). | Parcial (`x-feature-create`, `x-feature-ideate` já existem; falta capability/product). | W10 |
| P1.C3 | **Architecture Planning (3 níveis)** | `forge arch plan product/capability/feature`; coleta de NFRs mínimos (usuários, simultaneidade, latência, disponibilidade, volume, segurança); architecture decision log; **Feature → Epic gate** (`architecture-feature` aprovado + remote-clean); `forge arch system update` incremental. | **Sim — `x-arch-plan-product` + `x-arch-plan-capability` em EPIC-0077; `x-arch-plan` (feature), `x-arch-update`, `x-arch-system-update` já existem; C4 obrigatório.** | W10 |
| P1.C4 | **Feature → Epic Generation** | `forge epic create <FEATURE>` gera epic + stories + IMPLEMENTATION-MAP a partir da feature e do `architecture-feature` aprovado; bidirectional linking; backlog versioning (commit/PR); replanejamento incremental quando arquitetura/feature mudam; **bug/change → change-epic correction-story** (Post-Delivery); effort scoring misto humano/IA. | Parcial (`x-internal-create-epic`/`x-internal-create-story`/`x-internal-map-epic` já existem). | W10 |
| P1.C5 | **Post-Delivery Lifecycle** | Taxonomia tipada de entradas pós-entrega: `Bug`, `FeatureChange`, `FeatureDeprecation`, `FeatureRemoval`, `SecurityFinding`, `SpecDrift`, `Maintenance`, `DependencyUpgrade`, `Rollback`, `Experiment`, `SupportRequest`. State machines, lineage, impact assessment, comandos `forge bug/feature change/feature deprecate/feature remove/security finding/spec drift/maintenance/dependency upgrade/rollback/experiment/support`. | Não. (V3 §9 inteira é nova.) | W10/W11 |

### §6.4. P2 — Composition Engine & Multi-Target

| ID | Nome | Escopo | Herdado de `ia-dev-env`? | Wave |
| :--- | :--- | :--- | :--- | :-: |
| P2.C1 | **Profile Management v6** | Schema unificado JSON-Schema versionado; `forge migrate --from-iadev`; inheritance & overlays; detecção automática de stack. | Parcial (ProjectConfig Java reutilizável). | W4 |
| P2.C2 | **Capability Composition v2** | Resolver com cache local; **Frontmatter v4 (com `provides:`)**; plug-in capabilities externas; composition diff & dry-run. | **Sim — base v3.0 (Rule 28).** | W4 |
| P2.C3 | **Multi-Target Adapters** | `claude-code` (default), `cursor`, `windsurf`, `aider`, `gemini-cli`, `codex-cli`, `generic-mcp`; overlay system para customizações sem perder regen. | Não (hoje só `claude-code`). | W4 |
| P2.C4 | **Multi-Stack & Polyglot** | Catálogo oficial de stacks; stack templates community-contributed; polyglot monorepos; reutilização de KPs via taxonomia comum. | Parcial. | W4 |

### §6.5. P3 — Orchestration Runtime

| ID | Nome | Escopo | Herdado de `ia-dev-env`? | Wave |
| :--- | :--- | :--- | :--- | :-: |
| P3.C1 | **Story / Epic / Task Implementation** | `forge story implement`, `forge epic implement`, `forge task implement`, `forge story refine`, `forge epic refine`, `forge story plan`, `forge task plan`, `forge test tdd`. | **Sim — `x-story-implement`, `x-epic-implement`, `x-task-implement`, `x-story-refine`, `x-epic-refine`, `x-story-plan`, `x-task-plan`, `x-test-tdd-drive` (?ex-`x-drive-tdd`).** Migra para Java. | W5 |
| P3.C2 | **Release & Merge Train** | `forge release [--patch/--minor/--major]`, `forge merge-train`, `forge epic orchestrate`. | **Sim — `x-release`, `x-pr-merge-train` (?ex-`x-manage-pr-merge-train`), `x-epic-orchestrate`.** Migra para Java. | W5 |
| P3.C3 | **PR Lifecycle Commands** | `forge pr create`, `forge pr merge`, `forge pr fix`, `forge pr fix --epic`, `forge pr watch`. | **Sim — `x-pr-create`, `x-pr-merge`, `x-pr-fix`, `x-pr-fix-epic` (?), `x-pr-ci-watch`.** Migra para Java. | W2 |
| P3.C4 | **Reliability & Replay** | Determinismo controlado; snapshot de contexto local; idempotência por comando/skill; file locking local. | Parcial (parte vem de P-1.C02). | W5 |

### §6.6. P4 — Quality, Reviews, Docs & Release

| ID | Nome | Escopo | Herdado de `ia-dev-env`? | Wave |
| :--- | :--- | :--- | :--- | :-: |
| P4.C1 | **Quality Gates** | `forge test {run,plan,e2e,contract,mutation,performance,regression-shell,smoke api/socket}`; `forge code {format,lint,audit}`. | **Sim — todos os 9 conditional/test + core/test + core/code já existem como skills.** Migra para adapters/commands. | W6 |
| P4.C2 | **Review Workers** | `forge review <STORY>` (parallel dispatcher) + `forge review pr <PR>` (Tech Lead) + 14 specialists (api, compliance, data-modeling, database, devops, events, gateway, graphql, grpc, observability, security, performance, qa, codebase). | **Sim — `x-codebase-review`, `x-pr-review`, e os 11 conditional/review + 3 core/review.** Migra para command + workers. | W7 |
| P4.C3 | **Documentation** | `forge doc {generate,validate}`, `forge adr generate`, `forge release changelog`, `forge arch update`, `forge arch system update`. | **Sim — todos já existem.** Migra. | W6 |
| P4.C4 | **Security Suite** | `forge security {owasp,dependency,supply-chain,dashboard,pipeline,hardening,runtime,pentest,dep-policy validate,container,dast,infra,sast,secrets,sonar,threat-model}`. | **Sim — todos os 8 core/security + 8 conditional/security + `x-threat-model` (?ex-`x-model-threats`).** Migra. | W8 |

### §6.7. P5 — Developer Experience

| ID | Nome | Escopo | Herdado de `ia-dev-env`? | Wave |
| :--- | :--- | :--- | :--- | :-: |
| P5.C1 | **CLI v2** | `forge` unificado com `--output text/json/ndjson`; `forge repl`; `forge migrate`; `forge init` (em P-1.C01). | Parcial. | W0 + progressivo |
| P5.C2 | **TUI & Local UI** | `forge tui`; `forge watch`; `forge ui` (web console local); editor visual de rules/skills. | Não. | W11 |
| P5.C3 | **IDE Extensions** | Extensão VS Code; extensão JetBrains; painel inline de evidências; auto-complete profile/capabilities. | Não. | W11 |
| P5.C4 | **Onboarding & Time-to-Value** | `forge init` 5-7 perguntas; templates por persona; tutorial guiado in-IDE; `forge doctor`. | Parcial. | W0/W11 |
| P5.C5 | **Squad Operating System** | `forge intake/triage/route/close`; `forge backlog {groom,score,explain-score,reorder}`; `forge item ready-check/ask-missing`; `forge sprint {plan,start,rebalance,close}`; `forge review {queue,assign,nudge,load}`; `forge ai {plan,run,budget forecast}`; `forge {daily,status report,stakeholder update,squad health,release readiness,release risk}`; `forge incident {open,mitigate,postmortem,create-actions}`; `forge metrics {flow,quality,ai}`; modelo de **esforço misto humano/IA** com 5 dimensões (`human/ai/supervision/review/risk`). | Não. (V3 §10 inteira é nova.) | W11 |

### §6.8. P6 — Knowledge & Marketplace

| ID | Nome | Escopo | Herdado de `ia-dev-env`? | Wave |
| :--- | :--- | :--- | :--- | :-: |
| P6.C1 | **Skill Marketplace** | Registry central / self-hosted; SemVer; dependency resolution; trust model (signing + sandbox + SBOM); compatibility matrix por modelo/provider. | Não. | W11 |
| P6.C2 | **Rule & Governance Library** | Rule packs por domínio; rule simulator; rule conflict detector; custom rule authoring. | Não. | W11 |
| P6.C3 | **Template & Profile Catalog** | Catálogo profiles oficial/community; rating + usage stats; `forge profile fork`; profile lineage. | Não. | W11 |
| P6.C4 | **Cross-Project Intelligence** | Padrões agregados anonimizados; recommendation engine; drift detection cross-repo; knowledge graph navegável. | Não. | W11 |
| P6.C5 | **Plugins V0** | `forge jira create-epic`, `forge jira create-stories`, `forge mcp recommend`, MCP marketplace cache. | Parcial (Jira skills + `x-mcp-recommend` (?ex-`x-recommend-mcp`)). | W11 |

### §6.9. P7 — Observability, Analytics & FinOps

| ID | Nome | Escopo | Herdado de `ia-dev-env`? | Wave |
| :--- | :--- | :--- | :--- | :-: |
| P7.C1 | **Local & Real-Time Telemetry** | Captura local NDJSON + queries CLI; streaming opt-in OTLP/Datadog/HTTP custom; dashboard live; alerting; trace OTel-compatible. | Parcial (NDJSON local + `x-telemetry-analyze`/`x-telemetry-trend`). | W11 |
| P7.C2 | **Quality & Compliance Metrics** | Coverage longitudinal; refinement quality score; doc freshness heatmap; compliance posture report. | Parcial. | W11 |
| P7.C3 | **FinOps & Cost Insights** | Custo de LLM por skill/story/epic/org; sugestão de model downgrade; budget guardrails locais; comparativo por provider. | Não. | W11 |
| P7.C4 | **Research & Benchmarking** | A/B testing de skills; benchmark suite; regression detection; public leaderboard opt-in. | Não. | W11 |

### §6.10. P8 — Governance, Security & Trust avançado

| ID | Nome | Escopo | Herdado de `ia-dev-env`? | Wave |
| :--- | :--- | :--- | :--- | :-: |
| P8.C1 | **Audit & Compliance Engine** | Audit log local imutável (em P-1.C05); evidence vault; reports SOC2/ISO 27001/LGPD; forensics; **`forge ci verify` em P0.C6**. | Parcial. | W11 |
| P8.C2 | **Refinement & Quality Gates v2** | AI-assisted refinement; refinement memory; refinement templates por domínio; NO-GO library. | Parcial (Rule 29 base). | W11 |
| P8.C3 | **Security Posture & Threat Modeling** | Continuous threat modeling; SBOM gerado e validado; secret scanning integrado; supply chain trust score. | Parcial (`x-threat-model` + dep audit). | W11 |
| P8.C4 | **Privacy, Multi-Tenancy & RBAC** | Multi-tenant; RBAC; data residency; PII scrubbing para telemetria remota. | Parcial (`TelemetryScrubber`). | W11 |

---

## §7. Catálogo de Features V0

> Sintaxe: `P{N}.C{M}.F{K}` — todas marcadas `[V0]`. Use como input direto de `forge feature create <CAPABILITY-CODE>`.
> "Herdado" indica como o `ia-dev-env` atual já cobre parcialmente — usar como referência ao escrever a feature.

### §7.1. P-1 Foundations — Features

#### P-1.C01 — Project Bootstrap

- **F1 `[V0]`** — `forge` CLI binário (Picocli + GraalVM ou JVM com `--enable-preview`) com subcomandos `init`, `doctor`, `sync`, `index`, `migrate`, `version`. **Herdado:** parcial (Picocli já é stack).
- **F2 `[V0]`** — `forge init` cria control repository (`projects/`, `ai/`, `.forge/`), profile YAML inicial, `.gitignore`, README, detecta stack. **Herdado:** parte de `x-setup-env`.
- **F3 `[V0]`** — Layout canônico `projects/project-XXXX/products/.../capabilities/.../features/.../epics/...`. **Herdado:** layout v4 atual em `ai/epics/` é precursor; novo layout é alinhado à hierarquia Product-First.
- **F4 `[V0]`** — Detecção automática de stack (pom.xml/package.json/go.mod/requirements.txt/Cargo.toml). **Herdado:** parcial.
- **F5 `[V0]`** — `forge doctor` health-check (git, gh, jq, build tool, profile válido, control repo íntegro). **Herdado:** substitui `x-setup-env`.

#### P-1.C02 — Storage Primitives

- **F1 `[V0]`** — Git canonical (commits semânticos em eventos de lifecycle).
- **F2 `[V0]`** — SQLite operational index reconstruível (`.forge/state/index.sqlite` com FTS5).
- **F3 `[V0]`** — Blob store local comprimido/dedup (`.forge/blobs/sha256/<aa>/<sha>.zst`).
- **F4 `[V0]`** — `forge sync` (reindex incremental do working tree).
- **F5 `[V0]`** — `forge index rebuild` (recriação completa do índice — recovery).
- **F6 `[V0]`** — Atomic write + flock substitutos do flock shell de `x-internal-status-update`.
- **F7 `[V0]`** — Sync journal append-only (`.forge/state/sync-journal.ndjson`) para resume cross-machine.

#### P-1.C03 — Identity, Schemas & Artifact Registry

- **F1 `[V0]`** — **Frontmatter v4** (evolução do v3.0 herdado de Rule 28) com `artifact_kind`, `schema_version`, `id`, `slug`, `title`, `status`, `parent`, `lineage`, `body_contract`, `llm_context`, `remote_checkpoint`, **`provides:`**.
- **F2 `[V0]`** — Artifact Registry tipado (catálogo de `forge.{product,capability,feature,epic,story,task,architecture-plan,adr,review,verify-envelope,telemetry-event,...}`).
- **F3 `[V0]`** — Registry linter (schemas, ids, lineage, broken refs, duplicate slugs). **Herdado:** `audit-frontmatter-schema.sh`, `audit-capability-graph.sh`.
- **F4 `[V0]`** — Lineage graph DAG navegável (Product → Capability → Feature → Architecture → Epic → Story → Task → Commit → PR).
- **F5 `[V0]`** — `forge explain <id>` (render de artefato com lineage, dependências e consumidores).

#### P-1.C04 — Profile & Capability Composition

- **F1 `[V0]`** — Profile schema v3 com herança e overlays. **Herdado:** ProjectConfig Java.
- **F2 `[V0]`** — CapabilityResolver evoluído (cache local, plug-in externo). **Herdado:** sim.
- **F3 `[V0]`** — Composition diff & dry-run.
- **F4 `[V0]`** — **Template Registry strict** (`template_id`, `schema_version`, JSON Schema input/output, consumidores declarados). **Herdado:** templates já existem; faltam schemas estrictos.
- **F5 `[V0]`** — KP Loader on-demand por fase/worker.
- **F6 `[V0]`** — `forge migrate --from-iadev` (migração assistida v5 → v6 do profile).

#### P-1.C05 — Output, Telemetry & Audit Log

- **F1 `[V0]`** — Output abstraction `--output text/json/ndjson` com schema declarado por command.
- **F2 `[V0]`** — In-process telemetry NDJSON (substitui 8 hooks shell). **Herdado:** events.ndjson + `TelemetryScrubber`.
- **F3 `[V0]`** — OTel adapter para OTLP/Datadog/custom HTTP (opt-in).
- **F4 `[V0]`** — Audit log local imutável (`.forge/audit/audit-log.ndjson` com hash chain SHA-256). **Herdado:** `audit-ndjson-hash-chain.sh`.
- **F5 `[V0]`** — Telemetry queries (`forge telemetry analyze`, `forge telemetry trend`). **Herdado:** sim, migra para command.
- **F6 `[V0]`** — Session resume baseado em events.ndjson.

#### P-1.C06 — LLM Provider Abstraction

- **F1 `[V0]`** — `LlmProvider` interface (`complete()`, `stream()`, `tool_use()`, `function_call()`).
- **F2 `[V0]`** — Anthropic adapter (Claude 3.5/4 — Haiku/Sonnet/Opus).
- **F3 `[V0]`** — OpenAI adapter (GPT-4/5 series).
- **F4 `[V0]`** — Gemini adapter (Pro/Flash).
- **F5 `[V0]`** — Local provider (Ollama / llama.cpp).
- **F6 `[V0]`** — Model router dinâmico (capability-aware: Haiku/Sonnet/Opus + custom). **Herdado:** Rule 23 base.
- **F7 `[V0]`** — Fallback automático provider B se A falha.
- **F8 `[V0]`** — Cost tracker (custo por skill/story/epic/org, real-time).
- **F9 `[V0]`** — Output schema validation (worker outputs validados contra JSON Schema).

#### P-1.C07 — Git & GitHub Adapters

- **F1 `[V0]`** — `GitAdapter.branch()` (idempotente, naming validation). **Herdado:** `x-git-branch-create` (?ex-`x-create-git-branch`).
- **F2 `[V0]`** — `GitAdapter.commit()` (Conventional Commits + pre-commit chain format→lint→compile). **Herdado:** `x-git-commit` (?ex-`x-commit-changes`).
- **F3 `[V0]`** — `GitAdapter.push()` (idempotente + retry tipado). **Herdado:** `x-git-push` (?).
- **F4 `[V0]`** — `GitAdapter.merge()` (strategy + rollback automático em conflito). **Herdado:** `x-git-merge` (?ex-`x-merge-branches`).
- **F5 `[V0]`** — `GitAdapter.worktree()` (lifecycle Rule 14 sob `.forge/worktrees/`). **Herdado:** `x-git-worktree-manage` (?ex-`x-manage-worktrees`).
- **F6 `[V0]`** — `GitAdapter.cleanup()` (prune origin + remove worktrees + branches obsoletas).
- **F7 `[V0]`** — `GitAdapter.precheck()` (CLEAN/DIRTY/DIVERGENT/AMBIGUOUS).
- **F8 `[V0]`** — `GitAdapter.epicBranch()` (idempotência da convenção `epic/<ID>` — Rule 21).
- **F9 `[V0]`** — `GithubAdapter.prCreate()` (title formatado, labels, body estruturado).
- **F10 `[V0]`** — `GithubAdapter.prMerge()` (gh CLI + strategy + idempotência).
- **F11 `[V0]`** — `GithubAdapter.prWatchCi()` (8 exit codes — Rule 45).
- **F12 `[V0]`** — `GithubAdapter.prComments()` (fetch + classify actionable/suggestion/question/praise).

#### P-1.C08 — Build, Test & Doc Adapters

- **F1 `[V0]`** — `BuildAdapter` por stack (maven/gradle/npm/pip/poetry/go/cargo).
- **F2 `[V0]`** — `TestAdapter.run()` com coverage filtering.
- **F3 `[V0]`** — `FormatAdapter` (Spotless/prettier/black/gofmt).
- **F4 `[V0]`** — `LintAdapter` (Checkstyle/eslint/ruff/golangci-lint).
- **F5 `[V0]`** — `DocAdapter` (OpenAPI lint, asyncapi, gRPC proto, README freshness).
- **F6 `[V0]`** — Stack catalog + `forge mcp recommend` (?ex-`x-recommend-mcp`).

### §7.2. P0 Spine — Features

#### P0.C1 — State Machine Engine

- **F1 `[V0]`** — Entity state enums (10 entidades — §5.3 v3).
- **F2 `[V0]`** — Transition validator (cada transição declara command autorizado, pré-condições, evidências).
- **F3 `[V0]`** — State persistence (frontmatter YAML canônico + projeção SQLite cache).
- **F4 `[V0]`** — Resume engine cross-machine via sync journal.
- **F5 `[V0]`** — `forge pause` / `forge resume` em qualquer command de longa duração.

#### P0.C2 — Phase Gate Engine

- **F1 `[V0]`** — `PhaseGateService` 4 modos (pre/post/wave/final). **Herdado:** `x-internal-verify-phase-gates`.
- **F2 `[V0]`** — Pre-conditions (predecessor phase completed + child tasks completed + artifacts on disk).
- **F3 `[V0]`** — Wave gates (paralelismo seguro por onda). **Herdado:** `audit-wave-dispatch.sh`.
- **F4 `[V0]`** — Final gate composição com `EpicIntegrityGate`. **Herdado:** `x-internal-verify-epic-integrity`.
- **F5 `[V0]`** — Hospedagem da Rule 25 (task hierarchy + phase gate contract) como policy.

#### P0.C3 — Policy Engine

- **F1 `[V0]`** — `Policy` interface (`policy_id`, `version`, `enforcement_point`, `evidence_kind`, `test()`).
- **F2 `[V0]`** — Policy registry (catálogo versionado).
- **F3 `[V0]`** — Enforcement matrix (policy_id → runtime/CI/doctrine).
- **F4 `[V0]`** — Policy tests obrigatórios (suite por policy_id).
- **F5 `[V0]`** — **Migração de Rules para policies executáveis**: 14 (project scope), 21 (epic branch), 22 (skill visibility), 24 (execution integrity), 27 (zero-bypass), 28a (capability frontmatter), 28b (tool-call grammar), 29 (refinement gate), 31 (doc freshness), 32 (dependency policy), 33 (AI memory), 45 (CI-watch), 11 (PCI), 25 (task hierarchy), 09 (branching).

#### P0.C4 — Worker Dispatcher

- **F1 `[V0]`** — Prompt template registry (`prompt_id`, `version`, system/user blocks, JSON Schema output).
- **F2 `[V0]`** — Provider routing por capability (haiku/sonnet/opus). **Herdado:** Rule 23.
- **F3 `[V0]`** — Output validation contra schema; retry tipado.
- **F4 `[V0]`** — Cost attribution (cada chamada anota custo no audit log).
- **F5 `[V0]`** — Fan-out/fan-in declarativo (dispatch paralelo de N workers + agregação tipada). **Herdado:** padrão de `x-internal-build-story-plan` (5 sub-skills paralelos).

#### P0.C5 — Refinement Gate (built-in)

- **F1 `[V0]`** — Pré-condição nativa em `forge {story,epic,task} implement` e `forge epic orchestrate`. **Herdado:** Rule 29.
- **F2 `[V0]`** — Refinement verdict schema (`refinementVerdict.{status,scope,checkedAt,verdictHash,dimensions,blockers}`).
- **F3 `[V0]`** — Hash divergence detection (state ↔ markdown).
- **F4 `[V0]`** — Recovery escape via `CLAUDE_RECOVERY_MODE=1` com WARNING.
- **F5 `[V0]`** — Multi-persona dispatcher (PO + Tech Lead + Architect + Security + QA + perf/SRE conditional). **Herdado:** `x-story-refine` / `x-epic-refine`.

#### P0.C6 — Execution Integrity (built-in)

- **F1 `[V0]`** — Camada A (runtime gate in-process).
- **F2 `[V0]`** — Camada B (`forge ci verify`).
- **F3 `[V0]`** — Zero-bypass arquitetural (sem flag `--skip-*` no caminho oficial).
- **F4 `[V0]`** — Continuous flow monitor (Stop quando fase aberta sem progresso). **Herdado:** `enforce-continuous-flow.sh`.
- **F5 `[V0]`** — Evidence Ledger (catálogo tipado).
- **F6 `[V0]`** — **14 surfaces enforcement** (13 da Rule 27 + 1 nova: `architecture-product/capability/feature` aprovado).
- **F7 `[V0]`** — `forge ci verify` modular (subcomandos por capability) **vs** monolítico — ver §13 Open Question.

### §7.3. P1 Strategic Chain — Features

> Esta é a **camada que o EPIC-0077 do `ia-dev-env` atualmente está construindo**. Todas as features V0 do Forge nesta capability são **evoluções diretas** do que está sendo entregue agora.

#### P1.C1 — Project & Product Lifecycle

- **F1 `[V0]`** — `forge project create/approve` (NOVO no Forge — `Project` é entidade explícita).
- **F2 `[V0]`** — `forge product create/approve` (com `_TEMPLATE-PRODUCT.md` 8 seções, RNFs Root). **Herdado:** template em EPIC-0077.
- **F3 `[V0]`** — `forge product propose-capabilities` (worker que sugere candidatas a partir de Product aprovado).
- **F4 `[V0]`** — Predecessor remote gate (Project aprovado + commitado + sincronizado).

#### P1.C2 — Capability & Feature Lifecycle

- **F1 `[V0]`** — `forge capability create/approve` (com `_TEMPLATE-CAPABILITY.md` 7 seções, RNF no-relax). **Herdado:** template em EPIC-0077.
- **F2 `[V0]`** — `forge feature create/approve`. **Herdado:** `x-feature-create`.
- **F3 `[V0]`** — Predecessor remote gate (Capability aprovada + commitada + sincronizada).
- **F4 `[V0]`** — `forge ideate --kind product/capability/feature` (multi-round, personas avançadas, comparação de alternativas). **Herdado:** `x-feature-ideate` parcial.
- **F5 `[V0]`** — RNF inheritance gate (no-relax). **Herdado:** `x-internal-rnf-validate` em EPIC-0077.

#### P1.C3 — Architecture Planning (3 níveis)

- **F1 `[V0]`** — `forge arch plan product <PRODUCT-CODE>`. **Herdado:** `x-arch-plan-product` em EPIC-0077.
- **F2 `[V0]`** — `forge arch plan capability <CAPABILITY-CODE>`. **Herdado:** `x-arch-plan-capability` em EPIC-0077.
- **F3 `[V0]`** — `forge arch plan feature <FEATURE-CODE>`. **Herdado:** `x-arch-plan` (?ex-`x-plan-architecture`).
- **F4 `[V0]`** — Coleta obrigatória de NFRs mínimos (usuários, simultaneidade, latência, disponibilidade, volume, segurança).
- **F5 `[V0]`** — **C4 model obrigatório** (C1 System Context + C2 Container + C3 Component + C4 Code) em planning artifacts. **Herdado:** EPIC-0077.
- **F6 `[V0]`** — Architecture decision log + mini-ADRs por nível.
- **F7 `[V0]`** — Feature → Epic gate (`architecture-feature` aprovado + remote-clean).
- **F8 `[V0]`** — `forge arch system update` (incremental, idempotente). **Herdado:** `x-arch-system-update`.

#### P1.C4 — Feature → Epic Generation

- **F1 `[V0]`** — `forge epic create <FEATURE-CODE>` gera epic + stories + IMPLEMENTATION-MAP. **Herdado:** `x-internal-create-epic` + `x-internal-create-story` + `x-internal-map-epic`.
- **F2 `[V0]`** — Bidirectional linking (Feature ↔ Architecture ↔ Epic ↔ Stories).
- **F3 `[V0]`** — Backlog versioning (commit/PR para artefatos gerados).
- **F4 `[V0]`** — Replanejamento incremental quando arquitetura/feature mudam (descendentes ficam `STALE`).
- **F5 `[V0]`** — Change epic + correction story gerados a partir de bug/change aprovados (preservando lineage).
- **F6 `[V0]`** — Regra "no isolated behavior task" (alteração comportamental nunca como task solta).
- **F7 `[V0]`** — Effort scoring com esforço misto humano/IA (5 dimensões).

#### P1.C5 — Post-Delivery Lifecycle

- **F1 `[V0]`** — `forge bug {open,triage,fix}` (vínculo obrigatório a feature; severidade tipada).
- **F2 `[V0]`** — `forge feature change {open,assess,approve,implement}` (mudança comportamental aprovada como revision/change-epic/nova-feature).
- **F3 `[V0]`** — `forge feature deprecate` (plano de depreciação + comunicação + migration story + rollback).
- **F4 `[V0]`** — `forge feature remove` (removal epic + breaking-change note + cleanup stories).
- **F5 `[V0]`** — `forge security finding {open,triage,fix,accept-risk}` (SLA + confidencialidade).
- **F6 `[V0]`** — `forge spec drift {detect,classify,resolve}` (divergência docs/contracts/code/backlog).
- **F7 `[V0]`** — `forge maintenance {open,plan,implement}` (refactor/dívida sem mudança comportamental).
- **F8 `[V0]`** — `forge dependency upgrade {open,assess,implement}` (upgrade governado de stack/provider/plugin).
- **F9 `[V0]`** — `forge rollback {request,execute,reconcile}` (com reconciliação de estado e evidência).
- **F10 `[V0]`** — `forge experiment {open,start,evaluate,promote,rollback}` (feature flag + métricas + guardrails).
- **F11 `[V0]`** — `forge support {intake,triage}` (classificação antes do backlog).
- **F12 `[V0]`** — `forge impact assess` (serviço comum de stale propagation e lineage).

### §7.4. P2 Composition — Features

#### P2.C1 — Profile Management v6

- **F1 `[V0]`** — Schema unificado JSON-Schema versionado.
- **F2 `[V0]`** — `forge migrate --from-iadev`.
- **F3 `[V0]`** — Inheritance & overlays.
- **F4 `[V0]`** — Detecção automática de stack.

#### P2.C2 — Capability Composition v2

- **F1 `[V0]`** — Resolver com cache local. **Herdado:** sim.
- **F2 `[V0]`** — Frontmatter v4 com `provides:`. **Herdado:** v3.0 base.
- **F3 `[V0]`** — Plug-in capabilities externas.
- **F4 `[V0]`** — Composition diff & dry-run.

#### P2.C3 — Multi-Target Adapters

- **F1 `[V0]`** — Target adapter `claude-code` (default, atual).
- **F2 `[V0]`** — Target adapter `cursor`.
- **F3 `[V0]`** — Targets `windsurf`, `aider`, `gemini-cli`, `codex-cli`.
- **F4 `[V0]`** — Target adapter `generic-mcp`.
- **F5 `[V0]`** — Overlay system para customizações sem perder regen.

#### P2.C4 — Multi-Stack & Polyglot

- **F1 `[V0]`** — Catálogo de stacks oficial.
- **F2 `[V0]`** — Stack templates community-contributed.
- **F3 `[V0]`** — Polyglot monorepos.
- **F4 `[V0]`** — Reutilização de KPs via taxonomia comum.

### §7.5. P3 Orchestration — Features

#### P3.C1 — Story / Epic / Task Implementation

- **F1 `[V0]`** — `forge story implement <ID>`. **Herdado:** `x-story-implement`.
- **F2 `[V0]`** — `forge epic implement <ID>`. **Herdado:** `x-epic-implement`.
- **F3 `[V0]`** — `forge task implement <ID>`. **Herdado:** `x-task-implement`.
- **F4 `[V0]`** — `forge story refine <ID>` / `forge epic refine <ID>`. **Herdado:** `x-story-refine` / `x-epic-refine`.
- **F5 `[V0]`** — `forge story plan <ID>` / `forge task plan <ID>`. **Herdado:** `x-story-plan` / `x-task-plan`.
- **F6 `[V0]`** — `forge test tdd <TASK>` (RED/GREEN/REFACTOR cycles). **Herdado:** `x-test-tdd-drive` (?ex-`x-drive-tdd`).
- **F7 `[V0]`** — `forge epic orchestrate <ID>`. **Herdado:** `x-epic-orchestrate`.

#### P3.C2 — Release & Merge Train

- **F1 `[V0]`** — `forge release [--patch/--minor/--major]`. **Herdado:** `x-release`.
- **F2 `[V0]`** — `forge merge-train`. **Herdado:** `x-pr-merge-train` (?ex-`x-manage-pr-merge-train`).
- **F3 `[V0]`** — `forge feature create` / `forge feature ideate` (já em P1.C2, listado aqui para visibilidade).

#### P3.C3 — PR Lifecycle Commands

- **F1 `[V0]`** — `forge pr create`. **Herdado:** `x-pr-create`.
- **F2 `[V0]`** — `forge pr fix <PR>` / `forge pr fix --epic <EPIC>`. **Herdado:** `x-pr-fix` / `x-pr-fix-epic` (?).
- **F3 `[V0]`** — `forge pr merge`. **Herdado:** `x-pr-merge`.
- **F4 `[V0]`** — `forge pr watch`. **Herdado:** `x-pr-ci-watch`.

#### P3.C4 — Reliability & Replay

- **F1 `[V0]`** — Determinismo controlado.
- **F2 `[V0]`** — Snapshot de contexto local.
- **F3 `[V0]`** — Idempotência por comando/skill.
- **F4 `[V0]`** — File locking local (já em P-1.C02).

### §7.6. P4 Quality, Reviews, Docs & Release — Features

#### P4.C1 — Quality Gates

- **F1 `[V0]`** — `forge test run` (coverage filtering). **Herdado:** `x-test-execute` (?ex-`x-execute-tests`).
- **F2 `[V0]`** — `forge test plan`. **Herdado:** `x-test-plan` (?ex-`x-plan-tests`).
- **F3 `[V0]`** — `forge test e2e`. **Herdado:** `x-test-e2e-execute` (?).
- **F4 `[V0]`** — `forge test contract` + `forge test contract lint`. **Herdado:** EPIC-0072.
- **F5 `[V0]`** — `forge test mutation`. **Herdado:** EPIC-0072.
- **F6 `[V0]`** — `forge test performance`. **Herdado:** EPIC-0072.
- **F7 `[V0]`** — `forge test regression-shell`. **Herdado:** EPIC-0073.
- **F8 `[V0]`** — `forge test smoke api/socket`. **Herdado:** sim.
- **F9 `[V0]`** — `forge code format` / `forge code lint` / `forge code audit`. **Herdado:** sim.

#### P4.C2 — Review Workers

- **F1 `[V0]`** — `forge review <STORY>` (parallel dispatcher).
- **F2 `[V0]`** — `forge review pr <PR>` (Tech Lead worker).
- **F3 `[V0]`** — Specialist workers (14 total): api, compliance, data-modeling, database, devops, events, gateway, graphql, grpc, observability, security, performance, qa, codebase. **Herdado:** todos existem como skills.

#### P4.C3 — Documentation

- **F1 `[V0]`** — `forge doc generate`. **Herdado:** `x-doc-generate` (?).
- **F2 `[V0]`** — `forge doc validate` (Rule 31). **Herdado:** `x-doc-validate` (?).
- **F3 `[V0]`** — `forge adr generate`. **Herdado:** `x-adr-generate` (?ex-`x-generate-adr`).
- **F4 `[V0]`** — `forge release changelog`. **Herdado:** sim.
- **F5 `[V0]`** — `forge arch update` / `forge arch system update`. **Herdado:** sim.

#### P4.C4 — Security Suite

- **F1 `[V0]`** — `forge security owasp`.
- **F2 `[V0]`** — `forge security dependency`.
- **F3 `[V0]`** — `forge security supply-chain`.
- **F4 `[V0]`** — `forge security dashboard`.
- **F5 `[V0]`** — `forge security pipeline`.
- **F6 `[V0]`** — `forge security hardening`.
- **F7 `[V0]`** — `forge security runtime`.
- **F8 `[V0]`** — `forge security pentest` (+ `--dynamic` para DAST gate).
- **F9 `[V0]`** — `forge security {container,dast,infra,sast,secrets,sonar}` (conditional adapters).
- **F10 `[V0]`** — `forge security dep-policy validate` (Rule 32).
- **F11 `[V0]`** — `forge security threat-model`. **Herdado:** `x-threat-model` (?ex-`x-model-threats`).

### §7.7. P5 Developer Experience — Features

#### P5.C1 — CLI v2

- **F1 `[V0]`** — `forge` CLI unificado.
- **F2 `[V0]`** — Saída estruturada `--output text/json/ndjson`.
- **F3 `[V0]`** — `forge repl`.
- **F4 `[V0]`** — `forge migrate` (já em P-1.C04).
- **F5 `[V0]`** — `forge init` (já em P-1.C01).

#### P5.C2 — TUI & Local UI

- **F1 `[V0]`** — `forge tui`.
- **F2 `[V0]`** — `forge watch`.
- **F3 `[V0]`** — `forge ui` (web console local).
- **F4 `[V0]`** — Editor visual de rules/skills.

#### P5.C3 — IDE Extensions

- **F1 `[V0]`** — Extensão VS Code.
- **F2 `[V0]`** — Extensão JetBrains.
- **F3 `[V0]`** — Painel inline de evidências.
- **F4 `[V0]`** — Auto-complete profile/capabilities.

#### P5.C4 — Onboarding & Time-to-Value

- **F1 `[V0]`** — `forge init` 5-7 perguntas.
- **F2 `[V0]`** — Templates por persona.
- **F3 `[V0]`** — Tutorial guiado in-IDE.
- **F4 `[V0]`** — `forge doctor` (já em P-1.C01).

#### P5.C5 — Squad Operating System

- **F1 `[V0]`** — `forge intake {open,triage,route,close}` (fila única).
- **F2 `[V0]`** — `forge backlog {groom,score,explain-score,reorder}` + `forge item ready-check` + `forge item ask-missing`.
- **F3 `[V0]`** — Modelo de **esforço misto** com 5 dimensões (`human/ai/supervision/review/risk` + `blended.{score,confidence,rationale}`).
- **F4 `[V0]`** — `forge sprint {plan,start,rebalance,close}` com WIP limits por tipo.
- **F5 `[V0]`** — `forge ai {plan,run,budget forecast}` com 5 modos (assisted/autonomous/pairing/review-only/spike).
- **F6 `[V0]`** — `forge review {queue,assign,nudge,load}`.
- **F7 `[V0]`** — `forge daily` / `forge status report` / `forge stakeholder update` / `forge squad health` / `forge release readiness` / `forge release risk`.
- **F8 `[V0]`** — `forge incident {open,mitigate,postmortem,create-actions}`.
- **F9 `[V0]`** — `forge metrics {flow,quality,ai}`.
- **F10 `[V0]`** — `forge ci triage` (flaky/CI failure → bug/maintenance/test debt).
- **F11 `[V0]`** — `forge metric anomaly open` (degradação observada → bug/perf change/incident).

### §7.8. P6 Knowledge & Marketplace — Features

#### P6.C1 — Skill Marketplace

- **F1 `[V0]`** — Registry central / self-hosted.
- **F2 `[V0]`** — SemVer obrigatório.
- **F3 `[V0]`** — Dependency resolution.
- **F4 `[V0]`** — Trust model com signing + sandbox + SBOM.
- **F5 `[V0]`** — Compatibility matrix por modelo/provider.

#### P6.C2 — Rule & Governance Library

- **F1 `[V0]`** — Rule packs por domínio.
- **F2 `[V0]`** — Rule simulator.
- **F3 `[V0]`** — Rule conflict detector.
- **F4 `[V0]`** — Custom rule authoring.

#### P6.C3 — Template & Profile Catalog

- **F1 `[V0]`** — Catálogo de profiles oficial/community.
- **F2 `[V0]`** — Rating e usage stats.
- **F3 `[V0]`** — `forge profile fork`.
- **F4 `[V0]`** — Profile lineage.

#### P6.C4 — Cross-Project Intelligence

- **F1 `[V0]`** — Padrões agregados anonimizados.
- **F2 `[V0]`** — Recommendation engine.
- **F3 `[V0]`** — Drift detection cross-repo.
- **F4 `[V0]`** — Knowledge graph navegável.

#### P6.C5 — Plugins V0

- **F1 `[V0]`** — `forge jira create-epic`.
- **F2 `[V0]`** — `forge jira create-stories`.
- **F3 `[V0]`** — `forge mcp recommend`.
- **F4 `[V0]`** — MCP marketplace cache.

### §7.9. P7 Observability — Features

#### P7.C1 — Local & Real-Time Telemetry

- **F1 `[V0]`** — Captura local NDJSON + queries CLI.
- **F2 `[V0]`** — Streaming opt-in (OTLP/Datadog/HTTP custom).
- **F3 `[V0]`** — Dashboard live.
- **F4 `[V0]`** — Alerting.
- **F5 `[V0]`** — Trace OTel-compatible.

#### P7.C2 — Quality & Compliance Metrics

- **F1 `[V0]`** — Coverage longitudinal.
- **F2 `[V0]`** — Refinement quality score.
- **F3 `[V0]`** — Doc freshness heatmap.
- **F4 `[V0]`** — Compliance posture report.

#### P7.C3 — FinOps & Cost Insights

- **F1 `[V0]`** — Custo de LLM por skill/story/epic/org.
- **F2 `[V0]`** — Sugestão de model downgrade.
- **F3 `[V0]`** — Budget guardrails locais.
- **F4 `[V0]`** — Comparativo por provider.

#### P7.C4 — Research & Benchmarking

- **F1 `[V0]`** — A/B testing de skills.
- **F2 `[V0]`** — Benchmark suite.
- **F3 `[V0]`** — Regression detection.
- **F4 `[V0]`** — Public leaderboard opt-in.

### §7.10. P8 Governance avançado — Features

#### P8.C1 — Audit & Compliance Engine

- **F1 `[V0]`** — Audit log local imutável (em P-1.C05).
- **F2 `[V0]`** — Evidence vault local.
- **F3 `[V0]`** — Reports SOC2 / ISO 27001 / LGPD.
- **F4 `[V0]`** — Forensics.
- **F5 `[V0]`** — `forge ci verify` (em P0.C6).

#### P8.C2 — Refinement & Quality Gates v2

- **F1 `[V0]`** — AI-assisted refinement.
- **F2 `[V0]`** — Refinement memory.
- **F3 `[V0]`** — Refinement templates por domínio.
- **F4 `[V0]`** — NO-GO library.

#### P8.C3 — Security Posture & Threat Modeling

- **F1 `[V0]`** — Continuous threat modeling.
- **F2 `[V0]`** — SBOM gerado e validado.
- **F3 `[V0]`** — Secret scanning integrado.
- **F4 `[V0]`** — Supply chain trust score.

#### P8.C4 — Privacy, Multi-Tenancy & RBAC

- **F1 `[V0]`** — Multi-tenant.
- **F2 `[V0]`** — RBAC.
- **F3 `[V0]`** — Data residency.
- **F4 `[V0]`** — PII scrubbing para telemetria remota.

---

## §8. Mapa de Migração das 123 Skills Atuais → Forge

> Renames consolidados pós-EPIC-0076. Use como referência ao escrever Features que substituem skills.

### §8.1. Classificação resumida (123 skills → 107+ ativos)

| Classificação Forge | Total | Exemplos |
| :--- | :-: | :--- |
| `service` (interno Java) | 16 | `ArgsNormalizer`, `StateRepository`, `TemplateRenderer`, `PhaseGateService`, `StoryContextLoader`, `StoryPlanBuilder`, `EpicPlanBuilder`, `StoryVerifyGate`, `EpicIntegrityGate`, ... |
| `adapter` (sistema externo) | 12 | `GitAdapter.{branch,commit,push,merge,worktree,cleanup,precheck,epicBranch}`, `GithubAdapter.{prCreate,prMerge,prWatchCi,prComments}` |
| `command` (Forge CLI público) | 50 | Todos os `forge *` listados em §7. |
| `worker-prompt` (LLM, sem código) | 21 | Architecture/test/task plan workers; refinement personas; review specialists; threat-model; ADR. |
| `template` (Template Registry) | 5 | Scaffolds stack-specific (helidon/micronaut/picocli/quarkus/spring). |
| `kp` (knowledge pack) | 1+ | `planning-standards-kp` + KPs implícitos (architecture, testing, security, observability, …). |
| `plugin` (V0 opt-in) | 2 | Jira (create-epic, create-stories). |

### §8.2. Skills bloqueadoras (caminho crítico — Wave 1/3)

| Skill atual (pós-0076) | Destino Forge | Wave | Bloqueia |
| :--- | :--- | :-: | :-: |
| `x-internal-args-normalize` | `service ArgsNormalizer` | W1 | TODOS comandos |
| `x-internal-status-update` | `service StateRepository` | W1 | 22 skills |
| `x-internal-report-write` | `service TemplateRenderer` | W1 | 14 skills |
| `x-git-commit` (ex-`x-commit-changes`) | `GitAdapter.commit()` + `forge git commit` | W1 | 16 skills |
| `x-git-worktree-manage` (ex-`x-manage-worktrees`) | `GitAdapter.worktree()` + `forge git worktree` | W1 | 13 skills |
| `x-git-push` (ex-`x-push-branch`) | `GitAdapter.push()` + `forge git push` | W1 | 11 skills |
| `x-internal-ensure-epic-branch` | `service EpicBranchPolicy` | W1 | 10 skills |
| `x-internal-load-story-context` | `service StoryContextLoader` | W3 | story-implement |
| `x-internal-build-story-plan` | `service StoryPlanBuilder` | W3 | story-implement |
| `x-internal-verify-story` | `service StoryVerifyGate` | W3 | story-implement |
| `x-task-implement` | `command forge task implement` | W5 | 14 skills |
| `x-story-implement` | `command forge story implement` | W5 | **44 skills (hub central)** |

### §8.3. Mapeamento `x-*` → comando/serviço Forge (resumo categórico)

> Tabela completa em `forge-implementation-roadmap.md` §15. Aqui um resumo por categoria com nomes pós-0076.

| Categoria | Skills | Destino Forge predominante |
| :--- | :--- | :--- |
| `core/internal/ops` (3) | args-normalize, status-update, report-write | `service` (W1) |
| `core/internal/git` (2) | ensure-epic-branch, precheck-worktree | `service` (W1) |
| `core/git` (7) | branch-create, commit, push, merge, worktree-manage, cleanup, planning-commit | `adapter+command` (W1) |
| `core/code` (2) | format, lint | `adapter+command` (W1) |
| `core/internal/pr` (1) | render-pr-body | `service` (W2) |
| `core/pr` (6) | pr-create, pr-merge, pr-ci-watch, pr-fix, pr-fix-epic, pr-merge-train | `adapter+command` (W2) |
| `core/internal/plan` (12) | verify-phase-gates, load-story-context, build-story-plan, resume-story, verify-story, write-story-report, build-epic-plan, verify-epic-integrity, create-epic, create-story, map-epic, frontmatter-migrate | `service` (W3) + 1 `command` (W9) |
| `core/internal/memory` (1) | epic-summary | `service` (W9) |
| `core/lib` (3) | decompose-task, verify-group, audit-rules | `service`+worker (W3) + `command` (W6) |
| `core/plan` (workers) | arch-plan + arch-update + arch-system-update + task-plan + tests-plan + story-plan + story-refine + epic-refine + threat-model + adr + arch-plan-product + arch-plan-capability + feature-create + feature-ideate + evaluate-parallelism + templates-migrate | `worker-prompt` (W4) + `command` (W6/W10) |
| `core/dev` (12) | helidon/micronaut/picocli/quarkus/spring scaffolds + ci-generate + epic-implement + mcp-recommend + setup-env + spec-drift-detect + story-implement + task-implement | misto (W1/W5/W6/W9/W11) |
| `core/ops` (11) | doc-generate, doc-validate, memory-search, incident-handle, troubleshoot, perf-profile, release, release-changelog, status-reconcile, telemetry-analyze, telemetry-trend | `command` (W6/W9) |
| `core/test` (3) | test-plan, test-execute, test-tdd-drive | worker (W4) + `adapter+command` (W6) + `command` (W5) |
| `core/review` (5) | codebase-review, pr-review, perf-review, qa-review, code-audit | `command`+`worker` (W7/W6) |
| `core/security` (8) | dependency-audit, hardening-evaluate, owasp-scan, dast-pentest, runtime-evaluate, security-dashboard, security-pipeline, supply-chain-audit | `command` (W8) |
| `core/jira` (2) | jira-epic-create, jira-stories-create | `plugin` (W11) |
| `conditional/test` (9) | contract-execute/lint, e2e-execute, mutation-execute, perf-run, perf-execute, regression-shell-execute, smoke-api/socket | `command` (W6) |
| `conditional/security` (8) | dep-policy-validate, container-scan, dast-run, infra-assess, pentest-run, sast-run, secrets-scan, sonar-run | `command`/`adapter` (W8) |
| `conditional/review` (11) | api/compliance/data-modeling/db/devops/events/gateway/graphql/grpc/obs/security review | `worker-prompt` (W7) |
| `conditional/dev` (1) | setup-stack | `adapter+command` (W9) |
| `conditional/ops` (1) | observability-instrument | `worker+adapter` (W9) |

---

## §9. Migração Rules → Policies

> 32 rules atuais. Lista resumida; tabela completa em roadmap §16.

| Classe | Total | Comportamento no Forge |
| :--- | :-: | :--- |
| `policy_executable` | 12 | Vira `policy_id` versionada com testes; runtime e/ou CI aplica. |
| `policy+kp` | 13 | Policy + KP de doutrina sobre o "porquê". |
| `kp_doctrine` | 7 | Apenas KP/doutrina humana, sem enforcement automático. |

**Policies executáveis críticas** (W3 — P0.C3 Policy Engine):
- `forge.policy.refinement-gate@1` (Rule 29)
- `forge.policy.execution-integrity@1` (Rule 24 master)
- `forge.policy.zero-bypass@1` (Rule 27)
- `forge.policy.task-hierarchy@1` (Rule 25)
- `forge.policy.epic-branch-model@1` (Rule 21)
- `forge.policy.capability-frontmatter@1` (Rule 28a)
- `forge.policy.tool-call-grammar@1` (Rule 28b)
- `forge.policy.doc-freshness@1` (Rule 31, W6)
- `forge.policy.dependency-policy@1` (Rule 32, W8)
- `forge.policy.ci-watch@1` (Rule 45, W2)
- `forge.policy.project-scope@1` (Rule 14)
- `forge.policy.pci-prohibitions@1` (Rule 11, W7/W8)
- `forge.policy.ai-memory@1` (Rule 33, W9)
- `forge.policy.product-first-rnf-no-relax@1` (RNF gate, EPIC-0077-derived)

---

## §10. Templates, KPs, Hooks, Scripts

### §10.1. Templates (55 atuais → ~38 com schema strict)

- **28 plan templates** → `Template Registry` strict em P-1.C04 (Wave 4). Cada um ganha `template_id`, `schema_version`, JSON Schema input/output, consumidores declarados.
- **11 Pebble templates** → composition engine (P2.C2, Wave 4).
- **3 verbatim copy** (incident/postmortem/ADR) → P2.C2 (Wave 4).
- **3 JSON Schema validators** (execution-state, telemetry-event, refinement-verdict) → integrados a `StateRepository`/`TelemetryService`/`RefinementGate`.
- **1 retire** (`_TEMPLATE-SKILL.md`) → substituído por catálogo Forge.
- **8 authoring/reference** (changelog-entry, doc-validate-report, pentest-plan, threat-model, regression-shell, performance-baseline, telemetry-report) → registrados normalmente.

### §10.2. Knowledge Packs

- `planning-standards-kp`, `architecture`, `coding-standards`, `testing`, `security`, `compliance`, `observability`, `infrastructure`, `dockerfile`, `api-design`, `protocols`, `resilience`, `layer-templates`, `patterns` → carregados sob demanda pelo KP Loader (P-1.C04).
- **Decisão:** KPs **não** ganham frontend de comando (`forge explain` é a única superfície de leitura).

### §10.3. Hooks (21 bash files)

- **9 telemetry** → `TelemetryService` Java (W0).
- **7 runtime gates** → `PhaseGateService`, `BypassPolicy`, `RefinementGate`, `PreflightService`, `ContinuousFlowMonitor` (W3).
- **4 dual** (post-compile + verify-story) → `BuildAdapter.afterEdit()` + runtime + Camada B (W1+W3).
- **1 retire** (`TELEMETRY-README.md`).

### §10.4. Audit/preflight scripts (~35)

- **32 → `forge ci verify --<check>`** (subcomandos da Camada B).
- **1 retire** (`audit-hooks-self-check.sh` — Forge não tem hooks bash).
- **2 dual** (`audit-coverage-local.sh`, `audit-execution-integrity.sh`) → runtime + CI.

---

## §11. Wave Plan Executável

> Ajustado a partir do roadmap §19 considerando que **EPIC-0064/0070-0076 estão entregues** e **EPIC-0077 está em andamento** no `ia-dev-env`. Forge **inicia em W0 zerando** o substrato Java, mas reaproveitando código existente (CapabilityResolver/Composer/OutputPruner, ProjectConfig, TelemetryScrubber, etc.).

| Wave | Conteúdo | Épicos estimados | Paralelizável com |
| :-: | :--- | :-: | :--- |
| **W0** | P-1 Foundations: kernel CLI + repo + storage + identity + composition + telemetry + provider + adapters Git/GitHub/Build/Test/Doc. | 8-10 | — (gate inicial) |
| **W1** | Internal Primitives: args-normalize + status-update + report-write + 7 git skills + 2 code skills + 2 internal-git. | 6-8 | — |
| **W2** | GitHub & PR Primitives: 6 PR skills + render-pr-body. | 3-4 | W3 |
| **W3** | Spine: State Machine + Phase Gates + Policy Engine + 12 internal/plan + Refinement Gate + Execution Integrity. | 8-10 | W4 |
| **W4** | Workers: arch-plan workers + test-plan + task-plan + story/epic refine + threat-model + ADR + composition templates + Template Registry strict. | 6-8 | W3 |
| **W5** | Orchestrators: forge story implement + epic implement + task implement + release + epic orchestrate + merge-train + test tdd. | 5-6 | — (gate de capability completa) |
| **W6** | Quality + Docs + Code Audit: 9 conditional/test + doc-generate/validate + adr-generate cmd + release-changelog + arch-system-update + lib-audit-rules + code-audit. | 6-8 | W7, W8 |
| **W7** | Reviews: review dispatcher + 14 review specialists (workers). | 3-4 | W6, W8 |
| **W8** | Security: 8 core/security + 8 conditional/security. | 4-5 | W6, W7 |
| **W9** | DX & Migration: doctor + spec-drift + perf-profile + ops-incident + troubleshoot + status-reconcile + telemetry-analyze/trend + memory-search + epic-summary + frontmatter-migrate + template-migrate + setup-stack + obs-instrument + mcp-recommend + 4 forge novos. | 5-7 | qualquer |
| **W10** | Strategic Chain: forge product/capability/feature/architecture/ideate/epic-create + feature-create/ideate refactor + Post-Delivery Lifecycle. | 6-8 | W9, W11 partes |
| **W11+** | Marketplace + Squad OS + TUI/IDE/Web + Trust + 5 scaffolds + 2 jira plugins + Observability/Analytics avançados. | 15-20+ | — |
| **Total V0** | **73-96 épicos** estimados. | — | — |

### §11.1. Caminho crítico (10 skills bloqueadoras)

```text
W1: x-internal-args-normalize, x-internal-status-update, x-internal-report-write,
    x-git-{commit,push,branch,worktree-manage}, x-internal-ensure-epic-branch
W3: x-internal-load-story-context, x-internal-build-story-plan, x-internal-verify-story
W5: x-task-implement, x-story-implement (hub central — bloqueia 44 outras)
```

### §11.2. Phase gates de wave (Rule 25 aplicada ao Forge)

| Gate | Critério |
| :--- | :--- |
| W0 → W1 | P-1 Foundations ≥ 80% verde + smoke E2E `forge init && forge sync && forge index rebuild` passa. |
| W3 → W5 | Spine ≥ 99% verde + 1 orquestrador (story-implement OU epic-implement) com paridade comportamental medida. |
| W5 → W6/W7/W8 | ≥ 1 orquestrador feature-complete + dual-mode rodando contra `ia-dev-env`. |
| W10 → V0 GA | `forge product/capability/feature/architecture/ideate/epic-create` + 5 usuários reais executaram lifecycle completo sem recovery. |

---

## §12. Deltas vs Roadmap Original

| Tema | Roadmap original | Este plano | Razão |
| :--- | :--- | :--- | :--- |
| **Terminologia** | "Capacity" | **Capability** | Alinhamento com EPIC-0064 (capability composition) e EPIC-0077 (Product-First). Evita dois termos para o mesmo conceito. |
| **Skill names** | `x-implement-story`, `x-create-pr`, `x-plan-architecture`, `x-create-feature`, ... | `x-story-implement`, `x-pr-create`, `x-arch-plan`, `x-feature-create`, ... | Padrão verb-first pós-EPIC-0076. |
| **Architecture planning** | "Worker `x-plan-architecture` parametrizado por nível" (P1.C3) | **3 commands distintos** (`x-arch-plan-product`, `x-arch-plan-capability`, `x-arch-plan` para feature) já existindo em EPIC-0077. | EPIC-0077 já materializou. |
| **Templates Product/Capability** | "Templates novos" implícitos | `_TEMPLATE-PRODUCT.md` (8 seções), `_TEMPLATE-CAPABILITY.md` (7 seções) **já existem em EPIC-0077**. | EPIC-0077 já materializou. |
| **C4 model** | Não citado explicitamente | **Obrigatório** (C1-C4 Mermaid) em planning artifacts. | EPIC-0077 contrato. |
| **RNF inheritance gate** | "Refinement gate" genérico | **No-relax gate** explícito (`x-internal-rnf-validate`). | EPIC-0077 já materializou. |
| **`flowVersion`** | "1-4" | **1-5** (`5` = Product-First). | EPIC-0077. |
| **AI Memory Layer** | Em P7 ou implícito | **Já entregue (Rule 33 + ADR-0028). Reusar diretamente.** | EPIC-0075 concluído. |
| **Doc-as-DoD** | Em P4.C3 | **Já entregue (Rule 31 + ADR-0024). Reusar.** | EPIC-0071 concluído. |
| **Test strategy gates** | Em P4.C1 | **Já entregue (perf/mutation/contract — Rule 05 + ADR-0025). Reusar.** | EPIC-0072 concluído. |
| **Dependency policy gate** | Em P4.C4 | **Já entregue (Rule 32 + ADR-0027). Reusar.** | EPIC-0074 concluído. |
| **Camadas 0-4 → A/B** | Citado | **Camada 0 já existe** (`enforce-preflight-gates.sh`); Forge consolida em A/B. | EPIC-0063 concluído. |
| **Naming bug-of-feature** | Roadmap usa `forge bug fix` | Mantido; integra com Post-Delivery Lifecycle (P1.C5). | — |

### §12.1. Riscos críticos retomados

| Risco | Mitigação no plano |
| :--- | :--- |
| Pular W0/W1 e começar por W5 | Phase gate explícito: nenhum épico W5 entra em sprint sem W0+W1+W3 ≥ 90%. |
| Workers (W4) antes de Provider Abstraction (P-1.C06) | `LlmProvider` interface entregue na primeira épica de W0 antes de qualquer worker. |
| Strategic Chain (W10) antes de Spine (W3) | Phase gate: W10 só entra após W3+W4 mergeados. |
| Migration big-bang (sem dual-mode) | §11.2 obriga 1 release dual-mode antes de `forge migrate finalize`. |
| Hooks bash desligados antes de runtime cobrir invariantes | §10.3 e §10.4 mapeiam 1:1; cada hook só desliga após teste do Forge cobrir. |

---

## §13. Open Questions a resolver (antes de virar épicos)

> 14 questões herdadas do roadmap §22, ainda válidas. ADR para cada antes de decompor.

### §13.1. Plataforma

1. **Linguagem do `forge` CLI binário:** Java/Picocli (continuidade) com GraalVM native-image, ou migração para Go/Rust? **Impacta:** P-1.C01-C08.
2. **Storage de control repo concorrente:** SQLite por-máquina; quando 2+ engenheiros editam o mesmo control repo simultaneamente, como resolver lineage? Distributed lock via Git refs? Sync journal append-only? **Impacta:** P-1.C02.
3. **Provider Abstraction surface:** unifica streaming + function calling + tool use desde W0, ou começa com sync request-reply e expande? **Impacta:** P-1.C06 e todos os workers de W4.
4. **Spine event-bus:** runtime tem barramento interno de eventos (pub/sub) desde W3, ou só depois de Marketplace? **Impacta:** P0.C1 e P6.C1.
5. **Workers e persistência:** workers escrevem direto em disco, ou retornam payload tipado para o runtime persistir? **Impacta:** P0.C4 e workers de W4.

### §13.2. Produto

6. **`forge project` vs control repo:** Project é entidade explícita ou sinônimo de repo? Reconciliar antes de W10. **Impacta:** P1.C1.
7. **Strategic Chain ideation worker:** um único `WorkerPrompt` parametrizado por `--kind product/capability/feature`, ou três prompts distintos? **Impacta:** P1.C2.
8. **Architecture Plan templates:** 3 templates distintos ou um único parametrizado? (EPIC-0077 indica caminho para 3.) **Impacta:** P1.C3 e P-1.C04.

### §13.3. Migração

9. **Skills atuais como deprecation surface:** mantemos SKILL.md como redirects (`This skill is now forge story implement`), ou removemos cleanly após dual-mode? **Impacta:** §10.3 deprecation policy.
10. **Plugins V0 — Jira e MCP:** SemVer + signing + sandbox antes de funcionarem; são V0 mesmo, ou opt-in pós-V0? **Impacta:** P6.
11. **Conditional rules (10, 11, 12) — overlay ou rules separadas?** **Impacta:** §9 e P-1.C04.

### §13.4. Governança

12. **`forge ci verify` é monolítico ou modular?** Um único comando que roda 32+ checks, ou subcomandos por capability (`forge ci verify --dep-policy`)? **Impacta:** P0.C6 e P8.
13. **Audit log local imutável (Camada A) vs CI (Camada B):** mesmo schema NDJSON ou estruturas distintas? **Impacta:** P-1.C05.
14. **Refinement Verdict — quem assina?** LLM rubber-stamp, humano explícito ou híbrido? **Impacta:** P0.C5 e Rule 29.

### §13.5. Pós-Forge / Post-Delivery

15. **Post-Delivery Lifecycle (V3 §9) — V0 mesmo?** São 12 tipos de entrada com state machines próprias. Roadmap classifica W10/W11; este plano confirma V0 com priorização: bug, feature change, security finding na W10; deprecation/removal/experiment/support/maintenance/dependency-upgrade/rollback/spec-drift/incident-actions na W11.
16. **Squad Operating System (V3 §10) — V0 mesmo?** Modelo de esforço misto humano/IA é diferencial estratégico mas adiciona complexidade. Risco: parecer microgestão. Mitigação: medir fluxo, não rankear pessoas.

---

## §14. Próximos passos imediatos

1. **Refinement multi-persona deste plano** com PO + Tech Lead + Architect + Security + QA + SRE/DevOps. Use `/x-refine-epic` ou `/x-story-refine` (atualmente `x-refine-story`/`x-refine-epic`) com este documento como spec.
2. **Resolver 16 open questions de §13**, criando ADRs para cada decisão.
3. **Spike de inversão de controle** usando o caminho crítico W1 + um command stub:
   - Implementar `ArgsNormalizer` + `StateRepository` + `TemplateRenderer` em Java.
   - Stub `forge story refine` que use os três + chame Anthropic via `LlmProvider`.
   - Comparar com `x-story-refine` markdown atual: tempo, taxa de bypass, qualidade, debuggability.
4. **Spike de target adapter Cursor** em paralelo (P2.C3).
5. **Decompor W0 + W1** em épicos concretos do Forge usando este documento como entrada de `forge epic create` (atualmente via `x-feature-create` → `x-internal-create-epic`).
6. **Criar Project `Forge`** com este documento como artefato fundador. Cadeia: `forge project create FORGE` → `forge product create FORGE-FOUNDATIONS` → `forge capability create FORGE-FOUNDATIONS-PROJECT-BOOTSTRAP` → `forge feature create FORGE-FOUNDATIONS-PROJECT-BOOTSTRAP-CLI-BINARY` → `forge arch plan feature ...` → `forge epic create ...`.
7. **Atualizar `CLAUDE.md` raiz** com link para este plano como referência operacional adicional ao v3 e roadmap.
8. **Validar com 5-10 usuários atuais do `ia-dev-env`** a hipótese de inversão de controle e a cadeia Product-First (alguns já estão envolvidos via EPIC-0077).
9. **Definir licença do core e fronteira comercial** (core OSS + cloud paid).
10. **Registrar `Forge` como nome oficial do produto** (domínio + organização).

---

## §15. Notas de versionamento

- Este documento é versionado como **`forge-unified-product-plan.md` v1.0**.
- Mudanças em **hierarquia, naming ou waves** exigem ADR explícito.
- Mudanças em **conteúdo de feature** podem ser inline.
- Quando este plano for executado em ≥ 50%, §6 e §7 viram **dado vivo no SQLite operational index**, e o markdown passa a ser projeção legível para humanos.

> **Decisão estratégica:** este plano **não substitui** `forge-strategic-plan-v3.md` nem `forge-implementation-roadmap.md`. Ele os reconcilia, atualiza para o estado atual do `ia-dev-environment` (até EPIC-0076 + parcial 0077) e organiza para servir como entrada direta de ideação. Os três documentos evoluem juntos.
