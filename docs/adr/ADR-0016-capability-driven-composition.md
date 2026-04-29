# ADR-0016 — Capability-Driven Composition

| Field | Value |
| :--- | :--- |
| **Status** | Accepted |
| **Date** | 2026-04-28 |
| **Accepted** | 2026-04-29 |
| **Deciders** | Eder Junior, Claude Code (Opus 4.7) |
| **Supersedes** | — |
| **Related** | ADR-0003 (Skill Taxonomy), ADR-0006 (File-Conflict-Aware Parallelism), ADR-0014 (Task Hierarchy), ADR-0015 (Audit Gate Lifecycle) |
| **Epic** | EPIC-0064 |

## Context

O gerador `ia-dev-env` produz `.claude/` (skills, rules, knowledge packs, agents, hooks, scripts, templates) a partir de um YAML de input. Hoje a composição é majoritariamente **copy-cego**:

1. **Plataformas existem** (`Platform.CLAUDE_CODE` / `SHARED`) mas filtram apenas no nível "qual assembler roda", não no conteúdo dos artefatos.
2. **8 SkillGateEvaluators parciais** (`InterfaceGate`, `TestingGate`, `SecurityGate`, `SecurityScanningGate`, `ComplianceGate`, `PentestGate`, `ReviewGate`, `InfraGate`) cobrem ~26 skills condicionais; sobram dezenas de artefatos copiados sempre.
3. **`SkillRegistry.CORE_KNOWLEDGE_PACKS:43`** lista `data-management` como hardcoded "core". `KnowledgePackSelection.selectDataPacks()` filtra `database-patterns` e `data-modeling` quando `database=none`, mas o filtro é parcial e contraditório — Java CLI Picocli sem DB recebe os 3 KPs.
4. **Frameworks (Spring, Quarkus, Picocli, Helidon)** compartilham mesmo conteúdo. `stack-patterns/spring-patterns/` e `stack-patterns/quarkus-patterns/` são copiados juntos; 50% morto por projeto.
5. **16 agents** copiados sempre, mesmo `database-engineer` quando não há DB. **21 rules** sem gate de capability — Rule 09 (Data Management) e Rule 11 (PCI-DSS) têm seções inline que deveriam ser fragmentos compostos.
6. **`x-review`** (orquestrador de especialistas) hardcoda em texto markdown a tabela de sub-reviewers (linhas 86-99 do SKILL.md). Adicionar novo especialista exige editar 3 lugares manualmente.
7. **Frontmatter parser existe** (`FrontmatterInjector`, `FrontmatterParser`), mas **não é usado para filtrar cópias** — apenas para injetar `context-budget`.

A consequência: o YAML do projeto é **parcialmente decorativo**. Campos como `data.database.type=none` são lidos por alguns assemblers mas ignorados por outros, gerando outputs incoerentes. Onboarding de framework novo (Helidon, Micronaut) custa duplicação manual em ~6 lugares. Diff entre output Spring vs Quarkus é ~15% quando deveria ser >50%.

## Decision

Adotar **capability-driven composition** como modelo único de geração.

### Decisões fundamentais

1. **Capability como contrato declarativo.** Toda capability vive em `capabilities/<category>/<id>.yaml` (e.g., `capabilities/data/database/postgres.yaml`), com campos `id`, `category`, `kind` (`atomic`/`composite`/`profile`), `version`, `parameters`, `requires`, `provides`, `excludes`, `tags`. Glob `data.database.*` permitido. Excludes simétrico (A.excludes B ⇒ B.excludes A).
2. **Frontmatter universal.** Toda skill, rule, KP, agent, hook e template declara `requires-capabilities`, `excludes-capabilities`, `parameters-from`, `fragment-slot`, `fragment-slots` em frontmatter v3.0. Schema validado por `governance/schemas/frontmatter-3.0.json`. Ausência = build error (`audit-capability-coverage.sh`).
3. **Resolver determinístico em Java.** `dev.iadev.application.capability.CapabilityResolver` recebe `ProjectConfig` v3.0, expande profiles, constrói `CapabilityGraph` (DAG), valida via `CycleDetector` (Tarjan SCC), `MutexValidator`, `PrerequisiteValidator`, e produz `ResolvedCapabilitySet` imutável. Erros tipados (`CycleDetected`, `MutexConflict`, `MissingPrerequisite`, `UnknownCapability`, `AmbiguousProfile`).
4. **Composer substitui copy cego.** `dev.iadev.application.composition.CapabilityAwareAssembler` substitui `SkillsAssembler`, `KnowledgeAssembler`, `AgentsAssembler` e os 8 gates. Pipeline: `ArtifactScanner → FrontmatterParser → CapabilityMatcher → CompositionPlanner → CompositionEngine → PebbleRenderer → OutputWriter`.
5. **Composição em runtime para artefatos polimórficos.** `x-review` deixa de ter tabela hardcoded de especialistas; passa a ser composto a partir de `fragments/{qa,perf,db,devops,api,event,security,compliance}.md`, cada fragmento com `requires-capabilities` próprio. Mesma estrutura para Rule 09, Rule 06, KP `database-patterns`, etc.
6. **Three-level templating discriminado.** Composer (`{{ slot: X }}`, `{{ #each fragments.X }}`) roda ANTES de Pebble (`{{ var }}`, `{% if %}`); ambos em build-time. LLM placeholders (`{{UPPER_SNAKE}}`) ficam literais nos `_TEMPLATE-*.md` via `{% verbatim %}` automático. `PebbleSafeRenderer` faz pre-flight regex que falha se LLM placeholder foi processado por engano.
7. **Sem retrocompatibilidade.** Schema YAML salta para 3.0 e quebra v2. Não há dual-mode `flowVersion`-style. CHANGELOG marca `[Breaking]` + bump major. Goldens v2 deletados em Phase 7. Downstream consumers comunicados via release notes.
8. **6 audit scripts novos** integrados via Rule 26: `audit-capability-coverage`, `audit-capability-graph`, `audit-frontmatter-schema`, `audit-output-pruning`, `audit-fragment-coherence`, `audit-capability-determinism`. Baselines vazios (sem grandfathering, decisão consciente).

