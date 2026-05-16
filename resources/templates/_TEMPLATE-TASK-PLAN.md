---
generated-by: {{GENERATED_BY}}
generated-at: {{GENERATED_AT}}
task-id: {{TASK_ID}}
story-id: {{STORY_ID}}
---

# Plano de Task — {{TASK_ID}}

## Cabeçalho

| Campo | Valor |
|-------|-------|
| Task ID | {{TASK_ID}} |
| Story ID | {{STORY_ID}} |
| Épico | {{EPIC_ID}} |
| Layer | {{LAYER}} |
| Tipo de Teste | {{TEST_TYPE}} |
| Tamanho | {{SIZE}} |
| Status | Planejada |
| Gerado por | {{GENERATED_BY}} |
| Gerado em | {{GENERATED_AT}} |

---

## 1. Contexto na Story

> OBRIGATÓRIO — back-reference explícita: qual parte da story esta task implementa?
> Um leitor humano deve entender O QUÊ esta task entrega sem ler outros arquivos.
> Referenciar a seção correspondente no plan-story se disponível.

Esta task implementa: **{{STORY_CONTEXT}}**

Referência no plano da story: `plan-story-{{STORY_ID}}.md > {{STORY_PLAN_SECTION_REF}}`

---

## 2. Objetivo da Task

> OBRIGATÓRIO — 1-2 parágrafos em linguagem que um humano entende.
> Deve referenciar o(s) arquivo(s) concreto(s) que serão criados/modificados e o que farão.

{{TASK_OBJECTIVE}}

---

## 3. Guia de Implementação

### 3.1 Alvo Principal

| Campo | Valor |
|-------|-------|
| Arquivo principal | `{{MAIN_FILE}}` |
| Ação | {{MAIN_ACTION}} |
| Classe / Função / Componente | `{{CLASS_OR_FUNCTION}}` |
| Padrão de Design | {{DESIGN_PATTERN}} |

### 3.2 Diagrama de Classes / Componentes desta Task

> Subconjunto do diagrama do `plan-story` — apenas o que ESTA task toca.
> Ajuda a IA e o desenvolvedor a entender as relações entre os componentes criados/modificados.
> Para stories de Skills/Templates: usar flowchart ou diagrama de componentes.
> Se a task não cria nem modifica componentes estruturais, escrever "N/A".

```mermaid
{{TASK_COMPONENT_DIAGRAM}}
```

### 3.3 Testes Existentes Impactados por esta Task

> Quais testes já existem e serão afetados pela implementação DESTA task?
> Se nenhum, escrever explicitamente "Nenhum teste existente é impactado por esta task."

| Arquivo de Teste | Método | Tipo de Impacto | O que muda na asserção |
|-----------------|--------|----------------|----------------------|
| {{IMPACTED_TEST_FILE_1}} | {{IMPACTED_TEST_METHOD_1}} | MODIFY / DELETE | {{IMPACTED_TEST_ASSERTION_CHANGE_1}} |

### 3.4 O que fazer em cada arquivo

> Para cada arquivo impactado: descrever EM LINGUAGEM NATURAL o que deve ser escrito/alterado.
> Não escrever o código final — descrever a intenção e o resultado esperado.
> Seguir a ordem: camadas internas primeiro (Domain → Port → Adapter → Application → Config → Test).

**`{{FILE_PATH_1}}`** ({{FILE_ACTION_1}} — {{FILE_LAYER_1}})
> {{FILE_CHANGE_DESCRIPTION_1}}

**`{{FILE_PATH_2}}`** ({{FILE_ACTION_2}} — {{FILE_LAYER_2}})
> {{FILE_CHANGE_DESCRIPTION_2}}

### 3.5 Ordem de Implementação

> Em que sequência criar/modificar os arquivos? Regra: camadas internas primeiro.

| Passo | Arquivo | Motivo da Ordem |
|-------|---------|----------------|
| 1 | `{{IMPL_FILE_1}}` | {{IMPL_REASON_1}} |
| 2 | `{{IMPL_FILE_2}}` | {{IMPL_REASON_2}} |

---

## 4. Ciclos TDD (Ordem TPP)

> RED → GREEN → REFACTOR para cada ciclo.
> Ordem obrigatória: nil → constant → scalar → conditional → collection → complex.
> Mínimo 3 ciclos. Ciclo 1 SEMPRE degenerate (caso nulo/vazio/inválido).
> Cada ciclo descrito em linguagem natural: O QUÊ testar, POR QUE falha, O QUÊ implementar.

