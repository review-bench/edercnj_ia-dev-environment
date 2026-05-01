# Plano Estratégico v2 — Evolução do `ia-dev-environment` para NDP

> **Status:** Rascunho estratégico reorganizado (pré-refinement).
> **Origem:** Consolidação do `docs/ndp-strategic-plan.md` após inventários de skills, hooks, rules, KPs, templates e artefatos.
> **Objetivo:** Servir como fonte de entrada para planejar o futuro produto, suas capacidades, features e épicos.
> **Hierarquia-alvo:** `Project → Product → Capacity → Feature`. Épicos, Stories e Tasks ficam para a próxima etapa de refinement.
> **Codinome:** `NDP` — NextGen Dev Platform.

---

## 0. Como ler esta versão

A versão anterior acumulou informação de forma incremental: primeiro princípios, depois produtos, depois migração, depois inventários. Isso foi útil para descobrir fatos, mas deixou duplicações: skills aparecem por tipo, por papel, por grupo canônico e por destino; hooks aparecem tanto como problema de governança quanto como artefatos; templates e artefatos aparecem próximos, mas representam camadas diferentes.

Esta v2 reorganiza a história em uma sequência mais natural para tomada de decisão:

1. **Tese do produto:** o que o NDP é e quais decisões são inegociáveis.
2. **Diagnóstico:** como o `ia-dev-environment` funciona hoje e por que precisa evoluir.
3. **Modelo futuro:** como a inversão de controle muda runtime, LLM, policies, telemetria e artefatos.
4. **Inventário canônico:** o que existe hoje, classificado uma única vez por papel: commands/workers, policies, KPs, templates, artefatos e integrações.
5. **Taxonomia de domínio:** como esses ativos entram em grupos coerentes de produto.
6. **Roadmap:** Products, Capacities e Features.
7. **Riscos e próximos passos:** o que precisa ser provado antes de transformar isso em épicos.

Regra editorial desta versão: inventários detalhados continuam presentes porque são contexto estratégico, mas cada lista tem uma função clara. A lista de skills diz **qual papel cada skill terá no NDP**. A taxonomia diz **em qual domínio do produto esse papel vive**. Templates dizem **como artefatos são renderizados**. Artefatos dizem **o que aparece no disco e quem consome**.

---

## 1. Tese do Produto

### 1.1. Princípio fundador — local-first, CLI-first

O NDP é, em primeiro lugar, uma ferramenta que roda 100% local na máquina do desenvolvedor. A V0 é uma CLI, como o `ia-dev-env` é hoje. Toda interface gráfica (TUI, web console, dashboards, IDE extensions, SaaS) é pós-V0 e não bloqueia nenhuma capacidade do core.

| Decisão | Consequência |
| --- | --- |
| Local-first por padrão | Código-fonte, telemetria, audit log e estado de execução vivem no disco do desenvolvedor. Nada é enviado para nuvem sem opt-in explícito. |
| V0 = CLI | A primeira release entregável é `ndp ...`, sem servidor, sem login e sem dependência de rede no caminho feliz. |
| UI é v1+ | TUI, IDE panels e web console consomem o mesmo core da CLI. Nenhuma regra de negócio vive apenas na UI. |
| Headless friendly | Todo comando core precisa ser scriptável, com `--output json`, `--output text` e, quando fizer sentido, `--output ndjson`. |
| Network como opt-in | Marketplace, telemetria remota, analytics cross-project e multi-tenancy são desligados por padrão. |
| Sem vendor lock-in cloud | Cloud é serviço opcional sobre o core, não pré-requisito. |

Roadmap de interfaces:

```text
V0  -> CLI (ndp <command>)                      -> single source of truth
V1  -> TUI (ndp tui) e IDE panel mínimo         -> melhora UX, mesmo core
V2  -> Web console local (ndp ui, localhost)    -> dashboard offline opcional
V3+ -> SaaS multi-tenant e marketplace remoto   -> opt-in, separado do core
```

A CLI permanece canônica em todas as versões. Nenhuma feature do core pode existir apenas na UI.

### 1.2. Princípio de inversão de controle — o NDP é o orquestrador

A mudança mais importante não é trocar templates, renomear skills ou criar outro gerador. A mudança é inverter quem controla a execução.

Modelo atual:

```text
Usuário -> Claude Code -> lê SKILL.md -> LLM tenta executar passo a passo
                                      -> hooks bash e audit scripts tentam impedir bypass
```

O LLM é o orquestrador. Ele decide quais skills chamar, em qual ordem e com quais argumentos. As camadas de gates existem porque o LLM pode pular etapas, simular resultados e declarar sucesso sem gerar evidência.

Modelo NDP:

```text
Usuário -> ndp story implement STORY-0072-0001
             |
             v
          NDP controla state machine, fases, gates, evidências, git, PR e telemetria
             |
             v
          LLM é chamado apenas como worker criativo quando necessário
```

O NDP é o orquestrador determinístico. O LLM é um worker invocado para tarefas pontuais que exigem julgamento ou geração: refinar story, gerar plano, escrever código, revisar, redigir ADR. Tudo que é determinístico — ordem de fases, criação de branch, commit, PR, validação de artefatos, telemetria, retry, resume e gates — vira código do NDP.

### 1.3. Consequências práticas

| Efeito | Decisão de produto |
| --- | --- |
| Skills orquestradoras viram comandos CLI | `x-story-implement` vira `ndp story implement`; `x-epic-implement` vira `ndp epic implement`; `x-release` vira `ndp release`. |
| Hooks e scripts shell deixam de ser mecanismo primário | Invariantes passam para funções do runtime e `ndp ci verify`. |
| Camadas 0-4 colapsam em duas | **Camada A:** NDP runtime bloqueia em processo. **Camada B:** CI valida o que o NDP produziu. |
| Zero-bypass vira propriedade arquitetural | O fluxo oficial começa no comando NDP; o comando não oferece caminho feliz sem evidência. |
| Refinement gate vira pré-condição do comando | `ndp story implement` falha com código tipado se o refinement não estiver aprovado. |
| Tool-call grammar vira tipo | O que hoje é regex contra markdown vira schema, enum e contrato de comando. |
| Telemetria é in-process | NDJSON/OTel nasce no runtime que controla a execução, não em hooks externos. |
| Rules críticas viram policies | Rules deixam de ser apenas prosa para LLM e passam a ter `policy_id`, versão, testes e ponto de execução. |

### 1.4. Posicionamento

O NDP não compete em "a IA faz tudo sozinha". O diferencial é: **a IA faz certo, com governança, evidência e execução auditável, localmente, em qualquer IDE e com múltiplos LLMs**.

---

## 2. Diagnóstico do Estado Atual

### 2.1. O que o `ia-dev-environment` é hoje

O `ia-dev-environment` é um gerador CLI Java que materializa um ambiente `.claude/` opinativo a partir de um profile YAML. Ele possui:

| Camada | O que existe hoje | Limite percebido |
| --- | --- | --- |
| Núcleo de geração | `CapabilityResolver`, `CapabilityAwareComposer`, `OutputPruner`, templates Pebble e goldens. | Composição é file-system + templates; não há runtime de execução. |
| Governança | Rules 01-31, audit scripts, hooks, zero-bypass, refinement gate, doc-as-DoD. | Governança forte, mas espalhada em markdown, bash, Java tests e CI. |
| Orquestração | Skills como `x-story-implement`, `x-epic-implement`, `x-task-implement`, `x-review`, `x-release`. | O LLM interpreta markdown e tenta executar a state machine. |
| Observabilidade | `ai/epics/*/telemetry/events.ndjson`, `x-telemetry-analyze`, `x-telemetry-trend`. | Local e útil, mas não é runtime-native nem OTel-compliant. |
| Lifecycle | Local-first, folder layout v4, templates v2, task hierarchy, phase gates. | A aplicação real das regras depende de hooks e disciplina do agente. |
| Distribuição | Ativos copiados para cada repositório gerado. | Sem marketplace, SemVer de pacote, patching declarativo ou update centralizado. |

### 2.2. Problemas estruturais

| Problema | Por que importa |
| --- | --- |
| Mono-LLM e mono-IDE | O sistema assume Claude Code e modelos Anthropic. O próximo produto precisa suportar Cursor, Claude Code, Windsurf, Aider, Codex/Gemini CLI e LLMs locais. |
| Governança como texto | Rules são interpretadas pelo LLM; poucas são policies executáveis com testes. |
| Runtime ausente | Não há engine nativo controlando fases, resume, retries, locking e evidências. |
| Hooks como compensação | Hooks bloqueiam sintomas, não a causa. Eles existem porque o LLM controla o fluxo. |
| Artefatos por convenção | `plans/`, `reports/`, `execution-state.json` e `events.ndjson` têm semântica rica, mas não têm registry tipado comum. |
| Duplicação de conhecimento | O mesmo critério aparece em Rule, KP, SKILL.md, template e audit script. |
| Onboarding pesado | Para entender o sistema atual, o usuário precisa aprender dezenas de rules, skills e exceções. |
| Cada repo é uma ilha | Telemetria, learnings, skills e rules não evoluem como produto compartilhado. |

### 2.3. A leitura estratégica

O sistema atual já provou valor: ele tem uma gramática de governança, um catálogo grande de skills, um lifecycle de evidências, telemetry local e uma cultura de zero-bypass. O NDP não deve jogar isso fora. O que deve mudar é **onde a responsabilidade vive**.

O que hoje é espalhado como texto e shell deve virar:

- runtime determinístico;
- registry de ativos versionados;
- policies executáveis;
- KPs carregados sob demanda;
- templates com schema;
- artefatos com `artifact_kind`;
- telemetria e audit log nativos;
- comandos CLI que escondem complexidade operacional do usuário.

