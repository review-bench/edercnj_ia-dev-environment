# Prompt: Geração de Épico e História — Mid-Execution Continuous-Flow Heartbeat Hook

> **Instrução de uso**: Tratar este SPEC como gap-filler do EPIC-0063 (mergeado, PR #752). Decidir entre (a) extender 0063 com 22ª story via amendment, ou (b) abrir EPIC-0068 follow-up dedicado. Recomendado: (b), porque 0063 está fechado.

---

## Sistema

**Projeto**: `ia-dev-environment` — CLI generator de ambientes de desenvolvimento assistidos por IA.

**Versão base**: pós-merge EPIC-0063 (PR #752) e pós-merge EPIC-0061 story-0001 (Non-Interactive Default).

**Objetivo**: Tampar a janela cega entre as 4 camadas de Rule 24 + Rule 25 + EPIC-0063, capturando o cenário "LLM emite prosa em vez de tool call no meio de uma fase aberta sem chegar a boundary algum (push/PR/Skill explícito)".

**Princípio**: O modo `--non-interactive` (default pós-EPIC-0061-0001) elimina menus de gate, mas não elimina a possibilidade de o LLM **parar de emitir tool calls** mid-fase. Sem menu para responder, a sessão fica em silêncio até intervenção humana — o pior estado possível para um operador automatizado.

---

## Escopo do Épico (proposto: EPIC-0068 ou amendment EPIC-0063 v3)

### Contexto de negócio

Em 2026-04-28 durante execução de EPIC-0061 story-0061-0001, o LLM emitiu `TaskUpdate(status:"completed")` da sub-fase 3.1 (specialist reviews) e parou. Aguardou ~2-5 minutos por confirmação humana ("pode prosseguir?") antes que o operador percebesse o silêncio e perguntasse "estamos travados?". Essa interação se repete há meses, sob diferentes orquestradores e diferentes modelos. É a motivação direta dos EPICs 0061 (default flip) e 0063 (pre-flight gates / tool-call grammar).

**Cobertura atual** (defesa em profundidade já planejada):

| Camada | Cobre | Não cobre |
| :--- | :--- | :--- |
| Rule 13/24 normativo + CLAUDE.md | "skill inlined em prosa" | "LLM emitiu prosa esperando confirmação" |
| `verify-story-completion.sh` (Stop hook) | falta de evidência ao final | falta de progresso mid-execução |
| `enforce-preflight-gates.sh` (PreToolUse, 0063-0004/0013) | bypass em `git push` / `gh pr create` / `Skill x-pr-create` | LLM que não chega a emitir tool call algum |
| `audit-tool-call-grammar.sh` (Rule 28, 0063-0012) | `[required]` ausente em NDJSON pós-merge | detecção em tempo real durante execução |
| `enforce-phase-sequence.sh` (Rule 25 PreToolUse) | invocação fora de ordem | ausência de invocação |
| `verify-phase-gates.sh` (Rule 25 Stop hook) | gate não chamado ao final da fase | LLM travado entre invocações dentro de uma fase |

A janela cega: **LLM dentro de uma `## Phase N` aberta + último turno terminou em prosa + nenhum tool call subsequente em N segundos**. Nenhum dos hooks atuais dispara aqui porque não há evento de tool call para interceptar e a fase ainda não chegou ao Stop final.

### Proposta

Novo hook **Stop** — distinto do `verify-story-completion.sh` existente — chamado `enforce-continuous-flow.sh`:

1. Dispara em todo evento `Stop` (fim de turno do LLM).
2. Lê o `execution-state.json` mais recente sob `ai/epics/epic-*/` ou `plans/epic-*/`.
3. Aplica matriz de decisão:

| `interactiveMode` | `taskTracking.openTasks > 0` | Último evento NDJSON | Ação |
| :--- | :--- | :--- | :--- |
| `non-interactive` | sim | `tool.result` (não há `tool.call` posterior) | **NUDGE** (exit 2 + stderr) |
| `non-interactive` | sim | `phase.start` recente sem `phase.end` | **NUDGE** |
| `non-interactive` | não (todas tarefas completed) | qualquer | exit 0 — fluxo natural |
| `interactive` | qualquer | qualquer | exit 0 — comportamento clássico |
| qualquer | qualquer | último evento foi `error` ou `finding.critical`/`finding.high` | exit 0 — pausa legítima |

4. Quando NUDGE: emite via stderr a mensagem (Claude Code surfaca para o LLM no próximo turno):

```
CONTINUOUS_FLOW_INTERRUPT
Orchestrator: <name from state>
Open tasks: <N>
Phase: <current phase from state>
Next mandatory tool call (Rule 28 grammar): <derived from SKILL.md>
Action: emit the next tool call immediately. Do NOT generate prose.
Reference: feedback memory `continuous-flow-non-interactive`, Rule 24/27.
```

5. Self-check via Rule 26 (`enforce-continuous-flow.sh --self-check`).

**Diferença crítica em relação a hooks existentes**:

- `verify-story-completion.sh` checa **evidência** (artefatos no disco) ao final.
- `enforce-continuous-flow.sh` checa **progresso** (delta de tool calls) entre turnos.

São complementares, não redundantes.

### Stories propostas

| # | Título | Esforço | Bloqueia |
| :--- | :--- | :--- | :--- |
| -0001 | Field `interactiveMode` em `execution-state.json` (escrita pelos 8 orquestradores Anexo B) | 0.5 dia | -0002, -0003 |
| -0002 | Hook `enforce-continuous-flow.sh` + smoke test + registro em `settings.json` | 1 dia | -0004 |
| -0003 | Integração com Rule 28 grammar (deriva "next mandatory call" do SKILL.md ativo) | 0.5 dia | -0004 |
| -0004 | E2E smoke test (3 cenários: nudge, no-nudge interactive, no-nudge legitimate-pause) | 0.5 dia | — |

Total: ~2.5 dias.

### Fora do escopo

- Reescrever os 8 orquestradores para subagent-per-phase (analisado e rejeitado — alto custo, risco de regressão).
- Auto-retry quando NUDGE não destrava (LLM ainda preso após nudge): caso recovery_mode tradicional.
- Telemetria de "tempo morto" (gap entre eventos): pode ser feature do `/x-telemetry-analyze` em outro escopo.

### Acceptance criteria

```gherkin
Cenario: NUDGE em fase aberta sem progresso
  DADO orchestrator x-story-implement com interactiveMode=non-interactive
  E taskTracking.openTasks=[14, 15] (sub-fase 3.1 e 3.2 abertas)
  E último evento NDJSON é tool.result de TaskUpdate(14, completed)
  QUANDO Stop hook dispara
  ENTÃO exit 2
  E stderr contém "CONTINUOUS_FLOW_INTERRUPT"
  E stderr nomeia "x-review-pr" como next mandatory call

Cenario: sem NUDGE em modo interactive
  DADO orchestrator x-story-implement com interactiveMode=interactive
  E mesmas open tasks
  QUANDO Stop hook dispara
  ENTÃO exit 0
  E stderr vazio

Cenario: sem NUDGE quando há finding HIGH pendente
  DADO orchestrator x-story-implement non-interactive
  E último evento NDJSON é finding.high
  QUANDO Stop hook dispara
  ENTÃO exit 0 — pausa legítima para decisão humana

Cenario: sem NUDGE quando todas as tarefas estão completas
  DADO orchestrator x-story-implement non-interactive
  E taskTracking.openTasks=[]
  QUANDO Stop hook dispara
  ENTÃO exit 0 — fluxo natural de fim de orchestrator
```

### Métrica de sucesso

- Telemetria: P95 do gap entre `tool.call` consecutivos em modo non-interactive < 30s para 95% das stories merged pós-rollout.
- Operacional: zero ocorrências de "estamos travados?" reportadas em sessões pós-rollout (target qualitativo).
- CI: smoke test verde em todos PRs subsequentes ao merge.

---

## Notas de decisão

- **Por que Stop hook e não PostToolUse**: PostToolUse só dispara após tool call. Quando o LLM emite prosa, não há PostToolUse. Stop é o único evento que captura "fim de turno" independente do conteúdo.
- **Por que não rule nova**: Rule 28 (Tool-Call Grammar do EPIC-0063-0012) já cobre o contrato de invocação. O hook é a Camada 2 (runtime) dela, não rule independente.
- **Por que separar de `verify-story-completion.sh`**: separação de responsabilidades — o existente checa evidência ao final do orchestrator; o novo checa progresso mid-execução. Misturar viola SRP.
- **Backward compat**: `interactiveMode` ausente em state files legados → tratado como `interactive` (Rule 19 fallback matrix). Hook é no-op nesse caso.
