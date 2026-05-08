---
requires-capabilities: [governance.bug-lifecycle]
template-version: "1.0"
template-type: bug-story
ra9-sections: 9
bug-story-kind: {{BUG_STORY_KIND}}
---

# História: {{BUG_STORY_TITLE}}

**ID:** story-{{BUG_ID}}-{{STORY_SEQUENCE}}
**Tipo:** {{BUG_STORY_KIND}}
**Status:** Pendente
**Bug Pai:** bug-{{BUG_ID}}
**Branch:** bug/{{BUG_ID}}/story-{{STORY_SEQUENCE}}

> **Status Transitions:**
> `Pendente | Refinada | Planejada | Em Andamento | Concluída | Falha | Bloqueada`

---

## 1. Visão (Vision)

> **As a** {{PERSONA}},
> **I need** {{BUG_STORY_OBJECTIVE}},
> **so that** {{BUG_STORY_VALUE}}.

**Bug Reference:** `ai/bugs/bug-{{BUG_ID}}/bug.md`

**Story Kind:** {{BUG_STORY_KIND}}

---

## 2. Persona & Cenário de Uso (Persona & Usage Scenario)

**Persona:** {{PERSONA}}

**Cenário:** {{BUG_SCENARIO}}

**Precondições:**
- Bug `bug-{{BUG_ID}}` exists and is in status `Refinada` or later
- Bug has a complete Reproduction Recipe (Section 5 filled)
{{STORY_PRECONDITIONS}}

---

## 3. Entrega de Valor (Value Delivery)

- **Valor Principal:** {{BUG_STORY_VALUE}}
- **Métrica de Sucesso:** {{SUCCESS_METRIC}}
- **Impacto no Negócio:** {{BUSINESS_IMPACT}}
- **KPI Link:** Bug `bug-{{BUG_ID}}` resolution contributes to P75 resolution time ≤ 8 minutes.

---

## 4. Critérios de Aceite (Acceptance Criteria)

### AC-1 ({{BUG_STORY_KIND}} primary) — {{AC_1_TITLE}}

```gherkin
Given {{AC_1_GIVEN}}
When {{AC_1_WHEN}}
Then {{AC_1_THEN}}
And the regression test {{AC_1_TEST_ASSERTION}}
```

### AC-2 (regression guard) — No regressions introduced

```gherkin
Given the implementation is applied
When the existing test suite runs
Then all pre-existing tests pass
And coverage remains ≥ 95% line / ≥ 90% branch
```

---

## 5. Reproduction Recipe

> Inherited from bug-{{BUG_ID}}. Fill in fields specific to this story's scope.

### 5.1 Environment

| Field | Value |
| :---- | :---- |
| Version/Commit | {{VERSION_OR_SHA}} |
| OS / Runtime | {{OS_RUNTIME}} |
| Java Version | {{JAVA_VERSION}} |
| Reproduces? | Always / Intermittent / Once |

### 5.2 Scope of This Story

```
{{STORY_SCOPE_DESCRIPTION}}
```

### 5.3 Observed vs. Expected (from parent bug)

| | Behavior |
| :-- | :-- |
| **Observed** | {{OBSERVED_BEHAVIOR}} |
| **Expected** | {{EXPECTED_BEHAVIOR}} |

### 5.4 Artifacts from Parent Bug

- Logs: {{LOG_SNIPPET_OR_PATH}}
- Stack trace: {{STACK_TRACE_OR_N_A}}
- Bug file: `ai/bugs/bug-{{BUG_ID}}/bug.md`

---

## 6. Root-Cause Context (from bug-{{BUG_ID}})

> Populated during bug investigation. Do not fill before root-cause is confirmed.

### 6.1 Summary

{{ROOT_CAUSE_SUMMARY}}

### 6.2 Affected Components for this Story

| Component | File / Class | Story Role |
| :-------- | :----------- | :--------- |
| {{COMPONENT}} | {{FILE}} | {{STORY_ROLE}} |

### 6.3 Story Relationship to Root-Cause

| Attribute | Value |
| :-------- | :---- |
| Story contribution | Test only / Fix only / Both / Doc / Rollback |
| Root cause confirmed? | No (hypothesis) / Yes |

---

## 7. Dependências (Dependencies)

- **Blocked By:** {{BLOCKED_BY}}
- **Blocks:** {{BLOCKS_OR_NONE}}
- **Parent Bug:** `bug-{{BUG_ID}}`

---

## 8. Tasks

> Story-level task breakdown. Populated during refinement.

| Task ID | Type | Description |
| :------ | :--- | :---------- |
| {{TASK_ID}} | {{TASK_TYPE}} | {{TASK_DESCRIPTION}} |

---

## 9. Histórico de Decisão (Decision History)

> Decisions made during story scoping and implementation.

| Date | Decision | Rationale |
| :--- | :------- | :-------- |
| {{DATE}} | {{DECISION}} | {{RATIONALE}} |

---

## Refinement Verdict

```yaml
status: pending
verdictHash: ""
refinedBy: ""
refinedAt: ""
blockers: []
notes: ""
```
