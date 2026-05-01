# Plano Estratégico — Evolução do `ia-dev-environment` (NextGen Dev Platform — NDP)

> **Status:** Rascunho estratégico (pré-refinement). Hierarquia: `Project → Product → Capacity → Feature`. Épicos, Stories e Tasks ficam para a próxima rodada.
> **Autor:** Eder + Claude (sessão de planejamento de 2026-05-01).
> **Codinome de trabalho:** `NDP` — NextGen Dev Platform.

---

## 0. Princípio fundador — Local-first, CLI-first

Antes de qualquer Product, Capacity ou Feature deste plano, vale a regra de delivery do projeto:

> **O NDP é, em primeiro lugar, uma ferramenta que roda 100% local na máquina do desenvolvedor. A V0 é uma CLI — exatamente como o `ia-dev-env` é hoje. Toda interface gráfica (web console, dashboards, IDE extensions, mobile, etc.) é trabalho pós-V0 e não bloqueia nenhuma capacidade do core.**

### O que isso implica

| Decisão | Consequência |
| --- | --- |
| **Local-first por padrão** | Código-fonte, telemetria, audit log e estado de execução vivem no disco do desenvolvedor. Nada é enviado para nuvem sem opt-in explícito do usuário. |
| **V0 = CLI** | A primeira release entregável é executável via terminal (`ndp ...`), análoga ao `ia-dev-env generate` / `ia-dev-env validate` de hoje. Sem servidor, sem login, sem dependência de rede para o caminho feliz. |
| **Frontend é v1+** | Web console (P3.C1), IDE extensions (P3.C2), TUI (P3.C3.F2) são trabalho **posterior**. O core precisa estar estável e auditável via CLI antes de ganhar UI. |
| **Headless friendly** | Toda Feature do core precisa ser scriptável via CLI — UI futura consome a mesma camada que a CLI consome (sem lógica de negócio na UI). |
| **Network como opt-in** | Marketplace (P4), telemetria remota (P5.C1.F1), multi-tenancy (P6.C4) — tudo desligado por padrão. Usuário sem internet tem o produto inteiro funcionando offline. |
| **Sem vendor lock-in cloud** | Não dependemos de nenhum SaaS para rodar. Cloud é serviço opcional sobre o core, não pré-requisito. |

### Roadmap de interfaces (ordem de entrega)

```
V0  ─►  CLI (ndp <command>)                      ── single source of truth
V1  ─►  + TUI (ndp tui) e IDE panel mínimo        ── melhora UX, mesmo core
V2  ─►  + Web console local (ndp ui, localhost)   ── dashboard offline opcional
V3+ ─►  + SaaS multi-tenant, marketplace remoto   ── opt-in, separado do core
```

A CLI permanece **canônica em todas as versões**. Nenhuma feature do core pode existir apenas na UI; tudo precisa ter contrapartida CLI auditável e versionável.

---

## 0.5. Princípio de inversão de controle — NDP é o orquestrador

A V0 não vai apenas substituir o `ia-dev-env`: vai **inverter o modelo de execução**.

### Modelo atual (problema-raiz)

```
Usuário → Claude Code → lê x-epic-implement.md → LLM tenta executar passo-a-passo
                                                    ↑
                                                    Hooks bash + audit scripts
                                                    tentam impedir o LLM de pular
```

O LLM é o orquestrador. Ele decide quais skills chamar, em qual ordem, com quais argumentos. As 4 camadas de gates (Rule 24/26/27) existem **justamente porque o LLM pula etapas, simula resultados e silenciosamente declara sucesso sem ter executado**. Os hooks são curativos sobre uma fragilidade estrutural — não consertam a causa, só tentam detectar o sintoma depois.

### Modelo NDP (solução)

```
Usuário → ndp epic implement EPIC-0072
            ↓
         NDP (código determinístico) controla state machine, fases, gates, evidências
            ↓ chama via CLI quando precisa de criatividade do LLM
         claude -p "<prompt focado>" → resposta → NDP valida → próxima fase
```

**O NDP é o orquestrador.** O LLM (Claude na V0; outros providers em V1+) é um *worker* invocado para tarefas pontuais que só um modelo consegue fazer: gerar código, redigir review, escrever ADR, refinar story. Tudo o que é determinístico — fluxo entre fases, criação de branches, commits, validação de artefatos, telemetria, gates — vira **código compilado do NDP**, não markdown interpretado por LLM.

### Consequências práticas

| Efeito | Detalhe |
|---|---|
| **Skills orquestradoras viram comandos do CLI** | `x-epic-implement` → `ndp epic implement <ID>`. `x-story-implement` → `ndp story implement <ID>`. `x-task-implement` → `ndp task implement <ID>`. `x-review`, `x-review-pr`, `x-release`, `x-epic-orchestrate`, `x-pr-merge-train` — todas. Ver §5.1 para o mapeamento completo. |
| **Hooks e scripts shell são eliminados** | `.claude/hooks/*.sh` (Stop, PreToolUse, PostToolUse), `scripts/audit-*.sh`, `scripts/preflight.sh`, `scripts/enforce-*.sh` — substituídos. NDP aplica os mesmos invariantes em código nativo, antes/depois de cada fase, dentro do próprio processo. |
| **Camadas 0-4 da Rule 26 colapsam em 2** | Camada 0 (preventiva), 1 (normativa), 2 (CI bash), 3 (Java test), 4 (observability) → reduzem para: **Camada A** (NDP runtime — bloqueia em processo, antes de qualquer efeito colateral) e **Camada B** (CI — valida o que o NDP produziu). Carga cognitiva cai em ~80%. |
| **Zero-bypass vira propriedade arquitetural** | Rule 27 hoje exige 4 camadas porque o LLM pode bypassar. Com NDP como entry-point, **bypass é impossível por construção**: ou você roda `ndp ...` e ele aplica os gates, ou você não usa o produto. Acabaram os "LLM pulou a etapa X" silenciosos. |
| **Refinement gate vira pré-condição da CLI** | `ndp story implement <ID>` recusa começar se `ndp story refine <ID>` não tiver gerado verdict aprovado. Sem hook PreToolUse, sem audit script. Apenas `if (!storyRefined.approved) exit 33` em código. |
| **Tool-call grammar (Rule 28) vira tipagem** | Hoje é regex contra markdown. No NDP, `[required]/[optional]/[conditional]` é declarado em código (data class / enum) e enforçado em compile-time + runtime. Lint sobre markdown desaparece. |
| **Telemetria é in-process** | NDJSON gravado pelo NDP enquanto roda; não precisa de hooks bash emitindo eventos. Trace OTel-compatible nasce nativo, sem post-processing. |
| **Determinismo total da orquestração** | Mesma input → mesmo plano de execução, mesmo grafo de fases, mesmas validações. A variação fica isolada nos *outputs criativos do LLM*, nunca na orquestração. |
| **Phase gates viram função, não script** | `x-internal-phase-gate` em bash → método `phaseGate.assertPre(phase, ctx)` em código, com tipos, testes unitários e exceções tipadas. |
| **Rules como engine, não como prosa** | Rules continuam existindo, mas viram *políticas executáveis* (YAML+JSONLogic ou DSL) que o NDP avalia, em vez de markdown que o LLM tenta interpretar. |

### O que continua sendo "skill" no NDP

Skills *narrow / leaf* que pedem criatividade do LLM permanecem como **prompts versionados** consumidos pelo NDP:

- `x-arch-plan`, `x-test-plan`, `x-task-plan`, `x-story-refine`, `x-epic-refine` → continuam como templates de prompt + schema de saída esperado; o NDP invoca `claude -p` com o template, valida o JSON/Markdown de retorno contra schema, e só então persiste o artefato.
- `x-code-format`, `x-code-lint`, `x-test-run` → 100% determinísticas, viram código NDP que chama o tooling (mvn, gradle, pytest); LLM nem é envolvido.
- `x-test-tdd` (cycle Red-Green-Refactor) → orquestração em código (NDP decide quando rodar Red, Green, Refactor); LLM é chamado pontualmente para "redigir o teste falho" e "escrever o código mínimo para passar".
- `x-review`, `x-review-pr`, `x-doc-generate`, `x-adr-generate` → prompts focados, com NDP escolhendo *quando* invocar, *quem* invocar (qual persona/modelo) e validando o resultado antes de aceitar.