### Estrutura de pacotes Java (novos)

```
dev.iadev.domain.capability/
  CapabilityId, CapabilityDefinition, ActiveCapability,
  ResolvedCapabilitySet, CapabilityGlob, CapabilityRef,
  CapabilityKind enum, ParameterSpec, ResolutionWarning,
  CapabilityError sealed { MissingPrerequisite, MutexConflict,
    UnknownCapability, CycleDetected, AmbiguousProfile }

dev.iadev.application.capability/
  CapabilityRegistry, CapabilityResolver, CapabilityGraph,
  CapabilityExpressionParser, ProfileExpander,
  MutexValidator, PrerequisiteValidator, CycleDetector

dev.iadev.application.composition/
  ArtifactDescriptor, ArtifactKind enum,
  ArtifactScanner, FrontmatterParser, CapabilityMatcher,
  CompositionPlan, CompositionPlanner, CompositionEngine,
  CapabilityAwareAssembler

dev.iadev.domain.port.output/
  CapabilityCatalogRepository (port)

dev.iadev.infrastructure.adapter.output/
  YamlCapabilityCatalogAdapter (impl)
```

## Consequences

### Positivas

- **Output enxuto por perfil.** Java CLI Picocli sem DB → output sem `data-management/`, `database-patterns/`, `data-modeling/`, `database-engineer.md`, `x-review-db`, `messaging-patterns/`, `stack-patterns/quarkus/`. Redução estimada **≥ 45%** no tamanho do `.claude/` para perfis minimalistas.
- **Onboarding linear de novos frameworks.** Helidon vira "criar `capabilities/framework/helidon.yaml` + 3 fragmentos em `stack-patterns/helidon/`". Tempo estimado ≤ 4h vs impossível hoje (validado em story-0064-0408).
- **Determinismo testável.** `audit-capability-determinism.sh` valida 3 builds consecutivos = mesmo SHA por arquivo. Property-based suite (10K casos, 4 invariantes) cobre invariantes do resolver.
- **YAML deixa de ser decorativo.** Cada campo no `setup-config.*.yaml` se traduz em capabilities ativas via parser estruturado, com erros tipados e legíveis em vez de copy silencioso.
- **`x-review` extensível por composição.** Adicionar novo especialista (ex: `x-review-mobile`) custa criar `fragments/mobile.md` com `requires-capabilities: [framework.android OR framework.ios]`; nenhuma edição em `x-review/SKILL.md`.

### Negativas

- **Migração 1× de 182 arquivos.** Frontmatter v3.0 obrigatório em 90 SKILLs + 21 rules + 33 KPs + 16 agents + ~10 hooks + 12 templates. Pipeline híbrido AI-assisted + gate humano por categoria (12 PRs em Wave F).
- **Curva de aprendizado.** Colaboradores precisam entender: capability ID, glob match, fragment slot, três níveis de templating. Mitigação: documentação em CLAUDE.md, README de `.claude/`, exemplos canônicos em `x-review/fragments/`.
- **Quebra de downstream.** Projetos consumindo `.claude/` gerado precisarão re-gerar com schema v3.0. CHANGELOG `[Breaking]` + comunicação proativa. Smoke `Epic0064DownstreamProjectSmokeTest` valida fixture project.
- **Performance overhead.** Parsing de 182 frontmatters + grafo de capabilities adiciona ~1-3s ao build. Mitigação: lazy frontmatter loading, cache singleton do `CapabilityRegistry`, paralelização via `ResourceDiscovery`. Budget: `tempoCompose ≤ 1.2 × tempoCopyLegacy`.
- **Test matrix explosion.** `frameworks(5) × db(2) × messaging(3) × cache(2) × auth(3) = 180 perfis`. Mitigado por: 9 goldens canônicos congelados, pairwise covering (~30 perfis), property-based (jqwik), golden manifest (hash-set declarativo em vez de bytes).

