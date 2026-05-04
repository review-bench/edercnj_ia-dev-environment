---
requires-capabilities: [governance.value-driven-templates]
template-version: "2.0"
---

# Épico: <Título do Épico>

**Autor:** <Papel/Nome do autor>
**Data:** <Data de criação>
**Versão:** <Versão do documento>
**Status:** <Pendente | Refinada | Planejada | Em Andamento | Concluída | Falha | Bloqueada>

> **Status Transitions (Rule 29 — refinement-gate):**
> artifacts lifecycle-controlados (Story/Task) usam o enum canônico
> `Pendente | Refinada | Planejada | Em Andamento | Concluída | Falha | Bloqueada`.
> O campo Status do Épico aqui é documental e reflete o estado
> agregado das histórias filhas. Transições permitidas do enum:
> `Pendente → Refinada` (via `/x-refine-epic`);
> `Refinada → Planejada | Em Andamento | Bloqueada`;
> `Planejada → Em Andamento | Falha | Bloqueada`;
> `Em Andamento → Concluída | Falha | Bloqueada`;
> reabertura `Concluída → Em Andamento` (via `x-reconcile-status --apply`) e
> `Falha → Pendente`; `Bloqueada → Pendente | Planejada | Em Andamento | Falha`.
> Ver [`.claude/rules/29-refinement-gate.md`](../.claude/rules/29-refinement-gate.md).

---

## 0.5 Cross-Epic Dependencies

> **Cross-Epic Dependency Awareness (EPIC-0076)**
> Epic-level dependencies are declared in this section and used by the implementation workflow as a Phase 0.5 gate.
> If any dependency's `expectedStatus` is not met, `x-implement-epic` aborts synchronously with exit 1.
> TODO: link to the dedicated cross-epic dependency rule and ADR once those artifacts land in subsequent stories of EPIC-0076.

### Blocked By Epics

| Epic ID   | Title                      | Expected Status @ Start | Reason / Surface Touched                               |
| --------- | -------------------------- | ----------------------- | ------------------------------------------------------ |
| (none)    | —                          | —                       | —                                                      |

### Blocks (informational, derived)

| Epic ID   |
| --------- |
| (none)    |

### In-Flight Reference Allowance

(Nenhuma dependência em voo declarada.)

---

## 1. Visão & Problema

> **O que observamos — e por quê importa agora.**
> Descreva o problema como uma dor observável com evidências concretas: métricas, incidentes, feedback de usuários, débito acumulado.
> Responda: "O que está quebrando / faltando / custando caro hoje, e o que acontece se não fizermos nada?"
> Extensão recomendada: 3-8 parágrafos. Sem cap artificial — a narrativa deve ser completa.

**Chave Jira:** <CHAVE-JIRA>

<Descreva o contexto e a dor observada. Use evidências: "X% dos épicos revisados têm a seção Y preenchida com placeholder", "tempo médio de refinamento aumentou Z", etc.>

<Descreva o impacto se o problema não for resolvido: retrabalho, alucinações downstream, decisões sem rastreabilidade.>

<Descreva o escopo exato deste épico: o que será construído, o que explicitamente está fora (ver seção 5).>

### 1.1 Referências e Contexto

- <Nome do documento/spec, RFC, pós-mortem> (Link)
- <Diagrama/ADR relevante> (Link)

---

## 2. Persona & Stakeholders

> **Para quem estamos construindo isso.**
> Liste ≥ 1 persona principal. Use `### Persona-N` para cada persona distinta.
> Inclua: papel, responsabilidade relevante, dor atual, como este épico muda o dia a dia dela.

### Persona-1: <Papel principal — ex: "Engenheiro de Software planejando um novo épico">

- **Papel:** <Título/papel específico>
- **Dor atual:** <O que essa persona faz hoje que é ineficiente, inconsistente ou propenso a erro?>
- **Ganho com este épico:** <Como o trabalho dela muda após a entrega?>
- **Critério de sucesso pessoal:** <O que ela diria para confirmar que o épico entregou valor?>

