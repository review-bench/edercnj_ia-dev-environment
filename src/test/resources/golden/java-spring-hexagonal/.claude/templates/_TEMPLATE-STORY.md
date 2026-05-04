---
requires-capabilities: [governance.value-driven-templates]
template-version: "2.0"
---

# História: <Título da História>

**ID:** <story-XXXX-YYYY>
**Chave Jira:** <CHAVE-JIRA>
**Status:** Pendente

> **Status Transitions (Rule 29 — refinement-gate):**
> valores permitidos `Pendente | Refinada | Planejada | Em Andamento | Concluída | Falha | Bloqueada`.
> Transições válidas: `Pendente → Refinada` (via `/x-refine-story`);
> `Refinada → Planejada | Em Andamento | Bloqueada`;
> `Planejada → Em Andamento | Falha | Bloqueada`;
> `Em Andamento → Concluída | Falha | Bloqueada`;
> reabertura `Concluída → Em Andamento` (via `x-reconcile-status --apply`) e
> `Falha → Pendente`; `Bloqueada → Pendente | Planejada | Em Andamento | Falha`.
> Ver [`.claude/rules/29-refinement-gate.md`](../.claude/rules/29-refinement-gate.md).

---

## 1. Visão

> **O que queremos alcançar — em uma frase de usuário.**
> Formato: `Como <persona específica>, quero <capacidade>, para que <benefício mensurável>`.
> A persona deve ser um papel real (ex: "engenheiro de backend configurando um novo serviço"),
> não um sistema ("o sistema") nem um genérico ("o usuário").

Como **<persona específica>**, quero <capacidade/ação concreta>, para que <benefício mensurável/observável>.

<Contexto adicional (1-3 parágrafos): por que esta história existe agora, como ela se encaixa no épico, qual dor ela alivia. Seja específico — evite "melhora a experiência" sem evidência.>

---

## 2. Persona & Cenário

> **Quem usa isso — e em que contexto.**
> Descreva a persona com granularidade suficiente para guiar decisões de design.
> Inclua o cenário de uso (jornada do usuário até este ponto) e os critérios de sucesso pessoal.

**Persona:** <Papel/título específico>
**Cenário de uso:** <O que a persona está fazendo quando encontra essa feature. Ex: "configurando pipeline de CI pela primeira vez", "revisando uma story em refinement gate">
**Dor atual:** <O que a persona faz hoje que é lento, incorreto, ou frustrante>
**Critério de sucesso pessoal:** <O que a persona diria para confirmar que esta story entregou valor? Ex: "não preciso mais preencher manualmente X">
**Contexto técnico relevante:** <O que a persona sabe/não sabe; ex: "familiarizada com YAML, não com Pebble templates">

---

## 3. Entrega de Valor

> **Por que isso importa — em termos mensuráveis.**

- **Valor Principal:** <Descrição do valor de negócio mensurável. Evite "melhora eficiência" — seja específico: "reduz de 3 passos manuais para 1 clique">
- **Métrica de Sucesso:** <Como medir que o valor foi entregue. Unidade + valor-alvo + quando medir>
- **Impacto no Negócio:** <Impacto direto para o usuário/stakeholder. Ex: "revisores gastam 30% menos tempo em code review de stories mal-escritas">

---

## 4. AC (Gherkin — 4 categorias mandatórias)

> **Critérios de aceite em Gherkin PT-BR.**
> As 4 categorias abaixo são mandatórias. Adapte os cenários ao domínio desta story.
> Ordem TPP: degenerate → happy → error/boundary → performance/SLA → security.
>
> **Categorias:**
> - `Degenerate` — input vazio, zero, nulo, lista vazia
> - `Happy` — fluxo principal de valor (caminho dourado)
> - `Error/Boundary` — falha externa controlada, valores nos limites do domínio
> - `Performance/SLA` — tempo de resposta, throughput, tamanho de payload
> - `Security` — autenticação, autorização, injeção, dados sensíveis

```gherkin
Cenario: Degenerate — <input nulo/vazio/zero>
  DADO que <pré-condição de caso nulo ou entrada mínima>
  QUANDO <ação é executada com esse input>
  ENTÃO <comportamento esperado — ex: erro tipado, lista vazia, 0 resultados>

Cenario: Happy — <fluxo principal>
  DADO que <pré-condição válida>
  QUANDO <ação principal é executada>
  ENTÃO <resultado esperado>
  E <validação adicional, se necessária>

Cenario: Error — <falha externa ou condição de borda>
  DADO que <pré-condição de falha ou valor no limite do domínio>
  QUANDO <ação é executada>
  ENTÃO <comportamento de erro esperado>
  E <nenhum efeito colateral indesejado>

Cenario: Performance/SLA — <latência ou throughput dentro do SLA>
  DADO que <conjunto típico de dados de entrada>
  QUANDO <operação é executada>
  ENTÃO <tempo de resposta ≤ Xms (p95)> ou <throughput ≥ Y req/s>

Cenario: Security — <autenticação, autorização ou sanitização>
  DADO que <usuário sem permissão> ou <input malicioso>
  QUANDO <ação é tentada>
  ENTÃO <acesso negado> ou <input sanitizado sem efeito indesejado>
```

---