### Neutras

- ~20 dos 34 assemblers atuais deletados (substituídos por `CapabilityAwareAssembler`). 14 mantidos: `CicdAssembler`, `SettingsAssembler`, `RunbookAssembler`, `ReadmeTablesAssembler`, `ClaudeMdAssembler`, `DocsAdrAssembler`, `PlanTemplatesAssembler`, `ScriptsAssembler`, `HooksConfigAssembler`, `MCPAssembler`, etc.
- Novo CLI `validate --config <yaml>` (somente roda resolver, reporta erros) e `capability list [--category <name>]` (introspeção).

## Alternatives Considered

### A) Manter copy cego com gates incrementais (rejeitada)

Continuar adicionando `SkillGateEvaluator` por capability faltante (ex: `DataGate`, `MessagingGate`, `ObservabilityGate`). **Rejeitada porque:**
- Débito cresce: já temos 8 gates parciais e contraditórios (`KnowledgePackSelection.selectDataPacks` vs `SkillRegistry.CORE_KNOWLEDGE_PACKS:43`).
- Cada novo eixo é um remendo: gates não escalam para frameworks (Spring vs Quarkus vs Picocli vs Helidon).
- Frontmatter continua inerte (apenas `context-budget` injetado).
- Adiar a refatoração apenas torna a migração futura mais cara (mais artefatos para tocar).

### B) Per-profile fork de `.claude/` (rejeitada)

Manter forks separados de `.claude/` por perfil (`targets/claude-spring/`, `targets/claude-quarkus/`, etc.). **Rejeitada porque:**
- Explode em N perfis × 182 artefatos = milhares de arquivos a manter.
- Drift inevitável entre forks (correção em Spring não chega em Quarkus).
- Onboarding de framework novo = clonar fork inteiro.

### C) Templates Jinja/Pebble puros (rejeitada)

Usar apenas Pebble com `{% if %}` em vez de modelo de capabilities. **Rejeitada porque:**
- Pebble não cobre semântica de mutex (Spring + Quarkus simultâneos), prerequisites (db-jpa requer database), cycles (graph validation).
- Ifs aninhados em SKILL.md viram ilegíveis (~30 níveis em casos reais).
- Sem typed errors — falhas são silent rendering glitches.
- Frontmatter declarativo + Resolver tipado é estrutura mais forte do que ifs imperativos.

### D) Migração incremental por categoria com dual-mode (rejeitada)

Manter v2 e v3 em paralelo durante migração, com `flowVersion`-style discriminator. **Rejeitada porque:**
- Dono explicitamente pediu "sem retrocompatibilidade" (decisão de produto).
- Dual-mode dobra superfície de bugs (v2 quebra silenciosamente; v3 quebra ruidosamente; testes precisam cobrir ambos).
- Phase 2 já é big-bang em granularidade fina (12 PRs por categoria) — equivalente a migração incremental sem custo de coexistência.

## Implementation Plan

Detalhado em [`ai/epics/epic-0064-capability-driven-composition/IMPLEMENTATION-MAP.md`](../ai/epics/epic-0064-capability-driven-composition/IMPLEMENTATION-MAP.md). 79 stories, 8 fases, 15 waves.

## Audit / Enforcement

- `audit-capability-coverage.sh` — toda skill/rule/KP/agent/hook/template em diretório condicional declara `requires-capabilities`. Hard-fail em CI a partir do merge da story-0064-0215.
- `audit-capability-graph.sh` — sem ciclos, mutex simétrico, sem capability órfã.
- `audit-frontmatter-schema.sh` — 100% dos `.md` válidos contra schema 3.0.
- `audit-output-pruning.sh` — para cada perfil, output não contém artifact com capability inativa.
- `audit-fragment-coherence.sh` — toda referência a fragment existe em disco.
- `audit-capability-determinism.sh` — 3 builds = mesmo SHA bytewise.

Todos via Rule 26 (audit-gate-lifecycle).

## Status Transitions

`Proposed` → `Accepted` ao mergear epic/0064 → develop (story-0064-0704).
