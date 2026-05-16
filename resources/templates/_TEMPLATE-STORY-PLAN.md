---
generated-by: {{GENERATED_BY}}
generated-at: {{GENERATED_AT}}
story-id: {{STORY_ID}}
schema-version: "1.0"
---

# Plano de Implementação — {{STORY_ID}}

## Cabeçalho

| Campo | Valor |
|-------|-------|
| Story ID | {{STORY_ID}} |
| Épico | {{EPIC_ID}} |
| Fase do Épico | {{EPIC_PHASE}} |
| Status | Planejada |
| Gerado por | {{GENERATED_BY}} |
| Gerado em | {{GENERATED_AT}} |

---

## 1. Visão Geral da Implementação

> OBRIGATÓRIO — Escrito pelo Architect subagent em prosa, 3-6 parágrafos.
> Deve responder: O QUÊ será feito? POR QUÊ? COMO (abordagem de alto nível)?
> Legível por humanos SEM consultar outros documentos.
> Referenciar arquivos concretos com o que cada um fará.

{{IMPLEMENTATION_OVERVIEW}}

---

## 2. Escopo da Mudança

### 2.1 Resumo

| Categoria | Quantidade |
|-----------|-----------|
| Artefatos Criados | {{CREATED_COUNT}} |
| Artefatos Modificados | {{MODIFIED_COUNT}} |
| Artefatos Excluídos | {{DELETED_COUNT}} |

### 2.2 Artefatos Criados

> Para cada novo artefato: caminho + descrição verbal do que é e por que existe.
> Seja específico: o que este arquivo vai conter? Qual sua responsabilidade?

**`{{CREATED_PATH_1}}`** — CRIAR
> {{CREATED_DESCRIPTION_1}}

**`{{CREATED_PATH_2}}`** — CRIAR
> {{CREATED_DESCRIPTION_2}}

### 2.3 Artefatos Modificados

> Para cada artefato existente que muda: caminho + o que existe hoje + o que vai mudar + por quê.

**`{{MODIFIED_PATH_1}}`** — MODIFICAR
> Estado atual: {{MODIFIED_CURRENT_STATE_1}}
> Mudança: {{MODIFIED_CHANGE_DESCRIPTION_1}}
> Motivo: {{MODIFIED_REASON_1}}

**`{{MODIFIED_PATH_2}}`** — MODIFICAR
> Estado atual: {{MODIFIED_CURRENT_STATE_2}}
> Mudança: {{MODIFIED_CHANGE_DESCRIPTION_2}}
> Motivo: {{MODIFIED_REASON_2}}

### 2.4 Artefatos Excluídos

> Para cada artefato removido: caminho + motivo da exclusão.
> Se não há exclusões, escrever: "Nenhum artefato será excluído nesta story."

{{DELETED_ARTIFACTS_OR_NONE}}

### 2.5 Diagrama de Classes / Componentes Afetados

> OBRIGATÓRIO quando há criação ou modificação de classes, skills, ou componentes.
> Diagrama Mermaid mostrando classes novas (NEW), modificadas (MOD) e removidas (DEL).
> Para stories de Skills/Templates: usar diagrama de componentes (flowchart) no lugar de classDiagram.
> Se não houver componentes estruturais (e.g., story puramente de config), documentar "N/A — story sem impacto estrutural".

```mermaid
{{AFFECTED_COMPONENT_DIAGRAM}}
```

> Legenda:
> - `<<NEW>>` — criado nesta story
> - `<<MOD>>` — modificado nesta story
> - `<<DEL>>` — removido nesta story

---

## 3. Estratégia de Testes

### 3.1 Cenários Gherkin (Critérios de Aceite)

> Cenários completos em Gherkin. Mínimo 4 categorias obrigatórias:
> Degenerado (entrada nula/vazia), Happy Path (fluxo principal), Erro/Boundary, Segurança.
> Estes cenários são os testes de aceite (outer loop do Double-Loop TDD).