### Integração com Claude Code na V0

Na V0 o usuário tem dois modos de uso, equivalentes:

1. **Terminal puro** — `ndp epic implement EPIC-0072` direto no shell. Funciona sem Claude Code instalado.
2. **Dentro do Claude Code** — usuário digita um comando (ex.: `/ndp epic implement EPIC-0072`) que dispara o CLI. O Claude Code apenas repassa o comando; **toda a orquestração, todas as chamadas LLM e todas as validações são do NDP**.

Em ambos os casos o NDP usa o `claude` CLI (Anthropic CLI ou equivalente em V1+) para invocar o modelo nas etapas que pedem criatividade. **O Claude Code não orquestra nada**; vira apenas um terminal alternativo + cliente do modelo.

### O que isso muda no plano (re-priorização)

- **P2 (Orchestration Runtime) é o coração da V0.** Antes era "um runtime ao lado das skills"; agora **é o produto inteiro sob o capô**. Todas as skills orquestradoras existentes (Anexo B da Rule 25 — 8 skills) são re-implementadas como comandos NDP.
- **P6 (Governance) encolhe drasticamente.** Várias Features de audit (Camada 0/2/3/4) viram triviais ou desnecessárias. O esforço migra para *integration tests do orquestrador* — bem mais barato que manter 4 camadas de gates externas.
- **P1 (Core Engine) muda de foco.** Continua gerando artefatos para targets externos (Claude Code, Cursor, etc.), mas o "artefato principal" da V0 passa a ser o próprio binário/CLI do NDP — não mais um diretório `.claude/` para o LLM ler e tentar interpretar.

---

## 1. Análise do estado atual (sumário executivo)

O `ia-dev-environment` hoje é um **gerador CLI Java** que materializa um diretório `.claude/` opinativo a partir de um YAML de stack. Os ativos centrais são:

| Camada | O que existe hoje | Limite percebido |
| --- | --- | --- |
| **Núcleo de geração** | `CapabilityResolver` + `CapabilityAwareComposer` + `OutputPruner` (EPIC-0064 v5) | Composição é file-system + Pebble, sem runtime; mudança = regenerar + commit |
| **Governança** | 31+ Rules (`.claude/rules/`), 4 camadas de audit gates (Rule 26), zero-bypass (Rule 27), refinement gate (Rule 29) | Carga cognitiva alta; regras são markdown estático, não programáveis |
| **Orquestração** | ~85 skills (`x-epic-implement`, `x-story-implement`, `x-task-implement`, `x-review`, …), TDD double-loop, multi-agent dispatch via `Agent(...)` | Acoplado ao Claude Code (CLI Anthropic); single-LLM (Claude Opus/Sonnet/Haiku) |
| **Observabilidade** | Telemetria NDJSON local (`ai/epics/epic-*/telemetry/events.ndjson`), `x-telemetry-analyze`, `x-telemetry-trend` | Sem stream real-time, sem dashboard, sem cross-project analytics |
| **Lifecycle** | Local-First (EPIC-0061), Camada 0 preventiva (EPIC-0063), value-driven templates v2 (EPIC-0070), doc-as-DoD (EPIC-0071) | Tudo baseado em hooks bash + scripts shell; difícil estender em outros IDEs |
| **Distribuição** | Self-contained generator → diretório `.claude/` per-projeto | Nenhum marketplace, nenhum versionamento de skills, nenhum compartilhamento entre orgs |

### Limitações estruturais (vetores de evolução)

1. **Mono-LLM, mono-IDE.** Toda a arquitetura assume Claude Code + modelos Anthropic. A próxima geração precisa abstrair o "harness" (Claude Code, Cursor, Windsurf, Aider, Gemini CLI, OpenAI Codex CLI, Copilot Workspace) e o "engine" (Claude, GPT, Gemini, modelos locais).
2. **Governança como markdown.** Rules são prosa interpretada pelo LLM. Não há *engine* de regras programável, não há simulador de políticas, não há A/B test de governance.
3. **Telemetria offline.** NDJSON local → impossível observar uma orquestração em execução em tempo real, impossível correlacionar entre repositórios, impossível alertar.
4. **Sem ecossistema.** Skills/rules/agents são copiados em cada projeto. Atualizar uma skill em 200 repos = 200 PRs. Não há canal de distribuição nem versionamento semântico de governance.
5. **Output estático.** `.claude/` é regenerado; usuário não pode customizar sem perder no próximo regen. Não há *overlay* nem *patching* declarativo.
6. **Sem visão multi-projeto.** Cada repo é uma ilha. A inteligência acumulada (telemetria, P95 regressions, padrões de drift, NO-GO recorrentes do refinement) não atravessa fronteiras.
7. **Onboarding pesado.** Para um time adotar: instala o gerador, escreve YAML, regenera, lê 31 rules, aprende ~20 skills primárias. Curva alta.

### Visão para a próxima geração

> Uma **plataforma multi-LLM, multi-IDE, multi-projeto** de engenharia assistida por IA, **executada localmente por padrão** e com **inversão de controle: o NDP é o orquestrador determinístico, o LLM é o worker invocado pontualmente**. Governança é código compilado, não markdown interpretado. Orquestração é observável em tempo real (no próprio terminal na V0; com UI opcional depois). Conhecimento é compartilhável via marketplace opt-in. Hooks e audit scripts shell são eliminados — substituídos por gates in-process. A evolução das próprias regras é guiada por IA a partir de telemetria agregada.

---

## 2. Estrutura hierárquica do plano

### Project: **NextGen-Dev-Platform** (codinome `NDP`)

> Hierarquia conforme solicitado: Project → Product → Capacity → Feature.
> Épicos, Stories e Tasks ficam para a próxima rodada de refinement.
> **Marcação `[V0]` / `[V1+]` / `[V2+]` indica a release-target de cada Feature** — V0 = CLI local-first; tudo mais é posterior.

---

### Product P1: **Core Engine** — núcleo de composição e geração multi-target

> Evolução direta do `ia-dev-env` Java. Continua sendo a fonte da verdade da governance, mas vira **multi-target** (Claude Code, Cursor, Windsurf, Aider, agentes próprios) e **multi-LLM**. **Roda 100% local; é a CLI canônica do produto.**

#### Capacity P1.C1: **Configuração & Profile Management**
- Feature P1.C1.F1 `[V0]`: Schema unificado de profile (substitui o YAML atual) com validação JSON-Schema versionada
- Feature P1.C1.F2 `[V0]`: Migração assistida v5 (ia-dev-env atual) → v6 (NDP) com `ndp migrate --from-iadev`
- Feature P1.C1.F3 `[V0]`: Profile inheritance & overlays (base profile + org overlay + repo overlay)
- Feature P1.C1.F4 `[V1+]`: Detecção automática de stack (sem precisar escrever YAML manual) — usa heurísticas + LLM advisor

#### Capacity P1.C2: **Capability Composition v2**
- Feature P1.C2.F1 `[V0]`: Capability resolver com cache local (hoje recomputa a cada `generate`)
- Feature P1.C2.F2 `[V0]`: Frontmatter v4 com `provides:` (não só `requires:`) — capabilities expõem extension points
- Feature P1.C2.F3 `[V1+]`: Plug-in capabilities — pacotes externos (npm/maven/oci) que injetam capabilities sem fork
- Feature P1.C2.F4 `[V0]`: Composition diff & dry-run (`ndp generate --dry-run --diff`) — vê o que mudaria antes de aplicar

