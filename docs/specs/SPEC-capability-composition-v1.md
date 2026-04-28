# SPEC — Capability-Driven Composition v1

**Status:** Draft
**Versão:** 1.0
**Data:** 2026-04-28
**Autor:** Claude Code (Opus 4.7) + Eder Junior
**Epic:** EPIC-0064
**ADR:** ADR-0016

> Spec funcional canônica do modelo de capability-driven composition. Acompanha ADR-0016 e Rule 28.
> Derivada do plano executivo da sessão Plan Mode (`~/.claude/plans/quero-melhorar-isso-ainda-nifty-seal.md`, artefato local do usuário, não commitado). Esta SPEC é a versão canônica e auto-suficiente das decisões funcionais.

---

## 1. Glossário

| Termo | Definição |
| :--- | :--- |
| **Capability** | Asserção declarativa, atômica e versionada sobre o que o projeto-alvo "tem" ou "faz". Identificada por `<category>.<subcategory>.<atomic>` (e.g., `data.database.postgres`). |
| **Profile** | Macro/atalho que expande para um conjunto de atomics (e.g., `profile.spring-rest-api-postgres`). Não pode ser referenciado em frontmatter; apenas no YAML do projeto. |
| **Atomic** | Capability indivisível, mapeia 1:1 a um conceito técnico. |
| **Composite** | Agrupa atomics via `requires`; pode ser referenciada em frontmatter. |
| **Resolver** | Componente Java que recebe `ProjectConfig`, expande profiles, valida grafo (mutex/cycle/prereq), produz `ResolvedCapabilitySet` imutável. |
| **Composer** | Componente Java que recebe `ResolvedCapabilitySet` + scan de artefatos, filtra por frontmatter e produz `.claude/`. |
| **Fragment** | Arquivo sob `<artifact-parent>/fragments/` que NÃO gera output próprio; é incorporado ao artifact-pai pelo `CompositionEngine`. |
| **Slot** | Marcador `{{ slot: <name> }}` em arquivo composable; recebe fragmentos ordenados por `fragment-order`. |
| **Pruning** | Remoção de artefatos cuja capability requerida não está ativa. |

---

## 2. Modelo de Capability

### 2.1 Estrutura canônica de uma capability

```yaml
# capabilities/data/database/postgres.yaml
id: data.database.postgres
category: data
kind: atomic                      # atomic | composite | profile
version: "16"                     # versão do recurso
status: stable                    # experimental | stable | deprecated
parameters:
  schema-migration-tool:
    type: enum
    values: [flyway, liquibase, none]
    default: flyway
  pool-size:
    type: int
    default: 10
requires:
  - language.jvm OR language.python OR language.node
provides:
  - data.transactional-store
  - data.sql-engine
excludes:
  - data.database.dynamodb
  - data.database.cassandra
tags: [persistence, oltp, sql]
```

### 2.2 Taxonomia top-level

`language`, `framework`, `interface`, `data`, `messaging`, `architecture`, `observability`, `deployment`, `security`, `compliance`, `testing`, `cloud`, `governance`. Listas fechadas; novos namespaces requerem revisão de ADR-0016.

### 2.3 Layout no source-of-truth

```
java/src/main/resources/capabilities/
  _index.yaml                     # manifest: ID → path
  language/{java,python,...}.yaml
  framework/{spring-boot,quarkus,...}.yaml
  data/database/{postgres,mongo,...}.yaml
  ...
  profiles/
    spring-rest-api-postgres.yaml
    java-cli-picocli.yaml
```

Diretório por categoria — merge-friendly, paralelizável, escalável para 200+ capabilities.

---

## 3. Schema YAML do projeto-alvo (v3.0)

```yaml
schema-version: "3.0"

identity:
  name: my-service
  language: java
  framework: spring-boot

# OPÇÃO A — atalho via profile
profile: spring-rest-api-postgres

# OPÇÃO B — declarativo direto (override/complemento)
capabilities:
  enable:
    - data.database.postgres:
        version: "16"
        schema-migration-tool: flyway
    - messaging.kafka:
        version: "3.7"
    - observability.tracing.otel
    - compliance.pci-dss
  disable:                         # opt-out fino
    - testing.e2e.cypress
    - skill.x-review-perf          # também desabilita artefato específico

governance:
  adr-required: true
  audit-gate: strict
```

`capabilities.disable` aceita IDs de capabilities OU pseudo-IDs de artefatos (`skill.<name>`, `kp.<name>`, `agent.<name>`).

---

## 4. Contrato Universal de Frontmatter (v3.0)

Toda skill/rule/KP/agent/hook/template DEVE declarar:

```yaml
---
name: x-review-db
description: Database specialist review
visibility: public                    # public | internal | shared
platform: claude-code                 # claude-code | shared

requires-capabilities:                # AND lógico
  - data.database.*                   # glob match
requires-any:                         # OR lógico (opcional)
  - interface.rest
  - interface.graphql
excludes-capabilities:                # nenhuma pode estar ativa (opcional)
  - data.storage.flat-file
parameters-from:                      # variáveis Pebble (opcional)
  - data.database.name
  - framework.name

# Apenas se artefato é polimórfico:
fragment-slots:                       # declara que recebe fragmentos
  - slot: review-specialist
    ordering: fragment-order
fragment-slot:                        # declara que ESTE arquivo é fragmento
  slot: review-specialist
  fragment-id: db
  fragment-order: 30
---
```

Schema validador: `governance/schemas/frontmatter-3.0.json`.

### 4.1 Semântica de matching

| Sintaxe | Significado |
| :--- | :--- |
| `data.database.postgres` | Match exato |
| `data.database.*` | Glob: qualquer atomic sob essa raiz (não recursivo) |
| `data.database.**` | Glob recursivo |
| `requires-capabilities: []` ou ausente do frontmatter | Universal (sempre entra) — ausência só permitida com `audit-exempt` |
| `requires-any: [a, b]` | OR explícito |
| Mini-DSL `(a AND b) OR NOT c` | Reservado para casos exóticos (<5%); implementação opcional Phase 1.5 |

---

## 5. Resolver — Algoritmo

### 5.1 Pipeline lógico

```
ProjectConfig YAML (v3.0)
   │
   ▼
ProfileExpander          ─ expande profile.X em set de atomics
   │
   ▼
CapabilityRegistry.load() ─ carrega definitions de capabilities/**
   │
   ▼
CapabilityGraph builder   ─ DAG: nodes=capabilities, edges=requires
   │
   ▼
TransitiveClosure         ─ resolve requires recursivamente
   │
   ▼
MutexValidator            ─ verifica excludes; falha estrutural com erro
   │
   ▼
PrerequisiteValidator     ─ valida que todos requires foram satisfeitos
   │
   ▼
CycleDetector (Tarjan SCC) ─ DAG check
   │
   ▼
ResolvedCapabilitySet     ─ output imutável (Set<ActiveCapability>)
```

### 5.2 Erros estruturados

```
CapabilityError sealed:
  CycleDetected { path: List<CapabilityId> }
  MutexConflict { active: CapabilityId, conflicts-with: CapabilityId, introduced-by: String }
  MissingPrerequisite { needed-by: CapabilityId, missing: CapabilityId }
  UnknownCapability { id: String }
  AmbiguousProfile { profile-id: String, candidates: List<String> }
```

Erros são lançados como exceções tipadas pelo `CapabilityResolver`; CLI traduz em mensagens human-readable com `resolution:` (sugestão de correção).

---

## 6. Composer — Pipeline

```
ResolvedCapabilitySet (imutável)
   │
   ▼
ArtifactScanner          ─ varre targets/, descobre todos .md/.sh
   │
   ▼
FrontmatterParser        ─ Jackson YAML para cada arquivo
   │
   ▼
CapabilityMatcher        ─ filtra por requires/excludes/disable
   │   (drop se não bater)
   ▼
CompositionPlanner       ─ resolve fragment-slots (coleta fragments candidatos, ordena)
   │
   ▼
CompositionEngine        ─ substitui {{ slot: X }} e {{ #each fragments.Y }}
   │
   ▼
PebbleRenderer           ─ substitui {{ var }} usando context.capabilities
   │
   ▼
OutputWriter             ─ grava .claude/ + CAPABILITY-MANIFEST.md
```

`CAPABILITY-MANIFEST.md` lista capabilities ativas + hashes dos artefatos gerados (auditável).

---

## 7. Three-Level Templating

| Nível | Sintaxe | Quem processa | Ordem |
| :--- | :--- | :--- | :--- |
| Composer | `{{ slot: X }}`, `{{ #each fragments.X }}` | `CompositionEngine` | 1º (build-time) |
| Pebble | `{{ var }}`, `{% if %}` | `TemplateEngine` (Pebble 3.2.2) | 2º (build-time) |
| LLM placeholder | `{{UPPER_SNAKE_CASE}}` | Claude no runtime | Mantido literal |

Discriminação sintática:
- `^\{\{ slot:`, `^\{\{ #each fragments\.` → Composer
- `^\{\{[A-Z_][A-Z0-9_]*\}\}$` → LLM placeholder (Pebble vê literal via `{% verbatim %}` automático)
- `^\{\{ [a-z]` → Pebble

`PebbleSafeRenderer` faz pre-flight regex que falha se LLM placeholder foi processado por engano.