### Ciclo 1 — Degenerate (`{} → nil`)

**RED — Teste a escrever:**
Nome: `{{TDD_C1_TEST_NAME}}`
O que testar: {{TDD_C1_TEST_DESCRIPTION}}
Por que vai falhar: {{TDD_C1_FAILURE_REASON}}
Comando: `{{TEST_COMMAND}}`

**GREEN — Implementação mínima:**
{{TDD_C1_GREEN_DESCRIPTION}}
Comando: `{{COMPILE_COMMAND}}` → `{{TEST_COMMAND}}`

**REFACTOR:**
{{TDD_C1_REFACTOR_DESCRIPTION}}

**Commit:** `{{TDD_C1_COMMIT_MESSAGE}}`

---

### Ciclo 2 — Constant (`nil → constant`)

**RED — Teste a escrever:**
Nome: `{{TDD_C2_TEST_NAME}}`
O que testar: {{TDD_C2_TEST_DESCRIPTION}}
Por que vai falhar: {{TDD_C2_FAILURE_REASON}}
Comando: `{{TEST_COMMAND}}`

**GREEN — Implementação mínima:**
{{TDD_C2_GREEN_DESCRIPTION}}
Comando: `{{COMPILE_COMMAND}}` → `{{TEST_COMMAND}}`

**REFACTOR:**
{{TDD_C2_REFACTOR_DESCRIPTION}}

**Commit:** `{{TDD_C2_COMMIT_MESSAGE}}`

---

### Ciclo N — {{TDD_CN_LEVEL}} (`{{TDD_CN_TRANSFORM}}`)

**RED — Teste a escrever:**
Nome: `{{TDD_CN_TEST_NAME}}`
O que testar: {{TDD_CN_TEST_DESCRIPTION}}
Por que vai falhar: {{TDD_CN_FAILURE_REASON}}
Comando: `{{TEST_COMMAND}}`

**GREEN — Implementação mínima:**
{{TDD_CN_GREEN_DESCRIPTION}}
Comando: `{{COMPILE_COMMAND}}` → `{{TEST_COMMAND}}`

**REFACTOR:**
{{TDD_CN_REFACTOR_DESCRIPTION}}

**Commit:** `{{TDD_CN_COMMIT_MESSAGE}}`

---

## 5. Checklist de Segurança

> Adaptada ao tipo da task (Endpoint/API, Persistence/DB, Domain Logic, Config, Integration).
> Marcar como [ ] = pendente, [x] = verificado/não-aplicável com justificativa.

- [ ] {{SECURITY_ITEM_1}} ({{SECURITY_SEVERITY_1}})
- [ ] {{SECURITY_ITEM_2}} ({{SECURITY_SEVERITY_2}})
- [ ] {{SECURITY_ITEM_3}} ({{SECURITY_SEVERITY_3}})

---

## 6. File Footprint

> Consumido por `x-evaluate-parallelism` para detectar conflitos entre tasks paralelas.

### write:
{{FILE_FOOTPRINT_WRITE}}

### read:
{{FILE_FOOTPRINT_READ}}

### regen:
{{FILE_FOOTPRINT_REGEN}}

---

## 7. Dependências

| Depende de | Motivo |
|-----------|--------|
| {{DEPENDENCY_TASK_ID_1}} | {{DEPENDENCY_REASON_1}} |

---

## 8. Critérios de Conclusão desta Task

Ao terminar esta task, o executor DEVE:
- [ ] Todos os {{TDD_CYCLE_COUNT}} ciclos TDD completados (RED → GREEN → REFACTOR)
- [ ] Todos os testes passando: `{{TEST_COMMAND}}`
- [ ] Artefato válido/compilando: `{{COMPILE_COMMAND}}`
- [ ] Checklist de segurança (Seção 5) verificada
- [ ] Testes existentes impactados (Seção 3.3) modificados/excluídos conforme mapeado
- [ ] Nenhum TODO/FIXME/HACK no escopo desta task
- [ ] Critérios de aceite da story aplicáveis a esta task satisfeitos
- [ ] Escrever `**Status:** Concluída` no arquivo `task-{{TASK_ID}}.md`
- [ ] Atualizar `execution-state.json`: `tasks.{{TASK_ID}}.status = COMPLETE`