#### Capacity P1.C3: **Artifact Generation Multi-Target**
- Feature P1.C3.F1 `[V0]`: Target adapter `claude-code` (compatibilidade com `.claude/`)
- Feature P1.C3.F2 `[V0]`: Target adapter `cursor` (`.cursor/rules/`, `.cursorrules`)
- Feature P1.C3.F3 `[V1+]`: Target adapter `windsurf` / `aider` / `gemini-cli` / `codex-cli`
- Feature P1.C3.F4 `[V1+]`: Target adapter `generic-mcp` (qualquer cliente MCP-compatível)
- Feature P1.C3.F5 `[V0]`: Overlay system — usuário pode editar artefatos gerados sem perder customização no próximo regen

#### Capacity P1.C4: **Multi-Stack & Multi-Language**
- Feature P1.C4.F1 `[V0]`: Catálogo de stacks oficial (hoje: Java/Spring/Quarkus/Helidon/Micronaut/Picocli; expandir Python, TypeScript, Go, Rust, .NET, Kotlin)
- Feature P1.C4.F2 `[V1+]`: Stack templates community-contributed com versionamento semântico
- Feature P1.C4.F3 `[V2+]`: Polyglot projects (mono-repo com múltiplos stacks coexistindo)
- Feature P1.C4.F4 `[V0]`: Reutilização de KPs (knowledge packs) entre stacks via taxonomia comum

---

### Product P2: **Orchestration Runtime** — coração da V0; o NDP É o orquestrador

> **Este é o produto-âncora da V0.** Hoje a "execução" é o LLM interpretando markdown e os hooks bash tentando consertar o que ele pula. Aqui vira um **runtime in-process determinístico**, escrito em código, que invoca o LLM apenas para tarefas pontuais via `claude` CLI. Todas as skills orquestradoras de hoje (`x-epic-implement`, `x-story-implement`, `x-task-implement`, `x-review`, `x-review-pr`, `x-release`, `x-epic-orchestrate`, `x-pr-merge-train`) viram comandos do CLI `ndp`. Hooks e audit scripts shell são substituídos por gates in-process. **Roda como processo local; bypass é impossível por construção.**

#### Capacity P2.C0: **Comandos orquestradores nativos do CLI** *(nova capacity — substitui os hooks/scripts)*
- Feature P2.C0.F1 `[V0]`: `ndp epic implement <ID>` — substitui `x-epic-implement` (state machine de 6 fases em código)
- Feature P2.C0.F2 `[V0]`: `ndp story implement <ID>` — substitui `x-story-implement` (fase 1 planning paralelo, fase 2 implementação, fase 3 verify, fase 4 report — tudo determinístico)
- Feature P2.C0.F3 `[V0]`: `ndp task implement <ID>` — substitui `x-task-implement` (TDD double-loop em código; LLM só redige teste e código mínimo)
- Feature P2.C0.F4 `[V0]`: `ndp story refine <ID>` / `ndp epic refine <ID>` — substitui `x-story-refine` / `x-epic-refine` (multi-persona dispatch em código; cada persona é uma chamada `claude -p` com prompt versionado)
- Feature P2.C0.F5 `[V0]`: `ndp review <STORY>` / `ndp review pr <PR>` — substitui `x-review` / `x-review-pr` (especialistas em paralelo, consolidação determinística)
- Feature P2.C0.F6 `[V0]`: `ndp release [--patch|--minor|--major]` — substitui `x-release` (versionamento, changelog, release branch, gates em código)
- Feature P2.C0.F7 `[V0]`: `ndp epic orchestrate <ID>` — substitui `x-epic-orchestrate` (loop sequencial determinístico sobre stories)
- Feature P2.C0.F8 `[V0]`: `ndp merge-train [--epic <ID>|--prs <list>]` — substitui `x-pr-merge-train` (ordem topológica em código)
- Feature P2.C0.F9 `[V0]`: `ndp pr watch <PR>` — substitui `x-pr-watch-ci` (polling determinístico com 8 exit codes da Rule 45 como enum tipado)
- Feature P2.C0.F10 `[V0]`: `ndp pr fix <PR>` / `ndp pr fix-epic <EPIC>` — substitui `x-pr-fix` / `x-pr-fix-epic`
- Feature P2.C0.F11 `[V0]`: `ndp pipeline run <COMMAND>` — entry point único; aceita qualquer comando orquestrador, aplica os mesmos gates, telemetria e audit log
- Feature P2.C0.F12 `[V0]`: Headless mode (`--non-interactive`, `--output json`) garantido em todos os comandos para integração CI

#### Capacity P2.C1: **Agent Lifecycle Service**
- Feature P2.C1.F1 `[V0]`: State machine de skill (Pendente → Refinada → Planejada → Em Andamento → Concluída/Falha/Bloqueada) como serviço local
- Feature P2.C1.F2 `[V0]`: Pause/resume de orquestração long-running (hoje, perder a sessão = perder o progresso)
- Feature P2.C1.F3 `[V2+]`: Resume cross-machine (começa no laptop, termina no desktop, retoma em CI) — exige sync remoto opt-in
- Feature P2.C1.F4 `[V0]`: Sub-agent fan-out/fan-in declarativo (substitui o Batch-A/Batch-B manual de Pattern 2)

#### Capacity P2.C2: **Task Hierarchy & Phase Gates v2** *(em código nativo, não bash)*
- Feature P2.C2.F1 `[V0]`: Task tree como entidade de primeira classe do NDP (substitui o pareamento `TaskCreate`/`TaskUpdate` que dependia do harness do Claude Code)
- Feature P2.C2.F2 `[V0]`: **Phase gates como funções tipadas em código** — `phaseGate.assertPre(phase, ctx)` / `phaseGate.assertPost(phase, ctx, evidence)` substituindo `x-internal-phase-gate.sh` e os scripts `enforce-*.sh` / `audit-*.sh`. Exit codes viram exceptions tipadas; baselines viram fixtures de teste.
- Feature P2.C2.F3 `[V0]`: Pré-condições in-process antes de qualquer efeito colateral (substitui Camada 0 / `enforce-preflight-gates.sh`) — `ndp story implement` falha rapidamente se refinement não foi aprovado, working tree está sujo, ou capabilities estão inválidas. Sem hook PreToolUse, sem `CLAUDE_RECOVERY_MODE`.
- Feature P2.C2.F4 `[V1+]`: Gates customizáveis per-org (org adiciona "gate de SOC2", "gate de LGPD") via plug-in compilado, não via bash
- Feature P2.C2.F5 `[V0]`: Replay determinístico de execução (`ndp replay <run-id>`) — recria o grafo de fases a partir do audit log; LLM responses ficam em cache para reprodução offline

#### Capacity P2.C3: **LLM Abstraction Layer**
- Feature P2.C3.F1 `[V0]`: Provider abstraction — Claude/GPT/Gemini/local (Llama, Qwen) atrás da mesma interface
- Feature P2.C3.F2 `[V0]`: Model routing dinâmico (Rule 23 atual é estático; aqui é por skill + custo + latência observada)
- Feature P2.C3.F3 `[V0]`: Fallback automático (Opus indisponível → Sonnet com warning visível no terminal)
- Feature P2.C3.F4 `[V0]`: Custo por execução em tempo real, mostrado no terminal antes de aprovar PROCEED

#### Capacity P2.C4: **Reliability & Replay**
- Feature P2.C4.F1 `[V0]`: Determinismo controlado (mesma input + mesma seed = mesma saída quando possível)
- Feature P2.C4.F2 `[V0]`: Snapshot de contexto local (hoje, perder a janela = perder o contexto)
- Feature P2.C4.F3 `[V0]`: Idempotência por skill (chamar `x-pr-create` 2× para o mesmo task = no-op detectado)
- Feature P2.C4.F4 `[V0]`: File-locking local (evitar 2 agentes editando o mesmo arquivo em `--parallel`)

---

### Product P3: **Developer Experience** — CLI primária + TUI + IDE + (futuro) UI

> **V0 entrega CLI + saída estruturada (JSON/text).** TUI, IDE panels e Web Console são incrementais sobre a mesma camada de comandos. Nada do core depende de UI gráfica.

