# Feature: {{FEATURE_NAME}}

**Feature ID:** {{FEATURE_ID}}  
**Capability:** {{CAPABILITY_ID}}  
**Status:** Draft

---

## 1. Feature Statement

Como **{{PERSONA}}**, eu quero **{{FEATURE_ACTION}}**, para que **{{FEATURE_BENEFIT}}**.

### 1.1 Contexto

{{CONTEXT_NARRATIVE}}

### 1.2 Escopo

**In-scope:**
- {{IN_SCOPE_1}}
- {{IN_SCOPE_2}}

**Out-of-scope:**
- {{OUT_SCOPE_1}}

---

## 2. Casos de Uso

> Mínimo 3, máximo 8 use cases por feature.

### UC-001: {{USE_CASE_1_TITLE}}

| Campo | Valor |
| :--- | :--- |
| **Ator** | {{ACTOR_1}} |
| **Ação** | I want to {{ACTION_1}} |
| **Benefício** | so that {{BENEFIT_1}} |

### UC-002: {{USE_CASE_2_TITLE}}

| Campo | Valor |
| :--- | :--- |
| **Ator** | {{ACTOR_2}} |
| **Ação** | I want to {{ACTION_2}} |
| **Benefício** | so that {{BENEFIT_2}} |

### UC-003: {{USE_CASE_3_TITLE}}

| Campo | Valor |
| :--- | :--- |
| **Ator** | {{ACTOR_3}} |
| **Ação** | I want to {{ACTION_3}} |
| **Benefício** | so that {{BENEFIT_3}} |

---

## 3. Requisitos Funcionais

| ID | Requisito | Prioridade | UC |
| :--- | :--- | :--- | :--- |
| RF-001 | {{REQUIREMENT_1}} | Must | UC-001 |
| RF-002 | {{REQUIREMENT_2}} | Must | UC-001, UC-002 |
| RF-003 | {{REQUIREMENT_3}} | Should | UC-003 |

---

## 4. Interfaces Exposed

### 4.1 Input Contract

| Campo | Tipo | M/O | Validações |
| :--- | :--- | :--- | :--- |
| `{{INPUT_FIELD_1}}` | `{{TYPE_1}}` | `M` | {{VALIDATION_1}} |
| `{{INPUT_FIELD_2}}` | `{{TYPE_2}}` | `O` | {{VALIDATION_2}} |

### 4.2 Output Contract

| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `{{OUTPUT_FIELD_1}}` | `{{TYPE_O1}}` | {{DESCRIPTION_O1}} |
| `{{OUTPUT_FIELD_2}}` | `{{TYPE_O2}}` | {{DESCRIPTION_O2}} |

### 4.3 Events Emitted

| Evento | Trigger | Payload |
| :--- | :--- | :--- |
| `{{EVENT_1}}` | {{TRIGGER_1}} | `{ {{FIELD}}: {{TYPE}} }` |

---

## 5. Acceptance Criteria Detalhados

> Mínimo 10 acceptance criteria em Gherkin, cobrindo: happy path, error/boundary, performance/SLA, security/auth.

### Happy Path

```gherkin
Cenário: {{HAPPY_PATH_SCENARIO_1}}
  DADO que {{GIVEN_1}}
  QUANDO {{WHEN_1}}
  ENTÃO {{THEN_1}}
  E {{AND_1}}

Cenário: {{HAPPY_PATH_SCENARIO_2}}
  DADO que {{GIVEN_2}}
  QUANDO {{WHEN_2}}
  ENTÃO {{THEN_2}}
```

### Error & Boundary

```gherkin
Cenário: {{ERROR_SCENARIO_1}}
  DADO que {{GIVEN_E1}}
  QUANDO {{WHEN_E1}}
  ENTÃO erro "{{ERROR_MESSAGE_1}}" é retornado

Cenário: {{BOUNDARY_SCENARIO}}
  DADO que {{GIVEN_B}}
  QUANDO {{WHEN_B}}
  ENTÃO {{THEN_B}}
```

### Performance & SLA

```gherkin
Cenário: {{PERFORMANCE_SCENARIO}}
  DADO que {{GIVEN_P}}
  QUANDO {{WHEN_P}}
  ENTÃO resposta em menos de {{SLA_MS}}ms (P99)
```

### Security & Auth

```gherkin
Cenário: {{SECURITY_SCENARIO}}
  DADO que usuário não autenticado
  QUANDO {{WHEN_S}}
  ENTÃO acesso é negado com HTTP 401
```

---

## 6. Estimativa & Roadmap

| Componente | Esforço | Sprint |
| :--- | :--- | :--- |
| {{COMPONENT_1}} | {{EFFORT_1}} | {{SPRINT_1}} |
| {{COMPONENT_2}} | {{EFFORT_2}} | {{SPRINT_1}} |
| {{COMPONENT_3}} | {{EFFORT_3}} | {{SPRINT_2}} |

**Total estimado:** {{TOTAL_EFFORT}}

**Critério de release:** {{RELEASE_CRITERIA}}

---

## 7. Riscos & Mitigação

| Risco | Probabilidade | Impacto | Mitigação |
| :--- | :--- | :--- | :--- |
| {{RISK_1}} | {{PROB_1}} | {{IMPACT_1}} | {{MITIGATION_1}} |
| {{RISK_2}} | {{PROB_2}} | {{IMPACT_2}} | {{MITIGATION_2}} |