{{GHERKIN_SCENARIOS}}

### 3.2 Testes Existentes Impactados

> OBRIGATÓRIO — mapear TODOS os testes existentes que precisam ser MODIFICADOS ou EXCLUÍDOS.
> Se uma regra de negócio muda, os testes que a cobrem também mudam.
> Se não houver testes impactados, escrever explicitamente "Nenhum teste existente é impactado por esta story."

#### 3.2.1 Testes a Modificar

| Arquivo de Teste | Método de Teste | Motivo da Mudança | O que muda na asserção |
|-----------------|----------------|-----------------|----------------------|
| {{TEST_FILE_1}} | {{TEST_METHOD_1}} | {{TEST_REASON_1}} | {{TEST_ASSERTION_CHANGE_1}} |

#### 3.2.2 Testes a Excluir

| Arquivo de Teste | Método de Teste | Motivo da Exclusão |
|-----------------|----------------|------------------|
| {{DELETED_TEST_FILE_1}} | {{DELETED_TEST_METHOD_1}} | {{DELETED_TEST_REASON_1}} |

#### 3.2.3 Novos Testes de Regressão Necessários

| Nome do Teste | Cenário Coberto | Motivo de Criação |
|--------------|----------------|-----------------|
| {{NEW_REGRESSION_TEST_1}} | {{NEW_REGRESSION_SCENARIO_1}} | {{NEW_REGRESSION_REASON_1}} |

### 3.3 Plano TDD — Ciclos em Ordem TPP

> Para cada ciclo: descrever EM LINGUAGEM NATURAL o teste a escrever, por que falha (RED),
> o que implementar para passar (GREEN) e o que refatorar (REFACTOR).
> Ordem TPP obrigatória: nil → constant → scalar → conditional → collection → complex.
> Mínimo 3 ciclos. O Ciclo 1 DEVE ser degenerate (caso nulo/vazio/inválido).

#### Ciclo 1 — Degenerate (`{} → nil`)

**RED — Teste a escrever:**
{{TDD_CYCLE_1_RED}}

**Por que vai falhar:** {{TDD_CYCLE_1_FAILURE_REASON}}

**GREEN — Implementação mínima:**
{{TDD_CYCLE_1_GREEN}}

**REFACTOR — O que melhorar:**
{{TDD_CYCLE_1_REFACTOR}}

---

#### Ciclo 2 — Constant (`nil → constant`)

**RED — Teste a escrever:**
{{TDD_CYCLE_2_RED}}

**Por que vai falhar:** {{TDD_CYCLE_2_FAILURE_REASON}}

**GREEN — Implementação mínima:**
{{TDD_CYCLE_2_GREEN}}

**REFACTOR — O que melhorar:**
{{TDD_CYCLE_2_REFACTOR}}

---

#### Ciclo N — {{TDD_CYCLE_N_LEVEL}} (`{{TDD_CYCLE_N_TRANSFORM}}`)

**RED — Teste a escrever:**
{{TDD_CYCLE_N_RED}}

**Por que vai falhar:** {{TDD_CYCLE_N_FAILURE_REASON}}

**GREEN — Implementação mínima:**
{{TDD_CYCLE_N_GREEN}}

**REFACTOR — O que melhorar:**
{{TDD_CYCLE_N_REFACTOR}}

---

## 4. Tasks de Implementação

### 4.1 Tasks de Código

| Task ID | Descrição | Layer | Tam | Depende de | Arquivo(s) Principal(is) |
|---------|-----------|-------|-----|-----------|--------------------------|
| {{CODE_TASK_ID_1}} | {{CODE_TASK_DESC_1}} | {{CODE_TASK_LAYER_1}} | {{CODE_TASK_SIZE_1}} | {{CODE_TASK_DEPS_1}} | {{CODE_TASK_FILES_1}} |
| {{CODE_TASK_ID_2}} | {{CODE_TASK_DESC_2}} | {{CODE_TASK_LAYER_2}} | {{CODE_TASK_SIZE_2}} | {{CODE_TASK_DEPS_2}} | {{CODE_TASK_FILES_2}} |