### Persona-2: <Papel secundário, se aplicável>

<...>

### Stakeholders (informacional)

| Stakeholder | Interesse | Nível de envolvimento |
| :--- | :--- | :--- |
| <Papel> | <O que acompanha ou valida> | Informado / Consultado / Responsável |

---

## 3. Hipótese & OKRs

> **Por que acreditamos que isso vai funcionar — e como vamos medir.**
> A hipótese deve ter forma `Se <ação>, então <resultado observável>, porque <mecanismo causal>`.
> OKRs: ≥ 1 Objective com ≥ 1 Key Result mensurável (unidade + valor-alvo + prazo).

### Hipótese de Valor

**Se** <descreva a intervenção — o que será entregue>,
**então** <descreva o resultado esperado — comportamento que mudará>,
**porque** <explique o mecanismo causal — por que a intervenção produz o resultado>.

### OKRs

| Objetivo | Key Result | Métrica | Valor Atual | Meta | Prazo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| <Objetivo de negócio ou técnico> | <Resultado mensurável> | <Unidade de medida> | <Baseline> | <Alvo> | <Data/sprint> |

---

## 4. Alternativas Consideradas

> **O que descartamos e por quê.**
> ≥ 2 alternativas com rationale de rejeição. Sem alternativas = sem evidência de análise.
> Formato: `### Alternativa N — <Título>` + tabela de prós/contras + decisão.

### Alternativa 1 — <Título da opção não escolhida>

**Descrição:** <O que esta alternativa propõe.>

| Prós | Contras |
| :--- | :--- |
| <Vantagem 1> | <Desvantagem 1> |
| <Vantagem 2> | <Desvantagem 2> |

**Decisão de rejeição:** <Por que esta alternativa foi descartada. Seja específico — custo, risco, incompatibilidade com constraints, evidência contrária.>

### Alternativa 2 — <Título da segunda opção>

**Descrição:** <O que esta alternativa propõe.>

| Prós | Contras |
| :--- | :--- |
| <Vantagem 1> | <Desvantagem 1> |

**Decisão de rejeição:** <Motivo.>

---

## 5. Escopo

> **O que está IN e o que está explicitamente OUT.**
> Liste ≥ 3 itens em cada coluna — a lista OUT evita scope creep durante execução.

### In-Scope

- <Feature ou comportamento que será entregue — seja específico>
- <...>
- <...>

### Out-of-Scope (explícito)

> Itens aqui não serão implementados neste épico, independente de parecerem relacionados.

- <O que não será feito — e por quê (ex: "migração de dados legados → EPIC-XXXX")>
- <...>
- <...>

### Dependências Técnicas

| Dependência | Tipo | Épico/Ticket | Status |
| :--- | :--- | :--- | :--- |
| <Sistema/serviço/biblioteca> | Bloqueante / Informacional | <Link> | <Status> |

---

## 6. Riscos

> **O que pode dar errado — e como mitigamos.**
> ≥ 1 risco de produto + ≥ 1 risco técnico. Priorize por impacto × probabilidade.

| # | Risco | Tipo | Probabilidade | Impacto | Mitigação |
| :--- | :--- | :--- | :--- | :--- | :--- |
| R1 | <Risco de produto — ex: baixa adoção, mudança de requisito> | Produto | Alta/Média/Baixa | Alto/Médio/Baixo | <O que faremos para reduzir probabilidade ou impacto> |
| R2 | <Risco técnico — ex: dependência externa instável, performance> | Técnico | Alta/Média/Baixa | Alto/Médio/Baixo | <Mitigação> |
| R3 | <Risco de dados/migração, se aplicável> | Dados | — | — | — |

---

## 7. Índice de Histórias

