# Épico: Product-First Lifecycle & Planning C4 Model

**Autor:** Product Engineering / Platform Excellence
**Data:** 2026-04-29
**Versão:** RA9 v2
**Status:** Em Refinamento

> **Status Transitions (Rule 22 — lifecycle-integrity):**
> Artifacts lifecycle-controlados (Story/Task) usam o enum canônico
> `Pendente | Planejada | Em Andamento | Concluída | Falha | Bloqueada`.
> O campo Status do Épico aqui é documental e reflete o estado
> agregado das histórias filhas. Transições permitidas do enum:
> `Pendente → Planejada | Em Andamento | Falha | Bloqueada`;
> `Planejada → Em Andamento | Falha | Bloqueada`;
> `Em Andamento → Concluída | Falha | Bloqueada`;
> reabertura `Concluída → Em Andamento` (via `x-reconcile-status --apply`) e
> `Falha → Pendente`; `Bloqueada → Pendente | Planejada | Em Andamento | Falha`.
> Ver [`.claude/rules/22-lifecycle-integrity.md`](../../rules/22-lifecycle-integrity.md).

---

## 0.5 Cross-Epic Dependencies

> **Cross-Epic Dependency Awareness (EPIC-0076)**
> Epic-level dependencies are declared in this section and used by the implementation workflow as a Phase 0.5 gate.
> If any dependency's `expectedStatus` is not met, `x-implement-epic` aborts synchronously with exit 1.

### Blocked By Epics

| Epic ID   | Title                              | Expected Status @ Start | Reason / Surface Touched                             |
| --------- | ---------------------------------- | ----------------------- | ---------------------------------------------------- |
| EPIC-0064 | Capability-Driven Composition      | Concluída               | Capabilities v3.0 base + product-capability nesting |
| EPIC-0069 | Refinement & DoR Gate              | Concluída               | `enforce-refinement-gate.sh` e extensão DoR já entregues; Phase 2 depende dessa base pronta |

### In-Flight Reference Allowance

| Epic ID   | Title                              | Expected Status | Reason                                              |
| --------- | ---------------------------------- | ---------------- | --------------------------------------------------- |
| EPIC-0065 | Feature Creation Chain Refactor    | Backlog          | Gate explícito de anti-colisão em `story-0077-0003`; `story-0077-0011` permanece bloqueada até o naming contract estar fechado |
| EPIC-0070 | Value-Driven Templates v2          | Concluída        | Template Epic v3 refator sobre v2 (Phase 1)        |
| EPIC-0072 | Comprehensive Test Strategy        | Concluída        | RNF table popula de quality.* YAML (Phase 7)       |

### Blocks (informational, derived)

| Epic ID   | Reason                                                  |
| --------- | ------------------------------------------------------- |
| EPIC-0078 | Product-First skills + templates definem contracts    |
| EPIC-0079 | Feature-to-Epic hierarchy refactor usa PRODUCT base    |

---

## 1. Contexto & Escopo

**Chave Jira:** —

O épico Product-First Lifecycle & Planning C4 Model estabelece a hierarquia Ideation → Product → Capability → Feature → Epic → Story → Task como fundação canônica da plataforma. Define templates + skills + artefatos persistentes obrigatórios em cada nível de planejamento. Introduz C4 Model como contrato visual inviolável (C1: System Context, C2: Container, C3: Component, C4: Code) para validação de arquitetura em plans. Refoca QA em AC measurability, error-message catalog, response-time SLO, success metrics e E2E validação. Implementa RNF como entrada obrigatória com gate no-relax na refinement. Atualiza flowVersion para "5" em todos os artefatos de planejamento.

### 1.1 Problema Observável

Hoje a plataforma consegue planejar bem do nível **Epic → Story → Task**, mas não possui uma cadeia persistente e validável que conecte a intenção inicial de produto aos artefatos técnicos gerados. O efeito observável é que decisões de arquitetura, qualidade e segurança entram tarde demais no fluxo, gerando retrabalho e correções espalhadas ao longo do planejamento.

**Evidências operacionais independentes (observadas em épicos recentes concluídos, externas a este épico):**
- gaps de C4, RNF e security review foram descobertos tardiamente em épicos recentes, já depois da decomposição técnica, forçando retrofit em critérios de aceite, stories e gates;
- scripts e hooks de governança foram estendidos de forma corretiva em ondas sucessivas, sinal de que a camada preventiva de planning ainda nasce incompleta;
- a ausência de telemetria upstream antes de `flowVersion: "5"` impediu quantificar essas falhas de forma estruturada; este épico existe justamente para tornar essa medição objetiva daqui para frente.

**Evidências observáveis no próprio backlog deste épico:**
- são necessárias **5 refatorações de skills de planning** (`story-0077-0011` até `story-0077-0015`) para inserir C4, herança de RNF e validações que hoje não existem na origem do fluxo;
- são necessárias **4 stories de audit/validação** (`story-0077-0016` até `story-0077-0019`) para cobrir lacunas de integridade estrutural, RNF e pentest que hoje dependem de checagem tardia;
- `story-0077-0001` existe apenas para dar **ancoragem normativa** ao modelo, sinal de que o framework atual não formaliza essa cadeia de ponta a ponta;
- `story-0077-0028` precisa de um smoke test E2E cobrindo ideação → produto → capability → feature → epic → story → tasks porque esse fluxo ainda não é validado como contrato único.

Em termos operacionais, a dor é: **rastreabilidade upstream incompleta, descoberta tardia de gaps de C4/RNF/security e excesso de retrofits para alinhar planejamento, arquitetura e governança**.

### 1.2 Personas Afetadas

As personas impactadas por este épico são mais amplas do que um único executor técnico:

- **Primary persona — Product Engineering / Platform Excellence:** time que mantém o framework de planejamento e absorve hoje o custo operacional de corrigir planning, validações e governança tarde demais;
- **Secondary persona — Product Managers e Staff Engineers / Tech Leads:** responsáveis por transformar ideação em produto, capability e feature sem perder contexto de negócio, critérios de qualidade e dependências;
- **Secondary persona — Arquitetos e QA/Security leads:** precisam validar C4, RNFs, smoke coverage e requisitos de segurança antes que a decomposição chegue ao nível de story/task.

### 1.3 Hipótese & OKRs

**Hipótese de Valor**