#### Capacity P3.C1: **CLI v2 (canônica em todas as versões)**
- Feature P3.C1.F1 `[V0]`: `ndp` CLI unificado (substitui `ia-dev-env` + scripts bash dispersos)
- Feature P3.C1.F2 `[V0]`: Saída estruturada por padrão (`--output json`/`--output text`/`--output ndjson`) para integração com qualquer ferramenta
- Feature P3.C1.F3 `[V0]`: REPL para experimentar capabilities sem regen completo (`ndp repl`)
- Feature P3.C1.F4 `[V0]`: Migration assistant interativo (`ndp migrate`)
- Feature P3.C1.F5 `[V0]`: Onboarding interativo (`ndp init`) com 5-7 perguntas vs. YAML longo de hoje

#### Capacity P3.C2: **TUI & Local UI Auxiliar**
- Feature P3.C2.F1 `[V1+]`: Modo TUI (`ndp tui`) — interface terminal navegável (Charm/Textual-style)
- Feature P3.C2.F2 `[V1+]`: Live progress (`ndp watch`) — acompanha orquestrações em curso no próprio terminal
- Feature P3.C2.F3 `[V2+]`: Web console local (`ndp ui`, sobe localhost) — dashboard offline opcional
- Feature P3.C2.F4 `[V2+]`: Editor visual de rules / skills (markdown + preview de capability resolution)

#### Capacity P3.C3: **IDE Extensions**
- Feature P3.C3.F1 `[V1+]`: Extensão VS Code — invoca skills via CLI, mostra task tree, anexa contexto
- Feature P3.C3.F2 `[V2+]`: Extensão JetBrains
- Feature P3.C3.F3 `[V1+]`: Painel inline de evidências (`verify-envelope.json`, review-story-*, …) sem sair do editor
- Feature P3.C3.F4 `[V1+]`: Auto-complete de profile YAML / capabilities

#### Capacity P3.C4: **Onboarding & Time-to-Value**
- Feature P3.C4.F1 `[V0]`: `ndp init` interativo com 5-7 perguntas (vs. YAML longo hoje)
- Feature P3.C4.F2 `[V0]`: Templates por persona (backend Java sênior, frontend TS, fullstack Python, SRE)
- Feature P3.C4.F3 `[V1+]`: Tutorial guiado in-IDE (modo "teach")
- Feature P3.C4.F4 `[V0]`: Healthcheck de adoção (`ndp doctor`) — diagnostica setup e sugere próximos passos

---

### Product P4: **Knowledge & Marketplace** — ecossistema compartilhado (opt-in, network-required)

> **Opt-in explícito. Funciona offline com cache local.** Nenhuma feature do core depende do marketplace estar disponível.

#### Capacity P4.C1: **Skill Marketplace**
- Feature P4.C1.F1 `[V1+]`: Registry central (`registry.ndp.dev` ou self-hosted) para skills versionadas
- Feature P4.C1.F2 `[V1+]`: Versionamento semântico de skills (SemVer obrigatório, breaking changes detectados)
- Feature P4.C1.F3 `[V1+]`: Dependency resolution (skill A depende de capability X; resolver baixa skill B que provê X)
- Feature P4.C1.F4 `[V1+]`: Trust model — assinatura digital de skills oficiais; sandbox para skills community
- Feature P4.C1.F5 `[V2+]`: Skill compatibility matrix (skill X funciona com Claude 4.7, GPT-5, Gemini 2.5)

#### Capacity P4.C2: **Rule & Governance Library**
- Feature P4.C2.F1 `[V1+]`: Rule packs por domínio (`@ndp/finance-compliance`, `@ndp/healthcare-hipaa`, `@ndp/security-pci`)
- Feature P4.C2.F2 `[V2+]`: Rule simulator — testa "essa regra teria pegado X PRs do passado?"
- Feature P4.C2.F3 `[V1+]`: Rule conflict detector (rule A exige X, rule B proíbe X)
- Feature P4.C2.F4 `[V1+]`: Custom rule authoring com lint + test framework

#### Capacity P4.C3: **Template & Profile Catalog**
- Feature P4.C3.F1 `[V1+]`: Catálogo de profiles oficial e community (`spring-boot-mvp`, `quarkus-microservice`, `python-fastapi`)
- Feature P4.C3.F2 `[V2+]`: Profile rating / usage stats
- Feature P4.C3.F3 `[V1+]`: Fork & customize (`ndp profile fork @ndp/spring-boot-mvp my-org`)
- Feature P4.C3.F4 `[V1+]`: Profile lineage (rastreia de qual profile o seu derivou; herda updates de upstream)

#### Capacity P4.C4: **Cross-Project Intelligence**
- Feature P4.C4.F1 `[V2+]`: Padrões agregados (anonimizados) — "85% dos projetos Spring Boot que usam capability X também usam Y"
- Feature P4.C4.F2 `[V2+]`: Recommendation engine — "seu projeto tem perfil similar ao Z; considere skill W"
- Feature P4.C4.F3 `[V2+]`: Drift detection cross-repo — "essa capability está obsoleta em 12 dos 15 projetos da sua org"
- Feature P4.C4.F4 `[V2+]`: Knowledge graph navegável (dependências entre rules, skills, capabilities, ADRs)

---

### Product P5: **Observability, Analytics & FinOps**

> **V0 entrega observabilidade local-first** (mesma capacidade do `events.ndjson` atual + queries via CLI). Streaming remoto e dashboards SaaS são posteriores.

#### Capacity P5.C1: **Local & Real-Time Telemetry**
- Feature P5.C1.F1 `[V0]`: Captura local NDJSON (paridade com `ia-dev-env`) + queries via CLI (`ndp telemetry analyze`)
- Feature P5.C1.F2 `[V1+]`: Streaming opt-in para sinks remotos (OTLP, Datadog, custom HTTP)
- Feature P5.C1.F3 `[V2+]`: Dashboard live de execução (todos os agentes em paralelo da org agora) — UI separada
- Feature P5.C1.F4 `[V1+]`: Alerting (P95 de skill X regrediu > 30%, gate Y falhou 3× seguidas) — local hooks na V1, remoto na V2
- Feature P5.C1.F5 `[V1+]`: Trace distribuído OTel-compatible

#### Capacity P5.C2: **Quality & Compliance Metrics**
- Feature P5.C2.F1 `[V0]`: Coverage tracking longitudinal por epic / story / file (relatório CLI)
- Feature P5.C2.F2 `[V0]`: Refinement quality score (% stories aprovadas no 1º refinement vs. retrabalho)
- Feature P5.C2.F3 `[V1+]`: Doc freshness heatmap (Rule 31 visualizado por org)
- Feature P5.C2.F4 `[V1+]`: Compliance posture report (OWASP, ASVS, SOC2, LGPD) por release

#### Capacity P5.C3: **FinOps & Cost Insights**
- Feature P5.C3.F1 `[V0]`: Custo de LLM por skill / story / epic / org (registro local)
- Feature P5.C3.F2 `[V1+]`: Sugestão de model downgrade (skill X é Opus mas Sonnet entregou idêntico em 50 amostras)
- Feature P5.C3.F3 `[V0]`: Budget guardrails locais (org define teto/mês via config; gate bloqueia ao atingir 90%)
- Feature P5.C3.F4 `[V1+]`: Comparativo de custo por provedor (Claude vs. GPT vs. Gemini para a mesma skill)

#### Capacity P5.C4: **Research & Benchmarking**
- Feature P5.C4.F1 `[V2+]`: A/B testing de skills (versão A vs. B em produção, mede qualidade real)
- Feature P5.C4.F2 `[V1+]`: Benchmark suite (mesmo problema, vários modelos, tabela comparativa)
- Feature P5.C4.F3 `[V2+]`: Regression detection automática (model atualizou, qualidade caiu, alerta)
- Feature P5.C4.F4 `[V2+]`: Public leaderboard opt-in (similar a HumanEval mas para tarefas reais)

---

