# MVP — Notas de Arquitetura para Produto SDLC com Claude

> Compilado de sessão de discussão (2026-04-29). Referência para sessão futura de detalhamento. Produto não viverá neste repositório.

## Contexto / Problema

O projeto atual (`ia-dev-environment`) tem ~80 skills (`.claude/skills/`) que constituem praticamente um produto para desenvolver qualquer coisa via LLM. Faltam:

- Mais controle iterativo
- Interface visual
- Checkpoints (de onde parou → para onde vai)
- Status persistido das execuções
- Orquestração profunda

**Caso de uso exemplo:** Construir produto de Aluguel de Carros end-to-end — Ideação → Stories → Arquitetura → Microsserviços → Repositório → Build → Deploy.

**Diagnóstico-chave:** O problema recorrente de "LLM pula etapas" (que motivou Rules 24/25/27 + hooks Stop/PreToolUse + audits CI) é tratado como sintoma. A causa-raiz é arquitetural: **o orquestrador é a própria LLM**. Skills como `x-epic-implement` dependem da LLM lembrar de invocar cada sub-skill. A solução correta é **orquestrador programático**, com LLM apenas nos passos que exigem raciocínio.

---

## Claude Code — modos de uso

### CLI não-interativo (modo print)

```bash
claude -p "seu prompt aqui"
```

Executa one-shot, imprime em stdout, encerra — sem entrar na TUI.

**Flags principais:**

| Flag | Efeito |
|------|--------|
| `-p, --print` | Modo não-interativo |
| `--output-format text\|json\|stream-json` | Formato de saída |
| `--input-format text\|stream-json` | Aceita input estruturado |
| `--allowedTools "Bash,Edit"` | Autoriza ferramentas sem prompt |
| `--permission-mode acceptEdits\|bypassPermissions\|plan` | Política de permissões |
| `--continue` / `--resume <session-id>` | Retoma sessão |
| `--model claude-opus-4-7` | Escolhe modelo |

**Exemplos:**

```bash
# stdin via pipe
cat erro.log | claude -p "diagnostique este stacktrace"

# JSON pra parseamento
claude -p "liste arquivos modificados" --output-format json | jq '.result'

# Automação CI
claude -p "rode mvn test e resuma falhas" \
  --allowedTools "Bash" \
  --permission-mode acceptEdits

# Retomar sessão
claude --resume <session-id> -p "continue de onde parou"
```

---

## Claude Agent SDK

### O que é

Invólucro acima da API Messages da Anthropic que encapsula o **agent loop completo**: tool use, multi-turn, file operations, sub-agentes, hooks, MCP. É essencialmente o "motor" do Claude Code exposto como biblioteca.

### Diferença x API direta

- **API Messages (raw):** você escreve o loop manualmente — envia → recebe → detecta tool calls → executa → reenvia.
- **Agent SDK:** loop automático. Você dá uma tarefa, ele executa ferramentas até concluir.

### Linguagens oficialmente suportadas

**Apenas Python e TypeScript/Node.js.**

- Python: `pip install claude-agent-sdk`
- TypeScript: `npm install @anthropic-ai/claude-agent-sdk`

### Para outras linguagens (Java, Go, Rust, C#)

| Caminho | Quando usar |
|---|---|
| **API REST direta** | Você quer agent loop em Java/Go/Rust — chama endpoints HTTP diretamente |
| **`claude` CLI via subprocess** (`-p --output-format stream-json`) | Integração rápida; CLI já implementa agent loop |
| **SDKs comunitários** | Existem para API base em Go, Rust, Java |
| **MCP server** | Você quer *dar* ferramentas ao Claude, não *ser* o agente |

---

## SDK vs CLI — diferenças reais

### O que SÓ o SDK oferece

| Recurso | Por quê importa |
|---|---|
| Controle fino do loop | Interceptar cada turno, validar/transformar mensagens |
| Tool use nativo customizado | Handler em Python/TS chamando seu DB, queue, cache direto |
| Streaming tipado | Eventos estruturados (`content_block_start`, etc.) com types |
| Sem subprocess | Roda no mesmo processo da app — sem overhead de spawn |
| System prompts dinâmicos | Mudar prompt por requisição (multi-tenant, A/B test) |
| N conversas paralelas | Múltiplas sessions no mesmo processo |
| Observability nativa | Seus logs/traces capturam tudo |