---

## 3. Modelo Futuro do NDP

### 3.1. Arquitetura conceitual

```text
Profile / repo / command args
        |
        v
NDP CLI
        |
        v
Runtime determinístico
  - state machine
  - gates
  - artifact registry
  - policy engine
  - telemetry/audit log
  - provider/router de LLM
        |
        +--> adapters determinísticos (git, build, test, docs, security, PR)
        |
        +--> workers LLM (prompts versionados + schema de saída)
```

### 3.2. Tipos canônicos de ativo

| Tipo | Responsabilidade | Exemplo atual | Forma no NDP |
| --- | --- | --- | --- |
| `command` | Entrada pública de produto. | `x-story-implement`, `x-release`, `x-pr-watch-ci`. | `ndp story implement`, `ndp release`, `ndp pr watch`. |
| `internal-service` | Componente chamado por commands; não precisa ser exposto. | `x-internal-status-update`, `x-internal-phase-gate`. | Classe/serviço testável. |
| `worker-prompt` | Produz conteúdo criativo com LLM. | `x-arch-plan`, `x-test-plan`, review specialists. | Prompt versionado + schema de saída. |
| `adapter` | Executa tooling determinístico. | `x-test-run`, `x-code-lint`, `x-git-commit`. | Adapter local para git/build/test/PR. |
| `policy` | Regra executável ou gate. | Rules 24/27/29/31, audit scripts. | `policy_id`, schema, runtime gate, CI check. |
| `knowledge-pack` | Contexto, padrões, heurísticas e checklists. | `architecture`, `testing`, `security`, `planning-standards-kp`. | KP versionado carregado por fase/worker. |
| `template` | Estrutura renderizável. | `_TEMPLATE-STORY.md`, `_TEMPLATE-PR-IMPLEMENTATION.md`. | Template registry + input schema + renderer. |
| `artifact-kind` | Tipo de evidência persistida. | `verify-envelope`, `story-completion-report`, `events.ndjson`. | Schema + gerador autorizado + consumidores declarados. |
| `plugin` | Integração opcional fora do core offline. | Jira, MCP marketplace, SaaS observability. | Pacote opt-in com permissões claras. |

### 3.3. Contrato de registry

Todo ativo precisa declarar identidade, tipo, domínio, versão, dependências e outputs. Exemplo:

```yaml
id: ndp.story.implement
kind: command
domain: execution-governance
version: 1.0.0
requires-policies:
  - ndp.policy.refinement-gate@1
  - ndp.policy.execution-integrity@1
  - ndp.policy.task-hierarchy@1
requires-context:
  - ndp.kp.story-planning@1
  - ndp.kp.testing.tdd@1
  - ndp.kp.security.baseline@1
produces:
  - ndp.artifact.story-completion-report@1
  - ndp.artifact.verify-envelope@1
  - ndp.artifact.telemetry-run@1
```

### 3.4. Regra de ouro de classificação

| Se o ativo... | Então vira... |
| --- | --- |
| Decide ordem, fases, retries, commits, PRs ou resume | `command` ou `internal-service` do runtime. |
| Gera julgamento ou conteúdo com LLM | `worker-prompt`. |
| Roda uma ferramenta local determinística | `adapter` ou comando utilitário. |
| Define o que sempre deve ser verdade | `policy`. |
| Ensina contexto ou critérios, sem side effects | `knowledge-pack`. |
| Define a forma de um documento/código | `template`. |
| É arquivo persistido com evidência | `artifact-kind`. |

### 3.5. Blueprint de sequência dos comandos

Os comandos são o ponto de entrada do produto. Por isso, a forma mais clara de entender o NDP é partir do comando mais amplo e expandir as delegações. O fluxo raiz é `ndp epic implement`: ele coordena épico, histórias, tasks, PRs, reviews, gates, evidências e telemetria. Sempre que uma chamada delega para outro orquestrador e o diagrama ficaria ilegível, o detalhe aparece no diagrama seguinte.

Regra de leitura:

- `NDP Runtime` substitui a skill markdown atual como dono da state machine.
- `Policy/Gate Engine` substitui phase gates, hooks preventivos e audit checks locais.
- `Artifact Store` representa `ai/epics/*`, `ai/releases/*`, `ai/runs/*`, PR body e state local.
- `LLM Worker` só aparece quando há geração criativa ou julgamento.
- `Adapters` representam git, build, test, docs, security, GitHub/PR e CI.

#### 3.5.1. `ndp epic implement <EPIC-ID>` — sequência raiz

Este é o fluxo equivalente ao `x-epic-implement`. Ele preserva as seis fases atuais: args, plano, branch, loop de stories, gate de integridade e PR final.

```mermaid
sequenceDiagram
  autonumber
  actor User as Developer
  participant CLI as NDP CLI
  participant RT as NDP Runtime
  participant Gate as Policy/Gate Engine
  participant State as Execution State
  participant Art as Artifact Store
  participant Git as Git Adapter
  participant Story as ndp story implement
  participant PR as PR Adapter
  participant Tele as Telemetry/Audit

  User->>CLI: ndp epic implement EPIC-XXXX [flags]
  CLI->>RT: parse command envelope
  RT->>Tele: run.start(epic implement)
  RT->>Gate: assertPre(Phase 0 - Args)
  RT->>RT: normalize args, resolve flowVersion, flags, mode
  RT->>State: persist interactiveMode and command context
  RT->>Gate: assertPost(Phase 0 - Args)

  RT->>Gate: assertPre(Phase 1 - Plan)
  RT->>Art: read epic, stories, IMPLEMENTATION-MAP
  RT->>RT: build DAG, phases, critical path, resume projection
  RT->>Art: write epic-execution-plan
  RT->>Gate: assertPost(Phase 1 - Plan, expected plan)

  alt dry-run
    RT-->>CLI: return plan path and stop
  else executable run
    alt flowVersion is legacy
      RT->>Tele: skip Phase 2 branch setup
    else v2/v4 flow
      RT->>Gate: assertPre(Phase 2 - Branch)
      RT->>Git: ensure epic/XXXX from develop and push
      Git-->>RT: branch ready or conflict
      RT->>Gate: assertPost(Phase 2 - Branch)
    end

    RT->>Gate: assertPre(Phase 3 - Stories)
    loop each implementation phase
      loop each story in topological order
        RT->>State: mark story IN_PROGRESS
        RT->>Story: ndp story implement STORY-ID --target-branch epic/XXXX --auto-merge strategy
        Story-->>RT: story envelope {status, pr, coverage, report}
        RT->>State: persist story status, PR status, evidence paths
        alt story failed
          RT->>State: mark dependants BLOCKED
          RT->>Gate: raise STORY_FAILED or trigger recovery policy
        end
      end
      RT->>Gate: assertWave(phase stories completed and merged)
    end
    RT->>Gate: assertPost(Phase 3 - Stories)

    RT->>Gate: assertPre(Phase 4 - Integrity)
    RT->>Art: read story reports, verify envelopes, PR evidence, telemetry
    RT->>Gate: run epic integrity gate
    alt gate failed
      RT->>PR: optional ndp pr fix-epic or revert policy
      PR-->>RT: remediation result
      RT->>Gate: retry integrity gate once
    end
    RT->>Art: write epic execution report and verify envelope
    RT->>Gate: assertPost(Phase 4 - Integrity)

    alt non-legacy final PR
      RT->>Git: merge develop into epic/XXXX
      Git-->>RT: synced or conflict
      RT->>PR: create final PR epic/XXXX -> develop
      PR-->>RT: PR url/number
    end
  end

  RT->>Tele: run.end(epic implement, status)
  RT-->>CLI: structured output
  CLI-->>User: summary, evidence paths, final PR
```

#### 3.5.2. `ndp story implement <STORY-ID>` — ciclo de story

Este diagrama expande a chamada feita no loop do épico. Ele corresponde ao `x-story-implement`: prepara contexto, cria/valida contratos, planeja, executa tasks, cria PRs, valida, roda docs/reviews e escreve relatório final.

```mermaid
sequenceDiagram
  autonumber
  participant Epic as ndp epic implement
  participant Story as ndp story implement
  participant Gate as Policy/Gate Engine
  participant State as Execution State
  participant Art as Artifact Store
  participant Contract as Contract Adapter
  participant Plan as Story Planning Service
  participant Task as ndp task implement
  participant PR as PR/CI Adapter
  participant Docs as Docs Adapter
  participant Review as Review Commands
  participant Tele as Telemetry/Audit

  Epic->>Story: implement STORY-ID with target branch and flags
  Story->>Tele: phase.start(Prepare)
  Story->>Gate: assertPre(Phase 0 - Context)
  Story->>State: persist interactiveMode
  Story->>Art: load story, predecessor status, existing artifacts
  Story->>State: resume projection if --resume
  Story->>Gate: assertPost(Phase 0 - Context)

  opt story declares API contracts
    Story->>Contract: generate OpenAPI/Proto/AsyncAPI draft
    Contract->>Contract: lint contract
    Contract-->>Story: contract envelope
    Story->>Art: persist contracts
  end

  alt planning artifacts are fresh
    Story->>Tele: skip Phase 1 as PRE_PLANNED
  else planning required
    Story->>Gate: assertPre(Phase 1 - Plan)
    Story->>Plan: build story plan wave
    Plan-->>Story: artifacts envelope
    Story->>Gate: assertWave(arch, impl, tests, tasks, security, compliance)
    Story->>Gate: assertPost(Phase 1 - Plan)
  end

  Story->>Gate: assertPre(Phase 2 - Execute)
  Story->>Art: read tasks-story and task plans
  loop each pending task
    Story->>State: check dependencies
    alt dependency unresolved
      Story->>State: mark task BLOCKED
    else executable task
      Story->>Task: ndp task implement TASK-ID --orchestrated
      Task-->>Story: task envelope {status, branch, commit, coverage}
      alt task failed
        Story->>State: mark task FAILED and dependants BLOCKED
        Story-->>Epic: TASK_FAILED envelope
      else task done
        Story->>PR: create/watch/merge task PR
        PR-->>Story: prNumber, mergeStatus, ciStatus
        Story->>State: update task status and PR evidence
      end
    end
  end

  opt parent story PR mode
    Story->>PR: create story-level PR
    PR-->>Story: story PR envelope
  end
  Story->>Gate: assertPost(Phase 2 - Execute)

  Story->>Gate: assertPre(Phase 3 - Verify)
  Story->>Docs: generate docs
  Docs-->>Story: docs changed
  Story->>Docs: validate documentation freshness
  Docs-->>Story: doc-validate-report
  Story->>Gate: verify story evidence, coverage, ACs
  Gate-->>Story: verify-envelope
  Story->>Review: ndp review STORY-ID
  Review-->>Story: specialist dashboard
  Story->>Review: ndp review pr STORY-ID
  Review-->>Story: GO or NO-GO verdict
  alt NO-GO and remediation enabled
    Story->>PR: ndp pr fix
    PR-->>Story: remediation result
    Story->>Review: rerun required review gate
  end
  Story->>Art: write story-completion-report
  Story->>State: mark story COMPLETE
  Story->>Gate: assertFinal(verify, reviews, report, docs)
  Story->>Tele: phase.end(story lifecycle)
  Story-->>Epic: story envelope
```