### Product P6: **Governance, Security & Trust** *(reduzido pela inversão de controle)*

> A governança hoje (Rules 24/27/29/30/31) é forte mas estática, e exige **4 camadas de gates externos** (hooks, audit scripts, Java tests, CI workflow) porque o LLM é o orquestrador e pode bypassar. **Com a inversão de controle (§0.5), várias dessas camadas viram desnecessárias** — o NDP simplesmente não faz a coisa errada. P6 encolhe para o que realmente precisa de produto separado: audit log imutável, threat modeling, supply chain, e (em V2+) multi-tenancy. **V0 cobre o local-first; multi-tenant é V2+.**

#### Capacity P6.C1: **Audit & Compliance Engine** *(simplificado — gates viraram código do P2)*
- Feature P6.C1.F1 `[V0]`: Audit log local imutável escrito pelo próprio NDP em-processo (cadeia hash-linked, substitui telemetria NDJSON e o conjunto inteiro de `audit-*.sh`)
- Feature P6.C1.F2 `[V0]`: Evidence vault local (artefatos de Rule 24 com retenção/lifecycle policies) — populado nativamente pelo orquestrador, sem depender de hooks bash
- Feature P6.C1.F3 `[V1+]`: Reports SOC2 / ISO 27001 / LGPD prontos do CI
- Feature P6.C1.F4 `[V1+]`: Forensics — "quem aprovou o gate, com qual modelo, em qual contexto, contra qual policy"
- Feature P6.C1.F5 `[V0]`: **CI Camada B** (substitui Camadas 2/3/4 atuais) — único script CI: `ndp ci verify` valida o que o NDP produziu. Sem `audit-execution-integrity.sh`, `audit-bypass-flags.sh`, `audit-tool-call-grammar.sh`, etc.

#### Capacity P6.C2: **Refinement & Quality Gates v2**
- Feature P6.C2.F1 `[V0]`: AI-assisted refinement (`/x-story-refine` evolui para multi-round dialogue com persona-specific feedback inline)
- Feature P6.C2.F2 `[V1+]`: Refinement memory (lembra que essa persona já vetou X risco em stories similares)
- Feature P6.C2.F3 `[V0]`: Refinement templates por domínio (refinement de feature de pagamento vs. refinement de UI cosmético)
- Feature P6.C2.F4 `[V0]`: NO-GO library (catálogo de razões de NO-GO recorrentes para shortcut)

#### Capacity P6.C3: **Security Posture & Threat Modeling**
- Feature P6.C3.F1 `[V0]`: Continuous threat modeling (não só `x-threat-model` sob demanda; roda no CI a cada PR estrutural)
- Feature P6.C3.F2 `[V0]`: SBOM gerado e validado (Rule 06 evolui para CycloneDX + assinatura)
- Feature P6.C3.F3 `[V0]`: Secret scanning integrado (não só `audit-bypass-flags`, scan completo de credenciais)
- Feature P6.C3.F4 `[V1+]`: Supply chain trust score por skill/profile importado do marketplace

#### Capacity P6.C4: **Privacy, Multi-Tenancy & RBAC** *(toda esta Capacity é V2+ — exige cloud)*
- Feature P6.C4.F1 `[V2+]`: Multi-tenant — uma instância serve N orgs com isolamento criptográfico
- Feature P6.C4.F2 `[V2+]`: RBAC (quem pode editar rules, quem pode aprovar gates, quem pode publicar skills)
- Feature P6.C4.F3 `[V2+]`: Data residency (org escolhe região; código nunca atravessa fronteira sem consent)
- Feature P6.C4.F4 `[V1+]`: PII scrubbing automático na telemetria remota (Rule 20 vira política federada) — começa local na V1

---

## 3. Pontos-chave de alteração estrutural

> **Mudança #0 (a mais importante)** está separada porque é a fundação que torna todas as outras possíveis: a **inversão de controle**. Ela está descrita em §0.5; a tabela abaixo não a repete, apenas se apoia nela.

| # | Mudança | De | Para | Risco/Mitigação |
|---|---|---|---|---|
| 0 | **Inversão de controle (§0.5)** | LLM é o orquestrador; hooks/scripts shell tentam impedir bypass | NDP é o orquestrador in-process; LLM é worker invocado via `claude` CLI; bypass é impossível por construção | Risco: re-implementar 8 orquestradores em código é grande esforço. Mitigação: portar 1 por vez (ordem sugerida: `ndp story refine` → `ndp task implement` → `ndp story implement` → `ndp epic implement` → demais), validando em paralelo com a versão markdown atual. |
| 1 | **Harness abstraction** | Acoplado a Claude Code | Pluggable (Claude Code, Cursor, Windsurf, Aider, MCP genérico) | Risco: vazamento de abstração de cada IDE. Mitigação: começar com 2 targets reais (Claude Code + Cursor), generalizar depois. |
| 2 | **LLM abstraction** | Anthropic-only (Opus/Sonnet/Haiku) | Provider-neutral (+ GPT, Gemini, locais) | Risco: prompts otimizados para Claude degradam em outros. Mitigação: matriz de prompt-by-provider versionada por skill. |
| 3 | **Governança como código** | Rules em markdown interpretadas | Rules em DSL programável (eval, simulate, A/B test) | Risco: complexidade de uma linguagem proprietária. Mitigação: começar com YAML+JSONLogic; evoluir só se necessário. |
| 4 | **Output do generator** | `.claude/` regen-only | Overlay system (gera base + permite patches mantidos) | Risco: conflito de overlay vs. upstream update. Mitigação: 3-way merge declarativo + `ndp doctor` para detectar drift. |
| 5 | **Telemetria** | NDJSON local privado | Local-first com streaming opt-in (NUNCA forçado) | Risco: privacy concerns. Mitigação: local-first como padrão, Rule 20 evolui para política federada com PII scrubbing client-side antes de qualquer envio. |
| 6 | **Distribuição de skills** | Copy in-repo via `ScriptsAssembler` | Marketplace versionado (semver, signed, sandbox) — opcional | Risco: supply chain attacks. Mitigação: assinatura, trust score, sandboxing por padrão para skills community. Marketplace é opt-in; CLI funciona sem ele. |
| 7 | **Multi-projeto** | Cada repo é ilha | Knowledge graph cross-projeto (anonimizado, opt-in) | Risco: vazamento de IP via padrões agregados. Mitigação: differential privacy em estatísticas; opt-in granular; padrão é local-only. |
| 8 | **Identidade & multi-tenancy** | N/A (single-user local) | RBAC + multi-tenant + audit log | Risco: de simples para complexo num salto. Mitigação: V0/V1 são single-user local; multi-tenant entra só em V2+ quando houver demanda. |
| 9 | **Backward compat** | EPIC-0064 já é breaking (v5) | Migration tool obrigatório de ia-dev-env → NDP | Risco: usuários abandonam na migração. Mitigação: `ndp migrate --from-iadev` end-to-end testado contra os profiles canônicos. |
| 10 | **Open-source vs commercial** | 100% OSS hoje | Core OSS + cloud paid (marketplace, observability SaaS, multi-tenancy) | Risco: comunidade percebe como bait-and-switch. Mitigação: linha clara desde o dia 1 (Apache 2.0 core; commercial license para SaaS); o core local-first sempre será gratuito. |
| 11 | **Eliminação de hooks/scripts shell** | `.claude/hooks/*.sh`, `scripts/audit-*.sh`, `scripts/preflight.sh`, `scripts/enforce-*.sh` (~30+ scripts) | Removidos. Substituídos por gates in-process do NDP (Camada A) + um único `ndp ci verify` (Camada B) | Risco: perda de invariantes durante a migração. Mitigação: para cada script removido, criar teste unitário cobrindo o mesmo invariante no orquestrador NDP, e rodar dual-mode (script + NDP) por 1 release antes de remover o script. |
| 12 | **Rules viram engine, não prosa** | 31 rules em markdown interpretadas pelo LLM | Subset crítico vira políticas executáveis (YAML+JSONLogic ou DSL); resto continua como documentação humana | Risco: criar uma DSL nova é caro. Mitigação: começar com YAML+JSONLogic (existente, testado); migrar só rules que são realmente *enforced* (Rule 24/26/27/28/29 — não Rule 03/05). |