Se formalizarmos a cadeia **Ideation → Product → Capability → Feature → Epic → Story → Task** com artefatos persistentes, C4 Mermaid obrigatório, herança de RNFs e pentest always-on desde o início do planejamento, então reduziremos retrabalho e falhas tardias de governança, arquitetura e qualidade, **porque** escopo, arquitetura, critérios de teste e controles de segurança passarão a ser validados antes da criação das stories e não apenas nas fases finais.

Essa hipótese é fundamentada na evidência operacional qualitativa observada em épicos recentes: o padrão de descobertas tardias e retrofits de governança é recorrente e independente do backlog deste épico. A confirmação quantitativa passa a existir com os gates e eventos de telemetria introduzidos por `flowVersion: "5"`.

**OKRs / KPIs do Épico**

| Objetivo / KPI | Baseline | Target | Horizon | Measurement method |
| :--- | :--- | :--- | :--- | :--- |
| **Upstream traceability completeness** — porcentagem de iniciativas piloto com cadeia completa ideação → produto → capability → feature → epic → story persistida | 0% das iniciativas novas usam cadeia persistente completa; o fluxo atual nasce em epic/story | 100% do fluxo piloto EPIC-0077 gera e persiste toda a cadeia upstream | Até `story-0077-0028` | `Epic0077ProductFirstSmokeIT` + presença de artefatos em `ai/products`, `ai/capabilities`, `ai/features` e `ai/epics` |
| **Late planning gate failures** — quantidade de falhas críticas por ausência de C4/RNF/product lineage no fluxo piloto | Baseline qualitativa: épicos recentes evidenciam gaps de C4/RNF/security detectados tardiamente e scripts corretivos adicionados após a decomposição; quantificação sistemática começa com `flowVersion: "5"` | 0 falhas críticas de C4/RNF/lineage no fluxo smoke do épico | Fases 6 e 7 | `x-internal-c4-validate`, `enforce-refinement-gate.sh`, `audit-*.sh`, smoke E2E e eventos de telemetria em `events.ndjson` |
| **Security planning coverage** — porcentagem de features do fluxo piloto com pentest e critérios de segurança planejados antes do merge | Pentest e security review ainda não são parte explícita e sempre-ligada do fluxo base | 100% das features do piloto com pentest plan e security controls declarados | Até `story-0077-0023` | output de `x-plan-story` v5 + `story-0077-0023` + evidências de pentest gate |
| **Automated acceptance coverage** — quantidade de cenários smoke cobrindo a cadeia completa | 0 cenários smoke cobrindo a cadeia completa ideação → tasks | 4 cenários happy-path executando sem erro | Até `story-0077-0028` | `Smoke.yaml` + `Epic0077ProductFirstSmokeIT` |

### 1.4 Escopo Incluído

- 4 templates novos (Product, Capability, Feature, RNF-Validation)
- 1 template refatorado (Epic v3)
- 4 skills novas (x-create-product, x-create-capability, x-create-feature, x-promote-ideation)
- 5 skills refatoradas (x-epic-create, x-story-create, x-plan-architecture, x-plan-task, x-plan-story)
- 2 agents refatorados (planning-refinement, planning-decompose)
- 4 scripts audit (product-validate.sh, capability-validate.sh, feature-validate.sh, rnf-audit.sh)
- 10 SOLID rules (RULE-001 até RULE-010)
- 28 stories distribuídas em 7 phases (0-7, seriais)
- Quality gates (DoR 6-item, DoD 28-story + cobertura + audits)

### 1.5 Fora do Escopo

- Refatoração de skill-invoke ou runtime executor
- Mudança em contrato de artefatos persistentes versionados (v4)
- Implementação de tooling CI/CD (vide EPIC-0088)
- Data migration de ideations/products existentes (vide EPIC-0089)

### 1.6 Riscos Estratégicos

| Tipo | Risco | Impacto | Mitigação |
| :--- | :--- | :--- | :--- |
| Produto | Times continuarem iniciando planejamento direto em epic/story e ignorarem Product/Capability/Feature | O fluxo product-first vira exceção e não padrão corporativo | Rule 19 v5 normativa + skills `x-promote-ideation` / `x-create-*` + smoke E2E obrigatório |
| Produto | PMs e tech leads perceberem a hierarquia nova como burocracia adicional | Baixa adoção e retorno ao planejamento manual | Automatizar geração dos artefatos e provar ganho com `Epic0077ProductFirstSmokeIT` |
| Técnico | Ausência ou inconsistência de diagramas C4 Mermaid quebrar DoR e refactors de planning | Planning bloqueado nas fases 4-6 | `c4-model-architecture.md` + `x-internal-c4-validate` + gates de C4 completeness |
| Técnico | RNFs, pentest e critérios de segurança continuarem entrando tarde no fluxo | Rejeições em refinement, findings tardios e retrabalho em stories | RNF root table obrigatória + `quality.pentest-always-on` + gates de refinement e smoke |

### 1.7 Referências e Anexos

- [Planning Standards KP (RA9 v2)](../../rules/planning-standards-kp.md)
- [Rule 22: Lifecycle Integrity](../../rules/22-lifecycle-integrity.md)
- [C4 Model Architecture](../../rules/c4-model-architecture.md)
- [C4 System Context Bootstrap](./c4-system-context.md)
- [C4 Containers Bootstrap](./c4-containers.md)
- [EPIC-0064: Capability-Driven Composition](../epic-0064-capability-driven-composition/epic-0064-capability-driven-composition.md)

---

## 2. Packages (Hexagonal)

### Domain Layer

```
domain/products/                     # Product entity, factory, repository interface
domain/capabilities/                 # Capability entity, factory, repository interface
domain/features/                     # Feature entity, factory, repository interface
domain/planning/c4-model/            # C4-Diagram, C4-Container, C4-Component, C4-Code entities
domain/planning/rnf-validation/      # RNF entity, validator interface
```

**Impacto:** 5 packages novos no domain. Zero mudanças em domínios existentes.

### Application Layer

```
application/products/                # ProductService, ProductFactory impl
  ├── create/
  ├── read/
  └── validate/

application/capabilities/            # CapabilityService, CapabilityFactory impl
  ├── create/
  ├── read/
  └── validate/

application/features/                # FeatureService, FeatureFactory impl
  ├── create/
  ├── read/
  └── validate/

application/planning/c4-validation/  # C4DiagramValidator, C4ContainerValidator, etc
  ├── consistency/
  ├── hierarchy/
  └── notation/

application/planning/rnf-validation/ # RNFGate, RNFEnforcer, RNFValidator
  ├── gate/
  ├── schema/
  └── audit/
```