#### 3.5.3. Story planning wave — workers paralelos

Este diagrama detalha o subfluxo de planejamento (`x-internal-story-build-plan`). Ele é separado porque tem fan-out/fan-in e vários artefatos de Fase 1.

```mermaid
sequenceDiagram
  autonumber
  participant Story as ndp story implement
  participant Plan as Story Planning Service
  participant Gate as Policy/Gate Engine
  participant LLM as LLM Worker Pool
  participant Art as Artifact Store
  participant Parallel as Parallelism Evaluator

  Story->>Plan: build plan for STORY-ID with scope
  Plan->>Gate: assertPre(planning wave)
  Plan->>LLM: x-arch-plan worker
  par implementation plan
    Plan->>LLM: implementation-plan worker
  and test plan
    Plan->>LLM: test-plan worker
  and task breakdown
    Plan->>LLM: task-breakdown worker
  and security assessment when scope requires
    Plan->>LLM: security-assessment worker
  and compliance assessment when scope requires
    Plan->>LLM: compliance-assessment worker
  end
  LLM-->>Plan: structured artifact drafts
  Plan->>Art: write arch, plan, tests, tasks, security, compliance
  Plan->>Parallel: evaluate file footprint and hotspots
  alt hard or regen collision
    Parallel-->>Plan: degrade affected wave to serial
    Plan->>Art: record parallelismDowngrades
  else no collision
    Parallel-->>Plan: parallel execution allowed
  end
  Plan->>Gate: assertWave(expected artifacts)
  Plan-->>Story: artifacts envelope
```

#### 3.5.4. `ndp task implement <TASK-ID>` — TDD inner loop

Este diagrama expande o menor orquestrador de implementação. Ele mantém o Double-Loop TDD em código: o runtime decide ciclo, valida RED/GREEN/REFACTOR e chama LLM apenas para gerar teste/código quando necessário.

```mermaid
sequenceDiagram
  autonumber
  participant Story as ndp story implement
  participant Task as ndp task implement
  participant Gate as Policy/Gate Engine
  participant State as Execution State
  participant Art as Artifact Store
  participant Git as Git Adapter
  participant Build as Build/Test Adapter
  participant LLM as LLM Worker
  participant CI as CI Watch
  participant Tele as Telemetry/Audit

  Story->>Task: implement TASK-ID
  Task->>Tele: run.start(task implement)
  Task->>Gate: assertPre(Phase 0 - Setup)
  Task->>Art: resolve task file, task plan, implementation map
  Task->>Git: detect worktree context
  Task->>State: persist interactiveMode
  Task->>Gate: assertPost(Phase 0 - Setup)

  Task->>Gate: assertPre(Phase 1 - Prepare)
  Task->>Art: load KPs, plan, contracts, acceptance criteria
  Task->>LLM: prepare TDD implementation plan
  LLM-->>Task: ordered TDD cycles
  Task->>Gate: assertPost(Phase 1 - Prepare)

  Task->>Gate: assertPre(Phase 2 - TDD)
  loop each TDD cycle
    Task->>LLM: write failing test for next behavior
    LLM-->>Task: test patch
    Task->>Build: run targeted tests expecting RED
    alt test does not fail
      Task->>Gate: raise RED_NOT_OBSERVED
    else RED observed
      Task->>Git: commit RED test
    end
    Task->>LLM: write minimal implementation
    LLM-->>Task: implementation patch
    Task->>Build: run tests expecting GREEN
    alt tests fail
      Task->>LLM: repair minimal implementation
      Task->>Build: rerun tests
    end
    Task->>Git: commit GREEN implementation
    Task->>LLM: propose refactor if useful
    LLM-->>Task: refactor patch or no-op
    Task->>Build: rerun tests
    alt refactor broke tests
      Task->>Gate: raise REFACTOR_BROKE_TESTS
    else tests stay green
      Task->>Git: commit REFACTOR when changed
    end
  end
  Task->>Gate: assertWave(all TDD cycle tasks complete)

  Task->>Gate: assertPre(Phase 3 - Validate)
  Task->>Build: run acceptance tests and coverage
  Build-->>Task: coverage and test envelope
  Task->>Art: update task status and task map row
  Task->>Gate: assertPost(Phase 3 - Validate)

  Task->>Gate: assertPre(Phase 4 - Commit)
  Task->>Git: create final atomic task commit if needed
  opt standalone worktree PR
    Task->>CI: watch PR checks
    CI-->>Task: ci status file
  end
  Task->>Gate: assertPost(Phase 4 - Commit)

  Task->>Gate: assertPre(Phase 5 - Cleanup)
  Task->>Git: cleanup worktree according to mode
  Task->>Gate: assertFinal(Phase 5 - Cleanup)
  Task->>Tele: run.end(task implement)
  Task-->>Story: task envelope
```

#### 3.5.5. PR, CI-watch e auto-merge

Este diagrama detalha o fluxo que hoje é repartido entre `x-pr-create`, `x-pr-watch-ci`, `x-pr-merge` e renderização de PR body.

```mermaid
sequenceDiagram
  autonumber
  participant Caller as Story/Task/Epic Runtime
  participant PR as PR Service
  participant Gate as Policy/Gate Engine
  participant Art as Artifact Store
  participant Render as PR Body Renderer
  participant GitHub as GitHub Adapter
  participant CI as CI Watch Service
  participant Merge as Merge Service

  Caller->>PR: create PR envelope {head, target, kind, autoMerge}
  PR->>Gate: validate branch, task/story/epic ids, target policy
  PR->>Gate: preflight tests and evidence availability
  PR->>Render: render PR body
  Render->>Art: read story report, verify envelope, review paths, telemetry pointers
  Render-->>PR: body with Orchestrator Evidence
  PR->>GitHub: create PR with labels and body
  GitHub-->>PR: prNumber, prUrl
  PR->>CI: watch checks unless disabled by recovery policy
  CI-->>Art: write .claude/state/pr-watch-{PR}.json
  CI-->>PR: ci status
  alt autoMerge != none and CI green
    PR->>Merge: merge PR with selected strategy
    Merge->>GitHub: merge
    GitHub-->>Merge: merge result
    Merge-->>PR: prMergeStatus
  else manual or blocked
    PR-->>Caller: PR left open with evidence
  end
  PR-->>Caller: PR envelope
```

#### 3.5.6. Review gates — especialistas e Tech Lead

Este diagrama detalha as chamadas `ndp review` e `ndp review pr`, acionadas dentro de `story implement` e também úteis como comandos públicos.

```mermaid
sequenceDiagram
  autonumber
  participant Story as ndp story implement
  participant Review as ndp review
  participant TL as ndp review pr
  participant Gate as Policy/Gate Engine
  participant LLM as Specialist Worker Pool
  participant Build as Build/Test Adapter
  participant Art as Artifact Store
  participant Fix as ndp pr fix

  Story->>Review: run specialist review for STORY-ID
  Review->>Art: idempotency check for existing reports
  Review->>Review: detect diff and active specialists
  Review->>Gate: assertPre(SpecialistReviews)
  par QA
    Review->>LLM: qa review worker
  and Performance
    Review->>LLM: perf review worker
  and Security when active
    Review->>LLM: security review worker
  and Database/API/Event/DevOps when active
    Review->>LLM: conditional specialist workers
  end
  LLM-->>Review: review reports and scores
  Review->>Art: write review reports and dashboard
  Review->>Gate: assertWave(review reports exist)
  Review-->>Story: specialist dashboard

  Story->>TL: run Tech Lead review
  TL->>Art: load specialist dashboard, plans, tests, PR diff
  TL->>Build: compile, test, coverage, smoke when configured
  TL->>LLM: holistic 57-point review
  LLM-->>TL: GO or NO-GO report
  TL->>Art: write techlead review and update dashboard
  alt NO-GO and auto remediation enabled
    TL->>Fix: apply actionable fixes
    Fix-->>TL: fix result
    TL->>Build: rerun compile/tests
    TL->>LLM: rerun focused review
  end
  TL->>Gate: assertFinal(techlead report and dashboard)
  TL-->>Story: final review verdict
```

#### 3.5.7. Implicação para o design do runtime

Esses diagramas revelam a estrutura real do produto:

- `ndp epic implement` é um **composite command** que não deve conter lógica de story/task/review inline; ele coordena envelopes e policies.
- `ndp story implement` é o principal command de delivery: ele integra planejamento, task loop, docs, verify, review e report.
- `ndp task implement` é o inner loop TDD, onde a maior parte da criação de código acontece.
- PR/CI/review são subdomínios reutilizáveis, não detalhes acidentais de story.
- Cada seta que escreve em disco deve produzir um `artifact_kind` tipado.
- Cada `alt` de erro/recovery deve virar exceção tipada, política de retry ou estado persistido.

---

## 4. Inventário Canônico de Ativos Atuais e Destino NDP

Esta é a seção de preservação de contexto. Ela evita duplicação separando a pergunta em camadas:

- **Skills/comandos/workers:** quem executa ou gera algo?
- **Rules/policies:** quais invariantes precisam sobreviver?
- **KPs:** qual conhecimento alimenta workers?
- **Hooks/scripts:** quais invariantes shell precisam virar runtime?
- **Templates:** quais estruturas renderizam artefatos?
- **Artefatos:** o que aparece em disco e quem consome?

### 4.1. Skills e comandos atuais, classificados uma única vez

#### 4.1.1. Orquestradoras públicas — viram comandos NDP

Estas skills controlam fluxo amplo. No NDP elas devem sair de markdown interpretado e virar comandos CLI com state machine, persistência, gates, telemetria, retries e saída estruturada.

| Skill atual | Destino NDP V0 | Motivo |
| --- | --- | --- |
| `x-epic-implement` | `ndp epic implement <ID>` | Implementação de épico em fases, waves, gates e PRs. |
| `x-story-implement` | `ndp story implement <ID>` | Lifecycle end-to-end de story: planning, task execution, PR, review, verify, report. |
| `x-task-implement` | `ndp task implement <ID>` | TDD double-loop, validações, commits e PR/task. |
| `x-release` | `ndp release [--patch\|--minor\|--major]` | Versionamento, changelog, release branch, tag e back-merge. |
| `x-epic-orchestrate` | `ndp epic orchestrate <ID>` | Planejamento multi-story com checkpoints e resume. |
| `x-pr-merge-train` | `ndp merge-train` | Ordenação topológica de PRs, waves, CI e merge. |
| `x-review` | `ndp review <STORY>` | Fan-out/fan-in de especialistas e consolidação. |
| `x-review-pr` | `ndp review pr <PR>` | Veredito Tech Lead GO/NO-GO. |
| `x-story-refine` | `ndp story refine <ID>` | Refinement multi-persona com verdict persistido. |
| `x-epic-refine` | `ndp epic refine <ID>` | Refinement estratégico de épico. |
| `x-story-plan` | `ndp story plan <ID>` | Planning wave, task breakdown, task plans e DoR. |
| `x-feature-create` | `ndp feature create <SPEC>` | Spec → epic → stories → implementation map → PR. |
| `x-feature-ideate` | `ndp feature ideate` | Ideia livre → spec/backlog estruturado. |
| `x-test-tdd` | `ndp test tdd <TASK>` | Orquestra ciclos Red/Green/Refactor; LLM atua pontualmente. |

#### 4.1.2. Orquestradoras auxiliares — comando público, subcomando avançado ou serviço

Estas coordenam fluxo suficiente para não serem leaf prompts. A visibilidade pública deve ser decidida por UX, não por necessidade do LLM.

| Skill auxiliar | Destino provável |
| --- | --- |
| `x-code-audit` | `ndp code audit` ou parte de `ndp ci verify`. |
| `x-lib-audit-rules` | Serviço interno / `ndp lint policy`. |
| `x-doc-generate` | `ndp doc generate` e fase interna de story/release. |
| `x-template-migrate` | `ndp template migrate`. |
| `x-pr-create` | `ndp pr create` e serviço interno de PR. |
| `x-pr-fix` | `ndp pr fix <PR>`. |
| `x-pr-fix-epic` | `ndp pr fix --epic <EPIC>`. |
| `x-pr-watch-ci` | `ndp pr watch <PR>` com exit codes tipados. |
| `x-pr-merge` | `ndp pr merge` ou serviço interno usado por merge-train/release. |
| `x-git-push`, `x-git-commit`, `x-git-worktree`, `x-git-cleanup-branches` | Subcomandos `ndp git ...` e serviços transacionais internos. |
| `x-status-reconcile` | `ndp status reconcile` para recovery/admin. |
| `x-ci-generate` | `ndp ci generate`. |
| `x-setup-env` | `ndp setup env` ou `ndp doctor`. |
| `x-perf-profile` | `ndp perf profile`. |
| `x-ops-troubleshoot` | `ndp troubleshoot` ou worker acionado por falhas. |
| `x-ops-incident` | Comando opcional; provável V1+ ou plugin ops. |
| `x-jira-create-epic`, `x-jira-create-stories` | Plugins `ndp jira ...`, fora do core offline. |
| `x-adr-generate` | `ndp adr generate` e fase interna de arquitetura. |
| `x-owasp-scan`, `x-security-dashboard`, `x-security-pentest` | `ndp security ...`, alguns condicionais por capability/permissão. |

#### 4.1.3. Serviços internos — viram código testável

Estas skills existem hoje porque o LLM precisava chamar componentes internos por nome. No NDP, elas viram classes, serviços ou funções.

| Grupo interno | Skills atuais | Forma em NDP |
| --- | --- | --- |
| Gates | `x-internal-phase-gate`, `x-internal-story-verify`, `x-internal-epic-integrity-gate` | `PhaseGateService`, `StoryVerifyService`, `EpicIntegrityGate`. |
| Estado e contexto | `x-internal-status-update`, `x-internal-story-resume`, `x-internal-story-load-context`, `x-internal-args-normalize` | Repositório de estado, loader de contexto, parser tipado de args. |
| Planejamento | `x-internal-story-build-plan`, `x-internal-epic-build-plan`, `x-lib-task-decomposer` | Builders e dispatchers internos de workers. |
| Renderização | `x-internal-report-write`, `x-internal-pr-body-render`, `x-internal-story-report` | Renderers tipados com templates versionados. |
| Git/precheck | `x-internal-epic-branch-ensure`, `x-internal-worktree-precheck`, `x-lib-group-verifier` | Serviços de branch, worktree, locks e wave verification. |
| Criação interna | `x-internal-epic-create`, `x-internal-story-create`, `x-internal-epic-map` | Factories/renderers de backlog e mapa. |

#### 4.1.4. Skills puras — permanecem isoladas como workers, adapters ou comandos utilitários

Estas já têm uma responsabilidade dominante. O NDP deve portá-las sem inflar escopo. Elas não devem ganhar state machine própria nem coordenar lifecycle amplo.

| Grupo | Skills puras | Destino NDP |
| --- | --- | --- |
| `conditional/dev` | `x-setup-stack` | Adapter/comando de setup stack-specific. |
| `conditional/ops` | `x-obs-instrument` | Worker/adapter de instrumentação. |
| `conditional/review` | `x-review-api`, `x-review-compliance`, `x-review-data-modeling`, `x-review-db`, `x-review-devops`, `x-review-events`, `x-review-gateway`, `x-review-graphql`, `x-review-grpc`, `x-review-obs`, `x-review-security` | Workers especialistas que consomem KPs/policies. |
| `conditional/security` | `x-security-container`, `x-security-dast`, `x-security-infra`, `x-security-sast`, `x-security-secrets`, `x-security-sonar` | Security adapters/workers por superfície. |
| `conditional/test` | `x-test-contract-lint`, `x-test-contract`, `x-test-e2e`, `x-test-perf`, `x-test-smoke-api`, `x-test-smoke-socket` | Test adapters por categoria. |
| `core/code` | `x-code-format`, `x-code-lint` | Adapters determinísticos de format/lint. |
| `core/dev` | `helidon-scaffold`, `micronaut-scaffold`, `picocli-command`, `quarkus-resource`, `spring-controller` | Templates stack-specific + render command. |
| `core/dev` | `x-ci-generate`, `x-mcp-recommend`, `x-setup-env`, `x-spec-drift` | CI renderer, advisor command, setup/doctor, drift report. |
| `core/git` | `x-git-branch`, `x-git-cleanup-branches`, `x-git-commit`, `x-git-merge`, `x-git-push`, `x-git-worktree`, `x-planning-commit` | Git adapters transacionais. |
| `core/jira` | `x-jira-create-epic`, `x-jira-create-stories` | Plugins Jira. |
| `core/lib` | `x-lib-group-verifier`, `x-lib-task-decomposer` | Serviços internos reutilizáveis. |
| `core/ops` | `x-doc-generate`, `x-doc-validate`, `x-ops-incident`, `x-ops-troubleshoot`, `x-perf-profile`, `x-release-changelog`, `x-status-reconcile`, `x-telemetry-analyze`, `x-telemetry-trend` | Docs, ops, profiling, changelog, telemetry commands. |
| `core/plan` | `planning-standards-kp`, `x-adr-generate`, `x-arch-plan`, `x-arch-system-update`, `x-arch-update`, `x-parallel-eval`, `x-task-plan`, `x-template-migrate`, `x-threat-model` | KP, worker prompts, docs updates, validators e planners. |
| `core/pr` | `x-pr-create`, `x-pr-fix`, `x-pr-merge`, `x-pr-watch-ci` | PR commands/services. |
| `core/review` | `x-review-perf`, `x-review-pr`, `x-review-qa` | Review workers/commands. |
| `core/security` | `x-dependency-audit`, `x-hardening-eval`, `x-owasp-scan`, `x-runtime-eval`, `x-security-dashboard`, `x-security-pipeline`, `x-supply-chain-audit` | Security commands/adapters. |
| `core/test` | `x-test-plan`, `x-test-run` | Test plan worker + test runner adapter. |