> Tabela de todas as histórias do épico com suas dependências e entrega de valor.
> Planejamento: `Pendente | Refinada | Planejada | Em Andamento | Concluída`.

| ID | Título | Dependências (Blocked By) | Entrega de Valor | Planejamento |
| :--- | :--- | :--- | :--- | :--- |
| [story-XXXX-0001](./story-XXXX-0001.md) | <Título da história> | — | <Valor mensurável de negócio> | {{PLANNING_STATUS}} |
| [story-XXXX-0002](./story-XXXX-0002.md) | <Título da história> | story-XXXX-0001 | <Valor mensurável de negócio> | {{PLANNING_STATUS}} |
| [story-XXXX-YYYY](./story-XXXX-YYYY.md) | <Título da história> | <Dependências separadas por vírgula> | <Valor mensurável de negócio> | {{PLANNING_STATUS}} |

### File Footprint (EPIC-0041 parallelism evaluation)

```
write:
  - <path/to/new/file.java>
read:
  - <path/to/existing/dependency.java>
regen:
  - <path/to/golden/file.md>
```

---

## 8. Quality Gates

> **Critérios de pronto — globais para todas as histórias do épico.**

### Global Definition of Ready (DoR)

- <Critério 1 que deve estar satisfeito para qualquer história entrar em desenvolvimento>
- <Critério 2 — ex: "Refinement Verdict aprovado via `/x-refine-story`">
- <Critério N>

### Global Definition of Done (DoD)

- **Cobertura:** <Meta de cobertura — ex: ≥ 95% Line, ≥ 90% Branch (Rule 05 — absolute gate)>
- **Testes Automatizados:** <Tipos de testes exigidos e cenários obrigatórios>. Cada história DEVE ter pelo menos 1 teste automatizado validando o critério de aceite principal.
- **Smoke Tests:** Obrigatório quando `testing.smoke_tests == true`. Cada história deve passar no smoke gate.
- **Relatório de Cobertura:** <Formato e granularidade esperada>
- **Documentação:** <Artefatos de documentação que devem estar atualizados>
- **Persistência:** <Critério de integridade de dados, se aplicável — ou "N/A">
- **Performance:** <SLO de latência/throughput — ou "N/A">
- **TDD Compliance:** Commits show test-first pattern. Explicit refactoring after green. Tests are incremental (from simple to complex via TPP — Transformation Priority Premise).
- **Double-Loop TDD:** Acceptance tests derived from Gherkin scenarios (outer loop). Unit tests guided by TPP (inner loop).

---

## 9. Origem & Referências

> **De onde viemos — rastreabilidade completa.**
> Liste documentos originadores, ADRs, épicos relacionados, specs externas e qualquer artefato que justifique as decisões deste épico.

| Artefato | Tipo | Link | Relevância |
| :--- | :--- | :--- | :--- |
| <Nome do documento/spec> | Spec / ADR / Epic / PRD / Pós-mortem | <Link> | <Por que este artefato é relevante para este épico> |
| <ADR-XXXX — Nome> | ADR | <Link> | <Decisão que afeta este épico> |
| <EPIC-XXXX — Nome> | Epic predecessor | <Link> | <Como este épico se relaciona> |

### Branching (EPIC-0065 — Feature Creation Chain)

> Planning artifacts for this epic are committed on branch `docs/<epic-id>-<slug>`
> (base: `epic/XXXX`, target: `epic/XXXX`) via `x-create-feature`.
> The branch is auto-merged after CI passes (Rule 21 §Anti-Patterns EPIC-0065 exception).
> The final epic-to-develop PR (`epic/XXXX → develop`) is always a **manual gate**.

---

## Refinement Verdict

> _Slot reservado para `/x-refine-epic`. Não editar manualmente — `audit-refinement-gate.sh` detecta divergência via verdictHash (Rule 29 §verdictHash)._
>
> **Status:** TBD — execute `/x-refine-epic <epic-id>` para preencher.