### O que SÓ o CLI oferece (out-of-the-box)

| Recurso | Detalhe |
|---|---|
| Agent loop completo do Claude Code | File ops, Bash, Edit, Read, WebFetch já configurados |
| Skills/slash commands | Tudo de `.claude/skills/` carregado automaticamente |
| Hooks | PreToolUse / PostToolUse / Stop / SessionStart |
| MCP servers | Configuração via `~/.claude.json` |
| Sub-agentes (Task tool) | Spawning paralelo já pronto |
| CLAUDE.md + rules | Carregamento automático no system prompt |
| Sessões persistentes | `--resume`, `--continue`, contexto compactado |
| Auto-memory | Persistência entre conversas |
| Permissões interativas | Allow/deny granular |

### Diferença filosófica

- **CLI** = "Claude Code pronto, scriptável em modo `-p`."
- **SDK** = "Aqui estão Messages API + Tool Use + Sessions. Construa SEU agente."

> CLI é "saia do elevador rápido". SDK é "construa o elevador do seu jeito".

---

## Skills no SDK

Não há suporte nativo. Mas SKILL.md é só Markdown com frontmatter — você carrega como **prompt template**:

```python
skill_md = read("plans/skills/x-arch-plan/SKILL.md")
response = client.messages.create(
    system=skill_md_body,           # corpo da skill como system prompt
    messages=[{"role":"user","content": user_args}],
    tools=[...]                     # você define as tools
)
```

O que o SDK **não faz por você** (vira código seu): dispatch de `Skill(skill: "x-foo")` aninhado, hooks, sub-agentes via Task tool, MCP loading, CLAUDE.md auto-load.

---

## Arquitetura recomendada para o produto

```
┌─────────────────────────────────────────────────────────┐
│  Web UI (React/Next)                                    │
│  - Dashboard de fases (Ideação → Stories → Arch → ...) │
│  - Checkpoints visuais, retry/edit/resume por fase     │
│  - Streaming de eventos LLM                             │
└──────────────────┬──────────────────────────────────────┘
                   │ WebSocket / SSE
┌──────────────────▼──────────────────────────────────────┐
│  Orquestrador programático (Java/TS/Python)            │
│  - State machine de fases (XState, Temporal, etc)      │
│  - Persistência: Postgres (state, checkpoints, runs)   │
│  - Phase gates DETERMINÍSTICOS (código, não LLM)       │
│  - Retry, resume, branching de execução                │
└──────────────────┬──────────────────────────────────────┘
                   │
        ┌──────────┼──────────┬─────────────┐
        ▼          ▼          ▼             ▼
   Claude SDK   GitHub API   Docker     Deploy (k8s/...)
  (raciocínio) (PRs/repo)   (build)     (entrega)
```

### Como as skills atuais se encaixam

| Tipo de skill atual | Vira o quê no produto |
|---|---|
| Orquestradoras (`x-epic-implement`, `x-story-implement`, `x-task-implement`) | **Código Java/TS** — state machine, não prompt |
| Leaf de raciocínio (`x-arch-plan`, `x-test-plan`, `x-review`, `x-review-pr`) | **Prompt templates** chamados via SDK |
| Internas (`x-internal-*`) | **Funções** puras |
| Git/utility (`x-git-commit`, `x-pr-create`, `x-pr-watch-ci`) | **Código** chamando GitHub API direto |
| Audits (`audit-*.sh`) | **Validators** rodando entre fases como gates |

### Rules como invariantes

Rules 24/25/27 (zero-bypass, task hierarchy, execution integrity) viram **invariantes do state machine**: "não pode transicionar de Phase 2 → Phase 3 sem artefato X em disco". Enforcement por construção, não por audit retroativo.

---

## Auth e billing — distinção crítica

| | Auth aceito | Billing |
|---|---|---|
| **Claude Code CLI** | Subscription Pro/Max **OU** API key | Subscription cobre OU pay-per-token |
| **Claude Agent SDK** | **Apenas API key** | **Sempre** pay-per-token |
| **Anthropic SDK** (`anthropic`) | Apenas API key | Pay-per-token |

### Citação oficial da Anthropic sobre o SDK