#### 4.1.5. Skills candidatas a virar KP, policy ou template

| Ativo atual | Tipo-alvo provável | Justificativa |
| --- | --- | --- |
| `planning-standards-kp` | `knowledge-pack` | Já é uma fonte RA9; deve sair do catálogo de comandos. |
| `x-mcp-recommend` | KP + advisor command | O catálogo de MCPs é conhecimento; o comando só aplica matching. |
| Scaffolds (`spring-controller`, `quarkus-resource`, etc.) | `template` + render command | O valor principal é estrutura stack-specific. |
| Review specialists | `worker-prompt` + KP/policy externo | O review permanece ação, mas critérios devem sair do prompt e virar KP/policy. |
| `x-internal-phase-gate`, `x-internal-story-verify`, `x-internal-epic-integrity-gate` | `policy` / gate service | São invariantes de lifecycle. |
| `x-doc-validate` | Policy + command | O critério é policy; a execução é command/gate. |
| `x-lib-audit-rules` | Registry/policy validator | Deve validar registry, rules e policies tipadas. |
| `audit-*.sh`, `verify-*.sh`, `enforce-*.sh` | Policy executable | Não são skills; são invariantes que migram para runtime/CI. |

### 4.2. Rules e policies

| Domínio de rule atual | Exemplos atuais | Destino NDP |
| --- | --- | --- |
| Identidade, domínio e contexto | Rules 01, 02 | Doctrine humana + defaults de profile. |
| Coding standards, arquitetura, quality gates | Rules 03, 04, 05 | Thresholds/limites viram policy; explicações viram KP. |
| Segurança, operações, compliance | Rules 06, 07, conditional rules | Policy packs por domínio regulado + KP de referência. |
| Branching, release, Git Flow | Rules 08, 09, 21 | Serviços de branch/release e CI de consistência. |
| Skill invocation, visibility, capability grammar | Rules 13, 22, 28 | Registry/linter tipado; tool-call grammar vira schema/AST. |
| Model selection e custo | Rule 23 | Policy no model router. |
| Execution integrity e zero-bypass | Rules 24, 27 | Propriedade arquitetural do runtime; CI valida evidência. |
| Task hierarchy e phase gates | Rule 25 | State machine + phase gate service. |
| Audit lifecycle | Rule 26 | Biblioteca de auditoria; taxonomy curta em docs. |
| Refinement e DoR | Rule 29 | Pré-condição nativa dos comandos de implementação. |
| Documentation as DoD | Rule 31 | Policy de doc freshness + KP stack-aware. |

Decisão: rules críticas ganham `policy_id`, versão, testes e ponto de execução claro: runtime, CI Camada B ou doctrine humana.

### 4.3. Knowledge Packs

| KP/domínio | Destino NDP | Consumo típico |
| --- | --- | --- |
| `architecture`, `layer-templates`, `patterns` | KP oficial de arquitetura + templates stack-specific. | `ndp arch plan`, scaffolds, code workers. |
| `coding-standards` | KP de engenharia + ponte para policies executáveis. | `ndp task implement`, `ndp review`, `ndp code audit`. |
| `testing`, `story-planning`, `planning-standards-kp` | KP de TDD, TPP, RA9 e decomposição. | `ndp story plan`, `ndp task plan`, `ndp test tdd`. |
| `security`, `compliance` | KP base + overlays regulados (`pci`, `hipaa`, `lgpd`, `soc2`). | `ndp review security`, `ndp threat model`, `ndp ci verify`. |
| `observability`, `resilience`, `infrastructure`, `dockerfile` | KPs condicionais por capability runtime/infra. | `ndp ops`, `ndp review devops`, `ndp perf profile`. |
| `api-design`, `protocols` | KP por interface (`rest`, `grpc`, `graphql`, `event`). | `ndp review api`, `ndp arch plan`, contract tests. |

KP não deve ter side effect nem ser invocado como comando principal. Se houver UX de consulta, ela deve ser algo como `ndp explain <topic>`, não um lifecycle step.

### 4.4. Hooks e scripts de validação

Os hooks e scripts atuais são importantes porque explicitam invariantes. Eles não devem sobreviver como mecanismo primário, mas seus contratos devem sobreviver como código.

#### 4.4.1. Gatilhos atuais

| Gatilho | Hooks/scripts envolvidos | Responsabilidade |
| --- | --- | --- |
| `SessionStart` | `telemetry-session.sh` | Abre trilha de telemetria da sessão. |
| `PreToolUse` | `telemetry-pretool.sh`, `enforce-phase-sequence.sh`, `enforce-no-bypass-flags.sh`, `enforce-refinement-gate.sh`, `enforce-preflight-gates.sh` | Mede tool call e bloqueia fase inválida, bypass, ausência de refinement ou operação remota sem preflight. |
| `PostToolUse` (`Write`/`Edit`) | `post-compile-check.sh` | Compila após edição Java. |
| `PostToolUse` (`*`) | `telemetry-posttool.sh` | Fecha medição de tool call. |
| `SubagentStop` | `telemetry-subagent.sh` | Registra encerramento de subagente. |
| `Stop` | `telemetry-stop.sh`, `verify-story-completion.sh`, `verify-phase-gates.sh`, `enforce-continuous-flow.sh`, `stage-telemetry.sh` | Fecha sessão, verifica evidências, alerta gates e prepara telemetria. |
| PR/CI/`mvn verify` | `scripts/audit-*.sh`, `*AuditTest.java` | Validação detectiva no repositório. |

#### 4.4.2. Migração dos invariantes

| Script/hook/família | Invariante | Destino NDP |
| --- | --- | --- |
| `enforce-phase-sequence.sh`, `verify-phase-gates.sh`, `audit-phase-gates.sh` | Não avançar fase sem filhos/evidências/gates passados. | `PhaseGateService` + testes + `ndp ci verify`. |
| `enforce-no-bypass-flags.sh`, `audit-bypass-flags.sh` | Bypass flags só em recovery. | Parser tipado de flags + policy de recovery. |
| `enforce-refinement-gate.sh`, `audit-refinement-gate.sh` | Implementação exige refinement aprovado. | Pré-condição dos commands. |
| `enforce-preflight-gates.sh`, `scripts/preflight.sh` | Operação remota exige estado local íntegro. | Preflight in-process antes de push/PR. |
| `post-compile-check.sh` | Edição Java não deve quebrar compile. | Build adapter por stack. |
| `verify-story-completion.sh`, `audit-execution-integrity.sh` | PR/story exige evidência completa. | Completion gate + CI Camada B. |
| `enforce-continuous-flow.sh` | Orquestração não deve ficar parada em fase aberta. | Scheduler/state machine do runtime. |
| `telemetry-*`, `telemetry-phase.sh`, `stage-telemetry.sh` | Eventos de sessão/tool/fase/subagente precisam ser emitidos. | Telemetria in-process + audit log local. |
| `audit-doc-freshness.sh` | Doc-as-DoD. | Documentation policy + `ndp doc validate`. |
| `audit-template-version.sh`, `audit-flow-version.sh` | Templates e flowVersion precisam ser compatíveis. | Schema validators + migration assistant. |
| `audit-epic-branches.sh` | Branching de epic precisa ser consistente. | Branch policy service. |
| `audit-skill-visibility.sh`, `audit-model-selection.sh`, `audit-capability-graph.sh` | Registry, modelo e capability graph precisam ser íntegros. | Registry linter + resolver tipado. |

Decisão: nenhum hook shell deve ser mecanismo primário da V0. Para cada hook/script removido, criar teste no NDP cobrindo o mesmo invariante e rodar dual-mode por 1 release.

### 4.5. Templates

Templates são estruturas reutilizáveis. Eles não são artefatos finais; eles definem a forma dos artefatos.

| Família | Exemplos | Consumidores atuais | Destino NDP |
| --- | --- | --- | --- |
| Planning product | `_TEMPLATE-EPIC.md`, `_TEMPLATE-STORY.md`, `_TEMPLATE-TASK.md`, `_TEMPLATE-IMPLEMENTATION-MAP.md`, `_TEMPLATE-DOR-CHECKLIST.md` | Epic/story/feature creation, planning/refinement. | Template registry + schemas de backlog. |
| Execution governance | `_TEMPLATE-IMPLEMENTATION-PLAN.md`, `_TEMPLATE-TASK-BREAKDOWN.md`, `_TEMPLATE-EPIC-EXECUTION-PLAN.md`, `_TEMPLATE-STORY-COMPLETION-REPORT.md`, `_TEMPLATE-EXECUTION-STATE.json`, `_TEMPLATE-REFINEMENT-VERDICT.md` | Story/epic implement, reports, status update. | Renderer determinístico + schemas para estado/evidência. |
| Review governance | `_TEMPLATE-ARCHITECTURE-PLAN.md`, `_TEMPLATE-SPECIALIST-REVIEW.md`, `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md`, `_TEMPLATE-TECH-LEAD-REVIEW.md`, `_TEMPLATE-REVIEW-REMEDIATION.md` | Review, review-pr, remediation. | Worker review templates + structured verdicts. |
| Security, compliance, quality | `_TEMPLATE-SECURITY-ASSESSMENT.md`, `_TEMPLATE-COMPLIANCE-ASSESSMENT.md`, `_TEMPLATE-THREAT-MODEL.md`, `_TEMPLATE-SLO-SLI-DEFINITION.md`, `_TEMPLATE-TEST-PLAN.md` | Planning, threat model, test plan. | Security/testing templates tied to policies. |
| Documentation | `_TEMPLATE-ADR.md`, `_TEMPLATE-ARCHITECTURE-SYSTEM.md`, `_TEMPLATE-DOC-VALIDATE-REPORT.md`, `_TEMPLATE-CONTRIBUTING.md`, `CLAUDE.md`, `SYSTEM_SPECS.md` | Docs generation, ADR, target adapters. | Generated documentation views. |
| Observability/ops | `_TEMPLATE-TELEMETRY-EVENT.json`, `_TEMPLATE-TELEMETRY-REPORT.md`, runbooks | Telemetry hooks, telemetry analyze, ops skills. | Event schemas + report renderers. |
| Git/PR/release | `_TEMPLATE-CHANGELOG-ENTRY.md`, `_TEMPLATE-RELEASE-CHECKLIST.md`, `_TEMPLATE-PR-IMPLEMENTATION.md`, `_TEMPLATE-PR-BACKLOG.md` | Release/changelog/PR body rendering. | Strict renderers with evidence schema. |
| Meta-generator/infra | `_TEMPLATE-SKILL.md`, `constitution/*`, `domains/**`, `fragments/*`, `config-templates/*`, `cicd-templates/**/*.njk` | Generator, authoring, CI/CD assembler. | Template/profile/fragment registry. |