---

## 4. Coisas a antecipar (riscos transversais)

### 4.1 Técnicos

- **Performance da composition em escala.** Hoje compõe 182 artefatos em ~2-5s. Com plug-ins externos (P1.C2.F3) e marketplace, pode chegar a 1000+. Cache local distribuído (P1.C2.F1) é crítico, não nice-to-have.
- **Determinismo cross-LLM.** A garantia "compor 2× = mesmo SHA bytewise" (audit-capability-determinism.sh do EPIC-0064) é mais difícil quando o LLM faz parte da geração. Provavelmente: split entre *deterministic composition* (artefatos) e *LLM-rendered content* (placeholders {{...}}).
- **Estado distribuído.** Pause/resume cross-machine (P2.C1.F3) exige store de estado serializado. Decidir cedo na V2: PostgreSQL? S3? Sync via git? Para V0/V1, file-based local com lock é suficiente.
- **Trace OTel completo.** O atual `events.ndjson` não é OTel-compliant. Migração precisa ser planejada para não quebrar `x-telemetry-analyze` / `x-telemetry-trend`.

### 4.2 Produto

- **Time-to-first-value.** ia-dev-env hoje exige ler 31 rules para entender o sistema. NDP precisa de "5 minutos para ver valor". Onboarding por persona (P3.C4.F2) é determinante já na V0.
- **Adoption friction de migration.** Quem já está em ia-dev-env (com epics 0001-0071 acumulados) precisa migrar sem perder histórico. Migration assistant precisa ser caso de uso central da V0, não afterthought.
- **Marketplace cold-start.** Sem skills/profiles no dia 1, ninguém adota. Estratégia: portar TODOS os ~85 skills atuais como "skills oficiais @ndp/*" no lançamento — disponibilizados também via cache local embarcado para uso offline.
- **Pricing model.** Core OSS é claro. Mas: marketplace é freemium? Observability é por seat ou por evento? FinOps é incluso ou add-on? Decidir antes de qualquer linha de código de billing — não bloqueia V0.

### 4.3 Compliance & Segurança

- **LGPD/GDPR para telemetria remota.** Mesmo opt-in, Rule 20 precisa ser federada (cliente decide o que envia, servidor não pode pedir mais).
- **Supply chain.** Marketplace cria superfície de ataque tipo `npm`. SBOM, signing, sandbox e trust score (P6.C3.F4) são day-1 do marketplace, não day-N. Local-first mitiga: usuário sem marketplace está imune.
- **Auditabilidade legal.** Audit log imutável (P6.C1.F1) com retenção configurável é obrigatório para clientes enterprise (SOC2, HIPAA) — começa local na V0.
- **Cost-attack vector.** Um skill malicioso no marketplace pode queimar US$ via LLM API. Budget guardrails (P5.C3.F3) por skill, não só por org — locais desde V0.

### 4.4 Estratégia

- **Posicionamento vs. concorrentes.** GitHub Copilot Workspace, Cursor, Sweep, Devin, etc. estão no mesmo espaço. Diferencial do NDP: governance-first + multi-IDE + multi-LLM + auditável + extensível + **local-first por princípio**. Não competir em "AI faz tudo"; competir em "AI faz certo, comprovadamente, na sua máquina".
- **OSS strategy.** Core OSS com licença permissiva (Apache 2.0) atrai contribuição. Cloud features pagas. Linha *crystal clear* desde o início para evitar reclamação de bait-and-switch — o core local-first sempre será gratuito.
- **Comunidade e contribuição.** Hoje, contribuir para ia-dev-env é entender 70k+ linhas de Java. NDP precisa de extension points limpos: novo target adapter, nova capability, nova rule pack — todos sem fork do core.

---

## 5. Considerações de migração (para não perder o que existe)