### 4.2 Tasks de Review — OBRIGATÓRIAS

> Estas tasks são obrigatórias em toda story. Não podem ser removidas ou marcadas N/A.
> Dependem de todas as tasks de código estarem concluídas.

| Task ID | Review | Skill Invocada | Depende de |
|---------|--------|---------------|-----------|
| {{REVIEW_TASK_TL_ID}} | Tech Lead Review | `x-review-pr` | Todas as tasks de código |
| {{REVIEW_TASK_SEC_ID}} | Security Review | `x-review-security` | Todas as tasks de código |
| {{REVIEW_TASK_QA_ID}} | QA Review | `x-review-qa` | Todas as tasks de código |

### 4.3 Tasks de Documentação — OBRIGATÓRIAS

> Toda story deve atualizar ao menos um artefato de documentação.
> Exemplos: README, CHANGELOG, ADR, skill SKILL.md, comentários de conhecimento.

| Task ID | Artefato a Atualizar | Ação | Depende de |
|---------|---------------------|------|-----------|
| {{DOC_TASK_ID_1}} | {{DOC_TASK_ARTIFACT_1}} | {{DOC_TASK_ACTION_1}} | {{DOC_TASK_DEPS_1}} |

---

## 5. Critérios de Conclusão

### 5.1 Conclusão de Cada Task

Ao finalizar uma task, o executor (`x-implement-task`) DEVE:
- [ ] Completar todos os ciclos TDD (RED → GREEN → REFACTOR) do `plan-task-TASK-ID.md`
- [ ] Todos os testes passando: `{{TEST_COMMAND}}`
- [ ] Código/artefato compila ou é válido: `{{COMPILE_OR_VALIDATE_COMMAND}}`
- [ ] Checklist de segurança da task verificada
- [ ] Nenhum TODO/FIXME/HACK no escopo desta task
- [ ] Escrever `**Status:** Concluída` no arquivo `task-TASK-ID.md`
- [ ] Atualizar `execution-state.json`: `tasks.TASK-ID.status = COMPLETE`

### 5.2 Conclusão da Story

Ao finalizar todas as tasks, o executor (`x-implement-story`) DEVE:
- [ ] Todas as tasks de código com status `Concluída`
- [ ] Tech Lead Review (task {{REVIEW_TASK_TL_ID}}): executada e com resultado GO
- [ ] Security Review (task {{REVIEW_TASK_SEC_ID}}): executada
- [ ] QA Review (task {{REVIEW_TASK_QA_ID}}): executada
- [ ] Tasks de documentação concluídas
- [ ] Escrever `**Status:** Concluída` no arquivo `story-{{STORY_ID}}.md`
- [ ] Atualizar `execution-state.json`: `stories.{{STORY_ID}}.status = SUCCESS`
- [ ] Atualizar linha desta story no `IMPLEMENTATION-MAP.md` do épico → `Concluída`

---

## 6. Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| {{RISK_DESCRIPTION_1}} | {{RISK_PROBABILITY_1}} | {{RISK_IMPACT_1}} | {{RISK_MITIGATION_1}} |
| {{RISK_DESCRIPTION_2}} | {{RISK_PROBABILITY_2}} | {{RISK_IMPACT_2}} | {{RISK_MITIGATION_2}} |

---

## 7. Dependências e File Footprint

### 7.1 Dependências de Stories

| Story | Tipo | Motivo |
|-------|------|--------|
| {{STORY_DEP_ID_1}} | {{STORY_DEP_TYPE_1}} | {{STORY_DEP_REASON_1}} |

### 7.2 File Footprint

#### write:
{{FILE_FOOTPRINT_WRITE}}

#### read:
{{FILE_FOOTPRINT_READ}}

#### regen:
{{FILE_FOOTPRINT_REGEN}}