Decisões:

- Todo template ganha `template_id`, versão, categoria, input schema e consumidores.
- JSON templates como `_TEMPLATE-EXECUTION-STATE.json` viram schemas, não texto copiado.
- Templates `.njk`/YAML pertencem à composition engine, não ao lifecycle de skills.
- Goldens continuam testes de regressão, não fonte de verdade.

### 4.6. Artefatos padrão gerados

Artefatos são instâncias persistidas no disco. A fonte de layout v4 é `ai/README.md`: `ai/epics/epic-XXXX-<slug>/` concentra épico, stories, planos, relatórios, telemetria e estado; `ai/releases/` guarda release state; `ai/runs/` guarda artefatos por sessão/execução. Fluxos legados podem usar `plans/epic-XXXX/`, mas a semântica é a mesma.

#### 4.6.1. Raiz do épico

| Artefato | Objetivo | Quem gera | Consumidores |
| --- | --- | --- | --- |
| `epic-XXXX.md` / `EPIC-XXXX.md` | Fonte normativa do backlog do épico. | `x-epic-create`, `x-epic-decompose`, `x-feature-create`, builders internos. | Operadores, story creation, refinement, orchestrators, audits. |
| `story-XXXX-YYYY.md` | Contrato implementável da story. | `x-story-create`, `x-epic-decompose`, builders internos. | `x-story-implement`, `x-task-implement`, reviews, CI. |
| `IMPLEMENTATION-MAP.md` | DAG e fases entre stories. | `x-epic-map`, `x-feature-create`, `x-epic-decompose`. | `x-epic-implement`, `x-epic-orchestrate`, `x-parallel-eval`. |
| `execution-state.json` | Checkpoint de orquestração. | Orchestrators e status services. | Resume, phase gates, refinement gate, continuous flow, runtime NDP. |
| `epic-execution-plan.md` | Plano materializado do épico. | Epic build plan / implement. | `x-epic-implement`, relatórios, auditoria humana. |
| `epic-execution-report.md` | Encerramento agregado do épico. | `x-epic-implement`, report writer. | Release train, stakeholders, CI. |
| `spec-*.md` | Entrada de decomposição. | Humano ou feature pipeline. | Epic/story creation, refinement. |

#### 4.6.2. `plans/`

| Artefato | Objetivo | Quem gera | Consumidores |
| --- | --- | --- | --- |
| `arch-story-*.md` | Plano arquitetural. | `x-arch-plan` / build-plan. | Implementação, `x-arch-update`, reviews, Surface 07. |
| `plan-story-*.md` | Plano de implementação. | `x-internal-story-build-plan`. | Task implement, context loader, verify. |
| `tests-story-*.md` | Plano TDD/TPP. | `x-test-plan` / build-plan. | Task implement, QA, coverage. |
| `tasks-story-*.md` | Decomposição de tasks. | `x-lib-task-decomposer`. | Task implement, execution state. |
| `plan-task-*.md` / `task-plan-TASK-*.md` | Plano por task. | `x-task-plan`, `x-story-plan`. | Task implement, parallel eval. |
| `security-story-*.md` | Avaliação de segurança. | Build-plan Phase 1E. | Security review, compliance. |
| `compliance-story-*.md` | Avaliação compliance. | Build-plan Phase 1F. | Compliance gates. |
| `planning-report-story-*.md` / `story-planning-report-*.md` | Relatório de planejamento. | `x-story-plan`, `x-epic-orchestrate`. | DoR, resume, operadores. |
| `dor-story-*.md` | Definition of Ready. | `x-story-plan`. | `x-epic-orchestrate`, planning gate. |
| `review-*-story-*.md` | Review especialista. | `x-review`. | Dashboard, remediation, Surface 04. |
| `review-dashboard-story-*.md` | Consolidação de reviews. | `x-review`. | Tech lead, story owner. |
| `techlead-review-story-*.md` | Veredito GO/NO-GO. | `x-review-pr`. | Merge gate, Surface 05. |
| `remediation-story-*.md` | Plano de correções pós-review. | Story implement remediation phase. | PR fixes, implementation. |

O pacote de Fase 1 em fluxos zero-bypass é tipicamente: arch plan, implementation plan, test plan, task breakdown, security assessment e compliance assessment.

#### 4.6.3. `reports/`

| Artefato | Objetivo | Quem gera | Consumidores |
| --- | --- | --- | --- |
| `story-completion-report-STORY-ID.md` | Prova de fechamento da story. | `x-internal-story-report`. | Operadores, merge checklist, execution integrity audit. |
| `verify-envelope-STORY-ID.json` | Envelope estruturado do verify gate. | `x-internal-story-verify`. | CI, audits, Surface 03. |
| `verify-envelope-epic-XXXX.json` | Verificação de épico. | `x-internal-epic-integrity-gate`. | CI, release. |
| `dependency-audit-STORY-ID.md` | Evidência supply chain. | `x-dependency-audit`. | Security, PR evidence, Surface 08. |
| `doc-validate-report-STORY-ID.md` | Evidência doc-as-DoD. | `x-doc-validate`. | Stop hook, CI doc freshness. |
| `phase-report-epic-XXXX.md` | Relatório de fase. | `x-epic-implement`. | Epic orchestration, stakeholders. |
| `epic-planning-report-XXXX.md` | Planejamento consolidado. | `x-epic-orchestrate`. | Equipe, resume/replanning. |

#### 4.6.4. Telemetria, releases e adjacentes

| Artefato | Objetivo | Quem gera | Consumidores |
| --- | --- | --- | --- |
| `telemetry/events.ndjson` | Trilha auditável de fases/tools/subagentes. | Hooks e `telemetry-phase.sh` hoje; NDP runtime no futuro. | `x-telemetry-analyze`, `x-telemetry-trend`, audit, Surface 12. |
| `ai/releases/release-state-X.Y.Z.json` | Estado monotônico de release. | `x-release`. | Próximo release, CI, operadores. |
| `ai/runs/*` | Evidência por sessão/execução. | Ferramentas, hooks ou ops skills. | Troubleshooting/forensics. |
| `tasks/task-TASK-*.md` | Contrato task-first. | `x-story-plan`, `x-task-plan`. | `x-task-implement`. |
| `contracts/{STORY_ID}-*.yaml\|proto` | Contratos API-first. | Story implement Phase 0.5. | Contract lint, implementação, API review. |
| `.claude/state/pr-watch-{PR}.json` | Estado de CI-watch. | `x-pr-watch-ci`. | Stop hook, operadores, Surface 06. |
| PR body `## Orchestrator Evidence` | Ponte entre GitHub e evidências locais. | `x-pr-create`, PR body renderer. | Revisores, CI audit, Surface 11. |
| `governance/baselines/*.txt` | Exceções explícitas. | Humanos/scripts de baseline. | CI auditors, hotfix exceptions. |

Implicação NDP: cada artefato vira `artifact_kind` com schema, gerador autorizado e consumidores declarados. O runtime deixa de inferir por path e passa a validar contratos.

---

## 5. Taxonomia Canônica de Domínios

A taxonomia abaixo evita que o registry seja uma lista plana de skills. Um pacote pode conter policies, KPs, templates, commands e artifact kinds, desde que todos pertençam a um domínio coerente.