**Impacto:** 8 packages novos em application. Refatoração de planning/* para descentralizar responsabilidades.

### Adapter Inbound (CLI)

> Layout real do repositório: comandos CLI em `dev.iadev.cli`; assets de skill em `java/src/main/resources/targets/claude/skills/<skill-id>/SKILL.md` (montados em `.claude/skills/` pelo build).

```
java/src/main/java/dev/iadev/cli/
  ├── XCreateProductCommand.java        # NEW
  ├── XCreateCapabilityCommand.java     # NEW
  ├── XCreateFeatureCommand.java        # NEW
  ├── XPromoteIdeationCommand.java      # NEW
  ├── XEpicCreateCommand.java           # REFACTOR — deprecate steps 0-2
  ├── XStoryCreateCommand.java          # REFACTOR — validate Product/Capability base
  ├── XArchPlanCommand.java             # REFACTOR — add C4 mandatory input
  ├── XTaskPlanCommand.java             # REFACTOR — inherit C4 from epic
  └── XStoryPlanCommand.java            # REFACTOR — add RNF validation

java/src/main/resources/targets/claude/skills/
  ├── x-create-product/SKILL.md         # NEW
  ├── x-create-capability/SKILL.md      # NEW
  ├── x-create-feature/SKILL.md         # NEW
  ├── x-promote-ideation/SKILL.md       # NEW
  ├── x-epic-create/SKILL.md            # REFACTOR
  ├── x-story-create/SKILL.md           # REFACTOR
  ├── x-plan-architecture/SKILL.md              # REFACTOR
  ├── x-plan-task/SKILL.md              # REFACTOR
  └── x-plan-story/SKILL.md             # REFACTOR
```

**Impacto:** 4 skills novas, 5 refatoradas. Zero breaking changes em invoke interface.

### Adapter Outbound

> Não há `adapter/outbound/` separado: integrações outbound vivem em `dev.iadev.infrastructure`. Agentes de planning são assets em `java/src/main/resources/targets/claude/agents/{core,conditional}/`.

```
java/src/main/resources/targets/claude/agents/
  ├── core/qa-engineer.md               # REFACTOR (story-0077-0018)
  ├── core/pentest-engineer.md          # PROMOTE de conditional/ (story-0077-0019)
  └── core/{architect,product-owner,tech-lead,security-engineer,
              performance-engineer,sre-engineer}.md  # READ-ONLY
```

**Impacto:** 1 charter refatorado (qa-engineer), 1 promovido (pentest-engineer); zero novos agents.

### Infrastructure

> Source-of-truth de scripts em `java/src/main/resources/targets/claude/scripts/<stack>/` (montado em `.claude/scripts/` pelo `ScriptsAssembler`); templates em `java/src/main/resources/shared/templates/`.

```
java/src/main/resources/targets/claude/scripts/<stack>/
  ├── audit-product-upstream.sh         # NEW — verify product hierarchy + links
  ├── audit-c4-completeness.sh          # NEW — C4 Context+Container+Component+Code coverage
  ├── audit-rnf-gates.sh                # NEW — RNF table 100% coverage + no-relax
  └── audit-pentest-coverage.sh         # NEW — pentest plan presence per story

java/src/main/resources/shared/templates/
  ├── _TEMPLATE-IDEATION.md             # NEW — ideation artifact v1
  ├── _TEMPLATE-PRODUCT.md              # NEW — product artifact v1 (RNFs Root mandatory)
  ├── _TEMPLATE-CAPABILITY.md           # NEW — capability artifact v1 (no-relax)
  ├── _TEMPLATE-FEATURE.md              # NEW — feature artifact v1
  └── _TEMPLATE-EPIC.md                 # REFACTOR — v3 (Source Feature + Inherited RNFs)
```

**Impacto:** 5 templates (4 novos + 1 refatorado), 4 scripts audit Camada 2. Output gerado em `.claude/scripts/` via `ScriptsAssembler.AUDIT_SCRIPTS` — registrado no pipeline de geração, não criado em `ci/audit/` ad-hoc.

**Dependency direction:** `cli → application → domain` (inward only).
Domain MUST NOT import CLI or framework code (Rule 04).

---

## 3. Contratos & Endpoints

### Skills Inbound (UnicodeScript CLI)

| Skill ID | Input Contract | Output Contract | Descrição |
| :--- | :--- | :--- | :--- |
| `x-create-product` | `--title <str> --vision <str> [--roadmap-url <str>]` | `product.yaml` (v1 schema) | Criar nova Product com validação Ideation link |
| `x-create-capability` | `--product-id <ID> --title <str> --description <str>` | `capability.yaml` (v1 schema) | Criar nova Capability sob Product |
| `x-create-feature` | `--capability-id <ID> --title <str> --ac <str>` | `feature.yaml` (v1 schema) | Criar nova Feature sob Capability |
| `x-promote-ideation` | `--ideation-id <ID> [--product-create]` | `product.yaml` (v1 schema) | Promover Ideation → Product (ou fail se existe) |
| `x-epic-create` | `--feature-id <ID> --title <str> --c4-system <str>` | `epic-0XXX-*.md` (v3 schema + C4) | Criar Epic, validar C4 system diagram obrigatório |
| `x-story-create` | `--epic-id <ID> --title <str> --rnf <yaml>` | `story-XXXX-YYYY.md` (v4 schema + RNF) | Criar Story, validar RNF schema against epic gate |
| `x-plan-architecture` | `--epic-id <ID> --c4-container <str>` | `arch-plan-epic-XXXX.md` | Validar C4 Container obrigatório antes de arch plan |
| `x-plan-task` | `--story-id <ID> --rnf-inherit <yaml>` | `task-plan-story-XXXX-YYYY.md` | Herdar RNF de story, validar SLO + success metrics |
| `x-plan-story` | `--story-id <ID> [--verify-rnf]` | `story-plan-XXXX-YYYY.md` | RNF validation optional (default), escalate to epic if fails |

### Eventos/Commands (Internal Planning Layer)

| Evento | Source | Target | Payload |
| :--- | :--- | :--- | :--- |
| `ideation.promoted` | `x-promote-ideation` | `planning-refinement` | `{ideation_id, product_id, timestamp}` |
| `product.created` | `x-create-product` | `planning-decompose` | `{product_id, title, vision}` |
| `feature.created` | `x-create-feature` | `planning-decompose` | `{feature_id, capability_id, title}` |
| `epic.c4-required` | `x-epic-create` | `x-plan-architecture` | `{epic_id, feature_id, c4_requirement}` |
| `story.rnf-validated` | `x-story-create` | `planning-gate` | `{story_id, epic_id, rnf_status}` |

---

## 4. Materialização SOLID

| ID | Título | Descrição |
| :--- | :--- | :--- |
| **[RULE-001]** | Hierarquia Product-First Obrigatória | Toda Feature DEVE estar sob Capability. Toda Capability DEVE estar sob Product. Toda Epic DEVE estar sob Feature. Toda Story DEVE estar sob Epic. Violation → planning-gate rejeita (exit 1). Prioridade: consistência estrutural > refatoração incremental. |
| **[RULE-002]** | Numeração de IDs Canônica | IDEATION (não numerado), PRODUCT-PP (2 dígitos), CAPABILITY-CC-CCC (produto-2dig + capability-3dig), FEATURE-CC-FFF (capability-id + feature-3dig), EPIC-XXXX (4 dígitos), STORY-XXXX-YYYY (epic + story-4dig), TASK-XXXX-YYYY-ZZ (story + task-2dig). Refactor de IDs em voo requer double-link durante transição. |
| **[RULE-003]** | C4 Model Obrigatório em Plans | Toda Epic DEVE conter C4 System diagram (C1) e C4 Container (C2) em arquivos `.md` dedicados com Mermaid. Toda Story DEVE referenciar C4 Container (C2). Código DEVE referenciar C4 Component/Code (C3-C4). Plan validation falha sem C4 linkage. |
| **[RULE-004]** | RNF Entrada Obrigatória com Gate No-Relax | Toda Story DEVE declarar RNF table (performance, availability, security). RNF gate enforce-refinement-gate.sh verifica 100% coverage. Relaxamento proibido (no --force override). DoR = 6 épicos pré-requisito + RNF schema v1 sem omissões. |
| **[RULE-005]** | QA Charter v5 — AC Measurability + SLO Harness | AC DEVE ser observável (dado X, quando Y, ENTÃO métrica Z < threshold). Error-message catalog obrigatório (código HTTP + mensagem padrão). Response-time SLO declarado em task-plan. E2E scenario per Story. Success metrics in terms de negócio (conversion, retention, MTTR). |
| **[RULE-006]** | Pentest Unconditional Core | Toda Epic impactando auth/data DEVE submeter pentest (auth bypass, IDOR, SSRF, injection, race conditions). Output: pentest-checklist-story-XXXX-YYYY.md. Falha pentest = story não entra Concluída (gates em x-internal-verify-story). |
| **[RULE-007]** | O QUE (Product/Capability/Feature) vs O COMO (Epic/Story) | Product = problema de negócio. Feature = solução de negócio. Epic = como implementar. Story = task técnica. Plan DEVE mencionar trade-off entre o quê e como. Decision Rationale mínimo de 4 linhas. |
| **[RULE-008]** | flowVersion "5" em Todos Artefatos Planning | Epic v3, Story v4, Task v2, Product v1, Capability v1, Feature v1, RNF-Validation v1. Cada artifact contem `flowVersion: "5"` em frontmatter. Mismatch bloqueia planning-gate. |
| **[RULE-009]** | Decision Rationale Split (Produto / Execução / Local) | CADA decisão DEVE ser categorizada: Produto (O QUE — visível ao PM), Execução (COMO — visível ao tech lead), Local (implementation detail — visível ao dev). 4 linhas mínimo por decisão. Produto decisions bloqueiam refactoring, execução decisions bloqueiam deploys. |
| **[RULE-010]** | Coordenação EPIC-0065 — Feature Creation Chain Refactor | `story-0077-0003` fecha o naming contract com EPIC-0065 antes de `story-0077-0011`. Enquanto isso, `x-create-feature` é o nome canônico no escopo deste épico; o rename de EPIC-0065 fica encapsulado por alias backward-compatible. |

---

## 5. Quality Gates

### Global Definition of Ready (DoR)

- **Épicos pré-requisito:** EPIC-0064 Concluída (Capability v3.0 base), EPIC-0069 Concluída (DoR gate infra)
- **Linguistics:** Todas seções preenchidas (zero `{{PLACEHOLDER}}`). Nenhum `TODO` sem assignee + ETA
- **RNF Schema:** Epic declare RNF table v1 com mínimo 4 linhas (perf, avail, security, compliance)
- **C4 Notation:** `c4-system-context.md` (C1) e `c4-containers.md` (C2) presentes no diretório do épico com blocos `mermaid` válidos. Containment hierarchy clara entre C1, C2 e C3
- **Decision Rationale:** Mínimo 1 decisão, formato 4-linhas (Decisão / Motivo / Alternativa / Consequência)
- **Cross-Epic Links:** Todos épicos dependentes listados em seção 0.5 com expectedStatus. In-flight allowances explícitas

### 5.1.1 EPIC-0072 Conditional Quality Gates

| Gate | Enabled | Rationale |
| :--- | :---: | :--- |
| `x-test-performance` | ✅ `true` | O épico já declara SLOs numéricos rígidos e thresholds objetivos para CLI e validações batch |
| `x-test-mutation` | ❌ `false` | O escopo é majoritariamente planning/governance, com smoke/audit/pentest já cobrindo corretude comportamental |
| `x-test-contract` | ❌ `false` | O épico não introduz superfície de contrato de protocolo externo; schemas internos são cobertos pelos audit scripts declarativos |

### Global Definition of Done (DoD)

- **Cobertura:** ≥ 95% Line, ≥ 90% Branch (absolute gate via x-execute-tests). Delta < 2% entre main e branch
- **Testes Automatizados:** 28 stories × 1 acceptance test Gherkin mínimo por story = 28 testes passando. Double-Loop TDD: Gherkin AC outer loop, TPP unit tests inner loop. Gate aplicado em cada merge de story
- **Smoke Tests:** suíte happy-path cobrindo **ideation → product → capability → feature → epic → story → tasks**. `Smoke.yaml` propaga ao concluir story-0077-0019 e executa na Phase 6 antes de golden fixtures. Cenários negativos ficam fora de smoke e pertencem a acceptance, integration, audit e pentest
- **Golden Fixtures:** Subset de 920 fixtures regenerado (product-validate.sh, capability-validate.sh, feature-validate.sh outputs)
- **Relatório de Cobertura:** Coverage.xml + HTML report linked em final story report. Trend analysis vs EPIC-0064 baseline
- **Documentação:** CHANGELOG.md Updated (28 new skills/scripts/rules). ADR registry de 10 SOLID rules gerado. _TEMPLATE-*.md versionados (v1/v3)
- **Persistência:** Todos product.yaml, capability.yaml, feature.yaml validos contra JSON schema. Zero orphaned FKs (product_id not in products/*)
- **Performance:** Product create < 100ms. Epic plan gen < 5s. Batch validate 100 epics < 2s. No regression vs EPIC-0064 baseline (use x-analyze-telemetry-trends)
- **RNF Validation:** 100% de stories com RNF table declarada. enforce-refinement-gate.sh PASS em 28 stories. Zero RNF relaxamento via --force
- **Pentest:** Mínimo 1 pentest-checklist gerado (auth, IDOR). Core paths (product create, feature promote) PASS pentest gate
- **Audit Scripts:** scripts canônicos em `java/src/main/resources/targets/claude/scripts/<stack>/audit-*.sh` todos PASS em golden fixtures. Camada 2 health checks integrados

---

## 6. Segurança

> Políticas de segurança que se aplicam a todas as histórias do épico.

| Área | Controle | Rule Âncora | Detalhes |
| :--- | :--- | :--- | :--- |
| Input validation | Whitelist product ID format (PRODUCT-PP, 01-99) | RULE-002, RULE-006 | Regex `/^PRODUCT-\d{2}$/`. Rejeitar strings contendo `../`, `%`, null bytes |
| Input validation | Whitelist capability ID format (CAPABILITY-CC-CCC) | RULE-002, RULE-006 | Regex `/^CAPABILITY-\d{2}-\d{3}$/`. SQL injection prevention via prepared statements |
| Authentication | Product create requer role ROLE_PRODUCT_OWNER | RULE-006 | OAuth2 por **active introspection** contra authorization server corporativo. TTL máximo do token: **15 minutos**. Deny anonymous e allowlist explícita de roles |
| Authentication | Feature promote requer role ROLE_FEATURE_LEAD | RULE-006 | Mesma política de active introspection + TTL 15 min. Auditoria imutável da ação com `who`, `what`, `when`, `resource_id`, `outcome`, `correlation_id` |
| Token delivery | CLI obtém token via OS keychain / credential helper (local) ou variável de ambiente injetada em CI | RULE-006 | `--token` como argumento posicional ou opção CLI é explicitamente proibido para evitar exposição em shell history e process listing |
| Sensitive data | RNF tables MUST NOT conter credentials/tokens | RULE-004, RULE-006 | Scanning rule: rejeitar `password`, `secret`, `key` em RNF schema |
| Data classification | `product.yaml`, `capability.yaml`, `feature.yaml` são classificados como `INTERNAL` por padrão | RULE-006 | Artefatos persistidos no repositório **não podem conter PII bruta**; se a entrada tiver dados pessoais, apenas sumário redigido pode ser persistido |
| Read authorization | RBAC de repositório como controle de leitura para artefatos INTERNAL | RULE-006 | Leitura restrita aos grupos Platform, Product, Architecture, QA e Security; artefatos INTERNAL não se destinam a distribuição pública |
| Audit immutability | Sink externo write-only com hash chaining por entrada é a trilha autoritativa | RULE-006 | Logs locais NDJSON são cópias de conveniência e não substituem a fonte de verdade em auditoria |
| Path operations | product.yaml sempre em ai/products/PRODUCT-PP/ dir | RULE-002, RULE-006 | Normalizar caminho com `Path.normalize()` + `toRealPath()` sob base canônica, bloqueando traversal e bypass por encoding (CWE-22) |
| Outbound URL handling | `--roadmap-url` em `x-create-product` | RULE-006 | Aceitar apenas `https://` e hosts aprovados; negar RFC1918, metadata endpoints e schemas não aprovados para prevenir SSRF |
| CSRF protection | x-create-product POST requires CSRF token | RULE-006 | State-based CSRF validation via skill middleware |
| Rate limiting | Product create API 10 req/min por user | RULE-006 | Redis-backed rate limiter. Return 429 se excedido |

### 6.1 Gatilhos de Compliance

- **LGPD (condicional):** aplica-se sempre que ideação, visão de produto, roadmap links ou anexos upstream contiverem dados pessoais, user research identificável ou exemplos reais de clientes. Nesses casos, a regra é **minimização e redação antes da persistência**; os artefatos canônicos do repositório não armazenam PII bruta.
- **Internal audit / SOC2-style control baseline:** aplica-se a toda ação privilegiada protegida por OAuth2 (`x-create-product`, `x-create-feature`, `x-promote-ideation`) e exige trilha de auditoria imutável com retenção operacional mínima de **12 meses**. A fonte de verdade é um sink externo write-only com hash chaining por entrada.
- **Token credential hygiene:** `--token` como argumento CLI é proibido; uso local via keychain/credential helper e CI via variável de ambiente dedicada.
- **Acesso a artefatos INTERNAL:** o repositório de planning e seus branches são restritos aos grupos Platform, Product, Architecture, QA e Security; revisão de acesso é semestral.
- **PCI/HIPAA:** não são acionados por este épico, porque o fluxo não introduz pagamento, PHI ou processamento clínico. Se um produto futuro herdar esse fluxo para domínio regulado, a capability correspondente deve elevar o baseline na fase de planning.

### 6.2 Escopo de Threat Modeling

**Boundaries em escopo**
- operador ↔ CLI/skills de planning;
- CLI ↔ authorization server corporativo (active introspection);
- CLI ↔ filesystem versionado (`ai/products`, `ai/capabilities`, `ai/features`, `ai/epics`);
- CLI ↔ GitHub/Jira/links externos aprovados;
- CLI ↔ OS keychain / credential helper;
- CLI / CI ↔ sink externo de auditoria.

**Ameaças priorizadas**
- uso indevido de token, role spoofing ou token stale;
- exposição de credenciais em shell history ou process listing via argumento CLI;
- SSRF via `--roadmap-url`;
- path traversal / escrita fora do diretório canônico;
- enumeração de IDs sequenciais e leitura indevida de artefatos;
- leitura indevida de artefatos INTERNAL por ator fora dos grupos autorizados;
- adulteração de artefatos gerados sem trilha de auditoria;
- adulteração ou deleção da trilha de auditoria local;
- ausência de pentest e critérios de segurança antes da criação das stories.

**Mitigações mandatórias**
- active introspection com TTL máximo de 15 min;
- tokens via keychain/credential helper no uso local e variável de ambiente dedicada em CI; `--token` é proibido;
- RBAC de repositório como controle de leitura de artefatos INTERNAL;
- sink externo write-only com hash chaining como trilha autoritativa;
- allowlist de roles e de hosts externos;
- normalização canônica de paths;
- pentest coverage obrigatório no fluxo piloto (`story-0077-0019` e `story-0077-0023`).

---

## 7. Observabilidade

> SLOs/SLIs e requisitos de observabilidade aplicáveis ao épico.

| Componente | SLO | Métrica | AlertThreshold |
| :--- | :--- | :--- | :--- |
| ProductService.create() | P99 < 100ms | latency_ms | > 150ms = critical |
| CapabilityService.create() | P99 < 50ms | latency_ms | > 100ms = warning |
| FeatureService.create() | P99 < 50ms | latency_ms | > 100ms = warning |
| RNFValidator.validate() | P99 < 200ms | latency_ms | > 500ms = critical |
| C4DiagramValidator.validate() | P99 < 300ms | latency_ms | > 1s = critical |
| enforce-refinement-gate.sh | P99 < 2s | latency_ms | > 5s = critical |
| product-validate.sh (batch 100) | P99 < 2s | throughput | > 50 items/sec |

**Health checks:** `/health/live` (liveness), `/health/ready` (readiness) — Rule 07.
**Correlation ID:** `X-Correlation-ID` propagated through all downstream calls (product create → capability create → RNF validate).
**Structured logging fields required:** `timestamp`, `level`, `message`, `trace_id`, `span_id`, `service`, `product_id`, `capability_id`, `feature_id`, `epic_id`.
**Trace propagation:** OpenTelemetry OTLP export to Datadog/CloudTrace. 4-audit-scripts embed logs em Camada 2 (infrastructure health).

---

## 8. Decision Rationale

### DR-001: Hierarquia Product-First Como Base Canônica

**Decisão:** Product-First hierarchy (Product → Capability → Feature → Epic → Story → Task) será fundação inviolável de todos os planejamentos futuros, com validação automática em planning-gate.

**Motivo:** Desacoplamento entre "o quê" (Product/Feature — visível ao PM) e "o como" (Epic/Story — implementação). Reduz rework e alinha incentivos de Product vs Engineering. EPIC-0064 provou modelo viável com Capabilities v3.0.

**Alternativa descartada:** Flattening Epic ↔ Feature (quebra de rastreabilidade). Lazy validation post-implementation (permite inconsistência de longo prazo, custoso para audits).

**Consequência:** Todas skills de criação (x-create-product, x-epic-create) adotam contrato de input que requer parent IDs. Planning-gate rejeitará estruturas orphaned. Refactor de projects existentes (vide EPIC-0089) é atividade separada.

---

### DR-002: C4 Model Como Contrato Visual Obrigatório

**Decisão:** C4 diagrams em arquivos `.md` com blocos Mermaid (System C1, Container C2, Component C3, Code C4) tornam-se obrigatórios em Epic-level plans e validados automaticamente antes de Story creation.

**Motivo:** Arquitetura desacoplada de implementação é escopo de épico. C4 força discussão visual pré-dev, reduzindo "surprises" em task-plan. RULE-003 + x-plan-architecture validator detectam inconsistências cedo (C1 system conta com containers não declarados em C2, etc).

**Alternativa descartada:** Arquitetura em ADRs apenas (silos de tech leads, sem validação). Diagrama opcional (guessing game sobre scope). PlantUML/ASCII como formato primário foi descartado em favor de Mermaid em `.md`.

**Consequência:** Epic template v3 requer campo `c4_system_diagram` não-nulo em arquivo `.md` com bloco `mermaid`. x-plan-architecture falha se C4 Container não linkar com C1 System. Agents planning-refinement + planning-decompose herdam C4 obrigatoriedade. `story-0077-0001` bootstrapa `c4-system-context.md` e `c4-containers.md` antes da aprovação do DoR; `story-0077-0013` e `story-0077-0016` endurecem a validação sem alterar essa obrigatoriedade.

---

### DR-003: RNF Como Entrada Obrigatória com Gate No-Relax

**Decisão:** Non-Functional Requirements (performance, availability, security, compliance) são entrada obrigatória ao criar Story. Gate enforce-refinement-gate.sh verifica 100% coverage, proibindo --force overrides.

**Motivo:** QA charter v5 depende de RNF measurable (SLO, AC threshold). RNF tardio = testes sem baseline. No-relax gate evita cultura de "pediremos depois". EPIC-0072 popula RNF table de quality.* YAML.

**Alternativa descartada:** RNF recomendado (ignored por pressão). RNF post-development (testes sem contexto).

**Consequência:** x-story-create requer `--rnf <yaml>` argument. enforce-refinement-gate.sh retorna exit 1 se RNF omitido, bloqueando story entry. Docs MUST mostrar exemplos de RNF tables em each feature level.

---

### DR-004: QA Refocus em Measurability + Error Catalog

**Decisão:** QA charter v5 enfatiza AC measurable (dado X, quando Y, ENTÃO métrica Z < threshold), error-message catalog padrão (HTTP + descriptive), response-time SLO, success metrics negócio.

**Motivo:** "Testable acceptance criteria" reduz ambiguidade PM ↔ QA. Error catalog previne "what error?" debates em prod. RULE-005 + x-review-codebase-qa validator checam AC syntax. E2E E2E scenarios por story garantem integração.

**Alternativa descartada:** "Happy path only" (descobrem failures em prod). Generic error messages (bad DX).

**Consequência:** Story template v4 requer `acceptance_criteria` com operador relacional (`<`, `>`, `==`, `≤`, `≥`). x-review-codebase-qa rejeita "should be fast" (não mensurável). Pentest gate escalação automática para stories impactando auth/data.

---

### DR-005: Skills Novas (4) + Refatoradas (5) Para Frontload Validação

**Decisão:** x-create-product, x-create-capability, x-create-feature, x-promote-ideation são skills novas. x-epic-create, x-story-create, x-plan-architecture, x-plan-task, x-plan-story refatorados para consumir novos tipos de input (C4, RNF, hierarchy).

**Motivo:** Frontload de validação em skills vs. descoberta em planning-gate reduz buracos. Skills emitem eventos (ideation.promoted, product.created) que agents consumem. Refactor de epics existentes bloqueado até Phase 1 (seria disruptivo).

**Alternativa descartada:** Monolith única skill "create-all" (impossível validar in stages). Lazy validation post-decompose (permite epics mal-formados).

**Consequência:** Cada skill nova + refatorada é uma story separada. `story-0077-0003` fecha o naming contract com EPIC-0065 antes de qualquer alteração em `x-create-feature`; aliases backward-compatible absorvem o rename no limite entre os dois épicos. `x-skill-invoke` adapta UnicodeScript → bash bridge.

---

### DR-006: Audit Scripts Camada 2 Como Health Checks

**Decisão:** 4 audit scripts novos (product-validate.sh, capability-validate.sh, feature-validate.sh, rnf-audit.sh) validam estrutura + linkage. Integrados em infrastructure health checks (Camada 2).

**Motivo:** Validação declarativa (regex + JSON schema) é simples + rápido, permite audits batch (100 products em < 2s). Observabilidade: cada script emite logs estruturados (product_id, validation_status, error_code).

**Alternativa descartada:** Validação inline em skills (overhead por criação). Zero validation infra (discover issues via manual audits).

**Consequência:** source-of-truth dos scripts em `java/src/main/resources/targets/claude/scripts/<stack>/audit-*.sh`. Cada script outputs JSON (`valid_count`, `error_count`, `error_details`). CI integra via health-check stage (pré-deploy validação).

---

### DR-007: Refactor de Plans Primeiro, Skills Depois (Phase Ordering)

**Decisão:** Phases ordenadas: 0 (setup) → 1 (templates novos) → 2 (agents refatorados) → 3 (skills novas) → 4 (skills refatoradas) → 5 (scripts audit) → 6 (E2E + golden) → 7 (docs + CHANGELOG).

**Motivo:** Refactor de templates ANTES de skills evita skill imports de templates v2. Agents refatorados consomem templates novos. Scripts audit testam output de skills. Phase ordering reduz circular dependencies.

**Alternativa descartada:** Parallel Phase 1-4 (merge conflicts, undefined behavior se template v2 vs v3 em voo).

**Consequência:** Critical path é linear (0→7). Parallelism apenas dentro phase (múltiplas skills novas em Phase 3). EPIC-0041 parallelism evaluation valida file footprint (zero write-write conflicts).

---

### DR-008: Decision Rationale Split (Produto / Execução / Local)

**Decisão:** Cada decisão é categorizada: Produto (visível PM, influencia roadmap), Execução (visível tech lead, influencia architecture), Local (visível dev, influencia implementation).

**Motivo:** Rastreabilidade de "quem precisa saber". Produto decisions bloqueiam refactoring (impactam roadmap). Execução decisions bloqueiam deploys (impactam arch). Local decisions não bloqueiam nada (dev choice).

**Alternativa descartada:** Flat decision list (ambiguidade sobre quem valida).

**Consequência:** RULE-009. Decision Rationale obrigatório em cada story + task. x-internal-verify-story valida categoria (rejeita Decision Rationale não-categorizado).

---

## 9. Dependências & File Footprint

### Índice de Histórias

| ID | Título | Dependências (Blocked By) | Entrega de Valor | Phase |
| :--- | :--- | :--- | :--- | :--- |
| [story-0077-0001](./story-0077-0001.md) | Setup: Epic Template v3 + RNF-Validation Schema | - | Fundação para templates novos | 0 |
| [story-0077-0002](./story-0077-0002.md) | Template: _TEMPLATE-PRODUCT.md v1 | story-0077-0001 | Product artifact padrão + validação | 1 |
| [story-0077-0003](./story-0077-0003.md) | Template: _TEMPLATE-CAPABILITY.md v1 | story-0077-0002 | Capability artifact padrão | 1 |
| [story-0077-0004](./story-0077-0004.md) | Template: _TEMPLATE-FEATURE.md v1 | story-0077-0003 | Feature artifact padrão | 1 |
| [story-0077-0005](./story-0077-0005.md) | Agent Refactor: planning-refinement (Product hierarchy) | story-0077-0004 | Validação automática de hierarquia | 2 |
| [story-0077-0006](./story-0077-0006.md) | Agent Refactor: planning-decompose (C4 + RNF propagation) | story-0077-0005 | C4 e RNF herança automática | 2 |
| [story-0077-0007](./story-0077-0007.md) | Skill NEW: x-create-product UnicodeScript wrapper | story-0077-0006 | Product creation CLI | 3 |
| [story-0077-0008](./story-0077-0008.md) | Skill NEW: x-create-capability UnicodeScript wrapper | story-0077-0007 | Capability creation CLI | 3 |
| [story-0077-0009](./story-0077-0009.md) | Skill NEW: x-create-feature UnicodeScript wrapper | story-0077-0008 | Feature creation CLI | 3 |
| [story-0077-0010](./story-0077-0010.md) | Skill NEW: x-promote-ideation UnicodeScript wrapper | story-0077-0009 | Ideation → Product promotion CLI | 3 |
| [story-0077-0011](./story-0077-0011.md) | Skill REFACTOR: x-epic-create (C4 required, RULE-003) | story-0077-0010 | Epic creation com C4 mandatory | 4 |
| [story-0077-0012](./story-0077-0012.md) | Skill REFACTOR: x-story-create (RNF validation, RULE-004) | story-0077-0011 | Story creation com RNF gate | 4 |
| [story-0077-0013](./story-0077-0013.md) | Skill REFACTOR: x-plan-architecture (C4 Container validation) | story-0077-0012 | Architecture planning com C4 rigor | 4 |
| [story-0077-0014](./story-0077-0014.md) | Skill REFACTOR: x-plan-task (RNF inheritance, SLO) | story-0077-0013 | Task planning com SLO context | 4 |
| [story-0077-0015](./story-0077-0015.md) | Skill REFACTOR: x-plan-story (RNF validation layer) | story-0077-0014 | Story planning com RNF verify | 4 |
| [story-0077-0016](./story-0077-0016.md) | Script Audit: product-validate.sh + tests | story-0077-0015 | Product structure validation | 5 |
| [story-0077-0017](./story-0077-0017.md) | Script Audit: capability-validate.sh + tests | story-0077-0016 | Capability linkage audit | 5 |
| [story-0077-0018](./story-0077-0018.md) | Script Audit: feature-validate.sh + tests | story-0077-0017 | Feature linkage audit | 5 |
| [story-0077-0019](./story-0077-0019.md) | Script Audit: rnf-audit.sh + E2E | story-0077-0018 | RNF table coverage validation | 5 |
| [story-0077-0020](./story-0077-0020.md) | E2E Scenario 1: Ideation → Product → Capability | story-0077-0019 | End-to-end flow happy path | 6 |
| [story-0077-0021](./story-0077-0021.md) | E2E Scenario 2: Feature → Epic (C4) → Story (RNF) | story-0077-0020 | C4 + RNF integration test | 6 |
| [story-0077-0022](./story-0077-0022.md) | E2E Scenario 3: Epic decompose + 4 skills refactored | story-0077-0021 | Refactored skills integration | 6 |
| [story-0077-0023](./story-0077-0023.md) | E2E Scenario 4: Pentest (auth bypass, IDOR, SSRF) | story-0077-0022 | Security gate validation | 6 |
| [story-0077-0024](./story-0077-0024.md) | Golden Fixtures: Regenerate product-*.md, capability-*.md | story-0077-0023 | Artifact consistency | 6 |
| [story-0077-0025](./story-0077-0025.md) | Coverage: Unit + Integration (≥95% line, ≥90% branch) | story-0077-0024 | QA gate validation | 6 |
| [story-0077-0026](./story-0077-0026.md) | Docs: CHANGELOG.md + skills reference | story-0077-0025 | User-facing documentation | 7 |
| [story-0077-0027](./story-0077-0027.md) | Docs: ADR registry (10 SOLID rules) | story-0077-0026 | Architecture decision traceability | 7 |
| [story-0077-0028](./story-0077-0028.md) | Final QA: x-internal-verify-epic-integrity + audit | story-0077-0027 | Epic completion gate | 7 |

### File Footprint (EPIC-0041 parallelism evaluation)

```
write:
  - /ai/products/_TEMPLATE-PRODUCT.md (v1)
  - /ai/capabilities/_TEMPLATE-CAPABILITY.md (v1)
  - /ai/features/_TEMPLATE-FEATURE.md (v1)
  - /ai/epics/epic-0077-product-first-lifecycle/_TEMPLATE-EPIC.md (v3 refactor)
  - /.claude/rules/c4-model-architecture.md
  - /ai/planning/c4-validation/_TEMPLATE-C4-VALIDATION.md
  - /ai/planning/rnf-validation/_TEMPLATE-RNF-VALIDATION.md
  - /ai/epics/epic-0077-product-first-lifecycle/story-0077-000[1-9].md (28 stories)
  - /java/src/main/resources/targets/claude/scripts/<stack>/audit-*.sh
  - /skills/x-create-product/
  - /skills/x-create-capability/
  - /skills/x-create-feature/
  - /skills/x-promote-ideation/
  - /CHANGELOG.md (append 28 items)

read:
  - /ai/epics/epic-0064-capability-driven-composition/epic-0064-*.md
  - /.claude/rules/22-lifecycle-integrity.md
  - /.claude/rules/planning-standards-kp.md
  - /ai/epics/epic-0077-product-first-lifecycle/_TEMPLATE-EPIC.md (v2 baseline)
  - /skills/x-epic-create/ (refactor inputs)
  - /skills/x-story-create/ (refactor inputs)
  - /agents/planning-refinement/
  - /agents/planning-decompose/

regen:
  - /golden/products/*.md (subset, 5-10 fixtures)
  - /golden/capabilities/*.md (subset, 5-10 fixtures)
  - /golden/features/*.md (subset, 5-10 fixtures)
  - /golden/epics/epic-0077-product-first-lifecycle/*.md (28 story outputs)
  - /golden/scripts/audit/product-validate-output.json (4 scenarios)
```

### Critical Path & Parallelism

```
Phase 0 (Setup):
  story-0077-0001 ▁ (E.T.A 1 day)

Phase 1 (Templates):
  story-0077-0002 ▬ story-0077-0003 ▬ story-0077-0004
  (Parallelizable após 0002, E.T.A 3 days)

Phase 2 (Agents):
  story-0077-0005 ▬ story-0077-0006
  (Serial, E.T.A 2 days)

Phase 3 (Skills New):
  story-0077-0007 ▬ story-0077-0008 ▬ story-0077-0009 ▬ story-0077-0010
  (Serial, E.T.A 4 days)

Phase 4 (Skills Refactor):
  story-0077-0011 ▬ story-0077-0012 ▬ story-0077-0013 ▬ story-0077-0014 ▬ story-0077-0015
  (Serial, E.T.A 5 days)

Phase 5 (Scripts):
  story-0077-0016 ▬ story-0077-0017 ▬ story-0077-0018 ▬ story-0077-0019
  (Serial, E.T.A 4 days)

Phase 6 (E2E + Fixtures):
  story-0077-0020 ▬ story-0077-0021 ▬ story-0077-0022 ▬ story-0077-0023
  story-0077-0024 ▬ story-0077-0025
  (E2E scenarios 1-4 podem rodar em paralelo, então golden/coverage, E.T.A 5 days)

Phase 7 (Docs + QA):
  story-0077-0026 ▬ story-0077-0027 ▬ story-0077-0028
  (Serial, E.T.A 2 days)

Total E.T.A: ~26 days (serial critical path, início em Refinement)
```

---

## Refinement Verdict

**Status:** approved  
**Scope:** epic  
**Refined at:** 2026-05-03T20:03:14Z  
**Verdict hash:** `3bce87d04a0a1e0baf869e201c5779257b88ead7034a35ce92e561547bbbd6c8`

### Dimensions

| Dimension | Status | Blocker |
|-----------|--------|---------|
| problem | passed | — |
| persona | passed | — |
| value | passed | — |
| okrs | passed | — |
| alternatives | passed | — |
| risks | passed | — |
| scope | passed | — |

### Blockers

none

### Rationale

O épico agora explicita problema observável com evidências, personas afetadas, hipótese de valor, KPIs mensuráveis, alternativas estratégicas, riscos produto+técnicos e escopo/out-of-scope suficiente. As decisões de rerun foram materializadas no documento e não resta lacuna estratégica bloqueante para aprovação.

---

## Observações Finais

Este épico establece a fundação canônica Product-First da plataforma com 28 stories em 7 phases. Hierarquia rigorosa (Product → Capability → Feature → Epic → Story → Task) + C4 Model + RNF gate no-relax definem contratos imutáveis. QA refocus em measurability, pentest obrigatório, e audit scripts Camada 2 elevam quality bar.

**Transição para "Pendente":** Ao fim do decompose (story-0077-0001 Concluída), status → Pendente, requerendo aprovação PM + Tech Lead antes de Phase 1 kick-off.

---

**Documento versão:** RA9 v2  
**Última atualização:** 2026-05-03  
**Próxima revisão:** Pós-Phase 0 (story-0077-0001 conclusion)