## 5. Contratos

> **Interfaces tipadas — request, response, eventos.**
> Use tabelas para campos escalares. Para schemas complexos, use bloco JSON/TypeScript.
> Esses contratos são input para `x-detect-spec-drift` (EPIC-0071) — seja preciso nos tipos.

### 5.1 Request

| Campo | Tipo | M/O | Validações | Exemplo |
| :--- | :--- | :--- | :--- | :--- |
| `<campo>` | `<String(255)/UUID/BigDecimal/Integer/List<String>>` | `<M ou O>` | `<min/max, regex, enum values>` | `<valor concreto>` |

### 5.2 Response

| Campo | Tipo | Sempre presente | Descrição |
| :--- | :--- | :--- | :--- |
| `<campo>` | `<UUID/String/BigDecimal>` | `<Sim ou Não>` | `<descrição do campo>` |

### 5.3 Error Codes

| HTTP Status | Error Code | Condição |
| :--- | :--- | :--- |
| `<status>` | `<code>` | `<condição que dispara>` |

### 5.4 Event Schema (para event-driven)

> Incluir apenas quando a story produz ou consome eventos.

| Campo | Tipo | Obrigatório | Descrição |
| :--- | :--- | :--- | :--- |
| `eventType` | `String` | Sim | Tipo do evento |
| `eventVersion` | `String` | Sim | Versão do schema |
| `timestamp` | `Instant` | Sim | ISO-8601 UTC |
| `correlationId` | `UUID` | Sim | ID de correlação |
| `payload` | `Object` | Sim | Payload do evento |

---

## 6. Tasks

> **Decomposição em tarefas implementáveis.**
> Cada task = 1 branch = 1 PR. Mínimo 3, máximo 8. Tamanho ideal: M (50-150 LOC).

#### TASK-{{EPIC_ID}}-{{STORY_ID}}-001: <Título imperativo (máx 80 chars)>

- **Layer:** <Domain|Port|Adapter|Application|Config|Test|Doc>
- **Test Type:** <Unit|Integration|API|Contract|E2E|Smoke|Verification>
- **Size:** <S|M|L>
- **Dependencies:** —
- **Branch:** `feat/task-{{EPIC_ID}}-{{STORY_ID}}-001-short-desc`
- **Files:**
  - `path/to/file1.ext`
- **Acceptance Criteria:**
  - [ ] <critério 1>
  - [ ] <critério 2>

#### TASK-{{EPIC_ID}}-{{STORY_ID}}-002: <Título imperativo>

- **Layer:** <camada>
- **Test Type:** <tipo>
- **Size:** <S|M|L>
- **Dependencies:** TASK-{{EPIC_ID}}-{{STORY_ID}}-001
- **Branch:** `feat/task-{{EPIC_ID}}-{{STORY_ID}}-002-short-desc`
- **Files:**
  - `path/to/file1.ext`
- **Acceptance Criteria:**
  - [ ] <critério 1>

#### TASK-{{EPIC_ID}}-{{STORY_ID}}-003: <Título imperativo>

- **Layer:** <camada>
- **Test Type:** <tipo>
- **Size:** <S|M|L>
- **Dependencies:** TASK-{{EPIC_ID}}-{{STORY_ID}}-001, TASK-{{EPIC_ID}}-{{STORY_ID}}-002
- **Branch:** `feat/task-{{EPIC_ID}}-{{STORY_ID}}-003-short-desc`
- **Files:**
  - `path/to/file1.ext`
- **Acceptance Criteria:**
  - [ ] <critério 1>

---

## 7. Dependências

### 7.1 Dependências da Story

| Blocked By | Blocks |
| :--- | :--- |
| <story-XXXX-YYYY ou —> | <story-XXXX-YYYY ou —> |

### 7.2 File Footprint (EPIC-0041 parallelism evaluation)

```
write:
  - <path/to/new/file.java>
read:
  - <path/to/existing/dependency.java>
regen:
  - <path/to/golden/file.md>
```

---

## 8. Decision Rationale

> **Registro de decisões de design desta story.**
> Obrigatório: ≥ 1 item com o micro-template de 4 linhas.
> Aceita `N/A — <motivo curto>` apenas quando genuinamente sem trade-off relevante.

**Decisão:** <statement da decisão tomada>
**Motivo:** <por que esta opção foi escolhida — restrição técnica ou de negócio>
**Alternativa descartada:** <o que foi rejeitado e por quê>
**Consequência:** <trade-off ou implicação futura>

---

## 9. Refinement Verdict

<!-- CONTRACT: Este bloco é populado exclusivamente por `/x-refine-story` (EPIC-0069).
     Não editar manualmente — `audit-refinement-gate.sh` detecta divergência via
     verdictHash (Rule 29 §verdictHash). Estrutura canônica:

     **Status:** approved | rejected | tbd
     **Scope:** story
     **Refined at:** ISO-8601
     **Verdict hash:** sha256(## Refinement Verdict block content)
     ### Dimensions: table with per-dimension status
     ### Blockers: list (empty if approved)
     ### Rationale: paragraph
-->

> _Slot reservado para `/x-refine-story`. Não editar manualmente._
>
> **Status:** TBD — execute `/x-refine-story <story-id>` para preencher.