| Domínio | O que governa | Policies | KPs | Commands/workers/artifacts |
| --- | --- | --- | --- | --- |
| `engineering-standards` | Como código deve ser escrito e mantido. | Coding standards, quality gates, limites. | `coding-standards`, `patterns`, partes de `layer-templates`. | `x-code-format`, `x-code-lint`, `x-code-audit`, review specialists. |
| `architecture-standards` | Estrutura, camadas, APIs e decisões. | Dependency rules, architecture constraints. | `architecture`, `api-design`, `protocols`, `resilience`. | `x-arch-plan`, `x-arch-update`, scaffolds/templates. |
| `security-compliance` | Segurança mínima e regulação. | Security baseline, compliance gates, SARIF, evidence. | `security`, `compliance`, overlays. | `x-owasp-scan`, `x-dependency-audit`, `x-supply-chain-audit`. |
| `testing-quality` | Como provar comportamento. | Coverage, TDD, acceptance, smoke/contract rules. | `testing`, `story-planning`. | `x-test-plan`, `x-test-run`, `x-test-tdd`, contract/e2e/perf/smoke. |
| `planning-product` | Como transformar intenção em backlog. | Refinement gate, DoR, value-driven templates, flow version. | `story-planning`, `planning-standards-kp`. | `x-story-plan`, `x-task-plan`, `x-feature-create`, templates backlog. |
| `execution-governance` | Integridade, anti-bypass, phase gates, lifecycle. | Execution Integrity, Zero-bypass, Task Hierarchy, Audit Lifecycle. | Lifecycle guidance curta. | Orchestrators, internal gates, verify envelopes, reports. |
| `git-release-pr` | Branches, commits, PRs, release. | Branching, release process, CI-watch, PR evidence. | Git/release workflow guidance. | `x-git-*`, `x-pr-*`, `x-release`, merge train. |
| `documentation` | Documentação como DoD. | Doc freshness, ADR, changelog, system architecture. | Architecture docs, API docs, ADR/changelog knowledge. | `x-doc-generate`, `x-doc-validate`, `x-adr-generate`, reports. |
| `observability-ops` | Telemetria, operação, incidentes. | Telemetry privacy, ops baseline. | `observability`, `infrastructure`, `dockerfile`, `resilience`. | telemetry runtime, `x-telemetry-*`, `x-ops-*`, runbooks. |
| `review-governance` | Critérios e vereditos de review. | Review evidence, GO/NO-GO schema. | Security, testing, architecture, API, observability. | `x-review`, `x-review-pr`, specialist reviews, dashboards. |
| `capability-registry` | Como ativos são descritos e distribuídos. | Capability schema, visibility, model selection. | Governance authoring guidance. | registry linter, capability graph, frontmatter migration. |
| `ecosystem-integrations` | Integrações externas e limites de plugins. | Permission model, provider boundaries. | MCP/Jira/GitHub/provider docs. | `x-mcp-recommend`, Jira commands, marketplace plugins. |

Decisão de produto: essa taxonomia vira a navegação oficial do NDP para capability packaging, marketplace, documentação e `ndp explain`.

---

## 6. Roadmap de Produto

### 6.1. Project

**Project:** `NextGen-Dev-Platform` (`NDP`)

Hierarquia: `Project → Product → Capacity → Feature`. A marcação indica release alvo:

- `[V0]`: CLI local-first, core obrigatório.
- `[V1+]`: expansão de UX, marketplace ou integração, depois do core.
- `[V2+]`: cloud, multi-tenancy, analytics cross-project ou interface avançada.

### 6.2. Product P1 — Core Engine

Evolução direta do gerador Java. Continua sendo fonte da verdade de composition/governance, mas passa a ser multi-target e multi-LLM.

#### P1.C1 — Configuração & Profile Management

- P1.C1.F1 `[V0]`: Schema unificado de profile com JSON-Schema versionado.
- P1.C1.F2 `[V0]`: Migração assistida v5 (`ia-dev-env`) → v6 (`NDP`) com `ndp migrate --from-iadev`.
- P1.C1.F3 `[V0]`: Profile inheritance & overlays.
- P1.C1.F4 `[V1+]`: Detecção automática de stack.

#### P1.C2 — Capability Composition v2

- P1.C2.F1 `[V0]`: Capability resolver com cache local.
- P1.C2.F2 `[V0]`: Frontmatter v4 com `provides:`.
- P1.C2.F3 `[V1+]`: Plug-in capabilities externas.
- P1.C2.F4 `[V0]`: Composition diff & dry-run.

#### P1.C3 — Artifact Generation Multi-Target

- P1.C3.F1 `[V0]`: Target adapter `claude-code`.
- P1.C3.F2 `[V0]`: Target adapter `cursor`.
- P1.C3.F3 `[V1+]`: Targets `windsurf`, `aider`, `gemini-cli`, `codex-cli`.
- P1.C3.F4 `[V1+]`: Target adapter `generic-mcp`.
- P1.C3.F5 `[V0]`: Overlay system para customizações sem perder regen.

#### P1.C4 — Multi-Stack & Multi-Language

- P1.C4.F1 `[V0]`: Catálogo de stacks oficial.
- P1.C4.F2 `[V1+]`: Stack templates community-contributed.
- P1.C4.F3 `[V2+]`: Polyglot monorepos.
- P1.C4.F4 `[V0]`: Reutilização de KPs via taxonomia comum.

### 6.3. Product P2 — Orchestration Runtime

Produto-âncora da V0. Substitui markdown interpretado por state machines em código.

#### P2.C0 — Comandos orquestradores nativos

- P2.C0.F1 `[V0]`: `ndp epic implement <ID>`.
- P2.C0.F2 `[V0]`: `ndp story implement <ID>`.
- P2.C0.F3 `[V0]`: `ndp task implement <ID>`.
- P2.C0.F4 `[V0]`: `ndp story refine <ID>` / `ndp epic refine <ID>`.
- P2.C0.F5 `[V0]`: `ndp review <STORY>` / `ndp review pr <PR>`.
- P2.C0.F6 `[V0]`: `ndp release`.
- P2.C0.F7 `[V0]`: `ndp epic orchestrate <ID>`.
- P2.C0.F8 `[V0]`: `ndp merge-train`.
- P2.C0.F9 `[V0]`: `ndp pr watch <PR>`.
- P2.C0.F10 `[V0]`: `ndp pr fix <PR>` / `ndp pr fix-epic <EPIC>`.
- P2.C0.F11 `[V0]`: `ndp pipeline run <COMMAND>`.
- P2.C0.F12 `[V0]`: Headless mode em todos os comandos.

#### P2.C1 — Agent Lifecycle Service

- P2.C1.F1 `[V0]`: State machine local.
- P2.C1.F2 `[V0]`: Pause/resume de orquestração.
- P2.C1.F3 `[V2+]`: Resume cross-machine.
- P2.C1.F4 `[V0]`: Fan-out/fan-in declarativo.

#### P2.C2 — Task Hierarchy & Phase Gates v2

- P2.C2.F1 `[V0]`: Task tree como entidade de primeira classe.
- P2.C2.F2 `[V0]`: Phase gates tipados em código.
- P2.C2.F3 `[V0]`: Pré-condições in-process antes de efeitos colaterais.
- P2.C2.F4 `[V1+]`: Gates customizáveis por org.
- P2.C2.F5 `[V0]`: Replay determinístico de execução.

#### P2.C3 — LLM Abstraction Layer

- P2.C3.F1 `[V0]`: Provider abstraction.
- P2.C3.F2 `[V0]`: Model routing dinâmico.
- P2.C3.F3 `[V0]`: Fallback automático.
- P2.C3.F4 `[V0]`: Custo por execução em tempo real.

#### P2.C4 — Reliability & Replay

- P2.C4.F1 `[V0]`: Determinismo controlado.
- P2.C4.F2 `[V0]`: Snapshot de contexto local.
- P2.C4.F3 `[V0]`: Idempotência por comando/skill.
- P2.C4.F4 `[V0]`: File locking local.

### 6.4. Product P3 — Developer Experience

CLI primária; TUI, IDE e web UI são camadas posteriores.

#### P3.C1 — CLI v2

- P3.C1.F1 `[V0]`: `ndp` CLI unificado.
- P3.C1.F2 `[V0]`: Saída estruturada.
- P3.C1.F3 `[V0]`: `ndp repl`.
- P3.C1.F4 `[V0]`: `ndp migrate`.
- P3.C1.F5 `[V0]`: `ndp init`.

#### P3.C2 — TUI & Local UI

- P3.C2.F1 `[V1+]`: `ndp tui`.
- P3.C2.F2 `[V1+]`: `ndp watch`.
- P3.C2.F3 `[V2+]`: `ndp ui` local.
- P3.C2.F4 `[V2+]`: Editor visual de rules/skills.

#### P3.C3 — IDE Extensions

- P3.C3.F1 `[V1+]`: Extensão VS Code.
- P3.C3.F2 `[V2+]`: Extensão JetBrains.
- P3.C3.F3 `[V1+]`: Painel inline de evidências.
- P3.C3.F4 `[V1+]`: Auto-complete de profile/capabilities.

#### P3.C4 — Onboarding & Time-to-Value

- P3.C4.F1 `[V0]`: `ndp init` com 5-7 perguntas.
- P3.C4.F2 `[V0]`: Templates por persona.
- P3.C4.F3 `[V1+]`: Tutorial guiado in-IDE.
- P3.C4.F4 `[V0]`: `ndp doctor`.

### 6.5. Product P4 — Knowledge & Marketplace

Ecossistema compartilhado, opt-in e network-required; core funciona offline com cache embarcado.

#### P4.C1 — Skill Marketplace

- P4.C1.F1 `[V1+]`: Registry central ou self-hosted.
- P4.C1.F2 `[V1+]`: SemVer obrigatório.
- P4.C1.F3 `[V1+]`: Dependency resolution.
- P4.C1.F4 `[V1+]`: Trust model com assinatura e sandbox.
- P4.C1.F5 `[V2+]`: Compatibility matrix por modelo/provider.

#### P4.C2 — Rule & Governance Library

- P4.C2.F1 `[V1+]`: Rule packs por domínio.
- P4.C2.F2 `[V2+]`: Rule simulator.
- P4.C2.F3 `[V1+]`: Rule conflict detector.
- P4.C2.F4 `[V1+]`: Custom rule authoring.

#### P4.C3 — Template & Profile Catalog

- P4.C3.F1 `[V1+]`: Catálogo de profiles oficial/community.
- P4.C3.F2 `[V2+]`: Rating e usage stats.
- P4.C3.F3 `[V1+]`: `ndp profile fork`.
- P4.C3.F4 `[V1+]`: Profile lineage.

#### P4.C4 — Cross-Project Intelligence

- P4.C4.F1 `[V2+]`: Padrões agregados anonimizados.
- P4.C4.F2 `[V2+]`: Recommendation engine.
- P4.C4.F3 `[V2+]`: Drift detection cross-repo.
- P4.C4.F4 `[V2+]`: Knowledge graph navegável.