> "Anthropic does not allow third party developers to offer claude.ai login or rate limits for their products, including agents built on the Claude Agent SDK. Please use the API key authentication methods described in this document instead."

**Implicação:** Não dá pra rodar SaaS multi-usuário na sua subscription pessoal. Para produto comercial:

1. Conta na Anthropic Console (console.anthropic.com)
2. API key
3. SDK consome essa key
4. Cobrança por token (input/output)
5. Para produção: **prompt caching** agressivo (skills são prompts grandes/estáveis — cache pode reduzir custo input ~90%)
6. Considerar **BYOK** (cliente traz própria key) se for SaaS

Atalhos enterprise (não ajudam no MVP): AWS Bedrock, GCP Vertex, Azure Foundry.

---

## Recomendação final — caminho em duas etapas

### Etapa 1 — MVP com CLI + subscription pessoal (Semana 1–2)

**Objetivo:** validar a arquitetura "orquestrador programático + LLM em pontos específicos" sem queimar tokens da API.

**Como:**
- Pegar 1 epic existente (ex: epic-0064)
- Reimplementar `x-epic-implement` como código Java/TS puro
- Skills leaf (`x-arch-plan`, `x-review`) viram chamadas via subprocess `claude -p` com SKILL.md como prompt
- Estado em SQLite/Postgres
- UI minimalista: lista de fases, status, botão resume

**Custo:** zero (subscription já paga).

**Limites do CLI subprocess:**
- Não escala pra SaaS multi-usuário (termos da subscription)
- Performance pior (spawn por chamada)
- Streaming pra UI exige parsear NDJSON do stdout
- Sem controle fino do agent loop

### Etapa 2 — Migrar pra SDK + API key (Semana 3+)

Se MVP validar a arquitetura, troca apenas a camada de transporte (subprocess → SDK). State machine, UI, persistência: tudo aproveitado.

**Aí ativa:**
- Prompt caching das skills
- Paralelização real (múltiplas sessions no mesmo processo)
- Modelo certo por etapa (Opus pra arch, Sonnet pra código, Haiku pra utilities — Rule 23 já mapeia)
- Estimativa: $5–20 por epic completo (sem caching); ~$1–4 com caching agressivo

### Sequência dá

- Validação técnica barata sem comprometer arquitetura
- Caminho de migração curto (só transporte muda)
- Decisão go/no-go antes de pagar API

### O que NÃO fazer

- Construir MVP direto no SDK "pra economizar refactor" — gasta tokens validando ideia, joga fora se arquitetura estiver errada
- Construir MVP com CLI achando que vira SaaS — bate no muro dos termos de uso no segundo cliente
- Usar subscription em produção "só pra começar" — Anthropic detecta e suspende

---

## Próximos passos (próxima sessão)

Tópicos para detalhar:

1. **State machine concreto** — mapeamento `x-epic-implement` → estados/transições/guards (XState, Temporal, ou implementação caseira)
2. **Modelo de persistência** — schema Postgres para runs, fases, checkpoints, artefatos
3. **Catálogo de prompt templates** — como versionar, parametrizar, testar SKILL.md como prompt
4. **Estratégia de prompt caching** — quais blocos cachear, TTL, invalidação
5. **UI/UX de checkpoints** — como visualizar fase em curso, permitir intervenção humana, retry granular
6. **Multi-tenancy + BYOK** — modelo de billing, isolamento de execuções
7. **Tools customizadas via SDK** — handler nativo chamando GitHub API, Docker, k8s deploy
8. **Estratégia de testes** — como testar orquestrador determinístico + LLM steps (snapshot? eval suite?)
9. **Diferencial de produto** — Rules como "compliance modes" (PCI, SOC2, ISO) é o marketing
10. **Modelo de negócio** — pricing, BYOK vs all-in, target (dev individual? equipe? enterprise?)

## TL;DR

- **Subscription no SDK: não.**
- **MVP:** CLI subprocess + subscription pessoal, uso interno apenas.
- **Produto:** SDK + API key + prompt caching desde dia 1 da migração.
- **Sequência:** valide arquitetura grátis com CLI, migre pra SDK quando confirmar que vira produto.
- **Insight central:** orquestrador programático resolve "LLM pula etapas" por construção, não por audit.