| Ativo atual | Plano de migração |
| --- | --- |
| 31 Rules (`.claude/rules/01-31`) | Portadas como `@ndp/governance-base@1.0.0` rule pack oficial. Subset enforced (24/26/27/28/29) vira engine executável (§3 #12); resto fica como doc humana. |
| **Skills orquestradoras** | Separadas em três destinos: comandos públicos do CLI, orquestradores internos do aplicativo, e auxiliares com visibilidade a decidir — ver §5.1 |
| Skills leaf restantes | Portadas como `@ndp/skills-core@1.0.0` (Apache 2.0); viram *prompts versionados* invocados pelo NDP via `claude -p`; marketplace permite forks |
| 7 capability categories (EPIC-0064) | Migram 1:1 para v6 schema; campos novos (`provides:`) opcionais até v7 |
| Profiles (`my-java-cli`, etc.) | `ndp migrate --from-iadev <profile.yaml>` gera profile NDP equivalente |
| Telemetria histórica (`events.ndjson`) | Importable via `ndp telemetry import --format ndjson-iadev` |
| **~30+ hooks/scripts shell** (`hooks/*.sh`, `scripts/audit-*.sh`, `scripts/preflight.sh`, `scripts/enforce-*.sh`) | **Eliminados.** Cada invariante migra para teste unitário do orquestrador NDP correspondente. Dual-mode (script + NDP) por 1 release antes de remover. |
| ADRs 0001-0024 | Permanecem no repositório do projeto; NDP não migra ADRs (são do projeto, não do gerador) |
| Stories/Epics em flight (0001-0071) | EPIC-0049's `flowVersion` semântica preservada; NDP entende flowVersion 1/2/3/4 + adiciona "ndp-1" |

### 5.1. Mapeamento — Skills orquestradoras → NDP

O inventário atual tem três classes de skills com comportamento de orquestração. A decisão de migração não é apenas "skill vira prompt": quando a skill decide ordem, coordena fases, valida gates, manipula estado, abre PR, comita, dispara subskills/subagents, ou consolida múltiplos workers, ela precisa sair do markdown interpretado pelo LLM e virar código NDP.

#### 5.1.1. Orquestradoras principais → comandos públicos do CLI

Estas são as candidatas diretas a comandos de primeira classe do CLI. O NDP deve controlar a state machine, persistência, gates, telemetria, retries, idempotência, permissões e saída estruturada; o LLM entra apenas como worker criativo nas etapas que exigirem geração ou julgamento.

| Skill atual | Destino NDP V0 | Nota de migração |
| --- | --- | --- |
| `x-epic-implement` | `ndp epic implement <ID>` | State machine de 6 fases tipada; substitui hooks de phase gate, execution integrity e bypass audit. |
| `x-story-implement` | `ndp story implement <ID>` | Loop end-to-end de story: planning, task execution, PR, review, verify e report. |
| `x-task-implement` | `ndp task implement <ID>` | TDD double-loop em código; LLM só redige teste, implementação mínima e refactors pontuais. |
| `x-release` | `ndp release [--patch\|--minor\|--major]` | Versionamento, changelog, release branch, PR, aprovação, tag e back-merge sob controle determinístico. |
| `x-epic-orchestrate` | `ndp epic orchestrate <ID>` | Planejamento/orquestração multi-story com ordem de dependências, checkpoints e resume. |
| `x-pr-merge-train` | `ndp merge-train [--epic <ID>\|--prs <list>]` | Merge train com descoberta, validação, ordenação, waves, smoke verify e report. |
| `x-review` | `ndp review <STORY>` | Fan-out/fan-in de especialistas em paralelo; consolidação e scoring em código. |
| `x-review-pr` | `ndp review pr <PR>` | Review Tech Lead com checklist e veredito GO/NO-GO estruturado. |
| `x-story-refine` | `ndp story refine <ID>` | Refinement multi-persona; Q&A e verdict persistidos por schema validado. |
| `x-epic-refine` | `ndp epic refine <ID>` | Refinement estratégico multi-persona de epic; verdict de escopo `epic`. |
| `x-story-plan` | `ndp story plan <ID>` | Planejamento multi-agente, task breakdown, task plans e DoR validation. |
| `x-feature-create` | `ndp feature create <SPEC>` | Pipeline completo spec → epic → stories → implementation map → branch/PR. |
| `x-feature-ideate` | `ndp feature ideate` | Prosa livre → spec RA9 + PR docs; fluxo público de entrada no backlog. |
| `x-test-tdd` | `ndp test tdd <TASK>` | Ciclos Red/Green/Refactor, validações e commits ficam em código; LLM atua por ciclo. |

#### 5.1.2. Orquestradores internos → serviços internos do aplicativo

Estas skills não devem aparecer como comandos públicos por padrão. Elas viram serviços, componentes ou métodos internos chamados pelos comandos acima. O equivalente NDP deve ser testável em unidade, tipado, idempotente e com contratos de entrada/saída estáveis.

- `x-internal-story-build-plan`
- `x-internal-epic-build-plan`
- `x-internal-phase-gate`
- `x-internal-story-verify`
- `x-internal-epic-integrity-gate`
- `x-internal-epic-branch-ensure`
- `x-internal-status-update`
- `x-internal-report-write`
- `x-internal-pr-body-render`
- `x-internal-story-load-context`
- `x-internal-story-resume`
- `x-internal-story-report`
- `x-internal-args-normalize`
- `x-internal-worktree-precheck`
- `x-lib-group-verifier`

Destino sugerido:

| Grupo interno | Forma em NDP |
| --- | --- |
| Gates (`x-internal-phase-gate`, `x-internal-story-verify`, `x-internal-epic-integrity-gate`) | Serviços de policy/gate chamados antes/depois de cada fase. |
| Estado (`x-internal-status-update`, `x-internal-story-resume`, `x-internal-story-load-context`) | Repositório de estado + state machine local com locking e schema. |
| Planejamento (`x-internal-story-build-plan`, `x-internal-epic-build-plan`) | Builders de execution plan e dispatchers internos de workers LLM. |
| Renderização (`x-internal-report-write`, `x-internal-pr-body-render`, `x-internal-story-report`) | Renderers tipados com templates versionados e golden tests. |
| Git/precheck (`x-internal-epic-branch-ensure`, `x-internal-worktree-precheck`) | Serviços internos usados pelos comandos `ndp epic`, `ndp story` e `ndp git`. |
| Build groups (`x-lib-group-verifier`) | Verificador interno entre waves paralelas. |

#### 5.1.3. Orquestradoras auxiliares → avaliar visibilidade pública

Estas skills coordenam fluxo suficiente para não serem tratadas como leaf prompts, mas nem todas precisam continuar públicas. Cada uma deve passar por uma decisão explícita: comando público, subcomando avançado, serviço interno, ou prompt/tool privado chamado por outro comando.

| Skill auxiliar | Decisão a tomar |
| --- | --- |
| `x-code-audit` | Provável comando público (`ndp code audit`) ou parte de `ndp ci verify`; mantém fan-out de dimensões em workers. |
| `x-lib-audit-rules` | Provável serviço interno de governance/audit; avaliar se precisa de modo público para autores de rules. |
| `x-doc-generate` | Provável comando público (`ndp doc generate`) e fase interna de `ndp story implement`. |
| `x-template-migrate` | Provável comando público de migração (`ndp template migrate`), mas com engine interna reutilizável. |
| `x-pr-create` | Provável comando público (`ndp pr create`) e serviço interno de PR usado por story/task/release. |
| `x-pr-fix` | Provável comando público (`ndp pr fix <PR>`). |
| `x-pr-fix-epic` | Provável comando público (`ndp pr fix-epic <EPIC>`) ou modo de `ndp pr fix --epic`. |
| `x-pr-watch-ci` | Provável comando público (`ndp pr watch <PR>`) com exit codes como enum tipado. |
| `x-pr-merge` | Avaliar: comando público avançado (`ndp pr merge`) ou serviço interno usado por merge-train/release. |
| `x-git-push` | Avaliar: pode virar subcomandos `ndp git push/commit/pr`; parte pode ser interna. |
| `x-git-commit` | Provável comando público (`ndp git commit`) e serviço interno de commit transacional. |
| `x-git-worktree` | Provável comando público (`ndp git worktree`) e serviço interno de lifecycle. |
| `x-git-cleanup-branches` | Provável comando público avançado (`ndp git cleanup-branches`) com confirmação e dry-run. |
| `x-status-reconcile` | Avaliar: comando de recovery/admin (`ndp status reconcile`) ou ferramenta interna de migração. |
| `x-ci-generate` | Provável comando público (`ndp ci generate`). |
| `x-setup-env` | Provável comando público (`ndp doctor` / `ndp setup env`). |
| `x-perf-profile` | Provável comando público (`ndp perf profile`) com adapters por stack. |
| `x-ops-troubleshoot` | Avaliar: comando público assistivo (`ndp troubleshoot`) ou prompt versionado acionado por falhas. |
| `x-ops-incident` | Avaliar: comando público opcional; pode ficar fora do core V0 se não for essencial ao fluxo local-first. |
| `x-jira-create-stories` | Avaliar: integração opcional/plugin (`ndp jira create stories`), não core offline obrigatório. |
| `x-adr-generate` | Provável comando público (`ndp adr generate`) e fase interna de arquitetura/migração. |
| `x-owasp-scan` | Provável comando público (`ndp security owasp-scan`) e entrada do `ndp ci verify`. |
| `x-security-dashboard` | Avaliar: comando público de agregação local (`ndp security dashboard`) ou recurso V1+ com UI. |
| `x-security-pentest` | Avaliar: comando público condicional por capability, com restrições fortes de ambiente. |

**Princípio invariante:** se a skill atual era principalmente *fluxo* (decidir ordem, validar, comitar, abrir PR, consolidar workers, controlar retry/resume), vira comando ou serviço NDP. Se era principalmente *criatividade* (gerar plano, redigir review, escrever ADR), continua como prompt versionado consumido pelo comando NDP correspondente. Se hoje é pública apenas porque o LLM precisava chamá-la manualmente, sua visibilidade deve ser reavaliada: no NDP, o usuário vê comandos de produto; o restante é API interna.

### 5.2. Inventário — Skills puras / não-orquestradoras

As skills abaixo já têm o nível de isolamento necessário para serem consideradas **skills puras**: cada uma possui uma responsabilidade dominante, entrada/saída relativamente delimitada e não deveria decidir o fluxo maior de implementação, release, PR, refinement ou governança. No NDP, elas podem virar prompts versionados, comandos utilitários, adapters de tooling ou serviços internos, mas **não precisam carregar state machine própria nem coordenar lifecycle amplo**.

Critério usado: uma skill pura executa um trabalho focado — formatar, validar, auditar, gerar um artefato, revisar uma dimensão, rodar uma suíte, fazer scaffold, sincronizar com uma integração ou produzir uma recomendação. Quando houver retries, chamadas auxiliares ou validações internas, isso permanece aceitável desde que a skill continue responsável por **um único resultado de produto**.

#### 5.2.1. Conditional skills

| Grupo | Skills puras | Responsabilidade isolada |
| --- | --- | --- |
| `conditional/dev` | `x-setup-stack` | Setup pontual de stack local. |
| `conditional/ops` | `x-obs-instrument` | Instrumentação de observabilidade. |
| `conditional/review` | `x-review-api`, `x-review-compliance`, `x-review-data-modeling`, `x-review-db`, `x-review-devops`, `x-review-events`, `x-review-gateway`, `x-review-graphql`, `x-review-grpc`, `x-review-obs`, `x-review-security` | Reviews especialistas por uma dimensão técnica. |
| `conditional/security` | `x-security-container`, `x-security-dast`, `x-security-infra`, `x-security-sast`, `x-security-secrets`, `x-security-sonar` | Scans e avaliações de segurança por superfície. |
| `conditional/test` | `x-test-contract-lint`, `x-test-contract`, `x-test-e2e`, `x-test-perf`, `x-test-smoke-api`, `x-test-smoke-socket` | Execução ou validação de uma categoria de teste. |

#### 5.2.2. Core code/dev/git skills

| Grupo | Skills puras | Responsabilidade isolada |
| --- | --- | --- |
| `core/code` | `x-code-format`, `x-code-lint` | Formatação e lint como operações determinísticas. |
| `core/dev` | `helidon-scaffold`, `micronaut-scaffold`, `picocli-command`, `quarkus-resource`, `spring-controller` | Scaffold ou geração pontual de componente. |
| `core/dev` | `x-ci-generate`, `x-mcp-recommend`, `x-setup-env`, `x-spec-drift` | Geração de CI, recomendação, diagnóstico de ambiente ou drift report. |
| `core/git` | `x-git-branch`, `x-git-cleanup-branches`, `x-git-commit`, `x-git-merge`, `x-git-push`, `x-git-worktree`, `x-planning-commit` | Primitivas Git isoladas, reutilizáveis pelos orquestradores. |

#### 5.2.3. Internal skills com responsabilidade única

Estas skills continuam sendo candidatas naturais a serviços internos do NDP, mas não por serem orquestradoras de produto; elas são primitivas bem delimitadas usadas por fluxos maiores.

| Grupo | Skills puras | Responsabilidade isolada |
| --- | --- | --- |
| `core/internal/git` | `x-internal-epic-branch-ensure`, `x-internal-worktree-precheck` | Garantia de branch e pré-check de worktree. |
| `core/internal/ops` | `x-internal-args-normalize`, `x-internal-report-write`, `x-internal-status-update` | Normalização de argumentos, escrita de relatório e atualização de estado. |
| `core/internal/plan` | `x-frontmatter-migrate`, `x-internal-epic-build-plan`, `x-internal-epic-create`, `x-internal-epic-integrity-gate`, `x-internal-epic-map`, `x-internal-phase-gate`, `x-internal-story-create`, `x-internal-story-load-context`, `x-internal-story-report`, `x-internal-story-resume`, `x-internal-story-verify` | Migração, geração, carga de contexto, verificação, gates e reports com um objetivo explícito por skill. |
| `core/internal/pr` | `x-internal-pr-body-render` | Renderização do corpo de PR. |

#### 5.2.4. Integrações, bibliotecas e operações

| Grupo | Skills puras | Responsabilidade isolada |
| --- | --- | --- |
| `core/jira` | `x-jira-create-epic`, `x-jira-create-stories` | Sincronização local → Jira. |
| `core/lib` | `x-lib-group-verifier`, `x-lib-task-decomposer` | Verificação de grupo ou decomposição estrutural. |
| `core/ops` | `x-doc-generate`, `x-doc-validate`, `x-ops-incident`, `x-ops-troubleshoot`, `x-perf-profile`, `x-release-changelog`, `x-status-reconcile`, `x-telemetry-analyze`, `x-telemetry-trend` | Documentação, troubleshooting, profiling, changelog, reconciliação e análise operacional. |

#### 5.2.5. Planning, PR, review, security e test

| Grupo | Skills puras | Responsabilidade isolada |
| --- | --- | --- |
| `core/plan` | `planning-standards-kp`, `x-adr-generate`, `x-arch-plan`, `x-arch-system-update`, `x-arch-update`, `x-parallel-eval`, `x-task-plan`, `x-template-migrate`, `x-threat-model` | Knowledge pack, planos, ADRs, atualização documental, avaliação de paralelismo, migração de template e threat model. |
| `core/pr` | `x-pr-create`, `x-pr-fix`, `x-pr-merge`, `x-pr-watch-ci` | Operações pontuais de PR, correção localizada, merge e watch de CI. |
| `core/review` | `x-review-perf`, `x-review-pr`, `x-review-qa` | Reviews focados em performance, checklist Tech Lead de PR ou QA. |
| `core/security` | `x-dependency-audit`, `x-hardening-eval`, `x-owasp-scan`, `x-runtime-eval`, `x-security-dashboard`, `x-security-pipeline`, `x-supply-chain-audit` | Auditorias e avaliações de segurança com saída própria. |
| `core/test` | `x-test-plan`, `x-test-run` | Plano de testes ou execução de testes/cobertura. |

**Decisão de migração:** estas 98 skills devem ser portadas sem inflar seu escopo. O NDP pode chamá-las como workers, comandos auxiliares ou serviços internos, mas a responsabilidade de coordenar ordem, retries globais, gates cross-phase, commits, PRs e evidências pertence ao runtime de orquestração (§P2), não a essas skills.

---

## 6. Próximos passos sugeridos (sem entrar em épicos ainda)

1. **Decidir o nome real do projeto** (NDP é codinome) e registrar domínio + GitHub org.
2. **Definir a licença** (sugestão: Apache 2.0 para core, BSL/SSPL para componentes cloud).
3. **Validar a hipótese** com 5-10 usuários atuais do ia-dev-env: quais 3 produtos (P1-P6) eles pagariam? Quais resolveriam dor real? (Lembrar: V0 é gratuito local-first.)
4. **Spike técnico de inversão de controle (§0.5)** — 2-3 semanas. Reimplementar **um único orquestrador** (sugestão: `ndp story refine`) em código, usando `claude` CLI para invocar as 5-7 personas. Comparar tempo, taxa de bypass e qualidade de saída contra a versão markdown atual. **Se este spike falhar, o plano inteiro precisa ser revisto.**
5. **Spike técnico de target adapter** (P1.C3.F2 — Cursor) — 2 semanas. Se isso for viável em paralelo com o §6.4, o resto do plano é viável.
6. **Documentar como ADR** as 12 mudanças estruturais da seção §3 *antes* de começar qualquer código (evitar arrependimento estrutural depois). A ADR sobre inversão de controle (#0) é a mais crítica.
7. **Refinement gate aplicado a este próprio plano** — passar pelas 6 personas (PO, Tech Lead, Architect, Security, QA, SRE/DevOps) com `/x-epic-refine` adaptado, antes de promover qualquer Capacity para Epic.
8. **Definir o scope da V0** — escolher subconjunto mínimo de Features `[V0]` que entrega valor end-to-end via CLI. Sugestão de núcleo mínimo: P2.C0 (todos os 8 orquestradores), P2.C2 (phase gates como código), P2.C3.F1+F2+F4 (LLM abstraction básica + custo), P5.C1.F1 (telemetria local), P6.C1.F1+F2+F5 (audit log + evidence vault + `ndp ci verify`). Sem isso, não é V0.
9. **Plano de transição dual-mode** — durante 1 release, hooks/scripts atuais e NDP rodam em paralelo, validando paridade. Só então os scripts shell são removidos.

---

> **Nota de processo.** Este plano usa a hierarquia `Project → Product → Capacity → Feature` conforme solicitado, parando antes de Epic. Quando aprovado, cada Feature deve ser refinada via `/x-epic-refine` (Rule 29) antes de virar Epic, e cada Epic resultante deve passar pelos 7 artefatos de planejamento (Rule 27 §Surface 09) antes de qualquer story implementar código. Pular esse fluxo desfaz o próprio diferencial competitivo do NDP.
>
> **Nota de delivery.** O princípio fundador da seção §0 é vinculante para todas as Features marcadas `[V0]`: se uma Feature não puder ser entregue como CLI local-first, ela não está no escopo da V0 e precisa ser reclassificada para `[V1+]` ou `[V2+]`.
>
> **Nota de inversão de controle.** O princípio da seção §0.5 é igualmente vinculante: nenhuma Feature `[V0]` pode depender do LLM para *orquestrar* (decidir ordem, validar, comitar). LLM só entra para *gerar conteúdo criativo* (código, review, plano, refinement). Se uma Feature `[V0]` requer LLM-driven orchestration, ela está no produto errado — provavelmente é um leaf prompt do P1, não uma capability do P2.