### 6.6. Product P5 — Observability, Analytics & FinOps

V0 entrega observabilidade local; streaming remoto e dashboards são opt-in e posteriores.

#### P5.C1 — Local & Real-Time Telemetry

- P5.C1.F1 `[V0]`: Captura local NDJSON + queries CLI.
- P5.C1.F2 `[V1+]`: Streaming opt-in para OTLP/Datadog/custom HTTP.
- P5.C1.F3 `[V2+]`: Dashboard live de execução.
- P5.C1.F4 `[V1+]`: Alerting.
- P5.C1.F5 `[V1+]`: Trace OTel-compatible.

#### P5.C2 — Quality & Compliance Metrics

- P5.C2.F1 `[V0]`: Coverage longitudinal.
- P5.C2.F2 `[V0]`: Refinement quality score.
- P5.C2.F3 `[V1+]`: Doc freshness heatmap.
- P5.C2.F4 `[V1+]`: Compliance posture report.

#### P5.C3 — FinOps & Cost Insights

- P5.C3.F1 `[V0]`: Custo de LLM por skill/story/epic/org.
- P5.C3.F2 `[V1+]`: Sugestão de model downgrade.
- P5.C3.F3 `[V0]`: Budget guardrails locais.
- P5.C3.F4 `[V1+]`: Comparativo por provider.

#### P5.C4 — Research & Benchmarking

- P5.C4.F1 `[V2+]`: A/B testing de skills.
- P5.C4.F2 `[V1+]`: Benchmark suite.
- P5.C4.F3 `[V2+]`: Regression detection.
- P5.C4.F4 `[V2+]`: Public leaderboard opt-in.

### 6.7. Product P6 — Governance, Security & Trust

P6 reduz escopo porque gates básicos migram para P2. Fica com auditabilidade, compliance, threat modeling, supply chain e cloud trust.

#### P6.C1 — Audit & Compliance Engine

- P6.C1.F1 `[V0]`: Audit log local imutável.
- P6.C1.F2 `[V0]`: Evidence vault local.
- P6.C1.F3 `[V1+]`: Reports SOC2 / ISO 27001 / LGPD.
- P6.C1.F4 `[V1+]`: Forensics.
- P6.C1.F5 `[V0]`: CI Camada B com `ndp ci verify`.

#### P6.C2 — Refinement & Quality Gates v2

- P6.C2.F1 `[V0]`: AI-assisted refinement.
- P6.C2.F2 `[V1+]`: Refinement memory.
- P6.C2.F3 `[V0]`: Refinement templates por domínio.
- P6.C2.F4 `[V0]`: NO-GO library.

#### P6.C3 — Security Posture & Threat Modeling

- P6.C3.F1 `[V0]`: Continuous threat modeling.
- P6.C3.F2 `[V0]`: SBOM gerado e validado.
- P6.C3.F3 `[V0]`: Secret scanning integrado.
- P6.C3.F4 `[V1+]`: Supply chain trust score.

#### P6.C4 — Privacy, Multi-Tenancy & RBAC

- P6.C4.F1 `[V2+]`: Multi-tenant.
- P6.C4.F2 `[V2+]`: RBAC.
- P6.C4.F3 `[V2+]`: Data residency.
- P6.C4.F4 `[V1+]`: PII scrubbing para telemetria remota.

---

## 7. Alterações Estruturais Necessárias

| # | Mudança | De | Para | Risco / mitigação |
| --- | --- | --- | --- | --- |
| 0 | Inversão de controle | LLM orquestra; hooks tentam bloquear bypass. | NDP orquestra; LLM é worker. | Portar 1 orquestrador por vez e validar dual-mode. |
| 1 | Harness abstraction | Claude Code only. | Claude Code, Cursor, Windsurf, Aider, generic MCP. | Começar com Claude Code + Cursor. |
| 2 | LLM abstraction | Anthropic-only. | Claude/GPT/Gemini/local. | Prompt matrix por provider. |
| 3 | Governança como código | Rules markdown. | Policies executáveis. | Começar com YAML+JSONLogic para rules críticas. |
| 4 | Output do generator | Regen-only. | Overlay system. | 3-way merge declarativo + `ndp doctor`. |
| 5 | Telemetria | NDJSON via hooks. | Runtime telemetry + OTel-compatible. | Importer para histórico. |
| 6 | Distribuição de skills | Copy in-repo. | Marketplace/cache local versionado. | Assinatura, sandbox e core offline. |
| 7 | Multi-projeto | Repos isolados. | Opt-in cross-project intelligence. | Differential privacy e local-only default. |
| 8 | Multi-tenancy | N/A. | RBAC/cloud opcional V2+. | Manter V0/V1 single-user local. |
| 9 | Backward compat | Flow versions legados. | Migration assistant. | Testar contra profiles e epics canônicos. |
| 10 | OSS vs commercial | 100% OSS hoje. | Core OSS + cloud paid. | Linha clara desde o dia 1. |
| 11 | Hooks/scripts shell | `.claude/hooks`, `scripts/audit-*`. | Runtime gates + `ndp ci verify`. | Um teste por invariante migrado. |
| 12 | Rules engine | Prosa interpretada. | Policy engine + CI check. | Migrar só o que é realmente enforceable primeiro. |

---

## 8. Riscos Transversais

### 8.1. Técnicos

- **Performance da composition em escala.** Com plugins externos, pode crescer de centenas para milhares de artefatos. Cache local é V0.
- **Determinismo cross-LLM.** Separar composição determinística de conteúdo criativo gerado por LLM.
- **Estado distribuído.** Para V0/V1, file-based local com lock é suficiente; cross-machine fica V2+.
- **Trace OTel.** Migrar `events.ndjson` sem quebrar análises atuais.
- **Migração de hooks.** Perda de invariante é o maior risco. Dual-mode e testes por script mitigam.

### 8.2. Produto

- **Time-to-first-value.** O usuário precisa ver valor em 5 minutos; `ndp init` e `ndp doctor` são centrais.
- **Adoption friction.** Usuários com histórico de epics 0001-0071 precisam migrar sem perder evidência.
- **Marketplace cold-start.** Portar todos os ativos oficiais atuais como cache local embarcado.
- **Modelo de pricing.** Core local-first deve permanecer gratuito; cloud/marketplace/observability podem ser pagos.

### 8.3. Compliance e segurança

- **LGPD/GDPR para telemetria remota.** Scrubbing client-side e opt-in granular.
- **Supply chain do marketplace.** SBOM, signing, sandbox, trust score.
- **Auditabilidade legal.** Audit log local imutável começa na V0.
- **Cost-attack vector.** Budget guardrails por skill/provider.

### 8.4. Estratégia

- **Posicionamento.** Diferenciar por governance-first, evidence-first, local-first e multi-LLM.
- **OSS strategy.** Core Apache 2.0 é bom candidato; cloud/commercial separado.
- **Contribuição.** Extension points limpos: target adapter, capability, policy pack, template pack, plugin.

---

## 9. V0 Sugerida

O núcleo mínimo da V0 precisa provar o diferencial completo: NDP controla execução localmente, chama LLM como worker e produz evidências verificáveis.

Escopo mínimo sugerido:

| Área | Features mínimas |
| --- | --- |
| Orquestração | P2.C0.F1-F5, P2.C0.F7, P2.C0.F9, P2.C0.F12. |
| Runtime gates | P2.C2.F1-F3, P2.C4.F2-F4. |
| LLM/provider | P2.C3.F1-F4 com Claude como primeiro provider. |
| CLI/DX | P3.C1.F1-F5, P3.C4.F1, P3.C4.F4. |
| Composition/migration | P1.C1.F1-F3, P1.C2.F1-F2/F4, P1.C3.F1-F2/F5. |
| Telemetria/custos | P5.C1.F1, P5.C3.F1, P5.C3.F3. |
| Audit/evidence | P6.C1.F1-F2/F5. |
| Refinement/security | P6.C2.F1/F3/F4, P6.C3.F1-F3. |

Primeiro spike recomendado: reimplementar **um único orquestrador** como código, preferencialmente `ndp story refine` ou `ndp story implement` em escopo reduzido. Comparar contra a skill atual:

- tempo total;
- taxa de bypass;
- evidências produzidas;
- qualidade do output;
- quantidade de contexto que o LLM precisa receber;
- facilidade de debug/replay.

Se esse spike falhar, o plano inteiro precisa ser revisto antes de criar épicos.

---

## 10. Próximos Passos

1. Decidir o nome real do produto e registrar domínio/organização.
2. Definir licença do core e fronteira comercial.
3. Validar hipótese com 5-10 usuários atuais do `ia-dev-env`.
4. Rodar spike de inversão de controle com um orquestrador.
5. Rodar spike de target adapter Cursor.
6. Criar ADRs para as 12 mudanças estruturais antes de código de produção.
7. Aplicar refinement gate neste próprio plano, com personas PO, Tech Lead, Architect, Security, QA e SRE/DevOps.
8. Transformar V0 sugerida em épicos, depois stories e tasks.
9. Planejar transição dual-mode: hooks/scripts atuais e NDP rodando em paralelo por 1 release.

---

## 11. Notas de Processo

Este plano para em `Feature`. Quando aprovado, cada Feature deve passar por refinement antes de virar Epic. Cada Epic resultante deve produzir os artefatos de planejamento e evidência exigidos pelo próprio modelo que queremos vender.

O princípio local-first é vinculante para toda Feature `[V0]`: se uma Feature não puder rodar como CLI local, não é V0.

O princípio de inversão de controle também é vinculante para toda Feature `[V0]`: se uma Feature depende do LLM para orquestrar ordem, gates, commits ou validações, ela está no produto errado. O LLM pode gerar conteúdo; o NDP deve controlar o fluxo.