---

## 8. Casos de Uso Canônicos

### 8.1 Java CLI Picocli sem DB

**Input YAML:**
```yaml
schema-version: "3.0"
identity: { name: code-formatter, language: java, framework: picocli }
profile: java-cli-picocli
```

**Capabilities ativas:** `language.java`, `framework.picocli`, `interface.cli`, `testing.smoke`.

**Output esperado:**
- Inclui: `x-git-commit`, `x-code-format`, `x-test-tdd`, `x-review` (fragmentos `qa.md`, `perf.md` apenas), agentes `architect`, `tech-lead`, `qa-engineer`, KPs `coding-standards`, `testing`, `architecture`.
- **Exclui:** `data-management/`, `database-patterns/`, `data-modeling/`, `database-engineer.md`, `x-review-db`, `x-review-devops`, `x-review-events`, `messaging-patterns/`, `stack-patterns/quarkus/`, `stack-patterns/spring/` (apenas `picocli/`).

### 8.2 Spring Boot REST + Postgres + Kafka

**Input YAML:**
```yaml
schema-version: "3.0"
identity: { name: payment-processor, language: java, framework: spring-boot }
profile: spring-rest-api-postgres
capabilities:
  enable:
    - messaging.kafka: { version: "3.7" }
    - compliance.pci-dss
```

**Capabilities ativas:** `language.java`, `framework.spring-boot`, `interface.rest`, `data.database.postgres`, `messaging.kafka`, `compliance.pci-dss`, `testing.{smoke,contract,e2e}`, `observability.tracing.otel`.

**Output esperado:**
- `x-review` composto com 6 especialistas (`qa.md`, `perf.md`, `db.md`, `api.md`, `event.md`, `compliance.md`).
- KPs `database-patterns/postgres/` (não `mongo/`), `messaging-patterns/kafka/`, `pci-dss-requirements/`.
- Rule 09 inclui fragmento `data-migration` (Postgres-flavored).
- Rule 06 inclui fragmento `pci-dss`.
- Agentes `database-engineer`, `event-engineer`, `api-engineer`, `compliance-auditor`.
- **Exclui:** `stack-patterns/quarkus/`, `stack-patterns/picocli/`, `database-patterns/mongo/`.

---

## 9. Audits (Rule 26)

| Script | Valida | Exit codes |
| :--- | :--- | :--- |
| `audit-capability-coverage.sh` | Toda skill/rule/KP/agent/hook/template em diretório condicional declara `requires-capabilities` | 0 / 1 / 2 |
| `audit-capability-graph.sh` | Sem ciclos, mutex simétrico, sem capability órfã | 0 / 1 / 2 / 3 |
| `audit-frontmatter-schema.sh` | 100% dos `.md` válidos contra schema 3.0 | 0 / 1 |
| `audit-output-pruning.sh` | Para cada perfil, output sem artefato com capability inativa | 0 / 1 |
| `audit-fragment-coherence.sh` | Toda referência a fragment existe em disco | 0 / 1 |
| `audit-capability-determinism.sh` | 2× compose = mesmo SHA bytewise | 0 / 1 |

Todos integram via Rule 26 (catalog em `docs/audit-gates-catalog.md`).

---

## 10. Out of Scope (v1)

- AI-based capability inference de código (heurística manual + skill `/x-frontmatter-migrate` é suficiente).
- Hot-reload de capabilities em runtime.
- Diferenciação de capabilities por ambiente (dev/staging/prod) — feature de v2.
- Mini-DSL completa AND/OR/NOT — apenas `requires` (AND implícito) e `requires-any` (OR) na v1.
- Plug-in de capability externa (third-party) — feature de v2.

---

## 11. Migração de v2 para v3

Sem retrocompatibilidade. Phase 2 do epic migra os 182 artefatos de uma vez (12 PRs por categoria). Goldens v2 deletados em Phase 7. Downstream consumers re-geram com schema v3.0; CHANGELOG `[Breaking]` + bump major.

---

## 12. Open Questions

- **OQ-1:** Mini-DSL `AND/OR/NOT` — implementar na v1 ou diferir? Default: diferir (Phase 1.5 só se >5% dos artefatos precisarem).
- **OQ-2:** `capability list` CLI deve suportar `--graph` (Mermaid output)? Default: sim, mas em story dedicada da Phase 1 (não bloqueia).
- **OQ-3:** Como capabilities lidam com versões — `data.database.postgres@16` vs parâmetro `version`? Default: parâmetro (mais flexível).
- **OQ-4:** Templates `_TEMPLATE-*.md` precisam frontmatter `requires-capabilities`? Default: sim, mas `requires-capabilities: []` se sempre incluídos.
